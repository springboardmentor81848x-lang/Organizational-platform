import React, { useState, useEffect } from "react";
import { getMentorRequests, respondToMentorshipRequest } from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

const STATUS_FILTERS = [
  { id: "ALL", label: "All Requests" },
  { id: "PENDING", label: "Pending" },
  { id: "ACCEPTED", label: "Accepted" },
  { id: "DECLINED", label: "Declined" },
];

export default function MentorshipRequestsView({ onRequestsUpdated }) {
  const [filter, setFilter] = useState("ALL");
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Respond Modal State
  const [selectedReq, setSelectedReq] = useState(null);
  const [responseAction, setResponseAction] = useState("ACCEPTED");
  const [responseNote, setResponseNote] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadRequests = async () => {
    setLoading(true);
    try {
      const data = await getMentorRequests(filter);
      setRequests(data);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, [filter]);

  const openRespondModal = (req, action) => {
    setSelectedReq(req);
    setResponseAction(action);
    setResponseNote(
      action === "ACCEPTED"
        ? "I would be glad to mentor you. Let's schedule our introductory session."
        : "Thank you for reaching out. Unfortunately, I am currently at full mentee capacity."
    );
  };

  const handleConfirmResponse = async (e) => {
    e.preventDefault();
    if (!selectedReq) return;
    setSubmitting(true);
    setError("");
    setSuccess("");
    try {
      await respondToMentorshipRequest(selectedReq.id, {
        status: responseAction,
        responseNote,
      });
      setSuccess(`Mentorship request from ${selectedReq.employeeName} has been ${responseAction.toLowerCase()}!`);
      setSelectedReq(null);
      loadRequests();
      if (onRequestsUpdated) onRequestsUpdated();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Mentorship Requests</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Review incoming guidance inquiries from enterprise employees.
            </p>
          </div>

          {/* Filter Pills */}
          <div className="mentor-filter-bar">
            {STATUS_FILTERS.map((f) => (
              <button
                key={f.id}
                type="button"
                className={`mentor-pill-btn ${filter === f.id ? "is-active" : ""}`}
                onClick={() => setFilter(f.id)}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}
        {success && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{success}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading requests...</p>
        ) : requests.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
            <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No requests match the current filter.</p>
            <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>Incoming requests from employees will appear here.</p>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
            {requests.map((r) => {
              const badgeClass =
                r.status === "PENDING"
                  ? "mt-badge--pending"
                  : r.status === "ACCEPTED"
                  ? "mt-badge--active"
                  : "mt-badge--declined";

              return (
                <div
                  key={r.id}
                  style={{
                    padding: "18px 20px",
                    borderRadius: "10px",
                    border: "1px solid var(--mt-line)",
                    background: r.status === "PENDING" ? "#ffffff" : "#f8fafc",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    flexWrap: "wrap",
                    gap: "16px",
                  }}
                >
                  <div style={{ flex: "1 1 300px" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "6px" }}>
                      <strong style={{ fontSize: "1.05rem" }}>{r.employeeName}</strong>
                      <span className="mt-badge mt-badge--neutral">{r.employeeJobRole}</span>
                      <span className={`mt-badge ${badgeClass}`}>{r.status}</span>
                    </div>

                    <div style={{ fontSize: "0.82rem", color: "var(--mt-soft)", marginBottom: "8px" }}>
                      {r.employeeEmail} &bull; Received on {new Date(r.createdAt).toLocaleDateString()}
                    </div>

                    <div
                      style={{
                        padding: "10px 14px",
                        background: "#f1f5f9",
                        borderRadius: "8px",
                        fontSize: "0.88rem",
                        color: "var(--mt-ink)",
                        fontStyle: "italic",
                      }}
                    >
                      &ldquo;{r.message || "I would appreciate your mentorship guidance."}&rdquo;
                    </div>

                    {r.responseNote && (
                      <div style={{ marginTop: "10px", fontSize: "0.84rem", color: "var(--mt-soft)" }}>
                        <strong>Your Response Note:</strong> {r.responseNote}
                        {r.respondedAt && (
                          <span style={{ fontSize: "0.78rem", color: "var(--mt-faint)", marginLeft: "6px" }}>
                            ({new Date(r.respondedAt).toLocaleDateString()})
                          </span>
                        )}
                      </div>
                    )}
                  </div>

                  {r.status === "PENDING" && (
                    <div style={{ display: "flex", gap: "8px", alignSelf: "center" }}>
                      <button
                        type="button"
                        className="mt-btn mt-btn--primary"
                        onClick={() => openRespondModal(r, "ACCEPTED")}
                      >
                        Accept
                      </button>
                      <button
                        type="button"
                        className="mt-btn mt-btn--danger"
                        onClick={() => openRespondModal(r, "DECLINED")}
                      >
                        Decline
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Response Modal */}
      {selectedReq && (
        <div className="mt-modal-backdrop" onClick={() => setSelectedReq(null)}>
          <div className="mt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>
                {responseAction === "ACCEPTED" ? "Accept Mentorship Request" : "Decline Mentorship Request"}
              </h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setSelectedReq(null)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleConfirmResponse}>
              <div className="mt-modal-body">
                <div>
                  <strong>Mentee:</strong> {selectedReq.employeeName} ({selectedReq.employeeJobRole})
                </div>
                <div style={{ fontSize: "0.84rem", color: "var(--mt-soft)" }}>
                  <strong>Inquiry:</strong> &ldquo;{selectedReq.message}&rdquo;
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Response Note to Employee</label>
                  <textarea
                    rows={4}
                    required
                    className="mt-textarea"
                    value={responseNote}
                    onChange={(e) => setResponseNote(e.target.value)}
                    placeholder="Provide a constructive reply or next steps..."
                  />
                </div>
              </div>

              <div className="mt-modal-footer">
                <button
                  type="button"
                  className="mt-btn mt-btn--outline"
                  onClick={() => setSelectedReq(null)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className={`mt-btn ${responseAction === "ACCEPTED" ? "mt-btn--primary" : "mt-btn--danger"}`}
                  disabled={submitting}
                >
                  {submitting ? "Processing..." : `Confirm ${responseAction}`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
