package kr.ac.skku.scg.exhibition.global.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import kr.ac.skku.scg.exhibition.global.auth.config.AuthProperties;
import kr.ac.skku.scg.exhibition.global.error.UnauthorizedException;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final AuthProperties authProperties;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    public String createAccessToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(authProperties.getJwt().getAccessTokenExpirationSeconds());

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuer(authProperties.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(authProperties.getJwt().getRefreshTokenExpirationSeconds());

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuer(authProperties.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    public UUID extractUserIdFromAccessToken(String token) {
        Claims claims = parseClaims(token);
        assertTokenType(claims, TOKEN_TYPE_ACCESS);
        return UUID.fromString(claims.getSubject());
    }

    public UUID extractUserIdFromRefreshToken(String token) {
        Claims claims = parseClaims(token);
        assertTokenType(claims, TOKEN_TYPE_REFRESH);
        return UUID.fromString(claims.getSubject());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ex) {
            throw new UnauthorizedException("Invalid token");
        }
    }

    private void assertTokenType(Claims claims, String expectedType) {
        Object tokenType = claims.get(CLAIM_TOKEN_TYPE);
        if (!expectedType.equals(tokenType)) {
            throw new UnauthorizedException("Invalid token type");
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return authProperties.getJwt().getAccessTokenExpirationSeconds();
    }

    public long getRefreshTokenExpirationSeconds() {
        return authProperties.getJwt().getRefreshTokenExpirationSeconds();
    }

    private SecretKey getSigningKey() {
        String secret = authProperties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
