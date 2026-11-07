package com.skillsphere.service;

import com.skillsphere.dto.*;
import com.skillsphere.entity.*;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class KnowledgeService {

    private final KnowledgeCategoryRepository categoryRepository;
    private final KnowledgeArticleRepository articleRepository;
    private final DiscussionThreadRepository threadRepository;
    private final DiscussionReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public KnowledgeService(KnowledgeCategoryRepository categoryRepository,
                            KnowledgeArticleRepository articleRepository,
                            DiscussionThreadRepository threadRepository,
                            DiscussionReplyRepository replyRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.categoryRepository = categoryRepository;
        this.articleRepository = articleRepository;
        this.threadRepository = threadRepository;
        this.replyRepository = replyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeCategory> getCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ArticleDto> getArticles(String search) {
        List<KnowledgeArticle> articles;
        if (search != null && !search.isBlank()) {
            articles = articleRepository.searchPublished(search.trim());
        } else {
            articles = articleRepository.findByStatusOrderByCreatedAtDesc("PUBLISHED");
        }
        return articles.stream().map(this::toArticleDto).toList();
    }

    @Transactional(readOnly = true)
    public ArticleDto getArticleById(Long id) {
        KnowledgeArticle a = articleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Article not found."));
        return toArticleDto(a);
    }

    @Transactional
    public ArticleDto createArticle(String email, CreateArticleRequest req) {
        User author = getUserByEmail(email);
        KnowledgeCategory cat = null;
        if (req.categoryId() != null) {
            cat = categoryRepository.findById(req.categoryId()).orElse(null);
        }

        String status = req.status() != null ? req.status().toUpperCase() : "PUBLISHED";
        KnowledgeArticle article = new KnowledgeArticle(
                cat,
                author,
                req.title().trim(),
                req.content().trim(),
                req.tags(),
                status
        );
        KnowledgeArticle saved = articleRepository.save(article);
        return toArticleDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ThreadDto> getThreads(String search) {
        List<DiscussionThread> threads;
        if (search != null && !search.isBlank()) {
            threads = threadRepository.search(search.trim());
        } else {
            threads = threadRepository.findAllWithAuthor();
        }

        return threads.stream().map(t -> new ThreadDto(
                t.getId(),
                t.getAuthor().getId(),
                t.getAuthor().getFullName(),
                t.getTitle(),
                t.getContent(),
                t.getCategory(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getReplyCount(),
                Collections.emptyList()
        )).toList();
    }

    @Transactional(readOnly = true)
    public ThreadDto getThreadById(Long threadId) {
        DiscussionThread t = threadRepository.findById(threadId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Thread not found."));

        List<DiscussionReply> replies = replyRepository.findByThreadIdOrderByCreatedAtAsc(threadId);
        List<ReplyDto> replyDtos = replies.stream().map(r -> new ReplyDto(
                r.getId(),
                threadId,
                r.getAuthor().getId(),
                r.getAuthor().getFullName(),
                r.getContent(),
                r.isAcceptedAnswer(),
                r.getCreatedAt()
        )).toList();

        return new ThreadDto(
                t.getId(),
                t.getAuthor().getId(),
                t.getAuthor().getFullName(),
                t.getTitle(),
                t.getContent(),
                t.getCategory(),
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getReplyCount(),
                replyDtos
        );
    }

    @Transactional
    public ThreadDto createThread(String email, CreateThreadRequest req) {
        User author = getUserByEmail(email);
        DiscussionThread t = new DiscussionThread(
                author,
                req.title().trim(),
                req.content().trim(),
                req.category() != null ? req.category().trim() : "General"
        );
        DiscussionThread saved = threadRepository.save(t);
        return new ThreadDto(
                saved.getId(),
                author.getId(),
                author.getFullName(),
                saved.getTitle(),
                saved.getContent(),
                saved.getCategory(),
                saved.getCreatedAt(),
                saved.getUpdatedAt(),
                0,
                Collections.emptyList()
        );
    }

    @Transactional
    public ReplyDto postReply(String email, Long threadId, PostReplyRequest req) {
        User author = getUserByEmail(email);
        DiscussionThread t = threadRepository.findById(threadId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Thread not found."));

        DiscussionReply reply = new DiscussionReply(t, author, req.content().trim());
        DiscussionReply saved = replyRepository.save(reply);

        t.setReplyCount(t.getReplyCount() + 1);
        t.setUpdatedAt(Instant.now());
        threadRepository.save(t);

        if (!t.getAuthor().getId().equals(author.getId())) {
            notificationService.createNotification(
                    t.getAuthor(),
                    "FORUM_REPLY",
                    "New Reply on: " + t.getTitle(),
                    author.getFullName() + " replied: " + (req.content().length() > 50 ? req.content().substring(0, 47) + "..." : req.content()),
                    "/dashboard/employee"
            );
        }

        return new ReplyDto(
                saved.getId(),
                threadId,
                author.getId(),
                author.getFullName(),
                saved.getContent(),
                saved.isAcceptedAnswer(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public ReplyDto markAcceptedAnswer(String email, Long threadId, Long replyId) {
        User caller = getUserByEmail(email);
        DiscussionThread t = threadRepository.findById(threadId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Thread not found."));

        if (!t.getAuthor().getId().equals(caller.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the thread author can mark an accepted answer.");
        }

        DiscussionReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reply not found."));

        reply.setAcceptedAnswer(true);
        DiscussionReply saved = replyRepository.save(reply);

        notificationService.createNotification(
                reply.getAuthor(),
                "ANSWER_ACCEPTED",
                "Your Answer Was Accepted!",
                caller.getFullName() + " marked your reply as the accepted solution on \"" + t.getTitle() + "\".",
                "/dashboard/employee"
        );

        return new ReplyDto(
                saved.getId(),
                threadId,
                saved.getAuthor().getId(),
                saved.getAuthor().getFullName(),
                saved.getContent(),
                saved.isAcceptedAnswer(),
                saved.getCreatedAt()
        );
    }

    private ArticleDto toArticleDto(KnowledgeArticle a) {
        return new ArticleDto(
                a.getId(),
                a.getCategory() != null ? a.getCategory().getId() : null,
                a.getCategory() != null ? a.getCategory().getName() : "General",
                a.getAuthor().getId(),
                a.getAuthor().getFullName(),
                a.getTitle(),
                a.getContent(),
                a.getTags(),
                a.getStatus(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }
}
