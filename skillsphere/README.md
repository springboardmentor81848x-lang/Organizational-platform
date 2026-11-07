# SkillSphere — Role-Based Authentication and Employee Dashboard

Organizational Knowledge Gap Intelligence Platform · React + Java Spring Boot + PostgreSQL

The Employee workspace includes a profile page, a 12-question skills assessment, a knowledge-gap summary, **AI course recommendations powered by Google Gemini**, learning progress tracking, notifications and printable reports. Assessment results and progress are stored in the browser (localStorage) per user; they are not yet persisted to PostgreSQL. The authentication, registration, session, role checks, and admin user-management APIs remain backed by Spring Boot/PostgreSQL.

| Layer | Technology |
|-------|------------|
| Frontend | React 18 + Vite, CSS3, React Router, Axios |
| Backend | Java 17, Spring Boot 3.3, Spring Security, Spring Data JPA, Bean Validation |
| Database | PostgreSQL 14+ (schema managed by Flyway) |
| Auth | Server-side session in an **HttpOnly** cookie + CSRF double-submit cookie, BCrypt (cost 12) |

No Node.js backend is used — Node only runs the Vite dev tools.

```
skillsphere/
├── backend/            Spring Boot project (pom.xml, src/main/java/com/skillsphere/...)
│   └── src/main/resources/db/migration/   V1 users, V2 password_reset_tokens
├── frontend/           React + Vite project
├── database/schema.sql Reference SQL (Flyway applies the same schema automatically)
├── postman/            Importable Postman collection
├── docker-compose.yml  Optional local PostgreSQL
└── README.md
```

## 1. Prerequisites

- JDK 17+ and Maven 3.9+ (or open `backend/` in IntelliJ / Eclipse / VS Code)
- Node.js 18+ (for the React dev server only)
- PostgreSQL 14+ (or Docker)

## 2. Database

Create the database once (the tables are created by Flyway on first start):

```bash
psql -U postgres -c "CREATE DATABASE skillsphere_db;"
```

Or with Docker:

```bash
export POSTGRES_PASSWORD=choose_a_password     # PowerShell: $env:POSTGRES_PASSWORD="choose_a_password"
docker compose up -d
```

## 3. Backend (port 8080)

```bash
cd backend
cp .env.example .env          # Windows: copy .env.example .env
# edit .env: set DB_PASSWORD (and ADMIN_EMAIL / ADMIN_PASSWORD for your first admin)
mvn spring-boot:run
```

`.env` is read automatically from the `backend/` folder (spring-dotenv). You can also set the same names
as real environment variables. **Nothing secret is committed** — `.env` is git-ignored.

| Variable | Purpose |
|----------|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection (`DB_PASSWORD` is required) |
| `CORS_ALLOWED_ORIGINS` | Origins allowed to call the API (default `http://localhost:5173`) |
| `FRONTEND_URL` | Used to build password-reset links |
| `COOKIE_SECURE` | Set `true` behind HTTPS |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Optional: creates the first Admin if none exists |
| `LOG_RESET_LINKS` | Dev only: print reset links in the backend console |
| `GEMINI_API_KEY` | Google Gemini key for AI course recommendations (get one at <https://aistudio.google.com/apikey>). Stays on the server. |
| `GEMINI_MODEL` | Optional, default `gemini-3.5-flash`. Change it if Google retires the model. |

## 4. Frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>. If the API is not on `http://localhost:8080`, copy `frontend/.env.example`
to `frontend/.env` and change `VITE_API_BASE_URL`.

## 5. How the security works

- **Roles:** public registration always creates an **Employee**. The role dropdown is recorded as a *request*
  (`requested_role`) for an administrator; it never grants access. Admins change roles/status at
  `PATCH /api/admin/users/{id}/role|status` (enforced with `hasRole('ADMIN')`). The very first Admin comes from
  `ADMIN_EMAIL` / `ADMIN_PASSWORD` at startup.
- **Sessions:** `JSESSIONID` is HttpOnly + SameSite=Lax; nothing sensitive is in localStorage. "Remember me" keeps the
  session for 7 days (persistent cookie); otherwise 30 min idle timeout. A new session id is issued at login.
- **CSRF:** the API issues an `XSRF-TOKEN` cookie via `GET /api/auth/csrf`; the React app echoes it in `X-XSRF-TOKEN`.
- **Authorization:** every dashboard endpoint is protected server-side (`@PreAuthorize`); the React route guards are
  only for UX.
- **Brute force:** 5 failed logins per email lock sign-in for 15 minutes (in-memory; use Redis for multiple nodes).
- **Password reset:** random 256-bit token, only its SHA-256 hash is stored, expires in 30 min, single use; the
  response never reveals whether an email exists. No mail server is configured, so in development the link is
  **printed in the backend console** — plug a real `EmailService` implementation in for production.
- **Validation:** all input is validated again on the backend (Bean Validation) and unique constraints back up the
  duplicate checks.

## 6. AI-Powered Dynamic Skill Assessment & Employee Workspace

The SkillSphere assessment system provides dynamically generated, database-backed skill assessments powered by the Google Gemini API with seamless database fallback:

### 6.1 Architecture & Workflow
- **Dynamic Question Generation:** Every new assessment attempt for an employee generates a fresh set of questions tailored to:
  - Selected technical skill (e.g. Java, Spring Boot, SQL, Web Development, Automated Testing, Git, Data Structures, Problem Solving).
  - Target requested proficiency level (Beginner, Intermediate, Advanced, Expert).
  - Employee's current calibrated proficiency level (if available).
  - Configured difficulty blueprint: 30% Easy, 40% Medium, 30% Hard (10 questions total).
- **Lifecycle & Refresh Safety:**
  - Starting an assessment generates questions once and saves the attempt in `assessment_attempts` with status `IN_PROGRESS`.
  - Refreshing the page, reopening an ongoing attempt, or navigating away returns the existing stored attempt questions without regenerating.
  - Concurrent start requests recover the ongoing attempt to prevent duplicate attempts.
- **Secure Server-Side Scoring:**
  - Answer keys (`correct_answer_index`) and explanations are strictly omitted from frontend API responses before submission.
  - Submissions are evaluated 100% server-side by Spring Boot; Gemini is never allowed to calculate or tamper with employee scores.
  - Question explanations and score breakdowns are only revealed in the post-submission results.
- **Calibrated Skill Profile & Gap Integration:**
  - Submissions update employee skill records (`employee_skills`) based on score percentage (Unaware <= 25%, Beginner <= 50%, Intermediate <= 70%, Advanced <= 85%, Expert > 85%).
  - Skill gaps (`skill_gaps`) are recalculated automatically against the employee's role requirements.
- **Controlled Fallback Mechanism:**
  - When `GEMINI_API_KEY` is not configured, or if the Gemini API times out or exhausts retries, the platform automatically draws questions from the curated database question bank (`assessment_questions`).
  - Attempts are transparently flagged as `FALLBACK` (vs `AI_GENERATED`) in the UI and database.
- **Duplicate Prevention:**
  - The employee's last 5 attempts for that skill are queried.
  - Normalized string comparison and token-level Jaccard similarity (threshold 0.70) filter out identical or paraphrased questions.
  - Recent question titles are passed to Gemini in the prompt's exclusion list.
  - *Limitation:* Semantic variations that use entirely different vocabularies may not be caught by token Jaccard similarity; however, prompt exclusion guides Gemini to explore alternate subtopics.

### 6.2 Configuration
- `GEMINI_API_KEY`: API key from Google AI Studio. Set in `backend/.env`. Stays securely on the server.
- `GEMINI_MODEL`: Model identifier (default: `gemini-3.5-flash`).
- Database Schema: Managed additively by Flyway migration `V6__dynamic_assessment_enhancements.sql`.

## 7. Employee workspace notes

- Registration contains a **User role** dropdown: Employee, HR, Manager, and Admin.
- For security, public registration always creates an Employee account. Choosing HR, Manager, or Admin records an access request for administrator approval; it does not immediately grant those privileges.
- After registration, sign in. The backend role returned at login determines which dashboard opens. Employee accounts open the Employee workspace.
- **AI recommendations:** after the employee submits the assessment, the React app sends the per-skill results plus job role/experience to `POST /api/ai/recommendations`. The Spring Boot backend calls the Gemini API (key from `GEMINI_API_KEY`, never exposed to the browser) and returns 3-5 courses with priority, reasons and topics. If the key is missing or Gemini is unreachable, a rule-based fallback is returned and the UI labels it "Standard recommendations".
- Course links open a web search for the recommended course; Gemini is told not to invent URLs.

## 7. API

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| GET | `/api/auth/csrf` | public | sets XSRF-TOKEN cookie |
| POST | `/api/auth/register` | public | 201, always Employee |
| POST | `/api/auth/login` | public | 200 user JSON + session cookie |
| POST | `/api/auth/logout` | session | 200 |
| GET | `/api/auth/me` | session | current user |
| POST | `/api/auth/forgot-password` | public | always 200 |
| POST | `/api/auth/reset-password` | public | `{token,password,confirmPassword}` |
| GET | `/api/dashboard/{employee\|hr\|manager\|admin}` | role | 403 for other roles (Admin may view all) |
| POST | `/api/ai/recommendations` | Employee/Admin | Gemini course recommendations from assessment results |
| GET | `/api/admin/users` | Admin | list users |
| PATCH | `/api/admin/users/{id}/role` | Admin | `{ "role": "HR" }` |
| PATCH | `/api/admin/users/{id}/status` | Admin | `{ "status": "SUSPENDED" }` |

Errors share one shape: `{ timestamp, status, error, message, fieldErrors?, path }`.

## 8. Testing with Postman

Import `postman/SkillSphere.postman_collection.json`. Run **0. Get CSRF token** first, then the numbered requests in
order. Postman's cookie jar keeps `JSESSIONID` and `XSRF-TOKEN`; if a POST returns 403, re-run request 0.

### curl equivalent

```bash
# 1) get CSRF cookie + token
curl -s -c jar.txt http://localhost:8080/api/auth/csrf
TOKEN=$(grep XSRF-TOKEN jar.txt | awk '{print $7}')

# 2) register
curl -s -b jar.txt -c jar.txt -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $TOKEN" \
  -d '{"fullName":"Jordan Rivera","email":"jordan@example.com","password":"Str0ng!Passw0rd","confirmPassword":"Str0ng!Passw0rd"}'

# 3) login, then call /me
curl -s -b jar.txt -c jar.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $TOKEN" \
  -d '{"email":"jordan@example.com","password":"Str0ng!Passw0rd","rememberMe":false}'
curl -s -b jar.txt http://localhost:8080/api/auth/me
```

## 8. Troubleshooting

- **`Could not resolve placeholder 'DB_PASSWORD'`** — create `backend/.env` (see step 3).
- **`password authentication failed`** — `DB_USERNAME`/`DB_PASSWORD` do not match your PostgreSQL user.
- **Browser shows "Cannot reach the server"** — backend not running, or `CORS_ALLOWED_ORIGINS` does not match the
  address in your browser bar (use `localhost`, not `127.0.0.1`, on both sides).
- **403 on every POST** — the XSRF cookie is missing; make sure the frontend and API are both on `localhost`.

## 9. Extending

Add new feature modules as sibling packages next to `controller/ service/ repository/ entity/` and protect their
endpoints with `@PreAuthorize`. Skill-gap data (skills, proficiency, learning paths) can reference `users.id`.
