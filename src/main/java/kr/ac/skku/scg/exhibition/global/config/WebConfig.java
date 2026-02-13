package kr.ac.skku.scg.exhibition.global.config;

import java.util.List;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUserArgumentResolver;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectProvider<UserRepository> userRepositoryProvider;

    public WebConfig(ObjectProvider<UserRepository> userRepositoryProvider) {
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        UserRepository userRepository = userRepositoryProvider.getIfAvailable();
        if (userRepository != null) {
            resolvers.add(new CurrentUserArgumentResolver(userRepository));
        }
    }
}
