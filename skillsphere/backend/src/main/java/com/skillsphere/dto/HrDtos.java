package com.skillsphere.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class HrDtos {

    // ==========================================
    // 1. Overview / Executive Summary
    // ==========================================
    public record HrDashboardOverviewDto(
            long totalEmployees,
            long assessedEmployeesCount,
            long unassessedEmployeesCount,
            double skillCoveragePercentage,
            long criticalShortagesCount,
            long totalOrgGapsCount,
            long activeProgrammesCount,
            long totalCoursesCount,
            long totalEnrollmentsCount,
            double avgCompletionRate,
            List<HrUpcomingProgrammeDto> upcomingProgrammes,
            List<HrRecentActivityDto> recentActivities,
            List<HrPendingActionDto> pendingActions
    ) {}

    public record HrUpcomingProgrammeDto(
            Long id,
            String title,
            String departmentName,
            String startDate,
            String endDate,
            int enrolledCount,
            int capacity,
            String status
    ) {}

    public record HrRecentActivityDto(
            String type,
            String title,
            String description,
            Instant timestamp,
            String actorName
    ) {}

    public record HrPendingActionDto(
            String actionType,
            String title,
            String description,
            String linkTab,
            String priority
    ) {}

    // ==========================================
    // 2. Employee Directory & Profiles
    // ==========================================
    public record HrEmployeeDirectoryItemDto(
            Long id,
            String fullName,
            String email,
            String role,
            String departmentName,
            String jobRole,
            String experienceLevel,
            boolean assessed,
            int verifiedSkillsCount,
            int enrolledCoursesCount,
            Instant joinedDate
    ) {}

    public record HrEmployeeDossierDto(
            Long id,
            String fullName,
            String email,
            String role,
            String departmentName,
            String jobRole,
            String experienceLevel,
            String bio,
            List<HrEmployeeSkillDto> skills,
            List<HrAssessmentSummaryDto> assessmentHistory,
            List<HrEmployeeLearningProgressDto> enrolledCourses,
            List<HrTrainingEnrollmentDto> trainingProgrammes,
            List<HrMentorshipSummaryDto> mentorshipParticipations
    ) {}

    public record HrEmployeeSkillDto(
            Long skillId,
            String skillName,
            String categoryName,
            String proficiencyLevel,
            boolean verified,
            Instant updatedAt
    ) {}

    public record HrAssessmentSummaryDto(
            Long attemptId,
            String skillName,
            String targetLevel,
            Integer score,
            Boolean passed,
            String status,
            Instant completedAt
    ) {}

    public record HrEmployeeLearningProgressDto(
            Long enrollmentId,
            Long courseId,
            String courseTitle,
            String status,
            int progressPct,
            Instant enrolledAt,
            Instant completedAt
    ) {}

    public record HrTrainingEnrollmentDto(
            Long enrollmentId,
            Long programmeId,
            String programmeTitle,
            String status,
            Instant enrolledAt
    ) {}

    public record HrMentorshipSummaryDto(
            Long assignmentId,
            String mentorName,
            String menteeName,
            String roleInSession,
            String status,
            Instant assignedAt
    ) {}

    // ==========================================
    // 3. Competency Framework
    // ==========================================
    public record HrCompetencyDto(
            Long id,
            String name,
            String categoryName,
            Long categoryId,
            String description,
            boolean isActive,
            long associatedRolesCount,
            long associatedCoursesCount,
            long associatedPathsCount,
            Instant createdAt
    ) {}

    public record CreateCompetencyRequest(
            @NotBlank(message = "Competency name is required")
            String name,
            @NotNull(message = "Category is required")
            Long categoryId,
            String description
    ) {}

    public record UpdateCompetencyRequest(
            @NotBlank(message = "Competency name is required")
            String name,
            @NotNull(message = "Category is required")
            Long categoryId,
            String description,
            boolean isActive
    ) {}

    // ==========================================
    // 4. Role Requirements
    // ==========================================
    public record HrRoleRequirementDto(
            Long id,
            String jobRole,
            Long skillId,
            String skillName,
            String categoryName,
            String requiredProficiency,
            String priority,
            boolean isActive,
            Instant createdAt
    ) {}

    public record CreateRoleRequirementRequest(
            @NotBlank(message = "Job role is required")
            String jobRole,
            @NotNull(message = "Skill is required")
            Long skillId,
            @NotBlank(message = "Required proficiency is required")
            String requiredProficiency,
            @NotBlank(message = "Priority is required")
            String priority
    ) {}

    public record UpdateRoleRequirementRequest(
            @NotBlank(message = "Required proficiency is required")
            String requiredProficiency,
            @NotBlank(message = "Priority is required")
            String priority,
            boolean isActive
    ) {}

    public record RoleSkillMatrixDto(
            String jobRole,
            int totalSkillsRequired,
            int highPriorityCount,
            List<HrRoleRequirementDto> requirements
    ) {}

    // ==========================================
    // 5. Organisation Skill Gap Analysis
    // ==========================================
    public record HrSkillGapSummaryDto(
            long totalGaps,
            long highPriorityGaps,
            long mediumPriorityGaps,
            long lowPriorityGaps,
            double avgGapSize,
            List<DepartmentSkillGapDto> departmentBreakdown,
            List<CriticalShortageDto> criticalShortages,
            List<TopGapSkillDto> topGapSkills
    ) {}

    public record DepartmentSkillGapDto(
            String departmentName,
            int employeeCount,
            int gapCount,
            int highPriorityCount,
            List<String> topShortageSkills
    ) {}

    public record CriticalShortageDto(
            Long skillId,
            String skillName,
            String categoryName,
            long affectedEmployeeCount,
            double avgCurrentLevel,
            double avgRequiredLevel,
            double avgGap
    ) {}

    public record TopGapSkillDto(
            Long skillId,
            String skillName,
            String categoryName,
            long gapCount,
            double avgGapSize
    ) {}

    public record OrgHeatmapDto(
            List<String> departments,
            List<String> skills,
            List<OrgHeatmapCellDto> cells
    ) {}

    public record OrgHeatmapCellDto(
            String department,
            String skill,
            double avgProficiency,
            int gapCount,
            String status
    ) {}

    // ==========================================
    // 6. Learning Catalogue
    // ==========================================
    public record HrCourseDto(
            Long id,
            String title,
            String description,
            String level,
            int durationHours,
            String provider,
            String courseUrl,
            String thumbnailUrl,
            Long skillId,
            String skillName,
            boolean isActive,
            long enrolledCount,
            long completedCount,
            Instant createdAt
    ) {}

    public record CreateOrUpdateCourseRequest(
            @NotBlank(message = "Course title is required")
            String title,
            String description,
            Long skillId,
            @NotBlank(message = "Level is required")
            String level,
            @Min(value = 1, message = "Duration must be at least 1 hour")
            int durationHours,
            @NotBlank(message = "Provider is required")
            String provider,
            String courseUrl,
            String thumbnailUrl,
            boolean isActive
    ) {}

    // ==========================================
    // 7. Learning Paths
    // ==========================================
    public record HrLearningPathDto(
            Long id,
            String title,
            String description,
            String targetRole,
            Long skillId,
            String skillName,
            String difficultyLevel,
            boolean isActive,
            int estimatedHours,
            int courseCount,
            List<HrLearningPathItemDto> items,
            Instant createdAt
    ) {}

    public record HrLearningPathItemDto(
            Long id,
            Long courseId,
            String courseTitle,
            String courseProvider,
            String courseLevel,
            int durationHours,
            int stepOrder
    ) {}

    public record CreateOrUpdateLearningPathRequest(
            @NotBlank(message = "Learning path title is required")
            String title,
            String description,
            String targetRole,
            Long skillId,
            String difficultyLevel,
            boolean isActive,
            List<Long> courseIds
    ) {}

    // ==========================================
    // 8. Training Programmes
    // ==========================================
    public record HrTrainingProgrammeDto(
            Long id,
            String title,
            String description,
            Long departmentId,
            String departmentName,
            Long skillId,
            String skillName,
            Long courseId,
            String courseTitle,
            String startDate,
            String endDate,
            int capacity,
            int enrolledCount,
            String trainerName,
            String status,
            Instant createdAt
    ) {}

    public record CreateOrUpdateTrainingProgrammeRequest(
            @NotBlank(message = "Title is required")
            String title,
            String description,
            Long departmentId,
            Long skillId,
            Long courseId,
            @NotNull(message = "Start date is required")
            LocalDate startDate,
            @NotNull(message = "End date is required")
            LocalDate endDate,
            @Min(value = 1, message = "Capacity must be at least 1")
            int capacity,
            String trainerName,
            String status
    ) {}

    public record ProgrammeEnrollmentDetailDto(
            Long id,
            Long programmeId,
            String programmeTitle,
            Long userId,
            String employeeName,
            String employeeEmail,
            String departmentName,
            String status,
            Instant enrolledAt
    ) {}

    public record EnrollEmployeeRequest(
            @NotNull(message = "User ID is required")
            Long userId
    ) {}

    public record UpdateProgrammeEnrollmentStatusRequest(
            @NotBlank(message = "Status is required")
            String status
    ) {}

    // ==========================================
    // 9. Mentor Directory
    // ==========================================
    public record HrMentorDirectoryItemDto(
            Long mentorId,
            Long userId,
            String fullName,
            String email,
            String headline,
            String department,
            String jobRole,
            String experienceLevel,
            String expertiseAreas,
            List<String> expertiseTags,
            int maxMentees,
            int activeMenteesCount,
            long completedSessionsCount,
            double avgRating,
            boolean isActive,
            Instant approvedAt
    ) {}

    public record MentorsForSkillDto(
            Long skillId,
            String skillName,
            List<HrMentorDirectoryItemDto> mentors
    ) {}

    // ==========================================
    // 10. Learning Progress
    // ==========================================
    public record HrLearningProgressOverviewDto(
            long totalCourseEnrollments,
            long completedCourseEnrollments,
            long inProgressCourseEnrollments,
            double completionRatePercentage,
            long totalProgrammeEnrollments,
            long attendedProgrammeEnrollments,
            List<HrEmployeeCourseProgressItemDto> progressItems
    ) {}

    public record HrEmployeeCourseProgressItemDto(
            Long enrollmentId,
            Long userId,
            String employeeName,
            String employeeEmail,
            String departmentName,
            Long courseId,
            String courseTitle,
            String courseLevel,
            int progressPct,
            String status,
            Instant enrolledAt,
            Instant completedAt
    ) {}

    // ==========================================
    // 11. Feedback & Effectiveness
    // ==========================================
    public record HrFeedbackOverviewDto(
            double overallPlatformRating,
            long totalPlatformFeedbacks,
            double overallMentorshipRating,
            long totalMentorshipFeedbacks,
            List<HrPlatformFeedbackItemDto> platformFeedbacks,
            List<HrMentorshipFeedbackItemDto> mentorshipFeedbacks,
            Map<String, Double> ratingByCategory
    ) {}

    public record HrPlatformFeedbackItemDto(
            Long id,
            Long userId,
            String userName,
            String userEmail,
            String category,
            int rating,
            String comments,
            Instant createdAt
    ) {}

    public record HrMentorshipFeedbackItemDto(
            Long id,
            Long sessionId,
            String authorName,
            String mentorName,
            int rating,
            String comments,
            Instant createdAt
    ) {}

    // ==========================================
    // 12. Reports & Analytics & Leadership Sharing
    // ==========================================
    public record LeadershipReportShareDto(
            Long id,
            String reportType,
            String title,
            Long sharedById,
            String sharedByName,
            String sharedWithRole,
            Long departmentId,
            String departmentName,
            String summaryNotes,
            String reportDataJson,
            Instant createdAt
    ) {}

    public record ShareReportRequest(
            @NotBlank(message = "Report type is required")
            String reportType,
            @NotBlank(message = "Title is required")
            String title,
            @NotBlank(message = "Shared with role is required")
            String sharedWithRole,
            Long departmentId,
            String summaryNotes,
            String reportDataJson
    ) {}

    public record HrReportResultDto(
            String reportType,
            String title,
            Instant generatedAt,
            Map<String, Object> data
    ) {}

    // ==========================================
    // 13. Notifications & Announcements
    // ==========================================
    public record HrAnnouncementDto(
            Long id,
            Long authorId,
            String authorName,
            String targetType,
            Long departmentId,
            String departmentName,
            String targetRole,
            String title,
            String content,
            String priority,
            Instant createdAt,
            long readCount,
            long totalTargetCount,
            boolean readByCurrentUser
    ) {}

    public record CreateAnnouncementRequest(
            @NotBlank(message = "Title is required")
            String title,
            @NotBlank(message = "Content is required")
            String content,
            @NotBlank(message = "Target type is required")
            String targetType,
            Long departmentId,
            String targetRole,
            @NotBlank(message = "Priority is required")
            String priority
    ) {}
}
