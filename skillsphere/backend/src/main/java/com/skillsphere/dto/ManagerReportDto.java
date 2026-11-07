package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ManagerReportDto(
        String reportType,
        String title,
        Instant generatedAt,
        Long teamId,
        String teamName,
        String managerName,
        int totalRecords,
        Map<String, Object> summaryMetrics,
        List<String> columns,
        List<Map<String, Object>> rows
) {}
