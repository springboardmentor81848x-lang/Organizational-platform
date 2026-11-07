package com.skillsphere.dto;

import java.time.LocalDate;

public record TrainingProgrammeDto(
        Long id,
        String title,
        String description,
        Long departmentId,
        String departmentName,
        LocalDate startDate,
        LocalDate endDate,
        int capacity,
        int enrolledCount,
        String trainerName,
        String status, // UPCOMING, ACTIVE, COMPLETED
        boolean isEnrolled
) {}
