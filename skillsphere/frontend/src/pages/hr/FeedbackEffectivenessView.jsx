import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getHrFeedback } from "../../services/hrService.js";

export default function FeedbackEffectivenessView() {
  const [data, setData] = useState(null);
  const [activeTab, setActiveTab] = useState("platform"); // 'platform' or 'mentorship'
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const res = await getHrFeedback();
      setData(res);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load feedback analytics.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Feedback & Training Effectiveness</h3>
            <p className="hr-card-subtitle">
              Qualitative and quantitative metrics on learning initiatives, platform experience, and mentorship outcomes
            </p>
          </div>
          <button className="btn-secondary btn-sm" onClick={loadData}>
            <Icon name="refresh" size={14} /> Refresh
          </button>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}

          {/* Stat Grid */}
          <div className="hr-stat-grid">
            <div className="hr-stat-card">
              <div className="hr-stat-header">
                <span>Platform CSAT</span>
                <div className="hr-stat-icon emerald">
                  <Icon name="award" size={18} />
                </div>
              </div>
              <div className="hr-stat-value" style={{ color: "var(--hr-emerald)" }}>
                ★ {data?.overallPlatformRating ?? 4.5}
              </div>
              <div className="hr-stat-meta">{data?.totalPlatformFeedbacks ?? 0} reviews received</div>
            </div>

            <div className="hr-stat-card">
              <div className="hr-stat-header">
                <span>Mentorship Effectiveness</span>
                <div className="hr-stat-icon primary">
                  <Icon name="users" size={18} />
                </div>
              </div>
              <div className="hr-stat-value" style={{ color: "var(--hr-primary)" }}>
                ★ {data?.overallMentorshipRating ?? 4.8}
              </div>
              <div className="hr-stat-meta">{data?.totalMentorshipFeedbacks ?? 0} post-session evaluations</div>
            </div>
          </div>

          {/* Category breakdown */}
          {data?.ratingByCategory && Object.keys(data.ratingByCategory).length > 0 && (
            <div style={{ marginBottom: "24px", padding: "16px", background: "#f8fafc", borderRadius: "8px", border: "1px solid var(--hr-line)" }}>
              <h4 style={{ margin: "0 0 12px", fontSize: "14px", fontWeight: 700 }}>
                Platform Satisfaction by Category
              </h4>
              <div style={{ display: "flex", flexWrap: "wrap", gap: "16px" }}>
                {Object.entries(data.ratingByCategory).map(([cat, score]) => (
                  <div
                    key={cat}
                    style={{
                      padding: "8px 14px",
                      background: "#ffffff",
                      border: "1px solid var(--hr-line)",
                      borderRadius: "6px",
                      display: "flex",
                      alignItems: "center",
                      gap: "8px",
                      fontSize: "13px",
                    }}
                  >
                    <span style={{ fontWeight: 600, color: "var(--hr-ink)" }}>{cat}</span>
                    <span className="status-pill success" style={{ padding: "2px 6px" }}>
                      ★ {score}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Subtabs */}
          <div style={{ display: "flex", borderBottom: "1px solid var(--hr-line)", marginBottom: "16px" }}>
            <button
              style={{
                padding: "8px 16px",
                border: "none",
                background: "transparent",
                fontWeight: 600,
                fontSize: "13.5px",
                color: activeTab === "platform" ? "var(--hr-primary)" : "var(--hr-soft)",
                borderBottom: activeTab === "platform" ? "2px solid var(--hr-primary)" : "none",
                cursor: "pointer",
              }}
              onClick={() => setActiveTab("platform")}
            >
              Platform Experience Feedback ({data?.platformFeedbacks?.length ?? 0})
            </button>
            <button
              style={{
                padding: "8px 16px",
                border: "none",
                background: "transparent",
                fontWeight: 600,
                fontSize: "13.5px",
                color: activeTab === "mentorship" ? "var(--hr-primary)" : "var(--hr-soft)",
                borderBottom: activeTab === "mentorship" ? "2px solid var(--hr-primary)" : "none",
                cursor: "pointer",
              }}
              onClick={() => setActiveTab("mentorship")}
            >
              Mentorship Feedback ({data?.mentorshipFeedbacks?.length ?? 0})
            </button>
          </div>

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading feedback records...</h4>
            </div>
          ) : activeTab === "platform" ? (
            (!data?.platformFeedbacks || data.platformFeedbacks.length === 0) ? (
              <div className="hr-empty-state">
                <p>No platform feedback submissions yet.</p>
              </div>
            ) : (
              <div className="hr-table-wrap">
                <table className="hr-table">
                  <thead>
                    <tr>
                      <th>Contributor</th>
                      <th>Category</th>
                      <th>Rating</th>
                      <th>Feedback Comments</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.platformFeedbacks.map((f) => (
                      <tr key={f.id}>
                        <td>
                          <div style={{ fontWeight: 600 }}>{f.userName}</div>
                          {f.userEmail && <div style={{ fontSize: "11.5px", color: "var(--hr-soft)" }}>{f.userEmail}</div>}
                        </td>
                        <td>
                          <span className="status-pill purple">{f.category}</span>
                        </td>
                        <td>
                          <span className="status-pill success">★ {f.rating} / 5</span>
                        </td>
                        <td style={{ maxWidth: 380, fontSize: "13px" }}>{f.comments}</td>
                        <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                          {f.createdAt ? new Date(f.createdAt).toLocaleDateString() : ""}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          ) : (
            (!data?.mentorshipFeedbacks || data.mentorshipFeedbacks.length === 0) ? (
              <div className="hr-empty-state">
                <p>No mentorship session evaluations logged yet.</p>
              </div>
            ) : (
              <div className="hr-table-wrap">
                <table className="hr-table">
                  <thead>
                    <tr>
                      <th>Mentee (Author)</th>
                      <th>Mentor</th>
                      <th>Rating</th>
                      <th>Comments</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.mentorshipFeedbacks.map((mf) => (
                      <tr key={mf.id}>
                        <td style={{ fontWeight: 600 }}>{mf.authorName}</td>
                        <td>{mf.mentorName}</td>
                        <td>
                          <span className="status-pill success">★ {mf.rating} / 5</span>
                        </td>
                        <td style={{ maxWidth: 380, fontSize: "13px" }}>{mf.comments}</td>
                        <td style={{ fontSize: "12px", color: "var(--hr-soft)" }}>
                          {mf.createdAt ? new Date(mf.createdAt).toLocaleDateString() : ""}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          )}
        </div>
      </div>
    </div>
  );
}
