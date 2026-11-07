package com.skillsphere.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Full name is required.")
        @Size(min = 2, max = 120, message = "Full name must be 2-120 characters.")
        String fullName,

        @NotBlank(message = "Work email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email is too long.")
        String email,

        @Pattern(regexp = "^$|^[A-Za-z0-9._/-]{3,30}$",
                message = "Employee ID must be 3-30 letters, numbers, or . _ / -")
        String employeeId,

        /** What the person is asking for. It is recorded for admin review but never granted automatically. */
        @Pattern(regexp = "^(?i)(EMPLOYEE|MANAGER|MENTOR|HR_SPECIALIST|DEPARTMENT_HEAD|ADMIN)$", message = "Choose a valid role.")
        String role,

        @NotBlank(message = "Password is required.")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
                message = "Use 8-72 characters with upper and lower case letters, a number, and a symbol.")
        String password,

        @NotBlank(message = "Confirm your password.")
        String confirmPassword
) {}
