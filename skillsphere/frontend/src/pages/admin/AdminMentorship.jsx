import { useCallback, useEffect, useState } from "react";
import Alert from "../../components/Alert.jsx";
import Spinner from "../../components/Spinner.jsx";
import { parseApiError } from "../../services/authService.js";
import {
  adminApproveMentor,
  adminAssignMentor,
  adminEndAssignment,
  adminListAssignments,
  adminListEligibleEmployees,
  adminListMentors,
  adminListUnassignedEmployees,
  adminUpdateMentorStatus,
} from "../../services/mentorshipService.js";

const fmtDate = (iso) => (iso ? new Date(iso).toLocaleDateString([], { dateStyle: "medium" }) : "—");

export default function AdminMentorship() {
  const [subTab, setSubTab] = useState("mentors"); // "mentors" | "assignments"
  const [mentors, setMentors] = useState([]);
  const [eligibleEmployees, setEligibleEmployees] = useState([]);
  const [unassignedEmployees, setUnassignedEmployees] = useState([]);
  const [assignments, setAssignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Forms
  const [approveForm, setApproveForm] = useState({
    userId: "",
    headline: "",
    expertiseAreas: "",
    maxMentees: 3,
  });

  const [assignForm, setAssignForm] = useState({
    mentorUserId: "",
    employeeUserId: "",
    notes: "",
  });

  const [endModalAssignment, setEndModalAssignment] = useState(null);
  const [endStatus, setEndStatus] = useState("COMPLETED");
  const [endNotes, setEndNotes] = useState("");

  const loadAll = useCallback(() => {
    setLoading(true);
    Promise.all([
      adminListMentors(),
      adminListEligibleEmployees(),
      adminListUnassignedEmployees(),
      adminListAssignments(),
    ])
      .then(([mList, eligList, unassignedList, assignList]) => {
        setMentors(mList);
        setEligibleEmployees(eligList.filter((e) => !e.isApprovedMentor));
        setUnassignedEmployees(unassignedList);
        setAssignments(assignList);
      })
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  const handleApproveMentor = async (e) => {
    e.preventDefault();
    if (!approveForm.userId) {
      setError("Please select an employee to approve as mentor.");
      return;
    }
    setError("");
    setSuccess("");
    try {
      await adminApproveMentor(Number(approveForm.userId), {
        headline: approveForm.headline.trim(),
        expertiseAreas: approveForm.expertiseAreas.trim(),
        maxMentees: Number(approveForm.maxMentees),
      });
      setSuccess("Employee approved as mentor successfully!");
      setApproveForm({ userId: "", headline: "", expertiseAreas: "", maxMentees: 3 });
      loadAll();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleToggleMentorStatus = async (mentor) => {
    setError("");
    setSuccess("");
    try {
      await adminUpdateMentorStatus(mentor.userId, !mentor.isActive);
      setSuccess(`Mentor ${mentor.fullName} is now ${!mentor.isActive ? "Active" : "Inactive"}.`);
      loadAll();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleCreateAssignment = async (e) => {
    e.preventDefault();
    if (!assignForm.mentorUserId || !assignForm.employeeUserId) {
      setError("Please select both a mentor and an employee.");
      return;
    }
    setError("");
    setSuccess("");
    try {
      await adminAssignMentor({
        mentorUserId: Number(assignForm.mentorUserId),
        employeeUserId: Number(assignForm.employeeUserId),
        notes: assignForm.notes.trim() || null,
      });
      setSuccess("Mentorship assignment created successfully!");
      setAssignForm({ mentorUserId: "", employeeUserId: "", notes: "" });
      loadAll();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleEndAssignment = async (e) => {
    e.preventDefault();
    if (!endModalAssignment) return;
    setError("");
    setSuccess("");
    try {
      await adminEndAssignment(endModalAssignment.id, {
        status: endStatus,
        notes: endNotes.trim() || null,
      });
      setSuccess(`Assignment marked as ${endStatus}.`);
      setEndModalAssignment(null);
      setEndNotes("");
      loadAll();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  return (
    <section className="panel" aria-labelledby="mentorship-admin-h" style={{ marginTop: "1.5rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "10px" }}>
        <div>
          <h2 id="mentorship-admin-h" style={{ margin: 0 }}>Mentorship Governance</h2>
          <p className="muted" style={{ margin: "4px 0 0" }}>Manage expert eligibility, capacity limits, and mentor-to-employee pairings.</p>
        </div>

        <div className="tabs" style={{ margin: 0 }}>
          <button
            className={`tab-btn ${subTab === "mentors" ? "is-active" : ""}`}
            onClick={() => setSubTab("mentors")}
          >
            Mentors ({mentors.length})
          </button>
          <button
            className={`tab-btn ${subTab === "assignments" ? "is-active" : ""}`}
            onClick={() => setSubTab("assignments")}
          >
            Assignments ({assignments.length})
          </button>
        </div>
      </div>

      <div style={{ marginTop: "1rem" }}>
        <Alert type="error">{error}</Alert>
        <Alert type="success">{success}</Alert>
      </div>

      {loading ? (
        <div style={{ textAlign: "center", padding: "2rem" }}><Spinner /> Loading mentorship data…</div>
      ) : subTab === "mentors" ? (
        <div>
          {/* Approved Mentors Table */}
          <div className="table-wrap" style={{ marginTop: "1rem" }}>
            <table>
              <thead>
                <tr>
                  <th>Mentor Name</th>
                  <th>Headline & Expertise</th>
                  <th>Capacity</th>
                  <th>Approved By</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {mentors.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: "center", color: "var(--color-muted)", padding: "1.5rem" }}>
                      No approved mentors in the system. Use the form below to approve an employee as a mentor.
                    </td>
                  </tr>
                ) : (
                  mentors.map((m) => (
                    <tr key={m.mentorProfileId}>
                      <td>
                        <strong>{m.fullName}</strong>
                        <small>{m.email}</small>
                      </td>
                      <td>
                        <strong>"{m.headline}"</strong>
                        <small>{m.expertiseAreas.join(", ")}</small>
                      </td>
                      <td>
                        <span className={`badge ${m.currentMenteesCount >= m.maxMentees ? "badge--warn" : "badge--success"}`}>
                          {m.currentMenteesCount} / {m.maxMentees} mentees
                        </span>
                      </td>
                      <td>
                        <small>{m.approvedByName}</small>
                        <small>{fmtDate(m.createdAt)}</small>
                      </td>
                      <td>
                        <span className={`badge ${m.isActive ? "badge--success" : "badge--danger"}`}>
                          {m.isActive ? "Active" : "Inactive"}
                        </span>
                      </td>
                      <td>
                        <button
                          className={`btn ${m.isActive ? "btn--ghost" : "btn--primary"}`}
                          style={{ padding: "4px 8px", fontSize: "0.8rem" }}
                          onClick={() => handleToggleMentorStatus(m)}
                        >
                          {m.isActive ? "Deactivate" : "Activate"}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Form: Approve New Mentor */}
          <div className="panel" style={{ marginTop: "1.5rem", background: "var(--color-bg)" }}>
            <h3 style={{ margin: 0 }}>Approve Employee as Mentor</h3>
            <p className="muted" style={{ margin: "4px 0 1rem" }}>
              Select an eligible employee, specify their mentoring focus areas, and set their maximum mentee capacity.
            </p>

            <form onSubmit={handleApproveMentor} className="form">
              <div className="form__grid">
                <label className="field">
                  <span>Select Employee</span>
                  <select
                    value={approveForm.userId}
                    onChange={(e) => setApproveForm({ ...approveForm, userId: e.target.value })}
                    required
                  >
                    <option value="">-- Choose Eligible Employee --</option>
                    {eligibleEmployees.map((e) => (
                      <option key={e.userId} value={e.userId}>
                        {e.fullName} ({e.email}) — {e.jobRole} [{e.department || "General"}]
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field">
                  <span>Max Mentee Capacity</span>
                  <input
                    type="number"
                    min={1}
                    max={20}
                    value={approveForm.maxMentees}
                    onChange={(e) => setApproveForm({ ...approveForm, maxMentees: e.target.value })}
                    required
                  />
                </label>
              </div>

              <label className="field">
                <span>Professional Headline</span>
                <input
                  type="text"
                  placeholder="e.g. Senior Backend Engineer specializing in Spring Cloud & Microservices"
                  value={approveForm.headline}
                  onChange={(e) => setApproveForm({ ...approveForm, headline: e.target.value })}
                  required
                />
              </label>

              <label className="field">
                <span>Expertise Areas (comma-separated)</span>
                <input
                  type="text"
                  placeholder="e.g. Java, Spring Boot, PostgreSQL, Docker, Microservices"
                  value={approveForm.expertiseAreas}
                  onChange={(e) => setApproveForm({ ...approveForm, expertiseAreas: e.target.value })}
                  required
                />
              </label>

              <button type="submit" className="btn btn--primary" style={{ alignSelf: "flex-start" }}>
                Approve as Mentor
              </button>
            </form>
          </div>
        </div>
      ) : (
        <div>
          {/* Mentorship Assignments Table */}
          <div className="table-wrap" style={{ marginTop: "1rem" }}>
            <table>
              <thead>
                <tr>
                  <th>Mentor</th>
                  <th>Assigned Employee</th>
                  <th>Assigned Date</th>
                  <th>Status</th>
                  <th>Notes</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {assignments.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: "center", color: "var(--color-muted)", padding: "1.5rem" }}>
                      No mentorship pairings created yet. Use the form below to pair an employee with a mentor.
                    </td>
                  </tr>
                ) : (
                  assignments.map((a) => (
                    <tr key={a.id}>
                      <td>
                        <strong>{a.mentorName}</strong>
                        <small>{a.mentorEmail}</small>
                      </td>
                      <td>
                        <strong>{a.employeeName}</strong>
                        <small>{a.employeeEmail}</small>
                      </td>
                      <td>{fmtDate(a.assignedAt)}</td>
                      <td>
                        <span className={`badge ${a.status === "ACTIVE" ? "badge--success" : a.status === "COMPLETED" ? "badge--warn" : "badge--danger"}`}>
                          {a.status}
                        </span>
                      </td>
                      <td>
                        <small style={{ maxWidth: "200px", display: "block" }}>{a.notes || "—"}</small>
                      </td>
                      <td>
                        {a.status === "ACTIVE" ? (
                          <button
                            className="btn btn--ghost"
                            style={{ padding: "4px 8px", fontSize: "0.8rem", color: "var(--color-danger)" }}
                            onClick={() => { setEndModalAssignment(a); setEndNotes(""); setEndStatus("COMPLETED"); }}
                          >
                            End Assignment
                          </button>
                        ) : (
                          <small className="muted">Ended {fmtDate(a.endedAt)}</small>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Form: Create Assignment */}
          <div className="panel" style={{ marginTop: "1.5rem", background: "var(--color-bg)" }}>
            <h3 style={{ margin: 0 }}>Assign Mentor to Employee</h3>
            <p className="muted" style={{ margin: "4px 0 1rem" }}>
              Pair an employee who currently lacks an active mentor with an approved mentor who has available capacity.
            </p>

            <form onSubmit={handleCreateAssignment} className="form">
              <div className="form__grid">
                <label className="field">
                  <span>Select Employee (Unassigned)</span>
                  <select
                    value={assignForm.employeeUserId}
                    onChange={(e) => setAssignForm({ ...assignForm, employeeUserId: e.target.value })}
                    required
                  >
                    <option value="">-- Choose Unassigned Employee --</option>
                    {unassignedEmployees.map((e) => (
                      <option key={e.userId} value={e.userId}>
                        {e.fullName} ({e.email}) — {e.jobRole}
                      </option>
                    ))}
                  </select>
                </label>

                <label className="field">
                  <span>Select Active Mentor</span>
                  <select
                    value={assignForm.mentorUserId}
                    onChange={(e) => setAssignForm({ ...assignForm, mentorUserId: e.target.value })}
                    required
                  >
                    <option value="">-- Choose Available Mentor --</option>
                    {mentors
                      .filter((m) => m.isActive && m.currentMenteesCount < m.maxMentees)
                      .map((m) => (
                        <option key={m.userId} value={m.userId}>
                          {m.fullName} ({m.currentMenteesCount}/{m.maxMentees} mentees) — {m.headline}
                        </option>
                      ))}
                  </select>
                </label>
              </div>

              <label className="field">
                <span>Assignment Goals & Alignment Notes (Optional)</span>
                <textarea
                  rows={2}
                  placeholder="e.g. Focus on Java backend transition and microservices mentorship for Q3..."
                  value={assignForm.notes}
                  onChange={(e) => setAssignForm({ ...assignForm, notes: e.target.value })}
                />
              </label>

              <button type="submit" className="btn btn--primary" style={{ alignSelf: "flex-start" }}>
                Confirm Mentorship Assignment
              </button>
            </form>
          </div>

          {/* End Assignment Modal */}
          {endModalAssignment && (
            <div className="modal-backdrop">
              <div className="panel modal-card" style={{ maxWidth: "480px" }}>
                <h3>End Mentorship Assignment</h3>
                <p className="muted">
                  Pairing between <strong>{endModalAssignment.mentorName}</strong> and <strong>{endModalAssignment.employeeName}</strong>
                </p>

                <form onSubmit={handleEndAssignment} className="form" style={{ marginTop: "1rem" }}>
                  <label className="field">
                    <span>Outcome Status</span>
                    <select value={endStatus} onChange={(e) => setEndStatus(e.target.value)}>
                      <option value="COMPLETED">Completed (Mentorship goals fulfilled)</option>
                      <option value="TERMINATED">Terminated (Early conclusion or reassignment)</option>
                    </select>
                  </label>

                  <label className="field">
                    <span>Closing Notes (Optional)</span>
                    <textarea
                      rows={3}
                      placeholder="Reason for conclusion or transition notes..."
                      value={endNotes}
                      onChange={(e) => setEndNotes(e.target.value)}
                    />
                  </label>

                  <div style={{ display: "flex", gap: "8px", justifyContent: "flex-end", marginTop: "1rem" }}>
                    <button type="button" className="btn btn--ghost" onClick={() => setEndModalAssignment(null)}>
                      Cancel
                    </button>
                    <button type="submit" className="btn btn--primary">
                      Confirm End Assignment
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
