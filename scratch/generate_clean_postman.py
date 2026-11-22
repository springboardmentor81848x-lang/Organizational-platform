import json
import uuid

col_id = str(uuid.uuid4())

collection = {
    "info": {
        "_postman_id": col_id,
        "name": "Infosys Project - Complete Backend API Suite",
        "description": "Complete executable Postman Collection for the Organizational Knowledge Gap Intelligence Platform (Spring Boot & MySQL), containing all 124 REST endpoints organized into module folders, with JWT authentication, request bodies, and automated assertions.",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    "variable": [
        {"key": "base_url", "value": "http://localhost:8080/api", "type": "string"},
        {"key": "jwt_token", "value": "", "type": "string"}
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
    "pm.test('Response time is within acceptable limits (< 2000ms)', function () {",
    "    pm.expect(pm.response.responseTime).to.be.below(2000);",
    "});"
]

login_test_script = [
    "pm.test('Status code is 200 OK', function () {",
    "    pm.response.to.have.status(200);",
    "});",
    "var data = pm.response.json();",
    "pm.test('Response contains valid JWT token and role', function () {",
    "    pm.expect(data).to.have.property('token');",
    "    pm.expect(data).to.have.property('role');",
    "});",
    "if (data.token) {",
    "    pm.environment.set('jwt_token', data.token);",
    "    pm.collectionVariables.set('jwt_token', data.token);",
    "    console.log('Successfully saved jwt_token for role: ' + data.role);",
    "}"
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

folders = []

# 01. Authentication
folders.append({
    "id": str(uuid.uuid4()),
    "name": "01. Authentication",
    "description": "User registration and authentication. Login automatically saves generated JWT into jwt_token variable.",
    "item": [
        make_req("Register New User", "POST", "/auth/register", {
            "fullName": "Test Employee",
            "email": "test.emp@infosys.com",
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
        }, no_auth=True, custom_tests=login_test_script)
    ]
})

# 02. Core Identity & Admin
folders.append({
    "id": str(uuid.uuid4()),
    "name": "02. Core Identity & Admin",
    "description": "Token verification and administrator status check.",
    "item": [
        make_req("Get Current User Profile (/api/me)", "GET", "/me"),
        make_req("Admin Ping (Requires ADMIN role)", "GET", "/admin/ping")
    ]
})

# 03. Employee Profile Management
folders.append({
    "id": str(uuid.uuid4()),
    "name": "03. Employee Profile Management",
    "description": "Employee profile retrieval and update operations.",
    "item": [
        make_req("Get Employee Profile", "GET", "/employee/me/profile"),
        make_req("Update Employee Profile", "PUT", "/employee/me/profile", {
            "firstName": "Arjun",
            "lastName": "Mehta",
            "phone": "+91-9876543210",
            "location": "Bengaluru, India",
            "bio": "Senior Full Stack Engineer specializing in enterprise cloud systems",
            "linkedin": "https://linkedin.com/in/arjun-mehta",
            "education": "B.Tech Computer Science, 2020",
            "workExperience": "4 years in Java, Spring Boot, React & AWS",
            "department": "Engineering",
            "jobRole": "Senior Java Developer"
        })
    ]
})

# 04. Skills Management
folders.append({
    "id": str(uuid.uuid4()),
    "name": "04. Skills Management",
    "description": "Global skill catalog and employee personal skill inventory.",
    "item": [
        make_req("Get Full Skill Catalog", "GET", "/employee/skills"),
        make_req("Get My Skills", "GET", "/employee/me/skills"),
        make_req("Add or Rate Skill in My Skills", "PUT", "/employee/me/skills", {"skillId": 1}),
        make_req("Remove Skill from My Skills", "DELETE", "/employee/me/skills/1")
    ]
})

# 05. Skill Assessment
folders.append({
    "id": str(uuid.uuid4()),
    "name": "05. Skill Assessment",
    "description": "Skill question retrieval, test submission, and assessment history.",
    "item": [
        make_req("Get Assessment Questions by Skill ID", "GET", "/employee/me/assessment/questions?skillId=1"),
        make_req("Submit Assessment Answers", "POST", "/employee/me/assessment/submit", {
            "skillId": 1,
            "answers": [
                {"questionId": 1, "selectedOption": "A"},
                {"questionId": 2, "selectedOption": "B"},
                {"questionId": 3, "selectedOption": "C"}
            ]
        }),
        make_req("Get Assessment History", "GET", "/employee/me/assessment/history")
    ]
})

# 06. Knowledge Gap Analysis
folders.append({
    "id": str(uuid.uuid4()),
    "name": "06. Knowledge Gap Analysis",
    "description": "Computes current gaps between assessed proficiency and required benchmark.",
    "item": [
        make_req("Get Knowledge Gaps Analysis", "GET", "/employee/me/gaps")
    ]
})

# 07. Learning Recommendations
folders.append({
    "id": str(uuid.uuid4()),
    "name": "07. Learning Recommendations",
    "description": "Automated course and learning path recommendations tailored to open gaps.",
    "item": [
        make_req("Get Course & Learning Path Recommendations", "GET", "/employee/me/recommendations")
    ]
})

# 08. Learning Path and Course Enrollment
folders.append({
    "id": str(uuid.uuid4()),
    "name": "08. Learning Path and Course Enrollment",
    "description": "Enrollment and progress updates for courses and structured learning paths.",
    "item": [
        make_req("Get My Learning Enrollments & Progress", "GET", "/employee/me/learning"),
        make_req("Enroll in Single Course", "POST", "/employee/me/enroll", {"courseId": 1}),
        make_req("Enroll in Complete Learning Path", "POST", "/employee/me/enroll-path", {"pathId": 1}),
        make_req("Update Course Progress (0-100%)", "PUT", "/employee/me/progress", {
            "courseId": 1,
            "progressPct": 75
        })
    ]
})

# 09. Mentorship
folders.append({
    "id": str(uuid.uuid4()),
    "name": "09. Mentorship",
    "description": "Mentor matching, mentorship requests, session management and proposals.",
    "item": [
        make_req("Get My Mentorships", "GET", "/employee/me/mentorships"),
        make_req("Get Available Mentors Directory", "GET", "/employee/me/mentors"),
        make_req("Get Peer Mentor Suggestions", "GET", "/employee/me/mentor-suggestions"),
        make_req("Request Mentorship", "POST", "/employee/me/mentorship-requests", {
            "mentorId": 3,
            "topic": "Microservices Architecture & Performance Optimization"
        }),
        make_req("Answer Mentorship Request (ACTIVE/DECLINED/COMPLETED)", "PUT", "/employee/me/mentorships/1", {
            "status": "ACTIVE"
        }),
        make_req("Get Upcoming Knowledge Sessions", "GET", "/employee/sessions"),
        make_req("Register for Knowledge Session", "POST", "/employee/sessions/1/register"),
        make_req("Unregister from Knowledge Session", "DELETE", "/employee/sessions/1/register"),
        make_req("Propose Knowledge Sharing Session", "POST", "/employee/sessions", {
            "title": "Spring Boot 3 Deep Dive & Best Practices",
            "description": "AOT compilation, virtual threads, and REST optimizations",
            "scheduledAt": "2026-12-01T15:00:00"
        })
    ]
})

# 10. Notifications
folders.append({
    "id": str(uuid.uuid4()),
    "name": "10. Notifications",
    "description": "Employee notification feed, single mark read, and mark all read.",
    "item": [
        make_req("Get Notifications List & Unread Count", "GET", "/employee/me/notifications"),
        make_req("Mark Single Notification Read", "PUT", "/employee/me/notifications/1/read"),
        make_req("Mark All Notifications Read", "PUT", "/employee/me/notifications/read-all")
    ]
})

# 11. Certifications and Achievements
folders.append({
    "id": str(uuid.uuid4()),
    "name": "11. Certifications and Achievements",
    "description": "Industry certification portfolio CRUD and milestone progress badges.",
    "item": [
        make_req("Get My Certifications", "GET", "/employee/me/certifications"),
        make_req("Add New Certification", "POST", "/employee/me/certifications", {
            "title": "AWS Certified Solutions Architect",
            "issuer": "Amazon Web Services",
            "credentialId": "AWS-SA-987654",
            "issueDate": "2025-06-01",
            "expiryDate": "2028-06-01",
            "credentialUrl": "https://aws.amazon.com/verify"
        }),
        make_req("Update Certification", "PUT", "/employee/me/certifications/1", {
            "title": "AWS Certified Solutions Architect - Associate",
            "issuer": "Amazon Web Services",
            "credentialId": "AWS-SA-987654-V2",
            "issueDate": "2025-06-01",
            "expiryDate": "2028-06-01",
            "credentialUrl": "https://aws.amazon.com/verify"
        }),
        make_req("Delete Certification", "DELETE", "/employee/me/certifications/1"),
        make_req("Get Achievements & Progress Milestones", "GET", "/employee/me/achievements")
    ]
})

# 12. Role-Based Assessments
folders.append({
    "id": str(uuid.uuid4()),
    "name": "12. Role-Based Assessments",
    "description": "Role target configuration, multi-skill role tests, and composite evaluation.",
    "item": [
        make_req("Get Available Job Roles & Target Role", "GET", "/employee/me/job-roles"),
        make_req("Select Target Job Role", "PUT", "/employee/me/job-role", {"roleId": 1}),
        make_req("Get Role Assessment Questions", "GET", "/employee/me/role-assessment/questions?roleId=1"),
        make_req("Submit Role Assessment Answers", "POST", "/employee/me/role-assessment/submit", {
            "roleId": 1,
            "answers": [
                {"questionId": 1, "selectedOption": "A"},
                {"questionId": 2, "selectedOption": "B"}
            ]
        }),
        make_req("Get Latest Role Assessment Score & Breakdown", "GET", "/employee/me/role-assessment/latest")
    ]
})

# 13. AI Recommendations and Chatbot
folders.append({
    "id": str(uuid.uuid4()),
    "name": "13. AI Recommendations and Chatbot",
    "description": "AI-powered personalized study roadmaps and real-time knowledge assistant chat.",
    "item": [
        make_req("Get AI-Generated Study Plan", "GET", "/employee/me/ai-recommendations"),
        make_req("Force Refresh AI Study Plan", "POST", "/employee/me/ai-recommendations/refresh"),
        make_req("Ask Knowledge Gap AI Chatbot", "POST", "/employee/me/chat", {
            "message": "Why is database indexing critical for high-throughput query performance?",
            "history": []
        })
    ]
})

# 14. HR Dashboard
folders.append({
    "id": str(uuid.uuid4()),
    "name": "14. HR Dashboard",
    "description": "Workforce competency framework, skill catalog, courses, paths, training programs, and analytics.",
    "item": [
        make_req("Get HR Overview KPI Metrics", "GET", "/hr/overview"),
        make_req("Get Filtered Employees List", "GET", "/hr/employees?dept=all&status=all"),
        make_req("Get Employee Detailed Skill Profile", "GET", "/hr/employees/1"),
        make_req("Get Organization Metadata (Depts, Roles, Designations)", "GET", "/hr/meta"),
        make_req("Get Competency Framework Skills", "GET", "/hr/competency/skills"),
        make_req("Get Competency Skill Categories", "GET", "/hr/competency/categories"),
        make_req("Create Competency Skill", "POST", "/hr/competency/skills", {
            "skillName": "Kubernetes & Cloud Native",
            "category": "DevOps",
            "defaultRequiredLevel": 4
        }),
        make_req("Update Competency Skill", "PUT", "/hr/competency/skills/1", {
            "skillName": "Kubernetes Administration",
            "category": "DevOps",
            "defaultRequiredLevel": 4
        }),
        make_req("Delete Competency Skill", "DELETE", "/hr/competency/skills/999"),
        make_req("Get Target Job Roles", "GET", "/hr/roles"),
        make_req("Create Target Job Role", "POST", "/hr/roles", {
            "roleName": "DevOps Lead",
            "description": "Manages enterprise CI/CD pipelines and Kubernetes clusters"
        }),
        make_req("Update Target Job Role", "PUT", "/hr/roles/1", {
            "roleName": "Senior DevOps Lead",
            "description": "Oversees cloud infrastructure and reliability"
        }),
        make_req("Assign Skill to Job Role with Weight Pct", "POST", "/hr/roles/1/skills", {
            "skillId": 1,
            "weightPct": 30
        }),
        make_req("Remove Skill from Job Role", "DELETE", "/hr/roles/1/skills/1"),
        make_req("Delete Job Role", "DELETE", "/hr/roles/999"),
        make_req("Get Organization-wide Gap Analysis", "GET", "/hr/gaps/analysis"),
        make_req("Get Course Catalog", "GET", "/hr/courses"),
        make_req("Create New Training Course", "POST", "/hr/courses", {
            "title": "Mastering Docker & Containers",
            "provider": "Coursera",
            "skillId": 1,
            "durationHours": 12,
            "courseLevel": "Intermediate"
        }),
        make_req("Update Training Course", "PUT", "/hr/courses/1", {
            "title": "Mastering Docker & Kubernetes",
            "provider": "Coursera",
            "skillId": 1,
            "durationHours": 14,
            "courseLevel": "Advanced"
        }),
        make_req("Delete Training Course", "DELETE", "/hr/courses/999"),
        make_req("Get Learning Paths List", "GET", "/hr/learning-paths"),
        make_req("Create Learning Path", "POST", "/hr/learning-paths", {
            "title": "Full Stack Cloud Path",
            "description": "Comprehensive pathway from frontend to cloud deployment",
            "courseIds": [1, 2]
        }),
        make_req("Assign Learning Path to Employees", "POST", "/hr/learning-paths/assign", {
            "pathId": 1,
            "employeeIds": [1, 2]
        }),
        make_req("Get Training Programs", "GET", "/hr/training-programs"),
        make_req("Create Training Program", "POST", "/hr/training-programs", {
            "programName": "Q4 Cloud Upskilling",
            "description": "Intensive cloud migration training",
            "courseIds": [1, 2]
        }),
        make_req("Assign Training Program to Employees", "POST", "/hr/training-programs/1/assign", {
            "employeeIds": [1]
        }),
        make_req("Get Mentors Directory", "GET", "/hr/mentors"),
        make_req("Get Recommended Mentor-Mentee Matches", "GET", "/hr/mentors/matches"),
        make_req("Assign Mentor to Mentee", "POST", "/hr/mentors/assign", {
            "mentorId": 3,
            "menteeId": 1,
            "topic": "Cloud Architecture Mentorship"
        }),
        make_req("Get Organization Learning Progress", "GET", "/hr/learning-progress"),
        make_req("Get Feedback & Training Effectiveness", "GET", "/hr/feedback-effectiveness"),
        make_req("Get Comprehensive Reports Data", "GET", "/hr/reports"),
        make_req("Get Notification History", "GET", "/hr/notifications"),
        make_req("Broadcast Notification to Employees", "POST", "/hr/notifications/send", {
            "title": "Annual Skill Assessment Cycle",
            "message": "Please complete your role assessments before Friday.",
            "target": "ALL"
        })
    ]
})

# 15. Team Manager Dashboard
folders.append({
    "id": str(uuid.uuid4()),
    "name": "15. Team Manager Dashboard",
    "description": "Manager team notification feed, single mark read, and mark all read.",
    "item": [
        make_req("Get Manager Notifications", "GET", "/manager/notifications"),
        make_req("Mark Single Manager Notification Read", "PUT", "/manager/notifications/1/read"),
        make_req("Mark All Manager Notifications Read", "PUT", "/manager/notifications/read-all")
    ]
})

# 16. Mentor Dashboard
folders.append({
    "id": str(uuid.uuid4()),
    "name": "16. Mentor Dashboard",
    "description": "Mentor overview, mentorship requests, scheduled sessions, attendee tracking, and teaching profile.",
    "item": [
        make_req("Get Mentor Overview Statistics", "GET", "/mentor/overview"),
        make_req("Get Skill Gap Insights for Planning", "GET", "/mentor/skill-gaps"),
        make_req("Get Mentorship Requests", "GET", "/mentor/requests"),
        make_req("Answer Mentorship Request (ACTIVE/DECLINED)", "PUT", "/mentor/requests/1", {
            "status": "ACTIVE"
        }),
        make_req("Get Active Mentees Progress", "GET", "/mentor/mentees"),
        make_req("Get Scheduled Sessions", "GET", "/mentor/sessions"),
        make_req("Schedule Knowledge Sharing Session", "POST", "/mentor/sessions", {
            "title": "Effective SQL Query Optimization",
            "skillId": 1,
            "scheduledAt": "2026-12-05T14:00:00",
            "durationMin": 60,
            "mode": "ONLINE",
            "location": "https://meet.google.com/xyz-1234",
            "description": "Query profiling, index scans vs table scans"
        }),
        make_req("Cancel Knowledge Session", "DELETE", "/mentor/sessions/1"),
        make_req("Get Session Attendees List", "GET", "/mentor/sessions/1/attendees"),
        make_req("Mark Session Attendance", "PUT", "/mentor/sessions/1/attendance", {
            "attendedEmployeeIds": [1]
        }),
        make_req("Get Mentor Teaching Profile", "GET", "/mentor/profile"),
        make_req("Update Mentor Teaching Profile", "PUT", "/mentor/profile", {
            "headline": "Lead Architect, 10+ years experience",
            "bio": "Specialized in distributed systems and performance tuning",
            "accepting": 1,
            "skillIds": [1, 2]
        }),
        make_req("Get Mentor Notifications", "GET", "/mentor/notifications"),
        make_req("Mark Single Mentor Notification Read", "PUT", "/mentor/notifications/1/read"),
        make_req("Mark All Mentor Notifications Read", "PUT", "/mentor/notifications/read-all")
    ]
})

# 17. Department Head Dashboard
folders.append({
    "id": str(uuid.uuid4()),
    "name": "17. Department Head Dashboard",
    "description": "Strategic department KPIs, team breakdowns, skill matrix, gap heatmaps, capability risks, announcements, and CSV exports.",
    "item": [
        make_req("Get Department Overview Strategic KPIs", "GET", "/dept-head/overview"),
        make_req("Get Department Teams Breakdown", "GET", "/dept-head/teams"),
        make_req("Get Department Skill Matrix", "GET", "/dept-head/skill-matrix?category=all"),
        make_req("Get Department Top Gaps Analysis", "GET", "/dept-head/gaps"),
        make_req("Get Department Skill Gap Heatmap", "GET", "/dept-head/gap-heatmap"),
        make_req("Get Workforce Competency Distribution", "GET", "/dept-head/workforce"),
        make_req("Get Department Training & Learning Progress", "GET", "/dept-head/learning"),
        make_req("Get Capability Risk Alerts", "GET", "/dept-head/risks?severity=all"),
        make_req("Acknowledge Capability Risk Alert", "POST", "/dept-head/risks/1/acknowledge"),
        make_req("Get Department Announcements", "GET", "/dept-head/announcements"),
        make_req("Publish Department Announcement", "POST", "/dept-head/announcements", {
            "title": "Quarterly Technical Competency Goals",
            "body": "Focus for this quarter is Cloud & AI enablement across all squads."
        }),
        make_req("Get Department Notifications", "GET", "/dept-head/notifications"),
        make_req("Mark Single Department Notification Read", "POST", "/dept-head/notifications/1/read"),
        make_req("Mark All Department Notifications Read", "POST", "/dept-head/notifications/read-all"),
        make_req("Download Skill Matrix CSV Report", "GET", "/dept-head/reports/skill-matrix-csv"),
        make_req("Download Gap Report CSV Report", "GET", "/dept-head/reports/gap-report-csv"),
        make_req("Download Workforce CSV Report", "GET", "/dept-head/reports/workforce-csv")
    ]
})

# 18. Messaging System
folders.append({
    "id": str(uuid.uuid4()),
    "name": "18. Messaging System",
    "description": "Colleague discovery, follow/connect requests, direct conversations, and message thread history.",
    "item": [
        make_req("Get Messaging Summary (Unread count)", "GET", "/messages/summary"),
        make_req("Get Active Conversations List", "GET", "/messages/conversations"),
        make_req("Get Available Mentors for Chat", "GET", "/messages/mentors"),
        make_req("Search Directory Colleagues to Message", "GET", "/messages/people?q=arjun"),
        make_req("Get Pending Follow / Chat Requests", "GET", "/messages/requests"),
        make_req("Send Follow / Chat Request to User", "POST", "/messages/follow/2"),
        make_req("Accept Follow Request", "PUT", "/messages/follow/1/accept"),
        make_req("Decline Follow Request", "PUT", "/messages/follow/1/decline"),
        make_req("Unfollow User", "DELETE", "/messages/follow/2"),
        make_req("Get Chat Thread Messages with User", "GET", "/messages/with/2"),
        make_req("Send Chat Message to User", "POST", "/messages/with/2", {
            "content": "Hi! Could you share the reference materials from yesterday's session?"
        })
    ]
})

collection["item"] = folders

with open("Postman_Testing/Infosys_Project_API_Collection.json", "w", encoding="utf-8") as f:
    json.dump(collection, f, indent=2)

print(f"Generated collection with {len(folders)} folders and {sum(len(f['item']) for f in folders)} endpoints.")

