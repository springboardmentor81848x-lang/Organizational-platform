package com.skillsphere.repository;

import com.skillsphere.entity.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {

    @Query("select a from AssessmentAttempt a join fetch a.skill join fetch a.user left join fetch a.assessment where a.user.id = :userId and a.status = :status order by a.startedAt desc")
    List<AssessmentAttempt> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("select a from AssessmentAttempt a join fetch a.skill join fetch a.user left join fetch a.assessment where a.user.id = :userId and a.skill.id = :skillId and a.status = :status order by a.startedAt desc")
    List<AssessmentAttempt> findByUserIdAndSkillIdAndStatus(@Param("userId") Long userId, @Param("skillId") Long skillId, @Param("status") String status);

    @Query("select a from AssessmentAttempt a join fetch a.skill where a.user.id = :userId and a.skill.id = :skillId order by a.startedAt desc")
    List<AssessmentAttempt> findTop5ByUserIdAndSkillIdOrderByStartedAtDesc(@Param("userId") Long userId, @Param("skillId") Long skillId);

    @Query("select a from AssessmentAttempt a join fetch a.skill join fetch a.user left join fetch a.assessment where a.user.id = :userId order by a.startedAt desc")
    List<AssessmentAttempt> findByUserIdOrderByStartedAtDesc(@Param("userId") Long userId);

    @Query("select a from AssessmentAttempt a join fetch a.skill join fetch a.user left join fetch a.assessment where a.user.id in :userIds order by a.startedAt desc")
    List<AssessmentAttempt> findByUserIdsOrderByStartedAtDesc(@Param("userIds") List<Long> userIds);

    @Query("select a from AssessmentAttempt a join fetch a.skill join fetch a.user left join fetch a.assessment order by a.startedAt desc")
    List<AssessmentAttempt> findAllWithDetails();
}
