-- ============================================================
-- Employee dashboard tables + sample data (all new tables are named ed_...)
-- The backend runs this file automatically on startup (EmployeeDashboardSchema).
-- It is safe to run repeatedly and never changes your existing tables.
-- ============================================================

-- Extra profile details (kept in a separate table so `employees` is not altered)
CREATE TABLE IF NOT EXISTS ed_employee_profiles (
  employee_id BIGINT NOT NULL PRIMARY KEY,
  phone       VARCHAR(30),
  location    VARCHAR(100),
  bio         VARCHAR(1000),
  linkedin    VARCHAR(255),
  updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_skills (
  skill_id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  skill_name             VARCHAR(100) NOT NULL UNIQUE,
  category               VARCHAR(60)  NOT NULL,
  default_required_level TINYINT      NOT NULL DEFAULT 3
) ENGINE=InnoDB;

-- self_level / required_level are on a 1-5 scale
CREATE TABLE IF NOT EXISTS ed_employee_skills (
  employee_id    BIGINT  NOT NULL,
  skill_id       BIGINT  NOT NULL,
  self_level     TINYINT NOT NULL,
  required_level TINYINT NOT NULL DEFAULT 3,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (employee_id, skill_id),
  FOREIGN KEY (skill_id) REFERENCES ed_skills(skill_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_assessment_questions (
  question_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  topic VARCHAR(100) NOT NULL,
  question_text VARCHAR(500) NOT NULL,
  option_a VARCHAR(300) NOT NULL,
  option_b VARCHAR(300) NOT NULL,
  option_c VARCHAR(300) NOT NULL,
  option_d VARCHAR(300) NOT NULL,
  correct_option CHAR(1) NOT NULL,
  explanation VARCHAR(500),
  UNIQUE KEY uq_ed_question (skill_id, question_text),
  FOREIGN KEY (skill_id) REFERENCES ed_skills(skill_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_assessment_attempts (
  attempt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  total_questions INT NOT NULL,
  correct_answers INT NOT NULL,
  score_pct DECIMAL(5,2) NOT NULL,
  attempted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ed_attempt_latest (employee_id, skill_id, attempted_at),
  FOREIGN KEY (skill_id) REFERENCES ed_skills(skill_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_assessment_answers (
  answer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  selected_option CHAR(1) NOT NULL,
  is_correct TINYINT(1) NOT NULL,
  FOREIGN KEY (attempt_id) REFERENCES ed_assessment_attempts(attempt_id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES ed_assessment_questions(question_id) ON DELETE CASCADE,
  UNIQUE KEY uq_ed_answer (attempt_id, question_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_courses (
  course_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
  title          VARCHAR(150) NOT NULL,
  provider       VARCHAR(100),
  skill_id       BIGINT NOT NULL,
  course_level   TINYINT NOT NULL DEFAULT 3,
  duration_hours INT NOT NULL DEFAULT 4,
  FOREIGN KEY (skill_id) REFERENCES ed_skills(skill_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_learning_paths (
  path_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(150) NOT NULL,
  description VARCHAR(500)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_learning_path_courses (
  path_id   BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  seq       INT NOT NULL DEFAULT 1,
  PRIMARY KEY (path_id, course_id),
  FOREIGN KEY (path_id)   REFERENCES ed_learning_paths(path_id) ON DELETE CASCADE,
  FOREIGN KEY (course_id) REFERENCES ed_courses(course_id)      ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_enrollments (
  enrollment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id   BIGINT NOT NULL,
  course_id     BIGINT NOT NULL,
  progress_pct  INT NOT NULL DEFAULT 0,
  status        VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS | COMPLETED
  started_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  completed_at  DATETIME NULL,
  UNIQUE KEY uq_enroll (employee_id, course_id),
  FOREIGN KEY (course_id) REFERENCES ed_courses(course_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_mentorships (
  mentorship_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  mentor_id     BIGINT NOT NULL,
  mentee_id     BIGINT NOT NULL,
  topic         VARCHAR(200),
  status        VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',    -- REQUESTED | ACTIVE | DECLINED | COMPLETED
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_knowledge_sessions (
  session_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  title        VARCHAR(150) NOT NULL,
  description  VARCHAR(500),
  host_id      BIGINT NOT NULL,
  scheduled_at DATETIME NOT NULL,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_session_registrations (
  session_id  BIGINT NOT NULL,
  employee_id BIGINT NOT NULL,
  PRIMARY KEY (session_id, employee_id),
  FOREIGN KEY (session_id) REFERENCES ed_knowledge_sessions(session_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ed_notifications (
  notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id     BIGINT NOT NULL,
  message         VARCHAR(300) NOT NULL,
  is_read         TINYINT(1) NOT NULL DEFAULT 0,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notif_emp (employee_id, is_read)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Sample catalogue data (safe to edit / extend; re-running will not duplicate)
-- Written with scalar sub-selects so it never hits 'illegal mix of collations'.
-- ------------------------------------------------------------
INSERT IGNORE INTO ed_skills (skill_name, category, default_required_level) VALUES
 ('Java','Technical',4),('Spring Boot','Technical',3),('SQL & Databases','Technical',3),
 ('JavaScript','Technical',3),('Cloud Fundamentals','Technical',3),('Communication','Soft Skills',4),
 ('Teamwork','Soft Skills',3),('Problem Solving','Soft Skills',4),('Project Management','Management',3),
 ('Mentoring & Coaching','Management',3);

-- Diagnostic assessment bank. Answers are never sent to the browser before submission.
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'Core concepts', 'In Java, which keyword creates a subclass?', 'implements', 'extends', 'inherits', 'instanceof', 'B', 'The extends keyword establishes class inheritance.' FROM ed_skills WHERE skill_name='Java';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Collections', 'Which Java collection stores unique elements?', 'ArrayList', 'HashSet', 'LinkedList', 'Vector', 'B', 'A Set stores unique elements.' FROM ed_skills WHERE skill_name='Java';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Exceptions', 'Which block is used to handle an exception?', 'final', 'throws only', 'catch', 'assert', 'C', 'A catch block handles an exception.' FROM ed_skills WHERE skill_name='Java';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'REST APIs', 'Which HTTP method is commonly used to retrieve a resource?', 'POST', 'PATCH', 'GET', 'DELETE', 'C', 'GET retrieves a resource.' FROM ed_skills WHERE skill_name='Spring Boot';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Dependency injection', 'What is the main purpose of dependency injection?', 'Compile CSS', 'Provide dependencies from outside a class', 'Create database tables only', 'Encrypt URLs', 'B', 'Dependency injection supplies an object its dependencies.' FROM ed_skills WHERE skill_name='Spring Boot';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Configuration', 'Which file commonly stores Spring Boot application properties?', 'package.json', 'pom.lock', 'application.properties', 'index.html', 'C', 'Spring Boot reads configuration from application.properties.' FROM ed_skills WHERE skill_name='Spring Boot';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'SQL basics', 'Which SQL clause filters rows before grouping?', 'ORDER BY', 'WHERE', 'HAVING', 'LIMIT', 'B', 'WHERE filters rows before grouping.' FROM ed_skills WHERE skill_name='SQL & Databases';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Joins', 'Which join returns only matching rows from both tables?', 'LEFT JOIN', 'FULL JOIN', 'INNER JOIN', 'CROSS JOIN', 'C', 'INNER JOIN returns rows that match the join condition.' FROM ed_skills WHERE skill_name='SQL & Databases';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Aggregation', 'Which function counts rows?', 'SUM()', 'COUNT()', 'AVG()', 'ROUND()', 'B', 'COUNT() counts rows or non-null values.' FROM ed_skills WHERE skill_name='SQL & Databases';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'JavaScript basics', 'Which declaration cannot be reassigned?', 'let', 'var', 'const', 'static', 'C', 'A const binding cannot be reassigned.' FROM ed_skills WHERE skill_name='JavaScript';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Arrays', 'Which method creates a new array by transforming each element?', 'filter()', 'map()', 'push()', 'pop()', 'B', 'map() transforms each element into a new array.' FROM ed_skills WHERE skill_name='JavaScript';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Async JavaScript', 'Which construct is used to wait for a Promise inside an async function?', 'await', 'defer', 'yield only', 'static', 'A', 'await pauses the async function until the Promise settles.' FROM ed_skills WHERE skill_name='JavaScript';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'Cloud concepts', 'What does cloud elasticity mean?', 'Encrypting every file', 'Scaling resources with demand', 'Removing backups', 'Writing frontend code', 'B', 'Elasticity adjusts resources as demand changes.' FROM ed_skills WHERE skill_name='Cloud Fundamentals';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Availability', 'What is a key goal of a backup strategy?', 'Improve font size', 'Recover data after loss', 'Replace authentication', 'Reduce all testing', 'B', 'Backups support data recovery.' FROM ed_skills WHERE skill_name='Cloud Fundamentals';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Security', 'Which is a good practice for cloud credentials?', 'Commit secrets to Git', 'Share one password', 'Use managed secrets and least privilege', 'Disable access controls', 'C', 'Secrets management and least privilege reduce risk.' FROM ed_skills WHERE skill_name='Cloud Fundamentals';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'Listening', 'What is active listening?', 'Preparing your reply without listening', 'Focusing, clarifying, and confirming understanding', 'Interrupting quickly', 'Avoiding questions', 'B', 'Active listening includes focus and clarification.' FROM ed_skills WHERE skill_name='Communication';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Feedback', 'Which feedback is most useful?', 'Vague and personal', 'Specific, respectful, and actionable', 'Delayed with no examples', 'Only negative', 'B', 'Useful feedback is specific and actionable.' FROM ed_skills WHERE skill_name='Communication';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Presentations', 'What should a presentation opening do?', 'Introduce purpose and audience relevance', 'Show every detail immediately', 'Avoid a clear topic', 'Read all slides verbatim', 'A', 'A clear opening sets purpose and relevance.' FROM ed_skills WHERE skill_name='Communication';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'Team collaboration', 'When a teammate disagrees, what is the best first response?', 'Dismiss their idea', 'Listen and discuss evidence', 'Escalate immediately', 'Stop sharing updates', 'B', 'Constructive discussion helps evaluate different views.' FROM ed_skills WHERE skill_name='Teamwork';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Ownership', 'What supports reliable teamwork?', 'Hiding blockers', 'Clear responsibilities and updates', 'Duplicating work silently', 'Avoiding deadlines', 'B', 'Clear ownership and communication reduce confusion.' FROM ed_skills WHERE skill_name='Teamwork';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Conflict resolution', 'A team has conflicting priorities. What should it do?', 'Guess the priority', 'Agree on goals and trade-offs together', 'Ignore stakeholders', 'Stop tracking tasks', 'B', 'Shared goals and trade-off discussions align a team.' FROM ed_skills WHERE skill_name='Teamwork';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'Problem solving', 'What is a good first step when a bug is reported?', 'Rewrite the entire app', 'Reproduce and define the problem', 'Delete logs', 'Change random files', 'B', 'Reproducing the issue helps isolate its cause.' FROM ed_skills WHERE skill_name='Problem Solving';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Root cause', 'Why perform root-cause analysis?', 'To hide symptoms', 'To address the underlying cause', 'To skip testing', 'To assign blame', 'B', 'Root-cause analysis targets the underlying issue.' FROM ed_skills WHERE skill_name='Problem Solving';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Validation', 'After implementing a fix, what should you do?', 'Assume it works', 'Test the fix and regression cases', 'Remove monitoring', 'Close all related issues', 'B', 'Testing verifies the fix and guards against regressions.' FROM ed_skills WHERE skill_name='Problem Solving';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'Planning', 'What is a useful project milestone?', 'A vague intention', 'A measurable deliverable with a target date', 'An unowned task', 'A list with no dates', 'B', 'Milestones should be measurable and time-bound.' FROM ed_skills WHERE skill_name='Project Management';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Risk management', 'When should project risks be reviewed?', 'Only after failure', 'Regularly throughout the project', 'Never', 'Only at launch', 'B', 'Risks should be tracked and reviewed continuously.' FROM ed_skills WHERE skill_name='Project Management';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Scope', 'A stakeholder adds a major requirement. What should happen?', 'Accept without review', 'Assess impact and follow change control', 'Hide it from the team', 'Delete the schedule', 'B', 'Scope changes need impact assessment and agreement.' FROM ed_skills WHERE skill_name='Project Management';
INSERT IGNORE INTO ed_assessment_questions (skill_id, topic, question_text, option_a, option_b, option_c, option_d, correct_option, explanation)
SELECT skill_id, 'Coaching', 'A mentee is stuck. What is a good coaching approach?', 'Give no context and take over', 'Ask guiding questions and agree on next steps', 'Criticize the person', 'Avoid follow-up', 'B', 'Guiding questions help the mentee reason and act.' FROM ed_skills WHERE skill_name='Mentoring & Coaching';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Goal setting', 'A useful development goal should be...', 'Unmeasurable', 'Specific and reviewable', 'Secret from the mentee', 'Unrelated to needs', 'B', 'Clear goals make progress easier to review.' FROM ed_skills WHERE skill_name='Mentoring & Coaching';
INSERT IGNORE INTO ed_assessment_questions SELECT NULL, skill_id, 'Feedback', 'How should a mentor handle a mistake?', 'Use it as a learning opportunity', 'Shame the mentee', 'Ignore the cause', 'Prevent questions', 'A', 'Supportive feedback helps learning.' FROM ed_skills WHERE skill_name='Mentoring & Coaching';

INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Java Core to Advanced', 'Internal Academy', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Java'), 4, 12
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Java Core to Advanced');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Spring Boot REST APIs', 'Internal Academy', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Spring Boot'), 3, 8
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Spring Boot REST APIs');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Spring Security & JWT', 'Internal Academy', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Spring Boot'), 4, 6
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Spring Security & JWT');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'SQL for Application Developers', 'Internal Academy', (SELECT skill_id FROM ed_skills WHERE skill_name = 'SQL & Databases'), 3, 8
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'SQL for Application Developers');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Advanced Query Tuning', 'Internal Academy', (SELECT skill_id FROM ed_skills WHERE skill_name = 'SQL & Databases'), 4, 5
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Advanced Query Tuning');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Modern JavaScript (ES6+)', 'Internal Academy', (SELECT skill_id FROM ed_skills WHERE skill_name = 'JavaScript'), 3, 7
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Modern JavaScript (ES6+)');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Cloud Basics for Developers', 'Partner Portal', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Cloud Fundamentals'), 3, 6
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Cloud Basics for Developers');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Effective Workplace Communication', 'HR Learning', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Communication'), 4, 4
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Effective Workplace Communication');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Collaboration in Agile Teams', 'HR Learning', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Teamwork'), 3, 3
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Collaboration in Agile Teams');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Structured Problem Solving', 'HR Learning', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Problem Solving'), 4, 5
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Structured Problem Solving');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Project Management Essentials', 'HR Learning', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Project Management'), 3, 6
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Project Management Essentials');
INSERT INTO ed_courses (title, provider, skill_id, course_level, duration_hours)
SELECT 'Coaching & Mentoring Skills', 'HR Learning', (SELECT skill_id FROM ed_skills WHERE skill_name = 'Mentoring & Coaching'), 3, 4
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_courses WHERE title = 'Coaching & Mentoring Skills');

INSERT INTO ed_learning_paths (title, description)
SELECT 'Backend Developer Track', 'Java, Spring Boot, security and databases end to end.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_learning_paths WHERE title = 'Backend Developer Track');
INSERT INTO ed_learning_paths (title, description)
SELECT 'Communication & Collaboration', 'Soft skills for working effectively in teams.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_learning_paths WHERE title = 'Communication & Collaboration');
INSERT INTO ed_learning_paths (title, description)
SELECT 'Future Team Lead', 'Project management, coaching and problem solving.'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM ed_learning_paths WHERE title = 'Future Team Lead');

INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Backend Developer Track'), (SELECT course_id FROM ed_courses WHERE title = 'Java Core to Advanced'), 1);
INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Backend Developer Track'), (SELECT course_id FROM ed_courses WHERE title = 'Spring Boot REST APIs'), 2);
INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Backend Developer Track'), (SELECT course_id FROM ed_courses WHERE title = 'Spring Security & JWT'), 3);
INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Backend Developer Track'), (SELECT course_id FROM ed_courses WHERE title = 'SQL for Application Developers'), 4);
INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Communication & Collaboration'), (SELECT course_id FROM ed_courses WHERE title = 'Effective Workplace Communication'), 1);
INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Communication & Collaboration'), (SELECT course_id FROM ed_courses WHERE title = 'Collaboration in Agile Teams'), 2);
INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Future Team Lead'), (SELECT course_id FROM ed_courses WHERE title = 'Structured Problem Solving'), 1);
INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Future Team Lead'), (SELECT course_id FROM ed_courses WHERE title = 'Project Management Essentials'), 2);
INSERT IGNORE INTO ed_learning_path_courses (path_id, course_id, seq) VALUES
 ((SELECT path_id FROM ed_learning_paths WHERE title = 'Future Team Lead'), (SELECT course_id FROM ed_courses WHERE title = 'Coaching & Mentoring Skills'), 3);

-- One welcome notification for every existing employee (runs once per employee)
INSERT INTO ed_notifications (employee_id, message)
SELECT e.employee_id, 'Welcome to your dashboard! Start by rating your skills in "My Skills".'
FROM employees e
WHERE NOT EXISTS (SELECT 1 FROM ed_notifications n WHERE n.employee_id = e.employee_id);

-- Sample skill ratings for the placeholder employees of the original dump (DEMO_PLACEHOLDER password),
-- so that "Suggested mentors" has people to suggest. Real users are never given ratings.
-- Remove with: DELETE FROM ed_employee_skills WHERE employee_id IN
--   (SELECT employee_id FROM employees WHERE password_hash = 'DEMO_PLACEHOLDER');
INSERT IGNORE INTO ed_employee_skills (employee_id, skill_id, self_level, required_level)
SELECT e.employee_id, s.skill_id, 3 + MOD(e.employee_id * 7 + s.skill_id * 3, 3), s.default_required_level
FROM employees e CROSS JOIN ed_skills s
WHERE e.password_hash = 'DEMO_PLACEHOLDER' AND MOD(e.employee_id + s.skill_id, 2) = 0;

-- ============================================================
-- Profile Enhancement: extra columns
-- Column additions (education, work_experience, department, job_role)
-- are handled in EmployeeDashboardSchema.java since Spring's SQL
-- runner does not support DELIMITER / stored procedures.
-- ============================================================

-- ============================================================
-- Certifications table
-- ============================================================
CREATE TABLE IF NOT EXISTS ed_certifications (
  certification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id      BIGINT NOT NULL,
  title            VARCHAR(200) NOT NULL,
  issuer           VARCHAR(150),
  credential_id    VARCHAR(100),
  issue_date       DATE,
  expiry_date      DATE,
  credential_url   VARCHAR(500),
  created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_cert_emp (employee_id)
) ENGINE=InnoDB;

-- ============================================================
-- Achievements table (badges and milestones)
-- ============================================================
CREATE TABLE IF NOT EXISTS ed_achievements (
  achievement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id    BIGINT NOT NULL,
  title          VARCHAR(200) NOT NULL,
  description    VARCHAR(500),
  badge_type     VARCHAR(50) NOT NULL DEFAULT 'MILESTONE',  -- MILESTONE | COURSE_COMPLETE | SKILL_MASTERY | ASSESSMENT_ACE | STREAK
  earned_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_ach_emp (employee_id),
  UNIQUE KEY uq_achievement (employee_id, title)
) ENGINE=InnoDB;
