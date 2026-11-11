package com.skillsphere.repository;

import com.skillsphere.entity.AssessmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentSubmissionRepository extends JpaRepository<AssessmentSubmission, Long> {
    @Query("select s from AssessmentSubmission s join fetch s.assessment where s.user.id = :userId order by s.completedAt desc")
    List<AssessmentSubmission> findByUserIdOrderByCompletedAtDesc(@Param("userId") Long userId);

    @Query("select s from AssessmentSubmission s join fetch s.assessment join fetch s.user where s.user.id in :userIds order by s.completedAt desc")
    List<AssessmentSubmission> findByUserIdsOrderByCompletedAtDesc(@Param("userIds") List<Long> userIds);
}
