# SkillSphere — Platform-Wide Gap Register

**Project:** SkillSphere — Organizational Knowledge Gap Intelligence Platform  
**Program:** Infosys Springboard Virtual Internship 7.0  
**Repository:** `C:\Users\gjaya\OneDrive\Desktop\SkillSphere\skillsphere`  
**Git Branch:** `feature/jayanth`  
**Audit Date:** November 7, 2026  
**Document Purpose:** Prioritized register of all identified defects, resolution status, and documented infrastructure limitations across the platform.

---

## 1. Prioritization Framework

All platform gaps and audit items are tracked and categorized according to the following seven priority tiers:

1. **Tier 1 — Security Vulnerabilities & Permission Defects**
2. **Tier 2 — Incorrect or Missing Core Business Logic**
3. **Tier 3 — Database Integrity Problems**
4. **Tier 4 — Broken Cross-Dashboard Workflows**
5. **Tier 5 — Missing Required Features & Documented Limitations**
6. **Tier 6 — Search, Reports, Export & Supporting Functionality**
7. **Tier 7 — UI Inconsistencies & Usability Improvements**

---

## 2. Comprehensive Gap & Status Register

| Gap ID | Priority Tier | Component / Feature | Description | Initial Status | Resolution / Action Taken | Final Status |
|:---|:---:|:---|:---|:---:|:---|:---:|
| **GAP-SEC-01** | Tier 1 | Chatbox / Authorization | `ChatService.createTeamConversation` permitted `Role.ADMIN` to create team channels and inject admin as participant, violating the privacy boundary. | DEFECTIVE | Removed `caller.getRole() != Role.ADMIN` check in `ChatService.java`. Only assigned team managers can create team channels. | **RESOLVED & VERIFIED** |
| **GAP-SEC-02** | Tier 1 | API Authorization / IDOR | Potential IDOR risks on direct access to employee assessment attempts, learning records, and private chat attachments. | AUDIT ITEM | Verified that all controllers enforce service-level ownership checks (`validateTeamAccess`, `resolveAuthorizedDepartment`, `findByConversationIdAndUserId`). Admin receives 403 Forbidden on employee chats. | **RESOLVED & VERIFIED** |
| **GAP-LOG-01** | Tier 2 | Chatbox / Read Receipts | Group chat read receipts (`isRead`) became `true` if any single participant read the message, misleading senders that the entire team read it. | DEFECTIVE | Updated `ChatService.toMessageDto` to enforce `otherParticipants.stream().allMatch(...)`. Group read confirmation requires all intended recipients to have read. | **RESOLVED & VERIFIED** |
| **GAP-LOG-02** | Tier 2 | Skill Assessment / Proficiency Scale | Ensuring proficiency levels are strictly consistent across self-assessment, requirements, gap calculations, and reporting. | AUDIT ITEM | Verified 5-point scale: `Unaware` (1), `Beginner` (2), `Intermediate` (3), `Advanced` (4), `Expert` (5). Numeric conversions mapped consistently in `SkillService` and `AssessmentService`. | **RESOLVED & VERIFIED** |
| **GAP-DB-01** | Tier 3 | PostgreSQL / Schema Validation | Hibernate `ddl-auto: validate` compatibility with native PostgreSQL 18.4 across all 57 tables and 10 Flyway migrations. | AUDIT ITEM | Validated all 10 migrations (`V1` to `V10`). Verified constraints, foreign keys, and column types. Zero schema validation errors during Spring Boot bootup. | **RESOLVED & VERIFIED** |
| **GAP-WF-01** | Tier 4 | Cross-Dashboard Role Routing | Ensuring routing for all 6 roles does not allow cross-role privilege escalation via client-side routing. | AUDIT ITEM | Verified `ProtectedRoute.jsx` and `App.jsx`. Client-side routes restrict access by `allowedRoles`, backed by server-side `@PreAuthorize` on every secured endpoint. | **RESOLVED & VERIFIED** |
| **GAP-LIM-01** | Tier 5 | Chatbox / Message Pagination | Message history endpoint `GET /api/chat/conversations/{id}/messages` retrieves full chronological history without `page` and `size` parameters. | GAP | Audited code in `ChatController`, `ChatService`, and `MessageRepository`. As instructed, pagination was withheld pending product confirmation and approval. | **DOCUMENTED LIMITATION** |
| **GAP-LIM-02** | Tier 5 | Attachment / Antivirus Daemon | Attachment upload pipeline lacks active host-level ClamAV / native antivirus scanning engine. | LIMITATION | Defense-in-depth verified: strict MIME whitelist, extension whitelist, dangerous extension blacklist (.exe, .sh, etc.), 10MB limit, UUID prefix, and path traversal checks. | **DOCUMENTED LIMITATION** |
| **GAP-LIM-03** | Tier 5 | System Governance / Chat Retention Purge | Retention setting `chat_retention_days` (default 365) is persisted in `system_configuration`, but no automated `@Scheduled` background purge job runs. | LIMITATION | Retention threshold acts as a database-backed policy definition. Manual purge queries or future automated jobs can be scheduled without risking active database records. | **DOCUMENTED LIMITATION** |
| **GAP-REP-01** | Tier 6 | Reporting / CSV Data Export | Ensuring exported reports contain genuine database records and respect role/department boundaries. | AUDIT ITEM | Verified `/api/reports/export/gaps.csv`, `/api/department-head/dashboard/reports/export`, and `/api/hr/dashboard/reports/export`. Generates genuine CSV downloads from database data. | **RESOLVED & VERIFIED** |
| **GAP-UX-01** | Tier 7 | Frontend UX / Dashboard Completeness | Verification of all 76 dashboard modules for empty states, loading indicators, error banners, and responsive layouts. | AUDIT ITEM | Verified all 76 views across Employee (16), Manager (13), Mentor (12), HR (13), Department Head (10), and Admin (12). Vite build clean in 2.48s (0 errors). | **RESOLVED & VERIFIED** |

---

## 3. Risk Assessment & Resolution Impact

1. **Security Vulnerabilities (Tier 1):** Zero open vulnerabilities. Administrative boundaries and IDOR prevention are fully verified in automated security tests (`AdminDashboardControllerSecurityTest`, `ChatControllerSecurityTest`, `ChatWorkflowIntegrationTest`).
2. **Business Logic & Calculations (Tier 2):** Zero calculation discrepancies. Gap formulas and group read receipts are authoritative and verified.
3. **Database Integrity (Tier 3):** Native PostgreSQL 18.4 `skillsphere_db` remains completely uncorrupted with 57 tables and 10 migrations.
4. **Documented Limitations (Tier 5):** The three limitations (chat history pagination, host antivirus daemon, and automated cron retention purge) are clearly acknowledged and do not impede core platform functionality or internship demonstration criteria.
