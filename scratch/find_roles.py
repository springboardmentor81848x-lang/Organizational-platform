with open('backend/src/main/java/com/wki/auth/hr/HrDashboardController.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if '@PostMapping("/roles")' in line or '@PostMapping("/courses")' in line:
        for j in range(i, min(i+25, len(lines))):
            print(lines[j], end='')
        print("-" * 40)
