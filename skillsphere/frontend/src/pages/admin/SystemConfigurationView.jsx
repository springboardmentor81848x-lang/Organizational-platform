import { useState, useEffect } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getAdminSystemConfigurationOverview,
  updateAdminSystemConfig,
} from "../../services/adminService.js";
import { parseApiError } from "../../services/apiClient.js";

const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "—");

export default function SystemConfigurationView() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Edit Modal State
  const [editingItem, setEditingItem] = useState(null);
  const [editValue, setEditValue] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getAdminSystemConfigurationOverview();
      setData(res);
    } catch (err) {
      setError(parseApiError(err).message || "Failed to load system configuration.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenEdit = (item) => {
    setEditingItem(item);
    setEditValue(item.value || "");
  };

  const handleSaveConfig = async (e) => {
    e.preventDefault();
    if (!editingItem) return;

    try {
      setSubmitting(true);
      setError("");
      setSuccess("");
      await updateAdminSystemConfig(editingItem.key, editValue);
      setSuccess(`Configuration setting '${editingItem.key}' updated successfully.`);
      setEditingItem(null);
      await loadData();
    } catch (err) {
      setError(parseApiError(err).message || "Failed to update configuration setting.");
    } finally {
      setSubmitting(false);
    }
  };

  const gemini = data?.geminiStatus;
  const configs = data?.configurations || [];

  return (
    <div className="adm-view-container">
      {/* View Header */}
      <div className="adm-view-header">
        <div>
          <span className="adm-eyebrow">SYSTEM ENVIRONMENT & OPERATIONAL SETTINGS</span>
          <h1 className="adm-view-title">System Configuration Management</h1>
          <p className="adm-view-subtitle">
            Runtime platform properties, AI engine parameters, session policies, and dynamic application configurations.
          </p>
        </div>
        <div className="adm-view-actions">
          <button
            type="button"
            className="adm-btn adm-btn--ghost"
            onClick={loadData}
            disabled={loading}
          >
            <Icon name="refresh" size={16} /> Refresh Configuration
          </button>
        </div>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError("")} />}
      {success && <Alert type="success" message={success} onClose={() => setSuccess("")} />}

      {/* KPI / Runtime Cards */}
      <div className="adm-kpi-grid">
        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Active Environment</span>
            <div className="adm-kpi-icon adm-kpi-icon--navy">
              <Icon name="server" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value" style={{ textTransform: "capitalize" }}>
            {loading ? "—" : (data?.environment || "Production")}
          </div>
          <p className="adm-kpi-caption">Host Port: {data?.serverPort || 8080}</p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">AI Integration Status</span>
            <div className={`adm-kpi-icon ${gemini?.configured ? "adm-kpi-icon--emerald" : "adm-kpi-icon--amber"}`}>
              <Icon name="zap" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value" style={{ fontSize: "20px" }}>
            {loading ? "—" : (gemini?.status || "OPERATIONAL")}
          </div>
          <p className="adm-kpi-caption">Model: {gemini?.model || "gemini-1.5-flash"}</p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Session Idle Timeout</span>
            <div className="adm-kpi-icon adm-kpi-icon--purple">
              <Icon name="clock" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : `${data?.sessionTimeoutMinutes || 60}m`}</div>
          <p className="adm-kpi-caption">HttpOnly authenticated session lifetime</p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Active Parameters</span>
            <div className="adm-kpi-icon adm-kpi-icon--blue">
              <Icon name="sliders" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : configs.length}</div>
          <p className="adm-kpi-caption">Dynamic system_configuration records</p>
        </div>
      </div>

      {/* Gemini AI Subsystem Card */}
      <div className="adm-card">
        <div className="adm-card-header">
          <div>
            <h2 className="adm-card-title">Google Gemini AI Engine Telemetry</h2>
            <p className="adm-card-subtitle">
              Secure AI integration providing dynamic skill gap assessments and automated recommendations.
            </p>
          </div>
          <div>
            <span className={`adm-badge ${gemini?.configured ? "adm-badge--active" : "adm-badge--warning"}`}>
              {gemini?.status || "READY"}
            </span>
          </div>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "16px", marginTop: "12px" }}>
          <div style={{ padding: "12px 16px", backgroundColor: "#f8fafc", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
            <span style={{ fontSize: "12px", color: "#64748b", fontWeight: 600 }}>TARGET MODEL</span>
            <div style={{ fontSize: "14px", fontWeight: 600, color: "#1e293b", marginTop: "4px" }}>
              {gemini?.model || "gemini-1.5-flash"}
            </div>
          </div>

          <div style={{ padding: "12px 16px", backgroundColor: "#f8fafc", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
            <span style={{ fontSize: "12px", color: "#64748b", fontWeight: 600 }}>API TIMEOUT</span>
            <div style={{ fontSize: "14px", fontWeight: 600, color: "#1e293b", marginTop: "4px" }}>
              {gemini?.timeoutSeconds || 30} seconds
            </div>
          </div>

          <div style={{ padding: "12px 16px", backgroundColor: "#f8fafc", borderRadius: "8px", border: "1px solid #e2e8f0" }}>
            <span style={{ fontSize: "12px", color: "#64748b", fontWeight: 600 }}>API KEY INTEGRITY</span>
            <div style={{ fontSize: "14px", fontFamily: "monospace", color: "#0f766e", marginTop: "4px" }}>
              {gemini?.keyFingerprint || "AIzaSy...*** [MASKED]"}
            </div>
          </div>
        </div>

        <p style={{ margin: "14px 0 0 0", fontSize: "12px", color: "#64748b" }}>
          <Icon name="shield" size={14} style={{ verticalAlign: "middle", marginRight: "4px", color: "#0f766e" }} />
          Zero Secret Exposure Guarantee: Full Gemini API keys are never transmitted over administrative APIs, saved in client state, or rendered in logs.
        </p>
      </div>

      {/* Dynamic Key-Value Configurations */}
      <div className="adm-card">
        <div className="adm-card-header">
          <div>
            <h2 className="adm-card-title">Dynamic Platform Configuration Parameters</h2>
            <p className="adm-card-subtitle">
              Editable system configuration keys stored in PostgreSQL <code className="adm-code">system_configuration</code> table.
            </p>
          </div>
        </div>

        <div className="adm-table-container">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Configuration Key</th>
                <th>Current Value</th>
                <th>Description</th>
                <th>Last Modified</th>
                <th style={{ textAlign: "right" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: "center", padding: "32px" }}>
                    Loading configuration settings...
                  </td>
                </tr>
              ) : configs.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: "center", padding: "32px", color: "#64748b" }}>
                    No dynamic configuration parameters registered.
                  </td>
                </tr>
              ) : (
                configs.map((cfg) => (
                  <tr key={cfg.key}>
                    <td>
                      <code className="adm-code" style={{ fontSize: "13px", fontWeight: 600 }}>
                        {cfg.key}
                      </code>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, color: "#1e293b", fontFamily: "monospace" }}>
                        {cfg.value}
                      </span>
                    </td>
                    <td style={{ color: "#475569", fontSize: "13px" }}>
                      {cfg.description || "Platform operational parameter"}
                    </td>
                    <td style={{ fontSize: "12px", color: "#64748b", whiteSpace: "nowrap" }}>
                      {fmtDate(cfg.updatedAt)}
                    </td>
                    <td style={{ textAlign: "right" }}>
                      <button
                        type="button"
                        className="adm-btn adm-btn--ghost"
                        style={{ padding: "4px 10px", fontSize: "12px" }}
                        onClick={() => handleOpenEdit(cfg)}
                      >
                        <Icon name="edit" size={13} /> Edit
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Edit Configuration Modal */}
      {editingItem && (
        <div className="adm-modal-overlay">
          <div className="adm-modal" style={{ maxWidth: "500px" }}>
            <div className="adm-modal-header">
              <h2 className="adm-modal-title">Edit Configuration Setting</h2>
              <button
                type="button"
                className="adm-btn adm-btn--ghost"
                onClick={() => setEditingItem(null)}
                style={{ padding: "4px 8px" }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveConfig}>
              <div className="adm-modal-body">
                <div className="adm-form-group">
                  <label className="adm-form-label">Configuration Key</label>
                  <input
                    type="text"
                    className="adm-input"
                    value={editingItem.key}
                    disabled
                    style={{ backgroundColor: "#f1f5f9", cursor: "not-allowed" }}
                  />
                </div>

                <div className="adm-form-group">
                  <label className="adm-form-label">Description</label>
                  <p style={{ margin: 0, fontSize: "13px", color: "#64748b" }}>
                    {editingItem.description || "Platform operational parameter"}
                  </p>
                </div>

                <div className="adm-form-group">
                  <label className="adm-form-label">Setting Value *</label>
                  <input
                    type="text"
                    className="adm-input"
                    value={editValue}
                    onChange={(e) => setEditValue(e.target.value)}
                    required
                  />
                  <span className="adm-form-hint">
                    Enter the updated parameter value. Changes take effect immediately in the runtime environment.
                  </span>
                </div>
              </div>

              <div className="adm-modal-footer">
                <button
                  type="button"
                  className="adm-btn adm-btn--ghost"
                  onClick={() => setEditingItem(null)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="adm-btn adm-btn--primary"
                  disabled={submitting}
                >
                  {submitting ? "Saving..." : "Save Setting"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
