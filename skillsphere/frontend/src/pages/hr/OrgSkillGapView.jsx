import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getHrSkillGapSummary, getHrOrgHeatmap } from "../../services/hrService.js";

export default function OrgSkillGapView() {
  const [summary, setSummary] = useState(null);
  const [heatmap, setHeatmap] = useState(null);
  const [activeSubTab, setActiveSubTab] = useState("overview"); // 'overview', 'critical', 'heatmap'
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [sumRes, heatRes] = await Promise.all([
        getHrSkillGapSummary(),
        getHrOrgHeatmap(),
      ]);
      setSummary(sumRes);
      setHeatmap(heatRes);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load skill gap analytics.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Organisation Skill Gap Analysis</h3>
            <p className="hr-card-subtitle">
              Enterprise-wide gap metrics, departmental exposure, and critical shortage identification
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <div style={{ display: "flex", border: "1px solid var(--hr-line)", borderRadius: "8px", overflow: "hidden" }}>
              <button
                className={`btn-sm ${activeSubTab === "overview" ? "btn-primary" : "btn-secondary"}`}
                style={{ borderRadius: 0, border: "none" }}
                onClick={() => setActiveSubTab("overview")}
              >
                Summary & Departments
              </button>
              <button
                className={`btn-sm ${activeSubTab === "critical" ? "btn-primary" : "btn-secondary"}`}
                style={{ borderRadius: 0, border: "none" }}
                onClick={() => setActiveSubTab("critical")}
              >
                Critical Shortages
              </button>
              <button
                className={`btn-sm ${activeSubTab === "heatmap" ? "btn-primary" : "btn-secondary"}`}
                style={{ borderRadius: 0, border: "none" }}
                onClick={() => setActiveSubTab("heatmap")}
              >
                Enterprise Heatmap
              </button>
            </div>
            <button className="btn-secondary btn-sm" onClick={loadData}>
              <Icon name="refresh" size={14} /> Refresh
            </button>
          </div>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Analyzing workforce skill gaps...</h4>
            </div>
          ) : (
            <div>
              {/* Stat Grid */}
              <div className="hr-stat-grid">
                <div className="hr-stat-card">
                  <div className="hr-stat-header">
                    <span>Total Skill Gaps</span>
                    <div className="hr-stat-icon rose">
                      <Icon name="alert-triangle" size={18} />
                    </div>
                  </div>
                  <div className="hr-stat-value">{summary?.totalGaps ?? 0}</div>
                  <div className="hr-stat-meta">Across all job roles and competencies</div>
                </div>

                <div className="hr-stat-card">
                  <div className="hr-stat-header">
                    <span>High Priority Gaps</span>
                    <div className="hr-stat-icon rose">
                      <Icon name="target" size={18} />
                    </div>
                  </div>
                  <div className="hr-stat-value" style={{ color: "var(--hr-rose)" }}>
                    {summary?.highPriorityGaps ?? 0}
                  </div>
                  <div className="hr-stat-meta">Immediate training intervention required</div>
                </div>

                <div className="hr-stat-card">
                  <div className="hr-stat-header">
                    <span>Medium Priority</span>
                    <div className="hr-stat-icon amber">
                      <Icon name="trending" size={18} />
                    </div>
                  </div>
                  <div className="hr-stat-value">{summary?.mediumPriorityGaps ?? 0}</div>
                  <div className="hr-stat-meta">Scheduled for development paths</div>
                </div>

                <div className="hr-stat-card">
                  <div className="hr-stat-header">
                    <span>Average Gap Size</span>
                    <div className="hr-stat-icon blue">
                      <Icon name="bar-chart-2" size={18} />
                    </div>
                  </div>
                  <div className="hr-stat-value">{summary?.avgGapSize ?? 0} pts</div>
                  <div className="hr-stat-meta">Proficiency distance to target</div>
                </div>
              </div>

              {activeSubTab === "overview" && (
                <div>
                  <h4 style={{ margin: "20px 0 12px", fontSize: "15px", fontWeight: 700 }}>
                    Departmental Skill Gap Exposure
                  </h4>
                  {(!summary?.departmentBreakdown || summary.departmentBreakdown.length === 0) ? (
                    <div className="hr-empty-state">
                      <p>No departmental gaps recorded.</p>
                    </div>
                  ) : (
                    <div className="hr-table-wrap">
                      <table className="hr-table">
                        <thead>
                          <tr>
                            <th>Department</th>
                            <th>Headcount</th>
                            <th>Total Gaps</th>
                            <th>High Priority</th>
                            <th>Key Shortage Areas</th>
                          </tr>
                        </thead>
                        <tbody>
                          {summary.departmentBreakdown.map((dept, i) => (
                            <tr key={i}>
                              <td style={{ fontWeight: 600 }}>{dept.departmentName}</td>
                              <td>{dept.employeeCount}</td>
                              <td>{dept.gapCount}</td>
                              <td>
                                <span className={`status-pill ${dept.highPriorityCount > 0 ? "danger" : "success"}`}>
                                  {dept.highPriorityCount} critical
                                </span>
                              </td>
                              <td>
                                <div style={{ display: "flex", flexWrap: "wrap", gap: "6px" }}>
                                  {dept.topShortageSkills.map((s, idx) => (
                                    <span key={idx} className="status-pill purple">
                                      {s}
                                    </span>
                                  ))}
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}

              {activeSubTab === "critical" && (
                <div>
                  <h4 style={{ margin: "20px 0 12px", fontSize: "15px", fontWeight: 700 }}>
                    Enterprise Critical Skill Shortages
                  </h4>
                  {(!summary?.criticalShortages || summary.criticalShortages.length === 0) ? (
                    <div className="hr-empty-state">
                      <Icon name="check-circle" size={32} />
                      <h4>No critical shortages detected</h4>
                      <p>All core competencies are meeting minimum target benchmarks.</p>
                    </div>
                  ) : (
                    <div className="hr-table-wrap">
                      <table className="hr-table">
                        <thead>
                          <tr>
                            <th>Competency</th>
                            <th>Category</th>
                            <th>Affected Employees</th>
                            <th>Avg Current Level</th>
                            <th>Target Required Level</th>
                            <th>Avg Gap Size</th>
                          </tr>
                        </thead>
                        <tbody>
                          {summary.criticalShortages.map((c) => (
                            <tr key={c.skillId}>
                              <td style={{ fontWeight: 600 }}>{c.skillName}</td>
                              <td>
                                <span className="status-pill purple">{c.categoryName}</span>
                              </td>
                              <td style={{ fontWeight: 700, color: "var(--hr-rose)" }}>
                                {c.affectedEmployeeCount} employees
                              </td>
                              <td>Level {c.avgCurrentLevel}</td>
                              <td>Level {c.avgRequiredLevel}</td>
                              <td>
                                <span className="status-pill danger">{c.avgGap} pts</span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}

              {activeSubTab === "heatmap" && (
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", margin: "20px 0 12px" }}>
                    <h4 style={{ margin: 0, fontSize: "15px", fontWeight: 700 }}>
                      Department × Competency Heatmap Matrix
                    </h4>
                    <div style={{ display: "flex", gap: "12px", fontSize: "12px" }}>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}>
                        <span style={{ width: 12, height: 12, background: "#10b981", borderRadius: 2 }} /> Optimal
                      </span>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}>
                        <span style={{ width: 12, height: 12, background: "#3b82f6", borderRadius: 2 }} /> Low Gap
                      </span>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}>
                        <span style={{ width: 12, height: 12, background: "#f59e0b", borderRadius: 2 }} /> Moderate Gap
                      </span>
                      <span style={{ display: "inline-flex", alignItems: "center", gap: "4px" }}>
                        <span style={{ width: 12, height: 12, background: "#ef4444", borderRadius: 2 }} /> Critical Shortage
                      </span>
                    </div>
                  </div>

                  {(!heatmap?.departments || heatmap.departments.length === 0) ? (
                    <div className="hr-empty-state">
                      <p>Insufficient data to render heatmap.</p>
                    </div>
                  ) : (
                    <div className="heatmap-container">
                      <table className="heatmap-table">
                        <thead>
                          <tr>
                            <th style={{ minWidth: 140, textAlign: "left", padding: "8px", fontSize: "12px", color: "var(--hr-soft)" }}>
                              Department
                            </th>
                            {heatmap.skills.map((skill) => (
                              <th
                                key={skill}
                                style={{
                                  padding: "8px",
                                  fontSize: "11px",
                                  fontWeight: 600,
                                  color: "var(--hr-soft)",
                                  textAlign: "center",
                                  maxWidth: 90,
                                  overflow: "hidden",
                                  textOverflow: "ellipsis",
                                  whiteSpace: "nowrap",
                                }}
                              >
                                {skill}
                              </th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {heatmap.departments.map((dept) => (
                            <tr key={dept}>
                              <td style={{ fontWeight: 600, fontSize: "13px", padding: "8px", color: "var(--hr-ink)" }}>
                                {dept}
                              </td>
                              {heatmap.skills.map((skill) => {
                                const cell = heatmap.cells.find(
                                  (c) => c.department === dept && c.skill === skill
                                );
                                const status = cell?.status || "OPTIMAL";
                                return (
                                  <td key={skill} style={{ padding: 0 }}>
                                    <div className={`heatmap-cell ${status}`} title={`${dept} - ${skill}: ${cell?.avgProficiency ?? 3} / 5 (${cell?.gapCount ?? 0} gaps)`}>
                                      <span>{cell?.avgProficiency ?? 3.0}</span>
                                      <span style={{ fontSize: "10px", opacity: 0.85 }}>
                                        {cell?.gapCount ?? 0} gaps
                                      </span>
                                    </div>
                                  </td>
                                );
                              })}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
