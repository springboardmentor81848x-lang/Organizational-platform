import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getHrTrainingProgrammes,
  createHrTrainingProgramme,
  updateHrTrainingProgramme,
  getHrProgrammeEnrollments,
  enrollEmployeeInProgramme,
  updateHrProgrammeEnrollmentStatus,
  getHrEmployees,
  getHrCompetencies,
  getHrCourses,
} from "../../services/hrService.js";
import { getDepartments } from "../../services/platformService.js";

const STATUSES = ["UPCOMING", "ACTIVE", "COMPLETED", "CANCELLED"];

export default function TrainingProgrammesView() {
  const [programmes, setProgrammes] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [skills, setSkills] = useState([]);
  const [courses, setCourses] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Create / Edit modal
  const [showModal, setShowModal] = useState(false);
  const [editingProg, setEditingProg] = useState(null);
  const [formTitle, setFormTitle] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formDeptId, setFormDeptId] = useState("");
  const [formSkillId, setFormSkillId] = useState("");
  const [formCourseId, setFormCourseId] = useState("");
  const [formStartDate, setFormStartDate] = useState("");
  const [formEndDate, setFormEndDate] = useState("");
  const [formCapacity, setFormCapacity] = useState(25);
  const [formTrainer, setFormTrainer] = useState("");
  const [formStatus, setFormStatus] = useState("UPCOMING");
  const [saving, setSaving] = useState(false);

  // Enrollments modal
  const [selectedProg, setSelectedProg] = useState(null);
  const [enrollments, setEnrollments] = useState([]);
  const [enrollLoading, setEnrollLoading] = useState(false);
  const [enrollUserId, setEnrollUserId] = useState("");
  const [enrolling, setEnrolling] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [pList, dList, sList, cList, eList] = await Promise.all([
        getHrTrainingProgrammes(),
        getDepartments().catch(() => []),
        getHrCompetencies(),
        getHrCourses(),
        getHrEmployees(),
      ]);
      setProgrammes(pList);
      setDepartments(dList);
      setSkills(sList);
      setCourses(cList);
      setEmployees(eList);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load training programmes.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenAdd = () => {
    setEditingProg(null);
    setFormTitle("");
    setFormDesc("");
    setFormDeptId(departments[0]?.id || "");
    setFormSkillId(skills[0]?.id || "");
    setFormCourseId(courses[0]?.id || "");
    setFormStartDate(new Date(Date.now() + 86400000 * 2).toISOString().split("T")[0]);
    setFormEndDate(new Date(Date.now() + 86400000 * 5).toISOString().split("T")[0]);
    setFormCapacity(25);
    setFormTrainer("");
    setFormStatus("UPCOMING");
    setShowModal(true);
  };

  const handleOpenEdit = (p) => {
    setEditingProg(p);
    setFormTitle(p.title);
    setFormDesc(p.description || "");
    setFormDeptId(p.departmentId || "");
    setFormSkillId(p.skillId || "");
    setFormCourseId(p.courseId || "");
    setFormStartDate(p.startDate || "");
    setFormEndDate(p.endDate || "");
    setFormCapacity(p.capacity || 25);
    setFormTrainer(p.trainerName || "");
    setFormStatus(p.status || "UPCOMING");
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingProg(null);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formTitle.trim() || !formStartDate || !formEndDate) {
      setError("Please fill in programme title, start date, and end date.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      const payload = {
        title: formTitle.trim(),
        description: formDesc.trim(),
        departmentId: formDeptId ? Number(formDeptId) : null,
        skillId: formSkillId ? Number(formSkillId) : null,
        courseId: formCourseId ? Number(formCourseId) : null,
        startDate: formStartDate,
        endDate: formEndDate,
        capacity: Number(formCapacity),
        trainerName: formTrainer.trim(),
        status: formStatus,
      };

      if (editingProg) {
        await updateHrTrainingProgramme(editingProg.id, payload);
        setSuccess(`Updated training programme "${formTitle}".`);
      } else {
        await createHrTrainingProgramme(payload);
        setSuccess(`Created training programme "${formTitle}".`);
      }
      setShowModal(false);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to save programme.");
    } finally {
      setSaving(false);
    }
  };

  // Open enrollments
  const handleOpenEnrollments = async (prog) => {
    setSelectedProg(prog);
    setEnrollments([]);
    setEnrollUserId(employees[0]?.id || "");
    setEnrollLoading(true);
    try {
      const res = await getHrProgrammeEnrollments(prog.id);
      setEnrollments(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load enrollments.");
    } finally {
      setEnrollLoading(false);
    }
  };

  const handleCloseEnrollments = () => {
    setSelectedProg(null);
    setEnrollments([]);
  };

  const handleEnrollEmployee = async (e) => {
    e.preventDefault();
    if (!enrollUserId) return;

    try {
      setEnrolling(true);
      setError("");
      await enrollEmployeeInProgramme(selectedProg.id, { userId: Number(enrollUserId) });
      setSuccess("Employee enrolled in programme successfully.");
      const updated = await getHrProgrammeEnrollments(selectedProg.id);
      setEnrollments(updated);
      loadData(); // update capacity counts
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to enroll employee.");
    } finally {
      setEnrolling(false);
    }
  };

  const handleUpdateEnrollmentStatus = async (enrollmentId, newStatus) => {
    try {
      setError("");
      await updateHrProgrammeEnrollmentStatus(enrollmentId, { status: newStatus });
      const updated = await getHrProgrammeEnrollments(selectedProg.id);
      setEnrollments(updated);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to update enrollment status.");
    }
  };

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Training Programme Management</h3>
            <p className="hr-card-subtitle">
              Coordinate enterprise workshops, manage cohorts, track participant attendance and delivery
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <button className="btn-secondary btn-sm" onClick={loadData}>
              <Icon name="refresh" size={14} /> Refresh
            </button>
            <button className="btn-primary btn-sm" onClick={handleOpenAdd}>
              <Icon name="plus" size={14} /> Schedule Programme
            </button>
          </div>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}
          {success && <Alert type="success" message={success} />}

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading training programmes...</h4>
            </div>
          ) : programmes.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="calendar" size={32} />
              <h4>No programmes scheduled</h4>
              <p>Schedule your first corporate training programme or cohort bootcamp.</p>
            </div>
          ) : (
            <div className="hr-table-wrap">
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>Programme</th>
                    <th>Target Department</th>
                    <th>Competency</th>
                    <th>Dates</th>
                    <th>Trainer</th>
                    <th>Cohort Capacity</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {programmes.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <div style={{ fontWeight: 600, color: "var(--hr-ink)" }}>{p.title}</div>
                        {p.courseTitle && (
                          <div style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                            Course: {p.courseTitle}
                          </div>
                        )}
                      </td>
                      <td>{p.departmentName}</td>
                      <td>
                        <span className="status-pill purple">{p.skillName || "Cross-Functional"}</span>
                      </td>
                      <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                        {p.startDate} to {p.endDate}
                      </td>
                      <td>{p.trainerName || "Staff Trainer"}</td>
                      <td>
                        <span style={{ fontWeight: 600 }}>{p.enrolledCount}</span> / {p.capacity} enrolled
                      </td>
                      <td>
                        <span
                          className={`status-pill ${
                            p.status === "ACTIVE"
                              ? "success"
                              : p.status === "UPCOMING"
                              ? "info"
                              : p.status === "COMPLETED"
                              ? "neutral"
                              : "danger"
                          }`}
                        >
                          {p.status}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "6px" }}>
                          <button
                            className="btn-primary btn-sm"
                            onClick={() => handleOpenEnrollments(p)}
                          >
                            Participants
                          </button>
                          <button
                            className="btn-secondary btn-sm"
                            onClick={() => handleOpenEdit(p)}
                          >
                            Edit
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Schedule / Edit Modal */}
      {showModal && (
        <div className="hr-modal-overlay" onClick={handleCloseModal}>
          <div className="hr-modal" style={{ maxWidth: 740 }} onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleSave}>
              <div className="hr-modal-header">
                <h3>{editingProg ? "Edit Training Programme" : "Schedule New Training Programme"}</h3>
                <button type="button" className="hr-modal-close" onClick={handleCloseModal}>
                  &times;
                </button>
              </div>

              <div className="hr-modal-body">
                <div className="form-group">
                  <label className="form-label">Programme Title *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. AWS Cloud Solutions Architect Bootcamp"
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                  <div className="form-group">
                    <label className="form-label">Target Department</label>
                    <select
                      className="form-control"
                      value={formDeptId}
                      onChange={(e) => setFormDeptId(e.target.value)}
                    >
                      <option value="">All Departments (Enterprise)</option>
                      {departments.map((d) => (
                        <option key={d.id} value={d.id}>
                          {d.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Focus Competency</label>
                    <select
                      className="form-control"
                      value={formSkillId}
                      onChange={(e) => setFormSkillId(e.target.value)}
                    >
                      <option value="">Cross-Functional</option>
                      {skills.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.name} ({s.categoryName})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                  <div className="form-group">
                    <label className="form-label">Start Date *</label>
                    <input
                      type="date"
                      className="form-control"
                      value={formStartDate}
                      onChange={(e) => setFormStartDate(e.target.value)}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">End Date *</label>
                    <input
                      type="date"
                      className="form-control"
                      value={formEndDate}
                      onChange={(e) => setFormEndDate(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "16px" }}>
                  <div className="form-group">
                    <label className="form-label">Cohort Capacity *</label>
                    <input
                      type="number"
                      min={1}
                      className="form-control"
                      value={formCapacity}
                      onChange={(e) => setFormCapacity(e.target.value)}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Trainer / Lead</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Dr. Alex Vance"
                      value={formTrainer}
                      onChange={(e) => setFormTrainer(e.target.value)}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Programme Status</label>
                    <select
                      className="form-control"
                      value={formStatus}
                      onChange={(e) => setFormStatus(e.target.value)}
                    >
                      {STATUSES.map((st) => (
                        <option key={st} value={st}>
                          {st}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Associated Course Material</label>
                  <select
                    className="form-control"
                    value={formCourseId}
                    onChange={(e) => setFormCourseId(e.target.value)}
                  >
                    <option value="">No course link</option>
                    {courses.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.title} ({c.provider})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Programme Description & Prerequisites</label>
                  <textarea
                    className="form-control"
                    placeholder="Objectives, venue or video call details..."
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                  />
                </div>
              </div>

              <div className="hr-modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? "Saving..." : editingProg ? "Update Programme" : "Schedule Programme"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Participants & Cohort Modal */}
      {selectedProg && (
        <div className="hr-modal-overlay" onClick={handleCloseEnrollments}>
          <div className="hr-modal" style={{ maxWidth: 820 }} onClick={(e) => e.stopPropagation()}>
            <div className="hr-modal-header">
              <div>
                <h3>Cohort Participants: {selectedProg.title}</h3>
                <span style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                  {enrollments.length} / {selectedProg.capacity} seats filled • {selectedProg.startDate} to {selectedProg.endDate}
                </span>
              </div>
              <button className="hr-modal-close" onClick={handleCloseEnrollments}>
                &times;
              </button>
            </div>

            <div className="hr-modal-body">
              {/* Enroll Form */}
              <form
                onSubmit={handleEnrollEmployee}
                style={{
                  display: "flex",
                  gap: "10px",
                  padding: "14px",
                  background: "#f8fafc",
                  borderRadius: "8px",
                  border: "1px solid var(--hr-line)",
                  marginBottom: "20px",
                  alignItems: "flex-end",
                }}
              >
                <div style={{ flex: 1 }}>
                  <label className="form-label" style={{ marginBottom: "4px" }}>
                    Enroll Employee into Cohort
                  </label>
                  <select
                    className="form-control"
                    value={enrollUserId}
                    onChange={(e) => setEnrollUserId(e.target.value)}
                    required
                  >
                    <option value="">Select Employee</option>
                    {employees.map((emp) => (
                      <option key={emp.id} value={emp.id}>
                        {emp.fullName} ({emp.departmentName} - {emp.jobRole})
                      </option>
                    ))}
                  </select>
                </div>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={enrolling || enrollments.length >= selectedProg.capacity}
                >
                  {enrolling ? "Enrolling..." : "Enroll Participant"}
                </button>
              </form>

              {enrollLoading ? (
                <div className="hr-empty-state">
                  <Icon name="spinner" className="spin" size={24} />
                  <p>Loading cohort enrollments...</p>
                </div>
              ) : enrollments.length === 0 ? (
                <div className="hr-empty-state">
                  <p>No participants enrolled in this programme yet.</p>
                </div>
              ) : (
                <div className="hr-table-wrap">
                  <table className="hr-table">
                    <thead>
                      <tr>
                        <th>Participant</th>
                        <th>Email</th>
                        <th>Department</th>
                        <th>Status</th>
                        <th>Enrolled At</th>
                        <th>Attendance Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {enrollments.map((en) => (
                        <tr key={en.id}>
                          <td style={{ fontWeight: 600 }}>{en.employeeName}</td>
                          <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>{en.employeeEmail}</td>
                          <td>{en.departmentName}</td>
                          <td>
                            <span
                              className={`status-pill ${
                                en.status === "ATTENDED" || en.status === "COMPLETED"
                                  ? "success"
                                  : en.status === "CANCELLED"
                                  ? "danger"
                                  : "info"
                              }`}
                            >
                              {en.status}
                            </span>
                          </td>
                          <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                            {en.enrolledAt ? new Date(en.enrolledAt).toLocaleDateString() : ""}
                          </td>
                          <td>
                            <div style={{ display: "flex", gap: "6px" }}>
                              <button
                                className="btn-secondary btn-sm"
                                onClick={() => handleUpdateEnrollmentStatus(en.id, "ATTENDED")}
                              >
                                Mark Attended
                              </button>
                              <button
                                className="btn-secondary btn-sm"
                                onClick={() => handleUpdateEnrollmentStatus(en.id, "CANCELLED")}
                              >
                                Cancel
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="hr-modal-footer">
              <button className="btn-secondary" onClick={handleCloseEnrollments}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
