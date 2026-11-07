package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "hr_announcements")
public class HrAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType = "ORGANIZATION"; // ORGANIZATION, DEPARTMENT, ROLE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "target_role", length = 80)
    private String targetRole;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String priority = "NORMAL"; // NORMAL, HIGH, URGENT

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public HrAnnouncement() {}

    public HrAnnouncement(User author, String targetType, Department department, String targetRole, String title, String content, String priority) {
        this.author = author;
        this.targetType = targetType != null ? targetType : "ORGANIZATION";
        this.department = department;
        this.targetRole = targetRole;
        this.title = title;
        this.content = content;
        this.priority = priority != null ? priority : "NORMAL";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Instant getCreatedAt() { return createdAt; }
}
