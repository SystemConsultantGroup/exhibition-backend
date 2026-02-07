package kr.ac.skku.scg.exhibition.exhibition.dto.response;

import java.util.UUID;

public record ExhibitionResponse(
        UUID id,
        String slug,
        String name,
        String description,
        UUID logoMediaId,
        boolean popupEnabled,
        UUID popupImageMediaId,
        String popupUrl,
        String introTitle,
        String introDescription,
        UUID introVideoMediaId
) {
}
