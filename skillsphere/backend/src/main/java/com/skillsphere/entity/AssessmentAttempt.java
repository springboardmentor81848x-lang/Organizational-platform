package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "assessment_attempts")
public class AssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id")
    private SkillAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private AssessmentAssignment assignment;

    @Column(name = "target_level", nullable = false, length = 30)
    private String targetLevel = "Intermediate";

    @Column(name = "current_level", length = 30)
    private String currentLevel = "Unaware";

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS"; // IN_PROGRESS, COMPLETED, ABANDONED, EXPIRED

    @Column(name = "generation_source", nullable = false, length = 30)
    private String generationSource = "FALLBACK"; // AI_GENERATED, FALLBACK

    @Column(name = "generation_status", nullable = false, length = 30)
    private String generationStatus = "SUCCESS"; // SUCCESS, FALLBACK_TRIGGERED, FAILED

    @Column(name = "generation_error", columnDefinition = "TEXT")
    private String generationError;

    @Column
    private Integer score;

    @Column
    private Boolean passed;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    @Column(name = "answers_json", columnDefinition = "TEXT")
    private String answersJson;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    public AssessmentAttempt() {}

    public AssessmentAttempt(User user, Skill skill, SkillAssessment assessment, AssessmentAssignment assignment,
                             String targetLevel, String currentLevel, String generationSource) {
        this.user = user;
        this.skill = skill;
        this.assessment = assessment;
        this.assignment = assignment;
        this.targetLevel = targetLevel != null ? targetLevel : "Intermediate";
        this.currentLevel = currentLevel != null ? currentLevel : "Unaware";
        this.generationSource = generationSource != null ? generationSource : "FALLBACK";
        this.status = "IN_PROGRESS";
        this.startedAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public SkillAssessment getAssessment() { return assessment; }
    public void setAssessment(SkillAssessment assessment) { this.assessment = assessment; }
    public AssessmentAssignment getAssignment() { return assignment; }
    public void setAssignment(AssessmentAssignment assignment) { this.assignment = assignment; }
    public String getTargetLevel() { return targetLevel; }
    public void setTargetLevel(String targetLevel) { this.targetLevel = targetLevel; }
    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGenerationSource() { return generationSource; }
    public void setGenerationSource(String generationSource) { this.generationSource = generationSource; }
    public String getGenerationStatus() { return generationStatus; }
    public void setGenerationStatus(String generationStatus) { this.generationStatus = generationStatus; }
    public String getGenerationError() { return generationError; }
    public void setGenerationError(String generationError) { this.generationError = generationError; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public Integer getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(Integer correctAnswers) { this.correctAnswers = correctAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public String getAnswersJson() { return answersJson; }
    public void setAnswersJson(String answersJson) { this.answersJson = answersJson; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
