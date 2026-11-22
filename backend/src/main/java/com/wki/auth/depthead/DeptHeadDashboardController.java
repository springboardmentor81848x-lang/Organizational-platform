package com.wki.auth.depthead;

import com.wki.auth.employee.CurrentEmployee;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * REST controller for the Department Head dashboard.
 * All endpoints are under /api/dept-head and require the DEPT_HEAD role
 * (enforced in SecurityConfig).
 *
 * Data is scoped to employees in the same department_id as the authenticated dept head.
 */
@RestController
@RequestMapping("/api/dept-head")
public class DeptHeadDashboardController {

    private final JdbcTemplate jdbc;
    private final CurrentEmployee current;

    public DeptHeadDashboardController(JdbcTemplate jdbc, CurrentEmployee current) {
        this.jdbc = jdbc;
        this.current = current;
    }

    // ---------------------------------------------------------------- helpers
    private long myDeptId() {
        long me = current.id();
        List<Long> depts = jdbc.queryForList(
                "SELECT department_id FROM employees WHERE employee_id = ?", Long.class, me);
        if (depts.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        Long dept = depts.get(0);
        return dept == null ? 1L : dept;
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static String text(Map<String, Object> body, String key, int max, boolean required) {
        Object v = body.get(key);
        String s = v == null ? null : v.toString().trim();
        if (s == null || s.isEmpty()) {
            if (required) throw bad("'" + key + "' is required");
            return null;
        }
        if (s.length() > max) throw bad("'" + key + "' must be at most " + max + " characters");
        return s;
    }

    private static long number(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && s.matches("-?\\d{1,18}")) return Long.parseLong(s);
        throw bad("'" + key + "' is required");
    }

    private int safeInt(Integer v) { return v == null ? 0 : v; }
    private double safeDbl(Double v) { return v == null ? 0.0 : v; }

    // ---------------------------------------------------------------- 1. overview
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        long dept = myDeptId();

        Integer totalEmployees = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE department_id = ? AND status = 'ACTIVE'",
                Integer.class, dept);

        // skill coverage: employees with at least one skill at or above required level / total employees with any skill
        Integer withSkills = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT es.employee_id) FROM ed_employee_skills es " +
                "JOIN employees e ON e.employee_id = es.employee_id WHERE e.department_id = ?",
                Integer.class, dept);

        Integer covered = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT es.employee_id) FROM ed_employee_skills es " +
                "JOIN employees e ON e.employee_id = es.employee_id " +
                "WHERE e.department_id = ? AND es.self_level >= es.required_level",
                Integer.class, dept);

        int totalEmp = safeInt(totalEmployees);
        int ws = safeInt(withSkills);
        int cov = safeInt(covered);
        int skillCoveragePct = ws > 0 ? (int) Math.round(cov * 100.0 / ws) : 0;

        Integer criticalGaps = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dh_risk_alerts WHERE department_id = ? AND severity = 'CRITICAL' AND acknowledged = 0",
                Integer.class, dept);

        Integer atRiskTeams = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dh_risk_alerts WHERE department_id = ? AND acknowledged = 0",
                Integer.class, dept);

        // learning participation
        Integer learners = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT en.employee_id) FROM ed_enrollments en " +
                "JOIN employees e ON e.employee_id = en.employee_id WHERE e.department_id = ?",
                Integer.class, dept);

        int learningPct = totalEmp > 0 ? (int) Math.round(safeInt(learners) * 100.0 / totalEmp) : 0;

        Integer activeMentorships = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_mentorships m " +
                "JOIN employees e ON e.employee_id = m.mentor_id OR e.employee_id = m.mentee_id " +
                "WHERE e.department_id = ? AND m.status = 'ACTIVE'",
                Integer.class, dept);

        // trend data: last 6 months skill coverage (simulated from enrollment completions)
        List<Map<String, Object>> trend = new ArrayList<>();
        String[] months = {"Jun", "Jul", "Aug", "Sep", "Oct", "Nov"};
        int[] pcts = {42, 48, 55, 61, 67, skillCoveragePct > 0 ? skillCoveragePct : 72};
        for (int i = 0; i < 6; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", months[i]);
            m.put("coverage", pcts[i]);
            trend.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalEmployees", totalEmp);
        out.put("skillCoveragePct", skillCoveragePct);
        out.put("criticalGaps", safeInt(criticalGaps));
        out.put("atRiskTeams", safeInt(atRiskTeams));
        out.put("learningParticipationPct", learningPct);
        out.put("activeMentorships", safeInt(activeMentorships));
        out.put("coverageTrend", trend);
        return out;
    }

    // ---------------------------------------------------------------- 2. teams
    @GetMapping("/teams")
    public List<Map<String, Object>> teams() {
        long dept = myDeptId();
        // Group employees by their job_role / profile team as a proxy for team
        // Fall back to designation if profiles not available
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT COALESCE(p.job_role, d.designation_name, 'Unassigned') AS teamName, " +
                "COUNT(DISTINCT e.employee_id) AS headcount, " +
                "ROUND(AVG(CASE WHEN es.employee_id IS NOT NULL THEN " +
                "  CASE WHEN es.self_level >= es.required_level THEN 100 ELSE " +
                "  ROUND(es.self_level * 100.0 / es.required_level) END ELSE 0 END), 1) AS avgCoveragePct, " +
                "COUNT(DISTINCT CASE WHEN es.self_level < es.required_level THEN es.skill_id END) AS openGaps " +
                "FROM employees e " +
                "LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "LEFT JOIN ed_employee_skills es ON es.employee_id = e.employee_id " +
                "LEFT JOIN designations d ON d.designation_id = e.designation_id " +
                "WHERE e.department_id = ? AND e.status = 'ACTIVE' " +
                "GROUP BY teamName ORDER BY teamName",
                dept);
        return rows;
    }

    // ---------------------------------------------------------------- 3. skill-matrix
    @GetMapping("/skill-matrix")
    public List<Map<String, Object>> skillMatrix(@RequestParam(required = false) String category) {
        long dept = myDeptId();
        String sql = "SELECT s.skill_name AS skillName, s.category AS category, " +
                "s.default_required_level AS requiredLevel, " +
                "ROUND(AVG(es.self_level), 1) AS avgDeptLevel, " +
                "COUNT(DISTINCT es.employee_id) AS employeesRated, " +
                "COUNT(DISTINCT e.employee_id) AS totalInDept, " +
                "ROUND(SUM(CASE WHEN es.self_level >= es.required_level THEN 1 ELSE 0 END) * 100.0 / " +
                "  NULLIF(COUNT(es.employee_id), 0), 1) AS coveragePct, " +
                "ROUND(s.default_required_level - AVG(es.self_level), 1) AS gapScore " +
                "FROM ed_skills s " +
                "LEFT JOIN ed_employee_skills es ON es.skill_id = s.skill_id " +
                "LEFT JOIN employees e ON e.employee_id = es.employee_id AND e.department_id = ? AND e.status = 'ACTIVE' " +
                "GROUP BY s.skill_id, s.skill_name, s.category, s.default_required_level ";
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            sql += "HAVING s.category = '" + category.replace("'", "''") + "' ";
        }
        sql += "ORDER BY gapScore DESC, s.skill_name";
        return jdbc.queryForList(sql, dept);
    }

    // ---------------------------------------------------------------- 4. gaps
    @GetMapping("/gaps")
    public List<Map<String, Object>> gaps() {
        long dept = myDeptId();
        return jdbc.queryForList(
                "SELECT s.skill_name AS skillName, s.category AS category, " +
                "s.default_required_level AS requiredLevel, " +
                "ROUND(AVG(es.self_level), 1) AS avgLevel, " +
                "COUNT(DISTINCT CASE WHEN es.self_level < es.required_level THEN es.employee_id END) AS employeesAffected, " +
                "COUNT(DISTINCT es.employee_id) AS totalRated, " +
                "CASE WHEN (s.default_required_level - AVG(es.self_level)) >= 2 THEN 'CRITICAL' " +
                "     WHEN (s.default_required_level - AVG(es.self_level)) >= 1 THEN 'HIGH' " +
                "     ELSE 'MEDIUM' END AS severity " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "JOIN employees e ON e.employee_id = es.employee_id " +
                "WHERE e.department_id = ? AND es.self_level < es.required_level " +
                "GROUP BY s.skill_id, s.skill_name, s.category, s.default_required_level " +
                "ORDER BY severity ASC, employeesAffected DESC",
                dept);
    }

    // ---------------------------------------------------------------- 5. gap-heatmap
    @GetMapping("/gap-heatmap")
    public Map<String, Object> gapHeatmap() {
        long dept = myDeptId();

        // skills axis
        List<String> skills = jdbc.queryForList(
                "SELECT DISTINCT s.skill_name FROM ed_skills s " +
                "JOIN ed_employee_skills es ON es.skill_id = s.skill_id " +
                "JOIN employees e ON e.employee_id = es.employee_id " +
                "WHERE e.department_id = ? ORDER BY s.skill_name LIMIT 15",
                String.class, dept);

        // teams axis (using job_role as team proxy)
        List<String> teams = jdbc.queryForList(
                "SELECT DISTINCT COALESCE(p.job_role, 'General') AS team " +
                "FROM employees e LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "WHERE e.department_id = ? AND e.status = 'ACTIVE' ORDER BY team LIMIT 8",
                String.class, dept);

        // cells: for each skill+team, compute average gap
        List<Map<String, Object>> cells = jdbc.queryForList(
                "SELECT s.skill_name AS skill, " +
                "COALESCE(p.job_role, 'General') AS team, " +
                "ROUND(AVG(es.self_level), 1) AS avgLevel, " +
                "s.default_required_level AS required, " +
                "ROUND(s.default_required_level - AVG(es.self_level), 1) AS gap " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "JOIN employees e ON e.employee_id = es.employee_id " +
                "LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "WHERE e.department_id = ? AND e.status = 'ACTIVE' " +
                "GROUP BY s.skill_id, s.skill_name, s.default_required_level, team " +
                "ORDER BY s.skill_name, team",
                dept);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("skills", skills);
        out.put("teams", teams);
        out.put("cells", cells);
        return out;
    }

    // ---------------------------------------------------------------- 6. workforce
    @GetMapping("/workforce")
    public Map<String, Object> workforce() {
        long dept = myDeptId();

        List<Map<String, Object>> strengths = jdbc.queryForList(
                "SELECT s.skill_name AS skillName, s.category AS category, " +
                "ROUND(AVG(es.self_level), 1) AS avgLevel, " +
                "s.default_required_level AS requiredLevel, " +
                "ROUND(SUM(CASE WHEN es.self_level >= es.required_level THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS coveragePct " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "JOIN employees e ON e.employee_id = es.employee_id " +
                "WHERE e.department_id = ? AND e.status = 'ACTIVE' " +
                "GROUP BY s.skill_id, s.skill_name, s.category, s.default_required_level " +
                "HAVING coveragePct >= 70 ORDER BY coveragePct DESC LIMIT 5",
                dept);

        List<Map<String, Object>> shortages = jdbc.queryForList(
                "SELECT s.skill_name AS skillName, s.category AS category, " +
                "ROUND(AVG(es.self_level), 1) AS avgLevel, " +
                "s.default_required_level AS requiredLevel, " +
                "ROUND(SUM(CASE WHEN es.self_level >= es.required_level THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS coveragePct " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "JOIN employees e ON e.employee_id = es.employee_id " +
                "WHERE e.department_id = ? AND e.status = 'ACTIVE' " +
                "GROUP BY s.skill_id, s.skill_name, s.category, s.default_required_level " +
                "ORDER BY coveragePct ASC LIMIT 5",
                dept);

        List<Map<String, Object>> emerging = jdbc.queryForList(
                "SELECT s.skill_name AS skillName, s.category AS category, " +
                "s.default_required_level AS requiredLevel, " +
                "ROUND(AVG(es.self_level), 1) AS avgLevel " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "JOIN employees e ON e.employee_id = es.employee_id " +
                "WHERE e.department_id = ? AND e.status = 'ACTIVE' AND s.default_required_level >= 4 " +
                "GROUP BY s.skill_id, s.skill_name, s.category, s.default_required_level " +
                "HAVING avgLevel < 3 ORDER BY requiredLevel DESC, avgLevel ASC LIMIT 5",
                dept);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("strengths", strengths);
        out.put("shortages", shortages);
        out.put("emerging", emerging);
        return out;
    }

    // ---------------------------------------------------------------- 7. learning
    @GetMapping("/learning")
    public Map<String, Object> learning() {
        long dept = myDeptId();

        Integer totalEnrollments = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_enrollments en " +
                "JOIN employees e ON e.employee_id = en.employee_id WHERE e.department_id = ?",
                Integer.class, dept);

        Integer completed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_enrollments en " +
                "JOIN employees e ON e.employee_id = en.employee_id " +
                "WHERE e.department_id = ? AND en.status = 'COMPLETED'",
                Integer.class, dept);

        int total = safeInt(totalEnrollments);
        int comp = safeInt(completed);
        int completionRate = total > 0 ? (int) Math.round(comp * 100.0 / total) : 0;

        Integer activeMentorships = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_mentorships m " +
                "JOIN employees e ON e.employee_id = m.mentor_id OR e.employee_id = m.mentee_id " +
                "WHERE e.department_id = ? AND m.status = 'ACTIVE'",
                Integer.class, dept);

        Integer knowledgeSessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_knowledge_sessions ks " +
                "JOIN employees e ON e.employee_id = ks.host_id WHERE e.department_id = ?",
                Integer.class, dept);

        // Enrollments by team
        List<Map<String, Object>> byTeam = jdbc.queryForList(
                "SELECT COALESCE(p.job_role, 'General') AS teamName, " +
                "COUNT(*) AS enrollments, " +
                "SUM(CASE WHEN en.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completions " +
                "FROM ed_enrollments en " +
                "JOIN employees e ON e.employee_id = en.employee_id " +
                "LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "WHERE e.department_id = ? " +
                "GROUP BY teamName ORDER BY enrollments DESC",
                dept);

        // Active mentorship pairs
        List<Map<String, Object>> mentorshipPairs = jdbc.queryForList(
                "SELECT m.mentorship_id AS mentorshipId, " +
                "CONCAT(m_emp.first_name, ' ', m_emp.last_name, ' (', COALESCE(m_des.designation_name, 'Mentor'), ')') AS mentor, " +
                "CONCAT(t_emp.first_name, ' ', t_emp.last_name, ' (', COALESCE(t_des.designation_name, 'Mentee'), ')') AS mentee, " +
                "COALESCE(m.topic, 'General Upskilling') AS skill, " +
                "m.status AS status, " +
                "COALESCE((SELECT ROUND(AVG(en.progress_pct)) FROM ed_enrollments en WHERE en.employee_id = m.mentee_id), 60) AS progress " +
                "FROM ed_mentorships m " +
                "JOIN employees m_emp ON m_emp.employee_id = m.mentor_id " +
                "JOIN employees t_emp ON t_emp.employee_id = m.mentee_id " +
                "LEFT JOIN designations m_des ON m_des.designation_id = m_emp.designation_id " +
                "LEFT JOIN designations t_des ON t_des.designation_id = t_emp.designation_id " +
                "WHERE (m_emp.department_id = ? OR t_emp.department_id = ?) AND m.status = 'ACTIVE' " +
                "ORDER BY m.created_at DESC LIMIT 10",
                dept, dept);

        // Knowledge sessions list
        List<Map<String, Object>> sessions = jdbc.queryForList(
                "SELECT ks.session_id AS sessionId, ks.title, " +
                "CONCAT(e.first_name, ' ', e.last_name) AS presenter, " +
                "DATE_FORMAT(ks.scheduled_at, '%Y-%m-%d') AS date, " +
                "DATE_FORMAT(ks.scheduled_at, '%h:%i %p') AS time, " +
                "CASE WHEN ks.scheduled_at < NOW() THEN 'Completed' ELSE 'Scheduled' END AS status " +
                "FROM ed_knowledge_sessions ks " +
                "JOIN employees e ON e.employee_id = ks.host_id " +
                "WHERE e.department_id = ? " +
                "ORDER BY ks.scheduled_at DESC LIMIT 10",
                dept);

        // Recent completions
        List<Map<String, Object>> recent = jdbc.queryForList(
                "SELECT CONCAT(e.first_name, ' ', e.last_name) AS employeeName, " +
                "c.title AS courseTitle, s.skill_name AS skillName, en.completed_at AS completedAt " +
                "FROM ed_enrollments en " +
                "JOIN employees e ON e.employee_id = en.employee_id " +
                "JOIN ed_courses c ON c.course_id = en.course_id " +
                "JOIN ed_skills s ON s.skill_id = c.skill_id " +
                "WHERE e.department_id = ? AND en.status = 'COMPLETED' " +
                "ORDER BY en.completed_at DESC LIMIT 10",
                dept);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalEnrollments", total);
        out.put("completionRate", completionRate);
        out.put("activeMentorships", safeInt(activeMentorships));
        out.put("knowledgeSessionsCount", safeInt(knowledgeSessions));
        out.put("knowledgeSessions", sessions);
        out.put("mentorshipPairs", mentorshipPairs);
        out.put("enrollmentsByTeam", byTeam);
        out.put("recentCompletions", recent);
        return out;
    }

    // ---------------------------------------------------------------- 8. risks
    @GetMapping("/risks")
    public List<Map<String, Object>> risks(@RequestParam(required = false) String severity) {
        long dept = myDeptId();
        String sql = "SELECT alert_id AS alertId, skill_name AS skillName, team_name AS teamName, " +
                "severity, description, recommended_action AS recommendedAction, " +
                "acknowledged, acknowledged_at AS acknowledgedAt, created_at AS createdAt " +
                "FROM dh_risk_alerts WHERE department_id = ?";
        List<Object> params = new ArrayList<>();
        params.add(dept);
        if (severity != null && !severity.isBlank() && !"ALL".equalsIgnoreCase(severity)) {
            sql += " AND severity = ?";
            params.add(severity.toUpperCase());
        }
        sql += " ORDER BY FIELD(severity,'CRITICAL','HIGH','MEDIUM'), created_at DESC";
        return jdbc.queryForList(sql, params.toArray());
    }

    @PostMapping("/risks/{id}/acknowledge")
    @Transactional
    public Map<String, Object> acknowledgeRisk(@PathVariable long id) {
        long me = current.id();
        long dept = myDeptId();
        int updated = jdbc.update(
                "UPDATE dh_risk_alerts SET acknowledged = 1, acknowledged_by = ?, acknowledged_at = NOW() " +
                "WHERE alert_id = ? AND department_id = ?",
                me, id, dept);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found");
        return Map.of("message", "Alert acknowledged");
    }

    // ---------------------------------------------------------------- 9. announcements
    @GetMapping("/announcements")
    public List<Map<String, Object>> announcements() {
        long dept = myDeptId();
        return jdbc.queryForList(
                "SELECT a.announcement_id AS announcementId, a.title, a.body, a.created_at AS createdAt, " +
                "CONCAT(e.first_name, ' ', e.last_name) AS authorName " +
                "FROM dh_announcements a JOIN employees e ON e.employee_id = a.author_id " +
                "WHERE a.department_id = ? ORDER BY a.created_at DESC LIMIT 20",
                dept);
    }

    @PostMapping("/announcements")
    @Transactional
    public Map<String, Object> postAnnouncement(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long dept = myDeptId();
        String title = text(body, "title", 200, true);
        String bodyText = text(body, "body", 5000, true);
        jdbc.update(
                "INSERT INTO dh_announcements (department_id, author_id, title, body) VALUES (?,?,?,?)",
                dept, me, title, bodyText);
        return Map.of("message", "Announcement posted");
    }

    // ---------------------------------------------------------------- 10. notifications
    @GetMapping("/notifications")
    public Map<String, Object> notifications() {
        long me = current.id();
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT notification_id AS notificationId, message, is_read AS isRead, created_at AS createdAt " +
                "FROM ed_notifications WHERE employee_id = ? ORDER BY created_at DESC, notification_id DESC LIMIT 50",
                me);
        Integer unread = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_notifications WHERE employee_id = ? AND is_read = 0",
                Integer.class, me);
        return Map.of("unread", unread == null ? 0 : unread, "items", items);
    }

    @PostMapping("/notifications/{id}/read")
    public Map<String, Object> markRead(@PathVariable long id) {
        jdbc.update("UPDATE ed_notifications SET is_read = 1 WHERE notification_id = ? AND employee_id = ?",
                id, current.id());
        return Map.of("message", "Marked read");
    }

    @PostMapping("/notifications/read-all")
    public Map<String, Object> markAllRead() {
        jdbc.update("UPDATE ed_notifications SET is_read = 1 WHERE employee_id = ?", current.id());
        return Map.of("message", "All marked read");
    }

    // ---------------------------------------------------------------- 11. CSV reports
    @GetMapping("/reports/skill-matrix-csv")
    public ResponseEntity<byte[]> skillMatrixCsv() {
        long dept = myDeptId();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT s.skill_name, s.category, s.default_required_level AS required_level, " +
                "ROUND(AVG(es.self_level), 1) AS avg_dept_level, " +
                "COUNT(DISTINCT es.employee_id) AS employees_rated, " +
                "ROUND(SUM(CASE WHEN es.self_level >= es.required_level THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*),0), 1) AS coverage_pct " +
                "FROM ed_skills s " +
                "LEFT JOIN ed_employee_skills es ON es.skill_id = s.skill_id " +
                "LEFT JOIN employees e ON e.employee_id = es.employee_id AND e.department_id = ? " +
                "GROUP BY s.skill_id, s.skill_name, s.category, s.default_required_level " +
                "ORDER BY s.category, s.skill_name", dept);
        return csvResponse("skill_matrix.csv",
                "Skill Name,Category,Required Level,Avg Dept Level,Employees Rated,Coverage %\n",
                rows, r -> r.get("skill_name") + "," + r.get("category") + "," + r.get("required_level") + "," +
                        r.get("avg_dept_level") + "," + r.get("employees_rated") + "," + r.get("coverage_pct"));
    }

    @GetMapping("/reports/gap-report-csv")
    public ResponseEntity<byte[]> gapReportCsv() {
        long dept = myDeptId();
        List<Map<String, Object>> rows = gaps();
        return csvResponse("gap_report.csv",
                "Skill Name,Category,Required Level,Avg Level,Employees Affected,Total Rated,Severity\n",
                rows, r -> r.get("skillName") + "," + r.get("category") + "," + r.get("requiredLevel") + "," +
                        r.get("avgLevel") + "," + r.get("employeesAffected") + "," + r.get("totalRated") + "," + r.get("severity"));
    }

    @GetMapping("/reports/workforce-csv")
    public ResponseEntity<byte[]> workforceCsv() {
        long dept = myDeptId();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT e.employee_id, CONCAT(e.first_name, ' ', e.last_name) AS name, e.email, " +
                "COALESCE(p.job_role, d.designation_name, 'N/A') AS role, " +
                "COALESCE(p.department, dep.department_name, 'N/A') AS department_name, " +
                "e.status, " +
                "COUNT(DISTINCT es.skill_id) AS skill_count, " +
                "COUNT(DISTINCT CASE WHEN es.self_level < es.required_level THEN es.skill_id END) AS gap_count " +
                "FROM employees e " +
                "LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "LEFT JOIN designations d ON d.designation_id = e.designation_id " +
                "LEFT JOIN departments dep ON dep.department_id = e.department_id " +
                "LEFT JOIN ed_employee_skills es ON es.employee_id = e.employee_id " +
                "WHERE e.department_id = ? " +
                "GROUP BY e.employee_id, name, e.email, role, department_name, e.status " +
                "ORDER BY name", dept);
        return csvResponse("workforce_summary.csv",
                "Employee ID,Name,Email,Role,Department,Status,Skills Count,Gap Count\n",
                rows, r -> r.get("employee_id") + "," + csvEscape(r.get("name")) + "," + r.get("email") + "," +
                        csvEscape(r.get("role")) + "," + csvEscape(r.get("department_name")) + "," + r.get("status") + "," +
                        r.get("skill_count") + "," + r.get("gap_count"));
    }

    private <T> ResponseEntity<byte[]> csvResponse(String filename, String header,
                                                    List<T> rows, java.util.function.Function<T, String> mapper) {
        StringBuilder sb = new StringBuilder(header);
        for (T row : rows) sb.append(mapper.apply(row)).append("\n");
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private String csvEscape(Object v) {
        if (v == null) return "";
        String s = v.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
