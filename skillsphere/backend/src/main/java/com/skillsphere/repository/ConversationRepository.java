package com.skillsphere.repository;

import com.skillsphere.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("select c from Conversation c join ConversationParticipant cp on cp.conversation.id = c.id where cp.user.id = :userId order by c.updatedAt desc")
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);
}
