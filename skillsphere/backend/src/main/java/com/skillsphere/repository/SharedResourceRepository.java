package com.skillsphere.repository;

import com.skillsphere.entity.SharedResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SharedResourceRepository extends JpaRepository<SharedResource, Long> {
    @Query("select sr from SharedResource sr join fetch sr.sharedBy where sr.assignment.id = :assignmentId order by sr.createdAt desc")
    List<SharedResource> findByAssignmentIdOrderByCreatedAtDesc(@Param("assignmentId") Long assignmentId);
}
