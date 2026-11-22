package com.wki.auth.messaging;

import com.wki.auth.employee.CurrentEmployee;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

/**
 * Messages for the Employee and Mentor dashboards (kept apart from the bell notifications).
 *
 * Who may chat with whom:
 *   employee - employee : only after one sent a follow request and the other accepted it (like Instagram)
 *   employee - mentor   : always, in both directions, no request needed
 *   anyone else         : not allowed
 *
 * The caller is always taken from the verified login (CurrentEmployee), never from the URL or body.
 */
@RestController
@RequestMapping("/api/messages")
public class MessagingController {

    private static final String EMPLOYEE = "EMPLOYEE";
    private static final String MENTOR = "TRAINER_MENTOR";
    private static final int MAX_LEN = 2000;

    /** SQL fragment: "First Last", ignoring the "-" placeholder last name used at registration. */
    private static String name(String a) {
        return "CONCAT(" + a + ".first_name, CASE WHEN " + a + ".last_name IS NULL OR " + a + ".last_name IN ('', '-') THEN '' ELSE CONCAT(' ', " + a + ".last_name) END)";
    }

    private final JdbcTemplate jdbc;
    private final CurrentEmployee current;

    public MessagingController(JdbcTemplate jdbc, CurrentEmployee current) {
        this.jdbc = jdbc;
        this.current = current;
    }

    // ---------------------------------------------------------------- badge numbers
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        long me = current.id();
        long unread = count("SELECT COUNT(*) FROM ed_messages WHERE recipient_id = ? AND read_at IS NULL", me);
        long requests = count("SELECT COUNT(*) FROM ed_connections WHERE addressee_id = ? AND status = 'PENDING'", me);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("unread", unread);
        out.put("requests", requests);
        out.put("total", unread + requests);
        return out;
    }

    // ---------------------------------------------------------------- chat list
    /** Everyone I have talked to, plus employees I am connected with (so a new connection can be opened right away). */
    @GetMapping("/conversations")
    public List<Map<String, Object>> conversations() {
        long me = current.id();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT x.partner AS id, " + name("e") + " AS name, r.role_name AS role, m.body AS lastBody, m.created_at AS lastAt, " +
                "m.sender_id = ? AS lastMine, " +
                "(SELECT COUNT(*) FROM ed_messages u WHERE u.sender_id = x.partner AND u.recipient_id = ? AND u.read_at IS NULL) AS unread " +
                "FROM (SELECT CASE WHEN sender_id = ? THEN recipient_id ELSE sender_id END AS partner, MAX(message_id) AS last_id " +
                "      FROM ed_messages WHERE sender_id = ? OR recipient_id = ? GROUP BY partner) x " +
                "JOIN ed_messages m ON m.message_id = x.last_id " +
                "JOIN employees e ON e.employee_id = x.partner " +
                "JOIN roles r ON r.role_id = e.role_id " +
                "ORDER BY m.message_id DESC", me, me, me, me, me);
        Set<Long> seen = new HashSet<>();
        for (Map<String, Object> r : rows) {
            seen.add(((Number) r.get("id")).longValue());
            r.put("lastMine", truthy(r.get("lastMine")));
        }
        for (Map<String, Object> c : jdbc.queryForList(
                "SELECT e.employee_id AS id, " + name("e") + " AS name, r.role_name AS role " +
                "FROM ed_connections c JOIN employees e ON e.employee_id = IF(c.requester_id = ?, c.addressee_id, c.requester_id) " +
                "JOIN roles r ON r.role_id = e.role_id " +
                "WHERE c.status = 'ACCEPTED' AND (c.requester_id = ? OR c.addressee_id = ?) AND e.status = 'ACTIVE' ORDER BY name", me, me, me)) {
            if (seen.add(((Number) c.get("id")).longValue())) {
                c.put("lastBody", null); c.put("lastAt", null); c.put("lastMine", false); c.put("unread", 0);
                rows.add(c);
            }
        }
        return rows;
    }

    // ---------------------------------------------------------------- every mentor
    @GetMapping("/mentors")
    public List<Map<String, Object>> mentors() {
        long me = current.id();
        return jdbc.queryForList(
                "SELECT e.employee_id AS id, " + name("e") + " AS name, mp.headline AS headline, COALESCE(mp.accepting, 1) AS accepting, " +
                "(SELECT COUNT(*) FROM ed_messages u WHERE u.sender_id = e.employee_id AND u.recipient_id = ? AND u.read_at IS NULL) AS unread " +
                "FROM employees e JOIN roles r ON r.role_id = e.role_id " +
                "LEFT JOIN ed_mentor_profiles mp ON mp.employee_id = e.employee_id " +
                "WHERE r.role_name = ? AND e.status = 'ACTIVE' AND e.employee_id <> ? ORDER BY name", me, MENTOR, me);
    }

    // ---------------------------------------------------------------- find employees
    /** Employees with my relation to each: NONE, REQUESTED (I asked), INCOMING (they asked me), CONNECTED. Optional ?q= name search. */
    @GetMapping("/people")
    public List<Map<String, Object>> people(@RequestParam(required = false) String q) {
        long me = current.id();
        String myRole = roleOf(me);
        String like = "%" + (q == null ? "" : q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")) + "%";
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT e.employee_id AS id, " + name("e") + " AS name, p.job_role AS jobRole, p.department AS department, " +
                "c.status AS cstatus, c.requester_id AS requesterId " +
                "FROM employees e JOIN roles r ON r.role_id = e.role_id " +
                "LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "LEFT JOIN ed_connections c ON (c.requester_id = ? AND c.addressee_id = e.employee_id) OR (c.requester_id = e.employee_id AND c.addressee_id = ?) " +
                "WHERE r.role_name = ? AND e.status = 'ACTIVE' AND e.employee_id <> ? AND LOWER(" + name("e") + ") LIKE ? " +
                "ORDER BY (c.status = 'ACCEPTED') DESC, name LIMIT 50", me, me, EMPLOYEE, me, like);
        for (Map<String, Object> r : rows) {
            String relation = relationOf(r.get("cstatus"), r.get("requesterId"), me);
            r.remove("cstatus"); r.remove("requesterId");
            r.put("relation", relation);
            r.put("canChat", MENTOR.equals(myRole) || "CONNECTED".equals(relation));
        }
        return rows;
    }

    // ---------------------------------------------------------------- follow requests (employee - employee)
    @GetMapping("/requests")
    public Map<String, Object> requests() {
        long me = current.id();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("incoming", jdbc.queryForList(
                "SELECT e.employee_id AS id, " + name("e") + " AS name, p.job_role AS jobRole, c.created_at AS createdAt " +
                "FROM ed_connections c JOIN employees e ON e.employee_id = c.requester_id " +
                "LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "WHERE c.addressee_id = ? AND c.status = 'PENDING' AND e.status = 'ACTIVE' ORDER BY c.created_at DESC", me));
        out.put("sent", jdbc.queryForList(
                "SELECT e.employee_id AS id, " + name("e") + " AS name, p.job_role AS jobRole, c.created_at AS createdAt " +
                "FROM ed_connections c JOIN employees e ON e.employee_id = c.addressee_id " +
                "LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "WHERE c.requester_id = ? AND c.status = 'PENDING' AND e.status = 'ACTIVE' ORDER BY c.created_at DESC", me));
        return out;
    }

    /** Send a follow request. If that person had already asked me, this simply accepts it. */
    @PostMapping("/follow/{id}")
    @Transactional
    public Map<String, Object> follow(@PathVariable long id) {
        long me = current.id();
        if (me == id) throw bad("You cannot follow yourself");
        if (!EMPLOYEE.equals(roleOf(me))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only employees can send follow requests");
        if (!EMPLOYEE.equals(activeRoleOf(id))) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");

        List<Map<String, Object>> ex = jdbc.queryForList(
                "SELECT connection_id, requester_id, status FROM ed_connections " +
                "WHERE (requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)", me, id, id, me);
        if (!ex.isEmpty()) {
            Map<String, Object> c = ex.get(0);
            if ("ACCEPTED".equals(c.get("status"))) throw conflict("You are already connected");
            if (((Number) c.get("requester_id")).longValue() == me) throw conflict("Request already sent");
            jdbc.update("UPDATE ed_connections SET status = 'ACCEPTED', responded_at = NOW() WHERE connection_id = ?", c.get("connection_id"));
            return Map.of("relation", "CONNECTED");
        }
        jdbc.update("INSERT INTO ed_connections (requester_id, addressee_id) VALUES (?, ?)", me, id);
        return Map.of("relation", "REQUESTED");
    }

    @PutMapping("/follow/{id}/accept")
    public Map<String, Object> accept(@PathVariable long id) {
        long me = current.id();
        int n = jdbc.update("UPDATE ed_connections SET status = 'ACCEPTED', responded_at = NOW() " +
                "WHERE requester_id = ? AND addressee_id = ? AND status = 'PENDING'", id, me);
        if (n == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That request is no longer there");
        return Map.of("relation", "CONNECTED");
    }

    @PutMapping("/follow/{id}/decline")
    public Map<String, Object> decline(@PathVariable long id) {
        long me = current.id();
        jdbc.update("DELETE FROM ed_connections WHERE requester_id = ? AND addressee_id = ? AND status = 'PENDING'", id, me);
        return Map.of("relation", "NONE");
    }

    /** Cancel a request I sent, or unfollow someone I am connected with. */
    @DeleteMapping("/follow/{id}")
    public Map<String, Object> unfollow(@PathVariable long id) {
        long me = current.id();
        jdbc.update("DELETE FROM ed_connections WHERE (requester_id = ? AND addressee_id = ?) " +
                "OR (requester_id = ? AND addressee_id = ? AND status = 'ACCEPTED')", me, id, id, me);
        return Map.of("relation", "NONE");
    }

    // ---------------------------------------------------------------- one conversation
    /** Messages with one person. Opening it marks what they sent me as read. ?after=<messageId> returns only newer ones (for polling). */
    @GetMapping("/with/{id}")
    @Transactional
    public Map<String, Object> thread(@PathVariable long id, @RequestParam(required = false) Long after) {
        long me = current.id();
        String role = activeRoleOf(id);
        if (me == id || !(EMPLOYEE.equals(role) || MENTOR.equals(role)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found");

        jdbc.update("UPDATE ed_messages SET read_at = NOW() WHERE sender_id = ? AND recipient_id = ? AND read_at IS NULL", id, me);

        List<Map<String, Object>> msgs;
        String cols = "SELECT message_id AS id, sender_id = ? AS mine, body, created_at AS createdAt, read_at AS readAt FROM ed_messages ";
        String pair = "((sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?))";
        if (after != null) {
            msgs = jdbc.queryForList(cols + "WHERE " + pair + " AND message_id > ? ORDER BY message_id", me, me, id, id, me, after);
        } else {
            msgs = jdbc.queryForList("SELECT * FROM (" + cols + "WHERE " + pair + " ORDER BY message_id DESC LIMIT 100) t ORDER BY id", me, me, id, id, me);
        }
        for (Map<String, Object> m : msgs) m.put("mine", truthy(m.get("mine")));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("partner", partner(me, id, role));
        out.put("messages", msgs);
        return out;
    }

    /** Body: {body: "text"} */
    @PostMapping("/with/{id}")
    public Map<String, Object> send(@PathVariable long id, @RequestBody Map<String, Object> req) {
        long me = current.id();
        String text = req.get("body") == null ? "" : String.valueOf(req.get("body")).trim();
        if (text.isEmpty()) throw bad("Type a message first");
        if (text.length() > MAX_LEN) throw bad("Messages can be at most " + MAX_LEN + " characters");
        String role = activeRoleOf(id);
        if (me == id || !(EMPLOYEE.equals(role) || MENTOR.equals(role)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found");
        if (!canChat(me, roleOf(me), id, role))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only message employees who have accepted your follow request, and mentors");

        KeyHolder keys = new GeneratedKeyHolder();
        final String body = text;
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO ed_messages (sender_id, recipient_id, body) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, me); ps.setLong(2, id); ps.setString(3, body);
            return ps;
        }, keys);
        long newId = keys.getKey().longValue();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", newId);
        m.put("mine", true);
        m.put("body", text);
        m.put("createdAt", jdbc.queryForObject("SELECT created_at FROM ed_messages WHERE message_id = ?", Object.class, newId));
        m.put("readAt", null);
        return m;
    }

    // ---------------------------------------------------------------- helpers
    private Map<String, Object> partner(long me, long id, String role) {
        Map<String, Object> p = jdbc.queryForList("SELECT " + name("e") + " AS name FROM employees e WHERE e.employee_id = ?", id).get(0);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("name", p.get("name"));
        out.put("role", role);
        String myRole = roleOf(me);
        out.put("canChat", canChat(me, myRole, id, role));
        out.put("relation", EMPLOYEE.equals(myRole) && EMPLOYEE.equals(role) ? relationBetween(me, id) : "NONE");
        return out;
    }

    private boolean canChat(long me, String myRole, long other, String otherRole) {
        if (MENTOR.equals(myRole) && EMPLOYEE.equals(otherRole)) return true;
        if (EMPLOYEE.equals(myRole) && MENTOR.equals(otherRole)) return true;
        if (EMPLOYEE.equals(myRole) && EMPLOYEE.equals(otherRole)) return "CONNECTED".equals(relationBetween(me, other));
        return false;
    }

    private String relationBetween(long me, long other) {
        List<Map<String, Object>> r = jdbc.queryForList(
                "SELECT status, requester_id FROM ed_connections WHERE (requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)",
                me, other, other, me);
        return r.isEmpty() ? "NONE" : relationOf(r.get(0).get("status"), r.get(0).get("requester_id"), me);
    }

    private static String relationOf(Object status, Object requesterId, long me) {
        if (status == null) return "NONE";
        if ("ACCEPTED".equals(status)) return "CONNECTED";
        return ((Number) requesterId).longValue() == me ? "REQUESTED" : "INCOMING";
    }

    private String roleOf(long id) {
        List<String> r = jdbc.queryForList("SELECT r.role_name FROM employees e JOIN roles r ON r.role_id = e.role_id WHERE e.employee_id = ?", String.class, id);
        return r.isEmpty() ? "" : r.get(0);
    }

    /** Role of an ACTIVE account, or "" when the account does not exist or is not active. */
    private String activeRoleOf(long id) {
        List<String> r = jdbc.queryForList("SELECT r.role_name FROM employees e JOIN roles r ON r.role_id = e.role_id WHERE e.employee_id = ? AND e.status = 'ACTIVE'", String.class, id);
        return r.isEmpty() ? "" : r.get(0);
    }

    private long count(String sql, Object... args) {
        Long n = jdbc.queryForObject(sql, Long.class, args);
        return n == null ? 0 : n;
    }

    private static boolean truthy(Object o) {
        return o instanceof Boolean b ? b : o instanceof Number n && n.longValue() != 0;
    }

    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private static ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); }
}
