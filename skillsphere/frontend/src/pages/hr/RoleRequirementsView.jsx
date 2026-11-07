import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getHrRoleRequirements,
  getHrRoleMatrix,
  createHrRoleRequirement,
  updateHrRoleRequirement,
  deleteHrRoleRequirement,
  getHrCompetencies,
} from "../../services/hrService.js";

const PROFICIENCIES = ["Unaware", "Beginner", "Intermediate", "Advanced", "Expert"];
const PRIORITIES = ["HIGH", "MEDIUM", "LOW"];

export default function RoleRequirementsView() {
  const [requirements, setRequirements] = useState([]);
  const [roleMatrix, setRoleMatrix] = useState([]);
  const [skills, setSkills] = useState([]);
  const [viewMode, setViewMode] = useState("table"); // 'table' or 'matrix'
  const [selectedRole, setSelectedRole] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Modal
  const [showModal, setShowModal] = useState(false);
  const [editingReq, setEditingReq] = useState(null);
  const [formJobRole, setFormJobRole] = useState("");
  const [formSkillId, setFormSkillId] = useState("");
  const [formProficiency, setFormProficiency] = useState("Intermediate");
  const [formPriority, setFormPriority] = useState("HIGH");
  const [formIsActive, setFormIsActive] = useState(true);
  const [saving, setSaving] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [reqs, matrix, compList] = await Promise.all([
        getHrRoleRequirements(selectedRole !== "ALL" ? selectedRole : null),
        getHrRoleMatrix(),
        getHrCompetencies(),
      ]);
      setRequirements(reqs);
      setRoleMatrix(matrix);
      setSkills(compList);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load role requirements.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedRole]);

  const handleOpenAdd = () => {
    setEditingReq(null);
    setFormJobRole(selectedRole !== "ALL" ? selectedRole : "Software Engineer");
    setFormSkillId(skills[0]?.id || "");
    setFormProficiency("Intermediate");
    setFormPriority("HIGH");
    setFormIsActive(true);
    setShowModal(true);
  };

  const handleOpenEdit = (req) => {
    setEditingReq(req);
    setFormJobRole(req.jobRole);
    setFormSkillId(req.skillId);
    setFormProficiency(req.requiredProficiency);
    setFormPriority(req.priority);
    setFormIsActive(req.isActive);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingReq(null);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formJobRole.trim() || !formSkillId) {
      setError("Please fill in role and select a skill competency.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      if (editingReq) {
        await updateHrRoleRequirement(editingReq.id, {
          requiredProficiency: formProficiency,
          priority: formPriority,
          isActive: formIsActive,
        });
        setSuccess(`Updated requirement for ${editingReq.jobRole}.`);
      } else {
        await createHrRoleRequirement({
          jobRole: formJobRole.trim(),
          skillId: Number(formSkillId),
          requiredProficiency: formProficiency,
          priority: formPriority,
        });
        setSuccess(`Added competency requirement for ${formJobRole}.`);
      }
      setShowModal(false);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to save requirement.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id, jobRole, skillName) => {
    if (!window.confirm(`Are you sure you want to remove ${skillName} from ${jobRole} requirements?`)) {
      return;
    }
    try {
      setError("");
      await deleteHrRoleRequirement(id);
      setSuccess(`Removed requirement successfully.`);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to delete requirement.");
    }
  };

  const allRoles = Array.from(new Set(roleMatrix.map((m) => m.jobRole))).sort();

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Role Competency Requirements</h3>
            <p className="hr-card-subtitle">
              Benchmark standards and target proficiency profiles across all organization job roles
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <div style={{ display: "flex", border: "1px solid var(--hr-line)", borderRadius: "8px", overflow: "hidden" }}>
              <button
                className={`btn-sm ${viewMode === "table" ? "btn-primary" : "btn-secondary"}`}
                style={{ borderRadius: 0, border: "none" }}
                onClick={() => setViewMode("table")}
              >
                Table View
              </button>
              <button
                className={`btn-sm ${viewMode === "matrix" ? "btn-primary" : "btn-secondary"}`}
                style={{ borderRadius: 0, border: "none" }}
                onClick={() => setViewMode("matrix")}
              >
                Matrix View
              </button>
            </div>
            <button className="btn-primary btn-sm" onClick={handleOpenAdd}>
              <Icon name="plus" size={14} /> Add Role Requirement
            </button>
          </div>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}
          {success && <Alert type="success" message={success} />}

          {/* Role Filter */}
          <div className="hr-filter-bar">
            <label style={{ fontSize: "13px", fontWeight: 600, color: "var(--hr-soft)" }}>
              Filter by Job Role:
            </label>
            <select
              className="form-control"
              style={{ width: "auto" }}
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
            >
              <option value="ALL">All Roles ({allRoles.length})</option>
              {allRoles.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading requirements...</h4>
            </div>
          ) : viewMode === "matrix" ? (
            /* Matrix View */
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              {roleMatrix.map((matrix) => (
                <div
                  key={matrix.jobRole}
                  style={{
                    border: "1px solid var(--hr-line)",
                    borderRadius: "8px",
                    padding: "16px",
                    background: "#ffffff",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
                    <div>
                      <h4 style={{ margin: 0, fontSize: "15px", fontWeight: 700 }}>{matrix.jobRole}</h4>
                      <span style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                        {matrix.totalSkillsRequired} required skills • {matrix.highPriorityCount} high priority
                      </span>
                    </div>
                  </div>
                  <div style={{ display: "flex", flexWrap: "wrap", gap: "8px" }}>
                    {matrix.requirements.map((r) => (
                      <div
                        key={r.id}
                        style={{
                          padding: "6px 12px",
                          background: r.priority === "HIGH" ? "var(--hr-rose-tint)" : "var(--hr-blue-tint)",
                          border: `1px solid ${r.priority === "HIGH" ? "rgba(225, 29, 72, 0.2)" : "rgba(37, 99, 235, 0.2)"}`,
                          borderRadius: "6px",
                          fontSize: "12.5px",
                        }}
                      >
                        <span style={{ fontWeight: 600, color: "var(--hr-ink)" }}>{r.skillName}</span>
                        <span style={{ marginLeft: "6px", color: "var(--hr-soft)" }}>
                          ({r.requiredProficiency})
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          ) : requirements.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="target" size={32} />
              <h4>No requirements found</h4>
              <p>Define competencies required for this role.</p>
            </div>
          ) : (
            <div className="hr-table-wrap">
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>Job Role</th>
                    <th>Competency</th>
                    <th>Category</th>
                    <th>Target Proficiency</th>
                    <th>Priority</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {requirements.map((req) => (
                    <tr key={req.id}>
                      <td style={{ fontWeight: 600 }}>{req.jobRole}</td>
                      <td>{req.skillName}</td>
                      <td>
                        <span className="status-pill purple">{req.categoryName}</span>
                      </td>
                      <td>
                        <span className="status-pill info">{req.requiredProficiency}</span>
                      </td>
                      <td>
                        <span
                          className={`status-pill ${
                            req.priority === "HIGH"
                              ? "danger"
                              : req.priority === "MEDIUM"
                              ? "warning"
                              : "neutral"
                          }`}
                        >
                          {req.priority}
                        </span>
                      </td>
                      <td>
                        <span className={`status-pill ${req.isActive ? "success" : "neutral"}`}>
                          {req.isActive ? "Active" : "Inactive"}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "6px" }}>
                          <button
                            className="btn-secondary btn-sm"
                            onClick={() => handleOpenEdit(req)}
                          >
                            Edit
                          </button>
                          <button
                            className="btn-secondary btn-sm"
                            onClick={() => handleDelete(req.id, req.jobRole, req.skillName)}
                          >
                            Delete
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

      {/* Modal */}
      {showModal && (
        <div className="hr-modal-overlay" onClick={handleCloseModal}>
          <div className="hr-modal" onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleSave}>
              <div className="hr-modal-header">
                <h3>{editingReq ? "Edit Role Requirement" : "Add Role Requirement"}</h3>
                <button type="button" className="hr-modal-close" onClick={handleCloseModal}>
                  &times;
                </button>
              </div>

              <div className="hr-modal-body">
                <div className="form-group">
                  <label className="form-label">Job Role *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Full Stack Developer, Data Engineer"
                    value={formJobRole}
                    onChange={(e) => setFormJobRole(e.target.value)}
                    disabled={!!editingReq}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Skill Competency *</label>
                  <select
                    className="form-control"
                    value={formSkillId}
                    onChange={(e) => setFormSkillId(e.target.value)}
                    disabled={!!editingReq}
                    required
                  >
                    <option value="">Select Competency</option>
                    {skills.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} ({s.categoryName})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Target Proficiency Level *</label>
                  <select
                    className="form-control"
                    value={formProficiency}
                    onChange={(e) => setFormProficiency(e.target.value)}
                    required
                  >
                    {PROFICIENCIES.map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Priority Level *</label>
                  <select
                    className="form-control"
                    value={formPriority}
                    onChange={(e) => setFormPriority(e.target.value)}
                    required
                  >
                    {PRIORITIES.map((pr) => (
                      <option key={pr} value={pr}>
                        {pr}
                      </option>
                    ))}
                  </select>
                </div>

                {editingReq && (
                  <div className="form-group" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <input
                      type="checkbox"
                      id="activeCheck"
                      checked={formIsActive}
                      onChange={(e) => setFormIsActive(e.target.checked)}
                    />
                    <label htmlFor="activeCheck" style={{ margin: 0, fontSize: "13.5px" }}>
                      Active Requirement
                    </label>
                  </div>
                )}
              </div>

              <div className="hr-modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? "Saving..." : editingReq ? "Update Requirement" : "Add Requirement"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
