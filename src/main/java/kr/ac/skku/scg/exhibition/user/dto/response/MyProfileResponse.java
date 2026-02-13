package kr.ac.skku.scg.exhibition.user.dto.response;

import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.user.domain.UserType;

public record MyProfileResponse(
        String name,
        UserType role,
        String email,
        String department,
        String phoneNumber,
        String studentNumber,
        boolean registrationCompleted
) {
    public static MyProfileResponse from(AuthenticatedUser user) {
        return new MyProfileResponse(
                user.name(),
                user.role(),
                user.email(),
                user.department(),
                user.phoneNumber(),
                user.studentNumber(),
                user.registrationCompleted()
        );
    }
}
