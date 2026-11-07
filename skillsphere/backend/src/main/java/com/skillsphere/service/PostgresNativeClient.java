package com.skillsphere.service;

import com.skillsphere.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Native PostgreSQL client adapter for executing PostgreSQL 18 client utilities on Windows/Linux.
 * Targets the exact PostgreSQL 18 instance configured in Spring Boot datasource.
 * Prevents command injection by using fixed tokenized argument arrays and passing credentials
 * via the child process environment (PGPASSWORD).
 *
 * <p>Security & Credential Handling Considerations:
 * <ul>
 *   <li>Database credentials are not included in command-line arguments, process invocation logs,
 *       application logs, exception messages, or backup metadata companion files.</li>
 *   <li>Credentials are supplied exclusively via the {@code PGPASSWORD} environment variable
 *       in the child process's environment map.</li>
 *   <li>Security Boundary Note: While this avoids command-line observation (such as in standard process lists),
 *       privileged processes may potentially inspect process environment variables and memory paging cannot be ruled out.</li>
 * </ul>
 */
@Component
public class PostgresNativeClient {

    private static final Logger log = LoggerFactory.getLogger(PostgresNativeClient.class);
    private static final Pattern SAFE_DB_NAME = Pattern.compile("^[a-zA-Z0-9_]{1,63}$");
    private static final Pattern JDBC_PATTERN = Pattern.compile("^jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+)");

    private final String postgresBinPath;
    private final String dbHost;
    private final int dbPort;
    private final String dbName;
    private final String dbUser;
    private final String dbPassword;

    public record HostPortDb(String host, int port, String database) {}

    public PostgresNativeClient(
            @Value("${app.backup.postgres-bin-path:D:\\PostgreSQL\\18\\bin}") String postgresBinPath,
            @Value("${spring.datasource.url:jdbc:postgresql://127.0.0.1:5432/skillsphere_db}") String datasourceUrl,
            @Value("${spring.datasource.username:postgres}") String dbUser,
            @Value("${spring.datasource.password:}") String dbPassword) {
        this.postgresBinPath = postgresBinPath;
        HostPortDb parsed = parseJdbcUrl(datasourceUrl);
        this.dbHost = parsed.host();
        this.dbPort = parsed.port();
        this.dbName = parsed.database();
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public static HostPortDb parseJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return new HostPortDb("127.0.0.1", 5432, "skillsphere_db");
        }
        Matcher matcher = JDBC_PATTERN.matcher(url.trim());
        if (matcher.find()) {
            String host = matcher.group(1);
            if ("localhost".equalsIgnoreCase(host)) {
                host = "127.0.0.1";
            }
            int port = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 5432;
            String db = matcher.group(3);
            return new HostPortDb(host, port, db);
        }
        return new HostPortDb("127.0.0.1", 5432, "skillsphere_db");
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getPostgresBinPath() {
        return postgresBinPath;
    }

    /**
     * Resolves the full executable path for the given PostgreSQL utility.
     */
    public String resolveBinary(String utilityName) {
        Path binDir = Paths.get(postgresBinPath);
        Path exePath = binDir.resolve(utilityName + ".exe");
        if (Files.exists(exePath)) {
            return exePath.toAbsolutePath().toString();
        }
        Path directPath = binDir.resolve(utilityName);
        if (Files.exists(directPath)) {
            return directPath.toAbsolutePath().toString();
        }
        // Fallback to name if in PATH
        return utilityName;
    }

    /**
     * Verifies target database identity against Spring Boot DataSource and PostgreSQL 18 client tools.
     * Ensures we are connected to the true PostgreSQL 18 platform instance with full schema.
     */
    public void verifyDatabaseIdentity(DataSource dataSource) {
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection()) {
                String prodName = conn.getMetaData().getDatabaseProductName();
                String prodVersion = conn.getMetaData().getDatabaseProductVersion();
                int majorVersion = conn.getMetaData().getDatabaseMajorVersion();

                log.info("Verifying database identity via JDBC: {} version {}", prodName, prodVersion);
                if (prodName == null || !prodName.toLowerCase().contains("postgresql")) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Database identity verification failed: Connected to " + prodName + ", expected PostgreSQL.");
                }
                if (majorVersion < 18) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Database identity verification failed: Connected to PostgreSQL major version " + majorVersion +
                                    ", but SkillSphere requires PostgreSQL 18+.");
                }

                try (Statement stmt = conn.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery("SELECT current_database();")) {
                        if (rs.next()) {
                            String currentDb = rs.getString(1);
                            if (!this.dbName.equalsIgnoreCase(currentDb)) {
                                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Database identity verification failed: Expected database '" + this.dbName + "', found '" + currentDb + "'.");
                            }
                        }
                    }

                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")) {
                        if (rs.next()) {
                            int count = rs.getInt(1);
                            if (count == 0) {
                                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Database identity verification failed: Database contains 0 public tables.");
                            }
                        }
                    }

                    try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success = true;")) {
                        if (rs.next()) {
                            int migrations = rs.getInt(1);
                            if (migrations < 1) {
                                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Database identity verification failed: No applied Flyway migrations found in flyway_schema_history.");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (e instanceof ApiException) throw (ApiException) e;
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Database identity verification failed: " + e.getMessage());
            }
        }

        // Verify native client binary compatibility
        String clientVersion = getPostgresClientVersion(10);
        log.info("Verified PostgreSQL client binaries: {}", clientVersion);
        if (!clientVersion.contains("18")) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "PostgreSQL client binary version mismatch: " + clientVersion + " (expected PostgreSQL 18.x tools).");
        }
    }

    /**
     * Dumps the specified database using native pg_dump.exe in custom format (-Fc).
     * Output is streamed directly to targetStream.
     */
    public void dumpDatabase(String dbName, OutputStream targetStream, long timeoutMinutes) {
        validateDbName(dbName);
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("pg_dump"),
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "-Fc",
                dbName
        );

        executeStreamingProcess(pb, null, targetStream, timeoutMinutes, "pg_dump");
    }

    /**
     * Inspects the Table of Contents (TOC) of a dump file using pg_restore -l <dumpFile>.
     */
    public List<String> inspectArchiveToc(Path dumpFile, long timeoutMinutes) {
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("pg_restore"),
                "-l",
                dumpFile.toAbsolutePath().toString()
        );

        String stdout = executeCommand(pb, timeoutMinutes * 60, "pg_restore -l " + dumpFile.getFileName());
        return parseTocLines(stdout);
    }

    /**
     * Inspects TOC from an InputStream (useful for streaming checks or mocks).
     */
    public List<String> inspectArchiveToc(InputStream archiveStream, long timeoutMinutes) {
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("pg_restore"),
                "-l"
        );

        ByteArrayOutputStream capturedStdout = new ByteArrayOutputStream();
        executeStreamingProcess(pb, archiveStream, capturedStdout, timeoutMinutes, "pg_restore -l");
        return parseTocLines(capturedStdout.toString(StandardCharsets.UTF_8));
    }

    private List<String> parseTocLines(String tocContent) {
        List<String> lines = new ArrayList<>();
        for (String line : tocContent.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith(";")) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    /**
     * Creates a new temporary database on native PostgreSQL 18.
     */
    public void createDatabase(String dbName, long timeoutSeconds) {
        validateDbName(dbName);
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("createdb"),
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                dbName
        );
        executeCommand(pb, timeoutSeconds, "createdb " + dbName);
    }

    /**
     * Restores a dump file into the target database using native pg_restore.exe.
     */
    public void restoreDatabase(Path dumpFile, String targetDbName, long timeoutMinutes) {
        validateDbName(targetDbName);
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("pg_restore"),
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "-d", targetDbName,
                "--no-owner",
                "--no-privileges",
                dumpFile.toAbsolutePath().toString()
        );
        // pg_restore exit code 0 = ok, 1 = non-critical warnings (e.g. privilege grants), >1 = critical errors
        executeCommandWithWarningTolerance(pb, timeoutMinutes * 60, "pg_restore " + dumpFile.getFileName());
    }

    /**
     * Restores an archive stream into the target database.
     */
    public void restoreDatabase(InputStream archiveStream, String targetDbName, long timeoutMinutes) {
        validateDbName(targetDbName);
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("pg_restore"),
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "-d", targetDbName,
                "--no-owner",
                "--no-privileges"
        );
        executeRestoreStreamingProcess(pb, archiveStream, timeoutMinutes, "pg_restore");
    }

    /**
     * Drops a database if it exists using native dropdb.exe.
     */
    public void dropDatabase(String dbName, long timeoutSeconds) {
        validateDbName(dbName);
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("dropdb"),
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "--if-exists",
                dbName
        );
        executeCommand(pb, timeoutSeconds, "dropdb " + dbName);
    }

    /**
     * Queries comprehensive database statistics: public tables, users count, Flyway migrations,
     * and representative module record counts.
     */
    public Map<String, Object> queryDatabaseStats(String dbName, long timeoutSeconds) {
        validateDbName(dbName);

        // 1. Fetch public table names
        ProcessBuilder pbTables = createProcessBuilder(
                resolveBinary("psql"),
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "-d", dbName,
                "-t", "-A", "-c",
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;"
        );
        String tablesOutput = executeCommand(pbTables, timeoutSeconds, "query tables on " + dbName);
        List<String> tables = Arrays.stream(tablesOutput.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        int userCount = 0;
        if (tables.contains("users")) {
            ProcessBuilder pbUsers = createProcessBuilder(
                    resolveBinary("psql"),
                    "-h", dbHost,
                    "-p", String.valueOf(dbPort),
                    "-U", dbUser,
                    "-d", dbName,
                    "-t", "-A", "-c",
                    "SELECT COUNT(*) FROM users;"
            );
            try {
                String out = executeCommand(pbUsers, timeoutSeconds, "query user count on " + dbName).trim();
                userCount = Integer.parseInt(out);
            } catch (Exception ignored) {}
        }

        int migrationCount = 0;
        if (tables.contains("flyway_schema_history")) {
            ProcessBuilder pbMigrations = createProcessBuilder(
                    resolveBinary("psql"),
                    "-h", dbHost,
                    "-p", String.valueOf(dbPort),
                    "-U", dbUser,
                    "-d", dbName,
                    "-t", "-A", "-c",
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true;"
            );
            try {
                String out = executeCommand(pbMigrations, timeoutSeconds, "query migrations count on " + dbName).trim();
                migrationCount = Integer.parseInt(out);
            } catch (Exception ignored) {}
        }

        Map<String, Integer> moduleRecordCounts = new LinkedHashMap<>();
        String[] representativeTables = {"audit_logs", "skills", "skill_assessments", "employee_learning", "conversations"};
        for (String table : representativeTables) {
            if (tables.contains(table)) {
                ProcessBuilder pbRep = createProcessBuilder(
                        resolveBinary("psql"),
                        "-h", dbHost,
                        "-p", String.valueOf(dbPort),
                        "-U", dbUser,
                        "-d", dbName,
                        "-t", "-A", "-c",
                        "SELECT COUNT(*) FROM " + table + ";"
                );
                try {
                    String out = executeCommand(pbRep, timeoutSeconds, "query " + table + " count on " + dbName).trim();
                    moduleRecordCounts.put(table, Integer.parseInt(out));
                } catch (Exception ignored) {}
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("tableCount", tables.size());
        stats.put("tables", tables);
        stats.put("userCount", userCount);
        stats.put("migrationCount", migrationCount);
        stats.put("representativeRecordCounts", moduleRecordCounts);
        return stats;
    }

    /**
     * Retrieves the running PostgreSQL server version.
     */
    public String getPostgresServerVersion(long timeoutSeconds) {
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("psql"),
                "-h", dbHost,
                "-p", String.valueOf(dbPort),
                "-U", dbUser,
                "-d", dbName,
                "-t", "-A", "-c",
                "SELECT version();"
        );
        return executeCommand(pb, timeoutSeconds, "get postgres server version").trim();
    }

    /**
     * Retrieves the PostgreSQL client binary version.
     */
    public String getPostgresClientVersion(long timeoutSeconds) {
        ProcessBuilder pb = createProcessBuilder(
                resolveBinary("pg_dump"),
                "--version"
        );
        return executeCommand(pb, timeoutSeconds, "get pg_dump client version").trim();
    }

    // --- Private Helper Execution Engine ---

    /**
     * Creates a {@link ProcessBuilder} for executing PostgreSQL utilities with tokenized arguments.
     * Sets the database password via the {@code PGPASSWORD} environment variable in the child process.
     *
     * <p>Security Note: Credentials are not included in command-line arguments, logs, exceptions,
     * or backup metadata. However, privileged processes may potentially inspect process environment
     * variables and memory paging cannot be ruled out.
     */
    private ProcessBuilder createProcessBuilder(String... commands) {
        ProcessBuilder pb = new ProcessBuilder(commands);
        if (dbPassword != null && !dbPassword.isBlank()) {
            pb.environment().put("PGPASSWORD", dbPassword);
        }
        return pb;
    }

    private void validateDbName(String name) {
        if (name == null || !SAFE_DB_NAME.matcher(name).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid database name format.");
        }
    }

    private String executeCommand(ProcessBuilder pb, long timeoutSeconds, String description) {
        try {
            Process process = pb.start();
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = drainStream(process.getInputStream(), stdout);
            Thread errThread = drainStream(process.getErrorStream(), stderr);

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " timed out after " + timeoutSeconds + "s.");
            }

            outThread.join(2000);
            errThread.join(2000);

            if (process.exitValue() != 0) {
                String err = stderr.toString().trim();
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " failed (code " + process.exitValue() + "): " + err);
            }
            return stdout.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " was interrupted.");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to execute " + description + ": " + e.getMessage());
        }
    }

    private void executeCommandWithWarningTolerance(ProcessBuilder pb, long timeoutSeconds, String description) {
        try {
            Process process = pb.start();
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = drainStream(process.getInputStream(), stdout);
            Thread errThread = drainStream(process.getErrorStream(), stderr);

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " timed out after " + timeoutSeconds + "s.");
            }

            outThread.join(2000);
            errThread.join(2000);

            // pg_restore: code 0 = ok, code 1 = warnings (acceptable), code > 1 = fatal
            if (process.exitValue() > 1) {
                String err = stderr.toString().trim();
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " failed (code " + process.exitValue() + "): " + err);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " was interrupted.");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to execute " + description + ": " + e.getMessage());
        }
    }

    private void executeStreamingProcess(ProcessBuilder pb, InputStream inSource, OutputStream outDest, long timeoutMinutes, String description) {
        try {
            Process process = pb.start();
            StringBuilder stderr = new StringBuilder();
            Thread errThread = drainStream(process.getErrorStream(), stderr);

            ExecutorService pool = Executors.newFixedThreadPool(2);
            Future<?> inputPumpFuture = null;
            if (inSource != null) {
                inputPumpFuture = pool.submit(() -> {
                    try (OutputStream processIn = process.getOutputStream()) {
                        byte[] buffer = new byte[16384];
                        int read;
                        while ((read = inSource.read(buffer)) != -1) {
                            processIn.write(buffer, 0, read);
                        }
                        processIn.flush();
                    } catch (IOException ignored) {}
                });
            }

            Future<?> outputPumpFuture = pool.submit(() -> {
                try (InputStream processOut = process.getInputStream()) {
                    byte[] buffer = new byte[16384];
                    int read;
                    while ((read = processOut.read(buffer)) != -1) {
                        outDest.write(buffer, 0, read);
                    }
                    outDest.flush();
                } catch (IOException e) {
                    throw new RuntimeException("Error pumping process output", e);
                }
            });

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                pool.shutdownNow();
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " timed out after " + timeoutMinutes + "m.");
            }

            errThread.join(3000);
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            if (outputPumpFuture.isDone()) {
                try {
                    outputPumpFuture.get();
                } catch (ExecutionException e) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Streaming error: " + e.getCause().getMessage());
                }
            }

            if (process.exitValue() != 0) {
                String err = stderr.toString().trim();
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " failed with exit code " + process.exitValue() + ": " + err);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " was interrupted.");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "I/O error during " + description + ": " + e.getMessage());
        }
    }

    private void executeRestoreStreamingProcess(ProcessBuilder pb, InputStream inSource, long timeoutMinutes, String description) {
        try {
            Process process = pb.start();
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread outThread = drainStream(process.getInputStream(), stdout);
            Thread errThread = drainStream(process.getErrorStream(), stderr);

            try (OutputStream processIn = process.getOutputStream()) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = inSource.read(buffer)) != -1) {
                    processIn.write(buffer, 0, read);
                }
                processIn.flush();
            }

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " timed out after " + timeoutMinutes + "m.");
            }

            outThread.join(3000);
            errThread.join(3000);

            if (process.exitValue() > 1) {
                String err = stderr.toString().trim();
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " failed (code " + process.exitValue() + "): " + err);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, description + " was interrupted.");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "I/O error during " + description + ": " + e.getMessage());
        }
    }

    private Thread drainStream(InputStream in, StringBuilder accumulator) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    accumulator.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}
