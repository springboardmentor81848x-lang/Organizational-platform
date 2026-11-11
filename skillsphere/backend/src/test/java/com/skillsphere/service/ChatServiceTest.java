package com.skillsphere.service;

import com.skillsphere.dto.ConversationDto;
import com.skillsphere.dto.MessageDto;
import com.skillsphere.dto.SendMessageRequest;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationParticipantRepository participantRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TeamMembershipRepository teamMembershipRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private MentorAssignmentRepository mentorAssignmentRepository;
    @Mock
    private MentorshipRequestRepository mentorshipRequestRepository;
    @Mock
    private MessageReportRepository messageReportRepository;
    @Mock
    private EmployeeProfileRepository employeeProfileRepository;

    @InjectMocks
    private ChatService chatService;

    private User caller;
    private User intruder;
    private Conversation conversation;
    private ConversationParticipant participant;

    @BeforeEach
    void setUp() {
        caller = new User();
        setPrivateId(caller, 1L);
        caller.setFullName("Alice");
        caller.setEmail("alice@example.com");
        caller.setRole(Role.EMPLOYEE);

        intruder = new User();
        setPrivateId(intruder, 2L);
        intruder.setFullName("Mallory");
        intruder.setEmail("mallory@example.com");
        intruder.setRole(Role.ADMIN);

        conversation = new Conversation("DIRECT", null);
        setPrivateId(conversation, 10L);
        participant = new ConversationParticipant(conversation, caller);
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
    @DisplayName("Non-participant cannot read messages in conversation (throws 403 Forbidden)")
    void testGetMessages_NonParticipant_ThrowsForbidden() {
        when(userRepository.findByEmail("mallory@example.com")).thenReturn(Optional.of(intruder));
        when(participantRepository.findByConversationIdAndUserId(10L, intruder.getId())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () ->
                chatService.getMessages("mallory@example.com", 10L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
        verify(messageRepository, never()).findByConversationIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("Non-participant cannot send message to conversation (throws 403 Forbidden)")
    void testSendMessage_NonParticipant_ThrowsForbidden() {
        when(userRepository.findByEmail("mallory@example.com")).thenReturn(Optional.of(intruder));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(participantRepository.existsByConversationIdAndUserId(10L, intruder.getId())).thenReturn(false);

        SendMessageRequest req = new SendMessageRequest("Attempted injection", null, null);

        ApiException ex = assertThrows(ApiException.class, () ->
                chatService.sendMessage("mallory@example.com", 10L, req));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Participant can send message successfully and participants are notified")
    void testSendMessage_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conversation));
        when(participantRepository.existsByConversationIdAndUserId(10L, caller.getId())).thenReturn(true);

        User bob = new User();
        setPrivateId(bob, 3L);
        bob.setFullName("Bob");
        bob.setEmail("bob@example.com");
        bob.setRole(Role.EMPLOYEE);
        ConversationParticipant bobParticipant = new ConversationParticipant(conversation, bob);

        when(participantRepository.findByConversationId(10L)).thenReturn(List.of(participant, bobParticipant));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            setPrivateId(m, 101L);
            return m;
        });

        SendMessageRequest req = new SendMessageRequest("Hello Bob!", null, null);
        MessageDto result = chatService.sendMessage("alice@example.com", 10L, req);

        assertNotNull(result);
        assertEquals(101L, result.id());
        assertEquals("Hello Bob!", result.content());
        assertEquals(caller.getId(), result.senderId());
        verify(notificationService, times(1)).createNotification(
                eq(bob),
                eq("NEW_MESSAGE"),
                contains("Alice"),
                anyString(),
                eq("/dashboard")
        );
    }

    @Test
    @DisplayName("Participant can read messages in conversation and lastReadAt is updated")
    void testGetMessages_Participant_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(participantRepository.findByConversationIdAndUserId(10L, caller.getId())).thenReturn(Optional.of(participant));
        Message msg = new Message(conversation, caller, "Hello Alice!", null, null);
        setPrivateId(msg, 50L);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(msg));

        List<MessageDto> messages = chatService.getMessages("alice@example.com", 10L);

        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals("Hello Alice!", messages.get(0).content());
        verify(participantRepository, times(1)).save(participant);
    }

    @Test
    @DisplayName("Unrelated users cannot initiate direct conversation (throws 403 Forbidden)")
    void testCreateDirectConversation_UnrelatedUsers_ThrowsForbidden() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));

        User targetUser = new User();
        setPrivateId(targetUser, 5L);
        targetUser.setFullName("Random User");
        targetUser.setEmail("random@example.com");
        targetUser.setRole(Role.EMPLOYEE);
        when(userRepository.findById(5L)).thenReturn(Optional.of(targetUser));

        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(caller.getId())).thenReturn(List.of());
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(1L, 5L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(5L, 1L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(1L, 5L, "ACCEPTED")).thenReturn(false);
        when(mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(5L, 1L, "ACCEPTED")).thenReturn(false);
        when(teamMembershipRepository.findByUserId(1L)).thenReturn(List.of());
        when(teamMembershipRepository.findByUserId(5L)).thenReturn(List.of());
        when(teamRepository.findByManagerId(1L)).thenReturn(List.of());
        when(teamRepository.findByManagerId(5L)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () ->
                chatService.getOrCreateDirectConversation("alice@example.com", 5L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Admin cannot automatically initiate an unrelated private conversation (throws 403 Forbidden)")
    void testCreateDirectConversation_AdminUnrelatedUser_ThrowsForbidden() {
        User adminUser = new User();
        setPrivateId(adminUser, 99L);
        adminUser.setFullName("Admin Root");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        User targetEmployee = new User();
        setPrivateId(targetEmployee, 20L);
        targetEmployee.setFullName("Employee Dave");
        targetEmployee.setEmail("dave@example.com");
        targetEmployee.setRole(Role.EMPLOYEE);
        when(userRepository.findById(20L)).thenReturn(Optional.of(targetEmployee));

        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(99L)).thenReturn(List.of());
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(99L, 20L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(20L, 99L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(99L, 20L, "ACCEPTED")).thenReturn(false);
        when(mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(20L, 99L, "ACCEPTED")).thenReturn(false);
        when(teamMembershipRepository.findByUserId(99L)).thenReturn(List.of());
        when(teamMembershipRepository.findByUserId(20L)).thenReturn(List.of());
        when(teamRepository.findByManagerId(99L)).thenReturn(List.of());
        when(teamRepository.findByManagerId(20L)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () ->
                chatService.getOrCreateDirectConversation("admin@example.com", 20L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("HR Specialist cannot automatically initiate an unrelated private conversation (throws 403 Forbidden)")
    void testCreateDirectConversation_HrSpecialistUnrelatedUser_ThrowsForbidden() {
        User hrUser = new User();
        setPrivateId(hrUser, 88L);
        hrUser.setFullName("HR Specialist Helen");
        hrUser.setEmail("hr@example.com");
        hrUser.setRole(Role.HR_SPECIALIST);
        when(userRepository.findByEmail("hr@example.com")).thenReturn(Optional.of(hrUser));

        User targetEmployee = new User();
        setPrivateId(targetEmployee, 30L);
        targetEmployee.setFullName("Employee Eve");
        targetEmployee.setEmail("eve@example.com");
        targetEmployee.setRole(Role.EMPLOYEE);
        when(userRepository.findById(30L)).thenReturn(Optional.of(targetEmployee));

        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(88L)).thenReturn(List.of());
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(88L, 30L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(30L, 88L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(88L, 30L, "ACCEPTED")).thenReturn(false);
        when(mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(30L, 88L, "ACCEPTED")).thenReturn(false);
        when(teamMembershipRepository.findByUserId(88L)).thenReturn(List.of());
        when(teamMembershipRepository.findByUserId(30L)).thenReturn(List.of());
        when(teamRepository.findByManagerId(88L)).thenReturn(List.of());
        when(teamRepository.findByManagerId(30L)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () ->
                chatService.getOrCreateDirectConversation("hr@example.com", 30L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Authorized team members can initiate direct conversation successfully")
    void testCreateDirectConversation_AuthorizedTeamMembers_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));

        User teammate = new User();
        setPrivateId(teammate, 4L);
        teammate.setFullName("Teammate Tom");
        teammate.setEmail("tom@example.com");
        teammate.setRole(Role.EMPLOYEE);
        when(userRepository.findById(4L)).thenReturn(Optional.of(teammate));

        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(caller.getId())).thenReturn(List.of());

        // Mentorship not present
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(1L, 4L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(4L, 1L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(1L, 4L, "ACCEPTED")).thenReturn(false);
        when(mentorshipRequestRepository.existsByMentorIdAndEmployeeIdAndStatus(4L, 1L, "ACCEPTED")).thenReturn(false);

        // Same team (Team ID 100)
        Team team = new Team();
        setPrivateId(team, 100L);
        team.setName("Engineering");

        TeamMembership tm1 = new TeamMembership(team, caller, "MEMBER");
        TeamMembership tm2 = new TeamMembership(team, teammate, "MEMBER");
        when(teamMembershipRepository.findByUserId(1L)).thenReturn(List.of(tm1));
        when(teamMembershipRepository.findByUserId(4L)).thenReturn(List.of(tm2));

        Conversation newConv = new Conversation("DIRECT", null);
        setPrivateId(newConv, 77L);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(newConv);

        ConversationParticipant cp1 = new ConversationParticipant(newConv, caller);
        ConversationParticipant cp2 = new ConversationParticipant(newConv, teammate);
        when(participantRepository.findByConversationId(77L)).thenReturn(List.of(cp1, cp2));
        when(messageRepository.findRecentByConversationId(eq(77L), any(PageRequest.class))).thenReturn(List.of());

        ConversationDto dto = chatService.getOrCreateDirectConversation("alice@example.com", 4L);

        assertNotNull(dto);
        assertEquals(77L, dto.id());
        assertEquals("DIRECT", dto.type());
        verify(conversationRepository).save(any(Conversation.class));
        verify(participantRepository, times(2)).save(any(ConversationParticipant.class));
    }

    @Test
    @DisplayName("Authorized mentor pair can initiate direct conversation successfully")
    void testCreateDirectConversation_AuthorizedMentorPair_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));

        User mentor = new User();
        setPrivateId(mentor, 6L);
        mentor.setFullName("Mentor Mark");
        mentor.setEmail("mark@example.com");
        mentor.setRole(Role.MENTOR);
        when(userRepository.findById(6L)).thenReturn(Optional.of(mentor));

        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(caller.getId())).thenReturn(List.of());
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(1L, 6L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(mentorAssignmentRepository.existsByMentorIdAndEmployeeIdAndStatus(6L, 1L, AssignmentStatus.ACTIVE)).thenReturn(true);

        Conversation newConv = new Conversation("DIRECT", null);
        setPrivateId(newConv, 88L);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(newConv);

        ConversationParticipant cp1 = new ConversationParticipant(newConv, caller);
        ConversationParticipant cp2 = new ConversationParticipant(newConv, mentor);
        when(participantRepository.findByConversationId(88L)).thenReturn(List.of(cp1, cp2));
        when(messageRepository.findRecentByConversationId(eq(88L), any(PageRequest.class))).thenReturn(List.of());

        ConversationDto dto = chatService.getOrCreateDirectConversation("alice@example.com", 6L);

        assertNotNull(dto);
        assertEquals(88L, dto.id());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Existing conversation participants can continue direct conversation without re-checking relationship")
    void testExistingConversationParticipants_CanContinueDirectConversation() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));

        User bob = new User();
        setPrivateId(bob, 3L);
        bob.setFullName("Bob");
        bob.setEmail("bob@example.com");
        bob.setRole(Role.EMPLOYEE);
        when(userRepository.findById(3L)).thenReturn(Optional.of(bob));

        Conversation existingConv = new Conversation("DIRECT", null);
        setPrivateId(existingConv, 10L);

        ConversationParticipant cp1 = new ConversationParticipant(existingConv, caller);
        ConversationParticipant cp2 = new ConversationParticipant(existingConv, bob);

        when(conversationRepository.findByUserIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(existingConv));
        when(participantRepository.findByConversationId(10L)).thenReturn(List.of(cp1, cp2));
        when(messageRepository.findRecentByConversationId(eq(10L), any(PageRequest.class))).thenReturn(List.of());

        ConversationDto dto = chatService.getOrCreateDirectConversation("alice@example.com", 3L);

        assertNotNull(dto);
        assertEquals(10L, dto.id());
        // Verify relationship repositories are never called since conversation already exists
        verifyNoInteractions(mentorAssignmentRepository);
        verifyNoInteractions(teamMembershipRepository);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Cannot initiate direct conversation with self (throws 400 Bad Request)")
    void testCreateDirectConversation_WithSelf_ThrowsBadRequest() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(userRepository.findById(1L)).thenReturn(Optional.of(caller));

        ApiException ex = assertThrows(ApiException.class, () ->
                chatService.getOrCreateDirectConversation("alice@example.com", 1L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("yourself"));
    }

    @Test
    @DisplayName("Manager can create a team conversation with all members added")
    void testCreateTeamConversation_ManagerSuccess() {
        User manager = new User();
        setPrivateId(manager, 100L);
        manager.setFullName("Manager Mark");
        manager.setEmail("mark@example.com");
        manager.setRole(Role.MANAGER);

        Department dept = new Department();
        dept.setName("Engineering");
        Team team = new Team(dept, "Engineering Alpha", manager);
        setPrivateId(team, 50L);

        when(userRepository.findByEmail("mark@example.com")).thenReturn(Optional.of(manager));
        when(teamRepository.findById(50L)).thenReturn(Optional.of(team));

        User member = new User();
        setPrivateId(member, 101L);
        member.setFullName("Dev Dan");
        member.setEmail("dan@example.com");
        member.setRole(Role.EMPLOYEE);
        TeamMembership tm = new TeamMembership(team, member, "DEVELOPER");

        when(teamMembershipRepository.findByTeamId(50L)).thenReturn(List.of(tm));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            setPrivateId(c, 200L);
            return c;
        });

        ConversationDto dto = chatService.createTeamConversation("mark@example.com",
                new com.skillsphere.dto.ChatDtos.CreateTeamConversationRequest(50L, "Sprint Discussion"));

        assertNotNull(dto);
        assertEquals(200L, dto.id());
        assertEquals("TEAM", dto.type());
        assertEquals("Sprint Discussion", dto.title());
        verify(participantRepository, times(2)).save(any(ConversationParticipant.class));
    }

    @Test
    @DisplayName("Non-manager employee cannot create team conversation (throws 403 Forbidden)")
    void testCreateTeamConversation_NonManagerForbidden() {
        User employee = new User();
        setPrivateId(employee, 101L);
        employee.setFullName("Dev Dan");
        employee.setEmail("dan@example.com");
        employee.setRole(Role.EMPLOYEE);

        User manager = new User();
        setPrivateId(manager, 100L);

        Department dept = new Department();
        dept.setName("Engineering");
        Team team = new Team(dept, "Engineering Alpha", manager);
        setPrivateId(team, 50L);

        when(userRepository.findByEmail("dan@example.com")).thenReturn(Optional.of(employee));
        when(teamRepository.findById(50L)).thenReturn(Optional.of(team));

        ApiException ex = assertThrows(ApiException.class, () ->
                chatService.createTeamConversation("dan@example.com",
                        new com.skillsphere.dto.ChatDtos.CreateTeamConversationRequest(50L, "Sprint")));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Only the assigned manager"));
    }

    @Test
    @DisplayName("Participant can toggle mute on a conversation")
    void testToggleMute_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(participantRepository.findByConversationIdAndUserId(10L, 1L)).thenReturn(Optional.of(participant));

        assertFalse(participant.isMuted());
        boolean muted = chatService.toggleMute("alice@example.com", 10L);

        assertTrue(muted);
        assertTrue(participant.isMuted());
        verify(participantRepository).save(participant);
    }

    @Test
    @DisplayName("Participant can toggle archive on a conversation")
    void testToggleArchive_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(participantRepository.findByConversationIdAndUserId(10L, 1L)).thenReturn(Optional.of(participant));

        assertFalse(participant.isArchived());
        boolean archived = chatService.toggleArchive("alice@example.com", 10L);

        assertTrue(archived);
        assertTrue(participant.isArchived());
        verify(participantRepository).save(participant);
    }

    @Test
    @DisplayName("Participant can report an inappropriate message")
    void testReportMessage_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));

        Message msg = new Message(conversation, intruder, "Harassing content", null, null);
        setPrivateId(msg, 77L);

        when(messageRepository.findById(77L)).thenReturn(Optional.of(msg));
        when(participantRepository.existsByConversationIdAndUserId(10L, 1L)).thenReturn(true);
        when(messageReportRepository.existsByMessageIdAndReporterId(77L, 1L)).thenReturn(false);

        when(messageReportRepository.save(any(MessageReport.class))).thenAnswer(inv -> {
            MessageReport r = inv.getArgument(0);
            setPrivateId(r, 999L);
            return r;
        });

        com.skillsphere.dto.ChatDtos.ReportMessageRequest req =
                new com.skillsphere.dto.ChatDtos.ReportMessageRequest("Inappropriate language", "Offensive remarks");

        com.skillsphere.dto.ChatDtos.MessageReportResponse resp = chatService.reportMessage("alice@example.com", 77L, req);

        assertNotNull(resp);
        assertEquals(999L, resp.id());
        assertEquals(77L, resp.messageId());
        assertEquals("Inappropriate language", resp.reason());
        assertEquals("PENDING", resp.status());
    }

    @Test
    @DisplayName("Non-participant cannot report message in unrelated conversation (throws 403 Forbidden)")
    void testReportMessage_NonParticipantForbidden() {
        when(userRepository.findByEmail("mallory@example.com")).thenReturn(Optional.of(intruder));

        Message msg = new Message(conversation, caller, "Secret discussion", null, null);
        setPrivateId(msg, 88L);

        when(messageRepository.findById(88L)).thenReturn(Optional.of(msg));
        when(participantRepository.existsByConversationIdAndUserId(10L, 2L)).thenReturn(false);

        com.skillsphere.dto.ChatDtos.ReportMessageRequest req =
                new com.skillsphere.dto.ChatDtos.ReportMessageRequest("Suspicious", "I am snooping");

        ApiException ex = assertThrows(ApiException.class, () ->
                chatService.reportMessage("mallory@example.com", 88L, req));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("participate in"));
    }
}
