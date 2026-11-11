package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;

public record AdminMentorDto(
        Long mentorProfileId,
        Long userId,
        String fullName,
        String email,
        String employeeId,
        String headline,
        List<String> expertiseAreas,
        int maxMentees,
        int currentMenteesCount,
        boolean isActive,
        String approvedByName,
        Instant createdAt
) {}
