-- Run this AFTER importing workforce_knowledge_intelligence.sql
-- (Optional: the backend also does both steps automatically on startup.)
USE workforce_knowledge_intelligence;

-- 1) The register form offers 6 roles; the original dump only has 4.
--    INSERT IGNORE makes this safe to run more than once (role_name is UNIQUE).
INSERT IGNORE INTO roles (role_name, description) VALUES
  ('HR_MANAGER',     'Manages HR operations'),
  ('TRAINER_MENTOR', 'Trains and mentors employees');

-- Wireframe role  ->  roles.role_name
--   Super Admin          ADMIN
--   HR Manager           HR_MANAGER
--   Team Manager         MANAGER
--   Employee             EMPLOYEE
--   Trainer / Mentor     TRAINER_MENTOR
--   Learning Coordinator TRAINING_COORDINATOR

-- 2) Column that records the time of each successful login.
--    If you see "Duplicate column name 'last_login_at'" the column already exists: ignore it.
ALTER TABLE employees ADD COLUMN last_login_at DATETIME NULL;

SELECT role_id, role_name FROM roles ORDER BY role_id;

-- Check logins after you have signed in a few times:
SELECT e.employee_id, e.email, r.role_name, e.last_login_at
FROM employees e JOIN roles r ON r.role_id = e.role_id
ORDER BY e.last_login_at DESC;
