package com.skillsphere.repository;

import com.skillsphere.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    @Query("select c from Certificate c join fetch c.course where c.user.id = :userId order by c.issueDate desc")
    List<Certificate> findByUserId(@Param("userId") Long userId);

    Optional<Certificate> findByCertificateCode(String certificateCode);
}
