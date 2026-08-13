package kr.ac.skku.scg.exhibition.exhibition.dto.response;

import java.util.UUID;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;

public record ExhibitionResponse(
        UUID id,
        String slug,
        String defaultDomain,
        String customDomain,
        String name,
        String description,
        UUID logoMediaId,
        boolean bannerEnabled,
        UUID bannerMediaId,
        boolean popupEnabled,
        UUID popupImageMediaId,
        String popupUrl,
        String introTitle,
        String introDescription,
        UUID introVideoMediaId
) {
    public static ExhibitionResponse from(ExhibitionEntity exhibition) {
        return new ExhibitionResponse(
                exhibition.getId(),
                exhibition.getSlug(),
                exhibition.getDefaultDomain(),
                exhibition.getCustomDomain(),
                exhibition.getName(),
                exhibition.getDescription(),
                exhibition.getLogoMedia() == null ? null : exhibition.getLogoMedia().getId(),
                exhibition.isBannerEnabled(),
                exhibition.getBannerMedia() == null ? null : exhibition.getBannerMedia().getId(),
                exhibition.isPopupEnabled(),
                exhibition.getPopupImageMedia() == null ? null : exhibition.getPopupImageMedia().getId(),
                exhibition.getPopupUrl(),
                exhibition.getIntroTitle(),
                exhibition.getIntroDescription(),
                exhibition.getIntroVideoMedia() == null ? null : exhibition.getIntroVideoMedia().getId()
        );
    }
}
