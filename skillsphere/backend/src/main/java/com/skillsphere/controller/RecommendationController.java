package com.skillsphere.controller;

import com.skillsphere.dto.RecommendationRequest;
import com.skillsphere.dto.RecommendationResponse;
import com.skillsphere.service.GeminiRecommendationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI course recommendations (Google Gemini) for the Employee workspace. */
@RestController
@RequestMapping("/api/ai")
public class RecommendationController {

    private final GeminiRecommendationService service;

    public RecommendationController(GeminiRecommendationService service) {
        this.service = service;
    }

    @PostMapping("/recommendations")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request, Authentication auth) {
        return service.recommend(auth.getName(), request);
    }
}
