package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shared_resources")
public class SharedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private MentorAssignment assignment;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(name = "url_or_content", nullable = false, columnDefinition = "TEXT")
    private String urlOrContent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public SharedResource() {}

    public SharedResource(MentorAssignment assignment, String title, String urlOrContent, User sharedBy) {
        this.assignment = assignment;
        this.title = title;
        this.urlOrContent = urlOrContent;
        this.sharedBy = sharedBy;
        this.createdAt = Instant.now();
    }

    public SharedResource(MentorAssignment assignment, User sharedBy, String title, String urlOrContent) {
        this.assignment = assignment;
        this.sharedBy = sharedBy;
        this.title = title;
        this.urlOrContent = urlOrContent;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public MentorAssignment getAssignment() { return assignment; }
    public void setAssignment(MentorAssignment assignment) { this.assignment = assignment; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrlOrContent() { return urlOrContent; }
    public void setUrlOrContent(String urlOrContent) { this.urlOrContent = urlOrContent; }
    public User getSharedBy() { return sharedBy; }
    public void setSharedBy(User sharedBy) { this.sharedBy = sharedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
