package com.skillsphere.repository;

import com.skillsphere.entity.ManagerCourseRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ManagerCourseRecommendationRepository extends JpaRepository<ManagerCourseRecommendation, Long> {
    List<ManagerCourseRecommendation> findByManagerIdOrderByCreatedAtDesc(Long managerId);
    List<ManagerCourseRecommendation> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    @Query("SELECT r FROM ManagerCourseRecommendation r WHERE r.employee.id IN :employeeIds ORDER BY r.createdAt DESC")
    List<ManagerCourseRecommendation> findByEmployeeIdIn(@Param("employeeIds") List<Long> employeeIds);
}
