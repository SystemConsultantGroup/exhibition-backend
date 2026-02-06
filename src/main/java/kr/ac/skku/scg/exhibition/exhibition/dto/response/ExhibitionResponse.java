package kr.ac.skku.scg.exhibition.exhibition.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ExhibitionResponse(UUID id, String slug, String name, Instant createdAt, Instant updatedAt) {
}
