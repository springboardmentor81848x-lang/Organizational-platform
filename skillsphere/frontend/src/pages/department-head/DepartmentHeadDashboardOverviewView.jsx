import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getDepartmentHeadOverview } from "../../services/departmentHeadService.js";

export default function DepartmentHeadDashboardOverviewView({ onNavigateTab }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getDepartmentHeadOverview();
      setData(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load Department Head overview.");
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
        <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Loading Department Strategic Overview...</h4>
      </div>
    );
  }

  if (error) {
    return <Alert type="danger" message={error} />;
  }

  return (
    <div>
      {/* KPI Grid */}
      <div className="dh-kpi-grid">
        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Workforce Strength</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-primary)" }}>
              <Icon name="users" size={20} />
            </span>
          </div>
          <div className="dh-kpi-value">{data?.totalEmployees ?? 0}</div>
          <div className="dh-kpi-subtext">Across {data?.totalTeams ?? 0} functional teams</div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Skill Coverage</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-emerald)" }}>
              <Icon name="target" size={20} />
            </span>
          </div>
          <div className="dh-kpi-value">{data?.skillCoveragePercentage ?? 0}%</div>
          <div className="dh-kpi-subtext">{data?.assessedEmployeesCount ?? 0} evaluated / {data?.unassessedEmployeesCount ?? 0} pending</div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Critical Shortages</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-rose)" }}>
              <Icon name="alert" size={20} />
            </span>
          </div>
          <div className="dh-kpi-value" style={{ color: data?.criticalShortagesCount > 0 ? "var(--dh-rose)" : "inherit" }}>
            {data?.criticalShortagesCount ?? 0}
          </div>
          <div className="dh-kpi-subtext">Total gaps: {data?.totalDepartmentGapsCount ?? 0}</div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Active Programmes</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-accent)" }}>
              <Icon name="book" size={20} />
            </span>
          </div>
          <div className="dh-kpi-value">{data?.activeProgrammesCount ?? 0}</div>
          <div className="dh-kpi-subtext">{data?.totalEnrollmentsCount ?? 0} enrollments ({data?.avgCompletionRate ?? 0}% completion)</div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Mentorship Cohorts</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-indigo)" }}>
              <Icon name="award" size={20} />
            </span>
          </div>
          <div className="dh-kpi-value">{data?.activeMentorshipsCount ?? 0}</div>
          <div className="dh-kpi-subtext">Active 1-on-1 pairings</div>
        </div>
      </div>

      {/* Strategic Priority Alerts */}
      {data?.priorityAlerts && data.priorityAlerts.length > 0 && (
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="alert" size={18} style={{ color: "var(--dh-amber)" }} />
              Strategic Risk & Action Flags
            </h3>
            <button className="dh-btn dh-btn-secondary" onClick={() => onNavigateTab("risks")}>
              View All Risks
            </button>
          </div>
          <div style={{ display: "grid", gap: "10px" }}>
            {data.priorityAlerts.map((alert, idx) => (
              <div
                key={idx}
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  padding: "12px 16px",
                  borderRadius: "8px",
                  background: alert.severity === "CRITICAL" ? "var(--dh-rose-tint)" : "#fffbeb",
                  border: `1px solid ${alert.severity === "CRITICAL" ? "#fca5a5" : "#fcd34d"}`,
                }}
              >
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <span className={`dh-status-badge ${alert.severity.toLowerCase()}`}>{alert.severity}</span>
                    <strong style={{ fontSize: "14px", color: "var(--dh-ink)" }}>{alert.title}</strong>
                  </div>
                  <div style={{ fontSize: "13px", color: "var(--dh-soft)", marginTop: "4px" }}>
                    {alert.description}
                  </div>
                </div>
                <button
                  className="dh-btn dh-btn-secondary"
                  style={{ fontSize: "12px", padding: "6px 12px" }}
                  onClick={() => onNavigateTab(alert.linkTab || "risks")}
                >
                  Investigate
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Two Column Layout: Upcoming Programmes & Recent Activities */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px" }}>
        {/* Upcoming Programmes */}
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="calendar" size={18} />
              Upcoming Training Programmes
            </h3>
            <button className="dh-btn dh-btn-secondary" onClick={() => onNavigateTab("learning-mentoring")}>
              View Cohorts
            </button>
          </div>
          {data?.upcomingProgrammes && data.upcomingProgrammes.length > 0 ? (
            <div className="dh-table-container">
              <table className="dh-table">
                <thead>
                  <tr>
                    <th>Programme</th>
                    <th>Timeline</th>
                    <th>Enrollment</th>
                    <th>Trainer</th>
                  </tr>
                </thead>
                <tbody>
                  {data.upcomingProgrammes.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <strong>{p.title}</strong>
                      </td>
                      <td style={{ fontSize: "12px", color: "var(--dh-soft)" }}>
                        {p.startDate || "TBD"} – {p.endDate || "TBD"}
                      </td>
                      <td>
                        <span className="dh-badge">
                          {p.enrolledCount} / {p.capacity || "Open"}
                        </span>
                      </td>
                      <td>{p.trainerName || "L&D Specialist"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p style={{ color: "var(--dh-faint)", fontSize: "13.5px" }}>No upcoming training programmes scheduled.</p>
          )}
        </div>

        {/* Recent Activities */}
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="trending" size={18} />
              Department Operational Stream
            </h3>
          </div>
          {data?.recentActivities && data.recentActivities.length > 0 ? (
            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              {data.recentActivities.map((act, i) => (
                <div
                  key={i}
                  style={{
                    display: "flex",
                    alignItems: "flex-start",
                    gap: "12px",
                    paddingBottom: "10px",
                    borderBottom: i < data.recentActivities.length - 1 ? "1px solid #f1f5f9" : "none",
                  }}
                >
                  <div
                    style={{
                      width: "32px",
                      height: "32px",
                      borderRadius: "50%",
                      background: "var(--dh-primary-tint)",
                      color: "var(--dh-primary)",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      flexShrink: 0,
                    }}
                  >
                    <Icon name={act.type === "ASSESSMENT" ? "check" : act.type === "ENROLLMENT" ? "book" : "bell"} size={16} />
                  </div>
                  <div>
                    <div style={{ fontSize: "13.5px", fontWeight: "600", color: "var(--dh-ink)" }}>{act.title}</div>
                    <div style={{ fontSize: "12.5px", color: "var(--dh-soft)" }}>{act.description}</div>
                    <div style={{ fontSize: "11px", color: "var(--dh-faint)", marginTop: "2px" }}>
                      {act.actorName} • {act.timestamp ? new Date(act.timestamp).toLocaleDateString() : "Recent"}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: "var(--dh-faint)", fontSize: "13.5px" }}>No recent activity records found.</p>
          )}
        </div>
      </div>
    </div>
  );
}
