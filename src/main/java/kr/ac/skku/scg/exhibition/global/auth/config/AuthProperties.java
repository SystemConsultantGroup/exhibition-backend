package kr.ac.skku.scg.exhibition.global.auth.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private final Jwt jwt = new Jwt();
    private final Kakao kakao = new Kakao();

    public Jwt getJwt() {
        return jwt;
    }

    public Kakao getKakao() {
        return kakao;
    }

    public static class Jwt {
        private String secret;
        private long accessTokenExpirationSeconds = 3600;
        private long refreshTokenExpirationSeconds = 1209600;
        private String issuer = "exhibition";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getAccessTokenExpirationSeconds() {
            return accessTokenExpirationSeconds;
        }

        public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
            this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        }

        public long getRefreshTokenExpirationSeconds() {
            return refreshTokenExpirationSeconds;
        }

        public void setRefreshTokenExpirationSeconds(long refreshTokenExpirationSeconds) {
            this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Kakao {
        private String clientId;
        private String clientSecret;
        private List<String> redirectUris = new ArrayList<>();
        private String tokenUri = "https://kauth.kakao.com/oauth/token";
        private String userInfoUri = "https://kapi.kakao.com/v2/user/me";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return getPrimaryRedirectUri();
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUris = parseRedirectUris(redirectUri);
        }

        public List<String> getRedirectUris() {
            return redirectUris;
        }

        public void setRedirectUris(List<String> redirectUris) {
            this.redirectUris = normalizeRedirectUris(redirectUris);
        }

        public String getPrimaryRedirectUri() {
            return redirectUris.isEmpty() ? null : redirectUris.get(0);
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }

        private List<String> parseRedirectUris(String redirectUri) {
            if (redirectUri == null) {
                return new ArrayList<>();
            }
            return normalizeRedirectUris(Arrays.asList(redirectUri.split(",")));
        }

        private List<String> normalizeRedirectUris(List<String> redirectUris) {
            if (redirectUris == null) {
                return new ArrayList<>();
            }
            return redirectUris.stream()
                    .map(uri -> uri == null ? "" : uri.trim())
                    .filter(uri -> !uri.isBlank())
                    .distinct()
                    .toList();
        }
    }
}
