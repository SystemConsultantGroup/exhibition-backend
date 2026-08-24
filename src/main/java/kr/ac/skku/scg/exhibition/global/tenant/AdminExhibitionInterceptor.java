package kr.ac.skku.scg.exhibition.global.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import org.springframework.web.servlet.HandlerInterceptor;

/** 관리자 요청의 전시 ID 헤더를 현재 전시 컨텍스트로 변환한다. */
public class AdminExhibitionInterceptor implements HandlerInterceptor {

    public static final String EXHIBITION_ID_HEADER = "X-Exhibition-Id";

    private final ExhibitionDomainCacheService cacheService;

    public AdminExhibitionInterceptor(ExhibitionDomainCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        UUID exhibitionId = resolveExhibitionId(request.getHeader(EXHIBITION_ID_HEADER));
        ExhibitionEntity exhibition = cacheService.findById(exhibitionId)
                .orElseThrow(() -> new NotFoundException("Exhibition not found: " + exhibitionId));

        request.setAttribute(CurrentExhibitionArgumentResolver.REQUEST_ATTR_EXHIBITION, exhibition);
        return true;
    }

    /** 필수 관리자 전시 ID 헤더를 UUID로 변환한다. */
    private UUID resolveExhibitionId(String rawExhibitionId) {
        if (rawExhibitionId == null || rawExhibitionId.isBlank()) {
            throw new IllegalArgumentException(EXHIBITION_ID_HEADER + " header is required");
        }
        try {
            return UUID.fromString(rawExhibitionId.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(EXHIBITION_ID_HEADER + " header must be a valid UUID");
        }
    }
}
