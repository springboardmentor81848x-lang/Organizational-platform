import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getAdminNotificationSettings,
  updateAdminNotificationSettings,
} from "../../services/adminService.js";

export default function NotificationSettingsView() {
  const [settings, setSettings] = useState(null);
  const [inAppEnabled, setInAppEnabled] = useState(true);
  const [defaultPriority, setDefaultPriority] = useState("NORMAL");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadSettings = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getAdminNotificationSettings();
      setSettings(res);
      setInAppEnabled(res.inAppNotificationsEnabled);
      setDefaultPriority(res.defaultPriority || "NORMAL");
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load notification settings.");
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
      const updated = await updateAdminNotificationSettings({
        inAppNotificationsEnabled: inAppEnabled,
        defaultPriority,
      });

      setSettings(updated);
      setSuccess("Notification policies updated and applied successfully.");
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to update notification settings.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div className="adm-panel">
        <h3 className="adm-panel-title">
          <Icon name="bell" size={18} />
          System Notification Architecture & Delivery Controls
        </h3>
        <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--adm-soft)" }}>
          Configure enterprise notification priority policies, supported categories, and delivery telemetry.
        </p>
      </div>

      {error && <Alert type="danger" message={error} />}
      {success && <Alert type="success" message={success} />}

      {/* Telemetry KPI Cards */}
      <div className="adm-kpi-grid">
        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Delivered In-App</span>
            <span style={{ color: "var(--adm-blue)" }}>
              <Icon name="bell" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value">{settings?.totalNotificationsDelivered ?? 0}</div>
          <div className="adm-kpi-subtext">Total platform notifications logged</div>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Pending / Unread</span>
            <span style={{ color: "var(--adm-amber)" }}>
              <Icon name="clock" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value">{settings?.unreadNotificationsCount ?? 0}</div>
          <div className="adm-kpi-subtext">Awaiting user acknowledgement</div>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Service Channel Status</span>
            <span style={{ color: inAppEnabled ? "var(--adm-emerald)" : "var(--adm-rose)" }}>
              <Icon name="check" size={20} />
            </span>
          </div>
          <div className="adm-kpi-value" style={{ fontSize: "20px", marginTop: "4px" }}>
            {inAppEnabled ? "ACTIVE" : "PAUSED"}
          </div>
          <div className="adm-kpi-subtext">In-app delivery channel</div>
        </div>
      </div>

      {/* Policy Configuration Form */}
      <div className="adm-panel">
        <div className="adm-panel-header">
          <h3 className="adm-panel-title">
            <Icon name="settings" size={18} />
            Notification Delivery Parameters
          </h3>
        </div>

        {loading ? (
          <div style={{ textAlign: "center", padding: "40px 20px" }}>
            <Icon name="refresh" className="spin" size={32} />
            <h4 style={{ marginTop: "12px", color: "var(--adm-soft)" }}>Loading Settings...</h4>
          </div>
        ) : (
          <form onSubmit={handleSave}>
            <div style={{ marginBottom: "20px" }}>
              <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "6px" }}>
                Default Priority Tier for System Alerts
              </label>
              <select
                className="adm-select"
                style={{ width: "320px" }}
                value={defaultPriority}
                onChange={(e) => setDefaultPriority(e.target.value)}
              >
                <option value="NORMAL">NORMAL (Informational event)</option>
                <option value="HIGH">HIGH (Important milestone / deadline)</option>
                <option value="URGENT">URGENT (Action required)</option>
              </select>
            </div>

            <div style={{ marginBottom: "24px", padding: "14px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--adm-line)" }}>
              <label style={{ display: "flex", alignItems: "center", gap: "10px", cursor: "pointer", fontSize: "13.5px", fontWeight: "600" }}>
                <input
                  type="checkbox"
                  checked={inAppEnabled}
                  onChange={(e) => setInAppEnabled(e.target.checked)}
                />
                Enable In-App Notification Dispatch
              </label>
              <div style={{ fontSize: "12px", color: "var(--adm-soft)", marginTop: "4px", marginLeft: "24px" }}>
                When unchecked, notifications are recorded in database but toast banners are suppressed.
              </div>
            </div>

            <div style={{ marginBottom: "24px" }}>
              <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "8px" }}>
                Supported Notification Event Categories
              </label>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
                {settings?.supportedCategories?.map((cat) => (
                  <span key={cat} className="adm-badge" style={{ padding: "6px 12px", fontSize: "12px" }}>
                    ✓ {cat}
                  </span>
                ))}
              </div>
            </div>

            <button type="submit" className="adm-btn adm-btn-primary" disabled={saving}>
              {saving ? "Saving..." : "Save Notification Policies"}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
