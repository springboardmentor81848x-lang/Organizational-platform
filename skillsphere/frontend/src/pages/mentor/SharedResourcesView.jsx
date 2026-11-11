import React, { useState, useEffect } from "react";
import {
  getMyMentees,
  getMentorResources,
  createMentorResource,
  deleteMentorResource,
  getAllSkills,
} from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

const RESOURCE_TYPES = ["ARTICLE", "VIDEO", "CODE_REPO", "DOCUMENT", "COURSE"];

export default function SharedResourcesView() {
  const [mentees, setMentees] = useState([]);
  const [selectedMenteeFilter, setSelectedMenteeFilter] = useState("");
  const [resources, setResources] = useState([]);
  const [skills, setSkills] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Share Modal
  const [showModal, setShowModal] = useState(false);
  const [assignmentId, setAssignmentId] = useState("");
  const [title, setTitle] = useState("");
  const [urlOrContent, setUrlOrContent] = useState("");
  const [resourceType, setResourceType] = useState("ARTICLE");
  const [description, setDescription] = useState("");
  const [skillId, setSkillId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [mList, rList, sList] = await Promise.all([
        getMyMentees("ACTIVE"),
        getMentorResources({
          menteeId: selectedMenteeFilter ? Number(selectedMenteeFilter) : undefined,
        }),
        getAllSkills().catch(() => []),
      ]);
      setMentees(mList);
      setResources(rList);
      setSkills(sList);
      if (mList.length > 0 && !assignmentId) {
        setAssignmentId(mList[0].assignmentId);
      }
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedMenteeFilter]);

  const handleShare = async (e) => {
    e.preventDefault();
    if (!assignmentId || !title || !urlOrContent) return;
    setSubmitting(true);
    setError("");
    setSuccess("");
    try {
      await createMentorResource({
        assignmentId: Number(assignmentId),
        title,
        urlOrContent,
        resourceType,
        description,
        skillId: skillId ? Number(skillId) : null,
      });
      setSuccess("Learning resource shared with mentee!");
      setShowModal(false);
      setTitle("");
      setUrlOrContent("");
      setDescription("");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (resId) => {
    if (!window.confirm("Are you sure you want to delete this shared resource?")) return;
    try {
      await deleteMentorResource(resId);
      setSuccess("Resource removed.");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const filteredResources = resources.filter((r) => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      r.title.toLowerCase().includes(q) ||
      (r.description && r.description.toLowerCase().includes(q)) ||
      r.employeeName.toLowerCase().includes(q)
    );
  });

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Shared Learning Resources</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Curate documentation, repositories, articles, and training materials for your mentees.
            </p>
          </div>

          <button
            type="button"
            className="mt-btn mt-btn--primary"
            onClick={() => setShowModal(true)}
          >
            + Share Resource
          </button>
        </div>

        {/* Filter Bar */}
        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", alignItems: "center", marginBottom: "16px" }}>
          <select
            className="mt-select"
            style={{ width: "240px" }}
            value={selectedMenteeFilter}
            onChange={(e) => setSelectedMenteeFilter(e.target.value)}
          >
            <option value="">All Mentees</option>
            {mentees.map((m) => (
              <option key={m.employeeId} value={m.employeeId}>
                {m.employeeName}
              </option>
            ))}
          </select>

          <input
            type="text"
            className="mt-input"
            style={{ width: "280px" }}
            placeholder="Search resources by title or keyword..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}
        {success && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{success}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading shared resources...</p>
        ) : filteredResources.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
            <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No shared resources found.</p>
            <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>Click "+ Share Resource" to send curated links and documentation to your mentees.</p>
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(360px, 1fr))", gap: "16px" }}>
            {filteredResources.map((res) => (
              <div
                key={res.id}
                style={{
                  background: "#ffffff",
                  border: "1px solid var(--mt-line)",
                  borderRadius: "10px",
                  padding: "18px",
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "space-between",
                  gap: "12px",
                }}
              >
                <div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "8px" }}>
                    <strong style={{ fontSize: "1rem" }}>{res.title}</strong>
                    <span className="mt-badge mt-badge--neutral">{res.resourceType}</span>
                  </div>

                  <small style={{ color: "var(--mt-faint)", display: "block", marginTop: "2px" }}>
                    Shared with {res.employeeName} &bull; {new Date(res.createdAt).toLocaleDateString()}
                    {res.skillName && ` • Skill: ${res.skillName}`}
                  </small>

                  {res.description && (
                    <p style={{ margin: "8px 0 0", fontSize: "0.86rem", color: "var(--mt-soft)" }}>
                      {res.description}
                    </p>
                  )}
                </div>

                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderTop: "1px solid var(--mt-line)", paddingTop: "10px" }}>
                  <a
                    href={res.urlOrContent}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="mt-btn mt-btn--primary"
                    style={{ fontSize: "0.78rem", padding: "5px 12px" }}
                  >
                    Open Resource &rarr;
                  </a>

                  <button
                    type="button"
                    className="mt-btn mt-btn--danger"
                    style={{ fontSize: "0.78rem", padding: "5px 10px" }}
                    onClick={() => handleDelete(res.id)}
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Share Resource Modal */}
      {showModal && (
        <div className="mt-modal-backdrop" onClick={() => setShowModal(false)}>
          <div className="mt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>Share Learning Resource</h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setShowModal(false)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleShare}>
              <div className="mt-modal-body">
                <div className="mt-form-group">
                  <label className="mt-label">Mentee *</label>
                  <select
                    className="mt-select"
                    required
                    value={assignmentId}
                    onChange={(e) => setAssignmentId(e.target.value)}
                  >
                    {mentees.map((m) => (
                      <option key={m.assignmentId} value={m.assignmentId}>
                        {m.employeeName} ({m.jobRole})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Resource Title *</label>
                  <input
                    type="text"
                    required
                    className="mt-input"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="e.g. Real-World Kafka Architecture Patterns"
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                  <div className="mt-form-group">
                    <label className="mt-label">Resource Type</label>
                    <select
                      className="mt-select"
                      value={resourceType}
                      onChange={(e) => setResourceType(e.target.value)}
                    >
                      {RESOURCE_TYPES.map((t) => (
                        <option key={t} value={t}>
                          {t}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mt-form-group">
                    <label className="mt-label">Linked Skill (Optional)</label>
                    <select
                      className="mt-select"
                      value={skillId}
                      onChange={(e) => setSkillId(e.target.value)}
                    >
                      <option value="">None</option>
                      {skills.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Resource URL or Content Link *</label>
                  <input
                    type="text"
                    required
                    className="mt-input"
                    value={urlOrContent}
                    onChange={(e) => setUrlOrContent(e.target.value)}
                    placeholder="https://github.com/... or https://martinfowler.com/..."
                  />
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Summary / Why read this?</label>
                  <textarea
                    rows={3}
                    className="mt-textarea"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Explain why this resource will help address their current skill gap..."
                  />
                </div>
              </div>

              <div className="mt-modal-footer">
                <button
                  type="button"
                  className="mt-btn mt-btn--outline"
                  onClick={() => setShowModal(false)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="mt-btn mt-btn--primary"
                  disabled={submitting}
                >
                  {submitting ? "Sharing..." : "Share with Mentee"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
