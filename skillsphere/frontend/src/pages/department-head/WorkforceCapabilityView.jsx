import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getWorkforceCapability } from "../../services/departmentHeadService.js";

export default function WorkforceCapabilityView({ onNavigateTab }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadCapability = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getWorkforceCapability();
      setProfile(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load Workforce Capability profile.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCapability();
  }, []);

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "60px 20px" }}>
        <Icon name="refresh" className="spin" size={32} />
        <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Synthesizing Workforce Capability Profile...</h4>
      </div>
    );
  }

  if (error) {
    return <Alert type="danger" message={error} />;
  }

  return (
    <div>
      {/* Capability KPI Cards */}
      <div className="dh-kpi-grid">
        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Department Readiness</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-primary)" }}>
              <Icon name="award" size={20} />
            </span>
          </div>
          <div className="dh-kpi-value">{profile?.readinessPercentage ?? 0}%</div>
          <div className="dh-kpi-subtext">Composite strategic workforce benchmark</div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Assessment Verification</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-emerald)" }}>
              <Icon name="check" size={20} />
            </span>
          </div>
          <div className="dh-kpi-value">{profile?.assessmentCoveragePercentage ?? 0}%</div>
          <div className="dh-kpi-subtext">Workforce with completed baseline evaluations</div>
        </div>

        <div className="dh-kpi-card">
          <div className="dh-kpi-header">
            <span className="dh-kpi-label">Identified Shortages</span>
            <span className="dh-kpi-icon" style={{ color: "var(--dh-rose)" }}>
              <Icon name="alert" size={20} />
            </span>
          </div>
          <div className="dh-kpi-value" style={{ color: "var(--dh-rose)" }}>
            {profile?.criticalShortages?.length ?? 0}
          </div>
          <div className="dh-kpi-subtext">Emerging competencies requiring intervention</div>
        </div>
      </div>

      {/* Two Columns: Strongest vs Weakest Skills */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px", marginBottom: "24px" }}>
        {/* Strongest Skills */}
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="trending" size={18} style={{ color: "var(--dh-emerald)" }} />
              Top Core Strengths
            </h3>
          </div>
          {profile?.strongestSkills && profile.strongestSkills.length > 0 ? (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {profile.strongestSkills.map((s) => (
                <div
                  key={s.skillId}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "10px 14px",
                    borderRadius: "8px",
                    background: "var(--dh-emerald-tint)",
                    border: "1px solid #a7f3d0",
                  }}
                >
                  <div>
                    <strong style={{ fontSize: "14px", color: "var(--dh-ink)" }}>{s.skillName}</strong>
                    <div style={{ fontSize: "12px", color: "var(--dh-soft)" }}>
                      {s.categoryName} • {s.verifiedCount} verified engineers
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <span className="dh-status-badge low">Level {s.avgProficiency}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: "var(--dh-faint)", fontSize: "13.5px" }}>No proficiency records available.</p>
          )}
        </div>

        {/* Weakest Skills */}
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="alert" size={18} style={{ color: "var(--dh-rose)" }} />
              Competencies Requiring Support
            </h3>
          </div>
          {profile?.weakestSkills && profile.weakestSkills.length > 0 ? (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {profile.weakestSkills.map((s) => (
                <div
                  key={s.skillId}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "10px 14px",
                    borderRadius: "8px",
                    background: "var(--dh-rose-tint)",
                    border: "1px solid #fca5a5",
                  }}
                >
                  <div>
                    <strong style={{ fontSize: "14px", color: "var(--dh-ink)" }}>{s.skillName}</strong>
                    <div style={{ fontSize: "12px", color: "var(--dh-soft)" }}>
                      {s.categoryName} • {s.verifiedCount} evaluated
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <span className="dh-status-badge critical">Level {s.avgProficiency}</span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: "var(--dh-faint)", fontSize: "13.5px" }}>No vulnerability data recorded.</p>
          )}
        </div>
      </div>

      {/* Critical Shortages Table */}
      <div className="dh-panel">
        <div className="dh-panel-header">
          <h3 className="dh-panel-title">
            <Icon name="shield" size={18} />
            Critical Capability Shortages & Strategic Action Plan
          </h3>
          <button className="dh-btn dh-btn-primary" onClick={() => onNavigateTab("reports")}>
            Export Analytics
          </button>
        </div>

        <div className="dh-table-container">
          <table className="dh-table">
            <thead>
              <tr>
                <th>Competency</th>
                <th>Category</th>
                <th>Affected Teams</th>
                <th>Affected Personnel</th>
                <th>Current vs Target Level</th>
                <th>Average Net Gap</th>
                <th>Recommended Action</th>
              </tr>
            </thead>
            <tbody>
              {profile?.criticalShortages && profile.criticalShortages.length > 0 ? (
                profile.criticalShortages.map((item) => (
                  <tr key={item.skillId}>
                    <td>
                      <strong>{item.skillName}</strong>
                    </td>
                    <td>
                      <span className="dh-badge">{item.categoryName}</span>
                    </td>
                    <td>{item.affectedTeamsCount} Teams</td>
                    <td>{item.affectedEmployeesCount} Employees</td>
                    <td>
                      Lvl {item.avgProficiency} / Target Lvl {item.targetProficiency}
                    </td>
                    <td>
                      <span className="dh-status-badge critical">-{item.avgGap}</span>
                    </td>
                    <td style={{ fontSize: "12.5px", color: "var(--dh-soft)" }}>
                      {item.recommendedAction}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", color: "var(--dh-emerald)", padding: "24px" }}>
                    ✓ No critical shortages identified across current department skill models.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Strategic Recommendations */}
      {profile?.priorityRecommendations && profile.priorityRecommendations.length > 0 && (
        <div className="dh-panel">
          <div className="dh-panel-header">
            <h3 className="dh-panel-title">
              <Icon name="lightbulb" size={18} />
              Strategic Leadership Priorities
            </h3>
          </div>
          <ul style={{ margin: 0, paddingLeft: "20px", display: "flex", flexDirection: "column", gap: "8px" }}>
            {profile.priorityRecommendations.map((rec, i) => (
              <li key={i} style={{ fontSize: "14px", color: "var(--dh-soft)", lineHeight: "1.5" }}>
                {rec}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
