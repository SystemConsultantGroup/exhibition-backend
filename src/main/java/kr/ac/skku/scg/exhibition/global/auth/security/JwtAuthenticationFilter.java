package kr.ac.skku.scg.exhibition.global.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUserArgumentResolver;
import kr.ac.skku.scg.exhibition.global.auth.service.JwtTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null && !authorization.isBlank()) {
            if (!authorization.startsWith(BEARER_PREFIX)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid authorization header");
                return;
            }

            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            if (token.isBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access token is missing");
                return;
            }

            try {
                UUID userId = jwtTokenService.extractUserIdFromAccessToken(token);
                request.setAttribute(CurrentUserArgumentResolver.REQUEST_ATTR_USER_ID, userId);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(userId, null, List.of())
                );
            } catch (Exception ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
