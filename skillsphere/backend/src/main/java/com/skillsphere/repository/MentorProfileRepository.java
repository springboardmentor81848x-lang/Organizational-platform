package com.skillsphere.repository;

import com.skillsphere.entity.MentorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long> {

    @Query("select m from MentorProfile m join fetch m.user join fetch m.approvedBy where m.user.id = :userId")
    Optional<MentorProfile> findByUserId(@Param("userId") Long userId);

    @Query("select m from MentorProfile m join fetch m.user join fetch m.approvedBy where lower(m.user.email) = lower(:email)")
    Optional<MentorProfile> findByUserEmail(@Param("email") String email);

    boolean existsByUserId(Long userId);

    @Query("select m from MentorProfile m join fetch m.user where m.isActive = true")
    List<MentorProfile> findActiveMentors();

    @Query("select m from MentorProfile m join fetch m.user join fetch m.approvedBy order by m.createdAt desc")
    List<MentorProfile> findAllWithDetails();
}
