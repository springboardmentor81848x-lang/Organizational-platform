import React from "react";
import Icon from "../../components/Icons.jsx";

export default function DashboardOverviewView({
  overview,
  user,
  onNavigateTab,
  onOpenScheduleModal,
}) {
  if (!overview) return <div className="mentor-panel">Loading mentor workspace...</div>;

  const {
    activeMenteesCount,
    pendingRequestsCount,
    completedSessionsCount,
    upcomingSessionsCount,
    goalCompletionRate,
    sharedResourcesCount,
    knowledgeArticlesCount,
    upcomingSessions = [],
    pendingRequests = [],
    activeMentees = [],
    recentActivities = [],
  } = overview;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      {/* Hero Welcome */}
      <div className="mentor-hero">
        <div>
          <h1>Welcome, {user.fullName.split(" ")[0]}</h1>
          <p>
            Guiding enterprise talent, tracking milestone completions, and closing knowledge gaps.
          </p>
        </div>
        <div style={{ display: "flex", gap: "10px" }}>
          <button
            type="button"
            className="mt-btn"
            style={{ background: "#ffffff", color: "#0f766e" }}
            onClick={() => onNavigateTab("requests")}
          >
            Review Requests ({pendingRequestsCount})
          </button>
          <button
            type="button"
            className="mt-btn"
            style={{ background: "#042f2e", color: "#ffffff" }}
            onClick={onOpenScheduleModal}
          >
            Schedule Session &rarr;
          </button>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="mentor-kpi-grid">
        <div className="mentor-kpi-card" onClick={() => onNavigateTab("mentees")} style={{ cursor: "pointer" }}>
          <span className="mentor-kpi-label">Active Mentees</span>
          <span className="mentor-kpi-value">{activeMenteesCount}</span>
          <span className="mentor-kpi-sub">Currently paired mentees</span>
        </div>
        <div className="mentor-kpi-card" onClick={() => onNavigateTab("requests")} style={{ cursor: "pointer" }}>
          <span className="mentor-kpi-label">Pending Requests</span>
          <span className="mentor-kpi-value" style={{ color: pendingRequestsCount > 0 ? "#2563eb" : "inherit" }}>
            {pendingRequestsCount}
          </span>
          <span className="mentor-kpi-sub">Awaiting your response</span>
        </div>
        <div className="mentor-kpi-card" onClick={() => onNavigateTab("sessions")} style={{ cursor: "pointer" }}>
          <span className="mentor-kpi-label">Completed Sessions</span>
          <span className="mentor-kpi-value" style={{ color: "#16a34a" }}>{completedSessionsCount}</span>
          <span className="mentor-kpi-sub">{upcomingSessionsCount} upcoming scheduled</span>
        </div>
        <div className="mentor-kpi-card" onClick={() => onNavigateTab("goals")} style={{ cursor: "pointer" }}>
          <span className="mentor-kpi-label">Goal Completion Rate</span>
          <span className="mentor-kpi-value">{goalCompletionRate}%</span>
          <span className="mentor-kpi-sub">Milestones achieved</span>
        </div>
        <div className="mentor-kpi-card" onClick={() => onNavigateTab("resources")} style={{ cursor: "pointer" }}>
          <span className="mentor-kpi-label">Shared Resources</span>
          <span className="mentor-kpi-value">{sharedResourcesCount}</span>
          <span className="mentor-kpi-sub">Curated docs & repos</span>
        </div>
        <div className="mentor-kpi-card" onClick={() => onNavigateTab("contributions")} style={{ cursor: "pointer" }}>
          <span className="mentor-kpi-label">Knowledge Articles</span>
          <span className="mentor-kpi-value" style={{ color: "#7e22ce" }}>{knowledgeArticlesCount}</span>
          <span className="mentor-kpi-sub">Published best practices</span>
        </div>
      </div>

      {/* Main Split Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(420px, 1fr))", gap: "20px" }}>
        {/* Upcoming Mentoring Sessions */}
        <div className="mentor-panel">
          <div className="mentor-panel-header">
            <h2>Upcoming Sessions</h2>
            <button type="button" className="mt-btn mt-btn--outline" onClick={() => onNavigateTab("sessions")}>
              View All &rarr;
            </button>
          </div>

          {upcomingSessions.length === 0 ? (
            <p style={{ color: "var(--mt-soft)", fontStyle: "italic", padding: "16px 0" }}>
              No upcoming mentoring sessions. Click "Schedule Session" to coordinate with your mentees.
            </p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {upcomingSessions.map((s) => (
                <div
                  key={s.id}
                  style={{
                    padding: "12px 16px",
                    borderRadius: "8px",
                    border: "1px solid var(--mt-line)",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: "10px",
                  }}
                >
                  <div>
                    <strong style={{ display: "block", fontSize: "0.95rem" }}>{s.title}</strong>
                    <span style={{ fontSize: "0.82rem", color: "var(--mt-soft)" }}>
                      with {s.employeeName} &bull; {new Date(s.scheduledAt).toLocaleString()} ({s.durationMinutes} min)
                    </span>
                  </div>
                  {s.meetingLink && (
                    <a
                      href={s.meetingLink}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="mt-btn mt-btn--outline"
                      style={{ fontSize: "0.78rem" }}
                    >
                      Join Link
                    </a>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Pending Requests Preview */}
        <div className="mentor-panel">
          <div className="mentor-panel-header">
            <h2>Pending Requests ({pendingRequests.length})</h2>
            <button type="button" className="mt-btn mt-btn--outline" onClick={() => onNavigateTab("requests")}>
              Manage &rarr;
            </button>
          </div>

          {pendingRequests.length === 0 ? (
            <p style={{ color: "var(--mt-soft)", fontStyle: "italic", padding: "16px 0" }}>
              No pending mentorship requests. All requests have been addressed.
            </p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
              {pendingRequests.map((r) => (
                <div
                  key={r.id}
                  style={{
                    padding: "12px 16px",
                    borderRadius: "8px",
                    background: "#f8fafc",
                    border: "1px solid var(--mt-line)",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    gap: "10px",
                  }}
                >
                  <div>
                    <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                      <strong>{r.employeeName}</strong>
                      <span className="mt-badge mt-badge--neutral">{r.employeeJobRole}</span>
                    </div>
                    <p style={{ margin: "4px 0 0", fontSize: "0.82rem", color: "var(--mt-soft)" }}>
                      &ldquo;{r.message || "Mentorship requested."}&rdquo;
                    </p>
                  </div>
                  <button
                    type="button"
                    className="mt-btn mt-btn--primary"
                    style={{ fontSize: "0.78rem" }}
                    onClick={() => onNavigateTab("requests")}
                  >
                    Respond
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Active Mentees Table */}
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <h2>Active Mentees ({activeMentees.length})</h2>
          <button type="button" className="mt-btn mt-btn--outline" onClick={() => onNavigateTab("mentees")}>
            View Mentees Directory &rarr;
          </button>
        </div>

        {activeMentees.length === 0 ? (
          <p style={{ color: "var(--mt-soft)", fontStyle: "italic" }}>
            No active mentees. Accept incoming requests to establish new mentorship pairings.
          </p>
        ) : (
          <div className="mentor-table-wrap">
            <table className="mentor-table">
              <thead>
                <tr>
                  <th>Mentee</th>
                  <th>Role & Dept</th>
                  <th>Paired Since</th>
                  <th>Goals</th>
                  <th>Sessions</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {activeMentees.map((m) => (
                  <tr key={m.assignmentId}>
                    <td>
                      <strong>{m.employeeName}</strong>
                      <div style={{ fontSize: "0.78rem", color: "var(--mt-faint)" }}>{m.email}</div>
                    </td>
                    <td>
                      <div>{m.jobRole}</div>
                      <small style={{ color: "var(--mt-faint)" }}>{m.department}</small>
                    </td>
                    <td>{new Date(m.assignedAt).toLocaleDateString()}</td>
                    <td>
                      <span className="mt-badge mt-badge--neutral">
                        {m.completedGoals} / {m.totalGoals} Completed
                      </span>
                    </td>
                    <td>{m.totalSessions} total</td>
                    <td>
                      <button
                        type="button"
                        className="mt-btn mt-btn--outline"
                        style={{ fontSize: "0.78rem" }}
                        onClick={() => onNavigateTab("mentees")}
                      >
                        View Dossier &rarr;
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
  );
}
