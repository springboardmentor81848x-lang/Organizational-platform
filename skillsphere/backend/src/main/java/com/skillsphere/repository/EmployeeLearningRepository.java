package com.skillsphere.repository;

import com.skillsphere.entity.EmployeeLearning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeLearningRepository extends JpaRepository<EmployeeLearning, Long> {
    @Query("select el from EmployeeLearning el join fetch el.course c left join fetch c.skill where el.user.id = :userId")
    List<EmployeeLearning> findByUserId(@Param("userId") Long userId);

    Optional<EmployeeLearning> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("select el from EmployeeLearning el join fetch el.course join fetch el.user where el.user.id in :userIds")
    List<EmployeeLearning> findByUserIds(@Param("userIds") List<Long> userIds);

    @Query("select el from EmployeeLearning el join fetch el.course c left join fetch c.skill join fetch el.user u order by el.enrolledAt desc")
    List<EmployeeLearning> findAllWithDetails();
}
