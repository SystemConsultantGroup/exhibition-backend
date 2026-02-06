package kr.ac.skku.scg.exhibition.item.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ItemDtos {

    private ItemDtos() {
    }

    public record ItemResponse(
        UUID id,
        UUID exhibitionId,
        UUID categoryId,
        String title,
        String summary,
        String description,
        String authorName,
        String authorEmail,
        String visibility,
        Instant publishedAt
    ) {
    }

    public record CreateItemRequest(
        UUID categoryId,
        @NotBlank @Size(max = 200) String title,
        String summary,
        String description,
        @Size(max = 100) String authorName,
        @Email @Size(max = 200) String authorEmail,
        String visibility,
        Instant publishedAt
    ) {
    }

    public record PatchItemRequest(
        UUID categoryId,
        @Size(max = 200) String title,
        String summary,
        String description,
        @Size(max = 100) String authorName,
        @Email @Size(max = 200) String authorEmail,
        String visibility,
        Instant publishedAt
    ) {
    }
}
