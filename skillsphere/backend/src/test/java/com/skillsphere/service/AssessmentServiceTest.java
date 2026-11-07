package com.skillsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    @Mock
    private SkillAssessmentRepository assessmentRepository;
    @Mock
    private AssessmentQuestionRepository questionRepository;
    @Mock
    private AssessmentSubmissionRepository submissionRepository;
    @Mock
    private AssessmentResponseRepository responseRepository;
    @Mock
    private AssessmentAssignmentRepository assignmentRepository;
    @Mock
    private AssessmentAttemptRepository attemptRepository;
    @Mock
    private SkillGapRepository skillGapRepository;
    @Mock
    private EmployeeSkillRepository employeeSkillRepository;
    @Mock
    private RoleSkillRequirementRepository requirementRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private TeamMembershipRepository teamMembershipRepository;
    @Mock
    private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private GeminiAssessmentQuestionService geminiQuestionService;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AssessmentService assessmentService;

    private User user;
    private User otherUser;
    private SkillCategory category;
    private Skill skill;
    private SkillAssessment assessment;
    private AssessmentQuestion question1;
    private AssessmentQuestion question2;

    @BeforeEach
    void setUp() {
        user = new User();
        setPrivateId(user, 1L);
        user.setFullName("John Doe");
        user.setEmail("john@example.com");
        user.setEmployeeId("EMP-001");
        user.setRole(Role.EMPLOYEE);
        user.setRequestedRole(Role.EMPLOYEE);

        otherUser = new User();
        setPrivateId(otherUser, 2L);
        otherUser.setFullName("Jane Smith");
        otherUser.setEmail("jane@example.com");
        otherUser.setEmployeeId("EMP-002");
        otherUser.setRole(Role.EMPLOYEE);

        category = new SkillCategory("Technical", "Technical skills");
        setPrivateId(category, 1L);
        skill = new Skill(category, "Java Spring Boot", "Backend Java");
        setPrivateId(skill, 2L);

        assessment = new SkillAssessment("Java Assessment", "Test Java knowledge", category, "Full Stack Developer", 60, 30);
        setPrivateId(assessment, 1L);
        question1 = new AssessmentQuestion(assessment, skill, "What is Spring Boot?", "[\"A framework\",\"A tool\",\"A database\",\"None\"]", 0, 5);
        question2 = new AssessmentQuestion(assessment, skill, "What is IoC?", "[\"Control\",\"Inversion of Control\",\"Internet\",\"None\"]", 1, 5);
        setPrivateId(question1, 10L);
        setPrivateId(question2, 20L);
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

    @Test
    @DisplayName("Assessment questions are loaded from database successfully")
    void testGetAssessmentById_Success() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessmentId(1L)).thenReturn(List.of(question1, question2));

        AssessmentDetailDto detail = assessmentService.getAssessmentById(1L);

        assertNotNull(detail);
        assertEquals(1L, detail.id());
        assertEquals("Java Assessment", detail.title());
        assertEquals(2, detail.questions().size());
        assertEquals("What is Spring Boot?", detail.questions().get(0).questionText());
        assertEquals("Java Spring Boot", detail.questions().get(0).skillName());
    }

    @Test
    @DisplayName("Get assessment with non-existent ID throws 404 NOT_FOUND")
    void testGetAssessmentById_NotFound_ThrowsNotFound() {
        when(assessmentRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> assessmentService.getAssessmentById(999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Submit assessment calculates score, saves submission, and computes skill gaps")
    void testSubmitAssessment_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessmentId(1L)).thenReturn(List.of(question1, question2));
        when(submissionRepository.save(any(AssessmentSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        RoleSkillRequirement req = new RoleSkillRequirement("Full Stack Developer", skill, "Advanced", "HIGH");
        when(requirementRepository.findByJobRole(anyString())).thenReturn(List.of(req));
        when(employeeSkillRepository.findByUserId(any())).thenReturn(List.of());
        when(skillGapRepository.findByUserIdAndSkillId(any(), any())).thenReturn(Optional.empty());
        when(skillGapRepository.save(any(SkillGap.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<Long, Integer> answers = Map.of(10L, 0, 20L, 1);
        SubmitAssessmentRequest request = new SubmitAssessmentRequest(null, answers);

        AssessmentResultDto result = assessmentService.submitAssessment("john@example.com", 1L, request);

        assertNotNull(result);
        assertTrue(result.score() >= 0);
        verify(submissionRepository, times(1)).save(any(AssessmentSubmission.class));
        verify(notificationService, times(1)).createNotification(any(User.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Attempting to submit an already completed assignment throws 409 Conflict")
    void testSubmitAssessment_AlreadyCompletedAssignment_ThrowsConflict() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(questionRepository.findByAssessmentId(1L)).thenReturn(List.of(question1));

        AssessmentAssignment assignment = new AssessmentAssignment(assessment, user, null, null);
        assignment.setStatus("COMPLETED");
        setPrivateId(assignment, 501L);
        when(assignmentRepository.findById(501L)).thenReturn(Optional.of(assignment));

        SubmitAssessmentRequest request = new SubmitAssessmentRequest(501L, Map.of(10L, 0));

        ApiException ex = assertThrows(ApiException.class, () ->
                assessmentService.submitAssessment("john@example.com", 1L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already been completed"));
    }

    /* ---------------------------------------------------------------- Dynamic Assessment Lifecycle Tests */

    @Test
    @DisplayName("startAssessment generates questions via Gemini when configured and available")
    void testStartAssessment_GeminiSuccess() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(skillRepository.findById(2L)).thenReturn(Optional.of(skill));
        when(employeeSkillRepository.findByUserIdAndSkillId(1L, 2L)).thenReturn(Optional.empty());
        when(attemptRepository.findByUserIdAndSkillIdAndStatus(1L, 2L, "IN_PROGRESS")).thenReturn(List.of());
        when(attemptRepository.findTop5ByUserIdAndSkillIdOrderByStartedAtDesc(1L, 2L)).thenReturn(List.of());
        when(assessmentRepository.findAll()).thenReturn(List.of(assessment));

        when(geminiQuestionService.isConfigured()).thenReturn(true);
        List<GeminiAssessmentQuestionService.GeneratedQuestion> genList = List.of(
                new GeminiAssessmentQuestionService.GeneratedQuestion(
                        "What is Spring Boot Auto-configuration?",
                        "Configuration",
                        "EASY",
                        List.of("A mechanism that automatically configures beans based on classpath", "A database tool", "A compiler plugin", "None"),
                        0,
                        "Auto-configuration guesses what beans you need based on jars.",
                        1
                )
        );
        when(geminiQuestionService.generateQuestions(eq(skill), eq("Intermediate"), eq("Unaware"), eq(10), any()))
                .thenReturn(genList);

        when(attemptRepository.save(any(AssessmentAttempt.class))).thenAnswer(inv -> {
            AssessmentAttempt a = inv.getArgument(0);
            setPrivateId(a, 99L);
            return a;
        });
        when(questionRepository.save(any(AssessmentQuestion.class))).thenAnswer(inv -> {
            AssessmentQuestion q = inv.getArgument(0);
            setPrivateId(q, 101L);
            return q;
        });

        StartAssessmentRequest req = new StartAssessmentRequest(2L, "Intermediate", null, null);
        AssessmentAttemptDto attempt = assessmentService.startAssessment("john@example.com", req);

        assertNotNull(attempt);
        assertEquals(99L, attempt.attemptId());
        assertEquals("AI_GENERATED", attempt.generationSource());
        assertEquals(1, attempt.questions().size());
        assertEquals("What is Spring Boot Auto-configuration?", attempt.questions().get(0).questionText());
        verify(geminiQuestionService, times(1)).generateQuestions(eq(skill), eq("Intermediate"), eq("Unaware"), eq(10), any());
    }

    @Test
    @DisplayName("startAssessment returns existing in-progress attempt without regenerating questions (refresh safety)")
    void testStartAssessment_ActiveAttemptExists_ReturnsExistingWithoutGemini() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(skillRepository.findById(2L)).thenReturn(Optional.of(skill));
        when(employeeSkillRepository.findByUserIdAndSkillId(1L, 2L)).thenReturn(Optional.empty());

        AssessmentAttempt existingAttempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        setPrivateId(existingAttempt, 88L);
        when(attemptRepository.findByUserIdAndSkillIdAndStatus(1L, 2L, "IN_PROGRESS"))
                .thenReturn(List.of(existingAttempt));

        AssessmentQuestion existingQ = new AssessmentQuestion(assessment, skill, existingAttempt, "Existing Question?", "[\"A\",\"B\",\"C\",\"D\"]", 1, 1, "EASY", "General", "Exp", 1);
        setPrivateId(existingQ, 201L);
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(88L)).thenReturn(List.of(existingQ));

        StartAssessmentRequest req = new StartAssessmentRequest(2L, "Intermediate", null, null);
        AssessmentAttemptDto attempt = assessmentService.startAssessment("john@example.com", req);

        assertNotNull(attempt);
        assertEquals(88L, attempt.attemptId());
        assertEquals("AI_GENERATED", attempt.generationSource());
        assertEquals(1, attempt.questions().size());
        assertEquals("Existing Question?", attempt.questions().get(0).questionText());
        // Verify Gemini was NEVER called because active attempt was recovered
        verifyNoInteractions(geminiQuestionService);
    }

    @Test
    @DisplayName("startAssessment uses controlled fallback when Gemini fails")
    void testStartAssessment_GeminiFails_UsesFallback() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(skillRepository.findById(2L)).thenReturn(Optional.of(skill));
        when(employeeSkillRepository.findByUserIdAndSkillId(1L, 2L)).thenReturn(Optional.empty());
        when(attemptRepository.findByUserIdAndSkillIdAndStatus(1L, 2L, "IN_PROGRESS")).thenReturn(List.of());
        when(attemptRepository.findTop5ByUserIdAndSkillIdOrderByStartedAtDesc(1L, 2L)).thenReturn(List.of());
        when(assessmentRepository.findAll()).thenReturn(List.of(assessment));

        when(geminiQuestionService.isConfigured()).thenReturn(true);
        when(geminiQuestionService.generateQuestions(any(), any(), any(), anyInt(), any()))
                .thenThrow(new GeminiAssessmentQuestionService.GeminiGenerationException("Timeout", null));

        // Fallback question pool
        when(questionRepository.findBySkillIdAndAttemptIdIsNull(2L)).thenReturn(List.of(question1, question2));
        when(attemptRepository.save(any(AssessmentAttempt.class))).thenAnswer(inv -> {
            AssessmentAttempt a = inv.getArgument(0);
            setPrivateId(a, 77L);
            return a;
        });
        when(questionRepository.save(any(AssessmentQuestion.class))).thenAnswer(inv -> {
            AssessmentQuestion q = inv.getArgument(0);
            setPrivateId(q, 301L);
            return q;
        });

        StartAssessmentRequest req = new StartAssessmentRequest(2L, "Intermediate", null, null);
        AssessmentAttemptDto attempt = assessmentService.startAssessment("john@example.com", req);

        assertNotNull(attempt);
        assertEquals(77L, attempt.attemptId());
        assertEquals("FALLBACK", attempt.generationSource());
        assertTrue(attempt.questions().size() > 0);
    }

    @Test
    @DisplayName("submitAttempt scores correctly on server side and updates employee skill and gaps")
    void testSubmitAttempt_ServerSideScoring_UpdatesSkillProfile() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        setPrivateId(attempt, 55L);
        when(attemptRepository.findById(55L)).thenReturn(Optional.of(attempt));

        AssessmentQuestion q1 = new AssessmentQuestion(assessment, skill, attempt, "Q1?", "[\"A\",\"B\",\"C\",\"D\"]", 0, 1, "EASY", "General", "Exp 1", 1);
        AssessmentQuestion q2 = new AssessmentQuestion(assessment, skill, attempt, "Q2?", "[\"A\",\"B\",\"C\",\"D\"]", 2, 1, "MEDIUM", "General", "Exp 2", 2);
        setPrivateId(q1, 401L);
        setPrivateId(q2, 402L);
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(55L)).thenReturn(List.of(q1, q2));

        when(attemptRepository.save(any(AssessmentAttempt.class))).thenAnswer(inv -> inv.getArgument(0));
        when(submissionRepository.save(any(AssessmentSubmission.class))).thenAnswer(inv -> {
            AssessmentSubmission s = inv.getArgument(0);
            setPrivateId(s, 601L);
            return s;
        });

        RoleSkillRequirement req = new RoleSkillRequirement("Full Stack Developer", skill, "Intermediate", "MEDIUM");
        when(requirementRepository.findByJobRole(anyString())).thenReturn(List.of(req));
        when(employeeSkillRepository.findByUserId(1L)).thenReturn(List.of());
        when(employeeSkillRepository.findByUserIdAndSkillId(1L, 2L)).thenReturn(Optional.empty());
        when(employeeSkillRepository.save(any(EmployeeSkill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillGapRepository.findByUserIdAndSkillId(1L, 2L)).thenReturn(Optional.empty());
        when(skillGapRepository.save(any(SkillGap.class))).thenAnswer(inv -> inv.getArgument(0));

        // Submit 1 correct (q1 = 0) and 1 wrong (q2 = 1, expected 2)
        SubmitAssessmentRequest request = new SubmitAssessmentRequest(null, Map.of(401L, 0, 402L, 1));
        AssessmentResultDto result = assessmentService.submitAttempt("john@example.com", 55L, request);

        assertNotNull(result);
        assertEquals(50, result.score()); // 1/2 = 50%
        assertFalse(result.passed()); // pass score 60% > 50%
        assertEquals(1, result.correctAnswers());
        assertEquals(2, result.totalQuestions());
        assertEquals("COMPLETED", attempt.getStatus());
        assertEquals("AI_GENERATED", result.generationSource());

        // Verify explanations are returned in result after submission
        assertNotNull(result.questionResults());
        assertEquals(2, result.questionResults().size());
        assertEquals("Exp 1", result.questionResults().get(0).explanation());
        assertTrue(result.questionResults().get(0).isCorrect());
        assertFalse(result.questionResults().get(1).isCorrect());

        verify(employeeSkillRepository, times(1)).save(any(EmployeeSkill.class));
        verify(notificationService, times(1)).createNotification(any(User.class), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Employee cannot submit another employee's attempt (throws 403 FORBIDDEN)")
    void testSubmitAttempt_OtherEmployee_ThrowsForbidden() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(otherUser));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        setPrivateId(attempt, 55L);
        when(attemptRepository.findById(55L)).thenReturn(Optional.of(attempt));

        SubmitAssessmentRequest request = new SubmitAssessmentRequest(null, Map.of(10L, 0));

        ApiException ex = assertThrows(ApiException.class, () ->
                assessmentService.submitAttempt("jane@example.com", 55L, request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Submitting an already completed attempt throws 409 CONFLICT")
    void testSubmitAttempt_AlreadyCompleted_ThrowsConflict() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        attempt.setStatus("COMPLETED");
        setPrivateId(attempt, 55L);
        when(attemptRepository.findById(55L)).thenReturn(Optional.of(attempt));

        SubmitAssessmentRequest request = new SubmitAssessmentRequest(null, Map.of(10L, 0));

        ApiException ex = assertThrows(ApiException.class, () ->
                assessmentService.submitAttempt("john@example.com", 55L, request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("already been completed"));
    }

    @Test
    @DisplayName("Assessment history is preserved and loaded from database for user")
    void testGetUserSubmissions_PreservesHistory() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AssessmentSubmission s1 = new AssessmentSubmission(assessment, user, null, 85, true);
        setPrivateId(s1, 101L);
        when(submissionRepository.findByUserIdOrderByCompletedAtDesc(user.getId())).thenReturn(List.of(s1));

        AssessmentResponse r1 = new AssessmentResponse(s1, question1, 0, true);
        AssessmentResponse r2 = new AssessmentResponse(s1, question2, 1, true);
        when(responseRepository.findBySubmissionId(101L)).thenReturn(List.of(r1, r2));

        List<AssessmentResultDto> history = assessmentService.getUserSubmissions("john@example.com");

        assertNotNull(history);
        assertEquals(1, history.size());
        AssessmentResultDto first = history.get(0);
        assertEquals(101L, first.submissionId());
        assertEquals("Java Assessment", first.assessmentTitle());
        assertEquals(85, first.score());
        assertTrue(first.passed());
        assertEquals(2, first.totalQuestions());
        assertEquals(2, first.correctAnswers());
    }

    @Test
    @DisplayName("Recalculate skill gaps calculates required minus current level accurately")
    void testRecalculateSkillGaps_HighGap() {
        RoleSkillRequirement req = new RoleSkillRequirement("Full Stack Developer", skill, "Expert", "HIGH"); // Expert = 5
        when(requirementRepository.findByJobRole(anyString())).thenReturn(List.of(req));

        EmployeeSkill empSkill = new EmployeeSkill(user, skill, "Beginner"); // Beginner = 2
        when(employeeSkillRepository.findByUserId(user.getId())).thenReturn(List.of(empSkill));
        when(skillGapRepository.findByUserIdAndSkillId(user.getId(), skill.getId())).thenReturn(Optional.empty());
        when(skillGapRepository.save(any(SkillGap.class))).thenAnswer(inv -> inv.getArgument(0));

        List<SkillGapDto> gaps = assessmentService.recalculateSkillGapsForUser(user);

        assertNotNull(gaps);
        assertEquals(1, gaps.size());
        SkillGapDto gap = gaps.get(0);
        assertEquals(2, gap.currentLevel());
        assertEquals(5, gap.requiredLevel());
        assertEquals(3, gap.gapSize()); // 5 - 2 = 3
        assertEquals("HIGH", gap.priority()); // gap >= 2 -> HIGH
    }

    @Test
    @DisplayName("Recalculate skill gaps sets priority ON_TRACK when current >= required")
    void testRecalculateSkillGaps_OnTrack() {
        RoleSkillRequirement req = new RoleSkillRequirement("Full Stack Developer", skill, "Intermediate", "MEDIUM"); // 3
        when(requirementRepository.findByJobRole(anyString())).thenReturn(List.of(req));

        EmployeeSkill empSkill = new EmployeeSkill(user, skill, "Advanced"); // 4
        when(employeeSkillRepository.findByUserId(user.getId())).thenReturn(List.of(empSkill));
        when(skillGapRepository.findByUserIdAndSkillId(user.getId(), skill.getId())).thenReturn(Optional.empty());
        when(skillGapRepository.save(any(SkillGap.class))).thenAnswer(inv -> inv.getArgument(0));

        List<SkillGapDto> gaps = assessmentService.recalculateSkillGapsForUser(user);

        assertNotNull(gaps);
        assertEquals(1, gaps.size());
        SkillGapDto gap = gaps.get(0);
        assertEquals(0, gap.gapSize());
        assertEquals("ON_TRACK", gap.priority());
    }

    /* ---------------------------------------------------------------- Role Authorization Tests */

    @Test
    @DisplayName("Owner employee can view their own IN_PROGRESS attempt")
    void testGetAttemptById_Owner_InProgress_Allowed() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(99L)).thenReturn(List.of(question1));

        AssessmentAttemptDto result = assessmentService.getAttemptById("john@example.com", 99L);
        assertNotNull(result);
        assertEquals(99L, result.attemptId());
        assertEquals("IN_PROGRESS", result.status());
    }

    @Test
    @DisplayName("Non-owner cannot view an IN_PROGRESS attempt regardless of role (403 FORBIDDEN)")
    void testGetAttemptById_NonOwner_InProgress_ThrowsForbidden() {
        User manager = new User();
        setPrivateId(manager, 10L);
        manager.setEmail("manager@example.com");
        manager.setRole(Role.MANAGER);
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));

        ApiException ex = assertThrows(ApiException.class, () ->
                assessmentService.getAttemptById("manager@example.com", 99L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("In-progress assessment attempts are private"));
    }

    @Test
    @DisplayName("Manager can view completed attempt for employee in their managed team")
    void testGetAttemptById_Manager_InTeam_Allowed() {
        User manager = new User();
        setPrivateId(manager, 10L);
        manager.setEmail("manager@example.com");
        manager.setRole(Role.MANAGER);
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        attempt.setStatus("COMPLETED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));

        Department dept = new Department("Engineering", "ENG", "Eng Dept", null);
        Team team = new Team(dept, "Engineering Team", manager);
        TeamMembership tm = new TeamMembership(team, user, "Developer");
        when(teamMembershipRepository.findByManagerUserId(10L)).thenReturn(List.of(tm));
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(99L)).thenReturn(List.of(question1));

        AssessmentAttemptDto result = assessmentService.getAttemptById("manager@example.com", 99L);
        assertNotNull(result);
        assertEquals(99L, result.attemptId());
        assertEquals("COMPLETED", result.status());
    }

    @Test
    @DisplayName("Manager cannot view completed attempt for employee outside their team (403 FORBIDDEN)")
    void testGetAttemptById_Manager_NotInTeam_ThrowsForbidden() {
        User manager = new User();
        setPrivateId(manager, 10L);
        manager.setEmail("manager@example.com");
        manager.setRole(Role.MANAGER);
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        attempt.setStatus("COMPLETED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));

        when(teamMembershipRepository.findByManagerUserId(10L)).thenReturn(List.of()); // Not in manager's team

        ApiException ex = assertThrows(ApiException.class, () ->
                assessmentService.getAttemptById("manager@example.com", 99L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Managers can only view assessments for employees in their managed teams"));
    }

    @Test
    @DisplayName("Mentor can view completed attempt for their active assigned mentee")
    void testGetAttemptById_Mentor_ActiveMentee_Allowed() {
        User mentor = new User();
        setPrivateId(mentor, 20L);
        mentor.setEmail("mentor@example.com");
        mentor.setRole(Role.MENTOR);
        when(userRepository.findByEmail("mentor@example.com")).thenReturn(Optional.of(mentor));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        attempt.setStatus("COMPLETED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));

        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(20L, 1L, AssignmentStatus.ACTIVE)).thenReturn(true);
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(99L)).thenReturn(List.of(question1));

        AssessmentAttemptDto result = assessmentService.getAttemptById("mentor@example.com", 99L);
        assertNotNull(result);
        assertEquals(99L, result.attemptId());
    }

    @Test
    @DisplayName("Mentor cannot view completed attempt for non-mentee (403 FORBIDDEN)")
    void testGetAttemptById_Mentor_NotMentee_ThrowsForbidden() {
        User mentor = new User();
        setPrivateId(mentor, 20L);
        mentor.setEmail("mentor@example.com");
        mentor.setRole(Role.MENTOR);
        when(userRepository.findByEmail("mentor@example.com")).thenReturn(Optional.of(mentor));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        attempt.setStatus("COMPLETED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));

        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(20L, 1L, AssignmentStatus.ACTIVE)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                assessmentService.getAttemptById("mentor@example.com", 99L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Mentors can only view assessments for their active assigned mentees"));
    }

    @Test
    @DisplayName("Department Head can view completed attempt for employee in their department")
    void testGetAttemptById_DepartmentHead_InDept_Allowed() {
        User deptHead = new User();
        setPrivateId(deptHead, 30L);
        deptHead.setEmail("head@example.com");
        deptHead.setRole(Role.DEPARTMENT_HEAD);
        when(userRepository.findByEmail("head@example.com")).thenReturn(Optional.of(deptHead));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        attempt.setStatus("COMPLETED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));

        Department dept = new Department("Engineering", "ENG", "Eng Dept", deptHead);
        setPrivateId(dept, 5L);
        when(departmentRepository.findByHeadUserId(30L)).thenReturn(Optional.of(dept));

        Team team = new Team(dept, "Dev Team", null);
        TeamMembership tm = new TeamMembership(team, user, "Developer");
        when(teamMembershipRepository.findByDepartmentId(5L)).thenReturn(List.of(tm));
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(99L)).thenReturn(List.of(question1));

        AssessmentAttemptDto result = assessmentService.getAttemptById("head@example.com", 99L);
        assertNotNull(result);
        assertEquals(99L, result.attemptId());
    }

    @Test
    @DisplayName("Admin and HR Specialist can view completed attempt for any employee")
    void testGetAttemptById_AdminAndHr_Allowed() {
        User admin = new User();
        setPrivateId(admin, 40L);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        attempt.setStatus("COMPLETED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(99L)).thenReturn(List.of(question1));

        AssessmentAttemptDto result = assessmentService.getAttemptById("admin@example.com", 99L);
        assertNotNull(result);
        assertEquals(99L, result.attemptId());
    }

    @Test
    @DisplayName("Unrelated employee cannot view another employee's completed attempt (403 FORBIDDEN)")
    void testGetAttemptById_UnrelatedEmployee_ThrowsForbidden() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(otherUser));

        AssessmentAttempt attempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        attempt.setStatus("COMPLETED");
        setPrivateId(attempt, 99L);
        when(attemptRepository.findById(99L)).thenReturn(Optional.of(attempt));

        ApiException ex = assertThrows(ApiException.class, () ->
                assessmentService.getAttemptById("jane@example.com", 99L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("Manager can assign assessment to employee in their managed team")
    void testAssignAssessment_Manager_InTeam_Success() {
        User manager = new User();
        setPrivateId(manager, 10L);
        manager.setFullName("Manager Bob");
        manager.setEmail("manager@example.com");
        manager.setRole(Role.MANAGER);

        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));

        Department dept = new Department("Engineering", "ENG", "Eng Dept", null);
        Team team = new Team(dept, "Engineering Team", manager);
        TeamMembership tm = new TeamMembership(team, user, "Developer");
        when(teamMembershipRepository.findByManagerUserId(10L)).thenReturn(List.of(tm));
        when(assignmentRepository.save(any(AssessmentAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignAssessmentRequest req = new AssignAssessmentRequest(1L, 1L, java.time.LocalDate.now().plusDays(1));
        AssessmentAssignmentDto dto = assessmentService.assignAssessment("manager@example.com", req);

        assertNotNull(dto);
        assertEquals(1L, dto.userId());
        assertEquals("Java Assessment", dto.assessmentTitle());
        verify(notificationService, times(1)).createNotification(eq(user), eq("ASSESSMENT_ASSIGNED"), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Manager cannot assign assessment to employee outside their team (403 FORBIDDEN)")
    void testAssignAssessment_Manager_NotInTeam_ThrowsForbidden() {
        User manager = new User();
        setPrivateId(manager, 10L);
        manager.setEmail("manager@example.com");
        manager.setRole(Role.MANAGER);

        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));

        when(teamMembershipRepository.findByManagerUserId(10L)).thenReturn(List.of()); // Not in manager's team

        AssignAssessmentRequest req = new AssignAssessmentRequest(1L, 2L, java.time.LocalDate.now().plusDays(1));
        ApiException ex = assertThrows(ApiException.class, () ->
                assessmentService.assignAssessment("manager@example.com", req));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Managers can only assign assessments to employees in their managed teams"));
    }

    /* ---------------------------------------------------------------- Proficiency & Concurrency Tests */

    @Test
    @DisplayName("Proficiency levels are consistently normalized across all 5 canonical levels")
    void testNormalizeProficiencyLevel_AllCanonicalLevels() {
        assertEquals("Unaware", assessmentService.normalizeProficiencyLevel("unaware"));
        assertEquals("Beginner", assessmentService.normalizeProficiencyLevel("BEGINNER"));
        assertEquals("Intermediate", assessmentService.normalizeProficiencyLevel("Intermediate"));
        assertEquals("Advanced", assessmentService.normalizeProficiencyLevel("advanced"));
        assertEquals("Expert", assessmentService.normalizeProficiencyLevel("EXPERT"));
        // Defaults to Intermediate when null, empty, or unrecognized
        assertEquals("Intermediate", assessmentService.normalizeProficiencyLevel(null));
        assertEquals("Intermediate", assessmentService.normalizeProficiencyLevel(""));
        assertEquals("Intermediate", assessmentService.normalizeProficiencyLevel("Ninja"));
    }

    @Test
    @DisplayName("Percentage mapping covers all 5 proficiency tiers accurately")
    void testMapPercentageToProficiency_Tiers() {
        assertEquals("Unaware", assessmentService.mapPercentageToProficiency(0));
        assertEquals("Unaware", assessmentService.mapPercentageToProficiency(25));
        assertEquals("Beginner", assessmentService.mapPercentageToProficiency(26));
        assertEquals("Beginner", assessmentService.mapPercentageToProficiency(50));
        assertEquals("Intermediate", assessmentService.mapPercentageToProficiency(51));
        assertEquals("Intermediate", assessmentService.mapPercentageToProficiency(70));
        assertEquals("Advanced", assessmentService.mapPercentageToProficiency(71));
        assertEquals("Advanced", assessmentService.mapPercentageToProficiency(85));
        assertEquals("Expert", assessmentService.mapPercentageToProficiency(86));
        assertEquals("Expert", assessmentService.mapPercentageToProficiency(100));
    }

    @Test
    @DisplayName("startAssessment performs pessimistic row locking on user to prevent race conditions")
    void testStartAssessment_PessimisticRowLocking() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(skillRepository.findById(2L)).thenReturn(Optional.of(skill));
        when(employeeSkillRepository.findByUserIdAndSkillId(1L, 2L)).thenReturn(Optional.empty());

        AssessmentAttempt existingAttempt = new AssessmentAttempt(user, skill, assessment, null, "Intermediate", "Unaware", "AI_GENERATED");
        setPrivateId(existingAttempt, 88L);
        when(attemptRepository.findByUserIdAndSkillIdAndStatus(1L, 2L, "IN_PROGRESS"))
                .thenReturn(List.of(existingAttempt));

        AssessmentQuestion existingQ = new AssessmentQuestion(assessment, skill, existingAttempt, "Existing?", "[\"A\",\"B\",\"C\",\"D\"]", 1, 1, "EASY", "General", "Exp", 1);
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(88L)).thenReturn(List.of(existingQ));

        StartAssessmentRequest req = new StartAssessmentRequest(2L, "Intermediate", null, null);
        assessmentService.startAssessment("john@example.com", req);

        // Verify that pessimistic write lock was acquired on user record
        verify(userRepository, times(1)).findByIdForUpdate(1L);
    }

    @Test
    @DisplayName("Concurrent startAssessment requests for same user and skill serialize and return the same attempt")
    void testStartAssessment_ConcurrentRequests_ReturnsSameAttempt() throws Exception {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(skillRepository.findById(2L)).thenReturn(Optional.of(skill));
        when(employeeSkillRepository.findByUserIdAndSkillId(1L, 2L)).thenReturn(Optional.empty());
        when(assessmentRepository.findAll()).thenReturn(List.of(assessment));
        when(attemptRepository.findTop5ByUserIdAndSkillIdOrderByStartedAtDesc(1L, 2L)).thenReturn(List.of());

        List<AssessmentAttempt> dbActiveAttempts = Collections.synchronizedList(new ArrayList<>());
        List<AssessmentQuestion> dbQuestions = Collections.synchronizedList(new ArrayList<>());

        when(attemptRepository.findByUserIdAndSkillIdAndStatus(1L, 2L, "IN_PROGRESS"))
                .thenAnswer(inv -> new ArrayList<>(dbActiveAttempts));

        when(attemptRepository.save(any(AssessmentAttempt.class))).thenAnswer(inv -> {
            AssessmentAttempt a = inv.getArgument(0);
            setPrivateId(a, 999L);
            if (!dbActiveAttempts.contains(a)) {
                dbActiveAttempts.add(a);
            }
            return a;
        });

        AssessmentQuestion q = new AssessmentQuestion(assessment, skill, "Fallback Q?", "[\"A\",\"B\"]", 0, 1);
        when(questionRepository.findBySkillIdAndAttemptIdIsNull(2L)).thenReturn(List.of(q));
        when(questionRepository.save(any(AssessmentQuestion.class))).thenAnswer(inv -> {
            AssessmentQuestion savedQ = inv.getArgument(0);
            setPrivateId(savedQ, 1001L);
            dbQuestions.add(savedQ);
            return savedQ;
        });
        when(questionRepository.findByAttemptIdOrderByQuestionOrderAsc(999L))
                .thenAnswer(inv -> new ArrayList<>(dbQuestions));

        StartAssessmentRequest req = new StartAssessmentRequest(2L, "Intermediate", null, null);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Future<AssessmentAttemptDto> f1 = executor.submit(() -> assessmentService.startAssessment("john@example.com", req));
        java.util.concurrent.Future<AssessmentAttemptDto> f2 = executor.submit(() -> assessmentService.startAssessment("john@example.com", req));

        AssessmentAttemptDto res1 = f1.get(5, java.util.concurrent.TimeUnit.SECONDS);
        AssessmentAttemptDto res2 = f2.get(5, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertNotNull(res1);
        assertNotNull(res2);
        assertEquals(999L, res1.attemptId());
        assertEquals(999L, res2.attemptId());
        // Verify only one attempt entity was created in total
        assertEquals(1, dbActiveAttempts.size());
        verify(attemptRepository, times(2)).save(any(AssessmentAttempt.class));
    }
}
