package com.skillsphere.dto;

import java.util.List;

public record MentorDirectoryItem(
        Long mentorProfileId,
        Long userId,
        String fullName,
        String email,
        String headline,
        String department,
        String jobRole,
        String experienceLevel,
        List<String> expertiseAreas,
        String bio,
        int maxMentees,
        int currentMenteesCount,
        boolean isAvailable
) {}
