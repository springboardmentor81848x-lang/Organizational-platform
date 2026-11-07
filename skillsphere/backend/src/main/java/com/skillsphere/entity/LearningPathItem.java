package com.skillsphere.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "learning_path_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"learning_path_id", "course_id"})
})
public class LearningPathItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    public LearningPathItem() {}

    public LearningPathItem(LearningPath learningPath, Course course, int stepOrder) {
        this.learningPath = learningPath;
        this.course = course;
        this.stepOrder = stepOrder;
    }

    public Long getId() { return id; }
    public LearningPath getLearningPath() { return learningPath; }
    public void setLearningPath(LearningPath learningPath) { this.learningPath = learningPath; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
}
