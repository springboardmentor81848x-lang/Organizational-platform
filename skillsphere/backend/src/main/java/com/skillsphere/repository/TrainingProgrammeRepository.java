package com.skillsphere.repository;

import com.skillsphere.entity.TrainingProgramme;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TrainingProgrammeRepository extends JpaRepository<TrainingProgramme, Long> {
    List<TrainingProgramme> findByStatus(String status);
    List<TrainingProgramme> findByDepartmentId(Long departmentId);

    @org.springframework.data.jpa.repository.Query("select p from TrainingProgramme p left join fetch p.department left join fetch p.skill left join fetch p.course order by p.startDate desc")
    List<TrainingProgramme> findAllWithDetails();
}
