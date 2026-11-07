package com.skillsphere.repository;

import com.skillsphere.entity.DiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {
    @Query("select r from DiscussionReply r join fetch r.author where r.thread.id = :threadId order by r.createdAt asc")
    List<DiscussionReply> findByThreadIdOrderByCreatedAtAsc(@Param("threadId") Long threadId);
}
