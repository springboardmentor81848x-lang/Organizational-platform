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
import MentorshipHub from "./employee/MentorshipHub.jsx";
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
  Courses, Gaps, Mentorship, MyLearning, Certificates, MyProgress, Notifications, Overview, Profile, Reports, Results, slug,
} from "./employee/Pages.jsx";

const NAV_ITEMS = [
  { id: "overview", label: "Dashboard", icon: "dashboard" },
  { id: "profile", label: "My Profile Management", icon: "user" },
  { id: "skills", label: "My Skills & Evidence", icon: "clipboard" },
  { id: "assessment", label: "Skill Assessment", icon: "target" },
  { id: "gaps", label: "My Skill Gaps", icon: "chart" },
  { id: "recommendations", label: "Learning Recommendations", icon: "sparkles", tag: "AI" },
  { id: "learning", label: "My Learning", icon: "book" },
  { id: "mentors", label: "Mentors Directory", icon: "users" },
  { id: "mentorship-requests", label: "Mentorship Requests", icon: "clipboard" },
  { id: "chat", label: "Messages / Chatbox", icon: "message" },
  { id: "knowledge", label: "Knowledge Hub", icon: "book" },
  { id: "forum", label: "Discussion Forum", icon: "message" },
  { id: "progress", label: "My Progress", icon: "trending" },
  { id: "certificates", label: "Certificates", icon: "award" },
  { id: "notifications", label: "Notifications", icon: "bell" },
  { id: "support", label: "Help & Support", icon: "help" },
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
    department: "", jobRole: "Software Developer", experience: "Entry level", teamName: "", reportingManagerName: "", bio: "", skills: "",
  });
  const [profileSaved, setProfileSaved] = useState(false);
  const [profileSaveError, setProfileSaveError] = useState("");
  const [submissions, setSubmissions] = useState([]);
  const [recs, setRecs] = useState(saved.recs ? { status: "done", data: saved.recs, error: "" } : { status: "idle", data: null, error: "" });
  const [completed, setCompleted] = useState(saved.completed || []);
  const latestRequest = useRef(0);
  const inFlightRef = useRef(false);
  const abortControllerRef = useRef(null);
  const lastFetchKeyRef = useRef("");
  const profileRef = useRef(profile);
  profileRef.current = profile;
  const recsRef = useRef(recs);
  recsRef.current = recs;

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
      setAllSkills(skillsList);

      if (ongoing && ongoing.attemptId) {
        setActiveAttempt(ongoing);
        setRunningAssessmentId(ongoing.attemptId);
      }
    } catch (e) {
      console.warn("Failed to load backend employee dashboard data", e);
    }
  }, []);

  useEffect(() => {
    loadBackendData();
    getEmployeeProfile()
      .then((p) => {
        if (p) {
          setProfile((prev) => ({
            ...prev,
            fullName: p.fullName || prev.fullName,
            employeeId: p.employeeId || prev.employeeId,
            email: p.email || prev.email,
            department: p.department || prev.department,
            jobRole: p.jobRole || prev.jobRole,
            experience: p.experienceLevel || prev.experience,
            teamName: p.teamName || prev.teamName,
            reportingManagerName: p.reportingManagerName || prev.reportingManagerName,
            bio: p.bio || prev.bio,
            skills: p.skills || prev.skills,
          }));
        }
      })
      .catch(() => {});
  }, [loadBackendData]);

  // Sync state to local storage
  useEffect(() => {
    const toSave = { profile, recs: recs.data, completed };
    try {
      localStorage.setItem(storageKey(user.id), JSON.stringify(toSave));
    } catch {}
  }, [user.id, profile, recs.data, completed]);

  // Abort any pending recommendations request on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  // Request recommendations whenever assessment submission or server gaps change
  const fetchRecommendations = useCallback(async (sub, gaps, force = false) => {
    const currentProfile = profileRef.current;
    const skillResults = (gaps && gaps.length > 0)
      ? gaps.slice(0, 10).map((g) => ({
          skill: g.skillName,
          correct: Math.min(10, (g.currentLevel || 1) * 2),
          total: 10,
          level: g.currentLevel || 1,
          required: g.requiredLevel || 3,
        }))
      : (sub?.skillResults || [
          { skill: "Java", correct: 6, total: 10, level: 3, required: 4 },
          { skill: "SQL", correct: 5, total: 10, level: 2, required: 4 },
        ]);

    const payload = {
      jobRole: currentProfile.jobRole,
      experience: currentProfile.experience,
      department: currentProfile.department,
      overallScore: sub?.score ?? 75,
      skillResults,
    };

    const requestKey = JSON.stringify(payload);

    // Guard against duplicate requests in-flight or re-fetching identical data unless forced
    if (!force) {
      if (inFlightRef.current) return;
      if (lastFetchKeyRef.current === requestKey && recsRef.current.status === "done") return;
    }

    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    const controller = new AbortController();
    abortControllerRef.current = controller;
    inFlightRef.current = true;
    lastFetchKeyRef.current = requestKey;

    const id = ++latestRequest.current;
    setRecs((r) => ({ status: "loading", data: r.data, error: "" }));

    try {
      const data = await getAiRecommendations(payload, { signal: controller.signal });
      if (id === latestRequest.current) {
        setRecs({ status: "done", data, error: "" });
      }
    } catch (e) {
      if (controller.signal.aborted || e?.name === "CanceledError" || e?.name === "AbortError" || e?.code === "ERR_CANCELED") {
        return;
      }
      if (id === latestRequest.current) {
        setRecs({ status: "error", data: null, error: parseApiError(e).message });
      }
    } finally {
      if (id === latestRequest.current) {
        inFlightRef.current = false;
      }
    }
  }, []);

  useEffect(() => {
    if ((submissions.length > 0 || serverGaps.length > 0) && recs.status === "idle") {
      fetchRecommendations(submissions[0], serverGaps);
    }
  }, [submissions, serverGaps, recs.status, fetchRecommendations]);

  const retry = () => fetchRecommendations(submissions[0], serverGaps, true);

  const toggleCourse = (cSlug) => {
    setCompleted((prev) => (prev.includes(cSlug) ? prev.filter((s) => s !== cSlug) : [...prev, cSlug]));
  };

  const handleStartDynamicAssessment = async (overrideSkillId = null, overrideLevel = null, overrideAssessmentId = null) => {
    setStartError("");
    const targetSkillId = overrideSkillId || (selectedSkillId ? Number(selectedSkillId) : null);
    const targetLevel = overrideLevel || selectedLevel;

    if (!targetSkillId && !overrideAssessmentId) {
      setStartError("Please choose a skill or assessment to begin.");
      return;
    }

    setStartingAssessment(true);
    try {
      const attempt = await startAssessment({
        skillId: targetSkillId,
        proficiencyLevel: targetLevel,
        assessmentId: overrideAssessmentId,
      });
      setActiveAttempt(attempt);
      setRunningAssessmentId(attempt.attemptId);
      setActivePage("assessment");
    } catch (err) {
      setStartError(parseApiError(err).message);
    } finally {
      setStartingAssessment(false);
    }
  };

  const handleAssessmentComplete = (result) => {
    setRunningAssessmentId(null);
    setActiveAttempt(null);
    loadBackendData();
    fetchRecommendations(result, serverGaps, true);
    setActivePage("results");
  };

  const handleAssessmentCancel = () => {
    setRunningAssessmentId(null);
    setActiveAttempt(null);
    loadBackendData();
  };

  const saveProfile = async () => {
    setProfileSaveError("");
    try {
      const updated = await updateEmployeeProfile({
        jobRole: profile.jobRole,
        department: profile.department,
        experienceLevel: profile.experience,
        bio: profile.bio || null,
        skills: profile.skills || null,
      });
      if (updated) {
        setProfile((prev) => ({
          ...prev,
          department: updated.department || prev.department,
          jobRole: updated.jobRole || prev.jobRole,
          experience: updated.experienceLevel || prev.experience,
          teamName: updated.teamName || prev.teamName,
          reportingManagerName: updated.reportingManagerName || prev.reportingManagerName,
          bio: updated.bio || prev.bio,
          skills: updated.skills || prev.skills,
        }));
      }
      setProfileSaved(true);
      setTimeout(() => setProfileSaved(false), 4000);
      loadBackendData();
    } catch (e) {
      setProfileSaveError(parseApiError(e).message);
    }
  };

  const handleEnrollCourse = async (courseOrId) => {
    const courseId = typeof courseOrId === "object" ? courseOrId.id : courseOrId;
    if (!courseId) return;
    try {
      await enrollInCourse(courseId);
      setFeedbackMsg("Enrolled in course successfully! Added to My Learning.");
      loadBackendData();
      setTimeout(() => setFeedbackMsg(""), 4000);
    } catch (e) {
      setFeedbackMsg(parseApiError(e).message);
    }
  };

  const onSignOut = async () => { await signOut(); navigate("/login", { replace: true }); };
  const goTo = (id) => {
    setRunningAssessmentId(null);
    setActivePage(id);
    window.scrollTo({ top: 0 });
  };

  const latestSubmission = submissions.length > 0 ? submissions[0] : null;
  const shared = {
    user,
    profile,
    latestSubmission,
    submissions,
    serverGaps,
    recs,
    completed,
    toggleCourse,
    setActivePage: goTo,
    onRetry: retry,
  };

  // Resolve current active nav highlighting
  const activeNav = (activePage === "results") ? "assessment"
    : (activePage === "courses" ? "recommendations"
    : (activePage === "mentorship" ? "mentors" : activePage));

  const pageTitle = activePage === "results" ? "Assessment Results" : NAV_ITEMS.find((n) => n.id === activeNav)?.label || "Workspace";

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

          {/* Module 1: Dashboard */}
          {activePage === "overview" && <Overview {...shared} />}

          {/* Module 2: My Profile */}
          {activePage === "profile" && (
            <Profile profile={profile} setProfile={setProfile} onSave={saveProfile} saved={profileSaved} error={profileSaveError} />
          )}

          {/* Module 3: My Skills & Evidence */}
          {activePage === "skills" && (
            <SkillEvidenceSection />
          )}

          {/* Module 4: Skill Assessment */}
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
                        Every attempt generates questions powered by Google Gemini, tailored specifically to your target proficiency level.
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
                </div>
              )}
            </div>
          )}

          {activePage === "results" && (
            <Results
              {...shared}
              onEnroll={handleEnrollCourse}
              enrolledCourseIds={serverLearnings.map((l) => l.courseId)}
            />
          )}

          {/* Module 5: My Skill Gaps */}
          {activePage === "gaps" && <Gaps {...shared} />}

          {/* Module 6: Learning Recommendations */}
          {(activePage === "recommendations" || activePage === "courses") && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <Courses
                {...shared}
                onEnroll={handleEnrollCourse}
                enrolledCourseIds={serverLearnings.map((l) => l.courseId)}
              />

              {allCatalogueCourses.length > 0 && (
                <div className="panel">
                  <h2>Full Verified Course Catalogue</h2>
                  <p className="muted">Directly enroll in verified courses from NPTEL, SWAYAM, and Coursera covering all platform skills.</p>
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "14px", marginTop: "16px" }}>
                    {allCatalogueCourses.map((c) => {
                      const enrolled = serverLearnings.some((l) => l.courseId === c.id);
                      return (
                        <div key={c.id} style={{ padding: "16px", borderRadius: "12px", border: "1.5px solid #e2e9f3", background: "#f9fbfe", display: "flex", flexDirection: "column", gap: "8px" }}>
                          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                            <span className="badge" style={{ background: "#e0edff", color: "#1d4ed8", fontWeight: 700 }}>
                              {c.provider}
                            </span>
                            <span className="eyebrow">{c.skillName} • {c.level}</span>
                          </div>
                          <strong style={{ fontSize: "1rem" }}>{c.title}</strong>
                          <p style={{ margin: 0, fontSize: "0.82rem", color: "#5b6d86" }}>{c.description}</p>
                          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.78rem", color: "#8795a9", marginTop: "auto" }}>
                            <span>Duration: {c.durationHours} hrs</span>
                            {c.courseUrl ? (
                              <a href={c.courseUrl} target="_blank" rel="noopener noreferrer" style={{ color: "#2563eb" }}>
                                Provider Link <Icon name="external" size={13} />
                              </a>
                            ) : <span />}
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
                              Enroll in Course &rarr;
                            </button>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Module 7: My Learning */}
          {activePage === "learning" && (
            <MyLearning setActivePage={goTo} onCompleteRefresh={loadBackendData} />
          )}

          {/* Module 8: Mentors Directory */}
          {(activePage === "mentors" || activePage === "mentorship") && (
            <MentorshipHub initialTab="directory" />
          )}

          {/* Module 9: Mentorship Requests */}
          {activePage === "mentorship-requests" && (
            <MentorshipHub initialTab="requests" />
          )}

          {/* Module 10: Messages / Chatbox */}
          {activePage === "chat" && <ChatWidget currentUserId={user.id} />}

          {/* Module 11: Knowledge Hub */}
          {activePage === "knowledge" && <KnowledgeForumSection currentUserId={user.id} initialTab="articles" />}

          {/* Module 12: Discussion Forum */}
          {activePage === "forum" && <KnowledgeForumSection currentUserId={user.id} initialTab="forum" />}

          {/* Module 13: My Progress */}
          {activePage === "progress" && <MyProgress user={user} setActivePage={goTo} />}

          {/* Module 14: Certificates */}
          {activePage === "certificates" && <Certificates user={user} setActivePage={goTo} />}

          {/* Module 15: Notifications */}
          {activePage === "notifications" && <Notifications {...shared} />}

          {/* Module 16: Help & Support */}
          {activePage === "support" && <HelpSupportSection />}

          {/* Additional Workspace Features */}
          {activePage === "calendar" && <CalendarWidget currentUserId={user.id} />}
          {activePage === "reports" && <Reports {...shared} />}

          <p className="demo-disclaimer">
            Live Spring Boot API, PostgreSQL and Gemini AI Recommendations fully connected.
          </p>
        </div>
      </main>
    </div>
  );
}
