package com.skillsphere.service;

import com.skillsphere.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Simple in-memory brute-force protection (per e-mail). Replace with Redis for multi-node deployments. */
@Service
public class LoginAttemptService {

    private record Attempt(int failures, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockDuration;

    public LoginAttemptService(@Value("${app.login.max-failed-attempts}") int maxFailures,
                               @Value("${app.login.lock-minutes}") long lockMinutes) {
        this.maxFailures = maxFailures;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public void assertNotLocked(String email) {
        Attempt a = attempts.get(email);
        if (a != null && a.lockedUntil() != null) {
            if (Instant.now().isBefore(a.lockedUntil())) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Too many failed sign-in attempts. Try again in a few minutes.");
            }
            attempts.remove(email);
        }
    }

    public void recordFailure(String email) {
        attempts.merge(email, new Attempt(1, null), (old, ignored) -> {
            int failures = old.failures() + 1;
            return failures >= maxFailures
                    ? new Attempt(failures, Instant.now().plus(lockDuration))
                    : new Attempt(failures, null);
        });
    }

    public void reset(String email) {
        attempts.remove(email);
    }

    public List<String> getCurrentlyLockedEmails() {
        Instant now = Instant.now();
        return attempts.entrySet().stream()
                .filter(e -> e.getValue().lockedUntil() != null && now.isBefore(e.getValue().lockedUntil()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public int getRecentFailedAttemptsCount() {
        return attempts.values().stream().mapToInt(Attempt::failures).sum();
    }
}
