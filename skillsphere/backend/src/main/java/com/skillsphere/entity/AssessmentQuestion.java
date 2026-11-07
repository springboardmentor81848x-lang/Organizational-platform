package com.skillsphere.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private SkillAssessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private AssessmentAttempt attempt;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "correct_answer_index", nullable = false)
    private int correctAnswerIndex;

    @Column(nullable = false)
    private int points = 1;

    @Column(nullable = false, length = 20)
    private String difficulty = "MEDIUM"; // EASY, MEDIUM, HARD

    @Column(length = 100)
    private String subtopic;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "question_order", nullable = false)
    private int questionOrder = 0;

    public AssessmentQuestion() {}

    public AssessmentQuestion(SkillAssessment assessment, Skill skill, String questionText, String optionsJson, int correctAnswerIndex, int points) {
        this.assessment = assessment;
        this.skill = skill;
        this.questionText = questionText;
        this.optionsJson = optionsJson;
        this.correctAnswerIndex = correctAnswerIndex;
        this.points = points;
    }

    public AssessmentQuestion(SkillAssessment assessment, Skill skill, AssessmentAttempt attempt, String questionText,
                              String optionsJson, int correctAnswerIndex, int points, String difficulty,
                              String subtopic, String explanation, int questionOrder) {
        this.assessment = assessment;
        this.skill = skill;
        this.attempt = attempt;
        this.questionText = questionText;
        this.optionsJson = optionsJson;
        this.correctAnswerIndex = correctAnswerIndex;
        this.points = points;
        this.difficulty = difficulty != null ? difficulty : "MEDIUM";
        this.subtopic = subtopic;
        this.explanation = explanation;
        this.questionOrder = questionOrder;
    }

    public Long getId() { return id; }
    public SkillAssessment getAssessment() { return assessment; }
    public void setAssessment(SkillAssessment assessment) { this.assessment = assessment; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public AssessmentAttempt getAttempt() { return attempt; }
    public void setAttempt(AssessmentAttempt attempt) { this.attempt = attempt; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public String getSubtopic() { return subtopic; }
    public void setSubtopic(String subtopic) { this.subtopic = subtopic; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(int questionOrder) { this.questionOrder = questionOrder; }
}
