import json
import uuid

collection_id = str(uuid.uuid4())

collection = {
    "info": {
        "_postman_id": collection_id,
        "name": "Infosys Platform - Comprehensive CRUD & Regression Suite",
        "description": "Comprehensive Postman test collection for the Organizational Knowledge Gap Intelligence Platform (Spring Boot & MySQL). Contains 124 REST endpoints categorized into 21 modules, with end-to-end sequential CRUD lifecycles (C -> R -> U -> R -> D -> R), negative/edge-case tests, dynamic ID extraction, and automated assertions.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "variable": [
        {"key": "base_url", "value": "http://localhost:8080/api", "type": "string"},
        {"key": "jwt_token", "value": "", "type": "string"},
        {"key": "employee_id", "value": "24", "type": "string"},
        {"key": "skill_id", "value": "1", "type": "string"},
        {"key": "course_id", "value": "1", "type": "string"},
        {"key": "role_id", "value": "1", "type": "string"},
        {"key": "cert_id", "value": "1", "type": "string"},
        {"key": "session_id", "value": "1", "type": "string"},
        {"key": "path_id", "value": "1", "type": "string"},
        {"key": "program_id", "value": "1", "type": "string"}
    ],
    "auth": {
        "type": "bearer",
        "bearer": [{"key": "token", "value": "{{jwt_token}}", "type": "string"}]
    },
    "item": []
}

default_test_script = [
    "pm.test('Status code is 200/201/204', function () {",
    "    pm.expect(pm.response.code).to.be.oneOf([200, 201, 204]);",
    "});",
    "pm.test('Response time is acceptable (< 2000ms)', function () {",
    "    pm.expect(pm.response.responseTime).to.be.below(2000);",
    "});"
]

def make_req(name, method, url_path, body=None, no_auth=False, custom_tests=None):
    path_part = url_path.split("?")[0]
    query_part = url_path.split("?")[1] if "?" in url_path else ""
    path_segments = [p for p in path_part.split("/") if p]
    
    url_obj = {
        "raw": "{{base_url}}" + url_path,
        "host": ["{{base_url}}"],
        "path": path_segments
    }
    if query_part:
        url_obj["query"] = []
        for q in query_part.split("&"):
            if "=" in q:
                k, v = q.split("=", 1)
                url_obj["query"].append({"key": k, "value": v})
            else:
                url_obj["query"].append({"key": q, "value": ""})

    item = {
        "id": str(uuid.uuid4()),
        "name": name,
        "request": {
            "method": method,
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "url": url_obj
        },
        "response": []
    }
    if no_auth:
        item["request"]["auth"] = {"type": "noauth"}
    if body is not None:
        item["request"]["body"] = {
            "mode": "raw",
            "raw": json.dumps(body, indent=2),
            "options": {"raw": {"language": "json"}}
        }
    
    scripts = custom_tests if custom_tests is not None else default_test_script
    item["event"] = [{
        "listen": "test",
        "script": {
            "type": "text/javascript",
            "exec": scripts
        }
    }]
    return item

def login_script(role_label):
    return [
        "pm.test('Status code is 200 OK', function () {",
        "    pm.response.to.have.status(200);",
        "});",
        "var data = pm.response.json();",
        "pm.test('Token received for " + role_label + "', function () {",
        "    pm.expect(data).to.have.property('token');",
        "});",
        "if (data.token) {",
        "    pm.environment.set('jwt_token', data.token);",
        "    pm.collectionVariables.set('jwt_token', data.token);",
        "    if (data.employeeId) {",
        "        pm.environment.set('employee_id', String(data.employeeId));",
        "        pm.collectionVariables.set('employee_id', String(data.employeeId));",
        "    }",
        "    console.log('Saved jwt_token for role: ' + data.role);",
        "}"
    ]

folders = []

# ==========================================
# 01. Authentication
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "01. Authentication",
    "description": "User registration and role logins. Login automatically extracts and sets {{jwt_token}} and {{employee_id}}.",
    "item": [
        make_req("Register New User", "POST", "/auth/register", {
            "fullName": "Test Automation User",
            "email": "auto.test@infosys.com",
            "password": "Password@123",
            "confirmPassword": "Password@123",
            "role": "EMPLOYEE"
        }, no_auth=True, custom_tests=[
            "pm.test('Registration returns 201 Created or 409 Conflict', function () {",
            "    pm.expect(pm.response.code).to.be.oneOf([201, 409]);",
            "});"
        ]),
        make_req("Login", "POST", "/auth/login", {
            "email": "emp.test@infosys.com",
            "password": "Password@123"
        }, no_auth=True, custom_tests=login_script("EMPLOYEE")),
        make_req("Login - HR Specialist", "POST", "/auth/login", {
            "email": "hr.test@infosys.com",
            "password": "Password@123"
        }, no_auth=True, custom_tests=login_script("HR")),
        make_req("Login - Team Manager", "POST", "/auth/login", {
            "email": "mgr.test@infosys.com",
            "password": "Password@123"
        }, no_auth=True, custom_tests=login_script("MANAGER")),
        make_req("Login - Mentor", "POST", "/auth/login", {
            "email": "mentor.test@infosys.com",
            "password": "Password@123"
        }, no_auth=True, custom_tests=login_script("MENTOR")),
        make_req("Login - Department Head", "POST", "/auth/login", {
            "email": "depthead.test@infosys.com",
            "password": "Password@123"
        }, no_auth=True, custom_tests=login_script("DEPT_HEAD")),
        make_req("Get Current User Profile (/api/me)", "GET", "/me"),
        make_req("Admin Ping", "GET", "/admin/ping")
    ]
})

# ==========================================
# 02. Employee Profile
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "02. Employee Profile",
    "description": "Employee profile retrieval (READ) and in-place updates (UPDATE). Profile is initialized on registration.",
    "item": [
        make_req("READ Employee Profile", "GET", "/employee/me/profile", custom_tests=[
            "pm.test('Status code is 200', function () { pm.response.to.have.status(200); });",
            "var data = pm.response.json();",
            "pm.test('Profile contains basic details', function () {",
            "    pm.expect(data).to.have.property('email');",
            "});"
        ]),
        make_req("UPDATE Employee Profile", "PUT", "/employee/me/profile", {
            "firstName": "Arjun",
            "lastName": "Mehta",
            "phone": "+91-9876543210",
            "location": "Bengaluru, India",
            "bio": "Senior Cloud Software Engineer",
            "linkedin": "https://linkedin.com/in/arjun-mehta",
            "education": "B.Tech Computer Science",
            "workExperience": "4 years in Java, Spring Boot & AWS",
            "department": "Engineering",
            "jobRole": "Senior Java Developer"
        }, custom_tests=[
            "pm.test('Profile updated successfully', function () {",
            "    pm.expect(pm.response.code).to.be.oneOf([200, 204]);",
            "});"
        ])
    ]
})

# ==========================================
# 03. Employee Skills (CRUD)
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "03. Employee Skills",
    "description": "Employee personal skill inventory. Supports CREATE/RATE, READ, and DELETE.",
    "item": [
        make_req("READ Full Skill Catalog", "GET", "/employee/skills"),
        make_req("READ My Skills Inventory", "GET", "/employee/me/skills"),
        make_req("CREATE / UPDATE Skill in My Skills", "PUT", "/employee/me/skills", {"skillId": 2}, custom_tests=[
            "pm.test('Skill saved successfully', function () { pm.response.to.have.status(200); });"
        ]),
        make_req("DELETE Skill from My Skills", "DELETE", "/employee/me/skills/2", custom_tests=[
            "pm.test('Skill removed successfully', function () { pm.response.to.have.status(200); });"
        ])
    ]
})

# ==========================================
# 04. Skill Assessments
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "04. Skill Assessments",
    "description": "Assessment questions, submission attempt (CREATE), and assessment history (READ).",
    "item": [
        make_req("READ Assessment Questions by Skill", "GET", "/employee/me/assessment/questions?skillId=1"),
        make_req("CREATE Assessment Attempt & Answers", "POST", "/employee/me/assessment/submit", {
            "skillId": 1,
            "answers": [
                {"questionId": 1, "selectedOption": "A"},
                {"questionId": 2, "selectedOption": "B"},
                {"questionId": 3, "selectedOption": "C"}
            ]
        }, custom_tests=[
            "pm.test('Assessment submission scored', function () {",
            "    pm.expect(pm.response.code).to.be.oneOf([200, 201]);",
            "});"
        ]),
        make_req("READ Assessment History", "GET", "/employee/me/assessment/history")
    ]
})

# ==========================================
# 05. Knowledge Gap Analysis
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "05. Knowledge Gap Analysis",
    "description": "Calculates gap between current proficiency and benchmark requirements across employee and organizational scopes.",
    "item": [
        make_req("READ My Knowledge Gaps", "GET", "/employee/me/gaps"),
        make_req("READ HR Organization Gap Analysis", "GET", "/hr/gaps/analysis"),
        make_req("READ Dept Head Top Gaps", "GET", "/dept-head/gaps"),
        make_req("READ Dept Head Gap Heatmap", "GET", "/dept-head/gap-heatmap")
    ]
})

# ==========================================
# 06. Job Roles
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "06. Job Roles",
    "description": "Role target configuration, role-based composite assessments, and latest role score.",
    "item": [
        make_req("READ Available Job Roles & Target", "GET", "/employee/me/job-roles"),
        make_req("UPDATE Target Job Role Selection", "PUT", "/employee/me/job-role", {"roleId": 1}),
        make_req("READ Role Assessment Questions", "GET", "/employee/me/role-assessment/questions?roleId=1"),
        make_req("CREATE Role Assessment Submit", "POST", "/employee/me/role-assessment/submit", {
            "roleId": 1,
            "answers": [
                {"questionId": 1, "selectedOption": "A"},
                {"questionId": 2, "selectedOption": "B"}
            ]
        }),
        make_req("READ Latest Role Assessment Score", "GET", "/employee/me/role-assessment/latest")
    ]
})

# ==========================================
# 07. Certifications (Sequential CRUD Suite)
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "07. Certifications",
    "description": "Full Sequential CRUD: 1. CREATE -> 2. READ -> 3. UPDATE -> 4. READ -> 5. DELETE -> 6. READ.",
    "item": [
        make_req("1. CREATE Certification", "POST", "/employee/me/certifications", {
            "title": "Postman Automated Certification",
            "issuer": "Automated Testing Authority",
            "credentialId": "CERT-POSTMAN-001",
            "issueDate": "2026-01-01",
            "expiryDate": "2028-01-01",
            "credentialUrl": "https://infosys.com/cert/001"
        }, custom_tests=[
            "pm.test('Status is 200/201', function () { pm.expect(pm.response.code).to.be.oneOf([200, 201]); });"
        ]),
        make_req("2. READ Certifications (Capture cert_id)", "GET", "/employee/me/certifications", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var found = list.find(function (c) { return c.credentialId === 'CERT-POSTMAN-001'; });",
            "pm.test('Newly created certification found', function () { pm.expect(found).to.be.an('object'); });",
            "if (found && found.certificationId) {",
            "    pm.environment.set('cert_id', String(found.certificationId));",
            "    pm.collectionVariables.set('cert_id', String(found.certificationId));",
            "    console.log('Saved cert_id: ' + found.certificationId);",
            "}"
        ]),
        make_req("3. UPDATE Certification", "PUT", "/employee/me/certifications/{{cert_id}}", {
            "title": "Postman Automated Certification Updated",
            "issuer": "Automated Testing Authority",
            "credentialId": "CERT-POSTMAN-001",
            "issueDate": "2026-01-01",
            "expiryDate": "2029-01-01",
            "credentialUrl": "https://infosys.com/cert/001-v2"
        }, custom_tests=[
            "pm.test('Updated successfully', function () { pm.response.to.have.status(200); });"
        ]),
        make_req("4. READ Verified Updated Certification", "GET", "/employee/me/certifications", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var found = list.find(function (c) { return String(c.certificationId) === pm.environment.get('cert_id'); });",
            "pm.test('Updated title is reflected', function () {",
            "    pm.expect(found.title).to.eql('Postman Automated Certification Updated');",
            "});"
        ]),
        make_req("5. DELETE Certification", "DELETE", "/employee/me/certifications/{{cert_id}}", custom_tests=[
            "pm.test('Deleted successfully', function () { pm.response.to.have.status(200); });"
        ]),
        make_req("6. READ Verify Deletion", "GET", "/employee/me/certifications", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var found = list.find(function (c) { return String(c.certificationId) === pm.environment.get('cert_id'); });",
            "pm.test('Deleted certification no longer returned', function () { pm.expect(found).to.be.undefined; });"
        ])
    ]
})

# ==========================================
# 08. Achievements
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "08. Achievements",
    "description": "Employee learning milestones, badges, and achievements feed.",
    "item": [
        make_req("READ My Achievements & Milestones", "GET", "/employee/me/achievements")
    ]
})

# ==========================================
# 09. Learning Paths
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "09. Learning Paths",
    "description": "Learning path management and employee enrollment.",
    "item": [
        make_req("READ Learning Paths List", "GET", "/hr/learning-paths"),
        make_req("CREATE Learning Path", "POST", "/hr/learning-paths", {
            "title": "Cloud DevSecOps Career Path",
            "description": "Complete pathway for automated CI/CD and cloud security",
            "courseIds": [1, 2]
        }),
        make_req("CREATE / ASSIGN Learning Path to Employees", "POST", "/hr/learning-paths/assign", {
            "pathId": 1,
            "employeeIds": [24]
        }),
        make_req("CREATE Employee Path Self-Enrollment", "POST", "/employee/me/enroll-path", {"pathId": 1})
    ]
})

# ==========================================
# 10. Courses (Sequential CRUD Suite)
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "10. Courses",
    "description": "Full Sequential CRUD for Training Courses: 1. CREATE -> 2. READ -> 3. UPDATE -> 4. READ -> 5. DELETE -> 6. READ.",
    "item": [
        make_req("1. CREATE Training Course", "POST", "/hr/courses", {
            "title": "Postman Automated Course",
            "provider": "Infosys Internal Testing",
            "skillId": 1,
            "durationHours": 10,
            "level": 3
        }, custom_tests=[
            "pm.test('Course created (200/201)', function () { pm.expect(pm.response.code).to.be.oneOf([200, 201]); });",
            "var data = pm.response.json();",
            "if (data.courseId) {",
            "    pm.environment.set('course_id', String(data.courseId));",
            "    pm.collectionVariables.set('course_id', String(data.courseId));",
            "    console.log('Saved course_id: ' + data.courseId);",
            "}"
        ]),
        make_req("2. READ Course Catalog", "GET", "/hr/courses", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var cid = pm.environment.get('course_id');",
            "var found = list.find(function (c) { return String(c.id || c.courseId) === cid; });",
            "pm.test('Newly created course is present in catalog', function () { pm.expect(found).to.be.an('object'); });"
        ]),
        make_req("3. UPDATE Training Course", "PUT", "/hr/courses/{{course_id}}", {
            "title": "Postman Automated Course Updated",
            "provider": "Infosys Internal Testing Updated",
            "skillId": 1,
            "durationHours": 15,
            "level": 4
        }, custom_tests=[
            "pm.test('Course updated successfully', function () { pm.response.to.have.status(200); });"
        ]),
        make_req("4. READ Verified Updated Course", "GET", "/hr/courses", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var cid = pm.environment.get('course_id');",
            "var found = list.find(function (c) { return String(c.id || c.courseId) === cid; });",
            "pm.test('Updated title reflected', function () { pm.expect(found.title).to.eql('Postman Automated Course Updated'); });"
        ]),
        make_req("5. DELETE Training Course", "DELETE", "/hr/courses/{{course_id}}", custom_tests=[
            "pm.test('Course deleted successfully', function () { pm.response.to.have.status(200); });"
        ]),
        make_req("6. READ Verify Course Deletion", "GET", "/hr/courses", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var cid = pm.environment.get('course_id');",
            "var found = list.find(function (c) { return String(c.id || c.courseId) === cid; });",
            "pm.test('Deleted course no longer in catalog', function () { pm.expect(found).to.be.undefined; });"
        ])
    ]
})

# ==========================================
# 11. Training Programs
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "11. Training Programs",
    "description": "Training programs catalog and employee assignments.",
    "item": [
        make_req("READ Training Programs", "GET", "/hr/training-programs"),
        make_req("CREATE Training Program", "POST", "/hr/training-programs", {
            "programName": "Q4 Cloud Acceleration Program",
            "description": "Mandatory cloud upskilling for developers",
            "courseIds": [1, 2]
        }),
        make_req("CREATE / ASSIGN Training Program to Employees", "POST", "/hr/training-programs/1/assign", {
            "employeeIds": [24]
        })
    ]
})

# ==========================================
# 12. Learning Progress
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "12. Learning Progress",
    "description": "Course enrollment and completion tracking (0-100%).",
    "item": [
        make_req("READ My Learning Enrollments & Progress", "GET", "/employee/me/learning"),
        make_req("CREATE Single Course Enrollment", "POST", "/employee/me/enroll", {"courseId": 1}),
        make_req("UPDATE Course Progress Pct (0-100%)", "PUT", "/employee/me/progress", {
            "courseId": 1,
            "progressPct": 85
        }),
        make_req("READ Org-wide Learning Progress", "GET", "/hr/learning-progress"),
        make_req("READ Dept Learning Progress", "GET", "/dept-head/learning")
    ]
})

# ==========================================
# 13. Mentorship
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "13. Mentorship",
    "description": "Mentor directories, mentorship requests, and request answers.",
    "item": [
        make_req("READ Available Mentors Directory", "GET", "/employee/me/mentors"),
        make_req("READ Peer Mentor Suggestions", "GET", "/employee/me/mentor-suggestions"),
        make_req("READ My Mentorships", "GET", "/employee/me/mentorships"),
        make_req("CREATE Mentorship Request", "POST", "/employee/me/mentorship-requests", {
            "mentorId": 26,
            "topic": "Microservices Architecture & System Design"
        }),
        make_req("UPDATE Answer Mentorship Request (ACTIVE/DECLINED)", "PUT", "/employee/me/mentorships/1", {
            "status": "ACTIVE"
        }),
        make_req("READ HR Mentors Roster", "GET", "/hr/mentors"),
        make_req("READ HR Mentor Matches", "GET", "/hr/mentors/matches"),
        make_req("CREATE HR Mentor Assignment", "POST", "/hr/mentors/assign", {
            "mentorId": 26,
            "menteeId": 24,
            "topic": "Full Stack Cloud Architecture"
        })
    ]
})

# ==========================================
# 14. Mentor Sessions (CRUD)
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "14. Mentor Sessions",
    "description": "Knowledge sharing sessions scheduling (CREATE), attendee tracking (UPDATE), and cancellation (DELETE).",
    "item": [
        make_req("READ Upcoming Knowledge Sessions", "GET", "/employee/sessions"),
        make_req("CREATE Knowledge Session Proposal (Employee)", "POST", "/employee/sessions", {
            "title": "Spring Boot 3 Deep Dive",
            "description": "Virtual threads and memory footprint",
            "scheduledAt": "2026-12-15T15:00:00"
        }),
        make_req("CREATE Register for Session", "POST", "/employee/sessions/1/register"),
        make_req("DELETE Unregister from Session", "DELETE", "/employee/sessions/1/register"),
        make_req("READ Mentor Scheduled Sessions", "GET", "/mentor/sessions"),
        make_req("CREATE Mentor Knowledge Session", "POST", "/mentor/sessions", {
            "title": "Advanced SQL Tuning",
            "skillId": 1,
            "scheduledAt": "2026-12-20T14:00:00",
            "durationMin": 60,
            "mode": "ONLINE",
            "location": "https://meet.google.com/test-sql-session",
            "description": "Query plan indexing and optimization"
        }),
        make_req("READ Session Attendees List", "GET", "/mentor/sessions/1/attendees"),
        make_req("UPDATE Session Attendance", "PUT", "/mentor/sessions/1/attendance", {
            "attendedEmployeeIds": [24]
        }),
        make_req("DELETE / Cancel Mentor Session", "DELETE", "/mentor/sessions/1")
    ]
})

# ==========================================
# 15. Notifications
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "15. Notifications",
    "description": "In-app notifications across Employee, Manager, Mentor, HR, and Dept Head.",
    "item": [
        make_req("READ Employee Notifications", "GET", "/employee/me/notifications"),
        make_req("UPDATE Mark Single Notification Read", "PUT", "/employee/me/notifications/1/read"),
        make_req("UPDATE Mark All Notifications Read", "PUT", "/employee/me/notifications/read-all"),
        make_req("CREATE HR Broadcast Notification", "POST", "/hr/notifications/send", {
            "title": "Quarterly Skill Assessment Cycle",
            "message": "Please review and complete your competency tests.",
            "target": "ALL"
        }),
        make_req("READ HR Notifications History", "GET", "/hr/notifications"),
        make_req("READ Manager Notifications", "GET", "/manager/notifications"),
        make_req("UPDATE Mark Manager Notification Read", "PUT", "/manager/notifications/1/read"),
        make_req("READ Mentor Notifications", "GET", "/mentor/notifications"),
        make_req("READ Dept Head Notifications", "GET", "/dept-head/notifications"),
        make_req("UPDATE Mark Dept Head Notification Read", "POST", "/dept-head/notifications/1/read")
    ]
})

# ==========================================
# 16. Messaging
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "16. Messaging",
    "description": "Peer connection requests (ed_connections) and 1-on-1 direct chat threads (ed_messages).",
    "item": [
        make_req("READ Messaging Summary (Unread Count)", "GET", "/messages/summary"),
        make_req("READ Active Conversations", "GET", "/messages/conversations"),
        make_req("READ Available Mentors for Direct Chat", "GET", "/messages/mentors"),
        make_req("READ Directory Search People", "GET", "/messages/people?q=arjun"),
        make_req("READ Pending Follow Requests", "GET", "/messages/requests"),
        make_req("CREATE Follow Request to Employee 30", "POST", "/messages/follow/30"),
        make_req("UPDATE Accept Follow Request from Employee 24", "PUT", "/messages/follow/24/accept"),
        make_req("UPDATE Decline Follow Request", "PUT", "/messages/follow/24/decline"),
        make_req("DELETE Unfollow User", "DELETE", "/messages/follow/30"),
        make_req("CREATE Send Chat Message to Mentor 26", "POST", "/messages/with/26", {
            "content": "Hi Mentor! Could you please review my assessment results?"
        }),
        make_req("READ Chat Thread Messages with Mentor 26", "GET", "/messages/with/26")
    ]
})

# ==========================================
# 17. HR Dashboard & Competency Skills (CRUD)
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "17. HR Dashboard",
    "description": "Workforce analytics and Competency Skills Full Sequential CRUD.",
    "item": [
        make_req("READ HR Overview KPIs", "GET", "/hr/overview"),
        make_req("READ Employee Roster", "GET", "/hr/employees?dept=all&status=all"),
        make_req("READ Employee Details by ID", "GET", "/hr/employees/24"),
        make_req("READ Organization Metadata", "GET", "/hr/meta"),
        make_req("READ Competency Skill Categories", "GET", "/hr/competency/categories"),
        make_req("READ Feedback & Training Effectiveness", "GET", "/hr/feedback-effectiveness"),
        make_req("READ Comprehensive Reports Data", "GET", "/hr/reports"),
        # Sequential CRUD on ed_skills
        make_req("1. CREATE Competency Skill", "POST", "/hr/competency/skills", {
            "skillName": "Postman Automated Competency Skill",
            "category": "Quality Assurance",
            "defaultRequiredLevel": 3
        }, custom_tests=[
            "pm.test('Skill created (200/201)', function () { pm.expect(pm.response.code).to.be.oneOf([200, 201]); });",
            "var data = pm.response.json();",
            "if (data.skillId) {",
            "    pm.environment.set('skill_id', String(data.skillId));",
            "    pm.collectionVariables.set('skill_id', String(data.skillId));",
            "    console.log('Saved skill_id: ' + data.skillId);",
            "}"
        ]),
        make_req("2. READ Competency Skills", "GET", "/hr/competency/skills", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var sid = pm.environment.get('skill_id');",
            "var found = list.find(function (s) { return String(s.id || s.skillId) === sid; });",
            "pm.test('Newly created skill present in catalog', function () { pm.expect(found).to.be.an('object'); });"
        ]),
        make_req("3. UPDATE Competency Skill", "PUT", "/hr/competency/skills/{{skill_id}}", {
            "skillName": "Postman Automated Competency Skill Updated",
            "category": "Quality Assurance",
            "defaultRequiredLevel": 4
        }, custom_tests=[
            "pm.test('Skill updated successfully', function () { pm.response.to.have.status(200); });"
        ]),
        make_req("4. READ Verified Updated Competency Skill", "GET", "/hr/competency/skills", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var sid = pm.environment.get('skill_id');",
            "var found = list.find(function (s) { return String(s.id || s.skillId) === sid; });",
            "pm.test('Updated name reflected', function () { pm.expect(found.name || found.skillName).to.eql('Postman Automated Competency Skill Updated'); });"
        ]),
        make_req("5. DELETE Competency Skill", "DELETE", "/hr/competency/skills/{{skill_id}}", custom_tests=[
            "pm.test('Skill deleted successfully', function () { pm.response.to.have.status(200); });"
        ]),
        make_req("6. READ Verify Competency Skill Deletion", "GET", "/hr/competency/skills", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var sid = pm.environment.get('skill_id');",
            "var found = list.find(function (s) { return String(s.id || s.skillId) === sid; });",
            "pm.test('Deleted skill absent from list', function () { pm.expect(found).to.be.undefined; });"
        ]),
        # Sequential CRUD on ed_job_roles
        make_req("1. CREATE Job Role", "POST", "/hr/roles", {
            "roleName": "Postman Automated Lead",
            "description": "Role generated for automated verification"
        }, custom_tests=[
            "pm.test('Role created (200/201)', function () { pm.expect(pm.response.code).to.be.oneOf([200, 201]); });",
            "var data = pm.response.json();",
            "if (data.roleId) {",
            "    pm.environment.set('role_id', String(data.roleId));",
            "    pm.collectionVariables.set('role_id', String(data.roleId));",
            "    console.log('Saved role_id: ' + data.roleId);",
            "}"
        ]),
        make_req("2. READ Job Roles", "GET", "/hr/roles", custom_tests=[
            "pm.test('Status is 200', function () { pm.response.to.have.status(200); });",
            "var list = pm.response.json();",
            "var rid = pm.environment.get('role_id');",
            "var found = list.find(function (r) { return String(r.id || r.roleId) === rid; });",
            "pm.test('Newly created role present in list', function () { pm.expect(found).to.be.an('object'); });"
        ]),
        make_req("3. UPDATE Job Role", "PUT", "/hr/roles/{{role_id}}", {
            "roleName": "Postman Automated Lead Updated",
            "description": "Updated role description"
        }, custom_tests=[
            "pm.test('Role updated successfully', function () { pm.response.to.have.status(200); });"
        ]),
        make_req("4. CREATE Assign Skill to Role", "POST", "/hr/roles/{{role_id}}/skills", {
            "skillId": 1,
            "weightPct": 30
        }),
        make_req("5. DELETE Remove Skill from Role", "DELETE", "/hr/roles/{{role_id}}/skills/1"),
        make_req("6. DELETE Job Role", "DELETE", "/hr/roles/{{role_id}}", custom_tests=[
            "pm.test('Role deleted successfully', function () { pm.response.to.have.status(200); });"
        ])
    ]
})

# ==========================================
# 18. Manager Dashboard
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "18. Manager Dashboard",
    "description": "Team notifications and alerts management for managers.",
    "item": [
        make_req("READ Manager Notifications", "GET", "/manager/notifications"),
        make_req("UPDATE Mark Single Manager Alert Read", "PUT", "/manager/notifications/1/read"),
        make_req("UPDATE Mark All Manager Alerts Read", "PUT", "/manager/notifications/read-all")
    ]
})

# ==========================================
# 19. Mentor Dashboard
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "19. Mentor Dashboard",
    "description": "Mentor overview metrics, mentees progress, and teaching profile.",
    "item": [
        make_req("READ Mentor Overview Metrics", "GET", "/mentor/overview"),
        make_req("READ Skill Gap Insights for Planning", "GET", "/mentor/skill-gaps"),
        make_req("READ Mentor Requests", "GET", "/mentor/requests"),
        make_req("UPDATE Answer Mentorship Request", "PUT", "/mentor/requests/1", {"status": "ACTIVE"}),
        make_req("READ Active Mentees", "GET", "/mentor/mentees"),
        make_req("READ Mentor Teaching Profile", "GET", "/mentor/profile"),
        make_req("UPDATE Mentor Teaching Profile", "PUT", "/mentor/profile", {
            "headline": "Lead Architect & Cloud Mentor",
            "bio": "Specialized in cloud microservices and scalable architecture",
            "accepting": 1,
            "skillIds": [1, 2]
        })
    ]
})

# ==========================================
# 20. Department Head Dashboard
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "20. Department Head Dashboard",
    "description": "Strategic department KPIs, skill matrix, capability risks, announcements, and CSV exports.",
    "item": [
        make_req("READ Dept Head Strategic KPIs", "GET", "/dept-head/overview"),
        make_req("READ Department Teams Breakdown", "GET", "/dept-head/teams"),
        make_req("READ Department Skill Matrix", "GET", "/dept-head/skill-matrix?category=all"),
        make_req("READ Workforce Distribution", "GET", "/dept-head/workforce"),
        make_req("READ Capability Risk Alerts", "GET", "/dept-head/risks?severity=all"),
        make_req("UPDATE Acknowledge Capability Risk", "POST", "/dept-head/risks/1/acknowledge"),
        make_req("READ Department Announcements", "GET", "/dept-head/announcements"),
        make_req("CREATE Department Announcement", "POST", "/dept-head/announcements", {
            "title": "Quarterly Technical Enablement Goals",
            "body": "Focus for this quarter is AWS & Spring Boot microservices."
        }),
        make_req("READ Skill Matrix CSV Export", "GET", "/dept-head/reports/skill-matrix-csv"),
        make_req("READ Gap Report CSV Export", "GET", "/dept-head/reports/gap-report-csv"),
        make_req("READ Workforce CSV Export", "GET", "/dept-head/reports/workforce-csv")
    ]
})

# ==========================================
# 21. Negative & Boundary Scenarios
# ==========================================
folders.append({
    "id": str(uuid.uuid4()),
    "name": "21. Negative & Boundary Scenarios",
    "description": "Validates error handling: Unauthorized (401), Forbidden role mismatch, empty bodies (400), missing fields, nonexistent IDs (404), type mismatches, and duplicates (409).",
    "item": [
        make_req("Unauthorized: Request Without Token", "GET", "/employee/me/profile", no_auth=True, custom_tests=[
            "pm.test('Rejected with 401 Unauthorized', function () { pm.response.to.have.status(401); });"
        ]),
        make_req("Forbidden: Manager Accessing Dept Head API", "GET", "/dept-head/overview", custom_tests=[
            "pm.test('Access denied (HTTP 401/403)', function () { pm.expect(pm.response.code).to.be.oneOf([401, 403]); });"
        ]),
        make_req("Validation: Empty Body on Login", "POST", "/auth/login", {}, no_auth=True, custom_tests=[
            "pm.test('Rejected with 400 Bad Request', function () { pm.response.to.have.status(400); });"
        ]),
        make_req("Validation: Missing Password in Registration", "POST", "/auth/register", {
            "fullName": "Incomplete User",
            "email": "incomplete@infosys.com"
        }, no_auth=True, custom_tests=[
            "pm.test('Rejected with 400 Bad Request', function () { pm.response.to.have.status(400); });"
        ]),
        make_req("Validation: Invalid String in Integer Field", "POST", "/hr/competency/skills", {
            "skillName": "Invalid Level Skill",
            "defaultRequiredLevel": "NOT_A_NUMBER"
        }, custom_tests=[
            "pm.test('Handled invalid data type (400/500)', function () { pm.expect(pm.response.code).to.be.oneOf([400, 500]); });"
        ]),
        make_req("Boundary: DELETE Nonexistent Certification ID", "DELETE", "/employee/me/certifications/999999", custom_tests=[
            "pm.test('Handled nonexistent ID gracefully (404/400/200)', function () { pm.expect(pm.response.code).to.be.oneOf([200, 400, 404]); });"
        ]),
        make_req("Boundary: GET Nonexistent Employee Details", "GET", "/hr/employees/999999", custom_tests=[
            "pm.test('Handled nonexistent employee (404/400/200)', function () { pm.expect(pm.response.code).to.be.oneOf([200, 400, 404]); });"
        ]),
        make_req("Boundary: Invalid String in Path Variable", "GET", "/hr/employees/invalid-id-string", custom_tests=[
            "pm.test('Rejected with 400 Type Mismatch', function () { pm.response.to.have.status(400); });"
        ]),
        make_req("Conflict: Duplicate Email Registration", "POST", "/auth/register", {
            "fullName": "Arjun Mehta",
            "email": "emp.test@infosys.com",
            "password": "Password@123",
            "confirmPassword": "Password@123",
            "role": "EMPLOYEE"
        }, no_auth=True, custom_tests=[
            "pm.test('Rejected duplicate with 409 Conflict or 400', function () { pm.expect(pm.response.code).to.be.oneOf([400, 409]); });"
        ])
    ]
})

collection["item"] = folders

with open("Postman_Testing/Infosys_Project_API_Collection.json", "w", encoding="utf-8") as f:
    json.dump(collection, f, indent=2)

print(f"Generated comprehensive collection with {len(folders)} folders and {sum(len(f['item']) for f in folders)} requests.")

# Generate updated Environment JSON with all needed variables
env_data = {
    "id": str(uuid.uuid4()),
    "name": "WKI Local Backend Environment",
    "values": [
        {"key": "base_url", "value": "http://localhost:8080/api", "type": "default", "enabled": True},
        {"key": "jwt_token", "value": "", "type": "secret", "enabled": True},
        {"key": "employee_id", "value": "24", "type": "default", "enabled": True},
        {"key": "skill_id", "value": "1", "type": "default", "enabled": True},
        {"key": "course_id", "value": "1", "type": "default", "enabled": True},
        {"key": "role_id", "value": "1", "type": "default", "enabled": True},
        {"key": "cert_id", "value": "1", "type": "default", "enabled": True},
        {"key": "session_id", "value": "1", "type": "default", "enabled": True},
        {"key": "path_id", "value": "1", "type": "default", "enabled": True},
        {"key": "program_id", "value": "1", "type": "default", "enabled": True}
    ],
    "_postman_variable_scope": "environment",
    "_postman_exported_at": "2026-11-17T13:30:00.000Z",
    "_postman_exported_using": "Postman/11.0.0"
}

with open("Postman_Testing/Infosys_Project_Environment.json", "w", encoding="utf-8") as f:
    json.dump(env_data, f, indent=2)

print("Generated comprehensive environment file successfully.")
