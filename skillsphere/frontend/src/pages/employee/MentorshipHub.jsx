import { useCallback, useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import Spinner from "../../components/Spinner.jsx";
import { parseApiError } from "../../services/authService.js";
import {
  createMentorshipGoal,
  createMentorshipSession,
  getExpertDirectory,
  getMenteeProgress,
  getMentorStatus,
  getMyGoals,
  getMyMentees,
  getMyMentor,
  getMySessions,
  submitSessionFeedback,
  updateGoalStatus,
  updateSessionStatus,
  createMentorshipRequest,
  getMySentMentorshipRequests,
  cancelMentorshipRequest,
} from "../../services/mentorshipService.js";

const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "—");
const fmtShortDate = (iso) => (iso ? new Date(iso).toLocaleDateString([], { dateStyle: "medium" }) : "—");

export default function MentorshipHub({ initialTab = "my-mentorship" }) {
  const [tab, setTab] = useState(initialTab); // "my-mentorship" | "directory" | "requests" | "mentor-space"
  const [mentorStatus, setMentorStatus] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    if (initialTab) setTab(initialTab);
  }, [initialTab]);

  // Check mentor status
  useEffect(() => {
    getMentorStatus()
      .then(setMentorStatus)
      .catch(() => setMentorStatus({ isApprovedMentor: false }));
  }, []);

  const clearMessages = () => {
    setError("");
    setSuccess("");
  };

  return (
    <div className="mentorship-hub">
      <div className="page-header">
        <div>
          <p className="eyebrow">COMMUNITY & GROWTH</p>
          <h1>Knowledge Sharing & Peer Mentorship</h1>
          <p>Connect with organizational experts, work with mentors, and track development goals.</p>
        </div>
      </div>

      <Alert type="error">{error}</Alert>
      <Alert type="success">{success}</Alert>

      <div className="tabs" role="tablist" aria-label="Mentorship Views">
        <button
          className={`tab-btn ${tab === "my-mentorship" ? "is-active" : ""}`}
          onClick={() => { setTab("my-mentorship"); clearMessages(); }}
          role="tab"
          aria-selected={tab === "my-mentorship"}
        >
          <Icon name="user" size={17} /> My Mentorship
        </button>

        <button
          className={`tab-btn ${tab === "directory" ? "is-active" : ""}`}
          onClick={() => { setTab("directory"); clearMessages(); }}
          role="tab"
          aria-selected={tab === "directory"}
        >
          <Icon name="users" size={17} /> Expert Directory
        </button>

        <button
          className={`tab-btn ${tab === "requests" ? "is-active" : ""}`}
          onClick={() => { setTab("requests"); clearMessages(); }}
          role="tab"
          aria-selected={tab === "requests"}
        >
          <Icon name="clipboard" size={17} /> Mentorship Requests
        </button>

        <button
          className={`tab-btn ${tab === "mentor-space" ? "is-active" : ""}`}
          onClick={() => { setTab("mentor-space"); clearMessages(); }}
          role="tab"
          aria-selected={tab === "mentor-space"}
        >
          <Icon name="sparkles" size={17} /> Mentor Workspace
          {mentorStatus?.isApprovedMentor && mentorStatus.isActive && (
            <span className="badge badge--success" style={{ marginLeft: "6px" }}>Active Mentor</span>
          )}
        </button>
      </div>

      <div className="tab-content" style={{ marginTop: "1rem" }}>
        {tab === "my-mentorship" && (
          <MyMentorshipView
            onGoToDirectory={() => setTab("directory")}
            setError={setError}
            setSuccess={setSuccess}
          />
        )}

        {tab === "directory" && (
          <ExpertDirectoryView
            setError={setError}
            setSuccess={setSuccess}
            onGoToRequests={() => setTab("requests")}
          />
        )}

        {tab === "requests" && (
          <MentorshipRequestsView
            setError={setError}
            setSuccess={setSuccess}
            onGoToDirectory={() => setTab("directory")}
          />
        )}

        {tab === "mentor-space" && (
          <MentorWorkspaceView
            mentorStatus={mentorStatus}
            setError={setError}
            setSuccess={setSuccess}
          />
        )}
      </div>
    </div>
  );
}

/* =====================================================================
   TAB 1: MY MENTORSHIP (EMPLOYEE VIEW)
   ===================================================================== */
function MyMentorshipView({ onGoToDirectory, setError, setSuccess }) {
  const [loading, setLoading] = useState(true);
  const [mentorData, setMentorData] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [goals, setGoals] = useState([]);
  const [feedbackSession, setFeedbackSession] = useState(null);
  const [rating, setRating] = useState(5);
  const [comments, setComments] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadData = useCallback(() => {
    setLoading(true);
    Promise.all([getMyMentor(), getMySessions(), getMyGoals()])
      .then(([m, s, g]) => {
        setMentorData(m);
        setSessions(s || []);
        setGoals(g || []);
      })
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }, [setError]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleSubmitFeedback = async (e) => {
    e.preventDefault();
    if (!comments.trim()) {
      setError("Please write comments for your session feedback.");
      return;
    }
    setSubmitting(true);
    try {
      await submitSessionFeedback(feedbackSession.id, { rating: Number(rating), comments: comments.trim() });
      setSuccess("Session feedback submitted successfully!");
      setFeedbackSession(null);
      setComments("");
      setRating(5);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="panel" style={{ textAlign: "center", padding: "3rem" }}>
        <Spinner /> Loading your mentorship details…
      </div>
    );
  }

  if (!mentorData) {
    return (
      <section className="panel empty-state">
        <span className="empty-state__icon"><Icon name="users" size={32} /></span>
        <h2>No Active Mentor Assigned</h2>
        <p>
          You do not currently have a peer mentor assigned by an administrator.
          Browse our organizational Expert Directory to discover mentors in your skill area.
        </p>
        <button className="btn btn--primary" onClick={onGoToDirectory}>
          Explore Expert Directory <Icon name="arrow" size={16} />
        </button>
      </section>
    );
  }

  return (
    <div className="mentorship-grid">
      {/* Mentor Profile Overview */}
      <section className="panel mentor-summary-card">
        <div className="mentor-card-header">
          <div className="mentor-avatar-lg">
            {(mentorData.mentorName || "M").charAt(0).toUpperCase()}
          </div>
          <div>
            <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
              <h2 style={{ margin: 0 }}>{mentorData.mentorName}</h2>
              <span className="badge badge--success">{mentorData.status}</span>
            </div>
            <p className="eyebrow" style={{ marginTop: "4px" }}>
              {mentorData.jobRole} · {mentorData.department}
            </p>
            <p className="mentor-headline">"{mentorData.headline}"</p>
          </div>
        </div>

        <div className="mentor-meta-grid" style={{ marginTop: "1rem" }}>
          <div>
            <small className="muted">Contact Email</small>
            <div><strong>{mentorData.mentorEmail}</strong></div>
          </div>
          <div>
            <small className="muted">Assigned Since</small>
            <div><strong>{fmtShortDate(mentorData.assignedAt)}</strong></div>
          </div>
        </div>

        {mentorData.expertiseAreas?.length > 0 && (
          <div style={{ marginTop: "1rem" }}>
            <small className="muted">Areas of Expertise</small>
            <div className="chips" style={{ marginTop: "6px" }}>
              {mentorData.expertiseAreas.map((exp) => (
                <span className="chip" key={exp}>{exp}</span>
              ))}
            </div>
          </div>
        )}

        {mentorData.notes && (
          <div style={{ marginTop: "1rem", background: "var(--color-bg)", padding: "10px", borderRadius: "6px" }}>
            <small className="muted">Administrator Note:</small>
            <p style={{ margin: "4px 0 0", fontSize: "0.9rem" }}>{mentorData.notes}</p>
          </div>
        )}
      </section>

      {/* Development Goals Section */}
      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">TARGETS & MILESTONES</p>
            <h2>My Development Goals</h2>
          </div>
          <span className="badge">{goals.length} Goals</span>
        </div>

        {goals.length === 0 ? (
          <p className="muted" style={{ padding: "1rem 0" }}>
            No development goals have been assigned yet. Your mentor can set up structured goals with you during sessions.
          </p>
        ) : (
          <div className="goals-list">
            {goals.map((goal) => (
              <div key={goal.id} className="goal-item">
                <div style={{ flex: 1 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <strong>{goal.title}</strong>
                    <span className={`badge ${goal.status === "ACHIEVED" ? "badge--success" : goal.status === "ABANDONED" ? "badge--danger" : "badge--warn"}`}>
                      {goal.status.replace("_", " ")}
                    </span>
                  </div>
                  {goal.description && <p style={{ margin: "4px 0 0", color: "var(--color-muted)" }}>{goal.description}</p>}
                  {goal.targetDate && (
                    <small className="muted">Target Completion: {fmtShortDate(goal.targetDate)}</small>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Mentorship Sessions Section */}
      <section className="panel" style={{ gridColumn: "1 / -1" }}>
        <div className="section-heading">
          <div>
            <p className="eyebrow">MEETINGS & FEEDBACK</p>
            <h2>Mentorship Sessions</h2>
          </div>
          <span className="badge">{sessions.length} Sessions</span>
        </div>

        {sessions.length === 0 ? (
          <p className="muted" style={{ padding: "1.5rem 0" }}>
            No mentorship sessions scheduled yet. Your mentor will schedule 1-on-1 sessions with you.
          </p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Topic & Agenda</th>
                  <th>Scheduled Date & Time</th>
                  <th>Duration</th>
                  <th>Status</th>
                  <th>Meeting Link</th>
                  <th>Feedback / Action</th>
                </tr>
              </thead>
              <tbody>
                {sessions.map((sess) => (
                  <tr key={sess.id}>
                    <td>
                      <strong>{sess.title}</strong>
                      {sess.description && <small style={{ display: "block", color: "var(--color-muted)" }}>{sess.description}</small>}
                    </td>
                    <td>{fmtDate(sess.scheduledAt)}</td>
                    <td>{sess.durationMinutes} mins</td>
                    <td>
                      <span className={`badge ${sess.status === "COMPLETED" ? "badge--success" : sess.status === "CANCELLED" ? "badge--danger" : "badge--warn"}`}>
                        {sess.status}
                      </span>
                    </td>
                    <td>
                      {sess.meetingLink ? (
                        <a href={sess.meetingLink} target="_blank" rel="noopener noreferrer" className="link">
                          Join Link <Icon name="external" size={13} />
                        </a>
                      ) : (
                        <span className="muted">—</span>
                      )}
                    </td>
                    <td>
                      {sess.status === "COMPLETED" && (
                        sess.feedback ? (
                          <span className="badge badge--success" title={sess.feedback.comments}>
                            ★ {sess.feedback.rating}/5 Rated
                          </span>
                        ) : (
                          <button className="btn btn--ghost" style={{ padding: "4px 8px", fontSize: "0.85rem" }} onClick={() => setFeedbackSession(sess)}>
                            Submit Feedback
                          </button>
                        )
                      )}
                      {sess.status !== "COMPLETED" && <span className="muted">—</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Feedback Modal / Card */}
        {feedbackSession && (
          <div className="modal-backdrop">
            <div className="panel modal-card" style={{ maxWidth: "480px" }}>
              <h3>Session Feedback</h3>
              <p className="muted" style={{ marginTop: "4px" }}>
                Submit your review for session: <strong>{feedbackSession.title}</strong>
              </p>

              <form onSubmit={handleSubmitFeedback} className="form" style={{ marginTop: "1rem" }}>
                <label className="field">
                  <span>Rating (1 = Needs Work, 5 = Excellent)</span>
                  <select value={rating} onChange={(e) => setRating(e.target.value)}>
                    <option value={5}>5 Stars - Excellent Session</option>
                    <option value={4}>4 Stars - Very Helpful</option>
                    <option value={3}>3 Stars - Good Discussion</option>
                    <option value={2}>2 Stars - Average</option>
                    <option value={1}>1 Star - Needs Improvement</option>
                  </select>
                </label>

                <label className="field">
                  <span>Comments & Takeaways</span>
                  <textarea
                    rows={4}
                    value={comments}
                    onChange={(e) => setComments(e.target.value)}
                    placeholder="Share key learnings, takeaways, or suggestions..."
                    required
                  />
                </label>

                <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "1rem" }}>
                  <button type="button" className="btn btn--ghost" onClick={() => setFeedbackSession(null)}>
                    Cancel
                  </button>
                  <button type="submit" className="btn btn--primary" disabled={submitting}>
                    {submitting ? "Submitting…" : "Save Feedback"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

/* =====================================================================
   TAB 2: EXPERT DIRECTORY VIEW
   ===================================================================== */
function ExpertDirectoryView({ setError, setSuccess, onGoToRequests }) {
  const [mentors, setMentors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [deptFilter, setDeptFilter] = useState("");

  // Request mentorship modal state
  const [requestingMentor, setRequestingMentor] = useState(null);
  const [requestMessage, setRequestMessage] = useState("");
  const [sendingRequest, setSendingRequest] = useState(false);

  const fetchDirectory = useCallback(() => {
    setLoading(true);
    getExpertDirectory({ query: search, department: deptFilter })
      .then(setMentors)
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }, [search, deptFilter, setError]);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchDirectory();
    }, 250);
    return () => clearTimeout(timer);
  }, [fetchDirectory]);

  const handleSendRequest = async (e) => {
    e.preventDefault();
    if (!requestingMentor || !requestMessage.trim()) return;
    setSendingRequest(true);
    setError("");
    try {
      await createMentorshipRequest({
        mentorId: requestingMentor.userId,
        message: requestMessage.trim(),
      });
      if (setSuccess) {
        setSuccess(`Mentorship request successfully sent to ${requestingMentor.fullName}!`);
      }
      setRequestingMentor(null);
      setRequestMessage("");
      if (onGoToRequests) onGoToRequests();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSendingRequest(false);
    }
  };

  return (
    <div>
      <section className="panel" style={{ marginBottom: "1.5rem" }}>
        <div className="directory-filters">
          <label className="field" style={{ flex: 2 }}>
            <span>Search by Mentor Name or Expertise</span>
            <input
              type="text"
              placeholder="e.g. Java, Cloud, Data, Jordan..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </label>

          <label className="field" style={{ flex: 1 }}>
            <span>Filter by Department</span>
            <select value={deptFilter} onChange={(e) => setDeptFilter(e.target.value)}>
              <option value="">All Departments</option>
              <option value="Engineering">Engineering</option>
              <option value="Quality Assurance">Quality Assurance</option>
              <option value="Data & Analytics">Data & Analytics</option>
              <option value="Infrastructure">Infrastructure</option>
            </select>
          </label>
        </div>
      </section>

      {loading ? (
        <div className="panel" style={{ textAlign: "center", padding: "3rem" }}>
          <Spinner /> Loading expert directory…
        </div>
      ) : mentors.length === 0 ? (
        <section className="panel empty-state">
          <span className="empty-state__icon"><Icon name="users" size={32} /></span>
          <h2>No Mentors Found</h2>
          <p>No active mentors match your search criteria or none have been approved by administrators yet.</p>
        </section>
      ) : (
        <div className="expert-grid">
          {mentors.map((m) => (
            <article key={m.mentorProfileId} className="panel expert-card">
              <div className="expert-card-top">
                <div className="mentor-avatar-md">
                  {(m.fullName || "M").charAt(0).toUpperCase()}
                </div>
                <div style={{ flex: 1 }}>
                  <h3 style={{ margin: 0, fontSize: "1.1rem" }}>{m.fullName}</h3>
                  <small className="muted">{m.jobRole} · {m.department}</small>
                </div>
                <span className={`badge ${m.isAvailable ? "badge--success" : "badge--warn"}`}>
                  {m.isAvailable ? "Available" : "At Capacity"}
                </span>
              </div>

              <p className="expert-headline">"{m.headline}"</p>

              {m.bio && <p className="expert-bio">{m.bio}</p>}

              <div style={{ marginTop: "auto", paddingTop: "0.75rem" }}>
                <small className="muted" style={{ display: "block", marginBottom: "4px" }}>
                  Expertise Areas:
                </small>
                <div className="chips">
                  {m.expertiseAreas.map((exp) => (
                    <span className="chip" key={exp}>{exp}</span>
                  ))}
                </div>
              </div>

              <div className="expert-footer" style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "12px", borderTop: "1px solid #eef2f6", paddingTop: "10px" }}>
                <div>
                  <small className="muted" style={{ display: "block" }}>Capacity: {m.currentMenteesCount} / {m.maxMentees} mentees</small>
                  <small className="muted">{m.email}</small>
                </div>
                {m.isAvailable && (
                  <button
                    type="button"
                    className="btn btn--primary"
                    style={{ fontSize: "0.82rem", padding: "6px 12px" }}
                    onClick={() => {
                      setRequestingMentor(m);
                      setRequestMessage(`Hello ${m.fullName.split(" ")[0]}, I would like to request 1-on-1 mentorship to help strengthen my technical capabilities and development goals.`);
                    }}
                  >
                    Request Mentorship &rarr;
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}

      {/* Request Mentorship Modal */}
      {requestingMentor && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(11,37,69,0.45)",
            backdropFilter: "blur(4px)",
            display: "grid",
            placeItems: "center",
            zIndex: 999,
            padding: "20px",
          }}
        >
          <div className="panel" style={{ width: "100%", maxWidth: "480px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
              <h2 style={{ margin: 0, fontSize: "1.1rem" }}>Request Mentorship: {requestingMentor.fullName}</h2>
              <button type="button" className="btn btn--ghost" onClick={() => setRequestingMentor(null)} style={{ padding: "4px 8px" }}>
                <Icon name="x" size={18} />
              </button>
            </div>
            <form onSubmit={handleSendRequest} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <p style={{ fontSize: "0.88rem", color: "#64748b", margin: 0 }}>
                {requestingMentor.jobRole} · {requestingMentor.department}
              </p>
              <div>
                <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>
                  Your Message to the Mentor
                </label>
                <textarea
                  rows={4}
                  required
                  value={requestMessage}
                  onChange={(e) => setRequestMessage(e.target.value)}
                  placeholder="Introduce yourself and outline your learning goals..."
                  style={{ width: "100%", padding: "10px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "8px" }}>
                <button type="button" className="btn btn--ghost" onClick={() => setRequestingMentor(null)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn--primary" disabled={sendingRequest}>
                  {sendingRequest ? "Sending…" : "Send Request"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

/* =====================================================================
   TAB: MENTORSHIP REQUESTS VIEW (EMPLOYEE SENT REQUESTS)
   ===================================================================== */
function MentorshipRequestsView({ setError, setSuccess, onGoToDirectory }) {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchRequests = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getMySentMentorshipRequests();
      setRequests(data);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }, [setError]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleCancel = async (id) => {
    if (!window.confirm("Are you sure you want to cancel this mentorship request?")) return;
    if (setError) setError("");
    if (setSuccess) setSuccess("");
    try {
      await cancelMentorshipRequest(id);
      if (setSuccess) setSuccess("Mentorship request cancelled.");
      fetchRequests();
    } catch (err) {
      if (setError) setError(parseApiError(err).message);
    }
  };

  if (loading) {
    return (
      <div className="panel" style={{ textAlign: "center", padding: "3rem" }}>
        <Spinner /> Loading your mentorship requests…
      </div>
    );
  }

  if (requests.length === 0) {
    return (
      <section className="panel empty-state">
        <span className="empty-state__icon"><Icon name="users" size={32} /></span>
        <h2>No Mentorship Requests Sent</h2>
        <p>You haven't requested mentorship from any experts yet. Browse the expert directory to find a mentor.</p>
        <button className="btn btn--primary" onClick={onGoToDirectory} style={{ marginTop: "1rem" }}>
          Browse Mentors <Icon name="arrow" size={16} />
        </button>
      </section>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
      <div className="section-heading">
        <div>
          <h2>Sent Mentorship Requests ({requests.length})</h2>
          <p className="muted">Track the review status of your direct 1-on-1 mentorship requests.</p>
        </div>
      </div>
      <div className="table-wrap panel">
        <table>
          <thead>
            <tr>
              <th>Mentor</th>
              <th>Your Message</th>
              <th>Status</th>
              <th>Sent Date</th>
              <th>Response Note</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {requests.map((r) => (
              <tr key={r.id}>
                <td>
                  <strong>{r.mentorName}</strong>
                  <div style={{ fontSize: "0.82rem", color: "#64748b" }}>{r.mentorHeadline}</div>
                </td>
                <td><div style={{ maxWidth: "280px", whiteSpace: "pre-wrap", fontSize: "0.85rem" }}>{r.message}</div></td>
                <td>
                  <span
                    className="badge"
                    style={{
                      background: r.status === "ACCEPTED" ? "#e6f6ee" : (r.status === "REJECTED" ? "#fde8e8" : (r.status === "CANCELLED" ? "#f1f5f9" : "#fff4d6")),
                      color: r.status === "ACCEPTED" ? "#17754f" : (r.status === "REJECTED" ? "#b91c1c" : (r.status === "CANCELLED" ? "#64748b" : "#8a5a00")),
                    }}
                  >
                    {r.status}
                  </span>
                </td>
                <td>{r.createdAt ? new Date(r.createdAt).toLocaleDateString() : "—"}</td>
                <td>
                  <div style={{ fontSize: "0.85rem", color: r.responseNote ? "#1e293b" : "#94a3b8" }}>
                    {r.responseNote || "Awaiting mentor review"}
                  </div>
                </td>
                <td>
                  {r.status === "PENDING" ? (
                    <button
                      type="button"
                      className="btn btn--ghost"
                      onClick={() => handleCancel(r.id)}
                      style={{ fontSize: "0.78rem", padding: "4px 8px" }}
                    >
                      Cancel Request
                    </button>
                  ) : (
                    <span style={{ fontSize: "0.8rem", color: "#94a3b8" }}>—</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

/* =====================================================================
   TAB 3: MENTOR WORKSPACE VIEW (FOR APPROVED MENTORS)
   ===================================================================== */
function MentorWorkspaceView({ mentorStatus, setError, setSuccess }) {
  const [mentees, setMentees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedMentee, setSelectedMentee] = useState(null);
  const [menteeProgress, setMenteeProgress] = useState(null);

  // Modals
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [showGoalModal, setShowGoalModal] = useState(false);

  // Session form
  const [sessionForm, setSessionForm] = useState({
    assignmentId: "",
    title: "",
    description: "",
    scheduledAt: "",
    durationMinutes: 45,
    meetingLink: "",
  });

  // Goal form
  const [goalForm, setGoalForm] = useState({
    assignmentId: "",
    title: "",
    description: "",
    targetDate: "",
  });

  const loadMentees = useCallback(() => {
    if (!mentorStatus?.isApprovedMentor) {
      setLoading(false);
      return;
    }
    setLoading(true);
    getMyMentees()
      .then((data) => {
        setMentees(data);
        if (data.length > 0 && !selectedMentee) {
          setSelectedMentee(data[0]);
        }
      })
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }, [mentorStatus, selectedMentee, setError]);

  useEffect(() => {
    loadMentees();
  }, [loadMentees]);

  // Load progress when selected mentee changes
  useEffect(() => {
    if (selectedMentee) {
      getMenteeProgress(selectedMentee.employeeUserId)
        .then(setMenteeProgress)
        .catch((err) => setError(parseApiError(err).message));
    } else {
      setMenteeProgress(null);
    }
  }, [selectedMentee, setError]);

  const handleCreateSession = async (e) => {
    e.preventDefault();
    try {
      await createMentorshipSession({
        assignmentId: Number(sessionForm.assignmentId),
        title: sessionForm.title.trim(),
        description: sessionForm.description.trim(),
        scheduledAt: new Date(sessionForm.scheduledAt).toISOString(),
        durationMinutes: Number(sessionForm.durationMinutes),
        meetingLink: sessionForm.meetingLink.trim() || null,
      });
      setSuccess("Session scheduled successfully!");
      setShowSessionModal(false);
      setSessionForm({ assignmentId: "", title: "", description: "", scheduledAt: "", durationMinutes: 45, meetingLink: "" });
      if (selectedMentee) {
        getMenteeProgress(selectedMentee.employeeUserId).then(setMenteeProgress);
      }
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleCreateGoal = async (e) => {
    e.preventDefault();
    try {
      await createMentorshipGoal({
        assignmentId: Number(goalForm.assignmentId),
        title: goalForm.title.trim(),
        description: goalForm.description.trim(),
        targetDate: goalForm.targetDate || null,
      });
      setSuccess("Development goal created successfully!");
      setShowGoalModal(false);
      setGoalForm({ assignmentId: "", title: "", description: "", targetDate: "" });
      if (selectedMentee) {
        getMenteeProgress(selectedMentee.employeeUserId).then(setMenteeProgress);
      }
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleUpdateSessionStatus = async (sessionId, status) => {
    try {
      await updateSessionStatus(sessionId, status);
      setSuccess(`Session marked as ${status}!`);
      if (selectedMentee) {
        getMenteeProgress(selectedMentee.employeeUserId).then(setMenteeProgress);
      }
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleUpdateGoalStatus = async (goalId, status) => {
    try {
      await updateGoalStatus(goalId, status);
      setSuccess(`Goal status updated to ${status}!`);
      if (selectedMentee) {
        getMenteeProgress(selectedMentee.employeeUserId).then(setMenteeProgress);
      }
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  if (!mentorStatus?.isApprovedMentor) {
    return (
      <section className="panel empty-state">
        <span className="empty-state__icon"><Icon name="sparkles" size={32} /></span>
        <h2>Mentor Status Required</h2>
        <p>
          You are currently signed in as an Employee. Mentoring privileges are granted by SkillSphere administrators.
          Once approved, you will be able to guide assigned mentees, schedule sessions, and track development goals from this workspace.
        </p>
      </section>
    );
  }

  if (!mentorStatus.isActive) {
    return (
      <section className="panel empty-state">
        <span className="empty-state__icon"><Icon name="alert" size={32} /></span>
        <h2>Mentor Profile Inactive</h2>
        <p>Your mentor profile has been deactivated by an administrator. Contact an administrator to reactivate your mentoring status.</p>
      </section>
    );
  }

  return (
    <div>
      <section className="panel" style={{ marginBottom: "1.5rem" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <div>
            <h2 style={{ margin: 0 }}>Active Mentees ({mentees.length} of {mentorStatus.maxMentees} capacity)</h2>
            <p className="muted" style={{ margin: "4px 0 0" }}>Manage your assigned employees, sessions, and developmental milestones.</p>
          </div>
          {selectedMentee && (
            <div style={{ display: "flex", gap: "8px" }}>
              <button
                className="btn btn--primary"
                onClick={() => {
                  setSessionForm((f) => ({ ...f, assignmentId: selectedMentee.assignmentId }));
                  setShowSessionModal(true);
                }}
              >
                <Icon name="clock" size={15} /> Schedule Session
              </button>
              <button
                className="btn btn--ghost"
                onClick={() => {
                  setGoalForm((f) => ({ ...f, assignmentId: selectedMentee.assignmentId }));
                  setShowGoalModal(true);
                }}
              >
                <Icon name="target" size={15} /> Add Goal
              </button>
            </div>
          )}
        </div>

        {loading ? (
          <div style={{ textAlign: "center", padding: "2rem" }}><Spinner /> Loading mentees…</div>
        ) : mentees.length === 0 ? (
          <p className="muted" style={{ marginTop: "1rem" }}>
            You have no active mentees assigned at this time. Administrators assign mentees based on organizational matching.
          </p>
        ) : (
          <div className="mentee-selector-bar" style={{ marginTop: "1rem" }}>
            {mentees.map((m) => (
              <button
                key={m.assignmentId}
                className={`mentee-tab ${selectedMentee?.assignmentId === m.assignmentId ? "is-selected" : ""}`}
                onClick={() => setSelectedMentee(m)}
              >
                <strong>{m.fullName}</strong>
                <small>{m.jobRole} · {m.department || "General"}</small>
              </button>
            ))}
          </div>
        )}
      </section>

      {selectedMentee && menteeProgress && (
        <div className="mentee-detail-grid">
          {/* Mentee Overview Card */}
          <section className="panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">MENTEE PROFILE</p>
                <h2>{selectedMentee.fullName}</h2>
              </div>
              <span className="badge badge--success">Active Mentee</span>
            </div>
            <div className="mentor-meta-grid">
              <div><small className="muted">Email</small><div><strong>{selectedMentee.email}</strong></div></div>
              <div><small className="muted">Employee ID</small><div><strong>{selectedMentee.employeeId || "—"}</strong></div></div>
              <div><small className="muted">Job Role</small><div><strong>{selectedMentee.jobRole}</strong></div></div>
              <div><small className="muted">Department</small><div><strong>{selectedMentee.department || "General"}</strong></div></div>
            </div>
            {selectedMentee.notes && (
              <div style={{ marginTop: "1rem", background: "var(--color-bg)", padding: "10px", borderRadius: "6px" }}>
                <small className="muted">Assignment Notes:</small>
                <p style={{ margin: "4px 0 0", fontSize: "0.9rem" }}>{selectedMentee.notes}</p>
              </div>
            )}
          </section>

          {/* Mentee Goals */}
          <section className="panel">
            <div className="section-heading">
              <div>
                <p className="eyebrow">MILESTONES</p>
                <h2>Assigned Goals</h2>
              </div>
              <span className="badge">{menteeProgress.goals.length}</span>
            </div>

            {menteeProgress.goals.length === 0 ? (
              <p className="muted">No goals created yet. Click "Add Goal" to establish learning targets.</p>
            ) : (
              <div className="goals-list">
                {menteeProgress.goals.map((g) => (
                  <div key={g.id} className="goal-item" style={{ flexDirection: "column", gap: "6px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <strong>{g.title}</strong>
                      <div style={{ display: "flex", gap: "6px" }}>
                        <button
                          className={`btn ${g.status === "ACHIEVED" ? "btn--complete" : "btn--ghost"}`}
                          style={{ padding: "2px 8px", fontSize: "0.8rem" }}
                          onClick={() => handleUpdateGoalStatus(g.id, g.status === "ACHIEVED" ? "IN_PROGRESS" : "ACHIEVED")}
                        >
                          {g.status === "ACHIEVED" ? "✓ Achieved" : "Mark Achieved"}
                        </button>
                        {g.status !== "ABANDONED" && (
                          <button
                            className="btn btn--ghost"
                            style={{ padding: "2px 8px", fontSize: "0.8rem", color: "var(--color-danger)" }}
                            onClick={() => handleUpdateGoalStatus(g.id, "ABANDONED")}
                          >
                            Abandon
                          </button>
                        )}
                      </div>
                    </div>
                    {g.description && <p style={{ margin: 0, fontSize: "0.85rem", color: "var(--color-muted)" }}>{g.description}</p>}
                    {g.targetDate && <small className="muted">Target: {fmtShortDate(g.targetDate)}</small>}
                  </div>
                ))}
              </div>
            )}
          </section>

          {/* Mentee Sessions */}
          <section className="panel" style={{ gridColumn: "1 / -1" }}>
            <div className="section-heading">
              <div>
                <p className="eyebrow">1-ON-1 SCHEDULE</p>
                <h2>Sessions with {selectedMentee.fullName}</h2>
              </div>
              <span className="badge">{menteeProgress.sessions.length}</span>
            </div>

            {menteeProgress.sessions.length === 0 ? (
              <p className="muted">No sessions scheduled yet.</p>
            ) : (
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Title</th>
                      <th>Date & Time</th>
                      <th>Duration</th>
                      <th>Status</th>
                      <th>Meeting Link</th>
                      <th>Mentee Feedback</th>
                      <th>Status Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {menteeProgress.sessions.map((sess) => (
                      <tr key={sess.id}>
                        <td>
                          <strong>{sess.title}</strong>
                          {sess.description && <small style={{ display: "block", color: "var(--color-muted)" }}>{sess.description}</small>}
                        </td>
                        <td>{fmtDate(sess.scheduledAt)}</td>
                        <td>{sess.durationMinutes} mins</td>
                        <td>
                          <span className={`badge ${sess.status === "COMPLETED" ? "badge--success" : sess.status === "CANCELLED" ? "badge--danger" : "badge--warn"}`}>
                            {sess.status}
                          </span>
                        </td>
                        <td>
                          {sess.meetingLink ? (
                            <a href={sess.meetingLink} target="_blank" rel="noopener noreferrer" className="link">
                              Link <Icon name="external" size={13} />
                            </a>
                          ) : "—"}
                        </td>
                        <td>
                          {sess.feedback ? (
                            <span className="badge badge--success">★ {sess.feedback.rating}/5 ("{sess.feedback.comments}")</span>
                          ) : (
                            <span className="muted">Pending review</span>
                          )}
                        </td>
                        <td>
                          {sess.status === "SCHEDULED" && (
                            <div style={{ display: "flex", gap: "4px" }}>
                              <button
                                className="btn btn--primary"
                                style={{ padding: "4px 8px", fontSize: "0.8rem" }}
                                onClick={() => handleUpdateSessionStatus(sess.id, "COMPLETED")}
                              >
                                Complete
                              </button>
                              <button
                                className="btn btn--ghost"
                                style={{ padding: "4px 8px", fontSize: "0.8rem" }}
                                onClick={() => handleUpdateSessionStatus(sess.id, "CANCELLED")}
                              >
                                Cancel
                              </button>
                            </div>
                          )}
                          {sess.status !== "SCHEDULED" && <span className="muted">—</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      )}

      {/* Schedule Session Modal */}
      {showSessionModal && (
        <div className="modal-backdrop">
          <div className="panel modal-card" style={{ maxWidth: "500px" }}>
            <h3>Schedule 1-on-1 Session</h3>
            <p className="muted">Set up a session with <strong>{selectedMentee.fullName}</strong></p>

            <form onSubmit={handleCreateSession} className="form" style={{ marginTop: "1rem" }}>
              <label className="field">
                <span>Session Title</span>
                <input
                  type="text"
                  placeholder="e.g. Architecture Review & Spring Boot Deep Dive"
                  value={sessionForm.title}
                  onChange={(e) => setSessionForm({ ...sessionForm, title: e.target.value })}
                  required
                />
              </label>

              <label className="field">
                <span>Agenda / Description</span>
                <textarea
                  rows={3}
                  placeholder="Points to discuss..."
                  value={sessionForm.description}
                  onChange={(e) => setSessionForm({ ...sessionForm, description: e.target.value })}
                />
              </label>

              <div className="form__grid">
                <label className="field">
                  <span>Date & Time</span>
                  <input
                    type="datetime-local"
                    value={sessionForm.scheduledAt}
                    onChange={(e) => setSessionForm({ ...sessionForm, scheduledAt: e.target.value })}
                    required
                  />
                </label>

                <label className="field">
                  <span>Duration (mins)</span>
                  <select
                    value={sessionForm.durationMinutes}
                    onChange={(e) => setSessionForm({ ...sessionForm, durationMinutes: e.target.value })}
                  >
                    <option value={30}>30 mins</option>
                    <option value={45}>45 mins</option>
                    <option value={60}>60 mins</option>
                    <option value={90}>90 mins</option>
                  </select>
                </label>
              </div>

              <label className="field">
                <span>Meeting Link (Optional)</span>
                <input
                  type="url"
                  placeholder="https://meet.google.com/... or Teams link"
                  value={sessionForm.meetingLink}
                  onChange={(e) => setSessionForm({ ...sessionForm, meetingLink: e.target.value })}
                />
              </label>

              <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "1rem" }}>
                <button type="button" className="btn btn--ghost" onClick={() => setShowSessionModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn--primary">
                  Confirm Session
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Goal Modal */}
      {showGoalModal && (
        <div className="modal-backdrop">
          <div className="panel modal-card" style={{ maxWidth: "480px" }}>
            <h3>Create Development Goal</h3>
            <p className="muted">Establish a developmental milestone for <strong>{selectedMentee.fullName}</strong></p>

            <form onSubmit={handleCreateGoal} className="form" style={{ marginTop: "1rem" }}>
              <label className="field">
                <span>Goal Title</span>
                <input
                  type="text"
                  placeholder="e.g. Master JPA N+1 Query Optimization"
                  value={goalForm.title}
                  onChange={(e) => setGoalForm({ ...goalForm, title: e.target.value })}
                  required
                />
              </label>

              <label className="field">
                <span>Description / Success Criteria</span>
                <textarea
                  rows={3}
                  placeholder="Specific targets to complete..."
                  value={goalForm.description}
                  onChange={(e) => setGoalForm({ ...goalForm, description: e.target.value })}
                />
              </label>

              <label className="field">
                <span>Target Date (Optional)</span>
                <input
                  type="date"
                  value={goalForm.targetDate}
                  onChange={(e) => setGoalForm({ ...goalForm, targetDate: e.target.value })}
                />
              </label>

              <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "1rem" }}>
                <button type="button" className="btn btn--ghost" onClick={() => setShowGoalModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn--primary">
                  Create Goal
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
