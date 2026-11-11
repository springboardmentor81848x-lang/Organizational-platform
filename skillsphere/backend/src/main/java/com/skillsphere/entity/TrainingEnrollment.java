package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "training_enrollments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"programme_id", "user_id"})
})
public class TrainingEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "programme_id", nullable = false)
    private TrainingProgramme programme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String status = "ENROLLED"; // ENROLLED, ATTENDED, CANCELLED

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private Instant enrolledAt = Instant.now();

    public TrainingEnrollment() {}

    public TrainingEnrollment(TrainingProgramme programme, User user) {
        this.programme = programme;
        this.user = user;
        this.status = "ENROLLED";
        this.enrolledAt = Instant.now();
    }

    public Long getId() { return id; }
    public TrainingProgramme getProgramme() { return programme; }
    public void setProgramme(TrainingProgramme programme) { this.programme = programme; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getEnrolledAt() { return enrolledAt; }
}
