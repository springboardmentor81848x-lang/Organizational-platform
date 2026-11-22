package com.wki.auth.mentor;

import com.wki.auth.employee.CurrentEmployee;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Everything the Mentor (TRAINER_MENTOR) dashboard needs. The mentor is always taken from the verified login,
 * never from the URL, so a mentor can only see and change their own requests, mentees and sessions.
 */
@RestController
@RequestMapping("/api/mentor")
public class MentorDashboardController {

    private final JdbcTemplate jdbc;
    private final CurrentEmployee current;

    public MentorDashboardController(JdbcTemplate jdbc, CurrentEmployee current) {
        this.jdbc = jdbc;
        this.current = current;
    }

    // ---------------------------------------------------------------- overview (numbers and chart data)
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        long me = current.id();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeMentees", count("SELECT COUNT(DISTINCT mentee_id) FROM ed_mentorships WHERE mentor_id = ? AND status = 'ACTIVE'", me));
        stats.put("pendingRequests", count("SELECT COUNT(*) FROM ed_mentorships WHERE mentor_id = ? AND status = 'REQUESTED'", me));
        stats.put("upcomingSessions", count("SELECT COUNT(*) FROM ed_knowledge_sessions WHERE host_id = ? AND scheduled_at >= NOW()", me));
        stats.put("sessionsHeld", count("SELECT COUNT(*) FROM ed_knowledge_sessions WHERE host_id = ? AND scheduled_at < NOW()", me));
        stats.put("employeesReached", count(
                "SELECT COUNT(DISTINCT r.employee_id) FROM ed_session_registrations r JOIN ed_knowledge_sessions ks ON ks.session_id = r.session_id " +
                "WHERE ks.host_id = ? AND r.employee_id <> ks.host_id", me));
        List<Map<String, Object>> att = jdbc.queryForList(
                "SELECT COALESCE(SUM(r.attended = 1), 0) AS attended, COUNT(*) AS marked FROM ed_session_registrations r " +
                "JOIN ed_knowledge_sessions ks ON ks.session_id = r.session_id " +
                "WHERE ks.host_id = ? AND r.employee_id <> ks.host_id AND r.attended IS NOT NULL", me);
        long marked = ((Number) att.get(0).get("marked")).longValue();
        stats.put("attendanceRate", marked == 0 ? null : Math.round(((Number) att.get(0).get("attended")).doubleValue() * 100.0 / marked));

        // sessions and registrations for the last 6 months (months with nothing are filled with zeros)
        Map<String, long[]> byMonth = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) byMonth.put(now.minusMonths(i).toString(), new long[]{0, 0});
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT DATE_FORMAT(ks.scheduled_at, '%Y-%m') AS ym, COUNT(DISTINCT ks.session_id) AS sessions, COUNT(r.employee_id) AS registered " +
                "FROM ed_knowledge_sessions ks LEFT JOIN ed_session_registrations r ON r.session_id = ks.session_id AND r.employee_id <> ks.host_id " +
                "WHERE ks.host_id = ? AND ks.scheduled_at >= DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL 5 MONTH) GROUP BY ym", me)) {
            long[] v = byMonth.get(String.valueOf(r.get("ym")));
            if (v != null) { v[0] = ((Number) r.get("sessions")).longValue(); v[1] = ((Number) r.get("registered")).longValue(); }
        }
        List<Map<String, Object>> months = new ArrayList<>();
        for (Map.Entry<String, long[]> e : byMonth.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", YearMonth.parse(e.getKey()).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            m.put("sessions", e.getValue()[0]);
            m.put("registered", e.getValue()[1]);
            months.add(m);
        }

        Map<String, Long> status = new LinkedHashMap<>();
        for (String s : List.of("REQUESTED", "ACTIVE", "COMPLETED", "DECLINED")) status.put(s, 0L);
        for (Map<String, Object> r : jdbc.queryForList("SELECT status, COUNT(*) AS n FROM ed_mentorships WHERE mentor_id = ? GROUP BY status", me))
            status.put(String.valueOf(r.get("status")), ((Number) r.get("n")).longValue());

        List<Map<String, Object>> recent = jdbc.queryForList(
                "SELECT ks.session_id AS sessionId, ks.title AS title, ks.scheduled_at AS scheduledAt, " +
                "(SELECT COUNT(*) FROM ed_session_registrations r WHERE r.session_id = ks.session_id AND r.employee_id <> ks.host_id) AS registered, " +
                "(SELECT COUNT(*) FROM ed_session_registrations r WHERE r.session_id = ks.session_id AND r.employee_id <> ks.host_id AND r.attended = 1) AS attended, " +
                "(SELECT COUNT(*) FROM ed_session_registrations r WHERE r.session_id = ks.session_id AND r.employee_id <> ks.host_id AND r.attended IS NOT NULL) AS marked " +
                "FROM ed_knowledge_sessions ks WHERE ks.host_id = ? AND ks.scheduled_at < NOW() ORDER BY ks.scheduled_at DESC LIMIT 6", me);
        Collections.reverse(recent);

        List<Map<String, Object>> next = jdbc.queryForList(
                "SELECT ks.session_id AS sessionId, ks.title AS title, ks.scheduled_at AS scheduledAt, ks.duration_min AS durationMin, ks.session_mode AS mode, " +
                "(SELECT COUNT(*) FROM ed_session_registrations r WHERE r.session_id = ks.session_id AND r.employee_id <> ks.host_id) AS registered " +
                "FROM ed_knowledge_sessions ks WHERE ks.host_id = ? AND ks.scheduled_at >= NOW() ORDER BY ks.scheduled_at LIMIT 1", me);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stats", stats);
        out.put("months", months);
        out.put("mentorshipStatus", status);
        out.put("recentSessions", recent);
        out.put("nextSession", next.isEmpty() ? null : next.get(0));
        out.put("topGaps", gapSkills(6));
        return out;
    }

    // ---------------------------------------------------------------- what employees lack (topics worth a session)
    @GetMapping("/skill-gaps")
    public Map<String, Object> skillGaps() {
        current.id();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("gaps", gapSkills(12));
        out.put("catalog", jdbc.queryForList("SELECT skill_id AS skillId, skill_name AS skillName, category FROM ed_skills ORDER BY category, skill_name"));
        return out;
    }

    private List<Map<String, Object>> gapSkills(int limit) {
        return jdbc.queryForList(
                "SELECT s.skill_id AS skillId, s.skill_name AS skillName, s.category AS category, " +
                "SUM(es.required_level > es.self_level) AS withGap, COUNT(*) AS assessed, ROUND(AVG(es.self_level), 1) AS avgLevel, MAX(es.required_level) AS targetLevel " +
                "FROM ed_employee_skills es JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "WHERE EXISTS (SELECT 1 FROM ed_assessment_attempts a WHERE a.employee_id = es.employee_id AND a.skill_id = es.skill_id) " +
                "GROUP BY s.skill_id, s.skill_name, s.category HAVING withGap > 0 ORDER BY withGap DESC, s.skill_name LIMIT " + limit);
    }

    // ---------------------------------------------------------------- mentorship requests
    @GetMapping("/requests")
    public List<Map<String, Object>> requests() {
        long me = current.id();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT m.mentorship_id AS mentorshipId, m.topic AS topic, m.created_at AS createdAt, m.mentee_id AS menteeId, " +
                "CONCAT(e.first_name, ' ', e.last_name) AS menteeName " +
                "FROM ed_mentorships m JOIN employees e ON e.employee_id = m.mentee_id " +
                "WHERE m.mentor_id = ? AND m.status = 'REQUESTED' ORDER BY m.created_at DESC", me);
        for (Map<String, Object> r : rows) r.put("gaps", skillRows(((Number) r.get("menteeId")).longValue(), true, 4));
        return rows;
    }

    /** Body: {status: ACTIVE | DECLINED | COMPLETED} */
    @PutMapping("/requests/{id}")
    @Transactional
    public Map<String, Object> answer(@PathVariable long id, @RequestBody Map<String, Object> body) {
        long me = current.id();
        String status = String.valueOf(body.get("status"));
        if (!List.of("ACTIVE", "DECLINED", "COMPLETED").contains(status)) throw bad("Invalid status");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT mentee_id, topic FROM ed_mentorships WHERE mentorship_id = ? AND mentor_id = ?", id, me);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentorship not found");
        jdbc.update("UPDATE ed_mentorships SET status = ? WHERE mentorship_id = ?", status, id);
        String verb = status.equals("ACTIVE") ? "accepted" : status.equals("DECLINED") ? "declined" : "marked complete";
        notify(((Number) rows.get(0).get("mentee_id")).longValue(), "Your mentorship request \"" + rows.get(0).get("topic") + "\" was " + verb + ".");
        return Map.of("message", "Updated");
    }

    // ---------------------------------------------------------------- mentees
    @GetMapping("/mentees")
    public List<Map<String, Object>> mentees() {
        long me = current.id();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT m.mentorship_id AS mentorshipId, m.topic AS topic, m.status AS status, m.created_at AS createdAt, m.mentee_id AS menteeId, " +
                "CONCAT(e.first_name, ' ', e.last_name) AS menteeName " +
                "FROM ed_mentorships m JOIN employees e ON e.employee_id = m.mentee_id " +
                "WHERE m.mentor_id = ? AND m.status IN ('ACTIVE', 'COMPLETED') ORDER BY m.status, m.created_at DESC", me);
        for (Map<String, Object> r : rows) {
            long mentee = ((Number) r.get("menteeId")).longValue();
            r.put("skills", skillRows(mentee, false, 6));
            Map<String, Object> learn = jdbc.queryForMap(
                    "SELECT COUNT(*) AS enrolled, COALESCE(SUM(status = 'COMPLETED'), 0) AS completed, COALESCE(ROUND(AVG(progress_pct)), 0) AS avgProgress " +
                    "FROM ed_enrollments WHERE employee_id = ?", mentee);
            r.put("learning", learn);
            r.put("sessionsAttended", count(
                    "SELECT COUNT(*) FROM ed_session_registrations r JOIN ed_knowledge_sessions ks ON ks.session_id = r.session_id " +
                    "WHERE ks.host_id = ? AND r.employee_id = ? AND r.attended = 1", me, mentee));
        }
        return rows;
    }

    /** Assessed skills of one employee, weakest first. gapsOnly keeps only skills below target. */
    private List<Map<String, Object>> skillRows(long employeeId, boolean gapsOnly, int limit) {
        return jdbc.queryForList(
                "SELECT s.skill_name AS skillName, es.self_level AS level, es.required_level AS targetLevel, " +
                "(SELECT a.score_pct FROM ed_assessment_attempts a WHERE a.employee_id = es.employee_id AND a.skill_id = es.skill_id ORDER BY a.attempted_at DESC, a.attempt_id DESC LIMIT 1) AS scorePct " +
                "FROM ed_employee_skills es JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "WHERE es.employee_id = ? AND EXISTS (SELECT 1 FROM ed_assessment_attempts a WHERE a.employee_id = es.employee_id AND a.skill_id = es.skill_id) " +
                (gapsOnly ? "AND es.required_level > es.self_level " : "") +
                "ORDER BY (es.required_level - es.self_level) DESC, s.skill_name LIMIT " + limit, employeeId);
    }

    // ---------------------------------------------------------------- sessions
    @GetMapping("/sessions")
    public List<Map<String, Object>> sessions() {
        long me = current.id();
        return jdbc.queryForList(
                "SELECT ks.session_id AS sessionId, ks.title AS title, ks.description AS description, ks.scheduled_at AS scheduledAt, " +
                "ks.duration_min AS durationMin, ks.session_mode AS mode, ks.location AS location, s.skill_name AS skillName, " +
                "(ks.scheduled_at >= NOW()) AS upcoming, " +
                "(SELECT COUNT(*) FROM ed_session_registrations r WHERE r.session_id = ks.session_id AND r.employee_id <> ks.host_id) AS registered, " +
                "(SELECT COUNT(*) FROM ed_session_registrations r WHERE r.session_id = ks.session_id AND r.employee_id <> ks.host_id AND r.attended = 1) AS attended, " +
                "(SELECT COUNT(*) FROM ed_session_registrations r WHERE r.session_id = ks.session_id AND r.employee_id <> ks.host_id AND r.attended IS NOT NULL) AS marked " +
                "FROM ed_knowledge_sessions ks LEFT JOIN ed_skills s ON s.skill_id = ks.skill_id " +
                "WHERE ks.host_id = ? ORDER BY ks.scheduled_at DESC LIMIT 100", me);
    }

    /** Body: {title, description, scheduledAt "yyyy-MM-ddTHH:mm", durationMin, skillId?, mode ONLINE|IN_PERSON, location?} */
    @PostMapping("/sessions")
    @Transactional
    public Map<String, Object> createSession(@RequestBody Map<String, Object> body) {
        long me = current.id();
        String title = text(body, "title", 150, true);
        String description = text(body, "description", 500, false);
        String location = text(body, "location", 200, false);
        LocalDateTime when;
        try { when = LocalDateTime.parse(text(body, "scheduledAt", 30, true)); }
        catch (DateTimeParseException ex) { throw bad("Invalid date and time"); }
        if (when.isBefore(LocalDateTime.now())) throw bad("Choose a future date and time");
        int duration = body.get("durationMin") == null ? 60 : (int) number(body, "durationMin");
        if (duration < 15 || duration > 480) throw bad("Duration must be between 15 and 480 minutes");
        String mode = body.get("mode") == null ? "ONLINE" : String.valueOf(body.get("mode")).toUpperCase(Locale.ROOT);
        if (!List.of("ONLINE", "IN_PERSON").contains(mode)) throw bad("Invalid session type");

        Long skillId = null; String skillName = null;
        if (body.get("skillId") != null && !String.valueOf(body.get("skillId")).isBlank()) {
            skillId = number(body, "skillId");
            List<String> n = jdbc.queryForList("SELECT skill_name FROM ed_skills WHERE skill_id = ?", String.class, skillId);
            if (n.isEmpty()) throw bad("Unknown skill");
            skillName = n.get(0);
        }
        jdbc.update("INSERT INTO ed_knowledge_sessions (title, description, host_id, scheduled_at, skill_id, duration_min, session_mode, location) VALUES (?,?,?,?,?,?,?,?)",
                title, description, me, when, skillId, duration, mode, location);

        // Tell the people this session is for: employees who have a gap in the skill, and the mentor's active mentees.
        String host = jdbc.queryForObject("SELECT CONCAT(first_name, ' ', last_name) FROM employees WHERE employee_id = ?", String.class, me);
        String when2 = when.format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH));
        Map<Long, String> invite = new LinkedHashMap<>();
        for (Long id : jdbc.queryForList("SELECT DISTINCT mentee_id FROM ed_mentorships WHERE mentor_id = ? AND status = 'ACTIVE'", Long.class, me))
            invite.put(id, "Your mentor " + host + " scheduled \"" + title + "\" on " + when2 + ".");
        if (skillId != null) {
            for (Long id : jdbc.queryForList(
                    "SELECT DISTINCT es.employee_id FROM ed_employee_skills es JOIN employees e ON e.employee_id = es.employee_id " +
                    "WHERE es.skill_id = ? AND es.required_level > es.self_level AND e.status = 'ACTIVE' AND e.employee_id <> ? " +
                    "AND EXISTS (SELECT 1 FROM ed_assessment_attempts a WHERE a.employee_id = es.employee_id AND a.skill_id = es.skill_id) LIMIT 300", Long.class, skillId, me))
                invite.put(id, host + " scheduled \"" + title + "\" on " + when2 + ". It covers " + skillName + ", a skill where you have a gap. Register under Sharing & Mentorship.");
        }
        for (Map.Entry<Long, String> e : invite.entrySet()) notify(e.getKey(), e.getValue());
        return Map.of("message", "Session scheduled", "invited", invite.size());
    }

    @DeleteMapping("/sessions/{id}")
    @Transactional
    public Map<String, Object> cancel(@PathVariable long id) {
        long me = current.id();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT title, (scheduled_at >= NOW()) AS upcoming FROM ed_knowledge_sessions WHERE session_id = ? AND host_id = ?", id, me);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        if (((Number) rows.get(0).get("upcoming")).intValue() == 0) throw bad("Past sessions cannot be cancelled");
        for (Long emp : jdbc.queryForList("SELECT employee_id FROM ed_session_registrations WHERE session_id = ? AND employee_id <> ?", Long.class, id, me))
            notify(emp, "The session \"" + rows.get(0).get("title") + "\" was cancelled by the mentor.");
        jdbc.update("DELETE FROM ed_session_registrations WHERE session_id = ?", id);
        jdbc.update("DELETE FROM ed_knowledge_sessions WHERE session_id = ? AND host_id = ?", id, me);
        return Map.of("message", "Session cancelled");
    }

    @GetMapping("/sessions/{id}/attendees")
    public List<Map<String, Object>> attendees(@PathVariable long id) {
        ownSession(id);
        return jdbc.queryForList(
                "SELECT e.employee_id AS employeeId, CONCAT(e.first_name, ' ', e.last_name) AS name, r.attended AS attended " +
                "FROM ed_session_registrations r JOIN employees e ON e.employee_id = r.employee_id " +
                "WHERE r.session_id = ? AND r.employee_id <> ? ORDER BY e.first_name, e.last_name", id, current.id());
    }

    /** Body: {attendedIds:[employeeId, ...]}. Everyone registered and not listed is marked absent. */
    @PutMapping("/sessions/{id}/attendance")
    @Transactional
    public Map<String, Object> attendance(@PathVariable long id, @RequestBody Map<String, Object> body) {
        long me = current.id();
        ownSession(id);
        Integer started = jdbc.queryForObject("SELECT (scheduled_at <= NOW()) FROM ed_knowledge_sessions WHERE session_id = ?", Integer.class, id);
        if (started == null || started == 0) throw bad("Attendance can be marked once the session has started");
        Set<Long> present = new HashSet<>();
        if (body.get("attendedIds") instanceof List<?> l) for (Object o : l) if (o instanceof Number n) present.add(n.longValue());
        jdbc.update("UPDATE ed_session_registrations SET attended = 0 WHERE session_id = ? AND employee_id <> ?", id, me);
        for (Long emp : present)
            jdbc.update("UPDATE ed_session_registrations SET attended = 1 WHERE session_id = ? AND employee_id = ? AND employee_id <> ?", id, emp, me);
        return Map.of("message", "Attendance saved", "present", present.size());
    }

    // ---------------------------------------------------------------- the mentor's profile: skills they teach
    @GetMapping("/profile")
    public Map<String, Object> profile() {
        long me = current.id();
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> p = jdbc.queryForList("SELECT headline, accepting FROM ed_mentor_profiles WHERE employee_id = ?", me);
        out.put("headline", p.isEmpty() ? "" : p.get(0).get("headline"));
        out.put("accepting", p.isEmpty() || ((Number) p.get(0).get("accepting")).intValue() == 1);
        out.put("skillIds", jdbc.queryForList("SELECT skill_id FROM ed_mentor_skills WHERE employee_id = ?", Long.class, me));
        out.put("catalog", jdbc.queryForList("SELECT skill_id AS skillId, skill_name AS skillName, category FROM ed_skills ORDER BY category, skill_name"));
        return out;
    }

    /** Body: {skillIds:[...], headline, accepting}. Employees see these skills under Sharing and Mentorship. */
    @PutMapping("/profile")
    @Transactional
    public Map<String, Object> saveProfile(@RequestBody Map<String, Object> body) {
        long me = current.id();
        String headline = text(body, "headline", 200, false);
        boolean accepting = !Boolean.FALSE.equals(body.get("accepting"));
        Set<Long> ids = new LinkedHashSet<>();
        if (body.get("skillIds") instanceof List<?> l) for (Object o : l) if (o instanceof Number n) ids.add(n.longValue());
        if (ids.size() > 30) throw bad("Choose at most 30 skills");
        for (Long id : ids) {
            Integer ok = jdbc.queryForObject("SELECT COUNT(*) FROM ed_skills WHERE skill_id = ?", Integer.class, id);
            if (ok == null || ok == 0) throw bad("Unknown skill");
        }
        jdbc.update("INSERT INTO ed_mentor_profiles (employee_id, headline, accepting) VALUES (?,?,?) AS new_row " +
                "ON DUPLICATE KEY UPDATE headline = new_row.headline, accepting = new_row.accepting", me, headline, accepting ? 1 : 0);
        jdbc.update("DELETE FROM ed_mentor_skills WHERE employee_id = ?", me);
        for (Long id : ids) jdbc.update("INSERT INTO ed_mentor_skills (employee_id, skill_id) VALUES (?,?)", me, id);
        return Map.of("message", "Profile saved", "skills", ids.size());
    }

    // ---------------------------------------------------------------- the mentor's own notifications
    @GetMapping("/notifications")
    public Map<String, Object> notifications() {
        long me = current.id();
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT notification_id AS notificationId, message, is_read AS isRead, created_at AS createdAt " +
                "FROM ed_notifications WHERE employee_id = ? ORDER BY created_at DESC, notification_id DESC LIMIT 50", me);
        Integer unread = jdbc.queryForObject("SELECT COUNT(*) FROM ed_notifications WHERE employee_id = ? AND is_read = 0", Integer.class, me);
        return Map.of("unread", unread == null ? 0 : unread, "items", items);
    }

    @PutMapping("/notifications/{id}/read")
    public Map<String, Object> markRead(@PathVariable long id) {
        jdbc.update("UPDATE ed_notifications SET is_read = 1 WHERE notification_id = ? AND employee_id = ?", id, current.id());
        return Map.of("message", "Marked read");
    }

    @PutMapping("/notifications/read-all")
    public Map<String, Object> markAllRead() {
        jdbc.update("UPDATE ed_notifications SET is_read = 1 WHERE employee_id = ?", current.id());
        return Map.of("message", "All marked read");
    }

    // ---------------------------------------------------------------- helpers
    private void ownSession(long id) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM ed_knowledge_sessions WHERE session_id = ? AND host_id = ?", Integer.class, id, current.id());
        if (n == null || n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
    }

    private void notify(long employeeId, String message) {
        jdbc.update("INSERT INTO ed_notifications (employee_id, message) VALUES (?,?)", employeeId,
                message.length() > 300 ? message.substring(0, 297) + "..." : message);
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private static ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }

    private static long number(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && s.matches("-?\\d{1,18}")) return Long.parseLong(s);
        throw bad("'" + key + "' is required");
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
}
