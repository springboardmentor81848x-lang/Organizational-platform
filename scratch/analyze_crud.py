import os
import re

controllers = [
    'backend/src/main/java/com/wki/auth/controller/AuthController.java',
    'backend/src/main/java/com/wki/auth/controller/MeController.java',
    'backend/src/main/java/com/wki/auth/employee/EmployeeDashboardController.java',
    'backend/src/main/java/com/wki/auth/employee/KnowledgeChatController.java',
    'backend/src/main/java/com/wki/auth/employee/RoleAssessmentController.java',
    'backend/src/main/java/com/wki/auth/hr/HrDashboardController.java',
    'backend/src/main/java/com/wki/auth/manager/ManagerNotificationController.java',
    'backend/src/main/java/com/wki/auth/mentor/MentorDashboardController.java',
    'backend/src/main/java/com/wki/auth/depthead/DeptHeadDashboardController.java',
    'backend/src/main/java/com/wki/auth/messaging/MessagingController.java'
]

for c in controllers:
    with open(c, 'r', encoding='utf-8') as f:
        src = f.read()
    cname = os.path.basename(c)
    base_m = re.findall(r'@RequestMapping\(["\']([^"\']+)["\']\)', src)
    base = base_m[0] if base_m else ""
    print(f"=== {cname} (base: {base}) ===")
    
    # find methods
    # match annotations like @GetMapping, @PostMapping, etc.
    pattern = r'(@(GetMapping|PostMapping|PutMapping|DeleteMapping)(?:\((?:value\s*=\s*)?["\']([^"\']*)["\']\))?[\s\S]*?public\s+[\w<>,?\s\[\]]+\s+(\w+)\s*\(([\s\S]*?)\))'
    for m in re.finditer(r'@(GetMapping|PostMapping|PutMapping|DeleteMapping)(?:\((?:value\s*=\s*)?["\']([^"\']*)["\']\))?[\s\S]*?public\s+[\w<>,?\s\[\]]+\s+(\w+)\s*\(([^)]*)\)', src):
        http_method = m.group(1)
        subpath = m.group(2) if m.group(2) else ""
        method_name = m.group(3)
        params = m.group(4).replace('\n', ' ')
        print(f"  {http_method:6} {base + subpath:40} -> {method_name}({params.strip()[:60]}...)")
