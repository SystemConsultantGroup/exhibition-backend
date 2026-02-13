package kr.ac.skku.scg.exhibition.board.dto.response;

import java.util.UUID;

public record AttachmentMediaResponse(
        UUID id,
        String fileName
) {
}
