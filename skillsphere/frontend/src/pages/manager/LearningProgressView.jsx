import { useState } from "react";
import Icon from "../../components/Icons.jsx";

export default function LearningProgressView({ learnings = [] }) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  const filteredLearnings = learnings.filter((l) => {
    const matchesStatus = statusFilter === "ALL" || l.status.toUpperCase() === statusFilter;
    const matchesSearch =
      !search.trim() ||
      (l.courseTitle && l.courseTitle.toLowerCase().includes(search.trim().toLowerCase())) ||
      (l.skillName && l.skillName.toLowerCase().includes(search.trim().toLowerCase()));
    return matchesStatus && matchesSearch;
  });

  const completedCount = learnings.filter((l) => l.status === "COMPLETED" || l.status === "CERTIFIED").length;
  const inProgressCount = learnings.filter((l) => l.status === "IN_PROGRESS" || l.status === "ENROLLED").length;
  const avgProgress = learnings.length > 0
    ? Math.round(learnings.reduce((sum, l) => sum + (l.progressPct || 0), 0) / learnings.length)
    : 0;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header">
        <div>
          <span className="eyebrow">Workforce Progression</span>
          <h1>Team Learning & Course Compliance</h1>
          <p>
            Track real-time progress and completion milestones for all team members
            actively enrolled in training modules and certification courses.
          </p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="stat-grid" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))" }}>
        <div className="stat-card">
          <span className="stat-label">Total Course Enrollments</span>
          <strong>{learnings.length}</strong>
          <small>Active & completed</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">In Progress</span>
          <strong style={{ color: "#d97706" }}>{inProgressCount}</strong>
          <small>Active learning</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Completed & Certified</span>
          <strong style={{ color: "#15803d" }}>{completedCount}</strong>
          <small>{learnings.length > 0 ? Math.round((completedCount / learnings.length) * 100) : 0}% Completion Rate</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Average Team Progress</span>
          <strong>{avgProgress}%</strong>
          <small>Across all enrollments</small>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="filter-toolbar">
        <div className="filter-group">
          <div style={{ position: "relative" }}>
            <input
              type="text"
              className="filter-input"
              style={{ paddingLeft: "36px", minWidth: "260px" }}
              placeholder="Search by course or skill..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <span style={{ position: "absolute", left: "12px", top: "10px", color: "#94a3b8" }}>
              <Icon name="search" size={16} />
            </span>
          </div>

          <select
            className="filter-select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Statuses</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="ENROLLED">Enrolled</option>
            <option value="COMPLETED">Completed</option>
            <option value="CERTIFIED">Certified</option>
          </select>
        </div>

        <div style={{ fontSize: "0.85rem", color: "#64748b", fontWeight: 600 }}>
          Showing {filteredLearnings.length} of {learnings.length} Enrollments
        </div>
      </div>

      {/* Progress Table */}
      <div className="panel" style={{ padding: 0 }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Course Title</th>
                <th>Skill</th>
                <th>Provider & Level</th>
                <th>Progress</th>
                <th>Status</th>
                <th>Enrolled Date</th>
                <th>Completed Date</th>
              </tr>
            </thead>
            <tbody>
              {filteredLearnings.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                    No learning records found matching your filters.
                  </td>
                </tr>
              ) : (
                filteredLearnings.map((l) => (
                  <tr key={l.id}>
                    <td>
                      <strong style={{ display: "block" }}>{l.courseTitle}</strong>
                    </td>
                    <td>
                      <span className="badge">{l.skillName || "General"}</span>
                    </td>
                    <td>
                      <div>
                        <span>{l.provider}</span>
                        <small className="muted" style={{ display: "block" }}>{l.level}</small>
                      </div>
                    </td>
                    <td>
                      <div style={{ minWidth: "120px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem", fontWeight: 700, marginBottom: "3px" }}>
                          <span>{l.progressPct}%</span>
                        </div>
                        <div className="bar-track">
                          <div
                            className="bar-fill"
                            style={{
                              width: `${l.progressPct}%`,
                              background: l.progressPct === 100 ? "#10b981" : "#3b82f6",
                            }}
                          />
                        </div>
                      </div>
                    </td>
                    <td>
                      <span
                        className="badge"
                        style={{
                          background:
                            l.status === "COMPLETED" || l.status === "CERTIFIED" ? "#dcfce7" :
                            l.status === "IN_PROGRESS" ? "#eff6ff" : "#fef3c7",
                          color:
                            l.status === "COMPLETED" || l.status === "CERTIFIED" ? "#166534" :
                            l.status === "IN_PROGRESS" ? "#1d4ed8" : "#92400e",
                        }}
                      >
                        {l.status}
                      </span>
                    </td>
                    <td>
                      <small className="muted">
                        {l.enrolledAt ? new Date(l.enrolledAt).toLocaleDateString() : "N/A"}
                      </small>
                    </td>
                    <td>
                      <small className="muted">
                        {l.completedAt ? new Date(l.completedAt).toLocaleDateString() : "&mdash;"}
                      </small>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
