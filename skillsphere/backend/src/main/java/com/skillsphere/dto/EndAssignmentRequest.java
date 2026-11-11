package com.skillsphere.dto;

import com.skillsphere.entity.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EndAssignmentRequest(
        @NotNull(message = "Status must be COMPLETED or TERMINATED.")
        AssignmentStatus status,

        @Size(max = 1000)
        String notes
) {}
