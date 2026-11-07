import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getHrLearningPaths,
  createHrLearningPath,
  updateHrLearningPath,
  toggleHrLearningPathActive,
  getHrCourses,
  getHrCompetencies,
} from "../../services/hrService.js";

const LEVELS = ["Beginner", "Intermediate", "Advanced"];

export default function LearningPathsView() {
  const [paths, setPaths] = useState([]);
  const [courses, setCourses] = useState([]);
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Modal
  const [showModal, setShowModal] = useState(false);
  const [editingPath, setEditingPath] = useState(null);
  const [formTitle, setFormTitle] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formRole, setFormRole] = useState("Software Developer");
  const [formSkillId, setFormSkillId] = useState("");
  const [formLevel, setFormLevel] = useState("Beginner");
  const [formCourseIds, setFormCourseIds] = useState([]);
  const [formIsActive, setFormIsActive] = useState(true);
  const [saving, setSaving] = useState(false);

  // Expanded paths
  const [expandedPathId, setExpandedPathId] = useState(null);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [pList, cList, sList] = await Promise.all([
        getHrLearningPaths(),
        getHrCourses(),
        getHrCompetencies(),
      ]);
      setPaths(pList);
      setCourses(cList);
      setSkills(sList);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load learning paths.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleOpenAdd = () => {
    setEditingPath(null);
    setFormTitle("");
    setFormDesc("");
    setFormRole("Software Developer");
    setFormSkillId(skills[0]?.id || "");
    setFormLevel("Beginner");
    setFormCourseIds([]);
    setFormIsActive(true);
    setShowModal(true);
  };

  const handleOpenEdit = (path) => {
    setEditingPath(path);
    setFormTitle(path.title);
    setFormDesc(path.description || "");
    setFormRole(path.targetRole || "Software Developer");
    setFormSkillId(path.skillId || "");
    setFormLevel(path.difficultyLevel || "Beginner");
    setFormCourseIds(path.items ? path.items.map((i) => i.courseId) : []);
    setFormIsActive(path.isActive);
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingPath(null);
  };

  const handleToggleCourseSelect = (courseId) => {
    setFormCourseIds((prev) =>
      prev.includes(courseId) ? prev.filter((id) => id !== courseId) : [...prev, courseId]
    );
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!formTitle.trim()) {
      setError("Please provide a learning path title.");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      const payload = {
        title: formTitle.trim(),
        description: formDesc.trim(),
        targetRole: formRole.trim(),
        skillId: formSkillId ? Number(formSkillId) : null,
        difficultyLevel: formLevel,
        isActive: formIsActive,
        courseIds: formCourseIds,
      };

      if (editingPath) {
        await updateHrLearningPath(editingPath.id, payload);
        setSuccess(`Updated learning path "${formTitle}".`);
      } else {
        await createHrLearningPath(payload);
        setSuccess(`Created learning path "${formTitle}".`);
      }
      setShowModal(false);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to save learning path.");
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (id) => {
    try {
      setError("");
      await toggleHrLearningPathActive(id);
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
            <h3 className="hr-card-title">Structured Learning Paths</h3>
            <p className="hr-card-subtitle">
              Sequenced course roadmaps aligned to job roles and skill progression milestones
            </p>
          </div>
          <div style={{ display: "flex", gap: "10px" }}>
            <button className="btn-secondary btn-sm" onClick={loadData}>
              <Icon name="refresh" size={14} /> Refresh
            </button>
            <button className="btn-primary btn-sm" onClick={handleOpenAdd}>
              <Icon name="plus" size={14} /> Create Learning Path
            </button>
          </div>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}
          {success && <Alert type="success" message={success} />}

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading learning paths...</h4>
            </div>
          ) : paths.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="map" size={32} />
              <h4>No learning paths configured</h4>
              <p>Design a structured curriculum to guide workforce upskilling.</p>
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              {paths.map((p) => (
                <div
                  key={p.id}
                  style={{
                    border: "1px solid var(--hr-line)",
                    borderRadius: "8px",
                    background: "#ffffff",
                    overflow: "hidden",
                  }}
                >
                  <div
                    style={{
                      padding: "16px 20px",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      flexWrap: "wrap",
                      gap: "12px",
                      background: "#f8fafc",
                      borderBottom: expandedPathId === p.id ? "1px solid var(--hr-line)" : "none",
                    }}
                  >
                    <div>
                      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}>
                        <h4 style={{ margin: 0, fontSize: "16px", fontWeight: 700, color: "var(--hr-ink)" }}>
                          {p.title}
                        </h4>
                        <span className={`status-pill ${p.isActive ? "success" : "neutral"}`}>
                          {p.isActive ? "Active" : "Archived"}
                        </span>
                        <span className="status-pill info">{p.difficultyLevel}</span>
                      </div>
                      <div style={{ fontSize: "13px", color: "var(--hr-soft)" }}>
                        Target Role: <strong>{p.targetRole}</strong> • Competency:{" "}
                        <span className="status-pill purple">{p.skillName || "Cross-Functional"}</span> • {p.courseCount} courses (
                        {p.estimatedHours} hrs total)
                      </div>
                    </div>

                    <div style={{ display: "flex", gap: "8px" }}>
                      <button
                        className="btn-secondary btn-sm"
                        onClick={() => setExpandedPathId(expandedPathId === p.id ? null : p.id)}
                      >
                        {expandedPathId === p.id ? "Hide Courses" : "View Curriculum"}
                      </button>
                      <button className="btn-secondary btn-sm" onClick={() => handleOpenEdit(p)}>
                        Edit
                      </button>
                      <button className="btn-secondary btn-sm" onClick={() => handleToggle(p.id)}>
                        {p.isActive ? "Archive" : "Activate"}
                      </button>
                    </div>
                  </div>

                  {expandedPathId === p.id && (
                    <div style={{ padding: "16px 20px" }}>
                      {p.description && (
                        <p style={{ fontSize: "13px", color: "var(--hr-soft)", margin: "0 0 14px" }}>
                          {p.description}
                        </p>
                      )}

                      <h5 style={{ margin: "0 0 10px", fontSize: "13.5px", fontWeight: 700 }}>
                        Curriculum Sequence
                      </h5>

                      {(!p.items || p.items.length === 0) ? (
                        <p style={{ fontSize: "12.5px", color: "var(--hr-faint)" }}>
                          No course modules assigned to this path.
                        </p>
                      ) : (
                        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                          {p.items.map((item, idx) => (
                            <div
                              key={item.id}
                              style={{
                                display: "flex",
                                alignItems: "center",
                                gap: "12px",
                                padding: "10px 14px",
                                background: "#f8fafc",
                                border: "1px solid var(--hr-line)",
                                borderRadius: "6px",
                              }}
                            >
                              <div
                                style={{
                                  width: 26,
                                  height: 26,
                                  borderRadius: "50%",
                                  background: "var(--hr-primary-tint)",
                                  color: "var(--hr-primary-dark)",
                                  display: "flex",
                                  alignItems: "center",
                                  justifyContent: "center",
                                  fontWeight: 700,
                                  fontSize: "12px",
                                }}
                              >
                                {idx + 1}
                              </div>
                              <div style={{ flex: 1 }}>
                                <div style={{ fontWeight: 600, fontSize: "13.5px" }}>
                                  {item.courseTitle}
                                </div>
                                <div style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                                  {item.courseProvider} • {item.courseLevel} • {item.durationHours} hrs
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="hr-modal-overlay" onClick={handleCloseModal}>
          <div className="hr-modal" style={{ maxWidth: 740 }} onClick={(e) => e.stopPropagation()}>
            <form onSubmit={handleSave}>
              <div className="hr-modal-header">
                <h3>{editingPath ? "Edit Learning Path" : "Create Learning Path"}</h3>
                <button type="button" className="hr-modal-close" onClick={handleCloseModal}>
                  &times;
                </button>
              </div>

              <div className="hr-modal-body">
                <div className="form-group">
                  <label className="form-label">Path Title *</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Senior Backend Engineer Track"
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                  <div className="form-group">
                    <label className="form-label">Target Role *</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Full Stack Developer"
                      value={formRole}
                      onChange={(e) => setFormRole(e.target.value)}
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
                      <option value="">Cross-Functional / Broad</option>
                      {skills.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.name} ({s.categoryName})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label className="form-label">Difficulty Level</label>
                  <select
                    className="form-control"
                    value={formLevel}
                    onChange={(e) => setFormLevel(e.target.value)}
                  >
                    {LEVELS.map((l) => (
                      <option key={l} value={l}>
                        {l}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label className="form-label">Description & Career Progression Goals</label>
                  <textarea
                    className="form-control"
                    placeholder="Outline the journey and prerequisites..."
                    value={formDesc}
                    onChange={(e) => setFormDesc(e.target.value)}
                  />
                </div>

                {/* Course Selector */}
                <div className="form-group">
                  <label className="form-label">
                    Select & Sequence Courses ({formCourseIds.length} selected)
                  </label>
                  <div
                    style={{
                      maxHeight: "180px",
                      overflowY: "auto",
                      border: "1px solid var(--hr-line)",
                      borderRadius: "8px",
                      padding: "8px",
                      display: "flex",
                      flexDirection: "column",
                      gap: "6px",
                    }}
                  >
                    {courses.map((c) => {
                      const isSelected = formCourseIds.includes(c.id);
                      return (
                        <div
                          key={c.id}
                          onClick={() => handleToggleCourseSelect(c.id)}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "10px",
                            padding: "6px 10px",
                            borderRadius: "6px",
                            background: isSelected ? "var(--hr-primary-tint)" : "#f8fafc",
                            cursor: "pointer",
                            border: `1px solid ${isSelected ? "var(--hr-primary)" : "var(--hr-line)"}`,
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => {}} // handled by row click
                          />
                          <span style={{ fontSize: "13px", fontWeight: isSelected ? 600 : 500 }}>
                            {c.title}
                          </span>
                          <span style={{ marginLeft: "auto", fontSize: "11.5px", color: "var(--hr-soft)" }}>
                            {c.level} • {c.durationHours} hrs
                          </span>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {editingPath && (
                  <div className="form-group" style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                    <input
                      type="checkbox"
                      id="pathActiveCheck"
                      checked={formIsActive}
                      onChange={(e) => setFormIsActive(e.target.checked)}
                    />
                    <label htmlFor="pathActiveCheck" style={{ margin: 0, fontSize: "13.5px" }}>
                      Active Learning Path
                    </label>
                  </div>
                )}
              </div>

              <div className="hr-modal-footer">
                <button type="button" className="btn-secondary" onClick={handleCloseModal}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={saving}>
                  {saving ? "Saving..." : editingPath ? "Update Learning Path" : "Create Learning Path"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
