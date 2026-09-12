package com.victrom.jobtracker.dto;

import com.victrom.jobtracker.model.ApplicationEvent;
import com.victrom.jobtracker.model.JobApplication;
import com.victrom.jobtracker.pipeline.PipelineRules;
import org.hibernate.Hibernate;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

public final class ApplicationMapper {

    private ApplicationMapper() {
    }

    public static ApplicationResponse toResponse(JobApplication item) {
        LocalDate today = LocalDate.now();
        long daysOpen = item.getAppliedDate() == null ? 0 : ChronoUnit.DAYS.between(item.getAppliedDate(), today);
        boolean followUpDue = item.getNextFollowUp() != null
                && !item.getNextFollowUp().isAfter(today)
                && item.getStatus() != null
                && !item.getStatus().isClosed();
        return new ApplicationResponse(
                item.getId(),
                item.getCompany(),
                item.getRole(),
                item.getStatus(),
                item.getAppliedDate(),
                item.getLocation(),
                item.getSalaryMin(),
                item.getSalaryMax(),
                item.getNotes(),
                item.getSource(),
                item.getJobUrl(),
                item.getRecruiterName(),
                item.getRecruiterEmail(),
                item.getPriority(),
                item.getNextFollowUp(),
                List.copyOf(item.getTags()),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                daysOpen,
                followUpDue,
                eventCount(item)
        );
    }

    public static ApplicationEventResponse toEvent(ApplicationEvent event) {
        return new ApplicationEventResponse(
                event.getId(),
                event.getType(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getMessage(),
                event.getCreatedAt()
        );
    }

    public static ApplicationDetailResponse toDetail(JobApplication item) {
        List<ApplicationEventResponse> events = item.getEvents().stream()
                .sorted(Comparator
                        .comparing(ApplicationEvent::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ApplicationEvent::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ApplicationMapper::toEvent)
                .toList();
        return new ApplicationDetailResponse(
                toResponse(item),
                events,
                PipelineRules.allowedFrom(item.getStatus())
        );
    }

    private static long eventCount(JobApplication item) {
        if (item.getEvents() == null || !Hibernate.isInitialized(item.getEvents())) {
            return 0;
        }
        return item.getEvents().size();
    }
}
