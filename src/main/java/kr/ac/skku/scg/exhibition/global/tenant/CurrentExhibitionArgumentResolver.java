package kr.ac.skku.scg.exhibition.global.tenant;

import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.error.UnauthorizedException;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentExhibitionArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String REQUEST_ATTR_EXHIBITION = "tenant.exhibition";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentExhibition.class)
                && parameter.getParameterType().equals(ExhibitionEntity.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object exhibition = webRequest.getAttribute(REQUEST_ATTR_EXHIBITION, NativeWebRequest.SCOPE_REQUEST);
        if (!(exhibition instanceof ExhibitionEntity exhibitionEntity)) {
            throw new UnauthorizedException("Exhibition context is required");
        }
        return exhibitionEntity;
    }
}
