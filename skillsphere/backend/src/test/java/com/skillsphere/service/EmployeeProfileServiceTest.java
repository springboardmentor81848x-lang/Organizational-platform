package com.skillsphere.service;

import com.skillsphere.dto.EmployeeProfileDto;
import com.skillsphere.dto.UpdateProfileRequest;
import com.skillsphere.entity.EmployeeProfile;
import com.skillsphere.entity.Team;
import com.skillsphere.entity.TeamMembership;
import com.skillsphere.entity.User;
import com.skillsphere.repository.EmployeeProfileRepository;
import com.skillsphere.repository.TeamMembershipRepository;
import com.skillsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeProfileServiceTest {

    private EmployeeProfileRepository profileRepository;
    private UserRepository userRepository;
    private TeamMembershipRepository teamMembershipRepository;
    private EmployeeProfileService profileService;

    @BeforeEach
    void setUp() {
        profileRepository = mock(EmployeeProfileRepository.class);
        userRepository = mock(UserRepository.class);
        teamMembershipRepository = mock(TeamMembershipRepository.class);
        profileService = new EmployeeProfileService(profileRepository, userRepository, teamMembershipRepository);
    }

    @Test
    void getProfileByEmail_returnsEnrichedDtoWithTeamAndManager() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 101L);
        user.setEmail("emp@skillsphere.com");
        user.setFullName("Test Employee");
        user.setEmployeeId("EMP-101");

        when(userRepository.findByEmail("emp@skillsphere.com")).thenReturn(Optional.of(user));

        EmployeeProfile profile = new EmployeeProfile(user);
        profile.setJobRole("Software Developer");
        profile.setDepartment("Engineering");
        profile.setExperienceLevel("Intermediate");
        profile.setSkills("Java, Spring Boot");
        profile.setBio("Passionate backend engineer");

        when(profileRepository.findByUserId(101L)).thenReturn(Optional.of(profile));

        User mgr = new User();
        mgr.setFullName("Manager John");

        Team team = new Team(null, "Core Platform Team", mgr);
        TeamMembership tm = new TeamMembership(team, user);

        when(teamMembershipRepository.findByUserIdWithTeamAndManager(101L)).thenReturn(List.of(tm));

        EmployeeProfileDto dto = profileService.getProfileByEmail("emp@skillsphere.com");

        assertNotNull(dto);
        assertEquals(101L, dto.userId());
        assertEquals("Test Employee", dto.fullName());
        assertEquals("EMP-101", dto.employeeId());
        assertEquals("Software Developer", dto.jobRole());
        assertEquals("Engineering", dto.department());
        assertEquals("Intermediate", dto.experienceLevel());
        assertEquals("Core Platform Team", dto.teamName());
        assertEquals("Manager John", dto.reportingManagerName());
        assertEquals("Passionate backend engineer", dto.bio());
        assertEquals("Java, Spring Boot", dto.skills());
    }

    @Test
    void updateProfile_savesProfileAndReturnsEnrichedDto() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 101L);
        user.setEmail("emp@skillsphere.com");
        user.setFullName("Test Employee");
        user.setEmployeeId("EMP-101");

        when(userRepository.findByEmail("emp@skillsphere.com")).thenReturn(Optional.of(user));

        EmployeeProfile profile = new EmployeeProfile(user);
        when(profileRepository.findByUserId(101L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(EmployeeProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teamMembershipRepository.findByUserIdWithTeamAndManager(101L)).thenReturn(List.of());

        UpdateProfileRequest req = new UpdateProfileRequest(
                "QA Engineer",
                "Quality Assurance",
                "Experienced",
                "Automated testing enthusiast",
                "Selenium, JUnit, Playwright"
        );

        EmployeeProfileDto updated = profileService.updateProfile("emp@skillsphere.com", req);

        assertNotNull(updated);
        assertEquals("QA Engineer", updated.jobRole());
        assertEquals("Quality Assurance", updated.department());
        assertEquals("Experienced", updated.experienceLevel());
        assertEquals("Automated testing enthusiast", updated.bio());
        assertEquals("Selenium, JUnit, Playwright", updated.skills());
    }
}
