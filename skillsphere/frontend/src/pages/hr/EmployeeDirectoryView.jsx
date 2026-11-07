import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getHrEmployees, getHrEmployeeDossier } from "../../services/hrService.js";

export default function EmployeeDirectoryView() {
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [department, setDepartment] = useState("ALL");
  const [role, setRole] = useState("ALL");

  // Dossier modal
  const [selectedUser, setSelectedUser] = useState(null);
  const [dossier, setDossier] = useState(null);
  const [dossierLoading, setDossierLoading] = useState(false);
  const [dossierError, setDossierError] = useState("");

  const loadEmployees = async () => {
    try {
      setLoading(true);
      setError("");
      const params = {};
      if (search.trim()) params.search = search.trim();
      if (department !== "ALL") params.department = department;
      if (role !== "ALL") params.role = role;
      const res = await getHrEmployees(params);
      setEmployees(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load employee directory.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEmployees();
  }, [department, role]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    loadEmployees();
  };

  const handleOpenDossier = async (user) => {
    setSelectedUser(user);
    setDossier(null);
    setDossierError("");
    setDossierLoading(true);
    try {
      const res = await getHrEmployeeDossier(user.id);
      setDossier(res);
    } catch (err) {
      setDossierError(err?.response?.data?.message || err?.message || "Failed to load employee dossier.");
    } finally {
      setDossierLoading(false);
    }
  };

  const handleCloseDossier = () => {
    setSelectedUser(null);
    setDossier(null);
  };

  const departments = Array.from(new Set(employees.map((e) => e.departmentName).filter(Boolean))).sort();

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Employee Directory</h3>
            <p className="hr-card-subtitle">
              Comprehensive workforce registry with assessment and learning profiles
            </p>
          </div>
          <button className="btn-secondary btn-sm" onClick={loadEmployees}>
            <Icon name="refresh" size={14} /> Refresh
          </button>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}

          {/* Filter Bar */}
          <form className="hr-filter-bar" onSubmit={handleSearchSubmit}>
            <input
              type="text"
              className="form-control hr-search-input"
              placeholder="Search by employee name or email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="form-control"
              style={{ width: "auto" }}
              value={department}
              onChange={(e) => setDepartment(e.target.value)}
            >
              <option value="ALL">All Departments</option>
              {departments.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
            <select
              className="form-control"
              style={{ width: "auto" }}
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              <option value="ALL">All Roles</option>
              <option value="EMPLOYEE">Employee</option>
              <option value="MANAGER">Manager</option>
              <option value="MENTOR">Mentor</option>
              <option value="DEPARTMENT_HEAD">Department Head</option>
              <option value="HR_SPECIALIST">HR Specialist</option>
            </select>
            <button type="submit" className="btn-primary">
              <Icon name="search" size={14} /> Search
            </button>
          </form>

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading directory...</h4>
            </div>
          ) : employees.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="users" size={32} />
              <h4>No employees match your criteria</h4>
              <p>Try adjusting your search terms or filters.</p>
            </div>
          ) : (
            <div className="hr-table-wrap">
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>Employee</th>
                    <th>Department</th>
                    <th>Job Title</th>
                    <th>Role</th>
                    <th>Assessment Status</th>
                    <th>Verified Skills</th>
                    <th>Courses</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {employees.map((emp) => (
                    <tr key={emp.id}>
                      <td>
                        <div style={{ fontWeight: 600, color: "var(--hr-ink)" }}>{emp.fullName}</div>
                        <div style={{ fontSize: "12px", color: "var(--hr-soft)" }}>{emp.email}</div>
                      </td>
                      <td>{emp.departmentName}</td>
                      <td>{emp.jobRole}</td>
                      <td>
                        <span className="status-pill neutral">{emp.role}</span>
                      </td>
                      <td>
                        <span className={`status-pill ${emp.assessed ? "success" : "warning"}`}>
                          {emp.assessed ? "Assessed" : "Pending Baseline"}
                        </span>
                      </td>
                      <td>
                        <span className="status-pill purple">{emp.verifiedSkillsCount} Verified</span>
                      </td>
                      <td>{emp.enrolledCoursesCount} Enrolled</td>
                      <td>
                        <button
                          className="btn-secondary btn-sm"
                          onClick={() => handleOpenDossier(emp)}
                        >
                          <Icon name="user" size={13} /> View Dossier
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

      {/* Employee Dossier Modal */}
      {selectedUser && (
        <div className="hr-modal-overlay" onClick={handleCloseDossier}>
          <div className="hr-modal" style={{ maxWidth: 840 }} onClick={(e) => e.stopPropagation()}>
            <div className="hr-modal-header">
              <div>
                <h3>Employee Dossier: {selectedUser.fullName}</h3>
                <span style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                  {selectedUser.jobRole} • {selectedUser.departmentName} • {selectedUser.email}
                </span>
              </div>
              <button className="hr-modal-close" onClick={handleCloseDossier}>
                &times;
              </button>
            </div>

            <div className="hr-modal-body">
              {dossierLoading && (
                <div className="hr-empty-state">
                  <Icon name="spinner" className="spin" size={24} />
                  <p>Loading dossier details...</p>
                </div>
              )}

              {dossierError && <Alert type="danger" message={dossierError} />}

              {dossier && (
                <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                  {/* Bio & Details */}
                  {dossier.bio && (
                    <div style={{ padding: "12px 16px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--hr-line)" }}>
                      <div style={{ fontWeight: 600, fontSize: "13px", marginBottom: "4px" }}>Professional Bio</div>
                      <div style={{ fontSize: "13px", color: "var(--hr-soft)" }}>{dossier.bio}</div>
                    </div>
                  )}

                  {/* Skills Inventory */}
                  <div>
                    <h4 style={{ margin: "0 0 10px", fontSize: "14px", fontWeight: 700 }}>
                      Skill Competencies ({dossier.skills.length})
                    </h4>
                    {dossier.skills.length === 0 ? (
                      <p style={{ fontSize: "13px", color: "var(--hr-faint)" }}>No skill competencies logged yet.</p>
                    ) : (
                      <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
                        {dossier.skills.map((s, idx) => (
                          <div
                            key={idx}
                            style={{
                              padding: "6px 12px",
                              background: s.verified ? "var(--hr-emerald-tint)" : "var(--hr-primary-tint)",
                              border: `1px solid ${s.verified ? "rgba(5, 150, 105, 0.2)" : "rgba(124, 58, 237, 0.2)"}`,
                              borderRadius: "6px",
                              fontSize: "12.5px",
                            }}
                          >
                            <span style={{ fontWeight: 600, color: s.verified ? "var(--hr-emerald)" : "var(--hr-primary-dark)" }}>
                              {s.skillName}
                            </span>
                            <span style={{ marginLeft: "6px", color: "var(--hr-soft)" }}>
                              • {s.proficiencyLevel} {s.verified && "(Verified)"}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Assessment History */}
                  <div>
                    <h4 style={{ margin: "0 0 10px", fontSize: "14px", fontWeight: 700 }}>
                      Official Assessment History ({dossier.assessmentHistory.length})
                    </h4>
                    {dossier.assessmentHistory.length === 0 ? (
                      <p style={{ fontSize: "13px", color: "var(--hr-faint)" }}>No assessments completed.</p>
                    ) : (
                      <div className="hr-table-wrap">
                        <table className="hr-table">
                          <thead>
                            <tr>
                              <th>Skill</th>
                              <th>Target Level</th>
                              <th>Score</th>
                              <th>Outcome</th>
                              <th>Date</th>
                            </tr>
                          </thead>
                          <tbody>
                            {dossier.assessmentHistory.map((a) => (
                              <tr key={a.attemptId}>
                                <td style={{ fontWeight: 600 }}>{a.skillName}</td>
                                <td>{a.targetLevel}</td>
                                <td>{a.score != null ? `${a.score}%` : "In Progress"}</td>
                                <td>
                                  <span className={`status-pill ${a.passed ? "success" : "danger"}`}>
                                    {a.passed ? "Passed" : "Needs Practice"}
                                  </span>
                                </td>
                                <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                                  {a.completedAt ? new Date(a.completedAt).toLocaleDateString() : "Pending"}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>

                  {/* Course Progress */}
                  <div>
                    <h4 style={{ margin: "0 0 10px", fontSize: "14px", fontWeight: 700 }}>
                      Enrolled Courses ({dossier.enrolledCourses.length})
                    </h4>
                    {dossier.enrolledCourses.length === 0 ? (
                      <p style={{ fontSize: "13px", color: "var(--hr-faint)" }}>No courses currently in progress.</p>
                    ) : (
                      <div className="hr-table-wrap">
                        <table className="hr-table">
                          <thead>
                            <tr>
                              <th>Course</th>
                              <th>Progress</th>
                              <th>Status</th>
                              <th>Enrolled</th>
                            </tr>
                          </thead>
                          <tbody>
                            {dossier.enrolledCourses.map((c) => (
                              <tr key={c.enrollmentId}>
                                <td style={{ fontWeight: 600 }}>{c.courseTitle}</td>
                                <td>{c.progressPct}%</td>
                                <td>
                                  <span className={`status-pill ${c.status === "COMPLETED" ? "success" : "info"}`}>
                                    {c.status}
                                  </span>
                                </td>
                                <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                                  {c.enrolledAt ? new Date(c.enrolledAt).toLocaleDateString() : ""}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>

                  {/* Training Programmes */}
                  <div>
                    <h4 style={{ margin: "0 0 10px", fontSize: "14px", fontWeight: 700 }}>
                      Training Programme Attendance ({dossier.trainingProgrammes.length})
                    </h4>
                    {dossier.trainingProgrammes.length === 0 ? (
                      <p style={{ fontSize: "13px", color: "var(--hr-faint)" }}>No training programmes attended.</p>
                    ) : (
                      <div className="hr-table-wrap">
                        <table className="hr-table">
                          <thead>
                            <tr>
                              <th>Programme</th>
                              <th>Status</th>
                              <th>Enrolled Date</th>
                            </tr>
                          </thead>
                          <tbody>
                            {dossier.trainingProgrammes.map((t) => (
                              <tr key={t.enrollmentId}>
                                <td style={{ fontWeight: 600 }}>{t.programmeTitle}</td>
                                <td>
                                  <span className={`status-pill ${t.status === "ATTENDED" ? "success" : "info"}`}>
                                    {t.status}
                                  </span>
                                </td>
                                <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                                  {t.enrolledAt ? new Date(t.enrolledAt).toLocaleDateString() : ""}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>

                  {/* Mentorship Participations */}
                  <div>
                    <h4 style={{ margin: "0 0 10px", fontSize: "14px", fontWeight: 700 }}>
                      Mentorship Engagements ({dossier.mentorshipParticipations.length})
                    </h4>
                    {dossier.mentorshipParticipations.length === 0 ? (
                      <p style={{ fontSize: "13px", color: "var(--hr-faint)" }}>No mentorship assignments logged.</p>
                    ) : (
                      <div className="hr-table-wrap">
                        <table className="hr-table">
                          <thead>
                            <tr>
                              <th>Mentor</th>
                              <th>Status</th>
                              <th>Assigned Date</th>
                            </tr>
                          </thead>
                          <tbody>
                            {dossier.mentorshipParticipations.map((m) => (
                              <tr key={m.assignmentId}>
                                <td style={{ fontWeight: 600 }}>{m.mentorName}</td>
                                <td>
                                  <span className="status-pill success">{m.status}</span>
                                </td>
                                <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                                  {m.assignedAt ? new Date(m.assignedAt).toLocaleDateString() : ""}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>

                  <div style={{ padding: "10px 14px", background: "#f1f5f9", borderRadius: "6px", fontSize: "12px", color: "var(--hr-soft)" }}>
                    <strong>Privacy & Integrity Policy:</strong> HR Specialist view is strictly restricted from private mentor-mentee chat sessions and confidential mentor preparation notes. Official assessment scores and verified skills represent certified outcomes and cannot be mutated by HR.
                  </div>
                </div>
              )}
            </div>

            <div className="hr-modal-footer">
              <button className="btn-secondary" onClick={handleCloseDossier}>
                Close Dossier
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
