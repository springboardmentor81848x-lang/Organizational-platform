package com.skillsphere.service;

import com.skillsphere.dto.DepartmentDto;
import com.skillsphere.dto.TeamDto;
import com.skillsphere.dto.TeamMemberDto;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeProfileRepository profileRepository;

    @InjectMocks
    private OrganizationService organizationService;

    private User adminUser;
    private User deptHeadUser;
    private User managerUser;
    private User employeeUser;
    private Department engineeringDept;
    private Team backendTeam;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        setPrivateId(adminUser, 1L);
        adminUser.setFullName("Admin User");
        adminUser.setEmail("admin@skillsphere.com");
        adminUser.setRole(Role.ADMIN);

        deptHeadUser = new User();
        setPrivateId(deptHeadUser, 2L);
        deptHeadUser.setFullName("Dept Head User");
        deptHeadUser.setEmail("depthead@skillsphere.com");
        deptHeadUser.setRole(Role.DEPARTMENT_HEAD);

        managerUser = new User();
        setPrivateId(managerUser, 3L);
        managerUser.setFullName("Manager User");
        managerUser.setEmail("manager@skillsphere.com");
        managerUser.setRole(Role.MANAGER);

        employeeUser = new User();
        setPrivateId(employeeUser, 4L);
        employeeUser.setFullName("Employee User");
        employeeUser.setEmail("employee@skillsphere.com");
        employeeUser.setRole(Role.EMPLOYEE);

        engineeringDept = new Department("Engineering", "ENG", "Engineering department", deptHeadUser);
        setPrivateId(engineeringDept, 10L);

        backendTeam = new Team(engineeringDept, "Backend Core", managerUser);
        setPrivateId(backendTeam, 20L);
    }

    private void setPrivateId(Object obj, Long id) {
        try {
            var field = obj.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(obj, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- Department tests ---

    @Test
    @DisplayName("Get all departments returns mapped DTO list")
    void testGetAllDepartments_Success() {
        when(departmentRepository.findAll()).thenReturn(List.of(engineeringDept));
        List<DepartmentDto> result = organizationService.getAllDepartments();
        assertEquals(1, result.size());
        assertEquals("Engineering", result.get(0).name());
        assertEquals("ENG", result.get(0).code());
    }

    @Test
    @DisplayName("Get department by valid ID returns DepartmentDto")
    void testGetDepartmentById_Success() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineeringDept));
        DepartmentDto result = organizationService.getDepartmentById(10L);
        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Engineering", result.name());
    }

    @Test
    @DisplayName("Get department by invalid ID throws 404 NOT_FOUND")
    void testGetDepartmentById_NotFound_ThrowsNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () -> organizationService.getDepartmentById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Create department succeeds when name and code are unique")
    void testCreateDepartment_Success() {
        when(departmentRepository.findByName("Product")).thenReturn(Optional.empty());
        when(departmentRepository.findByCode("PROD")).thenReturn(Optional.empty());
        when(userRepository.findById(2L)).thenReturn(Optional.of(deptHeadUser));

        Department created = new Department("Product", "PROD", "Product team", deptHeadUser);
        setPrivateId(created, 11L);
        when(departmentRepository.save(any(Department.class))).thenReturn(created);

        DepartmentDto result = organizationService.createDepartment("Product", "PROD", "Product team", 2L);
        assertNotNull(result);
        assertEquals(11L, result.id());
        assertEquals("Product", result.name());
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("Create department with duplicate name throws 409 CONFLICT")
    void testCreateDepartment_DuplicateName_ThrowsConflict() {
        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(engineeringDept));
        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.createDepartment("Engineering", "NEWENG", "Desc", null));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Create department with duplicate code throws 409 CONFLICT")
    void testCreateDepartment_DuplicateCode_ThrowsConflict() {
        when(departmentRepository.findByName("New Engineering")).thenReturn(Optional.empty());
        when(departmentRepository.findByCode("ENG")).thenReturn(Optional.of(engineeringDept));
        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.createDepartment("New Engineering", "ENG", "Desc", null));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // --- Team creation tests ---

    @Test
    @DisplayName("Admin can create team in any department")
    void testCreateTeam_Admin_Success() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineeringDept));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of());
        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));

        Team newTeam = new Team(engineeringDept, "Frontend Team", managerUser);
        setPrivateId(newTeam, 25L);
        when(teamRepository.save(any(Team.class))).thenReturn(newTeam);
        when(membershipRepository.findByTeamId(25L)).thenReturn(List.of());

        TeamDto result = organizationService.createTeam(10L, "Frontend Team", 3L, "admin@skillsphere.com");
        assertNotNull(result);
        assertEquals(25L, result.id());
        assertEquals("Frontend Team", result.name());
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    @DisplayName("Department Head can create team in their own assigned department")
    void testCreateTeam_DepartmentHeadOwnDept_Success() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineeringDept));
        when(userRepository.findByEmail("depthead@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of());

        Team newTeam = new Team(engineeringDept, "DevOps Team", null);
        setPrivateId(newTeam, 26L);
        when(teamRepository.save(any(Team.class))).thenReturn(newTeam);
        when(membershipRepository.findByTeamId(26L)).thenReturn(List.of());

        TeamDto result = organizationService.createTeam(10L, "DevOps Team", null, "depthead@skillsphere.com");
        assertNotNull(result);
        assertEquals(26L, result.id());
    }

    @Test
    @DisplayName("Department Head cannot create team in another department (throws 403 FORBIDDEN)")
    void testCreateTeam_DepartmentHeadDifferentDept_ThrowsForbidden() {
        User otherHead = new User();
        setPrivateId(otherHead, 99L);
        Department hrDept = new Department("HR", "HR", "Human Resources", otherHead);
        setPrivateId(hrDept, 12L);

        when(departmentRepository.findById(12L)).thenReturn(Optional.of(hrDept));
        when(userRepository.findByEmail("depthead@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.createTeam(12L, "Recruiting", null, "depthead@skillsphere.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("department"));
    }

    @Test
    @DisplayName("Manager cannot create teams (throws 403 FORBIDDEN)")
    void testCreateTeam_ManagerCaller_ThrowsForbidden() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineeringDept));
        when(userRepository.findByEmail("manager@skillsphere.com")).thenReturn(Optional.of(managerUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.createTeam(10L, "Sub Team", null, "manager@skillsphere.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("Create team with duplicate name in same department throws 409 CONFLICT")
    void testCreateTeam_DuplicateNameInDepartment_ThrowsConflict() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineeringDept));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.createTeam(10L, "Backend Core", null, "admin@skillsphere.com"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("Create team with non-existent department throws 404 NOT_FOUND")
    void testCreateTeam_DepartmentNotFound_ThrowsNotFound() {
        when(departmentRepository.findById(999L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.createTeam(999L, "New Team", null, "admin@skillsphere.com"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Create team with non-existent manager user throws 404 NOT_FOUND")
    void testCreateTeam_ManagerNotFound_ThrowsNotFound() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(engineeringDept));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of());
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.createTeam(10L, "Unique Team", 999L, "admin@skillsphere.com"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // --- Team member addition tests ---

    @Test
    @DisplayName("Manager can add member to their own assigned team")
    void testAddTeamMember_ManagerOwnTeam_Success() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("manager@skillsphere.com")).thenReturn(Optional.of(managerUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(employeeUser));
        when(membershipRepository.existsByTeamIdAndUserId(20L, 4L)).thenReturn(false);

        TeamMembership tm = new TeamMembership(backendTeam, employeeUser, "DEVELOPER");
        setPrivateId(tm, 50L);
        when(membershipRepository.save(any(TeamMembership.class))).thenReturn(tm);
        when(profileRepository.findByUserId(4L)).thenReturn(Optional.empty());

        TeamMemberDto result = organizationService.addTeamMember(20L, 4L, "DEVELOPER", "manager@skillsphere.com");
        assertNotNull(result);
        assertEquals(4L, result.userId());
        assertEquals("Employee User", result.fullName());
        verify(membershipRepository).save(any(TeamMembership.class));
    }

    @Test
    @DisplayName("Manager cannot add member to another manager's team (throws 403 FORBIDDEN)")
    void testAddTeamMember_ManagerDifferentTeam_ThrowsForbidden() {
        User otherManager = new User();
        setPrivateId(otherManager, 55L);
        Team otherTeam = new Team(engineeringDept, "Other Team", otherManager);
        setPrivateId(otherTeam, 21L);

        when(teamRepository.findById(21L)).thenReturn(Optional.of(otherTeam));
        when(userRepository.findByEmail("manager@skillsphere.com")).thenReturn(Optional.of(managerUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.addTeamMember(21L, 4L, "MEMBER", "manager@skillsphere.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("own team"));
    }

    @Test
    @DisplayName("Department Head can add member to team within their department")
    void testAddTeamMember_DepartmentHeadOwnDeptTeam_Success() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("depthead@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(employeeUser));
        when(membershipRepository.existsByTeamIdAndUserId(20L, 4L)).thenReturn(false);

        TeamMembership tm = new TeamMembership(backendTeam, employeeUser, "MEMBER");
        when(membershipRepository.save(any(TeamMembership.class))).thenReturn(tm);

        TeamMemberDto result = organizationService.addTeamMember(20L, 4L, "MEMBER", "depthead@skillsphere.com");
        assertNotNull(result);
        verify(membershipRepository).save(any(TeamMembership.class));
    }

    @Test
    @DisplayName("Admin can add member to any team")
    void testAddTeamMember_Admin_Success() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(employeeUser));
        when(membershipRepository.existsByTeamIdAndUserId(20L, 4L)).thenReturn(false);

        TeamMembership tm = new TeamMembership(backendTeam, employeeUser, "MEMBER");
        when(membershipRepository.save(any(TeamMembership.class))).thenReturn(tm);

        TeamMemberDto result = organizationService.addTeamMember(20L, 4L, "MEMBER", "admin@skillsphere.com");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Employee cannot add team members (throws 403 FORBIDDEN)")
    void testAddTeamMember_EmployeeCaller_ThrowsForbidden() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("employee@skillsphere.com")).thenReturn(Optional.of(employeeUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.addTeamMember(20L, 4L, "MEMBER", "employee@skillsphere.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("HR Specialist cannot add team members (throws 403 FORBIDDEN)")
    void testAddTeamMember_HrSpecialistCaller_ThrowsForbidden() {
        User hrUser = new User();
        setPrivateId(hrUser, 66L);
        hrUser.setEmail("hr@skillsphere.com");
        hrUser.setRole(Role.HR_SPECIALIST);

        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("hr@skillsphere.com")).thenReturn(Optional.of(hrUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.addTeamMember(20L, 4L, "MEMBER", "hr@skillsphere.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("Add existing team member throws 409 CONFLICT (duplicate membership prevented)")
    void testAddTeamMember_DuplicateMembership_ThrowsConflict() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(employeeUser));
        when(membershipRepository.existsByTeamIdAndUserId(20L, 4L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.addTeamMember(20L, 4L, "MEMBER", "admin@skillsphere.com"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already a member"));
    }

    @Test
    @DisplayName("Add team member with non-existent team throws 404 NOT_FOUND")
    void testAddTeamMember_TeamNotFound_ThrowsNotFound() {
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());
        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.addTeamMember(999L, 4L, "MEMBER", "admin@skillsphere.com"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Add team member with non-existent user throws 404 NOT_FOUND")
    void testAddTeamMember_UserNotFound_ThrowsNotFound() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.addTeamMember(20L, 999L, "MEMBER", "admin@skillsphere.com"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // --- Team member removal tests ---

    @Test
    @DisplayName("Manager can remove member from their own assigned team")
    void testRemoveTeamMember_ManagerOwnTeam_Success() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("manager@skillsphere.com")).thenReturn(Optional.of(managerUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(employeeUser));

        TeamMembership tm = new TeamMembership(backendTeam, employeeUser);
        when(membershipRepository.findByTeamIdAndUserId(20L, 4L)).thenReturn(Optional.of(tm));

        organizationService.removeTeamMember(20L, 4L, "manager@skillsphere.com");
        verify(membershipRepository).delete(tm);
    }

    @Test
    @DisplayName("Manager cannot remove member from another manager's team (throws 403 FORBIDDEN)")
    void testRemoveTeamMember_ManagerDifferentTeam_ThrowsForbidden() {
        User otherManager = new User();
        setPrivateId(otherManager, 55L);
        Team otherTeam = new Team(engineeringDept, "Other Team", otherManager);
        setPrivateId(otherTeam, 21L);

        when(teamRepository.findById(21L)).thenReturn(Optional.of(otherTeam));
        when(userRepository.findByEmail("manager@skillsphere.com")).thenReturn(Optional.of(managerUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.removeTeamMember(21L, 4L, "manager@skillsphere.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(membershipRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Admin can remove member from any team")
    void testRemoveTeamMember_Admin_Success() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(employeeUser));

        TeamMembership tm = new TeamMembership(backendTeam, employeeUser);
        when(membershipRepository.findByTeamIdAndUserId(20L, 4L)).thenReturn(Optional.of(tm));

        organizationService.removeTeamMember(20L, 4L, "admin@skillsphere.com");
        verify(membershipRepository).delete(tm);
    }

    @Test
    @DisplayName("Employee cannot remove team members (throws 403 FORBIDDEN)")
    void testRemoveTeamMember_EmployeeCaller_ThrowsForbidden() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("employee@skillsphere.com")).thenReturn(Optional.of(employeeUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.removeTeamMember(20L, 4L, "employee@skillsphere.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("Remove non-member from team throws 404 NOT_FOUND")
    void testRemoveTeamMember_MembershipNotFound_ThrowsNotFound() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(employeeUser));
        when(membershipRepository.findByTeamIdAndUserId(20L, 4L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.removeTeamMember(20L, 4L, "admin@skillsphere.com"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertTrue(ex.getMessage().contains("not a member"));
    }

    // --- Team member read tests ---

    @Test
    @DisplayName("Team member can view members of their own team")
    void testGetTeamMembers_AuthorizedMember_Success() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("employee@skillsphere.com")).thenReturn(Optional.of(employeeUser));
        when(membershipRepository.existsByTeamIdAndUserId(20L, 4L)).thenReturn(true);

        TeamMembership tm = new TeamMembership(backendTeam, employeeUser);
        when(membershipRepository.findByTeamId(20L)).thenReturn(List.of(tm));
        when(profileRepository.findByUserId(4L)).thenReturn(Optional.empty());

        List<TeamMemberDto> result = organizationService.getTeamMembers(20L, "employee@skillsphere.com");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Employee User", result.get(0).fullName());
    }

    @Test
    @DisplayName("Unauthorized employee cannot view members of another team (throws 403 FORBIDDEN)")
    void testGetTeamMembers_UnauthorizedEmployee_ThrowsForbidden() {
        when(teamRepository.findById(20L)).thenReturn(Optional.of(backendTeam));
        when(userRepository.findByEmail("employee@skillsphere.com")).thenReturn(Optional.of(employeeUser));
        when(membershipRepository.existsByTeamIdAndUserId(20L, 4L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                organizationService.getTeamMembers(20L, "employee@skillsphere.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }
}
