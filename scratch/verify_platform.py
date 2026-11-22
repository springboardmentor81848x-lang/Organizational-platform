import urllib.request
import urllib.error
import json
import pymysql

results = {}

# 1. Test Frontend Pages Accessibility
frontend_pages = [
    "login.html",
    "register.html",
    "employee-dashboard.html",
    "hr-dashboard.html",
    "manager-dashboard.html",
    "trainer-dashboard.html",
    "dept-head-dashboard.html"
]

print("=== 1. TESTING FRONTEND PAGES (http://localhost:5500) ===")
for page in frontend_pages:
    url = f"http://localhost:5500/{page}"
    try:
        with urllib.request.urlopen(url) as resp:
            print(f"[PASS] {page:25} -> HTTP {resp.status}")
            results[f"frontend_{page}"] = resp.status
    except Exception as e:
        print(f"[FAIL] {page:25} -> {e}")
        results[f"frontend_{page}"] = str(e)

# 2. Test Backend API & Authentication
print("\n=== 2. TESTING BACKEND & REST APIS (http://localhost:8080/api) ===")
roles_to_test = {
    "EMPLOYEE": ("emp.test@infosys.com", "Password@123", "/employee/me/profile"),
    "HR_MANAGER": ("hr.test@infosys.com", "Password@123", "/hr/overview"),
    "MANAGER": ("mgr.test@infosys.com", "Password@123", "/manager/notifications"),
    "TRAINER_MENTOR": ("mentor.test@infosys.com", "Password@123", "/mentor/overview"),
    "DEPT_HEAD": ("depthead.test@infosys.com", "Password@123", "/dept-head/overview")
}

for role, (email, pwd, test_path) in roles_to_test.items():
    # Login
    login_data = json.dumps({"email": email, "password": pwd}).encode("utf-8")
    req = urllib.request.Request("http://localhost:8080/api/auth/login", data=login_data, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as resp:
            login_res = json.loads(resp.read().decode("utf-8"))
            token = login_res.get("token")
            print(f"[PASS] Login {role:15} -> HTTP {resp.status} (token received)")
            
            # Authenticated Call
            auth_req = urllib.request.Request(f"http://localhost:8080/api{test_path}", headers={"Authorization": f"Bearer {token}"})
            with urllib.request.urlopen(auth_req) as a_resp:
                print(f"[PASS] Dashboard API {role:15} ({test_path}) -> HTTP {a_resp.status}")
    except Exception as e:
        print(f"[FAIL] {role} -> {e}")

# 3. Test Database Connection and Integrity
print("\n=== 3. TESTING DATABASE (MySQL 3306: workforce_knowledge_intelligence) ===")
try:
    conn = pymysql.connect(host='localhost', port=3306, user='root', password='97@MySql#neha', database='workforce_knowledge_intelligence')
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM employees;")
    emp_count = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM roles;")
    role_count = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM ed_skills;")
    skill_count = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM ed_courses;")
    course_count = cur.fetchone()[0]
    print(f"[PASS] Database Connected! Total employees: {emp_count}, roles: {role_count}, skills: {skill_count}, courses: {course_count}")
    conn.close()
except Exception as e:
    print(f"[FAIL] Database connection error: {e}")
