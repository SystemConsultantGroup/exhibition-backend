package kr.ac.skku.scg.exhibition.exhibition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.AdminExhibitionCreateRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.request.AdminExhibitionUpdateRequest;
import kr.ac.skku.scg.exhibition.exhibition.dto.response.ExhibitionResponse;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.service.AdminAuthorizationService;
import kr.ac.skku.scg.exhibition.global.error.ConflictException;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionDomainCacheService;
import kr.ac.skku.scg.exhibition.media.repository.MediaAssetRepository;
import kr.ac.skku.scg.exhibition.media.service.MediaService;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserExhibitionAdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminExhibitionServiceTest {

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private UserExhibitionAdminRepository userExhibitionAdminRepository;

    @Mock
    private AdminAuthorizationService adminAuthorizationService;

    @Mock
    private ExhibitionDomainCacheService exhibitionDomainCacheService;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private MediaService mediaService;

    private AdminExhibitionService adminExhibitionService;

    @BeforeEach
    void setUp() {
        adminExhibitionService = new AdminExhibitionService(
                exhibitionRepository,
                userExhibitionAdminRepository,
                adminAuthorizationService,
                exhibitionDomainCacheService,
                mediaAssetRepository,
                mediaService
        );
    }

    @Test
    void listAccessible_returnsAllExhibitionsForAdmin() {
        AuthenticatedUser admin = user(UserType.ADMIN);
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        when(exhibitionRepository.findAllByOrderByNameAsc()).thenReturn(List.of(exhibition));

        List<ExhibitionResponse> result = adminExhibitionService.listAccessible(admin);

        assertThat(result).extracting(ExhibitionResponse::id).containsExactly(exhibition.getId());
        verifyNoInteractions(userExhibitionAdminRepository);
    }

    @Test
    void listAccessible_returnsAssignedExhibitionsForSubAdmin() {
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "slug", "전시");
        when(userExhibitionAdminRepository.findExhibitionsByUserId(subAdmin.id())).thenReturn(List.of(exhibition));

        List<ExhibitionResponse> result = adminExhibitionService.listAccessible(subAdmin);

        assertThat(result).extracting(ExhibitionResponse::id).containsExactly(exhibition.getId());
        verifyNoInteractions(exhibitionRepository);
    }

    @Test
    void listAccessible_rejectsNonAdmin() {
        assertThatThrownBy(() -> adminExhibitionService.listAccessible(user(UserType.STUDENT)))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(exhibitionRepository, userExhibitionAdminRepository);
    }

    @Test
    void create_savesNormalizedExhibition() {
        AdminExhibitionCreateRequest request = request();
        AuthenticatedUser admin = user(UserType.ADMIN);
        when(exhibitionRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ExhibitionEntity input = invocation.getArgument(0);
            return new ExhibitionEntity(
                    UUID.randomUUID(), input.getSlug(), input.getDefaultDomain(), input.getCustomDomain(),
                    input.getName(), input.getDescription());
        });

        ExhibitionResponse response = adminExhibitionService.create(request, null, null, null, null, admin);

        assertThat(response.slug()).isEqualTo("graduation-2026");
        assertThat(response.defaultDomain()).isEqualTo("graduation.example.com");
        assertThat(response.customDomain()).isEqualTo("custom.example.com");
        verify(adminAuthorizationService).requireSuperAdmin(admin);
        verify(exhibitionDomainCacheService).evictAll();
    }

    @Test
    void create_uploadsProvidedMediaFiles() {
        AuthenticatedUser admin = user(UserType.ADMIN);
        MockMultipartFile logo = new MockMultipartFile("logoFile", "logo.png", "image/png", new byte[] {1});
        when(exhibitionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        adminExhibitionService.create(request(), logo, null, null, null, admin);

        verify(mediaService).uploadForExhibition(any(), org.mockito.ArgumentMatchers.same(logo),
                org.mockito.ArgumentMatchers.eq("logo"), org.mockito.ArgumentMatchers.eq("image"));
    }

    @Test
    void create_rejectsDuplicateSlug() {
        AuthenticatedUser admin = user(UserType.ADMIN);
        when(exhibitionRepository.existsBySlug("graduation-2026")).thenReturn(true);

        assertThatThrownBy(() -> adminExhibitionService.create(request(), null, null, null, null, admin))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("slug");
    }

    @Test
    void update_allowsAssignedAdminAndNormalizesDomains() {
        UUID exhibitionId = UUID.randomUUID();
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        ExhibitionEntity exhibition = new ExhibitionEntity(
                exhibitionId, "slug", "old.example.com", null, "기존", null);
        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));

        ExhibitionResponse response = adminExhibitionService.update(exhibitionId,
                new AdminExhibitionUpdateRequest("NEW.EXAMPLE.COM", "", "새 이름", "설명",
                        null, true, null, true, null, "https://example.com", "인트로", "소개", null), subAdmin);

        verify(adminAuthorizationService).requireExhibitionAdmin(subAdmin, exhibitionId);
        assertThat(response.defaultDomain()).isEqualTo("new.example.com");
        assertThat(response.customDomain()).isNull();
        assertThat(response.name()).isEqualTo("새 이름");
    }

    @Test
    void delete_removesExhibitionAndAssignments() {
        UUID exhibitionId = UUID.randomUUID();
        AuthenticatedUser admin = user(UserType.ADMIN);
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        when(exhibitionRepository.findById(exhibitionId)).thenReturn(Optional.of(exhibition));

        adminExhibitionService.delete(exhibitionId, admin);

        verify(adminAuthorizationService).requireSuperAdmin(admin);
        verify(userExhibitionAdminRepository).deleteAllByExhibition_Id(exhibitionId);
        verify(exhibitionRepository).delete(exhibition);
        verify(exhibitionRepository).flush();
        verify(exhibitionDomainCacheService).evictAll();
    }

    @Test
    void create_stopsAfterAuthorizationFailure() {
        AuthenticatedUser subAdmin = user(UserType.SUB_ADMIN);
        org.mockito.Mockito.doThrow(new kr.ac.skku.scg.exhibition.global.error.ForbiddenException("forbidden"))
                .when(adminAuthorizationService).requireSuperAdmin(subAdmin);

        assertThatThrownBy(() -> adminExhibitionService.create(request(), null, null, null, null, subAdmin))
                .isInstanceOf(kr.ac.skku.scg.exhibition.global.error.ForbiddenException.class);
        verifyNoInteractions(exhibitionRepository);
    }

    private AdminExhibitionCreateRequest request() {
        return new AdminExhibitionCreateRequest(
                "graduation-2026",
                "Graduation.Example.com",
                "Custom.Example.com",
                "졸업 전시",
                "설명",
                true,
                true,
                "https://example.com",
                "인트로",
                "소개"
        );
    }

    private AuthenticatedUser user(UserType role) {
        return new AuthenticatedUser(UUID.randomUUID(), "user", role, null, null, null, null, true);
    }
}
