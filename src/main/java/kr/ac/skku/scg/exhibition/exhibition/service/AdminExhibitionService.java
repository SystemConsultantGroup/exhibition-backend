package kr.ac.skku.scg.exhibition.exhibition.service;

import java.util.Locale;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.AdminExhibitionCreateRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.AdminExhibitionUpdateRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.error.ConflictException;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionDomainCacheService;
import kr.ac.skku.scg.exhibition.user.repository.UserExhibitionAdminRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class AdminExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final UserExhibitionAdminRepository userExhibitionAdminRepository;
    private final AdminAuthorizationService adminAuthorizationService;
    private final ExhibitionDomainCacheService exhibitionDomainCacheService;

    public AdminExhibitionService(
            ExhibitionRepository exhibitionRepository,
            UserExhibitionAdminRepository userExhibitionAdminRepository,
            AdminAuthorizationService adminAuthorizationService,
            ExhibitionDomainCacheService exhibitionDomainCacheService
    ) {
        this.exhibitionRepository = exhibitionRepository;
        this.userExhibitionAdminRepository = userExhibitionAdminRepository;
        this.adminAuthorizationService = adminAuthorizationService;
        this.exhibitionDomainCacheService = exhibitionDomainCacheService;
    }

    /** 슈퍼유저 권한으로 전시회를 생성한다. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExhibitionResponse create(AdminExhibitionCreateRequest request, AuthenticatedUser currentUser) {
        adminAuthorizationService.requireSuperAdmin(currentUser);

        String slug = request.slug().trim();
        String name = request.name().trim();
        String defaultDomain = normalizeDomain(request.defaultDomain());
        String customDomain = normalizeNullableDomain(request.customDomain());

        if (customDomain != null && customDomain.equals(defaultDomain)) {
            throw new ConflictException("Default domain and custom domain must be different");
        }
        if (exhibitionRepository.existsBySlug(slug)) {
            throw new ConflictException("Exhibition slug already exists: " + slug);
        }
        if (exhibitionRepository.existsByName(name)) {
            throw new ConflictException("Exhibition name already exists: " + name);
        }
        requireAvailableDomain(defaultDomain);
        if (customDomain != null) {
            requireAvailableDomain(customDomain);
        }

        try {
            ExhibitionEntity exhibition = exhibitionRepository.saveAndFlush(new ExhibitionEntity(
                    null, slug, defaultDomain, customDomain, name, request.description()));
            evictCacheAfterCommit();
            return ExhibitionResponse.from(exhibition);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Exhibition slug, name, or domain already exists");
        }
    }

    /** 배정된 관리자가 전시회 기본 정보를 수정한다. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExhibitionResponse update(
            UUID exhibitionId,
            AdminExhibitionUpdateRequest request,
            AuthenticatedUser currentUser
    ) {
        adminAuthorizationService.requireExhibitionAdmin(currentUser, exhibitionId);
        ExhibitionEntity exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new NotFoundException("Exhibition not found: " + exhibitionId));
        String name = request.name().trim();
        String defaultDomain = normalizeDomain(request.defaultDomain());
        String customDomain = normalizeNullableDomain(request.customDomain());

        if (customDomain != null && customDomain.equals(defaultDomain)) {
            throw new ConflictException("Default domain and custom domain must be different");
        }
        if (exhibitionRepository.existsByNameAndIdNot(name, exhibitionId)) {
            throw new ConflictException("Exhibition name already exists: " + name);
        }
        requireAvailableDomain(defaultDomain, exhibitionId);
        if (customDomain != null) {
            requireAvailableDomain(customDomain, exhibitionId);
        }

        try {
            exhibition.updateMetadata(defaultDomain, customDomain, name, request.description());
            exhibitionRepository.flush();
            evictCacheAfterCommit();
            return ExhibitionResponse.from(exhibition);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Exhibition name or domain already exists");
        }
    }

    /** 슈퍼유저 권한으로 비어 있는 전시회를 삭제한다. */
    @Transactional
    public void delete(UUID exhibitionId, AuthenticatedUser currentUser) {
        adminAuthorizationService.requireSuperAdmin(currentUser);

        ExhibitionEntity exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new NotFoundException("Exhibition not found: " + exhibitionId));
        userExhibitionAdminRepository.deleteAllByExhibition_Id(exhibitionId);

        try {
            exhibitionRepository.delete(exhibition);
            exhibitionRepository.flush();
            evictCacheAfterCommit();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Exhibition contains data and cannot be deleted: " + exhibitionId);
        }
    }

    /** 트랜잭션 커밋 뒤 도메인 캐시를 비운다. */
    private void evictCacheAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            exhibitionDomainCacheService.evictAll();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                exhibitionDomainCacheService.evictAll();
            }
        });
    }

    /** 도메인이 다른 전시회에서 사용 중인지 확인한다. */
    private void requireAvailableDomain(String domain) {
        if (exhibitionRepository.existsByDefaultDomainIgnoreCaseOrCustomDomainIgnoreCase(domain, domain)) {
            throw new ConflictException("Exhibition domain already exists: " + domain);
        }
    }

    /** 수정 대상 외 전시회가 도메인을 사용하는지 확인한다. */
    private void requireAvailableDomain(String domain, UUID exhibitionId) {
        if (exhibitionRepository.existsByDomainUsedByAnotherExhibition(domain, exhibitionId)) {
            throw new ConflictException("Exhibition domain already exists: " + domain);
        }
    }

    /** 입력 도메인을 비교 가능한 호스트 형식으로 정규화한다. */
    private String normalizeDomain(String domain) {
        return domain.trim().toLowerCase(Locale.ROOT);
    }

    /** 선택 입력 도메인을 정규화하고 빈 값은 null로 변환한다. */
    private String normalizeNullableDomain(String domain) {
        return StringUtils.hasText(domain) ? normalizeDomain(domain) : null;
    }
}
