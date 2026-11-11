package com.skillsphere.repository;

import com.skillsphere.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("select m from Message m join fetch m.sender where m.conversation.id = :convId order by m.createdAt asc")
    List<Message> findByConversationIdOrderByCreatedAtAsc(@Param("convId") Long convId);

    @Query("select m from Message m join fetch m.sender where m.conversation.id = :convId order by m.createdAt desc")
    List<Message> findRecentByConversationId(@Param("convId") Long convId, Pageable pageable);

    @Query("select count(m) from Message m where m.conversation.id = :convId and m.createdAt > :since and m.sender.id <> :userId")
    long countUnreadMessages(@Param("convId") Long convId, @Param("since") Instant since, @Param("userId") Long userId);

    @Query("select count(m) from Message m join ConversationParticipant cp on cp.conversation.id = m.conversation.id " +
           "where cp.user.id = :userId and m.sender.id <> :userId and (cp.lastReadAt is null or m.createdAt > cp.lastReadAt)")
    long countTotalUnreadForUser(@Param("userId") Long userId);

    @Query("select m from Message m join fetch m.sender where m.conversation.id = :convId order by m.createdAt desc")
    List<Message> findByConversationIdPaged(@Param("convId") Long convId, Pageable pageable);
}
