package com.skillsphere.dto;

import com.skillsphere.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull(message = "Status is required.") AccountStatus status) {}
