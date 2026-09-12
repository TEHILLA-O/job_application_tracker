package com.victrom.jobtracker.model;

public enum ApplicationStatus {
    SAVED,
    APPLIED,
    ASSESSMENT,
    INTERVIEW,
    OFFER,
    ACCEPTED,
    REJECTED,
    WITHDRAWN;

    public boolean isClosed() {
        return this == ACCEPTED || this == REJECTED || this == WITHDRAWN;
    }

    public boolean isInterviewStage() {
        return this == INTERVIEW || this == OFFER || this == ACCEPTED;
    }
}
