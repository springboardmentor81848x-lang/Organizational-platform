package com.skillsphere.repository;

import com.skillsphere.entity.MentorshipRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MentorshipRequestRepository extends JpaRepository<MentorshipRequest, Long> {
    @Query("select mr from MentorshipRequest mr join fetch mr.mentor join fetch mr.employee where mr.mentor.id = :mentorId order by mr.createdAt desc")
    List<MentorshipRequest> findByMentorIdOrderByCreatedAtDesc(@Param("mentorId") Long mentorId);

    @Query("select mr from MentorshipRequest mr join fetch mr.mentor join fetch mr.employee where mr.employee.id = :employeeId order by mr.createdAt desc")
    List<MentorshipRequest> findByEmployeeIdOrderByCreatedAtDesc(@Param("employeeId") Long employeeId);

    boolean existsByMentorIdAndEmployeeIdAndStatus(Long mentorId, Long employeeId, String status);
}
