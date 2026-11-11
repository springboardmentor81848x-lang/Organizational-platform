import { useState, useEffect, useCallback } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getAdminSupportTickets,
  updateAdminSupportTicket,
  getAdminPlatformFeedback,
} from "../../services/adminService.js";
import { parseApiError } from "../../services/apiClient.js";

const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "—");

export default function SupportIssueLogsView() {
  const [activeTab, setActiveTab] = useState("tickets"); // "tickets" | "feedback"
  const [tickets, setTickets] = useState([]);
  const [feedbacks, setFeedbacks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Filters
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [searchQuery, setSearchQuery] = useState("");

  // Triage Modal
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [triageStatus, setTriageStatus] = useState("OPEN");
  const [triageNotes, setTriageNotes] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const params = {};
      if (statusFilter !== "ALL") params.status = statusFilter;
      if (priorityFilter !== "ALL") params.priority = priorityFilter;

      const [ticketsRes, feedbackRes] = await Promise.all([
        getAdminSupportTickets(params),
        getAdminPlatformFeedback().catch(() => []),
      ]);

      setTickets(ticketsRes || []);
      setFeedbacks(feedbackRes || []);
    } catch (err) {
      setError(parseApiError(err).message || "Failed to load support records.");
    } finally {
      setLoading(false);
    }
  }, [statusFilter, priorityFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleOpenTriage = (ticket) => {
    setSelectedTicket(ticket);
    setTriageStatus(ticket.status || "OPEN");
    setTriageNotes(ticket.adminNotes || "");
  };

  const handleSaveTriage = async (e) => {
    e.preventDefault();
    if (!selectedTicket) return;

    try {
      setSubmitting(true);
      setError("");
      setSuccess("");
      await updateAdminSupportTicket(selectedTicket.id, {
        status: triageStatus,
        adminNotes: triageNotes,
      });
      setSuccess(`Support ticket #${selectedTicket.id} updated to ${triageStatus}.`);
      setSelectedTicket(null);
      await loadData();
    } catch (err) {
      setError(parseApiError(err).message || "Failed to update support ticket.");
    } finally {
      setSubmitting(false);
    }
  };

  // KPIs
  const totalTicketsCount = tickets.length;
  const openTicketsCount = tickets.filter((t) => t.status === "OPEN" || t.status === "IN_PROGRESS").length;
  const resolvedTicketsCount = tickets.filter((t) => t.status === "RESOLVED" || t.status === "CLOSED").length;
  const avgFeedbackRating = feedbacks.length > 0
    ? (feedbacks.reduce((acc, f) => acc + (f.rating || 0), 0) / feedbacks.length).toFixed(1)
    : "—";

  const filteredTickets = tickets.filter((t) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const matchSub = t.subject?.toLowerCase().includes(q);
    const matchName = t.userName?.toLowerCase().includes(q);
    const matchEmail = t.userEmail?.toLowerCase().includes(q);
    return matchSub || matchName || matchEmail;
  });

  const getStatusBadge = (status) => {
    switch (status) {
      case "OPEN": return "adm-badge--warning";
      case "IN_PROGRESS": return "adm-badge--dept";
      case "RESOLVED": return "adm-badge--active";
      case "CLOSED": return "adm-badge--inactive";
      default: return "adm-badge--inactive";
    }
  };

  const getPriorityBadge = (priority) => {
    switch (priority) {
      case "URGENT": return "adm-badge--danger";
      case "HIGH": return "adm-badge--warning";
      case "MEDIUM": return "adm-badge--dept";
      case "LOW": return "adm-badge--info";
      default: return "adm-badge--inactive";
    }
  };

  return (
    <div className="adm-view-container">
      {/* View Header */}
      <div className="adm-view-header">
        <div>
          <span className="adm-eyebrow">HELPDESK & USER GOVERNANCE</span>
          <h1 className="adm-view-title">Support & Issue Resolution Center</h1>
          <p className="adm-view-subtitle">
            Manage user issue tickets, execute administrative triage, track resolution lifecycle, and review employee platform feedback.
          </p>
        </div>
        <div className="adm-view-actions">
          <button
            type="button"
            className="adm-btn adm-btn--ghost"
            onClick={loadData}
            disabled={loading}
          >
            <Icon name="refresh" size={16} /> Refresh Records
          </button>
        </div>
      </div>

      {error && <Alert type="error" message={error} onClose={() => setError("")} />}
      {success && <Alert type="success" message={success} onClose={() => setSuccess("")} />}

      {/* KPI Cards */}
      <div className="adm-kpi-grid">
        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Total Issue Tickets</span>
            <div className="adm-kpi-icon adm-kpi-icon--navy">
              <Icon name="help-circle" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : totalTicketsCount}</div>
          <p className="adm-kpi-caption">Logged platform inquiries</p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Active / In Triage</span>
            <div className="adm-kpi-icon adm-kpi-icon--amber">
              <Icon name="clock" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : openTicketsCount}</div>
          <p className="adm-kpi-caption">Open or In-Progress tickets</p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Resolved / Closed</span>
            <div className="adm-kpi-icon adm-kpi-icon--emerald">
              <Icon name="check-circle" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : resolvedTicketsCount}</div>
          <p className="adm-kpi-caption">Completed resolutions</p>
        </div>

        <div className="adm-kpi-card">
          <div className="adm-kpi-header">
            <span className="adm-kpi-label">Avg Platform Rating</span>
            <div className="adm-kpi-icon adm-kpi-icon--purple">
              <Icon name="star" size={20} />
            </div>
          </div>
          <div className="adm-kpi-value">{loading ? "—" : `${avgFeedbackRating} / 5`}</div>
          <p className="adm-kpi-caption">{feedbacks.length} user reviews submitted</p>
        </div>
      </div>

      {/* Tabs */}
      <div style={{ display: "flex", gap: "8px", borderBottom: "1px solid #e2e8f0", paddingBottom: "8px" }}>
        <button
          type="button"
          className={`adm-btn ${activeTab === "tickets" ? "adm-btn--primary" : "adm-btn--ghost"}`}
          onClick={() => setActiveTab("tickets")}
          style={{ fontSize: "14px" }}
        >
          <Icon name="help-circle" size={16} /> Support Tickets ({tickets.length})
        </button>
        <button
          type="button"
          className={`adm-btn ${activeTab === "feedback" ? "adm-btn--primary" : "adm-btn--ghost"}`}
          onClick={() => setActiveTab("feedback")}
          style={{ fontSize: "14px" }}
        >
          <Icon name="message-square" size={16} /> User Feedback ({feedbacks.length})
        </button>
      </div>

      {/* Support Tickets Tab Content */}
      {activeTab === "tickets" && (
        <div className="adm-card">
          <div className="adm-card-header">
            <div>
              <h2 className="adm-card-title">Support Ticket Triage Queue</h2>
              <p className="adm-card-subtitle">
                Inspect user submitted issues, update resolution status, and maintain administrative investigation notes.
              </p>
            </div>
            <div className="adm-filter-bar" style={{ margin: 0 }}>
              <div className="adm-search-box" style={{ maxWidth: "220px" }}>
                <Icon name="search" size={16} />
                <input
                  type="text"
                  placeholder="Search subject or user..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>

              <select
                className="adm-filter-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All Statuses</option>
                <option value="OPEN">Open</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="RESOLVED">Resolved</option>
                <option value="CLOSED">Closed</option>
              </select>

              <select
                className="adm-filter-select"
                value={priorityFilter}
                onChange={(e) => setPriorityFilter(e.target.value)}
              >
                <option value="ALL">All Priorities</option>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
          </div>

          <div className="adm-table-container">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Ticket</th>
                  <th>Requester</th>
                  <th>Subject</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Submitted</th>
                  <th style={{ textAlign: "right" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: "center", padding: "32px" }}>
                      Loading support tickets...
                    </td>
                  </tr>
                ) : filteredTickets.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: "center", padding: "32px", color: "#64748b" }}>
                      No support tickets found matching current filters.
                    </td>
                  </tr>
                ) : (
                  filteredTickets.map((t) => (
                    <tr key={t.id}>
                      <td>
                        <strong style={{ color: "#0f172a" }}>#{t.id}</strong>
                      </td>
                      <td>
                        <div>
                          <div style={{ fontWeight: 600, color: "#1e293b", fontSize: "13px" }}>
                            {t.userName || "Unknown User"}
                          </div>
                          <div style={{ fontSize: "12px", color: "#64748b" }}>
                            {t.userEmail}
                          </div>
                        </div>
                      </td>
                      <td>
                        <div style={{ fontWeight: 500, color: "#0f172a", fontSize: "13px" }}>
                          {t.subject}
                        </div>
                        {t.adminNotes && (
                          <div style={{ fontSize: "11px", color: "#059669", marginTop: "2px" }}>
                            <Icon name="check" size={11} style={{ verticalAlign: "middle" }} /> Notes: {t.adminNotes}
                          </div>
                        )}
                      </td>
                      <td>
                        <span className={`adm-badge ${getPriorityBadge(t.priority)}`}>
                          {t.priority}
                        </span>
                      </td>
                      <td>
                        <span className={`adm-badge ${getStatusBadge(t.status)}`}>
                          {t.status}
                        </span>
                      </td>
                      <td style={{ fontSize: "12px", color: "#64748b", whiteSpace: "nowrap" }}>
                        {fmtDate(t.createdAt)}
                      </td>
                      <td style={{ textAlign: "right" }}>
                        <button
                          type="button"
                          className="adm-btn adm-btn--ghost"
                          style={{ padding: "4px 10px", fontSize: "12px" }}
                          onClick={() => handleOpenTriage(t)}
                        >
                          <Icon name="edit" size={13} /> Triage
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Platform Feedback Tab Content */}
      {activeTab === "feedback" && (
        <div className="adm-card">
          <div className="adm-card-header">
            <div>
              <h2 className="adm-card-title">User Experience & Platform Feedback</h2>
              <p className="adm-card-subtitle">
                Direct ratings and commentary submitted by employees, mentors, and managers across all departments.
              </p>
            </div>
          </div>

          <div className="adm-table-container">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Submitted By</th>
                  <th>Category</th>
                  <th>Rating</th>
                  <th>Comments</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {feedbacks.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ textAlign: "center", padding: "32px", color: "#64748b" }}>
                      No platform feedback entries recorded.
                    </td>
                  </tr>
                ) : (
                  feedbacks.map((fb) => (
                    <tr key={fb.id}>
                      <td>
                        <div style={{ fontWeight: 600, color: "#1e293b", fontSize: "13px" }}>
                          {fb.user?.fullName || fb.userFullName || "Anonymous"}
                        </div>
                        <div style={{ fontSize: "12px", color: "#64748b" }}>
                          {fb.user?.email || fb.userEmail || "—"}
                        </div>
                      </td>
                      <td>
                        <span className="adm-badge adm-badge--dept">
                          {fb.category || "GENERAL"}
                        </span>
                      </td>
                      <td>
                        <span style={{ color: "#d97706", fontWeight: 700, fontSize: "14px" }}>
                          {"★".repeat(fb.rating || 0)}
                          {"☆".repeat(Math.max(0, 5 - (fb.rating || 0)))}
                        </span>
                        <span style={{ fontSize: "12px", color: "#64748b", marginLeft: "6px" }}>
                          ({fb.rating}/5)
                        </span>
                      </td>
                      <td style={{ maxWidth: "340px", fontSize: "13px", color: "#334155" }}>
                        {fb.comments || "No textual comments provided."}
                      </td>
                      <td style={{ fontSize: "12px", color: "#64748b", whiteSpace: "nowrap" }}>
                        {fmtDate(fb.createdAt)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Triage Modal */}
      {selectedTicket && (
        <div className="adm-modal-overlay">
          <div className="adm-modal" style={{ maxWidth: "550px" }}>
            <div className="adm-modal-header">
              <h2 className="adm-modal-title">Triage Support Ticket #{selectedTicket.id}</h2>
              <button
                type="button"
                className="adm-btn adm-btn--ghost"
                onClick={() => setSelectedTicket(null)}
                style={{ padding: "4px 8px" }}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveTriage}>
              <div className="adm-modal-body">
                <div style={{ padding: "12px 16px", backgroundColor: "#f8fafc", borderRadius: "8px", marginBottom: "16px", border: "1px solid #e2e8f0" }}>
                  <div style={{ fontSize: "12px", color: "#64748b" }}>REQUESTER</div>
                  <div style={{ fontWeight: 600, color: "#1e293b", fontSize: "14px" }}>
                    {selectedTicket.userName} ({selectedTicket.userEmail})
                  </div>
                  <div style={{ fontSize: "12px", color: "#64748b", marginTop: "8px" }}>SUBJECT</div>
                  <div style={{ fontWeight: 600, color: "#0f172a", fontSize: "14px" }}>
                    {selectedTicket.subject}
                  </div>
                  {selectedTicket.description && (
                    <div style={{ marginTop: "8px" }}>
                      <div style={{ fontSize: "12px", color: "#64748b" }}>DESCRIPTION</div>
                      <p style={{ margin: "4px 0 0 0", fontSize: "13px", color: "#334155", whiteSpace: "pre-wrap" }}>
                        {selectedTicket.description}
                      </p>
                    </div>
                  )}
                </div>

                <div className="adm-form-group">
                  <label className="adm-form-label">Update Status *</label>
                  <select
                    className="adm-input"
                    value={triageStatus}
                    onChange={(e) => setTriageStatus(e.target.value)}
                    required
                  >
                    <option value="OPEN">OPEN — Awaiting Investigation</option>
                    <option value="IN_PROGRESS">IN_PROGRESS — Active Troubleshooting</option>
                    <option value="RESOLVED">RESOLVED — Issue Rectified</option>
                    <option value="CLOSED">CLOSED — Concluded Without Further Action</option>
                  </select>
                </div>

                <div className="adm-form-group">
                  <label className="adm-form-label">Administrative Resolution Notes</label>
                  <textarea
                    className="adm-input"
                    rows={4}
                    placeholder="Enter diagnostic details, resolution summary, or instructions for the user..."
                    value={triageNotes}
                    onChange={(e) => setTriageNotes(e.target.value)}
                  />
                  <span className="adm-form-hint">
                    Recorded in audit trail and displayed on the ticket resolution record.
                  </span>
                </div>
              </div>

              <div className="adm-modal-footer">
                <button
                  type="button"
                  className="adm-btn adm-btn--ghost"
                  onClick={() => setSelectedTicket(null)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="adm-btn adm-btn--primary"
                  disabled={submitting}
                >
                  {submitting ? "Updating..." : "Save Triage Resolution"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
