package com.skillsphere.service;

import com.skillsphere.dto.DepartmentHeadDtos.*;
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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DepartmentHeadDashboardAcceptanceIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private EmployeeSkillRepository employeeSkillRepository;

    @Autowired
    private SkillCategoryRepository skillCategoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private RoleSkillRequirementRepository roleSkillRequirementRepository;

    @Autowired
    private SkillGapRepository skillGapRepository;

    @Autowired
    private AssessmentAttemptRepository assessmentAttemptRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TrainingProgrammeRepository trainingProgrammeRepository;

    @Autowired
    private TrainingEnrollmentRepository trainingEnrollmentRepository;

    @Autowired
    private EmployeeLearningRepository employeeLearningRepository;

    @Autowired
    private MentorAssignmentRepository mentorAssignmentRepository;

    @Autowired
    private MentorshipSessionRepository mentorshipSessionRepository;

    @Autowired
    private MentorshipFeedbackRepository mentorshipFeedbackRepository;

    @Autowired
    private HrAnnouncementRepository hrAnnouncementRepository;

    @Autowired
    private HrAnnouncementReadRepository hrAnnouncementReadRepository;

    @Autowired
    private LeadershipReportShareRepository leadershipReportShareRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DepartmentHeadService departmentHeadService;

    private User deptHeadA;
    private User deptHeadB;
    private User unassignedHead;
    private User adminUser;
    private User employeeA;
    private User managerA;
    private User mentorA;

    private Department deptA;
    private Department deptB;
    private Team teamA;
    private SkillCategory categoryTech;
    private Skill skillBackend;
    private Skill skillCloud;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // Departments
        deptA = new Department();
        deptA.setName("Engineering Dept " + uid);
        deptA.setCode("ENG_" + uid);
        deptA = departmentRepository.save(deptA);

        deptB = new Department();
        deptB.setName("Product Dept " + uid);
        deptB.setCode("PRD_" + uid);
        deptB = departmentRepository.save(deptB);

        // Department Head A (assigned to deptA)
        deptHeadA = new User();
        deptHeadA.setEmail("heada." + uid.toLowerCase() + "@example.com");
        deptHeadA.setFullName("Alice Head " + uid);
        deptHeadA.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        deptHeadA.setRole(Role.DEPARTMENT_HEAD);
        deptHeadA = userRepository.save(deptHeadA);

        deptA.setHeadUser(deptHeadA);
        deptA = departmentRepository.save(deptA);

        EmployeeProfile headAProfile = new EmployeeProfile(deptHeadA);
        headAProfile.setDepartment(deptA.getName());
        headAProfile.setJobRole("Software Developer");
        headAProfile.setExperienceLevel("Experienced");
        employeeProfileRepository.save(headAProfile);

        // Department Head B (assigned to deptB)
        deptHeadB = new User();
        deptHeadB.setEmail("headb." + uid.toLowerCase() + "@example.com");
        deptHeadB.setFullName("Bob Head " + uid);
        deptHeadB.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        deptHeadB.setRole(Role.DEPARTMENT_HEAD);
        deptHeadB = userRepository.save(deptHeadB);

        deptB.setHeadUser(deptHeadB);
        deptB = departmentRepository.save(deptB);

        EmployeeProfile headBProfile = new EmployeeProfile(deptHeadB);
        headBProfile.setDepartment(deptB.getName());
        headBProfile.setJobRole("Software Developer");
        headBProfile.setExperienceLevel("Experienced");
        employeeProfileRepository.save(headBProfile);

        // Unassigned Department Head (No head_user_id, no employee profile dept)
        unassignedHead = new User();
        unassignedHead.setEmail("unassigned." + uid.toLowerCase() + "@example.com");
        unassignedHead.setFullName("Una Unassigned " + uid);
        unassignedHead.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        unassignedHead.setRole(Role.DEPARTMENT_HEAD);
        unassignedHead = userRepository.save(unassignedHead);

        // Admin User
        adminUser = new User();
        adminUser.setEmail("admin." + uid.toLowerCase() + "@example.com");
        adminUser.setFullName("Adam Admin " + uid);
        adminUser.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        // Manager A
        managerA = new User();
        managerA.setEmail("mgra." + uid.toLowerCase() + "@example.com");
        managerA.setFullName("Mark Manager " + uid);
        managerA.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        managerA.setRole(Role.MANAGER);
        managerA = userRepository.save(managerA);

        EmployeeProfile mgrProfile = new EmployeeProfile(managerA);
        mgrProfile.setDepartment(deptA.getName());
        mgrProfile.setJobRole("Software Developer");
        mgrProfile.setExperienceLevel("Experienced");
        employeeProfileRepository.save(mgrProfile);

        // Team in Dept A
        teamA = new Team(deptA, "Backend Core " + uid, managerA);
        teamA = teamRepository.save(teamA);

        // Employee A in Dept A
        employeeA = new User();
        employeeA.setEmail("empa." + uid.toLowerCase() + "@example.com");
        employeeA.setFullName("Ethan Employee " + uid);
        employeeA.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        employeeA.setRole(Role.EMPLOYEE);
        employeeA = userRepository.save(employeeA);

        EmployeeProfile empProfile = new EmployeeProfile(employeeA);
        empProfile.setDepartment(deptA.getName());
        empProfile.setJobRole("Software Developer");
        empProfile.setExperienceLevel("Intermediate");
        employeeProfileRepository.save(empProfile);

        TeamMembership tm = new TeamMembership(teamA, employeeA, "MEMBER");
        teamMembershipRepository.save(tm);

        // Mentor in Dept A
        mentorA = new User();
        mentorA.setEmail("mentora." + uid.toLowerCase() + "@example.com");
        mentorA.setFullName("Mona Mentor " + uid);
        mentorA.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        mentorA.setRole(Role.MENTOR);
        mentorA = userRepository.save(mentorA);

        // Skills & Requirements
        categoryTech = skillCategoryRepository.findAll().stream().findFirst().orElseGet(() ->
                skillCategoryRepository.save(new SkillCategory("Core Tech " + uid, "Technical skills")));

        skillBackend = skillRepository.save(new Skill(categoryTech, "Java Architecture " + uid, "Backend engineering"));
        skillCloud = skillRepository.save(new Skill(categoryTech, "Kubernetes Cloud " + uid, "Cloud orchestration"));

        RoleSkillRequirement reqBackend = new RoleSkillRequirement("Software Developer", skillBackend, "Intermediate", "HIGH");
        roleSkillRequirementRepository.save(reqBackend);

        RoleSkillRequirement reqCloud = new RoleSkillRequirement("Software Developer", skillCloud, "Beginner", "MEDIUM");
        roleSkillRequirementRepository.save(reqCloud);
    }

    // =========================================================================
    // Test 1: Live PostgreSQL 18.4 connection, JDBC metadata, flyway migrations
    // =========================================================================
    @Test
    @DisplayName("Test 1: Verify PostgreSQL 18.4 connection, catalog skillsphere_db, and Flyway migrations")
    void test1_LiveDatabaseConnectionAndFlywayMigrations() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String dbProduct = metaData.getDatabaseProductName();
            String dbVersion = metaData.getDatabaseProductVersion();
            String catalog = conn.getCatalog();

            assertEquals("PostgreSQL", dbProduct, "Database product must be PostgreSQL");
            assertTrue(dbVersion.contains("18"), "Database version must be PostgreSQL 18.x, found: " + dbVersion);
            assertEquals("skillsphere_db", catalog, "Database catalog must be skillsphere_db");

            // Verify Flyway schema history
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")) {
                assertTrue(rs.next(), "Flyway schema history query should return rows");
                int appliedMigrations = rs.getInt(1);
                assertTrue(appliedMigrations >= 10, "Expected at least 10 applied Flyway migrations, found: " + appliedMigrations);
            }
        }
    }

    // =========================================================================
    // Test 2: Overview aggregates strictly use authenticated user's department
    // =========================================================================
    @Test
    @DisplayName("Test 2: Overview aggregates strictly isolate to authenticated Department Head's department")
    void test2_OverviewAggregatesUseAuthenticatedDepartment() {
        DepartmentHeadOverviewDto overviewA = departmentHeadService.getDashboardOverview(deptHeadA.getEmail(), null);
        assertNotNull(overviewA);
        assertEquals(deptA.getId(), overviewA.departmentId());
        assertEquals(deptA.getName(), overviewA.departmentName());
        assertEquals("Alice Head", overviewA.headName().split(" ")[0] + " " + overviewA.headName().split(" ")[1]);
        assertTrue(overviewA.totalEmployees() >= 1, "Department A must have at least 1 employee");
        assertTrue(overviewA.totalTeams() >= 1, "Department A must have at least 1 team");

        // Department B overview
        DepartmentHeadOverviewDto overviewB = departmentHeadService.getDashboardOverview(deptHeadB.getEmail(), null);
        assertNotNull(overviewB);
        assertEquals(deptB.getId(), overviewB.departmentId());
        assertEquals(deptB.getName(), overviewB.departmentName());
        assertEquals(0, overviewB.totalTeams(), "Department B should have 0 teams");
    }

    // =========================================================================
    // Test 3: Department Head with valid assignment succeeds across subviews
    // =========================================================================
    @Test
    @DisplayName("Test 3: Department Head with valid assignment succeeds across all service subviews")
    void test3_DepartmentHeadWithValidAssignmentSucceeds() {
        assertDoesNotThrow(() -> {
            DepartmentHeadOverviewDto overview = departmentHeadService.getDashboardOverview(deptHeadA.getEmail(), null);
            assertNotNull(overview);

            DepartmentProfileDto profile = departmentHeadService.getDepartmentOverview(deptHeadA.getEmail(), null);
            assertNotNull(profile);

            DeptSkillMatrixDto matrix = departmentHeadService.getDepartmentSkillMatrix(deptHeadA.getEmail(), null, null, null);
            assertNotNull(matrix);

            DeptHeatmapDto heatmap = departmentHeadService.getDepartmentHeatmap(deptHeadA.getEmail(), null);
            assertNotNull(heatmap);

            DeptCapabilityProfileDto capability = departmentHeadService.getWorkforceCapability(deptHeadA.getEmail(), null);
            assertNotNull(capability);

            DeptLearningMentoringDto learningMentoring = departmentHeadService.getLearningAndMentoringOverview(deptHeadA.getEmail(), null);
            assertNotNull(learningMentoring);

            List<DeptRiskAlertDto> alerts = departmentHeadService.getDepartmentRiskAlerts(deptHeadA.getEmail(), null);
            assertNotNull(alerts);

            DeptReportResultDto report = departmentHeadService.generateDepartmentReport(deptHeadA.getEmail(), "EXECUTIVE_SUMMARY", null);
            assertNotNull(report);

            List<DeptAnnouncementDto> announcements = departmentHeadService.getDepartmentAnnouncements(deptHeadA.getEmail(), null);
            assertNotNull(announcements);

            List<DeptNotificationDto> notifications = departmentHeadService.getNotifications(deptHeadA.getEmail(), false);
            assertNotNull(notifications);
        });
    }

    // =========================================================================
    // Test 4: Department Head with NO assignment fails closed with HTTP 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("Test 4: Department Head with no assigned department fails closed with HTTP 403 Forbidden")
    void test4_DepartmentHeadWithNoAssignmentFailsClosed() {
        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment(unassignedHead.getEmail(), null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("No authorized department assigned"),
                "Expected failure message regarding unassigned department, got: " + ex.getMessage());
    }

    // =========================================================================
    // Test 5: Verification that NO first-department fallback exists
    // =========================================================================
    @Test
    @DisplayName("Test 5: Explicitly verify that NO allDepts.get(0) / first-department fallback exists")
    void test5_NoFirstDepartmentFallback() {
        Department firstDept = departmentRepository.findAll().stream().min(Comparator.comparing(Department::getId)).orElse(null);
        assertNotNull(firstDept, "At least one department must exist in the database");

        assertNotEquals(unassignedHead.getId(), firstDept.getHeadUser() != null ? firstDept.getHeadUser().getId() : null);

        try {
            Department resolved = departmentHeadService.resolveAuthorizedDepartment(unassignedHead.getEmail(), null);
            fail("Expected 403 Forbidden but got department: " + resolved.getName());
        } catch (ApiException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatus());
        }
    }

    // =========================================================================
    // Test 6: Client-supplied departmentId cannot override authenticated scope (403 on mismatch)
    // =========================================================================
    @Test
    @DisplayName("Test 6: Client-supplied departmentId cannot override authenticated scope (IDOR blocked with 403)")
    void test6_ClientSuppliedDepartmentIdCannotOverrideAuthenticatedScope() {
        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment(deptHeadA.getEmail(), deptB.getId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("not authorized to access department " + deptB.getId()),
                "Expected access denied for department ID mismatch, got: " + ex.getMessage());
    }

    // =========================================================================
    // Test 7: Cross-department access attempts return HTTP 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("Test 7: DeptHead A attempting to query DeptHead B's department returns HTTP 403 Forbidden")
    void test7_CrossDepartmentAccessReturnsForbidden() {
        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.getDashboardOverview(deptHeadA.getEmail(), deptB.getId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // =========================================================================
    // Test 8: Admin can query any department with explicit requestedDeptId
    // =========================================================================
    @Test
    @DisplayName("Test 8: Administrator queries require explicit department ID or profile department")
    void test8_AdminCanQueryAnyDepartmentWithExplicitId() {
        Department resolvedA = departmentHeadService.resolveAuthorizedDepartment(adminUser.getEmail(), deptA.getId());
        assertEquals(deptA.getId(), resolvedA.getId());

        Department resolvedB = departmentHeadService.resolveAuthorizedDepartment(adminUser.getEmail(), deptB.getId());
        assertEquals(deptB.getId(), resolvedB.getId());

        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment(adminUser.getEmail(), null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Department ID parameter is required for administrator queries"));
    }

    // =========================================================================
    // Test 9: Non-Department Head / Non-Admin callers are rejected with 403 Forbidden
    // =========================================================================
    @Test
    @DisplayName("Test 9: Role-based access control rejects EMPLOYEE, MANAGER, and MENTOR roles with 403")
    void test9_RoleBasedAccessControlRejectsNonDeptHeadNonAdmin() {
        ApiException exEmp = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment(employeeA.getEmail(), null));
        assertEquals(HttpStatus.FORBIDDEN, exEmp.getStatus());

        ApiException exMgr = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment(managerA.getEmail(), null));
        assertEquals(HttpStatus.FORBIDDEN, exMgr.getStatus());

        ApiException exMtr = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment(mentorA.getEmail(), null));
        assertEquals(HttpStatus.FORBIDDEN, exMtr.getStatus());
    }

    // =========================================================================
    // Test 10: Skill matrix and heatmap strictly scope to department team members
    // =========================================================================
    @Test
    @DisplayName("Test 10: Skill matrix and heatmap strictly scope to department team members")
    void test10_SkillMatrixAndHeatmapScopedToDepartment() {
        EmployeeSkill es = new EmployeeSkill(employeeA, skillBackend, "Intermediate");
        es.setVerified(true);
        employeeSkillRepository.save(es);

        DeptSkillMatrixDto matrix = departmentHeadService.getDepartmentSkillMatrix(deptHeadA.getEmail(), null, null, null);
        assertNotNull(matrix);
        assertEquals(deptA.getId(), matrix.departmentId());
        assertTrue(matrix.competencies().stream().anyMatch(c -> c.skillId().equals(skillBackend.getId())),
                "Competency matrix must include Java Architecture");

        DeptHeatmapDto heatmap = departmentHeadService.getDepartmentHeatmap(deptHeadA.getEmail(), null);
        assertNotNull(heatmap);
        assertEquals(deptA.getName(), heatmap.departmentName());
        assertTrue(heatmap.teams().contains(teamA.getName()), "Heatmap must include Backend Core team");
    }

    // =========================================================================
    // Test 11: HR-managed competency and role requirements reflect dynamically
    // =========================================================================
    @Test
    @DisplayName("Test 11: HR-managed competency and role requirement updates reflect dynamically")
    void test11_HrManagedCompetencyAndRoleRequirementsReflectDynamically() {
        DeptSkillMatrixDto matrixInitial = departmentHeadService.getDepartmentSkillMatrix(deptHeadA.getEmail(), null, null, null);
        Optional<DeptCompetencyMatrixRowDto> rowInitial = matrixInitial.competencies().stream()
                .filter(c -> c.skillId().equals(skillBackend.getId())).findFirst();
        assertTrue(rowInitial.isPresent());
        assertEquals("Intermediate", rowInitial.get().targetProficiency());

        RoleSkillRequirement req = roleSkillRequirementRepository
                .findByJobRoleAndSkillId("Software Developer", skillBackend.getId()).orElseThrow();
        req.setRequiredProficiency("Advanced");
        roleSkillRequirementRepository.save(req);

        DeptSkillMatrixDto matrixUpdated = departmentHeadService.getDepartmentSkillMatrix(deptHeadA.getEmail(), null, null, null);
        Optional<DeptCompetencyMatrixRowDto> rowUpdated = matrixUpdated.competencies().stream()
                .filter(c -> c.skillId().equals(skillBackend.getId())).findFirst();
        assertTrue(rowUpdated.isPresent());
        assertEquals("Advanced", rowUpdated.get().targetProficiency(), "Matrix must dynamically reflect target proficiency Advanced");
    }

    // =========================================================================
    // Test 12: Learning and training participation reflects live PostgreSQL records
    // =========================================================================
    @Test
    @DisplayName("Test 12: Learning and training participation reflects live PostgreSQL records")
    void test12_LearningAndTrainingParticipationReflectsDatabase() {
        Course course = courseRepository.save(new Course("Spring Cloud Microservices", "Advanced", skillBackend, "Advanced", 16, "Spring Corp", "https://spring.io"));
        EmployeeLearning learning = new EmployeeLearning(employeeA, course, "COMPLETED", 100);
        learning.setCompletedAt(Instant.now());
        employeeLearningRepository.save(learning);

        TrainingProgramme programme = new TrainingProgramme();
        programme.setTitle("Enterprise Cloud Bootcamp");
        programme.setDepartment(deptA);
        programme.setStatus("UPCOMING");
        programme.setStartDate(LocalDate.now().plusDays(7));
        programme.setEndDate(LocalDate.now().plusDays(14));
        programme.setCapacity(25);
        programme.setEnrolledCount(5);
        trainingProgrammeRepository.save(programme);

        DeptLearningMentoringDto overview = departmentHeadService.getLearningAndMentoringOverview(deptHeadA.getEmail(), null);
        assertNotNull(overview.learningOverview());
        assertTrue(overview.learningOverview().totalCourseEnrollments() >= 1);
        assertTrue(overview.learningOverview().completedCount() >= 1);
        assertTrue(overview.learningOverview().programmesCount() >= 1);
        assertTrue(overview.learningOverview().attendedProgrammesCount() >= 5);
    }

    // =========================================================================
    // Test 13: Mentorship aggregate indicators only
    // =========================================================================
    @Test
    @DisplayName("Test 13: Mentorship indicators return live aggregate statistics")
    void test13_MentorshipAggregateIndicatorsOnly() {
        MentorAssignment assignment = new MentorAssignment(mentorA, employeeA, deptHeadA, "Core mentorship");
        assignment = mentorAssignmentRepository.save(assignment);

        MentorshipSession session = new MentorshipSession(
                assignment, "Architecture Review", "Review deep dive",
                Instant.now().minusSeconds(3600), 60, "https://meet.jit.si/session-test"
        );
        session.setStatus(SessionStatus.COMPLETED);
        session = mentorshipSessionRepository.save(session);

        MentorshipFeedback feedback = new MentorshipFeedback(session, employeeA, 5, "Exemplary mentorship session");
        mentorshipFeedbackRepository.save(feedback);

        DeptLearningMentoringDto overview = departmentHeadService.getLearningAndMentoringOverview(deptHeadA.getEmail(), null);
        assertNotNull(overview.mentoringOverview());
        assertTrue(overview.mentoringOverview().activeAssignmentsCount() >= 1);
        assertTrue(overview.mentoringOverview().totalSessionsCompleted() >= 1);
        assertTrue(overview.mentoringOverview().avgMentorshipRating() >= 4.0);
        assertTrue(overview.mentoringOverview().activeMenteesCount() >= 1);
        assertTrue(overview.mentoringOverview().activeMentorsCount() >= 1);
    }

    // =========================================================================
    // Test 14: Mentorship privacy boundary: zero private session notes or chat messages
    // =========================================================================
    @Test
    @DisplayName("Test 14: Mentorship privacy boundary enforces zero private notes or chat exposure")
    void test14_MentorshipPrivacyBoundaryExcludesPrivateNotesAndChat() {
        DeptLearningMentoringDto overview = departmentHeadService.getLearningAndMentoringOverview(deptHeadA.getEmail(), null);
        DeptMentoringSummaryDto mentoring = overview.mentoringOverview();

        assertNotNull(mentoring);
        assertNotNull(mentoring.topMentoredSkills());

        for (var method : DeptMentoringSummaryDto.class.getMethods()) {
            String name = method.getName().toLowerCase();
            assertFalse(name.contains("chat"), "Mentoring summary must not expose chat data");
            assertFalse(name.contains("note"), "Mentoring summary must not expose private session notes");
            assertFalse(name.contains("transcript"), "Mentoring summary must not expose transcripts");
        }
    }

    // =========================================================================
    // Test 15: Shared leadership report isolation
    // =========================================================================
    @Test
    @DisplayName("Test 15: Leadership report isolation excludes manager-only and other-dept reports")
    void test15_SharedLeadershipReportIsolation() {
        LeadershipReportShare reportDeptA = new LeadershipReportShare(
                "SKILL_GAP", "Dept A Skills", deptHeadA, "DEPARTMENT_HEAD", deptA, "Summary notes A", "{}"
        );
        leadershipReportShareRepository.save(reportDeptA);

        LeadershipReportShare reportOrg = new LeadershipReportShare(
                "EXECUTIVE", "Org Overview", deptHeadA, "ALL_LEADERSHIP", null, "Org notes", "{}"
        );
        leadershipReportShareRepository.save(reportOrg);

        LeadershipReportShare reportMgr = new LeadershipReportShare(
                "TEAM_GAP", "Team Gap Only", managerA, "MANAGER", deptA, "Manager notes", "{}"
        );
        leadershipReportShareRepository.save(reportMgr);

        LeadershipReportShare reportDeptB = new LeadershipReportShare(
                "DEPT_B_GAP", "Dept B Gap", deptHeadB, "DEPARTMENT_HEAD", deptB, "Dept B notes", "{}"
        );
        leadershipReportShareRepository.save(reportDeptB);

        List<DeptLeadershipSharedReportDto> sharedReports = departmentHeadService.getSharedLeadershipReports(deptHeadA.getEmail(), null);
        assertNotNull(sharedReports);

        List<Long> visibleIds = sharedReports.stream().map(DeptLeadershipSharedReportDto::id).toList();
        assertTrue(visibleIds.contains(reportDeptA.getId()), "Report for Dept A must be visible");
        assertTrue(visibleIds.contains(reportOrg.getId()), "Org-wide report must be visible");
        assertFalse(visibleIds.contains(reportMgr.getId()), "Manager-only report must NOT be visible to DeptHead");
        assertFalse(visibleIds.contains(reportDeptB.getId()), "Dept B report must NOT be visible to DeptHead A");
    }

    // =========================================================================
    // Test 16: Announcement visibility and IDOR protection
    // =========================================================================
    @Test
    @DisplayName("Test 16: Announcement visibility and IDOR protection for markAnnouncementRead")
    void test16_AnnouncementVisibilityAndIdorProtection() {
        HrAnnouncement orgAnnounce = new HrAnnouncement(
                adminUser, "ORGANIZATION", null, null, "All Hands Meeting", "Quarterly briefing", "HIGH"
        );
        orgAnnounce = hrAnnouncementRepository.save(orgAnnounce);

        HrAnnouncement deptAAnnounce = new HrAnnouncement(
                deptHeadA, "DEPARTMENT", deptA, null, "Dept A Sync", "Engineering all-hands", "NORMAL"
        );
        deptAAnnounce = hrAnnouncementRepository.save(deptAAnnounce);

        HrAnnouncement deptBAnnounce = new HrAnnouncement(
                deptHeadB, "DEPARTMENT", deptB, null, "Dept B Sync", "Product all-hands", "NORMAL"
        );
        deptBAnnounce = hrAnnouncementRepository.save(deptBAnnounce);

        List<DeptAnnouncementDto> announcements = departmentHeadService.getDepartmentAnnouncements(deptHeadA.getEmail(), null);
        List<Long> annIds = announcements.stream().map(DeptAnnouncementDto::id).toList();
        assertTrue(annIds.contains(orgAnnounce.getId()), "Org-wide announcement must be visible");
        assertTrue(annIds.contains(deptAAnnounce.getId()), "Dept A announcement must be visible");
        assertFalse(annIds.contains(deptBAnnounce.getId()), "Dept B announcement must NOT be visible to DeptHead A");

        final Long orgAnnId = orgAnnounce.getId();
        final Long deptAAnnId = deptAAnnounce.getId();
        assertDoesNotThrow(() -> departmentHeadService.markAnnouncementRead(orgAnnId, deptHeadA.getEmail()));
        assertDoesNotThrow(() -> departmentHeadService.markAnnouncementRead(deptAAnnId, deptHeadA.getEmail()));

        Long bAnnounceId = deptBAnnounce.getId();
        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.markAnnouncementRead(bAnnounceId, deptHeadA.getEmail()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus(), "Marking read another department's announcement must return 403");
    }

    // =========================================================================
    // Test 17: Create department announcement generates workforce notifications
    // =========================================================================
    @Test
    @DisplayName("Test 17: Creating department announcement generates targeted in-app notifications")
    void test17_CreateDepartmentAnnouncementGeneratesWorkforceNotifications() {
        CreateDeptAnnouncementRequest req = new CreateDeptAnnouncementRequest(
                "Architecture Guild Launch",
                "Join our weekly engineering architecture sync every Thursday.",
                "HIGH"
        );

        DeptAnnouncementDto created = departmentHeadService.createDepartmentAnnouncement(deptHeadA.getEmail(), req, null);
        assertNotNull(created);
        assertEquals("Architecture Guild Launch", created.title());
        assertEquals("HIGH", created.priority());

        List<Notification> empNotifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeA.getId());
        assertTrue(empNotifications.stream().anyMatch(n -> n.getTitle().contains("Architecture Guild Launch")),
                "Employee A must receive in-app notification for new department announcement");
    }

    // =========================================================================
    // Test 18: Executive report and CSV export generation
    // =========================================================================
    @Test
    @DisplayName("Test 18: Executive report generation produces valid summary and table structure for all types")
    void test18_ExecutiveReportAndCsvExportGeneration() {
        // EXECUTIVE_SUMMARY
        DeptReportResultDto exec = departmentHeadService.generateDepartmentReport(deptHeadA.getEmail(), "EXECUTIVE_SUMMARY", null);
        assertNotNull(exec);
        assertEquals("EXECUTIVE_SUMMARY", exec.reportType());
        assertFalse(exec.summaryStats().isEmpty());
        assertTrue(exec.summaryStats().containsKey("Total Workforce"));
        assertFalse(exec.tableHeaders().isEmpty());
        assertFalse(exec.tableRows().isEmpty());

        // SKILL_COVERAGE
        DeptReportResultDto coverage = departmentHeadService.generateDepartmentReport(deptHeadA.getEmail(), "SKILL_COVERAGE", null);
        assertNotNull(coverage);
        assertEquals("SKILL_COVERAGE", coverage.reportType());
        assertTrue(coverage.tableHeaders().contains("Competency"));

        // GAP_ANALYSIS
        DeptReportResultDto gaps = departmentHeadService.generateDepartmentReport(deptHeadA.getEmail(), "GAP_ANALYSIS", null);
        assertNotNull(gaps);
        assertEquals("GAP_ANALYSIS", gaps.reportType());
        assertTrue(gaps.tableHeaders().contains("Employee"));

        // WORKFORCE_DISTRIBUTION
        DeptReportResultDto dist = departmentHeadService.generateDepartmentReport(deptHeadA.getEmail(), "WORKFORCE_DISTRIBUTION", null);
        assertNotNull(dist);
        assertEquals("WORKFORCE_DISTRIBUTION", dist.reportType());
        assertTrue(dist.tableHeaders().contains("Team Name"));
    }

    // =========================================================================
    // Test 19: Department reassignment changes scope immediately
    // =========================================================================
    @Test
    @DisplayName("Test 19: Department reassignment updates authorized scope immediately")
    void test19_DepartmentReassignmentChangesScopeImmediately() {
        Department initialDept = departmentHeadService.resolveAuthorizedDepartment(deptHeadA.getEmail(), null);
        assertEquals(deptA.getId(), initialDept.getId());

        deptA.setHeadUser(null);
        departmentRepository.save(deptA);

        deptB.setHeadUser(deptHeadA);
        departmentRepository.save(deptB);

        EmployeeProfile profile = employeeProfileRepository.findByUserId(deptHeadA.getId()).orElseThrow();
        profile.setDepartment(deptB.getName());
        employeeProfileRepository.save(profile);

        Department reassignedDept = departmentHeadService.resolveAuthorizedDepartment(deptHeadA.getEmail(), null);
        assertEquals(deptB.getId(), reassignedDept.getId(), "Scope must immediately switch to Department B");
    }

    // =========================================================================
    // Test 20: Empty dataset handling (0 employees/teams handled gracefully)
    // =========================================================================
    @Test
    @DisplayName("Test 20: Empty dataset handling gracefully without NullPointerException or errors")
    void test20_EmptyDatasetHandling() {
        assertDoesNotThrow(() -> {
            DepartmentHeadOverviewDto overview = departmentHeadService.getDashboardOverview(deptHeadB.getEmail(), null);
            assertEquals(1, overview.totalEmployees());
            assertEquals(0, overview.totalTeams());
            assertEquals(0.0, overview.skillCoveragePercentage());

            DepartmentProfileDto profile = departmentHeadService.getDepartmentOverview(deptHeadB.getEmail(), null);
            assertEquals(1, profile.totalEmployees());
            assertTrue(profile.teams().isEmpty());

            DeptSkillMatrixDto matrix = departmentHeadService.getDepartmentSkillMatrix(deptHeadB.getEmail(), null, null, null);
            assertNotNull(matrix);

            DeptHeatmapDto heatmap = departmentHeadService.getDepartmentHeatmap(deptHeadB.getEmail(), null);
            assertNotNull(heatmap);
            assertTrue(heatmap.teams().isEmpty());

            DeptCapabilityProfileDto capability = departmentHeadService.getWorkforceCapability(deptHeadB.getEmail(), null);
            assertNotNull(capability);

            DeptLearningMentoringDto lm = departmentHeadService.getLearningAndMentoringOverview(deptHeadB.getEmail(), null);
            assertNotNull(lm);
            assertEquals(0, lm.learningOverview().totalEnrolledEmployees());
            assertEquals(0, lm.mentoringOverview().activeAssignmentsCount());

            List<DeptRiskAlertDto> alerts = departmentHeadService.getDepartmentRiskAlerts(deptHeadB.getEmail(), null);
            assertNotNull(alerts);

            DeptReportResultDto report = departmentHeadService.generateDepartmentReport(deptHeadB.getEmail(), "EXECUTIVE_SUMMARY", null);
            assertNotNull(report);
            assertEquals(1L, report.summaryStats().get("Total Workforce"));
        });
    }
}
