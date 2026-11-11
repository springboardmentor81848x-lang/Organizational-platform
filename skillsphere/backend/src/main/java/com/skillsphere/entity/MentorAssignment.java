package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mentor_assignments")
public class MentorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @PrePersist
    void onCreate() {
        if (assignedAt == null) {
            assignedAt = Instant.now();
        }
    }

    public MentorAssignment() {}

    public MentorAssignment(User mentor, User employee, User assignedBy, String notes) {
        this.mentor = mentor;
        this.employee = employee;
        this.assignedBy = assignedBy;
        this.notes = notes;
        this.status = AssignmentStatus.ACTIVE;
        this.assignedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getMentor() { return mentor; }
    public void setMentor(User mentor) { this.mentor = mentor; }
    public User getEmployee() { return employee; }
    public void setEmployee(User employee) { this.employee = employee; }
    public User getAssignedBy() { return assignedBy; }
    public void setAssignedBy(User assignedBy) { this.assignedBy = assignedBy; }
    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}
