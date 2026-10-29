package com.wki.auth.employee;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Finds out which employee is calling.
 *
 * JwtAuthFilter has already verified the token's signature and expiry and stored the email
 * (the JWT subject) as the authenticated user, so this class only turns that email into an
 * employee_id. The id is never taken from the URL or request body, which means an employee can
 * only ever read or change their own data.
 */
@Component
public class CurrentEmployee {

    private final JdbcTemplate jdbc;

    public CurrentEmployee(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in again");

        List<Long> ids = jdbc.queryForList(
                "SELECT employee_id FROM employees WHERE LOWER(email) = LOWER(?) AND status = 'ACTIVE'",
                Long.class, auth.getName());
        if (ids.isEmpty())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found or not active");
        return ids.get(0);
    }
}
