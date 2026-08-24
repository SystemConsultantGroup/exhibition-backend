package kr.ac.skku.scg.exhibition.global.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
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
class AdminExhibitionInterceptorTest {

    @Mock
    private ExhibitionDomainCacheService cacheService;

    private AdminExhibitionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AdminExhibitionInterceptor(cacheService);
    }

    @Test
    void preHandle_withValidExhibitionId_setsCurrentExhibition() {
        UUID exhibitionId = UUID.randomUUID();
        ExhibitionEntity exhibition = new ExhibitionEntity(exhibitionId, "slug", "전시");
        when(cacheService.findById(exhibitionId)).thenReturn(Optional.of(exhibition));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AdminExhibitionInterceptor.EXHIBITION_ID_HEADER, exhibitionId.toString());

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute(CurrentExhibitionArgumentResolver.REQUEST_ATTR_EXHIBITION))
                .isSameAs(exhibition);
    }

    @Test
    void preHandle_withoutExhibitionId_throwsBadRequestException() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("X-Exhibition-Id header is required");
    }

    @Test
    void preHandle_withInvalidExhibitionId_throwsBadRequestException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AdminExhibitionInterceptor.EXHIBITION_ID_HEADER, "not-a-uuid");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("X-Exhibition-Id header must be a valid UUID");
    }

    @Test
    void preHandle_withUnknownExhibitionId_throwsNotFound() {
        UUID exhibitionId = UUID.randomUUID();
        when(cacheService.findById(exhibitionId)).thenReturn(Optional.empty());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AdminExhibitionInterceptor.EXHIBITION_ID_HEADER, exhibitionId.toString());

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Exhibition not found: " + exhibitionId);
    }

    @Test
    void preHandle_withOptionsRequest_doesNotRequireExhibitionId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verifyNoInteractions(cacheService);
    }
}
