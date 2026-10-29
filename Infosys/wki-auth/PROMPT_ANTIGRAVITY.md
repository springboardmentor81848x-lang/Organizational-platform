# Antigravity prompt — Employee Dashboard Integration

I am building a Workforce Knowledge Intelligence Platform. I already have working employee login and registration, role-based dashboards, a Spring Boot backend, and a MySQL database. I am providing the existing project source code and database dump. **Inspect the files first; do not replace the project with a new app.**

## Goal
Finish and integrate the Employee Dashboard in the existing project while preserving the current login/register flow and other role dashboards.

## Employee dashboard modules
1. Employee profile management — view/edit own name and profile details.
2. My skills and diagnostic assessments — employees add/remove skills, then answer scored questions; never ask them to choose Beginner/Basic/Intermediate/Advanced/Expert.
3. Knowledge gap summary — derive skill levels from assessment scores, compare with required levels, and show gap severity only for skills with a completed assessment.
4. Recommended training and learning paths — show suitable courses and learning paths; allow enrollment.
5. Learning progress tracking — show enrolled courses, progress, status and completion.
6. Knowledge sharing and mentorship — show suggested mentors, allow mentorship requests, list knowledge-sharing sessions and registrations.
7. Notifications — list notifications and mark them read.
8. Reports — summary cards, printable report, and CSV export.

## Stack and constraints
- Frontend: keep the project's existing HTML/CSS/vanilla JavaScript structure and visual style.
- Backend: Java 17+ with the existing Spring Boot 3, Spring Security, JWT authentication, and JDBC/MySQL setup.
- Database: MySQL database `workforce_knowledge_intelligence`.
- Use the supplied SQL dump and existing tables as the source of truth for authentication. The supplied dashboard implementation may use its isolated `ed_` tables; inspect the implementation and preserve compatibility. Do not drop, rename, or destructively alter existing tables.
- Keep the existing login and registration pages and the current `wki_session` token storage/redirect behavior.
- Keep existing dashboards for ADMIN, HR_MANAGER, MANAGER, TRAINER_MENTOR and TRAINING_COORDINATOR unchanged unless a minimal shared integration fix is required.
- Restrict `/api/employee/**` to authenticated EMPLOYEE users.
- Determine the current employee from the verified JWT on the server. Never trust an employee ID supplied by the browser for access control. Every read/write must be scoped to the logged-in employee.
- Never store plaintext passwords or expose password hashes in API responses.
- Validate inputs, handle empty states/loading/API errors, prevent duplicate enrollments/registrations, and use parameterized SQL.
- Keep CORS configured for the actual local frontend origin. Do not hardcode secrets in source control.
- Keep UI responsive, accessible, and consistent. Do not use fake buttons: each visible action must work.
- Sample/demo data must be clearly identifiable and must not overwrite real user data.

## Work plan
1. Inspect the complete project and SQL dump. Summarize the existing architecture and identify any schema/API mismatch before editing.
2. Verify login/register still works; do not rewrite auth unless there is a demonstrable bug.
3. Implement and verify each dashboard module against real API endpoints and MySQL persistence.
4. Confirm database initialization is idempotent and does not damage existing tables.
5. Add/maintain a README with exact Windows PowerShell commands to import SQL, configure MySQL credentials/JWT secret, start backend, run frontend, register/login, and test each module.
6. Build the backend with Maven and fix compilation errors. Check browser console/network errors and ensure API errors are understandable.
7. Provide a concise list of files changed, endpoints, SQL tables, run steps, and any limitations. Do not claim tests passed unless you actually ran them.

## Acceptance checklist
- A newly registered employee can log in and sees only their own dashboard data.
- Profile edits persist after refresh.
- Assessment questions, attempts, answers, scores, and answer review persist in MySQL. Skill levels are derived by the backend from scores, never chosen by the employee.
- Recommendations only appear for assessed skills and prioritize courses mapped to the skills with measured gaps.
- Employee can enroll in a course and see progress; progress/status updates persist.
- Mentorship requests and knowledge-session registration persist.
- Notifications can be marked read and stay read after refresh.
- CSV export and print report work.
- Unauthorized requests return 401; wrong-role requests return 403; one employee cannot access another employee's records.
- Existing login/register and other role dashboards continue to work.
