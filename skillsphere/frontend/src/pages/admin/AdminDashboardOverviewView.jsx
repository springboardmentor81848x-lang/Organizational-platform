import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getAdminDashboardOverview } from "../../services/adminService.js";

export default function AdminDashboardOverviewView({ onNavigateTab }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getAdminDashboardOverview();
      setData(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load Admin Overview.");
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
        <h4 style={{ marginTop: "12px", color: "var(--adm-soft)" }}>Loading System Administration Overview...</h4>
      </div>
    );
  }

  if (error) {
    return <Alert type="danger" message={error} />;
  }

  const { systemHealth, usersByRole = {}, recentUsers = [], recentSecurityAlerts = [], recentAuditLogs = [] } = data || {};

  return (
    <div>
      {/* Platform KPI Grid */}
      <div className="adm-kpi-grid">
        <div className="adm-kpi-card" onClick={() => onNavigateTab("users")} style={{ cursor: "pointer" }}>
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Platform Accounts</span>
            <span style={{ color: "var(--adm-blue)" }}>
              <Icon name="users" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value">{data?.totalUsers ?? 0}</div>
          <div className="adm-kpi-subtext">
            {data?.activeUsers ?? 0} Active • {data?.inactiveUsers ?? 0} Inactive / Suspended
          </div>
        </div>

        <div className="adm-kpi-card" onClick={() => onNavigateTab("organization")} style={{ cursor: "pointer" }}>
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Organization Structure</span>
            <span style={{ color: "var(--adm-indigo)" }}>
              <Icon name="clipboard" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value">{data?.totalDepartments ?? 0}</div>
          <div className="adm-kpi-subtext">{data?.totalTeams ?? 0} Functional Teams Governed</div>
        </div>

        <div className="adm-kpi-card" onClick={() => onNavigateTab("security")} style={{ cursor: "pointer" }}>
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Security Alerts</span>
            <span style={{ color: recentSecurityAlerts.length > 0 ? "var(--adm-rose)" : "var(--adm-emerald)" }}>
              <Icon name="shield" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value" style={{ color: recentSecurityAlerts.length > 0 ? "var(--adm-rose)" : "inherit" }}>
            {recentSecurityAlerts.length}
          </div>
          <div className="adm-kpi-subtext">Automated access & brute-force monitoring</div>
        </div>

        <div className="adm-kpi-card" onClick={() => onNavigateTab("backup")} style={{ cursor: "pointer" }}>
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Database Health</span>
            <span style={{ color: "var(--adm-emerald)" }}>
              <Icon name="target" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value" style={{ fontSize: "20px", marginTop: "4px" }}>
            {systemHealth?.databaseStatus || "CONNECTED"}
          </div>
          <div className="adm-kpi-subtext">
            PostgreSQL 18.4 • {systemHealth?.activeTablesCount || 56} Tables
          </div>
        </div>

        <div className="adm-kpi-card" onClick={() => onNavigateTab("support")} style={{ cursor: "pointer" }}>
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Open Support Tickets</span>
            <span style={{ color: "var(--adm-amber)" }}>
              <Icon name="help" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value">{data?.openSupportTicketsCount ?? 0}</div>
          <div className="adm-kpi-subtext">Pending administrative triage</div>
        </div>
      </div>

      {/* System Health Status Panel */}
      <div className="adm-panel">
        <div className="adm-panel-header">
          <h3 className="adm-panel-title">
            <Icon name="settings" size={18} />
            Platform Architecture & Integration Health
          </h3>
          <span className="adm-status-badge active">{systemHealth?.status || "HEALTHY"}</span>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "14px", fontSize: "13px" }}>
          <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
            <span style={{ color: "var(--adm-faint)" }}>Database Engine</span>
            <div style={{ fontWeight: "700", marginTop: "2px" }}>{systemHealth?.databaseVersion || "PostgreSQL 18.4"}</div>
          </div>
          <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
            <span style={{ color: "var(--adm-faint)" }}>Flyway Schema</span>
            <div style={{ fontWeight: "700", marginTop: "2px" }}>{systemHealth?.appliedMigrationsCount || 9} Migrations (ddl-auto: validate)</div>
          </div>
          <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
            <span style={{ color: "var(--adm-faint)" }}>Google Gemini AI</span>
            <div style={{ fontWeight: "700", marginTop: "2px", color: systemHealth?.geminiConfigured ? "var(--adm-emerald)" : "var(--adm-amber)" }}>
              {systemHealth?.geminiConfigured ? "✓ Operational" : "Standby (Catalogue Fallback)"}
            </div>
          </div>
          <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
            <span style={{ color: "var(--adm-faint)" }}>JVM Heap Usage</span>
            <div style={{ fontWeight: "700", marginTop: "2px" }}>{systemHealth?.jvmMemoryUsage || "Healthy"}</div>
          </div>
          <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
            <span style={{ color: "var(--adm-faint)" }}>Latest Native Backup</span>
            <div style={{ fontWeight: "700", marginTop: "2px" }}>{systemHealth?.backupStatus || "READY"}</div>
          </div>
        </div>
      </div>

      {/* Two Column Section: Security Alerts & Recent Users */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px" }}>
        {/* Security Alerts */}
        <div className="adm-panel">
          <div className="adm-panel-header">
            <h3 className="adm-panel-title">
              <Icon name="shield" size={18} style={{ color: "var(--adm-rose)" }} />
              Security & Access Alerts
            </h3>
            <button className="adm-btn adm-btn-secondary" style={{ fontSize: "12px" }} onClick={() => onNavigateTab("security")}>
              View Security Center
            </button>
          </div>
          {recentSecurityAlerts.length > 0 ? (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {recentSecurityAlerts.map((alert) => (
                <div
                  key={alert.id}
                  style={{
                    padding: "12px",
                    borderRadius: "8px",
                    background: alert.severity === "HIGH" ? "var(--adm-rose-tint)" : "#f8fafc",
                    border: `1px solid ${alert.severity === "HIGH" ? "#fca5a5" : "var(--adm-line)"}`,
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <span className={`adm-status-badge ${alert.severity.toLowerCase()}`}>{alert.severity}</span>
                    <span style={{ fontSize: "11px", color: "var(--adm-faint)" }}>
                      {alert.timestamp ? new Date(alert.timestamp).toLocaleDateString() : ""}
                    </span>
                  </div>
                  <div style={{ fontWeight: "700", fontSize: "13.5px", marginTop: "6px", color: "var(--adm-ink)" }}>
                    {alert.eventCategory.replace(/_/g, " ")}: {alert.affectedAccount}
                  </div>
                  <div style={{ fontSize: "12px", color: "var(--adm-soft)", marginTop: "2px" }}>
                    {alert.supportingEvidence}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: "var(--adm-faint)", fontSize: "13px" }}>Zero active security alerts detected.</p>
          )}
        </div>

        {/* Recently Registered Accounts */}
        <div className="adm-panel">
          <div className="adm-panel-header">
            <h3 className="adm-panel-title">
              <Icon name="users" size={18} />
              Recent Account Registrations
            </h3>
            <button className="adm-btn adm-btn-secondary" style={{ fontSize: "12px" }} onClick={() => onNavigateTab("users")}>
              Manage Accounts
            </button>
          </div>
          {recentUsers.length > 0 ? (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {recentUsers.map((u) => (
                <div
                  key={u.id}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "8px 10px",
                    borderBottom: "1px solid #f1f5f9",
                  }}
                >
                  <div>
                    <div style={{ fontWeight: "600", fontSize: "13px", color: "var(--adm-ink)" }}>{u.fullName}</div>
                    <div style={{ fontSize: "11.5px", color: "var(--adm-faint)" }}>
                      {u.email} • {u.department || "No Department"}
                    </div>
                  </div>
                  <span className="adm-badge" style={{ fontSize: "10.5px" }}>
                    {u.role}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ color: "var(--adm-faint)", fontSize: "13px" }}>No recent user records.</p>
          )}
        </div>
      </div>
    </div>
  );
}
