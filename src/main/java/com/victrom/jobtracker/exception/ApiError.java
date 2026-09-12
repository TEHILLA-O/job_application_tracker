package com.victrom.jobtracker.exception;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields,
        String from,
        String to,
        Set<String> allowed
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null, null, null, null);
    }

    public static ApiError validation(String path, Map<String, String> fields) {
        return new ApiError(Instant.now(), 400, "Validation failed",
                "One or more fields are invalid", path, fields, null, null, null);
    }
}
