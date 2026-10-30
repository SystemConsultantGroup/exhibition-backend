package kr.ac.skku.scg.exhibition.category.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.category.dto.request.AdminCategoryOrderRequest;
import kr.ac.skku.scg.exhibition.category.dto.request.AdminCategoryUpdateRequest;
import kr.ac.skku.scg.exhibition.category.dto.request.CategoryListRequest;
import kr.ac.skku.scg.exhibition.category.dto.response.CategoryResponse;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.error.ConflictException;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public CategoryService(CategoryRepository categoryRepository, AdminAuthorizationService adminAuthorizationService) {
        this.categoryRepository = categoryRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    /** 전시회의 카테고리 하나를 조회한다. */
    public CategoryResponse get(UUID id, UUID exhibitionId) {
        return toResponse(find(id, exhibitionId));
    }

    /** 전시회의 카테고리를 정렬된 순서로 조회한다. */
    public List<CategoryResponse> list(CategoryListRequest request) {
        return categoryRepository.findAllByExhibition_Id(request.getExhibitionId()).stream().map(this::toResponse).toList();
    }

    /** 전시회 범위에서 중복되지 않는 카테고리를 생성한다. */
    @Transactional
    public CategoryResponse create(ExhibitionEntity exhibition, AdminCategoryUpdateRequest request, AuthenticatedUser user) {
        adminAuthorizationService.requireExhibitionAdmin(user, exhibition.getId());
        CategoryEntity category = new CategoryEntity(null, exhibition, request.name().trim());
        if (categoryRepository.findByExhibition_IdAndName(exhibition.getId(), category.getName()).isPresent()
                || categoryRepository.findByExhibition_IdAndSlug(exhibition.getId(), category.getSlug()).isPresent()) {
            throw new ConflictException("Category name or slug already exists: " + category.getName());
        }
        try {
            return toResponse(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Category name or slug already exists: " + category.getName());
        }
    }

    /** 참조 중인 작품이 없는 카테고리만 삭제한다. */
    @Transactional
    public void delete(UUID id, UUID exhibitionId, AuthenticatedUser user) {
        adminAuthorizationService.requireExhibitionAdmin(user, exhibitionId);
        CategoryEntity category = find(id, exhibitionId);
        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Category is referenced and cannot be deleted: " + id);
        }
    }

    /** 카테고리 이름과 슬러그를 수정한다. */
    @Transactional
    public CategoryResponse update(UUID id, UUID exhibitionId, AdminCategoryUpdateRequest request, AuthenticatedUser user) {
        adminAuthorizationService.requireExhibitionAdmin(user, exhibitionId);
        CategoryEntity category = find(id, exhibitionId);
        String name = request.name().trim();
        CategoryEntity candidate = new CategoryEntity(null, category.getExhibition(), name);
        if (categoryRepository.existsByExhibition_IdAndNameAndIdNot(exhibitionId, name, id)
                || categoryRepository.existsByExhibition_IdAndSlugAndIdNot(exhibitionId, candidate.getSlug(), id)) {
            throw new ConflictException("Category name or slug already exists: " + name);
        }
        try {
            category.updateName(name);
            categoryRepository.flush();
            return toResponse(category);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Category name or slug already exists: " + name);
        }
    }

    /** 전체 카테고리의 createdAt을 요청 순서대로 다시 기록한다. */
    @Transactional
    public List<CategoryResponse> reorder(UUID exhibitionId, AdminCategoryOrderRequest request, AuthenticatedUser user) {
        adminAuthorizationService.requireExhibitionAdmin(user, exhibitionId);
        List<UUID> ids = request.categoryIds();
        List<CategoryEntity> categories = categoryRepository.findAllByExhibition_Id(exhibitionId);
        if (new HashSet<>(ids).size() != ids.size() || ids.size() != categories.size()
                || !new HashSet<>(ids).equals(categories.stream().map(CategoryEntity::getId).collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalArgumentException("categoryIds must contain every exhibition category exactly once");
        }
        java.util.Map<UUID, CategoryEntity> byId = categories.stream()
                .collect(java.util.stream.Collectors.toMap(CategoryEntity::getId, category -> category));
        Instant first = Instant.now().minus(ids.size() + 1L, ChronoUnit.SECONDS);
        for (int i = 0; i < ids.size(); i++) {
            byId.get(ids.get(i)).reorderAt(first.plusSeconds(i));
        }
        categoryRepository.flush();
        return ids.stream().map(id -> toResponse(byId.get(id))).toList();
    }

    /** 전시회 범위에서 카테고리를 찾는다. */
    private CategoryEntity find(UUID id, UUID exhibitionId) {
        return categoryRepository.findByIdAndExhibition_Id(id, exhibitionId)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    /** 카테고리 응답을 만든다. */
    private CategoryResponse toResponse(CategoryEntity category) {
        return new CategoryResponse(category.getId(), category.getExhibition().getId(), category.getSlug(), category.getName());
    }
}
