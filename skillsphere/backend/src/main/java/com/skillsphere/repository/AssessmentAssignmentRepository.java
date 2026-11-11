package com.skillsphere.repository;

import com.skillsphere.entity.AssessmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentAssignmentRepository extends JpaRepository<AssessmentAssignment, Long> {
    @Query("select a from AssessmentAssignment a join fetch a.assessment join fetch a.assignedBy where a.user.id = :userId")
    List<AssessmentAssignment> findByUserId(@Param("userId") Long userId);

    @Query("select a from AssessmentAssignment a join fetch a.assessment join fetch a.user where a.assignedBy.id = :assignedById")
    List<AssessmentAssignment> findByAssignedById(@Param("assignedById") Long assignedById);
}
