import { useEffect, useState } from "react";
import Icon from "./Icons.jsx";
import Alert from "./Alert.jsx";
import {
  getOngoingAttempt,
  getAttemptById,
  saveAttemptAnswers,
  submitAttempt,
  startAssessment,
  getAssessmentById,
  submitAssessment,
} from "../services/platformService.js";
import { parseApiError } from "../services/apiClient.js";

export default function AssessmentRunner({
  attemptId = null,
  initialAttempt = null,
  assessmentId = null,
  skillId = null,
  proficiencyLevel = "Intermediate",
  assignmentId = null,
  onComplete,
  onCancel,
}) {
  const [attempt, setAttempt] = useState(initialAttempt);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState({}); // questionId -> selectedIndex
  const [loading, setLoading] = useState(!initialAttempt);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  useEffect(() => {
    if (initialAttempt) {
      setAttempt(initialAttempt);
      if (initialAttempt.savedAnswers) {
        setAnswers(initialAttempt.savedAnswers);
      }
      setLoading(false);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setError("");

    const loadOrStart = async () => {
      try {
        if (attemptId) {
          const data = await getAttemptById(attemptId);
          if (isMounted) {
            setAttempt(data);
            if (data.savedAnswers) setAnswers(data.savedAnswers);
          }
        } else if (skillId || assessmentId) {
          const data = await startAssessment({
            skillId,
            assessmentId,
            proficiencyLevel,
            assignmentId,
          });
          if (isMounted) {
            setAttempt(data);
            if (data.savedAnswers) setAnswers(data.savedAnswers);
          }
        } else {
          // Check if there's an ongoing attempt
          const ongoing = await getOngoingAttempt();
          if (isMounted) {
            if (ongoing) {
              setAttempt(ongoing);
              if (ongoing.savedAnswers) setAnswers(ongoing.savedAnswers);
            } else if (assessmentId) {
              // Legacy fallback
              const legacy = await getAssessmentById(assessmentId);
              setAttempt({
                attemptId: null,
                isLegacy: true,
                legacyId: legacy.id,
                skillName: legacy.title,
                targetLevel: "Standard",
                generationSource: "FALLBACK",
                passScore: legacy.passScore,
                timeLimitMinutes: legacy.timeLimitMinutes,
                questions: legacy.questions,
              });
            } else {
              setError("No assessment selected or ongoing attempt found.");
            }
          }
        }
      } catch (err) {
        if (isMounted) {
          setError(parseApiError(err).message || "Failed to initialize assessment.");
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    loadOrStart();
    return () => {
      isMounted = false;
    };
  }, [attemptId, assessmentId, skillId, proficiencyLevel, assignmentId, initialAttempt]);

  const selectOption = (qId, optionIdx) => {
    const updated = { ...answers, [qId]: optionIdx };
    setAnswers(updated);

    // Auto-save answers if working with a database-backed attempt
    if (attempt && attempt.attemptId && !attempt.isLegacy) {
      saveAttemptAnswers(attempt.attemptId, updated).catch((e) => {
        console.warn("Auto-save attempt answers error:", e);
      });
    }
  };

  const handleNext = () => {
    if (attempt && currentIndex < attempt.questions.length - 1) {
      setCurrentIndex((prev) => prev + 1);
    }
  };

  const handlePrev = () => {
    if (currentIndex > 0) {
      setCurrentIndex((prev) => prev - 1);
    }
  };

  const handleSubmit = async () => {
    if (!attempt) return;
    setSubmitting(true);
    setError("");

    try {
      let res;
      if (attempt.attemptId && !attempt.isLegacy) {
        res = await submitAttempt(attempt.attemptId, { answers });
      } else {
        // Legacy submission fallback
        res = await submitAssessment(attempt.legacyId || assessmentId, {
          assignmentId,
          answers,
        });
      }
      setResult(res);
      setShowConfirmModal(false);
      if (onComplete) {
        onComplete(res);
      }
    } catch (err) {
      setError(parseApiError(err).message || "Failed to submit assessment.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="panel" style={{ textAlign: "center", padding: "60px 20px" }}>
        <div style={{ display: "inline-block", marginBottom: "16px" }}>
          <div
            style={{
              width: "44px",
              height: "44px",
              border: "3.5px solid #e2e9f3",
              borderTopColor: "#2563eb",
              borderRadius: "50%",
              animation: "spin 0.8s linear infinite",
              margin: "0 auto",
            }}
          />
        </div>
        <h3 style={{ fontSize: "1.2rem", margin: "0 0 8px", color: "#0f2747" }}>
          Preparing your personalized assessment...
        </h3>
        <p style={{ margin: 0, fontSize: "0.88rem", color: "#5b6d86" }}>
          Generating dynamic questions tailored to your proficiency level using Google Gemini AI.
        </p>
        <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (error && !attempt) {
    return (
      <div className="panel" style={{ textAlign: "center", padding: "40px" }}>
        <Alert type="error">{error}</Alert>
        <button
          type="button"
          className="btn btn--primary"
          onClick={onCancel}
          style={{ marginTop: "16px" }}
        >
          Back to Dashboard
        </button>
      </div>
    );
  }

  // --- Assessment Results View with Question-by-Question Explanations ---
  if (result) {
    const isAi = result.generationSource === "AI_GENERATED";
    return (
      <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
        <div
          style={{
            textAlign: "center",
            padding: "24px 20px",
            background: result.passed ? "var(--e-ok-t, #e6f6ee)" : "var(--e-bad-t, #fde9e7)",
            borderRadius: "14px",
          }}
        >
          <div style={{ display: "flex", justifyContent: "center", gap: "8px", marginBottom: "10px" }}>
            <span
              className="badge"
              style={{
                fontSize: "0.85rem",
                padding: "4px 14px",
                background: result.passed ? "#17754f" : "#b42318",
                color: "#fff",
                fontWeight: 700,
              }}
            >
              {result.passed ? "ASSESSMENT PASSED" : "ASSESSMENT NOT PASSED"}
            </span>
            <span
              className="badge"
              style={{
                fontSize: "0.82rem",
                padding: "4px 12px",
                background: isAi ? "#7c3aed" : "#64748b",
                color: "#fff",
              }}
            >
              {isAi ? "✨ AI-Generated (Gemini)" : "Standard Fallback"}
            </span>
          </div>

          <h2 style={{ fontSize: "2.4rem", margin: "6px 0", color: "#0f2747" }}>
            {result.score}%
          </h2>
          <p style={{ color: "#334155", fontSize: "0.95rem", margin: 0, fontWeight: 500 }}>
            You answered <strong>{result.correctAnswers}</strong> out of{" "}
            <strong>{result.totalQuestions}</strong> questions correctly in{" "}
            <strong>{result.skillName || result.assessmentTitle}</strong> ({result.targetLevel || "Standard"}).
          </p>
        </div>

        {result.updatedGaps && result.updatedGaps.length > 0 && (
          <div>
            <h3 style={{ fontSize: "1.1rem", marginBottom: "12px", color: "#0f2747" }}>
              Calibrated Skill Gaps
            </h3>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))",
                gap: "12px",
              }}
            >
              {result.updatedGaps.map((g) => (
                <div
                  key={g.skillId}
                  style={{
                    padding: "12px 14px",
                    borderRadius: "10px",
                    border: "1px solid #e2e9f3",
                    background: "#f9fbfe",
                  }}
                >
                  <strong style={{ display: "block", fontSize: "0.9rem" }}>{g.skillName}</strong>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      fontSize: "0.8rem",
                      marginTop: "4px",
                      color: "#5b6d86",
                    }}
                  >
                    <span>Current: <strong>{g.currentProficiency}</strong></span>
                    <span>Required: <strong>{g.requiredProficiency}</strong></span>
                  </div>
                  <div style={{ marginTop: "6px" }}>
                    <span
                      className="badge"
                      style={{
                        fontSize: "0.72rem",
                        background:
                          g.priority === "HIGH" ? "#fde9e7" : g.priority === "MEDIUM" ? "#fff4d6" : "#e6f6ee",
                        color:
                          g.priority === "HIGH" ? "#b42318" : g.priority === "MEDIUM" ? "#8a5a00" : "#17754f",
                      }}
                    >
                      {g.priority === "ON_TRACK" ? "On Track" : `${g.priority} Priority Gap`}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Detailed Question Review & Explanations */}
        {result.questionResults && result.questionResults.length > 0 && (
          <div>
            <h3 style={{ fontSize: "1.1rem", marginBottom: "12px", color: "#0f2747" }}>
              Detailed Question Review & Explanations
            </h3>
            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              {result.questionResults.map((qr, idx) => (
                <div
                  key={qr.questionId || idx}
                  style={{
                    padding: "16px",
                    borderRadius: "12px",
                    border: qr.isCorrect ? "1.5px solid #a7f3d0" : "1.5px solid #fecaca",
                    background: qr.isCorrect ? "#f0fdf4" : "#fef2f2",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                    <span style={{ fontSize: "0.8rem", fontWeight: 700, color: qr.isCorrect ? "#15803d" : "#b91c1c" }}>
                      Question {idx + 1} • {qr.isCorrect ? "✓ CORRECT" : "✗ INCORRECT"}
                    </span>
                    <span
                      className="badge"
                      style={{
                        fontSize: "0.7rem",
                        background:
                          qr.difficulty === "HARD" ? "#fde9e7" : qr.difficulty === "MEDIUM" ? "#fff4d6" : "#e6f6ee",
                        color:
                          qr.difficulty === "HARD" ? "#b42318" : qr.difficulty === "MEDIUM" ? "#8a5a00" : "#17754f",
                      }}
                    >
                      {qr.difficulty} • {qr.subtopic}
                    </span>
                  </div>

                  <p style={{ margin: "0 0 12px", fontWeight: 600, fontSize: "0.92rem", color: "#0f2747" }}>
                    {qr.questionText}
                  </p>

                  <div style={{ display: "flex", flexDirection: "column", gap: "6px", marginBottom: "12px" }}>
                    {qr.options && qr.options.map((opt, optIdx) => {
                      const isUserChoice = qr.selectedOptionIndex === optIdx;
                      const isCorrectChoice = qr.correctAnswerIndex === optIdx;
                      let border = "1px solid #e2e9f3";
                      let bg = "#fff";
                      let textColor = "#475569";

                      if (isCorrectChoice) {
                        border = "2px solid #16a34a";
                        bg = "#dcfce7";
                        textColor = "#15803d";
                      } else if (isUserChoice && !qr.isCorrect) {
                        border = "2px solid #dc2626";
                        bg = "#fee2e2";
                        textColor = "#b91c1c";
                      }

                      return (
                        <div
                          key={optIdx}
                          style={{
                            padding: "8px 12px",
                            borderRadius: "8px",
                            border,
                            background: bg,
                            fontSize: "0.85rem",
                            fontWeight: isCorrectChoice || isUserChoice ? 600 : 400,
                            color: textColor,
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "center",
                          }}
                        >
                          <span>{opt}</span>
                          {isCorrectChoice && <span style={{ fontSize: "0.75rem", fontWeight: 700 }}>Correct Answer</span>}
                          {isUserChoice && !isCorrectChoice && <span style={{ fontSize: "0.75rem", fontWeight: 700 }}>Your Answer</span>}
                        </div>
                      );
                    })}
                  </div>

                  {qr.explanation && (
                    <div
                      style={{
                        background: "#fff",
                        padding: "10px 12px",
                        borderRadius: "8px",
                        border: "1px dashed #cbd5e1",
                        fontSize: "0.82rem",
                        color: "#334155",
                      }}
                    >
                      <strong>Explanation: </strong>
                      {qr.explanation}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        <div style={{ display: "flex", justifyContent: "center", gap: "12px", marginTop: "10px" }}>
          <button type="button" className="btn btn--primary" onClick={onCancel}>
            Back to Dashboard
          </button>
        </div>
      </div>
    );
  }

  if (!attempt || !attempt.questions || attempt.questions.length === 0) {
    return (
      <div className="panel" style={{ textAlign: "center", padding: "40px" }}>
        <p className="muted">No questions available for this assessment attempt.</p>
        <button type="button" className="btn btn--primary" onClick={onCancel} style={{ marginTop: "16px" }}>
          Back to Dashboard
        </button>
      </div>
    );
  }

  // --- Active Assessment Runner Interface ---
  const currentQ = attempt.questions[currentIndex];
  const totalCount = attempt.questions.length;
  const answeredCount = Object.keys(answers).length;
  const progressPct = Math.round((answeredCount / totalCount) * 100);
  const isAiAttempt = attempt.generationSource === "AI_GENERATED";

  return (
    <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      {/* Top Header */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "12px" }}>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}>
            <span className="eyebrow" style={{ margin: 0 }}>
              {attempt.categoryName || "Technical Assessment"}
            </span>
            <span
              className="badge"
              style={{
                fontSize: "0.72rem",
                padding: "2px 8px",
                background: isAiAttempt ? "var(--e-accent-t, #eef2ff)" : "#f1f5f9",
                color: isAiAttempt ? "#4f46e5" : "#475569",
                fontWeight: 600,
              }}
            >
              {isAiAttempt ? "✨ AI-Powered (Gemini)" : "Standard Bank"}
            </span>
            <span className="badge" style={{ fontSize: "0.72rem", padding: "2px 8px" }}>
              Level: {attempt.targetLevel || "Intermediate"}
            </span>
          </div>
          <h1 style={{ margin: "4px 0", fontSize: "1.4rem", color: "#0f2747" }}>
            {attempt.skillName} Skill Assessment
          </h1>
        </div>

        <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
          <span className="badge">Pass: {attempt.passScore}%</span>
          <span className="badge">Time: {attempt.timeLimitMinutes} min</span>
        </div>
      </div>

      {error && <Alert type="error">{error}</Alert>}

      {/* Progress Bar & Status */}
      <div>
        <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.82rem", color: "#5b6d86", marginBottom: "6px" }}>
          <span>Question <strong>{currentIndex + 1}</strong> of <strong>{totalCount}</strong></span>
          <span>Answered: <strong>{answeredCount}</strong> / {totalCount} ({progressPct}%)</span>
        </div>
        <div style={{ width: "100%", height: "6px", background: "#e2e9f3", borderRadius: "999px", overflow: "hidden" }}>
          <div
            style={{
              width: `${progressPct}%`,
              height: "100%",
              background: progressPct === 100 ? "var(--e-ok, #17754f)" : "#2563eb",
              transition: "width 0.25s ease",
            }}
          />
        </div>
      </div>

      {/* Question Number Pills Navigation */}
      <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", padding: "4px 0" }}>
        {attempt.questions.map((q, idx) => {
          const isAnswered = answers[q.id] !== undefined;
          const isCurrent = idx === currentIndex;
          return (
            <button
              key={q.id}
              type="button"
              onClick={() => setCurrentIndex(idx)}
              style={{
                width: "32px",
                height: "32px",
                borderRadius: "8px",
                border: isCurrent ? "2px solid #2563eb" : "1px solid #cbd5e1",
                background: isCurrent ? "#2563eb" : isAnswered ? "#dbeafe" : "#fff",
                color: isCurrent ? "#fff" : isAnswered ? "#1e40af" : "#475569",
                fontWeight: isCurrent || isAnswered ? 700 : 500,
                fontSize: "0.8rem",
                cursor: "pointer",
                transition: "all 0.15s ease",
              }}
              title={`Question ${idx + 1}${isAnswered ? " (Answered)" : ""}`}
            >
              {idx + 1}
            </button>
          );
        })}
      </div>

      {/* Current Question Card */}
      <div
        style={{
          padding: "20px 22px",
          borderRadius: "14px",
          border: "1.5px solid #e2e9f3",
          background: "#fff",
          boxShadow: "0 1px 3px rgba(0,0,0,0.04)",
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "12px" }}>
          <span style={{ fontSize: "0.8rem", fontWeight: 800, color: "#2563eb", textTransform: "uppercase" }}>
            Question {currentIndex + 1} • {currentQ.subtopic || currentQ.skillName}
          </span>
          <div style={{ display: "flex", gap: "6px", alignItems: "center" }}>
            <span
              className="badge"
              style={{
                fontSize: "0.7rem",
                background:
                  currentQ.difficulty === "HARD" ? "#fde9e7" : currentQ.difficulty === "MEDIUM" ? "#fff4d6" : "#e6f6ee",
                color:
                  currentQ.difficulty === "HARD" ? "#b42318" : currentQ.difficulty === "MEDIUM" ? "#8a5a00" : "#17754f",
              }}
            >
              {currentQ.difficulty || "MEDIUM"}
            </span>
            <span style={{ fontSize: "0.75rem", color: "#8795a9" }}>
              {currentQ.points} pt{currentQ.points > 1 ? "s" : ""}
            </span>
          </div>
        </div>

        <p style={{ margin: "0 0 18px", fontWeight: 600, fontSize: "1.05rem", color: "#0f2747", lineHeight: 1.5 }}>
          {currentQ.questionText}
        </p>

        {/* 4 Clean Radio Options */}
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          {currentQ.options &&
            currentQ.options.map((opt, optIdx) => {
              const selected = answers[currentQ.id] === optIdx;
              return (
                <label
                  key={optIdx}
                  onClick={() => selectOption(currentQ.id, optIdx)}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "12px",
                    padding: "12px 16px",
                    borderRadius: "10px",
                    border: selected ? "2px solid #2563eb" : "1.5px solid #e2e9f3",
                    background: selected ? "var(--e-tint, #eaf1ff)" : "#fff",
                    cursor: "pointer",
                    fontSize: "0.92rem",
                    fontWeight: selected ? 600 : 400,
                    color: selected ? "#1d4ed8" : "#0f2747",
                    transition: "all 0.15s ease",
                  }}
                >
                  <input
                    type="radio"
                    name={`question-${currentQ.id}`}
                    checked={selected}
                    onChange={() => selectOption(currentQ.id, optIdx)}
                    style={{ accentColor: "#2563eb", transform: "scale(1.15)" }}
                  />
                  <span>{opt}</span>
                </label>
              );
            })}
        </div>
      </div>

      {/* Navigation Buttons */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingTop: "8px" }}>
        <div>
          <button
            type="button"
            className="btn btn--ghost"
            onClick={handlePrev}
            disabled={currentIndex === 0 || submitting}
          >
            ← Previous
          </button>
        </div>

        <div style={{ display: "flex", gap: "10px" }}>
          <button
            type="button"
            className="btn btn--ghost"
            onClick={onCancel}
            disabled={submitting}
          >
            Exit Assessment
          </button>

          {currentIndex < totalCount - 1 ? (
            <button
              type="button"
              className="btn btn--primary"
              onClick={handleNext}
              disabled={submitting}
            >
              Next Question →
            </button>
          ) : (
            <button
              type="button"
              className="btn btn--primary"
              onClick={() => setShowConfirmModal(true)}
              disabled={submitting}
            >
              {submitting ? "Submitting…" : `Review & Submit (${answeredCount}/${totalCount})`}
            </button>
          )}
        </div>
      </div>

      {/* Confirmation Modal */}
      {showConfirmModal && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(15, 39, 71, 0.45)",
            backdropFilter: "blur(3px)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
        >
          <div
            className="panel"
            style={{
              maxWidth: "460px",
              width: "90%",
              padding: "24px",
              borderRadius: "16px",
              boxShadow: "0 20px 40px rgba(0,0,0,0.2)",
            }}
          >
            <h3 style={{ margin: "0 0 10px", fontSize: "1.2rem", color: "#0f2747" }}>
              Submit Assessment?
            </h3>
            <p style={{ fontSize: "0.9rem", color: "#5b6d86", margin: "0 0 16px" }}>
              You have answered <strong>{answeredCount}</strong> out of{" "}
              <strong>{totalCount}</strong> questions.
              {answeredCount < totalCount && (
                <span style={{ color: "#b42318", display: "block", marginTop: "6px" }}>
                  Warning: You have {totalCount - answeredCount} unanswered question(s). Unanswered questions receive 0 points.
                </span>
              )}
            </p>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
              <button
                type="button"
                className="btn btn--ghost"
                onClick={() => setShowConfirmModal(false)}
                disabled={submitting}
              >
                Back to Test
              </button>
              <button
                type="button"
                className="btn btn--primary"
                onClick={handleSubmit}
                disabled={submitting}
              >
                {submitting ? "Submitting…" : "Confirm & Submit"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
