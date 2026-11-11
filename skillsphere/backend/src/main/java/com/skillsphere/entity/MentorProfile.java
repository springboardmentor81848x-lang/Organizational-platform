package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mentor_profiles")
public class MentorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 140)
    private String headline;

    @Column(name = "expertise_areas", nullable = false, columnDefinition = "TEXT")
    private String expertiseAreas;

    @Column(name = "max_mentees", nullable = false)
    private int maxMentees = 3;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "preferred_format", length = 30)
    private String preferredFormat = "VIRTUAL";

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approved_by_id", nullable = false)
    private User approvedBy;

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

    public MentorProfile() {}

    public MentorProfile(User user, String headline, String expertiseAreas, int maxMentees, User approvedBy) {
        this.user = user;
        this.headline = headline;
        this.expertiseAreas = expertiseAreas;
        this.maxMentees = maxMentees;
        this.approvedBy = approvedBy;
        this.isActive = true;
        this.preferredFormat = "VIRTUAL";
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getExpertiseAreas() { return expertiseAreas; }
    public void setExpertiseAreas(String expertiseAreas) { this.expertiseAreas = expertiseAreas; }
    public int getMaxMentees() { return maxMentees; }
    public void setMaxMentees(int maxMentees) { this.maxMentees = maxMentees; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getPreferredFormat() { return preferredFormat; }
    public void setPreferredFormat(String preferredFormat) { this.preferredFormat = preferredFormat; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
