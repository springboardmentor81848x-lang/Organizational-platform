package com.skillsphere.repository;

import com.skillsphere.entity.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
    List<EventParticipant> findByEventId(Long eventId);
    List<EventParticipant> findByUserId(Long userId);
}
