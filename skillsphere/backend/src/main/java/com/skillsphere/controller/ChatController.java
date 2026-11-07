package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.dto.ChatDtos.*;
import com.skillsphere.service.ChatAttachmentService;
import com.skillsphere.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatAttachmentService attachmentService;

    public ChatController(ChatService chatService, ChatAttachmentService attachmentService) {
        this.chatService = chatService;
        this.attachmentService = attachmentService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.getUserConversations(userDetails.getUsername()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getGlobalUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        long count = chatService.getGlobalUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @GetMapping("/contacts/search")
    public ResponseEntity<List<ChatContactDto>> searchContacts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(chatService.searchPermittedContacts(userDetails.getUsername(), query));
    }

    @PostMapping("/conversations/direct")
    public ResponseEntity<ConversationDto> getOrCreateDirectConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateDirectConversationRequest req) {
        ConversationDto conv = chatService.getOrCreateDirectConversation(userDetails.getUsername(), req.targetUserId());
        return ResponseEntity.ok(conv);
    }

    @PostMapping("/conversations/team")
    public ResponseEntity<ConversationDto> createTeamConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateTeamConversationRequest req) {
        ConversationDto conv = chatService.createTeamConversation(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(conv);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(userDetails.getUsername(), id));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageDto> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest req) {
        MessageDto msg = chatService.sendMessage(userDetails.getUsername(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        chatService.markAsRead(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/conversations/{id}/mute")
    public ResponseEntity<Map<String, Boolean>> toggleMute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        boolean isMuted = chatService.toggleMute(userDetails.getUsername(), id);
        return ResponseEntity.ok(Map.of("isMuted", isMuted));
    }

    @PatchMapping("/conversations/{id}/archive")
    public ResponseEntity<Map<String, Boolean>> toggleArchive(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        boolean isArchived = chatService.toggleArchive(userDetails.getUsername(), id);
        return ResponseEntity.ok(Map.of("isArchived", isArchived));
    }

    @PostMapping("/conversations/{id}/attachments")
    public ResponseEntity<AttachmentUploadResponse> uploadAttachment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        AttachmentUploadResponse resp = attachmentService.uploadAttachment(userDetails.getUsername(), id, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/attachments/{fileName}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String fileName) {
        Resource file = attachmentService.downloadAttachment(userDetails.getUsername(), fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @PostMapping("/messages/{id}/report")
    public ResponseEntity<MessageReportResponse> reportMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ReportMessageRequest req) {
        MessageReportResponse report = chatService.reportMessage(userDetails.getUsername(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }
}
