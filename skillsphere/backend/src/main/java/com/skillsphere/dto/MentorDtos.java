package com.skillsphere.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class MentorDtos {

    public record MentorDashboardOverviewDto(
            MentorProfileDto mentorProfile,
            long activeMenteesCount,
            long pendingRequestsCount,
            long completedSessionsCount,
            long upcomingSessionsCount,
            double goalCompletionRate,
            long sharedResourcesCount,
            long knowledgeArticlesCount,
            List<MentorSessionSummaryDto> upcomingSessions,
            List<MentorshipRequestResponseDto> pendingRequests,
            List<MenteeOverviewItemDto> activeMentees,
            List<MentorRecentActivityDto> recentActivities
    ) {}

    public record MentorProfileDto(
            Long id,
            Long userId,
            String fullName,
            String email,
            String headline,
            String bio,
            String department,
            String jobRole,
            String experienceLevel,
            String expertiseAreas,
            List<String> expertiseTags,
            int maxMentees,
            int activeMenteesCount,
            boolean isActive,
            String preferredFormat,
            List<MentorAvailabilitySlotDto> availabilitySlots
    ) {}

    public record UpdateMentorProfileRequest(
            @NotBlank(message = "Headline is required")
            @Size(max = 140, message = "Headline must be 140 characters or fewer")
            String headline,

            String bio,

            @NotBlank(message = "Expertise areas are required")
            String expertiseAreas,

            @Min(value = 1, message = "Max mentees must be at least 1")
            @Max(value = 15, message = "Max mentees cannot exceed 15")
            int maxMentees,

            boolean isActive,

            String preferredFormat,

            List<MentorAvailabilitySlotDto> availabilitySlots
    ) {}

    public record MentorAvailabilitySlotDto(
            Long id,
            String dayOfWeek,
            String startTime,
            String endTime,
            String sessionFormat,
            boolean isAvailable
    ) {}

    public record MenteeOverviewItemDto(
            Long assignmentId,
            Long employeeId,
            String employeeName,
            String email,
            String employeeCode,
            String jobRole,
            String department,
            String status,
            Instant assignedAt,
            int totalGoals,
            int completedGoals,
            int totalSessions,
            Instant nextSessionDate
    ) {}

    public record MenteeDossierDto(
            MenteeOverviewItemDto summary,
            String bio,
            String experienceLevel,
            MenteeSkillDetailDto skills,
            List<MentorGoalDetailDto> goals,
            List<MentorSessionSummaryDto> sessions,
            List<MentorSharedResourceDto> resources,
            List<MentorEvaluationDto> evaluations
    ) {}

    public record MenteeSkillDetailDto(
            Long employeeId,
            String employeeName,
            boolean consentGranted,
            List<MenteeSkillRecordDto> skills,
            List<MenteeSkillGapRecordDto> gaps,
            List<MenteeAssessmentSummaryDto> assessments
    ) {}

    public record MenteeSkillRecordDto(
            Long skillId,
            String skillName,
            String category,
            String proficiencyLevel,
            boolean verified,
            Instant updatedAt
    ) {}

    public record MenteeSkillGapRecordDto(
            Long gapId,
            String skillName,
            String currentProficiency,
            String requiredProficiency,
            String severity
    ) {}

    public record MenteeAssessmentSummaryDto(
            Long assessmentId,
            String assessmentTitle,
            String skillName,
            int score,
            boolean passed,
            Instant completedAt
    ) {}

    public record MentorGoalDetailDto(
            Long id,
            Long assignmentId,
            Long employeeId,
            String employeeName,
            String title,
            String description,
            LocalDate targetDate,
            String status,
            String priority,
            Long skillId,
            String skillName,
            Instant createdAt,
            List<GoalActionItemDto> actionItems
    ) {}

    public record CreateMentorGoalRequest(
            @NotNull(message = "Assignment ID is required")
            Long assignmentId,

            @NotBlank(message = "Title is required")
            @Size(max = 140, message = "Title must be 140 characters or fewer")
            String title,

            String description,

            LocalDate targetDate,

            String priority,

            Long skillId
    ) {}

    public record UpdateMentorGoalRequest(
            @NotBlank(message = "Status is required")
            String status,

            String title,

            String description,

            LocalDate targetDate,

            String priority
    ) {}

    public record GoalActionItemDto(
            Long id,
            Long goalId,
            String title,
            String description,
            boolean isCompleted,
            LocalDate dueDate,
            Instant completedAt
    ) {}

    public record CreateGoalActionItemRequest(
            @NotBlank(message = "Action item title is required")
            @Size(max = 200, message = "Title must be 200 characters or fewer")
            String title,

            String description,

            LocalDate dueDate
    ) {}

    public record MentorSessionSummaryDto(
            Long id,
            Long assignmentId,
            Long employeeId,
            String employeeName,
            String title,
            String description,
            Instant scheduledAt,
            int durationMinutes,
            String meetingLink,
            String status,
            String sharedNotes,
            String mentorPrepNotes
    ) {}

    public record CreateMentorSessionRequest(
            @NotNull(message = "Assignment ID is required")
            Long assignmentId,

            @NotBlank(message = "Title is required")
            @Size(max = 140, message = "Title must be 140 characters or fewer")
            String title,

            String description,

            @NotNull(message = "Scheduled time is required")
            Instant scheduledAt,

            @Min(value = 15, message = "Duration must be at least 15 minutes")
            @Max(value = 240, message = "Duration cannot exceed 240 minutes")
            int durationMinutes,

            String meetingLink
    ) {}

    public record UpdateSessionNotesRequest(
            String sharedNotes,
            String mentorPrepNotes
    ) {}

    public record MentorSharedResourceDto(
            Long id,
            Long assignmentId,
            Long employeeId,
            String employeeName,
            String title,
            String urlOrContent,
            String resourceType,
            String description,
            Long skillId,
            String skillName,
            Instant createdAt
    ) {}

    public record CreateMentorSharedResourceRequest(
            @NotNull(message = "Assignment ID is required")
            Long assignmentId,

            @NotBlank(message = "Title is required")
            @Size(max = 140, message = "Title must be 140 characters or fewer")
            String title,

            @NotBlank(message = "URL or content is required")
            String urlOrContent,

            String resourceType,

            String description,

            Long skillId
    ) {}

    public record MentorEvaluationDto(
            Long id,
            Long mentorId,
            String mentorName,
            Long employeeId,
            String employeeName,
            Long sessionId,
            String sessionTitle,
            Integer rating,
            String strengths,
            String areasForGrowth,
            String comments,
            Instant createdAt
    ) {}

    public record CreateMentorEvaluationRequest(
            @NotNull(message = "Employee ID is required")
            Long employeeId,

            Long sessionId,

            @NotNull(message = "Rating is required")
            @Min(value = 1, message = "Rating must be between 1 and 5")
            @Max(value = 5, message = "Rating must be between 1 and 5")
            Integer rating,

            String strengths,

            String areasForGrowth,

            @NotBlank(message = "Comments are required")
            String comments
    ) {}

    public record MentorFeedbackOverviewDto(
            double averageRating,
            int totalReviewsReceived,
            Map<Integer, Long> ratingDistribution,
            List<MentorshipFeedbackResponseDto> receivedFeedback,
            List<MentorEvaluationDto> submittedEvaluations
    ) {}

    public record MentorshipFeedbackResponseDto(
            Long id,
            Long sessionId,
            String sessionTitle,
            String menteeName,
            Integer rating,
            String comments,
            Instant createdAt
    ) {}

    public record MentorArticleDto(
            Long id,
            Long categoryId,
            String categoryName,
            String title,
            String content,
            String tags,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CreateOrUpdateMentorArticleRequest(
            Long categoryId,

            @NotBlank(message = "Title is required")
            @Size(max = 140, message = "Title must be 140 characters or fewer")
            String title,

            @NotBlank(message = "Content is required")
            String content,

            String tags,

            String status
    ) {}

    public record MentorRecentActivityDto(
            String type,
            String title,
            String description,
            Instant timestamp,
            String link
    ) {}

    public record EndMentorshipRequest(
            String reason
    ) {}
}
