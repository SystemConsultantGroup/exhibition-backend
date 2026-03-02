package kr.ac.skku.scg.exhibition.global.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(@NotBlank String code, String redirectUri) {
}
