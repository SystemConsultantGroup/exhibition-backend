package kr.ac.skku.scg.exhibition.global.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Locale;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

public class ExhibitionTenantInterceptor implements HandlerInterceptor {

    private final ExhibitionDomainCacheService cacheService;

    public ExhibitionTenantInterceptor(ExhibitionDomainCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String domain = resolveDomain(request);
        ExhibitionEntity exhibition = cacheService.findByDomain(domain)
                .orElseThrow(() -> new NotFoundException("Exhibition not found for origin: " + domain));

        request.setAttribute(CurrentExhibitionArgumentResolver.REQUEST_ATTR_EXHIBITION, exhibition);
        return true;
    }

    private String resolveDomain(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank()) {
            try {
                String host = URI.create(origin.trim()).getHost();
                if (host != null && !host.isBlank()) {
                    return host.toLowerCase(Locale.ROOT);
                }
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException("Invalid Origin header");
            }
            throw new IllegalArgumentException("Invalid Origin header");
        }

        String hostHeader = request.getHeader(HttpHeaders.HOST);
        if (hostHeader == null || hostHeader.isBlank()) {
            throw new IllegalArgumentException("Origin header is required");
        }

        String normalizedHost = hostHeader.trim().toLowerCase(Locale.ROOT);
        int portIndex = normalizedHost.indexOf(':');
        return portIndex >= 0 ? normalizedHost.substring(0, portIndex) : normalizedHost;
    }
}
