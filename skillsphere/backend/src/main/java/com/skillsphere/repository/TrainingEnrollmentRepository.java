package com.skillsphere.repository;

import com.skillsphere.entity.TrainingEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrainingEnrollmentRepository extends JpaRepository<TrainingEnrollment, Long> {
    @Query("select te from TrainingEnrollment te join fetch te.programme where te.user.id = :userId")
    List<TrainingEnrollment> findByUserId(@Param("userId") Long userId);

    @Query("select te from TrainingEnrollment te join fetch te.user where te.programme.id = :programmeId")
    List<TrainingEnrollment> findByProgrammeId(@Param("programmeId") Long programmeId);

    Optional<TrainingEnrollment> findByProgrammeIdAndUserId(Long programmeId, Long userId);
}
