import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getHrCourses,
  createHrCourse,
  updateHrCourse,
  toggleHrCourseActive,
  getHrCompetencies,
} from "../../services/hrService.js";

const LEVELS = ["Beginner", "Intermediate", "Advanced"];

export default function LearningCatalogueView() {
  const [courses, setCourses] = useState([]);
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [search, setSearch] = useState("");

  // Modal
  const [showModal, setShowModal] = useState(false);
  const [editingCourse, setEditingCourse] = useState(null);
  const [formTitle, setFormTitle] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formSkillId, setFormSkillId] = useState("");
  const [formLevel, setFormLevel] = useState("Beginner");
  const [formDuration, setFormDuration] = useState(10);
  const [formProvider, setFormProvider] = useState("SkillSphere Academy");
  const [formUrl, setFormUrl] = useState("");
  const [formThumb, setFormThumb] = useState("");
  const [formIsActive, setFormIsActive] = useState(true);
  const [saving, setSaving] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [cList, sList] = await Promise.all([
        getHrCourses(),
        getHrCompetencies(),
      ]);
      setCourses(cList);
      setSkills(sList);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load learning catalogue.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenAdd = () => {
    setEditingCourse(null);
    setFormTitle("");
    setFormDesc("");
    setFormSkillId(skills[0]?.id || "");
    setFormLevel("Beginner");
    setFormDuration(10);
    setFormProvider("SkillSphere Academy");
    setFormUrl("");
    setFormThumb("");
    setFormIsActive(true);
    setShowModal(true);
  };

  const handleOpenEdit = (c) => {
    setEditingCourse(c);
    setFormTitle(c.title);
    setFormDesc(c.description || "");
    setFormSkillId(c.skillId || "");
    setFormLevel(c.level || "Beginner");
    setFormDuration(c.durationHours || 10);
    setFormProvider(c.provider || "SkillSphere Academy");
    setFormUrl(c.courseUrl || "");
    setFormThumb(c.thumbnailUrl || "");
    setFormIsActive(c.isActive);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingCourse(null);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formTitle.trim() || !formProvider.trim()) {
      setError("Please fill in course title and provider.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      const payload = {
        title: formTitle.trim(),
        description: formDesc.trim(),
        skillId: formSkillId ? Number(formSkillId) : null,
        level: formLevel,
        durationHours: Number(formDuration),
        provider: formProvider.trim(),
        courseUrl: formUrl.trim(),
        thumbnailUrl: formThumb.trim(),
        isActive: formIsActive,
      };

      if (editingCourse) {
        await updateHrCourse(editingCourse.id, payload);
        setSuccess(`Updated course "${formTitle}".`);
      } else {
        await createHrCourse(payload);
        setSuccess(`Added course "${formTitle}" to catalogue.`);
      }
      setShowModal(false);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to save course.");
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (id) => {
    try {
      setError("");
      await toggleHrCourseActive(id);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to toggle status.");
    }
  };

  const filteredCourses = courses.filter((c) => {
    if (!search.trim()) return true;
    const s = search.toLowerCase();
    return (
      c.title.toLowerCase().includes(s) ||
      (c.provider && c.provider.toLowerCase().includes(s)) ||
      (c.skillName && c.skillName.toLowerCase().includes(s))
    );
  });

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Learning Catalogue Management</h3>
            <p className="hr-card-subtitle">
              Curated course library, external provider integrations, and training content inventory
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <button className="btn-secondary btn-sm" onClick={loadData}>
              <Icon name="refresh" size={14} /> Refresh
            </button>
            <button className="btn-primary btn-sm" onClick={handleOpenAdd}>
              <Icon name="plus" size={14} /> Add New Course
            </button>
          </div>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}
          {success && <Alert type="success" message={success} />}

          {/* Search bar */}
          <div className="hr-filter-bar">
            <input
              type="text"
              className="form-control hr-search-input"
              placeholder="Search courses by title, provider, or competency..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading courses...</h4>
            </div>
          ) : filteredCourses.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="book" size={32} />
              <h4>No courses found</h4>
              <p>Add educational resources to enrich your workforce learning catalogue.</p>
            </div>
          ) : (
            <div className="hr-table-wrap">
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>Course Title</th>
                    <th>Provider</th>
                    <th>Competency</th>
                    <th>Level</th>
                    <th>Duration</th>
                    <th>Enrolled</th>
                    <th>Completed</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredCourses.map((c) => (
                    <tr key={c.id}>
                      <td>
                        <div style={{ fontWeight: 600, color: "var(--hr-ink)" }}>{c.title}</div>
                        {c.courseUrl && (
                          <a
                            href={c.courseUrl}
                            target="_blank"
                            rel="noreferrer"
                            style={{ fontSize: "11.5px", color: "var(--hr-blue)", textDecoration: "none" }}
                          >
                            Course Link ↗
                          </a>
                        )}
                      </td>
                      <td>{c.provider}</td>
                      <td>
                        <span className="status-pill purple">{c.skillName || "General"}</span>
                      </td>
                      <td>
                        <span className="status-pill info">{c.level}</span>
                      </td>
                      <td>{c.durationHours} hrs</td>
                      <td>{c.enrolledCount} learners</td>
                      <td>{c.completedCount} passed</td>
                      <td>
                        <span className={`status-pill ${c.isActive ? "success" : "neutral"}`}>
                          {c.isActive ? "Active" : "Archived"}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: "flex", gap: "6px" }}>
                          <button
                            className="btn-secondary btn-sm"
                            onClick={() => handleOpenEdit(c)}
                          >
                            Edit
                          </button>
                          <button
                            className="btn-secondary btn-sm"
                            onClick={() => handleToggle(c.id)}
                          >
                            {c.isActive ? "Archive" : "Activate"}
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
                <h3>{editingCourse ? "Edit Course" : "Add Course to Catalogue"}</h3>
                <button type="button" className="hr-modal-close" onClick={handleCloseModal}>
                  &times;
                </button>
              </div>

              <div className="hr-modal-body">
                <div className="form-group">
                  <label className="form-label">Course Title *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Production Spring Boot & Microservices"
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Provider *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. SkillSphere Academy, Coursera, Udemy"
                    value={formProvider}
                    onChange={(e) => setFormProvider(e.target.value)}
                    required
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Linked Competency</label>
                  <select
                    className="form-control"
                    value={formSkillId}
                    onChange={(e) => setFormSkillId(e.target.value)}
                  >
                    <option value="">General (No specific competency)</option>
                    {skills.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name} ({s.categoryName})
                      </option>
                    ))}
                  </select>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                  <div className="form-group">
                    <label className="form-label">Difficulty Level</label>
                    <select
                      className="form-control"
                      value={formLevel}
                      onChange={(e) => setFormLevel(e.target.value)}
                    >
                      {LEVELS.map((lvl) => (
                        <option key={lvl} value={lvl}>
                          {lvl}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="form-group">
                    <label className="form-label">Duration (Hours) *</label>
                    <input
                      type="number"
                      min={1}
                      className="form-control"
                      value={formDuration}
                      onChange={(e) => setFormDuration(e.target.value)}
                      required
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Course URL</label>
                  <input
                    type="url"
                    className="form-control"
                    placeholder="https://..."
                    value={formUrl}
                    onChange={(e) => setFormUrl(e.target.value)}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Description & Syllabus</label>
                  <textarea
                    className="form-control"
                    placeholder="Overview of modules, prerequisites, and learning objectives..."
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                  />
                </div>

                {editingCourse && (
                  <div className="form-group" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <input
                      type="checkbox"
                      id="courseActiveCheck"
                      checked={formIsActive}
                      onChange={(e) => setFormIsActive(e.target.checked)}
                    />
                    <label htmlFor="courseActiveCheck" style={{ margin: 0, fontSize: "13.5px" }}>
                      Active Course in Catalogue
                    </label>
                  </div>
                )}
              </div>

              <div className="hr-modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? "Saving..." : editingCourse ? "Update Course" : "Add Course"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
