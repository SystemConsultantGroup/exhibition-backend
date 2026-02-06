package kr.ac.skku.scg.exhibition.eventperiod.dto.response;

import java.time.Instant;
import java.util.UUID;

public record EventPeriodResponse(
        UUID id,
        UUID exhibitionId,
        String name,
        Instant startTime,
        Instant endTime
) {
}
