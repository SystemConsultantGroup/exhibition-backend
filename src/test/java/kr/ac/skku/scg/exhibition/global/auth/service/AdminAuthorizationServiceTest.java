package kr.ac.skku.scg.exhibition.global.auth.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserExhibitionAdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private UserExhibitionAdminRepository userExhibitionAdminRepository;

    private AdminAuthorizationService adminAuthorizationService;

    @BeforeEach
    void setUp() {
        adminAuthorizationService = new AdminAuthorizationService(userExhibitionAdminRepository);
    }

    @Test
    void requireSuperAdmin_allowsAdminOnly() {
        assertThatNoException().isThrownBy(() -> adminAuthorizationService.requireSuperAdmin(user(UserType.ADMIN)));
        assertThatThrownBy(() -> adminAuthorizationService.requireSuperAdmin(user(UserType.SUB_ADMIN)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireExhibitionAdmin_allowsSuperAdminWithoutMapping() {
        adminAuthorizationService.requireExhibitionAdmin(user(UserType.ADMIN), UUID.randomUUID());
        verifyNoInteractions(userExhibitionAdminRepository);
    }

    @Test
    void requireExhibitionAdmin_allowsAssignedSubAdmin() {
        AuthenticatedUser user = user(UserType.SUB_ADMIN);
        UUID exhibitionId = UUID.randomUUID();
        when(userExhibitionAdminRepository.existsByUser_IdAndExhibition_Id(user.id(), exhibitionId)).thenReturn(true);

        assertThatNoException().isThrownBy(
                () -> adminAuthorizationService.requireExhibitionAdmin(user, exhibitionId));
    }

    @Test
    void requireExhibitionAdmin_rejectsUnassignedUser() {
        AuthenticatedUser user = user(UserType.SUB_ADMIN);
        UUID exhibitionId = UUID.randomUUID();
        when(userExhibitionAdminRepository.existsByUser_IdAndExhibition_Id(user.id(), exhibitionId)).thenReturn(false);

        assertThatThrownBy(() -> adminAuthorizationService.requireExhibitionAdmin(user, exhibitionId))
                .isInstanceOf(ForbiddenException.class);
    }

    private AuthenticatedUser user(UserType role) {
        return new AuthenticatedUser(
                UUID.randomUUID(), "user", role, null, null, null, null, true);
    }
}
