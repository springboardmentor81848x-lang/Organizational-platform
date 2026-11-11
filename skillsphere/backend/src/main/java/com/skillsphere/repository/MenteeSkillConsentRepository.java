package com.skillsphere.repository;

import com.skillsphere.entity.MenteeSkillConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MenteeSkillConsentRepository extends JpaRepository<MenteeSkillConsent, Long> {
    Optional<MenteeSkillConsent> findByEmployeeIdAndMentorId(Long employeeId, Long mentorId);
}
