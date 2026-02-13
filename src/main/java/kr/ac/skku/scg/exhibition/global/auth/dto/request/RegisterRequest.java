package kr.ac.skku.scg.exhibition.global.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotNull RegisterUserType userType,
        @NotBlank String name,
        String email,
        String department,
        String phoneNumber,
        String studentNumber
) {
}
