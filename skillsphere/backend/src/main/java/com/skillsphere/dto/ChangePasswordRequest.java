package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,

        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&^_-]).{8,}$",
                 message = "New password must contain at least one letter, one number, and one special character.")
        String newPassword,

        @NotBlank(message = "Password confirmation is required.")
        String confirmPassword
) {}
