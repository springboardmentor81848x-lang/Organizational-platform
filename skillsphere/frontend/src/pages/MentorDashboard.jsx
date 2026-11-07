import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import Alert from "../components/Alert.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import ChatWidget from "../components/ChatWidget.jsx";
import CalendarWidget from "../components/CalendarWidget.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import {
  getMyMentees,
  getMentorStatus,
  createMentorshipSession,
  updateSessionStatus,
  createMentorshipGoal,
  updateGoalStatus,
} from "../services/mentorshipService.js";
import {
  getMyIncomingMentorshipRequests,
  respondToMentorshipRequest,
  shareMentorshipResource,
  getMentorshipSharedResources,
} from "../services/platformService.js";
import { parseApiError } from "../services/apiClient.js";

const NAV_ITEMS = [
  { id: "overview", label: "Mentor Workspace", icon: "dashboard" },
  { id: "requests", label: "Mentorship Requests", icon: "users" },
  { id: "mentees", label: "My Mentees & Goals", icon: "clipboard" },
  { id: "sessions", label: "Mentoring Sessions", icon: "clock" },
  { id: "resources", label: "Shared Resources", icon: "book" },
  { id: "chat", label: "Mentee Chatbox", icon: "message" },
  { id: "calendar", label: "Schedule & Calendar", icon: "calendar" },
];

export default function MentorDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");

  const [mentorStatus, setMentorStatus] = useState(null);
  const [mentees, setMentees] = useState([]);
  const [incomingRequests, setIncomingRequests] = useState([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Goal modal/form
  const [goalTitle, setGoalTitle] = useState("");
  const [goalDesc, setGoalDesc] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [selectedAssignmentId, setSelectedAssignmentId] = useState("");

  // Session form
  const [sessionTopic, setSessionTopic] = useState("");
  const [sessionTime, setSessionTime] = useState("");
  const [meetingLink, setMeetingLink] = useState("");

  // Resource form
  const [resTitle, setResTitle] = useState("");
  const [resUrl, setResUrl] = useState("");

  const loadData = async () => {
    try {
      const st = await getMentorStatus().catch(() => null);
      if (st) setMentorStatus(st);

      const m = await getMyMentees().catch(() => []);
      setMentees(m);
      if (m.length > 0 && !selectedAssignmentId) {
        setSelectedAssignmentId(m[0].assignmentId);
      }

      const reqs = await getMyIncomingMentorshipRequests().catch(() => []);
      setIncomingRequests(reqs);
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRespondRequest = async (reqId, status) => {
    setError("");
    setSuccess("");
    try {
      await respondToMentorshipRequest(reqId, { status, responseNote: "Looking forward to working with you!" });
      setSuccess(`Mentorship request ${status.toLowerCase()}!`);
      loadData();
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  const handleCreateGoal = async (e) => {
    e.preventDefault();
    if (!selectedAssignmentId || !goalTitle) return;
    try {
      await createMentorshipGoal({
        assignmentId: Number(selectedAssignmentId),
        title: goalTitle,
        description: goalDesc,
        targetDate: targetDate || null,
      });
      setSuccess("Development goal created!");
      setGoalTitle("");
      setGoalDesc("");
      setTargetDate("");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleCreateSession = async (e) => {
    e.preventDefault();
    if (!selectedAssignmentId || !sessionTopic || !sessionTime) return;
    try {
      await createMentorshipSession({
        assignmentId: Number(selectedAssignmentId),
        topic: sessionTopic,
        scheduledAt: new Date(sessionTime).toISOString(),
        durationMinutes: 45,
        meetingLink: meetingLink || "https://meet.google.com/demo-session",
      });
      setSuccess("Mentoring session scheduled!");
      setSessionTopic("");
      setSessionTime("");
      setMeetingLink("");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleShareResource = async (e) => {
    e.preventDefault();
    if (!selectedAssignmentId || !resTitle || !resUrl) return;
    try {
      await shareMentorshipResource({
        assignmentId: Number(selectedAssignmentId),
        title: resTitle,
        urlOrContent: resUrl,
      });
      setSuccess("Resource shared with mentee!");
      setResTitle("");
      setResUrl("");
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const onSignOut = async () => {
    await signOut();
    navigate("/login", { replace: true });
  };

  return (
    <div className="employee-app">
      <aside className="employee-sidebar">
        <div className="sidebar-brand">
          <Logo />
          <span>MENTOR WORKSPACE</span>
        </div>

        <nav>
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`nav-item ${activeTab === item.id ? "is-active" : ""}`}
              onClick={() => setActiveTab(item.id)}
            >
              <Icon name={item.icon} size={18} />
              <span className="nav-label">{item.label}</span>
              {item.id === "requests" && incomingRequests.filter((r) => r.status === "PENDING").length > 0 && (
                <span className="nav-tag">{incomingRequests.filter((r) => r.status === "PENDING").length}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-avatar">{user.fullName.charAt(0)}</div>
          <div>
            <strong>{user.fullName}</strong>
            <small>{user.email}</small>
          </div>
          <button type="button" className="sidebar-signout" onClick={onSignOut} title="Sign out">
            <Icon name="logout" size={18} />
          </button>
        </div>
      </aside>

      <div className="employee-main">
        <header className="employee-topbar">
          <span className="topbar-title">Mentor Hub • {NAV_ITEMS.find((n) => n.id === activeTab)?.label}</span>
          <div className="topbar-user">
            <span className="badge">MENTOR</span>
            <NotificationsPopover />
          </div>
        </header>

        <main className="employee-content">
          {error && <Alert type="error">{error}</Alert>}
          {success && <Alert type="success">{success}</Alert>}

          {/* OVERVIEW */}
          {activeTab === "overview" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="hero">
                <div>
                  <span className="eyebrow" style={{ color: "#dbe7ff" }}>Expert Network</span>
                  <h1>Welcome, {user.fullName.split(" ")[0]}</h1>
                  <p>
                    Guide employees through skill development, review mentorship requests, and track goal completions.
                  </p>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Active Mentees</span>
                  <strong style={{ fontSize: "1.8rem" }}>{mentees.length}</strong>
                  <span className="muted">Currently paired</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Pending Requests</span>
                  <strong style={{ fontSize: "1.8rem", color: "#2563eb" }}>
                    {incomingRequests.filter((r) => r.status === "PENDING").length}
                  </strong>
                  <span className="muted">Awaiting your response</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Expertise Areas</span>
                  <strong style={{ fontSize: "1.2rem" }}>
                    {mentorStatus && mentorStatus.mentorProfile ? mentorStatus.mentorProfile.expertiseAreas : "Full Stack & Architecture"}
                  </strong>
                  <span className="muted">Profile tags</span>
                </div>
              </div>

              <div className="panel">
                <h2>Active Mentees</h2>
                {mentees.length === 0 ? (
                  <p className="muted">No active mentees yet. Incoming mentorship requests will show in the requests tab.</p>
                ) : (
                  <div className="table-wrap">
                    <table>
                      <thead>
                        <tr><th>Mentee</th><th>Job Role</th><th>Paired Since</th><th>Actions</th></tr>
                      </thead>
                      <tbody>
                        {mentees.map((m) => (
                          <tr key={m.assignmentId}>
                            <td><strong>{m.employeeName}</strong></td>
                            <td>{m.jobRole}</td>
                            <td>{new Date(m.assignedAt).toLocaleDateString()}</td>
                            <td>
                              <button
                                type="button"
                                className="text-action"
                                onClick={() => {
                                  setSelectedAssignmentId(m.assignmentId);
                                  setActiveTab("mentees");
                                }}
                              >
                                View Goals & Sessions &rarr;
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* REQUESTS */}
          {activeTab === "requests" && (
            <div className="panel">
              <h2>Incoming Mentorship Requests</h2>
              <p className="muted">Employees seeking guidance in specific areas.</p>

              {incomingRequests.length === 0 ? (
                <div style={{ textAlign: "center", padding: "40px", color: "#8795a9" }}>
                  No mentorship requests at this time.
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "16px" }}>
                  {incomingRequests.map((r) => (
                    <div
                      key={r.id}
                      style={{
                        padding: "16px 20px",
                        borderRadius: "12px",
                        border: "1.5px solid #e2e9f3",
                        background: "#f9fbfe",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        flexWrap: "wrap",
                        gap: "12px",
                      }}
                    >
                      <div>
                        <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                          <strong>{r.employeeName}</strong>
                          <span className="badge">{r.employeeJobRole || "Employee"}</span>
                          <span
                            className="badge"
                            style={{
                              background: r.status === "PENDING" ? "#fff4d6" : r.status === "ACCEPTED" ? "#e6f6ee" : "#fde9e7",
                              color: r.status === "PENDING" ? "#8a5a00" : r.status === "ACCEPTED" ? "#17754f" : "#b42318",
                            }}
                          >
                            {r.status}
                          </span>
                        </div>
                        <p style={{ margin: "6px 0 0", fontSize: "0.86rem", color: "#5b6d86" }}>
                          &ldquo;{r.message || "I would appreciate your mentorship."}&rdquo;
                        </p>
                        <small style={{ color: "#8795a9" }}>
                          Received on {new Date(r.createdAt).toLocaleDateString()}
                        </small>
                      </div>

                      {r.status === "PENDING" && (
                        <div style={{ display: "flex", gap: "8px" }}>
                          <button
                            type="button"
                            className="btn btn--complete"
                            onClick={() => handleRespondRequest(r.id, "ACCEPTED")}
                          >
                            Accept
                          </button>
                          <button
                            type="button"
                            className="btn btn--ghost"
                            onClick={() => handleRespondRequest(r.id, "DECLINED")}
                          >
                            Decline
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* MENTEES & GOALS */}
          {activeTab === "mentees" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="panel">
                <h2>Set Development Goal</h2>
                <form onSubmit={handleCreateGoal} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px", maxWidth: "600px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Mentee Assignment</label>
                    <select
                      value={selectedAssignmentId}
                      onChange={(e) => setSelectedAssignmentId(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    >
                      {mentees.map((m) => (
                        <option key={m.assignmentId} value={m.assignmentId}>
                          {m.employeeName} ({m.jobRole})
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Goal Title</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Master Spring Security & CSRF Architecture"
                      value={goalTitle}
                      onChange={(e) => setGoalTitle(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Description & Success Criteria</label>
                    <textarea
                      rows={3}
                      placeholder="Complete practical assignment and configure filter chain..."
                      value={goalDesc}
                      onChange={(e) => setGoalDesc(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Target Date (Optional)</label>
                    <input
                      type="date"
                      value={targetDate}
                      onChange={(e) => setTargetDate(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <button type="submit" className="btn btn--primary" style={{ alignSelf: "flex-start" }}>
                    Create Goal
                  </button>
                </form>
              </div>
            </div>
          )}

          {/* SESSIONS */}
          {activeTab === "sessions" && (
            <div className="panel" style={{ maxWidth: "600px" }}>
              <h2>Schedule Mentoring Session</h2>
              <form onSubmit={handleCreateSession} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Mentee</label>
                  <select
                    value={selectedAssignmentId}
                    onChange={(e) => setSelectedAssignmentId(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  >
                    {mentees.map((m) => (
                      <option key={m.assignmentId} value={m.assignmentId}>
                        {m.employeeName} ({m.jobRole})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Session Topic</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Code Review & Architecture Guidance"
                    value={sessionTopic}
                    onChange={(e) => setSessionTopic(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Date & Time</label>
                  <input
                    type="datetime-local"
                    required
                    value={sessionTime}
                    onChange={(e) => setSessionTime(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Meeting Link</label>
                  <input
                    type="text"
                    placeholder="https://meet.google.com/..."
                    value={meetingLink}
                    onChange={(e) => setMeetingLink(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  />
                </div>
                <button type="submit" className="btn btn--primary">
                  Schedule Session
                </button>
              </form>
            </div>
          )}

          {/* SHARED RESOURCES */}
          {activeTab === "resources" && (
            <div className="panel" style={{ maxWidth: "600px" }}>
              <h2>Share Learning Resource</h2>
              <p className="muted">Send curated documentation, repositories, or guides to your mentee.</p>

              <form onSubmit={handleShareResource} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Mentee Assignment</label>
                  <select
                    value={selectedAssignmentId}
                    onChange={(e) => setSelectedAssignmentId(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  >
                    {mentees.map((m) => (
                      <option key={m.assignmentId} value={m.assignmentId}>
                        {m.employeeName} ({m.jobRole})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Resource Title</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Spring Boot Production Best Practices"
                    value={resTitle}
                    onChange={(e) => setResTitle(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>URL / Documentation Link</label>
                  <input
                    type="text"
                    required
                    placeholder="https://spring.io/guides/tutorials/..."
                    value={resUrl}
                    onChange={(e) => setResUrl(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  />
                </div>
                <button type="submit" className="btn btn--primary">
                  Share Resource
                </button>
              </form>
            </div>
          )}

          {/* CHAT */}
          {activeTab === "chat" && <ChatWidget currentUserId={user.id} />}

          {/* CALENDAR */}
          {activeTab === "calendar" && <CalendarWidget currentUserId={user.id} />}
        </main>
      </div>
    </div>
  );
}
