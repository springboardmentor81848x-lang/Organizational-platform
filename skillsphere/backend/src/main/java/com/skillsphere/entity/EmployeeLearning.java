package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "employee_learning", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "course_id"})
})
public class EmployeeLearning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 30)
    private String status = "NOT_STARTED"; // NOT_STARTED, IN_PROGRESS, COMPLETED, CERTIFIED, EXPIRED

    @Column(name = "progress_pct", nullable = false)
    private int progressPct = 0;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public EmployeeLearning() {}

    public EmployeeLearning(User user, Course course, String status, int progressPct) {
        this.user = user;
        this.course = course;
        this.status = status;
        this.progressPct = progressPct;
        this.enrolledAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getProgressPct() { return progressPct; }
    public void setProgressPct(int progressPct) { this.progressPct = progressPct; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
