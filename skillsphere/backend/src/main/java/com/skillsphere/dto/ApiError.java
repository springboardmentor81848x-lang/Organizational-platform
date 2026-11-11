package com.skillsphere.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors,
        String path
) {}
