import { useState } from "react";
import Icon from "../../components/Icons.jsx";

export default function AssessmentsView({
  assessments = [],
  reassessments = [],
  pendingEvidence = [],
  onRequestReassessment,
  onCancelReassessment,
  onReviewEvidence,
}) {
  const [activeSubTab, setActiveSubTab] = useState("attempts");

  const pendingCount = reassessments.filter((r) => r.status === "PENDING").length;
  const pendingEvidenceCount = pendingEvidence.length;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="page-header">
        <div>
          <span className="eyebrow">Assessment Operations</span>
          <h1>Employee Assessments & Reassessment Requests</h1>
          <p>
            Review AI-generated and standard competency assessments completed by team members.
            Issue and track formal reassessment requests for targeted skill validation.
          </p>
        </div>
        <button className="btn btn--primary" onClick={() => onRequestReassessment()}>
          <Icon name="plus" size={16} /> Request Reassessment
        </button>
      </div>

      {/* Sub Tabs */}
      <div style={{ display: "flex", borderBottom: "1px solid #e2e8f0" }}>
        <button
          style={{
            padding: "12px 20px",
            border: "none",
            borderBottom: activeSubTab === "attempts" ? "2px solid #2563eb" : "2px solid transparent",
            background: "transparent",
            fontWeight: 700,
            fontSize: "0.9rem",
            color: activeSubTab === "attempts" ? "#2563eb" : "#64748b",
            cursor: "pointer",
          }}
          onClick={() => setActiveSubTab("attempts")}
        >
          Team Assessment Attempts ({assessments.length})
        </button>

        <button
          style={{
            padding: "12px 20px",
            border: "none",
            borderBottom: activeSubTab === "reassessments" ? "2px solid #2563eb" : "2px solid transparent",
            background: "transparent",
            fontWeight: 700,
            fontSize: "0.9rem",
            color: activeSubTab === "reassessments" ? "#2563eb" : "#64748b",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            gap: "8px",
          }}
          onClick={() => setActiveSubTab("reassessments")}
        >
          Reassessment Requests ({reassessments.length})
          {pendingCount > 0 && (
            <span className="badge" style={{ background: "#fee2e2", color: "#991b1b" }}>
              {pendingCount} Pending
            </span>
          )}
        </button>

        <button
          style={{
            padding: "12px 20px",
            border: "none",
            borderBottom: activeSubTab === "evidence" ? "2px solid #2563eb" : "2px solid transparent",
            background: "transparent",
            fontWeight: 700,
            fontSize: "0.9rem",
            color: activeSubTab === "evidence" ? "#2563eb" : "#64748b",
            cursor: "pointer",
            display: "flex",
            alignItems: "center",
            gap: "8px",
          }}
          onClick={() => setActiveSubTab("evidence")}
        >
          Skill Evidence Review ({pendingEvidence.length})
          {pendingEvidenceCount > 0 && (
            <span className="badge" style={{ background: "#fef3c7", color: "#92400e" }}>
              {pendingEvidenceCount} Pending
            </span>
          )}
        </button>
      </div>

      {/* Sub Tab 1: Assessment Attempts */}
      {activeSubTab === "attempts" && (
        <div className="panel" style={{ padding: 0 }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Attempt ID</th>
                  <th>Skill & Category</th>
                  <th>Target Level</th>
                  <th>Generation Source</th>
                  <th>Questions</th>
                  <th>Status</th>
                  <th>Started At</th>
                </tr>
              </thead>
              <tbody>
                {assessments.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                      No assessment attempts recorded for your team.
                    </td>
                  </tr>
                ) : (
                  assessments.map((a) => (
                    <tr key={a.attemptId}>
                      <td>
                        <span style={{ fontFamily: "monospace", fontWeight: 600 }}>#{a.attemptId}</span>
                      </td>
                      <td>
                        <div>
                          <strong>{a.skillName}</strong>
                          <small className="muted" style={{ display: "block" }}>{a.categoryName}</small>
                        </div>
                      </td>
                      <td>
                        <span className="badge" style={{ background: "#eff6ff", color: "#1d4ed8" }}>
                          {a.targetLevel}
                        </span>
                      </td>
                      <td>
                        <span className="badge" style={{ background: "#f5f3ff", color: "#7c3aed" }}>
                          {a.generationSource || "AI_DYNAMIC"}
                        </span>
                      </td>
                      <td>
                        <span>{a.totalQuestions} Questions</span>
                      </td>
                      <td>
                        <span
                          className="badge"
                          style={{
                            background:
                              a.status === "COMPLETED" ? "#dcfce7" :
                              a.status === "IN_PROGRESS" ? "#fef3c7" : "#f1f5f9",
                            color:
                              a.status === "COMPLETED" ? "#166534" :
                              a.status === "IN_PROGRESS" ? "#92400e" : "#475569",
                          }}
                        >
                          {a.status}
                        </span>
                      </td>
                      <td>
                        <small className="muted">{new Date(a.startedAt).toLocaleString()}</small>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Sub Tab 2: Reassessment Requests */}
      {activeSubTab === "reassessments" && (
        <div className="panel" style={{ padding: 0 }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Request ID</th>
                  <th>Employee</th>
                  <th>Skill Target</th>
                  <th>Manager Justification</th>
                  <th>Status</th>
                  <th>Requested Date</th>
                  <th style={{ textAlign: "right" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {reassessments.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                      No reassessment requests have been initiated yet.
                    </td>
                  </tr>
                ) : (
                  reassessments.map((r) => (
                    <tr key={r.id}>
                      <td>
                        <span style={{ fontFamily: "monospace", fontWeight: 600 }}>#{r.id}</span>
                      </td>
                      <td>
                        <div>
                          <strong>{r.employeeName}</strong>
                          <small className="muted">{r.employeeEmail}</small>
                        </div>
                      </td>
                      <td>
                        <div>
                          <strong>{r.skillName}</strong>
                          <small className="muted" style={{ display: "block" }}>{r.categoryName}</small>
                        </div>
                      </td>
                      <td>
                        <span style={{ maxWidth: "260px", display: "inline-block" }}>{r.reason}</span>
                      </td>
                      <td>
                        <span
                          className="badge"
                          style={{
                            background:
                              r.status === "PENDING" ? "#fee2e2" :
                              r.status === "COMPLETED" ? "#dcfce7" : "#f1f5f9",
                            color:
                              r.status === "PENDING" ? "#991b1b" :
                              r.status === "COMPLETED" ? "#166534" : "#475569",
                          }}
                        >
                          {r.status}
                        </span>
                      </td>
                      <td>
                        <small className="muted">{new Date(r.createdAt).toLocaleDateString()}</small>
                      </td>
                      <td style={{ textAlign: "right" }}>
                        {r.status === "PENDING" && (
                          <button
                            className="btn btn--ghost"
                            style={{ padding: "4px 10px", minHeight: "28px", fontSize: "0.78rem", color: "#dc2626" }}
                            onClick={() => onCancelReassessment(r.id)}
                          >
                            Cancel Request
                          </button>
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Sub Tab 3: Pending Evidence Review */}
      {activeSubTab === "evidence" && (
        <div className="panel" style={{ padding: 0 }}>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Evidence ID</th>
                  <th>Employee</th>
                  <th>Skill & Proficiency</th>
                  <th>Evidence Type</th>
                  <th>Verification Evidence</th>
                  <th>Submitted Date</th>
                  <th style={{ textAlign: "right" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pendingEvidence.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: "center", padding: "36px", color: "#64748b" }}>
                      No pending skill evidence submissions from your team.
                    </td>
                  </tr>
                ) : (
                  pendingEvidence.map((ev) => (
                    <tr key={ev.id}>
                      <td>
                        <span style={{ fontFamily: "monospace", fontWeight: 600 }}>#{ev.id}</span>
                      </td>
                      <td>
                        <div>
                          <strong>{ev.employeeName || "Team Member"}</strong>
                        </div>
                      </td>
                      <td>
                        <div>
                          <strong>{ev.skillName}</strong>
                          {ev.currentLevel && (
                            <small className="muted" style={{ display: "block" }}>
                              Level: {ev.currentLevel}
                            </small>
                          )}
                        </div>
                      </td>
                      <td>
                        <span className="badge" style={{ background: "#f5f3ff", color: "#7c3aed" }}>
                          {ev.evidenceType || "CERTIFICATE"}
                        </span>
                      </td>
                      <td>
                        {ev.evidenceUrl ? (
                          <a
                            href={ev.evidenceUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                              display: "inline-flex",
                              alignItems: "center",
                              gap: "4px",
                              color: "#2563eb",
                              textDecoration: "underline",
                              fontSize: "0.85rem",
                              maxWidth: "220px",
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              whiteSpace: "nowrap",
                            }}
                          >
                            <Icon name="link" size={14} />
                            {ev.evidenceUrl}
                          </a>
                        ) : (
                          <span className="muted">No URL provided</span>
                        )}
                      </td>
                      <td>
                        <small className="muted">
                          {ev.createdAt ? new Date(ev.createdAt).toLocaleDateString() : "N/A"}
                        </small>
                      </td>
                      <td style={{ textAlign: "right" }}>
                        <div style={{ display: "inline-flex", gap: "8px" }}>
                          <button
                            className="btn btn--primary"
                            style={{
                              padding: "4px 12px",
                              minHeight: "28px",
                              fontSize: "0.78rem",
                              background: "#059669",
                              borderColor: "#059669",
                            }}
                            onClick={() => onReviewEvidence && onReviewEvidence(ev.id, "APPROVED")}
                          >
                            <Icon name="check" size={14} /> Approve
                          </button>
                          <button
                            className="btn btn--ghost"
                            style={{
                              padding: "4px 12px",
                              minHeight: "28px",
                              fontSize: "0.78rem",
                              color: "#dc2626",
                              borderColor: "#fecaca",
                            }}
                            onClick={() => onReviewEvidence && onReviewEvidence(ev.id, "REJECTED")}
                          >
                            Reject
                          </button>
                        </div>
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
