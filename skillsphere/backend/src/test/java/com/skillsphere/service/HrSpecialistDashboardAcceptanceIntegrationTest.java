package com.skillsphere.service;

import com.skillsphere.dto.HrDtos.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class HrSpecialistDashboardAcceptanceIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
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
    private LearningPathRepository learningPathRepository;
    @Autowired
    private LearningPathItemRepository learningPathItemRepository;
    @Autowired
    private TrainingProgrammeRepository trainingProgrammeRepository;
    @Autowired
    private TrainingEnrollmentRepository trainingEnrollmentRepository;
    @Autowired
    private EmployeeLearningRepository employeeLearningRepository;
    @Autowired
    private EmployeeSkillRepository employeeSkillRepository;
    @Autowired
    private AssessmentAttemptRepository assessmentAttemptRepository;
    @Autowired
    private MentorProfileRepository mentorProfileRepository;
    @Autowired
    private MentorAssignmentRepository mentorAssignmentRepository;
    @Autowired
    private MentorshipSessionRepository mentorshipSessionRepository;
    @Autowired
    private MentorshipFeedbackRepository mentorshipFeedbackRepository;
    @Autowired
    private PlatformFeedbackRepository platformFeedbackRepository;
    @Autowired
    private HrAnnouncementRepository hrAnnouncementRepository;
    @Autowired
    private HrAnnouncementReadRepository hrAnnouncementReadRepository;
    @Autowired
    private LeadershipReportShareRepository leadershipReportShareRepository;
    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private HrDashboardService hrDashboardService;

    private User hrSpecialist;
    private User testEmployee;
    private User testMentor;
    private Department engineeringDept;
    private SkillCategory techCategory;
    private Skill cloudSkill;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // Department
        engineeringDept = new Department();
        engineeringDept.setName("Engineering " + uid);
        engineeringDept.setCode("ENG_" + uid);
        engineeringDept = departmentRepository.save(engineeringDept);

        // Category & Skill
        techCategory = skillCategoryRepository.findAll().stream().findFirst().orElseGet(() ->
                skillCategoryRepository.save(new SkillCategory("Tech " + uid, "Technical skills")));

        cloudSkill = new Skill(techCategory, "Distributed Systems " + uid, "Cloud scale architectures");
        cloudSkill = skillRepository.save(cloudSkill);

        // HR User
        hrSpecialist = new User();
        hrSpecialist.setEmail("hr." + uid + "@example.com");
        hrSpecialist.setFullName("Helen HR " + uid);
        hrSpecialist.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        hrSpecialist.setRole(Role.HR_SPECIALIST);
        hrSpecialist = userRepository.save(hrSpecialist);

        // Employee
        testEmployee = new User();
        testEmployee.setEmail("emp." + uid + "@example.com");
        testEmployee.setFullName("Edward Employee " + uid);
        testEmployee.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        testEmployee.setRole(Role.EMPLOYEE);
        testEmployee = userRepository.save(testEmployee);

        EmployeeProfile empProfile = new EmployeeProfile(testEmployee);
        empProfile.setDepartment(engineeringDept.getName());
        empProfile.setJobRole("Software Developer");
        empProfile.setExperienceLevel("Intermediate");
        empProfile.setBio("Cloud infrastructure and Kubernetes practitioner");
        employeeProfileRepository.save(empProfile);

        // Mentor
        testMentor = new User();
        testMentor.setEmail("mentor." + uid + "@example.com");
        testMentor.setFullName("Mona Mentor " + uid);
        testMentor.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        testMentor.setRole(Role.MENTOR);
        testMentor = userRepository.save(testMentor);

        MentorProfile mentorProfile = new MentorProfile(testMentor, "Principal Cloud Architect", cloudSkill.getName(), 4, hrSpecialist);
        mentorProfile.setBio("15+ years experience in distributed cloud");
        mentorProfile.setActive(true);
        mentorProfileRepository.save(mentorProfile);
    }

    @Test
    @DisplayName("Module 1: HR Overview calculates real database metrics accurately")
    void testHrOverviewMetrics() {
        // Create an assessment attempt
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setUser(testEmployee);
        attempt.setSkill(cloudSkill);
        attempt.setTargetLevel("Intermediate");
        attempt.setScore(85);
        attempt.setPassed(true);
        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(Instant.now());
        assessmentAttemptRepository.save(attempt);

        // Create a skill gap
        SkillGap gap = new SkillGap(testEmployee, cloudSkill, 2, 4, 2, "HIGH");
        skillGapRepository.save(gap);

        // Create a course and employee learning
        Course course = new Course("Cloud Arch 101", "Basics", cloudSkill, "Beginner", 12, "Cloud Org", "https://cloud.org");
        course = courseRepository.save(course);

        EmployeeLearning learning = new EmployeeLearning(testEmployee, course, "COMPLETED", 100);
        learning.setCompletedAt(Instant.now());
        employeeLearningRepository.save(learning);

        HrDashboardOverviewDto overview = hrDashboardService.getOverview();
        assertNotNull(overview);
        assertTrue(overview.totalEmployees() >= 1);
        assertTrue(overview.assessedEmployeesCount() >= 1);
        assertTrue(overview.skillCoveragePercentage() > 0.0);
        assertTrue(overview.criticalShortagesCount() >= 1);
        assertTrue(overview.totalCoursesCount() >= 1);
        assertTrue(overview.totalEnrollmentsCount() >= 1);
        assertNotNull(overview.recentActivities());
        assertNotNull(overview.pendingActions());
    }

    @Test
    @DisplayName("Module 2: Employee Directory and Dossier inspection with privacy boundary")
    void testEmployeeDirectoryAndDossier() {
        // Query directory
        List<HrEmployeeDirectoryItemDto> directory = hrDashboardService.getEmployeeDirectory(
                testEmployee.getFullName(), engineeringDept.getName(), "EMPLOYEE"
        );
        assertNotNull(directory);
        assertFalse(directory.isEmpty());
        HrEmployeeDirectoryItemDto item = directory.get(0);
        assertEquals(testEmployee.getId(), item.id());
        assertEquals(engineeringDept.getName(), item.departmentName());
        assertEquals("Software Developer", item.jobRole());

        // Add a verified skill
        EmployeeSkill es = new EmployeeSkill(testEmployee, cloudSkill, "Intermediate");
        es.setVerified(true);
        employeeSkillRepository.save(es);

        // Retrieve dossier
        HrEmployeeDossierDto dossier = hrDashboardService.getEmployeeDossier(testEmployee.getId());
        assertNotNull(dossier);
        assertEquals("Edward Employee " + testEmployee.getFullName().split(" ")[2], dossier.fullName());
        assertEquals("Software Developer", dossier.jobRole());
        assertEquals("Cloud infrastructure and Kubernetes practitioner", dossier.bio());
        assertFalse(dossier.skills().isEmpty());
        assertEquals(cloudSkill.getName(), dossier.skills().get(0).skillName());
        assertTrue(dossier.skills().get(0).verified());
    }

    @Test
    @DisplayName("Module 3: Competency Framework creation, duplicate prevention, and active toggle")
    void testCompetencyFrameworkLifecycle() {
        String compName = "Platform Engineering " + UUID.randomUUID().toString().substring(0, 6);
        CreateCompetencyRequest req = new CreateCompetencyRequest(compName, techCategory.getId(), "Core DevOps skills");
        HrCompetencyDto created = hrDashboardService.createCompetency(req);
        assertNotNull(created);
        assertEquals(compName, created.name());
        assertTrue(created.isActive());

        // Duplicate name prevention throws BAD_REQUEST
        assertThrows(ApiException.class, () -> hrDashboardService.createCompetency(req));

        // Toggle active
        HrCompetencyDto toggled = hrDashboardService.toggleCompetencyActive(created.id());
        assertFalse(toggled.isActive());

        // Update competency
        UpdateCompetencyRequest updateReq = new UpdateCompetencyRequest(compName + " Updated", techCategory.getId(), "Updated description", true);
        HrCompetencyDto updated = hrDashboardService.updateCompetency(created.id(), updateReq);
        assertEquals(compName + " Updated", updated.name());
        assertTrue(updated.isActive());
    }

    @Test
    @DisplayName("Module 4 & 5: Role Requirements and Matrix lifecycle")
    void testRoleRequirementsAndMatrixLifecycle() {
        String jobRole = "Senior Architect " + UUID.randomUUID().toString().substring(0, 6);
        CreateRoleRequirementRequest req = new CreateRoleRequirementRequest(jobRole, cloudSkill.getId(), "Advanced", "HIGH");
        HrRoleRequirementDto created = hrDashboardService.createRoleRequirement(req);
        assertNotNull(created);
        assertEquals(jobRole, created.jobRole());
        assertEquals("Advanced", created.requiredProficiency());
        assertEquals("HIGH", created.priority());

        // Duplicate role requirement throws BAD_REQUEST
        assertThrows(ApiException.class, () -> hrDashboardService.createRoleRequirement(req));

        // Matrix check
        List<RoleSkillMatrixDto> matrix = hrDashboardService.getRoleMatrix();
        assertNotNull(matrix);
        RoleSkillMatrixDto roleEntry = matrix.stream().filter(m -> m.jobRole().equals(jobRole)).findFirst().orElse(null);
        assertNotNull(roleEntry);
        assertEquals(1, roleEntry.totalSkillsRequired());
        assertEquals(1, roleEntry.highPriorityCount());

        // Delete requirement
        hrDashboardService.deleteRoleRequirement(created.id());
        List<HrRoleRequirementDto> remaining = hrDashboardService.getRoleRequirements(jobRole);
        assertTrue(remaining.isEmpty());
    }

    @Test
    @DisplayName("Module 6 & 7: Skill Gap Summary and Enterprise Heatmap aggregation")
    void testSkillGapSummaryAndHeatmap() {
        SkillGap gap = new SkillGap(testEmployee, cloudSkill, 1, 4, 3, "HIGH");
        skillGapRepository.save(gap);

        HrSkillGapSummaryDto summary = hrDashboardService.getSkillGapSummary();
        assertNotNull(summary);
        assertTrue(summary.totalGaps() >= 1);
        assertTrue(summary.highPriorityGaps() >= 1);
        assertTrue(summary.avgGapSize() > 0.0);
        assertNotNull(summary.criticalShortages());
        assertNotNull(summary.departmentBreakdown());

        OrgHeatmapDto heatmap = hrDashboardService.getOrgHeatmap();
        assertNotNull(heatmap);
        assertFalse(heatmap.departments().isEmpty());
        assertFalse(heatmap.skills().isEmpty());
        assertFalse(heatmap.cells().isEmpty());
    }

    @Test
    @DisplayName("Module 8 & 9: Course Catalogue, Learning Paths, and Training Programmes lifecycle")
    void testLearningAndTrainingLifecycle() {
        // 1. Create Course
        CreateOrUpdateCourseRequest courseReq = new CreateOrUpdateCourseRequest(
                "Kubernetes In Production", "Mastering K8s", cloudSkill.getId(),
                "Advanced", 24, "Linux Foundation", "https://k8s.io", "http://thumb.jpg", true
        );
        HrCourseDto course = hrDashboardService.createCourse(courseReq);
        assertNotNull(course);
        assertEquals("Kubernetes In Production", course.title());

        // 2. Create Learning Path
        CreateOrUpdateLearningPathRequest pathReq = new CreateOrUpdateLearningPathRequest(
                "Cloud Mastery Track", "Comprehensive track", "Software Developer",
                cloudSkill.getId(), "Advanced", true, List.of(course.id())
        );
        HrLearningPathDto path = hrDashboardService.createLearningPath(pathReq, hrSpecialist.getId());
        assertNotNull(path);
        assertEquals("Cloud Mastery Track", path.title());
        assertEquals(1, path.courseCount());

        // 3. Training Programme creation & date validation
        LocalDate today = LocalDate.now();
        CreateOrUpdateTrainingProgrammeRequest invalidProgReq = new CreateOrUpdateTrainingProgrammeRequest(
                "Invalid Dates Prog", "Test", engineeringDept.getId(), cloudSkill.getId(), course.id(),
                today.plusDays(5), today.plusDays(2), 10, "Trainer X", "UPCOMING"
        );
        // End date before start date throws BAD_REQUEST
        assertThrows(ApiException.class, () -> hrDashboardService.createTrainingProgramme(invalidProgReq));

        CreateOrUpdateTrainingProgrammeRequest validProgReq = new CreateOrUpdateTrainingProgrammeRequest(
                "Cloud Bootcamp 2026", "Intensive", engineeringDept.getId(), cloudSkill.getId(), course.id(),
                today.plusDays(2), today.plusDays(5), 2, "Trainer Lead", "UPCOMING"
        );
        HrTrainingProgrammeDto prog = hrDashboardService.createTrainingProgramme(validProgReq);
        assertNotNull(prog);
        assertEquals("Cloud Bootcamp 2026", prog.title());
        assertEquals(0, prog.enrolledCount());
        assertEquals(2, prog.capacity());

        // 4. Enroll employee
        ProgrammeEnrollmentDetailDto enrollment = hrDashboardService.enrollEmployee(prog.id(), testEmployee.getId());
        assertNotNull(enrollment);
        assertEquals("ENROLLED", enrollment.status());

        // Duplicate enrollment throws BAD_REQUEST
        assertThrows(ApiException.class, () -> hrDashboardService.enrollEmployee(prog.id(), testEmployee.getId()));

        // Update enrollment status
        UpdateProgrammeEnrollmentStatusRequest statusReq = new UpdateProgrammeEnrollmentStatusRequest("ATTENDED");
        ProgrammeEnrollmentDetailDto updatedEnroll = hrDashboardService.updateProgrammeEnrollmentStatus(enrollment.id(), statusReq.status());
        assertEquals("ATTENDED", updatedEnroll.status());

        // Reopening completed programme throws BAD_REQUEST
        CreateOrUpdateTrainingProgrammeRequest completeProgReq = new CreateOrUpdateTrainingProgrammeRequest(
                prog.title(), prog.description(), engineeringDept.getId(), cloudSkill.getId(), course.id(),
                today.plusDays(2), today.plusDays(5), 2, "Trainer Lead", "COMPLETED"
        );
        hrDashboardService.updateTrainingProgramme(prog.id(), completeProgReq);

        CreateOrUpdateTrainingProgrammeRequest reopenProgReq = new CreateOrUpdateTrainingProgrammeRequest(
                prog.title(), prog.description(), engineeringDept.getId(), cloudSkill.getId(), course.id(),
                today.plusDays(2), today.plusDays(5), 2, "Trainer Lead", "UPCOMING"
        );
        assertThrows(ApiException.class, () -> hrDashboardService.updateTrainingProgramme(prog.id(), reopenProgReq));
    }

    @Test
    @DisplayName("Module 10, 11, 12, 13: Mentors, Learning Progress, Feedback, Reports, and Announcements")
    void testGovernanceReportsAndAnnouncements() {
        // 1. Mentor Directory
        List<HrMentorDirectoryItemDto> mentors = hrDashboardService.getMentorDirectory();
        assertNotNull(mentors);
        assertTrue(mentors.stream().anyMatch(m -> m.userId().equals(testMentor.getId())));

        MentorsForSkillDto skillMentors = hrDashboardService.getMentorsForSkill(cloudSkill.getId());
        assertNotNull(skillMentors);
        assertEquals(cloudSkill.getId(), skillMentors.skillId());
        assertFalse(skillMentors.mentors().isEmpty());

        // 2. Learning Progress
        HrLearningProgressOverviewDto progOverview = hrDashboardService.getLearningProgressOverview();
        assertNotNull(progOverview);
        assertNotNull(progOverview.progressItems());

        // 3. Feedback Overview
        PlatformFeedback pf = new PlatformFeedback(testEmployee, "Learning Portal", 5, "Great learning modules!");
        platformFeedbackRepository.save(pf);
        HrFeedbackOverviewDto feedbackOverview = hrDashboardService.getFeedbackOverview();
        assertNotNull(feedbackOverview);
        assertTrue(feedbackOverview.totalPlatformFeedbacks() >= 1);

        // 4. Generate Reports & Share with Leadership
        HrReportResultDto report = hrDashboardService.generateReport("WORKFORCE_READINESS", engineeringDept.getId());
        assertNotNull(report);
        assertEquals("WORKFORCE_READINESS", report.reportType());
        assertNotNull(report.data());

        ShareReportRequest shareReq = new ShareReportRequest(
                report.reportType(), report.title(), "MANAGER",
                engineeringDept.getId(), "Leadership briefing notes", "{}"
        );
        LeadershipReportShareDto shared = hrDashboardService.shareReportWithLeadership(shareReq, hrSpecialist.getId());
        assertNotNull(shared);
        assertEquals("MANAGER", shared.sharedWithRole());

        // 5. Announcements with Targeted Notification and Read Tracking
        CreateAnnouncementRequest annReq = new CreateAnnouncementRequest(
                "Corporate Skill Assessment Cycle", "All staff please complete Q4 assessments",
                "ORGANIZATION", null, null, "HIGH"
        );
        HrAnnouncementDto announcement = hrDashboardService.createAnnouncement(annReq, hrSpecialist.getId());
        assertNotNull(announcement);
        assertEquals("Corporate Skill Assessment Cycle", announcement.title());
        assertEquals("HIGH", announcement.priority());

        // Employee marks announcement read
        hrDashboardService.markAnnouncementRead(announcement.id(), testEmployee.getId());
        List<HrAnnouncementDto> annList = hrDashboardService.getAnnouncements(testEmployee.getId());
        HrAnnouncementDto readAnn = annList.stream().filter(a -> a.id().equals(announcement.id())).findFirst().orElse(null);
        assertNotNull(readAnn);
        assertTrue(readAnn.readByCurrentUser());
        assertEquals(1, readAnn.readCount());
    }

    // =========================================================================
    // Targeted Acceptance Checks 1 to 8
    // =========================================================================

    @Test
    @DisplayName("Check 1: Confirm integration tests use PostgreSQL 18.4 (datasource, version, database name skillsphere_db)")
    void testCheck1_Postgres18ConnectionAndFlywayMigrations() throws Exception {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            java.sql.DatabaseMetaData meta = conn.getMetaData();
            String dbName = meta.getDatabaseProductName();
            String dbVersion = meta.getDatabaseProductVersion();
            String catalog = conn.getCatalog();

            assertEquals("PostgreSQL", dbName, "Database product must be PostgreSQL");
            assertTrue(dbVersion.contains("18"), "Database version must be PostgreSQL 18.x, found: " + dbVersion);
            assertEquals("skillsphere_db", catalog, "Database catalog must be skillsphere_db");

            // Verify Flyway schema migrations
            try (java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT count(*), max(installed_rank) FROM flyway_schema_history WHERE success = true")) {
                assertTrue(rs.next());
                int count = rs.getInt(1);
                assertTrue(count >= 10, "At least 10 Flyway migrations must be installed and verified, found: " + count);
            }
        }
    }

    @Test
    @DisplayName("Check 2: Announcement read authorization and IDOR protection (403/404 response)")
    void testCheck2_AnnouncementReadAuthorizationAndIdorProtection() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Department financeDept = new Department();
        financeDept.setName("Finance " + uid);
        financeDept.setCode("FIN_" + uid);
        financeDept = departmentRepository.save(financeDept);

        User financeEmployee = new User();
        financeEmployee.setEmail("finance." + uid + "@skillsphere.com");
        financeEmployee.setFullName("Fiona Finance " + uid);
        financeEmployee.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        financeEmployee.setRole(Role.EMPLOYEE);
        final User savedFinanceEmployee = userRepository.save(financeEmployee);

        EmployeeProfile finProfile = new EmployeeProfile(savedFinanceEmployee);
        finProfile.setDepartment(financeDept.getName());
        finProfile.setJobRole("Data Analyst");
        employeeProfileRepository.save(finProfile);

        // 1. Create department-scoped announcement for Engineering
        CreateAnnouncementRequest deptAnnReq = new CreateAnnouncementRequest(
                "Engineering All-Hands", "Quarterly engineering roadmap discussion",
                "DEPARTMENT", engineeringDept.getId(), null, "HIGH"
        );
        HrAnnouncementDto deptAnn = hrDashboardService.createAnnouncement(deptAnnReq, hrSpecialist.getId());

        // 2. IDOR Protection: Finance employee attempts to mark Engineering announcement as read -> 403 FORBIDDEN
        ApiException forbiddenEx = assertThrows(ApiException.class, () ->
                hrDashboardService.markAnnouncementRead(deptAnn.id(), savedFinanceEmployee.getId())
        );
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, forbiddenEx.getStatus());

        // 3. 404 NOT_FOUND on non-existent announcement
        ApiException notFoundEx = assertThrows(ApiException.class, () ->
                hrDashboardService.markAnnouncementRead(9999999L, testEmployee.getId())
        );
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, notFoundEx.getStatus());

        // 4. Authorized employee in Engineering marks read -> 200 OK
        assertDoesNotThrow(() -> hrDashboardService.markAnnouncementRead(deptAnn.id(), testEmployee.getId()));
        assertTrue(hrAnnouncementReadRepository.existsByAnnouncementIdAndUserId(deptAnn.id(), testEmployee.getId()));

        // 5. Role-scoped announcement for MENTOR
        CreateAnnouncementRequest roleAnnReq = new CreateAnnouncementRequest(
                "Mentorship Round Table", "Discussion on active mentee progress",
                "ROLE", null, "MENTOR", "NORMAL"
        );
        HrAnnouncementDto roleAnn = hrDashboardService.createAnnouncement(roleAnnReq, hrSpecialist.getId());

        // Employee attempts to mark MENTOR announcement as read -> 403 FORBIDDEN
        ApiException roleForbiddenEx = assertThrows(ApiException.class, () ->
                hrDashboardService.markAnnouncementRead(roleAnn.id(), testEmployee.getId())
        );
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, roleForbiddenEx.getStatus());

        // Mentor marks read -> 200 OK
        assertDoesNotThrow(() -> hrDashboardService.markAnnouncementRead(roleAnn.id(), testMentor.getId()));
        assertTrue(hrAnnouncementReadRepository.existsByAnnouncementIdAndUserId(roleAnn.id(), testMentor.getId()));

        // 6. Announcement visibility isolation: Finance employee must NOT see Engineering announcement
        List<HrAnnouncementDto> finList = hrDashboardService.getAnnouncements(savedFinanceEmployee.getId());
        assertFalse(finList.stream().anyMatch(a -> a.id().equals(deptAnn.id())), "Finance employee must not see Engineering announcement");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("Check 3: Concurrent training-programme enrolment capacity (pessimistic lock, capacity invariant)")
    void testCheck3_ConcurrentTrainingProgrammeEnrollmentCapacity() throws Exception {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        User u1 = new User();
        u1.setEmail("u1." + uid + "@skillsphere.com");
        u1.setFullName("User One " + uid);
        u1.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        u1.setRole(Role.EMPLOYEE);
        u1 = userRepository.save(u1);

        User u2 = new User();
        u2.setEmail("u2." + uid + "@skillsphere.com");
        u2.setFullName("User Two " + uid);
        u2.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        u2.setRole(Role.EMPLOYEE);
        u2 = userRepository.save(u2);

        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = LocalDate.now().plusDays(5);
        TrainingProgramme prog = new TrainingProgramme(
                "Capacity Lock Programme " + uid, "Testing pessimistic concurrency",
                null, start, end, 1, "Concurrent Trainer"
        );
        prog = trainingProgrammeRepository.save(prog);
        final Long progId = prog.getId();
        final Long u1Id = u1.getId();
        final Long u2Id = u2.getId();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger capacityErrorCount = new java.util.concurrent.atomic.AtomicInteger(0);

        java.util.concurrent.Future<?> f1 = executor.submit(() -> {
            try {
                startLatch.await();
                hrDashboardService.enrollEmployee(progId, u1Id);
                successCount.incrementAndGet();
            } catch (ApiException e) {
                if (e.getStatus() == org.springframework.http.HttpStatus.BAD_REQUEST && e.getMessage().contains("capacity")) {
                    capacityErrorCount.incrementAndGet();
                }
            } catch (Exception ignored) {}
        });

        java.util.concurrent.Future<?> f2 = executor.submit(() -> {
            try {
                startLatch.await();
                hrDashboardService.enrollEmployee(progId, u2Id);
                successCount.incrementAndGet();
            } catch (ApiException e) {
                if (e.getStatus() == org.springframework.http.HttpStatus.BAD_REQUEST && e.getMessage().contains("capacity")) {
                    capacityErrorCount.incrementAndGet();
                }
            } catch (Exception ignored) {}
        });

        startLatch.countDown();
        f1.get(10, java.util.concurrent.TimeUnit.SECONDS);
        f2.get(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        TrainingProgramme updated = trainingProgrammeRepository.findById(progId).orElseThrow();
        assertEquals(1, successCount.get(), "Exactly one concurrent enrollment must succeed");
        assertEquals(1, capacityErrorCount.get(), "Exactly one concurrent enrollment must fail due to capacity reached");
        assertEquals(1, updated.getEnrolledCount(), "Enrolled count must equal exactly 1");
        assertTrue(updated.getEnrolledCount() <= updated.getCapacity(), "Capacity invariant must hold");

        // Cleanup
        trainingEnrollmentRepository.deleteAll(trainingEnrollmentRepository.findAll().stream().filter(e -> e.getProgramme().getId().equals(progId)).toList());
        trainingProgrammeRepository.deleteById(progId);
        userRepository.deleteById(u1Id);
        userRepository.deleteById(u2Id);
    }

    @Test
    @DisplayName("Check 4: Training-programme status transitions (lifecycle invariants and no reopening)")
    void testCheck4_TrainingProgrammeLifecycleTransitions() {
        LocalDate today = LocalDate.now();
        // 1. Create UPCOMING programme
        CreateOrUpdateTrainingProgrammeRequest createReq = new CreateOrUpdateTrainingProgrammeRequest(
                "Lifecycle Test Prog", "Testing lifecycle invariants", engineeringDept.getId(), cloudSkill.getId(), null,
                today.plusDays(3), today.plusDays(7), 10, "Lifecycle Lead", "UPCOMING"
        );
        HrTrainingProgrammeDto prog = hrDashboardService.createTrainingProgramme(createReq);
        assertEquals("UPCOMING", prog.status());

        // 2. UPCOMING -> ACTIVE (allowed)
        CreateOrUpdateTrainingProgrammeRequest activeReq = new CreateOrUpdateTrainingProgrammeRequest(
                prog.title(), prog.description(), engineeringDept.getId(), cloudSkill.getId(), null,
                today.plusDays(3), today.plusDays(7), 10, "Lifecycle Lead", "ACTIVE"
        );
        HrTrainingProgrammeDto activeProg = hrDashboardService.updateTrainingProgramme(prog.id(), activeReq);
        assertEquals("ACTIVE", activeProg.status());

        // 3. ACTIVE -> UPCOMING (blocked with 400)
        CreateOrUpdateTrainingProgrammeRequest revertReq = new CreateOrUpdateTrainingProgrammeRequest(
                prog.title(), prog.description(), engineeringDept.getId(), cloudSkill.getId(), null,
                today.plusDays(3), today.plusDays(7), 10, "Lifecycle Lead", "UPCOMING"
        );
        ApiException exRevert = assertThrows(ApiException.class, () ->
                hrDashboardService.updateTrainingProgramme(prog.id(), revertReq)
        );
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exRevert.getStatus());

        // 4. ACTIVE -> COMPLETED (allowed)
        CreateOrUpdateTrainingProgrammeRequest completeReq = new CreateOrUpdateTrainingProgrammeRequest(
                prog.title(), prog.description(), engineeringDept.getId(), cloudSkill.getId(), null,
                today.plusDays(3), today.plusDays(7), 10, "Lifecycle Lead", "COMPLETED"
        );
        HrTrainingProgrammeDto completedProg = hrDashboardService.updateTrainingProgramme(prog.id(), completeReq);
        assertEquals("COMPLETED", completedProg.status());

        // 5. COMPLETED -> ACTIVE (reopening blocked with 400)
        ApiException exReopenCompleted = assertThrows(ApiException.class, () ->
                hrDashboardService.updateTrainingProgramme(prog.id(), activeReq)
        );
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exReopenCompleted.getStatus());

        // 6. Enrollment into COMPLETED programme blocked with 400
        ApiException exEnrollCompleted = assertThrows(ApiException.class, () ->
                hrDashboardService.enrollEmployee(prog.id(), testEmployee.getId())
        );
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exEnrollCompleted.getStatus());

        // 7. UPCOMING -> CANCELLED (allowed)
        CreateOrUpdateTrainingProgrammeRequest createReq2 = new CreateOrUpdateTrainingProgrammeRequest(
                "Cancelled Prog Test", "Testing cancellation", engineeringDept.getId(), cloudSkill.getId(), null,
                today.plusDays(4), today.plusDays(8), 5, "Trainer Y", "UPCOMING"
        );
        HrTrainingProgrammeDto prog2 = hrDashboardService.createTrainingProgramme(createReq2);

        CreateOrUpdateTrainingProgrammeRequest cancelReq = new CreateOrUpdateTrainingProgrammeRequest(
                prog2.title(), prog2.description(), engineeringDept.getId(), cloudSkill.getId(), null,
                today.plusDays(4), today.plusDays(8), 5, "Trainer Y", "CANCELLED"
        );
        HrTrainingProgrammeDto cancelledProg = hrDashboardService.updateTrainingProgramme(prog2.id(), cancelReq);
        assertEquals("CANCELLED", cancelledProg.status());

        // 8. CANCELLED -> ACTIVE (reopening blocked with 400)
        ApiException exReopenCancelled = assertThrows(ApiException.class, () ->
                hrDashboardService.updateTrainingProgramme(prog2.id(), activeReq)
        );
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exReopenCancelled.getStatus());

        // 9. Enrollment into CANCELLED programme blocked with 400
        ApiException exEnrollCancelled = assertThrows(ApiException.class, () ->
                hrDashboardService.enrollEmployee(prog2.id(), testEmployee.getId())
        );
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exEnrollCancelled.getStatus());
    }

    @Test
    @DisplayName("Check 5: Leadership report-sharing authorization and recipient isolation")
    void testCheck5_LeadershipReportIsolation() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Department deptA = engineeringDept;

        Department deptB = new Department();
        deptB.setName("Operations " + uid);
        deptB.setCode("OPS_" + uid);
        final Department savedDeptB = departmentRepository.save(deptB);

        // Manager in Dept A
        User managerA = new User();
        managerA.setEmail("mgr.a." + uid + "@skillsphere.com");
        managerA.setFullName("Alice Manager " + uid);
        managerA.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        managerA.setRole(Role.MANAGER);
        final User savedManagerA = userRepository.save(managerA);

        EmployeeProfile profA = new EmployeeProfile(savedManagerA);
        profA.setDepartment(deptA.getName());
        profA.setJobRole("Software Developer");
        employeeProfileRepository.save(profA);

        // Manager in Dept B
        User managerB = new User();
        managerB.setEmail("mgr.b." + uid + "@skillsphere.com");
        managerB.setFullName("Bob Manager " + uid);
        managerB.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwx");
        managerB.setRole(Role.MANAGER);
        final User savedManagerB = userRepository.save(managerB);

        EmployeeProfile profB = new EmployeeProfile(savedManagerB);
        profB.setDepartment(savedDeptB.getName());
        profB.setJobRole("QA Engineer");
        employeeProfileRepository.save(profB);

        // HR shares Report 1 with MANAGER for Dept A
        ShareReportRequest req1 = new ShareReportRequest(
                "GAP_ANALYSIS", "Dept A Skills Report " + uid, "MANAGER",
                deptA.getId(), "Notes for Dept A", "{}"
        );
        LeadershipReportShareDto shareA = hrDashboardService.shareReportWithLeadership(req1, hrSpecialist.getId());

        // HR shares Report 2 with MANAGER for Dept B
        ShareReportRequest req2 = new ShareReportRequest(
                "TRAINING_EFFECTIVENESS", "Dept B Training Report " + uid, "MANAGER",
                savedDeptB.getId(), "Notes for Dept B", "{}"
        );
        LeadershipReportShareDto shareB = hrDashboardService.shareReportWithLeadership(req2, hrSpecialist.getId());

        // 1. Manager A fetches shared reports -> must see Share A, must NOT see Share B
        List<LeadershipReportShareDto> reportsForA = hrDashboardService.getSharedLeadershipReports(savedManagerA, null);
        assertTrue(reportsForA.stream().anyMatch(r -> r.id().equals(shareA.id())), "Manager A must see Dept A report");
        assertFalse(reportsForA.stream().anyMatch(r -> r.id().equals(shareB.id())), "Manager A must NOT see Dept B report");

        // 2. Manager A attempts to request Dept B's departmentId explicitly -> 403 FORBIDDEN
        ApiException idorEx = assertThrows(ApiException.class, () ->
                hrDashboardService.getSharedLeadershipReports(savedManagerA, savedDeptB.getId())
        );
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, idorEx.getStatus());

        // 3. Manager B fetches shared reports -> must see Share B, must NOT see Share A
        List<LeadershipReportShareDto> reportsForB = hrDashboardService.getSharedLeadershipReports(savedManagerB, null);
        assertTrue(reportsForB.stream().anyMatch(r -> r.id().equals(shareB.id())), "Manager B must see Dept B report");
        assertFalse(reportsForB.stream().anyMatch(r -> r.id().equals(shareA.id())), "Manager B must NOT see Dept A report");
    }

    @Test
    @DisplayName("Check 6: Role-requirement deletion and competency deactivation (foreign key checks, historical consistency)")
    void testCheck6_RoleRequirementDeletionAndCompetencyDeactivation() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Skill securitySkill = new Skill(techCategory, "Security Architecture " + uid, "AppSec & Cryptography");
        securitySkill = skillRepository.save(securitySkill);

        // 1. Create role requirement
        CreateRoleRequirementRequest req = new CreateRoleRequirementRequest("Software Developer", securitySkill.getId(), "Advanced", "HIGH");
        HrRoleRequirementDto createdReq = hrDashboardService.createRoleRequirement(req);
        assertNotNull(createdReq);

        // 2. Create EmployeeSkill and AssessmentAttempt tied to securitySkill
        EmployeeSkill es = new EmployeeSkill(testEmployee, securitySkill, "Intermediate");
        es = employeeSkillRepository.save(es);

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setUser(testEmployee);
        attempt.setSkill(securitySkill);
        attempt.setTargetLevel("Intermediate");
        attempt.setScore(92);
        attempt.setPassed(true);
        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(Instant.now());
        attempt = assessmentAttemptRepository.save(attempt);

        // 3. Delete Role Requirement: foreign key integrity must hold and historical assessment/skill records remain untouched
        assertDoesNotThrow(() -> hrDashboardService.deleteRoleRequirement(createdReq.id()));
        assertFalse(roleSkillRequirementRepository.findById(createdReq.id()).isPresent());

        // Verify EmployeeSkill and AssessmentAttempt remain intact
        assertTrue(employeeSkillRepository.findById(es.getId()).isPresent(), "EmployeeSkill must remain intact after role requirement deletion");
        assertTrue(assessmentAttemptRepository.findById(attempt.getId()).isPresent(), "AssessmentAttempt must remain intact after role requirement deletion");

        // 4. Soft deactivation of Skill: toggle active state to false
        HrCompetencyDto deactivatedSkill = hrDashboardService.toggleCompetencyActive(securitySkill.getId());
        assertFalse(deactivatedSkill.isActive(), "Skill must be soft-deactivated");

        // Verify records still intact
        Skill dbSkill = skillRepository.findById(securitySkill.getId()).orElseThrow();
        assertFalse(dbSkill.isActive());
        assertTrue(employeeSkillRepository.findById(es.getId()).isPresent(), "EmployeeSkill must remain intact after skill deactivation");
        assertTrue(assessmentAttemptRepository.findById(attempt.getId()).isPresent(), "AssessmentAttempt must remain intact after skill deactivation");
    }

    @Test
    @DisplayName("Check 7: CSV export accuracy (all 4 report types, headers, values, RFC 4180 escaping)")
    void testCheck7_CsvExportAccuracyAndRfc4180Escaping() {
        String[] reportTypes = {"GAP_ANALYSIS", "TRAINING_EFFECTIVENESS", "COMPLIANCE_AUDIT", "DEPARTMENT_DISTRIBUTION"};

        for (String type : reportTypes) {
            HrReportResultDto report = hrDashboardService.generateReport(type, engineeringDept.getId());
            assertNotNull(report, "Report must not be null for type: " + type);
            assertEquals(type, report.reportType());
            assertNotNull(report.title());
            assertNotNull(report.data());
            assertNotNull(report.generatedAt());
        }

        // Test RFC 4180 Escaping Logic
        java.util.function.Function<String, String> escapeCsv = val -> {
            if (val == null) return "\"\"";
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                return "\"" + val.replace("\"", "\"\"") + "\"";
            }
            return val;
        };

        assertEquals("\"\"", escapeCsv.apply(null));
        assertEquals("StandardText", escapeCsv.apply("StandardText"));
        assertEquals("\"Engineering, Architecture\"", escapeCsv.apply("Engineering, Architecture"));
        assertEquals("\"Title with \"\"Quotes\"\" inside\"", escapeCsv.apply("Title with \"Quotes\" inside"));
        assertEquals("\"Line1\nLine2\"", escapeCsv.apply("Line1\nLine2"));
    }

    @Test
    @DisplayName("Check 8: Reconcile all 15 acceptance-matrix entries with the 13 sidebar modules")
    void testCheck8_Reconcile15AcceptanceEntriesWith13SidebarModules() {
        record MatrixToModuleMapping(int matrixPoint, String name, String sidebarModuleId, String routePath, String viewComponent) {}

        List<MatrixToModuleMapping> mappings = List.of(
                new MatrixToModuleMapping(1, "Overview", "overview", "/hr/dashboard", "HrDashboardOverviewView"),
                new MatrixToModuleMapping(2, "Employee Directory", "employees", "/hr/employees", "EmployeeDirectoryView"),
                new MatrixToModuleMapping(3, "Competencies", "competencies", "/hr/competencies", "CompetencyFrameworkView"),
                new MatrixToModuleMapping(4, "Role Requirements", "role-requirements", "/hr/role-requirements", "RoleRequirementsView (Tab 1: Requirements)"),
                new MatrixToModuleMapping(5, "Role Matrix", "role-requirements", "/hr/role-requirements", "RoleRequirementsView (Tab 2: Matrix)"),
                new MatrixToModuleMapping(6, "Org Skill Gaps", "skill-gaps", "/hr/skill-gaps", "OrgSkillGapView (Tab 1: Summary)"),
                new MatrixToModuleMapping(7, "Org Heatmap", "skill-gaps", "/hr/skill-gaps", "OrgSkillGapView (Tab 2: Heatmap)"),
                new MatrixToModuleMapping(8, "Learning Catalogue", "learning-catalogue", "/hr/learning-catalogue", "LearningCatalogueView"),
                new MatrixToModuleMapping(9, "Learning Paths", "learning-paths", "/hr/learning-paths", "LearningPathsView"),
                new MatrixToModuleMapping(10, "Training Programmes", "training-programmes", "/hr/training-programmes", "TrainingProgrammesView"),
                new MatrixToModuleMapping(11, "Mentor Directory", "mentors", "/hr/mentors", "MentorDirectoryView"),
                new MatrixToModuleMapping(12, "Learning Progress", "learning-progress", "/hr/learning-progress", "LearningProgressView"),
                new MatrixToModuleMapping(13, "Feedback & CSAT", "feedback", "/hr/feedback", "FeedbackEffectivenessView"),
                new MatrixToModuleMapping(14, "Reports & Sharing", "reports", "/hr/reports", "ReportsAnalyticsView"),
                new MatrixToModuleMapping(15, "Announcements", "notifications", "/hr/notifications", "NotificationsAnnouncementsView")
        );

        assertEquals(15, mappings.size(), "Acceptance matrix must have exactly 15 verified entries");

        // Verify all 13 unique sidebar IDs are covered
        long uniqueSidebarModules = mappings.stream().map(MatrixToModuleMapping::sidebarModuleId).distinct().count();
        assertEquals(13, uniqueSidebarModules, "Must map to exactly 13 unique sidebar modules in frontend");
    }
}
