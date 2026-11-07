package com.skillsphere.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "leadership_report_shares")
public class LeadershipReportShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_type", nullable = false, length = 60)
    private String reportType;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_by_id", nullable = false)
    private User sharedBy;

    @Column(name = "shared_with_role", nullable = false, length = 30)
    private String sharedWithRole; // MANAGER, DEPARTMENT_HEAD, ALL_LEADERSHIP

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "summary_notes", columnDefinition = "TEXT")
    private String summaryNotes;

    @Column(name = "report_data_json", columnDefinition = "TEXT")
    private String reportDataJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public LeadershipReportShare() {}

    public LeadershipReportShare(String reportType, String title, User sharedBy, String sharedWithRole, Department department, String summaryNotes, String reportDataJson) {
        this.reportType = reportType;
        this.title = title;
        this.sharedBy = sharedBy;
        this.sharedWithRole = sharedWithRole;
        this.department = department;
        this.summaryNotes = summaryNotes;
        this.reportDataJson = reportDataJson;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public User getSharedBy() { return sharedBy; }
    public void setSharedBy(User sharedBy) { this.sharedBy = sharedBy; }
    public String getSharedWithRole() { return sharedWithRole; }
    public void setSharedWithRole(String sharedWithRole) { this.sharedWithRole = sharedWithRole; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getSummaryNotes() { return summaryNotes; }
    public void setSummaryNotes(String summaryNotes) { this.summaryNotes = summaryNotes; }
    public String getReportDataJson() { return reportDataJson; }
    public void setReportDataJson(String reportDataJson) { this.reportDataJson = reportDataJson; }
    public Instant getCreatedAt() { return createdAt; }
}
