import React, { useState, useEffect } from "react";
import {
  getMentorArticles,
  createMentorArticle,
  updateMentorArticle,
  deleteMentorArticle,
  getSkillCategories,
} from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function KnowledgeContributionsView() {
  const [articles, setArticles] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Editor Modal
  const [editingArticle, setEditingArticle] = useState(null); // null means new, obj means edit
  const [showModal, setShowModal] = useState(false);
  const [title, setTitle] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [content, setContent] = useState("");
  const [tags, setTags] = useState("");
  const [status, setStatus] = useState("PUBLISHED");
  const [submitting, setSubmitting] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [artList, catList] = await Promise.all([
        getMentorArticles(),
        getSkillCategories().catch(() => []),
      ]);
      setArticles(artList);
      setCategories(catList);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const openNewArticleModal = () => {
    setEditingArticle(null);
    setTitle("");
    setCategoryId(categories.length > 0 ? categories[0].id : "");
    setContent("");
    setTags("");
    setStatus("PUBLISHED");
    setShowModal(true);
  };

  const openEditArticleModal = (art) => {
    setEditingArticle(art);
    setTitle(art.title);
    setCategoryId(art.categoryId || "");
    setContent(art.content);
    setTags(art.tags || "");
    setStatus(art.status);
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title || !content) return;
    setSubmitting(true);
    setError("");
    setSuccess("");
    try {
      const payload = {
        title,
        categoryId: categoryId ? Number(categoryId) : null,
        content,
        tags,
        status,
      };

      if (editingArticle) {
        await updateMentorArticle(editingArticle.id, payload);
        setSuccess("Knowledge article updated successfully!");
      } else {
        await createMentorArticle(payload);
        setSuccess("Knowledge article published to the enterprise base!");
      }

      setShowModal(false);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (artId) => {
    if (!window.confirm("Are you sure you want to delete this article?")) return;
    try {
      await deleteMentorArticle(artId);
      setSuccess("Article removed.");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Knowledge Contributions & Architectural Guides</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Author organizational articles, architectural guidelines, and best practices.
            </p>
          </div>

          <button
            type="button"
            className="mt-btn mt-btn--primary"
            onClick={openNewArticleModal}
          >
            + Author New Article
          </button>
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}
        {success && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{success}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading your knowledge contributions...</p>
        ) : articles.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
            <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No knowledge contributions authored yet.</p>
            <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>Click "+ Author New Article" to share technical expertise across the organization.</p>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            {articles.map((art) => (
              <div
                key={art.id}
                style={{
                  padding: "20px",
                  borderRadius: "10px",
                  border: "1px solid var(--mt-line)",
                  background: "#ffffff",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "flex-start",
                  flexWrap: "wrap",
                  gap: "16px",
                }}
              >
                <div style={{ flex: 1 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px" }}>
                    <strong style={{ fontSize: "1.1rem" }}>{art.title}</strong>
                    <span className={`mt-badge ${art.status === "PUBLISHED" ? "mt-badge--active" : "mt-badge--pending"}`}>
                      {art.status}
                    </span>
                    <span className="mt-badge mt-badge--neutral">{art.categoryName}</span>
                  </div>

                  <small style={{ color: "var(--mt-faint)", display: "block", marginBottom: "10px" }}>
                    Last updated on {new Date(art.updatedAt || art.createdAt).toLocaleDateString()}
                  </small>

                  <p style={{ margin: "0 0 10px", fontSize: "0.88rem", color: "var(--mt-soft)", whiteSpace: "pre-line", maxHeight: "120px", overflow: "hidden", textOverflow: "ellipsis" }}>
                    {art.content}
                  </p>

                  {art.tags && (
                    <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
                      {art.tags.split(",").map((t, idx) => (
                        <span key={idx} className="mt-badge mt-badge--neutral" style={{ fontSize: "0.72rem" }}>
                          #{t.trim()}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <div style={{ display: "flex", gap: "8px", alignSelf: "flex-start" }}>
                  <button
                    type="button"
                    className="mt-btn mt-btn--outline"
                    style={{ fontSize: "0.8rem" }}
                    onClick={() => openEditArticleModal(art)}
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    className="mt-btn mt-btn--danger"
                    style={{ fontSize: "0.8rem" }}
                    onClick={() => handleDelete(art.id)}
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Editor Modal */}
      {showModal && (
        <div className="mt-modal-backdrop" onClick={() => setShowModal(false)}>
          <div className="mt-modal mt-modal--wide" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>
                {editingArticle ? "Edit Knowledge Article" : "Author Knowledge Article"}
              </h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setShowModal(false)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <div className="mt-modal-body">
                <div className="mt-form-group">
                  <label className="mt-label">Article Title *</label>
                  <input
                    type="text"
                    required
                    className="mt-input"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="e.g. Designing Resilient Microservices with Resilience4j"
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                  <div className="mt-form-group">
                    <label className="mt-label">Category</label>
                    <select
                      className="mt-select"
                      value={categoryId}
                      onChange={(e) => setCategoryId(e.target.value)}
                    >
                      <option value="">General</option>
                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mt-form-group">
                    <label className="mt-label">Status</label>
                    <select
                      className="mt-select"
                      value={status}
                      onChange={(e) => setStatus(e.target.value)}
                    >
                      <option value="PUBLISHED">PUBLISHED (Visible to Organization)</option>
                      <option value="DRAFT">DRAFT (Personal Workspace Only)</option>
                    </select>
                  </div>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Tags (comma-separated)</label>
                  <input
                    type="text"
                    className="mt-input"
                    value={tags}
                    onChange={(e) => setTags(e.target.value)}
                    placeholder="e.g. java, spring-boot, circuit-breaker, architecture"
                  />
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Article Body & Code Examples *</label>
                  <textarea
                    rows={8}
                    required
                    className="mt-textarea"
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="Write detailed technical documentation, design patterns, and code snippets..."
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
                  {submitting ? "Saving..." : editingArticle ? "Update Article" : "Publish Article"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
