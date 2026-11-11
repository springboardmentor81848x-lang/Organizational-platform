package com.skillsphere.dto;

import com.skillsphere.entity.SessionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionStatusRequest(
        @NotNull(message = "Status is required.")
        SessionStatus status
) {}
