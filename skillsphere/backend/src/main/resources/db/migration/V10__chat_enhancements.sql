-- V10__chat_enhancements.sql
-- Additive enhancements for Chatbox & Communication Module:
-- 1. Conversation muting and archiving per participant
-- 2. Optional team link for team channels
-- 3. Inappropriate message reporting table

ALTER TABLE conversation_participants
    ADD COLUMN IF NOT EXISTS is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_archived BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS team_id BIGINT REFERENCES teams(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS message_reports (
    id              BIGSERIAL PRIMARY KEY,
    message_id      BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    reporter_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason          VARCHAR(255) NOT NULL,
    details         TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_msg_report UNIQUE (message_id, reporter_id)
);

CREATE INDEX IF NOT EXISTS idx_msg_reports_status ON message_reports(status);
CREATE INDEX IF NOT EXISTS idx_conversations_team ON conversations(team_id);
