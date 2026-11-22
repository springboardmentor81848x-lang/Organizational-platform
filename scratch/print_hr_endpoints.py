import re

with open('backend/src/main/java/com/wki/auth/hr/HrDashboardController.java', 'r', encoding='utf-8') as f:
    src = f.read()

for m in re.finditer(r'@(GetMapping|PostMapping|PutMapping|DeleteMapping)(?:\((?:value\s*=\s*)?["\']([^"\']*)["\']\))?', src):
    sub = m.group(2) if m.group(2) else ""
    print(f"{m.group(1):15} /api/hr{sub}")
