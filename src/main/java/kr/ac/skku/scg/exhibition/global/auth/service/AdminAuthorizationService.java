package kr.ac.skku.scg.exhibition.global.auth.service;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.error.ForbiddenException;
import kr.ac.skku.scg.exhibition.user.domain.UserType;
import kr.ac.skku.scg.exhibition.user.repository.UserExhibitionAdminRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthorizationService {

    private final UserExhibitionAdminRepository userExhibitionAdminRepository;

    public AdminAuthorizationService(UserExhibitionAdminRepository userExhibitionAdminRepository) {
        this.userExhibitionAdminRepository = userExhibitionAdminRepository;
    }

    /** 슈퍼유저 권한을 확인한다. */
    public void requireSuperAdmin(AuthenticatedUser user) {
        if (user.role() != UserType.ADMIN) {
            throw new ForbiddenException("Super admin role is required");
        }
    }

    /** 슈퍼유저 또는 해당 전시회에 배정된 관리자 권한을 확인한다. */
    public void requireExhibitionAdmin(AuthenticatedUser user, UUID exhibitionId) {
        if (user.role() == UserType.ADMIN) {
            return;
        }
        if (user.role() != UserType.SUB_ADMIN
                || !userExhibitionAdminRepository.existsByUser_IdAndExhibition_Id(user.id(), exhibitionId)) {
            throw new ForbiddenException("Exhibition admin role is required: " + exhibitionId);
        }
    }
}
