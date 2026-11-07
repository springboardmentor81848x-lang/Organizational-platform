package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mentee_skill_consents")
public class MenteeSkillConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @Column(name = "can_view_skills", nullable = false)
    private boolean canViewSkills = true;

    @Column(name = "can_view_gaps", nullable = false)
    private boolean canViewGaps = true;

    @Column(name = "can_view_assessments", nullable = false)
    private boolean canViewAssessments = true;

    @Column(name = "consent_status", nullable = false, length = 20)
    private String consentStatus = "GRANTED";

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

    public MenteeSkillConsent() {}

    public MenteeSkillConsent(User employee, User mentor, boolean canViewSkills, boolean canViewGaps, boolean canViewAssessments, String consentStatus) {
        this.employee = employee;
        this.mentor = mentor;
        this.canViewSkills = canViewSkills;
        this.canViewGaps = canViewGaps;
        this.canViewAssessments = canViewAssessments;
        this.consentStatus = consentStatus != null ? consentStatus : "GRANTED";
    }

    public Long getId() { return id; }
    public User getEmployee() { return employee; }
    public void setEmployee(User employee) { this.employee = employee; }
    public User getMentor() { return mentor; }
    public void setMentor(User mentor) { this.mentor = mentor; }
    public boolean isCanViewSkills() { return canViewSkills; }
    public void setCanViewSkills(boolean canViewSkills) { this.canViewSkills = canViewSkills; }
    public boolean isCanViewGaps() { return canViewGaps; }
    public void setCanViewGaps(boolean canViewGaps) { this.canViewGaps = canViewGaps; }
    public boolean isCanViewAssessments() { return canViewAssessments; }
    public void setCanViewAssessments(boolean canViewAssessments) { this.canViewAssessments = canViewAssessments; }
    public String getConsentStatus() { return consentStatus; }
    public void setConsentStatus(String consentStatus) { this.consentStatus = consentStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
