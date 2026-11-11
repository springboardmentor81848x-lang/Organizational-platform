package com.skillsphere.repository;

import com.skillsphere.entity.TrainingProgramme;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TrainingProgrammeRepository extends JpaRepository<TrainingProgramme, Long> {
    List<TrainingProgramme> findByStatus(String status);
    List<TrainingProgramme> findByDepartmentId(Long departmentId);

    @Query("select p from TrainingProgramme p left join fetch p.department left join fetch p.skill left join fetch p.course order by p.startDate desc")
    List<TrainingProgramme> findAllWithDetails();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TrainingProgramme p where p.id = :id")
    Optional<TrainingProgramme> findByIdWithLock(@Param("id") Long id);
}
