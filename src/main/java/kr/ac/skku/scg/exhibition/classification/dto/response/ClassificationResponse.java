package kr.ac.skku.scg.exhibition.classification.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ClassificationResponse(UUID id, UUID exhibitionId, String name, Instant createdAt) {
}
