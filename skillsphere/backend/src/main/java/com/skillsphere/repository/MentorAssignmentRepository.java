package com.skillsphere.repository;

import com.skillsphere.entity.AssignmentStatus;
import com.skillsphere.entity.MentorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MentorAssignmentRepository extends JpaRepository<MentorAssignment, Long> {

    @Query("select a from MentorAssignment a join fetch a.mentor join fetch a.employee join fetch a.assignedBy " +
           "where a.employee.id = :employeeId and a.status = :status")
    Optional<MentorAssignment> findByEmployeeIdAndStatus(@Param("employeeId") Long employeeId,
                                                         @Param("status") AssignmentStatus status);

    @Query("select a from MentorAssignment a join fetch a.mentor join fetch a.employee join fetch a.assignedBy " +
           "where a.mentor.id = :mentorId and a.status = :status order by a.assignedAt desc")
    List<MentorAssignment> findByMentorIdAndStatus(@Param("mentorId") Long mentorId,
                                                  @Param("status") AssignmentStatus status);

    @Query("select a from MentorAssignment a join fetch a.mentor join fetch a.employee join fetch a.assignedBy " +
           "where a.mentor.id = :mentorId order by a.assignedAt desc")
    List<MentorAssignment> findByMentorIdOrderByAssignedAtDesc(@Param("mentorId") Long mentorId);

    @Query("select a from MentorAssignment a join fetch a.mentor join fetch a.employee join fetch a.assignedBy " +
           "where a.employee.id = :employeeId order by a.assignedAt desc")
    List<MentorAssignment> findByEmployeeIdOrderByAssignedAtDesc(@Param("employeeId") Long employeeId);

    long countByMentorIdAndStatus(Long mentorId, AssignmentStatus status);

    boolean existsByEmployeeIdAndStatus(Long employeeId, AssignmentStatus status);

    boolean existsByMentorIdAndEmployeeIdAndStatus(Long mentorId, Long employeeId, AssignmentStatus status);

    @Query("select a from MentorAssignment a join fetch a.mentor join fetch a.employee join fetch a.assignedBy " +
           "order by a.assignedAt desc")
    List<MentorAssignment> findAllWithUsers();
}
