package com.skillsphere.repository;

import com.skillsphere.entity.MentorshipSession;
import com.skillsphere.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MentorshipSessionRepository extends JpaRepository<MentorshipSession, Long> {

    @Query("select s from MentorshipSession s join fetch s.assignment a " +
           "where a.id = :assignmentId order by s.scheduledAt desc")
    List<MentorshipSession> findByAssignmentIdOrderByScheduledAtDesc(@Param("assignmentId") Long assignmentId);

    @Query("select s from MentorshipSession s join fetch s.assignment a " +
           "where a.id = :assignmentId and s.status = :status order by s.scheduledAt desc")
    List<MentorshipSession> findByAssignmentIdAndStatus(@Param("assignmentId") Long assignmentId,
                                                        @Param("status") SessionStatus status);

    @Query("select s from MentorshipSession s join fetch s.assignment a join fetch a.employee " +
           "where a.mentor.id = :mentorId order by s.scheduledAt desc")
    List<MentorshipSession> findByMentorIdOrderByScheduledAtDesc(@Param("mentorId") Long mentorId);

    @Query("select s from MentorshipSession s join fetch s.assignment a join fetch a.employee " +
           "where a.mentor.id = :mentorId and s.status = :status order by s.scheduledAt desc")
    List<MentorshipSession> findByMentorIdAndStatus(@Param("mentorId") Long mentorId,
                                                        @Param("status") SessionStatus status);

    long countByAssignmentMentorIdAndStatus(Long mentorId, SessionStatus status);
}
