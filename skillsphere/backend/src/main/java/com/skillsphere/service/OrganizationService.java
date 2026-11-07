package com.skillsphere.service;

import com.skillsphere.dto.DepartmentDto;
import com.skillsphere.dto.TeamDto;
import com.skillsphere.dto.TeamMemberDto;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrganizationService {

    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository profileRepository;

    public OrganizationService(DepartmentRepository departmentRepository,
                               TeamRepository teamRepository,
                               TeamMembershipRepository membershipRepository,
                               UserRepository userRepository,
                               EmployeeProfileRepository profileRepository) {
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream().map(this::toDeptDto).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentDto getDepartmentById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found."));
        return toDeptDto(dept);
    }

    @Transactional
    public DepartmentDto createDepartment(String name, String code, String description, Long headUserId) {
        if (name == null || name.trim().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Department name is required.");
        }
        if (code == null || code.trim().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Department code is required.");
        }
        if (departmentRepository.findByName(name.trim()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Department name already exists.");
        }
        if (departmentRepository.findByCode(code.trim().toUpperCase()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Department code already exists.");
        }
        User headUser = headUserId != null ? userRepository.findById(headUserId).orElse(null) : null;
        Department d = new Department(name.trim(), code.trim().toUpperCase(), description, headUser);
        return toDeptDto(departmentRepository.save(d));
    }

    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream().map(this::toTeamDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamDto> getTeamsByDepartmentId(Long deptId) {
        if (!departmentRepository.existsById(deptId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Department not found.");
        }
        return teamRepository.findByDepartmentId(deptId).stream().map(this::toTeamDto).toList();
    }

    @Transactional(readOnly = true)
    public TeamDto getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."));
        return toTeamDto(team);
    }

    @Transactional(readOnly = true)
    public List<TeamDto> getMyTeams(String email) {
        User user = getUserByEmail(email);
        List<Team> teams = new ArrayList<>();
        if (user.getRole() == Role.MANAGER) {
            teams.addAll(teamRepository.findByManagerId(user.getId()));
        } else if (user.getRole() == Role.DEPARTMENT_HEAD) {
            departmentRepository.findByHeadUserId(user.getId())
                    .ifPresent(d -> teams.addAll(teamRepository.findByDepartmentId(d.getId())));
        }
        // Also add any team user is a member of
        List<TeamMembership> memberships = membershipRepository.findByUserId(user.getId());
        for (TeamMembership tm : memberships) {
            if (!teams.contains(tm.getTeam())) {
                teams.add(tm.getTeam());
            }
        }
        return teams.stream().map(this::toTeamDto).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamMemberDto> getTeamMembers(Long teamId, String callerEmail) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."));

        User caller = getUserByEmail(callerEmail);
        boolean isAuthorized = caller.getRole() == Role.ADMIN
                || caller.getRole() == Role.HR_SPECIALIST
                || (caller.getRole() == Role.MANAGER && team.getManager() != null && team.getManager().getId().equals(caller.getId()))
                || (caller.getRole() == Role.DEPARTMENT_HEAD && team.getDepartment().getHeadUser() != null && team.getDepartment().getHeadUser().getId().equals(caller.getId()))
                || membershipRepository.existsByTeamIdAndUserId(teamId, caller.getId());

        if (!isAuthorized) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this team's members.");
        }

        return membershipRepository.findByTeamId(teamId).stream()
                .map(tm -> toTeamMemberDto(tm, team))
                .toList();
    }

    @Transactional
    public TeamDto createTeam(Long departmentId, String name, Long managerUserId, String callerEmail) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Department not found."));

        if (callerEmail != null) {
            User caller = getUserByEmail(callerEmail);
            assertCanCreateTeam(dept, caller);
        }

        if (name == null || name.trim().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Team name is required.");
        }

        boolean nameExists = teamRepository.findByDepartmentId(departmentId).stream()
                .anyMatch(t -> t.getName().equalsIgnoreCase(name.trim()));
        if (nameExists) {
            throw new ApiException(HttpStatus.CONFLICT, "A team with this name already exists in this department.");
        }

        User manager = null;
        if (managerUserId != null) {
            manager = userRepository.findById(managerUserId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Manager user not found."));
        }

        Team team = new Team(dept, name.trim(), manager);
        return toTeamDto(teamRepository.save(team));
    }

    @Transactional
    public TeamDto createTeam(Long departmentId, String name, Long managerUserId) {
        return createTeam(departmentId, name, managerUserId, null);
    }

    @Transactional
    public TeamMemberDto addTeamMember(Long teamId, Long userId, String roleInTeam, String callerEmail) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."));

        if (callerEmail != null) {
            User caller = getUserByEmail(callerEmail);
            assertCanManageTeamMembers(team, caller);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        if (membershipRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "User is already a member of this team.");
        }

        String assignedRole = (roleInTeam != null && !roleInTeam.trim().isBlank()) ? roleInTeam.trim() : "MEMBER";
        TeamMembership tm = new TeamMembership(team, user, assignedRole);
        TeamMembership saved = membershipRepository.save(tm);
        return toTeamMemberDto(saved, team);
    }

    @Transactional
    public void addTeamMember(Long teamId, Long userId) {
        addTeamMember(teamId, userId, "MEMBER", null);
    }

    @Transactional
    public void removeTeamMember(Long teamId, Long userId, String callerEmail) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team not found."));

        if (callerEmail != null) {
            User caller = getUserByEmail(callerEmail);
            assertCanManageTeamMembers(team, caller);
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));

        TeamMembership tm = membershipRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User is not a member of this team."));

        membershipRepository.delete(tm);
    }

    private void assertCanCreateTeam(Department dept, User caller) {
        if (caller.getRole() == Role.ADMIN) {
            return;
        }
        if (caller.getRole() == Role.DEPARTMENT_HEAD) {
            if (dept.getHeadUser() != null && dept.getHeadUser().getId().equals(caller.getId())) {
                return;
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only create teams in your department.");
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You do not have permission to create teams.");
    }

    private void assertCanManageTeamMembers(Team team, User caller) {
        if (caller.getRole() == Role.ADMIN) {
            return;
        }
        if (caller.getRole() == Role.DEPARTMENT_HEAD) {
            Department dept = team.getDepartment();
            if (dept.getHeadUser() != null && dept.getHeadUser().getId().equals(caller.getId())) {
                return;
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only manage teams in your department.");
        }
        if (caller.getRole() == Role.MANAGER) {
            if (team.getManager() != null && team.getManager().getId().equals(caller.getId())) {
                return;
            }
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You can only manage members of your own team.");
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Access denied. You do not have permission to manage team members.");
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private DepartmentDto toDeptDto(Department d) {
        return new DepartmentDto(
                d.getId(),
                d.getName(),
                d.getCode(),
                d.getDescription(),
                d.getHeadUser() != null ? d.getHeadUser().getId() : null,
                d.getHeadUser() != null ? d.getHeadUser().getFullName() : null
        );
    }

    private TeamMemberDto toTeamMemberDto(TeamMembership tm, Team team) {
        User u = tm.getUser();
        EmployeeProfile p = profileRepository.findByUserId(u.getId()).orElse(null);
        return new TeamMemberDto(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getEmployeeId(),
                u.getRole().name(),
                p != null && p.getJobRole() != null ? p.getJobRole() : "Software Developer",
                p != null && p.getDepartment() != null ? p.getDepartment() : team.getDepartment().getName(),
                tm.getJoinedAt()
        );
    }

    private TeamDto toTeamDto(Team t) {
        List<TeamMembership> members = membershipRepository.findByTeamId(t.getId());
        List<TeamMemberDto> memberDtos = members.stream()
                .map(tm -> toTeamMemberDto(tm, t))
                .toList();

        return new TeamDto(
                t.getId(),
                t.getDepartment().getId(),
                t.getDepartment().getName(),
                t.getName(),
                t.getManager() != null ? t.getManager().getId() : null,
                t.getManager() != null ? t.getManager().getFullName() : null,
                members.size(),
                memberDtos
        );
    }
}
