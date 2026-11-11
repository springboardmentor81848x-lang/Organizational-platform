import { useState } from "react";
import Icon from "../../components/Icons.jsx";

export default function SkillHeatmapView({
  heatmapData,
  onRequestReassessment,
  onRecommendCourse,
}) {
  const [matrixView, setMatrixView] = useState("EMPLOYEE_SKILL"); // 'EMPLOYEE_SKILL' or 'SKILL_TEAM'
  const [selectedCell, setSelectedCell] = useState(null);

  if (!heatmapData) {
    return (
      <div className="panel" style={{ textAlign: "center", padding: "48px 24px" }}>
        <p className="muted">Loading heatmap visualization...</p>
      </div>
    );
  }

  const { employeeSkillMatrix, skillTeamMatrix } = heatmapData;
  const employees = employeeSkillMatrix?.employees || [];
  const skills = employeeSkillMatrix?.skills || [];
  const empCells = employeeSkillMatrix?.cells || [];

  const teams = skillTeamMatrix?.teams || [];
  const teamCells = skillTeamMatrix?.cells || [];

  // Build lookup for View A: employeeId_skillId -> cell
  const empCellLookup = {};
  empCells.forEach((c) => {
    empCellLookup[`${c.employeeId}_${c.skillId}`] = c;
  });

  // Build lookup for View B: teamId_skillId -> cell
  const teamCellLookup = {};
  teamCells.forEach((c) => {
    teamCellLookup[`${c.teamId}_${c.skillId}`] = c;
  });

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header">
        <div>
          <span className="eyebrow">Visual Analytics</span>
          <h1>Skill Gap Heatmap</h1>
          <p>
            Color-coded capability matrix. Rapidly spot competency blind spots,
            severe gaps, and benchmark compliance across individuals and teams.
          </p>
        </div>

        {/* View Mode Toggle */}
        <div style={{ display: "flex", background: "#f1f5f9", padding: "3px", borderRadius: "10px" }}>
          <button
            style={{
              padding: "8px 16px",
              border: 0,
              borderRadius: "8px",
              background: matrixView === "EMPLOYEE_SKILL" ? "#ffffff" : "transparent",
              color: matrixView === "EMPLOYEE_SKILL" ? "#2563eb" : "#64748b",
              fontWeight: 700,
              fontSize: "0.84rem",
              cursor: "pointer",
              boxShadow: matrixView === "EMPLOYEE_SKILL" ? "0 1px 3px rgba(0,0,0,0.08)" : "none",
            }}
            onClick={() => setMatrixView("EMPLOYEE_SKILL")}
          >
            Employee vs Skill
          </button>
          <button
            style={{
              padding: "8px 16px",
              border: 0,
              borderRadius: "8px",
              background: matrixView === "SKILL_TEAM" ? "#ffffff" : "transparent",
              color: matrixView === "SKILL_TEAM" ? "#2563eb" : "#64748b",
              fontWeight: 700,
              fontSize: "0.84rem",
              cursor: "pointer",
              boxShadow: matrixView === "SKILL_TEAM" ? "0 1px 3px rgba(0,0,0,0.08)" : "none",
            }}
            onClick={() => setMatrixView("SKILL_TEAM")}
          >
            Skill vs Team
          </button>
        </div>
      </div>

      {/* Interactive Legend */}
      <div className="filter-toolbar" style={{ justifyContent: "flex-start", gap: "18px" }}>
        <span style={{ fontSize: "0.82rem", fontWeight: 700, color: "#475569" }}>Legend:</span>
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <span className="heatmap-cell heatmap-cell--critical" style={{ padding: "3px 8px", fontSize: "0.75rem" }}>
            Critical Gap (&ge; 2)
          </span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <span className="heatmap-cell heatmap-cell--moderate" style={{ padding: "3px 8px", fontSize: "0.75rem" }}>
            Moderate Gap (1)
          </span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <span className="heatmap-cell heatmap-cell--met" style={{ padding: "3px 8px", fontSize: "0.75rem" }}>
            Benchmark Met
          </span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <span className="heatmap-cell heatmap-cell--exceeded" style={{ padding: "3px 8px", fontSize: "0.75rem" }}>
            Benchmark Exceeded
          </span>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          <span className="heatmap-cell heatmap-cell--no-data" style={{ padding: "3px 8px", fontSize: "0.75rem" }}>
            Not Assessed
          </span>
        </div>
      </div>

      {/* View A: Employee vs Skill Matrix */}
      {matrixView === "EMPLOYEE_SKILL" && (
        <div className="heatmap-container">
          {employees.length === 0 ? (
            <p className="muted" style={{ textAlign: "center", padding: "40px" }}>
              No team employees found to display heatmap.
            </p>
          ) : (
            <table className="heatmap-table">
              <thead>
                <tr>
                  <th style={{ textAlign: "left", paddingLeft: "16px", minWidth: "180px", position: "sticky", left: 0, zIndex: 5, background: "#f8fafc" }}>
                    Employee / Skill
                  </th>
                  {skills.map((s) => (
                    <th key={s.id} className="heatmap-col-header" title={`${s.name} (${s.categoryName})`}>
                      <div style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                        {s.name}
                      </div>
                      <small className="muted" style={{ fontWeight: 400, fontSize: "0.72rem" }}>
                        {s.categoryName}
                      </small>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {employees.map((emp) => (
                  <tr key={emp.id}>
                    <td className="heatmap-row-header">
                      <div>
                        <strong>{emp.name}</strong>
                        <small className="muted" style={{ display: "block", fontSize: "0.75rem" }}>{emp.teamName}</small>
                      </div>
                    </td>
                    {skills.map((s) => {
                      const cell = empCellLookup[`${emp.id}_${s.id}`];
                      const severity = cell?.severity || "NO_DATA";
                      const currentLvl = cell?.currentLevel || 0;
                      const reqLvl = cell?.requiredLevel || 0;

                      return (
                        <td key={s.id} style={{ padding: "3px" }}>
                          <div
                            className={`heatmap-cell heatmap-cell--${severity.toLowerCase().replace("_", "-")}`}
                            title={`${emp.name} - ${s.name}\nCurrent: ${cell?.currentProficiency || "Not Assessed"} (${currentLvl})\nRequired: ${cell?.requiredProficiency || "N/A"} (${reqLvl})\nGap: ${cell?.gapSize || 0}`}
                            onClick={() => {
                              setSelectedCell({
                                employeeId: emp.id,
                                employeeName: emp.name,
                                skillId: s.id,
                                skillName: s.name,
                                currentLevel: currentLvl,
                                currentProficiency: cell?.currentProficiency || "Not Assessed",
                                requiredLevel: reqLvl,
                                requiredProficiency: cell?.requiredProficiency || "Intermediate",
                                gapSize: cell?.gapSize || 0,
                                severity,
                              });
                            }}
                          >
                            {severity === "NO_DATA" ? (
                              <span style={{ fontSize: "0.75rem" }}>&mdash;</span>
                            ) : (
                              <div style={{ fontSize: "0.8rem", lineHeight: 1.1 }}>
                                <span>{currentLvl}/{reqLvl}</span>
                                {cell?.gapSize > 0 && (
                                  <small style={{ display: "block", fontSize: "0.68rem" }}>
                                    -{cell.gapSize}
                                  </small>
                                )}
                              </div>
                            )}
                          </div>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* View B: Skill vs Team Matrix */}
      {matrixView === "SKILL_TEAM" && (
        <div className="heatmap-container">
          {skills.length === 0 ? (
            <p className="muted" style={{ textAlign: "center", padding: "40px" }}>
              No skills found to display matrix.
            </p>
          ) : (
            <table className="heatmap-table">
              <thead>
                <tr>
                  <th style={{ textAlign: "left", paddingLeft: "16px", minWidth: "200px", position: "sticky", left: 0, zIndex: 5, background: "#f8fafc" }}>
                    Skill / Team
                  </th>
                  {teams.map((t) => (
                    <th key={t.id} className="heatmap-col-header" style={{ minWidth: "140px" }}>
                      {t.name}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {skills.map((s) => (
                  <tr key={s.id}>
                    <td className="heatmap-row-header">
                      <div>
                        <strong>{s.name}</strong>
                        <small className="muted" style={{ display: "block", fontSize: "0.75rem" }}>{s.categoryName}</small>
                      </div>
                    </td>
                    {teams.map((t) => {
                      const cell = teamCellLookup[`${t.id}_${s.id}`];
                      const avg = cell?.averageLevel || 0;
                      const cov = cell?.coveragePercentage || 0;
                      const gaps = cell?.gapCount || 0;

                      return (
                        <td key={t.id} style={{ padding: "4px" }}>
                          <div
                            className="heatmap-cell"
                            style={{
                              background:
                                avg >= 3.5 ? "#dcfce7" :
                                avg >= 2.5 ? "#eff6ff" :
                                avg > 0 ? "#fef3c7" : "#f1f5f9",
                              color:
                                avg >= 3.5 ? "#166534" :
                                avg >= 2.5 ? "#1e40af" :
                                avg > 0 ? "#92400e" : "#94a3b8",
                              padding: "8px 6px",
                            }}
                            title={`Team: ${t.name}\nSkill: ${s.name}\nAverage Level: ${avg} / 5.0\nCoverage: ${cov}%\nGaps: ${gaps}`}
                          >
                            <div style={{ fontWeight: 700, fontSize: "0.85rem" }}>
                              {avg > 0 ? `${avg} avg` : "No data"}
                            </div>
                            <small className="muted" style={{ display: "block", fontSize: "0.72rem" }}>
                              {cov}% cov &bull; {gaps} gaps
                            </small>
                          </div>
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* Cell Detail / Action Modal */}
      {selectedCell && (
        <div className="modal-backdrop" onClick={() => setSelectedCell(null)}>
          <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Heatmap Cell Detail</h2>
              <button
                style={{ border: 0, background: "transparent", cursor: "pointer", color: "#64748b" }}
                onClick={() => setSelectedCell(null)}
              >
                <Icon name="x" size={20} />
              </button>
            </div>
            <div className="modal-body" style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              <div style={{ padding: "14px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0" }}>
                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                  <span className="muted">Employee</span>
                  <strong>{selectedCell.employeeName}</strong>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                  <span className="muted">Skill</span>
                  <strong>{selectedCell.skillName}</strong>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                  <span className="muted">Current Assessment</span>
                  <span className="badge">
                    {selectedCell.currentProficiency} ({selectedCell.currentLevel})
                  </span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                  <span className="muted">Target Benchmark</span>
                  <span className="badge" style={{ background: "#eff6ff", color: "#1d4ed8" }}>
                    {selectedCell.requiredProficiency} ({selectedCell.requiredLevel})
                  </span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span className="muted">Deficit Gap</span>
                  <strong style={{ color: selectedCell.gapSize > 0 ? "#dc2626" : "#15803d" }}>
                    {selectedCell.gapSize > 0 ? `-${selectedCell.gapSize} Levels` : "Benchmark Met"}
                  </strong>
                </div>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                <h4 style={{ fontSize: "0.9rem", margin: 0 }}>Recommended Interventions</h4>
                <div style={{ display: "flex", gap: "10px" }}>
                  <button
                    className="btn btn--primary"
                    style={{ flex: 1 }}
                    onClick={() => {
                      const c = selectedCell;
                      setSelectedCell(null);
                      onRequestReassessment({
                        employeeId: c.employeeId,
                        skillId: c.skillId,
                        employeeName: c.employeeName,
                        skillName: c.skillName,
                      });
                    }}
                  >
                    <Icon name="refresh" size={16} /> Request Reassessment
                  </button>
                  <button
                    className="btn btn--ghost"
                    style={{ flex: 1 }}
                    onClick={() => {
                      const c = selectedCell;
                      setSelectedCell(null);
                      onRecommendCourse({
                        employeeId: c.employeeId,
                        skillId: c.skillId,
                        employeeName: c.employeeName,
                        skillName: c.skillName,
                      });
                    }}
                  >
                    <Icon name="book" size={16} /> Recommend Course
                  </button>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn--ghost" onClick={() => setSelectedCell(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
