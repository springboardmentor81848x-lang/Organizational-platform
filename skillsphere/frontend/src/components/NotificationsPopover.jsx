import { useEffect, useState } from "react";
import Icon from "./Icons.jsx";
import {
  getNotifications,
  getUnreadNotificationsCount,
  markAllNotificationsRead,
  markNotificationRead,
} from "../services/platformService.js";

export default function NotificationsPopover() {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);

  const refresh = () => {
    getUnreadNotificationsCount()
      .then(setUnreadCount)
      .catch(() => {});
  };

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 20000);
    window.addEventListener("notifications-updated", refresh);
    return () => {
      clearInterval(interval);
      window.removeEventListener("notifications-updated", refresh);
    };
  }, []);

  const toggleOpen = () => {
    if (!open) {
      getNotifications()
        .then((data) => {
          setNotifications(data);
          setOpen(true);
        })
        .catch(() => {});
    } else {
      setOpen(false);
    }
  };

  const handleMarkAll = async () => {
    await markAllNotificationsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    setUnreadCount(0);
    window.dispatchEvent(new Event("notifications-updated"));
  };

  const handleMarkOne = async (id) => {
    await markNotificationRead(id);
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
    setUnreadCount((c) => Math.max(0, c - 1));
    window.dispatchEvent(new Event("notifications-updated"));
  };

  return (
    <div style={{ position: "relative" }}>
      <button
        type="button"
        className="btn btn--ghost"
        onClick={toggleOpen}
        aria-label="Notifications"
        style={{ position: "relative", padding: "8px 10px", borderRadius: "10px" }}
      >
        <Icon name="bell" size={20} />
        {unreadCount > 0 && (
          <span
            style={{
              position: "absolute",
              top: "-2px",
              right: "-2px",
              background: "#b42318",
              color: "#fff",
              borderRadius: "99px",
              fontSize: "0.68rem",
              fontWeight: 800,
              padding: "1px 6px",
              minWidth: "18px",
              textAlign: "center",
            }}
          >
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          style={{
            position: "absolute",
            right: 0,
            top: "calc(100% + 8px)",
            width: "340px",
            background: "#fff",
            borderRadius: "14px",
            border: "1px solid var(--e-line, #e2e9f3)",
            boxShadow: "0 10px 30px -10px rgba(15,39,71,0.25)",
            zIndex: 100,
            overflow: "hidden",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              padding: "12px 16px",
              borderBottom: "1px solid var(--e-line, #e2e9f3)",
              background: "#f9fbfe",
            }}
          >
            <strong style={{ fontSize: "0.9rem" }}>Notifications</strong>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAll}
                className="text-action"
                style={{ fontSize: "0.78rem" }}
              >
                Mark all read
              </button>
            )}
          </div>

          <div style={{ maxHeight: "360px", overflowY: "auto" }}>
            {notifications.length === 0 ? (
              <div style={{ padding: "24px 16px", textAlign: "center", color: "#8795a9", fontSize: "0.85rem" }}>
                No notifications right now.
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => !n.read && handleMarkOne(n.id)}
                  style={{
                    padding: "12px 16px",
                    borderBottom: "1px solid #f0f4f9",
                    background: n.read ? "#fff" : "#f1f6ff",
                    cursor: n.read ? "default" : "pointer",
                    display: "flex",
                    flexDirection: "column",
                    gap: "4px",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "baseline" }}>
                    <span style={{ fontWeight: 700, fontSize: "0.85rem", color: "#0f2747" }}>
                      {n.title}
                    </span>
                    {!n.read && (
                      <span
                        style={{
                          width: "8px",
                          height: "8px",
                          borderRadius: "50%",
                          background: "#2563eb",
                        }}
                      />
                    )}
                  </div>
                  <p style={{ margin: 0, fontSize: "0.8rem", color: "#5b6d86", lineHeight: 1.4 }}>
                    {n.message}
                  </p>
                  <small style={{ fontSize: "0.7rem", color: "#8795a9" }}>
                    {new Date(n.createdAt).toLocaleDateString()} {new Date(n.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                  </small>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
