package com.skillsphere.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Development stand-in for a real mail sender: prints the reset link to the server
 * console. Replace with a JavaMailSender / SES implementation for production and set
 * LOG_RESET_LINKS=false.
 */
@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);
    private final boolean logLinks;

    public ConsoleEmailService(@Value("${app.password-reset.log-links-to-console}") boolean logLinks) {
        this.logLinks = logLinks;
    }

    @Override
    public void sendPasswordReset(String toEmail, String fullName, String resetLink) {
        if (logLinks) {
            log.info("[DEV MAIL] Password reset for {} <{}>: {}", fullName, toEmail, resetLink);
        } else {
            log.warn("No mail provider configured; password reset e-mail for {} was not delivered.", toEmail);
        }
    }
}
