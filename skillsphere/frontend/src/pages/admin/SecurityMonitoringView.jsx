import { useState, useEffect } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getAdminSecurityOverview } from "../../services/adminService.js";
import { parseApiError } from "../../services/apiClient.js";

const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "—");

export default function SecurityMonitoringView() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [severityFilter, setSeverityFilter] = useState("ALL");
  const [searchTerm, setSearchTerm] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getAdminSecurityOverview();
      setData(res);
    } catch (err) {
      setError(parseApiError(err).message || "Failed to load security overview.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const lockedAccounts = data?.currentlyLockedAccounts || [];
  const rawAlerts = data?.securityAlerts || [];

  const filteredAlerts = rawAlerts.filter((a) => {
    if (severityFilter !== "ALL" && a.severity !== severityFilter) return false;
    if (searchTerm.trim()) {
      const q = searchTerm.toLowerCase();
      const matchCat = a.eventCategory?.toLowerCase().includes(q);
      const matchAcc = a.affectedAccount?.toLowerCase().includes(q);
      const matchEv = a.supportingEvidence?.toLowerCase().includes(q);
      if (!matchCat && !matchAcc && !matchEv) return false;
    }
    return true;
  });

  const getSeverityBadgeClass = (sev) => {
    switch (sev) {
      case "CRITICAL": return "adm-badge--danger";
      case "HIGH": return "adm-badge--warning";
      case "MEDIUM": return "adm-badge--dept";
      case "LOW": return "adm-badge--info";
      default: return "adm-badge--inactive";
    }
  };

  return (
    <div className="adm-view-container">
      {/* View Header */}
      <div className="adm-view-header">
        <div>
          <span className="adm-eyebrow">SECURITY GOVERNANCE & TELEMETRY</span>
          <h1 className="adm-view-title">Security Monitoring & Threat Detection</h1>
          <p className="adm-view-subtitle">
            Real-time brute force defense, locked account tracking, privilege auditing, and automated incident indicators.
          </p>
        </div>
        <div className="adm-view-actions">
          <button
            type="button"
            className="adm-btn adm-btn--ghost"
            onClick={loadData}
            disabled={loading}
          >
            <Icon name="refresh" size={16} /> Refresh Telemetry
          </button>
        </div>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError("")} />}

      {/* KPI Metric Cards */}
      <div className="adm-kpi-grid">
        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Currently Locked Accounts</span>
            <div className={`adm-kpi-icon ${lockedAccounts.length > 0 ? "adm-kpi-icon--amber" : "adm-kpi-icon--emerald"}`}>
              <Icon name="lock" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : lockedAccounts.length}</div>
          <p className="adm-kpi-caption">
            {lockedAccounts.length > 0 ? "Active brute force lockouts" : "No locked accounts"}
          </p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Recent Failed Attempts</span>
            <div className="adm-kpi-icon adm-kpi-icon--amber">
              <Icon name="alert-triangle" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : (data?.recentFailedAttemptsCount ?? 0)}</div>
          <p className="adm-kpi-caption">Within active lockout evaluation window</p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Audit Logs (Last 24h)</span>
            <div className="adm-kpi-icon adm-kpi-icon--navy">
              <Icon name="activity" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : (data?.totalAuditLogsLast24h ?? 0)}</div>
          <p className="adm-kpi-caption">Platform security & operational events</p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Privilege Modifications</span>
            <div className="adm-kpi-icon adm-kpi-icon--purple">
              <Icon name="shield" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : (data?.privilegeChangesCount ?? 0)}</div>
          <p className="adm-kpi-caption">Role grants, status changes, and admin acts</p>
        </div>
      </div>

      {/* Locked Accounts Banner */}
      {lockedAccounts.length > 0 ? (
        <div className="adm-card" style={{ borderLeft: "4px solid #dc2626" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "12px" }}>
            <Icon name="alert-circle" size={20} style={{ color: "#dc2626" }} />
            <h3 style={{ margin: 0, fontSize: "16px", color: "#991b1b" }}>
              Accounts Currently Locked Under Brute-Force Rate Limiting
            </h3>
          </div>
          <p style={{ margin: "0 0 12px 0", fontSize: "13px", color: "#475569" }}>
            The following accounts have exceeded max consecutive failed authentication attempts. Requests from these identities are temporarily throttled:
          </p>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
            {lockedAccounts.map((acc) => (
              <span key={acc} className="adm-badge adm-badge--danger" style={{ fontSize: "13px", padding: "6px 12px" }}>
                <Icon name="lock" size={12} /> {acc}
              </span>
            ))}
          </div>
        </div>
      ) : (
        <div className="adm-card" style={{ borderLeft: "4px solid #10b981", padding: "16px 20px" }}>
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <Icon name="check-circle" size={18} style={{ color: "#10b981" }} />
            <div>
              <span style={{ fontWeight: 600, fontSize: "14px", color: "#065f46" }}>Authentication Gate Clear</span>
              <p style={{ margin: "2px 0 0 0", fontSize: "12px", color: "#047857" }}>
                Zero user accounts are currently locked under brute force mitigation controls. Normal login flow operational.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Security Alerts Log Panel */}
      <div className="adm-card">
        <div className="adm-card-header">
          <div>
            <h2 className="adm-card-title">Security & Integrity Event Log</h2>
            <p className="adm-card-subtitle">
              Dynamic incident detection synthesized from security audit records and threat heuristics.
            </p>
          </div>
          <div className="adm-filter-bar" style={{ margin: 0 }}>
            <div className="adm-search-box" style={{ maxWidth: "260px" }}>
              <Icon name="search" size={16} />
              <input
                type="text"
                placeholder="Search alerts or account..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <select
              className="adm-filter-select"
              value={severityFilter}
              onChange={(e) => setSeverityFilter(e.target.value)}
            >
              <option value="ALL">All Severities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
              <option value="INFO">Info</option>
            </select>
          </div>
        </div>

        <div className="adm-table-container">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Severity</th>
                <th>Category</th>
                <th>Timestamp</th>
                <th>Target / Account</th>
                <th>Supporting Evidence</th>
                <th>Recommended Action</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "32px" }}>
                    Loading security telemetry...
                  </td>
                </tr>
              ) : filteredAlerts.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "32px", color: "#64748b" }}>
                    No security alerts matching current criteria.
                  </td>
                </tr>
              ) : (
                filteredAlerts.map((al) => (
                  <tr key={al.id}>
                    <td>
                      <span className={`adm-badge ${getSeverityBadgeClass(al.severity)}`}>
                        {al.severity}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, color: "#1e293b", fontSize: "13px" }}>
                        {al.eventCategory}
                      </span>
                    </td>
                    <td style={{ whiteSpace: "nowrap", color: "#64748b", fontSize: "12px" }}>
                      {fmtDate(al.timestamp)}
                    </td>
                    <td style={{ fontWeight: 500, color: "#0f172a" }}>
                      {al.affectedAccount || "SYSTEM"}
                    </td>
                    <td style={{ maxWidth: "280px", fontSize: "12px", color: "#475569" }}>
                      {al.supportingEvidence}
                    </td>
                    <td style={{ maxWidth: "260px", fontSize: "12px", color: "#0369a1" }}>
                      {al.recommendedResponse}
                    </td>
                    <td>
                      <span className="adm-badge adm-badge--active">
                        {al.status || "ACTIVE"}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Threat Mitigation Architecture Standards Panel */}
      <div className="adm-card" style={{ backgroundColor: "#f8fafc", border: "1px solid #e2e8f0" }}>
        <h3 style={{ margin: "0 0 12px 0", fontSize: "15px", color: "#1e293b" }}>
          Platform Hardening & Security Standards
        </h3>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))", gap: "16px" }}>
          <div style={{ display: "flex", gap: "12px", alignItems: "flex-start" }}>
            <Icon name="shield" size={18} style={{ color: "#0f766e", marginTop: "2px" }} />
            <div>
              <strong style={{ fontSize: "13px", color: "#0f172a" }}>Credential Protection:</strong>
              <p style={{ margin: "2px 0 0 0", fontSize: "12px", color: "#64748b" }}>
                Bcrypt password hashing with salt. Passwords and reset hashes are never exposed via APIs or rendered in views.
              </p>
            </div>
          </div>

          <div style={{ display: "flex", gap: "12px", alignItems: "flex-start" }}>
            <Icon name="lock" size={18} style={{ color: "#0f766e", marginTop: "2px" }} />
            <div>
              <strong style={{ fontSize: "13px", color: "#0f172a" }}>Transport & Session Security:</strong>
              <p style={{ margin: "2px 0 0 0", fontSize: "12px", color: "#64748b" }}>
                HttpOnly session cookies with SameSite controls, strict CSRF validation on all state-mutating requests.
              </p>
            </div>
          </div>

          <div style={{ display: "flex", gap: "12px", alignItems: "flex-start" }}>
            <Icon name="eye-off" size={18} style={{ color: "#0f766e", marginTop: "2px" }} />
            <div>
              <strong style={{ fontSize: "13px", color: "#0f172a" }}>Communication Confidentiality:</strong>
              <p style={{ margin: "2px 0 0 0", fontSize: "12px", color: "#64748b" }}>
                Administrative least-privilege boundary strictly enforced. Zero administrative access to private employee messages or mentor notes.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
