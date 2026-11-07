import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getDepartmentSkillMatrix, getDepartmentTeams } from "../../services/departmentHeadService.js";

export default function DepartmentSkillMatrixView() {
  const [matrix, setMatrix] = useState(null);
  const [teams, setTeams] = useState([]);
  const [selectedTeamId, setSelectedTeamId] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadTeams = async () => {
    try {
      const res = await getDepartmentTeams();
      setTeams(res || []);
    } catch {
      // ignore
    }
  };

  const loadMatrix = async () => {
    try {
      setLoading(true);
      setError("");
      const params = {};
      if (selectedTeamId) params.teamId = selectedTeamId;
      if (search) params.search = search;
      const res = await getDepartmentSkillMatrix(params);
      setMatrix(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load Skill Matrix.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTeams();
  }, []);

  useEffect(() => {
    loadMatrix();
  }, [selectedTeamId]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    loadMatrix();
  };

  return (
    <div>
      <div className="dh-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="dh-panel-title" style={{ margin: 0 }}>
              <Icon name="target" size={18} />
              Department Competency & Compliance Matrix
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--dh-soft)" }}>
              Evaluating workforce capability against standardized role benchmarks.
            </p>
          </div>

          {/* Filters */}
          <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
            <select
              className="dh-select"
              value={selectedTeamId}
              onChange={(e) => setSelectedTeamId(e.target.value)}
            >
              <option value="">All Teams (Entire Department)</option>
              {teams.map((t) => (
                <option key={t.teamId} value={t.teamId}>
                  {t.teamName}
                </option>
              ))}
            </select>

            <form onSubmit={handleSearchSubmit} style={{ display: "flex", gap: "6px" }}>
              <input
                type="text"
                className="dh-input"
                placeholder="Filter competency or category..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              <button type="submit" className="dh-btn dh-btn-secondary">
                <Icon name="search" size={14} />
              </button>
            </form>
          </div>
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}

      {loading ? (
        <div style={{ textAlign: "center", padding: "60px 20px" }}>
          <Icon name="refresh" className="spin" size={32} />
          <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Evaluating Competency Matrix...</h4>
        </div>
      ) : (
        <div className="dh-panel">
          <div className="dh-table-container">
            <table className="dh-table">
              <thead>
                <tr>
                  <th>Competency</th>
                  <th>Category</th>
                  <th>Target Level</th>
                  <th>Priority</th>
                  <th>Compliance %</th>
                  <th>Distribution (Exceeding / Meeting / Below)</th>
                  <th>Team Performance</th>
                </tr>
              </thead>
              <tbody>
                {matrix?.competencies && matrix.competencies.length > 0 ? (
                  matrix.competencies.map((row) => (
                    <tr key={row.skillId}>
                      <td>
                        <strong>{row.skillName}</strong>
                      </td>
                      <td>
                        <span className="dh-badge">{row.categoryName}</span>
                      </td>
                      <td>
                        <span style={{ fontWeight: "600" }}>{row.targetProficiency}</span>
                      </td>
                      <td>
                        <span
                          className={`dh-status-badge ${
                            row.priority === "HIGH" ? "critical" : row.priority === "MEDIUM" ? "high" : "low"
                          }`}
                        >
                          {row.priority}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <div
                            style={{
                              width: "60px",
                              height: "6px",
                              borderRadius: "3px",
                              background: "#e2e8f0",
                              overflow: "hidden",
                            }}
                          >
                            <div
                              style={{
                                width: `${Math.min(100, row.compliancePercentage)}%`,
                                height: "100%",
                                background:
                                  row.compliancePercentage >= 75
                                    ? "var(--dh-emerald)"
                                    : row.compliancePercentage >= 50
                                    ? "var(--dh-amber)"
                                    : "var(--dh-rose)",
                              }}
                            />
                          </div>
                          <span style={{ fontWeight: "700", fontSize: "12px" }}>
                            {row.compliancePercentage}%
                          </span>
                        </div>
                      </td>
                      <td>
                        <div style={{ fontSize: "12px", display: "flex", gap: "6px" }}>
                          <span style={{ color: "var(--dh-emerald)", fontWeight: "600" }} title="Exceeding target">
                            ↑ {row.exceedingCount}
                          </span>
                          <span style={{ color: "var(--dh-ink)", fontWeight: "600" }} title="Meeting target">
                            ✓ {row.meetingCount}
                          </span>
                          <span style={{ color: "var(--dh-rose)", fontWeight: "600" }} title="Below target">
                            ↓ {row.belowCount}
                          </span>
                          <span style={{ color: "var(--dh-faint)" }} title="Unassessed">
                            ? {row.unassessedCount}
                          </span>
                        </div>
                      </td>
                      <td>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
                          {row.teamBreakdown && row.teamBreakdown.length > 0 ? (
                            row.teamBreakdown.map((t) => (
                              <span
                                key={t.teamId}
                                className={`dh-status-badge ${
                                  t.complianceStatus === "COMPLIANT"
                                    ? "low"
                                    : t.complianceStatus === "MODERATE"
                                    ? "high"
                                    : "critical"
                                }`}
                                style={{ fontSize: "10px" }}
                                title={`${t.teamName}: avg lvl ${t.avgProficiency}`}
                              >
                                {t.teamName.split(" ")[0]}: {t.avgProficiency}
                              </span>
                            ))
                          ) : (
                            <span style={{ fontSize: "11px", color: "var(--dh-faint)" }}>—</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} style={{ textAlign: "center", color: "var(--dh-faint)", padding: "20px" }}>
                      No competencies matching the specified filters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
