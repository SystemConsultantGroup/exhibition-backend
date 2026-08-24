package kr.ac.skku.scg.exhibition.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionDomainCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    @Test
    void cors_allowsConfiguredAdminHostWithoutExhibitionDomain() {
        ExhibitionDomainCacheService cacheService = org.mockito.Mockito.mock(ExhibitionDomainCacheService.class);
        ObjectProvider<ExhibitionDomainCacheService> provider = provider(cacheService);
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(
                provider, List.of("manage.exhibition.scg.sh"));
        MockHttpServletRequest request = request("https://manage.exhibition.scg.sh");

        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("https://manage.exhibition.scg.sh");
        assertThat(configuration.getAllowedHeaders()).containsExactly("*");
    }

    @Test
    void cors_stillAllowsRegisteredExhibitionDomain() {
        ExhibitionDomainCacheService cacheService = org.mockito.Mockito.mock(ExhibitionDomainCacheService.class);
        when(cacheService.findByDomain("expo.example.com"))
                .thenReturn(Optional.of(org.mockito.Mockito.mock(ExhibitionEntity.class)));
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(
                provider(cacheService), List.of("manage.exhibition.scg.sh"));

        assertThat(source.getCorsConfiguration(request("https://expo.example.com"))).isNotNull();
    }

    @Test
    void cors_rejectsUnknownHost() {
        ExhibitionDomainCacheService cacheService = org.mockito.Mockito.mock(ExhibitionDomainCacheService.class);
        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(
                provider(cacheService), List.of("manage.exhibition.scg.sh"));

        assertThat(source.getCorsConfiguration(request("https://unknown.example.com"))).isNull();
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ExhibitionDomainCacheService> provider(ExhibitionDomainCacheService cacheService) {
        ObjectProvider<ExhibitionDomainCacheService> provider = org.mockito.Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(cacheService);
        return provider;
    }

    private MockHttpServletRequest request(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Origin", origin);
        return request;
    }
}
