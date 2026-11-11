package com.skillsphere.dto;

import java.time.Instant;

public record SessionResponse(
        Long id,
        Long assignmentId,
        String title,
        String description,
        Instant scheduledAt,
        int durationMinutes,
        String meetingLink,
        String status,
        Instant createdAt,
        FeedbackResponse feedback
) {}
