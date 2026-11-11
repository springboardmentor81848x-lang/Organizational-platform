package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;

public record PostReplyRequest(
        @NotBlank(message = "Reply content cannot be blank.")
        String content
) {}
