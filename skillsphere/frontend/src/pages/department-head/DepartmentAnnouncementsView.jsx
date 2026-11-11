import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getDepartmentAnnouncements,
  createDepartmentAnnouncement,
  markDepartmentAnnouncementRead,
} from "../../services/departmentHeadService.js";

export default function DepartmentAnnouncementsView() {
  const [announcements, setAnnouncements] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showModal, setShowModal] = useState(false);

  // Form state
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [priority, setPriority] = useState("NORMAL");
  const [submitting, setSubmitting] = useState(false);

  const loadAnnouncements = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getDepartmentAnnouncements();
      setAnnouncements(res || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load announcements.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAnnouncements();
  }, []);

  const handleMarkRead = async (id) => {
    try {
      await markDepartmentAnnouncementRead(id);
      setAnnouncements((prev) =>
        prev.map((a) => (a.id === id ? { ...a, readByCurrentUser: true, readCount: a.readCount + 1 } : a))
      );
    } catch {
      // ignore
    }
  };

  const handleCreateAnnouncement = async (e) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      setError("Please provide both a title and content for the announcement.");
      return;
    }

    try {
      setSubmitting(true);
      setError("");
      await createDepartmentAnnouncement({
        title: title.trim(),
        content: content.trim(),
        priority,
      });

      setSuccess("Department directive broadcasted successfully to all teams.");
      setShowModal(false);
      setTitle("");
      setContent("");
      setPriority("NORMAL");
      loadAnnouncements();
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to broadcast announcement.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div>
      <div className="dh-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="dh-panel-title" style={{ margin: 0 }}>
              <Icon name="bell" size={18} />
              Department Communications & Directives
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--dh-soft)" }}>
              Broadcast strategic guidance, milestone notices, and technical directives to your workforce.
            </p>
          </div>

          <button className="dh-btn dh-btn-primary" onClick={() => setShowModal(true)}>
            <Icon name="plus" size={14} />
            Post Department Directive
          </button>
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}
      {success && <Alert type="success" message={success} />}

      {loading ? (
        <div style={{ textAlign: "center", padding: "60px 20px" }}>
          <Icon name="refresh" className="spin" size={32} />
          <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Loading Communications...</h4>
        </div>
      ) : announcements.length === 0 ? (
        <div className="dh-panel" style={{ textAlign: "center", padding: "40px 20px" }}>
          <Icon name="message" size={32} style={{ color: "var(--dh-faint)" }} />
          <h4 style={{ margin: "12px 0 6px", color: "var(--dh-ink)" }}>No Active Directives</h4>
          <p style={{ margin: 0, color: "var(--dh-soft)", fontSize: "13.5px" }}>
            Click "Post Department Directive" to dispatch targeted instructions to your teams.
          </p>
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {announcements.map((a) => (
            <div
              key={a.id}
              className="dh-panel"
              style={{
                marginBottom: 0,
                borderLeft: `4px solid ${
                  a.priority === "URGENT"
                    ? "var(--dh-rose)"
                    : a.priority === "HIGH"
                    ? "var(--dh-amber)"
                    : "var(--dh-primary)"
                }`,
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "12px" }}>
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                    <span
                      className={`dh-status-badge ${
                        a.priority === "URGENT" ? "critical" : a.priority === "HIGH" ? "high" : "low"
                      }`}
                    >
                      {a.priority}
                    </span>
                    <strong style={{ fontSize: "16px", color: "var(--dh-ink)" }}>{a.title}</strong>
                  </div>
                  <div style={{ fontSize: "12px", color: "var(--dh-faint)", marginTop: "4px" }}>
                    Broadcasted by <strong>{a.authorName}</strong> •{" "}
                    {a.createdAt ? new Date(a.createdAt).toLocaleString() : "Recently"}
                  </div>
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                  <span className="dh-badge" title="Read receipts by department personnel">
                    ✓ Read by {a.readCount} / {a.totalTargetCount}
                  </span>
                  {!a.readByCurrentUser && (
                    <button
                      className="dh-btn dh-btn-secondary"
                      style={{ fontSize: "12px", padding: "4px 8px" }}
                      onClick={() => handleMarkRead(a.id)}
                    >
                      Mark Read
                    </button>
                  )}
                </div>
              </div>

              <p style={{ margin: "14px 0 0", fontSize: "14px", color: "var(--dh-soft)", lineHeight: "1.6", whiteSpace: "pre-wrap" }}>
                {a.content}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Broadcast Modal */}
      {showModal && (
        <div className="dh-modal-overlay" onClick={() => setShowModal(false)}>
          <div className="dh-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px", color: "var(--dh-ink)" }}>
                Broadcast Department Directive
              </h3>
              <button
                style={{ background: "none", border: "none", cursor: "pointer", color: "var(--dh-soft)" }}
                onClick={() => setShowModal(false)}
              >
                <Icon name="x" size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateAnnouncement}>
              <div style={{ marginBottom: "14px" }}>
                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "6px" }}>
                  Directive Title
                </label>
                <input
                  type="text"
                  className="dh-input"
                  style={{ width: "100%" }}
                  placeholder="e.g. Q4 Cloud Certification Deadline & Assessment Policy"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                />
              </div>

              <div style={{ marginBottom: "14px" }}>
                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "6px" }}>
                  Priority Level
                </label>
                <select
                  className="dh-select"
                  style={{ width: "100%" }}
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                >
                  <option value="NORMAL">NORMAL (Informational guidance)</option>
                  <option value="HIGH">HIGH (Important milestone / deadline)</option>
                  <option value="URGENT">URGENT (Immediate operational action)</option>
                </select>
              </div>

              <div style={{ marginBottom: "20px" }}>
                <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "6px" }}>
                  Message Content
                </label>
                <textarea
                  className="dh-input"
                  rows={5}
                  style={{ width: "100%", fontFamily: "inherit" }}
                  placeholder="Outline expectations, affected personnel, deliverables, or deadlines..."
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
                <button
                  type="button"
                  className="dh-btn dh-btn-secondary"
                  onClick={() => setShowModal(false)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button type="submit" className="dh-btn dh-btn-primary" disabled={submitting}>
                  <Icon name="send" size={14} />
                  {submitting ? "Broadcasting..." : "Broadcast Directive"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
