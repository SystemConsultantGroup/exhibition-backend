package kr.ac.skku.scg.exhibition.media.dto.response;

import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
        UUID id,
        UUID exhibitionId,
        UUID itemId,
        String objectKey,
        String mediaType,
        long size,
        Instant createdAt
) {
}
