package kr.ac.skku.scg.exhibition.exhibition.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ExhibitionDtos {

    private ExhibitionDtos() {
    }

    public record ExhibitionResponse(
        UUID id,
        String slug,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        boolean isActive,
        boolean popupEnabled,
        UUID logoMediaId,
        UUID popupImageMediaId,
        String introTitle,
        String introDescription,
        UUID introVideoMediaId
    ) {
    }

    public record CreateExhibitionRequest(
        @NotBlank @Size(max = 64) String slug,
        @NotBlank @Size(max = 128) String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        UUID logoMediaId
    ) {
    }

    public record PatchExhibitionRequest(
        @Size(max = 128) String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isActive,
        UUID logoMediaId
    ) {
    }

    public record PatchPopupRequest(
        Boolean popupEnabled,
        UUID popupImageMediaId
    ) {
    }

    public record PatchIntroRequest(
        String introTitle,
        String introDescription,
        UUID introVideoMediaId
    ) {
    }
}
