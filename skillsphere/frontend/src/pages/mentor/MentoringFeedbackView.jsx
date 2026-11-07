import React, { useState, useEffect } from "react";
import {
  getMyMentees,
  getMentorFeedback,
  createMentorEvaluation,
} from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function MentoringFeedbackView() {
  const [feedbackData, setFeedbackData] = useState(null);
  const [mentees, setMentees] = useState([]);
  const [activeTab, setActiveTab] = useState("RECEIVED"); // RECEIVED vs SUBMITTED
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Submit Evaluation Modal
  const [showEvalModal, setShowEvalModal] = useState(false);
  const [employeeId, setEmployeeId] = useState("");
  const [rating, setRating] = useState(5);
  const [strengths, setStrengths] = useState("");
  const [areasForGrowth, setAreasForGrowth] = useState("");
  const [comments, setComments] = useState("");
  const [submittingEval, setSubmittingEval] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [fb, mList] = await Promise.all([
        getMentorFeedback(),
        getMyMentees("ACTIVE"),
      ]);
      setFeedbackData(fb);
      setMentees(mList);
      if (mList.length > 0 && !employeeId) {
        setEmployeeId(mList[0].employeeId);
      }
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleSubmitEvaluation = async (e) => {
    e.preventDefault();
    if (!employeeId || !comments) return;
    setSubmittingEval(true);
    setError("");
    setSuccess("");
    try {
      await createMentorEvaluation({
        employeeId: Number(employeeId),
        rating: Number(rating),
        strengths,
        areasForGrowth,
        comments,
      });
      setSuccess("Mentee evaluation successfully submitted!");
      setShowEvalModal(false);
      setStrengths("");
      setAreasForGrowth("");
      setComments("");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmittingEval(false);
    }
  };

  const renderStars = (count) => {
    return Array.from({ length: 5 }, (_, i) => (
      <span key={i} style={{ color: i < count ? "#f59e0b" : "#cbd5e1", fontSize: "1.1rem" }}>
        ★
      </span>
    ));
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      {/* Analytics Summary */}
      {feedbackData && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: "16px" }}>
          <div className="mentor-kpi-card">
            <span className="mentor-kpi-label">Average Mentee Rating</span>
            <div style={{ display: "flex", alignItems: "center", gap: "10px", marginTop: "4px" }}>
              <span className="mentor-kpi-value" style={{ color: "#d97706" }}>
                {feedbackData.averageRating > 0 ? feedbackData.averageRating.toFixed(1) : "N/A"}
              </span>
              <div className="mt-stars">{renderStars(Math.round(feedbackData.averageRating))}</div>
            </div>
            <span className="mentor-kpi-sub">Based on {feedbackData.totalReviewsReceived} mentee reviews</span>
          </div>

          <div className="mentor-kpi-card">
            <span className="mentor-kpi-label">Reviews Received</span>
            <span className="mentor-kpi-value">{feedbackData.totalReviewsReceived}</span>
            <span className="mentor-kpi-sub">Feedback on completed mentoring sessions</span>
          </div>

          <div className="mentor-kpi-card">
            <span className="mentor-kpi-label">Evaluations Delivered</span>
            <span className="mentor-kpi-value">{feedbackData.submittedEvaluations.length}</span>
            <span className="mentor-kpi-sub">Performance & growth reviews given to mentees</span>
          </div>
        </div>
      )}

      {/* Main Feedback Panel */}
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Mentorship Feedback & Evaluations</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Review mentee ratings and author developmental appraisals.
            </p>
          </div>

          <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
            <div className="mentor-filter-bar">
              <button
                type="button"
                className={`mentor-pill-btn ${activeTab === "RECEIVED" ? "is-active" : ""}`}
                onClick={() => setActiveTab("RECEIVED")}
              >
                Mentee Feedback Received ({feedbackData?.receivedFeedback?.length || 0})
              </button>
              <button
                type="button"
                className={`mentor-pill-btn ${activeTab === "SUBMITTED" ? "is-active" : ""}`}
                onClick={() => setActiveTab("SUBMITTED")}
              >
                My Mentee Evaluations ({feedbackData?.submittedEvaluations?.length || 0})
              </button>
            </div>

            <button
              type="button"
              className="mt-btn mt-btn--primary"
              onClick={() => setShowEvalModal(true)}
            >
              + Evaluate Mentee
            </button>
          </div>
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}
        {success && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{success}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading feedback...</p>
        ) : activeTab === "RECEIVED" ? (
          /* Received Feedback List */
          feedbackData?.receivedFeedback?.length === 0 ? (
            <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
              <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No feedback received from mentees yet.</p>
              <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>Session feedback submitted by mentees after completed sessions will appear here.</p>
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
              {feedbackData.receivedFeedback.map((f) => (
                <div
                  key={f.id}
                  style={{
                    padding: "18px",
                    borderRadius: "10px",
                    border: "1px solid var(--mt-line)",
                    background: "#ffffff",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "flex-start",
                    gap: "12px",
                  }}
                >
                  <div style={{ flex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "4px" }}>
                      <strong>{f.menteeName}</strong>
                      <span className="mt-badge mt-badge--neutral">Session: {f.sessionTitle}</span>
                      <div className="mt-stars">{renderStars(f.rating)}</div>
                    </div>
                    <small style={{ color: "var(--mt-faint)", display: "block", marginBottom: "8px" }}>
                      Submitted on {new Date(f.createdAt).toLocaleDateString()}
                    </small>
                    <p style={{ margin: 0, fontSize: "0.9rem", color: "var(--mt-ink)", fontStyle: "italic" }}>
                      &ldquo;{f.comments}&rdquo;
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )
        ) : (
          /* Submitted Evaluations List */
          feedbackData?.submittedEvaluations?.length === 0 ? (
            <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
              <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No evaluations submitted yet.</p>
              <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>Click "+ Evaluate Mentee" to record structured performance feedback for your mentees.</p>
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
              {feedbackData.submittedEvaluations.map((ev) => (
                <div
                  key={ev.id}
                  style={{
                    padding: "18px",
                    borderRadius: "10px",
                    border: "1px solid var(--mt-line)",
                    background: "#f8fafc",
                    display: "flex",
                    flexDirection: "column",
                    gap: "10px",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <strong style={{ fontSize: "1.05rem" }}>Evaluation for {ev.employeeName}</strong>
                      <small style={{ color: "var(--mt-faint)", display: "block" }}>
                        Delivered on {new Date(ev.createdAt).toLocaleDateString()}
                      </small>
                    </div>
                    <div className="mt-stars">{renderStars(ev.rating)}</div>
                  </div>

                  {ev.strengths && (
                    <div style={{ fontSize: "0.86rem" }}>
                      <strong style={{ color: "var(--mt-ok)" }}>Demonstrated Strengths:</strong> {ev.strengths}
                    </div>
                  )}

                  {ev.areasForGrowth && (
                    <div style={{ fontSize: "0.86rem" }}>
                      <strong style={{ color: "var(--mt-warn)" }}>Target Areas for Growth:</strong> {ev.areasForGrowth}
                    </div>
                  )}

                  <div style={{ fontSize: "0.88rem", color: "var(--mt-soft)", fontStyle: "italic" }}>
                    &ldquo;{ev.comments}&rdquo;
                  </div>
                </div>
              ))}
            </div>
          )
        )}
      </div>

      {/* Evaluate Mentee Modal */}
      {showEvalModal && (
        <div className="mt-modal-backdrop" onClick={() => setShowEvalModal(false)}>
          <div className="mt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>Author Mentee Evaluation</h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setShowEvalModal(false)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleSubmitEvaluation}>
              <div className="mt-modal-body">
                <div className="mt-form-group">
                  <label className="mt-label">Mentee *</label>
                  <select
                    className="mt-select"
                    required
                    value={employeeId}
                    onChange={(e) => setEmployeeId(e.target.value)}
                  >
                    {mentees.map((m) => (
                      <option key={m.employeeId} value={m.employeeId}>
                        {m.employeeName} ({m.jobRole})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Overall Progress Rating (1 to 5) *</label>
                  <select
                    className="mt-select"
                    value={rating}
                    onChange={(e) => setRating(Number(e.target.value))}
                  >
                    <option value={5}>5 Stars &mdash; Outstanding Technical Growth</option>
                    <option value={4}>4 Stars &mdash; Strong Progress, Consistently Improving</option>
                    <option value={3}>3 Stars &mdash; Satisfactory, Meeting Core Goals</option>
                    <option value={2}>2 Stars &mdash; Needs More Focus on Key Milestones</option>
                    <option value={1}>1 Star &mdash; Unsatisfactory Engagement</option>
                  </select>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Demonstrated Strengths</label>
                  <input
                    type="text"
                    className="mt-input"
                    value={strengths}
                    onChange={(e) => setStrengths(e.target.value)}
                    placeholder="e.g. Rapid adoption of reactive patterns, strong problem solving"
                  />
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Areas for Further Growth</label>
                  <input
                    type="text"
                    className="mt-input"
                    value={areasForGrowth}
                    onChange={(e) => setAreasForGrowth(e.target.value)}
                    placeholder="e.g. Unit test coverage, production observability"
                  />
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Constructive Coaching Comments *</label>
                  <textarea
                    rows={4}
                    required
                    className="mt-textarea"
                    value={comments}
                    onChange={(e) => setComments(e.target.value)}
                    placeholder="Provide actionable feedback and encouragement..."
                  />
                </div>
              </div>

              <div className="mt-modal-footer">
                <button
                  type="button"
                  className="mt-btn mt-btn--outline"
                  onClick={() => setShowEvalModal(false)}
                  disabled={submittingEval}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="mt-btn mt-btn--primary"
                  disabled={submittingEval}
                >
                  {submittingEval ? "Submitting..." : "Submit Evaluation"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
