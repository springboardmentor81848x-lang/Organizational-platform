-- Notifications shown on the Team Manager dashboard (safe to run on every startup).
CREATE TABLE IF NOT EXISTS mgr_notifications (
  notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  manager_id      BIGINT NOT NULL,
  employee_id     BIGINT NULL,
  type            VARCHAR(30) NOT NULL DEFAULT 'INFO',
  message         VARCHAR(300) NOT NULL,
  is_read         TINYINT(1) NOT NULL DEFAULT 0,
  created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_mgr_notif (manager_id, is_read)
) ENGINE=InnoDB;
