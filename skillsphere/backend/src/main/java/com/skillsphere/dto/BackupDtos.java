package com.skillsphere.dto;

import java.time.Instant;
import java.util.List;

public class BackupDtos {

    public record BackupMetadataDto(
            String filename,
            long sizeBytes,
            String formattedSize,
            Instant createdAt,
            String sha256,
            String status, // SUCCESS, FAILED
            String databaseName,
            String postgresVersion,
            int tableCount,
            String note,
            boolean protectedSnapshot,
            boolean latestBackup
    ) {}

    public record BackupVerificationDto(
            String filename,
            boolean valid,
            String sha256,
            boolean sha256Matches,
            boolean tocValid,
            int tocEntryCount,
            List<String> tablesFound,
            String message,
            Instant verifiedAt
    ) {}

    public record TestRestoreResultDto(
            boolean success,
            String filename,
            String tempDatabaseName,
            long durationMs,
            int restoredTableCount,
            int verifiedUserCount,
            int verifiedMigrationCount,
            boolean activeDatabaseUnchanged,
            String message,
            Instant completedAt
    ) {}

    public record CreateBackupRequest(
            String note
    ) {}
}
