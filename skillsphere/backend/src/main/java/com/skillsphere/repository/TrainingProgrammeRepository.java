package com.skillsphere.repository;

import com.skillsphere.entity.TrainingProgramme;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingProgrammeRepository extends JpaRepository<TrainingProgramme, Long> {
    List<TrainingProgramme> findByStatus(String status);
    List<TrainingProgramme> findByDepartmentId(Long departmentId);
}
