import json
import urllib.request
import urllib.error
import pymysql
import sys

BASE_URL = "http://localhost:8080/api"
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "97@MySql#neha",
    "database": "workforce_knowledge_intelligence",
    "autocommit": True
}

results = {
    "total": 0,
    "passed": 0,
    "failed": 0,
    "details": []
}

def api_call(method, path, body=None, token=None):
    url = BASE_URL + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            content = resp.read().decode("utf-8")
            try:
                parsed = json.loads(content)
            except Exception:
                parsed = content
            return status, parsed
    except urllib.error.HTTPError as e:
        content = e.read().decode("utf-8")
        try:
            parsed = json.loads(content)
        except Exception:
            parsed = content
        return e.code, parsed
    except Exception as e:
        return 0, str(e)

def log_test(test_name, category, passed, status_code, message=""):
    results["total"] += 1
    if passed:
        results["passed"] += 1
    else:
        results["failed"] += 1
    results["details"].append({
        "test": test_name,
        "category": category,
        "passed": passed,
        "status": status_code,
        "message": message
    })
    status_str = "PASS" if passed else "FAIL"
    print(f"[{status_str}] ({status_code}) {category} - {test_name}: {message}")

db_conn = pymysql.connect(**DB_CONFIG)
db_cursor = db_conn.cursor()

def db_query(sql, args=()):
    db_conn.commit()  # ensure read of latest committed data
    db_cursor.execute(sql, args)
    return db_cursor.fetchall()

print("==================================================")
print("1. AUTHENTICATION & ROLE-BASED TOKENS")
print("==================================================")

tokens = {}
roles = {
    "EMPLOYEE": ("emp.test@infosys.com", "Password@123"),
    "HR": ("hr.test@infosys.com", "Password@123"),
    "MANAGER": ("mgr.test@infosys.com", "Password@123"),
    "MENTOR": ("mentor.test@infosys.com", "Password@123"),
    "DEPT_HEAD": ("depthead.test@infosys.com", "Password@123")
}

for role_key, (email, pwd) in roles.items():
    code, res = api_call("POST", "/auth/login", {"email": email, "password": pwd})
    if code == 200 and isinstance(res, dict) and "token" in res:
        tokens[role_key] = res["token"]
        log_test(f"Login {role_key}", "Authentication", True, code, f"Token received, employeeId: {res.get('employeeId')}")
    else:
        log_test(f"Login {role_key}", "Authentication", False, code, f"Failed: {res}")

emp_token = tokens.get("EMPLOYEE")
hr_token = tokens.get("HR")
mgr_token = tokens.get("MANAGER")
mentor_token = tokens.get("MENTOR")
dh_token = tokens.get("DEPT_HEAD")

print("\n==================================================")
print("2. SEQUENTIAL CRUD LIFECYCLE TESTS (C -> R -> U -> R -> D -> R)")
print("==================================================")

# ----------------------------------------------------
# 2.1 Resource: Employee Certifications (ed_certifications)
# ----------------------------------------------------
print("\n--- 2.1 Resource: Employee Certifications (ed_certifications) ---")
# Clean up any leftover test certs first
db_cursor.execute("DELETE FROM ed_certifications WHERE credential_id LIKE 'CERT-CRUD-%'")

cert_payload = {
    "title": "CRUD Test Certification",
    "issuer": "Automated Test Suite",
    "credentialId": "CERT-CRUD-001",
    "issueDate": "2026-01-01",
    "expiryDate": "2028-01-01",
    "credentialUrl": "https://infosys.com/verify/crud-001"
}

# 1. CREATE
code, res = api_call("POST", "/employee/me/certifications", cert_payload, token=emp_token)
log_test("1. CREATE Certification", "Certifications", code in (200, 201), code, f"Response: {res}")

# Check in DB & retrieve generated ID
db_rows = db_query("SELECT certification_id, title, credential_id FROM ed_certifications WHERE credential_id = %s", ("CERT-CRUD-001",))
created_cert_id = db_rows[0][0] if db_rows else None
db_ok = len(db_rows) == 1
log_test("1b. Verify CREATE in MySQL", "Certifications", db_ok, code, f"MySQL row verified with certification_id: {created_cert_id}")

# 2. READ
code, res = api_call("GET", "/employee/me/certifications", token=emp_token)
read_ok = False
if code == 200 and isinstance(res, list):
    found = [c for c in res if c.get("certificationId") == created_cert_id]
    read_ok = len(found) == 1
log_test("2. READ Newly Created Certification", "Certifications", read_ok, code, f"Found record in GET response: {read_ok}")

# 3. UPDATE
update_cert_payload = dict(cert_payload)
update_cert_payload["title"] = "CRUD Test Certification Updated"
code, res = api_call("PUT", f"/employee/me/certifications/{created_cert_id}", update_cert_payload, token=emp_token)
log_test("3. UPDATE Certification", "Certifications", code == 200, code, f"Response: {res}")

# Check update in DB
db_rows = db_query("SELECT title FROM ed_certifications WHERE certification_id = %s", (created_cert_id,))
db_update_ok = len(db_rows) == 1 and db_rows[0][0] == "CRUD Test Certification Updated"
log_test("3b. Verify UPDATE in MySQL", "Certifications", db_update_ok, code, "Updated title verified in ed_certifications")

# 4. READ Updated
code, res = api_call("GET", "/employee/me/certifications", token=emp_token)
read_upd_ok = False
if code == 200 and isinstance(res, list):
    found = [c for c in res if c.get("certificationId") == created_cert_id and c.get("title") == "CRUD Test Certification Updated"]
    read_upd_ok = len(found) == 1
log_test("4. READ Verified Updated Certification", "Certifications", read_upd_ok, code, "Updated title reflected in GET")

# 5. DELETE
code, res = api_call("DELETE", f"/employee/me/certifications/{created_cert_id}", token=emp_token)
log_test("5. DELETE Certification", "Certifications", code == 200, code, f"Response: {res}")

# Check deletion in DB
db_rows = db_query("SELECT certification_id FROM ed_certifications WHERE certification_id = %s", (created_cert_id,))
db_del_ok = len(db_rows) == 0
log_test("5b. Verify DELETE in MySQL", "Certifications", db_del_ok, code, "Record removed from ed_certifications")

# 6. READ Deletion Verification
code, res = api_call("GET", "/employee/me/certifications", token=emp_token)
read_del_ok = False
if code == 200 and isinstance(res, list):
    found = [c for c in res if c.get("certificationId") == created_cert_id]
    read_del_ok = len(found) == 0
log_test("6. READ Verify Deletion in List", "Certifications", read_del_ok, code, "Deleted certification absent from GET")


# ----------------------------------------------------
# 2.2 Resource: HR Competency Skills (ed_skills)
# ----------------------------------------------------
print("\n--- 2.2 Resource: HR Competency Skills (ed_skills) ---")
skill_payload = {
    "skillName": "CRUD Test Automation Skill",
    "category": "Testing",
    "defaultRequiredLevel": 3
}

# 1. CREATE
code, res = api_call("POST", "/hr/competency/skills", skill_payload, token=hr_token)
created_skill_id = res.get("skillId") if isinstance(res, dict) else None
log_test("1. CREATE HR Competency Skill", "HR Competency Skills", code in (200, 201) and created_skill_id is not None, code, f"Generated skillId: {created_skill_id}")

db_rows = db_query("SELECT skill_id, skill_name, category FROM ed_skills WHERE skill_id = %s", (created_skill_id,))
db_ok = len(db_rows) == 1 and db_rows[0][1] == "CRUD Test Automation Skill"
log_test("1b. Verify CREATE in MySQL", "HR Competency Skills", db_ok, code, "MySQL row verified in ed_skills")

# 2. READ
code, res = api_call("GET", "/hr/competency/skills", token=hr_token)
read_ok = False
if code == 200 and isinstance(res, list):
    found = [s for s in res if s.get("id") == created_skill_id or s.get("skillId") == created_skill_id]
    read_ok = len(found) == 1
log_test("2. READ Newly Created HR Skill", "HR Competency Skills", read_ok, code, f"Skill {created_skill_id} present in GET")

# 3. UPDATE
update_skill_payload = {
    "skillName": "CRUD Test Automation Skill Updated",
    "category": "Testing",
    "defaultRequiredLevel": 4
}
code, res = api_call("PUT", f"/hr/competency/skills/{created_skill_id}", update_skill_payload, token=hr_token)
log_test("3. UPDATE HR Competency Skill", "HR Competency Skills", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT skill_name, default_required_level FROM ed_skills WHERE skill_id = %s", (created_skill_id,))
db_upd_ok = len(db_rows) == 1 and db_rows[0][0] == "CRUD Test Automation Skill Updated" and db_rows[0][1] == 4
log_test("3b. Verify UPDATE in MySQL", "HR Competency Skills", db_upd_ok, code, "Updated skill_name & level verified in MySQL")

# 4. READ Updated
code, res = api_call("GET", "/hr/competency/skills", token=hr_token)
read_upd_ok = False
if code == 200 and isinstance(res, list):
    found = [s for s in res if (s.get("id") == created_skill_id or s.get("skillId") == created_skill_id) and (s.get("name") == "CRUD Test Automation Skill Updated" or s.get("skillName") == "CRUD Test Automation Skill Updated")]
    read_upd_ok = len(found) == 1
log_test("4. READ Verified Updated HR Skill", "HR Competency Skills", read_upd_ok, code, "Updated name reflected in GET")

# 5. DELETE
code, res = api_call("DELETE", f"/hr/competency/skills/{created_skill_id}", token=hr_token)
log_test("5. DELETE HR Competency Skill", "HR Competency Skills", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT skill_id FROM ed_skills WHERE skill_id = %s", (created_skill_id,))
db_del_ok = len(db_rows) == 0
log_test("5b. Verify DELETE in MySQL", "HR Competency Skills", db_del_ok, code, "Skill deleted from ed_skills")

# 6. READ Deletion Verification
code, res = api_call("GET", "/hr/competency/skills", token=hr_token)
read_del_ok = False
if code == 200 and isinstance(res, list):
    found = [s for s in res if s.get("id") == created_skill_id or s.get("skillId") == created_skill_id]
    read_del_ok = len(found) == 0
log_test("6. READ Verify Deletion in Catalog", "HR Competency Skills", read_del_ok, code, "Deleted skill absent from GET")


# ----------------------------------------------------
# 2.3 Resource: HR Job Roles (ed_job_roles)
# ----------------------------------------------------
print("\n--- 2.3 Resource: HR Job Roles (ed_job_roles) ---")
role_payload = {
    "roleName": "CRUD Test Automation Lead",
    "description": "Oversees comprehensive automated API & regression test suites"
}

# 1. CREATE
code, res = api_call("POST", "/hr/roles", role_payload, token=hr_token)
created_role_id = res.get("roleId") if isinstance(res, dict) else None
log_test("1. CREATE HR Job Role", "Job Roles", code in (200, 201) and created_role_id is not None, code, f"Generated roleId: {created_role_id}")

db_rows = db_query("SELECT role_id, role_name, description FROM ed_job_roles WHERE role_id = %s", (created_role_id,))
db_ok = len(db_rows) == 1 and db_rows[0][1] == "CRUD Test Automation Lead"
log_test("1b. Verify CREATE in MySQL", "Job Roles", db_ok, code, "MySQL row verified in ed_job_roles")

# 2. READ
code, res = api_call("GET", "/hr/roles", token=hr_token)
read_ok = False
if code == 200 and isinstance(res, list):
    found = [r for r in res if r.get("id") == created_role_id or r.get("roleId") == created_role_id]
    read_ok = len(found) == 1
log_test("2. READ Newly Created Job Role", "Job Roles", read_ok, code, f"Role {created_role_id} present in GET")

# 3. UPDATE
update_role_payload = {
    "roleName": "CRUD Test Automation Lead Updated",
    "description": "Updated description for automated suite"
}
code, res = api_call("PUT", f"/hr/roles/{created_role_id}", update_role_payload, token=hr_token)
log_test("3. UPDATE HR Job Role", "Job Roles", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT role_name, description FROM ed_job_roles WHERE role_id = %s", (created_role_id,))
db_upd_ok = len(db_rows) == 1 and db_rows[0][0] == "CRUD Test Automation Lead Updated"
log_test("3b. Verify UPDATE in MySQL", "Job Roles", db_upd_ok, code, "Updated role_name verified in MySQL")

# 4. READ Updated
code, res = api_call("GET", "/hr/roles", token=hr_token)
read_upd_ok = False
if code == 200 and isinstance(res, list):
    found = [r for r in res if (r.get("id") == created_role_id or r.get("roleId") == created_role_id) and (r.get("name") == "CRUD Test Automation Lead Updated" or r.get("roleName") == "CRUD Test Automation Lead Updated")]
    read_upd_ok = len(found) == 1
log_test("4. READ Verified Updated Job Role", "Job Roles", read_upd_ok, code, "Updated name reflected in GET")

# 5. DELETE
code, res = api_call("DELETE", f"/hr/roles/{created_role_id}", token=hr_token)
log_test("5. DELETE HR Job Role", "Job Roles", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT role_id FROM ed_job_roles WHERE role_id = %s", (created_role_id,))
db_del_ok = len(db_rows) == 0
log_test("5b. Verify DELETE in MySQL", "Job Roles", db_del_ok, code, "Role deleted from ed_job_roles")

# 6. READ Deletion Verification
code, res = api_call("GET", "/hr/roles", token=hr_token)
read_del_ok = False
if code == 200 and isinstance(res, list):
    found = [r for r in res if r.get("id") == created_role_id or r.get("roleId") == created_role_id]
    read_del_ok = len(found) == 0
log_test("6. READ Verify Deletion in Job Roles", "Job Roles", read_del_ok, code, "Deleted role absent from GET")


# ----------------------------------------------------
# 2.4 Resource: HR Training Courses (ed_courses)
# ----------------------------------------------------
print("\n--- 2.4 Resource: HR Training Courses (ed_courses) ---")
course_payload = {
    "title": "CRUD Automated Test Course",
    "provider": "Infosys Internal",
    "skillId": 1,
    "durationHours": 8,
    "level": 3
}

# 1. CREATE
code, res = api_call("POST", "/hr/courses", course_payload, token=hr_token)
created_course_id = res.get("courseId") if isinstance(res, dict) else None
log_test("1. CREATE HR Training Course", "Courses", code in (200, 201) and created_course_id is not None, code, f"Generated courseId: {created_course_id}")

db_rows = db_query("SELECT course_id, title, provider FROM ed_courses WHERE course_id = %s", (created_course_id,))
db_ok = len(db_rows) == 1 and db_rows[0][1] == "CRUD Automated Test Course"
log_test("1b. Verify CREATE in MySQL", "Courses", db_ok, code, "MySQL row verified in ed_courses")

# 2. READ
code, res = api_call("GET", "/hr/courses", token=hr_token)
read_ok = False
if code == 200 and isinstance(res, list):
    found = [c for c in res if c.get("id") == created_course_id or c.get("courseId") == created_course_id]
    read_ok = len(found) == 1
log_test("2. READ Newly Created Course", "Courses", read_ok, code, f"Course {created_course_id} present in GET")

# 3. UPDATE
update_course_payload = {
    "title": "CRUD Automated Test Course Updated",
    "provider": "Infosys Internal Updated",
    "skillId": 1,
    "durationHours": 10,
    "level": 4
}
code, res = api_call("PUT", f"/hr/courses/{created_course_id}", update_course_payload, token=hr_token)
log_test("3. UPDATE HR Training Course", "Courses", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT title, duration_hours FROM ed_courses WHERE course_id = %s", (created_course_id,))
db_upd_ok = len(db_rows) == 1 and db_rows[0][0] == "CRUD Automated Test Course Updated" and db_rows[0][1] == 10
log_test("3b. Verify UPDATE in MySQL", "Courses", db_upd_ok, code, "Updated title & duration verified in MySQL")

# 4. READ Updated
code, res = api_call("GET", "/hr/courses", token=hr_token)
read_upd_ok = False
if code == 200 and isinstance(res, list):
    found = [c for c in res if (c.get("id") == created_course_id or c.get("courseId") == created_course_id) and c.get("title") == "CRUD Automated Test Course Updated"]
    read_upd_ok = len(found) == 1
log_test("4. READ Verified Updated Course", "Courses", read_upd_ok, code, "Updated title reflected in GET")

# 5. DELETE
code, res = api_call("DELETE", f"/hr/courses/{created_course_id}", token=hr_token)
log_test("5. DELETE HR Training Course", "Courses", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT course_id FROM ed_courses WHERE course_id = %s", (created_course_id,))
db_del_ok = len(db_rows) == 0
log_test("5b. Verify DELETE in MySQL", "Courses", db_del_ok, code, "Course deleted from ed_courses")

# 6. READ Deletion Verification
code, res = api_call("GET", "/hr/courses", token=hr_token)
read_del_ok = False
if code == 200 and isinstance(res, list):
    found = [c for c in res if c.get("id") == created_course_id or c.get("courseId") == created_course_id]
    read_del_ok = len(found) == 0
log_test("6. READ Verify Deletion in Course Catalog", "Courses", read_del_ok, code, "Deleted course absent from GET")


# ----------------------------------------------------
# 2.5 Resource: Employee Skills (ed_employee_skills)
# ----------------------------------------------------
print("\n--- 2.5 Resource: Employee Skills (ed_employee_skills) ---")
# 1. CREATE / RATE
skill_rate_payload = {"skillId": 2}
code, res = api_call("PUT", "/employee/me/skills", skill_rate_payload, token=emp_token)
log_test("1. CREATE / ADD Employee Skill", "Employee Skills", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT employee_id, skill_id, self_level FROM ed_employee_skills WHERE employee_id = 24 AND skill_id = 2")
db_ok = len(db_rows) == 1
log_test("1b. Verify Employee Skill in MySQL", "Employee Skills", db_ok, code, "MySQL row confirmed in ed_employee_skills")

# 2. READ
code, res = api_call("GET", "/employee/me/skills", token=emp_token)
read_ok = False
if code == 200 and isinstance(res, list):
    found = [s for s in res if s.get("skillId") == 2 or s.get("id") == 2]
    read_ok = len(found) == 1
log_test("2. READ Employee My Skills", "Employee Skills", read_ok, code, "Skill 2 present in employee skills list")

# 3. DELETE
code, res = api_call("DELETE", "/employee/me/skills/2", token=emp_token)
log_test("3. DELETE Employee Skill", "Employee Skills", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT employee_id FROM ed_employee_skills WHERE employee_id = 24 AND skill_id = 2")
db_del_ok = len(db_rows) == 0
log_test("3b. Verify Skill Deletion in MySQL", "Employee Skills", db_del_ok, code, "Skill 2 removed from ed_employee_skills")

# 4. READ Deletion Verification
code, res = api_call("GET", "/employee/me/skills", token=emp_token)
read_del_ok = False
if code == 200 and isinstance(res, list):
    found = [s for s in res if s.get("skillId") == 2 or s.get("id") == 2]
    read_del_ok = len(found) == 0
log_test("4. READ Verify Skill Deleted from My Skills", "Employee Skills", read_del_ok, code, "Skill 2 absent from employee skills list")


# ----------------------------------------------------
# 2.6 Resource: Mentor Knowledge Sessions (ed_knowledge_sessions)
# ----------------------------------------------------
print("\n--- 2.6 Resource: Mentor Knowledge Sessions (ed_knowledge_sessions) ---")
# Clean up any previous test sessions first
db_cursor.execute("DELETE FROM ed_knowledge_sessions WHERE title LIKE 'CRUD Test Automation Session%'")

session_payload = {
    "title": "CRUD Test Automation Session",
    "skillId": 1,
    "scheduledAt": "2026-12-30T10:00:00",
    "durationMin": 45,
    "mode": "ONLINE",
    "location": "https://meet.google.com/test-crud-session",
    "description": "Automated verification of knowledge sharing session CRUD"
}

# 1. CREATE
code, res = api_call("POST", "/mentor/sessions", session_payload, token=mentor_token)
log_test("1. CREATE Mentor Session", "Mentor Sessions", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT session_id, title FROM ed_knowledge_sessions WHERE title = %s ORDER BY session_id DESC LIMIT 1", ("CRUD Test Automation Session",))
created_session_id = db_rows[0][0] if db_rows else None
db_ok = len(db_rows) == 1
log_test("1b. Verify Session CREATE in MySQL", "Mentor Sessions", db_ok, code, f"MySQL row verified with session_id: {created_session_id}")

# 2. READ
code, res = api_call("GET", "/mentor/sessions", token=mentor_token)
read_ok = False
if code == 200 and isinstance(res, list):
    found = [s for s in res if s.get("sessionId") == created_session_id]
    read_ok = len(found) == 1
log_test("2. READ Newly Created Session", "Mentor Sessions", read_ok, code, f"Session {created_session_id} found in list")

# 3. UPDATE Attendance
if created_session_id:
    attendance_payload = {"attendedEmployeeIds": [24]}
    code, res = api_call("PUT", f"/mentor/sessions/{created_session_id}/attendance", attendance_payload, token=mentor_token)
    log_test("3. UPDATE Session Attendance", "Mentor Sessions", code == 200, code, f"Attendance updated: {res}")

# 4. DELETE Session
if created_session_id:
    code, res = api_call("DELETE", f"/mentor/sessions/{created_session_id}", token=mentor_token)
    log_test("4. DELETE Mentor Session", "Mentor Sessions", code == 200, code, f"Response: {res}")
    
    db_rows = db_query("SELECT session_id FROM ed_knowledge_sessions WHERE session_id = %s", (created_session_id,))
    db_del_ok = len(db_rows) == 0
    log_test("4b. Verify Session Deletion in MySQL", "Mentor Sessions", db_del_ok, code, "Session removed from ed_knowledge_sessions")

# ----------------------------------------------------
# 2.7 Resource: Peer-to-Peer Following (ed_connections)
# ----------------------------------------------------
print("\n--- 2.7 Resource: Peer-to-Peer Messaging Following (ed_connections) ---")
# Clean up any existing connection between 24 and 30
db_cursor.execute("DELETE FROM ed_connections WHERE (requester_id = 24 AND addressee_id = 30) OR (requester_id = 30 AND addressee_id = 24)")

# Employee 24 sends follow request to Employee 30
code, res = api_call("POST", "/messages/follow/30", token=emp_token)
log_test("1. CREATE Follow Request", "Messaging", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT connection_id, requester_id, addressee_id, status FROM ed_connections WHERE requester_id = 24 AND addressee_id = 30")
conn_id = db_rows[0][0] if db_rows else None
db_ok = len(db_rows) == 1 and db_rows[0][3] in ("PENDING", "ACCEPTED")
log_test("1b. Verify Follow Request in MySQL", "Messaging", db_ok, code, f"Connection record in MySQL (id: {conn_id})")

# 2. READ Requests as Employee 30
# Get token for Employee 30
code, res = api_call("POST", "/auth/login", {"email": "test.emp@infosys.com", "password": "Password@123"})
emp30_token = res.get("token")

code, res = api_call("GET", "/messages/requests", token=emp30_token)
read_ok = False
if code == 200 and isinstance(res, list):
    found = [r for r in res if r.get("id") == conn_id or r.get("connectionId") == conn_id or r.get("requesterId") == 24]
    read_ok = len(found) >= 1
log_test("2. READ Follow Requests", "Messaging", read_ok, code, "Request listed for recipient")

# 3. UPDATE Accept Follow
if conn_id:
    code, res = api_call("PUT", f"/messages/follow/{conn_id}/accept", token=emp30_token)
    log_test("3. UPDATE Accept Follow Request", "Messaging", code == 200, code, f"Response: {res}")

# 4. DELETE Unfollow
code, res = api_call("DELETE", "/messages/follow/30", token=emp_token)
log_test("4. DELETE Unfollow", "Messaging", code == 200, code, f"Response: {res}")

db_rows = db_query("SELECT connection_id FROM ed_connections WHERE requester_id = 24 AND addressee_id = 30")
db_del_ok = len(db_rows) == 0
log_test("4b. Verify Unfollow in MySQL", "Messaging", db_del_ok, code, "Connection record deleted from ed_connections")


print("\n==================================================")
print("3. TEST NEGATIVE AND EDGE-CASE SCENARIOS")
print("==================================================")

# Scenario 1: Unauthorized Request (Missing JWT token)
code, res = api_call("GET", "/employee/me/profile", token=None)
log_test("Unauthorized: Missing JWT Token", "Security & Auth", code == 401, code, f"Rejected with HTTP {code} Unauthorized")

# Scenario 2: Role Authorization Mismatch (Employee accessing /api/hr)
code, res = api_call("GET", "/hr/overview", token=emp_token)
log_test("Forbidden: Employee Accessing HR Overview", "Security & Auth", code in (401, 403), code, f"Access correctly denied (HTTP {code})")

# Scenario 3: Role Authorization Mismatch (Manager accessing /api/dept-head)
code, res = api_call("GET", "/dept-head/overview", token=mgr_token)
log_test("Forbidden: Manager Accessing Dept Head Overview", "Security & Auth", code in (401, 403), code, f"Access correctly denied (HTTP {code})")

# Scenario 4: Empty Request Body on Login
code, res = api_call("POST", "/auth/login", {}, token=None)
log_test("Validation: Empty Request Body on Login", "Validation & Errors", code == 400, code, f"Validation correctly rejected empty body (HTTP {code})")

# Scenario 5: Missing Required Fields in Registration
incomplete_reg = {
    "fullName": "Incomplete User",
    "email": "incomplete@infosys.com"
}
code, res = api_call("POST", "/auth/register", incomplete_reg, token=None)
log_test("Validation: Missing Password in Registration", "Validation & Errors", code == 400, code, f"Validation rejected missing fields (HTTP {code})")

# Scenario 6: Invalid Data Type (String in required numeric field)
invalid_type_skill = {
    "skillName": "Invalid Type Skill",
    "category": "Testing",
    "defaultRequiredLevel": "NOT_A_NUMBER"
}
code, res = api_call("POST", "/hr/competency/skills", invalid_type_skill, token=hr_token)
log_test("Validation: Invalid Data Type (String in Integer Field)", "Validation & Errors", code in (400, 500), code, f"Handled type mismatch (HTTP {code})")

# Scenario 7: Nonexistent Record ID on DELETE
code, res = api_call("DELETE", "/employee/me/certifications/999999", token=emp_token)
log_test("Boundary: DELETE Nonexistent Certification ID", "Validation & Errors", code in (200, 400, 404), code, f"Handled nonexistent ID gracefully (HTTP {code})")

# Scenario 8: Nonexistent Record ID on GET
code, res = api_call("GET", "/hr/employees/999999", token=hr_token)
log_test("Boundary: GET Nonexistent Employee Details", "Validation & Errors", code in (400, 404, 200), code, f"Response handled (HTTP {code})")

# Scenario 9: Invalid String in Path Variable Integer
code, res = api_call("GET", "/hr/employees/invalid-string-id", token=hr_token)
log_test("Boundary: Invalid String in Path Variable", "Validation & Errors", code == 400, code, f"Spring TypeMismatch handled (HTTP {code})")

# Scenario 10: Duplicate User Registration
dup_reg = {
    "fullName": "Arjun Mehta",
    "email": "emp.test@infosys.com",
    "password": "Password@123",
    "confirmPassword": "Password@123",
    "role": "EMPLOYEE"
}
code, res = api_call("POST", "/auth/register", dup_reg, token=None)
log_test("Conflict: Duplicate Email Registration", "Validation & Errors", code in (400, 409), code, f"Rejected duplicate email (HTTP {code})")

db_conn.close()

print("\n==================================================")
print("TEST EXECUTION SUMMARY REPORT")
print("==================================================")
print(f"Total Test Cases Executed: {results['total']}")
print(f"Passed Test Cases:         {results['passed']}")
print(f"Failed Test Cases:         {results['failed']}")
print(f"Pass Rate:                 {(results['passed']/results['total'])*100:.1f}%")

with open("scratch/crud_test_results.json", "w", encoding="utf-8") as f:
    json.dump(results, f, indent=2)
