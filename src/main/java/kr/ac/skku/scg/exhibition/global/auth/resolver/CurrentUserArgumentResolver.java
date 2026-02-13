package kr.ac.skku.scg.exhibition.global.auth.resolver;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.error.UnauthorizedException;
import kr.ac.skku.scg.exhibition.user.domain.UserEntity;
import kr.ac.skku.scg.exhibition.user.repository.UserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String REQUEST_ATTR_USER_ID = "auth.userId";

    private final UserRepository userRepository;

    public CurrentUserArgumentResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
            return false;
        }

        Class<?> parameterType = parameter.getParameterType();
        return parameterType.equals(AuthenticatedUser.class) || parameterType.equals(UserEntity.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object userIdAttr = webRequest.getAttribute(REQUEST_ATTR_USER_ID, NativeWebRequest.SCOPE_REQUEST);
        if (!(userIdAttr instanceof UUID userId)) {
            throw new UnauthorizedException("Authentication is required");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + userId));

        if (parameter.getParameterType().equals(UserEntity.class)) {
            return user;
        }
        return AuthenticatedUser.from(user);
    }
}
