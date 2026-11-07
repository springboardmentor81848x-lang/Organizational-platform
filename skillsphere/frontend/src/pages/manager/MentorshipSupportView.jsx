import { useState } from "react";
import Icon from "../../components/Icons.jsx";

export default function MentorshipSupportView({
  mentorshipStatus,
  onReferMentor,
}) {
  const [activeSubTab, setActiveSubTab] = useState("pairings");

  if (!mentorshipStatus) {
    return (
      <div className="panel" style={{ textAlign: "center", padding: "48px 24px" }}>
        <p className="muted">Loading mentorship support overview...</p>
      </div>
    );
  }

  const {
    totalEmployees = 0,
    mentoredEmployeesCount = 0,
    unmentoredEmployeesCount = 0,
    activeMentorships = [],
    opportunities = [],
    availableMentors = [],
  } = mentorshipStatus;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header">
        <div>
          <span className="eyebrow">Professional Guidance</span>
          <h1>Team Mentorship Support & Pairings</h1>
          <p>
            Monitor team mentorship coverage, match employees with critical skill gaps to approved subject-matter
            mentors, and refer team members for developmental pairings.
          </p>
        </div>
        <button className="btn btn--primary" onClick={() => onReferMentor()}>
          <Icon name="plus" size={16} /> Refer Team Member to Mentor
        </button>
      </div>

      {/* KPI Cards */}
      <div className="stat-grid" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))" }}>
        <div className="stat-card">
          <span className="stat-label">Mentored Team Members</span>
          <strong style={{ color: "#15803d" }}>{mentoredEmployeesCount}</strong>
          <small>{totalEmployees > 0 ? Math.round((mentoredEmployeesCount / totalEmployees) * 100) : 0}% Coverage</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Unmentored with Gaps</span>
          <strong style={{ color: unmentoredEmployeesCount > 0 ? "#dc2626" : "inherit" }}>
            {unmentoredEmployeesCount}
          </strong>
          <small>Mentorship recommended</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Active Pairings</span>
          <strong>{activeMentorships.length}</strong>
          <small>Ongoing mentorships</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Available Approved Mentors</span>
          <strong>{availableMentors.length}</strong>
          <small>Across organization</small>
        </div>
      </div>

      {/* Sub Tabs */}
      <div style={{ display: "flex", borderBottom: "1px solid #e2e8f0" }}>
        <button
          style={{
            padding: "12px 20px",
            border: "none",
            borderBottom: activeSubTab === "pairings" ? "2px solid #2563eb" : "2px solid transparent",
            background: "transparent",
            fontWeight: 700,
            fontSize: "0.9rem",
            color: activeSubTab === "pairings" ? "#2563eb" : "#64748b",
            cursor: "pointer",
          }}
          onClick={() => setActiveSubTab("pairings")}
        >
          Active Team Pairings ({activeMentorships.length})
        </button>

        <button
          style={{
            padding: "12px 20px",
            border: "none",
            borderBottom: activeSubTab === "opportunities" ? "2px solid #2563eb" : "2px solid transparent",
            background: "transparent",
            fontWeight: 700,
            fontSize: "0.9rem",
            color: activeSubTab === "opportunities" ? "#2563eb" : "#64748b",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            gap: "8px",
          }}
          onClick={() => setActiveSubTab("opportunities")}
        >
          Mentorship Opportunities ({opportunities.length})
          {opportunities.length > 0 && (
            <span className="badge" style={{ background: "#fee2e2", color: "#991b1b" }}>
              Action Needed
            </span>
          )}
        </button>

        <button
          style={{
            padding: "12px 20px",
            border: "none",
            borderBottom: activeSubTab === "mentors" ? "2px solid #2563eb" : "2px solid transparent",
            background: "transparent",
            fontWeight: 700,
            fontSize: "0.9rem",
            color: activeSubTab === "mentors" ? "#2563eb" : "#64748b",
            cursor: "pointer",
          }}
          onClick={() => setActiveSubTab("mentors")}
        >
          Mentors Directory ({availableMentors.length})
        </button>
      </div>

      {/* Sub Tab 1: Active Pairings */}
      {activeSubTab === "pairings" && (
        <div className="panel" style={{ padding: 0 }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Assigned Mentor</th>
                  <th>Focus Area</th>
                  <th>Status</th>
                  <th>Assigned Date</th>
                </tr>
              </thead>
              <tbody>
                {activeMentorships.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                      No active mentorship assignments found for your team members.
                    </td>
                  </tr>
                ) : (
                  activeMentorships.map((m) => (
                    <tr key={m.assignmentId}>
                      <td>
                        <strong>{m.employeeName}</strong>
                        <small className="muted" style={{ display: "block" }}>{m.employeeEmail}</small>
                      </td>
                      <td>
                        <strong>{m.mentorName}</strong>
                        <small className="muted" style={{ display: "block" }}>{m.mentorEmail}</small>
                      </td>
                      <td>
                        <span>{m.focusArea}</span>
                      </td>
                      <td>
                        <span className="badge" style={{ background: "#dcfce7", color: "#166534" }}>
                          {m.status}
                        </span>
                      </td>
                      <td>
                        <small className="muted">{new Date(m.assignedAt).toLocaleDateString()}</small>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Sub Tab 2: Mentorship Opportunities */}
      {activeSubTab === "opportunities" && (
        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {opportunities.length === 0 ? (
            <div className="panel" style={{ textAlign: "center", padding: "40px" }}>
              <p className="muted">No unmentored skill gap opportunities detected.</p>
            </div>
          ) : (
            opportunities.map((opp, idx) => (
              <div
                key={idx}
                className="panel"
                style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "16px", flexWrap: "wrap" }}
              >
                <div>
                  <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}>
                    <strong style={{ fontSize: "1rem" }}>{opp.employeeName}</strong>
                    <span className="badge" style={{ background: "#fee2e2", color: "#991b1b" }}>
                      Gap: {opp.skillName} (Current: {opp.currentLevel} &rarr; Required: {opp.requiredLevel})
                    </span>
                  </div>

                  <div style={{ fontSize: "0.84rem", color: "#475569" }}>
                    {opp.matchingMentors && opp.matchingMentors.length > 0 ? (
                      <span>
                        <strong>{opp.matchingMentors.length} Matching Mentors Available:</strong>{" "}
                        {opp.matchingMentors.map((m) => m.fullName).join(", ")}
                      </span>
                    ) : (
                      <span className="muted">No mentors currently registered with exact skill expertise.</span>
                    )}
                  </div>
                </div>

                <button
                  className="btn btn--primary"
                  style={{ minHeight: "34px", padding: "6px 14px", fontSize: "0.82rem" }}
                  onClick={() =>
                    onReferMentor({
                      employeeId: opp.employeeId,
                      employeeName: opp.employeeName,
                      skillId: opp.skillId,
                      skillName: opp.skillName,
                      matchingMentors: opp.matchingMentors,
                    })
                  }
                >
                  <Icon name="users" size={15} /> Refer to Mentor
                </button>
              </div>
            ))
          )}
        </div>
      )}

      {/* Sub Tab 3: Mentors Directory */}
      {activeSubTab === "mentors" && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "16px" }}>
          {availableMentors.map((m) => (
            <div key={m.userId} className="panel" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between", gap: "12px" }}>
              <div>
                <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "8px" }}>
                  <div className="sidebar-avatar">{m.fullName.charAt(0)}</div>
                  <div>
                    <strong style={{ fontSize: "0.95rem" }}>{m.fullName}</strong>
                    <small className="muted" style={{ display: "block" }}>{m.email}</small>
                  </div>
                </div>

                <p style={{ fontSize: "0.85rem", color: "#475569", margin: "0 0 10px" }}>
                  {m.headline || "Experienced Technical Mentor"}
                </p>

                <div style={{ display: "flex", flexWrap: "wrap", gap: "4px", marginBottom: "12px" }}>
                  {m.expertiseAreas && m.expertiseAreas.map((area, i) => (
                    <span key={i} style={{ fontSize: "0.72rem", padding: "2px 6px", borderRadius: "4px", background: "#f1f5f9", color: "#475569" }}>
                      {area}
                    </span>
                  ))}
                </div>

                <div style={{ fontSize: "0.8rem", color: "#64748b" }}>
                  <span>Capacity: {m.currentMenteesCount} / {m.maxMentees} Mentees</span>
                </div>
              </div>

              <div style={{ paddingTop: "10px", borderTop: "1px solid #e2e8f0" }}>
                <button
                  className="btn btn--ghost"
                  style={{ width: "100%", minHeight: "32px", fontSize: "0.82rem" }}
                  onClick={() => onReferMentor({ mentorId: m.userId, mentorName: m.fullName })}
                >
                  Select Mentor for Referral
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
