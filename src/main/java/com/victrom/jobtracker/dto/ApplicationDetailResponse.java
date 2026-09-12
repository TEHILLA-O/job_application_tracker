package com.victrom.jobtracker.dto;

import java.util.List;
import java.util.Set;

import com.victrom.jobtracker.model.ApplicationStatus;

public record ApplicationDetailResponse(
        ApplicationResponse application,
        List<ApplicationEventResponse> events,
        Set<ApplicationStatus> allowedNextStatuses
) {
}
