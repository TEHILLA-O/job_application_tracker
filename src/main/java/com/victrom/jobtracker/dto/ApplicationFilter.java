package com.victrom.jobtracker.dto;

import com.victrom.jobtracker.model.ApplicationSource;
import com.victrom.jobtracker.model.ApplicationStatus;
import com.victrom.jobtracker.model.Priority;

public record ApplicationFilter(
        ApplicationStatus status,
        String company,
        String q,
        ApplicationSource source,
        Priority priority,
        Boolean followUpDue
) {
}
