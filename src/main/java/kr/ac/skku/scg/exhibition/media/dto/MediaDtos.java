package kr.ac.skku.scg.exhibition.media.dto;

import java.time.Instant;
import java.util.UUID;

public final class MediaDtos {

    private MediaDtos() {
    }

    public record MediaResponse(
        UUID id,
        UUID itemId,
        UUID exhibitionId,
        String objectKey,
        String mediaType,
        long size,
        Instant createdAt
    ) {
    }
}
