package com.skillsphere.service;

import com.skillsphere.dto.ChatDtos.AttachmentUploadResponse;
import com.skillsphere.dto.ChatDtos.CreateTeamConversationRequest;
import com.skillsphere.dto.ConversationDto;
import com.skillsphere.dto.MessageDto;
import com.skillsphere.dto.SendMessageRequest;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "app.chat.upload-dir=target/test-chat-uploads"
})
class ChatWorkflowIntegrationTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatAttachmentService attachmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMembershipRepository teamMembershipRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private MessageRepository messageRepository;

    private User createTestUser(String email, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setPasswordHash("$2a$12$DUMMYHASHFORTESTINGPURPOSESONLY");
        u.setRole(role);
        u.setAccountStatus(AccountStatus.ACTIVE);
        return userRepository.save(u);
    }

    private Department createTestDepartment(String name, String code) {
        Department d = new Department();
        d.setName(name);
        d.setCode(code);
        d.setDescription("Test department");
        return departmentRepository.save(d);
    }

    private void linkProfile(User u, Department d) {
        EmployeeProfile p = new EmployeeProfile();
        p.setUser(u);
        p.setJobRole("Software Developer");
        p.setDepartment(d.getName());
        employeeProfileRepository.save(p);
    }

    @Test
    @DisplayName("Check 6.1: Direct 1-to-1 Chat Workflow backed by Real PostgreSQL Entities")
    void testDirectChatWorkflow_RealDb() {
        Department dept = createTestDepartment("Software Eng", "SWE");
        User alice = createTestUser("alice_test@skillsphere.com", "Alice Test", Role.EMPLOYEE);
        User bob = createTestUser("bob_test@skillsphere.com", "Bob Test", Role.EMPLOYEE);
        linkProfile(alice, dept);
        linkProfile(bob, dept);

        // 1. Create direct conversation
        ConversationDto conv = chatService.getOrCreateDirectConversation(alice.getEmail(), bob.getId());
        assertNotNull(conv);
        assertEquals("DIRECT", conv.type());
        assertTrue(conversationRepository.existsById(conv.id()));

        // 2. Alice sends a message
        SendMessageRequest req = new SendMessageRequest("Hello Bob, reviewing the architecture.", null, null);
        MessageDto sent = chatService.sendMessage(alice.getEmail(), conv.id(), req);
        assertNotNull(sent);
        assertEquals("Hello Bob, reviewing the architecture.", sent.content());
        assertFalse(sent.isRead(), "Message should initially be unread by recipient");

        // 3. Bob inspects conversations
        List<ConversationDto> bobConvs = chatService.getUserConversations(bob.getEmail());
        assertEquals(1, bobConvs.size());
        assertEquals(1, bobConvs.get(0).unreadCount(), "Bob should have 1 unread message");

        // 4. Bob reads message history
        List<MessageDto> bobMessages = chatService.getMessages(bob.getEmail(), conv.id());
        assertEquals(1, bobMessages.size());

        // 5. Verify read state updated
        ConversationParticipant bobParticipant = participantRepository
                .findByConversationIdAndUserId(conv.id(), bob.getId()).orElseThrow();
        assertNotNull(bobParticipant.getLastReadAt());

        // 6. Alice retrieves messages: read indicator should now be true
        List<MessageDto> aliceMessages = chatService.getMessages(alice.getEmail(), conv.id());
        assertTrue(aliceMessages.get(0).isRead(), "Message should now be marked as read by Bob");
    }

    @Test
    @DisplayName("Check 3 & 6.2: Group Read Receipts - Never displays read confirmation when only one participant has read")
    void testGroupChatWorkflow_RealDb() {
        Department dept = createTestDepartment("Core Platform", "CORE");
        User manager = createTestUser("manager_mark@skillsphere.com", "Manager Mark", Role.MANAGER);
        User devDan = createTestUser("dan_dev@skillsphere.com", "Dan Dev", Role.EMPLOYEE);
        User devDiana = createTestUser("diana_dev@skillsphere.com", "Diana Dev", Role.EMPLOYEE);
        linkProfile(manager, dept);
        linkProfile(devDan, dept);
        linkProfile(devDiana, dept);

        Team team = new Team(dept, "Platform Infra", manager);
        teamRepository.save(team);
        teamMembershipRepository.save(new TeamMembership(team, devDan, "BACKEND"));
        teamMembershipRepository.save(new TeamMembership(team, devDiana, "FRONTEND"));

        // Manager creates team channel
        ConversationDto teamConv = chatService.createTeamConversation(
                manager.getEmail(),
                new CreateTeamConversationRequest(team.getId(), "Infra Sync")
        );
        assertEquals("TEAM", teamConv.type());
        assertEquals(3, participantRepository.findByConversationId(teamConv.id()).size());

        // Manager sends group message
        MessageDto groupMsg = chatService.sendMessage(
                manager.getEmail(),
                teamConv.id(),
                new SendMessageRequest("Deployment planned for 8 PM.", null, null)
        );
        assertFalse(groupMsg.isRead());

        // Only Dan reads the message
        chatService.getMessages(devDan.getEmail(), teamConv.id());

        // Manager checks message status: Dan read it, but Diana has NOT yet read it
        List<MessageDto> mgrMessages = chatService.getMessages(manager.getEmail(), teamConv.id());
        assertFalse(mgrMessages.get(0).isRead(),
                "GROUP READ RULE: Must NOT confirm read when only one of multiple recipients has read it");

        // Diana now also reads the message
        chatService.getMessages(devDiana.getEmail(), teamConv.id());

        // Manager checks message status again: ALL recipients have read it
        List<MessageDto> mgrMessagesAllRead = chatService.getMessages(manager.getEmail(), teamConv.id());
        assertTrue(mgrMessagesAllRead.get(0).isRead(),
                "Group message should confirm read only when all recipients have read it");
    }

    @Test
    @DisplayName("Check 2 & 6.3: Administrator Privacy & IDOR Boundary - Admin and unauthorized users cannot read private chats")
    void testCrossUserAccessAndIdor_RealDb() {
        Department dept = createTestDepartment("Security Operations", "SEC");
        User alice = createTestUser("alice_sec@skillsphere.com", "Alice Sec", Role.EMPLOYEE);
        User bob = createTestUser("bob_sec@skillsphere.com", "Bob Sec", Role.EMPLOYEE);
        User mallory = createTestUser("mallory_intruder@skillsphere.com", "Mallory Intruder", Role.EMPLOYEE);
        User adminArthur = createTestUser("admin_arthur@skillsphere.com", "Arthur Admin", Role.ADMIN);
        linkProfile(alice, dept);
        linkProfile(bob, dept);

        // Alice and Bob have a direct chat
        ConversationDto conv = chatService.getOrCreateDirectConversation(alice.getEmail(), bob.getId());
        chatService.sendMessage(alice.getEmail(), conv.id(), new SendMessageRequest("Confidential vulnerability data.", null, null));

        // 1. Employee Mallory attempts IDOR access to read conversation
        ApiException exMallory = assertThrows(ApiException.class, () ->
                chatService.getMessages(mallory.getEmail(), conv.id()));
        assertEquals(HttpStatus.FORBIDDEN, exMallory.getStatus(), "Mallory should be forbidden from reading Alice's chat");

        // 2. Administrator Arthur attempts IDOR access to read conversation
        ApiException exAdmin = assertThrows(ApiException.class, () ->
                chatService.getMessages(adminArthur.getEmail(), conv.id()));
        assertEquals(HttpStatus.FORBIDDEN, exAdmin.getStatus(), "ADMIN PRIVACY GUARANTEE: Admin cannot read private conversations merely due to role");

        // 3. Mallory attempts to send a message to Alice and Bob's chat
        ApiException exMallorySend = assertThrows(ApiException.class, () ->
                chatService.sendMessage(mallory.getEmail(), conv.id(), new SendMessageRequest("Injected msg", null, null)));
        assertEquals(HttpStatus.FORBIDDEN, exMallorySend.getStatus());

        // 4. Admin cannot create team channels for teams they do not manage
        Team foreignTeam = new Team(dept, "Foreign Team", bob);
        teamRepository.save(foreignTeam);
        ApiException exAdminTeam = assertThrows(ApiException.class, () ->
                chatService.createTeamConversation(adminArthur.getEmail(), new CreateTeamConversationRequest(foreignTeam.getId(), "Admin Intercept")));
        assertEquals(HttpStatus.FORBIDDEN, exAdminTeam.getStatus(), "Admin cannot create team channels or insert themselves into teams");
    }

    @Test
    @DisplayName("Check 4 & 6.4: Attachment Upload, Download & IDOR Access Control")
    void testAttachmentSecurity_RealDb() {
        Department dept = createTestDepartment("Analytics Dept", "ANA");
        User alice = createTestUser("alice_att@skillsphere.com", "Alice Att", Role.EMPLOYEE);
        User bob = createTestUser("bob_att@skillsphere.com", "Bob Att", Role.EMPLOYEE);
        User mallory = createTestUser("mallory_att@skillsphere.com", "Mallory Att", Role.EMPLOYEE);
        linkProfile(alice, dept);
        linkProfile(bob, dept);

        ConversationDto conv = chatService.getOrCreateDirectConversation(alice.getEmail(), bob.getId());

        // Alice uploads allowed PDF attachment
        MockMultipartFile file = new MockMultipartFile("file", "quarterly_kpi.pdf", "application/pdf", "Quarterly Results".getBytes());
        AttachmentUploadResponse uploadResp = attachmentService.uploadAttachment(alice.getEmail(), conv.id(), file);
        assertNotNull(uploadResp);
        assertTrue(uploadResp.fileUrl().startsWith("/api/chat/attachments/"));

        // Alice sends message referencing the attachment
        chatService.sendMessage(alice.getEmail(), conv.id(), new SendMessageRequest("Attached KPI report", uploadResp.fileUrl(), uploadResp.originalFileName()));

        String storedFileName = uploadResp.fileUrl().replace("/api/chat/attachments/", "").replace("/download", "");

        // Bob (authorized participant) downloads attachment
        Resource bobResource = attachmentService.downloadAttachment(bob.getEmail(), storedFileName);
        assertNotNull(bobResource);
        assertTrue(bobResource.exists());

        // Mallory (unauthorized third party) attempts to download attachment
        ApiException exMalloryDownload = assertThrows(ApiException.class, () ->
                attachmentService.downloadAttachment(mallory.getEmail(), storedFileName));
        assertEquals(HttpStatus.FORBIDDEN, exMalloryDownload.getStatus(), "Unauthorized user cannot download attachment");
    }

    @Test
    @DisplayName("Check 6.5: Conversation Muting and Archiving Persistence in PostgreSQL")
    void testMuteAndArchive_RealDb() {
        Department dept = createTestDepartment("Marketing Dept", "MKT");
        User alice = createTestUser("alice_mkt@skillsphere.com", "Alice Mkt", Role.EMPLOYEE);
        User bob = createTestUser("bob_mkt@skillsphere.com", "Bob Mkt", Role.EMPLOYEE);
        linkProfile(alice, dept);
        linkProfile(bob, dept);

        ConversationDto conv = chatService.getOrCreateDirectConversation(alice.getEmail(), bob.getId());

        // Toggle mute
        boolean muted = chatService.toggleMute(alice.getEmail(), conv.id());
        assertTrue(muted);
        ConversationParticipant pMuted = participantRepository.findByConversationIdAndUserId(conv.id(), alice.getId()).orElseThrow();
        assertTrue(pMuted.isMuted(), "is_muted column must be persisted in PostgreSQL");

        // Toggle archive
        boolean archived = chatService.toggleArchive(alice.getEmail(), conv.id());
        assertTrue(archived);
        ConversationParticipant pArchived = participantRepository.findByConversationIdAndUserId(conv.id(), alice.getId()).orElseThrow();
        assertTrue(pArchived.isArchived(), "is_archived column must be persisted in PostgreSQL");
    }
}
