package kr.ac.skku.scg.exhibition.global.config;

import java.util.List;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUserArgumentResolver;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibitionArgumentResolver;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionDomainCacheService;
import kr.ac.skku.scg.exhibition.global.tenant.ExhibitionTenantInterceptor;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final ObjectProvider<ExhibitionDomainCacheService> cacheServiceProvider;

    public WebConfig(
            ObjectProvider<UserRepository> userRepositoryProvider,
            ObjectProvider<ExhibitionDomainCacheService> cacheServiceProvider
    ) {
        this.userRepositoryProvider = userRepositoryProvider;
        this.cacheServiceProvider = cacheServiceProvider;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentExhibitionArgumentResolver());

        UserRepository userRepository = userRepositoryProvider.getIfAvailable();
        if (userRepository != null) {
            resolvers.add(new CurrentUserArgumentResolver(userRepository));
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        ExhibitionDomainCacheService cacheService = cacheServiceProvider.getIfAvailable();
        if (cacheService == null) {
            return;
        }

        registry.addInterceptor(new ExhibitionTenantInterceptor(cacheService))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/media/**",
                        "/users/**",
                        "/error",
                        "/docs/**",
                        "/favicon.ico"
                );
    }
}
