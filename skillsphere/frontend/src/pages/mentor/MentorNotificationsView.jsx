import React, { useState, useEffect } from "react";
import {
  getMentorNotifications,
  markNotificationRead,
  markAllNotificationsRead,
} from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function MentorNotificationsView({ onNavigateTab }) {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = async () => {
    setLoading(true);
    try {
      const list = await getMentorNotifications(unreadOnly);
      setNotifications(list);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [unreadOnly]);

  const handleMarkRead = async (id) => {
    try {
      await markNotificationRead(id);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await markAllNotificationsRead();
      setSuccess("All notifications marked as read.");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleActionClick = (n) => {
    if (!n.isRead) {
      markNotificationRead(n.id).catch(() => {});
    }
    if (n.link && onNavigateTab) {
      if (n.link.includes("request")) onNavigateTab("requests");
      else if (n.link.includes("session")) onNavigateTab("sessions");
      else if (n.link.includes("goal")) onNavigateTab("goals");
      else if (n.link.includes("feedback")) onNavigateTab("feedback");
      else onNavigateTab("overview");
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Mentor Notifications Center</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Alerts regarding mentorship requests, session reminders, milestone progress, and feedback.
            </p>
          </div>

          <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
            <div className="mentor-filter-bar">
              <button
                type="button"
                className={`mentor-pill-btn ${!unreadOnly ? "is-active" : ""}`}
                onClick={() => setUnreadOnly(false)}
              >
                All Notifications
              </button>
              <button
                type="button"
                className={`mentor-pill-btn ${unreadOnly ? "is-active" : ""}`}
                onClick={() => setUnreadOnly(true)}
              >
                Unread Only
              </button>
            </div>

            <button
              type="button"
              className="mt-btn mt-btn--outline"
              onClick={handleMarkAllRead}
            >
              Mark All as Read
            </button>
          </div>
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}
        {success && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{success}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading notifications...</p>
        ) : notifications.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
            <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No notifications found.</p>
            <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>You are all caught up with your mentoring events.</p>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {notifications.map((n) => (
              <div
                key={n.id}
                style={{
                  padding: "16px 20px",
                  borderRadius: "10px",
                  border: "1px solid var(--mt-line)",
                  background: n.isRead ? "#f8fafc" : "#ffffff",
                  borderLeft: n.isRead ? "1px solid var(--mt-line)" : "4px solid var(--mt-primary)",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  gap: "14px",
                }}
              >
                <div style={{ flex: 1 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <strong style={{ fontSize: "0.95rem" }}>{n.title}</strong>
                    <span className="mt-badge mt-badge--neutral">{n.type}</span>
                    {!n.isRead && <span className="mt-badge mt-badge--active">New</span>}
                  </div>

                  <p style={{ margin: "4px 0 2px", fontSize: "0.88rem", color: "var(--mt-soft)" }}>
                    {n.message}
                  </p>

                  <small style={{ color: "var(--mt-faint)" }}>
                    {new Date(n.createdAt).toLocaleString()}
                  </small>
                </div>

                <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                  <button
                    type="button"
                    className="mt-btn mt-btn--primary"
                    style={{ fontSize: "0.78rem", padding: "5px 12px" }}
                    onClick={() => handleActionClick(n)}
                  >
                    View &rarr;
                  </button>

                  {!n.isRead && (
                    <button
                      type="button"
                      className="mt-btn mt-btn--outline"
                      style={{ fontSize: "0.78rem", padding: "5px 10px" }}
                      onClick={() => handleMarkRead(n.id)}
                    >
                      Dismiss
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
