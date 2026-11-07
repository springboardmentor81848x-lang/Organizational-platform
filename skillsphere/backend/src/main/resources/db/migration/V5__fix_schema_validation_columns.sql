-- V5: Add missing columns required for Hibernate schema validation
-- Resolves startup failure: "Schema-validation: missing column [reply_count] in table [discussion_threads]"
-- Also resolves missing columns in team_memberships and training_programmes

-- 1. discussion_threads: reply_count
ALTER TABLE discussion_threads
    ADD COLUMN IF NOT EXISTS reply_count INT NOT NULL DEFAULT 0;

-- Backfill reply_count from existing replies
UPDATE discussion_threads dt
SET reply_count = COALESCE((
    SELECT COUNT(*)
    FROM discussion_replies dr
    WHERE dr.thread_id = dt.id
), 0);

-- 2. team_memberships: role_in_team
ALTER TABLE team_memberships
    ADD COLUMN IF NOT EXISTS role_in_team VARCHAR(50) NOT NULL DEFAULT 'MEMBER';

-- 3. training_programmes: enrolled_count
ALTER TABLE training_programmes
    ADD COLUMN IF NOT EXISTS enrolled_count INT NOT NULL DEFAULT 0;

-- Backfill enrolled_count from active/attended enrollments
UPDATE training_programmes tp
SET enrolled_count = COALESCE((
    SELECT COUNT(*)
    FROM training_enrollments te
    WHERE te.programme_id = tp.id
      AND te.status != 'CANCELLED'
), 0);
