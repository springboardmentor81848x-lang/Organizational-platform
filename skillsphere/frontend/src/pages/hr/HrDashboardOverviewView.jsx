import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getHrOverview } from "../../services/hrService.js";

export default function HrDashboardOverviewView({ onNavigateTab }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getHrOverview();
      setData(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load HR overview.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) {
    return (
      <div className="hr-empty-state">
        <Icon name="spinner" className="spin" size={32} />
        <h4>Loading Executive Dashboard...</h4>
      </div>
    );
  }

  if (error) {
    return <Alert type="danger" message={error} />;
  }

  return (
    <div>
      {/* Stat Grid */}
      <div className="hr-stat-grid">
        <div className="hr-stat-card">
          <div className="hr-stat-header">
            <span>Total Workforce</span>
            <div className="hr-stat-icon primary">
              <Icon name="users" size={18} />
            </div>
          </div>
          <div className="hr-stat-value">{data?.totalEmployees ?? 0}</div>
          <div className="hr-stat-meta">Active employees registered</div>
        </div>

        <div className="hr-stat-card">
          <div className="hr-stat-header">
            <span>Skill Coverage</span>
            <div className="hr-stat-icon emerald">
              <Icon name="target" size={18} />
            </div>
          </div>
          <div className="hr-stat-value">{data?.skillCoveragePercentage ?? 0}%</div>
          <div className="hr-stat-meta">
            {data?.assessedEmployeesCount ?? 0} assessed / {data?.unassessedEmployeesCount ?? 0} unassessed
          </div>
        </div>

        <div className="hr-stat-card">
          <div className="hr-stat-header">
            <span>Critical Shortages</span>
            <div className="hr-stat-icon rose">
              <Icon name="alert-triangle" size={18} />
            </div>
          </div>
          <div className="hr-stat-value">{data?.criticalShortagesCount ?? 0}</div>
          <div className="hr-stat-meta">{data?.totalOrgGapsCount ?? 0} total gaps tracked</div>
        </div>

        <div className="hr-stat-card">
          <div className="hr-stat-header">
            <span>Active Programmes</span>
            <div className="hr-stat-icon blue">
              <Icon name="calendar" size={18} />
            </div>
          </div>
          <div className="hr-stat-value">{data?.activeProgrammesCount ?? 0}</div>
          <div className="hr-stat-meta">{data?.totalCoursesCount ?? 0} catalogue courses</div>
        </div>

        <div className="hr-stat-card">
          <div className="hr-stat-header">
            <span>Course Completion</span>
            <div className="hr-stat-icon amber">
              <Icon name="trending" size={18} />
            </div>
          </div>
          <div className="hr-stat-value">{data?.avgCompletionRate ?? 0}%</div>
          <div className="hr-stat-meta">{data?.totalEnrollmentsCount ?? 0} total enrollments</div>
        </div>
      </div>

      {/* Pending Actions Alert Card */}
      {data?.pendingActions && data.pendingActions.length > 0 && (
        <div className="hr-card" style={{ borderLeft: "4px solid var(--hr-amber)" }}>
          <div className="hr-card-header">
            <div>
              <h3 className="hr-card-title">Priority Attention Required</h3>
              <p className="hr-card-subtitle">Recommended actions to maintain workforce competency and readiness</p>
            </div>
          </div>
          <div className="hr-card-body" style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            {data.pendingActions.map((act, i) => (
              <div
                key={i}
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  padding: "12px 16px",
                  background: "#f8fafc",
                  borderRadius: "8px",
                  border: "1px solid var(--hr-line)",
                }}
              >
                <div>
                  <div style={{ fontWeight: 600, color: "var(--hr-ink)", marginBottom: 2 }}>{act.title}</div>
                  <div style={{ fontSize: "13px", color: "var(--hr-soft)" }}>{act.description}</div>
                </div>
                <button
                  className="btn-secondary btn-sm"
                  onClick={() => onNavigateTab && onNavigateTab(act.linkTab)}
                >
                  Resolve
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Grid: Upcoming Training & Recent Activities */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(400px, 1fr))", gap: "24px" }}>
        {/* Upcoming Programmes */}
        <div className="hr-card">
          <div className="hr-card-header">
            <div>
              <h3 className="hr-card-title">Upcoming Training Programmes</h3>
              <p className="hr-card-subtitle">Scheduled sessions ready for workforce delivery</p>
            </div>
            <button className="btn-secondary btn-sm" onClick={() => onNavigateTab("training-programmes")}>
              View All
            </button>
          </div>
          <div className="hr-card-body" style={{ padding: 0 }}>
            {(!data?.upcomingProgrammes || data.upcomingProgrammes.length === 0) ? (
              <div className="hr-empty-state" style={{ padding: "30px 20px" }}>
                <p>No upcoming training programmes scheduled.</p>
              </div>
            ) : (
              <div className="hr-table-wrap">
                <table className="hr-table">
                  <thead>
                    <tr>
                      <th>Programme</th>
                      <th>Department</th>
                      <th>Dates</th>
                      <th>Capacity</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.upcomingProgrammes.map((p) => (
                      <tr key={p.id}>
                        <td style={{ fontWeight: 600 }}>{p.title}</td>
                        <td>{p.departmentName}</td>
                        <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                          {p.startDate} to {p.endDate}
                        </td>
                        <td>
                          {p.enrolledCount} / {p.capacity}
                        </td>
                        <td>
                          <span className={`status-pill ${p.status === "ACTIVE" ? "success" : "info"}`}>
                            {p.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Recent Activities */}
        <div className="hr-card">
          <div className="hr-card-header">
            <div>
              <h3 className="hr-card-title">Workforce Learning Activity</h3>
              <p className="hr-card-subtitle">Live events across skill assessments and completions</p>
            </div>
          </div>
          <div className="hr-card-body">
            {(!data?.recentActivities || data.recentActivities.length === 0) ? (
              <div className="hr-empty-state" style={{ padding: "30px 20px" }}>
                <p>No recent activities recorded.</p>
              </div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
                {data.recentActivities.map((act, i) => (
                  <div
                    key={i}
                    style={{
                      display: "flex",
                      alignItems: "flex-start",
                      gap: "12px",
                      paddingBottom: "12px",
                      borderBottom: i < data.recentActivities.length - 1 ? "1px solid var(--hr-line)" : "none",
                    }}
                  >
                    <div
                      className={`hr-stat-icon ${act.type === "ASSESSMENT_COMPLETED" ? "primary" : "emerald"}`}
                      style={{ width: 32, height: 32 }}
                    >
                      <Icon name={act.type === "ASSESSMENT_COMPLETED" ? "award" : "check"} size={16} />
                    </div>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 600, fontSize: "13.5px", color: "var(--hr-ink)" }}>
                        {act.title}
                      </div>
                      <div style={{ fontSize: "13px", color: "var(--hr-soft)" }}>{act.description}</div>
                      <div style={{ fontSize: "11.5px", color: "var(--hr-faint)", marginTop: 2 }}>
                        {act.timestamp ? new Date(act.timestamp).toLocaleString() : ""}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
