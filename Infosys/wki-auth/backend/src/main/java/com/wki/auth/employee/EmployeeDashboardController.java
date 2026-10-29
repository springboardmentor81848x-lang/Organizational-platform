package com.wki.auth.employee;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Everything the employee dashboard needs. All data is scoped to the logged-in employee
 * (taken from the JWT), never from a URL or request body.
 */
@RestController
@RequestMapping("/api/employee")
public class EmployeeDashboardController {

    private final JdbcTemplate jdbc;
    private final CurrentEmployee current;

    public EmployeeDashboardController(JdbcTemplate jdbc, CurrentEmployee current) {
        this.jdbc = jdbc;
        this.current = current;
    }

    // ---------------------------------------------------------------- 1. profile
    @GetMapping("/me/profile")
    public Map<String, Object> profile() {
        long me = current.id();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT e.employee_id AS employeeId, e.first_name AS firstName, e.last_name AS lastName, " +
                "e.email AS email, e.status AS status, e.role AS role, p.phone AS phone, p.location AS location, " +
                "p.bio AS bio, p.linkedin AS linkedin, " +
                "p.education AS education, p.work_experience AS workExperience, " +
                "p.department AS department, p.job_role AS jobRole " +
                "FROM employees e LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "WHERE e.employee_id = ?", me);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        return rows.get(0);
    }

    @PutMapping("/me/profile")
    @Transactional
    public Map<String, Object> updateProfile(@RequestBody Map<String, Object> body) {
        long me = current.id();
        String first = text(body, "firstName", 100, true);
        String last = text(body, "lastName", 100, false);
        jdbc.update("UPDATE employees SET first_name = ?, last_name = ? WHERE employee_id = ?",
                first, last == null || last.isBlank() ? "-" : last, me);
        jdbc.update(
                "INSERT INTO ed_employee_profiles (employee_id, phone, location, bio, linkedin, education, work_experience, department, job_role) " +
                "VALUES (?,?,?,?,?,?,?,?,?) AS new_row " +
                "ON DUPLICATE KEY UPDATE phone = new_row.phone, location = new_row.location, " +
                "bio = new_row.bio, linkedin = new_row.linkedin, education = new_row.education, " +
                "work_experience = new_row.work_experience, department = new_row.department, job_role = new_row.job_role",
                me, text(body, "phone", 30, false), text(body, "location", 100, false),
                text(body, "bio", 1000, false), text(body, "linkedin", 255, false),
                text(body, "education", 500, false), text(body, "workExperience", 1000, false),
                text(body, "department", 100, false), text(body, "jobRole", 100, false));
        return profile();
    }

    // ---------------------------------------------------------------- 2. evidence-based skill assessments
    @GetMapping("/me/assessment/questions")
    public List<Map<String, Object>> assessmentQuestions(@RequestParam long skillId) {
        long me = current.id();
        Integer selected = jdbc.queryForObject("SELECT COUNT(*) FROM ed_employee_skills WHERE employee_id = ? AND skill_id = ?", Integer.class, me, skillId);
        if (selected == null || selected == 0) throw bad("Add this skill to My Skills before starting an assessment");
        return jdbc.queryForList(
                "SELECT question_id AS questionId, topic, question_text AS questionText, option_a AS optionA, " +
                "option_b AS optionB, option_c AS optionC, option_d AS optionD " +
                "FROM ed_assessment_questions WHERE skill_id = ? ORDER BY question_id", skillId);
    }

    @PostMapping("/me/assessment/submit")
    @Transactional
    public Map<String, Object> submitAssessment(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long skillId = number(body, "skillId");
        Object raw = body.get("answers");
        if (!(raw instanceof List<?> answers) || answers.isEmpty()) throw bad("Please answer the assessment questions");
        Integer selected = jdbc.queryForObject("SELECT COUNT(*) FROM ed_employee_skills WHERE employee_id = ? AND skill_id = ?", Integer.class, me, skillId);
        if (selected == null || selected == 0) throw bad("Add this skill to My Skills before taking the assessment");
        List<Map<String,Object>> questions = jdbc.queryForList("SELECT question_id, correct_option, topic, explanation FROM ed_assessment_questions WHERE skill_id = ?", skillId);
        Map<Long, Map<String,Object>> byId = new HashMap<>();
        for (Map<String,Object> q : questions) byId.put(((Number)q.get("question_id")).longValue(), q);
        int correct = 0;
        List<Map<String,Object>> checked = new ArrayList<>();
        for (Object item : answers) {
            if (!(item instanceof Map<?,?> a)) continue;
            Object qidObj = a.get("questionId"), optionObj = a.get("selectedOption");
            if (!(qidObj instanceof Number) || optionObj == null) continue;
            long qid = ((Number) qidObj).longValue();
            String option = String.valueOf(optionObj).toUpperCase(Locale.ROOT);
            Map<String,Object> q = byId.get(qid);
            if (q == null || !List.of("A","B","C","D").contains(option)) continue;
            boolean isCorrect = option.equalsIgnoreCase(String.valueOf(q.get("correct_option")));
            if (isCorrect) correct++;
            Map<String,Object> row = new LinkedHashMap<>(); row.put("questionId", qid); row.put("selectedOption", option); row.put("isCorrect", isCorrect); row.put("correctOption", q.get("correct_option")); row.put("topic", q.get("topic")); row.put("explanation", q.get("explanation"));
            checked.add(row);
        }
        if (checked.size() != questions.size()) throw bad("Please answer every question before submitting");
        double score = Math.round(correct * 10000.0 / questions.size()) / 100.0;
        int mappedLevel = score >= 90 ? 5 : score >= 75 ? 4 : score >= 55 ? 3 : score >= 35 ? 2 : 1;
        jdbc.update("INSERT INTO ed_assessment_attempts (employee_id, skill_id, total_questions, correct_answers, score_pct) VALUES (?,?,?,?,?)", me, skillId, questions.size(), correct, score);
        Long attemptId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        for (Map<String,Object> a : checked) jdbc.update("INSERT INTO ed_assessment_answers (attempt_id, question_id, selected_option, is_correct) VALUES (?,?,?,?)", attemptId, a.get("questionId"), a.get("selectedOption"), a.get("isCorrect"));
        jdbc.update("UPDATE ed_employee_skills SET self_level = ? WHERE employee_id = ? AND skill_id = ?", mappedLevel, me, skillId);
        String skillName = jdbc.queryForObject("SELECT skill_name FROM ed_skills WHERE skill_id = ?", String.class, skillId);
        notify(me, "Assessment completed for " + skillName + ": " + score + "%. Course recommendations have been updated.");
        checkAutoAchievements(me);
        Map<String,Object> out = new LinkedHashMap<>(); out.put("attemptId", attemptId); out.put("skillId", skillId); out.put("skillName", skillName); out.put("totalQuestions", questions.size()); out.put("correctAnswers", correct); out.put("scorePct", score); out.put("assessedLevel", mappedLevel); out.put("answerReview", checked);
        return out;
    }

    @GetMapping("/me/assessment/history")
    public List<Map<String,Object>> assessmentHistory() {
        return jdbc.queryForList("SELECT a.attempt_id AS attemptId, s.skill_name AS skillName, a.total_questions AS totalQuestions, a.correct_answers AS correctAnswers, a.score_pct AS scorePct, a.attempted_at AS attemptedAt FROM ed_assessment_attempts a JOIN ed_skills s ON s.skill_id = a.skill_id WHERE a.employee_id = ? ORDER BY a.attempted_at DESC, a.attempt_id DESC", current.id());
    }

    // ---------------------------------------------------------------- 2. skills
    @GetMapping("/skills")
    public List<Map<String, Object>> catalog() {
        return jdbc.queryForList(
                "SELECT skill_id AS skillId, skill_name AS skillName, category, " +
                "default_required_level AS defaultRequiredLevel FROM ed_skills ORDER BY category, skill_name");
    }

    @GetMapping("/me/skills")
    public List<Map<String, Object>> mySkills() {
        return skillRows(current.id());
    }

    private List<Map<String, Object>> skillRows(long me) {
        return jdbc.queryForList(
                "SELECT s.skill_id AS skillId, s.skill_name AS skillName, s.category AS category, " +
                "es.self_level AS selfLevel, es.required_level AS requiredLevel, " +
                "(SELECT a.score_pct FROM ed_assessment_attempts a WHERE a.employee_id = es.employee_id AND a.skill_id = es.skill_id ORDER BY a.attempted_at DESC, a.attempt_id DESC LIMIT 1) AS assessmentPercent, " +
                "(SELECT a.attempted_at FROM ed_assessment_attempts a WHERE a.employee_id = es.employee_id AND a.skill_id = es.skill_id ORDER BY a.attempted_at DESC, a.attempt_id DESC LIMIT 1) AS lastAssessedAt " +
                "FROM ed_employee_skills es JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "WHERE es.employee_id = ? ORDER BY s.category, s.skill_name", me);
    }

    /** Add a skill to the employee's assessment list. Proficiency is set only by scored assessments. */
    @PutMapping("/me/skills")
    public Map<String, Object> rateSkill(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long skillId = number(body, "skillId");
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM ed_skills WHERE skill_id = ?", Integer.class, skillId);
        if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown skill");
        jdbc.update(
                "INSERT INTO ed_employee_skills (employee_id, skill_id, self_level, required_level) " +
                "SELECT ?, skill_id, 1, default_required_level FROM ed_skills WHERE skill_id = ? " +
                "ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP",
                me, skillId);
        return Map.of("message", "Skill saved");
    }

    @DeleteMapping("/me/skills/{skillId}")
    public Map<String, Object> removeSkill(@PathVariable long skillId) {
        jdbc.update("DELETE FROM ed_employee_skills WHERE employee_id = ? AND skill_id = ?", current.id(), skillId);
        return Map.of("message", "Skill removed");
    }

    // ---------------------------------------------------------------- 3. knowledge gaps
    @GetMapping("/me/gaps")
    public Map<String, Object> gaps() {
        List<Map<String, Object>> all = skillRows(current.id()).stream()
                .filter(s -> s.get("assessmentPercent") != null).toList();
        List<Map<String, Object>> gaps = new ArrayList<>();
        List<Map<String, Object>> strengths = new ArrayList<>();
        int sumSelf = 0, sumGap = 0;
        for (Map<String, Object> s : all) {
            int self = ((Number) s.get("selfLevel")).intValue();
            int required = ((Number) s.get("requiredLevel")).intValue();
            sumSelf += self;
            Map<String, Object> row = new LinkedHashMap<>(s);
            row.put("gap", Math.max(required - self, 0));
            if (required > self) { gaps.add(row); sumGap += required - self; }
            else strengths.add(row);
        }
        gaps.sort((a, b) -> ((Integer) b.get("gap")).compareTo((Integer) a.get("gap")));
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("assessed", all.size());
        summary.put("gapCount", gaps.size());
        summary.put("strengthCount", strengths.size());
        summary.put("avgSelf", all.isEmpty() ? 0 : Math.round(sumSelf * 10.0 / all.size()) / 10.0);
        summary.put("totalGapPoints", sumGap);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("summary", summary);
        out.put("gaps", gaps);
        out.put("strengths", strengths);
        return out;
    }

    // ---------------------------------------------------------------- 4. recommendations
    @GetMapping("/me/recommendations")
    public Map<String, Object> recommendations() {
        long me = current.id();
        List<Map<String, Object>> courses = jdbc.queryForList(
                "SELECT c.course_id AS courseId, c.title AS title, c.provider AS provider, " +
                "c.course_level AS courseLevel, c.duration_hours AS durationHours, " +
                "s.skill_name AS skillName, (es.required_level - es.self_level) AS gap " +
                "FROM ed_employee_skills es " +
                "JOIN ed_courses c ON c.skill_id = es.skill_id " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "WHERE es.employee_id = ? AND EXISTS (SELECT 1 FROM ed_assessment_attempts aa WHERE aa.employee_id = es.employee_id AND aa.skill_id = es.skill_id) AND es.required_level > es.self_level " +
                "AND c.course_id NOT IN (SELECT course_id FROM ed_enrollments WHERE employee_id = ?) " +
                "ORDER BY gap DESC, c.duration_hours ASC LIMIT 12", me, me);

        List<Map<String, Object>> paths = jdbc.queryForList(
                "SELECT lp.path_id AS pathId, lp.title AS title, lp.description AS description, " +
                "COUNT(DISTINCT lpc.course_id) AS totalCourses, " +
                "COUNT(DISTINCT CASE WHEN en.status = 'COMPLETED' THEN lpc.course_id END) AS completedCourses, " +
                "(SELECT COUNT(*) FROM ed_learning_path_courses x " +
                "   JOIN ed_courses cc ON cc.course_id = x.course_id " +
                "   JOIN ed_employee_skills g ON g.skill_id = cc.skill_id AND g.employee_id = ? " +
                "        AND g.required_level > g.self_level " +
                "  WHERE x.path_id = lp.path_id) AS gapMatches " +
                "FROM ed_learning_paths lp " +
                "LEFT JOIN ed_learning_path_courses lpc ON lpc.path_id = lp.path_id " +
                "LEFT JOIN ed_enrollments en ON en.course_id = lpc.course_id AND en.employee_id = ? " +
                "GROUP BY lp.path_id, lp.title, lp.description " +
                "ORDER BY gapMatches DESC, lp.title", me, me);

        return Map.of("courses", courses, "paths", paths);
    }

    // ---------------------------------------------------------------- 5. learning progress
    @GetMapping("/me/learning")
    public Map<String, Object> learning() {
        long me = current.id();
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT en.enrollment_id AS enrollmentId, c.course_id AS courseId, c.title AS title, " +
                "c.provider AS provider, s.skill_name AS skillName, c.duration_hours AS durationHours, " +
                "en.progress_pct AS progressPct, en.status AS status, " +
                "en.started_at AS startedAt, en.completed_at AS completedAt " +
                "FROM ed_enrollments en JOIN ed_courses c ON c.course_id = en.course_id " +
                "JOIN ed_skills s ON s.skill_id = c.skill_id " +
                "WHERE en.employee_id = ? ORDER BY en.status ASC, en.started_at DESC", me);
        int inProgress = 0, completed = 0, hours = 0;
        for (Map<String, Object> i : items) {
            if ("COMPLETED".equals(i.get("status"))) {
                completed++;
                hours += ((Number) i.get("durationHours")).intValue();
            } else inProgress++;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enrolled", items.size());
        summary.put("inProgress", inProgress);
        summary.put("completed", completed);
        summary.put("hoursCompleted", hours);
        return Map.of("summary", summary, "items", items);
    }

    @PostMapping("/me/enroll")
    @Transactional
    public Map<String, Object> enroll(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long courseId = number(body, "courseId");
        enrollInCourse(me, courseId);
        return Map.of("message", "Enrolled");
    }

    @PostMapping("/me/enroll-path")
    @Transactional
    public Map<String, Object> enrollPath(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long pathId = number(body, "pathId");
        List<Long> ids = jdbc.queryForList(
                "SELECT course_id FROM ed_learning_path_courses WHERE path_id = ? ORDER BY seq", Long.class, pathId);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning path not found");
        for (Long id : ids) enrollInCourse(me, id);
        return Map.of("message", "Enrolled in " + ids.size() + " courses");
    }

    private void enrollInCourse(long me, long courseId) {
        List<String> t = jdbc.queryForList("SELECT title FROM ed_courses WHERE course_id = ?", String.class, courseId);
        if (t.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        int added = jdbc.update(
                "INSERT IGNORE INTO ed_enrollments (employee_id, course_id, progress_pct, status) VALUES (?,?,0,'IN_PROGRESS')",
                me, courseId);
        if (added > 0) notify(me, "You enrolled in \"" + t.get(0) + "\". Good luck!");
    }

    /** Body: {courseId, progressPct 0-100}. 100 marks the course completed. */
    @PutMapping("/me/progress")
    @Transactional
    public Map<String, Object> progress(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long courseId = number(body, "courseId");
        int pct = (int) number(body, "progressPct");
        if (pct < 0 || pct > 100) throw bad("Progress must be between 0 and 100");
        int changed = pct >= 100
                ? jdbc.update("UPDATE ed_enrollments SET progress_pct = 100, status = 'COMPLETED', " +
                              "completed_at = COALESCE(completed_at, NOW()) WHERE employee_id = ? AND course_id = ?", me, courseId)
                : jdbc.update("UPDATE ed_enrollments SET progress_pct = ?, status = 'IN_PROGRESS', completed_at = NULL " +
                              "WHERE employee_id = ? AND course_id = ?", pct, me, courseId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not enrolled in this course");
        if (pct >= 100) {
            List<String> t = jdbc.queryForList("SELECT title FROM ed_courses WHERE course_id = ?", String.class, courseId);
            if (!t.isEmpty()) notify(me, "Congratulations! You completed \"" + t.get(0) + "\". Update your skill rating to reflect what you learned.");
            checkAutoAchievements(me);
        }
        return Map.of("message", "Progress saved");
    }

    // ---------------------------------------------------------------- 6. mentorship & sharing
    @GetMapping("/me/mentorships")
    public List<Map<String, Object>> mentorships() {
        long me = current.id();
        return jdbc.queryForList(
                "SELECT m.mentorship_id AS mentorshipId, m.topic AS topic, m.status AS status, " +
                "m.created_at AS createdAt, " +
                "CASE WHEN m.mentor_id = ? THEN 'MENTOR' ELSE 'MENTEE' END AS myRole, " +
                "CONCAT(mt.first_name, ' ', mt.last_name) AS mentorName, " +
                "CONCAT(me.first_name, ' ', me.last_name) AS menteeName " +
                "FROM ed_mentorships m " +
                "JOIN employees mt ON mt.employee_id = m.mentor_id " +
                "JOIN employees me ON me.employee_id = m.mentee_id " +
                "WHERE m.mentor_id = ? OR m.mentee_id = ? ORDER BY m.created_at DESC", me, me, me);
    }

    /** Colleagues rated 4+ in skills where I have a gap. */
    @GetMapping("/me/mentor-suggestions")
    public List<Map<String, Object>> mentorSuggestions() {
        long me = current.id();
        return jdbc.queryForList(
                "SELECT e.employee_id AS mentorId, CONCAT(e.first_name, ' ', e.last_name) AS mentorName, " +
                "s.skill_name AS skillName, theirs.self_level AS level " +
                "FROM ed_employee_skills mine " +
                "JOIN ed_employee_skills theirs ON theirs.skill_id = mine.skill_id " +
                "     AND theirs.employee_id <> mine.employee_id AND theirs.self_level >= 4 " +
                "JOIN employees e ON e.employee_id = theirs.employee_id AND e.status = 'ACTIVE' " +
                "JOIN ed_skills s ON s.skill_id = mine.skill_id " +
                "WHERE mine.employee_id = ? AND mine.required_level > mine.self_level " +
                "AND NOT EXISTS (SELECT 1 FROM ed_mentorships m WHERE m.mentee_id = mine.employee_id " +
                "                AND m.mentor_id = e.employee_id AND m.status IN ('REQUESTED','ACTIVE')) " +
                "ORDER BY theirs.self_level DESC, s.skill_name LIMIT 10", me);
    }

    /** Body: {mentorId, topic} */
    @PostMapping("/me/mentorship-requests")
    @Transactional
    public Map<String, Object> requestMentor(@RequestBody Map<String, Object> body) {
        long me = current.id();
        long mentorId = number(body, "mentorId");
        String topic = text(body, "topic", 200, true);
        if (mentorId == me) throw bad("You cannot mentor yourself");
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE employee_id = ?", Integer.class, mentorId);
        if (exists == null || exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentor not found");
        Integer open = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_mentorships WHERE mentor_id = ? AND mentee_id = ? AND status IN ('REQUESTED','ACTIVE')",
                Integer.class, mentorId, me);
        if (open != null && open > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an open request with this mentor");
        jdbc.update("INSERT INTO ed_mentorships (mentor_id, mentee_id, topic, status) VALUES (?,?,?, 'REQUESTED')", mentorId, me, topic);
        notify(mentorId, "You have a new mentorship request: \"" + topic + "\".");
        return Map.of("message", "Request sent");
    }

    /** The mentor answers a request. Body: {status: ACTIVE | DECLINED | COMPLETED} */
    @PutMapping("/me/mentorships/{id}")
    @Transactional
    public Map<String, Object> answerMentorship(@PathVariable long id,
                                                @RequestBody Map<String, Object> body) {
        long me = current.id();
        String status = text(body, "status", 20, true);
        if (!List.of("ACTIVE", "DECLINED", "COMPLETED").contains(status)) throw bad("Invalid status");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT mentee_id FROM ed_mentorships WHERE mentorship_id = ? AND mentor_id = ?", id, me);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mentorship not found");
        jdbc.update("UPDATE ed_mentorships SET status = ? WHERE mentorship_id = ?", status, id);
        long mentee = ((Number) rows.get(0).get("mentee_id")).longValue();
        String verb = status.equals("ACTIVE") ? "accepted" : status.equals("DECLINED") ? "declined" : "marked complete";
        notify(mentee, "Your mentorship request was " + verb + ".");
        return Map.of("message", "Updated");
    }

    @GetMapping("/sessions")
    public List<Map<String, Object>> sessions() {
        long me = current.id();
        return jdbc.queryForList(
                "SELECT ks.session_id AS sessionId, ks.title AS title, ks.description AS description, " +
                "ks.scheduled_at AS scheduledAt, ks.host_id AS hostId, " +
                "CONCAT(h.first_name, ' ', h.last_name) AS hostName, " +
                "(SELECT COUNT(*) FROM ed_session_registrations r WHERE r.session_id = ks.session_id) AS attendees, " +
                "EXISTS (SELECT 1 FROM ed_session_registrations r WHERE r.session_id = ks.session_id AND r.employee_id = ?) AS registered " +
                "FROM ed_knowledge_sessions ks JOIN employees h ON h.employee_id = ks.host_id " +
                "WHERE ks.scheduled_at >= NOW() - INTERVAL 1 DAY ORDER BY ks.scheduled_at", me);
    }

    @PostMapping("/sessions/{id}/register")
    public Map<String, Object> register(@PathVariable long id) {
        long me = current.id();
        Integer ok = jdbc.queryForObject("SELECT COUNT(*) FROM ed_knowledge_sessions WHERE session_id = ?", Integer.class, id);
        if (ok == null || ok == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        jdbc.update("INSERT IGNORE INTO ed_session_registrations (session_id, employee_id) VALUES (?,?)", id, me);
        return Map.of("message", "Registered");
    }

    @DeleteMapping("/sessions/{id}/register")
    public Map<String, Object> unregister(@PathVariable long id) {
        jdbc.update("DELETE FROM ed_session_registrations WHERE session_id = ? AND employee_id = ?", id, current.id());
        return Map.of("message", "Unregistered");
    }

    /** Share knowledge: propose a session. Body: {title, description, scheduledAt "yyyy-MM-ddTHH:mm"} */
    @PostMapping("/sessions")
    @Transactional
    public Map<String, Object> proposeSession(@RequestBody Map<String, Object> body) {
        long me = current.id();
        String title = text(body, "title", 150, true);
        String description = text(body, "description", 500, false);
        LocalDateTime when;
        try {
            when = LocalDateTime.parse(text(body, "scheduledAt", 30, true));
        } catch (DateTimeParseException ex) {
            throw bad("Invalid date and time");
        }
        if (when.isBefore(LocalDateTime.now())) throw bad("Choose a future date and time");
        jdbc.update("INSERT INTO ed_knowledge_sessions (title, description, host_id, scheduled_at) VALUES (?,?,?,?)",
                title, description, me, when);
        // Same transaction = same connection, so LAST_INSERT_ID() is the session we just created.
        jdbc.update("INSERT IGNORE INTO ed_session_registrations (session_id, employee_id) VALUES (LAST_INSERT_ID(), ?)", me);
        return Map.of("message", "Session scheduled");
    }

    // ---------------------------------------------------------------- 7. notifications
    @GetMapping("/me/notifications")
    public Map<String, Object> notifications() {
        long me = current.id();
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT notification_id AS notificationId, message, is_read AS isRead, created_at AS createdAt " +
                "FROM ed_notifications WHERE employee_id = ? ORDER BY created_at DESC, notification_id DESC LIMIT 50", me);
        Integer unread = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_notifications WHERE employee_id = ? AND is_read = 0", Integer.class, me);
        return Map.of("unread", unread == null ? 0 : unread, "items", items);
    }

    @PutMapping("/me/notifications/{id}/read")
    public Map<String, Object> markRead(@PathVariable long id) {
        jdbc.update("UPDATE ed_notifications SET is_read = 1 WHERE notification_id = ? AND employee_id = ?", id, current.id());
        return Map.of("message", "Marked read");
    }

    @PutMapping("/me/notifications/read-all")
    public Map<String, Object> markAllRead() {
        jdbc.update("UPDATE ed_notifications SET is_read = 1 WHERE employee_id = ?", current.id());
        return Map.of("message", "All marked read");
    }

    // ---------------------------------------------------------------- 8. certifications
    @GetMapping("/me/certifications")
    public List<Map<String, Object>> certifications() {
        return jdbc.queryForList(
                "SELECT certification_id AS certificationId, title, issuer, credential_id AS credentialId, " +
                "issue_date AS issueDate, expiry_date AS expiryDate, credential_url AS credentialUrl, " +
                "created_at AS createdAt " +
                "FROM ed_certifications WHERE employee_id = ? ORDER BY issue_date DESC", current.id());
    }

    @PostMapping("/me/certifications")
    @Transactional
    public Map<String, Object> addCertification(@RequestBody Map<String, Object> body) {
        long me = current.id();
        String title = text(body, "title", 200, true);
        String issuer = text(body, "issuer", 150, false);
        String credId = text(body, "credentialId", 100, false);
        String issueDate = text(body, "issueDate", 20, false);
        String expiryDate = text(body, "expiryDate", 20, false);
        String url = text(body, "credentialUrl", 500, false);
        jdbc.update("INSERT INTO ed_certifications (employee_id, title, issuer, credential_id, issue_date, expiry_date, credential_url) " +
                    "VALUES (?,?,?,?,?,?,?)", me, title, issuer, credId,
                    issueDate != null && !issueDate.isBlank() ? issueDate : null,
                    expiryDate != null && !expiryDate.isBlank() ? expiryDate : null,
                    url);
        awardAchievement(me, "First Certification", "Earned your first professional certification", "MILESTONE");
        notify(me, "Certification \"" + title + "\" has been added to your profile.");
        return Map.of("message", "Certification added");
    }

    @PutMapping("/me/certifications/{id}")
    @Transactional
    public Map<String, Object> updateCertification(@PathVariable long id, @RequestBody Map<String, Object> body) {
        long me = current.id();
        String title = text(body, "title", 200, true);
        String issuer = text(body, "issuer", 150, false);
        String credId = text(body, "credentialId", 100, false);
        String issueDate = text(body, "issueDate", 20, false);
        String expiryDate = text(body, "expiryDate", 20, false);
        String url = text(body, "credentialUrl", 500, false);
        int changed = jdbc.update("UPDATE ed_certifications SET title=?, issuer=?, credential_id=?, issue_date=?, " +
                    "expiry_date=?, credential_url=? WHERE certification_id=? AND employee_id=?",
                    title, issuer, credId,
                    issueDate != null && !issueDate.isBlank() ? issueDate : null,
                    expiryDate != null && !expiryDate.isBlank() ? expiryDate : null,
                    url, id, me);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Certification not found");
        return Map.of("message", "Certification updated");
    }

    @DeleteMapping("/me/certifications/{id}")
    public Map<String, Object> deleteCertification(@PathVariable long id) {
        int changed = jdbc.update("DELETE FROM ed_certifications WHERE certification_id = ? AND employee_id = ?", id, current.id());
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Certification not found");
        return Map.of("message", "Certification deleted");
    }

    // ---------------------------------------------------------------- 9. achievements & progress
    @GetMapping("/me/achievements")
    public Map<String, Object> achievements() {
        long me = current.id();
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT achievement_id AS achievementId, title, description, badge_type AS badgeType, " +
                "earned_at AS earnedAt FROM ed_achievements WHERE employee_id = ? ORDER BY earned_at DESC", me);

        // Compute progress stats
        Integer totalSkills = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_employee_skills WHERE employee_id = ?", Integer.class, me);
        Integer assessedSkills = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT a.skill_id) FROM ed_assessment_attempts a WHERE a.employee_id = ?", Integer.class, me);
        Integer completedCourses = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_enrollments WHERE employee_id = ? AND status = 'COMPLETED'", Integer.class, me);
        Integer totalEnrolled = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_enrollments WHERE employee_id = ?", Integer.class, me);
        Integer totalCerts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_certifications WHERE employee_id = ?", Integer.class, me);

        // Skill improvement: compare first and latest assessment for each skill
        List<Map<String, Object>> improvements = jdbc.queryForList(
                "SELECT s.skill_name AS skillName, " +
                "first_a.score_pct AS firstScore, latest_a.score_pct AS latestScore, " +
                "(latest_a.score_pct - first_a.score_pct) AS improvement, " +
                "first_a.attempted_at AS firstDate, latest_a.attempted_at AS latestDate " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "JOIN ed_assessment_attempts first_a ON first_a.employee_id = es.employee_id AND first_a.skill_id = es.skill_id " +
                "  AND first_a.attempt_id = (SELECT MIN(a2.attempt_id) FROM ed_assessment_attempts a2 WHERE a2.employee_id = es.employee_id AND a2.skill_id = es.skill_id) " +
                "JOIN ed_assessment_attempts latest_a ON latest_a.employee_id = es.employee_id AND latest_a.skill_id = es.skill_id " +
                "  AND latest_a.attempt_id = (SELECT MAX(a3.attempt_id) FROM ed_assessment_attempts a3 WHERE a3.employee_id = es.employee_id AND a3.skill_id = es.skill_id) " +
                "WHERE es.employee_id = ? AND first_a.attempt_id <> latest_a.attempt_id " +
                "ORDER BY improvement DESC", me);

        // Learning milestones (completed courses timeline)
        List<Map<String, Object>> milestones = jdbc.queryForList(
                "SELECT c.title AS courseTitle, s.skill_name AS skillName, en.completed_at AS completedAt, " +
                "c.duration_hours AS durationHours " +
                "FROM ed_enrollments en JOIN ed_courses c ON c.course_id = en.course_id " +
                "JOIN ed_skills s ON s.skill_id = c.skill_id " +
                "WHERE en.employee_id = ? AND en.status = 'COMPLETED' ORDER BY en.completed_at DESC", me);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalSkills", totalSkills != null ? totalSkills : 0);
        stats.put("assessedSkills", assessedSkills != null ? assessedSkills : 0);
        stats.put("completedCourses", completedCourses != null ? completedCourses : 0);
        stats.put("totalEnrolled", totalEnrolled != null ? totalEnrolled : 0);
        stats.put("totalCertifications", totalCerts != null ? totalCerts : 0);
        stats.put("totalAchievements", items.size());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stats", stats);
        out.put("achievements", items);
        out.put("improvements", improvements);
        out.put("milestones", milestones);
        return out;
    }

    // ---------------------------------------------------------------- helpers
    private void notify(long employeeId, String message) {
        jdbc.update("INSERT INTO ed_notifications (employee_id, message) VALUES (?,?)", employeeId, message);
    }

    private void awardAchievement(long employeeId, String title, String description, String badgeType) {
        try {
            jdbc.update("INSERT IGNORE INTO ed_achievements (employee_id, title, description, badge_type) VALUES (?,?,?,?)",
                    employeeId, title, description, badgeType);
        } catch (Exception ignored) { /* duplicate key = already awarded */ }
    }

    /** Check and award auto-achievements after key actions. */
    private void checkAutoAchievements(long me) {
        Integer completed = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_enrollments WHERE employee_id = ? AND status = 'COMPLETED'", Integer.class, me);
        if (completed != null) {
            if (completed >= 1) awardAchievement(me, "First Course Complete", "Completed your first training course", "COURSE_COMPLETE");
            if (completed >= 3) awardAchievement(me, "Hat Trick", "Completed 3 training courses", "COURSE_COMPLETE");
            if (completed >= 5) awardAchievement(me, "Learning Champion", "Completed 5 training courses", "COURSE_COMPLETE");
            if (completed >= 10) awardAchievement(me, "Knowledge Master", "Completed 10 training courses", "COURSE_COMPLETE");
        }
        Integer assessed = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT skill_id) FROM ed_assessment_attempts WHERE employee_id = ?", Integer.class, me);
        if (assessed != null) {
            if (assessed >= 1) awardAchievement(me, "First Assessment", "Completed your first skill assessment", "MILESTONE");
            if (assessed >= 3) awardAchievement(me, "Self-Aware", "Assessed 3 different skills", "MILESTONE");
            if (assessed >= 5) awardAchievement(me, "Skill Explorer", "Assessed 5 different skills", "MILESTONE");
        }
        // Check for ace scores (90%+)
        Integer aces = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_assessment_attempts WHERE employee_id = ? AND score_pct >= 90", Integer.class, me);
        if (aces != null && aces >= 1) awardAchievement(me, "Assessment Ace", "Scored 90% or above on an assessment", "ASSESSMENT_ACE");
        // Check for skill mastery (level 5)
        Integer mastered = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_employee_skills WHERE employee_id = ? AND self_level = 5", Integer.class, me);
        if (mastered != null && mastered >= 1) awardAchievement(me, "Skill Mastery", "Reached expert level (5/5) in a skill", "SKILL_MASTERY");
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
