package kr.ac.skku.scg.exhibition.classification.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ClassificationDtos {

    private ClassificationDtos() {
    }

    public record ClassificationResponse(
        UUID id,
        UUID exhibitionId,
        String name
    ) {
    }

    public record CreateClassificationRequest(
        @NotBlank @Size(max = 100) String name
    ) {
    }

    public record AttachClassificationRequest(
        UUID classificationId
    ) {
    }
}
