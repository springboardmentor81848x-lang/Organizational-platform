package com.skillsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.config.SecurityConfig;
import com.skillsphere.dto.ConversationDto;
import com.skillsphere.dto.CreateDirectConversationRequest;
import com.skillsphere.dto.MessageDto;
import com.skillsphere.dto.SendMessageRequest;
import com.skillsphere.dto.ChatDtos.CreateTeamConversationRequest;
import com.skillsphere.dto.ChatDtos.ReportMessageRequest;
import com.skillsphere.repository.UserRepository;
import com.skillsphere.security.JsonErrorWriter;
import com.skillsphere.service.ChatAttachmentService;
import com.skillsphere.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, JsonErrorWriter.class})
@TestPropertySource(properties = {
        "app.cors.allowed-origins=http://localhost:5173"
})
class ChatControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @MockBean
    private ChatAttachmentService attachmentService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/chat/conversations without authentication returns 401")
    void testGetConversations_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/chat/unread-count without authentication returns 401")
    void testGetUnreadCount_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/chat/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/chat/contacts/search without authentication returns 401")
    void testSearchContacts_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/chat/contacts/search"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/chat/conversations with authenticated employee returns 200 OK")
    void testGetConversations_AuthenticatedEmployee_Returns200() throws Exception {
        when(chatService.getUserConversations("alice@example.com")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"EMPLOYEE"})
    @DisplayName("GET /api/chat/unread-count returns unread count JSON")
    void testGetUnreadCount_Success() throws Exception {
        when(chatService.getGlobalUnreadCount("alice@example.com")).thenReturn(3L);

        mockMvc.perform(get("/api/chat/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"EMPLOYEE"})
    @DisplayName("POST /api/chat/conversations/direct without CSRF returns 403 Forbidden")
    void testCreateDirectConversation_WithoutCsrf_Returns403() throws Exception {
        CreateDirectConversationRequest req = new CreateDirectConversationRequest(2L);

        mockMvc.perform(post("/api/chat/conversations/direct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"EMPLOYEE"})
    @DisplayName("POST /api/chat/conversations/direct with CSRF returns 200 OK")
    void testCreateDirectConversation_WithCsrf_Returns200() throws Exception {
        CreateDirectConversationRequest req = new CreateDirectConversationRequest(2L);
        ConversationDto conv = new ConversationDto(10L, "DIRECT", "Bob", Instant.now(), List.of(), null, 0);

        when(chatService.getOrCreateDirectConversation(eq("alice@example.com"), eq(2L))).thenReturn(conv);

        mockMvc.perform(post("/api/chat/conversations/direct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.type").value("DIRECT"));
    }

    @Test
    @WithMockUser(username = "manager@example.com", roles = {"MANAGER"})
    @DisplayName("POST /api/chat/conversations/team with CSRF returns 201 Created")
    void testCreateTeamConversation_WithCsrf_Returns201() throws Exception {
        CreateTeamConversationRequest req = new CreateTeamConversationRequest(5L, "Backend Team");
        ConversationDto conv = new ConversationDto(20L, "TEAM", "Backend Team", Instant.now(), List.of(), null, 0);

        when(chatService.createTeamConversation(eq("manager@example.com"), any())).thenReturn(conv);

        mockMvc.perform(post("/api/chat/conversations/team")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.type").value("TEAM"));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"EMPLOYEE"})
    @DisplayName("POST /api/chat/conversations/{id}/messages with valid payload returns 201 Created")
    void testSendMessage_WithCsrf_Returns201() throws Exception {
        SendMessageRequest req = new SendMessageRequest("Hello team!", null, null);
        MessageDto msg = new MessageDto(50L, 10L, 1L, "Alice", "Hello team!", null, null, Instant.now(), false);

        when(chatService.sendMessage(eq("alice@example.com"), eq(10L), any())).thenReturn(msg);

        mockMvc.perform(post("/api/chat/conversations/10/messages")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.content").value("Hello team!"));
    }

    @Test
    @WithMockUser(username = "alice@example.com", roles = {"EMPLOYEE"})
    @DisplayName("PATCH /api/chat/conversations/{id}/mute toggles mute state")
    void testToggleMute_WithCsrf_Returns200() throws Exception {
        when(chatService.toggleMute("alice@example.com", 10L)).thenReturn(true);

        mockMvc.perform(patch("/api/chat/conversations/10/mute")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMuted").value(true));
    }
}
