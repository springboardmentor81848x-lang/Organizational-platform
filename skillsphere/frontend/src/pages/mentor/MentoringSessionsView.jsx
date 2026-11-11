import React, { useState, useEffect } from "react";
import {
  getMyMentees,
  getMentorSessions,
  createMentorSession,
  updateSessionStatus,
  updateSessionNotes,
} from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

const STATUS_FILTERS = [
  { id: "ALL", label: "All Sessions" },
  { id: "SCHEDULED", label: "Upcoming / Scheduled" },
  { id: "COMPLETED", label: "Completed" },
  { id: "CANCELLED", label: "Cancelled" },
];

export default function MentoringSessionsView({ showScheduleModal, onCloseScheduleModal }) {
  const [mentees, setMentees] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selectedMenteeFilter, setSelectedMenteeFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Schedule Modal State
  const [isScheduling, setIsScheduling] = useState(false);
  const [assignmentId, setAssignmentId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [scheduledAt, setScheduledAt] = useState("");
  const [durationMinutes, setDurationMinutes] = useState(45);
  const [meetingLink, setMeetingLink] = useState("");
  const [submittingSchedule, setSubmittingSchedule] = useState(false);

  // Notes Modal State
  const [activeSessionForNotes, setActiveSessionForNotes] = useState(null);
  const [sharedNotes, setSharedNotes] = useState("");
  const [mentorPrepNotes, setMentorPrepNotes] = useState("");
  const [savingNotes, setSavingNotes] = useState(false);

  useEffect(() => {
    if (showScheduleModal) setIsScheduling(true);
  }, [showScheduleModal]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [mList, sList] = await Promise.all([
        getMyMentees("ACTIVE"),
        getMentorSessions({
          menteeId: selectedMenteeFilter ? Number(selectedMenteeFilter) : undefined,
          status: statusFilter !== "ALL" ? statusFilter : undefined,
        }),
      ]);
      setMentees(mList);
      setSessions(sList);
      if (mList.length > 0 && !assignmentId) {
        setAssignmentId(mList[0].assignmentId);
      }
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [statusFilter, selectedMenteeFilter]);

  const handleCreateSession = async (e) => {
    e.preventDefault();
    if (!assignmentId || !title || !scheduledAt) return;
    setSubmittingSchedule(true);
    setError("");
    setSuccess("");
    try {
      await createMentorSession({
        assignmentId: Number(assignmentId),
        title,
        description,
        scheduledAt: new Date(scheduledAt).toISOString(),
        durationMinutes: Number(durationMinutes),
        meetingLink: meetingLink || "https://meet.google.com/skill-sphere",
      });
      setSuccess("Mentoring session scheduled successfully!");
      setIsScheduling(false);
      if (onCloseScheduleModal) onCloseScheduleModal();
      setTitle("");
      setDescription("");
      setScheduledAt("");
      setMeetingLink("");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmittingSchedule(false);
    }
  };

  const handleUpdateStatus = async (sessionId, status) => {
    try {
      await updateSessionStatus(sessionId, status);
      setSuccess(`Session marked as ${status.toLowerCase()}`);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const openNotesModal = (session) => {
    setActiveSessionForNotes(session);
    setSharedNotes(session.sharedNotes || "");
    setMentorPrepNotes(session.mentorPrepNotes || "");
  };

  const handleSaveNotes = async (e) => {
    e.preventDefault();
    if (!activeSessionForNotes) return;
    setSavingNotes(true);
    try {
      await updateSessionNotes(activeSessionForNotes.id, {
        sharedNotes,
        mentorPrepNotes,
      });
      setSuccess("Session notes saved!");
      setActiveSessionForNotes(null);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSavingNotes(false);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Mentoring Sessions</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Coordinate 1-on-1 consultations, record meeting outcomes, and keep confidential preparation notes.
            </p>
          </div>

          <button
            type="button"
            className="mt-btn mt-btn--primary"
            onClick={() => setIsScheduling(true)}
          >
            + Schedule Session
          </button>
        </div>

        {/* Filter Controls */}
        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", alignItems: "center", marginBottom: "16px" }}>
          <select
            className="mt-select"
            style={{ width: "240px" }}
            value={selectedMenteeFilter}
            onChange={(e) => setSelectedMenteeFilter(e.target.value)}
          >
            <option value="">All Mentees</option>
            {mentees.map((m) => (
              <option key={m.employeeId} value={m.employeeId}>
                {m.employeeName}
              </option>
            ))}
          </select>

          <div className="mentor-filter-bar">
            {STATUS_FILTERS.map((f) => (
              <button
                key={f.id}
                type="button"
                className={`mentor-pill-btn ${statusFilter === f.id ? "is-active" : ""}`}
                onClick={() => setStatusFilter(f.id)}
              >
                {f.label}
              </button>
            ))}
          </div>
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}
        {success && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{success}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading mentoring sessions...</p>
        ) : sessions.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
            <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No mentoring sessions found.</p>
            <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>Click "+ Schedule Session" to set up a new meeting.</p>
          </div>
        ) : (
          <div className="mentor-table-wrap">
            <table className="mentor-table">
              <thead>
                <tr>
                  <th>Topic & Mentee</th>
                  <th>Date & Time</th>
                  <th>Duration</th>
                  <th>Status</th>
                  <th>Notes Recorded</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((s) => {
                  const badgeClass =
                    s.status === "COMPLETED"
                      ? "mt-badge--active"
                      : s.status === "SCHEDULED"
                      ? "mt-badge--pending"
                      : "mt-badge--declined";

                  return (
                    <tr key={s.id}>
                      <td>
                        <strong>{s.title}</strong>
                        <div style={{ fontSize: "0.78rem", color: "var(--mt-faint)" }}>with {s.employeeName}</div>
                      </td>
                      <td>
                        <div>{new Date(s.scheduledAt).toLocaleDateString()}</div>
                        <small style={{ color: "var(--mt-faint)" }}>
                          {new Date(s.scheduledAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                        </small>
                      </td>
                      <td>{s.durationMinutes} mins</td>
                      <td>
                        <span className={`mt-badge ${badgeClass}`}>{s.status}</span>
                      </td>
                      <td>
                        <span style={{ fontSize: "0.82rem", color: s.sharedNotes || s.mentorPrepNotes ? "var(--mt-ok)" : "var(--mt-faint)" }}>
                          {s.sharedNotes || s.mentorPrepNotes ? "Notes added" : "No notes yet"}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "6px", alignItems: "center" }}>
                          {s.meetingLink && s.status === "SCHEDULED" && (
                            <a
                              href={s.meetingLink}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="mt-btn mt-btn--primary"
                              style={{ fontSize: "0.78rem", padding: "5px 10px" }}
                            >
                              Join Meeting
                            </a>
                          )}

                          <button
                            type="button"
                            className="mt-btn mt-btn--outline"
                            style={{ fontSize: "0.78rem", padding: "5px 10px" }}
                            onClick={() => openNotesModal(s)}
                          >
                            Notes
                          </button>

                          {s.status === "SCHEDULED" && (
                            <button
                              type="button"
                              className="mt-btn mt-btn--outline"
                              style={{ fontSize: "0.78rem", padding: "5px 10px", color: "var(--mt-ok)" }}
                              onClick={() => handleUpdateStatus(s.id, "COMPLETED")}
                            >
                              Complete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Schedule Modal */}
      {isScheduling && (
        <div className="mt-modal-backdrop" onClick={() => setIsScheduling(false)}>
          <div className="mt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>Schedule Mentoring Session</h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setIsScheduling(false)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateSession}>
              <div className="mt-modal-body">
                <div className="mt-form-group">
                  <label className="mt-label">Mentee Assignment *</label>
                  <select
                    className="mt-select"
                    required
                    value={assignmentId}
                    onChange={(e) => setAssignmentId(e.target.value)}
                  >
                    {mentees.map((m) => (
                      <option key={m.assignmentId} value={m.assignmentId}>
                        {m.employeeName} ({m.jobRole})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Session Topic *</label>
                  <input
                    type="text"
                    required
                    className="mt-input"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="e.g. System Design Consultation & Database Indexing"
                  />
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Agenda / Description</label>
                  <textarea
                    rows={3}
                    className="mt-textarea"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Key topics to discuss, prerequisites, or code to review..."
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                  <div className="mt-form-group">
                    <label className="mt-label">Date & Time *</label>
                    <input
                      type="datetime-local"
                      required
                      className="mt-input"
                      value={scheduledAt}
                      onChange={(e) => setScheduledAt(e.target.value)}
                    />
                  </div>

                  <div className="mt-form-group">
                    <label className="mt-label">Duration (Minutes)</label>
                    <input
                      type="number"
                      min={15}
                      max={240}
                      className="mt-input"
                      value={durationMinutes}
                      onChange={(e) => setDurationMinutes(e.target.value)}
                    />
                  </div>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Virtual Meeting Link</label>
                  <input
                    type="url"
                    className="mt-input"
                    value={meetingLink}
                    onChange={(e) => setMeetingLink(e.target.value)}
                    placeholder="https://meet.google.com/..."
                  />
                </div>
              </div>

              <div className="mt-modal-footer">
                <button
                  type="button"
                  className="mt-btn mt-btn--outline"
                  onClick={() => setIsScheduling(false)}
                  disabled={submittingSchedule}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="mt-btn mt-btn--primary"
                  disabled={submittingSchedule}
                >
                  {submittingSchedule ? "Scheduling..." : "Confirm Schedule"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Notes Modal */}
      {activeSessionForNotes && (
        <div className="mt-modal-backdrop" onClick={() => setActiveSessionForNotes(null)}>
          <div className="mt-modal mt-modal--wide" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>Session Notes &mdash; {activeSessionForNotes.title}</h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setActiveSessionForNotes(null)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSaveNotes}>
              <div className="mt-modal-body">
                <div className="mt-form-group">
                  <label className="mt-label">Shared Meeting Notes (Visible to Mentee)</label>
                  <textarea
                    rows={4}
                    className="mt-textarea"
                    value={sharedNotes}
                    onChange={(e) => setSharedNotes(e.target.value)}
                    placeholder="Meeting summaries, key takeaways, recommended action items..."
                  />
                  <small style={{ color: "var(--mt-soft)" }}>These notes are shared transparently with {activeSessionForNotes.employeeName}.</small>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label" style={{ color: "#7e22ce" }}>
                    Private Mentor Prep & Observations Notes (Confidential)
                  </label>
                  <textarea
                    rows={4}
                    className="mt-textarea"
                    style={{ borderColor: "#d8b4fe" }}
                    value={mentorPrepNotes}
                    onChange={(e) => setMentorPrepNotes(e.target.value)}
                    placeholder="Confidential observations, potential knowledge bottlenecks, coaching notes..."
                  />
                  <small style={{ color: "var(--mt-soft)" }}>
                    Strictly private to the mentor. Never rendered on mentee-facing screens or APIs.
                  </small>
                </div>
              </div>

              <div className="mt-modal-footer">
                <button
                  type="button"
                  className="mt-btn mt-btn--outline"
                  onClick={() => setActiveSessionForNotes(null)}
                  disabled={savingNotes}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="mt-btn mt-btn--primary"
                  disabled={savingNotes}
                >
                  {savingNotes ? "Saving Notes..." : "Save Notes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
