package com.victrom.jobtracker.exception;

import com.victrom.jobtracker.model.ApplicationStatus;

import java.util.Set;

public class InvalidTransitionException extends RuntimeException {

    private final ApplicationStatus from;
    private final ApplicationStatus to;
    private final Set<ApplicationStatus> allowed;

    public InvalidTransitionException(ApplicationStatus from,
                                      ApplicationStatus to,
                                      Set<ApplicationStatus> allowed) {
        super("Cannot move an application from " + from + " to " + to);
        this.from = from;
        this.to = to;
        this.allowed = allowed;
    }

    public ApplicationStatus getFrom() {
        return from;
    }

    public ApplicationStatus getTo() {
        return to;
    }

    public Set<ApplicationStatus> getAllowed() {
        return allowed;
    }
}
