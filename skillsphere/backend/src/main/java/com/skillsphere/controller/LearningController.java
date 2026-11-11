package com.skillsphere.controller;

import com.skillsphere.dto.*;
import com.skillsphere.service.LearningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning")
public class LearningController {

    private final LearningService learningService;

    public LearningController(LearningService learningService) {
        this.learningService = learningService;
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseDto>> getAllCourses() {
        return ResponseEntity.ok(learningService.getAllCourses());
    }

    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<CourseDto> createCourse(@RequestBody CourseDto dto) {
        CourseDto created = learningService.createCourse(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<CourseDto>> getRecommendations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(learningService.getRecommendedCourses(userDetails.getUsername()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EmployeeLearningDto>> getMyLearning(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(learningService.getMyLearning(userDetails.getUsername()));
    }

    @PostMapping("/enroll/{courseId}")
    public ResponseEntity<EmployeeLearningDto> enroll(@AuthenticationPrincipal UserDetails userDetails,
                                                      @PathVariable Long courseId) {
        EmployeeLearningDto enrolled = learningService.enrollInCourse(userDetails.getUsername(), courseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(enrolled);
    }

    @PutMapping("/progress/{courseId}")
    public ResponseEntity<EmployeeLearningDto> updateProgress(@AuthenticationPrincipal UserDetails userDetails,
                                                              @PathVariable Long courseId,
                                                              @RequestBody Map<String, Integer> body) {
        int progress = body.getOrDefault("progressPct", 0);
        EmployeeLearningDto updated = learningService.updateProgress(userDetails.getUsername(), courseId, progress);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/certificates")
    public ResponseEntity<List<CertificateDto>> getMyCertificates(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(learningService.getMyCertificates(userDetails.getUsername()));
    }

    @GetMapping("/training-programmes")
    public ResponseEntity<List<TrainingProgrammeDto>> getTrainingProgrammes(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(learningService.getAllTrainingProgrammes(userDetails.getUsername()));
    }

    @PostMapping("/training-programmes")
    @PreAuthorize("hasAnyRole('HR_SPECIALIST', 'ADMIN')")
    public ResponseEntity<TrainingProgrammeDto> createTrainingProgramme(@RequestBody TrainingProgrammeDto dto) {
        TrainingProgrammeDto created = learningService.createTrainingProgramme(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/training-programmes/{id}/enroll")
    public ResponseEntity<TrainingProgrammeDto> enrollInTrainingProgramme(@AuthenticationPrincipal UserDetails userDetails,
                                                                          @PathVariable Long id) {
        TrainingProgrammeDto result = learningService.enrollInTrainingProgramme(userDetails.getUsername(), id);
        return ResponseEntity.ok(result);
    }
}
