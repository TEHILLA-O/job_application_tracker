package com.victrom.jobtracker.dto;

import java.time.LocalDate;

public record FollowUpItem(Long id, String company, String role, LocalDate nextFollowUp, boolean overdue) {
}
