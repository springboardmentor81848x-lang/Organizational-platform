package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType; // ASSESSMENT, MENTORSHIP, LEARNING, TRAINING, TEAM

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "location_or_link", length = 500)
    private String locationOrLink;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public CalendarEvent() {}

    public CalendarEvent(User creator, String title, String description, String eventType, Instant startTime, Instant endTime, String locationOrLink, Long referenceId) {
        this.creator = creator;
        this.title = title;
        this.description = description;
        this.eventType = eventType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.locationOrLink = locationOrLink;
        this.referenceId = referenceId;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getLocationOrLink() { return locationOrLink; }
    public void setLocationOrLink(String locationOrLink) { this.locationOrLink = locationOrLink; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public Instant getCreatedAt() { return createdAt; }
}
