import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getHrCompetencies,
  createHrCompetency,
  updateHrCompetency,
  toggleHrCompetencyActive,
} from "../../services/hrService.js";
import { getSkillCategories } from "../../services/platformService.js";

export default function CompetencyFrameworkView() {
  const [competencies, setCompetencies] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Modal states
  const [showModal, setShowModal] = useState(false);
  const [editingCompetency, setEditingCompetency] = useState(null);
  const [formName, setFormName] = useState("");
  const [formCategoryId, setFormCategoryId] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formIsActive, setFormIsActive] = useState(true);
  const [saving, setSaving] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [comps, cats] = await Promise.all([
        getHrCompetencies(),
        getSkillCategories().catch(() => []),
      ]);
      setCompetencies(comps);
      setCategories(cats);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load competencies.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenAdd = () => {
    setEditingCompetency(null);
    setFormName("");
    setFormCategoryId(categories[0]?.id || "");
    setFormDesc("");
    setFormIsActive(true);
    setShowModal(true);
  };

  const handleOpenEdit = (comp) => {
    setEditingCompetency(comp);
    setFormName(comp.name);
    setFormCategoryId(comp.categoryId || categories[0]?.id || "");
    setFormDesc(comp.description || "");
    setFormIsActive(comp.isActive);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingCompetency(null);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formName.trim() || !formCategoryId) {
      setError("Please fill in competency name and category.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      if (editingCompetency) {
        await updateHrCompetency(editingCompetency.id, {
          name: formName.trim(),
          categoryId: Number(formCategoryId),
          description: formDesc.trim(),
          isActive: formIsActive,
        });
        setSuccess(`Updated competency "${formName}".`);
      } else {
        await createHrCompetency({
          name: formName.trim(),
          categoryId: Number(formCategoryId),
          description: formDesc.trim(),
        });
        setSuccess(`Created new competency "${formName}".`);
      }
      setShowModal(false);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to save competency.");
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (id) => {
    try {
      setError("");
      await toggleHrCompetencyActive(id);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to toggle status.");
    }
  };

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Competency Framework</h3>
            <p className="hr-card-subtitle">
              Standardized skill dictionary, definitions, and curriculum linkages
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <button className="btn-secondary btn-sm" onClick={loadData}>
              <Icon name="refresh" size={14} /> Refresh
            </button>
            <button className="btn-primary btn-sm" onClick={handleOpenAdd}>
              <Icon name="plus" size={14} /> Add Competency
            </button>
          </div>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}
          {success && <Alert type="success" message={success} />}

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading competency framework...</h4>
            </div>
          ) : competencies.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="clipboard" size={32} />
              <h4>No competencies configured</h4>
              <p>Add your organization's first skill competency to begin framework mapping.</p>
            </div>
          ) : (
            <div className="hr-table-wrap">
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>Competency</th>
                    <th>Category</th>
                    <th>Description</th>
                    <th>Linked Roles</th>
                    <th>Linked Courses</th>
                    <th>Learning Paths</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {competencies.map((comp) => (
                    <tr key={comp.id}>
                      <td style={{ fontWeight: 600 }}>{comp.name}</td>
                      <td>
                        <span className="status-pill purple">{comp.categoryName}</span>
                      </td>
                      <td style={{ maxWidth: 260, fontSize: "12.5px", color: "var(--hr-soft)" }}>
                        {comp.description || "—"}
                      </td>
                      <td>{comp.associatedRolesCount} roles</td>
                      <td>{comp.associatedCoursesCount} courses</td>
                      <td>{comp.associatedPathsCount} paths</td>
                      <td>
                        <span className={`status-pill ${comp.isActive ? "success" : "neutral"}`}>
                          {comp.isActive ? "Active" : "Archived"}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "6px" }}>
                          <button
                            className="btn-secondary btn-sm"
                            onClick={() => handleOpenEdit(comp)}
                          >
                            Edit
                          </button>
                          <button
                            className="btn-secondary btn-sm"
                            onClick={() => handleToggle(comp.id)}
                          >
                            {comp.isActive ? "Archive" : "Activate"}
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

      {/* Add / Edit Modal */}
      {showModal && (
        <div className="hr-modal-overlay" onClick={handleCloseModal}>
          <div className="hr-modal" onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleSave}>
              <div className="hr-modal-header">
                <h3>{editingCompetency ? "Edit Competency" : "Add Skill Competency"}</h3>
                <button type="button" className="hr-modal-close" onClick={handleCloseModal}>
                  &times;
                </button>
              </div>

              <div className="hr-modal-body">
                <div className="form-group">
                  <label className="form-label">Competency Name *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Cloud Security Architecture"
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Category *</label>
                  <select
                    className="form-control"
                    value={formCategoryId}
                    onChange={(e) => setFormCategoryId(e.target.value)}
                    required
                  >
                    <option value="">Select Category</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Description & Proficiency Indicators</label>
                  <textarea
                    className="form-control"
                    placeholder="Define behavioral and technical standards for this competency..."
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                  />
                </div>

                {editingCompetency && (
                  <div className="form-group" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <input
                      type="checkbox"
                      id="activeCheck"
                      checked={formIsActive}
                      onChange={(e) => setFormIsActive(e.target.checked)}
                    />
                    <label htmlFor="activeCheck" style={{ margin: 0, fontSize: "13.5px" }}>
                      Active Competency (Available in assessments and requirements)
                    </label>
                  </div>
                )}
              </div>

              <div className="hr-modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? "Saving..." : editingCompetency ? "Update Competency" : "Create Competency"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
