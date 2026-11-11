package com.skillsphere.repository;

import com.skillsphere.entity.ReassessmentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReassessmentRequestRepository extends JpaRepository<ReassessmentRequest, Long> {
    List<ReassessmentRequest> findByManagerIdOrderByCreatedAtDesc(Long managerId);
    List<ReassessmentRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    Optional<ReassessmentRequest> findByEmployeeIdAndSkillIdAndStatus(Long employeeId, Long skillId, String status);

    @Query("SELECT r FROM ReassessmentRequest r WHERE r.employee.id IN :employeeIds ORDER BY r.createdAt DESC")
    List<ReassessmentRequest> findByEmployeeIdIn(@Param("employeeIds") List<Long> employeeIds);
}
