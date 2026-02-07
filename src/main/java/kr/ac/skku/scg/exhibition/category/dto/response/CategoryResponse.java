package kr.ac.skku.scg.exhibition.category.dto.response;

import java.util.UUID;

public record CategoryResponse(UUID id, UUID exhibitionId, String name) {
}
