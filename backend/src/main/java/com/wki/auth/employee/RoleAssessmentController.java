package com.wki.auth.employee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wki.auth.ai.AiRecommendationService;
import com.wki.auth.manager.ManagerNotifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * Role-based assessments.
 *
 * The employee picks a job role (for example "Java Developer"). The role is made of weighted skills that add up
 * to 100 marks, for example Java 35, Spring Boot 25, SQL 25, Problem Solving 15. One assessment covers all of the
 * role's skills. Each skill's marks are scored separately, so "SQL is worth 30 marks and you earned 10" shows up as a
 * skill gap, which drives the course recommendations and the AI recommendations.
 */
@RestController
@RequestMapping("/api/employee")
public class RoleAssessmentController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final JdbcTemplate jdbc;
    private final CurrentEmployee current;
    private final ManagerNotifier managers;
    private final AiRecommendationService ai;

    public RoleAssessmentController(JdbcTemplate jdbc, CurrentEmployee current, ManagerNotifier managers, AiRecommendationService ai) {
        this.jdbc = jdbc;
        this.current = current;
        this.managers = managers;
        this.ai = ai;
    }

    // ---------------------------------------------------------------- roles
    @GetMapping("/me/job-roles")
    public Map<String, Object> jobRoles() {
        long me = current.id();
        List<Map<String, Object>> roles = jdbc.queryForList(
                "SELECT role_id AS roleId, role_name AS roleName, description FROM ed_job_roles ORDER BY role_name");
        Map<Long, List<Map<String, Object>>> skillsByRole = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(
                "SELECT rs.role_id AS roleId, s.skill_id AS skillId, s.skill_name AS skillName, rs.weight_pct AS weightPct " +
                "FROM ed_job_role_skills rs JOIN ed_skills s ON s.skill_id = rs.skill_id ORDER BY rs.weight_pct DESC, s.skill_name")) {
            skillsByRole.computeIfAbsent(((Number) r.get("roleId")).longValue(), k -> new ArrayList<>()).add(r);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : roles) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            m.put("skills", skillsByRole.getOrDefault(((Number) r.get("roleId")).longValue(), List.of()));
            out.add(m);
        }

        Long selected = null;
        String jobRoleText = null;
        List<Map<String, Object>> p = jdbc.queryForList("SELECT target_role_id, job_role FROM ed_employee_profiles WHERE employee_id = ?", me);
        if (!p.isEmpty()) {
            Object t = p.get(0).get("target_role_id");
            if (t instanceof Number n) selected = n.longValue();
            Object j = p.get(0).get("job_role");
            if (j != null) jobRoleText = j.toString();
        }
        // If the employee typed a job role in "My Profile" (e.g. "Senior Java Developer"), suggest the matching role.
        Long suggested = null;
        if (jobRoleText != null && !jobRoleText.isBlank()) {
            String txt = jobRoleText.trim().toLowerCase(Locale.ROOT);
            for (Map<String, Object> r : roles) {
                String name = r.get("roleName").toString().toLowerCase(Locale.ROOT);
                if (txt.contains(name) || name.contains(txt)) { suggested = ((Number) r.get("roleId")).longValue(); break; }
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("roles", out);
        res.put("selectedRoleId", selected);
        res.put("suggestedRoleId", suggested);
        return res;
    }

    /** Body: {roleId}. Saves the employee's role and keeps the profile "job role" text in step. */
    @PutMapping("/me/job-role")
    public Map<String, Object> chooseRole(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long roleId = number(body, "roleId");
        List<String> names = jdbc.queryForList("SELECT role_name FROM ed_job_roles WHERE role_id = ?", String.class, roleId);
        if (names.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        jdbc.update("INSERT INTO ed_employee_profiles (employee_id, target_role_id, job_role) VALUES (?,?,?) AS new_row " +
                "ON DUPLICATE KEY UPDATE target_role_id = new_row.target_role_id, job_role = new_row.job_role",
                me, roleId, names.get(0));
        return Map.of("message", "Role saved", "roleName", names.get(0));
    }

    // ---------------------------------------------------------------- questions
    @GetMapping("/me/role-assessment/questions")
    public List<Map<String, Object>> questions(@RequestParam long roleId) {
        current.id();
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM ed_job_roles WHERE role_id = ?", Integer.class, roleId);
        if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        return jdbc.queryForList(
                "SELECT q.question_id AS questionId, q.topic AS topic, q.question_text AS questionText, " +
                "q.option_a AS optionA, q.option_b AS optionB, q.option_c AS optionC, q.option_d AS optionD, " +
                "s.skill_id AS skillId, s.skill_name AS skillName, rs.weight_pct AS weightPct " +
                "FROM ed_job_role_skills rs JOIN ed_skills s ON s.skill_id = rs.skill_id " +
                "JOIN ed_assessment_questions q ON q.skill_id = rs.skill_id " +
                "WHERE rs.role_id = ? ORDER BY rs.weight_pct DESC, s.skill_name, q.question_id", roleId);
    }

    // ---------------------------------------------------------------- submit
    /** Body: {roleId, answers:[{questionId, selectedOption}]} */
    @PostMapping("/me/role-assessment/submit")
    @Transactional
    public Map<String, Object> submit(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long roleId = number(body, "roleId");
        Object raw = body.get("answers");
        if (!(raw instanceof List<?> answers) || answers.isEmpty()) throw bad("Please answer the assessment questions");

        List<Map<String, Object>> roleSkills = jdbc.queryForList(
                "SELECT rs.skill_id, rs.weight_pct, s.skill_name, s.default_required_level " +
                "FROM ed_job_role_skills rs JOIN ed_skills s ON s.skill_id = rs.skill_id " +
                "WHERE rs.role_id = ? ORDER BY rs.weight_pct DESC, s.skill_name", roleId);
        if (roleSkills.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        String roleName = jdbc.queryForObject("SELECT role_name FROM ed_job_roles WHERE role_id = ?", String.class, roleId);

        Map<Long, String> chosen = new HashMap<>();
        for (Object item : answers) {
            if (!(item instanceof Map<?, ?> a)) continue;
            Object qid = a.get("questionId"), opt = a.get("selectedOption");
            if (!(qid instanceof Number) || opt == null) continue;
            String o = String.valueOf(opt).toUpperCase(Locale.ROOT);
            if (List.of("A", "B", "C", "D").contains(o)) chosen.put(((Number) qid).longValue(), o);
        }

        // Grade every skill first (nothing is written until we know the whole assessment was answered)
        record Graded(long skillId, String skillName, int weight, int total, int correct, double pct, double marks,
                      int required, List<long[]> idCorrect, List<String> picked) {}
        List<Graded> graded = new ArrayList<>();
        double overall = 0;
        for (Map<String, Object> rs : roleSkills) {
            long skillId = ((Number) rs.get("skill_id")).longValue();
            int weight = ((Number) rs.get("weight_pct")).intValue();
            List<Map<String, Object>> qs = jdbc.queryForList(
                    "SELECT question_id, correct_option FROM ed_assessment_questions WHERE skill_id = ? ORDER BY question_id", skillId);
            if (qs.isEmpty()) throw bad("No questions are available for " + rs.get("skill_name"));
            int correct = 0;
            List<long[]> idCorrect = new ArrayList<>();
            List<String> picked = new ArrayList<>();
            for (Map<String, Object> q : qs) {
                long qid = ((Number) q.get("question_id")).longValue();
                String sel = chosen.get(qid);
                if (sel == null) throw bad("Please answer every question before submitting");
                boolean ok = sel.equalsIgnoreCase(String.valueOf(q.get("correct_option")));
                if (ok) correct++;
                idCorrect.add(new long[]{qid, ok ? 1 : 0});
                picked.add(sel);
            }
            double pct = Math.round(correct * 10000.0 / qs.size()) / 100.0;
            double marks = Math.round(weight * correct * 100.0 / qs.size()) / 100.0;
            overall += marks;
            graded.add(new Graded(skillId, String.valueOf(rs.get("skill_name")), weight, qs.size(), correct, pct, marks,
                    ((Number) rs.get("default_required_level")).intValue(), idCorrect, picked));
        }
        overall = Math.round(overall * 100.0) / 100.0;

        jdbc.update("INSERT INTO ed_role_attempts (employee_id, role_id, overall_pct) VALUES (?,?,?)", me, roleId, overall);
        Long roleAttemptId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        List<String> gapNames = new ArrayList<>();
        for (Graded g : graded) {
            jdbc.update("INSERT INTO ed_assessment_attempts (employee_id, skill_id, total_questions, correct_answers, score_pct) VALUES (?,?,?,?,?)",
                    me, g.skillId(), g.total(), g.correct(), g.pct());
            Long attemptId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            for (int i = 0; i < g.idCorrect().size(); i++) {
                jdbc.update("INSERT INTO ed_assessment_answers (attempt_id, question_id, selected_option, is_correct) VALUES (?,?,?,?)",
                        attemptId, g.idCorrect().get(i)[0], g.picked().get(i), g.idCorrect().get(i)[1] == 1);
            }
            jdbc.update("INSERT INTO ed_role_attempt_skills (role_attempt_id, skill_id, weight_pct, marks_earned, skill_pct, attempt_id) VALUES (?,?,?,?,?,?)",
                    roleAttemptId, g.skillId(), g.weight(), g.marks(), g.pct(), attemptId);
            int level = level(g.pct());
            jdbc.update("INSERT INTO ed_employee_skills (employee_id, skill_id, self_level, required_level) VALUES (?,?,?,?) AS new_row " +
                    "ON DUPLICATE KEY UPDATE self_level = new_row.self_level", me, g.skillId(), level, g.required());
            Integer req = jdbc.queryForObject("SELECT required_level FROM ed_employee_skills WHERE employee_id = ? AND skill_id = ?", Integer.class, me, g.skillId());
            if (req != null && req > level) gapNames.add(g.skillName());
        }

        String msg = "Role assessment completed for " + roleName + ": " + overall + "%. " +
                (gapNames.isEmpty() ? "You meet the target on every skill." :
                        "Skill gaps found in " + String.join(", ", gapNames) + ". Check your recommended courses and AI recommendations.");
        jdbc.update("INSERT INTO ed_notifications (employee_id, message) VALUES (?,?)", me, msg.length() > 300 ? msg.substring(0, 297) + "..." : msg);
        managers.notifyManagers(me, gapNames.isEmpty() ? "ASSESSMENT" : "SKILL_GAP",
                "completed the " + roleName + " assessment with " + overall + "%" + (gapNames.isEmpty() ? "." : " (skill gaps: " + String.join(", ", gapNames) + ")."));

        return buildResult(roleAttemptId, me);
    }

    // ---------------------------------------------------------------- results
    @GetMapping("/me/role-assessment/latest")
    public Map<String, Object> latest() {
        long me = current.id();
        List<Long> ids = jdbc.queryForList(
                "SELECT role_attempt_id FROM ed_role_attempts WHERE employee_id = ? ORDER BY taken_at DESC, role_attempt_id DESC LIMIT 1", Long.class, me);
        if (ids.isEmpty()) return Map.of("exists", false);
        return buildResult(ids.get(0), me);
    }

    private Map<String, Object> buildResult(long roleAttemptId, long me) {
        List<Map<String, Object>> head = jdbc.queryForList(
                "SELECT ra.role_attempt_id AS roleAttemptId, ra.role_id AS roleId, r.role_name AS roleName, " +
                "ra.overall_pct AS overallPct, ra.taken_at AS takenAt " +
                "FROM ed_role_attempts ra JOIN ed_job_roles r ON r.role_id = ra.role_id " +
                "WHERE ra.role_attempt_id = ? AND ra.employee_id = ?", roleAttemptId, me);
        if (head.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found");

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT ras.skill_id AS skillId, s.skill_name AS skillName, ras.weight_pct AS weightPct, " +
                "ras.marks_earned AS marksEarned, ras.skill_pct AS skillPct, ras.attempt_id AS attemptId, " +
                "a.total_questions AS total, a.correct_answers AS correct, " +
                "COALESCE(es.required_level, s.default_required_level) AS requiredLevel " +
                "FROM ed_role_attempt_skills ras JOIN ed_skills s ON s.skill_id = ras.skill_id " +
                "LEFT JOIN ed_assessment_attempts a ON a.attempt_id = ras.attempt_id " +
                "LEFT JOIN ed_employee_skills es ON es.employee_id = ? AND es.skill_id = ras.skill_id " +
                "WHERE ras.role_attempt_id = ? ORDER BY ras.weight_pct DESC, s.skill_name", me, roleAttemptId);

        List<Map<String, Object>> skills = new ArrayList<>();
        int gapCount = 0;
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            double pct = ((Number) r.get("skillPct")).doubleValue();
            int lvl = level(pct);
            int req = ((Number) r.get("requiredLevel")).intValue();
            boolean gap = req > lvl;
            if (gap) gapCount++;
            m.put("level", lvl);
            m.put("gap", gap);
            m.remove("attemptId");

            List<Map<String, Object>> wrong = jdbc.queryForList(
                    "SELECT q.topic, q.question_text, q.explanation, q.correct_option, a.selected_option, " +
                    "q.option_a, q.option_b, q.option_c, q.option_d " +
                    "FROM ed_assessment_answers a JOIN ed_assessment_questions q ON q.question_id = a.question_id " +
                    "WHERE a.attempt_id = ? AND a.is_correct = 0 ORDER BY q.question_id", r.get("attemptId"));
            LinkedHashSet<String> topics = new LinkedHashSet<>();
            List<Map<String, Object>> missed = new ArrayList<>();
            for (Map<String, Object> w : wrong) {
                topics.add(String.valueOf(w.get("topic")));
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("topic", w.get("topic"));
                x.put("questionText", w.get("question_text"));
                x.put("yourAnswer", optionText(w, String.valueOf(w.get("selected_option"))));
                x.put("correctAnswer", optionText(w, String.valueOf(w.get("correct_option"))));
                x.put("explanation", w.get("explanation"));
                missed.add(x);
            }
            m.put("weakTopics", new ArrayList<>(topics));
            m.put("missed", missed);
            skills.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>(head.get(0));
        out.put("exists", true);
        out.put("gapCount", gapCount);
        out.put("skills", skills);
        return out;
    }

    private static String optionText(Map<String, Object> row, String letter) {
        Object v = row.get("option_" + letter.toLowerCase(Locale.ROOT));
        return letter + ". " + (v == null ? "" : v);
    }

    // ---------------------------------------------------------------- AI recommendations
    @GetMapping("/me/ai-recommendations")
    public Map<String, Object> aiRecommendations() {
        return aiFor(current.id(), false);
    }

    @PostMapping("/me/ai-recommendations/refresh")
    public Map<String, Object> aiRefresh() {
        return aiFor(current.id(), true);
    }

    private Map<String, Object> aiFor(long me, boolean force) {
        List<Long> ids = jdbc.queryForList(
                "SELECT role_attempt_id FROM ed_role_attempts WHERE employee_id = ? ORDER BY taken_at DESC, role_attempt_id DESC LIMIT 1", Long.class, me);
        if (ids.isEmpty()) return Map.of("ready", false, "message", "Take a role assessment first. Recommendations are built from your results.");
        long attemptId = ids.get(0);

        if (!force) {
            List<Map<String, Object>> cached = jdbc.queryForList(
                    "SELECT content, created_at FROM ed_ai_recommendations WHERE role_attempt_id = ? AND employee_id = ?", attemptId, me);
            if (!cached.isEmpty()) {
                try {
                    Map<String, Object> m = MAPPER.readValue(String.valueOf(cached.get(0).get("content")), new TypeReference<LinkedHashMap<String, Object>>() {});
                    m.put("ready", true);
                    m.put("generatedAt", String.valueOf(cached.get(0).get("created_at")));
                    return m;
                } catch (Exception ignored) { /* fall through and regenerate */ }
            }
        }

        Map<String, Object> result = buildResult(attemptId, me);
        String roleName = String.valueOf(result.get("roleName"));
        double overall = ((Number) result.get("overallPct")).doubleValue();
        @SuppressWarnings("unchecked") List<Map<String, Object>> skills = (List<Map<String, Object>>) result.get("skills");
        for (Map<String, Object> s : skills) {
            s.put("courses", jdbc.queryForList(
                    "SELECT title, duration_hours AS hours FROM ed_courses WHERE skill_id = ? ORDER BY course_level, duration_hours", s.get("skillId")));
        }
        Map<String, Object> rec = ai.generate(roleName, overall, skills);
        try {
            jdbc.update("REPLACE INTO ed_ai_recommendations (role_attempt_id, employee_id, source, content) VALUES (?,?,?,?)",
                    attemptId, me, String.valueOf(rec.get("source")), MAPPER.writeValueAsString(rec));
        } catch (Exception e) {
            System.err.println("Could not cache recommendations: " + e.getMessage());
        }
        rec.put("ready", true);
        rec.put("generatedAt", new Date().toString());
        return rec;
    }

    // ---------------------------------------------------------------- helpers
    /** Same mapping as the single-skill assessment: percent score to a 1-5 level. */
    private static int level(double score) {
        return score >= 90 ? 5 : score >= 75 ? 4 : score >= 55 ? 3 : score >= 35 ? 2 : 1;
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static long number(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && s.matches("-?\\d{1,18}")) return Long.parseLong(s);
        throw bad("'" + key + "' is required");
    }
}
