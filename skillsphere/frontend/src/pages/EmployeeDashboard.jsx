import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import Alert from "../components/Alert.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import ChatWidget from "../components/ChatWidget.jsx";
import CalendarWidget from "../components/CalendarWidget.jsx";
import AssessmentRunner from "../components/AssessmentRunner.jsx";
import SkillEvidenceSection from "./employee/SkillEvidenceSection.jsx";
import KnowledgeForumSection from "./employee/KnowledgeForumSection.jsx";
import HelpSupportSection from "./employee/HelpSupportSection.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import { getAiRecommendations, parseApiError } from "../services/authService.js";
import { getEmployeeProfile, updateEmployeeProfile } from "../services/mentorshipService.js";
import {
  getMySkillGaps,
  getAllAssessments,
  getMySubmissions,
  getMyCertificates,
  getMyLearning,
  getAllCourses,
  getAllSkills,
  getOngoingAttempt,
  startAssessment,
  enrollInCourse,
  updateCourseProgress,
} from "../services/platformService.js";
import {
  Courses, Gaps, Mentorship, Notifications, Overview, Profile, Progress, Reports, Results, slug,
} from "./employee/Pages.jsx";

const NAV_ITEMS = [
  { id: "overview", label: "Dashboard", icon: "dashboard" },
  { id: "profile", label: "My Profile Management", icon: "user" },
  { id: "skills", label: "My Skills & Evidence", icon: "clipboard" },
  { id: "assessment", label: "Skill Assessment", icon: "target" },
  { id: "gaps", label: "Knowledge Gap Summary", icon: "chart" },
  { id: "courses", label: "Course Recommendations", icon: "sparkles", tag: "AI" },
  { id: "progress", label: "Learning & Certificates", icon: "award" },
  { id: "mentorship", label: "Knowledge Sharing & Mentorship", icon: "users" },
  { id: "chat", label: "Messages / Chatbox", icon: "message" },
  { id: "knowledge", label: "Knowledge Hub & Forum", icon: "book" },
  { id: "calendar", label: "Schedule & Calendar", icon: "calendar" },
  { id: "notifications", label: "Notifications", icon: "bell" },
  { id: "reports", label: "Reports", icon: "file" },
  { id: "support", label: "Help & Feedback", icon: "help" },
];

const storageKey = (id) => `skillsphere.employee.${id}`;
function loadSaved(id) {
  try {
    const raw = JSON.parse(localStorage.getItem(storageKey(id))) || {};
    return {
      profile: raw.profile,
      recs: raw.recs,
      completed: raw.completed,
    };
  } catch {
    return {};
  }
}

export default function EmployeeDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const saved = useMemo(() => loadSaved(user.id), [user.id]);

  const [activePage, setActivePage] = useState("overview");
  const [profile, setProfile] = useState(saved.profile || {
    fullName: user.fullName || "", employeeId: user.employeeId || "", email: user.email || "",
    department: "", jobRole: "Software Developer", experience: "Entry level", bio: "", skills: "",
  });
  const [profileSaved, setProfileSaved] = useState(false);
  const [submissions, setSubmissions] = useState([]);
  const [recs, setRecs] = useState(saved.recs ? { status: "done", data: saved.recs, error: "" } : { status: "idle", data: null, error: "" });
  const [completed, setCompleted] = useState(saved.completed || []);
  const latestRequest = useRef(0);

  // Live backend states
  const [serverGaps, setServerGaps] = useState([]);
  const [serverAssessments, setServerAssessments] = useState([]);
  const [runningAssessmentId, setRunningAssessmentId] = useState(null);
  const [activeAttempt, setActiveAttempt] = useState(null);
  const [allSkills, setAllSkills] = useState([]);
  const [selectedSkillId, setSelectedSkillId] = useState("");
  const [selectedLevel, setSelectedLevel] = useState("Intermediate");
  const [startingAssessment, setStartingAssessment] = useState(false);
  const [startError, setStartError] = useState("");
  const [certificates, setCertificates] = useState([]);
  const [serverLearnings, setServerLearnings] = useState([]);
  const [allCatalogueCourses, setAllCatalogueCourses] = useState([]);
  const [feedbackMsg, setFeedbackMsg] = useState("");

  const loadBackendData = useCallback(async () => {
    try {
      const [gaps, asms, subs, certs, lrn, crs, skillsList, ongoing] = await Promise.all([
        getMySkillGaps().catch(() => []),
        getAllAssessments().catch(() => []),
        getMySubmissions().catch(() => []),
        getMyCertificates().catch(() => []),
        getMyLearning().catch(() => []),
        getAllCourses().catch(() => []),
        getAllSkills().catch(() => []),
        getOngoingAttempt().catch(() => null),
      ]);
      setServerGaps(gaps);
      setServerAssessments(asms);
      setSubmissions(subs);
      setCertificates(certs);
      setServerLearnings(lrn);
      setAllCatalogueCourses(crs);
      setAllSkills(skillsList || []);
      if (skillsList && skillsList.length > 0 && !selectedSkillId) {
        setSelectedSkillId(String(skillsList[0].id));
      }
      if (ongoing) {
        setActiveAttempt(ongoing);
        setRunningAssessmentId(ongoing.attemptId);
      }
    } catch {
      // Graceful local operation continues
    }
  }, [selectedSkillId]);

  useEffect(() => {
    loadBackendData();
  }, [loadBackendData]);

  // Sync profile from backend on mount
  useEffect(() => {
    getEmployeeProfile()
      .then((p) => {
        setProfile((prev) => ({
          ...prev,
          fullName: p.fullName || prev.fullName,
          email: p.email || prev.email,
          employeeId: p.employeeId || prev.employeeId,
          jobRole: p.jobRole || prev.jobRole,
          department: p.department || prev.department || "",
          experience: p.experienceLevel || prev.experience,
          bio: p.bio || prev.bio || "",
          skills: p.skills || prev.skills || "",
        }));
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    try {
      localStorage.setItem(storageKey(user.id), JSON.stringify({ profile, recs: recs.data, completed }));
    } catch {}
  }, [user.id, profile, recs.data, completed]);

  const requestRecommendationsFromServerResults = useCallback(async (score, gaps, currentProfile) => {
    if (!gaps || gaps.length === 0) return;
    const ticket = ++latestRequest.current;
    setRecs((r) => ({ ...r, status: "loading", error: "" }));
    try {
      const skillResults = gaps.map((g) => ({
        skill: g.skillName || g.skill || "Technical Skill",
        correct: Math.min(5, Math.max(0, g.currentLevel ?? 1)),
        total: 5,
        level: Math.min(5, Math.max(1, g.currentLevel ?? 1)),
        required: Math.min(5, Math.max(1, g.requiredLevel ?? 4)),
      }));
      const data = await getAiRecommendations({
        jobRole: currentProfile.jobRole || "Software Developer",
        experience: currentProfile.experience || "Entry level",
        department: currentProfile.department || "",
        overallScore: Math.min(100, Math.max(0, Math.round(score))),
        skillResults: skillResults.slice(0, 12),
      });
      if (ticket !== latestRequest.current) return;
      setRecs({ status: "done", data, error: "" });
      setCompleted([]);
    } catch (e) {
      if (ticket !== latestRequest.current) return;
      setRecs({ status: "error", error: parseApiError(e).message });
    }
  }, []);

  const handleAssessmentComplete = async (res) => {
    setActiveAttempt(null);
    setRunningAssessmentId(null);
    await loadBackendData();
    if (res && res.updatedGaps && res.updatedGaps.length > 0) {
      requestRecommendationsFromServerResults(res.score, res.updatedGaps, profile);
    }
  };

  const handleAssessmentCancel = () => {
    setActiveAttempt(null);
    setRunningAssessmentId(null);
    loadBackendData();
  };

  const handleStartDynamicAssessment = async (skillIdToStart = selectedSkillId, levelToStart = selectedLevel, assessmentIdToStart = null) => {
    setStartingAssessment(true);
    setStartError("");
    try {
      const attempt = await startAssessment({
        skillId: skillIdToStart ? Number(skillIdToStart) : null,
        assessmentId: assessmentIdToStart ? Number(assessmentIdToStart) : null,
        proficiencyLevel: levelToStart,
      });
      setActiveAttempt(attempt);
      setRunningAssessmentId(attempt.attemptId);
    } catch (err) {
      setStartError(parseApiError(err).message || "Failed to start assessment.");
    } finally {
      setStartingAssessment(false);
    }
  };

  const retry = () => {
    if (submissions.length > 0 && serverGaps.length > 0) {
      requestRecommendationsFromServerResults(submissions[0].score, serverGaps, profile);
    }
  };
  const toggleCourse = (id) => setCompleted((list) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id]));

  const saveProfile = async () => {
    try {
      await updateEmployeeProfile({
        jobRole: profile.jobRole || "Software Developer",
        department: profile.department || null,
        experienceLevel: profile.experience || "Entry level",
        bio: profile.bio || null,
        skills: profile.skills || null,
      });
    } catch {}
    setProfileSaved(true);
    setTimeout(() => setProfileSaved(false), 4000);
  };

  const handleEnrollCourse = async (courseId) => {
    try {
      await enrollInCourse(courseId);
      setFeedbackMsg("Enrolled in course successfully!");
      loadBackendData();
      setTimeout(() => setFeedbackMsg(""), 3000);
    } catch (e) {
      setFeedbackMsg(parseApiError(e).message);
    }
  };

  const handleUpdateProgress = async (courseId, pct) => {
    try {
      await updateCourseProgress(courseId, pct);
      setFeedbackMsg(pct >= 100 ? "Course completed! Certificate issued!" : "Progress updated.");
      loadBackendData();
      setTimeout(() => setFeedbackMsg(""), 4000);
    } catch (e) {
      setFeedbackMsg(parseApiError(e).message);
    }
  };

  const courseList = recs.data?.recommendations || [];
  const progress = courseList.length ? Math.round((courseList.filter((c) => completed.includes(slug(c.title))).length / courseList.length) * 100) : 0;
  const onSignOut = async () => { await signOut(); navigate("/login", { replace: true }); };
  const goTo = (id) => { setRunningAssessmentId(null); setActivePage(id); window.scrollTo({ top: 0 }); };

  const latestSubmission = submissions.length > 0 ? submissions[0] : null;
  const shared = {
    user,
    profile,
    latestSubmission,
    submissions,
    serverGaps,
    recs,
    completed,
    progress,
    toggleCourse,
    setActivePage: goTo,
    onRetry: retry,
  };
  const activeNav = activePage === "results" ? "assessment" : activePage;
  const pageTitle = activePage === "results" ? "Assessment Results" : NAV_ITEMS.find((n) => n.id === activePage)?.label;

  return (
    <div className="employee-app">
      <aside className="employee-sidebar">
        <div className="sidebar-brand">
          <Logo />
          <span>EMPLOYEE WORKSPACE</span>
        </div>
        <nav aria-label="Employee navigation">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${activeNav === item.id ? "is-active" : ""}`}
              aria-current={activeNav === item.id ? "page" : undefined}
              onClick={() => goTo(item.id)}
            >
              <Icon name={item.icon} size={18} />
              <span className="nav-label">{item.label}</span>
              {item.tag && <em className="nav-tag">{item.tag}</em>}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <span className="sidebar-avatar">{(user.fullName || "E").slice(0, 1).toUpperCase()}</span>
          <div>
            <strong>{user.fullName}</strong>
            <small>{user.email}</small>
          </div>
          <button className="sidebar-signout" onClick={onSignOut} aria-label="Sign out" title="Sign out">
            <Icon name="logout" size={18} />
          </button>
        </div>
      </aside>

      <main className="employee-main">
        <header className="employee-topbar">
          <div>
            <span className="mobile-brand">SkillSphere</span>
            <span className="topbar-title">{pageTitle}</span>
          </div>
          <div className="topbar-user">
            <span className="badge">Employee</span>
            <span className="topbar-name">{user.fullName}</span>
            <NotificationsPopover />
            <button className="btn btn--ghost" onClick={onSignOut}>Sign out</button>
          </div>
        </header>

        <div className="employee-content">
          {feedbackMsg && <Alert type="success">{feedbackMsg}</Alert>}

          {activePage === "overview" && <Overview {...shared} />}

          {activePage === "profile" && (
            <Profile profile={profile} setProfile={setProfile} onSave={saveProfile} saved={profileSaved} />
          )}

          {activePage === "skills" && (
            <SkillEvidenceSection />
          )}

          {activePage === "assessment" && (
            <div>
              {runningAssessmentId || activeAttempt ? (
                <AssessmentRunner
                  initialAttempt={activeAttempt}
                  attemptId={runningAssessmentId}
                  onComplete={handleAssessmentComplete}
                  onCancel={handleAssessmentCancel}
                />
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
                  {/* Dynamic AI Assessment Setup Panel */}
                  <div
                    className="panel"
                    style={{
                      border: "1.5px solid #c7d2fe",
                      background: "linear-gradient(180deg, #f8faff 0%, #ffffff 100%)",
                      borderRadius: "14px",
                    }}
                  >
                    <div style={{ marginBottom: "16px" }}>
                      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "4px" }}>
                        <span className="badge" style={{ background: "#4f46e5", color: "#fff", fontWeight: 700 }}>
                          ✨ AI-POWERED
                        </span>
                        <span className="eyebrow" style={{ margin: 0 }}>Dynamic Evaluation</span>
                      </div>
                      <h2 style={{ margin: "4px 0", fontSize: "1.3rem", color: "#0f2747" }}>
                        Start a New Skill Assessment
                      </h2>
                      <p className="muted" style={{ margin: 0, fontSize: "0.88rem" }}>
                        Every attempt generates a fresh set of questions powered by Google Gemini, tailored specifically to your target proficiency level.
                      </p>
                    </div>

                    {startError && <Alert type="error">{startError}</Alert>}

                    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px", marginTop: "12px" }}>
                      <div>
                        <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, color: "#0f2747", marginBottom: "6px" }}>
                          Select Skill to Assess
                        </label>
                        <select
                          className="input"
                          value={selectedSkillId}
                          onChange={(e) => setSelectedSkillId(e.target.value)}
                          style={{ width: "100%", padding: "10px 12px" }}
                        >
                          <option value="">-- Choose a Skill --</option>
                          {allSkills.map((sk) => (
                            <option key={sk.id} value={sk.id}>
                              {sk.name} ({sk.categoryName || "General"})
                            </option>
                          ))}
                        </select>
                      </div>

                      <div>
                        <label style={{ display: "block", fontSize: "0.85rem", fontWeight: 600, color: "#0f2747", marginBottom: "6px" }}>
                          Target Proficiency Level
                        </label>
                        <select
                          className="input"
                          value={selectedLevel}
                          onChange={(e) => setSelectedLevel(e.target.value)}
                          style={{ width: "100%", padding: "10px 12px" }}
                        >
                          <option value="Unaware">Unaware (Level 1)</option>
                          <option value="Beginner">Beginner (Level 2)</option>
                          <option value="Intermediate">Intermediate (Level 3)</option>
                          <option value="Advanced">Advanced (Level 4)</option>
                          <option value="Expert">Expert (Level 5)</option>
                        </select>
                      </div>
                    </div>

                    {/* Assessment Blueprint Specification */}
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
                        gap: "10px",
                        marginTop: "16px",
                        padding: "12px 14px",
                        background: "#f1f5f9",
                        borderRadius: "10px",
                        fontSize: "0.82rem",
                        color: "#334155",
                      }}
                    >
                      <div><strong>Length:</strong> 10 Questions</div>
                      <div><strong>Difficulty:</strong> 30% Easy, 40% Med, 30% Hard</div>
                      <div><strong>Pass Mark:</strong> 70%</div>
                      <div><strong>Scoring:</strong> Evaluated by Server</div>
                    </div>

                    <div style={{ marginTop: "18px", display: "flex", justifyContent: "flex-end" }}>
                      <button
                        type="button"
                        className="btn btn--primary"
                        disabled={startingAssessment || !selectedSkillId}
                        onClick={() => handleStartDynamicAssessment()}
                        style={{ minWidth: "180px" }}
                      >
                        {startingAssessment ? "Preparing your assessment..." : "Start Dynamic Assessment →"}
                      </button>
                    </div>
                  </div>

                  {/* Available Platform Skill Assessments */}
                  <div className="panel">
                    <h2>Available Platform Skill Assessments</h2>
                    <p className="muted">Take official tests to evaluate your competence and automatically calibrate skill gaps.</p>
                    {serverAssessments.length === 0 ? (
                      <p className="muted" style={{ marginTop: "12px" }}>No platform assessments are currently published.</p>
                    ) : (
                      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "16px" }}>
                        {serverAssessments.map((a) => {
                          const hasTaken = submissions.some((s) => s.assessmentId === a.id);
                          return (
                            <div key={a.id} style={{ padding: "16px", borderRadius: "12px", border: "1.5px solid #e2e9f3", background: "#f9fbfe", display: "flex", flexDirection: "column", gap: "8px" }}>
                              <span className="eyebrow">{a.categoryName} • {a.targetRole}</span>
                              <strong style={{ fontSize: "1.05rem" }}>{a.title}</strong>
                              <p style={{ margin: 0, fontSize: "0.82rem", color: "#5b6d86" }}>{a.description}</p>
                              <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem", color: "#8795a9", marginTop: "4px" }}>
                                <span>Pass mark: {a.passScore}%</span>
                                <span>{a.questionCount} questions</span>
                              </div>
                              <button
                                type="button"
                                className="btn btn--primary"
                                onClick={() => handleStartDynamicAssessment(null, "Intermediate", a.id)}
                                disabled={startingAssessment}
                                style={{ marginTop: "8px" }}
                              >
                                {hasTaken ? "Retake Assessment →" : "Start Assessment →"}
                              </button>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>

                  {/* Assessment History with Type Badge */}
                  <div className="panel">
                    <h2>My Assessment History ({submissions.length})</h2>
                    <p className="muted">Database-backed records of completed assessments and evaluated proficiency scores.</p>
                    {submissions.length === 0 ? (
                      <p className="muted" style={{ marginTop: "12px" }}>No assessments completed yet. Take an assessment above to verify your skills.</p>
                    ) : (
                      <div className="table-wrap" style={{ marginTop: "14px" }}>
                        <table>
                          <thead>
                            <tr>
                              <th>Assessment / Skill</th>
                              <th>Type</th>
                              <th>Date Completed</th>
                              <th>Score</th>
                              <th>Correct / Total</th>
                              <th>Status</th>
                              <th>Action</th>
                            </tr>
                          </thead>
                          <tbody>
                            {submissions.map((sub) => {
                              const isAi = sub.generationSource === "AI_GENERATED";
                              return (
                                <tr key={sub.submissionId}>
                                  <td><strong>{sub.assessmentTitle || `Assessment #${sub.assessmentId}`}</strong></td>
                                  <td>
                                    <span
                                      className="badge"
                                      style={{
                                        fontSize: "0.72rem",
                                        background: isAi ? "#eef2ff" : "#f1f5f9",
                                        color: isAi ? "#4f46e5" : "#475569",
                                        fontWeight: 600,
                                      }}
                                    >
                                      {isAi ? "✨ AI-Generated" : "Standard"}
                                    </span>
                                  </td>
                                  <td>{sub.completedAt ? new Date(sub.completedAt).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "—"}</td>
                                  <td><strong>{sub.score}%</strong></td>
                                  <td>{sub.correctAnswers} / {sub.totalQuestions}</td>
                                  <td>
                                    <span
                                      className="badge"
                                      style={{
                                        background: sub.passed ? "#e6f6ee" : "#fde9e7",
                                        color: sub.passed ? "#17754f" : "#b42318",
                                      }}
                                    >
                                      {sub.passed ? "PASSED" : "NOT PASSED"}
                                    </span>
                                  </td>
                                  <td>
                                    <button
                                      type="button"
                                      className="btn btn--ghost"
                                      style={{ fontSize: "0.8rem", padding: "4px 10px" }}
                                      onClick={() => handleStartDynamicAssessment(sub.skillId || null, sub.targetLevel || "Intermediate", sub.assessmentId || null)}
                                    >
                                      Retake
                                    </button>
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}

          {activePage === "results" && <Results {...shared} />}

          {activePage === "gaps" && <Gaps {...shared} />}

          {activePage === "courses" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              {allCatalogueCourses.length > 0 && (
                <div className="panel">
                  <h2>Organization Learning Catalogue</h2>
                  <p className="muted">Directly enroll in courses aligned with your identified skill gaps.</p>
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "16px" }}>
                    {allCatalogueCourses.map((c) => {
                      const enrolled = serverLearnings.some((l) => l.courseId === c.id);
                      return (
                        <div key={c.id} style={{ padding: "16px", borderRadius: "12px", border: "1.5px solid #e2e9f3", background: "#f9fbfe", display: "flex", flexDirection: "column", gap: "8px" }}>
                          <span className="badge">{c.skillName} • {c.level}</span>
                          <strong style={{ fontSize: "1rem" }}>{c.title}</strong>
                          <p style={{ margin: 0, fontSize: "0.82rem", color: "#5b6d86" }}>{c.description}</p>
                          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem", color: "#8795a9", marginTop: "auto" }}>
                            <span>Duration: {c.durationHours} hrs</span>
                            <span>{c.provider}</span>
                          </div>
                          {enrolled ? (
                            <span className="badge" style={{ background: "#e6f6ee", color: "#17754f", textAlign: "center", padding: "6px" }}>
                              Enrolled
                            </span>
                          ) : (
                            <button
                              type="button"
                              className="btn btn--primary"
                              onClick={() => handleEnrollCourse(c.id)}
                              style={{ marginTop: "6px" }}
                            >
                              Enroll in Course
                            </button>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
              <Courses {...shared} />
            </div>
          )}

          {activePage === "progress" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              {/* Official Certificates */}
              <div className="panel">
                <h2>Issued SkillSphere Certificates ({certificates.length})</h2>
                {certificates.length === 0 ? (
                  <p className="muted">Complete any course to 100% progress to automatically earn and verify an official certificate.</p>
                ) : (
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "14px", marginTop: "14px" }}>
                    {certificates.map((cert) => (
                      <div
                        key={cert.id}
                        style={{
                          padding: "20px",
                          borderRadius: "14px",
                          border: "2px solid #2563eb",
                          background: "linear-gradient(135deg, #ffffff 0%, #f4f8ff 100%)",
                          boxShadow: "0 4px 12px rgba(37,99,235,0.08)",
                          display: "flex",
                          flexDirection: "column",
                          gap: "8px",
                        }}
                      >
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                          <span className="badge" style={{ background: "#2563eb", color: "#fff" }}>CERTIFICATE OF COMPLETION</span>
                          <Icon name="award" size={24} />
                        </div>
                        <strong style={{ fontSize: "1.1rem", color: "#0f2747", marginTop: "6px" }}>{cert.title}</strong>
                        <div style={{ fontSize: "0.85rem", color: "#5b6d86" }}>
                          Verification Code: <strong style={{ color: "#2563eb", fontFamily: "monospace" }}>{cert.certificateCode}</strong>
                        </div>
                        <div style={{ fontSize: "0.78rem", color: "#8795a9", marginTop: "auto" }}>
                          Issued on {cert.issueDate} • Valid through {cert.expiryDate}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Active Enrolled Courses */}
              {serverLearnings.length > 0 && (
                <div className="panel">
                  <h2>Active Enrolled Courses & Progress</h2>
                  <div style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "14px" }}>
                    {serverLearnings.map((el) => (
                      <div key={el.id} style={{ padding: "16px", borderRadius: "12px", border: "1.5px solid #e2e9f3", background: "#f9fbfe", display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
                        <div>
                          <strong>{el.courseTitle}</strong>
                          <div style={{ fontSize: "0.84rem", color: "#5b6d86" }}>
                            Status: <span className="badge">{el.status}</span> • Progress: <strong>{el.progressPct}%</strong>
                          </div>
                        </div>
                        <div style={{ display: "flex", gap: "8px" }}>
                          <button
                            type="button"
                            className="btn btn--ghost"
                            onClick={() => handleUpdateProgress(el.courseId, Math.min(100, el.progressPct + 25))}
                          >
                            +25% Progress
                          </button>
                          <button
                            type="button"
                            className="btn btn--complete"
                            onClick={() => handleUpdateProgress(el.courseId, 100)}
                          >
                            Complete & Certify
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <Progress {...shared} />
            </div>
          )}

          {activePage === "mentorship" && <Mentorship />}

          {activePage === "chat" && <ChatWidget currentUserId={user.id} />}

          {activePage === "knowledge" && <KnowledgeForumSection currentUserId={user.id} />}

          {activePage === "calendar" && <CalendarWidget currentUserId={user.id} />}

          {activePage === "notifications" && <Notifications {...shared} />}

          {activePage === "reports" && <Reports {...shared} />}

          {activePage === "support" && <HelpSupportSection />}

          <p className="demo-disclaimer">
            Live Spring Boot API, PostgreSQL and Gemini AI Recommendations fully connected.
          </p>
        </div>
      </main>
    </div>
  );
}
