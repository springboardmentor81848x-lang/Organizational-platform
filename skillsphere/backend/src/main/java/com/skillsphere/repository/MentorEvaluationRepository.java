package com.skillsphere.repository;

import com.skillsphere.entity.MentorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MentorEvaluationRepository extends JpaRepository<MentorEvaluation, Long> {
    List<MentorEvaluation> findByMentorIdOrderByCreatedAtDesc(Long mentorId);
    List<MentorEvaluation> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
}
