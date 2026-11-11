-- =========================================================================
-- V7: Manager Dashboard Enhancements
-- Adds formal Reassessment Requests and Manager Course Recommendations
-- =========================================================================

-- 1. Reassessment Requests (Module 6)
CREATE TABLE reassessment_requests (
    id              BIGSERIAL PRIMARY KEY,
    manager_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id        BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    reason          TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reassessment_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_reassessment_manager ON reassessment_requests(manager_id);
CREATE INDEX idx_reassessment_employee ON reassessment_requests(employee_id);

-- 2. Manager Course Recommendations (Module 7)
CREATE TABLE manager_course_recommendations (
    id              BIGSERIAL PRIMARY KEY,
    manager_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id       BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    skill_id        BIGINT REFERENCES skills(id) ON DELETE SET NULL,
    reason          TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'RECOMMENDED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_mgr_rec_status CHECK (status IN ('RECOMMENDED', 'ENROLLED', 'DISMISSED'))
);

CREATE INDEX idx_mgr_rec_manager ON manager_course_recommendations(manager_id);
CREATE INDEX idx_mgr_rec_employee ON manager_course_recommendations(employee_id);
