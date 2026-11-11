package com.skillsphere.dto;

public record SkillDto(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String description
) {}
