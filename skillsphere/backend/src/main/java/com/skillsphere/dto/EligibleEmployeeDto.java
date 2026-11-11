package com.skillsphere.dto;

public record EligibleEmployeeDto(
        Long userId,
        String fullName,
        String email,
        String employeeId,
        String jobRole,
        String department,
        boolean isApprovedMentor,
        boolean hasActiveMentor
) {}
