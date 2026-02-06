package kr.ac.skku.scg.exhibition.global.error;

import java.time.Instant;

public record ApiErrorResponse(String code, String message, Instant timestamp) {

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, Instant.now());
    }
}
