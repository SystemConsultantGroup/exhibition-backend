package kr.ac.skku.scg.exhibition.global.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import kr.ac.skku.scg.exhibition.global.auth.config.AuthProperties;
import kr.ac.skku.scg.exhibition.global.error.UnauthorizedException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoAuthClient {

    private final RestClient restClient;
    private final AuthProperties authProperties;

    public KakaoAuthClient(RestClient authRestClient, AuthProperties authProperties) {
        this.restClient = authRestClient;
        this.authProperties = authProperties;
    }

    public KakaoUserProfile getUserProfile(String code) {
        AuthProperties.Kakao kakao = authProperties.getKakao();
        validateKakaoProperties(kakao);

        KakaoTokenResponse tokenResponse = requestToken(kakao, code);
        KakaoUserResponse userResponse = requestUserInfo(kakao, tokenResponse.accessToken());

        String providerId = userResponse.id() == null ? null : userResponse.id().toString();
        if (providerId == null || providerId.isBlank()) {
            throw new UnauthorizedException("Kakao user id is missing");
        }

        String name = userResponse.properties() == null ? null : userResponse.properties().nickname();
        String email = userResponse.kakaoAccount() == null ? null : userResponse.kakaoAccount().email();

        return new KakaoUserProfile(providerId, name, email);
    }

    private KakaoTokenResponse requestToken(AuthProperties.Kakao kakao, String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakao.getClientId());
        form.add("code", code);
        form.add("redirect_uri", kakao.getRedirectUri());
        if (kakao.getClientSecret() != null && !kakao.getClientSecret().isBlank()) {
            form.add("client_secret", kakao.getClientSecret());
        }

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(kakao.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new UnauthorizedException("Kakao access token is missing");
            }

            return response;
        } catch (Exception ex) {
            throw new UnauthorizedException("Failed to request Kakao token");
        }
    }

    private KakaoUserResponse requestUserInfo(AuthProperties.Kakao kakao, String accessToken) {
        try {
            KakaoUserResponse response = restClient.post()
                    .uri(kakao.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null) {
                throw new UnauthorizedException("Kakao user info response is empty");
            }
            return response;
        } catch (Exception ex) {
            throw new UnauthorizedException("Failed to request Kakao user info");
        }
    }

    private void validateKakaoProperties(AuthProperties.Kakao kakao) {
        if (kakao.getClientId() == null || kakao.getClientId().isBlank()) {
            throw new IllegalArgumentException("Kakao client id is not configured");
        }
        if (kakao.getRedirectUri() == null || kakao.getRedirectUri().isBlank()) {
            throw new IllegalArgumentException("Kakao redirect uri is not configured");
        }
    }

    private record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    private record KakaoUserResponse(
            Long id,
            KakaoProperties properties,
            @JsonProperty("kakao_account") KakaoAccount kakaoAccount
    ) {
    }

    private record KakaoProperties(String nickname) {
    }

    private record KakaoAccount(String email) {
    }
}
