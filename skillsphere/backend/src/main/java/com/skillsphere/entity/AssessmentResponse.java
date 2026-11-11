package com.skillsphere.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "assessment_responses")
public class AssessmentResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private AssessmentSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(name = "selected_option_index", nullable = false)
    private int selectedOptionIndex;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    public AssessmentResponse() {}

    public AssessmentResponse(AssessmentSubmission submission, AssessmentQuestion question, int selectedOptionIndex, boolean isCorrect) {
        this.submission = submission;
        this.question = question;
        this.selectedOptionIndex = selectedOptionIndex;
        this.isCorrect = isCorrect;
    }

    public Long getId() { return id; }
    public AssessmentSubmission getSubmission() { return submission; }
    public void setSubmission(AssessmentSubmission submission) { this.submission = submission; }
    public AssessmentQuestion getQuestion() { return question; }
    public void setQuestion(AssessmentQuestion question) { this.question = question; }
    public int getSelectedOptionIndex() { return selectedOptionIndex; }
    public void setSelectedOptionIndex(int selectedOptionIndex) { this.selectedOptionIndex = selectedOptionIndex; }
    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}
