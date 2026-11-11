package com.skillsphere.repository;

import com.skillsphere.entity.AssessmentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentResponseRepository extends JpaRepository<AssessmentResponse, Long> {
    @Query("select r from AssessmentResponse r join fetch r.question q join fetch q.skill where r.submission.id = :submissionId")
    List<AssessmentResponse> findBySubmissionId(@Param("submissionId") Long submissionId);
}
