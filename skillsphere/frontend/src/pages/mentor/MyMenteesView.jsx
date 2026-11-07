import React, { useState, useEffect } from "react";
import { getMyMentees, getMenteeDossier, endMentorship } from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function MyMenteesView({ onNavigateTab }) {
  const [statusFilter, setStatusFilter] = useState("ACTIVE");
  const [mentees, setMentees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Dossier Modal
  const [dossier, setDossier] = useState(null);
  const [loadingDossier, setLoadingDossier] = useState(false);

  // End Mentorship Modal
  const [endingAssignment, setEndingAssignment] = useState(null);
  const [endReason, setEndReason] = useState("");
  const [submittingEnd, setSubmittingEnd] = useState(false);

  const loadMentees = async () => {
    setLoading(true);
    try {
      const data = await getMyMentees(statusFilter);
      setMentees(data);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMentees();
  }, [statusFilter]);

  const handleOpenDossier = async (assignmentId) => {
    setLoadingDossier(true);
    setError("");
    try {
      const data = await getMenteeDossier(assignmentId);
      setDossier(data);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoadingDossier(false);
    }
  };

  const handleEndMentorship = async (e) => {
    e.preventDefault();
    if (!endingAssignment) return;
    setSubmittingEnd(true);
    try {
      await endMentorship(endingAssignment.assignmentId, { reason: endReason });
      setSuccess(`Mentorship with ${endingAssignment.employeeName} marked as completed!`);
      setEndingAssignment(null);
      setEndReason("");
      loadMentees();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmittingEnd(false);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>My Mentees Directory</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Active mentorship pairings, dossiers, goal milestones, and sessions.
            </p>
          </div>

          <div className="mentor-filter-bar">
            <button
              type="button"
              className={`mentor-pill-btn ${statusFilter === "ACTIVE" ? "is-active" : ""}`}
              onClick={() => setStatusFilter("ACTIVE")}
            >
              Active Mentees
            </button>
            <button
              type="button"
              className={`mentor-pill-btn ${statusFilter === "COMPLETED" ? "is-active" : ""}`}
              onClick={() => setStatusFilter("COMPLETED")}
            >
              Past / Completed
            </button>
            <button
              type="button"
              className={`mentor-pill-btn ${statusFilter === "ALL" ? "is-active" : ""}`}
              onClick={() => setStatusFilter("ALL")}
            >
              All Records
            </button>
          </div>
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}
        {success && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{success}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading mentees...</p>
        ) : mentees.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
            <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No {statusFilter.toLowerCase()} mentees found.</p>
            <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>
              Accept incoming requests to add mentees to your roster.
            </p>
          </div>
        ) : (
          <div className="mentor-table-wrap">
            <table className="mentor-table">
              <thead>
                <tr>
                  <th>Mentee</th>
                  <th>Role & Dept</th>
                  <th>Status</th>
                  <th>Paired Date</th>
                  <th>Goals</th>
                  <th>Sessions</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {mentees.map((m) => (
                  <tr key={m.assignmentId}>
                    <td>
                      <strong>{m.employeeName}</strong>
                      <div style={{ fontSize: "0.78rem", color: "var(--mt-faint)" }}>{m.email}</div>
                    </td>
                    <td>
                      <div>{m.jobRole}</div>
                      <small style={{ color: "var(--mt-faint)" }}>{m.department}</small>
                    </td>
                    <td>
                      <span className={`mt-badge ${m.status === "ACTIVE" ? "mt-badge--active" : "mt-badge--neutral"}`}>
                        {m.status}
                      </span>
                    </td>
                    <td>{new Date(m.assignedAt).toLocaleDateString()}</td>
                    <td>
                      <span className="mt-badge mt-badge--neutral">
                        {m.completedGoals} / {m.totalGoals} Completed
                      </span>
                    </td>
                    <td>
                      <div>{m.totalSessions} Sessions</div>
                      {m.nextSessionDate && (
                        <small style={{ color: "#059669" }}>
                          Next: {new Date(m.nextSessionDate).toLocaleDateString()}
                        </small>
                      )}
                    </td>
                    <td>
                      <div style={{ display: "flex", gap: "6px" }}>
                        <button
                          type="button"
                          className="mt-btn mt-btn--primary"
                          style={{ fontSize: "0.78rem" }}
                          onClick={() => handleOpenDossier(m.assignmentId)}
                        >
                          View Dossier
                        </button>
                        {m.status === "ACTIVE" && (
                          <button
                            type="button"
                            className="mt-btn mt-btn--outline"
                            style={{ fontSize: "0.78rem" }}
                            onClick={() => setEndingAssignment(m)}
                          >
                            End Mentorship
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Mentee Dossier Modal */}
      {dossier && (
        <div className="mt-modal-backdrop" onClick={() => setDossier(null)}>
          <div className="mt-modal mt-modal--wide" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <div>
                <h3 style={{ margin: 0 }}>Mentee Dossier &mdash; {dossier.summary.employeeName}</h3>
                <span style={{ fontSize: "0.82rem", color: "var(--mt-soft)" }}>
                  {dossier.summary.jobRole} &bull; {dossier.summary.department}
                </span>
              </div>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setDossier(null)}
              >
                &times;
              </button>
            </div>

            <div className="mt-modal-body">
              {/* Dossier Header Info */}
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "12px", background: "#f8fafc", padding: "14px", borderRadius: "8px" }}>
                <div>
                  <small style={{ color: "var(--mt-faint)", fontWeight: 700, textTransform: "uppercase" }}>Corporate Email</small>
                  <div>{dossier.summary.email}</div>
                </div>
                <div>
                  <small style={{ color: "var(--mt-faint)", fontWeight: 700, textTransform: "uppercase" }}>Experience Level</small>
                  <div>{dossier.experienceLevel}</div>
                </div>
                <div>
                  <small style={{ color: "var(--mt-faint)", fontWeight: 700, textTransform: "uppercase" }}>Paired Since</small>
                  <div>{new Date(dossier.summary.assignedAt).toLocaleDateString()}</div>
                </div>
                <div>
                  <small style={{ color: "var(--mt-faint)", fontWeight: 700, textTransform: "uppercase" }}>Goals Completed</small>
                  <div>{dossier.summary.completedGoals} / {dossier.summary.totalGoals}</div>
                </div>
              </div>

              {dossier.bio && (
                <div>
                  <h4 style={{ margin: "0 0 4px", fontSize: "0.95rem" }}>Mentee Background</h4>
                  <p style={{ margin: 0, fontSize: "0.88rem", color: "var(--mt-soft)" }}>{dossier.bio}</p>
                </div>
              )}

              {/* Skills & Gaps Summary */}
              <div>
                <h4 style={{ margin: "0 0 8px", fontSize: "0.95rem" }}>Verified Skills & Knowledge Gaps</h4>
                {dossier.skills && dossier.skills.skills && dossier.skills.skills.length > 0 ? (
                  <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginBottom: "8px" }}>
                    {dossier.skills.skills.map((sk) => (
                      <span key={sk.skillId} className="mt-badge mt-badge--active">
                        {sk.skillName} ({sk.proficiencyLevel})
                      </span>
                    ))}
                  </div>
                ) : (
                  <p style={{ fontSize: "0.84rem", color: "var(--mt-soft)" }}>No verified skills on record.</p>
                )}

                {dossier.skills && dossier.skills.gaps && dossier.skills.gaps.length > 0 && (
                  <div style={{ marginTop: "6px" }}>
                    <small style={{ color: "#d97706", fontWeight: 700, display: "block", marginBottom: "4px" }}>
                      Target Knowledge Gaps:
                    </small>
                    <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                      {dossier.skills.gaps.map((gp) => (
                        <span key={gp.gapId} className="mt-badge mt-badge--pending">
                          {gp.skillName}: {gp.currentProficiency} &rarr; {gp.requiredProficiency}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Development Goals */}
              <div>
                <h4 style={{ margin: "0 0 8px", fontSize: "0.95rem" }}>Development Goals ({dossier.goals.length})</h4>
                {dossier.goals.length === 0 ? (
                  <p style={{ fontSize: "0.84rem", color: "var(--mt-soft)" }}>No goals defined yet.</p>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                    {dossier.goals.map((g) => (
                      <div
                        key={g.id}
                        style={{
                          padding: "10px 14px",
                          borderRadius: "8px",
                          border: "1px solid var(--mt-line)",
                          background: "#ffffff",
                        }}
                      >
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                          <strong>{g.title}</strong>
                          <span className={`mt-badge ${g.status === "ACHIEVED" || g.status === "COMPLETED" ? "mt-badge--active" : "mt-badge--pending"}`}>
                            {g.status}
                          </span>
                        </div>
                        {g.description && (
                          <p style={{ margin: "4px 0 0", fontSize: "0.82rem", color: "var(--mt-soft)" }}>
                            {g.description}
                          </p>
                        )}
                        {g.actionItems && g.actionItems.length > 0 && (
                          <small style={{ color: "var(--mt-faint)", marginTop: "4px", display: "block" }}>
                            {g.actionItems.filter((i) => i.isCompleted).length} / {g.actionItems.length} Milestones done
                          </small>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Mentoring Sessions */}
              <div>
                <h4 style={{ margin: "0 0 8px", fontSize: "0.95rem" }}>Mentoring Sessions ({dossier.sessions.length})</h4>
                {dossier.sessions.length === 0 ? (
                  <p style={{ fontSize: "0.84rem", color: "var(--mt-soft)" }}>No sessions scheduled yet.</p>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                    {dossier.sessions.slice(0, 5).map((s) => (
                      <div
                        key={s.id}
                        style={{
                          padding: "10px 14px",
                          borderRadius: "8px",
                          border: "1px solid var(--mt-line)",
                          background: "#ffffff",
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                        }}
                      >
                        <div>
                          <strong>{s.title}</strong>
                          <div style={{ fontSize: "0.78rem", color: "var(--mt-faint)" }}>
                            {new Date(s.scheduledAt).toLocaleString()} ({s.durationMinutes} min)
                          </div>
                        </div>
                        <span className={`mt-badge ${s.status === "COMPLETED" ? "mt-badge--active" : "mt-badge--pending"}`}>
                          {s.status}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="mt-modal-footer">
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                onClick={() => setDossier(null)}
              >
                Close Dossier
              </button>
            </div>
          </div>
        </div>
      )}

      {/* End Mentorship Modal */}
      {endingAssignment && (
        <div className="mt-modal-backdrop" onClick={() => setEndingAssignment(null)}>
          <div className="mt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>Conclude Mentorship &mdash; {endingAssignment.employeeName}</h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setEndingAssignment(null)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleEndMentorship}>
              <div className="mt-modal-body">
                <p style={{ fontSize: "0.88rem", color: "var(--mt-soft)" }}>
                  Concluding this mentorship will mark the relationship as completed, release a mentee slot in your capacity, and notify the mentee.
                </p>

                <div className="mt-form-group">
                  <label className="mt-label">Completion Summary & Outcome Notes</label>
                  <textarea
                    rows={4}
                    className="mt-textarea"
                    value={endReason}
                    onChange={(e) => setEndReason(e.target.value)}
                    placeholder="e.g. Completed target goals in architecture and testing. Ready for independent project delivery."
                  />
                </div>
              </div>

              <div className="mt-modal-footer">
                <button
                  type="button"
                  className="mt-btn mt-btn--outline"
                  onClick={() => setEndingAssignment(null)}
                  disabled={submittingEnd}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="mt-btn mt-btn--primary"
                  disabled={submittingEnd}
                >
                  {submittingEnd ? "Processing..." : "Confirm Mentorship Conclusion"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
