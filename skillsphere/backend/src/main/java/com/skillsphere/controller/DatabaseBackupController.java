package com.skillsphere.controller;

import com.skillsphere.dto.BackupDtos.*;
import com.skillsphere.service.DatabaseBackupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/backups")
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseBackupController {

    private final DatabaseBackupService backupService;

    public DatabaseBackupController(DatabaseBackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping
    public ResponseEntity<List<BackupMetadataDto>> listBackups() {
        return ResponseEntity.ok(backupService.listBackups());
    }

    @PostMapping
    public ResponseEntity<BackupMetadataDto> createBackup(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) CreateBackupRequest req,
            HttpServletRequest request) {
        String note = (req != null) ? req.note() : null;
        BackupMetadataDto dto = backupService.createBackup(userDetails.getUsername(), note, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{filename}/verify")
    public ResponseEntity<BackupVerificationDto> verifyBackup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String filename,
            HttpServletRequest request) {
        BackupVerificationDto dto = backupService.verifyBackup(filename, userDetails.getUsername(), request.getRemoteAddr());
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{filename}/test-restore")
    public ResponseEntity<TestRestoreResultDto> testRestore(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String filename,
            HttpServletRequest request) {
        TestRestoreResultDto dto = backupService.testRestore(filename, userDetails.getUsername(), request.getRemoteAddr());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<Void> deleteBackup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String filename,
            HttpServletRequest request) {
        backupService.deleteBackup(filename, userDetails.getUsername(), request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{filename}/download")
    public ResponseEntity<Resource> downloadBackup(@PathVariable String filename) {
        Resource file = backupService.downloadBackup(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }
}
