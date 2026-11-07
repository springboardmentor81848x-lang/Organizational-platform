package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.entity.KnowledgeCategory;
import com.skillsphere.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    // --- Knowledge Hub ---

    @GetMapping("/knowledge/categories")
    public ResponseEntity<List<KnowledgeCategory>> getCategories() {
        return ResponseEntity.ok(knowledgeService.getCategories());
    }

    @GetMapping("/knowledge/articles")
    public ResponseEntity<List<ArticleDto>> getArticles(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(knowledgeService.getArticles(search));
    }

    @GetMapping("/knowledge/articles/{id}")
    public ResponseEntity<ArticleDto> getArticleById(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeService.getArticleById(id));
    }

    @PostMapping("/knowledge/articles")
    public ResponseEntity<ArticleDto> createArticle(@AuthenticationPrincipal UserDetails userDetails,
                                                    @Valid @RequestBody CreateArticleRequest req) {
        ArticleDto dto = knowledgeService.createArticle(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // --- Discussion Forum ---

    @GetMapping("/forum/threads")
    public ResponseEntity<List<ThreadDto>> getThreads(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(knowledgeService.getThreads(search));
    }

    @GetMapping("/forum/threads/{id}")
    public ResponseEntity<ThreadDto> getThreadById(@PathVariable Long id) {
        return ResponseEntity.ok(knowledgeService.getThreadById(id));
    }

    @PostMapping("/forum/threads")
    public ResponseEntity<ThreadDto> createThread(@AuthenticationPrincipal UserDetails userDetails,
                                                  @Valid @RequestBody CreateThreadRequest req) {
        ThreadDto dto = knowledgeService.createThread(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/forum/threads/{id}/replies")
    public ResponseEntity<ReplyDto> postReply(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable Long id,
                                              @Valid @RequestBody PostReplyRequest req) {
        ReplyDto dto = knowledgeService.postReply(userDetails.getUsername(), id, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/forum/threads/{threadId}/replies/{replyId}/accept")
    public ResponseEntity<ReplyDto> markAcceptedAnswer(@AuthenticationPrincipal UserDetails userDetails,
                                                       @PathVariable Long threadId,
                                                       @PathVariable Long replyId) {
        ReplyDto dto = knowledgeService.markAcceptedAnswer(userDetails.getUsername(), threadId, replyId);
        return ResponseEntity.ok(dto);
    }
}
