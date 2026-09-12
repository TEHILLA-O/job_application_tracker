package com.victrom.jobtracker.repository;

import com.victrom.jobtracker.dto.ApplicationFilter;
import com.victrom.jobtracker.model.ApplicationStatus;
import com.victrom.jobtracker.model.JobApplication;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ApplicationSpecifications {

    private ApplicationSpecifications() {
    }

    public static Specification<JobApplication> fromFilter(ApplicationFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.company() != null && !filter.company().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("company")), contains(filter.company())));
            }
            if (filter.source() != null) {
                predicates.add(cb.equal(root.get("source"), filter.source()));
            }
            if (filter.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.priority()));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String like = contains(filter.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("company")), like),
                        cb.like(cb.lower(root.get("role")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("location"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), like)
                ));
            }
            if (Boolean.TRUE.equals(filter.followUpDue())) {
                predicates.add(cb.isNotNull(root.get("nextFollowUp")));
                predicates.add(cb.lessThanOrEqualTo(root.get("nextFollowUp"), LocalDate.now()));
                predicates.add(root.get("status").in(
                        ApplicationStatus.ACCEPTED,
                        ApplicationStatus.REJECTED,
                        ApplicationStatus.WITHDRAWN
                ).not());
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String contains(String value) {
        return "%" + value.trim().toLowerCase() + "%";
    }
}
