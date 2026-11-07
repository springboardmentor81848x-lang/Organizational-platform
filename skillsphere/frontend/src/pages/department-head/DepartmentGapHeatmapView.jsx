import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getDepartmentHeatmap } from "../../services/departmentHeadService.js";

export default function DepartmentGapHeatmapView() {
  const [heatmap, setHeatmap] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedCell, setSelectedCell] = useState(null);

  const loadHeatmap = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getDepartmentHeatmap();
      setHeatmap(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load Department Gap Heatmap.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadHeatmap();
  }, []);

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "60px 20px" }}>
        <Icon name="refresh" className="spin" size={32} />
        <h4 style={{ marginTop: "12px", color: "var(--dh-soft)" }}>Generating Department Gap Heatmap...</h4>
      </div>
    );
  }

  if (error) {
    return <Alert type="danger" message={error} />;
  }

  const { teams = [], competencies = [], cells = [] } = heatmap || {};

  // Build lookup map for (teamName, skillName) -> cell
  const cellMap = new Map();
  for (const cell of cells) {
    cellMap.set(`${cell.teamName}:::${cell.skillName}`, cell);
  }

  const getCellClass = (sev) => {
    switch (sev) {
      case "CRITICAL":
        return "dh-cell-critical";
      case "HIGH":
        return "dh-cell-high";
      case "MODERATE":
        return "dh-cell-moderate";
      case "LOW":
        return "dh-cell-low";
      case "ADEQUATE":
      default:
        return "dh-cell-adequate";
    }
  };

  return (
    <div>
      <div className="dh-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="dh-panel-title" style={{ margin: 0 }}>
              <Icon name="chart" size={18} />
              Department Gap Heatmap (Team × Competency)
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--dh-soft)" }}>
              Cross-functional visualization of proficiency gaps across engineering and functional teams.
            </p>
          </div>

          {/* Legend */}
          <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
            <span className="dh-status-badge critical">Critical Gap (≥2.0)</span>
            <span className="dh-status-badge high">High Gap (≥1.2)</span>
            <span className="dh-status-badge medium">Moderate Gap (≥0.5)</span>
            <span className="dh-status-badge low">Adequate / On Target</span>
          </div>
        </div>
      </div>

      {/* Heatmap Grid Table */}
      <div className="dh-panel" style={{ overflowX: "auto" }}>
        {teams.length === 0 || competencies.length === 0 ? (
          <div style={{ textAlign: "center", color: "var(--dh-faint)", padding: "40px 20px" }}>
            No teams or competency evaluations recorded for this department.
          </div>
        ) : (
          <table className="dh-heatmap-table">
            <thead>
              <tr>
                <th className="dh-heatmap-th" style={{ textAlign: "left", width: "180px" }}>
                  Functional Team
                </th>
                {competencies.map((comp, idx) => (
                  <th key={idx} className="dh-heatmap-th" style={{ minWidth: "110px" }}>
                    {comp}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {teams.map((teamName, tIdx) => (
                <tr key={tIdx}>
                  <td style={{ fontWeight: "700", fontSize: "13px", color: "var(--dh-ink)", padding: "10px" }}>
                    {teamName}
                  </td>
                  {competencies.map((skillName, cIdx) => {
                    const cell = cellMap.get(`${teamName}:::${skillName}`);
                    if (!cell) {
                      return (
                        <td key={cIdx} className="dh-heatmap-cell dh-cell-adequate">
                          —
                        </td>
                      );
                    }

                    return (
                      <td
                        key={cIdx}
                        className={`dh-heatmap-cell ${getCellClass(cell.severity)}`}
                        onClick={() => setSelectedCell(cell)}
                        title={`Click to view details for ${teamName} - ${skillName}`}
                      >
                        <div>Lvl {cell.avgProficiency}</div>
                        <div style={{ fontSize: "10px", opacity: 0.85 }}>
                          Gap: {cell.gapSize > 0 ? `-${cell.gapSize}` : "0.0"}
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

      {/* Cell Detail Modal */}
      {selectedCell && (
        <div className="dh-modal-overlay" onClick={() => setSelectedCell(null)}>
          <div className="dh-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px", color: "var(--dh-ink)" }}>
                Gap Analysis Detail: {selectedCell.skillName}
              </h3>
              <button
                style={{ background: "none", border: "none", cursor: "pointer", color: "var(--dh-soft)" }}
                onClick={() => setSelectedCell(null)}
              >
                <Icon name="x" size={20} />
              </button>
            </div>

            <div style={{ background: "#f8fafc", padding: "14px", borderRadius: "8px", marginBottom: "16px" }}>
              <div style={{ fontSize: "13px", color: "var(--dh-soft)" }}>Functional Team</div>
              <div style={{ fontSize: "16px", fontWeight: "700", color: "var(--dh-ink)" }}>{selectedCell.teamName}</div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px", marginBottom: "16px" }}>
              <div style={{ padding: "12px", borderRadius: "8px", border: "1px solid var(--dh-line)" }}>
                <div style={{ fontSize: "11px", color: "var(--dh-faint)", textTransform: "uppercase" }}>Average Proficiency</div>
                <div style={{ fontSize: "20px", fontWeight: "800", color: "var(--dh-ink)", marginTop: "4px" }}>
                  Level {selectedCell.avgProficiency}
                </div>
              </div>
              <div style={{ padding: "12px", borderRadius: "8px", border: "1px solid var(--dh-line)" }}>
                <div style={{ fontSize: "11px", color: "var(--dh-faint)", textTransform: "uppercase" }}>Required Level</div>
                <div style={{ fontSize: "20px", fontWeight: "800", color: "var(--dh-ink)", marginTop: "4px" }}>
                  Level {selectedCell.requiredLevel}
                </div>
              </div>
            </div>

            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px", borderRadius: "8px", background: selectedCell.severity === "CRITICAL" ? "var(--dh-rose-tint)" : "#f0fdfa", marginBottom: "20px" }}>
              <div>
                <span className={`dh-status-badge ${selectedCell.severity.toLowerCase()}`}>{selectedCell.severity}</span>
                <span style={{ marginLeft: "8px", fontSize: "13px", fontWeight: "600" }}>
                  Net Gap: {selectedCell.gapSize}
                </span>
              </div>
              <div style={{ fontSize: "12px", color: "var(--dh-soft)" }}>
                {selectedCell.assessedCount} / {selectedCell.totalTeamMembers} members evaluated
              </div>
            </div>

            <div style={{ textAlign: "right" }}>
              <button className="dh-btn dh-btn-primary" onClick={() => setSelectedCell(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
