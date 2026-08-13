package kr.ac.skku.scg.exhibition.item.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminItemRequest(
        @NotNull UUID categoryId,
        UUID eventPeriodId,
        @NotBlank @Size(max = 200) String title,
        String description,
        String participantNames,
        String participantEmails,
        String advisorNames,
        boolean awarded
) {
}
