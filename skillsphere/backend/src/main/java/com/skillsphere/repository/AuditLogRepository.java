package com.skillsphere.repository;

import com.skillsphere.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("select al from AuditLog al left join fetch al.user order by al.createdAt desc")
    List<AuditLog> findAllWithUser();
}
