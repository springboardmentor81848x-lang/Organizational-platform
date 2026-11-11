-- V4: Complete Platform Schema & Multi-Role Architecture

-- 1. Update user roles to support the 6 final documented roles:
-- EMPLOYEE, MANAGER, MENTOR, HR_SPECIALIST, DEPARTMENT_HEAD, ADMIN
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
UPDATE users SET role = 'HR_SPECIALIST' WHERE role = 'HR';
ALTER TABLE users ADD CONSTRAINT chk_users_role 
    CHECK (role IN ('EMPLOYEE', 'MANAGER', 'MENTOR', 'HR_SPECIALIST', 'DEPARTMENT_HEAD', 'ADMIN'));

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_req_role;
UPDATE users SET requested_role = 'HR_SPECIALIST' WHERE requested_role = 'HR';
ALTER TABLE users ADD CONSTRAINT chk_users_req_role 
    CHECK (requested_role IS NULL OR requested_role IN ('EMPLOYEE', 'MANAGER', 'MENTOR', 'HR_SPECIALIST', 'DEPARTMENT_HEAD', 'ADMIN'));

-- Migrate existing approved active mentors to MENTOR role if they are currently EMPLOYEE
UPDATE users 
SET role = 'MENTOR' 
WHERE role = 'EMPLOYEE' 
  AND id IN (SELECT user_id FROM mentor_profiles WHERE is_active = true);

-- -------------------------------------------------------------
-- 2. Organization: Departments & Teams
-- -------------------------------------------------------------
CREATE TABLE departments (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL UNIQUE,
    code                VARCHAR(20) NOT NULL UNIQUE,
    description         TEXT,
    head_user_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE teams (
    id                  BIGSERIAL PRIMARY KEY,
    department_id       BIGINT NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    manager_user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE team_memberships (
    id                  BIGSERIAL PRIMARY KEY,
    team_id             BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_teams_department ON teams(department_id);
CREATE INDEX idx_team_memberships_user ON team_memberships(user_id);

-- -------------------------------------------------------------
-- 3. Competency Framework & Skills
-- -------------------------------------------------------------
CREATE TABLE skill_categories (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(80) NOT NULL UNIQUE,
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE skills (
    id                  BIGSERIAL PRIMARY KEY,
    category_id         BIGINT NOT NULL REFERENCES skill_categories(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL UNIQUE,
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE role_skill_requirements (
    id                  BIGSERIAL PRIMARY KEY,
    job_role            VARCHAR(80) NOT NULL,
    skill_id            BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    required_proficiency VARCHAR(20) NOT NULL 
                        CHECK (required_proficiency IN ('Unaware', 'Beginner', 'Intermediate', 'Advanced', 'Expert')),
    priority            VARCHAR(20) NOT NULL DEFAULT 'HIGH' 
                        CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_role_skill UNIQUE (job_role, skill_id)
);

CREATE TABLE employee_skills (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id            BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    proficiency_level   VARCHAR(20) NOT NULL 
                        CHECK (proficiency_level IN ('Unaware', 'Beginner', 'Intermediate', 'Advanced', 'Expert')),
    verified            BOOLEAN NOT NULL DEFAULT false,
    verified_by_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_skill UNIQUE (user_id, skill_id)
);

CREATE TABLE skill_evidence (
    id                  BIGSERIAL PRIMARY KEY,
    employee_skill_id   BIGINT NOT NULL REFERENCES employee_skills(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    evidence_url        VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
                        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by_id      BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------
-- 4. Assessments & Skill Gaps
-- -------------------------------------------------------------
CREATE TABLE skill_assessments (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    category_id         BIGINT REFERENCES skill_categories(id) ON DELETE SET NULL,
    target_role         VARCHAR(80),
    pass_score          INT NOT NULL DEFAULT 70,
    time_limit_minutes  INT NOT NULL DEFAULT 30,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE assessment_questions (
    id                  BIGSERIAL PRIMARY KEY,
    assessment_id       BIGINT NOT NULL REFERENCES skill_assessments(id) ON DELETE CASCADE,
    skill_id            BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    question_text       TEXT NOT NULL,
    options_json        TEXT NOT NULL,
    correct_answer_index INT NOT NULL,
    points              INT NOT NULL DEFAULT 1
);

CREATE TABLE assessment_assignments (
    id                  BIGSERIAL PRIMARY KEY,
    assessment_id       BIGINT NOT NULL REFERENCES skill_assessments(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    due_date            DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
                        CHECK (status IN ('PENDING', 'COMPLETED', 'OVERDUE')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE assessment_submissions (
    id                  BIGSERIAL PRIMARY KEY,
    assessment_id       BIGINT NOT NULL REFERENCES skill_assessments(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id       BIGINT REFERENCES assessment_assignments(id) ON DELETE SET NULL,
    score               INT NOT NULL,
    passed              BOOLEAN NOT NULL DEFAULT false,
    completed_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE assessment_responses (
    id                  BIGSERIAL PRIMARY KEY,
    submission_id       BIGINT NOT NULL REFERENCES assessment_submissions(id) ON DELETE CASCADE,
    question_id         BIGINT NOT NULL REFERENCES assessment_questions(id) ON DELETE CASCADE,
    selected_option_index INT NOT NULL,
    is_correct          BOOLEAN NOT NULL
);

CREATE TABLE skill_gaps (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id            BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    current_level       INT NOT NULL,
    required_level      INT NOT NULL,
    gap_size            INT NOT NULL,
    priority            VARCHAR(20) NOT NULL CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW', 'ON_TRACK')),
    calculated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_skill_gap UNIQUE (user_id, skill_id)
);

CREATE INDEX idx_skill_gaps_user ON skill_gaps(user_id);

-- -------------------------------------------------------------
-- 5. Learning, Courses, Paths & Certificates
-- -------------------------------------------------------------
CREATE TABLE courses (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    skill_id            BIGINT REFERENCES skills(id) ON DELETE SET NULL,
    level               VARCHAR(20) NOT NULL CHECK (level IN ('Beginner', 'Intermediate', 'Advanced')),
    duration_hours      INT NOT NULL DEFAULT 10,
    provider            VARCHAR(80) NOT NULL DEFAULT 'SkillSphere Academy',
    course_url          VARCHAR(500),
    thumbnail_url       VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE learning_paths (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    target_role         VARCHAR(80),
    created_by_id       BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE learning_path_items (
    id                  BIGSERIAL PRIMARY KEY,
    learning_path_id    BIGINT NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    course_id           BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    step_order          INT NOT NULL,
    CONSTRAINT uq_path_course UNIQUE (learning_path_id, course_id)
);

CREATE TABLE employee_learning (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id           BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    status              VARCHAR(30) NOT NULL DEFAULT 'NOT_STARTED' 
                        CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'CERTIFIED', 'EXPIRED')),
    progress_pct        INT NOT NULL DEFAULT 0,
    enrolled_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,
    expires_at          TIMESTAMPTZ,
    CONSTRAINT uq_user_course UNIQUE (user_id, course_id)
);

CREATE TABLE certificates (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id           BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    certificate_code    VARCHAR(80) NOT NULL UNIQUE,
    title               VARCHAR(140) NOT NULL,
    issue_date          DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date         DATE,
    credential_url      VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE training_programmes (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    department_id       BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    capacity            INT NOT NULL DEFAULT 30,
    trainer_name        VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'UPCOMING' 
                        CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE training_enrollments (
    id                  BIGSERIAL PRIMARY KEY,
    programme_id        BIGINT NOT NULL REFERENCES training_programmes(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' 
                        CHECK (status IN ('ENROLLED', 'ATTENDED', 'CANCELLED')),
    enrolled_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_programme_user UNIQUE (programme_id, user_id)
);

-- -------------------------------------------------------------
-- 6. Mentorship Requests & Shared Resources
-- -------------------------------------------------------------
CREATE TABLE mentorship_requests (
    id                  BIGSERIAL PRIMARY KEY,
    mentor_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message             TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
                        CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'CANCELLED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at        TIMESTAMPTZ,
    response_note       TEXT
);

CREATE INDEX idx_mentorship_requests_mentor ON mentorship_requests(mentor_id);
CREATE INDEX idx_mentorship_requests_employee ON mentorship_requests(employee_id);

CREATE TABLE shared_resources (
    id                  BIGSERIAL PRIMARY KEY,
    assignment_id       BIGINT NOT NULL REFERENCES mentor_assignments(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    url_or_content      TEXT NOT NULL,
    shared_by_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------
-- 7. Secure Messaging & Chatbox
-- -------------------------------------------------------------
CREATE TABLE conversations (
    id                  BIGSERIAL PRIMARY KEY,
    type                VARCHAR(20) NOT NULL DEFAULT 'DIRECT' CHECK (type IN ('DIRECT', 'TEAM')),
    title               VARCHAR(140),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE conversation_participants (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_read_at        TIMESTAMPTZ,
    CONSTRAINT uq_conv_user UNIQUE (conversation_id, user_id)
);

CREATE TABLE messages (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content             TEXT NOT NULL,
    attachment_url      VARCHAR(500),
    attachment_name     VARCHAR(140),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_created ON messages(created_at);

-- -------------------------------------------------------------
-- 8. Knowledge Hub & Discussion Forum
-- -------------------------------------------------------------
CREATE TABLE knowledge_categories (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(80) NOT NULL UNIQUE,
    description         TEXT
);

CREATE TABLE knowledge_articles (
    id                  BIGSERIAL PRIMARY KEY,
    category_id         BIGINT REFERENCES knowledge_categories(id) ON DELETE SET NULL,
    author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    content             TEXT NOT NULL,
    tags                VARCHAR(200),
    status              VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' 
                        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE discussion_threads (
    id                  BIGSERIAL PRIMARY KEY,
    author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    content             TEXT NOT NULL,
    category            VARCHAR(80),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE discussion_replies (
    id                  BIGSERIAL PRIMARY KEY,
    thread_id           BIGINT NOT NULL REFERENCES discussion_threads(id) ON DELETE CASCADE,
    author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content             TEXT NOT NULL,
    is_accepted_answer  BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------
-- 9. Calendar & Scheduling
-- -------------------------------------------------------------
CREATE TABLE calendar_events (
    id                  BIGSERIAL PRIMARY KEY,
    creator_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    event_type          VARCHAR(20) NOT NULL 
                        CHECK (event_type IN ('ASSESSMENT', 'MENTORSHIP', 'LEARNING', 'TRAINING', 'TEAM')),
    start_time          TIMESTAMPTZ NOT NULL,
    end_time            TIMESTAMPTZ NOT NULL,
    location_or_link    VARCHAR(500),
    reference_id        BIGINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE event_participants (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            BIGINT NOT NULL REFERENCES calendar_events(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL DEFAULT 'INVITED' 
                        CHECK (status IN ('INVITED', 'ACCEPTED', 'DECLINED')),
    CONSTRAINT uq_event_user UNIQUE (event_id, user_id)
);

-- -------------------------------------------------------------
-- 10. Notifications
-- -------------------------------------------------------------
CREATE TABLE notifications (
    id                  BIGSERIAL PRIMARY KEY,
    recipient_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type                VARCHAR(30) NOT NULL,
    title               VARCHAR(140) NOT NULL,
    message             TEXT NOT NULL,
    link                VARCHAR(250),
    is_read             BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);

-- -------------------------------------------------------------
-- 11. Platform Governance, Feedback, Audit & Support
-- -------------------------------------------------------------
CREATE TABLE platform_feedback (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id) ON DELETE SET NULL,
    category            VARCHAR(80) NOT NULL,
    rating              INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments            TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action              VARCHAR(100) NOT NULL,
    entity_name         VARCHAR(100) NOT NULL,
    entity_id           VARCHAR(100),
    details             TEXT,
    ip_address          VARCHAR(45),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE support_tickets (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject             VARCHAR(140) NOT NULL,
    description         TEXT NOT NULL,
    priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' 
                        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status              VARCHAR(20) NOT NULL DEFAULT 'OPEN' 
                        CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    admin_notes         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE system_configuration (
    config_key          VARCHAR(100) PRIMARY KEY,
    config_value        TEXT NOT NULL,
    description         TEXT,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- -------------------------------------------------------------
-- 12. Seed Foundational Data for Out-of-the-Box Demo Readiness
-- -------------------------------------------------------------
-- Departments
INSERT INTO departments (name, code, description) VALUES
('Engineering', 'ENG', 'Core software development and platform infrastructure'),
('Quality Assurance', 'QA', 'Software testing, automation, and quality engineering'),
('Data & Analytics', 'DATA', 'Data engineering, analytics, and business intelligence'),
('Human Resources', 'HR', 'Talent management, recruitment, and organizational growth')
ON CONFLICT (name) DO NOTHING;

-- Skill Categories
INSERT INTO skill_categories (name, description) VALUES
('Software Engineering', 'Backend, frontend, and core programming languages'),
('Data & Analytics', 'Database querying, modeling, and analytical frameworks'),
('Quality & Testing', 'Automated testing and QA methodologies'),
('DevOps & Cloud', 'Containers, CI/CD pipelines, and cloud architecture'),
('Core Problem Solving', 'Algorithmic thinking and algorithmic data structures')
ON CONFLICT (name) DO NOTHING;

-- Skills
INSERT INTO skills (category_id, name, description) VALUES
(1, 'Java', 'Object-oriented programming, modern Java 17+, JVM tuning'),
(1, 'Spring Boot', 'Spring framework, dependency injection, REST APIs, and microservices'),
(1, 'Web Development', 'HTML5, CSS3, modern JavaScript, and frontend architectures'),
(2, 'SQL', 'Relational database querying, join optimizations, and indexing'),
(3, 'Automated Testing', 'Unit testing, integration testing, and mock frameworks'),
(4, 'Git and Version Control', 'Branching workflows, merges, rebasing, and GitHub collaboration'),
(5, 'Data Structures', 'Arrays, hash maps, trees, and algorithmic complexity'),
(5, 'Problem Solving', 'Root cause debugging, analytical reasoning, and optimization')
ON CONFLICT (name) DO NOTHING;

-- Role Skill Requirements for Software Developer
INSERT INTO role_skill_requirements (job_role, skill_id, required_proficiency, priority) VALUES
('Software Developer', 1, 'Advanced', 'HIGH'),
('Software Developer', 2, 'Advanced', 'HIGH'),
('Software Developer', 4, 'Advanced', 'HIGH'),
('Software Developer', 3, 'Intermediate', 'MEDIUM'),
('Software Developer', 6, 'Intermediate', 'MEDIUM'),
('Software Developer', 7, 'Advanced', 'HIGH'),
('Software Developer', 8, 'Advanced', 'HIGH')
ON CONFLICT (job_role, skill_id) DO NOTHING;

-- Role Skill Requirements for QA Engineer
INSERT INTO role_skill_requirements (job_role, skill_id, required_proficiency, priority) VALUES
('QA Engineer', 1, 'Intermediate', 'MEDIUM'),
('QA Engineer', 5, 'Advanced', 'HIGH'),
('QA Engineer', 4, 'Intermediate', 'MEDIUM'),
('QA Engineer', 6, 'Intermediate', 'MEDIUM')
ON CONFLICT (job_role, skill_id) DO NOTHING;

-- Role Skill Requirements for Data Analyst
INSERT INTO role_skill_requirements (job_role, skill_id, required_proficiency, priority) VALUES
('Data Analyst', 4, 'Expert', 'HIGH'),
('Data Analyst', 8, 'Advanced', 'HIGH'),
('Data Analyst', 1, 'Intermediate', 'MEDIUM')
ON CONFLICT (job_role, skill_id) DO NOTHING;

-- Courses Catalogue
INSERT INTO courses (title, description, skill_id, level, duration_hours, provider) VALUES
('Enterprise Java 17 Masterclass', 'Master records, sealed classes, and concurrency in modern Java.', 1, 'Advanced', 24, 'SkillSphere Academy'),
('Spring Boot 3 & Microservices Architecture', 'Production-ready REST APIs, Spring Data JPA, and security best practices.', 2, 'Advanced', 32, 'SkillSphere Academy'),
('High-Performance PostgreSQL & SQL Tuning', 'Advanced indexing, query execution plans, and transaction isolation.', 4, 'Advanced', 18, 'SkillSphere Academy'),
('Full-Stack Web Development with React', 'Component architecture, state management, and responsive styling.', 3, 'Intermediate', 20, 'SkillSphere Academy'),
('Git Workflow & Branching for Engineering Teams', 'Professional Git strategies, rebase workflows, and conflict resolution.', 6, 'Intermediate', 8, 'SkillSphere Academy'),
('Data Structures & Algorithms in Practice', 'Complexity analysis, hash maps, graph traversal, and dynamic programming.', 7, 'Advanced', 28, 'SkillSphere Academy')
ON CONFLICT DO NOTHING;

-- Knowledge Hub Categories
INSERT INTO knowledge_categories (name, description) VALUES
('Architecture & Design', 'System design patterns, microservices guidelines, and database architecture'),
('Engineering Tutorials', 'Step-by-step guides for Spring Boot, React, and PostgreSQL'),
('Best Practices & Standards', 'Code conventions, security guidelines, and review standards'),
('Organization Announcements', 'Company-wide technical initiatives and skill programs')
ON CONFLICT (name) DO NOTHING;

-- System Configuration Default Values
INSERT INTO system_configuration (config_key, config_value, description) VALUES
('max_upload_size_mb', '10', 'Maximum file upload size in megabytes'),
('allowed_attachment_types', 'pdf,png,jpg,jpeg,doc,docx,zip', 'Comma-separated permitted attachment extensions'),
('auto_archive_inactive_mentorship_days', '90', 'Days of inactivity before closing dormant mentorships'),
('chat_retention_days', '365', 'Days to retain chat messages')
ON CONFLICT (config_key) DO NOTHING;

-- Seed Default Skill Assessment with the 12 questions
INSERT INTO skill_assessments (id, title, description, category_id, target_role, pass_score, time_limit_minutes) VALUES
(1, 'General Technical Proficiency Assessment', 'Evaluates core competencies across Java, SQL, Web Development, Problem Solving, Data Structures, and Git.', 1, 'Software Developer', 70, 25)
ON CONFLICT (id) DO NOTHING;

INSERT INTO assessment_questions (assessment_id, skill_id, question_text, options_json, correct_answer_index, points) VALUES
(1, 1, 'Which keyword is used to inherit a class in Java?', '["implements", "extends", "inherits", "super"]', 1, 1),
(1, 1, 'Which collection stores only unique elements?', '["ArrayList", "HashSet", "LinkedList", "ArrayDeque"]', 1, 1),
(1, 4, 'Which SQL clause filters rows after they have been grouped?', '["WHERE", "HAVING", "ORDER BY", "LIMIT"]', 1, 1),
(1, 4, 'Which JOIN returns only the rows that match in both tables?', '["LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "CROSS JOIN"]', 2, 1),
(1, 3, 'Which language is used to style a web page?', '["HTML", "CSS", "SQL", "Java"]', 1, 1),
(1, 3, 'Which HTML element is best suited for a site main navigation links?', '["<nav>", "<div>", "<section>", "<span>"]', 0, 1),
(1, 8, 'What is the best first step when debugging a failing feature?', '["Rewrite the whole feature", "Ignore the error message", "Reproduce the issue and read the error", "Delete the project"]', 2, 1),
(1, 8, 'A page loads slowly. What should you do before optimising?', '["Guess the cause and change code", "Measure to find the real bottleneck", "Add caching everywhere", "Buy a bigger server"]', 1, 1),
(1, 7, 'Which structure offers average O(1) lookup by key?', '["Linked list", "Hash map", "Sorted array", "Stack"]', 1, 1),
(1, 7, 'Which structure follows last-in, first-out (LIFO) order?', '["Queue", "Stack", "Heap", "Graph"]', 1, 1),
(1, 6, 'Which command creates a new branch and switches to it?', '["git branch -d", "git checkout -b", "git merge", "git stash"]', 1, 1),
(1, 6, 'Which command downloads remote changes and merges them into your branch?', '["git fetch", "git pull", "git push", "git clone"]', 1, 1)
ON CONFLICT DO NOTHING;
