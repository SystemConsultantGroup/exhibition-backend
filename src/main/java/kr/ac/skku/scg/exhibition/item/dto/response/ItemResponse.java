package kr.ac.skku.scg.exhibition.item.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID exhibitionId,
        UUID categoryId,
        UUID eventPeriodId,
        String title,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
