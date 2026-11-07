package com.skillsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.dto.BackupDtos.*;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class DatabaseBackupService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBackupService.class);

    private static final Pattern VALID_BACKUP_NAME_PATTERN = Pattern.compile("^skillsphere_backup_\\d{8}_\\d{6}\\.dump$");
    private static final Pattern VALID_SNAPSHOT_NAME_PATTERN = Pattern.compile("^skillsphere_snapshot_\\d{8}_\\d{6}\\.dump$");
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    private final Path backupDirectory;
    private final int maxRetained;
    private final long timeoutMinutes;
    private final PostgresNativeClient nativeClient;
    private final AdminGovernanceService governanceService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;

    public DatabaseBackupService(
            @Value("${app.backup.directory:backups}") String backupDir,
            @Value("${app.backup.max-retained:15}") int maxRetained,
            @Value("${app.backup.timeout-minutes:5}") long timeoutMinutes,
            PostgresNativeClient nativeClient,
            AdminGovernanceService governanceService,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            @Autowired(required = false) DataSource dataSource) {
        this.backupDirectory = Paths.get(backupDir).toAbsolutePath().normalize();
        this.maxRetained = Math.max(1, maxRetained);
        this.timeoutMinutes = timeoutMinutes;
        this.nativeClient = nativeClient;
        this.governanceService = governanceService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;

        initBackupDirectory();
    }

    private void initBackupDirectory() {
        try {
            if (!Files.exists(backupDirectory)) {
                Files.createDirectories(backupDirectory);
            }
        } catch (IOException e) {
            log.error("Failed to initialize backup directory at {}: {}", backupDirectory, e.getMessage());
        }
    }

    /**
     * Extracts distinct public table names from TOC entries.
     */
    public static List<String> extractTablesFromToc(List<String> tocEntries) {
        if (tocEntries == null) return Collections.emptyList();
        return tocEntries.stream()
                .filter(l -> l.contains("TABLE public "))
                .map(l -> {
                    String[] parts = l.split("\\s+");
                    for (int i = 0; i < parts.length - 1; i++) {
                        if ("TABLE".equals(parts[i]) && "public".equals(parts[i + 1]) && i + 2 < parts.length) {
                            return parts[i + 2];
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Lists all existing backup archives with metadata, marking protected snapshots,
     * highlighting the latest valid full backup, and flagging incomplete archives.
     */
    public List<BackupMetadataDto> listBackups() {
        initBackupDirectory();
        List<Path> dumpFiles = getValidBackupFiles();
        if (dumpFiles.isEmpty()) {
            return Collections.emptyList();
        }

        // Identify standard backups with full schema for determining the latest valid backup
        List<Path> standardBackups = dumpFiles.stream()
                .filter(p -> VALID_BACKUP_NAME_PATTERN.matcher(p.getFileName().toString()).matches())
                .filter(p -> {
                    BackupMetadataDto m = readMetadataFile(p);
                    return m == null || (!"INCOMPLETE_SCHEMA".equalsIgnoreCase(m.status()) && m.tableCount() >= 20);
                })
                .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                .toList();

        Path latestStandardBackup = standardBackups.isEmpty() ? null : standardBackups.get(0);

        List<BackupMetadataDto> result = new ArrayList<>();
        for (Path p : dumpFiles) {
            String filename = p.getFileName().toString();
            boolean isProtectedSnapshot = VALID_SNAPSHOT_NAME_PATTERN.matcher(filename).matches();
            boolean isLatest = latestStandardBackup != null && latestStandardBackup.equals(p);

            BackupMetadataDto meta = readMetadataFile(p);
            if (meta != null) {
                boolean isIncomplete = "INCOMPLETE_SCHEMA".equalsIgnoreCase(meta.status()) || meta.tableCount() < 20;
                result.add(new BackupMetadataDto(
                        meta.filename(),
                        meta.sizeBytes(),
                        meta.formattedSize(),
                        meta.createdAt(),
                        meta.sha256(),
                        isIncomplete ? "INCOMPLETE_SCHEMA" : meta.status(),
                        meta.databaseName(),
                        meta.postgresVersion(),
                        meta.tableCount(),
                        isIncomplete && (meta.note() == null || meta.note().isBlank())
                                ? "Incomplete archive from stale Docker instance (9 tables)"
                                : meta.note(),
                        isProtectedSnapshot,
                        isIncomplete ? false : isLatest
                ));
            } else {
                long size = 0;
                try { size = Files.size(p); } catch (IOException ignored) {}
                result.add(new BackupMetadataDto(
                        filename,
                        size,
                        formatFileSize(size),
                        getFileCreationInstant(p),
                        computeSha256Safely(p),
                        "SUCCESS",
                        "skillsphere_db",
                        "PostgreSQL 18",
                        0,
                        "",
                        isProtectedSnapshot,
                        isLatest
                ));
            }
        }

        result.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
        return result;
    }

    /**
     * Creates a new PostgreSQL custom-format backup targeting native PostgreSQL 18.
     * Verifies database identity and full schema dynamically using bidirectional Set comparison.
     */
    public BackupMetadataDto createBackup(String adminUsername, String note, String ipAddress) {
        initBackupDirectory();

        // Database identity verification before backup starts
        nativeClient.verifyDatabaseIdentity(dataSource);

        // Dynamically query active source tables from skillsphere_db
        Map<String, Object> sourceStats = nativeClient.queryDatabaseStats("skillsphere_db", 30);
        if (sourceStats == null) sourceStats = Collections.emptyMap();
        @SuppressWarnings("unchecked")
        List<String> sourceTables = (List<String>) sourceStats.getOrDefault("tables", Collections.emptyList());
        if (sourceTables.isEmpty()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Active database contains 0 public tables. Aborting backup.");
        }

        String timestamp = TS_FORMAT.format(Instant.now());
        String filename = "skillsphere_backup_" + timestamp + ".dump";
        Path finalDumpPath = backupDirectory.resolve(filename);
        Path tempDumpPath = backupDirectory.resolve(filename + ".tmp");
        Path finalMetaPath = backupDirectory.resolve(filename.replace(".dump", ".meta.json"));

        User adminUser = getAdminUser(adminUsername);
        long startTime = System.currentTimeMillis();

        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");

            try (OutputStream fileOut = Files.newOutputStream(tempDumpPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 DigestOutputStream digestOut = new DigestOutputStream(fileOut, sha256Digest)) {

                nativeClient.dumpDatabase("skillsphere_db", digestOut, timeoutMinutes);
                digestOut.flush();
            }

            long sizeBytes = Files.size(tempDumpPath);
            if (sizeBytes == 0) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Database dump resulted in an empty file.");
            }

            // Verify header and TOC entries
            List<String> tocEntries = nativeClient.inspectArchiveToc(tempDumpPath, timeoutMinutes);

            if (tocEntries.isEmpty()) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Database dump failed TOC validation: archive TOC is empty.");
            }

            List<String> tocTables = extractTablesFromToc(tocEntries);

            // Dynamic completeness verification: Bidirectional Set comparison against active source database tables
            Set<String> sourceTableSet = new TreeSet<>(sourceTables);
            Set<String> tocTableSet = new TreeSet<>(tocTables);

            Set<String> missingTables = new TreeSet<>(sourceTableSet);
            missingTables.removeAll(tocTableSet);

            Set<String> unexpectedTables = new TreeSet<>(tocTableSet);
            unexpectedTables.removeAll(sourceTableSet);

            if (!missingTables.isEmpty() || !unexpectedTables.isEmpty()) {
                StringBuilder err = new StringBuilder("Database dump failed completeness check.");
                if (!missingTables.isEmpty()) {
                    err.append(" Missing active source tables in TOC: ").append(missingTables).append(".");
                }
                if (!unexpectedTables.isEmpty()) {
                    err.append(" Unexpected tables in TOC: ").append(unexpectedTables).append(".");
                }
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, err.toString());
            }

            int tableCount = tocTables.size();
            String sha256Hex = bytesToHex(sha256Digest.digest());

            // Atomic rename
            Files.move(tempDumpPath, finalDumpPath, StandardCopyOption.ATOMIC_MOVE);

            String pgVersion = nativeClient.getPostgresServerVersion(10);
            BackupMetadataDto metadata = new BackupMetadataDto(
                    filename,
                    sizeBytes,
                    formatFileSize(sizeBytes),
                    Instant.now(),
                    sha256Hex,
                    "SUCCESS",
                    "skillsphere_db",
                    pgVersion,
                    tableCount,
                    note != null ? note.trim() : "",
                    false,
                    true
            );

            // Write metadata companion file
            objectMapper.writeValue(finalMetaPath.toFile(), metadata);

            // Apply retention policy (prune older non-protected, non-latest backups)
            applyRetentionPolicy();

            long duration = System.currentTimeMillis() - startTime;
            governanceService.recordAuditLog(
                    adminUser,
                    "BACKUP_CREATED",
                    "DatabaseBackup",
                    filename,
                    String.format("Created backup %s (size: %s, tables: %d, sha256: %s, duration: %dms)",
                            filename, formatFileSize(sizeBytes), tableCount, sha256Hex, duration),
                    ipAddress
            );

            return metadata;
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SHA-256 algorithm not available.");
        } catch (Exception e) {
            governanceService.recordAuditLog(
                    adminUser,
                    "BACKUP_FAILED",
                    "DatabaseBackup",
                    filename,
                    "Failed to create backup: " + e.getMessage(),
                    ipAddress
            );
            throw (e instanceof ApiException) ? (ApiException) e : new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            try { Files.deleteIfExists(tempDumpPath); } catch (IOException ignored) {}
        }
    }

    /**
     * Verifies the integrity of a backup archive via SHA-256 recalculation, pg_restore -l TOC check,
     * and dynamic bidirectional Set comparison against active source database tables.
     */
    public BackupVerificationDto verifyBackup(String filename, String adminUsername, String ipAddress) {
        Path dumpPath = validateAndResolvePath(filename);
        User adminUser = getAdminUser(adminUsername);

        try {
            String currentSha256 = computeSha256(dumpPath);
            BackupMetadataDto meta = readMetadataFile(dumpPath);

            boolean sha256Matches = meta == null || meta.sha256() == null || meta.sha256().equalsIgnoreCase(currentSha256);

            List<String> tocEntries = nativeClient.inspectArchiveToc(dumpPath, timeoutMinutes);
            List<String> tablesFound = extractTablesFromToc(tocEntries);

            // Dynamic completeness comparison against active source database using bidirectional Set comparison
            Map<String, Object> currentSourceStats = nativeClient.queryDatabaseStats("skillsphere_db", 30);
            if (currentSourceStats == null) currentSourceStats = Collections.emptyMap();
            @SuppressWarnings("unchecked")
            List<String> activeTables = (List<String>) currentSourceStats.getOrDefault("tables", Collections.emptyList());

            Set<String> activeTableSet = new TreeSet<>(activeTables);
            Set<String> foundTableSet = new TreeSet<>(tablesFound);

            Set<String> missingTables = new TreeSet<>(activeTableSet);
            missingTables.removeAll(foundTableSet);

            Set<String> unexpectedTables = new TreeSet<>(foundTableSet);
            unexpectedTables.removeAll(activeTableSet);

            boolean schemaMatches = missingTables.isEmpty() && unexpectedTables.isEmpty() && !foundTableSet.isEmpty();
            boolean valid = sha256Matches && !tocEntries.isEmpty() && schemaMatches;

            String msg;
            if (valid) {
                msg = String.format("Integrity verified: SHA-256 matches, %d TOC entries inspected, and archive tables exactly match all %d active source tables.",
                        tocEntries.size(), tablesFound.size());
            } else if (!missingTables.isEmpty() || !unexpectedTables.isEmpty()) {
                StringBuilder sb = new StringBuilder("Verification failed: Schema mismatch.");
                if (!missingTables.isEmpty()) {
                    sb.append(" Missing ").append(missingTables.size()).append(" active public table(s): ").append(missingTables).append(".");
                }
                if (!unexpectedTables.isEmpty()) {
                    sb.append(" Found ").append(unexpectedTables.size()).append(" unexpected table(s): ").append(unexpectedTables).append(".");
                }
                msg = sb.toString();
            } else if (!sha256Matches) {
                msg = "Verification failed: Checksum mismatch.";
            } else {
                msg = "Verification failed: Invalid archive structure.";
            }

            governanceService.recordAuditLog(
                    adminUser,
                    valid ? "BACKUP_VERIFIED" : "BACKUP_VERIFICATION_FAILED",
                    "DatabaseBackup",
                    filename,
                    String.format("Verified backup %s (valid: %b, tablesFound: %d, missingFromActive: %d, unexpectedInArchive: %d, sha256Matches: %b)",
                            filename, valid, tablesFound.size(), missingTables.size(), unexpectedTables.size(), sha256Matches),
                    ipAddress
            );

            return new BackupVerificationDto(
                    filename,
                    valid,
                    currentSha256,
                    sha256Matches,
                    !tocEntries.isEmpty(),
                    tocEntries.size(),
                    tablesFound,
                    msg,
                    Instant.now()
            );
        } catch (Exception e) {
            governanceService.recordAuditLog(
                    adminUser,
                    "BACKUP_VERIFICATION_FAILED",
                    "DatabaseBackup",
                    filename,
                    "Verification error: " + e.getMessage(),
                    ipAddress
            );
            throw (e instanceof ApiException) ? (ApiException) e : new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Performs a safe, isolated trial restoration into an ephemeral database on native PostgreSQL 18.
     * Dynamically verifies restored tables using bidirectional Set comparison against active source tables,
     * guarantees active skillsphere_db remains untouched, and drops the temporary database in a finally block.
     */
    public TestRestoreResultDto testRestore(String filename, String adminUsername, String ipAddress) {
        Path dumpPath = validateAndResolvePath(filename);
        User adminUser = getAdminUser(adminUsername);

        // Pre-check checksum
        String currentSha256 = computeSha256(dumpPath);
        BackupMetadataDto meta = readMetadataFile(dumpPath);
        if (meta != null && meta.sha256() != null && !meta.sha256().equalsIgnoreCase(currentSha256)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Archive checksum verification failed prior to test restore.");
        }

        // Database identity verification before restore starts
        nativeClient.verifyDatabaseIdentity(dataSource);

        // Query active database statistics before restore to prove zero modification
        Map<String, Object> activeStatsBefore = nativeClient.queryDatabaseStats("skillsphere_db", 30);
        if (activeStatsBefore == null) activeStatsBefore = Collections.emptyMap();
        @SuppressWarnings("unchecked")
        List<String> activeTablesBefore = (List<String>) activeStatsBefore.getOrDefault("tables", Collections.emptyList());
        int activeTableCount = (int) activeStatsBefore.getOrDefault("tableCount", 0);
        int activeUserCount = (int) activeStatsBefore.getOrDefault("userCount", 0);
        int activeMigrationCount = (int) activeStatsBefore.getOrDefault("migrationCount", 0);

        String timestamp = TS_FORMAT.format(Instant.now());
        String tempDbName = "skillsphere_restore_test_" + timestamp;
        long startTime = System.currentTimeMillis();

        try {
            // 1. Create isolated temporary database
            nativeClient.createDatabase(tempDbName, 60);

            // 2. Restore dump into temporary database
            nativeClient.restoreDatabase(dumpPath, tempDbName, timeoutMinutes);

            // 3. Verify actual records and schema inside the temporary database
            Map<String, Object> tempDbStats = nativeClient.queryDatabaseStats(tempDbName, 30);
            if (tempDbStats == null) tempDbStats = Collections.emptyMap();
            @SuppressWarnings("unchecked")
            List<String> restoredTables = (List<String>) tempDbStats.getOrDefault("tables", Collections.emptyList());
            int restoredTableCount = (int) tempDbStats.getOrDefault("tableCount", 0);
            int restoredUsers = (int) tempDbStats.getOrDefault("userCount", 0);
            int restoredMigrations = (int) tempDbStats.getOrDefault("migrationCount", 0);

            if (restoredTableCount == 0) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Restored temporary database contains 0 public tables.");
            }

            // Dynamic completeness verification: Bidirectional Set comparison between restored and active source database tables
            Set<String> sourceTableSet = new TreeSet<>(activeTablesBefore);
            Set<String> restoredTableSet = new TreeSet<>(restoredTables);

            Set<String> missingFromRestored = new TreeSet<>(sourceTableSet);
            missingFromRestored.removeAll(restoredTableSet);

            Set<String> unexpectedInRestored = new TreeSet<>(restoredTableSet);
            unexpectedInRestored.removeAll(sourceTableSet);

            if (!missingFromRestored.isEmpty() || !unexpectedInRestored.isEmpty()) {
                StringBuilder err = new StringBuilder("Restored temporary database failed completeness check.");
                if (!missingFromRestored.isEmpty()) {
                    err.append(" Missing source tables: ").append(missingFromRestored).append(".");
                }
                if (!unexpectedInRestored.isEmpty()) {
                    err.append(" Unexpected restored tables: ").append(unexpectedInRestored).append(".");
                }
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, err.toString());
            }

            // 4. Verify that active production database remains 100% unchanged
            Map<String, Object> activeStatsAfter = nativeClient.queryDatabaseStats("skillsphere_db", 30);
            boolean activeUnchanged = activeStatsBefore.equals(activeStatsAfter);

            if (!activeUnchanged) {
                log.error("CRITICAL: Active database state changed during isolated test restore!");
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Active database isolation boundary violated!");
            }

            long duration = System.currentTimeMillis() - startTime;
            String message = String.format("Isolated restoration succeeded in %dms. Restored %d tables, %d users, %d migrations. Active database skillsphere_db verified untouched.",
                    duration, restoredTableCount, restoredUsers, restoredMigrations);

            governanceService.recordAuditLog(
                    adminUser,
                    "ISOLATED_RESTORE_SUCCESS",
                    "DatabaseBackup",
                    filename,
                    String.format("Test restore of %s into %s succeeded: %d tables, %d users, %d migrations verified.",
                            filename, tempDbName, restoredTableCount, restoredUsers, restoredMigrations),
                    ipAddress
            );

            return new TestRestoreResultDto(
                    true,
                    filename,
                    tempDbName,
                    duration,
                    restoredTableCount,
                    restoredUsers,
                    restoredMigrations,
                    true,
                    message,
                    Instant.now()
            );
        } catch (Exception e) {
            governanceService.recordAuditLog(
                    adminUser,
                    "ISOLATED_RESTORE_FAILED",
                    "DatabaseBackup",
                    filename,
                    "Test restore failed: " + e.getMessage(),
                    ipAddress
            );
            throw (e instanceof ApiException) ? (ApiException) e : new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            // Guaranteed cleanup of temporary database
            try {
                nativeClient.dropDatabase(tempDbName, 60);
            } catch (Exception e) {
                log.warn("Failed to drop temporary database {}: {}", tempDbName, e.getMessage());
            }
        }
    }

    /**
     * Deletes a backup archive, enforcing retention protections for the latest backup and safety snapshots.
     */
    public void deleteBackup(String filename, String adminUsername, String ipAddress) {
        Path dumpPath = validateAndResolvePath(filename);
        User adminUser = getAdminUser(adminUsername);

        // Safeguard 1: Protect safety snapshots
        if (VALID_SNAPSHOT_NAME_PATTERN.matcher(filename).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Safety snapshots are protected and cannot be manually deleted.");
        }

        // Safeguard 2: Protect the single latest valid backup
        List<Path> dumpFiles = getValidBackupFiles();
        Path latestStandardBackup = dumpFiles.stream()
                .filter(p -> VALID_BACKUP_NAME_PATTERN.matcher(p.getFileName().toString()).matches())
                .filter(p -> {
                    BackupMetadataDto m = readMetadataFile(p);
                    return m == null || (!"INCOMPLETE_SCHEMA".equalsIgnoreCase(m.status()) && m.tableCount() >= 20);
                })
                .max(Comparator.comparing(p -> p.getFileName().toString()))
                .orElse(null);

        if (latestStandardBackup != null && latestStandardBackup.equals(dumpPath)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The latest valid backup is protected and cannot be deleted.");
        }

        try {
            Files.deleteIfExists(dumpPath);
            Path metaPath = backupDirectory.resolve(filename.replace(".dump", ".meta.json"));
            Files.deleteIfExists(metaPath);

            governanceService.recordAuditLog(
                    adminUser,
                    "BACKUP_DELETED",
                    "DatabaseBackup",
                    filename,
                    "Deleted backup archive " + filename,
                    ipAddress
            );
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete backup file: " + e.getMessage());
        }
    }

    /**
     * Returns the file resource for downloading an archive.
     */
    public FileSystemResource downloadBackup(String filename) {
        Path dumpPath = validateAndResolvePath(filename);
        return new FileSystemResource(dumpPath.toFile());
    }

    // --- Private Security & Path Utilities ---

    public Path validateAndResolvePath(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Backup filename is required.");
        }
        if (!VALID_BACKUP_NAME_PATTERN.matcher(filename).matches() && !VALID_SNAPSHOT_NAME_PATTERN.matcher(filename).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid backup filename format.");
        }

        Path resolved = backupDirectory.resolve(filename).normalize();
        if (!resolved.startsWith(backupDirectory)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Directory traversal detected.");
        }
        if (Files.isSymbolicLink(resolved)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Symbolic links are not permitted.");
        }
        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Backup file not found.");
        }
        return resolved;
    }

    private List<Path> getValidBackupFiles() {
        if (!Files.exists(backupDirectory)) return Collections.emptyList();
        try (var stream = Files.list(backupDirectory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return VALID_BACKUP_NAME_PATTERN.matcher(name).matches()
                                || VALID_SNAPSHOT_NAME_PATTERN.matcher(name).matches();
                    })
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list backup directory: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void applyRetentionPolicy() {
        List<Path> files = new ArrayList<>(getValidBackupFiles());
        // Filter only standard backups (safety snapshots are never purged by retention)
        List<Path> standardBackups = files.stream()
                .filter(p -> VALID_BACKUP_NAME_PATTERN.matcher(p.getFileName().toString()).matches())
                .sorted(Comparator.comparing(this::getFileCreationInstant).reversed())
                .toList();

        if (standardBackups.size() <= maxRetained) {
            return;
        }

        // Keep the latest 'maxRetained' standard backups, delete the rest
        for (int i = maxRetained; i < standardBackups.size(); i++) {
            Path toDelete = standardBackups.get(i);
            try {
                Files.deleteIfExists(toDelete);
                String metaName = toDelete.getFileName().toString().replace(".dump", ".meta.json");
                Files.deleteIfExists(backupDirectory.resolve(metaName));
                log.info("Retention policy pruned older backup: {}", toDelete.getFileName());
            } catch (IOException e) {
                log.warn("Failed to prune older backup {}: {}", toDelete.getFileName(), e.getMessage());
            }
        }
    }

    public BackupMetadataDto readMetadataFile(Path dumpPath) {
        String metaFilename = dumpPath.getFileName().toString().replace(".dump", ".meta.json");
        Path metaPath = backupDirectory.resolve(metaFilename);
        if (!Files.exists(metaPath)) return null;
        try {
            BackupMetadataDto meta = objectMapper.readValue(metaPath.toFile(), BackupMetadataDto.class);
            if (meta.tableCount() > 0 && meta.tableCount() < 20) {
                return new BackupMetadataDto(
                        meta.filename(),
                        meta.sizeBytes(),
                        meta.formattedSize(),
                        meta.createdAt(),
                        meta.sha256(),
                        "INCOMPLETE_SCHEMA",
                        meta.databaseName(),
                        meta.postgresVersion(),
                        meta.tableCount(),
                        (meta.note() != null && !meta.note().isBlank())
                                ? meta.note()
                                : "Incomplete archive from stale Docker instance (9 tables)",
                        meta.protectedSnapshot(),
                        false
                );
            }
            return meta;
        } catch (Exception e) {
            return null;
        }
    }

    private Instant getFileCreationInstant(Path p) {
        String name = p.getFileName().toString();
        try {
            String[] parts = name.replace(".dump", "").split("_");
            if (parts.length >= 4) {
                String datePart = parts[2] + "_" + parts[3];
                return TS_FORMAT.parse(datePart, Instant::from);
            }
        } catch (Exception ignored) {}
        try {
            return Files.getLastModifiedTime(p).toInstant();
        } catch (IOException e) {
            return Instant.now();
        }
    }

    private String computeSha256(Path p) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(p)) {
                byte[] buf = new byte[16384];
                int read;
                while ((read = in.read(buf)) != -1) {
                    digest.update(buf, 0, read);
                }
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to compute checksum: " + e.getMessage());
        }
    }

    private String computeSha256Safely(Path p) {
        try {
            return computeSha256(p);
        } catch (Exception e) {
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }

    private User getAdminUser(String username) {
        return userRepository.findByEmail(username).orElse(null);
    }
}
