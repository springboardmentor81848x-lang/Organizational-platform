package com.skillsphere.dto;

public record TeamSkillInventoryDto(
        Long skillId,
        String skillName,
        String categoryName,
        int employeeCount,
        double averageProficiencyLevel,
        String averageProficiencyName,
        int requiredProficiencyLevel,
        String requiredProficiencyName,
        int unawareCount,
        int beginnerCount,
        int intermediateCount,
        int advancedCount,
        int expertCount,
        String status // MET, GAP, EXCEEDED
) {}
