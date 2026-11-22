-- Messages feature (employee-employee chat after follow + accept, employee-mentor chat).
-- The backend creates these automatically on startup (MessagingSchema); run this by hand only if you see "MessagingSchema FAILED".
CREATE TABLE IF NOT EXISTS ed_connections (
  connection_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  requester_id  BIGINT NOT NULL,
  addressee_id  BIGINT NOT NULL,
  status        VARCHAR(12) NOT NULL DEFAULT 'PENDING',   -- PENDING until accepted, then ACCEPTED
  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
  responded_at  DATETIME NULL,
  UNIQUE KEY uq_ed_connection (requester_id, addressee_id),
  INDEX idx_ed_conn_inbox (addressee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ed_messages (
  message_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id    BIGINT NOT NULL,
  recipient_id BIGINT NOT NULL,
  body         VARCHAR(2000) NOT NULL,
  created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
  read_at      DATETIME NULL,
  INDEX idx_ed_msg_pair (sender_id, recipient_id, message_id),
  INDEX idx_ed_msg_inbox (recipient_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
