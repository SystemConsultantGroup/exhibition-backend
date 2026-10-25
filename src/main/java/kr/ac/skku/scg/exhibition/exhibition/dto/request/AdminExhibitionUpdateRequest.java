package kr.ac.skku.scg.exhibition.exhibition.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminExhibitionUpdateRequest(
        @NotBlank @Size(max = 255)
        @Pattern(regexp = "^(?:localhost|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)(?:\\.(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?))*)$",
                message = "must be a hostname without scheme, port, path, or whitespace")
        String defaultDomain,
        @Size(max = 255)
        @Pattern(regexp = "^$|^(?:localhost|(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)(?:\\.(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?))*)$",
                message = "must be a hostname without scheme, port, path, or whitespace")
        String customDomain,
        @NotBlank @Size(max = 128) String name,
        String description,
        UUID logoMediaId,
        boolean bannerEnabled,
        UUID bannerMediaId,
        boolean popupEnabled,
        UUID popupImageMediaId,
        String popupUrl,
        @Size(max = 200) String introTitle,
        String introDescription,
        UUID introVideoMediaId,
        Boolean showAwards,
        Boolean defaultToLatestEventPeriod
) {
}
