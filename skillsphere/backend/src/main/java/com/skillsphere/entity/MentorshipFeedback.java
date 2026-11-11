package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mentorship_feedback")
public class MentorshipFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private MentorshipSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String comments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MentorshipFeedback() {}

    public MentorshipFeedback(MentorshipSession session, User author, int rating, String comments) {
        this.session = session;
        this.author = author;
        this.rating = rating;
        this.comments = comments;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public MentorshipSession getSession() { return session; }
    public void setSession(MentorshipSession session) { this.session = session; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public Instant getCreatedAt() { return createdAt; }
}
