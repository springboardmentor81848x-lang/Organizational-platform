package com.skillsphere.service;

import com.skillsphere.dto.HrDtos.*;
import com.skillsphere.entity.*;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrDashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeProfileRepository employeeProfileRepository;
    @Mock private EmployeeSkillRepository employeeSkillRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private SkillCategoryRepository categoryRepository;
    @Mock private RoleSkillRequirementRepository requirementRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private LearningPathRepository learningPathRepository;
    @Mock private LearningPathItemRepository learningPathItemRepository;
    @Mock private TrainingProgrammeRepository trainingProgrammeRepository;
    @Mock private TrainingEnrollmentRepository trainingEnrollmentRepository;
    @Mock private EmployeeLearningRepository employeeLearningRepository;
    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private MentorshipSessionRepository mentorshipSessionRepository;
    @Mock private MentorshipFeedbackRepository mentorshipFeedbackRepository;
    @Mock private PlatformFeedbackRepository platformFeedbackRepository;
    @Mock private AssessmentAttemptRepository assessmentAttemptRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private HrAnnouncementRepository hrAnnouncementRepository;
    @Mock private HrAnnouncementReadRepository hrAnnouncementReadRepository;
    @Mock private LeadershipReportShareRepository leadershipReportShareRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private HrDashboardService hrDashboardService;

    private User employeeUser;
    private User hrUser;
    private Department engineeringDept;
    private SkillCategory techCategory;
    private Skill javaSkill;

    @BeforeEach
    void setUp() {
        engineeringDept = new Department();
        engineeringDept.setName("Engineering");
        engineeringDept.setCode("ENG");

        techCategory = new SkillCategory("Technical", "Technical Skills");
        org.springframework.test.util.ReflectionTestUtils.setField(techCategory, "id", 1L);

        javaSkill = new Skill(techCategory, "Java", "Backend development with Java");
        org.springframework.test.util.ReflectionTestUtils.setField(javaSkill, "id", 1L);

        employeeUser = new User();
        employeeUser.setFullName("John Doe");
        employeeUser.setEmail("john.doe@skillsphere.com");
        employeeUser.setRole(Role.EMPLOYEE);
        org.springframework.test.util.ReflectionTestUtils.setField(employeeUser, "id", 2L);

        hrUser = new User();
        hrUser.setFullName("Helen HR");
        hrUser.setEmail("hr@skillsphere.com");
        hrUser.setRole(Role.HR_SPECIALIST);
        org.springframework.test.util.ReflectionTestUtils.setField(hrUser, "id", 1L);
    }

    @Test
    @DisplayName("getOverview calculates metrics accurately")
    void testGetOverview() {
        when(userRepository.findAll()).thenReturn(List.of(employeeUser, hrUser));
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setUser(employeeUser);
        attempt.setSkill(javaSkill);
        attempt.setStatus("COMPLETED");
        attempt.setScore(85);
        attempt.setCompletedAt(Instant.now());
        when(assessmentAttemptRepository.findAllWithDetails()).thenReturn(List.of(attempt));

        SkillGap gap = new SkillGap(employeeUser, javaSkill, 2, 4, 2, "HIGH");
        when(skillGapRepository.findAll()).thenReturn(List.of(gap));

        TrainingProgramme prog = new TrainingProgramme(
                "Java Masterclass", "Deep dive", engineeringDept,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), 20, "Lead Dev"
        );
        when(trainingProgrammeRepository.findAllWithDetails()).thenReturn(List.of(prog));
        when(courseRepository.count()).thenReturn(15L);

        EmployeeLearning learning = new EmployeeLearning(employeeUser, new Course("Java 101", "Basics", javaSkill, "Beginner", 10, "SkillSphere", "http://example.com"), "COMPLETED", 100);
        learning.setCompletedAt(Instant.now());
        when(employeeLearningRepository.findAllWithDetails()).thenReturn(List.of(learning));

        HrDashboardOverviewDto overview = hrDashboardService.getOverview();

        assertNotNull(overview);
        assertEquals(1, overview.totalEmployees());
        assertEquals(1, overview.assessedEmployeesCount());
        assertEquals(0, overview.unassessedEmployeesCount());
        assertEquals(100.0, overview.skillCoveragePercentage());
        assertEquals(1, overview.criticalShortagesCount());
        assertEquals(1, overview.totalOrgGapsCount());
        assertEquals(1, overview.activeProgrammesCount());
        assertEquals(15, overview.totalCoursesCount());
        assertEquals(1, overview.totalEnrollmentsCount());
        assertEquals(100.0, overview.avgCompletionRate());
        assertEquals(1, overview.upcomingProgrammes().size());
    }

    @Test
    @DisplayName("getEmployeeDossier enforces privacy: zero chat messages and zero mentor prep notes")
    void testGetEmployeeDossier_PrivacyGuaranteed() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employeeUser));
        EmployeeProfile profile = new EmployeeProfile(employeeUser);
        profile.setDepartment("Engineering");
        profile.setJobRole("Backend Engineer");
        when(employeeProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        EmployeeSkill es = new EmployeeSkill(employeeUser, javaSkill, "Intermediate");
        es.setVerified(true);
        when(employeeSkillRepository.findByUserId(1L)).thenReturn(List.of(es));

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setUser(employeeUser);
        attempt.setSkill(javaSkill);
        attempt.setTargetLevel("Intermediate");
        attempt.setScore(80);
        attempt.setPassed(true);
        attempt.setStatus("COMPLETED");
        when(assessmentAttemptRepository.findByUserIdOrderByStartedAtDesc(1L)).thenReturn(List.of(attempt));

        when(employeeLearningRepository.findByUserId(1L)).thenReturn(List.of());
        when(trainingEnrollmentRepository.findByUserId(1L)).thenReturn(List.of());
        when(mentorAssignmentRepository.findByEmployeeIdOrderByAssignedAtDesc(1L)).thenReturn(List.of());

        HrEmployeeDossierDto dossier = hrDashboardService.getEmployeeDossier(1L);

        assertNotNull(dossier);
        assertEquals("John Doe", dossier.fullName());
        assertEquals("Engineering", dossier.departmentName());
        assertEquals(1, dossier.skills().size());
        assertEquals(1, dossier.assessmentHistory().size());
        // Verify only high level scores/outcomes exposed, not mutating official scores
        assertEquals(80, dossier.assessmentHistory().get(0).score());
        assertTrue(dossier.assessmentHistory().get(0).passed());
        // Verify chat repositories or prep notes are never queried
        verifyNoInteractions(mentorshipSessionRepository);
    }

    @Test
    @DisplayName("createCompetency creates a new active skill competency")
    void testCreateCompetency() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(skillRepository.findByNameIgnoreCase("Kubernetes")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateCompetencyRequest req = new CreateCompetencyRequest("Kubernetes", 1L, "Container orchestration");
        HrCompetencyDto res = hrDashboardService.createCompetency(req);

        assertNotNull(res);
        assertEquals("Kubernetes", res.name());
        assertEquals("Technical", res.categoryName());
        assertTrue(res.isActive());
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    @DisplayName("createRoleRequirement adds competency requirement for job role")
    void testCreateRoleRequirement() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(javaSkill));
        when(requirementRepository.findByJobRoleAndSkillId("Backend Engineer", 1L)).thenReturn(Optional.empty());
        when(requirementRepository.save(any(RoleSkillRequirement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateRoleRequirementRequest req = new CreateRoleRequirementRequest("Backend Engineer", 1L, "Advanced", "HIGH");
        HrRoleRequirementDto res = hrDashboardService.createRoleRequirement(req);

        assertNotNull(res);
        assertEquals("Backend Engineer", res.jobRole());
        assertEquals("Advanced", res.requiredProficiency());
        assertEquals("HIGH", res.priority());
    }

    @Test
    @DisplayName("createCourse adds course to learning catalogue")
    void testCreateCourse() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(javaSkill));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrUpdateCourseRequest req = new CreateOrUpdateCourseRequest(
                "Advanced Spring Boot", "Microservices architecture", 1L,
                "Advanced", 20, "Coursera", "https://coursera.org", "http://thumb.jpg", true
        );
        HrCourseDto course = hrDashboardService.createCourse(req);

        assertNotNull(course);
        assertEquals("Advanced Spring Boot", course.title());
        assertEquals("Java", course.skillName());
        assertEquals(20, course.durationHours());
        assertTrue(course.isActive());
    }

    @Test
    @DisplayName("enrollEmployee adds employee to training programme and sends notification")
    void testEnrollEmployee() {
        TrainingProgramme prog = new TrainingProgramme(
                "Cloud Security Bootcamp", "Hands-on", engineeringDept,
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(10), 10, "Sec Lead"
        );
        when(trainingProgrammeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(prog));
        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeUser));
        when(trainingEnrollmentRepository.findByProgrammeIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(trainingEnrollmentRepository.save(any(TrainingEnrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProgrammeEnrollmentDetailDto dto = hrDashboardService.enrollEmployee(1L, 2L);

        assertNotNull(dto);
        assertEquals("Cloud Security Bootcamp", dto.programmeTitle());
        assertEquals(1, prog.getEnrolledCount());
        verify(notificationService).createNotification(eq(employeeUser), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("createAnnouncement broadcasts targeted notification and tracks delivery")
    void testCreateAnnouncement() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(hrUser));
        when(hrAnnouncementRepository.save(any(HrAnnouncement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findAll()).thenReturn(List.of(employeeUser, hrUser));

        CreateAnnouncementRequest req = new CreateAnnouncementRequest(
                "Platform Maintenance", "Scheduled Sunday maintenance window",
                "ORGANIZATION", null, null, "URGENT"
        );
        HrAnnouncementDto dto = hrDashboardService.createAnnouncement(req, 1L);

        assertNotNull(dto);
        assertEquals("Platform Maintenance", dto.title());
        assertEquals("URGENT", dto.priority());
        // Verify notification was sent to other users
        verify(notificationService).createNotification(eq(employeeUser), eq("HR_ANNOUNCEMENT"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("shareReportWithLeadership saves report share with leadership role")
    void testShareReportWithLeadership() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(hrUser));
        when(leadershipReportShareRepository.save(any(LeadershipReportShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShareReportRequest req = new ShareReportRequest(
                "SKILL_GAP_ANALYSIS", "Q4 Skill Gap Leadership Briefing",
                "MANAGER", null, "Critical gaps in Cloud Security", "{}"
        );
        LeadershipReportShareDto share = hrDashboardService.shareReportWithLeadership(req, 1L);

        assertNotNull(share);
        assertEquals("Q4 Skill Gap Leadership Briefing", share.title());
        assertEquals("MANAGER", share.sharedWithRole());
        verify(leadershipReportShareRepository).save(any(LeadershipReportShare.class));
    }
}
