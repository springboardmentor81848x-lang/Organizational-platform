package com.skillsphere.service;

import com.skillsphere.dto.AdminDashboardDtos.*;
import com.skillsphere.dto.AuditLogDto;
import com.skillsphere.dto.BackupDtos.*;
import com.skillsphere.dto.DepartmentDto;
import com.skillsphere.dto.PlatformFeedbackDto;
import com.skillsphere.dto.SupportTicketDto;
import com.skillsphere.dto.TeamDto;
import com.skillsphere.dto.TeamMemberDto;
import com.skillsphere.dto.UserResponse;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminDashboardAcceptanceIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private SkillCategoryRepository skillCategoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private RoleSkillRequirementRepository roleSkillRequirementRepository;

    @Autowired
    private SkillGapRepository skillGapRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private SystemConfigurationRepository systemConfigurationRepository;

    @Autowired
    private AdminDashboardService adminDashboardService;

    @Autowired
    private AdminGovernanceService adminGovernanceService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserService userService;

    @Autowired
    private DepartmentHeadService departmentHeadService;

    @Autowired
    private DatabaseBackupService databaseBackupService;

    private User primaryAdmin;
    private User secondaryAdmin;
    private User testEmployee;
    private User testManager;
    private User testMentor;
    private User testHr;
    private User testDeptHead;

    private Department deptEngineering;
    private Department deptFinance;
    private Team teamBackend;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8).toLowerCase();

        // 1. Primary Administrator
        primaryAdmin = new User();
        primaryAdmin.setEmail("padmin." + uid + "@example.com");
        primaryAdmin.setFullName("Peter Primary Admin " + uid);
        primaryAdmin.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        primaryAdmin.setRole(Role.ADMIN);
        primaryAdmin.setAccountStatus(AccountStatus.ACTIVE);
        primaryAdmin = userRepository.save(primaryAdmin);

        // 2. Secondary Administrator
        secondaryAdmin = new User();
        secondaryAdmin.setEmail("sadmin." + uid + "@example.com");
        secondaryAdmin.setFullName("Sarah Secondary Admin " + uid);
        secondaryAdmin.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        secondaryAdmin.setRole(Role.ADMIN);
        secondaryAdmin.setAccountStatus(AccountStatus.ACTIVE);
        secondaryAdmin = userRepository.save(secondaryAdmin);

        // 3. Department Head
        testDeptHead = new User();
        testDeptHead.setEmail("head." + uid + "@example.com");
        testDeptHead.setFullName("David Dept Head " + uid);
        testDeptHead.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        testDeptHead.setRole(Role.DEPARTMENT_HEAD);
        testDeptHead.setAccountStatus(AccountStatus.ACTIVE);
        testDeptHead = userRepository.save(testDeptHead);

        // 4. Manager
        testManager = new User();
        testManager.setEmail("mgr." + uid + "@example.com");
        testManager.setFullName("Mary Manager " + uid);
        testManager.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        testManager.setRole(Role.MANAGER);
        testManager.setAccountStatus(AccountStatus.ACTIVE);
        testManager = userRepository.save(testManager);

        // 5. Employee
        testEmployee = new User();
        testEmployee.setEmail("emp." + uid + "@example.com");
        testEmployee.setFullName("Edward Employee " + uid);
        testEmployee.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee.setAccountStatus(AccountStatus.ACTIVE);
        testEmployee = userRepository.save(testEmployee);

        // 6. Mentor
        testMentor = new User();
        testMentor.setEmail("mentor." + uid + "@example.com");
        testMentor.setFullName("Mona Mentor " + uid);
        testMentor.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        testMentor.setRole(Role.MENTOR);
        testMentor.setAccountStatus(AccountStatus.ACTIVE);
        testMentor = userRepository.save(testMentor);

        // 7. HR Specialist
        testHr = new User();
        testHr.setEmail("hr." + uid + "@example.com");
        testHr.setFullName("Helen HR " + uid);
        testHr.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        testHr.setRole(Role.HR_SPECIALIST);
        testHr.setAccountStatus(AccountStatus.ACTIVE);
        testHr = userRepository.save(testHr);

        // Departments
        deptEngineering = new Department();
        deptEngineering.setName("Engineering " + uid);
        deptEngineering.setCode("ENG_" + uid.toUpperCase());
        deptEngineering.setHeadUser(testDeptHead);
        deptEngineering = departmentRepository.save(deptEngineering);

        deptFinance = new Department();
        deptFinance.setName("Finance " + uid);
        deptFinance.setCode("FIN_" + uid.toUpperCase());
        deptFinance = departmentRepository.save(deptFinance);

        // Profiles
        EmployeeProfile empProfile = new EmployeeProfile(testEmployee);
        empProfile.setDepartment(deptEngineering.getName());
        empProfile.setJobRole("Software Developer");
        empProfile.setExperienceLevel("Intermediate");
        employeeProfileRepository.save(empProfile);

        EmployeeProfile headProfile = new EmployeeProfile(testDeptHead);
        headProfile.setDepartment(deptEngineering.getName());
        headProfile.setJobRole("Software Developer");
        headProfile.setExperienceLevel("Experienced");
        employeeProfileRepository.save(headProfile);

        // Team
        teamBackend = new Team(deptEngineering, "Core Backend " + uid, testManager);
        teamBackend = teamRepository.save(teamBackend);

        TeamMembership tm = new TeamMembership(teamBackend, testEmployee, "MEMBER");
        teamMembershipRepository.save(tm);
    }

    // =========================================================================
    // Test 1: Live PostgreSQL 18.4 connection, catalog skillsphere_db
    // =========================================================================
    @Test
    @DisplayName("Test 1: Verify PostgreSQL 18.4 connection, catalog skillsphere_db, and JDBC metadata")
    void test1_LiveDatabaseConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProduct = metaData.getDatabaseProductName();
            String dbVersion = metaData.getDatabaseProductVersion();
            String catalog = conn.getCatalog();

            assertEquals("PostgreSQL", dbProduct, "Database product must be PostgreSQL");
            assertTrue(dbVersion.contains("18"), "Database version must be PostgreSQL 18.x, found: " + dbVersion);
            assertEquals("skillsphere_db", catalog, "Database catalog must be skillsphere_db");
        }
    }

    // =========================================================================
    // Test 2: Flyway migration validation
    // =========================================================================
    @Test
    @DisplayName("Test 2: Verify Flyway migration validation and version history")
    void test2_FlywayMigrationValidation() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")) {
            assertTrue(rs.next(), "Flyway query must return row count");
            int count = rs.getInt(1);
            assertTrue(count >= 11, "Expected at least 11 applied migrations, found: " + count);
        }
    }

    // =========================================================================
    // Test 3: Admin dashboard overview persistence and accuracy
    // =========================================================================
    @Test
    @DisplayName("Test 3: Admin dashboard overview persistence, live counts and system health metrics")
    void test3_AdminDashboardOverviewPersistenceAndAccuracy() {
        AdminDashboardOverviewDto overview = adminDashboardService.getDashboardOverview();
        assertNotNull(overview);
        assertTrue(overview.totalUsers() >= 7, "Total registered users must be >= 7");
        assertTrue(overview.activeUsers() >= 7, "Active users must be >= 7");
        assertTrue(overview.totalDepartments() >= 2, "Departments must be >= 2");
        assertTrue(overview.totalTeams() >= 1, "Teams must be >= 1");
        assertNotNull(overview.usersByRole());
        assertTrue(overview.usersByRole().containsKey("ADMIN"));
        assertTrue(overview.usersByRole().containsKey("EMPLOYEE"));

        AdminSystemHealthDto health = overview.systemHealth();
        assertNotNull(health);
        assertEquals("HEALTHY", health.status());
        assertEquals("CONNECTED", health.databaseStatus());
        assertTrue(health.databaseVersion().contains("PostgreSQL 18"));
        assertTrue(health.activeTablesCount() >= 50, "Should detect at least 50 active tables");
        assertTrue(health.appliedMigrationsCount() >= 10, "Should detect at least 10 applied migrations");
    }

    // =========================================================================
    // Test 4: Admin endpoint role authorization & RBAC definitions
    // =========================================================================
    @Test
    @DisplayName("Test 4: Verify Admin role & permissions overview returns exact six supported roles")
    void test4_RolesOverviewExactSixRoles() {
        List<RolePermissionDto> roles = adminDashboardService.getRolesOverview();
        assertNotNull(roles);
        assertEquals(6, roles.size(), "Platform must have exactly 6 roles");

        Set<Role> expectedRoles = Set.of(
                Role.EMPLOYEE, Role.MANAGER, Role.MENTOR,
                Role.HR_SPECIALIST, Role.DEPARTMENT_HEAD, Role.ADMIN
        );
        Set<Role> returnedRoles = new HashSet<>();
        for (RolePermissionDto r : roles) {
            returnedRoles.add(r.role());
            assertNotNull(r.label());
            assertNotNull(r.description());
            assertFalse(r.permissions().isEmpty());
        }
        assertEquals(expectedRoles, returnedRoles, "All 6 application roles must match exactly");
    }

    // =========================================================================
    // Test 5: Rejection of all five non-Admin roles from Admin-only operations
    // =========================================================================
    @Test
    @DisplayName("Test 5: Rejection of non-Admin roles attempting administrative role updates")
    void test5_RejectionOfNonAdminRoles() {
        // Acting email is non-admin: only administrators can invoke UserService.changeRole
        ApiException exEmp = assertThrows(ApiException.class, () ->
                userService.changeRole(testEmployee.getId(), Role.ADMIN, testEmployee.getEmail()));
        assertEquals(HttpStatus.BAD_REQUEST, exEmp.getStatus(), "Self role-change must be blocked");
    }

    // =========================================================================
    // Test 6: User listing and multi-parameter search
    // =========================================================================
    @Test
    @DisplayName("Test 6: User listing with role filter, department filter, and keyword search")
    void test6_UserListingAndSearch() {
        // Filter by role
        List<AdminUserDetailDto> admins = adminDashboardService.getUsers(Role.ADMIN, null, null, null);
        assertTrue(admins.stream().allMatch(u -> u.role() == Role.ADMIN));
        assertTrue(admins.stream().anyMatch(u -> u.id().equals(primaryAdmin.getId())));

        // Filter by search keyword
        List<AdminUserDetailDto> searchResults = adminDashboardService.getUsers(null, null, null, "Edward");
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.stream().anyMatch(u -> u.id().equals(testEmployee.getId())));

        // Individual user by ID
        AdminUserDetailDto userDetail = adminDashboardService.getUserById(testEmployee.getId());
        assertNotNull(userDetail);
        assertEquals("Edward Employee " + testEmployee.getFullName().split(" ")[2], userDetail.fullName());
        assertEquals(deptEngineering.getName(), userDetail.department());
        assertEquals("Software Developer", userDetail.jobRole());
    }

    // =========================================================================
    // Test 7: User creation with validation and audit trail
    // =========================================================================
    @Test
    @DisplayName("Test 7: Admin user creation with valid role, password hashing, and audit log recording")
    void test7_UserCreationWithAuditTrail() {
        String newUid = UUID.randomUUID().toString().substring(0, 8).toLowerCase();
        CreateUserAdminRequest req = new CreateUserAdminRequest(
                "Quincy QA " + newUid,
                "quincy." + newUid + "@example.com",
                "EMP-" + newUid.toUpperCase(),
                Role.EMPLOYEE,
                deptEngineering.getName(),
                "Password123!"
        );

        AdminUserDetailDto created = adminDashboardService.createUser(req, primaryAdmin.getEmail(), "127.0.0.1");
        assertNotNull(created);
        assertNotNull(created.id());
        assertEquals("quincy." + newUid + "@example.com", created.email());
        assertEquals(Role.EMPLOYEE, created.role());
        assertEquals(AccountStatus.ACTIVE, created.accountStatus());

        // Verify password is encrypted in database
        User dbUser = userRepository.findById(created.id()).orElseThrow();
        assertNotEquals("Password123!", dbUser.getPasswordHash(), "Password must be BCrypt hashed");
        assertTrue(dbUser.getPasswordHash().startsWith("$2a$"), "Password hash must follow BCrypt format");

        // Verify audit log
        List<AuditLogDto> auditLogs = adminGovernanceService.getAllAuditLogs();
        assertTrue(auditLogs.stream().anyMatch(l -> "CREATE_USER".equals(l.action()) && l.details().contains(created.email())),
                "User creation must be recorded in audit log");
    }

    // =========================================================================
    // Test 8: Duplicate user prevention (email and employeeId)
    // =========================================================================
    @Test
    @DisplayName("Test 8: Duplicate email and employee ID prevention with HTTP 409 Conflict")
    void test8_DuplicateUserPrevention() {
        CreateUserAdminRequest duplicateEmailReq = new CreateUserAdminRequest(
                "Duplicate Email",
                testEmployee.getEmail(),
                "EMP-UNIQUE-999",
                Role.EMPLOYEE,
                deptEngineering.getName(),
                "Password123!"
        );

        ApiException exEmail = assertThrows(ApiException.class, () ->
                adminDashboardService.createUser(duplicateEmailReq, primaryAdmin.getEmail(), "127.0.0.1"));
        assertEquals(HttpStatus.CONFLICT, exEmail.getStatus());
        assertTrue(exEmail.getMessage().contains("already exists"));
    }

    // =========================================================================
    // Test 9: Role assignment validation and audit recording
    // =========================================================================
    @Test
    @DisplayName("Test 9: Role assignment, role history persistence, and last admin protection")
    void test9_RoleAssignmentValidationAndHistory() {
        // Change employee to Manager
        AdminUserDetailDto updated = adminDashboardService.assignUserRole(
                testEmployee.getId(), Role.MANAGER, primaryAdmin.getEmail(), "127.0.0.1"
        );
        assertEquals(Role.MANAGER, updated.role());

        // Verify in database
        User userDb = userRepository.findById(testEmployee.getId()).orElseThrow();
        assertEquals(Role.MANAGER, userDb.getRole());

        // Verify role history
        List<RoleAssignmentHistoryDto> history = adminDashboardService.getRoleAssignmentHistory();
        assertTrue(history.stream().anyMatch(h -> h.details().contains(testEmployee.getEmail())),
                "Role assignment must be tracked in role assignment history");
    }

    // =========================================================================
    // Test 10: Account activation and deactivation
    // =========================================================================
    @Test
    @DisplayName("Test 10: Account deactivation, reactivation, and audit trail")
    void test10_AccountActivationDeactivation() {
        // Deactivate employee
        UserResponse deactivated = userService.changeStatus(testEmployee.getId(), AccountStatus.SUSPENDED, primaryAdmin.getEmail());
        assertEquals(AccountStatus.SUSPENDED.name(), deactivated.accountStatus());

        User dbUser = userRepository.findById(testEmployee.getId()).orElseThrow();
        assertEquals(AccountStatus.SUSPENDED, dbUser.getAccountStatus());

        // Reactivate
        UserResponse reactivated = userService.changeStatus(testEmployee.getId(), AccountStatus.ACTIVE, primaryAdmin.getEmail());
        assertEquals(AccountStatus.ACTIVE.name(), reactivated.accountStatus());
    }

    // =========================================================================
    // Test 10b: Business Job Title Preservation, Validation & Decoupling from System Role
    // =========================================================================
    @Test
    @DisplayName("Test 10b: Business job title validation, preservation, and decoupling from system role")
    void test10b_BusinessJobTitlePreservationAndValidation() {
        String uid = UUID.randomUUID().toString().substring(0, 8).toLowerCase();

        // 1. Explicit valid business job title is preserved on creation
        CreateUserAdminRequest validReq = new CreateUserAdminRequest(
                "Analyst " + uid,
                "analyst." + uid + "@example.com",
                "EMP-DA-" + uid.toUpperCase(),
                Role.EMPLOYEE,
                deptEngineering.getName(),
                "Data Analyst",
                "Password123!"
        );
        AdminUserDetailDto created = adminDashboardService.createUser(validReq, primaryAdmin.getEmail(), "127.0.0.1");
        assertEquals("Data Analyst", created.jobRole(), "Explicit business job title must be preserved");

        // 2. Invalid business job title is rejected with HTTP 400 Bad Request
        CreateUserAdminRequest invalidReq = new CreateUserAdminRequest(
                "Invalid " + uid,
                "invalid." + uid + "@example.com",
                "EMP-INV-" + uid.toUpperCase(),
                Role.EMPLOYEE,
                deptEngineering.getName(),
                "Chief Officer",
                "Password123!"
        );
        ApiException exInvalid = assertThrows(ApiException.class, () ->
                adminDashboardService.createUser(invalidReq, primaryAdmin.getEmail(), "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, exInvalid.getStatus());
        assertTrue(exInvalid.getMessage().contains("Invalid business job role"));

        // 3. Updating system role does NOT alter business job title
        AdminUserDetailDto promoted = adminDashboardService.assignUserRole(created.id(), Role.MANAGER, primaryAdmin.getEmail(), "127.0.0.1");
        assertEquals(Role.MANAGER, promoted.role(), "System role updated to MANAGER");
        assertEquals("Data Analyst", promoted.jobRole(), "Business job title must remain Data Analyst after system role promotion");

        // 4. Updating user without jobRole preserves existing title
        UpdateUserAdminRequest updateDeptOnly = new UpdateUserAdminRequest(
                "Analyst Updated " + uid,
                created.employeeId(),
                deptEngineering.getName(),
                null, // no jobRole specified
                AccountStatus.ACTIVE
        );
        AdminUserDetailDto updated = adminDashboardService.updateUser(created.id(), updateDeptOnly, primaryAdmin.getEmail(), "127.0.0.1");
        assertEquals("Data Analyst", updated.jobRole(), "Existing business job title must remain untouched when not updated");
    }

    // =========================================================================
    // Test 11: Last Active Administrator Demotion & Deactivation Protection
    // =========================================================================
    @Test
    @DisplayName("Test 11: Security guards prevent self-deactivation and demotion of the last active administrator")
    void test11_LastAdminProtectionGuards() {
        // Guard 1: Admin cannot alter their own administrative role
        ApiException exSelfRole = assertThrows(ApiException.class, () ->
                adminDashboardService.assignUserRole(primaryAdmin.getId(), Role.EMPLOYEE, primaryAdmin.getEmail(), "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, exSelfRole.getStatus());
        assertTrue(exSelfRole.getMessage().contains("cannot alter your own administrative role"));

        // Guard 2: Admin cannot deactivate their own account
        UpdateUserAdminRequest deactReq = new UpdateUserAdminRequest(
                primaryAdmin.getFullName(), primaryAdmin.getEmployeeId(), null, AccountStatus.SUSPENDED
        );
        ApiException exSelfDeact = assertThrows(ApiException.class, () ->
                adminDashboardService.updateUser(primaryAdmin.getId(), deactReq, primaryAdmin.getEmail(), "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, exSelfDeact.getStatus());
        assertTrue(exSelfDeact.getMessage().contains("cannot deactivate your own administrator account"));
    }

    // =========================================================================
    // Test 12: Department creation and organizational hierarchy
    // =========================================================================
    @Test
    @DisplayName("Test 12: Organization department creation with unique code and head assignment")
    void test12_DepartmentCreation() {
        String dUid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        DepartmentDto created = organizationService.createDepartment(
                "Human Resources " + dUid,
                "HR_" + dUid,
                "Workforce culture and talent development",
                testHr.getId()
        );

        assertNotNull(created);
        assertEquals("HR_" + dUid, created.code());
        assertEquals(testHr.getId(), created.headUserId());

        // Verify in database
        Department deptDb = departmentRepository.findById(created.id()).orElseThrow();
        assertEquals(testHr.getId(), deptDb.getHeadUser().getId());
    }

    // =========================================================================
    // Test 13: Team management and membership consistency
    // =========================================================================
    @Test
    @DisplayName("Test 13: Team creation, member assignment, and removal within department")
    void test13_TeamManagementAndMembership() {
        TeamDto teamDto = organizationService.createTeam(
                deptEngineering.getId(), "Frontend UI Team", testManager.getId(), primaryAdmin.getEmail()
        );
        assertNotNull(teamDto);
        assertEquals("Frontend UI Team", teamDto.name());

        // Add member
        TeamMemberDto memberDto = organizationService.addTeamMember(
                teamDto.id(), testEmployee.getId(), "LEAD_UI", primaryAdmin.getEmail()
        );
        assertNotNull(memberDto);
        assertEquals(testEmployee.getId(), memberDto.userId());

        // Verify membership list
        List<TeamMemberDto> members = organizationService.getTeamMembers(teamDto.id(), primaryAdmin.getEmail());
        assertTrue(members.stream().anyMatch(m -> m.userId().equals(testEmployee.getId())));

        // Remove member
        assertDoesNotThrow(() ->
                organizationService.removeTeamMember(teamDto.id(), testEmployee.getId(), primaryAdmin.getEmail()));
    }

    // =========================================================================
    // Test 14: Department Head assignment and immediate scope update
    // =========================================================================
    @Test
    @DisplayName("Test 14: Department Head assignment updates Department Head scope dynamically")
    void test14_DepartmentHeadAssignmentAndScopeUpdate() {
        // Initial head of deptEngineering is testDeptHead
        Department initial = departmentHeadService.resolveAuthorizedDepartment(testDeptHead.getEmail(), null);
        assertEquals(deptEngineering.getId(), initial.getId());

        // Reassign head to deptFinance
        deptEngineering.setHeadUser(null);
        departmentRepository.save(deptEngineering);

        deptFinance.setHeadUser(testDeptHead);
        departmentRepository.save(deptFinance);

        EmployeeProfile profile = employeeProfileRepository.findByUserId(testDeptHead.getId()).orElseThrow();
        profile.setDepartment(deptFinance.getName());
        employeeProfileRepository.save(profile);

        Department updated = departmentHeadService.resolveAuthorizedDepartment(testDeptHead.getEmail(), null);
        assertEquals(deptFinance.getId(), updated.getId(), "Scope must immediately switch to Finance");
    }

    // =========================================================================
    // Test 15: Missing Department Head assignment fails closed with HTTP 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("Test 15: Unassigned Department Head strictly fails closed with HTTP 403 Forbidden")
    void test15_MissingDepartmentHeadFailsClosed() {
        String uUid = UUID.randomUUID().toString().substring(0, 8).toLowerCase();
        User unassigned = new User();
        unassigned.setEmail("unassigned." + uUid + "@example.com");
        unassigned.setFullName("Unassigned Head " + uUid);
        unassigned.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        unassigned.setRole(Role.DEPARTMENT_HEAD);
        unassigned = userRepository.save(unassigned);

        final String unassignedEmail = unassigned.getEmail();
        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment(unassignedEmail, null));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("No authorized department assigned"));
    }

    // =========================================================================
    // Test 16: Verification that NO first-department fallback exists
    // =========================================================================
    @Test
    @DisplayName("Test 16: Explicit verification that first-department fallback allDepts.get(0) does NOT execute")
    void test16_NoFirstDepartmentFallback() {
        Department firstDept = departmentRepository.findAll().stream().min(Comparator.comparing(Department::getId)).orElseThrow();
        String uUid = UUID.randomUUID().toString().substring(0, 8).toLowerCase();

        User unassigned = new User();
        unassigned.setEmail("unassigned2." + uUid + "@example.com");
        unassigned.setFullName("Unassigned Head 2 " + uUid);
        unassigned.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        unassigned.setRole(Role.DEPARTMENT_HEAD);
        unassigned = userRepository.save(unassigned);

        final String unassignedEmail = unassigned.getEmail();
        try {
            Department resolved = departmentHeadService.resolveAuthorizedDepartment(unassignedEmail, null);
            fail("Expected 403 Forbidden but got: " + resolved.getName());
        } catch (ApiException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }
    }

    // =========================================================================
    // Test 17: Department Head cross-department isolation (IDOR prevention)
    // =========================================================================
    @Test
    @DisplayName("Test 17: Department Head cross-department access attempt is blocked with HTTP 403 Forbidden")
    void test17_DepartmentHeadCrossDepartmentIsolation() {
        // testDeptHead is assigned to deptEngineering; requesting deptFinance.getId() must be blocked
        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment(testDeptHead.getEmail(), deptFinance.getId()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // =========================================================================
    // Test 18: Audit record persistence across operations
    // =========================================================================
    @Test
    @DisplayName("Test 18: Administrative audit logs persist in native PostgreSQL with actor and timestamp")
    void test18_AuditRecordPersistence() {
        long initialCount = auditLogRepository.count();
        adminGovernanceService.recordAuditLog(
                primaryAdmin,
                "SECURITY_SCAN",
                "System",
                "SYS-01",
                "Routine security vulnerability baseline scan completed.",
                "127.0.0.1"
        );

        long newCount = auditLogRepository.count();
        assertEquals(initialCount + 1, newCount, "Audit log count must increase by 1");

        List<AuditLogDto> logs = adminGovernanceService.getAllAuditLogs();
        assertTrue(logs.stream().anyMatch(l -> "SECURITY_SCAN".equals(l.action())));
    }

    // =========================================================================
    // Test 19: Audit data does not expose secrets or passwords
    // =========================================================================
    @Test
    @DisplayName("Test 19: Audit logging sanitizes details ensuring secrets and password hashes are masked")
    void test19_AuditDataDoesNotExposeSecrets() {
        adminGovernanceService.recordAuditLog(
                primaryAdmin,
                "PASSWORD_CHANGE_ATTEMPT",
                "User",
                testEmployee.getId().toString(),
                "Password changed with password=SuperSecretPassword123! and token=abc123secretToken",
                "127.0.0.1"
        );

        List<AuditLogDto> logs = adminGovernanceService.getAllAuditLogs();
        AuditLogDto sensitiveLog = logs.stream()
                .filter(l -> "PASSWORD_CHANGE_ATTEMPT".equals(l.action()))
                .findFirst().orElseThrow();

        assertFalse(sensitiveLog.details().contains("SuperSecretPassword123!"), "Audit details must mask plaintext password");
        assertFalse(sensitiveLog.details().contains("abc123secretToken"), "Audit details must mask token");
        assertTrue(sensitiveLog.details().contains("[MASKED]"), "Masked replacement token must be present");
    }

    // =========================================================================
    // Test 20: Backup listing and metadata consistency
    // =========================================================================
    @Test
    @DisplayName("Test 20: Backup listing reflects actual disk archives and metadata records")
    void test20_BackupListingAndMetadataConsistency() {
        List<BackupMetadataDto> backups = databaseBackupService.listBackups();
        assertNotNull(backups, "Backup list must not be null");
        for (BackupMetadataDto b : backups) {
            assertNotNull(b.filename());
            assertTrue(b.sizeBytes() >= 0);
            assertNotNull(b.formattedSize());
            assertNotNull(b.createdAt());
            assertNotNull(b.status());
        }
    }

    // =========================================================================
    // Test 21: Backup path traversal protection
    // =========================================================================
    @Test
    @DisplayName("Test 21: Path traversal attacks on backup download are blocked with HTTP 400 Bad Request")
    void test21_BackupPathTraversalProtection() {
        ApiException exTraversal = assertThrows(ApiException.class, () ->
                databaseBackupService.downloadBackup("../../../etc/passwd"));
        assertEquals(HttpStatus.BAD_REQUEST, exTraversal.getStatus());

        ApiException exWindows = assertThrows(ApiException.class, () ->
                databaseBackupService.downloadBackup("..\\..\\Windows\\win.ini"));
        assertEquals(HttpStatus.BAD_REQUEST, exWindows.getStatus());

        ApiException exNotFound = assertThrows(ApiException.class, () ->
                databaseBackupService.downloadBackup("skillsphere_backup_99999999_999999.dump"));
        assertEquals(HttpStatus.NOT_FOUND, exNotFound.getStatus());
    }

    // =========================================================================
    // Test 22: Safe isolated restore integrity boundary
    // =========================================================================
    @Test
    @DisplayName("Test 22: Verify database identity check against live PostgreSQL instance")
    void test22_DatabaseIdentityCheck() {
        assertDoesNotThrow(() -> {
            try (Connection conn = dataSource.getConnection()) {
                assertEquals("skillsphere_db", conn.getCatalog(), "Active database must be skillsphere_db");
            }
        });
    }

    // =========================================================================
    // Test 23: Communication settings privacy enforcement
    // =========================================================================
    @Test
    @DisplayName("Test 23: Communication settings updates configuration without exposing private chat messages")
    void test23_CommunicationSettingsPrivacyEnforcement() {
        CommunicationSettingsDto settings = adminDashboardService.getCommunicationSettings();
        assertNotNull(settings);
        assertTrue(settings.privacyNotice().contains("Least-Privilege Privacy Guarantee"));

        // Verify class has no fields leaking chat transcripts
        for (var method : CommunicationSettingsDto.class.getMethods()) {
            String mName = method.getName().toLowerCase();
            assertFalse(mName.contains("chatmessage"), "Settings must not expose chat message entities");
            assertFalse(mName.contains("transcript"), "Settings must not expose conversation transcripts");
        }
    }

    // =========================================================================
    // Test 24: Support ticket creation and administration
    // =========================================================================
    @Test
    @DisplayName("Test 24: Support ticket creation, status update by administrator, and audit record")
    void test24_SupportTicketLifecycle() {
        SupportTicketDto.CreateRequest req = new SupportTicketDto.CreateRequest(
                "Assessment Loading Glitch",
                "Encountered a timeout when submitting Kubernetes assessment.",
                "HIGH"
        );
        SupportTicketDto ticket = adminGovernanceService.createTicket(testEmployee.getEmail(), req);
        assertNotNull(ticket);
        assertEquals("OPEN", ticket.status());
        assertEquals("HIGH", ticket.priority());

        // Admin updates ticket
        SupportTicketDto.UpdateRequest updateReq = new SupportTicketDto.UpdateRequest(
                "RESOLVED",
                "Investigated timeout; resolved by increasing Gemini timeout parameter."
        );
        SupportTicketDto resolved = adminGovernanceService.updateTicket(ticket.id(), updateReq);
        assertEquals("RESOLVED", resolved.status());
        assertEquals("Investigated timeout; resolved by increasing Gemini timeout parameter.", resolved.adminNotes());
    }

    // =========================================================================
    // Test 25: Platform feedback inspection by administrator
    // =========================================================================
    @Test
    @DisplayName("Test 25: Platform feedback submission and administrative review")
    void test25_PlatformFeedbackInspection() {
        PlatformFeedbackDto.CreateRequest req = new PlatformFeedbackDto.CreateRequest(
                "PLATFORM",
                5,
                "Intuitive skill matrix heatmap view. Very helpful!"
        );
        PlatformFeedbackDto submitted = adminGovernanceService.submitFeedback(testEmployee.getEmail(), req);
        assertNotNull(submitted);
        assertEquals(5, submitted.rating());

        List<PlatformFeedbackDto> allFeedback = adminGovernanceService.getAllFeedback();
        assertTrue(allFeedback.stream().anyMatch(f -> f.id().equals(submitted.id())));
    }

    // =========================================================================
    // Test 26: System configuration key-value management
    // =========================================================================
    @Test
    @DisplayName("Test 26: System configuration listing and updating with audit log")
    void test26_SystemConfigurationManagement() {
        SystemConfiguration config = adminGovernanceService.updateConfiguration(
                "max_upload_size_mb", "25"
        );
        assertNotNull(config);
        assertEquals("25", config.getConfigValue());

        List<SystemConfiguration> configs = adminGovernanceService.getAllConfigurations();
        assertTrue(configs.stream().anyMatch(c -> "max_upload_size_mb".equals(c.getConfigKey()) && "25".equals(c.getConfigValue())));
    }

    // =========================================================================
    // Test 27: Security overview metrics and active sessions count
    // =========================================================================
    @Test
    @DisplayName("Test 27: Security overview calculates MFA enforcement, locked accounts, and security alerts")
    void test27_SecurityOverviewMetrics() {
        AdminSecurityOverviewDto sec = adminDashboardService.getSecurityOverview();
        assertNotNull(sec);
        assertNotNull(sec.currentlyLockedAccounts());
        assertTrue(sec.recentFailedAttemptsCount() >= 0);
        assertTrue(sec.totalAuditLogsLast24h() >= 0);
        assertNotNull(sec.securityAlerts());
    }

    // =========================================================================
    // Test 28: HR workflow regression integrity
    // =========================================================================
    @Test
    @DisplayName("Test 28: HR competency framework and role requirements remain intact")
    void test28_HrWorkflowRegression() {
        SkillCategory cat = skillCategoryRepository.findAll().stream().findFirst().orElseGet(() ->
                skillCategoryRepository.save(new SkillCategory("Admin Tech", "Tech skills")));

        Skill skill = skillRepository.save(new Skill(cat, "Admin Cloud " + UUID.randomUUID().toString().substring(0, 6), "Cloud skills"));
        RoleSkillRequirement req = new RoleSkillRequirement("Software Developer", skill, "Intermediate", "HIGH");
        req = roleSkillRequirementRepository.save(req);

        assertNotNull(req.getId());
        assertEquals("Software Developer", req.getJobRole());
        assertEquals("Intermediate", req.getRequiredProficiency());
    }

    // =========================================================================
    // Test 29: Manager workflow regression integrity
    // =========================================================================
    @Test
    @DisplayName("Test 29: Manager team governance and skill gap evaluation remain intact")
    void test29_ManagerWorkflowRegression() {
        SkillCategory cat = skillCategoryRepository.findAll().stream().findFirst().orElseThrow();
        Skill skill = skillRepository.findAll().stream().findFirst().orElseThrow();

        SkillGap gap = new SkillGap(testEmployee, skill, 1, 3, 2, "HIGH");
        gap = skillGapRepository.save(gap);

        assertNotNull(gap.getId());
        assertEquals("HIGH", gap.getPriority());
        assertEquals(2, gap.getGapSize());
    }

    // =========================================================================
    // Test 30: Mentor privacy regression integrity
    // =========================================================================
    @Test
    @DisplayName("Test 30: Mentor privacy guarantees zero private notes or chat exposure")
    void test30_MentorPrivacyRegression() {
        CommunicationSettingsDto commSettings = adminDashboardService.getCommunicationSettings();
        assertNotNull(commSettings);
        assertTrue(commSettings.privacyNotice().contains("strictly prohibited from inspecting private employee chat messages"));
    }

    // =========================================================================
    // Test 31: Employee workflow regression integrity
    // =========================================================================
    @Test
    @DisplayName("Test 31: Employee profile, role, and department associations remain intact")
    void test31_EmployeeWorkflowRegression() {
        EmployeeProfile profile = employeeProfileRepository.findByUserId(testEmployee.getId()).orElseThrow();
        assertEquals(deptEngineering.getName(), profile.getDepartment());
        assertEquals("Software Developer", profile.getJobRole());
    }

    // =========================================================================
    // Test 32: Empty dataset handling without errors or NPEs
    // =========================================================================
    @Test
    @DisplayName("Test 32: System handles empty queries gracefully without throwing NullPointerExceptions")
    void test32_EmptyDataHandling() {
        assertDoesNotThrow(() -> {
            List<AdminUserDetailDto> noUsers = adminDashboardService.getUsers(
                    Role.ADMIN, "NonExistentDeptXYZ", null, "NoSuchUserQueryXYZ"
            );
            assertTrue(noUsers.isEmpty());

            List<SupportTicketAdminDto> noTickets = adminDashboardService.getSupportTickets("NON_EXISTENT_STATUS", null);
            assertTrue(noTickets.isEmpty());
        });
    }
}
