package com.wki.auth.repository;

import com.wki.auth.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);

    /** Updates only the last_login_at column, so no other column can be overwritten. */
    @Modifying
    @Transactional
    @Query("update Employee e set e.lastLoginAt = :time where e.employeeId = :id")
    int updateLastLogin(@Param("id") Long id, @Param("time") LocalDateTime time);
}
