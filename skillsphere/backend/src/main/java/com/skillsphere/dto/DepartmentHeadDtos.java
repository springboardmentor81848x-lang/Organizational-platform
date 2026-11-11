package com.skillsphere.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DepartmentHeadDtos {

    // ==========================================
    // 1. Dashboard Overview
    // ==========================================
    public record DepartmentHeadOverviewDto(
            Long departmentId,
            String departmentName,
            String departmentCode,
            String headName,
            String headEmail,
            long totalEmployees,
            long totalTeams,
            long totalManagers,
            long assessedEmployeesCount,
            long unassessedEmployeesCount,
            double skillCoveragePercentage,
            long criticalShortagesCount,
            long totalDepartmentGapsCount,
            long activeProgrammesCount,
            long totalEnrollmentsCount,
            double avgCompletionRate,
            long activeMentorshipsCount,
            List<DeptUpcomingProgrammeDto> upcomingProgrammes,
            List<DeptRecentActivityDto> recentActivities,
            List<DeptPendingAlertDto> priorityAlerts
    ) {}

    public record DeptUpcomingProgrammeDto(
            Long id,
            String title,
            String startDate,
            String endDate,
            int enrolledCount,
            int capacity,
            String trainerName,
            String status
    ) {}

    public record DeptRecentActivityDto(
            String type,
            String title,
            String description,
            Instant timestamp,
            String actorName
    ) {}

    public record DeptPendingAlertDto(
            String alertType,
            String title,
            String description,
            String severity,
            String linkTab,
            String responsibleRole
    ) {}

    // ==========================================
    // 2. Department Overview & Team Breakdown
    // ==========================================
    public record DepartmentProfileDto(
            Long id,
            String name,
            String code,
            String description,
            String headName,
            String headEmail,
            long totalEmployees,
            long totalTeams,
            long totalManagers,
            List<DeptTeamSummaryDto> teams,
            List<DeptWorkforceDistributionDto> workforceDistribution
    ) {}

    public record DeptTeamSummaryDto(
            Long teamId,
            String teamName,
            Long managerId,
            String managerName,
            String managerEmail,
            int memberCount,
            int assessedCount,
            int unassessedCount,
            double skillCoveragePercentage,
            int criticalGapsCount,
            List<String> topSkills
    ) {}

    public record DeptWorkforceDistributionDto(
            String roleOrTitle,
            int count,
            double percentage
    ) {}

    // ==========================================
    // 3. Department Skill Matrix
    // ==========================================
    public record DeptSkillMatrixDto(
            Long departmentId,
            String departmentName,
            int totalCompetencies,
            List<DeptCompetencyMatrixRowDto> competencies
    ) {}

    public record DeptCompetencyMatrixRowDto(
            Long skillId,
            String skillName,
            String categoryName,
            String targetProficiency,
            int targetLevel,
            String priority,
            int totalRequired,
            int exceedingCount,
            int meetingCount,
            int belowCount,
            int unassessedCount,
            double compliancePercentage,
            List<DeptTeamSkillStatusDto> teamBreakdown
    ) {}

    public record DeptTeamSkillStatusDto(
            Long teamId,
            String teamName,
            double avgProficiency,
            String complianceStatus
    ) {}

    // ==========================================
    // 4. Gap Heatmap
    // ==========================================
    public record DeptHeatmapDto(
            String departmentName,
            List<String> teams,
            List<String> competencies,
            List<DeptHeatmapCellDto> cells
    ) {}

    public record DeptHeatmapCellDto(
            Long teamId,
            String teamName,
            Long skillId,
            String skillName,
            double avgProficiency,
            int requiredLevel,
            double gapSize,
            String severity, // CRITICAL, HIGH, MODERATE, LOW, ADEQUATE
            int assessedCount,
            int totalTeamMembers
    ) {}

    // ==========================================
    // 5. Workforce Capability
    // ==========================================
    public record DeptCapabilityProfileDto(
            Long departmentId,
            String departmentName,
            double readinessPercentage,
            double assessmentCoveragePercentage,
            List<DeptCompetencyStrengthDto> strongestSkills,
            List<DeptCompetencyStrengthDto> weakestSkills,
            List<DeptCriticalShortageItemDto> criticalShortages,
            List<String> priorityRecommendations
    ) {}

    public record DeptCompetencyStrengthDto(
            Long skillId,
            String skillName,
            String categoryName,
            double avgProficiency,
            int verifiedCount,
            String status // STRONG, MODERATE, WEAK
    ) {}

    public record DeptCriticalShortageItemDto(
            Long skillId,
            String skillName,
            String categoryName,
            int affectedTeamsCount,
            int affectedEmployeesCount,
            double avgProficiency,
            double targetProficiency,
            double avgGap,
            String recommendedAction
    ) {}

    // ==========================================
    // 6. Learning & Mentoring Overview
    // ==========================================
    public record DeptLearningMentoringDto(
            DeptLearningSummaryDto learningOverview,
            DeptMentoringSummaryDto mentoringOverview
    ) {}

    public record DeptLearningSummaryDto(
            long totalEnrolledEmployees,
            long totalCourseEnrollments,
            long inProgressCount,
            long completedCount,
            double completionRatePercentage,
            long programmesCount,
            long attendedProgrammesCount,
            List<DeptCourseParticipationDto> topCourses
    ) {}

    public record DeptMentoringSummaryDto(
            long activeAssignmentsCount,
            long totalSessionsCompleted,
            double avgMentorshipRating,
            long activeMenteesCount,
            long activeMentorsCount,
            List<String> topMentoredSkills
    ) {}

    public record DeptCourseParticipationDto(
            Long courseId,
            String courseTitle,
            String provider,
            String level,
            int enrolledCount,
            int completedCount,
            double avgProgressPct
    ) {}

    // ==========================================
    // 7. Risk & Priority Alerts
    // ==========================================
    public record DeptRiskAlertDto(
            String id,
            String alertType,
            String title,
            String description,
            String affectedTeamName,
            String affectedSkillName,
            String severity, // CRITICAL, HIGH, MEDIUM, LOW
            String evidenceSummary,
            Instant dateIdentified,
            String recommendedAction,
            String responsibleRole,
            String status // ACTIVE, IN_REVIEW, RESOLVED
    ) {}

    // ==========================================
    // 8. Reports & Analytics
    // ==========================================
    public record DeptReportResultDto(
            String reportType,
            String title,
            String departmentName,
            Instant generatedAt,
            Map<String, Object> summaryStats,
            List<String> tableHeaders,
            List<Map<String, Object>> tableRows
    ) {}

    public record DeptLeadershipSharedReportDto(
            Long id,
            String reportType,
            String title,
            String sharedByName,
            String sharedWithRole,
            String summaryNotes,
            String reportDataJson,
            Instant createdAt
    ) {}

    // ==========================================
    // 9. Announcements
    // ==========================================
    public record DeptAnnouncementDto(
            Long id,
            Long authorId,
            String authorName,
            String title,
            String content,
            String priority,
            Instant createdAt,
            long readCount,
            long totalTargetCount,
            boolean readByCurrentUser
    ) {}

    public record CreateDeptAnnouncementRequest(
            @NotBlank(message = "Title is required")
            String title,
            @NotBlank(message = "Content is required")
            String content,
            @NotBlank(message = "Priority is required")
            String priority
    ) {}

    // ==========================================
    // 10. Notifications
    // ==========================================
    public record DeptNotificationDto(
            Long id,
            String type,
            String title,
            String message,
            String link,
            boolean isRead,
            Instant createdAt
    ) {}
}
