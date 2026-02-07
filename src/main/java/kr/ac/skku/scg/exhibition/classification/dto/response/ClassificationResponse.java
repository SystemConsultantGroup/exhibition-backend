package kr.ac.skku.scg.exhibition.classification.dto.response;

import java.util.UUID;

public record ClassificationResponse(UUID id, UUID exhibitionId, String name) {
}
