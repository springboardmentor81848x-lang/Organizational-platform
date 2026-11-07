package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assessment_submissions")
public class AssessmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private SkillAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private AssessmentAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "generation_source", nullable = false, length = 30)
    private String generationSource = "FALLBACK";

    @Column(name = "target_level", length = 30)
    private String targetLevel;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private boolean passed = false;

    @Column(name = "completed_at", nullable = false, updatable = false)
    private Instant completedAt = Instant.now();

    public AssessmentSubmission() {}

    public AssessmentSubmission(SkillAssessment assessment, User user, AssessmentAssignment assignment, int score, boolean passed) {
        this.assessment = assessment;
        this.user = user;
        this.assignment = assignment;
        this.score = score;
        this.passed = passed;
        this.completedAt = Instant.now();
    }

    public AssessmentSubmission(SkillAssessment assessment, User user, AssessmentAssignment assignment,
                                AssessmentAttempt attempt, Skill skill, String generationSource,
                                String targetLevel, int score, boolean passed) {
        this.assessment = assessment;
        this.user = user;
        this.assignment = assignment;
        this.attempt = attempt;
        this.skill = skill;
        this.generationSource = generationSource != null ? generationSource : "FALLBACK";
        this.targetLevel = targetLevel;
        this.score = score;
        this.passed = passed;
        this.completedAt = Instant.now();
    }

    public Long getId() { return id; }
    public SkillAssessment getAssessment() { return assessment; }
    public void setAssessment(SkillAssessment assessment) { this.assessment = assessment; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public AssessmentAssignment getAssignment() { return assignment; }
    public void setAssignment(AssessmentAssignment assignment) { this.assignment = assignment; }
    public AssessmentAttempt getAttempt() { return attempt; }
    public void setAttempt(AssessmentAttempt attempt) { this.attempt = attempt; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public String getGenerationSource() { return generationSource; }
    public void setGenerationSource(String generationSource) { this.generationSource = generationSource; }
    public String getTargetLevel() { return targetLevel; }
    public void setTargetLevel(String targetLevel) { this.targetLevel = targetLevel; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public Instant getCompletedAt() { return completedAt; }
}
