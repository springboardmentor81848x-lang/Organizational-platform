package com.skillsphere.dto;

import java.util.List;

public record MenteeProgressResponse(
        MenteeSummaryDto mentee,
        List<GoalResponse> goals,
        List<SessionResponse> sessions
) {}
