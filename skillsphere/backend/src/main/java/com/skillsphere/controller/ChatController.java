package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.getUserConversations(userDetails.getUsername()));
    }

    @PostMapping("/conversations/direct")
    public ResponseEntity<ConversationDto> getOrCreateDirectConversation(@AuthenticationPrincipal UserDetails userDetails,
                                                                         @Valid @RequestBody CreateDirectConversationRequest req) {
        ConversationDto conv = chatService.getOrCreateDirectConversation(userDetails.getUsername(), req.targetUserId());
        return ResponseEntity.ok(conv);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@AuthenticationPrincipal UserDetails userDetails,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(chatService.getMessages(userDetails.getUsername(), id));
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageDto> sendMessage(@AuthenticationPrincipal UserDetails userDetails,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody SendMessageRequest req) {
        MessageDto msg = chatService.sendMessage(userDetails.getUsername(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    @PostMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal UserDetails userDetails,
                                           @PathVariable Long id) {
        chatService.markAsRead(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
