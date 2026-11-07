import { useState } from "react";
import Icon from "../../components/Icons.jsx";

export default function LearningRecommendationsView({
  aiRecommendations,
  courseRecommendations = [],
  onRecommendCourse,
}) {
  const [activeSubTab, setActiveSubTab] = useState("ai");

  const recCourses = aiRecommendations?.recommendations || [];
  const source = aiRecommendations?.source || "rules";
  const notice = aiRecommendations?.notice || "";

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header">
        <div>
          <span className="eyebrow">Targeted Upskilling</span>
          <h1>Learning Recommendations</h1>
          <p>
            AI-driven course suggestions tailored to team skill deficits from the verified database
            catalogue. Direct recommendations with employee tracking.
          </p>
        </div>
        <button className="btn btn--primary" onClick={() => onRecommendCourse()}>
          <Icon name="plus" size={16} /> Recommend Course to Employee
        </button>
      </div>

      {/* Sub Tabs */}
      <div style={{ display: "flex", borderBottom: "1px solid #e2e8f0" }}>
        <button
          style={{
            padding: "12px 20px",
            border: "none",
            borderBottom: activeSubTab === "ai" ? "2px solid #2563eb" : "2px solid transparent",
            background: "transparent",
            fontWeight: 700,
            fontSize: "0.9rem",
            color: activeSubTab === "ai" ? "#2563eb" : "#64748b",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            gap: "8px",
          }}
          onClick={() => setActiveSubTab("ai")}
        >
          <Icon name="sparkles" size={16} />
          AI Course Recommendations ({recCourses.length})
        </button>

        <button
          style={{
            padding: "12px 20px",
            border: "none",
            borderBottom: activeSubTab === "history" ? "2px solid #2563eb" : "2px solid transparent",
            background: "transparent",
            fontWeight: 700,
            fontSize: "0.9rem",
            color: activeSubTab === "history" ? "#2563eb" : "#64748b",
            cursor: "pointer",
          }}
          onClick={() => setActiveSubTab("history")}
        >
          Issued Course Recommendations ({courseRecommendations.length})
        </button>
      </div>

      {/* Sub Tab 1: AI Recommendations */}
      {activeSubTab === "ai" && (
        <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          {notice && (
            <div style={{ padding: "12px 16px", background: "#f8fafc", borderRadius: "10px", border: "1px solid #e2e8f0", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
              <span className="muted" style={{ fontSize: "0.85rem" }}>{notice}</span>
              <span className="badge" style={{ background: source === "gemini" ? "#f3e8ff" : "#f1f5f9", color: source === "gemini" ? "#7e22ce" : "#475569" }}>
                Source: {source === "gemini" ? "Google Gemini AI" : "Verified Catalogue Fallback"}
              </span>
            </div>
          )}

          {recCourses.length === 0 ? (
            <div className="panel" style={{ textAlign: "center", padding: "40px" }}>
              <p className="muted">No learning recommendations available for the current team.</p>
            </div>
          ) : (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(360px, 1fr))", gap: "16px" }}>
              {recCourses.map((c, i) => (
                <div key={c.id || i} className="panel" style={{ display: "flex", flexDirection: "column", justifyContent: "space-between", gap: "12px" }}>
                  <div>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "6px" }}>
                      <span className="badge" style={{ background: "#eff6ff", color: "#1d4ed8" }}>
                        {c.skill || "Core Skill"}
                      </span>
                      <span className="badge">{c.level || "Intermediate"}</span>
                    </div>

                    <h3 style={{ fontSize: "1.05rem", margin: "6px 0 4px", fontWeight: 800 }}>
                      {c.title}
                    </h3>

                    <p style={{ fontSize: "0.86rem", color: "#475569", margin: "0 0 10px", lineHeight: 1.4 }}>
                      {c.reason || "Addresses critical team competency requirements."}
                    </p>

                    <div style={{ display: "flex", gap: "12px", fontSize: "0.78rem", color: "#64748b" }}>
                      <span><strong>Provider:</strong> {c.provider || "SkillSphere Academy"}</span>
                      {c.duration && <span><strong>Duration:</strong> {c.duration}</span>}
                    </div>

                    {c.topics && c.topics.length > 0 && (
                      <div style={{ marginTop: "10px", display: "flex", flexWrap: "wrap", gap: "4px" }}>
                        {c.topics.slice(0, 3).map((t, idx) => (
                          <span key={idx} style={{ fontSize: "0.72rem", padding: "2px 6px", borderRadius: "4px", background: "#f1f5f9", color: "#475569" }}>
                            {t}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  <div style={{ paddingTop: "12px", borderTop: "1px solid #e2e8f0", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    {c.courseUrl ? (
                      <a
                        href={c.courseUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-action"
                        style={{ fontSize: "0.82rem" }}
                      >
                        Course Link <Icon name="external" size={14} />
                      </a>
                    ) : <span />}

                    <button
                      className="btn btn--primary"
                      style={{ minHeight: "32px", padding: "6px 14px", fontSize: "0.82rem" }}
                      onClick={() => onRecommendCourse({ courseId: c.id, courseTitle: c.title, skillName: c.skill })}
                    >
                      Recommend to Employee
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Sub Tab 2: Issued Recommendations */}
      {activeSubTab === "history" && (
        <div className="panel" style={{ padding: 0 }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Recommended Course</th>
                  <th>Skill Target</th>
                  <th>Manager Note</th>
                  <th>Status</th>
                  <th>Date Recommended</th>
                </tr>
              </thead>
              <tbody>
                {courseRecommendations.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                      No courses have been recommended by you yet.
                    </td>
                  </tr>
                ) : (
                  courseRecommendations.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <strong>{r.employeeName}</strong>
                      </td>
                      <td>
                        <div>
                          <strong>{r.courseTitle}</strong>
                          <small className="muted" style={{ display: "block" }}>{r.courseProvider} &bull; {r.courseLevel}</small>
                        </div>
                      </td>
                      <td>
                        <span className="badge">{r.skillName}</span>
                      </td>
                      <td>
                        <span style={{ maxWidth: "260px", display: "inline-block" }}>{r.reason}</span>
                      </td>
                      <td>
                        <span
                          className="badge"
                          style={{
                            background:
                              r.status === "ENROLLED" ? "#dcfce7" :
                              r.status === "RECOMMENDED" ? "#eff6ff" : "#f1f5f9",
                            color:
                              r.status === "ENROLLED" ? "#166534" :
                              r.status === "RECOMMENDED" ? "#1d4ed8" : "#475569",
                          }}
                        >
                          {r.status}
                        </span>
                      </td>
                      <td>
                        <small className="muted">{new Date(r.createdAt).toLocaleDateString()}</small>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
