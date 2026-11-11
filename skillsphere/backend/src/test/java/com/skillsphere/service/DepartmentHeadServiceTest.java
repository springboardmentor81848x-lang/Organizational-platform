package com.skillsphere.service;

import com.skillsphere.dto.DepartmentHeadDtos.*;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DepartmentHeadServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMembershipRepository teamMembershipRepository;
    @Mock private EmployeeProfileRepository employeeProfileRepository;
    @Mock private EmployeeSkillRepository employeeSkillRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private RoleSkillRequirementRepository roleSkillRequirementRepository;
    @Mock private SkillGapRepository skillGapRepository;
    @Mock private AssessmentAttemptRepository assessmentAttemptRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TrainingProgrammeRepository trainingProgrammeRepository;
    @Mock private TrainingEnrollmentRepository trainingEnrollmentRepository;
    @Mock private EmployeeLearningRepository employeeLearningRepository;
    @Mock private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock private MentorshipSessionRepository mentorshipSessionRepository;
    @Mock private MentorshipFeedbackRepository mentorshipFeedbackRepository;
    @Mock private HrAnnouncementRepository hrAnnouncementRepository;
    @Mock private HrAnnouncementReadRepository hrAnnouncementReadRepository;
    @Mock private LeadershipReportShareRepository leadershipReportShareRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private DepartmentHeadService departmentHeadService;

    private User deptHeadUser;
    private User adminUser;
    private User employeeUser;
    private Department engineeringDept;
    private Department financeDept;
    private Team backendTeam;
    private Skill javaSkill;
    private SkillCategory techCategory;

    @BeforeEach
    void setUp() {
        deptHeadUser = new User();
        ReflectionTestUtils.setField(deptHeadUser, "id", 100L);
        deptHeadUser.setFullName("Dr. Sarah Vance");
        deptHeadUser.setEmail("sarah.vance@skillsphere.com");
        deptHeadUser.setRole(Role.DEPARTMENT_HEAD);

        adminUser = new User();
        ReflectionTestUtils.setField(adminUser, "id", 1L);
        adminUser.setFullName("System Admin");
        adminUser.setEmail("admin@skillsphere.com");
        adminUser.setRole(Role.ADMIN);

        employeeUser = new User();
        ReflectionTestUtils.setField(employeeUser, "id", 200L);
        employeeUser.setFullName("John Developer");
        employeeUser.setEmail("john.dev@skillsphere.com");
        employeeUser.setRole(Role.EMPLOYEE);

        engineeringDept = new Department();
        ReflectionTestUtils.setField(engineeringDept, "id", 10L);
        engineeringDept.setName("Engineering");
        engineeringDept.setCode("ENG");
        engineeringDept.setDescription("Software and Systems Engineering");
        engineeringDept.setHeadUser(deptHeadUser);

        financeDept = new Department();
        ReflectionTestUtils.setField(financeDept, "id", 20L);
        financeDept.setName("Finance");
        financeDept.setCode("FIN");

        backendTeam = new Team();
        ReflectionTestUtils.setField(backendTeam, "id", 50L);
        backendTeam.setName("Backend Platform");
        backendTeam.setDepartment(engineeringDept);
        backendTeam.setManager(deptHeadUser);

        techCategory = new SkillCategory("Technical", "Technical Skills");
        ReflectionTestUtils.setField(techCategory, "id", 1L);

        javaSkill = new Skill(techCategory, "Java", "Core backend development");
        ReflectionTestUtils.setField(javaSkill, "id", 1L);
    }

    // =========================================================================
    // 1. Department Resolution & Security / IDOR Tests
    // =========================================================================
    @Test
    @DisplayName("resolveAuthorizedDepartment: Success for authorized DEPARTMENT_HEAD")
    void testResolveAuthorizedDepartment_Success() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));

        Department resolved = departmentHeadService.resolveAuthorizedDepartment("sarah.vance@skillsphere.com", 10L);
        assertNotNull(resolved);
        assertEquals(10L, resolved.getId());
        assertEquals("Engineering", resolved.getName());
    }

    @Test
    @DisplayName("resolveAuthorizedDepartment: 403 IDOR rejection when requesting other department")
    void testResolveAuthorizedDepartment_IdorRejection() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));

        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment("sarah.vance@skillsphere.com", 20L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied: You are not authorized to access department 20"));
    }

    @Test
    @DisplayName("resolveAuthorizedDepartment: Fallback via EmployeeProfile department name")
    void testResolveAuthorizedDepartment_FallbackProfile() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.empty());

        EmployeeProfile profile = new EmployeeProfile();
        profile.setUser(deptHeadUser);
        profile.setDepartment("Engineering");
        when(employeeProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(departmentRepository.findByName("Engineering")).thenReturn(Optional.of(engineeringDept));

        Department resolved = departmentHeadService.resolveAuthorizedDepartment("sarah.vance@skillsphere.com", null);
        assertNotNull(resolved);
        assertEquals("Engineering", resolved.getName());
    }

    @Test
    @DisplayName("resolveAuthorizedDepartment: Admin can access requested department")
    void testResolveAuthorizedDepartment_AdminAccess() {
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));
        when(departmentRepository.findById(20L)).thenReturn(Optional.of(financeDept));

        Department resolved = departmentHeadService.resolveAuthorizedDepartment("admin@skillsphere.com", 20L);
        assertNotNull(resolved);
        assertEquals("Finance", resolved.getName());
    }

    @Test
    @DisplayName("resolveAuthorizedDepartment: 403 when caller is not DEPARTMENT_HEAD or ADMIN")
    void testResolveAuthorizedDepartment_NonDeptHeadForbidden() {
        when(userRepository.findByEmail("john.dev@skillsphere.com")).thenReturn(Optional.of(employeeUser));

        ApiException ex = assertThrows(ApiException.class, () ->
                departmentHeadService.resolveAuthorizedDepartment("john.dev@skillsphere.com", null));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // =========================================================================
    // 2. Dashboard Overview Tests
    // =========================================================================
    @Test
    @DisplayName("getDashboardOverview: Computes correct aggregate KPIs")
    void testGetDashboardOverview() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));

        TeamMembership tm = new TeamMembership();
        tm.setTeam(backendTeam);
        tm.setUser(employeeUser);
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(List.of(tm));
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());

        // Assessment attempts
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setUser(employeeUser);
        attempt.setStatus("COMPLETED");
        attempt.setScore(88);
        when(assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(anyList())).thenReturn(List.of(attempt));

        // Skill gaps
        SkillGap gap = new SkillGap(employeeUser, javaSkill, 1, 3, 2, "HIGH");
        when(skillGapRepository.findByUserIds(anyList())).thenReturn(List.of(gap));

        when(trainingProgrammeRepository.findByDepartmentId(10L)).thenReturn(Collections.emptyList());
        when(employeeLearningRepository.findByUserIds(anyList())).thenReturn(Collections.emptyList());
        when(mentorAssignmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(mentorshipSessionRepository.findAll()).thenReturn(Collections.emptyList());
        when(mentorshipFeedbackRepository.findAll()).thenReturn(Collections.emptyList());

        DepartmentHeadOverviewDto overview = departmentHeadService.getDashboardOverview("sarah.vance@skillsphere.com", null);

        assertNotNull(overview);
        assertEquals(10L, overview.departmentId());
        assertEquals("Engineering", overview.departmentName());
        assertEquals("Dr. Sarah Vance", overview.headName());
        assertEquals(1, overview.totalTeams());
        assertEquals(1, overview.totalDepartmentGapsCount());
    }

    // =========================================================================
    // 3. Department Overview & Teams
    // =========================================================================
    @Test
    @DisplayName("getDepartmentOverview: Returns team summaries and manager information")
    void testGetDepartmentOverview() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));

        TeamMembership tm = new TeamMembership();
        tm.setTeam(backendTeam);
        tm.setUser(employeeUser);
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(List.of(tm));
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());
        when(skillGapRepository.findByUserIds(anyList())).thenReturn(Collections.emptyList());

        DepartmentProfileDto profile = departmentHeadService.getDepartmentOverview("sarah.vance@skillsphere.com", null);

        assertNotNull(profile);
        assertEquals("Engineering", profile.name());
        assertEquals(1, profile.teams().size());
        assertEquals("Backend Platform", profile.teams().get(0).teamName());
        assertEquals("Dr. Sarah Vance", profile.teams().get(0).managerName());
    }

    // =========================================================================
    // 4. Department Skill Matrix
    // =========================================================================
    @Test
    @DisplayName("getDepartmentSkillMatrix: Generates competency matrix and compliance %")
    void testGetDepartmentSkillMatrix() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));

        TeamMembership tm = new TeamMembership();
        tm.setTeam(backendTeam);
        tm.setUser(employeeUser);
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(List.of(tm));
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());

        EmployeeSkill empSkill = new EmployeeSkill();
        empSkill.setUser(employeeUser);
        empSkill.setSkill(javaSkill);
        empSkill.setProficiencyLevel("Advanced");
        empSkill.setVerified(true);
        when(employeeSkillRepository.findByUserIds(anyList())).thenReturn(List.of(empSkill));

        RoleSkillRequirement req = new RoleSkillRequirement();
        req.setJobRole("Software Engineer");
        req.setSkill(javaSkill);
        req.setRequiredProficiency("Intermediate");
        req.setPriority("HIGH");
        when(roleSkillRequirementRepository.findAll()).thenReturn(List.of(req));
        when(skillRepository.findAll()).thenReturn(List.of(javaSkill));

        DeptSkillMatrixDto matrix = departmentHeadService.getDepartmentSkillMatrix("sarah.vance@skillsphere.com", null, null, null);

        assertNotNull(matrix);
        assertEquals(10L, matrix.departmentId());
        assertFalse(matrix.competencies().isEmpty());
        assertEquals("Java", matrix.competencies().get(0).skillName());
    }

    // =========================================================================
    // 5. Gap Heatmap
    // =========================================================================
    @Test
    @DisplayName("getDepartmentHeatmap: Team x Competency gap severity calculation")
    void testGetDepartmentHeatmap() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));

        TeamMembership tm = new TeamMembership();
        tm.setTeam(backendTeam);
        tm.setUser(employeeUser);
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(List.of(tm));
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());

        when(skillRepository.findAll()).thenReturn(List.of(javaSkill));
        when(roleSkillRequirementRepository.findAll()).thenReturn(Collections.emptyList());
        when(employeeSkillRepository.findByUserIds(anyList())).thenReturn(Collections.emptyList());

        DeptHeatmapDto heatmap = departmentHeadService.getDepartmentHeatmap("sarah.vance@skillsphere.com", null);

        assertNotNull(heatmap);
        assertEquals("Engineering", heatmap.departmentName());
        assertFalse(heatmap.cells().isEmpty());
        DeptHeatmapCellDto cell = heatmap.cells().get(0);
        assertEquals("Backend Platform", cell.teamName());
        assertEquals("Java", cell.skillName());
    }

    // =========================================================================
    // 6. Workforce Capability
    // =========================================================================
    @Test
    @DisplayName("getWorkforceCapability: Identifies strongest/weakest skills and critical shortages")
    void testGetWorkforceCapability() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(Collections.emptyList());
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());

        EmployeeSkill empSkill = new EmployeeSkill();
        empSkill.setUser(deptHeadUser);
        empSkill.setSkill(javaSkill);
        empSkill.setProficiencyLevel("Expert");
        empSkill.setVerified(true);
        when(employeeSkillRepository.findByUserIds(anyList())).thenReturn(List.of(empSkill));

        SkillGap gap = new SkillGap(deptHeadUser, javaSkill, 1, 4, 3, "HIGH");
        when(skillGapRepository.findByUserIds(anyList())).thenReturn(List.of(gap));
        when(assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(anyList())).thenReturn(Collections.emptyList());

        DeptCapabilityProfileDto profile = departmentHeadService.getWorkforceCapability("sarah.vance@skillsphere.com", null);

        assertNotNull(profile);
        assertEquals("Engineering", profile.departmentName());
        assertFalse(profile.criticalShortages().isEmpty());
        assertEquals("Java", profile.criticalShortages().get(0).skillName());
    }

    // =========================================================================
    // 7. Learning & Mentoring Overview (Strict Privacy)
    // =========================================================================
    @Test
    @DisplayName("getLearningAndMentoringOverview: Strict privacy preserved (no chats or mentor notes)")
    void testGetLearningAndMentoringOverview_PrivacyPreserved() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(Collections.emptyList());
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());

        TrainingProgramme prog = new TrainingProgramme();
        prog.setTitle("Cloud Architecture Bootcamp");
        prog.setDepartment(engineeringDept);
        when(trainingProgrammeRepository.findByDepartmentId(10L)).thenReturn(List.of(prog));
        when(employeeLearningRepository.findByUserIds(anyList())).thenReturn(Collections.emptyList());
        when(mentorAssignmentRepository.findAll()).thenReturn(Collections.emptyList());
        when(mentorshipFeedbackRepository.findAll()).thenReturn(Collections.emptyList());

        DeptLearningMentoringDto dto = departmentHeadService.getLearningAndMentoringOverview("sarah.vance@skillsphere.com", null);

        assertNotNull(dto);
        assertNotNull(dto.learningOverview());
        assertNotNull(dto.mentoringOverview());
        // Verify no notification/chat repositories were invoked
        verifyNoInteractions(notificationRepository);
    }

    // =========================================================================
    // 8. Risk Alerts
    // =========================================================================
    @Test
    @DisplayName("getDepartmentRiskAlerts: Flags critical gaps and unassigned managers")
    void testGetDepartmentRiskAlerts() {
        Team unmanagedTeam = new Team();
        ReflectionTestUtils.setField(unmanagedTeam, "id", 60L);
        unmanagedTeam.setName("DevOps Team");
        unmanagedTeam.setDepartment(engineeringDept);
        unmanagedTeam.setManager(null); // Unassigned manager

        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam, unmanagedTeam));

        TeamMembership tm = new TeamMembership();
        tm.setTeam(backendTeam);
        tm.setUser(employeeUser);
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(List.of(tm));
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());

        SkillGap gap = new SkillGap(employeeUser, javaSkill, 1, 4, 3, "HIGH");
        when(skillGapRepository.findByUserIds(anyList())).thenReturn(List.of(gap));
        when(assessmentAttemptRepository.findByUserIdsOrderByStartedAtDesc(anyList())).thenReturn(Collections.emptyList());

        List<DeptRiskAlertDto> alerts = departmentHeadService.getDepartmentRiskAlerts("sarah.vance@skillsphere.com", null);

        assertNotNull(alerts);
        assertFalse(alerts.isEmpty());
        boolean hasManagerAlert = alerts.stream().anyMatch(a -> "UNASSIGNED_MANAGER".equals(a.alertType()) || a.title().contains("Unassigned"));
        boolean hasSkillAlert = alerts.stream().anyMatch(a -> "CRITICAL_SHORTAGE".equals(a.alertType()) || a.title().contains("Critical"));
        assertTrue(hasManagerAlert, "Should flag unmanaged team");
        assertTrue(hasSkillAlert, "Should flag critical skill gap");
    }

    // =========================================================================
    // 9. Reports & Leadership Sharing
    // =========================================================================
    @Test
    @DisplayName("generateDepartmentReport: Generates comprehensive department report")
    void testGenerateDepartmentReport() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(Collections.emptyList());
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());

        DeptReportResultDto report = departmentHeadService.generateDepartmentReport("sarah.vance@skillsphere.com", "EXECUTIVE_SUMMARY", null);

        assertNotNull(report);
        assertEquals("EXECUTIVE_SUMMARY", report.reportType());
        assertEquals("Engineering", report.departmentName());
        assertFalse(report.tableHeaders().isEmpty());
    }

    @Test
    @DisplayName("getSharedLeadershipReports: Returns reports shared with department head")
    void testGetSharedLeadershipReports() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));

        LeadershipReportShare share = new LeadershipReportShare();
        ReflectionTestUtils.setField(share, "id", 1L);
        share.setDepartment(engineeringDept);
        share.setSharedWithRole("DEPARTMENT_HEAD");
        share.setTitle("Q3 Workforce Capability Review");
        share.setReportType("CAPABILITY_REVIEW");
        share.setSharedBy(adminUser);
        when(leadershipReportShareRepository.findForLeadership(anyList(), eq(10L))).thenReturn(List.of(share));

        List<DeptLeadershipSharedReportDto> shared = departmentHeadService.getSharedLeadershipReports("sarah.vance@skillsphere.com", null);

        assertNotNull(shared);
        assertEquals(1, shared.size());
        assertEquals("Q3 Workforce Capability Review", shared.get(0).title());
    }

    // =========================================================================
    // 10. Announcements & Notifications
    // =========================================================================
    @Test
    @DisplayName("createDepartmentAnnouncement: Saves announcement and dispatches notifications")
    void testCreateDepartmentAnnouncement() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));
        when(departmentRepository.findByHeadUserId(100L)).thenReturn(Optional.of(engineeringDept));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(List.of(backendTeam));

        TeamMembership tm = new TeamMembership();
        tm.setTeam(backendTeam);
        tm.setUser(employeeUser);
        when(teamMembershipRepository.findByDepartmentId(10L)).thenReturn(List.of(tm));
        when(employeeProfileRepository.findAllWithUser()).thenReturn(Collections.emptyList());

        CreateDeptAnnouncementRequest req = new CreateDeptAnnouncementRequest(
                "Engineering Q4 Hackathon",
                "Participation is mandatory for all engineers.",
                "HIGH"
        );

        HrAnnouncement saved = new HrAnnouncement();
        ReflectionTestUtils.setField(saved, "id", 999L);
        saved.setTitle(req.title());
        saved.setContent(req.content());
        saved.setPriority(req.priority());
        saved.setDepartment(engineeringDept);
        saved.setAuthor(deptHeadUser);
        when(hrAnnouncementRepository.save(any(HrAnnouncement.class))).thenReturn(saved);

        DeptAnnouncementDto dto = departmentHeadService.createDepartmentAnnouncement("sarah.vance@skillsphere.com", req, null);

        assertNotNull(dto);
        assertEquals("Engineering Q4 Hackathon", dto.title());
        verify(hrAnnouncementRepository, times(1)).save(any(HrAnnouncement.class));
        verify(notificationService, atLeastOnce()).createNotification(eq(200L), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("getNotifications and markNotificationRead")
    void testDepartmentNotifications() {
        when(userRepository.findByEmail("sarah.vance@skillsphere.com")).thenReturn(Optional.of(deptHeadUser));

        Notification notif = new Notification(deptHeadUser, "ALERT", "System Alert", "Quarterly audit completed", "/alerts");
        ReflectionTestUtils.setField(notif, "id", 555L);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(notif));

        List<DeptNotificationDto> notifications = departmentHeadService.getNotifications("sarah.vance@skillsphere.com", false);
        assertEquals(1, notifications.size());
        assertEquals("System Alert", notifications.get(0).title());

        when(notificationRepository.findById(555L)).thenReturn(Optional.of(notif));
        departmentHeadService.markNotificationRead(555L, "sarah.vance@skillsphere.com");
        assertTrue(notif.isRead());
    }
}
