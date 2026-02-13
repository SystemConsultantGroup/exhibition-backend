package kr.ac.skku.scg.exhibition.board.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BoardResponse(
        UUID id,
        UUID exhibitionId,
        String title,
        String content,
        List<UUID> attachmentMediaIds,
        List<AttachmentMediaResponse> attachmentMedias,
        UUID authorUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
