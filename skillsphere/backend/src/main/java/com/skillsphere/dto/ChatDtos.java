package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class ChatDtos {

    public record ChatContactDto(
            Long id,
            String fullName,
            String email,
            String role,
            String department,
            String relationship // MENTOR, MENTEE, TEAM_MEMBER, MANAGER, COLLEAGUE
    ) {}

    public record CreateTeamConversationRequest(
            @NotNull(message = "Team ID is required")
            Long teamId,

            @Size(max = 140, message = "Title cannot exceed 140 characters")
            String title
    ) {}

    public record ReportMessageRequest(
            @NotBlank(message = "Reason is required")
            @Size(max = 255, message = "Reason cannot exceed 255 characters")
            String reason,

            @Size(max = 2000, message = "Details cannot exceed 2000 characters")
            String details
    ) {}

    public record MessageReportResponse(
            Long id,
            Long messageId,
            Long reporterId,
            String reason,
            String status,
            Instant createdAt
    ) {}

    public record AttachmentUploadResponse(
            String fileUrl,
            String originalFileName,
            long sizeBytes,
            String contentType
    ) {}
}
