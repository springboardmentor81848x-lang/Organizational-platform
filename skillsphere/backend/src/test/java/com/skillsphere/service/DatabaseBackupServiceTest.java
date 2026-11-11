package com.skillsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.dto.BackupDtos.*;
import com.skillsphere.entity.Role;
import com.skillsphere.entity.User;
import com.skillsphere.exception.ApiException;
import com.skillsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DatabaseBackupServiceTest {

    private PostgresNativeClient nativeClient;
    private AdminGovernanceService governanceService;
    private UserRepository userRepository;
    private ObjectMapper objectMapper;
    private DataSource dataSource;
    private DatabaseBackupService backupService;

    @TempDir
    Path tempBackupDir;

    private User adminUser;
    private List<String> defaultTestTables;

    @BeforeEach
    void setUp() {
        nativeClient = mock(PostgresNativeClient.class);
        governanceService = mock(AdminGovernanceService.class);
        userRepository = mock(UserRepository.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        dataSource = mock(DataSource.class);

        adminUser = new User();
        adminUser.setEmail("admin@skillsphere.com");
        adminUser.setPasswordHash("hash");
        adminUser.setFullName("Admin User");
        adminUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@skillsphere.com")).thenReturn(Optional.of(adminUser));

        defaultTestTables = new ArrayList<>();
        for (int i = 1; i <= 46; i++) {
            defaultTestTables.add("table_" + i);
        }

        when(nativeClient.queryDatabaseStats(eq("skillsphere_db"), anyLong()))
                .thenReturn(Map.of(
                        "tableCount", 46,
                        "tables", defaultTestTables,
                        "userCount", 2,
                        "migrationCount", 5
                ));

        backupService = new DatabaseBackupService(
                tempBackupDir.toString(),
                3, // maxRetained = 3
                5,
                nativeClient,
                governanceService,
                userRepository,
                objectMapper,
                dataSource
        );
    }

    @Test
    @DisplayName("Create backup dynamically checks active source tables, dumps 46 tables and creates metadata")
    void testCreateBackup_Success_CreatesArchiveAndMetadata() throws IOException {
        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(1);
            out.write("PGDUMP_NATIVE_18_BINARY_DATA".getBytes());
            return null;
        }).when(nativeClient).dumpDatabase(eq("skillsphere_db"), any(OutputStream.class), anyLong());

        List<String> tocEntries = new ArrayList<>();
        for (int i = 1; i <= 46; i++) {
            tocEntries.add(i + "; 1259 " + (16400 + i) + " TABLE public table_" + i + " postgres");
        }
        when(nativeClient.inspectArchiveToc(any(Path.class), anyLong())).thenReturn(tocEntries);
        when(nativeClient.getPostgresServerVersion(anyLong())).thenReturn("PostgreSQL 18.4 on x86_64-pc-windows-msvc");

        BackupMetadataDto result = backupService.createBackup("admin@skillsphere.com", "Test native backup", "127.0.0.1");

        assertNotNull(result);
        assertTrue(result.filename().startsWith("skillsphere_backup_"));
        assertTrue(result.filename().endsWith(".dump"));
        assertTrue(result.sizeBytes() > 0);
        assertEquals(46, result.tableCount());
        assertEquals("PostgreSQL 18.4 on x86_64-pc-windows-msvc", result.postgresVersion());

        Path dumpPath = tempBackupDir.resolve(result.filename());
        assertTrue(Files.exists(dumpPath), "Final dump file must exist.");

        Path metaPath = tempBackupDir.resolve(result.filename().replace(".dump", ".meta.json"));
        assertTrue(Files.exists(metaPath), "Metadata file must exist.");

        verify(nativeClient, times(1)).verifyDatabaseIdentity(any());
        verify(governanceService, times(1)).recordAuditLog(
                eq(adminUser),
                eq("BACKUP_CREATED"),
                eq("DatabaseBackup"),
                eq(result.filename()),
                anyString(),
                eq("127.0.0.1")
        );
    }

    @Test
    @DisplayName("Create backup fails if archive TOC is missing any active source tables")
    void testCreateBackup_FailsWhenDumpMissingSourceTables() throws IOException {
        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(1);
            out.write("PGDUMP_INCOMPLETE_BINARY".getBytes());
            return null;
        }).when(nativeClient).dumpDatabase(eq("skillsphere_db"), any(OutputStream.class), anyLong());

        // TOC only has 45 tables, missing table_46
        List<String> incompleteToc = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            incompleteToc.add(i + "; 1259 " + (16400 + i) + " TABLE public table_" + i + " postgres");
        }
        when(nativeClient.inspectArchiveToc(any(Path.class), anyLong())).thenReturn(incompleteToc);

        ApiException ex = assertThrows(ApiException.class, () ->
                backupService.createBackup("admin@skillsphere.com", "Incomplete dump test", "127.0.0.1"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertTrue(ex.getMessage().contains("Missing active source tables in TOC"));
        assertTrue(ex.getMessage().contains("table_46"));
    }

    @Test
    @DisplayName("Create backup fails if archive TOC contains unexpected tables")
    void testCreateBackup_FailsWhenDumpHasUnexpectedTables() throws IOException {
        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(1);
            out.write("PGDUMP_EXTRA_BINARY".getBytes());
            return null;
        }).when(nativeClient).dumpDatabase(eq("skillsphere_db"), any(OutputStream.class), anyLong());

        List<String> extraToc = new ArrayList<>();
        for (int i = 1; i <= 46; i++) {
            extraToc.add(i + "; 1259 " + (16400 + i) + " TABLE public table_" + i + " postgres");
        }
        extraToc.add("47; 1259 16447 TABLE public unexpected_table postgres");
        when(nativeClient.inspectArchiveToc(any(Path.class), anyLong())).thenReturn(extraToc);

        ApiException ex = assertThrows(ApiException.class, () ->
                backupService.createBackup("admin@skillsphere.com", "Extra table dump test", "127.0.0.1"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertTrue(ex.getMessage().contains("Unexpected tables in TOC"));
        assertTrue(ex.getMessage().contains("unexpected_table"));
    }

    @Test
    @DisplayName("Create backup fails and cleans up temp files when dump process fails")
    void testCreateBackup_Failure_CleansUpTempFile() {
        doThrow(new RuntimeException("PostgreSQL 18 process failed"))
                .when(nativeClient).dumpDatabase(anyString(), any(OutputStream.class), anyLong());

        assertThrows(ApiException.class, () ->
                backupService.createBackup("admin@skillsphere.com", "Fail test", "127.0.0.1"));

        try (var stream = Files.list(tempBackupDir)) {
            assertEquals(0, stream.count(), "No incomplete files should remain.");
        } catch (IOException e) {
            fail(e);
        }

        verify(governanceService, times(1)).recordAuditLog(
                eq(adminUser),
                eq("BACKUP_FAILED"),
                eq("DatabaseBackup"),
                anyString(),
                contains("PostgreSQL 18 process failed"),
                eq("127.0.0.1")
        );
    }

    @Test
    @DisplayName("Directory traversal attempts are rejected")
    void testPathTraversal_RejectsMaliciousFilenames() {
        ApiException ex1 = assertThrows(ApiException.class, () ->
                backupService.validateAndResolvePath("../../etc/passwd"));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());

        ApiException ex2 = assertThrows(ApiException.class, () ->
                backupService.validateAndResolvePath("skillsphere_backup_20261106_120000.dump/../other.dump"));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatus());

        ApiException ex3 = assertThrows(ApiException.class, () ->
                backupService.validateAndResolvePath("arbitrary.txt"));
        assertEquals(HttpStatus.BAD_REQUEST, ex3.getStatus());
    }

    @Test
    @DisplayName("Verify backup validates SHA-256 and TOC successfully when all active source tables match exactly")
    void testVerifyBackup_Success_ValidatesChecksumAndToc() throws IOException {
        String filename = "skillsphere_backup_20261106_120000.dump";
        Path dumpFile = tempBackupDir.resolve(filename);
        Files.writeString(dumpFile, "TEST_DUMP_PAYLOAD");

        when(nativeClient.queryDatabaseStats(eq("skillsphere_db"), anyLong()))
                .thenReturn(Map.of("tableCount", 2, "tables", List.of("users", "roles"), "userCount", 2, "migrationCount", 1));

        when(nativeClient.inspectArchiveToc(any(Path.class), anyLong()))
                .thenReturn(List.of("1; 1259 16399 TABLE public users postgres", "2; 1259 16400 TABLE public roles postgres"));

        BackupVerificationDto verification = backupService.verifyBackup(filename, "admin@skillsphere.com", "127.0.0.1");

        assertNotNull(verification);
        assertTrue(verification.valid());
        assertTrue(verification.sha256Matches());
        assertEquals(2, verification.tablesFound().size());
        assertTrue(verification.tablesFound().contains("users"));
        assertTrue(verification.tablesFound().contains("roles"));

        verify(governanceService, times(1)).recordAuditLog(
                eq(adminUser),
                eq("BACKUP_VERIFIED"),
                eq("DatabaseBackup"),
                eq(filename),
                anyString(),
                eq("127.0.0.1")
        );
    }

    @Test
    @DisplayName("Verify backup marks valid=false when archive is missing active source tables")
    void testVerifyBackup_FailsWhenArchiveMissingSourceTables() throws IOException {
        String filename = "skillsphere_backup_20261106_120000.dump";
        Path dumpFile = tempBackupDir.resolve(filename);
        Files.writeString(dumpFile, "TEST_DUMP_PAYLOAD");

        // Active DB has users, roles, audit_logs
        when(nativeClient.queryDatabaseStats(eq("skillsphere_db"), anyLong()))
                .thenReturn(Map.of("tableCount", 3, "tables", List.of("users", "roles", "audit_logs"), "userCount", 2, "migrationCount", 1));

        // Archive only has users, roles (missing audit_logs)
        when(nativeClient.inspectArchiveToc(any(Path.class), anyLong()))
                .thenReturn(List.of("1; 1259 16399 TABLE public users postgres", "2; 1259 16400 TABLE public roles postgres"));

        BackupVerificationDto verification = backupService.verifyBackup(filename, "admin@skillsphere.com", "127.0.0.1");

        assertNotNull(verification);
        assertFalse(verification.valid(), "Verification must fail when active source tables are missing from archive");
        assertTrue(verification.message().contains("Missing 1 active public table(s): [audit_logs]"));

        verify(governanceService, times(1)).recordAuditLog(
                eq(adminUser),
                eq("BACKUP_VERIFICATION_FAILED"),
                eq("DatabaseBackup"),
                eq(filename),
                anyString(),
                eq("127.0.0.1")
        );
    }

    @Test
    @DisplayName("Verify backup marks valid=false when archive contains unexpected tables")
    void testVerifyBackup_FailsWhenArchiveHasUnexpectedTables() throws IOException {
        String filename = "skillsphere_backup_20261106_120000.dump";
        Path dumpFile = tempBackupDir.resolve(filename);
        Files.writeString(dumpFile, "TEST_DUMP_PAYLOAD");

        // Active DB has users, roles
        when(nativeClient.queryDatabaseStats(eq("skillsphere_db"), anyLong()))
                .thenReturn(Map.of("tableCount", 2, "tables", List.of("users", "roles"), "userCount", 2, "migrationCount", 1));

        // Archive has users, roles, unexpected_table
        when(nativeClient.inspectArchiveToc(any(Path.class), anyLong()))
                .thenReturn(List.of("1; 1259 16399 TABLE public users postgres", "2; 1259 16400 TABLE public roles postgres", "3; 1259 16401 TABLE public unexpected_table postgres"));

        BackupVerificationDto verification = backupService.verifyBackup(filename, "admin@skillsphere.com", "127.0.0.1");

        assertNotNull(verification);
        assertFalse(verification.valid(), "Verification must fail when unexpected tables exist in archive");
        assertTrue(verification.message().contains("Found 1 unexpected table(s): [unexpected_table]"));
    }

    @Test
    @DisplayName("Isolated test restore dynamically verifies all restored tables match source tables and leaves active database untouched")
    void testTestRestore_IsolatedExecution_GuaranteesActiveDatabaseUnchanged() throws IOException {
        String filename = "skillsphere_backup_20261106_120000.dump";
        Path dumpFile = tempBackupDir.resolve(filename);
        Files.writeString(dumpFile, "TEST_DUMP_PAYLOAD");

        Map<String, Object> activeStatsBefore = Map.of(
                "tableCount", 46,
                "tables", defaultTestTables,
                "userCount", 2,
                "migrationCount", 5
        );
        Map<String, Object> activeStatsAfter = Map.of(
                "tableCount", 46,
                "tables", defaultTestTables,
                "userCount", 2,
                "migrationCount", 5
        );
        Map<String, Object> tempDbStats = Map.of(
                "tableCount", 46,
                "tables", defaultTestTables,
                "userCount", 2,
                "migrationCount", 5
        );

        when(nativeClient.queryDatabaseStats(eq("skillsphere_db"), anyLong()))
                .thenReturn(activeStatsBefore, activeStatsAfter);
        when(nativeClient.queryDatabaseStats(startsWith("skillsphere_restore_test_"), anyLong()))
                .thenReturn(tempDbStats);

        TestRestoreResultDto result = backupService.testRestore(filename, "admin@skillsphere.com", "127.0.0.1");

        assertNotNull(result);
        assertTrue(result.success());
        assertTrue(result.activeDatabaseUnchanged());
        assertEquals(46, result.restoredTableCount());
        assertEquals(2, result.verifiedUserCount());
        assertEquals(5, result.verifiedMigrationCount());

        verify(nativeClient, times(1)).createDatabase(startsWith("skillsphere_restore_test_"), anyLong());
        verify(nativeClient, times(1)).restoreDatabase(eq(dumpFile), startsWith("skillsphere_restore_test_"), anyLong());
        verify(nativeClient, times(1)).dropDatabase(startsWith("skillsphere_restore_test_"), anyLong());

        verify(governanceService, times(1)).recordAuditLog(
                eq(adminUser),
                eq("ISOLATED_RESTORE_SUCCESS"),
                eq("DatabaseBackup"),
                eq(filename),
                anyString(),
                eq("127.0.0.1")
        );
    }

    @Test
    @DisplayName("Isolated test restore fails if restored database contains unexpected tables")
    void testTestRestore_FailsWhenRestoredHasUnexpectedTables() throws IOException {
        String filename = "skillsphere_backup_20261106_120000.dump";
        Path dumpFile = tempBackupDir.resolve(filename);
        Files.writeString(dumpFile, "TEST_DUMP_PAYLOAD");

        Map<String, Object> activeStatsBefore = Map.of(
                "tableCount", 46,
                "tables", defaultTestTables,
                "userCount", 2,
                "migrationCount", 5
        );
        List<String> extraTables = new ArrayList<>(defaultTestTables);
        extraTables.add("unexpected_restored_table");

        Map<String, Object> tempDbStats = Map.of(
                "tableCount", 47,
                "tables", extraTables,
                "userCount", 2,
                "migrationCount", 5
        );

        when(nativeClient.queryDatabaseStats(eq("skillsphere_db"), anyLong()))
                .thenReturn(activeStatsBefore);
        when(nativeClient.queryDatabaseStats(startsWith("skillsphere_restore_test_"), anyLong()))
                .thenReturn(tempDbStats);

        ApiException ex = assertThrows(ApiException.class, () ->
                backupService.testRestore(filename, "admin@skillsphere.com", "127.0.0.1"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        assertTrue(ex.getMessage().contains("Unexpected restored tables"));
        assertTrue(ex.getMessage().contains("unexpected_restored_table"));
    }

    @Test
    @DisplayName("Delete backup protects latest valid backup and safety snapshots")
    void testDeleteBackup_ProtectsLatestBackupAndSafetySnapshots() throws IOException {
        String backup1 = "skillsphere_backup_20261106_100000.dump";
        String backup2 = "skillsphere_backup_20261106_110000.dump";
        String snapshot = "skillsphere_snapshot_20261106_090000.dump";

        Files.writeString(tempBackupDir.resolve(backup1), "payload 1");
        Files.writeString(tempBackupDir.resolve(backup2), "payload 2");
        Files.writeString(tempBackupDir.resolve(snapshot), "snapshot payload");

        ApiException exSnapshot = assertThrows(ApiException.class, () ->
                backupService.deleteBackup(snapshot, "admin@skillsphere.com", "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, exSnapshot.getStatus());
        assertTrue(exSnapshot.getMessage().contains("Safety snapshots are protected"));

        ApiException exLatest = assertThrows(ApiException.class, () ->
                backupService.deleteBackup(backup2, "admin@skillsphere.com", "127.0.0.1"));
        assertEquals(HttpStatus.BAD_REQUEST, exLatest.getStatus());
        assertTrue(exLatest.getMessage().contains("latest valid backup is protected"));

        assertDoesNotThrow(() -> backupService.deleteBackup(backup1, "admin@skillsphere.com", "127.0.0.1"));
        assertFalse(Files.exists(tempBackupDir.resolve(backup1)));
    }

    @Test
    @DisplayName("List backups flags archives with incomplete schema (tableCount < 20)")
    void testListBackups_FlagsIncompleteSchema() throws IOException {
        String oldBackup = "skillsphere_backup_20261106_111017.dump";
        Files.writeString(tempBackupDir.resolve(oldBackup), "payload old");

        BackupMetadataDto oldMeta = new BackupMetadataDto(
                oldBackup,
                35507,
                "34.7 KB",
                Instant.now(),
                "sha256old",
                "SUCCESS",
                "skillsphere_db",
                "PostgreSQL 16.15",
                9,
                "Phase 3 End-to-End Live Evaluation Backup",
                false,
                true
        );
        objectMapper.writeValue(tempBackupDir.resolve("skillsphere_backup_20261106_111017.meta.json").toFile(), oldMeta);

        List<BackupMetadataDto> backups = backupService.listBackups();
        assertEquals(1, backups.size());
        BackupMetadataDto dto = backups.get(0);
        assertEquals("INCOMPLETE_SCHEMA", dto.status());
        assertEquals(9, dto.tableCount());
        assertFalse(dto.latestBackup(), "Incomplete backup must not be marked as latest");
    }
}
