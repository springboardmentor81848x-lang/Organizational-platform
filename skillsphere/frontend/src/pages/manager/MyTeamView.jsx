import { useState } from "react";
import Icon from "../../components/Icons.jsx";
import { getManagerEmployeeDetail } from "../../services/managerService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function MyTeamView({
  teamMembers = [],
  onRequestReassessment,
  onRecommendCourse,
  onReferMentor,
}) {
  const [search, setSearch] = useState("");
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [dossierLoading, setDossierLoading] = useState(false);
  const [dossierError, setDossierError] = useState("");
  const [dossierTab, setDossierTab] = useState("skills");

  const filteredMembers = teamMembers.filter((m) => {
    if (!search.trim()) return true;
    const q = search.trim().toLowerCase();
    return (
      (m.fullName && m.fullName.toLowerCase().includes(q)) ||
      (m.email && m.email.toLowerCase().includes(q)) ||
      (m.employeeId && m.employeeId.toLowerCase().includes(q)) ||
      (m.jobRole && m.jobRole.toLowerCase().includes(q))
    );
  });

  const openDossier = async (employeeId) => {
    setDossierLoading(true);
    setDossierError("");
    setDossierTab("skills");
    try {
      const data = await getManagerEmployeeDetail(employeeId);
      setSelectedEmployee(data);
    } catch (err) {
      setDossierError(parseApiError(err).message);
    } finally {
      setDossierLoading(false);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header">
        <div>
          <span className="eyebrow">Workforce Management</span>
          <h1>My Team Roster</h1>
          <p>
            Complete directory of employees across your managed reporting scope.
            Review competencies, track progression, and access individual employee dossiers.
          </p>
        </div>
      </div>

      {/* Search Bar */}
      <div className="filter-toolbar">
        <div style={{ position: "relative", minWidth: "300px", flex: "1 1 300px" }}>
          <input
            type="text"
            className="filter-input"
            style={{ width: "100%", paddingLeft: "36px" }}
            placeholder="Search by employee name, email, role, or ID..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <span style={{ position: "absolute", left: "12px", top: "10px", color: "#94a3b8" }}>
            <Icon name="search" size={16} />
          </span>
        </div>
        <div style={{ fontSize: "0.85rem", color: "#64748b", fontWeight: 600 }}>
          Showing {filteredMembers.length} of {teamMembers.length} Employees
        </div>
      </div>

      {/* Team Members Table */}
      <div className="panel" style={{ padding: "0" }}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Employee</th>
                <th>Employee ID</th>
                <th>Role & Department</th>
                <th>Team</th>
                <th>Assessed Skills</th>
                <th>Open Gaps</th>
                <th>Learning Progress</th>
                <th>Assigned Mentor</th>
                <th style={{ textAlign: "right" }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredMembers.length === 0 ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                    No team members found matching your search.
                  </td>
                </tr>
              ) : (
                filteredMembers.map((m) => (
                  <tr key={m.userId}>
                    <td>
                      <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                        <div
                          style={{
                            width: "36px",
                            height: "36px",
                            borderRadius: "10px",
                            background: "#eff6ff",
                            color: "#1d4ed8",
                            fontWeight: 800,
                            display: "grid",
                            placeItems: "center",
                            fontSize: "0.85rem",
                          }}
                        >
                          {m.fullName.charAt(0)}
                        </div>
                        <div>
                          <strong style={{ display: "block" }}>{m.fullName}</strong>
                          <small className="muted">{m.email}</small>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span style={{ fontFamily: "monospace", fontWeight: 600 }}>{m.employeeId || "N/A"}</span>
                    </td>
                    <td>
                      <div>
                        <span style={{ fontWeight: 600 }}>{m.jobRole}</span>
                        <small className="muted">{m.departmentName}</small>
                      </div>
                    </td>
                    <td>
                      <span className="badge">{m.teamName}</span>
                    </td>
                    <td>
                      <strong>{m.skillsAssessedCount}</strong>
                    </td>
                    <td>
                      {m.totalGapsCount === 0 ? (
                        <span className="badge" style={{ background: "#dcfce7", color: "#166534" }}>
                          0 Gaps
                        </span>
                      ) : (
                        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                          <span className="badge" style={{ background: "#fee2e2", color: "#991b1b" }}>
                            {m.totalGapsCount} Gaps
                          </span>
                          {m.criticalGapsCount > 0 && (
                            <span className="badge" style={{ background: "#dc2626", color: "#fff" }}>
                              {m.criticalGapsCount} Critical
                            </span>
                          )}
                        </div>
                      )}
                    </td>
                    <td>
                      <div style={{ minWidth: "100px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem", fontWeight: 600, marginBottom: "3px" }}>
                          <span>{m.learningProgressPercent}%</span>
                          <span className="muted">{m.completedCoursesCount} done</span>
                        </div>
                        <div className="bar-track">
                          <div className="bar-fill" style={{ width: `${m.learningProgressPercent}%` }} />
                        </div>
                      </div>
                    </td>
                    <td>
                      {m.mentorName ? (
                        <span style={{ fontSize: "0.84rem", fontWeight: 600, color: "#15803d" }}>
                          {m.mentorName}
                        </span>
                      ) : (
                        <span className="muted" style={{ fontSize: "0.82rem" }}>None</span>
                      )}
                    </td>
                    <td style={{ textAlign: "right", whiteSpace: "nowrap" }}>
                      <button
                        className="btn btn--ghost"
                        style={{ padding: "6px 12px", minHeight: "32px", fontSize: "0.82rem" }}
                        onClick={() => openDossier(m.userId)}
                      >
                        View Dossier
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Dossier Modal */}
      {selectedEmployee && (
        <div className="modal-backdrop" onClick={() => setSelectedEmployee(null)}>
          <div
            className="modal-dialog modal-dialog--lg"
            onClick={(e) => e.stopPropagation()}
            style={{ maxHeight: "88vh" }}
          >
            <div className="modal-header">
              <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                <div className="dossier-avatar">{selectedEmployee.fullName.charAt(0)}</div>
                <div>
                  <h2 style={{ fontSize: "1.2rem" }}>{selectedEmployee.fullName}</h2>
                  <p className="muted" style={{ margin: "2px 0 0", fontSize: "0.84rem" }}>
                    {selectedEmployee.jobRole} &bull; {selectedEmployee.departmentName} &bull; Team {selectedEmployee.teamName}
                  </p>
                </div>
              </div>
              <button
                style={{ border: 0, background: "transparent", cursor: "pointer", color: "#64748b" }}
                onClick={() => setSelectedEmployee(null)}
              >
                <Icon name="x" size={20} />
              </button>
            </div>

            {/* Dossier Navigation Tabs */}
            <div style={{ display: "flex", borderBottom: "1px solid #e2e8f0", background: "#f8fafc", padding: "0 24px" }}>
              {[
                { id: "skills", label: `Skills (${selectedEmployee.skills?.length || 0})` },
                { id: "gaps", label: `Skill Gaps (${selectedEmployee.gaps?.length || 0})` },
                { id: "assessments", label: `Assessments (${selectedEmployee.assessments?.length || 0})` },
                { id: "learning", label: `Courses (${selectedEmployee.learnings?.length || 0})` },
                { id: "actions", label: "Interventions" },
              ].map((tab) => (
                <button
                  key={tab.id}
                  style={{
                    padding: "12px 16px",
                    border: "none",
                    borderBottom: dossierTab === tab.id ? "2px solid #2563eb" : "2px solid transparent",
                    background: "transparent",
                    fontWeight: 700,
                    fontSize: "0.85rem",
                    color: dossierTab === tab.id ? "#2563eb" : "#64748b",
                    cursor: "pointer",
                  }}
                  onClick={() => setDossierTab(tab.id)}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <div className="modal-body">
              {dossierTab === "skills" && (
                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                  {selectedEmployee.skills?.length === 0 ? (
                    <p className="muted">No skills assessed yet for this employee.</p>
                  ) : (
                    selectedEmployee.skills.map((s) => (
                      <div
                        key={s.id}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          padding: "12px 14px",
                          border: "1px solid #e2e8f0",
                          borderRadius: "10px",
                          background: "#ffffff",
                        }}
                      >
                        <div>
                          <strong>{s.skillName}</strong>
                          <small className="muted" style={{ display: "block" }}>{s.categoryName}</small>
                        </div>
                        <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                          <span
                            className="badge"
                            style={{
                              background:
                                s.proficiencyLevel === "Expert" ? "#dbeafe" :
                                s.proficiencyLevel === "Advanced" ? "#dcfce7" :
                                s.proficiencyLevel === "Intermediate" ? "#fef3c7" : "#fee2e2",
                              color:
                                s.proficiencyLevel === "Expert" ? "#1e40af" :
                                s.proficiencyLevel === "Advanced" ? "#166534" :
                                s.proficiencyLevel === "Intermediate" ? "#92400e" : "#991b1b",
                            }}
                          >
                            Level {s.numericLevel} &bull; {s.proficiencyLevel}
                          </span>
                          {s.verified && (
                            <span className="badge" style={{ background: "#dcfce7", color: "#166534" }}>
                              Verified
                            </span>
                          )}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}

              {dossierTab === "gaps" && (
                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                  {selectedEmployee.gaps?.length === 0 ? (
                    <p className="muted">No active skill gaps identified for this employee.</p>
                  ) : (
                    selectedEmployee.gaps.map((g) => (
                      <div
                        key={g.skillId}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          padding: "12px 14px",
                          border: "1px solid #fee2e2",
                          borderRadius: "10px",
                          background: "#fff5f5",
                        }}
                      >
                        <div>
                          <strong style={{ color: "#991b1b" }}>{g.skillName}</strong>
                          <small className="muted" style={{ display: "block" }}>
                            Current: {g.currentProficiency} ({g.currentLevel}) &rarr; Required: {g.requiredProficiency} ({g.requiredLevel})
                          </small>
                        </div>
                        <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                          <span className="badge" style={{ background: "#fee2e2", color: "#991b1b" }}>
                            Deficit: -{g.gapSize}
                          </span>
                          <span className="badge" style={{ background: "#b91c1c", color: "#ffffff" }}>
                            {g.priority}
                          </span>
                          <button
                            className="btn btn--ghost"
                            style={{ padding: "4px 8px", minHeight: "28px", fontSize: "0.75rem" }}
                            onClick={() => {
                              onRequestReassessment({
                                employeeId: selectedEmployee.userId,
                                skillId: g.skillId,
                                employeeName: selectedEmployee.fullName,
                                skillName: g.skillName,
                              });
                            }}
                          >
                            Reassess
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}

              {dossierTab === "assessments" && (
                <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                  {selectedEmployee.assessments?.length === 0 ? (
                    <p className="muted">No assessment attempts found.</p>
                  ) : (
                    selectedEmployee.assessments.map((a) => (
                      <div
                        key={a.attemptId}
                        style={{
                          padding: "12px 14px",
                          border: "1px solid #e2e8f0",
                          borderRadius: "10px",
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                        }}
                      >
                        <div>
                          <strong>{a.skillName}</strong>
                          <small className="muted" style={{ display: "block" }}>
                            Target: {a.targetLevel} &bull; Started {new Date(a.startedAt).toLocaleDateString()}
                          </small>
                        </div>
                        <span
                          className="badge"
                          style={{
                            background: a.status === "COMPLETED" ? "#dcfce7" : "#fef3c7",
                            color: a.status === "COMPLETED" ? "#166534" : "#92400e",
                          }}
                        >
                          {a.status}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              )}

              {dossierTab === "learning" && (
                <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                  {selectedEmployee.learnings?.length === 0 ? (
                    <p className="muted">No course enrollments found.</p>
                  ) : (
                    selectedEmployee.learnings.map((l) => (
                      <div
                        key={l.id}
                        style={{
                          padding: "12px 14px",
                          border: "1px solid #e2e8f0",
                          borderRadius: "10px",
                          display: "flex",
                          flexDirection: "column",
                          gap: "6px",
                        }}
                      >
                        <div style={{ display: "flex", justifyContent: "space-between" }}>
                          <strong>{l.courseTitle}</strong>
                          <span className="badge">{l.status}</span>
                        </div>
                        <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem" }} className="muted">
                          <span>{l.provider} &bull; {l.level}</span>
                          <span>{l.progressPct}% Complete</span>
                        </div>
                        <div className="bar-track">
                          <div className="bar-fill" style={{ width: `${l.progressPct}%` }} />
                        </div>
                      </div>
                    ))
                  )}
                </div>
              )}

              {dossierTab === "actions" && (
                <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
                  <div style={{ padding: "14px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0" }}>
                    <h3 style={{ fontSize: "0.95rem", margin: "0 0 6px" }}>Manager Intervention Shortcuts</h3>
                    <p className="muted" style={{ fontSize: "0.82rem", margin: "0 0 12px" }}>
                      Take immediate action to guide this employee's competency development.
                    </p>
                    <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
                      <button
                        className="btn btn--primary"
                        onClick={() => {
                          onRequestReassessment({
                            employeeId: selectedEmployee.userId,
                            employeeName: selectedEmployee.fullName,
                          });
                        }}
                      >
                        <Icon name="refresh" size={16} /> Request Reassessment
                      </button>
                      <button
                        className="btn btn--ghost"
                        onClick={() => {
                          onRecommendCourse({
                            employeeId: selectedEmployee.userId,
                            employeeName: selectedEmployee.fullName,
                          });
                        }}
                      >
                        <Icon name="book" size={16} /> Recommend Course
                      </button>
                      <button
                        className="btn btn--ghost"
                        onClick={() => {
                          onReferMentor({
                            employeeId: selectedEmployee.userId,
                            employeeName: selectedEmployee.fullName,
                          });
                        }}
                      >
                        <Icon name="users" size={16} /> Refer Mentor
                      </button>
                    </div>
                  </div>

                  {/* History of Reassessment Requests for this employee */}
                  <div>
                    <h4 style={{ fontSize: "0.9rem", margin: "0 0 8px" }}>Reassessment History</h4>
                    {selectedEmployee.reassessmentRequests?.length === 0 ? (
                      <p className="muted" style={{ fontSize: "0.82rem" }}>No reassessments requested yet.</p>
                    ) : (
                      <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                        {selectedEmployee.reassessmentRequests.map((r) => (
                          <div key={r.id} style={{ padding: "8px 12px", border: "1px solid #e2e8f0", borderRadius: "8px", fontSize: "0.84rem" }}>
                            <strong>{r.skillName}</strong> &bull; <span className="muted">{r.reason}</span> ({r.status})
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn btn--ghost" onClick={() => setSelectedEmployee(null)}>
                Close Dossier
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
