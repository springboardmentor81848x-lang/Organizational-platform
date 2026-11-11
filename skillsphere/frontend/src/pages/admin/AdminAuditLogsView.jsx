import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getAdminAuditLogs } from "../../services/adminService.js";

export default function AdminAuditLogsView() {
  const [logs, setLogs] = useState([]);
  const [search, setSearch] = useState("");
  const [actionFilter, setActionFilter] = useState("");
  const [entityFilter, setEntityFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedLog, setSelectedLog] = useState(null);

  const loadLogs = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getAdminAuditLogs();
      setLogs(res || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load audit logs.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, []);

  const filteredLogs = logs.filter((l) => {
    if (actionFilter && l.action !== actionFilter) return false;
    if (entityFilter && l.entityName !== entityFilter) return false;
    if (search) {
      const q = search.toLowerCase();
      return (
        (l.userName && l.userName.toLowerCase().contains(q)) ||
        (l.action && l.action.toLowerCase().contains(q)) ||
        (l.entityName && l.entityName.toLowerCase().contains(q)) ||
        (l.entityId && l.entityId.toLowerCase().contains(q)) ||
        (l.details && l.details.toLowerCase().contains(q)) ||
        (l.ipAddress && l.ipAddress.contains(q))
      );
    }
    return true;
  });

  const uniqueActions = [...new Set(logs.map((l) => l.action).filter(Boolean))];
  const uniqueEntities = [...new Set(logs.map((l) => l.entityName).filter(Boolean))];

  return (
    <div>
      <div className="adm-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="adm-panel-title" style={{ margin: 0 }}>
              <Icon name="file" size={18} />
              Platform Security & Administrative Audit Trail
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--adm-soft)" }}>
              Immutable server-side record of administrative mutations, role escalations, and system configuration updates.
            </p>
          </div>

          <button className="adm-btn adm-btn-secondary" onClick={loadLogs} disabled={loading}>
            <Icon name="refresh" size={14} className={loading ? "spin" : ""} />
            Refresh Trail
          </button>
        </div>

        {/* Filters */}
        <div style={{ display: "flex", gap: "10px", marginTop: "16px", flexWrap: "wrap", alignItems: "center" }}>
          <input
            type="text"
            className="adm-input"
            style={{ flex: 1, minWidth: "220px" }}
            placeholder="Search by actor, entity ID, IP, or details..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <select className="adm-select" value={actionFilter} onChange={(e) => setActionFilter(e.target.value)}>
            <option value="">All Action Types ({uniqueActions.length})</option>
            {uniqueActions.map((a) => (
              <option key={a} value={a}>
                {a}
              </option>
            ))}
          </select>

          <select className="adm-select" value={entityFilter} onChange={(e) => setEntityFilter(e.target.value)}>
            <option value="">All Target Entities ({uniqueEntities.length})</option>
            {uniqueEntities.map((ent) => (
              <option key={ent} value={ent}>
                {ent}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}

      {/* Audit Log Table */}
      <div className="adm-panel">
        {loading ? (
          <div style={{ textAlign: "center", padding: "60px 20px" }}>
            <Icon name="refresh" className="spin" size={32} />
            <h4 style={{ marginTop: "12px", color: "var(--adm-soft)" }}>Retrieving Audit Trail...</h4>
          </div>
        ) : (
          <div className="adm-table-container">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Event ID</th>
                  <th>Timestamp</th>
                  <th>Acting Personnel</th>
                  <th>Action Type</th>
                  <th>Target Entity</th>
                  <th>Origin IP</th>
                  <th>Audit Details</th>
                </tr>
              </thead>
              <tbody>
                {filteredLogs.length > 0 ? (
                  filteredLogs.map((l) => (
                    <tr
                      key={l.id}
                      onClick={() => setSelectedLog(l)}
                      style={{ cursor: "pointer" }}
                      title="Click to view complete event JSON"
                    >
                      <td>#{l.id}</td>
                      <td style={{ fontSize: "12px", color: "var(--adm-faint)", whiteSpace: "nowrap" }}>
                        {l.createdAt ? new Date(l.createdAt).toLocaleString() : ""}
                      </td>
                      <td style={{ fontWeight: "600", color: "var(--adm-ink)" }}>{l.userName || "System"}</td>
                      <td>
                        <span className="adm-badge" style={{ fontSize: "10.5px" }}>
                          {l.action}
                        </span>
                      </td>
                      <td>
                        <span style={{ fontWeight: "600", fontSize: "12px" }}>{l.entityName}</span>
                        {l.entityId && (
                          <span style={{ color: "var(--adm-faint)", fontSize: "11px", marginLeft: "4px" }}>
                            ({l.entityId})
                          </span>
                        )}
                      </td>
                      <td style={{ fontSize: "12px", fontFamily: "monospace" }}>{l.ipAddress || "—"}</td>
                      <td style={{ fontSize: "12.5px", color: "var(--adm-soft)", maxWidth: "340px" }}>
                        {l.details || "—"}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} style={{ textAlign: "center", color: "var(--adm-faint)", padding: "24px" }}>
                      No audit records matching the specified filter criteria.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Audit Detail Modal */}
      {selectedLog && (
        <div className="adm-modal-overlay" onClick={() => setSelectedLog(null)}>
          <div className="adm-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px" }}>Audit Event #{selectedLog.id}</h3>
              <button style={{ background: "none", border: "none", cursor: "pointer" }} onClick={() => setSelectedLog(null)}>
                <Icon name="x" size={20} />
              </button>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", marginBottom: "16px" }}>
              <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
                <span style={{ fontSize: "11px", color: "var(--adm-faint)", textTransform: "uppercase" }}>Acting User</span>
                <div style={{ fontWeight: "700", marginTop: "2px" }}>{selectedLog.userName}</div>
              </div>
              <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
                <span style={{ fontSize: "11px", color: "var(--adm-faint)", textTransform: "uppercase" }}>Timestamp</span>
                <div style={{ fontWeight: "700", marginTop: "2px" }}>
                  {selectedLog.createdAt ? new Date(selectedLog.createdAt).toLocaleString() : "—"}
                </div>
              </div>
            </div>

            <div style={{ marginBottom: "16px" }}>
              <span style={{ fontSize: "11px", color: "var(--adm-faint)", textTransform: "uppercase" }}>Action Type & Target</span>
              <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "4px" }}>
                <span className="adm-badge">{selectedLog.action}</span>
                <span style={{ fontWeight: "600" }}>{selectedLog.entityName}</span>
                <span style={{ color: "var(--adm-faint)" }}>ID: {selectedLog.entityId || "N/A"}</span>
              </div>
            </div>

            <div style={{ marginBottom: "20px" }}>
              <span style={{ fontSize: "11px", color: "var(--adm-faint)", textTransform: "uppercase" }}>Details & Parameters</span>
              <div style={{ padding: "12px", background: "#f8fafc", borderRadius: "8px", marginTop: "4px", fontSize: "13px", color: "var(--adm-ink)", border: "1px solid var(--adm-line)" }}>
                {selectedLog.details}
              </div>
            </div>

            <div style={{ textAlign: "right" }}>
              <button className="adm-btn adm-btn-primary" onClick={() => setSelectedLog(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
