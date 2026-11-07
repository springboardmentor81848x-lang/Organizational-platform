# SkillSphere — Platform-Wide Implementation & Verification Plan

**Project:** SkillSphere — Organizational Knowledge Gap Intelligence Platform  
**Program:** Infosys Springboard Virtual Internship 7.0  
**Repository:** `C:\Users\gjaya\OneDrive\Desktop\SkillSphere\skillsphere`  
**Git Branch:** `feature/jayanth`  
**Plan Scope:** Controlled stage-by-stage verification, integration testing, and regression analysis of the entire SkillSphere platform across all 6 roles and 76 dashboard modules.

---

## 1. Controlled Implementation & Verification Stages

To ensure zero disruption to teammates' work, active database records, or existing accepted dashboards, changes and verifications are executed in seven structured stages:

```
Stage 1: Security & RBAC Boundary Enforcement
   ↓
Stage 2: Core Business Logic & Authoritative Gap Engine
   ↓
Stage 3: Cross-Dashboard Integration & Workflows 1–8
   ↓
Stage 4: Database Preservation & Native Backup Verification
   ↓
Stage 5: Frontend Build & User Experience Validation
   ↓
Stage 6: Automated Regression Testing (Full 287-Test Suite)
   ↓
Stage 7: Minimum Demonstration Checklist Verification
```

---

## 2. Stage-by-Stage Implementation & Verification Details

### Stage 1: Security & RBAC Boundary Enforcement
- **Requirement:** Absolute role separation across `EMPLOYEE`, `MANAGER`, `MENTOR`, `HR_SPECIALIST`, `DEPARTMENT_HEAD`, and `ADMIN`. Prevention of IDOR, URL manipulation, and unauthorized administrative access to private communications.
- **Current Implementation:** Spring Security 6, `@EnableMethodSecurity`, method `@PreAuthorize`, service-level data filtering in `ManagerService`, `DepartmentHeadService`, `MentorDashboardService`, and `ChatService`.
- **Targeted Modifications Applied:**
  - `ChatService.java`: Removed `caller.getRole() != Role.ADMIN` in `createTeamConversation`.
  - `ChatAttachmentService.java`: Validated participant-only checks on upload and download.
- **Database Impact:** None (existing schema and tables preserved).
- **Security Impact:** Eliminates administrative privilege escalation and preserves private conversation confidentiality.
- **Regression Risk:** Very Low; isolated to team conversation creation.
- **Tests Required:** `AdminDashboardControllerSecurityTest`, `ChatControllerSecurityTest`, `ChatAttachmentSecurityTest`, `DepartmentHeadControllerSecurityTest`, `HrDashboardControllerSecurityTest`, `ManagerControllerSecurityTest`, `MentorDashboardControllerSecurityTest`.

### Stage 2: Core Business Logic & Authoritative Gap Engine
- **Requirement:** Consistent 5-point proficiency scale (`Unaware=1`, `Beginner=2`, `Intermediate=3`, `Advanced=4`, `Expert=5`), automated server-side assessment scoring, single authoritative gap formula ($\max(0, \text{Required} - \text{Current})$), and multi-recipient consensus group read receipts.
- **Current Implementation:**
  - `AssessmentService.recalculateSkillGapsForUser`: Updates `skill_gaps` table dynamically upon assessment submission or skill updates.
  - `ChatService.toMessageDto`: Computes `isRead` using `allMatch` across all other participants in group conversations.
- **Targeted Modifications Applied:**
  - `ChatService.java`: Enforced `allMatch` across `otherParticipants` for group conversations.
- **Database Impact:** None.
- **Security Impact:** Prevents client-side score manipulation; guarantees authentic read status indicators.
- **Regression Risk:** None; validated by unit and integration tests.
- **Tests Required:** `ChatServiceTest`, `AssessmentServiceTest`, `SkillServiceTest`, `ChatWorkflowIntegrationTest`.

### Stage 3: Cross-Dashboard Integration & Workflows 1–8
- **Requirement:** Seamless data flow across all 8 platform workflows:
  - Workflow 1: Admin User & Role Management
  - Workflow 2: HR Skill Catalogue & Role Requirements
  - Workflow 3: Employee Profile & Dynamic Assessment
  - Workflow 4: Learning Recommendations & Course Completion
  - Workflow 5: Mentorship Matching & Communication
  - Workflow 6: Manager Team Review & Heatmaps
  - Workflow 7: HR & Dept Head Capability Reporting
  - Workflow 8: Continuous Improvement & Reassessment
- **Current Implementation:** Shared PostgreSQL 18.4 tables (`skillsphere_db`), REST API contracts, unified DTOs, and React dashboard components.
- **Database Impact:** None.
- **Regression Risk:** None.
- **Tests Required:** End-to-end integration tests and controller test suites.

### Stage 4: Database Preservation & Native Backup Verification
- **Requirement:** Native PostgreSQL 18.4 (`skillsphere_db`) on port 5432 with 57 public tables and 10 Flyway migrations must remain 100% intact. Live backup creation and isolated sandbox restoration must be verified.
- **Verification Strategy:**
  - Execute `DatabaseBackupEndToEndIntegrationTest.java`.
  - Capture pre-execution table count (57), migration count (10), and user count (4).
  - Dump database via native `pg_dump.exe` (-Fc custom format).
  - Verify archive TOC via `pg_restore.exe -l`.
  - Restore archive into temporary isolated sandbox database (`skillsphere_restore_test_*`).
  - Drop temporary sandbox database via `dropdb.exe`.
  - Verify active `skillsphere_db` remains unchanged.
- **Database Impact:** Safe isolated verification; active DB untouched.
- **Regression Risk:** Zero.

### Stage 5: Frontend Build & User Experience Validation
- **Requirement:** Production bundle compilation without syntax errors, missing dependencies, or broken imports across all 6 dashboards (76 views).
- **Execution Command:** `npm --prefix frontend run build`
- **Acceptance Criteria:** Vite production build generates valid distribution bundle; 0 errors.

### Stage 6: Automated Regression Testing
- **Requirement:** Execution of the complete backend Maven test suite without mock failures or skipped tests.
- **Execution Command:** `mvn test -f backend/pom.xml`
- **Acceptance Criteria:** All 287 tests pass with 0 failures, 0 errors, and 0 skipped.

### Stage 7: Minimum Demonstration Checklist Verification
- **Requirement:** Verification of all 25 items from the Phase 8 demonstration checklist using actual application behavior and real database records.
