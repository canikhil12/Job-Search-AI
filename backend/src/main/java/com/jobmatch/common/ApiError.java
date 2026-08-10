package com.jobmatch.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Consistent error body returned by every handled exception.
 * fieldErrors is omitted from the JSON when null/empty (e.g. non-validation errors).
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String message) {
        return new ApiError(Instant.now(), status, message, Map.of());
    }

    public static ApiError of(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, message, fieldErrors);
    }
}
