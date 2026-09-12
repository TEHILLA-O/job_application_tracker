package com.victrom.jobtracker.dto;

import com.victrom.jobtracker.model.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull ApplicationStatus status) {
}
