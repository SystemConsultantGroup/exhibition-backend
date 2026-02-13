package kr.ac.skku.scg.exhibition.global.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String name,
        String email,
        String department,
        String phoneNumber,
        String studentNumber
) {
}
