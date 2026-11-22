package com.wki.auth.hr;

import com.wki.auth.employee.CurrentEmployee;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

/**
 * Controller providing all backend APIs for the HR Specialist Dashboard.
 * Protected with RBAC in SecurityConfig (accessible only by HR_MANAGER and ADMIN).
 */
@RestController
@RequestMapping("/api/hr")
public class HrDashboardController {

    private final JdbcTemplate jdbc;
    private final CurrentEmployee current;

    public HrDashboardController(JdbcTemplate jdbc, CurrentEmployee current) {
        this.jdbc = jdbc;
        this.current = current;
    }

    // =========================================================================
    // A. DASHBOARD OVERVIEW
    // =========================================================================
    @GetMapping("/overview")
    public Map<String, Object> getOverview() {
        Map<String, Object> res = new HashMap<>();

        // 1. Total employees
        Integer totalEmployees = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employees WHERE status = 'ACTIVE'", Integer.class);
        res.put("totalEmployees", totalEmployees != null ? totalEmployees : 0);

        // 2. Skill coverage
        Integer totalSkills = jdbc.queryForObject("SELECT COUNT(*) FROM ed_skills", Integer.class);
        Integer assessedSkills = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT skill_id) FROM ed_employee_skills", Integer.class);
        int totalSkillsVal = totalSkills != null ? totalSkills : 1;
        int assessedSkillsVal = assessedSkills != null ? assessedSkills : 0;
        int coveragePct = Math.min(100, Math.round((float) assessedSkillsVal / totalSkillsVal * 100));
        res.put("totalSkills", totalSkillsVal);
        res.put("assessedSkills", assessedSkillsVal);
        res.put("skillCoveragePct", coveragePct);

        // 3. Critical gaps and total gaps
        Integer criticalGaps = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_employee_skills WHERE required_level > self_level AND (required_level - self_level) >= 2", Integer.class);
        Integer totalGaps = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_employee_skills WHERE required_level > self_level", Integer.class);
        res.put("criticalGaps", criticalGaps != null ? criticalGaps : 0);
        res.put("totalGaps", totalGaps != null ? totalGaps : 0);

        // 4. Learning participation & completion
        Integer activeLearners = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT employee_id) FROM ed_enrollments", Integer.class);
        Integer totalEnrollments = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_enrollments", Integer.class);
        Integer completedEnrollments = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ed_enrollments WHERE status = 'COMPLETED'", Integer.class);
        
        int completionRate = 0;
        if (totalEnrollments != null && totalEnrollments > 0 && completedEnrollments != null) {
            completionRate = Math.round((float) completedEnrollments / totalEnrollments * 100);
        }
        res.put("activeLearners", activeLearners != null ? activeLearners : 0);
        res.put("totalEnrollments", totalEnrollments != null ? totalEnrollments : 0);
        res.put("completedEnrollments", completedEnrollments != null ? completedEnrollments : 0);
        res.put("trainingCompletionRate", completionRate);

        // 5. Employees requiring attention (>= 2 critical gaps or 0% training progress)
        List<Map<String, Object>> needsAttention = jdbc.queryForList(
                "SELECT e.employee_id AS id, CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS name, " +
                "e.email, COALESCE(d.department_name, 'General') AS department, " +
                "COALESCE(ds.designation_name, 'Staff') AS designation, " +
                "SUM(CASE WHEN es.required_level - es.self_level >= 2 THEN 1 ELSE 0 END) AS criticalGaps, " +
                "SUM(CASE WHEN es.required_level > es.self_level THEN 1 ELSE 0 END) AS totalGaps " +
                "FROM employees e " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "LEFT JOIN designations ds ON ds.designation_id = e.designation_id " +
                "JOIN ed_employee_skills es ON es.employee_id = e.employee_id " +
                "WHERE e.status = 'ACTIVE' " +
                "GROUP BY e.employee_id, e.first_name, e.last_name, e.email, d.department_name, ds.designation_name " +
                "HAVING criticalGaps >= 1 OR totalGaps >= 2 " +
                "ORDER BY criticalGaps DESC, totalGaps DESC LIMIT 6"
        );
        res.put("needsAttention", needsAttention);

        // 6. Department-wise skill gap summary
        List<Map<String, Object>> deptGaps = jdbc.queryForList(
                "SELECT COALESCE(d.department_name, 'General') AS department, " +
                "COUNT(DISTINCT e.employee_id) AS totalEmployees, " +
                "SUM(CASE WHEN es.required_level > es.self_level THEN 1 ELSE 0 END) AS gapCount, " +
                "ROUND(AVG(CASE WHEN es.required_level > es.self_level THEN es.required_level - es.self_level ELSE 0 END), 2) AS avgGap " +
                "FROM employees e " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "LEFT JOIN ed_employee_skills es ON es.employee_id = e.employee_id " +
                "WHERE e.status = 'ACTIVE' " +
                "GROUP BY d.department_name ORDER BY gapCount DESC"
        );
        res.put("departmentOverview", deptGaps);

        // 7. Top 5 gap skills
        List<Map<String, Object>> topGaps = jdbc.queryForList(
                "SELECT s.skill_id AS skillId, s.skill_name AS skill, s.category, " +
                "COUNT(es.employee_id) AS employeesAffected, " +
                "ROUND(AVG(es.self_level), 1) AS avgProficiency, " +
                "ROUND(AVG(es.required_level), 1) AS avgRequired, " +
                "ROUND(AVG(es.required_level - es.self_level), 1) AS avgGap " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "WHERE es.required_level > es.self_level " +
                "GROUP BY s.skill_id, s.skill_name, s.category " +
                "ORDER BY employeesAffected DESC, avgGap DESC LIMIT 5"
        );
        res.put("topGaps", topGaps);

        // 8. Gap severity distribution
        Integer noGapCount = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT e.employee_id) FROM employees e " +
                "WHERE e.status = 'ACTIVE' AND e.employee_id NOT IN (" +
                "  SELECT DISTINCT employee_id FROM ed_employee_skills WHERE required_level > self_level" +
                ")", Integer.class);
        Integer minorGapCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (" +
                "  SELECT employee_id FROM ed_employee_skills WHERE required_level > self_level " +
                "  GROUP BY employee_id HAVING COUNT(*) BETWEEN 1 AND 2" +
                ") AS t", Integer.class);
        Integer criticalEmpCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM (" +
                "  SELECT employee_id FROM ed_employee_skills WHERE required_level > self_level " +
                "  GROUP BY employee_id HAVING COUNT(*) >= 3" +
                ") AS t", Integer.class);
        Map<String, Integer> gapDist = new HashMap<>();
        gapDist.put("noGap", noGapCount != null ? noGapCount : 0);
        gapDist.put("minorGap", minorGapCount != null ? minorGapCount : 0);
        gapDist.put("criticalGap", criticalEmpCount != null ? criticalEmpCount : 0);
        res.put("gapDistribution", gapDist);

        return res;
    }

    // =========================================================================
    // B. EMPLOYEE DIRECTORY
    // =========================================================================
    @GetMapping("/employees")
    public List<Map<String, Object>> getEmployees(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long designationId,
            @RequestParam(required = false) Long roleId) {

        StringBuilder sql = new StringBuilder(
                "SELECT e.employee_id AS id, CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS name, " +
                "e.email, e.status, e.last_login_at AS lastLoginAt, " +
                "COALESCE(d.department_name, 'General') AS department, d.department_id AS departmentId, " +
                "COALESCE(ds.designation_name, 'Staff') AS designation, ds.designation_id AS designationId, " +
                "COALESCE(r.role_name, 'EMPLOYEE') AS role, r.role_id AS roleId, " +
                "COUNT(DISTINCT es.skill_id) AS totalSkills, " +
                "SUM(CASE WHEN es.required_level > es.self_level THEN 1 ELSE 0 END) AS totalGaps, " +
                "SUM(CASE WHEN es.required_level - es.self_level >= 2 THEN 1 ELSE 0 END) AS criticalGaps, " +
                "COALESCE(ROUND(AVG(en.progress_pct), 0), 0) AS trainingProgress " +
                "FROM employees e " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "LEFT JOIN designations ds ON ds.designation_id = e.designation_id " +
                "LEFT JOIN roles r ON r.role_id = e.role_id " +
                "LEFT JOIN ed_employee_skills es ON es.employee_id = e.employee_id " +
                "LEFT JOIN ed_enrollments en ON en.employee_id = e.employee_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(e.first_name) LIKE ? OR LOWER(e.last_name) LIKE ? OR LOWER(e.email) LIKE ?) ");
            String q = "%" + search.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }
        if (departmentId != null) {
            sql.append("AND e.department_id = ? ");
            params.add(departmentId);
        }
        if (designationId != null) {
            sql.append("AND e.designation_id = ? ");
            params.add(designationId);
        }
        if (roleId != null) {
            sql.append("AND e.role_id = ? ");
            params.add(roleId);
        }

        sql.append("GROUP BY e.employee_id, e.first_name, e.last_name, e.email, e.status, e.last_login_at, " +
                   "d.department_name, d.department_id, ds.designation_name, ds.designation_id, r.role_name, r.role_id " +
                   "ORDER BY e.first_name ASC");

        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    @GetMapping("/employees/{id}")
    public Map<String, Object> getEmployeeDetails(@PathVariable long id) {
        List<Map<String, Object>> base = jdbc.queryForList(
                "SELECT e.employee_id AS id, e.first_name AS firstName, e.last_name AS lastName, " +
                "CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS fullName, e.email, e.status, " +
                "COALESCE(d.department_name, 'General') AS department, " +
                "COALESCE(ds.designation_name, 'Staff') AS designation, " +
                "COALESCE(r.role_name, 'EMPLOYEE') AS role, " +
                "p.phone, p.location, p.bio, p.linkedin, p.education, p.work_experience AS workExperience, " +
                "jr.role_name AS targetRole " +
                "FROM employees e " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "LEFT JOIN designations ds ON ds.designation_id = e.designation_id " +
                "LEFT JOIN roles r ON r.role_id = e.role_id " +
                "LEFT JOIN ed_employee_profiles p ON p.employee_id = e.employee_id " +
                "LEFT JOIN ed_job_roles jr ON jr.role_id = p.target_role_id " +
                "WHERE e.employee_id = ?", id);
        if (base.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
        Map<String, Object> emp = new HashMap<>(base.get(0));

        // Employee skills & gaps
        List<Map<String, Object>> skills = jdbc.queryForList(
                "SELECT s.skill_id AS skillId, s.skill_name AS skillName, s.category, " +
                "es.self_level AS selfLevel, es.required_level AS requiredLevel, " +
                "(es.required_level - es.self_level) AS gap, es.updated_at AS lastAssessedAt " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "WHERE es.employee_id = ? " +
                "ORDER BY gap DESC, s.skill_name ASC", id);
        emp.put("skills", skills);

        // Course enrollments
        List<Map<String, Object>> courses = jdbc.queryForList(
                "SELECT en.enrollment_id AS enrollmentId, c.title AS courseTitle, c.provider, " +
                "c.course_level AS level, c.duration_hours AS durationHours, " +
                "en.progress_pct AS progressPct, en.status, en.started_at AS startedAt, en.completed_at AS completedAt " +
                "FROM ed_enrollments en " +
                "JOIN ed_courses c ON c.course_id = en.course_id " +
                "WHERE en.employee_id = ? ORDER BY en.started_at DESC", id);
        emp.put("courses", courses);

        // Training programs
        List<Map<String, Object>> trainings = jdbc.queryForList(
                "SELECT te.enrollment_id AS enrollmentId, tp.title, tp.provider_name AS provider, " +
                "tp.mode, tp.duration_hours AS duration, te.enrollment_date AS enrollmentDate, " +
                "te.status, te.score, te.completion_date AS completionDate, te.feedback " +
                "FROM training_enrollments te " +
                "JOIN training_programs tp ON tp.training_id = te.training_id " +
                "WHERE te.employee_id = ? ORDER BY te.enrollment_date DESC", id);
        emp.put("trainings", trainings);

        // Learning path assignments
        List<Map<String, Object>> paths = jdbc.queryForList(
                "SELECT lpa.assignment_id AS assignmentId, lp.path_id AS pathId, lp.title AS pathTitle, " +
                "lpa.status, lpa.assigned_at AS assignedAt, lpa.target_completion_date AS targetDate " +
                "FROM ed_learning_path_assignments lpa " +
                "JOIN ed_learning_paths lp ON lp.path_id = lpa.path_id " +
                "WHERE lpa.employee_id = ? ORDER BY lpa.assigned_at DESC", id);
        emp.put("learningPaths", paths);

        return emp;
    }

    // =========================================================================
    // C. COMPETENCY FRAMEWORK & SKILLS
    // =========================================================================
    @GetMapping("/competency/skills")
    public List<Map<String, Object>> getSkills() {
        return jdbc.queryForList(
                "SELECT s.skill_id AS skillId, s.skill_name AS skillName, s.category, " +
                "s.default_required_level AS defaultRequiredLevel, " +
                "COUNT(DISTINCT es.employee_id) AS employeesCount, " +
                "COALESCE(ROUND(AVG(es.self_level), 1), 0.0) AS avgProficiency, " +
                "SUM(CASE WHEN es.required_level > es.self_level THEN 1 ELSE 0 END) AS gapsCount " +
                "FROM ed_skills s " +
                "LEFT JOIN ed_employee_skills es ON es.skill_id = s.skill_id " +
                "GROUP BY s.skill_id, s.skill_name, s.category, s.default_required_level " +
                "ORDER BY s.category ASC, s.skill_name ASC");
    }

    @GetMapping("/competency/categories")
    public List<Map<String, Object>> getSkillCategories() {
        return jdbc.queryForList(
                "SELECT category AS categoryName, COUNT(*) AS skillCount " +
                "FROM ed_skills GROUP BY category ORDER BY category ASC");
    }

    @PostMapping("/competency/skills")
    @Transactional
    public Map<String, Object> createSkill(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("skillName");
        String category = (String) body.get("category");
        Number reqLevel = (Number) body.get("defaultRequiredLevel");
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skill name is required");
        }
        int level = reqLevel != null ? reqLevel.intValue() : 3;
        category = category != null && !category.trim().isEmpty() ? category.trim() : "General";

        jdbc.update("INSERT INTO ed_skills (skill_name, category, default_required_level) VALUES (?, ?, ?)",
                name.trim(), category, level);

        Long skillId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("skillId", skillId != null ? skillId : 0, "message", "Skill created successfully");
    }

    @PutMapping("/competency/skills/{id}")
    @Transactional
    public Map<String, Object> updateSkill(@PathVariable long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("skillName");
        String category = (String) body.get("category");
        Number reqLevel = (Number) body.get("defaultRequiredLevel");

        jdbc.update("UPDATE ed_skills SET skill_name = COALESCE(?, skill_name), " +
                    "category = COALESCE(?, category), " +
                    "default_required_level = COALESCE(?, default_required_level) WHERE skill_id = ?",
                name, category, reqLevel != null ? reqLevel.intValue() : null, id);

        return Map.of("message", "Skill updated successfully");
    }

    @DeleteMapping("/competency/skills/{id}")
    @Transactional
    public Map<String, Object> deleteSkill(@PathVariable long id) {
        jdbc.update("DELETE FROM ed_job_role_skills WHERE skill_id = ?", id);
        jdbc.update("DELETE FROM ed_employee_skills WHERE skill_id = ?", id);
        jdbc.update("DELETE FROM ed_courses WHERE skill_id = ?", id);
        jdbc.update("DELETE FROM ed_skills WHERE skill_id = ?", id);
        return Map.of("message", "Skill deleted successfully");
    }

    // =========================================================================
    // D. ROLE REQUIREMENTS (TARGET ROLES)
    // =========================================================================
    @GetMapping("/roles")
    public List<Map<String, Object>> getTargetRoles() {
        List<Map<String, Object>> roles = jdbc.queryForList(
                "SELECT role_id AS roleId, role_name AS roleName, description FROM ed_job_roles ORDER BY role_name ASC");

        for (Map<String, Object> role : roles) {
            long roleId = ((Number) role.get("roleId")).longValue();
            List<Map<String, Object>> reqSkills = jdbc.queryForList(
                    "SELECT jrs.skill_id AS skillId, s.skill_name AS skillName, s.category, " +
                    "jrs.required_level AS requiredLevel, jrs.weight_pct AS weightPct " +
                    "FROM ed_job_role_skills jrs " +
                    "JOIN ed_skills s ON s.skill_id = jrs.skill_id " +
                    "WHERE jrs.role_id = ? ORDER BY jrs.weight_pct DESC", roleId);
            role.put("skills", reqSkills);

            // Count employees having target role
            Integer empCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ed_employee_profiles WHERE target_role_id = ?", Integer.class, roleId);
            role.put("assignedEmployees", empCount != null ? empCount : 0);
        }
        return roles;
    }

    @PostMapping("/roles")
    @Transactional
    public Map<String, Object> createTargetRole(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("roleName");
        String desc = (String) body.get("description");
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role name is required");
        }

        jdbc.update("INSERT INTO ed_job_roles (role_name, description) VALUES (?, ?)",
                name.trim(), desc != null ? desc.trim() : "");
        Long roleId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("roleId", roleId != null ? roleId : 0, "message", "Target role created successfully");
    }

    @PutMapping("/roles/{id}")
    @Transactional
    public Map<String, Object> updateTargetRole(@PathVariable long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("roleName");
        String desc = (String) body.get("description");
        jdbc.update("UPDATE ed_job_roles SET role_name = COALESCE(?, role_name), " +
                    "description = COALESCE(?, description) WHERE role_id = ?", name, desc, id);
        return Map.of("message", "Target role updated successfully");
    }

    @PostMapping("/roles/{id}/skills")
    @Transactional
    public Map<String, Object> setRoleSkill(@PathVariable long id, @RequestBody Map<String, Object> body) {
        Number skillId = (Number) body.get("skillId");
        Number reqLevel = (Number) body.get("requiredLevel");
        Number weight = (Number) body.get("weightPct");

        if (skillId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "skillId is required");
        int rLevel = reqLevel != null ? reqLevel.intValue() : 3;
        int wPct = weight != null ? weight.intValue() : 25;

        jdbc.update(
                "INSERT INTO ed_job_role_skills (role_id, skill_id, required_level, weight_pct) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE required_level = VALUES(required_level), weight_pct = VALUES(weight_pct)",
                id, skillId.longValue(), rLevel, wPct);

        return Map.of("message", "Role skill requirement saved successfully");
    }

    @DeleteMapping("/roles/{id}/skills/{skillId}")
    @Transactional
    public Map<String, Object> removeRoleSkill(@PathVariable long id, @PathVariable long skillId) {
        jdbc.update("DELETE FROM ed_job_role_skills WHERE role_id = ? AND skill_id = ?", id, skillId);
        return Map.of("message", "Skill removed from target role");
    }

    @DeleteMapping("/roles/{id}")
    @Transactional
    public Map<String, Object> deleteTargetRole(@PathVariable long id) {
        jdbc.update("DELETE FROM ed_job_role_skills WHERE role_id = ?", id);
        jdbc.update("UPDATE ed_employee_profiles SET target_role_id = NULL WHERE target_role_id = ?", id);
        jdbc.update("DELETE FROM ed_job_roles WHERE role_id = ?", id);
        return Map.of("message", "Target role deleted successfully");
    }

    // =========================================================================
    // E. ORGANISATION SKILL GAP ANALYSIS
    // =========================================================================
    @GetMapping("/gaps/analysis")
    public Map<String, Object> getGapAnalysis() {
        Map<String, Object> res = new HashMap<>();

        // Detailed skill-by-skill gap matrix across whole organisation
        List<Map<String, Object>> skillsGaps = jdbc.queryForList(
                "SELECT s.skill_id AS skillId, s.skill_name AS skill, s.category, " +
                "ROUND(AVG(es.self_level), 1) AS orgAvg, " +
                "ROUND(AVG(es.required_level), 1) AS requiredLevel, " +
                "ROUND(AVG(GREATEST(0, es.required_level - es.self_level)), 1) AS avgGap, " +
                "SUM(CASE WHEN es.required_level > es.self_level THEN 1 ELSE 0 END) AS employeesAffected, " +
                "COUNT(es.employee_id) AS totalAssessed, " +
                "CASE " +
                "  WHEN AVG(es.required_level - es.self_level) >= 1.5 OR SUM(CASE WHEN es.required_level - es.self_level >= 2 THEN 1 ELSE 0 END) >= 2 THEN 'critical' " +
                "  WHEN AVG(es.required_level - es.self_level) >= 0.8 THEN 'medium' " +
                "  ELSE 'low' " +
                "END AS severity " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "GROUP BY s.skill_id, s.skill_name, s.category " +
                "HAVING employeesAffected > 0 " +
                "ORDER BY employeesAffected DESC, avgGap DESC");
        res.put("skillsGaps", skillsGaps);

        // Department-wise analysis
        List<Map<String, Object>> deptAnalysis = jdbc.queryForList(
                "SELECT COALESCE(d.department_name, 'General') AS department, " +
                "COUNT(DISTINCT e.employee_id) AS employeeCount, " +
                "COUNT(DISTINCT es.skill_id) AS assessedSkills, " +
                "ROUND(AVG(es.self_level), 1) AS avgProficiency, " +
                "SUM(CASE WHEN es.required_level > es.self_level THEN 1 ELSE 0 END) AS totalGaps, " +
                "ROUND(AVG(GREATEST(0, es.required_level - es.self_level)), 1) AS avgGap " +
                "FROM employees e " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "LEFT JOIN ed_employee_skills es ON es.employee_id = e.employee_id " +
                "WHERE e.status = 'ACTIVE' " +
                "GROUP BY d.department_name ORDER BY totalGaps DESC");
        res.put("departmentAnalysis", deptAnalysis);

        // Target role readiness analysis
        List<Map<String, Object>> roleAnalysis = jdbc.queryForList(
                "SELECT jr.role_id AS roleId, jr.role_name AS roleName, " +
                "COUNT(DISTINCT p.employee_id) AS aspiringCount, " +
                "COUNT(DISTINCT jrs.skill_id) AS requiredSkillsCount " +
                "FROM ed_job_roles jr " +
                "LEFT JOIN ed_employee_profiles p ON p.target_role_id = jr.role_id " +
                "LEFT JOIN ed_job_role_skills jrs ON jrs.role_id = jr.role_id " +
                "GROUP BY jr.role_id, jr.role_name ORDER BY aspiringCount DESC");
        res.put("roleAnalysis", roleAnalysis);

        // Overall proficiency level distribution (1 to 5)
        List<Map<String, Object>> profDist = jdbc.queryForList(
                "SELECT self_level AS level, COUNT(*) AS count " +
                "FROM ed_employee_skills GROUP BY self_level ORDER BY self_level ASC");
        res.put("proficiencyDistribution", profDist);

        return res;
    }

    // =========================================================================
    // F. LEARNING CATALOGUE
    // =========================================================================
    @GetMapping("/courses")
    public List<Map<String, Object>> getCourses() {
        return jdbc.queryForList(
                "SELECT c.course_id AS courseId, c.title, c.provider, c.course_level AS level, " +
                "c.duration_hours AS durationHours, c.description, c.url, c.is_active AS isActive, " +
                "s.skill_id AS skillId, s.skill_name AS skillName, s.category, " +
                "COUNT(DISTINCT en.employee_id) AS enrolledCount, " +
                "SUM(CASE WHEN en.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount " +
                "FROM ed_courses c " +
                "LEFT JOIN ed_skills s ON s.skill_id = c.skill_id " +
                "LEFT JOIN ed_enrollments en ON en.course_id = c.course_id " +
                "GROUP BY c.course_id, c.title, c.provider, c.course_level, c.duration_hours, " +
                "c.description, c.url, c.is_active, s.skill_id, s.skill_name, s.category " +
                "ORDER BY c.title ASC");
    }

    @PostMapping("/courses")
    @Transactional
    public Map<String, Object> createCourse(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String provider = (String) body.get("provider");
        Number skillId = (Number) body.get("skillId");
        Number level = (Number) body.get("level");
        Number duration = (Number) body.get("durationHours");
        String desc = (String) body.get("description");
        String url = (String) body.get("url");

        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course title is required");
        }

        jdbc.update(
                "INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours, description, url, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 1)",
                title.trim(),
                provider != null ? provider.trim() : "Internal Academy",
                skillId != null ? skillId.longValue() : 1L,
                level != null ? level.intValue() : 3,
                duration != null ? duration.intValue() : 5,
                desc,
                url
        );
        Long courseId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("courseId", courseId != null ? courseId : 0, "message", "Course added to catalogue successfully");
    }

    @PutMapping("/courses/{id}")
    @Transactional
    public Map<String, Object> updateCourse(@PathVariable long id, @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String provider = (String) body.get("provider");
        Number skillId = (Number) body.get("skillId");
        Number level = (Number) body.get("level");
        Number duration = (Number) body.get("durationHours");
        String desc = (String) body.get("description");
        String url = (String) body.get("url");
        Boolean active = (Boolean) body.get("isActive");

        jdbc.update(
                "UPDATE ed_courses SET " +
                "title = COALESCE(?, title), " +
                "provider = COALESCE(?, provider), " +
                "skill_id = COALESCE(?, skill_id), " +
                "course_level = COALESCE(?, course_level), " +
                "duration_hours = COALESCE(?, duration_hours), " +
                "description = COALESCE(?, description), " +
                "url = COALESCE(?, url), " +
                "is_active = COALESCE(?, is_active) WHERE course_id = ?",
                title, provider, skillId != null ? skillId.longValue() : null,
                level != null ? level.intValue() : null,
                duration != null ? duration.intValue() : null,
                desc, url, active != null ? (active ? 1 : 0) : null, id
        );
        return Map.of("message", "Course updated successfully");
    }

    @DeleteMapping("/courses/{id}")
    @Transactional
    public Map<String, Object> deleteCourse(@PathVariable long id) {
        jdbc.update("DELETE FROM ed_learning_path_courses WHERE course_id = ?", id);
        jdbc.update("DELETE FROM ed_enrollments WHERE course_id = ?", id);
        jdbc.update("DELETE FROM ed_courses WHERE course_id = ?", id);
        return Map.of("message", "Course removed from catalogue");
    }

    // =========================================================================
    // G. LEARNING PATHS
    // =========================================================================
    @GetMapping("/learning-paths")
    public List<Map<String, Object>> getLearningPaths() {
        List<Map<String, Object>> paths = jdbc.queryForList(
                "SELECT path_id AS pathId, title, description FROM ed_learning_paths ORDER BY title ASC");

        for (Map<String, Object> p : paths) {
            long pathId = ((Number) p.get("pathId")).longValue();
            List<Map<String, Object>> courses = jdbc.queryForList(
                    "SELECT c.course_id AS courseId, c.title AS courseTitle, c.course_level AS level, " +
                    "c.duration_hours AS durationHours, lpc.seq " +
                    "FROM ed_learning_path_courses lpc " +
                    "JOIN ed_courses c ON c.course_id = lpc.course_id " +
                    "WHERE lpc.path_id = ? ORDER BY lpc.seq ASC", pathId);
            p.put("courses", courses);

            Integer assignedCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ed_learning_path_assignments WHERE path_id = ?", Integer.class, pathId);
            p.put("assignedCount", assignedCount != null ? assignedCount : 0);
        }
        return paths;
    }

    @PostMapping("/learning-paths")
    @Transactional
    public Map<String, Object> createLearningPath(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String desc = (String) body.get("description");
        @SuppressWarnings("unchecked")
        List<Number> courseIds = (List<Number>) body.get("courseIds");

        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Learning path title is required");
        }

        jdbc.update("INSERT INTO ed_learning_paths (title, description) VALUES (?, ?)",
                title.trim(), desc != null ? desc.trim() : "");
        Long pathId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        if (courseIds != null && pathId != null) {
            int seq = 1;
            for (Number cid : courseIds) {
                jdbc.update("INSERT INTO ed_learning_path_courses (path_id, course_id, seq) VALUES (?, ?, ?)",
                        pathId, cid.longValue(), seq++);
            }
        }
        return Map.of("pathId", pathId != null ? pathId : 0, "message", "Learning path created successfully");
    }

    @PostMapping("/learning-paths/assign")
    @Transactional
    public Map<String, Object> assignLearningPath(@RequestBody Map<String, Object> body) {
        Number pathId = (Number) body.get("pathId");
        Number empId = (Number) body.get("employeeId");
        Number deptId = (Number) body.get("departmentId");
        String targetDateStr = (String) body.get("targetDate");

        if (pathId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pathId is required");
        long hrId = current.id();
        LocalDate targetDate = targetDateStr != null && !targetDateStr.isBlank() ? LocalDate.parse(targetDateStr) : null;

        List<Long> targetEmployees = new ArrayList<>();
        if (empId != null) {
            targetEmployees.add(empId.longValue());
        } else if (deptId != null) {
            targetEmployees.addAll(jdbc.queryForList(
                    "SELECT employee_id FROM employees WHERE department_id = ? AND status = 'ACTIVE'",
                    Long.class, deptId.longValue()));
        }

        String pathTitle = jdbc.queryForObject("SELECT title FROM ed_learning_paths WHERE path_id = ?", String.class, pathId.longValue());

        for (Long eid : targetEmployees) {
            jdbc.update(
                    "INSERT INTO ed_learning_path_assignments (path_id, employee_id, assigned_by, status, target_completion_date) " +
                    "VALUES (?, ?, ?, 'ASSIGNED', ?)",
                    pathId.longValue(), eid, hrId, targetDate);

            // Also auto-enroll in path courses if not already enrolled
            List<Long> coursesInPath = jdbc.queryForList(
                    "SELECT course_id FROM ed_learning_path_courses WHERE path_id = ? ORDER BY seq ASC",
                    Long.class, pathId.longValue());
            for (Long cid : coursesInPath) {
                jdbc.update(
                        "INSERT IGNORE INTO ed_enrollments (employee_id, course_id, progress_pct, status, started_at) " +
                        "VALUES (?, ?, 0, 'IN_PROGRESS', NOW())", eid, cid);
            }

            // Create notification for employee
            jdbc.update("INSERT INTO ed_notifications (employee_id, message) VALUES (?, ?)",
                    eid, "HR has assigned you the learning path: " + (pathTitle != null ? pathTitle : "Track"));
        }

        return Map.of("assignedCount", targetEmployees.size(), "message", "Learning path assigned successfully");
    }

    // =========================================================================
    // H. TRAINING PROGRAMMES
    // =========================================================================
    @GetMapping("/training-programs")
    public List<Map<String, Object>> getTrainingPrograms() {
        return jdbc.queryForList(
                "SELECT tp.training_id AS trainingId, tp.title, tp.provider_type AS providerType, " +
                "tp.provider_name AS providerName, tp.description, tp.start_date AS startDate, " +
                "tp.end_date AS endDate, tp.mode, tp.duration_hours AS durationHours, " +
                "COALESCE(s.skill_name, 'General') AS skillName, " +
                "COUNT(te.enrollment_id) AS enrolledCount, " +
                "SUM(CASE WHEN te.status = 'completed' THEN 1 ELSE 0 END) AS completedCount, " +
                "COALESCE(ROUND(AVG(te.score), 1), 0.0) AS avgScore " +
                "FROM training_programs tp " +
                "LEFT JOIN skills s ON s.skill_id = tp.skill_id " +
                "LEFT JOIN training_enrollments te ON te.training_id = tp.training_id " +
                "GROUP BY tp.training_id, tp.title, tp.provider_type, tp.provider_name, " +
                "tp.description, tp.start_date, tp.end_date, tp.mode, tp.duration_hours, s.skill_name " +
                "ORDER BY tp.start_date DESC");
    }

    @PostMapping("/training-programs")
    @Transactional
    public Map<String, Object> createTrainingProgram(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String providerType = (String) body.get("providerType");
        String providerName = (String) body.get("providerName");
        String desc = (String) body.get("description");
        String startDate = (String) body.get("startDate");
        String endDate = (String) body.get("endDate");
        String mode = (String) body.get("mode");
        Number duration = (Number) body.get("durationHours");
        Number skillId = (Number) body.get("skillId");

        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Training title is required");
        }

        long hrId = current.id();
        jdbc.update(
                "INSERT INTO training_programs (title, provider_type, provider_name, description, start_date, end_date, mode, duration_hours, skill_id, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                title.trim(),
                providerType != null ? providerType : "Internal",
                providerName != null ? providerName : "HR Learning & Development",
                desc, startDate, endDate, mode != null ? mode : "Online",
                duration != null ? duration.doubleValue() : 10.0,
                skillId != null ? skillId.intValue() : 1,
                hrId
        );
        Long tId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("trainingId", tId != null ? tId : 0, "message", "Training program created successfully");
    }

    @PostMapping("/training-programs/{id}/assign")
    @Transactional
    public Map<String, Object> assignTrainingProgram(@PathVariable int id, @RequestBody Map<String, Object> body) {
        Number empId = (Number) body.get("employeeId");
        Number deptId = (Number) body.get("departmentId");

        List<Integer> targetEmps = new ArrayList<>();
        if (empId != null) {
            targetEmps.add(empId.intValue());
        } else if (deptId != null) {
            targetEmps.addAll(jdbc.queryForList(
                    "SELECT employee_id FROM employees WHERE department_id = ? AND status = 'ACTIVE'",
                    Integer.class, deptId.intValue()));
        }

        String progTitle = jdbc.queryForObject("SELECT title FROM training_programs WHERE training_id = ?", String.class, id);

        for (Integer eid : targetEmps) {
            jdbc.update(
                    "INSERT IGNORE INTO training_enrollments (employee_id, training_id, enrollment_date, status) " +
                    "VALUES (?, ?, CURDATE(), 'enrolled')", eid, id);

            jdbc.update("INSERT INTO ed_notifications (employee_id, message) VALUES (?, ?)",
                    eid.longValue(), "You have been enrolled into training programme: " + (progTitle != null ? progTitle : "Training"));
        }
        return Map.of("assignedCount", targetEmps.size(), "message", "Employees enrolled into training programme successfully");
    }

    // =========================================================================
    // I. MENTOR DIRECTORY & MATCHING
    // =========================================================================
    @GetMapping("/mentors")
    public List<Map<String, Object>> getMentors() {
        return jdbc.queryForList(
                "SELECT mp.employee_id AS mentorId, CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS name, " +
                "e.email, COALESCE(d.department_name, 'Engineering') AS department, " +
                "mp.headline, mp.accepting, " +
                "COUNT(DISTINCT m.mentorship_id) AS activeMentees " +
                "FROM ed_mentor_profiles mp " +
                "JOIN employees e ON e.employee_id = mp.employee_id " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "LEFT JOIN ed_mentorships m ON m.mentor_id = mp.employee_id AND m.status = 'ACTIVE' " +
                "GROUP BY mp.employee_id, e.first_name, e.last_name, e.email, d.department_name, mp.headline, mp.accepting " +
                "ORDER BY mp.accepting DESC, activeMentees ASC");
    }

    @GetMapping("/mentors/matches")
    public List<Map<String, Object>> getMentorMatches() {
        // Find employees with gaps, and find mentors who are strong (self_level >= 4) in that skill
        return jdbc.queryForList(
                "SELECT es.employee_id AS menteeId, CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS menteeName, " +
                "e.email AS menteeEmail, COALESCE(d.department_name, 'General') AS menteeDept, " +
                "s.skill_id AS skillId, s.skill_name AS gapSkill, " +
                "es.self_level AS menteeLevel, es.required_level AS requiredLevel, " +
                "mp.employee_id AS mentorId, CONCAT(me.first_name, ' ', COALESCE(me.last_name, '')) AS mentorName, " +
                "me.email AS mentorEmail, mp.headline AS mentorHeadline, ms.self_level AS mentorSkillLevel " +
                "FROM ed_employee_skills es " +
                "JOIN employees e ON e.employee_id = es.employee_id " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "JOIN ed_employee_skills ms ON ms.skill_id = es.skill_id AND ms.self_level >= 4 AND ms.employee_id <> es.employee_id " +
                "JOIN ed_mentor_profiles mp ON mp.employee_id = ms.employee_id AND mp.accepting = 1 " +
                "JOIN employees me ON me.employee_id = mp.employee_id " +
                "WHERE es.required_level > es.self_level " +
                "LIMIT 12");
    }

    @PostMapping("/mentors/assign")
    @Transactional
    public Map<String, Object> assignMentor(@RequestBody Map<String, Object> body) {
        Number mentorId = (Number) body.get("mentorId");
        Number menteeId = (Number) body.get("menteeId");
        String topic = (String) body.get("topic");

        if (mentorId == null || menteeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mentorId and menteeId are required");
        }

        jdbc.update(
                "INSERT INTO ed_mentorships (mentor_id, mentee_id, topic, status) VALUES (?, ?, ?, 'ACTIVE')",
                mentorId.longValue(), menteeId.longValue(), topic != null ? topic : "Workforce Skill Coaching");

        // Notify mentee and mentor
        jdbc.update("INSERT INTO ed_notifications (employee_id, message) VALUES (?, ?)",
                menteeId.longValue(), "HR has connected you with a mentor: " + topic);
        jdbc.update("INSERT INTO ed_notifications (employee_id, message) VALUES (?, ?)",
                mentorId.longValue(), "HR has assigned you a new mentee for: " + topic);

        return Map.of("message", "Mentor paired successfully");
    }

    // =========================================================================
    // J. LEARNING PROGRESS
    // =========================================================================
    @GetMapping("/learning-progress")
    public Map<String, Object> getLearningProgress() {
        Map<String, Object> res = new HashMap<>();

        // Individual training and course statuses with overdue flags
        List<Map<String, Object>> activities = jdbc.queryForList(
                "SELECT en.enrollment_id AS id, 'Course' AS type, " +
                "CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS employeeName, " +
                "e.email, COALESCE(d.department_name, 'General') AS department, " +
                "c.title AS activityTitle, en.progress_pct AS progressPct, en.status, " +
                "en.started_at AS startedDate, en.completed_at AS completedDate, " +
                "CASE WHEN en.status <> 'COMPLETED' AND DATEDIFF(NOW(), en.started_at) > 30 THEN 1 ELSE 0 END AS isOverdue " +
                "FROM ed_enrollments en " +
                "JOIN employees e ON e.employee_id = en.employee_id " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "JOIN ed_courses c ON c.course_id = en.course_id " +
                "ORDER BY isOverdue DESC, en.started_at DESC LIMIT 30");
        res.put("activities", activities);

        // Progress summary by department
        List<Map<String, Object>> deptProgress = jdbc.queryForList(
                "SELECT COALESCE(d.department_name, 'General') AS department, " +
                "COUNT(en.enrollment_id) AS totalEnrollments, " +
                "SUM(CASE WHEN en.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount, " +
                "COALESCE(ROUND(AVG(en.progress_pct), 0), 0) AS avgProgress " +
                "FROM employees e " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "LEFT JOIN ed_enrollments en ON en.employee_id = e.employee_id " +
                "WHERE e.status = 'ACTIVE' " +
                "GROUP BY d.department_name ORDER BY avgProgress DESC");
        res.put("deptProgress", deptProgress);

        return res;
    }

    // =========================================================================
    // K. FEEDBACK & EFFECTIVENESS
    // =========================================================================
    @GetMapping("/feedback-effectiveness")
    public Map<String, Object> getFeedbackAndEffectiveness() {
        Map<String, Object> res = new HashMap<>();

        // Learner feedback and training ratings
        List<Map<String, Object>> feedbackList = jdbc.queryForList(
                "SELECT te.enrollment_id AS id, tp.title AS trainingTitle, " +
                "CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS learnerName, " +
                "te.completion_date AS completionDate, te.score, te.feedback " +
                "FROM training_enrollments te " +
                "JOIN training_programs tp ON tp.training_id = te.training_id " +
                "JOIN employees e ON e.employee_id = te.employee_id " +
                "WHERE te.feedback IS NOT NULL AND te.feedback <> '' " +
                "ORDER BY te.completion_date DESC");
        res.put("feedbackList", feedbackList);

        // Training program outcomes (average score, completion pct)
        List<Map<String, Object>> outcomes = jdbc.queryForList(
                "SELECT tp.training_id AS trainingId, tp.title, " +
                "COUNT(te.enrollment_id) AS enrolledCount, " +
                "SUM(CASE WHEN te.status = 'completed' THEN 1 ELSE 0 END) AS completedCount, " +
                "ROUND(SUM(CASE WHEN te.status = 'completed' THEN 1 ELSE 0 END) / COUNT(te.enrollment_id) * 100, 0) AS completionPct, " +
                "COALESCE(ROUND(AVG(te.score), 1), 0.0) AS avgScore " +
                "FROM training_programs tp " +
                "JOIN training_enrollments te ON te.training_id = tp.training_id " +
                "GROUP BY tp.training_id, tp.title " +
                "ORDER BY avgScore DESC");
        res.put("outcomes", outcomes);

        // Pre vs Post Assessment Scores Comparison where available
        List<Map<String, Object>> prePost = jdbc.queryForList(
                "SELECT s.skill_name AS skill, CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS learner, " +
                "MIN(aa.score_pct) AS preScore, MAX(aa.score_pct) AS postScore, " +
                "ROUND(MAX(aa.score_pct) - MIN(aa.score_pct), 1) AS improvement " +
                "FROM ed_assessment_attempts aa " +
                "JOIN ed_skills s ON s.skill_id = aa.skill_id " +
                "JOIN employees e ON e.employee_id = aa.employee_id " +
                "GROUP BY s.skill_name, e.employee_id, e.first_name, e.last_name " +
                "HAVING COUNT(aa.attempt_id) >= 2 AND improvement >= 0 " +
                "ORDER BY improvement DESC LIMIT 10");
        res.put("prePostComparison", prePost);

        return res;
    }

    // =========================================================================
    // L. REPORTS & ANALYTICS
    // =========================================================================
    @GetMapping("/reports")
    public Map<String, Object> getReportsData() {
        Map<String, Object> rep = new HashMap<>();

        // 1. Workforce Capability Index (avg proficiency across all measured skills)
        Double avgProficiency = jdbc.queryForObject(
                "SELECT AVG(self_level) FROM ed_employee_skills", Double.class);
        rep.put("capabilityIndex", avgProficiency != null ? Math.round(avgProficiency * 20.0) : 65); // Out of 100

        // 2. Department-wise skill capability rankings
        List<Map<String, Object>> deptRankings = jdbc.queryForList(
                "SELECT COALESCE(d.department_name, 'General') AS department, " +
                "COUNT(DISTINCT e.employee_id) AS employees, " +
                "ROUND(COALESCE(AVG(es.self_level), 0), 1) AS avgSkillLevel, " +
                "SUM(CASE WHEN es.required_level > es.self_level THEN 1 ELSE 0 END) AS totalGaps, " +
                "ROUND(COALESCE(AVG(en.progress_pct), 0), 0) AS avgTrainingProgress " +
                "FROM employees e " +
                "LEFT JOIN departments d ON d.department_id = e.department_id " +
                "LEFT JOIN ed_employee_skills es ON es.employee_id = e.employee_id " +
                "LEFT JOIN ed_enrollments en ON en.employee_id = e.employee_id " +
                "WHERE e.status = 'ACTIVE' " +
                "GROUP BY d.department_name ORDER BY avgSkillLevel DESC");
        rep.put("departmentRankings", deptRankings);

        // 3. Critical skill shortage rankings
        List<Map<String, Object>> shortageRankings = jdbc.queryForList(
                "SELECT s.skill_name AS skill, s.category, " +
                "COUNT(es.employee_id) AS affectedEmployees, " +
                "ROUND(AVG(es.required_level - es.self_level), 1) AS gapSeverity " +
                "FROM ed_employee_skills es " +
                "JOIN ed_skills s ON s.skill_id = es.skill_id " +
                "WHERE es.required_level > es.self_level " +
                "GROUP BY s.skill_name, s.category ORDER BY affectedEmployees DESC, gapSeverity DESC LIMIT 8");
        rep.put("shortageRankings", shortageRankings);

        return rep;
    }

    // =========================================================================
    // M. NOTIFICATIONS & ANNOUNCEMENTS
    // =========================================================================
    @GetMapping("/notifications")
    public List<Map<String, Object>> getNotificationsHistory() {
        return jdbc.queryForList(
                "SELECT a.announcement_id AS id, a.title, a.message, a.target_type AS targetType, " +
                "a.created_at AS createdAt, CONCAT(e.first_name, ' ', COALESCE(e.last_name, '')) AS senderName " +
                "FROM hr_announcements a " +
                "JOIN employees e ON e.employee_id = a.sent_by " +
                "ORDER BY a.created_at DESC LIMIT 20");
    }

    @PostMapping("/notifications/send")
    @Transactional
    public Map<String, Object> sendNotification(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String message = (String) body.get("message");
        String targetType = (String) body.get("targetType"); // ALL, DEPARTMENT, ROLE, EMPLOYEE
        Number targetId = (Number) body.get("targetId");

        if (title == null || message == null || title.isBlank() || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title and message are required");
        }
        if (targetType == null) targetType = "ALL";

        long hrId = current.id();
        jdbc.update(
                "INSERT INTO hr_announcements (title, message, target_type, target_id, sent_by) VALUES (?, ?, ?, ?, ?)",
                title.trim(), message.trim(), targetType, targetId != null ? targetId.longValue() : null, hrId);

        // Deliver to recipients' individual notifications inbox
        List<Long> recipients = new ArrayList<>();
        if ("ALL".equalsIgnoreCase(targetType)) {
            recipients.addAll(jdbc.queryForList(
                    "SELECT employee_id FROM employees WHERE status = 'ACTIVE'", Long.class));
        } else if ("DEPARTMENT".equalsIgnoreCase(targetType) && targetId != null) {
            recipients.addAll(jdbc.queryForList(
                    "SELECT employee_id FROM employees WHERE department_id = ? AND status = 'ACTIVE'",
                    Long.class, targetId.longValue()));
        } else if ("EMPLOYEE".equalsIgnoreCase(targetType) && targetId != null) {
            recipients.add(targetId.longValue());
        }

        for (Long recipientId : recipients) {
            jdbc.update("INSERT INTO ed_notifications (employee_id, message) VALUES (?, ?)",
                    recipientId, "[" + title.trim() + "] " + message.trim());
        }

        return Map.of("recipientsCount", recipients.size(), "message", "Notification broadcasted successfully");
    }

    // Helper: list departments and designations for dropdowns
    @GetMapping("/meta")
    public Map<String, Object> getMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("departments", jdbc.queryForList(
                "SELECT department_id AS id, department_name AS name FROM departments ORDER BY department_name ASC"));
        meta.put("designations", jdbc.queryForList(
                "SELECT designation_id AS id, designation_name AS name FROM designations ORDER BY designation_name ASC"));
        meta.put("roles", jdbc.queryForList(
                "SELECT role_id AS id, role_name AS name FROM roles ORDER BY role_name ASC"));
        meta.put("skills", jdbc.queryForList(
                "SELECT skill_id AS id, skill_name AS name, category FROM ed_skills ORDER BY skill_name ASC"));
        return meta;
    }
}

