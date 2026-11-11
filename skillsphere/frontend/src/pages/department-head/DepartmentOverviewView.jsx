import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getDepartmentOverview } from "../../services/departmentHeadService.js";

export default function DepartmentOverviewView({ onNavigateTab }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getDepartmentOverview();
      setData(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load Department details.");
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
        <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Loading Department Profile & Teams...</h4>
      </div>
    );
  }

  if (error) {
    return <Alert type="danger" message={error} />;
  }

  return (
    <div>
      {/* Department Profile Summary */}
      <div className="dh-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
          <div>
            <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
              <h2 style={{ margin: 0, fontSize: "22px", color: "var(--dh-ink)" }}>{data?.name}</h2>
              <span className="dh-badge">{data?.code}</span>
            </div>
            <p style={{ color: "var(--dh-soft)", marginTop: "6px", fontSize: "14px" }}>
              {data?.description || "Strategic organizational unit."}
            </p>
          </div>
          <div style={{ textAlign: "right", background: "#f8fafc", padding: "10px 16px", borderRadius: "8px", border: "1px solid var(--dh-line)" }}>
            <div style={{ fontSize: "12px", color: "var(--dh-faint)", textTransform: "uppercase", fontWeight: "600" }}>
              Department Head
            </div>
            <div style={{ fontSize: "14px", fontWeight: "700", color: "var(--dh-ink)", marginTop: "2px" }}>
              {data?.headName}
            </div>
            <div style={{ fontSize: "12px", color: "var(--dh-soft)" }}>{data?.headEmail}</div>
          </div>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "16px", marginTop: "20px", borderTop: "1px solid #f1f5f9", paddingTop: "16px" }}>
          <div>
            <span style={{ fontSize: "12px", color: "var(--dh-soft)" }}>Total Members</span>
            <div style={{ fontSize: "20px", fontWeight: "800", color: "var(--dh-ink)" }}>{data?.totalEmployees ?? 0}</div>
          </div>
          <div>
            <span style={{ fontSize: "12px", color: "var(--dh-soft)" }}>Total Teams</span>
            <div style={{ fontSize: "20px", fontWeight: "800", color: "var(--dh-ink)" }}>{data?.totalTeams ?? 0}</div>
          </div>
          <div>
            <span style={{ fontSize: "12px", color: "var(--dh-soft)" }}>Designated Managers</span>
            <div style={{ fontSize: "20px", fontWeight: "800", color: "var(--dh-ink)" }}>{data?.totalManagers ?? 0}</div>
          </div>
        </div>
      </div>

      {/* Teams Breakdown */}
      <div className="dh-panel">
        <div className="dh-panel-header">
          <h3 className="dh-panel-title">
            <Icon name="users" size={18} />
            Department Teams & Operational Health
          </h3>
          <button className="dh-btn dh-btn-secondary" onClick={() => onNavigateTab("skill-matrix")}>
            View Matrix
          </button>
        </div>

        <div className="dh-table-container">
          <table className="dh-table">
            <thead>
              <tr>
                <th>Team Name</th>
                <th>Manager / Lead</th>
                <th>Members</th>
                <th>Assessed</th>
                <th>Coverage %</th>
                <th>Critical Gaps</th>
                <th>Top Competencies</th>
              </tr>
            </thead>
            <tbody>
              {data?.teams && data.teams.length > 0 ? (
                data.teams.map((t) => (
                  <tr key={t.teamId}>
                    <td>
                      <strong>{t.teamName}</strong>
                    </td>
                    <td>
                      <div>{t.managerName}</div>
                      <div style={{ fontSize: "11px", color: "var(--dh-faint)" }}>{t.managerEmail}</div>
                    </td>
                    <td>{t.memberCount}</td>
                    <td>{t.assessedCount}</td>
                    <td>
                      <span
                        className={`dh-status-badge ${
                          t.skillCoveragePercentage >= 75
                            ? "low"
                            : t.skillCoveragePercentage >= 50
                            ? "medium"
                            : "critical"
                        }`}
                      >
                        {t.skillCoveragePercentage}%
                      </span>
                    </td>
                    <td>
                      <span
                        className={`dh-status-badge ${
                          t.criticalGapsCount === 0 ? "low" : "critical"
                        }`}
                      >
                        {t.criticalGapsCount} Gaps
                      </span>
                    </td>
                    <td>
                      <div style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
                        {t.topSkills && t.topSkills.length > 0 ? (
                          t.topSkills.map((s, i) => (
                            <span key={i} className="dh-badge" style={{ fontSize: "10px" }}>
                              {s}
                            </span>
                          ))
                        ) : (
                          <span style={{ fontSize: "11px", color: "var(--dh-faint)" }}>None recorded</span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", color: "var(--dh-faint)", padding: "20px" }}>
                    No teams configured in this department.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Workforce Role Distribution */}
      {data?.workforceDistribution && data.workforceDistribution.length > 0 && (
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="chart" size={18} />
              Workforce Distribution by Job Role
            </h3>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "12px" }}>
            {data.workforceDistribution.map((role, idx) => (
              <div
                key={idx}
                style={{
                  padding: "14px",
                  borderRadius: "8px",
                  background: "#f8fafc",
                  border: "1px solid var(--dh-line)",
                }}
              >
                <div style={{ fontSize: "12px", color: "var(--dh-soft)", fontWeight: "500" }}>{role.roleOrTitle}</div>
                <div style={{ fontSize: "20px", fontWeight: "700", color: "var(--dh-ink)", marginTop: "4px" }}>
                  {role.count} <span style={{ fontSize: "12px", fontWeight: "500", color: "var(--dh-faint)" }}>({role.percentage}%)</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
