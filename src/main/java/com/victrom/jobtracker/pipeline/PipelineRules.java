package com.victrom.jobtracker.pipeline;

import com.victrom.jobtracker.exception.InvalidTransitionException;
import com.victrom.jobtracker.model.ApplicationStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Allowed status moves. Edit this map when you want a stricter or looser pipeline.
 */
public final class PipelineRules {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED.put(ApplicationStatus.SAVED, EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN));
        ALLOWED.put(ApplicationStatus.APPLIED, EnumSet.of(
                ApplicationStatus.ASSESSMENT, ApplicationStatus.INTERVIEW,
                ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));
        ALLOWED.put(ApplicationStatus.ASSESSMENT, EnumSet.of(
                ApplicationStatus.INTERVIEW, ApplicationStatus.APPLIED,
                ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));
        ALLOWED.put(ApplicationStatus.INTERVIEW, EnumSet.of(
                ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER,
                ApplicationStatus.ASSESSMENT, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));
        ALLOWED.put(ApplicationStatus.OFFER, EnumSet.of(
                ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED,
                ApplicationStatus.WITHDRAWN, ApplicationStatus.INTERVIEW));
        ALLOWED.put(ApplicationStatus.ACCEPTED, EnumSet.noneOf(ApplicationStatus.class));
        ALLOWED.put(ApplicationStatus.REJECTED, EnumSet.of(ApplicationStatus.SAVED, ApplicationStatus.APPLIED));
        ALLOWED.put(ApplicationStatus.WITHDRAWN, EnumSet.of(ApplicationStatus.SAVED, ApplicationStatus.APPLIED));
    }

    private PipelineRules() {
    }

    public static Set<ApplicationStatus> allowedFrom(ApplicationStatus from) {
        return Set.copyOf(ALLOWED.getOrDefault(from, Set.of()));
    }

    public static void assertTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == to) {
            return;
        }
        Set<ApplicationStatus> allowed = allowedFrom(from);
        if (!allowed.contains(to)) {
            throw new InvalidTransitionException(from, to, allowed);
        }
    }
}
