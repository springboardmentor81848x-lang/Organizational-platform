package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mentor_evaluations")
public class MentorEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private User mentor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private MentorshipSession session;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "areas_for_growth", columnDefinition = "TEXT")
    private String areasForGrowth;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MentorEvaluation() {}

    public MentorEvaluation(User mentor, User employee, MentorshipSession session, Integer rating, String strengths, String areasForGrowth, String comments) {
        this.mentor = mentor;
        this.employee = employee;
        this.session = session;
        this.rating = rating;
        this.strengths = strengths;
        this.areasForGrowth = areasForGrowth;
        this.comments = comments;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getMentor() { return mentor; }
    public void setMentor(User mentor) { this.mentor = mentor; }
    public User getEmployee() { return employee; }
    public void setEmployee(User employee) { this.employee = employee; }
    public MentorshipSession getSession() { return session; }
    public void setSession(MentorshipSession session) { this.session = session; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    public String getAreasForGrowth() { return areasForGrowth; }
    public void setAreasForGrowth(String areasForGrowth) { this.areasForGrowth = areasForGrowth; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public Instant getCreatedAt() { return createdAt; }
}
