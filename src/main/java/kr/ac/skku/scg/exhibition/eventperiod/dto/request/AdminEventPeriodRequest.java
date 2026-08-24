package kr.ac.skku.scg.exhibition.eventperiod.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record AdminEventPeriodRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull Instant startTime,
        @NotNull Instant endTime
) {
}
