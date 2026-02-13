package kr.ac.skku.scg.exhibition.item.dto.response;

import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID exhibitionId,
        UUID categoryId,
        UUID eventPeriodId,
        String title,
        String description,
        String participantNames,
        String advisorNames,
        UUID thumbnailMediaId,
        UUID posterMediaId,
        UUID presentationVideoMediaId,
        long likes
) {
}
