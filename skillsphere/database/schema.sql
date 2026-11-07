-- Run once as a PostgreSQL superuser to create the database:
--   psql -U postgres -c "CREATE DATABASE skillsphere_db;"
-- The tables are created automatically by Flyway when the backend starts
-- (see backend/src/main/resources/db/migration). The statements below are the
-- same schema, provided for reference or manual setup.

\connect skillsphere_db

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(120)  NOT NULL,
    email           VARCHAR(254)  NOT NULL,
    employee_id     VARCHAR(30),
    password_hash   VARCHAR(100)  NOT NULL,
    role            VARCHAR(20)   NOT NULL DEFAULT 'EMPLOYEE',
    requested_role  VARCHAR(20),
    account_status  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email       UNIQUE (email),
    CONSTRAINT uq_users_employee_id UNIQUE (employee_id),
    CONSTRAINT chk_users_email_lc   CHECK (email = lower(email)),
    CONSTRAINT chk_users_role       CHECK (role IN ('EMPLOYEE', 'HR', 'MANAGER', 'ADMIN')),
    CONSTRAINT chk_users_req_role   CHECK (requested_role IS NULL OR requested_role IN ('EMPLOYEE', 'HR', 'MANAGER', 'ADMIN')),
    CONSTRAINT chk_users_status     CHECK (account_status IN ('ACTIVE', 'PENDING', 'SUSPENDED'))
);
CREATE INDEX IF NOT EXISTS idx_users_role ON users (role);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash)
);
CREATE INDEX IF NOT EXISTS idx_prt_user ON password_reset_tokens (user_id);


-- V3: Knowledge Sharing, Peer Mentorship and Employee Profiles
CREATE TABLE IF NOT EXISTS employee_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    job_role            VARCHAR(80) NOT NULL DEFAULT 'Software Developer',
    department          VARCHAR(80),
    experience_level    VARCHAR(40) NOT NULL DEFAULT 'Entry level',
    bio                 TEXT,
    skills              TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_profile_job_role CHECK (job_role IN ('Software Developer', 'Data Analyst', 'QA Engineer', 'Support Engineer')),
    CONSTRAINT chk_profile_experience CHECK (experience_level IN ('Entry level', 'Intermediate', 'Experienced'))
);
CREATE INDEX IF NOT EXISTS idx_employee_profiles_user ON employee_profiles (user_id);
CREATE INDEX IF NOT EXISTS idx_employee_profiles_job_role ON employee_profiles (job_role);

CREATE TABLE IF NOT EXISTS mentor_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    headline            VARCHAR(140) NOT NULL,
    expertise_areas     TEXT NOT NULL,
    max_mentees         INT NOT NULL DEFAULT 3 CHECK (max_mentees > 0),
    is_active           BOOLEAN NOT NULL DEFAULT true,
    approved_by_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_mentor_profiles_active ON mentor_profiles (is_active);
CREATE INDEX IF NOT EXISTS idx_mentor_profiles_user ON mentor_profiles (user_id);

CREATE TABLE IF NOT EXISTS mentor_assignments (
    id                  BIGSERIAL PRIMARY KEY,
    mentor_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' 
                        CHECK (status IN ('ACTIVE', 'COMPLETED', 'TERMINATED')),
    notes               TEXT,
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at            TIMESTAMPTZ,
    CONSTRAINT chk_no_self_mentoring CHECK (mentor_id <> employee_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_employee_mentor ON mentor_assignments (employee_id) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_mentor_assignments_mentor ON mentor_assignments (mentor_id);
CREATE INDEX IF NOT EXISTS idx_mentor_assignments_employee ON mentor_assignments (employee_id);

CREATE TABLE IF NOT EXISTS mentorship_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    assignment_id       BIGINT NOT NULL REFERENCES mentor_assignments(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    scheduled_at        TIMESTAMPTZ NOT NULL,
    duration_minutes    INT NOT NULL DEFAULT 45 CHECK (duration_minutes > 0),
    meeting_link        VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' 
                        CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_sessions_assignment ON mentorship_sessions (assignment_id);
CREATE INDEX IF NOT EXISTS idx_sessions_scheduled ON mentorship_sessions (scheduled_at);

CREATE TABLE IF NOT EXISTS mentorship_goals (
    id                  BIGSERIAL PRIMARY KEY,
    assignment_id       BIGINT NOT NULL REFERENCES mentor_assignments(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    target_date         DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' 
                        CHECK (status IN ('IN_PROGRESS', 'ACHIEVED', 'ABANDONED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_goals_assignment ON mentorship_goals (assignment_id);

CREATE TABLE IF NOT EXISTS mentorship_feedback (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL UNIQUE REFERENCES mentorship_sessions(id) ON DELETE CASCADE,
    author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating              INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments            TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_feedback_author ON mentorship_feedback (author_id);


-- -------------------------------------------------------------
-- V4: Multi-Role Architecture, Organization, Skills, Learning, Chat, Knowledge & Governance
-- -------------------------------------------------------------

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role 
    CHECK (role IN ('EMPLOYEE', 'MANAGER', 'MENTOR', 'HR_SPECIALIST', 'DEPARTMENT_HEAD', 'ADMIN'));

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_req_role;
ALTER TABLE users ADD CONSTRAINT chk_users_req_role 
    CHECK (requested_role IS NULL OR requested_role IN ('EMPLOYEE', 'MANAGER', 'MENTOR', 'HR_SPECIALIST', 'DEPARTMENT_HEAD', 'ADMIN'));

CREATE TABLE IF NOT EXISTS departments (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL UNIQUE,
    code                VARCHAR(20) NOT NULL UNIQUE,
    description         TEXT,
    head_user_id        BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS teams (
    id                  BIGSERIAL PRIMARY KEY,
    department_id       BIGINT NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    manager_user_id     BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS team_memberships (
    id                  BIGSERIAL PRIMARY KEY,
    team_id             BIGINT NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_in_team        VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_team_user UNIQUE (team_id, user_id)
);

CREATE TABLE IF NOT EXISTS skill_categories (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(80) NOT NULL UNIQUE,
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS skills (
    id                  BIGSERIAL PRIMARY KEY,
    category_id         BIGINT NOT NULL REFERENCES skill_categories(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL UNIQUE,
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS role_skill_requirements (
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

CREATE TABLE IF NOT EXISTS employee_skills (
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

CREATE TABLE IF NOT EXISTS skill_evidence (
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

CREATE TABLE IF NOT EXISTS skill_assessments (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    category_id         BIGINT REFERENCES skill_categories(id) ON DELETE SET NULL,
    target_role         VARCHAR(80),
    pass_score          INT NOT NULL DEFAULT 70,
    time_limit_minutes  INT NOT NULL DEFAULT 30,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessment_questions (
    id                  BIGSERIAL PRIMARY KEY,
    assessment_id       BIGINT NOT NULL REFERENCES skill_assessments(id) ON DELETE CASCADE,
    skill_id            BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    question_text       TEXT NOT NULL,
    options_json        TEXT NOT NULL,
    correct_answer_index INT NOT NULL,
    points              INT NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS assessment_assignments (
    id                  BIGSERIAL PRIMARY KEY,
    assessment_id       BIGINT NOT NULL REFERENCES skill_assessments(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_by_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    due_date            DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
                        CHECK (status IN ('PENDING', 'COMPLETED', 'OVERDUE')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessment_submissions (
    id                  BIGSERIAL PRIMARY KEY,
    assessment_id       BIGINT NOT NULL REFERENCES skill_assessments(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assignment_id       BIGINT REFERENCES assessment_assignments(id) ON DELETE SET NULL,
    score               INT NOT NULL,
    passed              BOOLEAN NOT NULL DEFAULT false,
    completed_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS assessment_responses (
    id                  BIGSERIAL PRIMARY KEY,
    submission_id       BIGINT NOT NULL REFERENCES assessment_submissions(id) ON DELETE CASCADE,
    question_id         BIGINT NOT NULL REFERENCES assessment_questions(id) ON DELETE CASCADE,
    selected_option_index INT NOT NULL,
    is_correct          BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS skill_gaps (
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

CREATE TABLE IF NOT EXISTS courses (
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

CREATE TABLE IF NOT EXISTS learning_paths (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    target_role         VARCHAR(80),
    created_by_id       BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS learning_path_items (
    id                  BIGSERIAL PRIMARY KEY,
    learning_path_id    BIGINT NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
    course_id           BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    step_order          INT NOT NULL,
    CONSTRAINT uq_path_course UNIQUE (learning_path_id, course_id)
);

CREATE TABLE IF NOT EXISTS employee_learning (
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

CREATE TABLE IF NOT EXISTS certificates (
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

CREATE TABLE IF NOT EXISTS training_programmes (
    id                  BIGSERIAL PRIMARY KEY,
    title               VARCHAR(140) NOT NULL,
    description         TEXT,
    department_id       BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    capacity            INT NOT NULL DEFAULT 30,
    enrolled_count      INT NOT NULL DEFAULT 0,
    trainer_name        VARCHAR(100),
    status              VARCHAR(20) NOT NULL DEFAULT 'UPCOMING' 
                        CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS training_enrollments (
    id                  BIGSERIAL PRIMARY KEY,
    programme_id        BIGINT NOT NULL REFERENCES training_programmes(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' 
                        CHECK (status IN ('ENROLLED', 'ATTENDED', 'CANCELLED')),
    enrolled_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_programme_user UNIQUE (programme_id, user_id)
);

CREATE TABLE IF NOT EXISTS mentorship_requests (
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

CREATE TABLE IF NOT EXISTS shared_resources (
    id                  BIGSERIAL PRIMARY KEY,
    assignment_id       BIGINT NOT NULL REFERENCES mentor_assignments(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    url_or_content      TEXT NOT NULL,
    shared_by_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS conversations (
    id                  BIGSERIAL PRIMARY KEY,
    type                VARCHAR(20) NOT NULL DEFAULT 'DIRECT' CHECK (type IN ('DIRECT', 'TEAM')),
    title               VARCHAR(140),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS conversation_participants (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_read_at        TIMESTAMPTZ,
    CONSTRAINT uq_conv_user UNIQUE (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id                  BIGSERIAL PRIMARY KEY,
    conversation_id     BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content             TEXT NOT NULL,
    attachment_url      VARCHAR(500),
    attachment_name     VARCHAR(140),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS knowledge_categories (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(80) NOT NULL UNIQUE,
    description         TEXT
);

CREATE TABLE IF NOT EXISTS knowledge_articles (
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

CREATE TABLE IF NOT EXISTS discussion_threads (
    id                  BIGSERIAL PRIMARY KEY,
    author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               VARCHAR(140) NOT NULL,
    content             TEXT NOT NULL,
    category            VARCHAR(80),
    reply_count         INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS discussion_replies (
    id                  BIGSERIAL PRIMARY KEY,
    thread_id           BIGINT NOT NULL REFERENCES discussion_threads(id) ON DELETE CASCADE,
    author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content             TEXT NOT NULL,
    is_accepted_answer  BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS calendar_events (
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

CREATE TABLE IF NOT EXISTS event_participants (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            BIGINT NOT NULL REFERENCES calendar_events(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status              VARCHAR(20) NOT NULL DEFAULT 'INVITED' 
                        CHECK (status IN ('INVITED', 'ACCEPTED', 'DECLINED')),
    CONSTRAINT uq_event_user UNIQUE (event_id, user_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id                  BIGSERIAL PRIMARY KEY,
    recipient_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type                VARCHAR(30) NOT NULL,
    title               VARCHAR(140) NOT NULL,
    message             TEXT NOT NULL,
    link                VARCHAR(250),
    is_read             BOOLEAN NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS platform_feedback (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id) ON DELETE SET NULL,
    category            VARCHAR(80) NOT NULL,
    rating              INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments            TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action              VARCHAR(100) NOT NULL,
    entity_name         VARCHAR(100) NOT NULL,
    entity_id           VARCHAR(100),
    details             TEXT,
    ip_address          VARCHAR(45),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS support_tickets (
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

CREATE TABLE IF NOT EXISTS system_configuration (
    config_key          VARCHAR(100) PRIMARY KEY,
    config_value        TEXT NOT NULL,
    description         TEXT,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
