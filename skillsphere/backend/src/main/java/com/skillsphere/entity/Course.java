package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false, length = 20)
    private String level; // Beginner, Intermediate, Advanced

    @Column(name = "duration_hours", nullable = false)
    private int durationHours = 10;

    @Column(nullable = false, length = 80)
    private String provider = "SkillSphere Academy";

    @Column(name = "course_url", length = 500)
    private String courseUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Course() {}

    public Course(String title, String description, Skill skill, String level, int durationHours, String provider, String courseUrl) {
        this.title = title;
        this.description = description;
        this.skill = skill;
        this.level = level;
        this.durationHours = durationHours;
        this.provider = provider;
        this.courseUrl = courseUrl;
        this.createdAt = Instant.now();
    }

    public Course(String title, String description, Skill skill, String level, int durationHours, String provider, String courseUrl, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.skill = skill;
        this.level = level;
        this.durationHours = durationHours;
        this.provider = provider;
        this.courseUrl = courseUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getCourseUrl() { return courseUrl; }
    public void setCourseUrl(String courseUrl) { this.courseUrl = courseUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public Instant getCreatedAt() { return createdAt; }
}
