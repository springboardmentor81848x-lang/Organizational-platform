package com.skillsphere.dto;

import java.time.LocalDate;

public record CertificateDto(
        Long id,
        Long courseId,
        String courseTitle,
        String certificateCode,
        String title,
        LocalDate issueDate,
        LocalDate expiryDate,
        String credentialUrl
) {}
