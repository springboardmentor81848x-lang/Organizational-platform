package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;

public record MyMentorResponse(
        Long assignmentId,
        Long mentorUserId,
        String mentorName,
        String mentorEmail,
        String headline,
        String department,
        String jobRole,
        List<String> expertiseAreas,
        String notes,
        String status,
        Instant assignedAt
) {}
