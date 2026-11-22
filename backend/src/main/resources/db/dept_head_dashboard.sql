-- dept_head_dashboard.sql
-- Safe-to-rerun SQL for the Department Head dashboard tables and sample data.
-- All CREATE TABLE statements use IF NOT EXISTS; inserts use INSERT IGNORE.

-- ============================================================
-- dh_risk_alerts: capability risk tracking for a department
-- ============================================================
CREATE TABLE IF NOT EXISTS dh_risk_alerts (
  alert_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  department_id     BIGINT       NOT NULL,
  skill_name        VARCHAR(100) NOT NULL,
  team_name         VARCHAR(100),
  severity          VARCHAR(20)  NOT NULL DEFAULT 'HIGH',  -- CRITICAL | HIGH | MEDIUM
  description       VARCHAR(500),
  recommended_action VARCHAR(300),
  acknowledged      TINYINT(1)   NOT NULL DEFAULT 0,
  acknowledged_by   BIGINT,
  acknowledged_at   DATETIME,
  created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dh_alert_dept (department_id, acknowledged)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- dh_announcements: department-scoped announcements
-- ============================================================
CREATE TABLE IF NOT EXISTS dh_announcements (
  announcement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  department_id   BIGINT       NOT NULL,
  author_id       BIGINT       NOT NULL,
  title           VARCHAR(200) NOT NULL,
  body            TEXT         NOT NULL,
  created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dh_ann_dept (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Sample risk alerts for department_id = 1
-- ============================================================
INSERT IGNORE INTO dh_risk_alerts
  (alert_id, department_id, skill_name, team_name, severity, description, recommended_action, acknowledged)
VALUES
  (1, 1, 'Cloud Architecture', 'Team Alpha', 'CRITICAL',
   'Only 20% of Alpha team members meet the required Cloud Architecture level. Critical delivery risk for Q1 cloud migration.',
   'Enroll all Alpha engineers in AWS Solutions Architect course immediately. Target 60% coverage by end of month.',
   0),
  (2, 1, 'Kubernetes & Container Orchestration', 'Team Beta', 'CRITICAL',
   'Team Beta has zero certified Kubernetes practitioners. DevOps pipeline is at risk.',
   'Fast-track Kubernetes certification for at least 2 Beta team leads. Consider external trainer for in-house workshop.',
   0),
  (3, 1, 'Cybersecurity & Compliance', 'Team Gamma', 'HIGH',
   'Cybersecurity skills average is 1.8/5 across Team Gamma, below the department minimum of 3.0.',
   'Schedule CompTIA Security+ training cohort. Pair with Team Delta security SME for mentorship.',
   0),
  (4, 1, 'Machine Learning / AI', 'Team Delta', 'HIGH',
   'AI initiative requires ML skills at level 3+. Only 30% of Delta team qualifies.',
   'Identify top 5 candidates for ML upskilling. Enroll in Google ML crash course and internal AI bootcamp.',
   0),
  (5, 1, 'Data Analysis & BI', 'Team Alpha', 'MEDIUM',
   'Power BI and Tableau coverage is insufficient for the upcoming analytics project.',
   'Assign online learning paths for BI tools. Target completion within 6 weeks.',
   0),
  (6, 1, 'Agile Leadership', 'Team Beta', 'MEDIUM',
   'No certified Scrum Masters in Beta team leading to process inefficiencies.',
   'Nominate 1 Beta team member for Scrum Master certification this quarter.',
   0);

-- ============================================================
-- Sample announcements for department_id = 1
-- ============================================================
INSERT IGNORE INTO dh_announcements
  (announcement_id, department_id, author_id, title, body)
VALUES
  (1, 1, 1,
   'Q4 Skill Development Sprint – Action Required',
   'All team managers must submit their team skill gap reports by November 15th. The department is targeting 70% skill coverage across all critical areas by year-end. Please prioritize enrollments in the Cloud, Security, and AI tracks. Reach out to HR if you need budget approvals expedited.'),
  (2, 1, 1,
   'New Mentorship Program Launched',
   'We are excited to launch the departmental cross-team mentorship program. Senior engineers with level 4+ skills are encouraged to sign up as mentors. This initiative aims to reduce critical skill gaps within 90 days. Sign up via the WKI portal under Learning & Mentoring.'),
  (3, 1, 1,
   'Kubernetes Workshop – Nov 20th',
   'An in-house Kubernetes workshop has been arranged for November 20th (full day). All Team Beta members are mandatorily invited. Team Alpha and Gamma members with DevOps responsibilities should also attend. Location: Training Room B, 3rd Floor.'),
  (4, 1, 1,
   'Year-End Performance Review Reminder',
   'Skill assessments completed on the WKI platform will be factored into the year-end performance review. Please ensure all team members have completed at least 3 skill assessments and 2 course enrollments before December 1st.');
