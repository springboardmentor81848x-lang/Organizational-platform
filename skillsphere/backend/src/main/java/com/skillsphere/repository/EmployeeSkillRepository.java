package com.skillsphere.repository;

import com.skillsphere.entity.EmployeeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, Long> {
    @Query("select es from EmployeeSkill es join fetch es.skill s join fetch s.category where es.user.id = :userId")
    List<EmployeeSkill> findByUserId(@Param("userId") Long userId);

    Optional<EmployeeSkill> findByUserIdAndSkillId(Long userId, Long skillId);

    @Query("select es from EmployeeSkill es join fetch es.user join fetch es.skill where es.user.id in :userIds")
    List<EmployeeSkill> findByUserIds(@Param("userIds") List<Long> userIds);
}
