package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;

public record ManagerMentorshipStatusDto(
        int totalEmployees,
        int mentoredEmployeesCount,
        int unmentoredEmployeesCount,
        List<ActiveMentorshipItem> activeMentorships,
        List<MentorshipOpportunityItem> opportunities,
        List<AdminMentorDto> availableMentors
) {
    public record ActiveMentorshipItem(
            Long assignmentId,
            Long employeeId,
            String employeeName,
            String employeeEmail,
            Long mentorId,
            String mentorName,
            String mentorEmail,
            String focusArea,
            String status,
            Instant assignedAt
    ) {}

    public record MentorshipOpportunityItem(
            Long employeeId,
            String employeeName,
            Long skillId,
            String skillName,
            int currentLevel,
            int requiredLevel,
            String gapPriority,
            List<AdminMentorDto> matchingMentors
    ) {}
}
