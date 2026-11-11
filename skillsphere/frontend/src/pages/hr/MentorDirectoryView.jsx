import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import { getHrMentors, getHrCompetencies } from "../../services/hrService.js";

export default function MentorDirectoryView() {
  const [mentors, setMentors] = useState([]);
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedSkill, setSelectedSkill] = useState("ALL");
  const [search, setSearch] = useState("");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [mList, sList] = await Promise.all([
        getHrMentors(),
        getHrCompetencies(),
      ]);
      setMentors(mList);
      setSkills(sList);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load mentor directory.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const filteredMentors = mentors.filter((m) => {
    if (selectedSkill !== "ALL") {
      const matchSkill = m.expertiseAreas && m.expertiseAreas.toLowerCase().includes(selectedSkill.toLowerCase());
      if (!matchSkill) return false;
    }
    if (search.trim()) {
      const s = search.toLowerCase();
      const matchName = m.fullName.toLowerCase().includes(s);
      const matchDept = m.department && m.department.toLowerCase().includes(s);
      const matchExp = m.expertiseAreas && m.expertiseAreas.toLowerCase().includes(s);
      if (!matchName && !matchDept && !matchExp) return false;
    }
    return true;
  });

  return (
    <div>
      <div className="hr-card">
        <div className="hr-card-header">
          <div>
            <h3 className="hr-card-title">Mentor Governance Directory</h3>
            <p className="hr-card-subtitle">
              Oversee organizational mentor capacity, subject-matter expertise pools, and feedback scores
            </p>
          </div>
          <button className="btn-secondary btn-sm" onClick={loadData}>
            <Icon name="refresh" size={14} /> Refresh
          </button>
        </div>

        <div className="hr-card-body">
          {error && <Alert type="danger" message={error} />}

          {/* Filter Bar */}
          <div className="hr-filter-bar">
            <input
              type="text"
              className="form-control hr-search-input"
              placeholder="Search by mentor name, department, or expertise..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="form-control"
              style={{ width: "auto" }}
              value={selectedSkill}
              onChange={(e) => setSelectedSkill(e.target.value)}
            >
              <option value="ALL">All Competencies</option>
              {skills.map((s) => (
                <option key={s.id} value={s.name}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>

          <div
            style={{
              padding: "10px 14px",
              background: "#f1f5f9",
              borderRadius: "6px",
              fontSize: "12px",
              color: "var(--hr-soft)",
              marginBottom: "16px",
            }}
          >
            <strong>Mentorship Privacy Protocol:</strong> HR Specialists govern mentor eligibility, cohort capacities, and aggregate rating metrics. One-on-one session transcripts and private mentor preparation notes are strictly confidential to the mentor and mentee.
          </div>

          {loading ? (
            <div className="hr-empty-state">
              <Icon name="spinner" className="spin" size={28} />
              <h4>Loading mentor directory...</h4>
            </div>
          ) : filteredMentors.length === 0 ? (
            <div className="hr-empty-state">
              <Icon name="users" size={32} />
              <h4>No mentors match your criteria</h4>
              <p>Approved mentors will appear here once verified.</p>
            </div>
          ) : (
            <div className="hr-table-wrap">
              <table className="hr-table">
                <thead>
                  <tr>
                    <th>Mentor</th>
                    <th>Department</th>
                    <th>Role</th>
                    <th>Expertise Areas</th>
                    <th>Cohort Capacity</th>
                    <th>Sessions Completed</th>
                    <th>Avg Rating</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredMentors.map((m) => (
                    <tr key={m.mentorId}>
                      <td>
                        <div style={{ fontWeight: 600, color: "var(--hr-ink)" }}>{m.fullName}</div>
                        <div style={{ fontSize: "12px", color: "var(--hr-soft)" }}>{m.headline || m.email}</div>
                      </td>
                      <td>{m.department}</td>
                      <td>{m.jobRole}</td>
                      <td style={{ maxWidth: 280 }}>
                        <div style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
                          {m.expertiseTags.map((tag, idx) => (
                            <span key={idx} className="status-pill purple" style={{ fontSize: "11px" }}>
                              {tag}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>
                        <span style={{ fontWeight: 600 }}>{m.activeMenteesCount}</span> / {m.maxMentees} active
                      </td>
                      <td>{m.completedSessionsCount} sessions</td>
                      <td>
                        <span className="status-pill success">
                          ★ {m.avgRating.toFixed(1)} / 5.0
                        </span>
                      </td>
                      <td>
                        <span className={`status-pill ${m.isActive ? "success" : "neutral"}`}>
                          {m.isActive ? "Available" : "At Capacity"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
