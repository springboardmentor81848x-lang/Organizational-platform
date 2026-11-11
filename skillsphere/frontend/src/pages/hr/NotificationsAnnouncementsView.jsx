import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getHrAnnouncements,
  createHrAnnouncement,
  markHrAnnouncementRead,
} from "../../services/hrService.js";
import { getDepartments } from "../../services/platformService.js";

const PRIORITIES = ["NORMAL", "HIGH", "URGENT"];
const TARGET_TYPES = [
  { id: "ORGANIZATION", label: "Enterprise-wide (All Employees)" },
  { id: "DEPARTMENT", label: "Specific Department" },
  { id: "ROLE", label: "Specific Role (e.g. MANAGER, EMPLOYEE)" },
];

export default function NotificationsAnnouncementsView() {
  const [announcements, setAnnouncements] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Modal
  const [showModal, setShowModal] = useState(false);
  const [formTitle, setFormTitle] = useState("");
  const [formContent, setFormContent] = useState("");
  const [formTargetType, setFormTargetType] = useState("ORGANIZATION");
  const [formDeptId, setFormDeptId] = useState("");
  const [formTargetRole, setFormTargetRole] = useState("EMPLOYEE");
  const [formPriority, setFormPriority] = useState("NORMAL");
  const [saving, setSaving] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [annList, deptList] = await Promise.all([
        getHrAnnouncements(),
        getDepartments().catch(() => []),
      ]);
      setAnnouncements(annList);
      setDepartments(deptList);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load announcements.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenAdd = () => {
    setFormTitle("");
    setFormContent("");
    setFormTargetType("ORGANIZATION");
    setFormDeptId(departments[0]?.id || "");
    setFormTargetRole("EMPLOYEE");
    setFormPriority("NORMAL");
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formTitle.trim() || !formContent.trim()) {
      setError("Please provide both title and content.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      const payload = {
        title: formTitle.trim(),
        content: formContent.trim(),
        targetType: formTargetType,
        departmentId: formTargetType === "DEPARTMENT" && formDeptId ? Number(formDeptId) : null,
        targetRole: formTargetType === "ROLE" ? formTargetRole.trim() : null,
        priority: formPriority,
      };

      await createHrAnnouncement(payload);
      setSuccess(`Published announcement "${formTitle}". Targeted notifications dispatched.`);
      setShowModal(false);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to create announcement.");
    } finally {
      setSaving(false);
    }
  };

  const handleMarkRead = async (id) => {
    try {
      await markHrAnnouncementRead(id);
      loadData();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Notifications & Workforce Announcements</h3>
            <p className="hr-card-subtitle">
              Publish enterprise broadcasts, assessment deadline reminders, and monitor workforce read receipts
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <button className="btn-secondary btn-sm" onClick={loadData}>
              <Icon name="refresh" size={14} /> Refresh
            </button>
            <button className="btn-primary btn-sm" onClick={handleOpenAdd}>
              <Icon name="bell" size={14} /> Post Announcement
            </button>
          </div>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}
          {success && <Alert type="success" message={success} />}

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading announcements...</h4>
            </div>
          ) : announcements.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="bell" size={32} />
              <h4>No announcements published</h4>
              <p>Send your first targeted reminder or corporate broadcast to employees.</p>
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              {announcements.map((a) => {
                const readPct = a.totalTargetCount > 0
                  ? Math.round((a.readCount * 100) / a.totalTargetCount)
                  : 0;

                return (
                  <div
                    key={a.id}
                    style={{
                      border: "1px solid var(--hr-line)",
                      borderRadius: "8px",
                      background: "#ffffff",
                      padding: "20px",
                      borderLeft: `4px solid ${
                        a.priority === "URGENT"
                          ? "var(--hr-rose)"
                          : a.priority === "HIGH"
                          ? "var(--hr-amber)"
                          : "var(--hr-primary)"
                      }`,
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "flex-start",
                        flexWrap: "wrap",
                        gap: "10px",
                        marginBottom: "10px",
                      }}
                    >
                      <div>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}>
                          <h4 style={{ margin: 0, fontSize: "16px", fontWeight: 700, color: "var(--hr-ink)" }}>
                            {a.title}
                          </h4>
                          <span
                            className={`status-pill ${
                              a.priority === "URGENT"
                                ? "danger"
                                : a.priority === "HIGH"
                                ? "warning"
                                : "info"
                            }`}
                          >
                            {a.priority}
                          </span>
                          <span className="status-pill purple">
                            {a.targetType === "ORGANIZATION"
                              ? "Enterprise"
                              : a.targetType === "DEPARTMENT"
                              ? a.departmentName
                              : `Role: ${a.targetRole}`}
                          </span>
                        </div>
                        <div style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                          Posted by {a.authorName} • {new Date(a.createdAt).toLocaleString()}
                        </div>
                      </div>

                      {/* Read Tracking Pill */}
                      <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                        <div
                          style={{
                            padding: "6px 12px",
                            background: "#f8fafc",
                            border: "1px solid var(--hr-line)",
                            borderRadius: "6px",
                            fontSize: "12.5px",
                          }}
                        >
                          <strong>{a.readCount}</strong> / {a.totalTargetCount} read ({readPct}%)
                        </div>
                        {!a.readByCurrentUser && (
                          <button
                            className="btn-secondary btn-sm"
                            onClick={() => handleMarkRead(a.id)}
                          >
                            Mark Read
                          </button>
                        )}
                      </div>
                    </div>

                    <p style={{ margin: 0, fontSize: "13.5px", color: "var(--hr-ink)", lineHeight: 1.5, whiteSpace: "pre-wrap" }}>
                      {a.content}
                    </p>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="hr-modal-overlay" onClick={handleCloseModal}>
          <div className="hr-modal" style={{ maxWidth: 680 }} onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleSave}>
              <div className="hr-modal-header">
                <h3>Post Announcement & Targeted Reminder</h3>
                <button type="button" className="hr-modal-close" onClick={handleCloseModal}>
                  &times;
                </button>
              </div>

              <div className="hr-modal-body">
                <div className="form-group">
                  <label className="form-label">Announcement Title *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Q4 Baseline Skill Assessment Due in 7 Days"
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                  <div className="form-group">
                    <label className="form-label">Target Audience *</label>
                    <select
                      className="form-control"
                      value={formTargetType}
                      onChange={(e) => setFormTargetType(e.target.value)}
                    >
                      {TARGET_TYPES.map((t) => (
                        <option key={t.id} value={t.id}>
                          {t.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Urgency Priority</label>
                    <select
                      className="form-control"
                      value={formPriority}
                      onChange={(e) => setFormPriority(e.target.value)}
                    >
                      {PRIORITIES.map((p) => (
                        <option key={p} value={p}>
                          {p}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                {formTargetType === "DEPARTMENT" && (
                  <div className="form-group">
                    <label className="form-label">Target Department *</label>
                    <select
                      className="form-control"
                      value={formDeptId}
                      onChange={(e) => setFormDeptId(e.target.value)}
                      required
                    >
                      {departments.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {formTargetType === "ROLE" && (
                  <div className="form-group">
                    <label className="form-label">Target Role *</label>
                    <select
                      className="form-control"
                      value={formTargetRole}
                      onChange={(e) => setFormTargetRole(e.target.value)}
                      required
                    >
                      <option value="EMPLOYEE">Employees Only</option>
                      <option value="MANAGER">Managers / Team Leads Only</option>
                      <option value="MENTOR">Mentors Only</option>
                      <option value="DEPARTMENT_HEAD">Department Heads Only</option>
                    </select>
                  </div>
                )}

                <div className="form-group">
                  <label className="form-label">Announcement Content & Instructions *</label>
                  <textarea
                    className="form-control"
                    placeholder="Write detailed announcement body, links, or deadlines..."
                    style={{ minHeight: "120px" }}
                    value={formContent}
                    onChange={(e) => setFormContent(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="hr-modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? "Publishing..." : "Publish & Notify"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
