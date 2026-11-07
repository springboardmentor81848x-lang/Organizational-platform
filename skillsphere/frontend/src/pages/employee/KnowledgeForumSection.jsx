import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getKnowledgeCategories,
  getKnowledgeArticles,
  createKnowledgeArticle,
  getForumThreads,
  getForumThreadById,
  createForumThread,
  postForumReply,
  acceptForumAnswer,
} from "../../services/platformService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function KnowledgeForumSection({ currentUserId }) {
  const [subTab, setSubTab] = useState("articles"); // "articles" | "forum"
  const [categories, setCategories] = useState([]);
  const [articles, setArticles] = useState([]);
  const [threads, setThreads] = useState([]);
  const [activeThread, setActiveThread] = useState(null);
  const [search, setSearch] = useState("");

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Create article modal
  const [showArticleModal, setShowArticleModal] = useState(false);
  const [artTitle, setArtTitle] = useState("");
  const [artContent, setArtContent] = useState("");
  const [artTags, setArtTags] = useState("");
  const [artCatId, setArtCatId] = useState("");

  // Create thread modal
  const [showThreadModal, setShowThreadModal] = useState(false);
  const [thrTitle, setThrTitle] = useState("");
  const [thrContent, setThrContent] = useState("");
  const [thrCat, setThrCat] = useState("General");

  // Post reply
  const [replyContent, setReplyContent] = useState("");

  const loadData = async () => {
    try {
      const cats = await getKnowledgeCategories().catch(() => []);
      setCategories(cats);
      if (cats.length > 0 && !artCatId) setArtCatId(cats[0].id);

      const arts = await getKnowledgeArticles(search).catch(() => []);
      setArticles(arts);

      const thrs = await getForumThreads(search).catch(() => []);
      setThreads(thrs);
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    loadData();
  }, [search]);

  const handleCreateArticle = async (e) => {
    e.preventDefault();
    if (!artTitle || !artContent) return;
    setError(""); setSuccess("");
    try {
      await createKnowledgeArticle({
        categoryId: artCatId ? Number(artCatId) : null,
        title: artTitle,
        content: artContent,
        tags: artTags,
        status: "PUBLISHED",
      });
      setSuccess("Article published to Knowledge Hub!");
      setShowArticleModal(false);
      setArtTitle(""); setArtContent(""); setArtTags("");
      loadData();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handleCreateThread = async (e) => {
    e.preventDefault();
    if (!thrTitle || !thrContent) return;
    setError(""); setSuccess("");
    try {
      await createForumThread({
        title: thrTitle,
        content: thrContent,
        category: thrCat,
      });
      setSuccess("Discussion topic posted!");
      setShowThreadModal(false);
      setThrTitle(""); setThrContent("");
      loadData();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const openThread = async (tId) => {
    try {
      const full = await getForumThreadById(tId);
      setActiveThread(full);
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handlePostReply = async (e) => {
    e.preventDefault();
    if (!activeThread || !replyContent) return;
    try {
      const rep = await postForumReply(activeThread.id, { content: replyContent });
      setActiveThread((prev) => ({
        ...prev,
        replies: [...(prev.replies || []), rep],
      }));
      setReplyContent("");
      setSuccess("Reply posted!");
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handleAcceptAnswer = async (replyId) => {
    if (!activeThread) return;
    try {
      const updated = await acceptForumAnswer(activeThread.id, replyId);
      setActiveThread((prev) => ({
        ...prev,
        replies: prev.replies.map((r) => (r.id === replyId ? updated : r)),
      }));
      setSuccess("Answer accepted as solution!");
    } catch (err) { setError(parseApiError(err).message); }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="section-heading">
        <div>
          <h2>Knowledge Hub & Discussion Forum</h2>
          <p className="muted">Read best-practice articles, ask technical questions, and share domain expertise.</p>
        </div>
        <div style={{ display: "flex", gap: "10px" }}>
          <button
            type="button"
            className={`btn ${subTab === "articles" ? "btn--primary" : "btn--ghost"}`}
            onClick={() => { setSubTab("articles"); setActiveThread(null); }}
          >
            <Icon name="book" size={16} /> Knowledge Base
          </button>
          <button
            type="button"
            className={`btn ${subTab === "forum" ? "btn--primary" : "btn--ghost"}`}
            onClick={() => setSubTab("forum")}
          >
            <Icon name="message" size={16} /> Community Forum
          </button>
        </div>
      </div>

      {error && <Alert type="error">{error}</Alert>}
      {success && <Alert type="success">{success}</Alert>}

      {/* ARTICLES SUBTAB */}
      {subTab === "articles" && (
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
            <input
              type="text"
              placeholder="Search articles by keyword or tag..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ flex: 1, minWidth: "240px", padding: "10px 14px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
            />
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => setShowArticleModal(true)}
            >
              <Icon name="plus" size={16} /> Publish Article
            </button>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "14px" }}>
            {articles.length === 0 ? (
              <div className="panel" style={{ gridColumn: "1/-1", textAlign: "center", color: "#8795a9", padding: "32px" }}>
                No articles matching your search.
              </div>
            ) : (
              articles.map((a) => (
                <div key={a.id} className="panel" style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
                  <span className="eyebrow">{a.categoryName}</span>
                  <strong style={{ fontSize: "1.05rem" }}>{a.title}</strong>
                  <p style={{ margin: 0, fontSize: "0.85rem", color: "#5b6d86", lineHeight: 1.5 }}>
                    {a.content.length > 140 ? a.content.substring(0, 137) + "..." : a.content}
                  </p>
                  {a.tags && (
                    <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "4px" }}>
                      {a.tags.split(",").map((t) => (
                        <span key={t} className="badge" style={{ fontSize: "0.72rem" }}>#{t.trim()}</span>
                      ))}
                    </div>
                  )}
                  <div style={{ marginTop: "auto", paddingTop: "8px", borderTop: "1px solid #f0f4f9", fontSize: "0.78rem", color: "#8795a9", display: "flex", justifyContent: "space-between" }}>
                    <span>By {a.authorName}</span>
                    <span>{new Date(a.createdAt).toLocaleDateString()}</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* FORUM SUBTAB */}
      {subTab === "forum" && (
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          {!activeThread ? (
            <>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: "12px", flexWrap: "wrap" }}>
                <input
                  type="text"
                  placeholder="Search forum topics..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  style={{ flex: 1, minWidth: "240px", padding: "10px 14px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                />
                <button
                  type="button"
                  className="btn btn--primary"
                  onClick={() => setShowThreadModal(true)}
                >
                  <Icon name="plus" size={16} /> Ask Question
                </button>
              </div>

              <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                {threads.length === 0 ? (
                  <div className="panel" style={{ textAlign: "center", color: "#8795a9", padding: "32px" }}>
                    No discussions found. Ask a question to start the conversation!
                  </div>
                ) : (
                  threads.map((t) => (
                    <div
                      key={t.id}
                      onClick={() => openThread(t.id)}
                      className="panel"
                      style={{
                        cursor: "pointer",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        gap: "12px",
                        flexWrap: "wrap",
                      }}
                    >
                      <div>
                        <span className="badge">{t.category}</span>
                        <h3 style={{ margin: "6px 0 4px", fontSize: "1.05rem" }}>{t.title}</h3>
                        <p style={{ margin: 0, fontSize: "0.84rem", color: "#5b6d86" }}>
                          {t.content.length > 120 ? t.content.substring(0, 117) + "..." : t.content}
                        </p>
                        <small style={{ color: "#8795a9", marginTop: "4px", display: "inline-block" }}>
                          Asked by {t.authorName} • {new Date(t.createdAt).toLocaleDateString()}
                        </small>
                      </div>
                      <div style={{ display: "flex", alignItems: "center", gap: "6px", color: "#2563eb", fontWeight: 700, fontSize: "0.9rem" }}>
                        <Icon name="message" size={18} /> {t.replyCount} answers &rarr;
                      </div>
                    </div>
                  ))
                )}
              </div>
            </>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              <button
                type="button"
                className="text-action"
                onClick={() => setActiveThread(null)}
                style={{ alignSelf: "flex-start" }}
              >
                &larr; Back to all discussions
              </button>

              <div className="panel">
                <span className="badge">{activeThread.category}</span>
                <h1 style={{ fontSize: "1.4rem", margin: "8px 0" }}>{activeThread.title}</h1>
                <p style={{ fontSize: "0.95rem", color: "#0f2747", lineHeight: 1.6 }}>{activeThread.content}</p>
                <small style={{ color: "#8795a9" }}>
                  Posted by {activeThread.authorName} on {new Date(activeThread.createdAt).toLocaleString()}
                </small>
              </div>

              <h3>Replies ({activeThread.replies ? activeThread.replies.length : 0})</h3>
              <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                {activeThread.replies && activeThread.replies.map((rep) => (
                  <div
                    key={rep.id}
                    className="panel"
                    style={{
                      borderLeft: rep.isAcceptedAnswer ? "4px solid #17754f" : "1.5px solid #e2e9f3",
                      background: rep.isAcceptedAnswer ? "#f9fefb" : "#fff",
                    }}
                  >
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "6px" }}>
                      <strong>{rep.authorName}</strong>
                      {rep.isAcceptedAnswer && (
                        <span className="badge" style={{ background: "#e6f6ee", color: "#17754f" }}>
                          <Icon name="check" size={14} /> ACCEPTED SOLUTION
                        </span>
                      )}
                    </div>
                    <p style={{ margin: "4px 0 10px", fontSize: "0.9rem", color: "#0f2747", lineHeight: 1.5 }}>
                      {rep.content}
                    </p>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                      <small style={{ color: "#8795a9" }}>{new Date(rep.createdAt).toLocaleString()}</small>
                      {activeThread.authorId === currentUserId && !rep.isAcceptedAnswer && (
                        <button
                          type="button"
                          className="btn btn--complete"
                          onClick={() => handleAcceptAnswer(rep.id)}
                          style={{ padding: "4px 10px", fontSize: "0.78rem" }}
                        >
                          Mark as Accepted Answer
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              {/* Reply form */}
              <div className="panel">
                <h3>Post Your Answer</h3>
                <form onSubmit={handlePostReply} style={{ display: "flex", flexDirection: "column", gap: "10px", marginTop: "10px" }}>
                  <textarea
                    rows={4}
                    required
                    placeholder="Write your solution or technical explanation..."
                    value={replyContent}
                    onChange={(e) => setReplyContent(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  />
                  <button type="submit" className="btn btn--primary" style={{ alignSelf: "flex-start" }}>
                    Submit Answer
                  </button>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modal: Publish Article */}
      {showArticleModal && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(11,37,69,0.45)", backdropFilter: "blur(4px)", display: "grid", placeItems: "center", zIndex: 999, padding: "20px" }}>
          <div className="panel" style={{ width: "100%", maxWidth: "540px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
              <h2 style={{ margin: 0, fontSize: "1.1rem" }}>Publish Knowledge Article</h2>
              <button type="button" className="btn btn--ghost" onClick={() => setShowArticleModal(false)}><Icon name="x" size={18} /></button>
            </div>
            <form onSubmit={handleCreateArticle} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Category</label>
                <select value={artCatId} onChange={(e) => setArtCatId(e.target.value)} style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}>
                  {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Title</label>
                <input type="text" required placeholder="Article title..." value={artTitle} onChange={(e) => setArtTitle(e.target.value)} style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }} />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Tags (comma-separated)</label>
                <input type="text" placeholder="java, spring, security" value={artTags} onChange={(e) => setArtTags(e.target.value)} style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }} />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Content</label>
                <textarea rows={5} required placeholder="Full article content..." value={artContent} onChange={(e) => setArtContent(e.target.value)} style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }} />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                <button type="button" className="btn btn--ghost" onClick={() => setShowArticleModal(false)}>Cancel</button>
                <button type="submit" className="btn btn--primary">Publish</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: New Forum Thread */}
      {showThreadModal && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(11,37,69,0.45)", backdropFilter: "blur(4px)", display: "grid", placeItems: "center", zIndex: 999, padding: "20px" }}>
          <div className="panel" style={{ width: "100%", maxWidth: "540px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
              <h2 style={{ margin: 0, fontSize: "1.1rem" }}>Start Discussion Topic</h2>
              <button type="button" className="btn btn--ghost" onClick={() => setShowThreadModal(false)}><Icon name="x" size={18} /></button>
            </div>
            <form onSubmit={handleCreateThread} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Category</label>
                <input type="text" placeholder="e.g. Architecture, Security, Frontend" value={thrCat} onChange={(e) => setThrCat(e.target.value)} style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }} />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Question Title</label>
                <input type="text" required placeholder="What is your question?" value={thrTitle} onChange={(e) => setThrTitle(e.target.value)} style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }} />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Details & Code Context</label>
                <textarea rows={5} required placeholder="Describe what you tried, errors encountered..." value={thrContent} onChange={(e) => setThrContent(e.target.value)} style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }} />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                <button type="button" className="btn btn--ghost" onClick={() => setShowThreadModal(false)}>Cancel</button>
                <button type="submit" className="btn btn--primary">Post Topic</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
