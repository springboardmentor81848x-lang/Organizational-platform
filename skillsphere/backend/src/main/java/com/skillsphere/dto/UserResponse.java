package com.skillsphere.dto;

import com.skillsphere.entity.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String employeeId,
        String role,
        String requestedRole,
        String accountStatus,
        Instant createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getEmployeeId(),
                u.getRole().name(),
                u.getRequestedRole() == null ? null : u.getRequestedRole().name(),
                u.getAccountStatus().name(),
                u.getCreatedAt());
    }
}
