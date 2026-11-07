import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getMySkills,
  getAllSkills,
  addOrUpdateSkill,
  getMyEvidence,
  submitSkillEvidence,
} from "../../services/platformService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function SkillEvidenceSection() {
  const [mySkills, setMySkills] = useState([]);
  const [allSkills, setAllSkills] = useState([]);
  const [myEvidenceList, setMyEvidenceList] = useState([]);
  const [selectedSkillId, setSelectedSkillId] = useState("");
  const [proficiency, setProficiency] = useState("Intermediate");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Evidence modal state
  const [showEvidenceModal, setShowEvidenceModal] = useState(false);
  const [selectedEmpSkillId, setSelectedEmpSkillId] = useState(null);
  const [evidenceTitle, setEvidenceTitle] = useState("");
  const [evidenceDesc, setEvidenceDesc] = useState("");
  const [evidenceUrl, setEvidenceUrl] = useState("");

  const loadData = async () => {
    try {
      const [ms, as, evList] = await Promise.all([
        getMySkills().catch(() => []),
        getAllSkills().catch(() => []),
        getMyEvidence().catch(() => []),
      ]);
      setMySkills(ms);
      setAllSkills(as);
      setMyEvidenceList(evList);
      if (as.length > 0 && !selectedSkillId) setSelectedSkillId(as[0].id);
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleAddSkill = async (e) => {
    e.preventDefault();
    if (!selectedSkillId) return;
    setError(""); setSuccess("");
    try {
      await addOrUpdateSkill({
        skillId: Number(selectedSkillId),
        proficiencyLevel: proficiency,
      });
      setSuccess("Skill self-claim recorded! Skill gaps have been recalculated.");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleSubmitEvidence = async (e) => {
    e.preventDefault();
    if (!selectedEmpSkillId || !evidenceTitle) return;
    setError(""); setSuccess("");
    try {
      await submitSkillEvidence({
        employeeSkillId: selectedEmpSkillId,
        title: evidenceTitle,
        description: evidenceDesc,
        evidenceUrl: evidenceUrl || null,
      });
      setSuccess("Evidence submitted for manager review!");
      setShowEvidenceModal(false);
      setEvidenceTitle(""); setEvidenceDesc(""); setEvidenceUrl("");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="section-heading">
        <div>
          <h2>My Skills & Competencies</h2>
          <p className="muted">Add skills to your profile and submit evidence for manager verification.</p>
        </div>
      </div>

      {error && <Alert type="error">{error}</Alert>}
      {success && <Alert type="success">{success}</Alert>}

      {/* Claim a skill form */}
      <div className="panel" style={{ maxWidth: "600px" }}>
        <h3>Add or Update Self-Claimed Skill</h3>
        <form onSubmit={handleAddSkill} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "12px" }}>
          <div>
            <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Skill</label>
            <select
              value={selectedSkillId}
              onChange={(e) => setSelectedSkillId(e.target.value)}
              style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
            >
              {allSkills.map((s) => (
                <option key={s.id} value={s.id}>{s.name} ({s.categoryName})</option>
              ))}
            </select>
          </div>
          <div>
            <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Self-Assessed Proficiency</label>
            <select
              value={proficiency}
              onChange={(e) => setProficiency(e.target.value)}
              style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
            >
              <option value="Unaware">Unaware (Level 1)</option>
              <option value="Beginner">Beginner (Level 2)</option>
              <option value="Intermediate">Intermediate (Level 3)</option>
              <option value="Advanced">Advanced (Level 4)</option>
              <option value="Expert">Expert (Level 5)</option>
            </select>
          </div>
          <button type="submit" className="btn btn--primary" style={{ alignSelf: "flex-start" }}>
            Save Skill
          </button>
        </form>
      </div>

      {/* Verified / My skills grid */}
      <div className="panel">
        <h3>My Catalogued Skills ({mySkills.length})</h3>
        {mySkills.length === 0 ? (
          <p className="muted">No skills logged yet. Add one above or complete an assessment!</p>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "14px" }}>
            {mySkills.map((s) => (
              <div
                key={s.id}
                style={{
                  padding: "16px",
                  borderRadius: "12px",
                  border: "1.5px solid #e2e9f3",
                  background: "#f9fbfe",
                  display: "flex",
                  flexDirection: "column",
                  gap: "8px",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <span className="eyebrow">{s.categoryName}</span>
                  <span
                    className="badge"
                    style={{
                      background: s.verified ? "#e6f6ee" : "#fff4d6",
                      color: s.verified ? "#17754f" : "#8a5a00",
                    }}
                  >
                    {s.verified ? "VERIFIED" : "SELF-CLAIMED"}
                  </span>
                </div>
                <strong style={{ fontSize: "1.05rem" }}>{s.skillName}</strong>
                <div style={{ fontSize: "0.85rem", color: "#5b6d86" }}>
                  Proficiency: <strong>{s.proficiencyLevel}</strong> (Level {s.numericLevel}/5)
                </div>
                {s.verifiedBy && (
                  <small style={{ color: "#8795a9" }}>Verified by {s.verifiedBy}</small>
                )}
                {!s.verified && (
                  <button
                    type="button"
                    className="text-action"
                    onClick={() => {
                      setSelectedEmpSkillId(s.id);
                      setShowEvidenceModal(true);
                    }}
                    style={{ marginTop: "4px" }}
                  >
                    <Icon name="check" size={14} /> Submit Proof / Evidence &rarr;
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* My Submitted Evidence Section */}
      <div className="panel">
        <h3>My Submitted Evidence ({myEvidenceList.length})</h3>
        {myEvidenceList.length === 0 ? (
          <p className="muted">No evidence submitted yet. Click "Submit Proof / Evidence" on any unverified skill to provide documentation.</p>
        ) : (
          <div className="table-wrap" style={{ marginTop: "12px" }}>
            <table>
              <thead>
                <tr>
                  <th>Skill</th>
                  <th>Evidence Title & Description</th>
                  <th>Artifact Link</th>
                  <th>Status</th>
                  <th>Reviewer</th>
                  <th>Submitted At</th>
                </tr>
              </thead>
              <tbody>
                {myEvidenceList.map((ev) => (
                  <tr key={ev.id}>
                    <td><strong>{ev.skillName}</strong></td>
                    <td>
                      <div><strong>{ev.title}</strong></div>
                      {ev.description && <div style={{ fontSize: "0.82rem", color: "#5b6d86" }}>{ev.description}</div>}
                    </td>
                    <td>
                      {ev.evidenceUrl ? (
                        <a href={ev.evidenceUrl} target="_blank" rel="noopener noreferrer" style={{ color: "#0066cc", fontSize: "0.85rem" }}>
                          View Artifact <Icon name="external" size={13} />
                        </a>
                      ) : "—"}
                    </td>
                    <td>
                      <span
                        className="badge"
                        style={{
                          background: ev.status === "APPROVED" ? "#e6f6ee" : (ev.status === "REJECTED" ? "#fde8e8" : "#fff4d6"),
                          color: ev.status === "APPROVED" ? "#17754f" : (ev.status === "REJECTED" ? "#b91c1c" : "#8a5a00"),
                        }}
                      >
                        {ev.status}
                      </span>
                    </td>
                    <td>{ev.reviewedByName || "Pending Review"}</td>
                    <td>{ev.createdAt ? new Date(ev.createdAt).toLocaleDateString() : "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {showEvidenceModal && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(11,37,69,0.45)",
            backdropFilter: "blur(4px)",
            display: "grid",
            placeItems: "center",
            zIndex: 999,
            padding: "20px",
          }}
        >
          <div className="panel" style={{ width: "100%", maxWidth: "480px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
              <h2 style={{ margin: 0, fontSize: "1.1rem" }}>Submit Skill Evidence</h2>
              <button type="button" className="btn btn--ghost" onClick={() => setShowEvidenceModal(false)} style={{ padding: "4px 8px" }}>
                <Icon name="x" size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmitEvidence} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Evidence Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. GitHub Repository, Certification Link"
                  value={evidenceTitle}
                  onChange={(e) => setEvidenceTitle(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Artifact URL (Optional)</label>
                <input
                  type="text"
                  placeholder="https://github.com/... or https://coursera.org/verify/..."
                  value={evidenceUrl}
                  onChange={(e) => setEvidenceUrl(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                />
              </div>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Description of Contribution / Work</label>
                <textarea
                  rows={3}
                  placeholder="Summarize your hands-on implementation..."
                  value={evidenceDesc}
                  onChange={(e) => setEvidenceDesc(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                />
              </div>
              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                <button type="button" className="btn btn--ghost" onClick={() => setShowEvidenceModal(false)}>Cancel</button>
                <button type="submit" className="btn btn--primary">Submit for Review</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
