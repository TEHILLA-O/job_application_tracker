package com.victrom.jobtracker.dto;

import java.util.List;
import java.util.Map;

public record ApplicationStats(
        long total,
        long active,
        long offers,
        long accepted,
        long rejected,
        long withdrawn,
        double interviewRatePercent,
        double offerRatePercent,
        double averageDaysOpen,
        Map<String, Long> byStatus,
        Map<String, Long> bySource,
        Map<String, Long> byPriority,
        List<FunnelStep> funnel,
        List<FollowUpItem> upcomingFollowUps,
        List<CompanyCount> topCompanies
) {
}
