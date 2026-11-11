package com.skillsphere.dto;

public record CourseDto(
        Long id,
        String title,
        String description,
        Long skillId,
        String skillName,
        String level,
        int durationHours,
        String provider,
        String courseUrl,
        String thumbnailUrl,
        String targetGapPriority // HIGH, MEDIUM, ON_TRACK if recommended
) {}
