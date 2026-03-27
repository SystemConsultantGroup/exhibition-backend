package kr.ac.skku.scg.exhibition.global.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.error.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class ExhibitionTenantInterceptorTest {

    @Mock
    private ExhibitionDomainCacheService cacheService;

    private ExhibitionTenantInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ExhibitionTenantInterceptor(cacheService);
    }

    // --- Origin 헤더 기반 ---

    @Test
    void preHandle_withValidOrigin_setsExhibitionAttributeAndReturnsTrue() throws Exception {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "sw-gp", "테스트 전시");
        when(cacheService.findByDomain("exhibition.example.com")).thenReturn(Optional.of(exhibition));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://exhibition.example.com");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute(CurrentExhibitionArgumentResolver.REQUEST_ATTR_EXHIBITION))
                .isSameAs(exhibition);
    }

    @Test
    void preHandle_withUpperCaseOriginHost_normalizesToLowerCase() throws Exception {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "sw-gp", "테스트 전시");
        when(cacheService.findByDomain("exhibition.example.com")).thenReturn(Optional.of(exhibition));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://EXHIBITION.EXAMPLE.COM");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
    }

    @Test
    void preHandle_withValidOriginButExhibitionNotFound_throwsNotFoundException() {
        when(cacheService.findByDomain("unknown.example.com")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "https://unknown.example.com");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void preHandle_withMalformedOriginUri_throwsIllegalArgumentException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "not a valid uri");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Origin header");
    }

    @Test
    void preHandle_withOriginThatHasNoHost_throwsIllegalArgumentException() {
        // 상대 URI는 파싱 성공하지만 getHost()가 null 반환
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", "not-a-uri");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid Origin header");
    }

    // --- Host 헤더 폴백 ---

    @Test
    void preHandle_withHostHeaderContainingPort_stripsPortAndResolvesExhibition() throws Exception {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "sw-gp", "테스트 전시");
        when(cacheService.findByDomain("exhibition.example.com")).thenReturn(Optional.of(exhibition));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "exhibition.example.com:8080");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute(CurrentExhibitionArgumentResolver.REQUEST_ATTR_EXHIBITION))
                .isSameAs(exhibition);
    }

    @Test
    void preHandle_withHostHeaderNoPort_resolvesExhibition() throws Exception {
        ExhibitionEntity exhibition = new ExhibitionEntity(UUID.randomUUID(), "sw-gp", "테스트 전시");
        when(cacheService.findByDomain("exhibition.example.com")).thenReturn(Optional.of(exhibition));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Host", "exhibition.example.com");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
    }

    // --- 헤더 없음 ---

    @Test
    void preHandle_withNoOriginAndNoHost_throwsIllegalArgumentException() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Origin header is required");
    }
}
