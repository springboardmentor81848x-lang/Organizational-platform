package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "skill_gaps", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "skill_id"})
})
public class SkillGap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "current_level", nullable = false)
    private int currentLevel;

    @Column(name = "required_level", nullable = false)
    private int requiredLevel;

    @Column(name = "gap_size", nullable = false)
    private int gapSize;

    @Column(nullable = false, length = 20)
    private String priority; // HIGH, MEDIUM, LOW, ON_TRACK

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt = Instant.now();

    public SkillGap() {}

    public SkillGap(User user, Skill skill, int currentLevel, int requiredLevel, int gapSize, String priority) {
        this.user = user;
        this.skill = skill;
        this.currentLevel = currentLevel;
        this.requiredLevel = requiredLevel;
        this.gapSize = gapSize;
        this.priority = priority;
        this.calculatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }
    public int getGapSize() { return gapSize; }
    public void setGapSize(int gapSize) { this.gapSize = gapSize; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
}
