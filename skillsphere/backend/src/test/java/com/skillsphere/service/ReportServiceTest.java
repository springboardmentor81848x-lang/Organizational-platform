package com.skillsphere.service;

import com.skillsphere.dto.ReportDtos;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmployeeProfileRepository profileRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMembershipRepository membershipRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private EmployeeSkillRepository employeeSkillRepository;
    @Mock
    private SkillGapRepository skillGapRepository;
    @Mock
    private EmployeeLearningRepository learningRepository;
    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock
    private AssessmentSubmissionRepository submissionRepository;
    @Mock
    private SupportTicketRepository supportTicketRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private TrainingEnrollmentRepository trainingEnrollmentRepository;

    @InjectMocks
    private ReportService reportService;

    private User manager;
    private User otherManager;
    private User deptHead;
    private User otherDeptHead;
    private User admin;
    private Team team;
    private Department department;

    @BeforeEach
    void setUp() throws Exception {
        manager = createUser(1L, "Manager One", "manager1@example.com", Role.MANAGER);
        otherManager = createUser(2L, "Manager Two", "manager2@example.com", Role.MANAGER);
        deptHead = createUser(3L, "Head Engineering", "head.eng@example.com", Role.DEPARTMENT_HEAD);
        otherDeptHead = createUser(4L, "Head Sales", "head.sales@example.com", Role.DEPARTMENT_HEAD);
        admin = createUser(5L, "Admin User", "admin@example.com", Role.ADMIN);

        department = new Department("Engineering", "ENG", "Eng Dept", deptHead);
        setPrivateId(department, 10L);

        team = new Team(department, "Backend Team", manager);
        setPrivateId(team, 100L);
    }

    private User createUser(Long id, String name, String email, Role role) throws Exception {
        User u = new User();
        setPrivateId(u, id);
        u.setFullName(name);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    private void setPrivateId(Object obj, Long id) throws Exception {
        var field = obj.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(obj, id);
    }

    @Test
    @DisplayName("Manager successfully accesses own assigned team report")
    void testGetTeamReport_ManagerOwnTeam_Success() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(userRepository.findByEmail("manager1@example.com")).thenReturn(Optional.of(manager));
        when(membershipRepository.findByTeamId(100L)).thenReturn(Collections.emptyList());

        ReportDtos.TeamReportDto report = reportService.getTeamReport(100L, "manager1@example.com");

        assertNotNull(report);
        assertEquals("Backend Team", report.teamName());
    }

    @Test
    @DisplayName("Manager accessing another team's report throws 403 Forbidden")
    void testGetTeamReport_ManagerAnotherTeam_ThrowsForbidden() {
        when(teamRepository.findById(100L)).thenReturn(Optional.of(team));
        when(userRepository.findByEmail("manager2@example.com")).thenReturn(Optional.of(otherManager));

        ApiException ex = assertThrows(ApiException.class, () ->
                reportService.getTeamReport(100L, "manager2@example.com"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Department Head successfully accesses own department report")
    void testGetDepartmentReport_HeadOwnDepartment_Success() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(userRepository.findByEmail("head.eng@example.com")).thenReturn(Optional.of(deptHead));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(Collections.emptyList());
        when(membershipRepository.findByDepartmentId(10L)).thenReturn(Collections.emptyList());

        ReportDtos.DepartmentReportDto report = reportService.getDepartmentReport(10L, "head.eng@example.com");

        assertNotNull(report);
        assertEquals("Engineering", report.departmentName());
    }

    @Test
    @DisplayName("Department Head accessing another department's report throws 403 Forbidden")
    void testGetDepartmentReport_HeadAnotherDepartment_ThrowsForbidden() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(userRepository.findByEmail("head.sales@example.com")).thenReturn(Optional.of(otherDeptHead));

        ApiException ex = assertThrows(ApiException.class, () ->
                reportService.getDepartmentReport(10L, "head.sales@example.com"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Admin successfully accesses any department report")
    void testGetDepartmentReport_Admin_Success() {
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(teamRepository.findByDepartmentId(10L)).thenReturn(Collections.emptyList());
        when(membershipRepository.findByDepartmentId(10L)).thenReturn(Collections.emptyList());

        ReportDtos.DepartmentReportDto report = reportService.getDepartmentReport(10L, "admin@example.com");

        assertNotNull(report);
        assertEquals("Engineering", report.departmentName());
    }

    @Test
    @DisplayName("Authenticated employee successfully retrieves their own report")
    void testGetEmployeeReport_Success() throws Exception {
        User employee = createUser(9L, "Jane Doe", "jane@example.com", Role.EMPLOYEE);
        employee.setEmployeeId("EMP-009");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(employee));

        EmployeeProfile profile = new EmployeeProfile(employee);
        profile.setJobRole("Full Stack Engineer");
        profile.setDepartment("Engineering");
        when(profileRepository.findByUserId(9L)).thenReturn(Optional.of(profile));

        SkillCategory cat = new SkillCategory("Technical", "Technical Skills");
        setPrivateId(cat, 1L);
        Skill skill = new Skill(cat, "Spring Boot", "Spring framework");
        setPrivateId(skill, 2L);

        SkillGap gap = new SkillGap(employee, skill, 2, 4, 2, "HIGH");
        when(skillGapRepository.findByUserId(9L)).thenReturn(List.of(gap));

        Course course = new Course("Spring Microservices", "Spring Cloud", skill, "Advanced", 20, "Internal", "/courses/spring");
        setPrivateId(course, 3L);
        EmployeeLearning learning = new EmployeeLearning(employee, course, "COMPLETED", 100);
        when(learningRepository.findByUserId(9L)).thenReturn(List.of(learning));

        when(employeeSkillRepository.findByUserId(9L)).thenReturn(Collections.emptyList());
        when(certificateRepository.findByUserId(9L)).thenReturn(Collections.emptyList());
        when(mentorAssignmentRepository.countByMentorIdAndStatus(9L, AssignmentStatus.ACTIVE)).thenReturn(0L);
        when(mentorAssignmentRepository.findByEmployeeIdAndStatus(9L, AssignmentStatus.ACTIVE)).thenReturn(Optional.empty());

        ReportDtos.EmployeeReportDto report = reportService.getEmployeeReport("jane@example.com");

        assertNotNull(report);
        assertEquals("Jane Doe", report.employeeName());
        assertEquals("EMP-009", report.employeeId());
        assertEquals("jane@example.com", report.email());
        assertEquals("Full Stack Engineer", report.jobRole());
        assertEquals("Engineering", report.department());
        assertEquals(1, report.activeGaps());
        assertEquals(1, report.coursesCompleted());
        assertEquals(1, report.skillGaps().size());
        assertEquals("Spring Boot", report.skillGaps().get(0).skillName());
        assertEquals(1, report.learningRecords().size());
        assertEquals("Spring Microservices", report.learningRecords().get(0).courseTitle());
    }

    @Test
    @DisplayName("Employee report with non-existent user throws 404 NOT_FOUND")
    void testGetEmployeeReport_UserNotFound_ThrowsNotFound() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                reportService.getEmployeeReport("nonexistent@example.com"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
