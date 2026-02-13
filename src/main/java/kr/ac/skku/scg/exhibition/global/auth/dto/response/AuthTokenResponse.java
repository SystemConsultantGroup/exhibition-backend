package kr.ac.skku.scg.exhibition.global.auth.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        boolean registrationRequired
) {
}
