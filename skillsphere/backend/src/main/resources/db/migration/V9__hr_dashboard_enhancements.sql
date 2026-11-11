-- =========================================================================
-- V9: HR Specialist Dashboard Enhancements
-- Adds HR announcements, read receipts, leadership report sharing,
-- and enhances learning paths, training programmes, courses, and skills.
-- =========================================================================

-- 1. HR Announcements & Targeted Reminders
CREATE TABLE hr_announcements (
    id                  BIGSERIAL PRIMARY KEY,
    author_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type         VARCHAR(30) NOT NULL DEFAULT 'ORGANIZATION' 
                        CHECK (target_type IN ('ORGANIZATION', 'DEPARTMENT', 'ROLE')),
    department_id       BIGINT REFERENCES departments(id) ON DELETE CASCADE,
    target_role         VARCHAR(80),
    title               VARCHAR(200) NOT NULL,
    content             TEXT NOT NULL,
    priority            VARCHAR(20) NOT NULL DEFAULT 'NORMAL' 
                        CHECK (priority IN ('NORMAL', 'HIGH', 'URGENT')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_hr_announcements_author ON hr_announcements(author_id);
CREATE INDEX idx_hr_announcements_dept ON hr_announcements(department_id);

-- 2. Announcement Read Receipts
CREATE TABLE hr_announcement_reads (
    id                  BIGSERIAL PRIMARY KEY,
    announcement_id     BIGINT NOT NULL REFERENCES hr_announcements(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_announcement_user UNIQUE (announcement_id, user_id)
);

CREATE INDEX idx_hr_announcement_reads_user ON hr_announcement_reads(user_id);

-- 3. Leadership Report Sharing (HR to Managers & Dept Heads)
CREATE TABLE leadership_report_shares (
    id                  BIGSERIAL PRIMARY KEY,
    report_type         VARCHAR(60) NOT NULL,
    title               VARCHAR(200) NOT NULL,
    shared_by_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shared_with_role    VARCHAR(30) NOT NULL 
                        CHECK (shared_with_role IN ('MANAGER', 'DEPARTMENT_HEAD', 'ALL_LEADERSHIP')),
    department_id       BIGINT REFERENCES departments(id) ON DELETE CASCADE,
    summary_notes       TEXT,
    report_data_json    TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_report_shares_role ON leadership_report_shares(shared_with_role);
CREATE INDEX idx_report_shares_dept ON leadership_report_shares(department_id);

-- 4. Additive Columns to Learning Paths and Training Programmes
ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS skill_id BIGINT REFERENCES skills(id) ON DELETE SET NULL;
ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS difficulty_level VARCHAR(30) DEFAULT 'Beginner';

ALTER TABLE training_programmes ADD COLUMN IF NOT EXISTS skill_id BIGINT REFERENCES skills(id) ON DELETE SET NULL;
ALTER TABLE training_programmes ADD COLUMN IF NOT EXISTS course_id BIGINT REFERENCES courses(id) ON DELETE SET NULL;

-- 5. Additive Columns to Skills, Courses and Role Requirements
ALTER TABLE skills ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE role_skill_requirements ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
