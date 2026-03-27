package kr.ac.skku.scg.exhibition.exhibition.dto.response;

import java.util.UUID;

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
}
