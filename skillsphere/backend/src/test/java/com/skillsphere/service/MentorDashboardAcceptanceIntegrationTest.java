package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.dto.MentorDtos.*;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MentorDashboardAcceptanceIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;
    @Autowired
    private MentorProfileRepository mentorProfileRepository;
    @Autowired
    private MentorAvailabilityRepository mentorAvailabilityRepository;
    @Autowired
    private MentorAssignmentRepository mentorAssignmentRepository;
    @Autowired
    private MentorshipRequestRepository mentorshipRequestRepository;
    @Autowired
    private MentorshipGoalRepository mentorshipGoalRepository;
    @Autowired
    private GoalActionItemRepository goalActionItemRepository;
    @Autowired
    private MentorshipSessionRepository mentorshipSessionRepository;
    @Autowired
    private MentorshipFeedbackRepository mentorshipFeedbackRepository;
    @Autowired
    private MentorEvaluationRepository mentorEvaluationRepository;
    @Autowired
    private MenteeSkillConsentRepository menteeSkillConsentRepository;
    @Autowired
    private SharedResourceRepository sharedResourceRepository;
    @Autowired
    private KnowledgeArticleRepository knowledgeArticleRepository;
    @Autowired
    private KnowledgeCategoryRepository knowledgeCategoryRepository;
    @Autowired
    private SkillCategoryRepository skillCategoryRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private EmployeeSkillRepository employeeSkillRepository;
    @Autowired
    private SkillGapRepository skillGapRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private TeamMembershipRepository teamMembershipRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MentorDashboardService mentorDashboardService;
    @Autowired
    private MentorshipRequestService mentorshipRequestService;
    @Autowired
    private ManagerService managerService;

    private User mentorA;
    private User mentorB;
    private User employeeA;
    private User employeeB;
    private User managerA;
    private Department department;
    private Team teamA;
    private SkillCategory skillCategory;
    private Skill skillCloud;

    private User createTestUser(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
        u.setFullName(name);
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        u.setEmployeeId("EMP-" + UUID.randomUUID().toString().substring(0, 7).toUpperCase());
        return userRepository.save(u);
    }

    @BeforeEach
    void setUp() {
        department = departmentRepository.save(new Department("Cloud Platform Group", "CPG", "Platform Architecture", null));

        mentorA = createTestUser("mentor_a_" + UUID.randomUUID() + "@skillsphere.com", "Mentor Alice", Role.MENTOR);
        mentorB = createTestUser("mentor_b_" + UUID.randomUUID() + "@skillsphere.com", "Mentor Bob", Role.MENTOR);

        managerA = createTestUser("mgr_a_" + UUID.randomUUID() + "@skillsphere.com", "Manager Mark", Role.MANAGER);
        employeeA = createTestUser("emp_a_" + UUID.randomUUID() + "@skillsphere.com", "Mentee Charlie", Role.EMPLOYEE);
        employeeB = createTestUser("emp_b_" + UUID.randomUUID() + "@skillsphere.com", "Mentee Dave", Role.EMPLOYEE);

        teamA = teamRepository.save(new Team(department, "Alpha Squad", managerA));
        teamMembershipRepository.save(new TeamMembership(teamA, employeeA));
        teamMembershipRepository.save(new TeamMembership(teamA, employeeB));

        EmployeeProfile profA = employeeProfileRepository.findByUserId(employeeA.getId()).orElseGet(() -> new EmployeeProfile(employeeA));
        profA.setJobRole("Software Developer");
        profA.setDepartment("Cloud Platform Group");
        profA.setExperienceLevel("Intermediate");
        profA.setBio("Software engineer specializing in backend systems.");
        employeeProfileRepository.save(profA);

        EmployeeProfile profB = employeeProfileRepository.findByUserId(employeeB.getId()).orElseGet(() -> new EmployeeProfile(employeeB));
        profB.setJobRole("QA Engineer");
        profB.setDepartment("Cloud Platform Group");
        profB.setExperienceLevel("Entry level");
        employeeProfileRepository.save(profB);

        skillCategory = skillCategoryRepository.save(new SkillCategory("Cloud Architecture", "Cloud computing and microservices"));
        skillCloud = skillRepository.save(new Skill(skillCategory, "Kubernetes Platform", "Container orchestration and deployments"));
    }

    @Test
    @DisplayName("Module 1 & 2: Mentor Overview, Profile Lifecycle, and Auto-Creation Persistence")
    void testMentorOverviewAndProfileAutoCreationAndPersistence() {
        // 1. Initial Overview call auto-creates profile in PostgreSQL
        MentorDashboardOverviewDto overview = mentorDashboardService.getOverview(mentorA.getEmail());
        assertNotNull(overview);
        assertNotNull(overview.mentorProfile());
        assertEquals(mentorA.getFullName(), overview.mentorProfile().fullName());
        assertEquals(0, overview.activeMenteesCount());
        assertEquals(0, overview.pendingRequestsCount());

        // 2. Profile read-back from PostgreSQL
        MentorProfileDto profile = mentorDashboardService.getProfile(mentorA.getEmail());
        assertNotNull(profile);
        assertEquals(mentorA.getEmail(), profile.email());
        assertEquals(5, profile.maxMentees());

        // 3. Update Mentor Profile with availability slots
        MentorAvailabilitySlotDto slot = new MentorAvailabilitySlotDto(null, "MONDAY", "10:00", "12:00", "VIRTUAL", true);
        UpdateMentorProfileRequest updateReq = new UpdateMentorProfileRequest(
                "Principal Cloud & Systems Architect",
                "Dedicated technical mentor with 12+ years in distributed systems architecture.",
                "Kubernetes, Microservices, Java 21, Cloud Design",
                4,
                true,
                "VIRTUAL",
                List.of(slot)
        );

        MentorProfileDto updated = mentorDashboardService.updateProfile(mentorA.getEmail(), updateReq);
        assertNotNull(updated);
        assertEquals("Principal Cloud & Systems Architect", updated.headline());
        assertEquals(4, updated.maxMentees());
        assertFalse(updated.availabilitySlots().isEmpty());
        assertEquals("MONDAY", updated.availabilitySlots().get(0).dayOfWeek());

        // Verify entity updated in PostgreSQL
        MentorProfile mp = mentorProfileRepository.findByUserId(mentorA.getId()).orElseThrow();
        assertEquals("Principal Cloud & Systems Architect", mp.getHeadline());
        assertEquals(4, mp.getMaxMentees());
        List<MentorAvailability> slots = mentorAvailabilityRepository.findByMentorIdOrderByDayOfWeekAscStartTimeAsc(mentorA.getId());
        assertEquals(1, slots.size());
        assertEquals("MONDAY", slots.get(0).getDayOfWeek());
    }

    @Test
    @DisplayName("Module 3: Mentorship Requests, Manager Referral Integration, and Capacity Verification")
    void testMentorshipRequestLifecycleAndCapacityEnforcement() {
        // 1. Employee creates a direct mentorship request
        CreateMentorshipRequestDto directReq = new CreateMentorshipRequestDto(
                mentorA.getId(),
                "Interested in advancing my container architecture skills under your mentorship."
        );
        MentorshipRequestResponseDto directCreated = mentorshipRequestService.createRequest(employeeA.getEmail(), directReq);
        assertNotNull(directCreated);
        assertEquals("PENDING", directCreated.status());
        assertEquals(mentorA.getFullName(), directCreated.mentorName());

        // 2. Manager refers Mentor A to Employee B via ManagerService
        ManagerMentorReferralRequest referralReq = new ManagerMentorReferralRequest(
                employeeB.getId(),
                mentorA.getId(),
                skillCloud.getId(),
                "High priority mentorship referral for upcoming cloud migration project."
        );
        assertDoesNotThrow(() -> managerService.referMentor(managerA.getEmail(), referralReq));

        // Verify referral persisted in mentorship_requests
        List<MentorshipRequest> reqsForMentorA = mentorshipRequestRepository.findByMentorIdOrderByCreatedAtDesc(mentorA.getId());
        assertEquals(2, reqsForMentorA.size());
        MentorshipRequest referral = reqsForMentorA.stream()
                .filter(r -> r.getEmployee().getId().equals(employeeB.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(referral);
        assertEquals("PENDING", referral.getStatus());
        assertTrue(referral.getMessage().contains("Manager Referral from"));

        // 3. Mentor B attempts to respond to Mentor A's request -> throws 403 FORBIDDEN
        RespondMentorshipRequestDto acceptDto = new RespondMentorshipRequestDto("ACCEPTED", "Welcome to the cloud mentorship track!");
        ApiException exCrossRespond = assertThrows(ApiException.class, () ->
                mentorDashboardService.respondToRequest(mentorB.getEmail(), directCreated.id(), acceptDto)
        );
        assertEquals(HttpStatus.FORBIDDEN, exCrossRespond.getStatus());

        // 4. Mentor A accepts the direct request -> creates active MentorAssignment
        MentorshipRequestResponseDto accepted = mentorDashboardService.respondToRequest(mentorA.getEmail(), directCreated.id(), acceptDto);
        assertNotNull(accepted);
        assertEquals("ACCEPTED", accepted.status());

        // Verify active assignment in PostgreSQL
        List<MentorAssignment> active = mentorAssignmentRepository.findByMentorIdAndStatus(mentorA.getId(), AssignmentStatus.ACTIVE);
        assertEquals(1, active.size());
        assertEquals(employeeA.getId(), active.get(0).getEmployee().getId());

        // 5. Attempting to accept the already-processed request throws 400 BAD REQUEST
        ApiException exDoubleProcess = assertThrows(ApiException.class, () ->
                mentorDashboardService.respondToRequest(mentorA.getEmail(), directCreated.id(), acceptDto)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exDoubleProcess.getStatus());

        // 6. Test Max Capacity Conflict: set maxMentees = 1, try accepting second request
        MentorProfile mp = mentorProfileRepository.findByUserId(mentorA.getId()).orElseThrow();
        mp.setMaxMentees(1);
        mentorProfileRepository.save(mp);

        ApiException exCapacity = assertThrows(ApiException.class, () ->
                mentorDashboardService.respondToRequest(mentorA.getEmail(), referral.getId(), acceptDto)
        );
        assertEquals(HttpStatus.CONFLICT, exCapacity.getStatus());
        assertTrue(exCapacity.getMessage().contains("maximum mentee capacity"));
    }

    @Test
    @DisplayName("Module 4 & 5: Mentee Management, Dossier Access, and Skill Privacy Isolation")
    void testMenteeManagementDossierAndSkillIsolation() {
        // Create active assignment between Mentor A and Employee A
        MentorAssignment assignment = mentorAssignmentRepository.save(new MentorAssignment(
                mentorA, employeeA, mentorA, "Active architectural guidance"
        ));

        // Create unverified skill and skill gap for Employee A
        EmployeeSkill es = new EmployeeSkill(employeeA, skillCloud, "Intermediate");
        es.setVerified(true);
        employeeSkillRepository.save(es);

        SkillGap gap = new SkillGap(employeeA, skillCloud, 3, 4, 1, "HIGH");
        skillGapRepository.save(gap);

        // 1. Mentor A retrieves active mentees -> sees Employee A
        List<MenteeOverviewItemDto> menteesA = mentorDashboardService.getMentees(mentorA.getEmail(), "ACTIVE");
        assertEquals(1, menteesA.size());
        assertEquals(employeeA.getFullName(), menteesA.get(0).employeeName());

        // 2. Mentor B retrieves active mentees -> does NOT see Employee A (isolation)
        List<MenteeOverviewItemDto> menteesB = mentorDashboardService.getMentees(mentorB.getEmail(), "ACTIVE");
        assertTrue(menteesB.isEmpty());

        // 3. Mentor A accesses Mentee Dossier
        MenteeDossierDto dossier = mentorDashboardService.getMenteeDossier(mentorA.getEmail(), assignment.getId());
        assertNotNull(dossier);
        assertEquals(employeeA.getFullName(), dossier.summary().employeeName());
        assertNotNull(dossier.skills());
        assertTrue(dossier.skills().consentGranted());

        // 4. Mentor B attempts to access Mentor A's mentee dossier -> throws 403 FORBIDDEN
        ApiException exCrossDossier = assertThrows(ApiException.class, () ->
                mentorDashboardService.getMenteeDossier(mentorB.getEmail(), assignment.getId())
        );
        assertEquals(HttpStatus.FORBIDDEN, exCrossDossier.getStatus());

        // 5. Mentor A views mentee skill overview via /mentees/{id}/skills
        MenteeSkillDetailDto skillDetail = mentorDashboardService.getMenteeSkills(mentorA.getEmail(), employeeA.getId());
        assertNotNull(skillDetail);
        assertTrue(skillDetail.consentGranted());
        assertFalse(skillDetail.skills().isEmpty());
        assertEquals(skillCloud.getName(), skillDetail.skills().get(0).skillName());

        // 6. Mentor B attempts to view skills for employee they do not mentor -> throws 403 FORBIDDEN
        ApiException exCrossSkill = assertThrows(ApiException.class, () ->
                mentorDashboardService.getMenteeSkills(mentorB.getEmail(), employeeA.getId())
        );
        assertEquals(HttpStatus.FORBIDDEN, exCrossSkill.getStatus());

        // 7. Mentor A concludes mentorship via endMentorship
        EndMentorshipRequest endReq = new EndMentorshipRequest("Completed key cloud architecture milestones successfully.");
        MenteeOverviewItemDto ended = mentorDashboardService.endMentorship(mentorA.getEmail(), assignment.getId(), endReq);
        assertNotNull(ended);
        assertEquals("COMPLETED", ended.status());

        // Verify assignment status in DB
        MentorAssignment refreshedAssignment = mentorAssignmentRepository.findById(assignment.getId()).orElseThrow();
        assertEquals(AssignmentStatus.COMPLETED, refreshedAssignment.getStatus());
        assertNotNull(refreshedAssignment.getEndedAt());

        // Attempting to end an already completed mentorship throws 400 BAD REQUEST
        ApiException exAlreadyEnded = assertThrows(ApiException.class, () ->
                mentorDashboardService.endMentorship(mentorA.getEmail(), assignment.getId(), endReq)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exAlreadyEnded.getStatus());
    }

    @Test
    @DisplayName("Module 6 & 7: Development Goals, Action Items, and Mentoring Sessions with Conflict Detection")
    void testGoalsActionItemsAndSessionsLifecycle() {
        MentorAssignment assignment = mentorAssignmentRepository.save(new MentorAssignment(
                mentorA, employeeA, mentorA, "Active architectural guidance"
        ));

        // 1. Mentor A creates a development goal
        CreateMentorGoalRequest goalReq = new CreateMentorGoalRequest(
                assignment.getId(),
                "Design Production Kubernetes Cluster",
                "Build multi-zone HA Kubernetes cluster configuration with ArgoCD",
                LocalDate.now().plusMonths(1),
                "HIGH",
                skillCloud.getId()
        );
        MentorGoalDetailDto createdGoal = mentorDashboardService.createGoal(mentorA.getEmail(), goalReq);
        assertNotNull(createdGoal);
        assertEquals("Design Production Kubernetes Cluster", createdGoal.title());
        assertEquals("IN_PROGRESS", createdGoal.status());

        // 2. Mentor A adds an action item to the goal
        CreateGoalActionItemRequest itemReq = new CreateGoalActionItemRequest(
                "Configure Ingress Controller with TLS",
                "Deploy cert-manager and ingress-nginx",
                LocalDate.now().plusWeeks(2)
        );
        GoalActionItemDto actionItem = mentorDashboardService.createGoalActionItem(mentorA.getEmail(), createdGoal.id(), itemReq);
        assertNotNull(actionItem);
        assertFalse(actionItem.isCompleted());

        // Toggle action item
        GoalActionItemDto toggledItem = mentorDashboardService.toggleGoalActionItem(mentorA.getEmail(), actionItem.id());
        assertTrue(toggledItem.isCompleted());
        assertNotNull(toggledItem.completedAt());

        // 3. Mentor B attempts to update Mentor A's goal -> throws 403 FORBIDDEN
        UpdateMentorGoalRequest updateGoalReq = new UpdateMentorGoalRequest("COMPLETED", "Goal updated", "Updated desc", LocalDate.now().plusDays(20), "HIGH");
        ApiException exCrossGoal = assertThrows(ApiException.class, () ->
                mentorDashboardService.updateGoal(mentorB.getEmail(), createdGoal.id(), updateGoalReq)
        );
        assertEquals(HttpStatus.FORBIDDEN, exCrossGoal.getStatus());

        // Mentor A updates goal status to COMPLETED -> succeeds
        MentorGoalDetailDto updatedGoal = mentorDashboardService.updateGoal(mentorA.getEmail(), createdGoal.id(), updateGoalReq);
        assertEquals("COMPLETED", updatedGoal.status());

        // 4. Session Scheduling: Mentor A schedules a session for tomorrow at 10:00 AM (duration 45m)
        Instant sessionTime = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        CreateMentorSessionRequest sessionReq = new CreateMentorSessionRequest(
                assignment.getId(),
                "Kubernetes Cluster Review",
                "Deep dive into ArgoCD GitOps pipelines",
                sessionTime,
                45,
                "https://meet.google.com/test-meet"
        );
        MentorSessionSummaryDto scheduledSession = mentorDashboardService.createSession(mentorA.getEmail(), sessionReq);
        assertNotNull(scheduledSession);
        assertEquals("SCHEDULED", scheduledSession.status());

        // 5. Conflict Detection: Attempting to schedule an overlapping session (15m into the 45m window)
        CreateMentorSessionRequest conflictReq = new CreateMentorSessionRequest(
                assignment.getId(),
                "Overlapping Session",
                "Conflict attempt",
                sessionTime.plus(15, ChronoUnit.MINUTES),
                30,
                "https://meet.google.com/conflict"
        );
        ApiException exConflict = assertThrows(ApiException.class, () ->
                mentorDashboardService.createSession(mentorA.getEmail(), conflictReq)
        );
        assertEquals(HttpStatus.CONFLICT, exConflict.getStatus());
        assertTrue(exConflict.getMessage().contains("Time conflict"));

        // 6. Mentor A updates session status to COMPLETED
        MentorSessionSummaryDto completedSession = mentorDashboardService.updateSessionStatus(
                mentorA.getEmail(), scheduledSession.id(), "COMPLETED"
        );
        assertEquals("COMPLETED", completedSession.status());

        // 7. Mentor A updates session notes
        UpdateSessionNotesRequest notesReq = new UpdateSessionNotesRequest(
                "Mentee demonstrated strong mastery of Kubernetes stateful sets.",
                "Next session should cover service mesh mTLS."
        );
        MentorSessionSummaryDto updatedNotes = mentorDashboardService.updateSessionNotes(
                mentorA.getEmail(), scheduledSession.id(), notesReq
        );
        assertEquals("Mentee demonstrated strong mastery of Kubernetes stateful sets.", updatedNotes.sharedNotes());

        // Mentor B attempts to update session notes -> throws 403 FORBIDDEN
        ApiException exCrossNotes = assertThrows(ApiException.class, () ->
                mentorDashboardService.updateSessionNotes(mentorB.getEmail(), scheduledSession.id(), notesReq)
        );
        assertEquals(HttpStatus.FORBIDDEN, exCrossNotes.getStatus());
    }

    @Test
    @DisplayName("Module 8 & 9: Resource Sharing and Knowledge Contributions")
    void testResourceSharingAndKnowledgeContributions() {
        MentorAssignment assignment = mentorAssignmentRepository.save(new MentorAssignment(
                mentorA, employeeA, mentorA, "Active pairing"
        ));

        // 1. Mentor A shares a resource with the mentee
        CreateMentorSharedResourceRequest resReq = new CreateMentorSharedResourceRequest(
                assignment.getId(),
                "Production Kubernetes Best Practices Guide",
                "https://kubernetes.io/docs/concepts/cluster-administration/",
                "DOCUMENTATION",
                "Core reference for cluster hardening and security context",
                skillCloud.getId()
        );
        MentorSharedResourceDto resource = mentorDashboardService.createResource(mentorA.getEmail(), resReq);
        assertNotNull(resource);
        assertEquals("Production Kubernetes Best Practices Guide", resource.title());

        // Verify resource stored in PostgreSQL
        List<SharedResource> resources = sharedResourceRepository.findByMentorIdOrderByCreatedAtDesc(mentorA.getId());
        assertEquals(1, resources.size());

        // 2. Mentor B attempts to delete Mentor A's resource -> throws 403 FORBIDDEN
        ApiException exDeleteResource = assertThrows(ApiException.class, () ->
                mentorDashboardService.deleteResource(mentorB.getEmail(), resource.id())
        );
        assertEquals(HttpStatus.FORBIDDEN, exDeleteResource.getStatus());

        // 3. Mentor A publishes a Knowledge Article
        KnowledgeCategory cat = knowledgeCategoryRepository.save(new KnowledgeCategory("Architecture", "Software architecture"));
        CreateOrUpdateMentorArticleRequest articleReq = new CreateOrUpdateMentorArticleRequest(
                cat.getId(),
                "Zero Trust Microservices in Cloud Environments",
                "Detailed architectural guide on implementing SPIFFE/SPIRE with Envoy proxies.",
                "cloud,security,microservices",
                "PUBLISHED"
        );
        MentorArticleDto article = mentorDashboardService.createArticle(mentorA.getEmail(), articleReq);
        assertNotNull(article);
        assertEquals("Zero Trust Microservices in Cloud Environments", article.title());

        // 4. Mentor B attempts to update or delete Mentor A's article -> throws 403 FORBIDDEN
        ApiException exUpdateArticle = assertThrows(ApiException.class, () ->
                mentorDashboardService.updateArticle(mentorB.getEmail(), article.id(), articleReq)
        );
        assertEquals(HttpStatus.FORBIDDEN, exUpdateArticle.getStatus());

        ApiException exDeleteArticle = assertThrows(ApiException.class, () ->
                mentorDashboardService.deleteArticle(mentorB.getEmail(), article.id())
        );
        assertEquals(HttpStatus.FORBIDDEN, exDeleteArticle.getStatus());

        // Mentor A deletes own article -> succeeds
        assertDoesNotThrow(() -> mentorDashboardService.deleteArticle(mentorA.getEmail(), article.id()));
    }

    @Test
    @DisplayName("Module 10 & 11: Feedback Overview, Evaluation Delivery, and Notifications Scoping")
    void testFeedbackOverviewAndEvaluationDelivery() {
        MentorAssignment assignment = mentorAssignmentRepository.save(new MentorAssignment(
                mentorA, employeeA, mentorA, "Active pairing"
        ));

        Instant sessionTime = Instant.now().minus(2, ChronoUnit.DAYS);
        MentorshipSession session = mentorshipSessionRepository.save(new MentorshipSession(
                assignment, "Architecture Review Session", "Review", sessionTime, 60, "https://meet.google.com/test"
        ));
        session.setStatus(SessionStatus.COMPLETED);
        mentorshipSessionRepository.save(session);

        // 1. Mentee submits session feedback for completed session
        MentorshipFeedback feedback = mentorshipFeedbackRepository.save(new MentorshipFeedback(
                session, employeeA, 5, "Incredible mentor! Elena gave phenomenal architectural advice."
        ));

        // 2. Mentor A retrieves Feedback Overview
        MentorFeedbackOverviewDto fbOverview = mentorDashboardService.getFeedbackOverview(mentorA.getEmail());
        assertNotNull(fbOverview);
        assertEquals(5.0, fbOverview.averageRating());
        assertEquals(1, fbOverview.totalReviewsReceived());
        assertEquals(1, fbOverview.receivedFeedback().size());
        assertEquals("Incredible mentor! Elena gave phenomenal architectural advice.", fbOverview.receivedFeedback().get(0).comments());

        // 3. Mentor B retrieves Feedback Overview -> receives clean empty lists without exceptions
        MentorFeedbackOverviewDto fbOverviewB = mentorDashboardService.getFeedbackOverview(mentorB.getEmail());
        assertNotNull(fbOverviewB);
        assertEquals(0.0, fbOverviewB.averageRating());
        assertEquals(0, fbOverviewB.totalReviewsReceived());
        assertTrue(fbOverviewB.receivedFeedback().isEmpty());
        assertTrue(fbOverviewB.submittedEvaluations().isEmpty());

        // 4. Mentor A authors a Mentee Evaluation
        CreateMentorEvaluationRequest evalReq = new CreateMentorEvaluationRequest(
                employeeA.getId(),
                session.getId(),
                5,
                "Strong grasp of containerization and clear technical articulation",
                "Could deepen knowledge of eBPF networking",
                "Excellent progress across all development milestones."
        );
        MentorEvaluationDto evaluation = mentorDashboardService.createEvaluation(mentorA.getEmail(), evalReq);
        assertNotNull(evaluation);
        assertEquals(5, evaluation.rating());
        assertEquals("Strong grasp of containerization and clear technical articulation", evaluation.strengths());

        // Verify notification dispatched to employee
        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employeeA.getId());
        assertTrue(notifs.stream().anyMatch(n -> n.getTitle().contains("Mentor Evaluation")));

        // 5. Mentor B attempts to evaluate an employee they have never mentored -> throws 403 FORBIDDEN
        CreateMentorEvaluationRequest unauthorizedEvalReq = new CreateMentorEvaluationRequest(
                employeeA.getId(),
                null,
                4,
                "Good",
                "None",
                "Unauthorized evaluation attempt"
        );
        ApiException exCrossEval = assertThrows(ApiException.class, () ->
                mentorDashboardService.createEvaluation(mentorB.getEmail(), unauthorizedEvalReq)
        );
        assertEquals(HttpStatus.FORBIDDEN, exCrossEval.getStatus());

        // 6. Mentor Notifications scoping
        notificationRepository.save(new Notification(
                mentorA, "MENTOR_UPDATE", "Test Mentor Notification", "Notification content", "/dashboard/mentor"
        ));
        List<NotificationDto> mentorNotifs = mentorDashboardService.getNotifications(mentorA.getEmail(), false);
        assertFalse(mentorNotifs.isEmpty());
        assertTrue(mentorNotifs.stream().anyMatch(n -> n.title().equals("Test Mentor Notification")));

        // Mark notification as read
        Long notifId = mentorNotifs.get(0).id();
        assertDoesNotThrow(() -> mentorDashboardService.markNotificationAsRead(mentorA.getEmail(), notifId));
    }
}
