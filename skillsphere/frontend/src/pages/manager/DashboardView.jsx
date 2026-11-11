import Icon from "../../components/Icons.jsx";

export default function DashboardView({ overview, onNavigate, selectedTeamName }) {
  if (!overview) {
    return (
      <div className="panel" style={{ textAlign: "center", padding: "48px 24px" }}>
        <p className="muted">Loading dashboard overview...</p>
      </div>
    );
  }

  const {
    totalTeams,
    totalEmployees,
    totalAssessedSkills,
    skillCoveragePercentage,
    totalGaps,
    criticalGaps,
    highGaps,
    mediumGaps,
    lowGaps,
    pendingReassessments,
    activeCourseEnrollments,
    completedCourses,
    courseCompletionRate,
    activeMentorships,
    proficiencyDistribution = {},
    skillCoverageSummaries = [],
    recentActivities = [],
    recentAlerts = [],
  } = overview;

  const totalProficiencyCount = Object.values(proficiencyDistribution).reduce((a, b) => a + b, 0);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      {/* Hero Welcome Banner */}
      <div className="hero" style={{ padding: "28px 32px" }}>
        <div>
          <span className="eyebrow">Team Intelligence Hub</span>
          <h1 style={{ margin: "6px 0 10px" }}>Team Capability & Performance</h1>
          <p style={{ maxWidth: "640px", fontSize: "0.95rem" }}>
            Currently managing <strong>{selectedTeamName}</strong>. Tracking competency benchmarks,
            addressing critical skill deficits, and accelerating employee development.
          </p>
        </div>
        <div className="hero__art" style={{ width: "90px", height: "90px" }}>
          <Icon name="chart" size={44} />
        </div>
      </div>

      {/* Primary KPI Grid */}
      <div className="stat-grid" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <div className="stat-card">
          <div className="stat-icon" style={{ background: "#eff6ff", color: "#2563eb" }}>
            <Icon name="users" size={22} />
          </div>
          <span className="stat-label">Managed Workforce</span>
          <strong>{totalEmployees}</strong>
          <small>{totalTeams} {totalTeams === 1 ? "Active Team" : "Active Teams"}</small>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: "#ecfdf5", color: "#059669" }}>
            <Icon name="target" size={22} />
          </div>
          <span className="stat-label">Assessed Skills</span>
          <strong>{totalAssessedSkills}</strong>
          <small>{skillCoveragePercentage}% Team Coverage</small>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: "#fef2f2", color: "#dc2626" }}>
            <Icon name="alert" size={22} />
          </div>
          <span className="stat-label">Open Skill Gaps</span>
          <strong style={{ color: criticalGaps > 0 ? "#dc2626" : "inherit" }}>{totalGaps}</strong>
          <small>{criticalGaps} Critical, {highGaps} High Priority</small>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: "#fffbeb", color: "#d97706" }}>
            <Icon name="refresh" size={22} />
          </div>
          <span className="stat-label">Reassessment Requests</span>
          <strong>{pendingReassessments}</strong>
          <small>Pending validation</small>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: "#f5f3ff", color: "#7c3aed" }}>
            <Icon name="book" size={22} />
          </div>
          <span className="stat-label">Learning Progress</span>
          <strong>{courseCompletionRate}%</strong>
          <small>{completedCourses} Completed of {activeCourseEnrollments + completedCourses}</small>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: "#fdf4ff", color: "#c026d3" }}>
            <Icon name="award" size={22} />
          </div>
          <span className="stat-label">Mentorship Coverage</span>
          <strong>{activeMentorships}</strong>
          <small>Active Mentorship Pairings</small>
        </div>
      </div>

      {/* Two Column Section: Competency Distribution & Skill Coverage */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(420px, 1fr))", gap: "20px" }}>
        {/* Proficiency Distribution */}
        <div className="panel">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Competency Benchmark</span>
              <h2>Team Proficiency Distribution</h2>
            </div>
          </div>
          <p className="muted" style={{ fontSize: "0.86rem", marginBottom: "16px" }}>
            5-tier skill proficiency distribution across all validated employee capabilities.
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            {[
              { level: "Expert", color: "#2563eb", count: proficiencyDistribution.Expert || 0 },
              { level: "Advanced", color: "#059669", count: proficiencyDistribution.Advanced || 0 },
              { level: "Intermediate", color: "#d97706", count: proficiencyDistribution.Intermediate || 0 },
              { level: "Beginner", color: "#f97316", count: proficiencyDistribution.Beginner || 0 },
              { level: "Unaware", color: "#dc2626", count: proficiencyDistribution.Unaware || 0 },
            ].map((tier) => {
              const pct = totalProficiencyCount > 0 ? Math.round((tier.count / totalProficiencyCount) * 100) : 0;
              return (
                <div key={tier.level} style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.84rem", fontWeight: 600 }}>
                    <span style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                      <span style={{ width: "10px", height: "10px", borderRadius: "50%", background: tier.color }} />
                      {tier.level}
                    </span>
                    <span className="muted">{tier.count} skills ({pct}%)</span>
                  </div>
                  <div className="bar-track">
                    <div className="bar-fill" style={{ width: `${pct}%`, background: tier.color }} />
                  </div>
                </div>
              );
            })}
          </div>

          <div style={{ marginTop: "20px", display: "flex", justifyContent: "flex-end" }}>
            <button className="text-action" onClick={() => onNavigate("inventory")}>
              View Full Skill Inventory <Icon name="arrow" size={14} />
            </button>
          </div>
        </div>

        {/* Top Skill Coverage */}
        <div className="panel">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Capability Coverage</span>
              <h2>Top Team Skills</h2>
            </div>
          </div>
          <p className="muted" style={{ fontSize: "0.86rem", marginBottom: "16px" }}>
            Workforce coverage for essential department skills.
          </p>

          {skillCoverageSummaries.length === 0 ? (
            <p className="muted" style={{ textAlign: "center", padding: "24px 0" }}>
              No team skill assessments recorded yet.
            </p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              {skillCoverageSummaries.slice(0, 5).map((s) => (
                <div key={s.skillId} style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.84rem", fontWeight: 600 }}>
                    <span>{s.skillName} <small className="muted">({s.categoryName})</small></span>
                    <span style={{ color: s.coveragePercentage >= 80 ? "#15803d" : "#b45309" }}>
                      {s.employeesWithSkill} of {s.totalEmployees} ({s.coveragePercentage}%)
                    </span>
                  </div>
                  <div className="bar-track">
                    <div
                      className="bar-fill"
                      style={{
                        width: `${s.coveragePercentage}%`,
                        background: s.coveragePercentage >= 80 ? "#10b981" : s.coveragePercentage >= 50 ? "#3b82f6" : "#f59e0b",
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          )}

          <div style={{ marginTop: "20px", display: "flex", justifyContent: "flex-end" }}>
            <button className="text-action" onClick={() => onNavigate("heatmap")}>
              Open Skill Gap Heatmap <Icon name="arrow" size={14} />
            </button>
          </div>
        </div>
      </div>

      {/* Two Column Section: Recent Activities & Alerts */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(420px, 1fr))", gap: "20px" }}>
        {/* Recent Activities */}
        <div className="panel">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Audit Stream</span>
              <h2>Recent Team Development</h2>
            </div>
          </div>
          {recentActivities.length === 0 ? (
            <p className="muted" style={{ padding: "16px 0" }}>No recent team activity recorded.</p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
              {recentActivities.slice(0, 6).map((item) => (
                <div key={item.id} className="mini-course">
                  <div className="course-dot">
                    <Icon
                      name={
                        item.activityType === "ASSESSMENT" ? "target" :
                        item.activityType === "ENROLLMENT" ? "book" :
                        item.activityType === "REASSESSMENT" ? "refresh" : "award"
                      }
                      size={18}
                    />
                  </div>
                  <div>
                    <strong>{item.title}</strong>
                    <small>{item.employeeName} &bull; {item.description}</small>
                  </div>
                  <small className="muted" style={{ whiteSpace: "nowrap" }}>
                    {new Date(item.timestamp).toLocaleDateString()}
                  </small>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Quick Action Hub */}
        <div className="panel" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between" }}>
          <div>
            <span className="eyebrow">Action Workflows</span>
            <h2>Manager Priority Shortcuts</h2>
            <p className="muted" style={{ fontSize: "0.86rem", marginBottom: "18px" }}>
              Quick administrative actions to intervene on skill gaps and mentor assignments.
            </p>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
              <button
                className="btn btn--ghost"
                style={{ justifyContent: "flex-start", padding: "12px 14px", border: "1.5px solid #e2e8f0" }}
                onClick={() => onNavigate("gaps")}
              >
                <Icon name="alert" size={18} style={{ color: "#dc2626" }} />
                <span>Review Gaps</span>
              </button>

              <button
                className="btn btn--ghost"
                style={{ justifyContent: "flex-start", padding: "12px 14px", border: "1.5px solid #e2e8f0" }}
                onClick={() => onNavigate("assessments")}
              >
                <Icon name="refresh" size={18} style={{ color: "#2563eb" }} />
                <span>Request Reassessment</span>
              </button>

              <button
                className="btn btn--ghost"
                style={{ justifyContent: "flex-start", padding: "12px 14px", border: "1.5px solid #e2e8f0" }}
                onClick={() => onNavigate("learning-recs")}
              >
                <Icon name="sparkles" size={18} style={{ color: "#7c3aed" }} />
                <span>Recommend Course</span>
              </button>

              <button
                className="btn btn--ghost"
                style={{ justifyContent: "flex-start", padding: "12px 14px", border: "1.5px solid #e2e8f0" }}
                onClick={() => onNavigate("mentorship")}
              >
                <Icon name="users" size={18} style={{ color: "#059669" }} />
                <span>Refer Mentor</span>
              </button>

              <button
                className="btn btn--ghost"
                style={{ justifyContent: "flex-start", padding: "12px 14px", border: "1.5px solid #e2e8f0" }}
                onClick={() => onNavigate("reports")}
              >
                <Icon name="file" size={18} style={{ color: "#b45309" }} />
                <span>Export Reports</span>
              </button>

              <button
                className="btn btn--ghost"
                style={{ justifyContent: "flex-start", padding: "12px 14px", border: "1.5px solid #e2e8f0" }}
                onClick={() => onNavigate("calendar")}
              >
                <Icon name="calendar" size={18} style={{ color: "#0284c7" }} />
                <span>Team Calendar</span>
              </button>
            </div>
          </div>

          <div style={{ marginTop: "20px", padding: "14px 16px", background: "#f8fafc", borderRadius: "12px", border: "1px solid #e2e8f0" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "8px", color: "#1e293b", fontWeight: 700, fontSize: "0.88rem" }}>
              <Icon name="shield" size={16} style={{ color: "#2563eb" }} />
              <span>Strict Organizational Isolation Active</span>
            </div>
            <p className="muted" style={{ margin: "4px 0 0", fontSize: "0.78rem" }}>
              All queries and aggregations are scoped to your assigned teams. Private employee-to-employee chats remain strictly confidential.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
