package com.victrom.jobtracker.dto;

import com.victrom.jobtracker.model.ApplicationSource;
import com.victrom.jobtracker.model.ApplicationStatus;
import com.victrom.jobtracker.model.Priority;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ApplicationResponse(
        Long id,
        String company,
        String role,
        ApplicationStatus status,
        LocalDate appliedDate,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String notes,
        ApplicationSource source,
        String jobUrl,
        String recruiterName,
        String recruiterEmail,
        Priority priority,
        LocalDate nextFollowUp,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt,
        long daysOpen,
        boolean followUpDue,
        long eventCount
) {
}
