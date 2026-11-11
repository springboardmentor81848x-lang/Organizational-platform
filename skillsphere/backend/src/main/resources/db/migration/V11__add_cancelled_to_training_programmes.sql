-- =========================================================================
-- V11: Training Programme Lifecycle Enhancements
-- Allows CANCELLED status in training_programmes status check constraint
-- =========================================================================

ALTER TABLE training_programmes DROP CONSTRAINT IF EXISTS training_programmes_status_check;
ALTER TABLE training_programmes ADD CONSTRAINT training_programmes_status_check 
    CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELLED'));
