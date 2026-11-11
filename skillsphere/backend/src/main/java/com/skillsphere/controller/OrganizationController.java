package com.skillsphere.controller;

import com.skillsphere.dto.AddTeamMemberRequest;
import com.skillsphere.dto.CreateDepartmentRequest;
import com.skillsphere.dto.CreateTeamRequest;
import com.skillsphere.dto.DepartmentDto;
import com.skillsphere.dto.TeamDto;
import com.skillsphere.dto.TeamMemberDto;
import com.skillsphere.exception.ApiException;
import com.skillsphere.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/departments")
    public List<DepartmentDto> getDepartments() {
        return organizationService.getAllDepartments();
    }

    @GetMapping("/departments/{id}")
    public DepartmentDto getDepartment(@PathVariable Long id) {
        return organizationService.getDepartmentById(id);
    }

    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentDto createDepartment(@Valid @RequestBody CreateDepartmentRequest req) {
        return organizationService.createDepartment(req.name(), req.code(), req.description(), req.headUserId());
    }

    @GetMapping("/departments/{deptId}/teams")
    public List<TeamDto> getDepartmentTeams(@PathVariable Long deptId) {
        return organizationService.getTeamsByDepartmentId(deptId);
    }

    @PostMapping("/departments/{deptId}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_HEAD')")
    public TeamDto createDepartmentTeam(
            @PathVariable Long deptId,
            @Valid @RequestBody CreateTeamRequest req,
            Authentication auth
    ) {
        return organizationService.createTeam(deptId, req.name(), req.effectiveManagerUserId(), auth.getName());
    }

    @GetMapping("/teams")
    public List<TeamDto> getAllTeams() {
        return organizationService.getAllTeams();
    }

    @GetMapping("/teams/my")
    public List<TeamDto> getMyTeams(Authentication auth) {
        return organizationService.getMyTeams(auth.getName());
    }

    @GetMapping("/teams/{id}")
    public TeamDto getTeam(@PathVariable Long id) {
        return organizationService.getTeamById(id);
    }

    @PostMapping("/teams")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DEPARTMENT_HEAD')")
    public TeamDto createTeam(@Valid @RequestBody CreateTeamRequest req, Authentication auth) {
        Long deptId = req.departmentId();
        if (deptId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Department ID is required.");
        }
        return organizationService.createTeam(deptId, req.name(), req.effectiveManagerUserId(), auth.getName());
    }

    @GetMapping("/teams/{id}/members")
    public List<TeamMemberDto> getTeamMembers(@PathVariable Long id, Authentication auth) {
        return organizationService.getTeamMembers(id, auth.getName());
    }

    @PostMapping("/teams/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DEPARTMENT_HEAD')")
    public TeamMemberDto addTeamMember(
            @PathVariable Long id,
            @Valid @RequestBody AddTeamMemberRequest req,
            Authentication auth
    ) {
        return organizationService.addTeamMember(id, req.userId(), req.roleInTeam(), auth.getName());
    }

    @DeleteMapping("/teams/{teamId}/members/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','DEPARTMENT_HEAD')")
    public Map<String, Object> removeTeamMember(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            Authentication auth
    ) {
        organizationService.removeTeamMember(teamId, userId, auth.getName());
        return Map.of("success", true, "message", "Team member removed successfully.");
    }
}
