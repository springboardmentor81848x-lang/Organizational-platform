import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getDepartmentRiskAlerts } from "../../services/departmentHeadService.js";

export default function RiskPriorityAlertsView() {
  const [alerts, setAlerts] = useState([]);
  const [severityFilter, setSeverityFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadAlerts = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getDepartmentRiskAlerts();
      setAlerts(res || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load Risk & Priority Alerts.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAlerts();
  }, []);

  const filteredAlerts = alerts.filter((a) => {
    if (severityFilter === "ALL") return true;
    return a.severity.toUpperCase() === severityFilter.toUpperCase();
  });

  return (
    <div>
      <div className="dh-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="dh-panel-title" style={{ margin: 0 }}>
              <Icon name="alert" size={18} />
              Strategic Risk & Governance Priority Register
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--dh-soft)" }}>
              Continuous automated detection of workforce skill deficits, low assessment coverage, and team lead vacancies.
            </p>
          </div>

          <div style={{ display: "flex", gap: "8px" }}>
            {["ALL", "CRITICAL", "HIGH", "MEDIUM"].map((sev) => (
              <button
                key={sev}
                className={`dh-btn ${severityFilter === sev ? "dh-btn-primary" : "dh-btn-secondary"}`}
                onClick={() => setSeverityFilter(sev)}
                style={{ fontSize: "12px", padding: "6px 12px" }}
              >
                {sev}
              </button>
            ))}
          </div>
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}

      {loading ? (
        <div style={{ textAlign: "center", padding: "60px 20px" }}>
          <Icon name="refresh" className="spin" size={32} />
          <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Auditing Department Governance & Skill Risks...</h4>
        </div>
      ) : filteredAlerts.length === 0 ? (
        <div className="dh-panel" style={{ textAlign: "center", padding: "40px 20px" }}>
          <Icon name="check" size={32} style={{ color: "var(--dh-emerald)" }} />
          <h4 style={{ margin: "12px 0 6px", color: "var(--dh-ink)" }}>No Active Risks Detected</h4>
          <p style={{ margin: 0, color: "var(--dh-soft)", fontSize: "13.5px" }}>
            All functional teams meet governance thresholds and minimum competency requirements.
          </p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {filteredAlerts.map((item) => (
            <div
              key={item.id}
              className="dh-panel"
              style={{
                marginBottom: 0,
                borderLeft: `4px solid ${
                  item.severity === "CRITICAL"
                    ? "var(--dh-rose)"
                    : item.severity === "HIGH"
                    ? "var(--dh-amber)"
                    : "var(--dh-accent)"
                }`,
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "12px" }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                    <span className={`dh-status-badge ${item.severity.toLowerCase()}`}>{item.severity}</span>
                    <span className="dh-badge">{item.alertType}</span>
                    <strong style={{ fontSize: "16px", color: "var(--dh-ink)" }}>{item.title}</strong>
                  </div>
                  <p style={{ margin: "8px 0 10px", fontSize: "14px", color: "var(--dh-soft)" }}>
                    {item.description}
                  </p>
                </div>
                <div style={{ textAlign: "right", flexShrink: 0 }}>
                  <span className="dh-status-badge low">{item.status}</span>
                  <div style={{ fontSize: "11px", color: "var(--dh-faint)", marginTop: "4px" }}>
                    {item.dateIdentified ? new Date(item.dateIdentified).toLocaleDateString() : "Active"}
                  </div>
                </div>
              </div>

              {/* Detail Pills */}
              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
                  gap: "10px",
                  padding: "12px",
                  borderRadius: "8px",
                  background: "#f8fafc",
                  marginTop: "8px",
                  fontSize: "12.5px",
                }}
              >
                <div>
                  <span style={{ color: "var(--dh-faint)" }}>Affected Scope:</span>{" "}
                  <strong>{item.affectedTeamName}</strong> ({item.affectedSkillName})
                </div>
                <div>
                  <span style={{ color: "var(--dh-faint)" }}>Evidence Metric:</span>{" "}
                  <span style={{ color: "var(--dh-ink)" }}>{item.evidenceSummary}</span>
                </div>
                <div>
                  <span style={{ color: "var(--dh-faint)" }}>Responsible Role:</span>{" "}
                  <span className="dh-badge" style={{ fontSize: "11px" }}>
                    {item.responsibleRole}
                  </span>
                </div>
              </div>

              {/* Action Directive */}
              <div style={{ marginTop: "12px", display: "flex", alignItems: "center", gap: "8px" }}>
                <span style={{ color: "var(--dh-primary)", fontWeight: "600", fontSize: "13px" }}>
                  Action Directive:
                </span>
                <span style={{ fontSize: "13px", color: "var(--dh-ink)" }}>{item.recommendedAction}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
