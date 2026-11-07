package com.skillsphere.repository;

import com.skillsphere.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    @Query("select e from CalendarEvent e where e.creator.id = :userId or e.id in " +
           "(select ep.event.id from EventParticipant ep where ep.user.id = :userId) order by e.startTime asc")
    List<CalendarEvent> findUserEvents(@Param("userId") Long userId);

    @Query("select e from CalendarEvent e where (e.creator.id = :userId or e.id in " +
           "(select ep.event.id from EventParticipant ep where ep.user.id = :userId)) " +
           "and e.startTime >= :from and e.endTime <= :to order by e.startTime asc")
    List<CalendarEvent> findUserEventsInRange(@Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to);
}
