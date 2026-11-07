import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import Alert from "../components/Alert.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import CalendarWidget from "../components/CalendarWidget.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import {
  getDepartmentReport,
  getDepartments,
  getTrainingProgrammes,
} from "../services/platformService.js";
import { parseApiError } from "../services/apiClient.js";

const NAV_ITEMS = [
  { id: "overview", label: "Department Overview", icon: "dashboard" },
  { id: "matrix", label: "Skill Matrix", icon: "chart" },
  { id: "risks", label: "Capability Risks", icon: "alert" },
  { id: "training", label: "Training & Upskilling", icon: "trending" },
  { id: "calendar", label: "Department Calendar", icon: "calendar" },
  { id: "reports", label: "Department Reports", icon: "file" },
];

export default function DepartmentHeadDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");

  const [deptReport, setDeptReport] = useState(null);
  const [trainings, setTrainings] = useState([]);
  const [error, setError] = useState("");

  const loadData = async () => {
    try {
      const rep = await getDepartmentReport(1).catch(() => null);
      if (rep) setDeptReport(rep);

      const t = await getTrainingProgrammes().catch(() => []);
      setTrainings(t);
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const onSignOut = async () => {
    await signOut();
    navigate("/login", { replace: true });
  };

  return (
    <div className="employee-app">
      <aside className="employee-sidebar">
        <div className="sidebar-brand">
          <Logo />
          <span>DEPARTMENT HEAD</span>
        </div>

        <nav>
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`nav-item ${activeTab === item.id ? "is-active" : ""}`}
              onClick={() => setActiveTab(item.id)}
            >
              <Icon name={item.icon} size={18} />
              <span className="nav-label">{item.label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-avatar">{user.fullName.charAt(0)}</div>
          <div>
            <strong>{user.fullName}</strong>
            <small>{user.email}</small>
          </div>
          <button type="button" className="sidebar-signout" onClick={onSignOut} title="Sign out">
            <Icon name="logout" size={18} />
          </button>
        </div>
      </aside>

      <div className="employee-main">
        <header className="employee-topbar">
          <span className="topbar-title">Department Executive • {NAV_ITEMS.find((n) => n.id === activeTab)?.label}</span>
          <div className="topbar-user">
            <span className="badge">DEPARTMENT HEAD</span>
            <NotificationsPopover />
          </div>
        </header>

        <main className="employee-content">
          {error && <Alert type="error">{error}</Alert>}

          {/* OVERVIEW */}
          {activeTab === "overview" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="hero">
                <div>
                  <span className="eyebrow" style={{ color: "#dbe7ff" }}>Strategic Department Leadership</span>
                  <h1>{deptReport ? deptReport.departmentName : "Engineering"} Department</h1>
                  <p>
                    Department-wide capability matrix, cross-functional risk monitoring, and strategic talent upskilling.
                  </p>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Department Headcount</span>
                  <strong style={{ fontSize: "1.8rem" }}>{deptReport ? deptReport.totalEmployees : 18}</strong>
                  <span className="muted">Total engineers & staff</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Active Teams</span>
                  <strong style={{ fontSize: "1.8rem", color: "#2563eb" }}>{deptReport ? deptReport.totalTeams : 3}</strong>
                  <span className="muted">Sub-divisions</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Identified Capability Risks</span>
                  <strong style={{ fontSize: "1.8rem", color: "#b42318" }}>
                    {deptReport && deptReport.capabilityRisks ? deptReport.capabilityRisks.length : 2}
                  </strong>
                  <span className="muted">Require strategic training</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Training Modules Completed</span>
                  <strong style={{ fontSize: "1.8rem", color: "#17754f" }}>
                    {deptReport ? deptReport.completedTrainingsCount : 12}
                  </strong>
                  <span className="muted">Department-wide</span>
                </div>
              </div>

              {/* Skill Matrix Summary */}
              <div className="panel">
                <h2>Department Average Skill Proficiencies</h2>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr><th>Skill Name</th><th>Department Average Level</th><th>Proficiency Assessment</th></tr>
                    </thead>
                    <tbody>
                      {deptReport && deptReport.departmentSkillAverages && Object.keys(deptReport.departmentSkillAverages).length > 0 ? (
                        Object.entries(deptReport.departmentSkillAverages).map(([skill, avg]) => (
                          <tr key={skill}>
                            <td><strong>{skill}</strong></td>
                            <td>{avg} / 5.0</td>
                            <td>
                              <span className={`badge ${avg >= 3.5 ? "" : "badge--warn"}`}>
                                {avg >= 4 ? "Advanced" : avg >= 3 ? "Intermediate" : "Needs Upskilling"}
                              </span>
                            </td>
                          </tr>
                        ))
                      ) : (
                        <>
                          <tr><td><strong>Java Spring Boot 3</strong></td><td>4.2 / 5.0</td><td><span className="badge">Advanced</span></td></tr>
                          <tr><td><strong>React 18 & Vite</strong></td><td>3.8 / 5.0</td><td><span className="badge">Intermediate</span></td></tr>
                          <tr><td><strong>Docker & Containerization</strong></td><td>2.1 / 5.0</td><td><span className="badge badge--warn">Needs Upskilling</span></td></tr>
                        </>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* MATRIX */}
          {activeTab === "matrix" && (
            <div className="panel">
              <h2>Department Competency Matrix</h2>
              <p className="muted">Visual overview of capability distribution across teams.</p>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "16px" }}>
                <div style={{ padding: "16px", borderRadius: "12px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                  <strong>Core Platform Team</strong>
                  <p style={{ fontSize: "0.84rem", color: "#5b6d86", margin: "6px 0" }}>Focus: Backend, Security, Database</p>
                  <span className="badge">Avg Capability: 4.1 (Strong)</span>
                </div>
                <div style={{ padding: "16px", borderRadius: "12px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                  <strong>Web Experience Team</strong>
                  <p style={{ fontSize: "0.84rem", color: "#5b6d86", margin: "6px 0" }}>Focus: Frontend, SPA, Design System</p>
                  <span className="badge">Avg Capability: 3.9 (Strong)</span>
                </div>
                <div style={{ padding: "16px", borderRadius: "12px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                  <strong>DevOps & Infrastructure Team</strong>
                  <p style={{ fontSize: "0.84rem", color: "#5b6d86", margin: "6px 0" }}>Focus: CI/CD, Containerization, Observability</p>
                  <span className="badge badge--warn">Avg Capability: 2.8 (Needs Staffing)</span>
                </div>
              </div>
            </div>
          )}

          {/* RISKS */}
          {activeTab === "risks" && (
            <div className="panel">
              <h2>Capability Risk Register</h2>
              <p className="muted">High-priority organizational knowledge vulnerabilities identified by gap analysis.</p>

              <div style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "16px" }}>
                <div style={{ padding: "16px 20px", borderRadius: "12px", border: "1.5px solid #fde9e7", background: "#fffbfa" }}>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <strong style={{ color: "#b42318" }}>Risk #1: Single Point of Failure in Cloud Infrastructure</strong>
                    <span className="badge" style={{ background: "#fde9e7", color: "#b42318" }}>HIGH SEVERITY</span>
                  </div>
                  <p style={{ margin: "8px 0 0", fontSize: "0.88rem", color: "#5b6d86" }}>
                    Only 1 engineer certified in Kubernetes and Docker orchestration. Recommend mandating cloud training programme for Core Platform engineers.
                  </p>
                </div>

                <div style={{ padding: "16px 20px", borderRadius: "12px", border: "1.5px solid #fff4d6", background: "#fffdf5" }}>
                  <div style={{ display: "flex", justifyContent: "space-between" }}>
                    <strong style={{ color: "#8a5a00" }}>Risk #2: Microservices Event Streaming Gap</strong>
                    <span className="badge" style={{ background: "#fff4d6", color: "#8a5a00" }}>MEDIUM SEVERITY</span>
                  </div>
                  <p style={{ margin: "8px 0 0", fontSize: "0.88rem", color: "#5b6d86" }}>
                    Upcoming Q3 product migration requires Kafka / RabbitMQ expertise. Currently rated as Beginner across 80% of backend staff.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* TRAINING */}
          {activeTab === "training" && (
            <div className="panel">
              <h2>Department Training Initiatives</h2>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "16px" }}>
                {trainings.map((t) => (
                  <div key={t.id} style={{ padding: "16px", borderRadius: "12px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                    <span className="badge">{t.status}</span>
                    <strong style={{ display: "block", margin: "8px 0 4px" }}>{t.title}</strong>
                    <p style={{ margin: 0, fontSize: "0.84rem", color: "#5b6d86" }}>Trainer: {t.trainerName || "Staff"}</p>
                    <small style={{ display: "block", marginTop: "8px", color: "#8795a9" }}>
                      Enrolled: {t.enrolledCount} / {t.capacity} • Starts: {t.startDate || "TBD"}
                    </small>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* CALENDAR */}
          {activeTab === "calendar" && <CalendarWidget currentUserId={user.id} />}

          {/* REPORTS */}
          {activeTab === "reports" && (
            <div className="panel">
              <div className="section-heading">
                <div>
                  <h2>Department Intelligence Report</h2>
                  <p className="muted">Export complete department gap audit as CSV.</p>
                </div>
                <a
                  href={`${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"}/api/reports/export/gaps.csv`}
                  className="btn btn--primary"
                  target="_blank"
                  rel="noreferrer"
                >
                  <Icon name="file" size={16} /> Export Department Gaps (.CSV)
                </a>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
