package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "role_skill_requirements", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"job_role", "skill_id"})
})
public class RoleSkillRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_role", nullable = false, length = 80)
    private String jobRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "required_proficiency", nullable = false, length = 20)
    private String requiredProficiency; // Unaware, Beginner, Intermediate, Advanced, Expert

    @Column(nullable = false, length = 20)
    private String priority = "HIGH"; // HIGH, MEDIUM, LOW

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RoleSkillRequirement() {}

    public RoleSkillRequirement(String jobRole, Skill skill, String requiredProficiency, String priority) {
        this.jobRole = jobRole;
        this.skill = skill;
        this.requiredProficiency = requiredProficiency;
        this.priority = priority;
        this.isActive = true;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getJobRole() { return jobRole; }
    public void setJobRole(String jobRole) { this.jobRole = jobRole; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public String getRequiredProficiency() { return requiredProficiency; }
    public void setRequiredProficiency(String requiredProficiency) { this.requiredProficiency = requiredProficiency; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Instant getCreatedAt() { return createdAt; }
}
