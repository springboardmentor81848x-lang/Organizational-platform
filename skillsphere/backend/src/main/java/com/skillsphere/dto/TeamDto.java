package com.skillsphere.dto;

import java.util.List;

public record TeamDto(
        Long id,
        Long departmentId,
        String departmentName,
        String name,
        Long managerUserId,
        String managerName,
        int memberCount,
        List<TeamMemberDto> members
) {}
