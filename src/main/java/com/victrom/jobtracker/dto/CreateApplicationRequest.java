package com.victrom.jobtracker.dto;

import com.victrom.jobtracker.model.ApplicationSource;
import com.victrom.jobtracker.model.ApplicationStatus;
import com.victrom.jobtracker.model.Priority;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 120) String company,
        @NotBlank @Size(max = 160) String role,
        @NotNull ApplicationStatus status,
        @NotNull LocalDate appliedDate,
        @Size(max = 160) String location,
        @PositiveOrZero BigDecimal salaryMin,
        @PositiveOrZero BigDecimal salaryMax,
        @Size(max = 3000) String notes,
        ApplicationSource source,
        @Size(max = 500) String jobUrl,
        @Size(max = 120) String recruiterName,
        @Email @Size(max = 160) String recruiterEmail,
        Priority priority,
        LocalDate nextFollowUp,
        List<@Size(max = 40) String> tags
) {
}
