package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mentorship_sessions")
public class MentorshipSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private MentorAssignment assignment;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 45;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @Column(name = "shared_notes", columnDefinition = "TEXT")
    private String sharedNotes;

    @Column(name = "mentor_prep_notes", columnDefinition = "TEXT")
    private String mentorPrepNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public MentorshipSession() {}

    public MentorshipSession(MentorAssignment assignment, String title, String description,
                             Instant scheduledAt, int durationMinutes, String meetingLink) {
        this.assignment = assignment;
        this.title = title;
        this.description = description;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.meetingLink = meetingLink;
        this.status = SessionStatus.SCHEDULED;
    }

    public Long getId() { return id; }
    public MentorAssignment getAssignment() { return assignment; }
    public void setAssignment(MentorAssignment assignment) { this.assignment = assignment; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public String getSharedNotes() { return sharedNotes; }
    public void setSharedNotes(String sharedNotes) { this.sharedNotes = sharedNotes; }
    public String getMentorPrepNotes() { return mentorPrepNotes; }
    public void setMentorPrepNotes(String mentorPrepNotes) { this.mentorPrepNotes = mentorPrepNotes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
