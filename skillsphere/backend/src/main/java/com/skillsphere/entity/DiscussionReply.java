package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "discussion_replies")
public class DiscussionReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thread_id", nullable = false)
    private DiscussionThread thread;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_accepted_answer", nullable = false)
    private boolean isAcceptedAnswer = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DiscussionReply() {}

    public DiscussionReply(DiscussionThread thread, User author, String content) {
        this.thread = thread;
        this.author = author;
        this.content = content;
        this.isAcceptedAnswer = false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public DiscussionThread getThread() { return thread; }
    public void setThread(DiscussionThread thread) { this.thread = thread; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isAcceptedAnswer() { return isAcceptedAnswer; }
    public void setAcceptedAnswer(boolean acceptedAnswer) { isAcceptedAnswer = acceptedAnswer; }
    public Instant getCreatedAt() { return createdAt; }
}
