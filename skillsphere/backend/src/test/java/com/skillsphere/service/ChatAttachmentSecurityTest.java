package com.skillsphere.service;

import com.skillsphere.dto.ChatDtos.AttachmentUploadResponse;
import com.skillsphere.entity.Conversation;
import com.skillsphere.entity.ConversationParticipant;
import com.skillsphere.entity.Message;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.ConversationParticipantRepository;
import com.skillsphere.repository.MessageRepository;
import com.skillsphere.repository.SystemConfigurationRepository;
import com.skillsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAttachmentSecurityTest {

    @Mock
    private ConversationParticipantRepository participantRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SystemConfigurationRepository configRepository;

    @TempDir
    Path tempUploadDir;

    private ChatAttachmentService attachmentService;

    private User caller;
    private User intruder;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        attachmentService = new ChatAttachmentService(
                tempUploadDir.toString(),
                participantRepository,
                messageRepository,
                userRepository,
                configRepository
        );

        caller = new User();
        setPrivateId(caller, 1L);
        caller.setEmail("alice@example.com");

        intruder = new User();
        setPrivateId(intruder, 2L);
        intruder.setEmail("mallory@example.com");

        conversation = new Conversation("DIRECT", null);
        setPrivateId(conversation, 10L);
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
    @DisplayName("Non-participant cannot upload attachment to conversation (throws 403 Forbidden)")
    void testUpload_NonParticipant_ThrowsForbidden() {
        when(userRepository.findByEmail("mallory@example.com")).thenReturn(Optional.of(intruder));
        when(participantRepository.existsByConversationIdAndUserId(10L, 2L)).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "Hello".getBytes());

        ApiException ex = assertThrows(ApiException.class, () ->
                attachmentService.uploadAttachment("mallory@example.com", 10L, file));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    @DisplayName("Dangerous executable file types are rejected (throws 400 Bad Request)")
    void testUpload_DangerousExtension_ThrowsBadRequest() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(participantRepository.existsByConversationIdAndUserId(10L, 1L)).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", "binary".getBytes());

        ApiException ex = assertThrows(ApiException.class, () ->
                attachmentService.uploadAttachment("alice@example.com", 10L, file));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Executable and script attachments"));
    }

    @Test
    @DisplayName("Oversized attachment exceeding configured max limit is rejected")
    void testUpload_OversizedFile_ThrowsBadRequest() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(participantRepository.existsByConversationIdAndUserId(10L, 1L)).thenReturn(true);

        // 11 MB file exceeds 10MB default
        byte[] largeBytes = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", largeBytes);

        ApiException ex = assertThrows(ApiException.class, () ->
                attachmentService.uploadAttachment("alice@example.com", 10L, file));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("exceeds the platform maximum limit"));
    }

    @Test
    @DisplayName("Authorized participant can upload allowed file type successfully")
    void testUpload_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(participantRepository.existsByConversationIdAndUserId(10L, 1L)).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "notes.pdf", "application/pdf", "Valid PDF content".getBytes());

        AttachmentUploadResponse resp = attachmentService.uploadAttachment("alice@example.com", 10L, file);

        assertNotNull(resp);
        assertEquals("notes.pdf", resp.originalFileName());
        assertTrue(resp.fileUrl().startsWith("/api/chat/attachments/"));
        assertTrue(resp.sizeBytes() > 0);
    }

    @Test
    @DisplayName("Non-participant cannot download attachment (throws 403 Forbidden)")
    void testDownload_NonParticipant_ThrowsForbidden() throws Exception {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(caller));
        when(userRepository.findByEmail("mallory@example.com")).thenReturn(Optional.of(intruder));
        when(participantRepository.existsByConversationIdAndUserId(10L, 1L)).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "confidential.pdf", "application/pdf", "Secret data".getBytes());
        AttachmentUploadResponse uploadResp = attachmentService.uploadAttachment("alice@example.com", 10L, file);

        String storedFileName = uploadResp.fileUrl().replace("/api/chat/attachments/", "").replace("/download", "");

        Message msg = new Message(conversation, caller, "Attached report", uploadResp.fileUrl(), "confidential.pdf");
        when(messageRepository.findAll()).thenReturn(List.of(msg));
        when(participantRepository.existsByConversationIdAndUserId(10L, 2L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () ->
                attachmentService.downloadAttachment("mallory@example.com", storedFileName));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Access denied"));
    }
}
