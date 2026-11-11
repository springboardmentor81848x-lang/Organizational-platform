package com.skillsphere.repository;

import com.skillsphere.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);
    Optional<Department> findByCode(String code);
    Optional<Department> findByHeadUserId(Long headUserId);
}
