-- V3: Knowledge Sharing, Peer Mentorship and Employee Profiles

CREATE TABLE employee_profiles (
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

CREATE INDEX idx_employee_profiles_user ON employee_profiles (user_id);
CREATE INDEX idx_employee_profiles_job_role ON employee_profiles (job_role);

-- Backfill profile records for all existing users so data is consistent
INSERT INTO employee_profiles (user_id, job_role, experience_level)
SELECT id, 'Software Developer', 'Entry level'
FROM users
ON CONFLICT (user_id) DO NOTHING;

CREATE TABLE mentor_profiles (
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

CREATE INDEX idx_mentor_profiles_active ON mentor_profiles (is_active);
CREATE INDEX idx_mentor_profiles_user ON mentor_profiles (user_id);

CREATE TABLE mentor_assignments (
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

CREATE UNIQUE INDEX uq_active_employee_mentor ON mentor_assignments (employee_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_mentor_assignments_mentor ON mentor_assignments (mentor_id);
CREATE INDEX idx_mentor_assignments_employee ON mentor_assignments (employee_id);

CREATE TABLE mentorship_sessions (
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

CREATE INDEX idx_sessions_assignment ON mentorship_sessions (assignment_id);
CREATE INDEX idx_sessions_scheduled ON mentorship_sessions (scheduled_at);

CREATE TABLE mentorship_goals (
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

CREATE INDEX idx_goals_assignment ON mentorship_goals (assignment_id);

CREATE TABLE mentorship_feedback (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL UNIQUE REFERENCES mentorship_sessions(id) ON DELETE CASCADE,
    author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating              INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comments            TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_feedback_author ON mentorship_feedback (author_id);
