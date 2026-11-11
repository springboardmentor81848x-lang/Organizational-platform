import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getDepartmentNotifications,
  markDepartmentNotificationRead,
  markAllDepartmentNotificationsRead,
} from "../../services/departmentHeadService.js";

export default function DepartmentNotificationsView() {
  const [notifications, setNotifications] = useState([]);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadNotifications = async (onlyUnread = unreadOnly) => {
    try {
      setLoading(true);
      setError("");
      const res = await getDepartmentNotifications(onlyUnread);
      setNotifications(res || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load notifications.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadNotifications(unreadOnly);
  }, [unreadOnly]);

  const handleMarkRead = async (id) => {
    try {
      await markDepartmentNotificationRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
      );
    } catch {
      // ignore
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllDepartmentNotificationsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    } catch {
      // ignore
    }
  };

  return (
    <div>
      <div className="dh-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="dh-panel-title" style={{ margin: 0 }}>
              <Icon name="bell" size={18} />
              Department Leadership Notification Center
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--dh-soft)" }}>
              System notifications, assessment completions, governance alerts, and broadcast confirmations.
            </p>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <label style={{ fontSize: "13px", display: "flex", alignItems: "center", gap: "6px", cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={unreadOnly}
                onChange={(e) => setUnreadOnly(e.target.checked)}
              />
              Show Unread Only
            </label>

            <button className="dh-btn dh-btn-secondary" onClick={handleMarkAllRead}>
              <Icon name="check" size={14} />
              Mark All Read
            </button>
          </div>
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}

      {loading ? (
        <div style={{ textAlign: "center", padding: "60px 20px" }}>
          <Icon name="refresh" className="spin" size={32} />
          <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Loading Notifications...</h4>
        </div>
      ) : notifications.length === 0 ? (
        <div className="dh-panel" style={{ textAlign: "center", padding: "40px 20px" }}>
          <Icon name="bell" size={32} style={{ color: "var(--dh-faint)" }} />
          <h4 style={{ margin: "12px 0 6px", color: "var(--dh-ink)" }}>No Notifications</h4>
          <p style={{ margin: 0, color: "var(--dh-soft)", fontSize: "13.5px" }}>
            {unreadOnly ? "You have no unread notifications." : "Your notification inbox is clean."}
          </p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          {notifications.map((notif) => (
            <div
              key={notif.id}
              className="dh-panel"
              style={{
                marginBottom: 0,
                padding: "16px 20px",
                background: notif.isRead ? "#ffffff" : "var(--dh-primary-tint)",
                borderLeft: notif.isRead ? "1px solid var(--dh-line)" : "4px solid var(--dh-primary)",
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "12px" }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <span className="dh-badge" style={{ fontSize: "10.5px" }}>
                      {notif.type}
                    </span>
                    <strong style={{ fontSize: "14.5px", color: "var(--dh-ink)" }}>{notif.title}</strong>
                  </div>
                  <p style={{ margin: "6px 0 0", fontSize: "13.5px", color: "var(--dh-soft)" }}>
                    {notif.message}
                  </p>
                  <div style={{ fontSize: "11px", color: "var(--dh-faint)", marginTop: "6px" }}>
                    {notif.createdAt ? new Date(notif.createdAt).toLocaleString() : ""}
                  </div>
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: "8px", flexShrink: 0 }}>
                  {!notif.isRead && (
                    <button
                      className="dh-btn dh-btn-secondary"
                      style={{ fontSize: "11.5px", padding: "4px 8px" }}
                      onClick={() => handleMarkRead(notif.id)}
                    >
                      Mark Read
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
