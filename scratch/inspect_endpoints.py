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

total = 0
for path in controllers:
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    class_map = re.findall(r'@RequestMapping\(["\']([^"\']+)["\']\)', content)
    base = class_map[0] if class_map else ''
    # Find all mapping methods
    patterns = [
        (r'@GetMapping(?:\((?:value\s*=\s*)?["\']([^"\']*)["\']\))?', 'GET'),
        (r'@PostMapping(?:\((?:value\s*=\s*)?["\']([^"\']*)["\']\))?', 'POST'),
        (r'@PutMapping(?:\((?:value\s*=\s*)?["\']([^"\']*)["\']\))?', 'PUT'),
        (r'@DeleteMapping(?:\((?:value\s*=\s*)?["\']([^"\']*)["\']\))?', 'DELETE')
    ]
    matches = []
    for pattern, http_method in patterns:
        for m in re.finditer(pattern, content):
            subpath = m.group(1) if m.group(1) else ''
            matches.append((http_method, base + subpath, m.start()))
    
    matches.sort(key=lambda x: x[2])
    print(f"{os.path.basename(path)}: base='{base}', count={len(matches)}")
    for http_method, full_path, _ in matches:
        print(f"  {http_method:6} {full_path}")
    total += len(matches)

print(f"\nTotal endpoints found in source code: {total}")

