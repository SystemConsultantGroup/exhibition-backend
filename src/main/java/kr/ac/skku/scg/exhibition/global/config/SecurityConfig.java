package kr.ac.skku.scg.exhibition.global.config;

import java.util.List;
import java.util.Locale;
import kr.ac.skku.scg.exhibition.exhibition.repository.ExhibitionRepository;
import kr.ac.skku.scg.exhibition.global.auth.security.JwtAuthenticationFilter;
import kr.ac.skku.scg.exhibition.global.auth.service.JwtTokenService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(ObjectProvider<ExhibitionRepository> exhibitionRepositoryProvider) {
        return request -> {
            String origin = request.getHeader(HttpHeaders.ORIGIN);
            if (origin == null || origin.isBlank()) {
                return null;
            }

            String host = request.getHeader(HttpHeaders.ORIGIN);
            try {
                host = java.net.URI.create(origin.trim()).getHost();
            } catch (IllegalArgumentException ignored) {
                return null;
            }

            if (host == null || host.isBlank()) {
                return null;
            }

            if (!isAllowedOrigin(host.toLowerCase(Locale.ROOT), exhibitionRepositoryProvider.getIfAvailable())) {
                return null;
            }

            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(List.of(origin));
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("*"));
            configuration.setExposedHeaders(List.of("Authorization"));
            configuration.setAllowCredentials(true);
            configuration.setMaxAge(3600L);
            return configuration;
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            ObjectProvider<JwtTokenService> jwtTokenServiceProvider) throws Exception {
        JwtTokenService jwtTokenService = jwtTokenServiceProvider.getIfAvailable();

        if (jwtTokenService != null) {
            JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenService);
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    private boolean isAllowedOrigin(String host, ExhibitionRepository exhibitionRepository) {
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            return true;
        }
        return exhibitionRepository != null
                && exhibitionRepository.existsByDefaultDomainIgnoreCaseOrCustomDomainIgnoreCase(host, host);
    }
}
