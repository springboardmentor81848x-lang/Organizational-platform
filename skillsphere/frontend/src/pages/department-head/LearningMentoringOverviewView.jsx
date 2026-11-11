import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getDepartmentLearningMentoring } from "../../services/departmentHeadService.js";

export default function LearningMentoringOverviewView() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getDepartmentLearningMentoring();
      setData(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load Learning & Mentoring overview.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "60px 20px" }}>
        <Icon name="refresh" className="spin" size={32} />
        <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Gathering Learning & Mentoring Intelligence...</h4>
      </div>
    );
  }

  if (error) {
    return <Alert type="danger" message={error} />;
  }

  const { learningOverview, mentoringOverview } = data || {};

  return (
    <div>
      {/* Privacy Guarantee Banner */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "12px",
          padding: "12px 18px",
          borderRadius: "8px",
          background: "var(--dh-primary-tint)",
          border: "1px solid rgba(15, 118, 110, 0.2)",
          marginBottom: "20px",
        }}
      >
        <span style={{ color: "var(--dh-primary-dark)" }}>
          <Icon name="shield" size={20} />
        </span>
        <div style={{ fontSize: "13px", color: "var(--dh-primary-dark)" }}>
          <strong>Department Privacy Guarantee:</strong> Department Head views present aggregate learning metrics and program outcomes. Individual mentor preparation notes and private mentee chat communications are strictly confidential and excluded from management oversight.
        </div>
      </div>

      {/* Learning Intelligence KPI Grid */}
      <h3 style={{ fontSize: "16px", fontWeight: "700", color: "var(--dh-ink)", marginBottom: "14px" }}>
        Corporate Learning & Upskilling Velocity
      </h3>
      <div className="dh-kpi-grid">
        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Active Learners</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-primary)" }}>
              <Icon name="users" size={18} />
            </span>
          </div>
          <div className="dh-kpi-value">{learningOverview?.totalEnrolledEmployees ?? 0}</div>
          <div className="dh-kpi-subtext">Total enrollments: {learningOverview?.totalCourseEnrollments ?? 0}</div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Completion Velocity</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-emerald)" }}>
              <Icon name="check" size={18} />
            </span>
          </div>
          <div className="dh-kpi-value">{learningOverview?.completionRatePercentage ?? 0}%</div>
          <div className="dh-kpi-subtext">
            {learningOverview?.completedCount ?? 0} completed / {learningOverview?.inProgressCount ?? 0} in progress
          </div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Department Programmes</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-accent)" }}>
              <Icon name="book" size={18} />
            </span>
          </div>
          <div className="dh-kpi-value">{learningOverview?.programmesCount ?? 0}</div>
          <div className="dh-kpi-subtext">Sponsored bootcamps & academies</div>
        </div>
      </div>

      {/* Top Courses Participation Table */}
      <div className="dh-panel">
        <div className="dh-panel-header">
          <h3 className="dh-panel-title">
            <Icon name="book" size={18} />
            Highest Demand Learning Resources
          </h3>
        </div>
        <div className="dh-table-container">
          <table className="dh-table">
            <thead>
              <tr>
                <th>Course Title</th>
                <th>Provider</th>
                <th>Difficulty Level</th>
                <th>Enrolled Personnel</th>
                <th>Completed</th>
                <th>Average Progress</th>
              </tr>
            </thead>
            <tbody>
              {learningOverview?.topCourses && learningOverview.topCourses.length > 0 ? (
                learningOverview.topCourses.map((c) => (
                  <tr key={c.courseId}>
                    <td>
                      <strong>{c.courseTitle}</strong>
                    </td>
                    <td>{c.provider || "SkillSphere Academy"}</td>
                    <td>
                      <span className="dh-badge">{c.level || "Intermediate"}</span>
                    </td>
                    <td>{c.enrolledCount} Engineers</td>
                    <td>{c.completedCount}</td>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <div
                          style={{
                            width: "70px",
                            height: "6px",
                            borderRadius: "3px",
                            background: "#e2e8f0",
                            overflow: "hidden",
                          }}
                        >
                          <div
                            style={{
                              width: `${Math.min(100, c.avgProgressPct)}%`,
                              height: "100%",
                              background: "var(--dh-primary)",
                            }}
                          />
                        </div>
                        <span style={{ fontSize: "12px", fontWeight: "600" }}>{c.avgProgressPct}%</span>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} style={{ textAlign: "center", color: "var(--dh-faint)", padding: "20px" }}>
                    No department course enrollments recorded.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Mentorship Operational Health */}
      <h3 style={{ fontSize: "16px", fontWeight: "700", color: "var(--dh-ink)", margin: "30px 0 14px" }}>
        Mentorship Ecosystem Performance
      </h3>
      <div className="dh-kpi-grid">
        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Active Pairings</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-indigo)" }}>
              <Icon name="users" size={18} />
            </span>
          </div>
          <div className="dh-kpi-value">{mentoringOverview?.activeAssignmentsCount ?? 0}</div>
          <div className="dh-kpi-subtext">
            {mentoringOverview?.activeMenteesCount ?? 0} mentees guided by {mentoringOverview?.activeMentorsCount ?? 0} mentors
          </div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Sessions Delivered</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-emerald)" }}>
              <Icon name="clock" size={18} />
            </span>
          </div>
          <div className="dh-kpi-value">{mentoringOverview?.totalSessionsCompleted ?? 0}</div>
          <div className="dh-kpi-subtext">Completed developmental sessions</div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Satisfaction Index</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-amber)" }}>
              <Icon name="award" size={18} />
            </span>
          </div>
          <div className="dh-kpi-value">
            {mentoringOverview?.avgMentorshipRating ?? 0} <span style={{ fontSize: "16px", color: "var(--dh-soft)" }}>/ 5.0</span>
          </div>
          <div className="dh-kpi-subtext">Based on post-session mentee ratings</div>
        </div>
      </div>

      {/* Top Mentored Competencies */}
      {mentoringOverview?.topMentoredSkills && mentoringOverview.topMentoredSkills.length > 0 && (
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="sparkles" size={18} />
              Key Competencies Supported Through Mentorship
            </h3>
          </div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
            {mentoringOverview.topMentoredSkills.map((skill, i) => (
              <span
                key={i}
                style={{
                  padding: "8px 14px",
                  borderRadius: "20px",
                  background: "#f8fafc",
                  border: "1px solid var(--dh-line)",
                  fontSize: "13px",
                  fontWeight: "600",
                  color: "var(--dh-ink)",
                }}
              >
                ★ {skill}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
