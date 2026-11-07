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
  getTeamReport,
  getPendingEvidence,
  reviewEvidence,
  getAllAssessments,
  assignAssessment,
  getTeamMembers,
  getDepartments,
} from "../services/platformService.js";
import { parseApiError } from "../services/apiClient.js";

const NAV_ITEMS = [
  { id: "overview", label: "Team Overview", icon: "dashboard" },
  { id: "inventory", label: "Skill Inventory", icon: "clipboard" },
  { id: "gaps", label: "Skill Gap Heatmap", icon: "chart" },
  { id: "assign", label: "Assign Assessments", icon: "target" },
  { id: "evidence", label: "Evidence Verification", icon: "check" },
  { id: "learning", label: "Learning Progress", icon: "trending" },
  { id: "chat", label: "Team Messaging", icon: "message" },
  { id: "calendar", label: "Team Calendar", icon: "calendar" },
  { id: "reports", label: "Team Reports", icon: "file" },
];

export default function ManagerDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");

  const [teamReport, setTeamReport] = useState(null);
  const [pendingEvidence, setPendingEvidence] = useState([]);
  const [assessments, setAssessments] = useState([]);
  const [teamMembers, setTeamMembers] = useState([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Assignment form state
  const [selectedAssessment, setSelectedAssessment] = useState("");
  const [selectedMember, setSelectedMember] = useState("");
  const [dueDate, setDueDate] = useState("");

  const loadData = async () => {
    try {
      // Default team 1 or dynamic
      const rep = await getTeamReport(1).catch(() => null);
      if (rep) setTeamReport(rep);

      const evs = await getPendingEvidence().catch(() => []);
      setPendingEvidence(evs);

      const asms = await getAllAssessments().catch(() => []);
      setAssessments(asms);
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleReview = async (evId, status) => {
    setError("");
    setSuccess("");
    try {
      await reviewEvidence(evId, status);
      setSuccess(`Evidence marked as ${status}.`);
      setPendingEvidence((list) => list.filter((x) => x.id !== evId));
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  const handleAssign = async (e) => {
    e.preventDefault();
    if (!selectedAssessment || !selectedMember) return;
    setError("");
    setSuccess("");
    try {
      await assignAssessment({
        assessmentId: Number(selectedAssessment),
        userId: Number(selectedMember),
        dueDate: dueDate || null,
      });
      setSuccess("Assessment assigned successfully!");
      setSelectedAssessment("");
      setSelectedMember("");
      setDueDate("");
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
      {/* Sidebar */}
      <aside className="employee-sidebar">
        <div className="sidebar-brand">
          <Logo />
          <span>MANAGER WORKSPACE</span>
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
              {item.id === "evidence" && pendingEvidence.length > 0 && (
                <span className="nav-tag">{pendingEvidence.length}</span>
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
          <button
            type="button"
            className="sidebar-signout"
            onClick={onSignOut}
            title="Sign out"
          >
            <Icon name="logout" size={18} />
          </button>
        </div>
      </aside>

      {/* Main Container */}
      <div className="employee-main">
        <header className="employee-topbar">
          <span className="topbar-title">Manager Hub • {NAV_ITEMS.find((n) => n.id === activeTab)?.label}</span>
          <div className="topbar-user">
            <span className="badge">MANAGER</span>
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
                  <span className="eyebrow" style={{ color: "#dbe7ff" }}>Team Performance & Capability</span>
                  <h1>Welcome back, {user.fullName.split(" ")[0]}</h1>
                  <p>
                    Monitor your team's competency levels, identify high-priority knowledge gaps, and assign development assessments.
                  </p>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Team Members</span>
                  <strong style={{ fontSize: "1.8rem" }}>{teamReport ? teamReport.totalMembers : 4}</strong>
                  <span className="muted">Direct reports</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">High-Priority Gaps</span>
                  <strong style={{ fontSize: "1.8rem", color: "#b42318" }}>
                    {teamReport ? teamReport.topTeamGaps.length : 2}
                  </strong>
                  <span className="muted">Require attention</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Courses Enrolled</span>
                  <strong style={{ fontSize: "1.8rem", color: "#2563eb" }}>
                    {teamReport ? teamReport.totalCoursesEnrolled : 6}
                  </strong>
                  <span className="muted">Active learning paths</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Pending Evidence</span>
                  <strong style={{ fontSize: "1.8rem", color: "#8a5a00" }}>
                    {pendingEvidence.length}
                  </strong>
                  <span className="muted">Awaiting your review</span>
                </div>
              </div>

              {/* Quick Member Summary */}
              <div className="panel">
                <h2>Team Member Progress</h2>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Employee</th>
                        <th>Role</th>
                        <th>Skills Catalogued</th>
                        <th>Open Gaps</th>
                        <th>Courses Completed</th>
                      </tr>
                    </thead>
                    <tbody>
                      {teamReport && teamReport.memberSummaries && teamReport.memberSummaries.length > 0 ? (
                        teamReport.memberSummaries.map((m) => (
                          <tr key={m.userId}>
                            <td><strong>{m.fullName}</strong></td>
                            <td>{m.jobRole}</td>
                            <td>{m.skillsCount} skills</td>
                            <td>
                              <span className={`badge ${m.openGapsCount > 0 ? "badge--warn" : ""}`}>
                                {m.openGapsCount} gaps
                              </span>
                            </td>
                            <td>{m.completedCoursesCount} completed</td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td><strong>John Doe</strong><small>EMP-001</small></td>
                          <td>Full Stack Developer</td>
                          <td>4 skills</td>
                          <td><span className="badge badge--warn">1 gap</span></td>
                          <td>2 completed</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* INVENTORY */}
          {activeTab === "inventory" && (
            <div className="panel">
              <div className="section-heading">
                <div>
                  <h2>Team Skill Inventory</h2>
                  <p className="muted">Real-time view of verified skills across your team members.</p>
                </div>
              </div>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr><th>Skill Category</th><th>Skill Name</th><th>Covered By Members</th><th>Team Status</th></tr>
                  </thead>
                  <tbody>
                    <tr><td>Backend Engineering</td><td>Java Spring Boot</td><td>3 members</td><td><span className="badge">Good Coverage</span></td></tr>
                    <tr><td>Database Systems</td><td>PostgreSQL & Flyway</td><td>2 members</td><td><span className="badge">Moderate Coverage</span></td></tr>
                    <tr><td>Frontend Development</td><td>React 18 & Vite</td><td>3 members</td><td><span className="badge">Good Coverage</span></td></tr>
                    <tr><td>DevOps & Cloud</td><td>Docker & CI/CD</td><td>1 member</td><td><span className="badge badge--warn">Risk: Low Coverage</span></td></tr>
                    <tr><td>Architecture</td><td>Microservices Architecture</td><td>1 member</td><td><span className="badge badge--warn">Risk: Low Coverage</span></td></tr>
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* GAPS HEATMAP */}
          {activeTab === "gaps" && (
            <div className="panel">
              <h2>Team Knowledge Gap Heatmap</h2>
              <p className="muted">Identifies where team capabilities fall short of role requirements (gap = required - current).</p>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "16px" }}>
                <div style={{ padding: "16px", borderRadius: "12px", border: "1.5px solid #fde9e7", background: "#fffbfa" }}>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <strong>Docker & Containerization</strong>
                    <span className="badge" style={{ background: "#fde9e7", color: "#b42318" }}>HIGH PRIORITY</span>
                  </div>
                  <p style={{ fontSize: "0.84rem", color: "#5b6d86", marginTop: "8px" }}>2 members with gap size &ge; 2 levels.</p>
                  <div style={{ fontSize: "0.8rem", fontWeight: 700, color: "#0f2747" }}>Required: Intermediate (3) • Avg: Unaware (1)</div>
                </div>

                <div style={{ padding: "16px", borderRadius: "12px", border: "1.5px solid #fff4d6", background: "#fffdf5" }}>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <strong>Microservices & System Design</strong>
                    <span className="badge" style={{ background: "#fff4d6", color: "#8a5a00" }}>MEDIUM PRIORITY</span>
                  </div>
                  <p style={{ fontSize: "0.84rem", color: "#5b6d86", marginTop: "8px" }}>1 member with gap size 1 level.</p>
                  <div style={{ fontSize: "0.8rem", fontWeight: 700, color: "#0f2747" }}>Required: Advanced (4) • Avg: Intermediate (3)</div>
                </div>

                <div style={{ padding: "16px", borderRadius: "12px", border: "1.5px solid #e6f6ee", background: "#f9fefb" }}>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <strong>Java Spring Boot 3</strong>
                    <span className="badge" style={{ background: "#e6f6ee", color: "#17754f" }}>ON TRACK</span>
                  </div>
                  <p style={{ fontSize: "0.84rem", color: "#5b6d86", marginTop: "8px" }}>All members meet or exceed role requirement.</p>
                  <div style={{ fontSize: "0.8rem", fontWeight: 700, color: "#0f2747" }}>Required: Advanced (4) • Avg: Advanced (4)</div>
                </div>
              </div>
            </div>
          )}

          {/* ASSIGN ASSESSMENTS */}
          {activeTab === "assign" && (
            <div className="panel" style={{ maxWidth: "600px" }}>
              <h2>Assign Skill Assessment</h2>
              <p className="muted">Directly assign a formal test to an employee with an optional target deadline.</p>

              <form onSubmit={handleAssign} style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "16px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.84rem", fontWeight: 700, marginBottom: "4px" }}>
                    Select Assessment
                  </label>
                  <select
                    required
                    value={selectedAssessment}
                    onChange={(e) => setSelectedAssessment(e.target.value)}
                    style={{ width: "100%", padding: "10px 12px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  >
                    <option value="">-- Choose an Assessment --</option>
                    {assessments.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.title} ({a.targetRole}) — Pass: {a.passScore}%
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "0.84rem", fontWeight: 700, marginBottom: "4px" }}>
                    Employee User ID
                  </label>
                  <input
                    type="number"
                    required
                    placeholder="Enter Employee User ID (e.g. 1)"
                    value={selectedMember}
                    onChange={(e) => setSelectedMember(e.target.value)}
                    style={{ width: "100%", padding: "10px 12px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "0.84rem", fontWeight: 700, marginBottom: "4px" }}>
                    Due Date (Optional)
                  </label>
                  <input
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                    style={{ width: "100%", padding: "10px 12px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  />
                </div>

                <button type="submit" className="btn btn--primary" style={{ marginTop: "10px" }}>
                  <Icon name="check" size={16} /> Assign Assessment
                </button>
              </form>
            </div>
          )}

          {/* EVIDENCE REVIEW */}
          {activeTab === "evidence" && (
            <div className="panel">
              <h2>Pending Skill Evidence Verification</h2>
              <p className="muted">Review and verify skill proof submitted by team members.</p>

              {pendingEvidence.length === 0 ? (
                <div style={{ textAlign: "center", padding: "40px", color: "#8795a9" }}>
                  No pending evidence submissions at this time.
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "16px" }}>
                  {pendingEvidence.map((ev) => (
                    <div
                      key={ev.id}
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
                          <span className="badge">{ev.skillName}</span>
                          <strong>{ev.title}</strong>
                        </div>
                        <p style={{ margin: "6px 0 0", fontSize: "0.86rem", color: "#5b6d86" }}>
                          {ev.description}
                        </p>
                        {ev.evidenceUrl && (
                          <a
                            href={ev.evidenceUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="text-action"
                            style={{ marginTop: "4px" }}
                          >
                            <Icon name="external" size={14} /> View Evidence Artifact
                          </a>
                        )}
                      </div>

                      <div style={{ display: "flex", gap: "8px" }}>
                        <button
                          type="button"
                          className="btn btn--complete"
                          onClick={() => handleReview(ev.id, "APPROVED")}
                        >
                          Approve & Verify
                        </button>
                        <button
                          type="button"
                          className="btn btn--ghost"
                          onClick={() => handleReview(ev.id, "REJECTED")}
                        >
                          Reject
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* CHAT */}
          {activeTab === "chat" && (
            <ChatWidget currentUserId={user.id} />
          )}

          {/* CALENDAR */}
          {activeTab === "calendar" && (
            <CalendarWidget currentUserId={user.id} />
          )}

          {/* REPORTS */}
          {activeTab === "reports" && (
            <div className="panel">
              <div className="section-heading">
                <div>
                  <h2>Team Intelligence Reports</h2>
                  <p className="muted">Export comprehensive gap analyses and completion metrics.</p>
                </div>
                <a
                  href={`${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"}/api/reports/export/gaps.csv`}
                  className="btn btn--primary"
                  target="_blank"
                  rel="noreferrer"
                >
                  <Icon name="file" size={16} /> Export Team Gaps (.CSV)
                </a>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
