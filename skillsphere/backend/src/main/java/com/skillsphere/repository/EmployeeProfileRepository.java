package com.skillsphere.repository;

import com.skillsphere.entity.EmployeeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {

    @Query("select p from EmployeeProfile p join fetch p.user where p.user.id = :userId")
    Optional<EmployeeProfile> findByUserId(@Param("userId") Long userId);

    @Query("select p from EmployeeProfile p join fetch p.user where lower(p.user.email) = lower(:email)")
    Optional<EmployeeProfile> findByUserEmail(@Param("email") String email);

    @Query("select p from EmployeeProfile p join fetch p.user")
    java.util.List<EmployeeProfile> findAllWithUser();

    boolean existsByUserId(Long userId);
}
