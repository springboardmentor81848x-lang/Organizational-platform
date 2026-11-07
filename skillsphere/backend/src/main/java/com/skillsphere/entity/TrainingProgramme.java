package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "training_programmes")
public class TrainingProgramme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private int capacity = 30;

    @Column(name = "enrolled_count", nullable = false)
    private int enrolledCount = 0;

    @Column(name = "trainer_name", length = 100)
    private String trainerName;

    @Column(nullable = false, length = 20)
    private String status = "UPCOMING"; // UPCOMING, ACTIVE, COMPLETED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TrainingProgramme() {}

    public TrainingProgramme(String title, String description, Department department, LocalDate startDate, LocalDate endDate, int capacity, String trainerName) {
        this.title = title;
        this.description = description;
        this.department = department;
        this.startDate = startDate;
        this.endDate = endDate;
        this.capacity = capacity;
        this.trainerName = trainerName;
        this.status = "UPCOMING";
        this.enrolledCount = 0;
        this.createdAt = Instant.now();
    }

    public TrainingProgramme(String title, String description, Department department, LocalDate startDate, LocalDate endDate, int capacity, String trainerName, String status) {
        this.title = title;
        this.description = description;
        this.department = department;
        this.startDate = startDate;
        this.endDate = endDate;
        this.capacity = capacity;
        this.trainerName = trainerName;
        this.status = status != null ? status : "UPCOMING";
        this.enrolledCount = 0;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Skill getSkill() { return skill; }
    public void setSkill(Skill skill) { this.skill = skill; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
