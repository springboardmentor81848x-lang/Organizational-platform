package com.skillsphere.repository;

import com.skillsphere.entity.AssessmentQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssessmentQuestionRepository extends JpaRepository<AssessmentQuestion, Long> {

    @Query("select q from AssessmentQuestion q join fetch q.skill where q.assessment.id = :assessmentId and q.attempt is null")
    List<AssessmentQuestion> findByAssessmentId(@Param("assessmentId") Long assessmentId);

    @Query("select q from AssessmentQuestion q join fetch q.skill where q.attempt.id = :attemptId order by q.questionOrder asc")
    List<AssessmentQuestion> findByAttemptIdOrderByQuestionOrderAsc(@Param("attemptId") Long attemptId);

    @Query("select q from AssessmentQuestion q join fetch q.skill where q.skill.id = :skillId and q.attempt is null")
    List<AssessmentQuestion> findBySkillIdAndAttemptIdIsNull(@Param("skillId") Long skillId);

    @Query("select q from AssessmentQuestion q join fetch q.skill where q.skill.id = :skillId and q.difficulty = :difficulty and q.attempt is null")
    List<AssessmentQuestion> findBySkillIdAndDifficultyAndAttemptIdIsNull(@Param("skillId") Long skillId, @Param("difficulty") String difficulty);
}
