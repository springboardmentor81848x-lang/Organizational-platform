import { useState } from "react";
import Icon from "../../components/Icons.jsx";

export default function GapAnalysisView({
  gaps = [],
  onRequestReassessment,
  onRecommendCourse,
  onReferMentor,
}) {
  const [search, setSearch] = useState("");
  const [priorityFilter, setPriorityFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");

  const categories = ["ALL", ...new Set(gaps.map((g) => g.categoryName).filter(Boolean))];

  const filteredGaps = gaps.filter((g) => {
    const matchesPriority = priorityFilter === "ALL" || g.priority.toUpperCase() === priorityFilter;
    const matchesCategory = categoryFilter === "ALL" || g.categoryName === categoryFilter;
    const matchesSearch =
      !search.trim() ||
      g.employeeName.toLowerCase().includes(search.trim().toLowerCase()) ||
      g.skillName.toLowerCase().includes(search.trim().toLowerCase());
    return matchesPriority && matchesCategory && matchesSearch;
  });

  const criticalCount = gaps.filter((g) => g.gapSize >= 2 || g.priority === "HIGH").length;
  const affectedEmployees = new Set(gaps.map((g) => g.employeeId)).size;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header">
        <div>
          <span className="eyebrow">Gap Intelligence</span>
          <h1>Team Skill Gap Analysis</h1>
          <p>
            Pinpoint skill deficits across team members. Initiate targeted interventions including
            skill reassessments, catalogue course recommendations, and mentorship referrals.
          </p>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="stat-grid" style={{ gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))" }}>
        <div className="stat-card">
          <span className="stat-label">Total Identified Gaps</span>
          <strong>{gaps.length}</strong>
          <small>Across managed workforce</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Critical / High Deficits</span>
          <strong style={{ color: criticalCount > 0 ? "#dc2626" : "inherit" }}>{criticalCount}</strong>
          <small>Deficit &ge; 2 levels or High Priority</small>
        </div>
        <div className="stat-card">
          <span className="stat-label">Employees with Gaps</span>
          <strong>{affectedEmployees}</strong>
          <small>Need developmental guidance</small>
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
              placeholder="Search by employee or skill..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <span style={{ position: "absolute", left: "12px", top: "10px", color: "#94a3b8" }}>
              <Icon name="search" size={16} />
            </span>
          </div>

          <select
            className="filter-select"
            value={priorityFilter}
            onChange={(e) => setPriorityFilter(e.target.value)}
          >
            <option value="ALL">All Priorities</option>
            <option value="HIGH">High Priority Only</option>
            <option value="MEDIUM">Medium Priority</option>
            <option value="LOW">Low Priority</option>
          </select>

          <select
            className="filter-select"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
          >
            {categories.map((c) => (
              <option key={c} value={c}>
                {c === "ALL" ? "All Skill Categories" : c}
              </option>
            ))}
          </select>
        </div>

        <div style={{ fontSize: "0.85rem", color: "#64748b", fontWeight: 600 }}>
          Showing {filteredGaps.length} of {gaps.length} Gaps
        </div>
      </div>

      {/* Gaps Table */}
      <div className="panel" style={{ padding: 0 }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Employee</th>
                <th>Skill & Category</th>
                <th>Current Level</th>
                <th>Target Required</th>
                <th>Deficit</th>
                <th>Priority</th>
                <th>Last Assessed</th>
                <th style={{ textAlign: "right" }}>Intervention Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredGaps.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                    No skill gaps found matching your filter criteria.
                  </td>
                </tr>
              ) : (
                filteredGaps.map((g, idx) => (
                  <tr key={`${g.employeeId}_${g.skillId}_${idx}`}>
                    <td>
                      <div>
                        <strong style={{ display: "block" }}>{g.employeeName}</strong>
                        <small className="muted">{g.teamName}</small>
                      </div>
                    </td>
                    <td>
                      <div>
                        <strong>{g.skillName}</strong>
                        <small className="muted" style={{ display: "block" }}>{g.categoryName}</small>
                      </div>
                    </td>
                    <td>
                      <span
                        className="badge"
                        style={{
                          background:
                            g.currentLevel >= 4 ? "#dcfce7" :
                            g.currentLevel === 3 ? "#fef3c7" : "#fee2e2",
                          color:
                            g.currentLevel >= 4 ? "#166534" :
                            g.currentLevel === 3 ? "#92400e" : "#991b1b",
                        }}
                      >
                        Level {g.currentLevel} &bull; {g.currentLevelName}
                      </span>
                    </td>
                    <td>
                      <span className="badge" style={{ background: "#eff6ff", color: "#1d4ed8" }}>
                        Level {g.requiredLevel} &bull; {g.requiredLevelName}
                      </span>
                    </td>
                    <td>
                      <span
                        style={{
                          display: "inline-block",
                          padding: "3px 8px",
                          borderRadius: "6px",
                          fontWeight: 800,
                          fontSize: "0.84rem",
                          background: g.gapSize >= 2 ? "#fee2e2" : "#fef3c7",
                          color: g.gapSize >= 2 ? "#b91c1c" : "#b45309",
                        }}
                      >
                        -{g.gapSize} {g.gapSize === 1 ? "Level" : "Levels"}
                      </span>
                    </td>
                    <td>
                      <span
                        className="badge"
                        style={{
                          background:
                            g.priority === "HIGH" ? "#fee2e2" :
                            g.priority === "MEDIUM" ? "#fef3c7" : "#f1f5f9",
                          color:
                            g.priority === "HIGH" ? "#b91c1c" :
                            g.priority === "MEDIUM" ? "#b45309" : "#475569",
                        }}
                      >
                        {g.priority}
                      </span>
                    </td>
                    <td>
                      <small className="muted">
                        {new Date(g.lastAssessedAt).toLocaleDateString()}
                      </small>
                    </td>
                    <td style={{ textAlign: "right", whiteSpace: "nowrap" }}>
                      <div style={{ display: "inline-flex", gap: "6px" }}>
                        <button
                          className="btn btn--ghost"
                          style={{ padding: "5px 10px", minHeight: "30px", fontSize: "0.78rem" }}
                          title="Request skill reassessment"
                          onClick={() => onRequestReassessment(g)}
                        >
                          <Icon name="refresh" size={14} /> Reassess
                        </button>
                        <button
                          className="btn btn--ghost"
                          style={{ padding: "5px 10px", minHeight: "30px", fontSize: "0.78rem" }}
                          title="Recommend catalogue course"
                          onClick={() => onRecommendCourse(g)}
                        >
                          <Icon name="book" size={14} /> Recommend
                        </button>
                        <button
                          className="btn btn--ghost"
                          style={{ padding: "5px 10px", minHeight: "30px", fontSize: "0.78rem" }}
                          title="Refer to mentor"
                          onClick={() => onReferMentor(g)}
                        >
                          <Icon name="users" size={14} /> Mentor
                        </button>
                      </div>
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
