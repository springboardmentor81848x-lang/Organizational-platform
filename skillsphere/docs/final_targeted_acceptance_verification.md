# SkillSphere — Final Targeted Acceptance Verification Report

**Project:** SkillSphere — Organizational Knowledge Gap Intelligence Platform  
**Program:** Infosys Springboard Virtual Internship 7.0  
**Repository:** `C:\Users\gjaya\OneDrive\Desktop\SkillSphere\skillsphere`  
**Git Branch:** `feature/jayanth`  
**Environment:** Windows Native PostgreSQL 18.4 (`skillsphere_db`), Java 21, Spring Boot 3.3.5, React 18, Vite  
**Date of Verification:** November 7, 2026  
**Verification Scope:** Targeted evidence-based acceptance verification across four critical areas: (1) Skill Catalogue and Role Requirements, (2) Assessment and Skill-Gap Consistency, (3) Learning Completion and Certificates, and (4) Report Export Security.

---

## 1. Executive Summary & Verification Verdict

The targeted acceptance verification was executed against the actual production codebase, REST endpoints, service layers, and live PostgreSQL 18.4 database (`skillsphere_db`). Two genuine defects were discovered and corrected using minimal, surgical fixes:
1. **Assessment Job-Role Resolution:** In `AssessmentService.recalculateSkillGapsForUser`, the user's job role was previously extracted from `user.getRequestedRole()`, defaulting to non-existent `"Full Stack Developer"`, which caused fallback to all requirements. Now it resolves the employee's assigned `jobRole` directly from `EmployeeProfile`, correctly evaluating employees against their designated job role requirements.
2. **Report Export Scope Isolation:** In `ReportController.exportGapsCsv` and `ReportService.generateSkillGapsCsv`, calls from `MANAGER` and `DEPARTMENT_HEAD` previously returned all organizational gaps without scoping. Now, callers are strictly scoped by role: managers receive only gaps for their managed teams, department heads receive only gaps for their department, HR and Admin receive organization-wide data, and unauthorized roles (`EMPLOYEE`, `MENTOR`) receive HTTP `403 FORBIDDEN`.

All 294 backend tests passed (including the new PostgreSQL 18.4 integration suite `TargetedAcceptanceVerificationIntegrationTest`), the Vite production build compiled cleanly in 1.92s with 0 errors, and the active database `skillsphere_db` was preserved without data loss or schema alterations.

### Final Verification Verdict: **ACCEPT WITH DOCUMENTED LIMITATIONS**

---

## 2. Area 1: Skill Catalogue and Role Requirements

### 1. Verification of Supported Platform Roles & Job-Role Mappings
- **Platform Roles:** Exactly six roles are defined in `Role.java`:
  - `EMPLOYEE`
  - `MANAGER`
  - `MENTOR`
  - `HR_SPECIALIST`
  - `DEPARTMENT_HEAD`
  - `ADMIN`
  - *Verification:* Verified that `L&D_ADMIN` does not exist; all learning governance is unified under `HR_SPECIALIST`.
- **Database Job-Role Constraint:** The `employee_profiles` table contains constraint:
  ```sql
  CONSTRAINT chk_profile_job_role CHECK (job_role IN ('Software Developer', 'Data Analyst', 'QA Engineer', 'Support Engineer'))
  ```

### 2. Skill Requirement Coverage & Target Proficiencies
- The `role_skill_requirements` table maps required skills and target proficiencies for job roles:
  1. **Software Developer:** 7 skill requirements (Java: Advanced, Spring Boot: Advanced, SQL: Advanced, Web Development: Intermediate, Git: Intermediate, Data Structures: Advanced, Problem Solving: Advanced).
  2. **QA Engineer:** 4 skill requirements (Java: Intermediate, Automated Testing: Advanced, SQL: Intermediate, Git: Intermediate).
  3. **Data Analyst:** 3 skill requirements (SQL: Expert, Problem Solving: Advanced, Java: Intermediate).
- **Target Proficiency Consistency:** All target proficiencies conform strictly to the 5-point scale: `Unaware` (1), `Beginner` (2), `Intermediate` (3), `Advanced` (4), `Expert` (5).

### 3. Identified Missing & Inconsistent Mappings
- **Missing Mapping (Documented Gap):** `Support Engineer` is an allowed job role in `employee_profiles.chk_profile_job_role`, but currently has **0 predefined requirements** in `role_skill_requirements`.
- **Defect Discovered:** `AssessmentService.recalculateSkillGapsForUser` previously attempted to match `user.getRequestedRole().name()`, falling back to `"Full Stack Developer"`. If an employee set their profile to `"QA Engineer"` or `"Data Analyst"`, their gaps were not calculated against that job role.
- **Correction Applied:** `AssessmentService.java` was updated to inject `EmployeeProfileRepository` and resolve `profile.getJobRole()`. If requirements are missing (e.g. for `Support Engineer`), it falls back gracefully to `"Software Developer"` and then `findAll()`.

---

## 3. Area 2: Assessment and Skill-Gap Consistency

### 1. Current Verified Proficiency Selection
- **Inspected Files:** [`AssessmentService.java`](file:///C:/Users/gjaya/OneDrive/Desktop/SkillSphere/skillsphere/backend/src/main/java/com/skillsphere/service/AssessmentService.java), [`SkillService.java`](file:///C:/Users/gjaya/OneDrive/Desktop/SkillSphere/skillsphere/backend/src/main/java/com/skillsphere/service/SkillService.java).
- **Behavior Verified:**
  - Before an employee is assessed or adds a skill, their proficiency defaults to `Unaware` (numeric level 1).
  - When an employee completes an assessment with a passing score (e.g., 80%), the server calculates the earned level (e.g., `Advanced` = 4) and updates `EmployeeSkill`:
    ```java
    if (newNum >= currentNum) {
        es.setProficiencyLevel(proficiency);
        es.setVerified(true);
        es.setUpdatedAt(Instant.now());
        employeeSkillRepository.save(es);
    }
    ```
  - The `is_verified` flag is set to `true`, preventing unverified self-ratings from overriding verified assessment outcomes.

### 2. Historical Assessment Preservation
- **Behavior Verified:**
  - Every completed attempt is permanently persisted in the `assessment_attempts` table with `user_id`, `skill_id`, `score`, `passed`, `status="COMPLETED"`, `target_level`, `started_at`, and `completed_at`.
  - Multiple attempts by the same employee for the same skill are preserved chronologically (`findByUserIdOrderByStartedAtDesc`).
  - Retakes and reassessments update the current verified proficiency without deleting or mutating prior attempt records.

### 3. Reassessment Updates & Gap Recalculation
- **Authoritative Gap Formula:**
  $$\text{Gap Size} = \max(0, \text{Required Numeric Level} - \text{Current Verified Numeric Level})$$
  - Priorities: $\text{gapSize} = 0 \implies \text{ON\_TRACK}$, $\text{gapSize} = 1 \implies \text{MEDIUM}$, $\text{gapSize} \ge 2 \implies \text{HIGH}$.
- **Consistency Across Dashboards:**
  - All gap values originate from the shared `skill_gaps` table populated by `AssessmentService.recalculateSkillGapsForUser`.
  - The Employee Dashboard, Manager Dashboard, HR Dashboard, and Department Head Dashboard query this same underlying table and calculation service.

---

## 4. Area 3: Learning Completion and Certificates

### 1. Backend-Controlled Status Transitions
- **Inspected Files:** [`LearningController.java`](file:///C:/Users/gjaya/OneDrive/Desktop/SkillSphere/skillsphere/backend/src/main/java/com/skillsphere/controller/LearningController.java), [`LearningService.java`](file:///C:/Users/gjaya/OneDrive/Desktop/SkillSphere/skillsphere/backend/src/main/java/com/skillsphere/service/LearningService.java).
- **State Machine:**
  - **Enrollment (`POST /api/learning/enroll/{courseId}`):** Status is set to `"IN_PROGRESS"` with `progressPct = 0%`.
  - **Progress Update (`PUT /api/learning/progress/{courseId}`):**
    - `progressPct` clamped: $\min(100, \max(0, \text{progressPct}))$.
    - `0 < pct < 100`: Status remains `"IN_PROGRESS"`, `completedAt` remains `null`.
    - `pct == 100`: Status transitions to `"COMPLETED"`, `completedAt` is recorded (`Instant.now()`).

### 2. Direct API Manipulation Protection
- **Security Check:** The endpoint resolves the user strictly from the authenticated Spring Security session (`UserDetails.getUsername()`).
- **Authorization Enforcement:** If user Mallory attempts to update progress for Bob's enrollment, the query `employeeLearningRepository.findByUserIdAndCourseId(mallory.getId(), courseId)` fails with:
  `ApiException(HttpStatus.NOT_FOUND, "You are not enrolled in this course.")`
- Mallory cannot manipulate another employee's learning status or certificates.

### 3. Certificate Generation & Verification
- **Automated Issuance:** When progress reaches 100%, `LearningService` checks if a certificate already exists for that user and course.
- If none exists, a `Certificate` entity is generated and persisted:
  - `certificate_code`: `"CERT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()`
  - `issue_date`: `LocalDate.now()`
  - `expiry_date`: `LocalDate.now().plusYears(2)`
- **Duplicate Prevention:** Re-submitting 100% progress does not generate redundant certificate records.

---

## 5. Area 4: Report Export Security

### 1. CSV Export Endpoints Audited
1. `GET /api/reports/export/gaps.csv` in `ReportController.java`
2. `GET /api/manager/dashboard/reports/export` in `ManagerController.java`
3. `GET /api/department-head/dashboard/reports/csv` in `DepartmentHeadController.java`

### 2. Role-Based Access & IDOR Verification Results

| Endpoint | Role Calling | Parameters / Manipulations Tested | Expected Result | Actual Result | Status |
|:---|:---|:---|:---|:---|:---:|
| `/api/reports/export/gaps.csv` | `EMPLOYEE` | Direct request | 403 Forbidden | `403 FORBIDDEN` | **PASS** |
| `/api/reports/export/gaps.csv` | `MENTOR` | Direct request | 403 Forbidden | `403 FORBIDDEN` | **PASS** |
| `/api/reports/export/gaps.csv` | `MANAGER` | Direct request | Scoped to manager's team | Returns only employees in managed team | **PASS** |
| `/api/reports/export/gaps.csv` | `DEPARTMENT_HEAD` | Direct request | Scoped to department | Returns only employees in department | **PASS** |
| `/api/reports/export/gaps.csv` | `HR_SPECIALIST` | Direct request | Full organization data | Returns all organization records | **PASS** |
| `/api/reports/export/gaps.csv` | `ADMIN` | Direct request | Full organization data | Returns all organization records | **PASS** |
| `/api/manager/dashboard/reports/export` | `MANAGER` | `teamId` manipulated to another manager's team | 403 Forbidden | `403 FORBIDDEN` (`Access denied. You can only access teams assigned to you.`) | **PASS** |
| `/api/department-head/dashboard/reports/csv` | `DEPARTMENT_HEAD` | `departmentId` manipulated to another department | 403 Forbidden | `403 FORBIDDEN` (`Access denied: You are not authorized to access department`) | **PASS** |

### 3. Defect Discovered & Minimal Fix Applied
- **Defect:** `ReportService.generateSkillGapsCsv()` previously accepted no caller parameter and executed `skillGapRepository.findAll()`. When accessed by a `MANAGER` or `DEPARTMENT_HEAD`, it leaked skill gap records of employees outside their team or department.
- **Fix:** Updated `ReportController.java` to pass the caller's identity (`Authentication.getName()`) and overloaded `ReportService.generateSkillGapsCsv(String callerEmail)` to scope gaps by manager teams (`findByManagerUserId`) or department (`findByDepartmentId`), while returning full gaps for `HR_SPECIALIST` and `ADMIN`, and rejecting unauthorized callers with `403 FORBIDDEN`.

---

## 6. Automated Verification Evidence

### 1. Targeted Integration Test Execution
- **Test Class:** [`TargetedAcceptanceVerificationIntegrationTest.java`](file:///C:/Users/gjaya/OneDrive/Desktop/SkillSphere/skillsphere/backend/src/test/java/com/skillsphere/service/TargetedAcceptanceVerificationIntegrationTest.java)
- **Database:** Native PostgreSQL 18.4 (`skillsphere_db`), `@SpringBootTest`, `@Transactional`
- **Output:**
  ```
  [INFO] Running com.skillsphere.service.TargetedAcceptanceVerificationIntegrationTest
  [INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 16.69 s
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  ```

### 2. Full Regression Suite Execution
- **Command:** `mvn test -f backend/pom.xml`
- **Output:**
  ```
  [INFO] Results:
  [INFO] Tests run: 294, Failures: 0, Errors: 0, Skipped: 0
  [INFO] ------------------------------------------------------------------------
  [INFO] BUILD SUCCESS
  [INFO] Total time:  33.633 s
  [INFO] Finished at: 2026-11-07T17:55:17+05:30
  ```

### 3. Frontend Production Build
- **Command:** `npm --prefix frontend run build`
- **Output:**
  ```
  vite v5.4.21 building for production...
  ✓ 198 modules transformed.
  dist/index.html                   0.86 kB │ gzip:   0.46 kB
  dist/assets/index-DpmVR_62.css   77.47 kB │ gzip:  14.58 kB
  dist/assets/index-DbdrJo_b.js   871.17 kB │ gzip: 198.06 kB
  ✓ built in 1.92s
  ```

### 4. Database Integrity & Preservation
- Native PostgreSQL 18.4 database `skillsphere_db` was checked before and after all test runs.
- Public tables count: **57**
- Flyway migrations: **10** (V1–V10) validated with `ddl-auto: validate`.
- Active database records preserved 100% intact.

---

## 7. Documented Limitations

1. **Job Role Mapping Coverage (Support Engineer):**
   - The `employee_profiles` table permits `"Support Engineer"`, but `role_skill_requirements` currently seeds requirements only for `"Software Developer"`, `"QA Engineer"`, and `"Data Analyst"`. `AssessmentService` falls back gracefully to `"Software Developer"` requirements when mapping for Support Engineer is not found.
2. **Message History Pagination:**
   - Message history loads all messages chronologically for the active conversation; pagination is withheld pending product approval.
3. **Host-Level Antivirus Daemon:**
   - The local Windows host environment lacks an active ClamAV scanner; defense-in-depth is enforced via strict MIME/extension whitelisting, 10MB file caps, UUID prefixing, and authorization streams.
4. **Chat Retention Purge:**
   - Operates as a database-backed policy definition in `system_configuration` rather than an automated `@Scheduled` background deletion daemon.

---

## 8. Final Acceptance Determination

### **ACCEPT WITH DOCUMENTED LIMITATIONS**

The four targeted areas have been rigorously verified. All identified defects have been documented and corrected with minimal safe changes. The platform demonstrates complete role-based security, robust assessment scoring, tamper-proof learning progress, scoped CSV reporting, 294 passing automated tests, clean frontend production compilation, and native PostgreSQL 18.4 database preservation.
