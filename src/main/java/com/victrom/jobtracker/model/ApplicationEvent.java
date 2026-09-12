package com.victrom.jobtracker.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "application_events", indexes = {
        @Index(name = "idx_event_application", columnList = "application_id")
})
public class ApplicationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus toStatus;

    @Column(length = 3000)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public ApplicationEvent() {
    }

    public ApplicationEvent(EventType type,
                            ApplicationStatus fromStatus,
                            ApplicationStatus toStatus,
                            String message) {
        this.type = type;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public JobApplication getApplication() {
        return application;
    }

    public void setApplication(JobApplication application) {
        this.application = application;
    }

    public EventType getType() {
        return type;
    }

    public ApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public ApplicationStatus getToStatus() {
        return toStatus;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
