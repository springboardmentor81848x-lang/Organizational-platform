package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "mentorship_goals")
public class MentorshipGoal {

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

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GoalStatus status = GoalStatus.IN_PROGRESS;

    @Column(length = 20)
    private String priority = "MEDIUM";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

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

    public MentorshipGoal() {}

    public MentorshipGoal(MentorAssignment assignment, String title, String description, LocalDate targetDate) {
        this.assignment = assignment;
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.status = GoalStatus.IN_PROGRESS;
        this.priority = "MEDIUM";
    }

    public MentorshipGoal(MentorAssignment assignment, String title, String description, LocalDate targetDate, String priority, Skill skill) {
        this.assignment = assignment;
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.status = GoalStatus.IN_PROGRESS;
        this.priority = priority != null ? priority : "MEDIUM";
        this.skill = skill;
    }

    public Long getId() { return id; }
    public MentorAssignment getAssignment() { return assignment; }
    public void setAssignment(MentorAssignment assignment) { this.assignment = assignment; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public GoalStatus getStatus() { return status; }
    public void setStatus(GoalStatus status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
