package com.skillsphere.dto;

import java.util.Map;

public record SaveAnswersRequest(
        Map<Long, Integer> answers
) {}
