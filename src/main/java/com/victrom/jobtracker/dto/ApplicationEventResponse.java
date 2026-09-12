package com.victrom.jobtracker.dto;

import com.victrom.jobtracker.model.ApplicationStatus;
import com.victrom.jobtracker.model.EventType;

import java.time.Instant;

public record ApplicationEventResponse(
        Long id,
        EventType type,
        ApplicationStatus fromStatus,
        ApplicationStatus toStatus,
        String message,
        Instant createdAt
) {
}
