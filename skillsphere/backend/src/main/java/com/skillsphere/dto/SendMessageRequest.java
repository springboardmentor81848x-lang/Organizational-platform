package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message content cannot be blank.")
        @Size(max = 5000, message = "Message is too long.")
        String content,

        @Size(max = 500)
        String attachmentUrl,

        @Size(max = 140)
        String attachmentName
) {}
