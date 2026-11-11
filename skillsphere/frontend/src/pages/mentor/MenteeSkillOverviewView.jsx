import React, { useState, useEffect } from "react";
import { getMyMentees, getMenteeSkills } from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function MenteeSkillOverviewView() {
  const [mentees, setMentees] = useState([]);
  const [selectedMenteeId, setSelectedMenteeId] = useState("");
  const [skillData, setSkillData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    getMyMentees("ACTIVE")
      .then((data) => {
        setMentees(data);
        if (data.length > 0) {
          setSelectedMenteeId(data[0].employeeId);
        }
      })
      .catch((err) => setError(parseApiError(err).message));
  }, []);

  useEffect(() => {
    if (!selectedMenteeId) return;
    setLoading(true);
    setError("");
    getMenteeSkills(selectedMenteeId)
      .then((data) => setSkillData(data))
      .catch((err) => setError(parseApiError(err).message))
      .finally(() => setLoading(false));
  }, [selectedMenteeId]);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Mentee Skill Overview & Gap Intelligence</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Inspect verified competencies, proficiency gaps, and dynamic assessment results for your active mentees.
            </p>
          </div>

          {/* Mentee Selector */}
          {mentees.length > 0 && (
            <div style={{ minWidth: "260px" }}>
              <select
                className="mt-select"
                value={selectedMenteeId}
                onChange={(e) => setSelectedMenteeId(e.target.value)}
              >
                {mentees.map((m) => (
                  <option key={m.employeeId} value={m.employeeId}>
                    {m.employeeName} ({m.jobRole})
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {/* Governance Notice */}
        <div
          style={{
            padding: "12px 16px",
            borderRadius: "8px",
            background: "#eff6ff",
            border: "1px solid #bfdbfe",
            color: "#1e40af",
            fontSize: "0.84rem",
            marginBottom: "16px",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <span>
            <strong>Read-Only Governance:</strong> Mentors can inspect skills and gaps to tailor guidance.
            Official proficiency records and assessment scores can only be validated via the enterprise assessment engine.
          </span>
          <span className="mt-badge mt-badge--active">Consent Active</span>
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading mentee skill profile...</p>
        ) : !skillData ? (
          <p style={{ color: "var(--mt-soft)" }}>Select an active mentee to review their competencies.</p>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
            {/* Verified Skills */}
            <div>
              <h3 style={{ fontSize: "1.05rem", margin: "0 0 12px" }}>
                Verified Skills ({skillData.skills.length})
              </h3>
              {skillData.skills.length === 0 ? (
                <p style={{ color: "var(--mt-soft)", fontStyle: "italic" }}>
                  No verified skills found for this mentee.
                </p>
              ) : (
                <div className="mentor-table-wrap">
                  <table className="mentor-table">
                    <thead>
                      <tr>
                        <th>Skill Name</th>
                        <th>Category</th>
                        <th>Proficiency Level</th>
                        <th>Verification Status</th>
                        <th>Last Verified</th>
                      </tr>
                    </thead>
                    <tbody>
                      {skillData.skills.map((sk) => (
                        <tr key={sk.skillId}>
                          <td><strong>{sk.skillName}</strong></td>
                          <td>{sk.category}</td>
                          <td>
                            <span className="mt-badge mt-badge--neutral">
                              {sk.proficiencyLevel}
                            </span>
                          </td>
                          <td>
                            <span className={`mt-badge ${sk.verified ? "mt-badge--active" : "mt-badge--pending"}`}>
                              {sk.verified ? "Verified" : "Self-Reported"}
                            </span>
                          </td>
                          <td>{sk.updatedAt ? new Date(sk.updatedAt).toLocaleDateString() : "N/A"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Knowledge Gaps */}
            <div>
              <h3 style={{ fontSize: "1.05rem", margin: "0 0 12px" }}>
                Identified Skill Gaps ({skillData.gaps.length})
              </h3>
              {skillData.gaps.length === 0 ? (
                <p style={{ color: "var(--mt-soft)", fontStyle: "italic" }}>
                  No active skill gaps identified for this role.
                </p>
              ) : (
                <div className="mentor-table-wrap">
                  <table className="mentor-table">
                    <thead>
                      <tr>
                        <th>Target Competency</th>
                        <th>Current Demonstrated Level</th>
                        <th>Required Role Benchmark</th>
                        <th>Priority / Severity</th>
                      </tr>
                    </thead>
                    <tbody>
                      {skillData.gaps.map((gp) => (
                        <tr key={gp.gapId}>
                          <td><strong>{gp.skillName}</strong></td>
                          <td>
                            <span className="mt-badge mt-badge--declined">{gp.currentProficiency}</span>
                          </td>
                          <td>
                            <span className="mt-badge mt-badge--active">{gp.requiredProficiency}</span>
                          </td>
                          <td>
                            <span
                              className={`mt-badge ${
                                gp.severity === "HIGH" || gp.severity === "CRITICAL"
                                  ? "mt-badge--declined"
                                  : "mt-badge--pending"
                              }`}
                            >
                              {gp.severity}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Dynamic Assessment History */}
            <div>
              <h3 style={{ fontSize: "1.05rem", margin: "0 0 12px" }}>
                Recent Dynamic Assessments ({skillData.assessments.length})
              </h3>
              {skillData.assessments.length === 0 ? (
                <p style={{ color: "var(--mt-soft)", fontStyle: "italic" }}>
                  No AI dynamic assessment records found for this mentee.
                </p>
              ) : (
                <div className="mentor-table-wrap">
                  <table className="mentor-table">
                    <thead>
                      <tr>
                        <th>Assessment</th>
                        <th>Skill Tested</th>
                        <th>Score</th>
                        <th>Result</th>
                        <th>Date Taken</th>
                      </tr>
                    </thead>
                    <tbody>
                      {skillData.assessments.map((att) => (
                        <tr key={att.assessmentId}>
                          <td><strong>{att.assessmentTitle}</strong></td>
                          <td>{att.skillName}</td>
                          <td><strong>{att.score}%</strong></td>
                          <td>
                            <span className={`mt-badge ${att.passed ? "mt-badge--active" : "mt-badge--declined"}`}>
                              {att.passed ? "Passed" : "Failed"}
                            </span>
                          </td>
                          <td>{new Date(att.completedAt).toLocaleDateString()}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
