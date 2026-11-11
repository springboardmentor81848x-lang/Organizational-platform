package com.skillsphere.controller;

import com.skillsphere.dto.EmployeeDashboardOverviewDto;
import com.skillsphere.service.EmployeeDashboardService;
import com.skillsphere.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Role-authorized workspace summary endpoints. Enforced on the server;
 * the React route guards improve user experience.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserService userService;
    private final EmployeeDashboardService employeeDashboardService;

    public DashboardController(UserService userService, EmployeeDashboardService employeeDashboardService) {
        this.userService = userService;
        this.employeeDashboardService = employeeDashboardService;
    }

    @GetMapping("/employee")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public Map<String, String> employee(Authentication auth) {
        return payload(auth, "Employee Workspace", "Measure skills, track learning paths, and connect with peer mentors.");
    }

    @GetMapping("/employee/overview")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<EmployeeDashboardOverviewDto> employeeOverview(Authentication auth) {
        return ResponseEntity.ok(employeeDashboardService.getOverview(auth.getName()));
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public Map<String, String> manager(Authentication auth) {
        return payload(auth, "Manager Workspace", "Monitor team capability, identify skill gaps, and support professional growth.");
    }

    @GetMapping("/mentor")
    @PreAuthorize("hasAnyRole('MENTOR','ADMIN')")
    public Map<String, String> mentor(Authentication auth) {
        return payload(auth, "Mentor Workspace", "Guide assigned mentees, conduct 1-on-1 sessions, and track development goals.");
    }

    @GetMapping("/hr")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST','ADMIN')")
    public Map<String, String> hr(Authentication auth) {
        return payload(auth, "HR Specialist Workspace", "Govern competency frameworks, organizational skill matrices, and learning initiatives.");
    }

    @GetMapping("/department-head")
    @PreAuthorize("hasAnyRole('DEPARTMENT_HEAD','ADMIN')")
    public Map<String, String> departmentHead(Authentication auth) {
        return payload(auth, "Department Head Workspace", "Analyze department skill coverage, workforce readiness, and strategic capability risks.");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> admin(Authentication auth) {
        return payload(auth, "Admin Console", "Manage users, configure permissions, and monitor system security.");
    }

    private Map<String, String> payload(Authentication auth, String title, String message) {
        var user = userService.getByEmail(auth.getName());
        return Map.of("title", title, "message", message,
                "fullName", user.getFullName(), "role", user.getRole().name());
    }
}
