package com.skillsphere.dto;

public record DepartmentDto(
        Long id,
        String name,
        String code,
        String description,
        Long headUserId,
        String headUserName
) {}
