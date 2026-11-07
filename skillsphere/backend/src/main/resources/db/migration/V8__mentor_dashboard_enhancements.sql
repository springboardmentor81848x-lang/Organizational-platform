-- =========================================================================
-- V8: Mentor Dashboard Enhancements
-- Adds availability, consent tracking, goal action items, evaluations,
-- and enhances mentorship sessions, goals, and shared resources.
-- =========================================================================

-- 1. Mentor Availability & Schedule Slots
CREATE TABLE mentor_availability (
    id                  BIGSERIAL PRIMARY KEY,
    mentor_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week         VARCHAR(20) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    session_format      VARCHAR(30) NOT NULL DEFAULT 'VIRTUAL' CHECK (session_format IN ('VIRTUAL', 'IN_PERSON', 'HYBRID')),
    is_available        BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_time_window CHECK (end_time > start_time)
);

CREATE INDEX idx_mentor_availability_mentor ON mentor_availability(mentor_id);

-- 2. Mentee Skill Sharing Consent & Governance
CREATE TABLE mentee_skill_consents (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    mentor_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    can_view_skills     BOOLEAN NOT NULL DEFAULT true,
    can_view_gaps       BOOLEAN NOT NULL DEFAULT true,
    can_view_assessments BOOLEAN NOT NULL DEFAULT true,
    consent_status      VARCHAR(20) NOT NULL DEFAULT 'GRANTED' CHECK (consent_status IN ('GRANTED', 'REVOKED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_mentee_mentor_consent UNIQUE (employee_id, mentor_id)
);

CREATE INDEX idx_mentee_consent_employee ON mentee_skill_consents(employee_id);
CREATE INDEX idx_mentee_consent_mentor ON mentee_skill_consents(mentor_id);

-- 3. Goal Action Items (Milestones)
CREATE TABLE goal_action_items (
    id                  BIGSERIAL PRIMARY KEY,
    goal_id             BIGINT NOT NULL REFERENCES mentorship_goals(id) ON DELETE CASCADE,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    is_completed        BOOLEAN NOT NULL DEFAULT false,
    due_date            DATE,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_goal_action_items_goal ON goal_action_items(goal_id);

-- 4. Mentor-to-Mentee Evaluations & Progress Reviews
CREATE TABLE mentor_evaluations (
    id                  BIGSERIAL PRIMARY KEY,
    mentor_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id          BIGINT REFERENCES mentorship_sessions(id) ON DELETE SET NULL,
    rating              INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    strengths           TEXT,
    areas_for_growth    TEXT,
    comments            TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_mentor_evaluations_mentor ON mentor_evaluations(mentor_id);
CREATE INDEX idx_mentor_evaluations_employee ON mentor_evaluations(employee_id);

-- 5. Additive Columns to Existing Tables
ALTER TABLE mentorship_sessions ADD COLUMN IF NOT EXISTS shared_notes TEXT;
ALTER TABLE mentorship_sessions ADD COLUMN IF NOT EXISTS mentor_prep_notes TEXT;

ALTER TABLE shared_resources ADD COLUMN IF NOT EXISTS resource_type VARCHAR(30) DEFAULT 'ARTICLE';
ALTER TABLE shared_resources ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE shared_resources ADD COLUMN IF NOT EXISTS skill_id BIGINT REFERENCES skills(id) ON DELETE SET NULL;

ALTER TABLE mentor_profiles ADD COLUMN IF NOT EXISTS preferred_format VARCHAR(30) DEFAULT 'VIRTUAL';
ALTER TABLE mentor_profiles ADD COLUMN IF NOT EXISTS bio TEXT;

ALTER TABLE mentorship_goals ADD COLUMN IF NOT EXISTS priority VARCHAR(20) DEFAULT 'MEDIUM';
ALTER TABLE mentorship_goals ADD COLUMN IF NOT EXISTS skill_id BIGINT REFERENCES skills(id) ON DELETE SET NULL;

-- 6. Expand Goal Status Constraint
ALTER TABLE mentorship_goals DROP CONSTRAINT IF EXISTS mentorship_goals_status_check;
ALTER TABLE mentorship_goals ADD CONSTRAINT mentorship_goals_status_check 
    CHECK (status IN ('PROPOSED', 'AGREED', 'IN_PROGRESS', 'ACHIEVED', 'COMPLETED', 'ON_HOLD', 'ABANDONED', 'CANCELLED'));
