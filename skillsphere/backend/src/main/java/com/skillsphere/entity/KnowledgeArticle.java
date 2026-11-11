package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "knowledge_articles")
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private KnowledgeCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 200)
    private String tags;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED"; // DRAFT, PUBLISHED, ARCHIVED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public KnowledgeArticle() {}

    public KnowledgeArticle(KnowledgeCategory category, User author, String title, String content, String tags, String status) {
        this.category = category;
        this.author = author;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public KnowledgeCategory getCategory() { return category; }
    public void setCategory(KnowledgeCategory category) { this.category = category; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
