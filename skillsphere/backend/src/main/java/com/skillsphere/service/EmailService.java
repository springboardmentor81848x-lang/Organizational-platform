package com.skillsphere.service;

public interface EmailService {
    void sendPasswordReset(String toEmail, String fullName, String resetLink);
}
