import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getAdminCommunicationSettings,
  updateAdminCommunicationSettings,
} from "../../services/adminService.js";

export default function ChatCommunicationSettingsView() {
  const [settings, setSettings] = useState(null);
  const [maxUploadSize, setMaxUploadSize] = useState(10);
  const [allowedTypes, setAllowedTypes] = useState("pdf,png,jpg,jpeg,doc,docx,zip");
  const [retentionDays, setRetentionDays] = useState(365);
  const [messagingEnabled, setMessagingEnabled] = useState(true);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadSettings = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getAdminCommunicationSettings();
      setSettings(res);
      setMaxUploadSize(res.maxUploadSizeMb);
      setAllowedTypes(res.allowedAttachmentTypes);
      setRetentionDays(res.chatRetentionDays);
      setMessagingEnabled(res.messagingEnabled);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load communication settings.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSettings();
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    try {
      setSaving(true);
      setError("");
      const updated = await updateAdminCommunicationSettings({
        maxUploadSizeMb: Number(maxUploadSize),
        allowedAttachmentTypes: allowedTypes.trim(),
        chatRetentionDays: Number(retentionDays),
        messagingEnabled,
      });

      setSettings(updated);
      setSuccess("Communication policy settings updated and applied successfully.");
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to update settings.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      {/* Privacy Guarantee Alert */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "14px",
          padding: "16px 20px",
          borderRadius: "8px",
          background: "var(--adm-primary-tint)",
          border: "1px solid rgba(37, 99, 235, 0.25)",
          marginBottom: "20px",
        }}
      >
        <span style={{ color: "var(--adm-blue)" }}>
          <Icon name="shield" size={24} />
        </span>
        <div style={{ fontSize: "13.5px", color: "var(--adm-primary-dark)", lineHeight: "1.5" }}>
          <strong>Least-Privilege Privacy Guarantee:</strong> Administrative governance controls system-wide attachment limits, file extensions, and retention rules. Private 1-on-1 employee conversations, direct message texts, and mentor preparation notes are strictly confidential and inaccessible via administrative endpoints.
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}
      {success && <Alert type="success" message={success} />}

      {/* Aggregate Activity KPI Grid */}
      <div className="adm-kpi-grid">
        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Active Conversations</span>
            <span style={{ color: "var(--adm-blue)" }}>
              <Icon name="message" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value">{settings?.totalConversationsCount ?? 0}</div>
          <div className="adm-kpi-subtext">Aggregate direct dialogues</div>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Dispatched Messages</span>
            <span style={{ color: "var(--adm-indigo)" }}>
              <Icon name="send" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value">{settings?.totalMessagesCount ?? 0}</div>
          <div className="adm-kpi-subtext">Cumulative message volume</div>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Messaging Service Status</span>
            <span style={{ color: messagingEnabled ? "var(--adm-emerald)" : "var(--adm-rose)" }}>
              <Icon name="check" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value" style={{ fontSize: "20px", marginTop: "4px" }}>
            {messagingEnabled ? "ONLINE" : "DISABLED"}
          </div>
          <div className="adm-kpi-subtext">Platform direct chat service</div>
        </div>
      </div>

      {/* Policy Form */}
      <div className="adm-panel">
        <div className="adm-panel-header">
          <h3 className="adm-panel-title">
            <Icon name="settings" size={18} />
            System Communication & Attachment Security Policies
          </h3>
        </div>

        {loading ? (
          <div style={{ textAlign: "center", padding: "40px 20px" }}>
            <Icon name="refresh" className="spin" size={32} />
            <h4 style={{ marginTop: "12px", color: "var(--adm-soft)" }}>Loading Settings...</h4>
          </div>
        ) : (
          <form onSubmit={handleSave}>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px", marginBottom: "16px" }}>
              <div>
                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "6px" }}>
                  Maximum Upload Size per File (MB)
                </label>
                <input
                  type="number"
                  className="adm-input"
                  style={{ width: "100%" }}
                  min={1}
                  max={100}
                  value={maxUploadSize}
                  onChange={(e) => setMaxUploadSize(e.target.value)}
                  required
                />
                <span style={{ fontSize: "12px", color: "var(--adm-faint)" }}>
                  Maximum file payload allowed for skill evidence and attachments.
                </span>
              </div>

              <div>
                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "6px" }}>
                  Chat History Retention Period (Days)
                </label>
                <input
                  type="number"
                  className="adm-input"
                  style={{ width: "100%" }}
                  min={30}
                  max={1825}
                  value={retentionDays}
                  onChange={(e) => setRetentionDays(e.target.value)}
                  required
                />
                <span style={{ fontSize: "12px", color: "var(--adm-faint)" }}>
                  Days before automated archival of inactive conversation threads.
                </span>
              </div>
            </div>

            <div style={{ marginBottom: "20px" }}>
              <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "6px" }}>
                Allowed Attachment Extensions (Comma-separated)
              </label>
              <input
                type="text"
                className="adm-input"
                style={{ width: "100%" }}
                value={allowedTypes}
                onChange={(e) => setAllowedTypes(e.target.value)}
                required
              />
              <span style={{ fontSize: "12px", color: "var(--adm-faint)" }}>
                Strict whitelist. Executable files (.exe, .bat, .sh, .cmd) are automatically rejected by server security.
              </span>
            </div>

            <div style={{ marginBottom: "24px", padding: "14px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "10px", cursor: "pointer", fontSize: "13.5px", fontWeight: "600" }}>
                <input
                  type="checkbox"
                  checked={messagingEnabled}
                  onChange={(e) => setMessagingEnabled(e.target.checked)}
                />
                Enable Platform Direct Messaging
              </label>
              <div style={{ fontSize: "12px", color: "var(--adm-soft)", marginTop: "4px", marginLeft: "24px" }}>
                When unchecked, user-to-user chat is paused across the application for maintenance.
              </div>
            </div>

            <button type="submit" className="adm-btn adm-btn-primary" disabled={saving}>
              {saving ? "Saving Changes..." : "Apply Communication Policy"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
