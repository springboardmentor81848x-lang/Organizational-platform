package com.skillsphere.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is missing.")
        String token,

        @NotBlank(message = "Password is required.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
                message = "Use 8-72 characters with upper and lower case letters, a number, and a symbol.")
        String password,

        @NotBlank(message = "Confirm your password.")
        String confirmPassword
) {}
