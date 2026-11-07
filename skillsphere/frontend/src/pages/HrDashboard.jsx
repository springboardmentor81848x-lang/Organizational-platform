import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import Alert from "../components/Alert.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import AdminMentorship from "./admin/AdminMentorship.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import {
  getOrganizationReport,
  getSkillCategories,
  getAllSkills,
  createSkill,
  getRoleRequirements,
  setRoleRequirement,
  getAllCourses,
  createCourse,
  getTrainingProgrammes,
  createTrainingProgramme,
  getAllPlatformFeedback,
} from "../services/platformService.js";
import { listUsers } from "../services/authService.js";
import { parseApiError } from "../services/apiClient.js";

const NAV_ITEMS = [
  { id: "overview", label: "HR Overview", icon: "dashboard" },
  { id: "directory", label: "Employee Directory", icon: "user" },
  { id: "framework", label: "Competency Framework", icon: "clipboard" },
  { id: "requirements", label: "Role Requirements", icon: "target" },
  { id: "gaps", label: "Org Skill Gap Analysis", icon: "chart" },
  { id: "courses", label: "Course Catalogue", icon: "book" },
  { id: "training", label: "Training Programmes", icon: "trending" },
  { id: "mentors", label: "Mentor Governance", icon: "users" },
  { id: "feedback", label: "Employee Feedback", icon: "lightbulb" },
  { id: "reports", label: "Reports & Analytics", icon: "file" },
];

export default function HrDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");

  const [orgReport, setOrgReport] = useState(null);
  const [employees, setEmployees] = useState([]);
  const [categories, setCategories] = useState([]);
  const [skills, setSkills] = useState([]);
  const [courses, setCourses] = useState([]);
  const [training, setTraining] = useState([]);
  const [feedback, setFeedback] = useState([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // New skill form
  const [newSkillName, setNewSkillName] = useState("");
  const [newSkillDesc, setNewSkillDesc] = useState("");
  const [selectedCatId, setSelectedCatId] = useState("");

  // Role requirement form
  const [targetRole, setTargetRole] = useState("Full Stack Developer");
  const [reqSkillId, setReqSkillId] = useState("");
  const [reqProficiency, setReqProficiency] = useState("Intermediate");
  const [reqPriority, setReqPriority] = useState("HIGH");

  // New course form
  const [courseTitle, setCourseTitle] = useState("");
  const [courseDesc, setCourseDesc] = useState("");
  const [courseSkillId, setCourseSkillId] = useState("");
  const [courseLevel, setCourseLevel] = useState("Intermediate");
  const [courseUrl, setCourseUrl] = useState("");

  // New training programme form
  const [progTitle, setProgTitle] = useState("");
  const [progDesc, setProgDesc] = useState("");
  const [progTrainer, setProgTrainer] = useState("");
  const [progCapacity, setProgCapacity] = useState(25);
  const [progStart, setProgStart] = useState("");
  const [progEnd, setProgEnd] = useState("");

  const loadData = async () => {
    try {
      const rep = await getOrganizationReport().catch(() => null);
      if (rep) setOrgReport(rep);

      const users = await listUsers().catch(() => []);
      setEmployees(users);

      const cats = await getSkillCategories().catch(() => []);
      setCategories(cats);
      if (cats.length > 0 && !selectedCatId) setSelectedCatId(cats[0].id);

      const sks = await getAllSkills().catch(() => []);
      setSkills(sks);
      if (sks.length > 0) {
        if (!reqSkillId) setReqSkillId(sks[0].id);
        if (!courseSkillId) setCourseSkillId(sks[0].id);
      }

      const crs = await getAllCourses().catch(() => []);
      setCourses(crs);

      const tp = await getTrainingProgrammes().catch(() => []);
      setTraining(tp);

      const fb = await getAllPlatformFeedback().catch(() => []);
      setFeedback(fb);
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateSkill = async (e) => {
    e.preventDefault();
    if (!selectedCatId || !newSkillName) return;
    setError(""); setSuccess("");
    try {
      await createSkill({ categoryId: Number(selectedCatId), name: newSkillName, description: newSkillDesc });
      setSuccess("Skill created successfully!");
      setNewSkillName(""); setNewSkillDesc("");
      loadData();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handleSetRequirement = async (e) => {
    e.preventDefault();
    if (!targetRole || !reqSkillId) return;
    setError(""); setSuccess("");
    try {
      await setRoleRequirement({
        jobRole: targetRole,
        skillId: Number(reqSkillId),
        requiredProficiency: reqProficiency,
        priority: reqPriority,
      });
      setSuccess("Role skill requirement saved!");
      loadData();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handleCreateCourse = async (e) => {
    e.preventDefault();
    if (!courseTitle || !courseSkillId) return;
    setError(""); setSuccess("");
    try {
      await createCourse({
        title: courseTitle,
        description: courseDesc,
        skillId: Number(courseSkillId),
        level: courseLevel,
        durationHours: 12,
        provider: "SkillSphere Learning",
        courseUrl: courseUrl || "https://learning.skillsphere.com",
      });
      setSuccess("Course added to catalogue!");
      setCourseTitle(""); setCourseDesc(""); setCourseUrl("");
      loadData();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handleCreateTraining = async (e) => {
    e.preventDefault();
    if (!progTitle) return;
    setError(""); setSuccess("");
    try {
      await createTrainingProgramme({
        title: progTitle,
        description: progDesc,
        trainerName: progTrainer,
        capacity: Number(progCapacity),
        startDate: progStart || null,
        endDate: progEnd || null,
        status: "UPCOMING",
      });
      setSuccess("Training programme announced!");
      setProgTitle(""); setProgDesc(""); setProgTrainer("");
      loadData();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const onSignOut = async () => {
    await signOut();
    navigate("/login", { replace: true });
  };

  return (
    <div className="employee-app">
      <aside className="employee-sidebar">
        <div className="sidebar-brand">
          <Logo />
          <span>HR WORKSPACE</span>
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
          <span className="topbar-title">HR Intelligence • {NAV_ITEMS.find((n) => n.id === activeTab)?.label}</span>
          <div className="topbar-user">
            <span className="badge">HR SPECIALIST</span>
            <NotificationsPopover />
          </div>
        </header>

        <main className="employee-content">
          {error && <Alert type="error">{error}</Alert>}
          {success && <Alert type="success">{success}</Alert>}

          {/* OVERVIEW */}
          {activeTab === "overview" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="hero">
                <div>
                  <span className="eyebrow" style={{ color: "#dbe7ff" }}>Human Resources & Talent Capability</span>
                  <h1>Welcome, {user.fullName.split(" ")[0]}</h1>
                  <p>
                    Govern the organizational competency framework, monitor workforce gaps, and schedule company-wide training programmes.
                  </p>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Workforce Headcount</span>
                  <strong style={{ fontSize: "1.8rem" }}>{orgReport ? orgReport.totalUsers : employees.length}</strong>
                  <span className="muted">Total registered users</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Skills in Framework</span>
                  <strong style={{ fontSize: "1.8rem", color: "#2563eb" }}>
                    {orgReport ? orgReport.totalSkillsCatalogued : skills.length}
                  </strong>
                  <span className="muted">Active competencies</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Open Skill Gaps</span>
                  <strong style={{ fontSize: "1.8rem", color: "#b42318" }}>
                    {orgReport ? orgReport.totalGapsIdentified : 8}
                  </strong>
                  <span className="muted">Identified via assessments</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Completed Courses</span>
                  <strong style={{ fontSize: "1.8rem", color: "#17754f" }}>
                    {orgReport ? orgReport.totalCoursesCompleted : 14}
                  </strong>
                  <span className="muted">Workforce completions</span>
                </div>
              </div>

              {/* Department breakdown table */}
              <div className="panel">
                <h2>Department Capability Breakdown</h2>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr><th>Department</th><th>Employees</th><th>Open Gaps</th><th>Trainings Completed</th></tr>
                    </thead>
                    <tbody>
                      {orgReport && orgReport.departmentSummaries && orgReport.departmentSummaries.length > 0 ? (
                        orgReport.departmentSummaries.map((d, i) => (
                          <tr key={i}>
                            <td><strong>{d.departmentName}</strong></td>
                            <td>{d.employeeCount} employees</td>
                            <td>
                              <span className={`badge ${d.openGapsCount > 3 ? "badge--warn" : ""}`}>
                                {d.openGapsCount} open gaps
                              </span>
                            </td>
                            <td>{d.completedTrainingsCount} completed</td>
                          </tr>
                        ))
                      ) : (
                        <tr><td><strong>Engineering</strong></td><td>18 employees</td><td>4 open gaps</td><td>12 completed</td></tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* DIRECTORY */}
          {activeTab === "directory" && (
            <div className="panel">
              <h2>Employee Directory</h2>
              <p className="muted">All employees across departments, their assigned roles, and current statuses.</p>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {employees.map((e) => (
                      <tr key={e.id}>
                        <td><strong>{e.fullName}</strong><small>{e.employeeId || "No ID"}</small></td>
                        <td>{e.email}</td>
                        <td><span className="badge">{e.role}</span></td>
                        <td><span className={`badge ${e.accountStatus === "ACTIVE" ? "" : "badge--warn"}`}>{e.accountStatus}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* COMPETENCY FRAMEWORK */}
          {activeTab === "framework" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="panel" style={{ maxWidth: "600px" }}>
                <h2>Create New Skill</h2>
                <form onSubmit={handleCreateSkill} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Category</label>
                    <select
                      value={selectedCatId}
                      onChange={(e) => setSelectedCatId(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    >
                      {categories.map((c) => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Skill Name</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Kubernetes Cluster Management"
                      value={newSkillName}
                      onChange={(e) => setNewSkillName(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Description</label>
                    <textarea
                      rows={2}
                      placeholder="Definition and expected competence..."
                      value={newSkillDesc}
                      onChange={(e) => setNewSkillDesc(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <button type="submit" className="btn btn--primary">
                    <Icon name="plus" size={16} /> Add Skill to Framework
                  </button>
                </form>
              </div>

              <div className="panel">
                <h2>Catalogued Skills ({skills.length})</h2>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: "12px", marginTop: "14px" }}>
                  {skills.map((s) => (
                    <div key={s.id} style={{ padding: "14px", borderRadius: "10px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                      <span className="eyebrow">{s.categoryName}</span>
                      <strong style={{ display: "block", margin: "4px 0" }}>{s.name}</strong>
                      <p style={{ margin: 0, fontSize: "0.8rem", color: "#5b6d86" }}>{s.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* ROLE REQUIREMENTS */}
          {activeTab === "requirements" && (
            <div className="panel" style={{ maxWidth: "600px" }}>
              <h2>Configure Role Skill Requirement</h2>
              <p className="muted">Set the benchmark proficiency level required for each job role in the organization.</p>

              <form onSubmit={handleSetRequirement} style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "16px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Target Job Role</label>
                  <select
                    value={targetRole}
                    onChange={(e) => setTargetRole(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  >
                    <option value="Full Stack Developer">Full Stack Developer</option>
                    <option value="Backend Developer">Backend Developer</option>
                    <option value="Frontend Developer">Frontend Developer</option>
                    <option value="DevOps Engineer">DevOps Engineer</option>
                    <option value="Data Engineer">Data Engineer</option>
                  </select>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Skill</label>
                  <select
                    value={reqSkillId}
                    onChange={(e) => setReqSkillId(e.target.value)}
                    style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                  >
                    {skills.map((s) => (
                      <option key={s.id} value={s.id}>{s.name} ({s.categoryName})</option>
                    ))}
                  </select>
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Required Proficiency</label>
                    <select
                      value={reqProficiency}
                      onChange={(e) => setReqProficiency(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    >
                      <option value="Beginner">Beginner (Level 2)</option>
                      <option value="Intermediate">Intermediate (Level 3)</option>
                      <option value="Advanced">Advanced (Level 4)</option>
                      <option value="Expert">Expert (Level 5)</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Gap Priority Weight</label>
                    <select
                      value={reqPriority}
                      onChange={(e) => setReqPriority(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    >
                      <option value="HIGH">High Priority</option>
                      <option value="MEDIUM">Medium Priority</option>
                      <option value="LOW">Low Priority</option>
                    </select>
                  </div>
                </div>

                <button type="submit" className="btn btn--primary">
                  Save Role Requirement
                </button>
              </form>
            </div>
          )}

          {/* COURSES */}
          {activeTab === "courses" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="panel" style={{ maxWidth: "600px" }}>
                <h2>Add Course to Learning Catalogue</h2>
                <form onSubmit={handleCreateCourse} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Course Title</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Modern React & State Management"
                      value={courseTitle}
                      onChange={(e) => setCourseTitle(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Tagged Skill</label>
                    <select
                      value={courseSkillId}
                      onChange={(e) => setCourseSkillId(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    >
                      {skills.map((s) => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Level</label>
                    <select
                      value={courseLevel}
                      onChange={(e) => setCourseLevel(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    >
                      <option value="Beginner">Beginner</option>
                      <option value="Intermediate">Intermediate</option>
                      <option value="Advanced">Advanced</option>
                    </select>
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Course URL</label>
                    <input
                      type="text"
                      placeholder="https://..."
                      value={courseUrl}
                      onChange={(e) => setCourseUrl(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <button type="submit" className="btn btn--primary">
                    Publish Course
                  </button>
                </form>
              </div>

              <div className="panel">
                <h2>Published Courses ({courses.length})</h2>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "14px" }}>
                  {courses.map((c) => (
                    <div key={c.id} style={{ padding: "16px", borderRadius: "12px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                      <span className="badge">{c.skillName} • {c.level}</span>
                      <strong style={{ display: "block", margin: "8px 0 4px", fontSize: "1rem" }}>{c.title}</strong>
                      <p style={{ margin: 0, fontSize: "0.82rem", color: "#5b6d86" }}>{c.description}</p>
                      <small style={{ display: "block", marginTop: "8px", color: "#8795a9" }}>Duration: {c.durationHours} hrs</small>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TRAINING PROGRAMMES */}
          {activeTab === "training" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="panel" style={{ maxWidth: "600px" }}>
                <h2>Schedule Training Programme</h2>
                <form onSubmit={handleCreateTraining} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Programme Title</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Enterprise Cloud & Microservices Bootcamp"
                      value={progTitle}
                      onChange={(e) => setProgTitle(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                    <div>
                      <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Trainer Name</label>
                      <input
                        type="text"
                        placeholder="e.g. Dr. A. Sharma"
                        value={progTrainer}
                        onChange={(e) => setProgTrainer(e.target.value)}
                        style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                      />
                    </div>
                    <div>
                      <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Capacity</label>
                      <input
                        type="number"
                        value={progCapacity}
                        onChange={(e) => setProgCapacity(e.target.value)}
                        style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                      />
                    </div>
                  </div>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                    <div>
                      <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Start Date</label>
                      <input
                        type="date"
                        value={progStart}
                        onChange={(e) => setProgStart(e.target.value)}
                        style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                      />
                    </div>
                    <div>
                      <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>End Date</label>
                      <input
                        type="date"
                        value={progEnd}
                        onChange={(e) => setProgEnd(e.target.value)}
                        style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                      />
                    </div>
                  </div>
                  <button type="submit" className="btn btn--primary">
                    Announce Programme
                  </button>
                </form>
              </div>

              <div className="panel">
                <h2>Active & Upcoming Programmes ({training.length})</h2>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "14px" }}>
                  {training.map((t) => (
                    <div key={t.id} style={{ padding: "16px", borderRadius: "12px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                      <span className="badge">{t.status}</span>
                      <strong style={{ display: "block", margin: "8px 0 4px", fontSize: "1rem" }}>{t.title}</strong>
                      <p style={{ margin: 0, fontSize: "0.84rem", color: "#5b6d86" }}>
                        Trainer: {t.trainerName || "Staff"}
                      </p>
                      <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.8rem", color: "#8795a9", marginTop: "10px" }}>
                        <span>Seats: {t.enrolledCount} / {t.capacity}</span>
                        <span>Starts: {t.startDate || "TBD"}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* MENTORS */}
          {activeTab === "mentors" && (
            <AdminMentorship />
          )}

          {/* FEEDBACK */}
          {activeTab === "feedback" && (
            <div className="panel">
              <h2>Workforce Platform Feedback</h2>
              <div style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                {feedback.length === 0 ? (
                  <p className="muted">No feedback submitted yet.</p>
                ) : (
                  feedback.map((fb) => (
                    <div key={fb.id} style={{ padding: "14px 18px", borderRadius: "10px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                      <div style={{ display: "flex", justifyContent: "space-between" }}>
                        <strong>{fb.userName} • {fb.category}</strong>
                        <span className="badge">Rating: {fb.rating} / 5 ⭐</span>
                      </div>
                      <p style={{ margin: "6px 0 0", fontSize: "0.88rem", color: "#5b6d86" }}>{fb.comments}</p>
                      <small style={{ color: "#8795a9" }}>{new Date(fb.createdAt).toLocaleDateString()}</small>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* REPORTS */}
          {activeTab === "reports" && (
            <div className="panel">
              <div className="section-heading">
                <div>
                  <h2>Organization Analytics & CSV Exports</h2>
                  <p className="muted">Download comprehensive skill gap reports across all departments.</p>
                </div>
                <a
                  href={`${import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"}/api/reports/export/gaps.csv`}
                  className="btn btn--primary"
                  target="_blank"
                  rel="noreferrer"
                >
                  <Icon name="file" size={16} /> Download Full Gap Report (.CSV)
                </a>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
