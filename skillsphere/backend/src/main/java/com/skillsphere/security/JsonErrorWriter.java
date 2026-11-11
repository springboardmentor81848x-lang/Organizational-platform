package com.skillsphere.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/** Writes the same JSON error shape as GlobalExceptionHandler for errors raised inside the filter chain. */
@Component
public class JsonErrorWriter {

    private final ObjectMapper mapper;

    public JsonErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletRequest req, HttpServletResponse res, HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), new ApiError(Instant.now(), status.value(),
                status.getReasonPhrase(), message, Map.of(), req.getRequestURI()));
    }
}
