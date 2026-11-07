package com.skillsphere.dto;

import java.util.List;
import java.util.Map;

public class ReportDtos {

    public record EmployeeReportDto(
            String employeeName,
            String employeeId,
            String email,
            String jobRole,
            String department,
            int totalSkills,
            int averageSkillLevel,
            int activeGaps,
            int coursesCompleted,
            int activeMentorships,
            List<SkillGapDto> skillGaps,
            List<EmployeeLearningDto> learningRecords,
            List<CertificateDto> certificates
    ) {}

    public record TeamReportDto(
            Long teamId,
            String teamName,
            String managerName,
            int totalMembers,
            Map<String, Integer> skillCoverageSummary,
            List<SkillGapDto> topTeamGaps,
            int totalCoursesEnrolled,
            int totalCoursesCompleted,
            List<TeamMemberProgressDto> memberSummaries
    ) {
        public record TeamMemberProgressDto(
                Long userId,
                String fullName,
                String jobRole,
                int skillsCount,
                int openGapsCount,
                int completedCoursesCount
        ) {}
    }

    public record DepartmentReportDto(
            Long departmentId,
            String departmentName,
            String headName,
            int totalEmployees,
            int totalTeams,
            Map<String, Double> departmentSkillAverages,
            List<DepartmentRiskDto> capabilityRisks,
            int completedTrainingsCount
    ) {
        public record DepartmentRiskDto(
                String skillName,
                int affectedEmployeesCount,
                String riskSeverity // HIGH, MEDIUM, LOW
        ) {}
    }

    public record OrganizationReportDto(
            int totalUsers,
            Map<String, Long> usersByRole,
            int totalDepartments,
            int totalSkillsCatalogued,
            int totalAssessmentsCompleted,
            int totalGapsIdentified,
            int totalCoursesCompleted,
            int totalActiveMentorships,
            List<DepartmentSummaryDto> departmentSummaries
    ) {
        public record DepartmentSummaryDto(
                String departmentName,
                int employeeCount,
                int openGapsCount,
                int completedTrainingsCount
        ) {}
    }

    public record AdminReportDto(
            long totalUsers,
            long activeUsers,
            long suspendedUsers,
            long pendingUsers,
            Map<String, Long> roleDistribution,
            long totalAuditLogs,
            long openSupportTickets,
            long totalConversations,
            long totalMessages
    ) {}
}
