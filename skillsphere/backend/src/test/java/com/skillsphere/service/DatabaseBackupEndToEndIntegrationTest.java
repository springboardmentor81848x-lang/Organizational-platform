package com.skillsphere.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsphere.dto.BackupDtos.*;
import com.skillsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseBackupEndToEndIntegrationTest {

    @Autowired
    private PostgresNativeClient nativeClient;

    @Autowired
    private AdminGovernanceService governanceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataSource dataSource;

    private DatabaseBackupService backupService;

    @BeforeEach
    void setUp() {
        backupService = new DatabaseBackupService(
                "backups",
                15,
                5,
                nativeClient,
                governanceService,
                userRepository,
                objectMapper,
                dataSource
        );
    }

    @Test
    @DisplayName("End-to-End Live Execution: Native PostgreSQL 18 Backup (46 Tables, V1-V5), SHA-256 Verification & Isolated Restore")
    void testLiveEndToEndBackupAndIsolatedRestore() {
        // Step 1: Verify database identity and pre-execution state of active skillsphere_db
        assertDoesNotThrow(() -> nativeClient.verifyDatabaseIdentity(dataSource),
                "Database identity check must succeed against live PostgreSQL 18 instance.");

        Map<String, Object> statsBefore = nativeClient.queryDatabaseStats("skillsphere_db", 30);
        int tablesBefore = (int) statsBefore.get("tableCount");
        int usersBefore = (int) statsBefore.get("userCount");
        int migrationsBefore = (int) statsBefore.get("migrationCount");

        System.out.println("=== PRE-EXECUTION ACTIVE DATABASE (skillsphere_db) ===");
        System.out.println("Public Tables: " + tablesBefore);
        System.out.println("Users: " + usersBefore);
        System.out.println("Migrations: " + migrationsBefore);
        System.out.println("Representative record counts: " + statsBefore.get("representativeRecordCounts"));

        assertEquals(47, tablesBefore, "Live SkillSphere database must have all 47 public tables.");
        assertEquals(6, migrationsBefore, "Live SkillSphere database must have all 6 Flyway migrations.");
        assertTrue(usersBefore >= 1, "Live database must contain users.");

        // Step 2: Trigger real backup
        System.out.println("\n=== STEP 1: EXECUTING LIVE NATIVE POSTGRESQL 18 BACKUP CREATION ===");
        BackupMetadataDto created = backupService.createBackup(
                "admin@skillsphere.com",
                "Phase 4 Native PostgreSQL 18 Verified Backup (47 tables)",
                "127.0.0.1"
        );

        assertNotNull(created);
        System.out.println("Generated Archive: " + created.filename());
        System.out.println("Archive Size: " + created.formattedSize() + " (" + created.sizeBytes() + " bytes)");
        System.out.println("SHA-256 Checksum: " + created.sha256());
        System.out.println("Postgres Version: " + created.postgresVersion());
        System.out.println("Tables Dumped: " + created.tableCount());

        assertTrue(created.sizeBytes() > 50000, "Dump of source tables should be > 50KB.");
        assertEquals(tablesBefore, created.tableCount(), "Created backup must contain all source tables.");
        assertNotNull(created.sha256());
        assertEquals("SUCCESS", created.status());
        assertTrue(created.latestBackup(), "Newly created valid backup must be flagged as latest.");

        Path dumpPath = Paths.get("backups", created.filename());
        assertTrue(dumpPath.toFile().exists(), "Dump file must physically exist in backups directory.");

        // Step 3: Trigger real verification
        System.out.println("\n=== STEP 2: EXECUTING LIVE ARCHIVE INTEGRITY VERIFICATION ===");
        BackupVerificationDto verified = backupService.verifyBackup(
                created.filename(),
                "admin@skillsphere.com",
                "127.0.0.1"
        );

        assertNotNull(verified);
        System.out.println("SHA-256 Match: " + verified.sha256Matches());
        System.out.println("TOC Valid: " + verified.tocValid() + " (" + verified.tocEntryCount() + " entries)");
        System.out.println("Tables Found: " + verified.tablesFound().size() + " tables");

        assertTrue(verified.valid());
        assertTrue(verified.sha256Matches());
        assertTrue(verified.tocValid());
        assertEquals(tablesBefore, verified.tablesFound().size(), "TOC must contain all tables.");
        assertTrue(verified.tablesFound().contains("users"));
        assertTrue(verified.tablesFound().contains("flyway_schema_history"));
        assertTrue(verified.tablesFound().contains("audit_logs"));
        assertTrue(verified.tablesFound().contains("skills"));
        assertTrue(verified.tablesFound().contains("conversations"));
        assertTrue(verified.tablesFound().contains("skill_assessments"));
        assertTrue(verified.tablesFound().contains("assessment_attempts"));

        // Step 4: Trigger real isolated sandbox restore
        System.out.println("\n=== STEP 3: EXECUTING LIVE ISOLATED SANDBOX RESTORE ===");
        TestRestoreResultDto restoreResult = backupService.testRestore(
                created.filename(),
                "admin@skillsphere.com",
                "127.0.0.1"
        );

        assertNotNull(restoreResult);
        System.out.println("Isolated Restore Success: " + restoreResult.success());
        System.out.println("Sandbox Database: " + restoreResult.tempDatabaseName());
        System.out.println("Duration: " + restoreResult.durationMs() + "ms");
        System.out.println("Restored Tables: " + restoreResult.restoredTableCount());
        System.out.println("Verified Users: " + restoreResult.verifiedUserCount());
        System.out.println("Verified Migrations: " + restoreResult.verifiedMigrationCount());
        System.out.println("Active DB Unchanged: " + restoreResult.activeDatabaseUnchanged());

        assertTrue(restoreResult.success());
        assertTrue(restoreResult.activeDatabaseUnchanged());
        assertEquals(tablesBefore, restoreResult.restoredTableCount(), "Restored tables must match all source tables.");
        assertEquals(usersBefore, restoreResult.verifiedUserCount(), "Restored users must match source users.");
        assertEquals(migrationsBefore, restoreResult.verifiedMigrationCount(), "Restored migrations must match Flyway migrations.");

        // Step 5: Confirm active production database skillsphere_db schema and data integrity
        Map<String, Object> statsAfter = nativeClient.queryDatabaseStats("skillsphere_db", 30);
        assertEquals(tablesBefore, (int) statsAfter.get("tableCount"), "Table count in skillsphere_db must not change.");
        assertEquals(usersBefore, (int) statsAfter.get("userCount"), "User count in skillsphere_db must not change.");
        assertEquals(migrationsBefore, (int) statsAfter.get("migrationCount"), "Migration count in skillsphere_db must not change.");
        assertEquals(statsBefore.get("tables"), statsAfter.get("tables"), "List of tables in skillsphere_db must remain identical.");

        System.out.println("\n=== POST-EXECUTION ACTIVE DATABASE VERIFICATION ===");
        System.out.println("skillsphere_db tables before: " + tablesBefore + ", after: " + statsAfter.get("tableCount"));
        System.out.println("skillsphere_db users before: " + usersBefore + ", after: " + statsAfter.get("userCount"));
        System.out.println("skillsphere_db migrations before: " + migrationsBefore + ", after: " + statsAfter.get("migrationCount"));
        System.out.println("CONFIRMED: Active database skillsphere_db was NOT modified by isolated restore.");
    }
}
