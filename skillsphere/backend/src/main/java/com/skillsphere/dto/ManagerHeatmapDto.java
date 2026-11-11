package com.skillsphere.dto;

import java.util.List;

public record ManagerHeatmapDto(
        EmployeeSkillMatrix employeeSkillMatrix,
        SkillTeamMatrix skillTeamMatrix
) {
    public record HeatmapEmployee(Long id, String name, Long teamId, String teamName) {}
    public record HeatmapTeam(Long id, String name) {}
    public record HeatmapSkill(Long id, String name, String categoryName) {}

    public record EmployeeSkillCell(
            Long employeeId,
            Long skillId,
            int currentLevel,
            String currentProficiency,
            int requiredLevel,
            String requiredProficiency,
            int gapSize,
            String severity // CRITICAL, MODERATE, MET, EXCEEDED, NO_DATA
    ) {}

    public record SkillTeamCell(
            Long teamId,
            Long skillId,
            double averageLevel,
            String averageProficiency,
            double coveragePercentage,
            int gapCount
    ) {}

    public record EmployeeSkillMatrix(
            List<HeatmapEmployee> employees,
            List<HeatmapSkill> skills,
            List<EmployeeSkillCell> cells
    ) {}

    public record SkillTeamMatrix(
            List<HeatmapTeam> teams,
            List<HeatmapSkill> skills,
            List<SkillTeamCell> cells
    ) {}
}
