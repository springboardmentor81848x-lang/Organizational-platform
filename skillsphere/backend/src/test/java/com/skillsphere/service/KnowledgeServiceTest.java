package com.skillsphere.service;

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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeCategoryRepository categoryRepository;
    @Mock
    private KnowledgeArticleRepository articleRepository;
    @Mock
    private DiscussionThreadRepository threadRepository;
    @Mock
    private DiscussionReplyRepository replyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private KnowledgeService knowledgeService;

    private User author;
    private User otherUser;
    private KnowledgeCategory category;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setFullName("Alice Author");
        author.setEmail("alice@example.com");
        author.setRole(Role.EMPLOYEE);
        ReflectionTestUtils.setField(author, "id", 101L);

        otherUser = new User();
        otherUser.setFullName("Bob Reader");
        otherUser.setEmail("bob@example.com");
        otherUser.setRole(Role.EMPLOYEE);
        ReflectionTestUtils.setField(otherUser, "id", 102L);

        category = new KnowledgeCategory("Architecture", "Software architecture");
        ReflectionTestUtils.setField(category, "id", 1L);
    }

    @Test
    @DisplayName("getArticles with search term queries searchPublished")
    void testGetArticles_WithSearch() {
        KnowledgeArticle art = new KnowledgeArticle(category, author, "Clean Code Practices", "Content here", "clean-code", "PUBLISHED");
        when(articleRepository.searchPublished("Clean")).thenReturn(List.of(art));

        List<ArticleDto> results = knowledgeService.getArticles("Clean");

        assertEquals(1, results.size());
        assertEquals("Clean Code Practices", results.get(0).title());
        verify(articleRepository).searchPublished("Clean");
    }

    @Test
    @DisplayName("createThread saves discussion thread and returns ThreadDto")
    void testCreateThread_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(author));
        when(threadRepository.save(any(DiscussionThread.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateThreadRequest req = new CreateThreadRequest("Spring Boot 3 Migration Questions", "How to migrate from 2.7?", "Technical");
        ThreadDto dto = knowledgeService.createThread("alice@example.com", req);

        assertNotNull(dto);
        assertEquals("Spring Boot 3 Migration Questions", dto.title());
        assertEquals(0, dto.replyCount());
        verify(threadRepository).save(any(DiscussionThread.class));
    }

    @Test
    @DisplayName("postReply increments thread reply count and notifies author")
    void testPostReply_Success() {
        DiscussionThread thread = new DiscussionThread(author, "Thread Title", "Thread Body", "General");
        ReflectionTestUtils.setField(thread, "id", 55L);

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));
        when(threadRepository.findById(55L)).thenReturn(Optional.of(thread));
        when(replyRepository.save(any(DiscussionReply.class))).thenAnswer(inv -> inv.getArgument(0));

        PostReplyRequest req = new PostReplyRequest("Use OpenRewrite to automate the migration!");
        ReplyDto replyDto = knowledgeService.postReply("bob@example.com", 55L, req);

        assertNotNull(replyDto);
        assertEquals(1, thread.getReplyCount());
        verify(threadRepository).save(thread);
        verify(notificationService).createNotification(eq(author), eq("FORUM_REPLY"), any(), any(), any());
    }

    @Test
    @DisplayName("markAcceptedAnswer succeeds when called by thread author")
    void testMarkAcceptedAnswer_Success() {
        DiscussionThread thread = new DiscussionThread(author, "Thread Title", "Thread Body", "General");
        ReflectionTestUtils.setField(thread, "id", 55L);

        DiscussionReply reply = new DiscussionReply(thread, otherUser, "Correct answer here");
        ReflectionTestUtils.setField(reply, "id", 77L);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(author));
        when(threadRepository.findById(55L)).thenReturn(Optional.of(thread));
        when(replyRepository.findById(77L)).thenReturn(Optional.of(reply));
        when(replyRepository.save(any(DiscussionReply.class))).thenAnswer(inv -> inv.getArgument(0));

        ReplyDto result = knowledgeService.markAcceptedAnswer("alice@example.com", 55L, 77L);

        assertNotNull(result);
        assertTrue(result.isAcceptedAnswer());
        verify(replyRepository).save(reply);
    }

    @Test
    @DisplayName("markAcceptedAnswer throws FORBIDDEN when called by non-author")
    void testMarkAcceptedAnswer_ForbiddenForNonAuthor() {
        DiscussionThread thread = new DiscussionThread(author, "Thread Title", "Thread Body", "General");
        ReflectionTestUtils.setField(thread, "id", 55L);

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));
        when(threadRepository.findById(55L)).thenReturn(Optional.of(thread));

        ApiException ex = assertThrows(ApiException.class, () ->
                knowledgeService.markAcceptedAnswer("bob@example.com", 55L, 77L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Only the thread author"));
    }
}
