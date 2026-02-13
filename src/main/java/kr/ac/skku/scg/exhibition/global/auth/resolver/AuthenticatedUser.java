package kr.ac.skku.scg.exhibition.global.auth.resolver;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.domain.UserType;

public record AuthenticatedUser(
        UUID id,
        String name,
        UserType role,
        String email,
        String department,
        String phoneNumber,
        String studentNumber,
        boolean registrationCompleted
) {
    public static AuthenticatedUser from(UserEntity user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.getEmail(),
                user.getDepartment(),
                user.getPhoneNumber(),
                user.getStudentNumber(),
                user.isRegistrationCompleted()
        );
    }
}
