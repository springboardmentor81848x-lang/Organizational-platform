# Workforce Intelligence: Login & Registration

Frontend: HTML, CSS, JavaScript. Backend: Spring Boot 3 (Java 25), Spring Security + JWT. Database: MySQL 8.

```
wki-auth/
├─ backend/    Spring Boot project
├─ frontend/   login, register and one dashboard per role
└─ database/   auth_setup.sql
```

## 1. Database
1. Import your dump: `mysql -u root -p < workforce_knowledge_intelligence.sql`
2. Run `database/auth_setup.sql` (adds the HR_MANAGER and TRAINER_MENTOR roles). Optional: the backend also adds any missing roles automatically on startup.

## 2. Backend
1. Open `backend/src/main/resources/application.properties` and set `spring.datasource.password`.
2. Change `app.jwt.secret` to a long random string (32+ characters).
3. Run:
   ```
   cd backend
   mvn spring-boot:run
   ```
   The API starts on http://localhost:8080.

## 3. Frontend
Serve the `frontend/` folder on port 5500. Easiest: VS Code, install **Live Server**, right-click `login.html`, then **Open with Live Server**.
No Live Server? From the `frontend/` folder run `python -m http.server 5500` and open http://localhost:5500/login.html.

If you use another port or host, add it to `app.cors.allowed-origins` in `application.properties`.

## 4. Try it
1. Register a user, choosing any role.
2. Log in. You are redirected to that role's dashboard:

| Role | Page |
|---|---|
| Super Admin | admin-dashboard.html |
| HR Manager | hr-dashboard.html |
| Team Manager | manager-dashboard.html |
| Employee | employee-dashboard.html |
| Trainer / Mentor | trainer-dashboard.html |
| Learning Coordinator | coordinator-dashboard.html |

Opening another role's dashboard sends you back to your own. A missing, expired or tampered token sends you to login.

## Employee dashboard

Employees land on a full dashboard (`employee-dashboard.html`) with: **My Profile**, **My Skills** (add skills without guessing a level), **Skill Assessments** (question bank, scored attempts, answer review), **Knowledge Gaps**, **Recommended Training** (courses and learning paths), **Learning Progress**, **Sharing & Mentorship** (mentor suggestions, requests, knowledge sessions), **Notifications** and **Reports** (CSV and print).

**Nothing extra to set up.** On startup the backend creates 11 new tables, all named `ed_...` (so they cannot clash with the tables in your dump) and loads sample skills, courses and learning paths. Look for this line in the console:
```
EmployeeDashboardSchema: employee dashboard tables are ready
```
If you see `EmployeeDashboardSchema FAILED` instead, run `database/employee_dashboard.sql` by hand in MySQL Workbench and restart.

How it fits the existing login:
- `auth.js` already sends the Employee role to `employee-dashboard.html`, and the page uses `guard.js` and the `wki_session` login, so Login, Register and Logout work exactly as before.
- The backend takes the employee from the verified JWT (`JwtAuthFilter`), never from the URL, so an employee can only read and change their own data.
- `/api/employee/**` accepts only the EMPLOYEE role (one line in `SecurityConfig`). To let other roles use it, change `hasRole("EMPLOYEE")` to `hasAnyRole("EMPLOYEE", "MANAGER", ...)`.
- `_generate_dashboards.py` skips pages marked `custom-page`, so regenerating the other dashboards will not overwrite this one.

Try it: register with the **Employee** role, log in, open **My Skills**, add Java or JavaScript, click **Take assessment**, answer every question, and submit. The backend scores the answers, records the attempt in MySQL, maps the result to an internal evidence-based level, and refreshes **Knowledge Gaps** and **Recommended Training**. In Workbench, inspect `SELECT * FROM ed_assessment_attempts;` and `SELECT * FROM ed_assessment_answers;`.

Notes:
- Target levels per skill live in `ed_skills.default_required_level` (for example `UPDATE ed_skills SET default_required_level = 5 WHERE skill_name = 'Java';`).
- "Suggested mentors" shows colleagues who rate themselves 4 or 5 in your gap skills. The placeholder employees from the dump get sample ratings so there is someone to suggest; real users appear once they rate themselves.
- These tables are separate from any skills or training tables already in your dump. If you want the dashboard to use those instead, send the table list and the code can be pointed at them.

## API
| Method | URL | Auth | Purpose |
|---|---|---|---|
| POST | /api/auth/register | none | create account |
| POST | /api/auth/login | none | returns `token`, `role`, `fullName`, ... |
| GET | /api/me | Bearer token | who am I |
| GET | /api/admin/ping | Bearer token, ADMIN only | example of role-protected endpoint |
| GET/PUT/POST/DELETE | /api/employee/... | Bearer token, EMPLOYEE only | employee dashboard data (profile, skills, gaps, training, mentorship, notifications) |

Errors always return `{ "message": "..." }`.

## Login tracking
Every successful login sets `employees.last_login_at` to the current time. The backend adds this column automatically on startup if it is missing. To check:

```sql
SELECT employee_id, email, last_login_at FROM employees ORDER BY last_login_at DESC;
```
Refresh your SQL client (re-run the query) after logging in, because clients do not update by themselves.

## Troubleshooting
- **Red lines everywhere in the IDE:** the dependencies have not loaded yet. IntelliJ: right-click `pom.xml`, Maven, Reload project. VS Code: Ctrl+Shift+P, "Java: Clean Java Language Server Workspace". Wait for the download to finish.
- **Red line on `new Role(name, description)`:** your `entity/Role.java` is the old one. Use the `Role.java` from this project (it has the two-argument constructor and the empty constructor).
- **"Invalid role" when registering:** the role is missing from the `roles` table. Restart the backend (`RoleSeeder` adds it) or run `database/auth_setup.sql`.
- **New row not visible in MySQL Workbench:** run `COMMIT;` and re-run the `SELECT`, or turn on auto-commit (lightning-bolt icon).
- Always use all files from this zip together: mixing them with older copies causes red lines.

## Notes
- The seed employees in your dump have `password_hash = 'DEMO_PLACEHOLDER'`, so they cannot log in. Register new users, or replace their hashes with BCrypt hashes.
- `app.allow-privileged-self-signup=true` lets anyone pick Super Admin or HR Manager (fine for a demo). Set it to `false` before real use.
- New users get department 1 and designation 1 by default (see `AuthService.register`).
- The token is kept in `localStorage`. For production use HTTPS and consider httpOnly cookies.
- To change dashboard content: edit tiles in `frontend/_generate_dashboards.py`, then run `python _generate_dashboards.py`.
- "Continue with Google" and "Forgot Password?" are visual placeholders only.


## Included files for this bundle
- `database/workforce_knowledge_intelligence.sql`: the database dump you supplied.
- `PROMPT_ANTIGRAVITY.md`: prompt to use with Google Antigravity to inspect and improve this project safely.


## Assessment-first recommendation logic

- Employees only add the skills they want to assess; they are not asked to pick Beginner/Intermediate/Expert.
- The assessment question bank is stored in `ed_assessment_questions` with topic, four options, answer key, and explanation. The answer key is not returned by the question endpoint.
- Submissions are scored by the Spring Boot backend and stored in `ed_assessment_attempts` and `ed_assessment_answers`.
- Only skills with a completed assessment appear in the knowledge-gap analysis and course recommendations. Course recommendations prioritize assessed skills below their target level.
- After submitting, the dashboard displays score and review feedback, and a retake can be used to measure improvement.

### MySQL import notes

`database/workforce_knowledge_intelligence.sql` is the original database dump supplied for this project and may contain `DROP TABLE` statements. Back up any existing database before importing it. If the original schema and login already work, do not re-import that dump just to enable assessments: the backend automatically runs `backend/src/main/resources/db/employee_dashboard.sql` at startup, which creates the `ed_assessment_*` tables and seeds sample questions without dropping existing tables. You can also run `database/employee_dashboard.sql` manually in Workbench; it uses `CREATE TABLE IF NOT EXISTS` and repeat-safe sample inserts.

## Role-based assessments and AI recommendations

**Role assessment.** In *Skill Assessments* the employee picks a job role (Java Developer, Full Stack Developer, Frontend Developer, Data Analyst, Cloud / DevOps Engineer, QA / Test Engineer, Project / Team Manager). If a matching job role is typed in *My Profile* it is pre-selected. Each role is a set of skills whose weights add up to 100 marks, for example Java Developer = Java 35, Spring Boot 25, SQL 25, Problem Solving 15. One assessment covers every skill of the role.

**Marks per skill.** The result shows marks earned against marks available for every skill (for example SQL 10 of 30 marks, 33%). A skill is a gap when its assessed level is below its target level (same rule as the rest of the dashboard: below 55% for a target of level 3, below 75% for level 4). Gap skills get:
- course recommendations for that skill (weakest first, with the reason shown), and
- an entry in the AI recommendations (focus topics taken from the questions answered wrongly, a study plan, practice ideas, estimated hours).

**AI recommendations.** Open *Recommended Training*. If an Anthropic API key is set (`app.ai.api-key` in `application.properties`, or the `ANTHROPIC_API_KEY` environment variable), the coaching plan is written by Claude and labelled "Written by AI". Without a key, or if the call fails, the built-in rule-based engine produces the same layout and it is labelled "Built-in recommendation engine". Only the role name, skill names, scores and weak topics are sent, never the employee's name or email. Course titles and reference links come from this server, never from the AI. Results are saved per assessment (`ed_ai_recommendations`); *Regenerate* asks again.

**Database.** Created automatically on startup by `RoleAssessmentSchema` (file `backend/src/main/resources/db/role_assessment.sql`, copy in `database/`): `ed_job_roles`, `ed_job_role_skills`, `ed_role_attempts`, `ed_role_attempt_skills`, `ed_ai_recommendations`, the column `ed_employee_profiles.target_role_id`, 7 new skills, about 60 new questions and 11 new courses. Look for `RoleAssessmentSchema: role assessment tables are ready` in the console.

**Editing roles.** Change weights or add roles directly in `ed_job_roles` / `ed_job_role_skills` (weights for one role should add up to 100).

| Method | URL | Purpose |
|---|---|---|
| GET | /api/employee/me/job-roles | roles with their skills and weights, selected and suggested role |
| PUT | /api/employee/me/job-role | save the employee's role |
| GET | /api/employee/me/role-assessment/questions?roleId= | questions for all skills of the role |
| POST | /api/employee/me/role-assessment/submit | grade, store marks per skill, update gaps, notify managers |
| GET | /api/employee/me/role-assessment/latest | latest result with marks per skill |
| GET / POST | /api/employee/me/ai-recommendations[/refresh] | AI (or built-in) recommendations for lacking skills |

## Look and feel, and the "Ask AI about your gaps" chat

**Theme.** All pages now share one calm enterprise look: slate header, a single teal for actions, amber only for "target" markers, thin borders, IBM Plex Sans (loaded from Google Fonts, with system fonts as the fallback). The sign-in pages show an example skill-gap chart. Colours are CSS variables at the top of `frontend/css/style.css` and at the top of the `<style>` block in `employee-dashboard.html` and `manager-dashboard.html`, so a new brand colour is a one-line change.

**Setting the AI key (chat and recommendations).** The key is read from an environment variable, so it is never stored in the code. Set it before starting the backend, then restart.

- Claude (paid): `ANTHROPIC_API_KEY=sk-ant-...` (nothing else needed).
- Free providers (no card): get a key from Google AI Studio (Gemini) or console.groq.com (Groq), then set `AI_PROVIDER=openai`, `AI_API_KEY=<your key>`, `AI_API_URL=<chat completions URL>` and `AI_MODEL=<model>`. Example values are in `application.properties`.
- Windows (PowerShell): `$env:AI_API_KEY="your-key"` then `mvn spring-boot:run` in the same window. Mac/Linux: `export AI_API_KEY="your-key"`.
- If the key is missing or wrong, the chat keeps working with the built-in helper and says so.

**Chat.** On *Knowledge Gaps* the employee can ask doubts ("why did I get this wrong?", "give me a 2-week plan for SQL"). The backend (`KnowledgeChatController`, `POST /api/employee/me/chat`) gives the assistant the employee's role, assessed skills, weak topics and missed questions so answers are personal. With an Anthropic API key (`app.ai.api-key`) answers come from Claude. Without one, a built-in helper answers questions about the employee's own results (missed questions, gaps, what to learn first) and says so. Each employee is limited to 20 messages per 10 minutes. Only skill names, scores, topics and question text leave the server, never name or email.

## Mentor dashboard (TRAINER_MENTOR)

`trainer-dashboard.html` is hand-built (it carries a `custom-page` comment, so `_generate_dashboards.py` leaves it alone).
Views: Overview (stat tiles plus charts: sessions and registrations per month, mentorship status donut, skill gaps, attendance per session),
Requests (accept or decline, with the requester's biggest gaps), My mentees (levels against target, course progress),
Sessions (schedule, cancel, mark attendance), Skill-gap insights ("Plan a session" pre-fills the form) and Notifications.

Backend: `mentor/MentorDashboardController` under `/api/mentor/**` (role TRAINER_MENTOR in `SecurityConfig`).
`config/MentorSchema` adds `skill_id`, `duration_min`, `session_mode`, `location` to `ed_knowledge_sessions` and `attended` to
`ed_session_registrations` on startup. Scheduling a session notifies the mentor's active mentees and employees with a gap in that skill.

The sign-in and register pages use the original layout with the teal and slate theme.

### Mentor skills and employee requests

A mentor lists the skills they teach under **My profile** in the mentor dashboard (tables `ed_mentor_profiles`, `ed_mentor_skills`,
created by `MentorSchema`). Employees see those mentors under **Sharing & Mentorship > Available mentors** (`GET /api/employee/me/mentors`),
with skills that match their own gaps marked and listed first. The employee picks a skill and sends a request. The mentor then accepts or
declines it under **Requests** and the employee is notified. A mentor who switches off "accepting new mentees" cannot be requested.
