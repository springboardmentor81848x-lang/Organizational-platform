package com.skillsphere.dto;

import com.skillsphere.entity.GoalStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateGoalStatusRequest(
        @NotNull(message = "Status is required.")
        GoalStatus status
) {}
