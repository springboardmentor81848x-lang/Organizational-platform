import { useCallback, useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import MentorshipHub from "./MentorshipHub.jsx";
import {
  getNotifications,
  getUnreadNotificationsCount,
  markAllNotificationsRead,
  markNotificationRead,
  getMyReport,
  getEmployeeDashboardOverview,
  changePassword,
  getMyLearning,
  updateCourseProgress,
  getMyCertificates,
  enrollInCourse,
  getMySubmissions,
} from "../../services/platformService.js";
import { parseApiError } from "../../services/apiClient.js";

const fmtDate = (iso) => (iso ? new Date(iso).toLocaleString([], { dateStyle: "medium", timeStyle: "short" }) : "");
export const slug = (t) => String(t).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
const priorityClass = (p) => `priority priority--${String(p).toLowerCase().replaceAll(" ", "-")}`;

function PageHeader({ eyebrow, title, subtitle, action }) {
  return <div className="page-header"><div>{eyebrow && <p className="eyebrow">{eyebrow}</p>}<h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div>{action}</div>;
}

function EmptyState({ icon = "clipboard", title, text, actionLabel, onAction }) {
  return <div className="empty-state"><span className="empty-state__icon"><Icon name={icon} size={28}/></span><h2>{title}</h2><p>{text}</p>{onAction && <button className="btn btn--primary" onClick={onAction}>{actionLabel} <Icon name="arrow" size={16}/></button>}</div>;
}

function AiBadge({ source }) {
  return source === "gemini"
    ? <span className="ai-badge"><Icon name="sparkles" size={14}/> Powered by Google Gemini</span>
    : <span className="ai-badge ai-badge--rules">Verified Catalogue Recommendations</span>;
}

function CourseSkeleton() {
  return <div className="ai-loading" role="status" aria-live="polite">
    <div className="ai-loading__head"><span className="ai-spinner" aria-hidden="true"/><div><strong>Gemini is analysing your results against verified course catalogue…</strong><small>Matching NPTEL, SWAYAM, and Coursera curriculum to your skill gaps. This takes a few seconds.</small></div></div>
    <div className="course-grid">{[0, 1, 2, 3].map((i) => <div className="course-card skeleton" key={i}><span className="sk sk--sm"/><span className="sk sk--lg"/><span className="sk"/><span className="sk sk--md"/></div>)}</div>
  </div>;
}

export function CourseCard({ course, onEnroll, isEnrolled }) {
  const providerLink = course.courseUrl || `https://www.google.com/search?q=${encodeURIComponent(course.searchQuery || course.title)}`;
  const providerClass = course.provider ? course.provider.toLowerCase().replace(/[^a-z0-9]/g, "") : "general";

  return <article className="course-card">
    <div className="course-card__top">
      <span className={priorityClass(course.priority === "Low" ? "on-track" : course.priority)}>{course.priority} priority</span>
      <span className="badge" style={{ background: "#e0edff", color: "#1d4ed8", fontWeight: 700 }}>{course.provider || "Catalogue"}</span>
    </div>
    <p className="eyebrow">{course.skill} • {course.level}</p>
    <h3>{course.title}</h3>
    <p className="course-reason">{course.reason}</p>
    {course.topics?.length > 0 && <ul className="chips">{course.topics.map((t) => <li key={t}>{t}</li>)}</ul>}
    <div className="course-meta">
      <span><Icon name="clock" size={15}/> {course.duration}</span>
      <span><Icon name="book" size={15}/> Level: {course.level}</span>
    </div>
    <div className="course-actions">
      <a className="btn btn--ghost" href={providerLink} target="_blank" rel="noopener noreferrer">
        View on {course.provider || "Web"} <Icon name="external" size={14}/>
      </a>
      {isEnrolled ? (
        <span className="badge" style={{ background: "#e6f6ee", color: "#17754f", padding: "8px 12px", display: "inline-flex", alignItems: "center", gap: "6px" }}>
          <Icon name="check" size={14} /> In My Learning
        </span>
      ) : (
        <button
          type="button"
          className="btn btn--primary"
          onClick={() => onEnroll && onEnroll(course)}
        >
          Enroll in Course &rarr;
        </button>
      )}
    </div>
  </article>;
}

function RecommendationBlock({ recs, onEnroll, enrolledCourseIds = [], onRetry, limit }) {
  if (recs.status === "loading") return <CourseSkeleton/>;
  if (recs.status === "error") return <div className="ai-error"><Icon name="alert" size={22}/><div><strong>We couldn't generate recommendations</strong><p>{recs.error}</p></div><button className="btn btn--primary" onClick={onRetry}><Icon name="refresh" size={16}/> Try again</button></div>;
  if (!recs.data) return null;
  const list = limit ? recs.data.recommendations.slice(0, limit) : recs.data.recommendations;
  return <>
    <div className="ai-summary"><span className="ai-summary__icon"><Icon name="lightbulb" size={22}/></span><div><AiBadge source={recs.data.source}/><p>{recs.data.summary}</p></div></div>
    {recs.data.notice && <Alert type="success">{recs.data.notice}</Alert>}
    <div className="course-grid">
      {list.map((c) => {
        const isEnrolled = enrolledCourseIds.includes(c.id);
        return <CourseCard key={c.id || slug(c.title)} course={c} isEnrolled={isEnrolled} onEnroll={onEnroll}/>;
      })}
    </div>
  </>;
}

/* ------------------------------------------------------------------ Overview */
export function Overview({ user, assessment, latestSubmission, serverGaps = [], recs, setActivePage }) {
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    getEmployeeDashboardOverview()
      .then((data) => {
        if (isMounted) setOverview(data);
      })
      .catch(() => {})
      .finally(() => {
        if (isMounted) setLoading(false);
      });
    return () => { isMounted = false; };
  }, []);

  const sub = latestSubmission || assessment;
  const gapCount = overview ? overview.activeGapsCount : (serverGaps.length > 0
    ? serverGaps.filter((s) => (s.gapSize ?? s.gap ?? 0) > 0).length
    : (sub?.skillResults ? sub.skillResults.filter((s) => s.gap > 0).length : null));
  const courseCount = recs.data?.recommendations?.length ?? 0;

  return <>
    <section className="hero">
      <div>
        <p className="eyebrow">SKILLSPHERE · EMPLOYEE WORKSPACE</p>
        <h1>Welcome back, {user.fullName?.split(" ")[0] || "there"}</h1>
        <p>Measure your skills with AI-powered assessments, uncover real knowledge gaps, and follow catalogue courses calibrated for your role.</p>
        <div style={{ display: "flex", gap: "10px", marginTop: "12px", flexWrap: "wrap" }}>
          <button className="btn btn--light" onClick={() => setActivePage("assessment")}>
            {sub ? "Take Assessment" : "Start First Assessment"} <Icon name="arrow" size={16}/>
          </button>
          <button className="btn btn--ghost" style={{ color: "#fff", borderColor: "rgba(255,255,255,0.4)" }} onClick={() => setActivePage("recommendations")}>
            View Course Recommendations
          </button>
        </div>
      </div>
      <div className="hero__art" aria-hidden="true"><Icon name="sparkles" size={56} strokeWidth={1.4}/></div>
    </section>

    {/* Metric Cards - Backed by PostgreSQL */}
    <div className="stat-grid">
      <div className="stat-card">
        <span className="stat-icon"><Icon name="clipboard"/></span>
        <span className="stat-label">Latest Assessment</span>
        <strong>{overview?.latestAssessmentScore !== null && overview?.latestAssessmentScore !== undefined ? `${overview.latestAssessmentScore}%` : (sub ? `${sub.score}%` : "—")}</strong>
        <small>{overview?.latestAssessmentSkill ? `${overview.latestAssessmentSkill} evaluated` : (sub ? "Evaluated by server" : "No assessment taken yet")}</small>
      </div>

      <div className="stat-card">
        <span className="stat-icon"><Icon name="target"/></span>
        <span className="stat-label">Active Skill Gaps</span>
        <strong style={{ color: (gapCount || 0) > 0 ? "#b42318" : "#17754f" }}>{gapCount !== null ? gapCount : "—"}</strong>
        <small>{gapCount === 0 ? "All skills on track!" : "Skills below target proficiency"}</small>
      </div>

      <div className="stat-card">
        <span className="stat-icon"><Icon name="book"/></span>
        <span className="stat-label">Active Learning</span>
        <strong>{overview ? overview.activeLearningsCount : "0"}</strong>
        <small>{overview ? `${overview.completedCoursesCount} completed courses` : "Courses in progress"}</small>
      </div>

      <div className="stat-card">
        <span className="stat-icon"><Icon name="award"/></span>
        <span className="stat-label">Certificates Earned</span>
        <strong style={{ color: "#1d4ed8" }}>{overview ? overview.certificatesCount : "0"}</strong>
        <small>Official verified credentials</small>
      </div>
    </div>

    {/* Two-Column Overview Content */}
    <div className="two-col" style={{ marginTop: "24px" }}>
      {/* Skill Gaps Snapshot */}
      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">SKILL BENCHMARK</p>
            <h2>Proficiency vs Target</h2>
          </div>
          <button className="text-action" onClick={() => setActivePage("gaps")}>
            Full Gap Summary <Icon name="arrow" size={14}/>
          </button>
        </div>
        {serverGaps.length > 0 ? (
          <SkillBars results={serverGaps}/>
        ) : (
          <p className="muted" style={{ padding: "12px 0" }}>
            No skill gaps logged yet. Take a skill assessment to evaluate your technical competencies!
          </p>
        )}
      </section>

      {/* Recent PostgreSQL Activities */}
      <section className="panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">ACTIVITY STREAM</p>
            <h2>Recent Milestones</h2>
          </div>
          <button className="text-action" onClick={() => setActivePage("progress")}>
            View Progress <Icon name="arrow" size={14}/>
          </button>
        </div>
        {overview?.recentActivities?.length > 0 ? (
          <div style={{ display: "flex", flexDirection: "column", gap: "10px", marginTop: "10px" }}>
            {overview.recentActivities.map((act, idx) => (
              <div key={idx} style={{ display: "flex", alignItems: "flex-start", gap: "12px", padding: "10px", borderRadius: "8px", background: "#f8faff", border: "1px solid #eef2f6" }}>
                <span style={{ padding: "6px", borderRadius: "6px", background: act.type === "CERTIFICATE" ? "#e0edff" : (act.type === "ASSESSMENT" ? "#eff6ff" : "#e6f6ee"), color: act.type === "CERTIFICATE" ? "#1d4ed8" : (act.type === "ASSESSMENT" ? "#2563eb" : "#17754f") }}>
                  <Icon name={act.type === "CERTIFICATE" ? "award" : (act.type === "ASSESSMENT" ? "clipboard" : "book")} size={16}/>
                </span>
                <div style={{ flex: 1 }}>
                  <strong style={{ fontSize: "0.92rem", display: "block" }}>{act.title}</strong>
                  <small style={{ color: "#64748b" }}>{act.description}</small>
                </div>
                <small style={{ color: "#94a3b8", fontSize: "0.75rem" }}>{act.timestamp ? new Date(act.timestamp).toLocaleDateString() : ""}</small>
              </div>
            ))}
          </div>
        ) : (
          <div style={{ padding: "16px 0", color: "#64748b", fontSize: "0.88rem" }}>
            No recent activity recorded. Take an assessment or enroll in a recommended course to see your progress here!
          </div>
        )}
      </section>
    </div>
  </>;
}

/* -------------------------------------------------------------------- Profile (+ Change Password) */
export function Profile({ profile, setProfile, onSave, saved }) {
  const set = (k) => (e) => setProfile({ ...profile, [k]: e.target.value });

  // Change Password State
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [pwSubmitting, setPwSubmitting] = useState(false);
  const [pwError, setPwError] = useState("");
  const [pwSuccess, setPwSuccess] = useState("");

  // Live password validation
  const hasMinLength = newPassword.length >= 8;
  const hasLetter = /[A-Za-z]/.test(newPassword);
  const hasNumber = /\d/.test(newPassword);
  const hasSpecial = /[@$!%*#?&^_-]/.test(newPassword);
  const passwordsMatch = newPassword.length > 0 && newPassword === confirmPassword;
  const isPasswordValid = hasMinLength && hasLetter && hasNumber && hasSpecial && passwordsMatch;

  const handleChangePassword = async (e) => {
    e.preventDefault();
    if (!isPasswordValid || !currentPassword) return;
    setPwSubmitting(true);
    setPwError("");
    setPwSuccess("");
    try {
      await changePassword({
        currentPassword,
        newPassword,
        confirmPassword,
      });
      setPwSuccess("Password changed successfully! Your account security is up to date.");
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      setPwError(parseApiError(err).message);
    } finally {
      setPwSubmitting(false);
    }
  };

  return <>
    <PageHeader eyebrow="ACCOUNT SETTINGS" title="My Profile Management" subtitle="Manage your profile information, job role preferences, and account security."/>

    {/* Profile Details Panel */}
    <section className="panel" style={{ marginBottom: "24px" }}>
      <h2>Personal & Professional Details</h2>
      {saved && <Alert type="success">Profile saved successfully to your SkillSphere account.</Alert>}
      <div className="profile-grid" style={{ marginTop: "14px" }}>
        <label className="field"><span>Full Name</span><input value={profile.fullName || ""} onChange={set("fullName")}/></label>
        <label className="field"><span>Employee ID</span><input value={profile.employeeId || ""} placeholder="Enter employee ID" onChange={set("employeeId")}/></label>
        <label className="field"><span>Email Address</span><input value={profile.email || ""} readOnly/></label>
        <label className="field"><span>Department</span><input value={profile.department || ""} placeholder="e.g. Engineering" maxLength={80} onChange={set("department")}/></label>
        <label className="field"><span>Job Role</span><select value={profile.jobRole || "Software Developer"} onChange={set("jobRole")}><option>Software Developer</option><option>Data Analyst</option><option>QA Engineer</option><option>Support Engineer</option></select></label>
        <label className="field"><span>Experience Level</span><select value={profile.experience || "Entry level"} onChange={set("experience")}><option>Entry level</option><option>Intermediate</option><option>Experienced</option></select></label>
      </div>
      <div style={{ marginTop: "1rem", display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
        <label className="field">
          <span>Technical Skills (comma-separated)</span>
          <input value={profile.skills || ""} placeholder="e.g. Java, Spring Boot, SQL, Docker, React" onChange={set("skills")} />
        </label>
        <label className="field">
          <span>Professional Bio / Growth Focus</span>
          <input value={profile.bio || ""} placeholder="Brief statement about your career or mentoring focus" onChange={set("bio")} />
        </label>
      </div>
      <button className="btn btn--primary" style={{ marginTop: "1rem" }} onClick={onSave}>Save Profile</button>
    </section>

    {/* Change Password Panel */}
    <section className="panel" style={{ maxWidth: "600px" }}>
      <div className="section-heading">
        <div>
          <h2>Change Password</h2>
          <p className="muted">Ensure your account uses a secure password with letters, numbers, and special symbols.</p>
        </div>
      </div>

      {pwError && <Alert type="error">{pwError}</Alert>}
      {pwSuccess && <Alert type="success">{pwSuccess}</Alert>}

      <form onSubmit={handleChangePassword} style={{ display: "flex", flexDirection: "column", gap: "14px", marginTop: "12px" }}>
        <label className="field">
          <span>Current Password</span>
          <input
            type="password"
            required
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder="Enter your current password"
          />
        </label>

        <label className="field">
          <span>New Password</span>
          <input
            type="password"
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="Min 8 characters, letter, number, symbol"
          />
        </label>

        <label className="field">
          <span>Confirm New Password</span>
          <input
            type="password"
            required
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Re-enter new password"
          />
        </label>

        {/* Password Strength Checklist */}
        <div style={{ background: "#f8faff", padding: "12px", borderRadius: "8px", border: "1px solid #eef2f6", fontSize: "0.82rem", display: "grid", gridTemplateColumns: "1fr 1fr", gap: "8px" }}>
          <span style={{ color: hasMinLength ? "#17754f" : "#64748b" }}>
            {hasMinLength ? "✓" : "○"} At least 8 characters
          </span>
          <span style={{ color: hasLetter ? "#17754f" : "#64748b" }}>
            {hasLetter ? "✓" : "○"} At least one letter
          </span>
          <span style={{ color: hasNumber ? "#17754f" : "#64748b" }}>
            {hasNumber ? "✓" : "○"} At least one number
          </span>
          <span style={{ color: hasSpecial ? "#17754f" : "#64748b" }}>
            {hasSpecial ? "✓" : "○"} At least one symbol (@$!%*#?&^_-)
          </span>
          <span style={{ color: passwordsMatch ? "#17754f" : "#64748b", gridColumn: "span 2" }}>
            {passwordsMatch ? "✓ Passwords match" : "○ Passwords must match"}
          </span>
        </div>

        <button
          type="submit"
          className="btn btn--primary"
          disabled={!isPasswordValid || !currentPassword || pwSubmitting}
          style={{ alignSelf: "flex-start", marginTop: "4px" }}
        >
          {pwSubmitting ? "Updating Password…" : "Update Password"}
        </button>
      </form>
    </section>
  </>;
}

/* -------------------------------------------------------------------- Results */
export function SkillBars({ results = [] }) {
  return <div className="skill-bars">{results.map((s) => {
    const skillName = s.skillName || s.skill || "Skill";
    const current = s.level ?? s.currentLevel ?? 0;
    const required = s.required ?? s.requiredLevel ?? 4;
    const gap = s.gapSize ?? s.gap ?? Math.max(0, required - current);
    const isHigh = gap >= 2 || s.priority === "HIGH";
    const isMedium = gap === 1 || s.priority === "MEDIUM";
    return (
      <div className="skill-bar" key={skillName}>
        <div className="skill-bar__head"><strong>{skillName}</strong><span>{current}/5 <small>· target {required}</small></span></div>
        <div className="skill-bar__track"><span className={`skill-bar__fill ${isHigh ? "is-high" : isMedium ? "is-ok" : "is-ok"}`} style={{ width: `${(current / 5) * 100}%` }}/><i style={{ left: `${(required / 5) * 100}%` }} title={`Target ${required}/5`}/></div>
      </div>
    );
  })}</div>;
}

export function Results({ assessment, latestSubmission, serverGaps = [], recs, onEnroll, enrolledCourseIds, onRetry, setActivePage }) {
  const sub = latestSubmission || assessment;
  if (!sub) {
    return (
      <>
        <PageHeader eyebrow="STEP 2 OF 2" title="Assessment Results" />
        <section className="panel">
          <EmptyState
            icon="clipboard"
            title="No assessment completed yet"
            text="Take an official platform assessment to view your evaluated score and calibrated skill gaps."
            actionLabel="Go to Assessments"
            onAction={() => setActivePage("assessment")}
          />
        </section>
      </>
    );
  }
  const score = sub.score ?? 0;
  const correct = sub.correctAnswers ?? sub.correct ?? 0;
  const total = sub.totalQuestions ?? sub.total ?? 0;
  const completedAt = sub.completedAt;

  return <>
    <PageHeader eyebrow="STEP 2 OF 2" title="Assessment Results" subtitle={completedAt ? `Completed ${fmtDate(completedAt)}` : ""}/>
    <section className="panel">
      <div className="result-banner">
        <span className="result-score" style={{ "--p": `${score}%` }}><b>{score}%</b></span>
        <div>
          <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "6px" }}>
            <h2 style={{ margin: 0 }}>You answered {correct} of {total} correctly</h2>
            {sub.passed !== undefined && (
              <span className="badge" style={{ background: sub.passed ? "#e6f6ee" : "#fde9e7", color: sub.passed ? "#17754f" : "#b42318" }}>
                {sub.passed ? "Passed" : "Not passed"}
              </span>
            )}
          </div>
          <p>{sub.assessmentTitle ? `Assessment: ${sub.assessmentTitle}. ` : ""}The comparison below demonstrates your evaluated proficiency against role targets.</p>
        </div>
      </div>
      <SkillBars results={serverGaps.length > 0 ? serverGaps : (sub.skillResults || [])}/>
    </section>
    <section className="panel">
      <div className="section-heading">
        <div><p className="eyebrow">AI RECOMMENDATIONS</p><h2>Courses picked for you</h2></div>
        {recs.status === "done" && <button className="text-action" onClick={() => setActivePage("recommendations")}>Open learning paths <Icon name="arrow" size={14}/></button>}
      </div>
      <RecommendationBlock recs={recs} onEnroll={onEnroll} enrolledCourseIds={enrolledCourseIds} onRetry={onRetry} limit={3}/>
    </section>
  </>;
}

/* ----------------------------------------------------------------------- Gaps */
export function Gaps({ assessment, serverGaps = [], setActivePage }) {
  const gaps = serverGaps.length > 0 ? serverGaps : (assessment?.skillResults || []);
  if (!gaps.length) {
    return (
      <>
        <PageHeader eyebrow="ANALYSIS" title="My Skill Gaps"/>
        <section className="panel">
          <EmptyState
            icon="target"
            title="No skill gaps calculated yet"
            text="Complete an assessment and SkillSphere will compare your proficiency with the role requirements for each skill."
            actionLabel="Take assessment"
            onAction={() => setActivePage("assessment")}
          />
        </section>
      </>
    );
  }
  return <>
    <PageHeader eyebrow="ANALYSIS" title="My Skill Gaps" subtitle="Your evaluated proficiency compared with the required benchmark for your role."/>
    <section className="panel">
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Skill</th>
              <th>Category</th>
              <th>Current Level</th>
              <th>Target Level</th>
              <th>Gap</th>
              <th>Priority</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {gaps.map((s) => {
              const skillName = s.skillName || s.skill;
              const category = s.categoryName || "Core";
              const current = s.currentProficiency ? `${s.currentProficiency} (L${s.currentLevel})` : `${s.level}/5`;
              const target = s.requiredProficiency ? `${s.requiredProficiency} (L${s.requiredLevel})` : `${s.required}/5`;
              const gap = s.gapSize ?? s.gap ?? 0;
              const priority = s.priority || (gap >= 2 ? "HIGH" : gap === 1 ? "MEDIUM" : "ON_TRACK");
              return (
                <tr key={skillName}>
                  <td><strong>{skillName}</strong></td>
                  <td>{category}</td>
                  <td>{current}</td>
                  <td>{target}</td>
                  <td><strong style={{ color: gap > 0 ? "#b42318" : "#17754f" }}>{gap > 0 ? `-${gap}` : "0"}</strong></td>
                  <td>
                    <span className={priorityClass(priority === "ON_TRACK" ? "on-track" : priority)}>
                      {priority}
                    </span>
                  </td>
                  <td>
                    {gap > 0 ? (
                      <button
                        type="button"
                        className="btn btn--primary"
                        onClick={() => setActivePage("recommendations")}
                        style={{ fontSize: "0.78rem", padding: "4px 10px" }}
                      >
                        Bridge Gap &rarr;
                      </button>
                    ) : (
                      <span className="badge" style={{ background: "#e6f6ee", color: "#17754f" }}>Mastered</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <div style={{ marginTop: "16px", display: "flex", gap: "10px" }}>
        <button className="btn btn--ghost" onClick={() => setActivePage("assessment")}>Take Another Assessment</button>
        <button className="btn btn--primary" onClick={() => setActivePage("recommendations")}>View Recommended Courses</button>
      </div>
    </section>
  </>;
}

/* -------------------------------------------------------------------- Recommendations */
export function Courses({ assessment, latestSubmission, recs, onEnroll, enrolledCourseIds, onRetry, setActivePage }) {
  const sub = latestSubmission || assessment;
  const action = sub && recs.status !== "loading" && <button className="btn btn--ghost" onClick={onRetry}><Icon name="refresh" size={16}/> Regenerate</button>;
  return <>
    <PageHeader eyebrow="AI-POWERED" title="Learning Recommendations" subtitle="Courses chosen from verified catalogues (NPTEL, SWAYAM, Coursera) to resolve your specific skill gaps." action={action}/>
    {!sub ? (
      <section className="panel">
        <EmptyState
          icon="sparkles"
          title="Take an assessment to unlock recommendations"
          text="Gemini needs your assessment result to match verified curriculum courses to your exact gaps."
          actionLabel="Start assessment"
          onAction={() => setActivePage("assessment")}
        />
      </section>
    ) : (
      <section className="panel">
        <RecommendationBlock recs={recs} onEnroll={onEnroll} enrolledCourseIds={enrolledCourseIds} onRetry={onRetry}/>
      </section>
    )}
  </>;
}

/* ------------------------------------------------------------------- My Learning */
export function MyLearning({ setActivePage, onCompleteRefresh }) {
  const [learnings, setLearnings] = useState([]);
  const [filter, setFilter] = useState("ALL"); // ALL, IN_PROGRESS, COMPLETED
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await getMyLearning();
      setLearnings(data);
    } catch (e) {
      setError(parseApiError(e).message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleUpdate = async (courseId, newPct) => {
    setError(""); setSuccess("");
    try {
      await updateCourseProgress(courseId, newPct);
      if (newPct >= 100) {
        setSuccess("🎉 Course 100% completed! An official verified Certificate of Completion has been issued to your profile.");
      } else {
        setSuccess(`Course progress updated to ${newPct}%.`);
      }
      loadData();
      if (onCompleteRefresh) onCompleteRefresh();
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  const filtered = learnings.filter((l) => {
    if (filter === "IN_PROGRESS") return l.progressPct < 100;
    if (filter === "COMPLETED") return l.progressPct >= 100;
    return true;
  });

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <PageHeader
        eyebrow="UPSKILLING"
        title="My Learning"
        subtitle="Manage enrolled courses, track real progress, and certify technical competencies."
        action={
          <button className="btn btn--primary" onClick={() => setActivePage("recommendations")}>
            Browse Recommendations &rarr;
          </button>
        }
      />

      {error && <Alert type="error">{error}</Alert>}
      {success && <Alert type="success">{success}</Alert>}

      {/* Filter Tabs */}
      <div className="tabs" role="tablist">
        <button className={`tab-btn ${filter === "ALL" ? "is-active" : ""}`} onClick={() => setFilter("ALL")}>
          All Courses ({learnings.length})
        </button>
        <button className={`tab-btn ${filter === "IN_PROGRESS" ? "is-active" : ""}`} onClick={() => setFilter("IN_PROGRESS")}>
          In Progress ({learnings.filter((l) => l.progressPct < 100).length})
        </button>
        <button className={`tab-btn ${filter === "COMPLETED" ? "is-active" : ""}`} onClick={() => setFilter("COMPLETED")}>
          Completed ({learnings.filter((l) => l.progressPct >= 100).length})
        </button>
      </div>

      <section className="panel">
        {loading ? (
          <p className="muted" style={{ padding: "2rem", textAlign: "center" }}>Loading your enrolled courses from database…</p>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon="book"
            title={filter === "COMPLETED" ? "No completed courses yet" : "No courses currently enrolled"}
            text="Explore the learning recommendations or course catalogue to enroll in a course."
            actionLabel="View Recommendations"
            onAction={() => setActivePage("recommendations")}
          />
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            {filtered.map((l) => (
              <div
                key={l.id}
                style={{
                  padding: "18px",
                  borderRadius: "12px",
                  border: "1.5px solid #e2e9f3",
                  background: l.progressPct >= 100 ? "#f9fcfb" : "#ffffff",
                  display: "flex",
                  flexDirection: "column",
                  gap: "12px",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "10px" }}>
                  <div>
                    <span className="badge" style={{ background: "#e0edff", color: "#1d4ed8", fontWeight: 700, marginRight: "8px" }}>
                      {l.provider || "Course"}
                    </span>
                    <span className="eyebrow">{l.skillName} • {l.level}</span>
                    <h3 style={{ margin: "4px 0 0 0", fontSize: "1.15rem" }}>{l.courseTitle}</h3>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <span className={`badge ${l.progressPct >= 100 ? "badge--success" : "badge--warn"}`}>
                      {l.progressPct >= 100 ? "COMPLETED" : `${l.progressPct}% IN PROGRESS`}
                    </span>
                    <small style={{ display: "block", color: "#8795a9", marginTop: "4px" }}>
                      Enrolled: {fmtDate(l.enrolledAt)}
                    </small>
                  </div>
                </div>

                {/* Progress bar */}
                <div style={{ background: "#e2e8f0", height: "8px", borderRadius: "4px", overflow: "hidden" }}>
                  <div
                    style={{
                      width: `${l.progressPct}%`,
                      height: "100%",
                      background: l.progressPct >= 100 ? "#10b981" : "#2563eb",
                      transition: "width 0.4s ease",
                    }}
                  />
                </div>

                {/* Action controls */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "10px" }}>
                  {l.courseUrl ? (
                    <a className="btn btn--ghost" href={l.courseUrl} target="_blank" rel="noopener noreferrer" style={{ fontSize: "0.85rem" }}>
                      Launch Course on Provider <Icon name="external" size={14} />
                    </a>
                  ) : <span />}

                  <div style={{ display: "flex", gap: "8px" }}>
                    {l.progressPct < 100 && (
                      <>
                        <button
                          type="button"
                          className="btn btn--ghost"
                          onClick={() => handleUpdate(l.courseId, Math.min(100, l.progressPct + 25))}
                          style={{ fontSize: "0.82rem", padding: "6px 12px" }}
                        >
                          +25% Progress
                        </button>
                        <button
                          type="button"
                          className="btn btn--complete"
                          onClick={() => handleUpdate(l.courseId, 100)}
                          style={{ fontSize: "0.82rem", padding: "6px 14px" }}
                        >
                          Complete & Certify &rarr;
                        </button>
                      </>
                    )}
                    {l.progressPct >= 100 && (
                      <button
                        type="button"
                        className="btn btn--ghost"
                        onClick={() => setActivePage("certificates")}
                        style={{ fontSize: "0.82rem", color: "#17754f" }}
                      >
                        <Icon name="award" size={15} /> View Certificate
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

/* ----------------------------------------------------------------- Certificates */
export function Certificates({ user, setActivePage }) {
  const [certs, setCerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedCert, setSelectedCert] = useState(null);

  useEffect(() => {
    let isMounted = true;
    getMyCertificates()
      .then((data) => {
        if (isMounted) setCerts(data);
      })
      .catch((e) => {
        if (isMounted) setError(parseApiError(e).message);
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });
    return () => { isMounted = false; };
  }, []);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <PageHeader
        eyebrow="CREDENTIALS"
        title="Certificates"
        subtitle="Verified SkillSphere certificates earned upon achieving 100% completion in curriculum courses."
      />

      {error && <Alert type="error">{error}</Alert>}

      <section className="panel">
        {loading ? (
          <p className="muted" style={{ padding: "2rem", textAlign: "center" }}>Loading your credentials from database…</p>
        ) : certs.length === 0 ? (
          <EmptyState
            icon="award"
            title="No certificates earned yet"
            text="Complete your enrolled courses to 100% progress and SkillSphere will automatically issue your verified certificate."
            actionLabel="Go to My Learning"
            onAction={() => setActivePage("learning")}
          />
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(320px, 1fr))", gap: "16px" }}>
            {certs.map((c) => (
              <div
                key={c.id}
                style={{
                  padding: "20px",
                  borderRadius: "14px",
                  border: "2px solid #2563eb",
                  background: "linear-gradient(135deg, #ffffff 0%, #f4f8ff 100%)",
                  boxShadow: "0 4px 12px rgba(37,99,235,0.08)",
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span className="badge" style={{ background: "#2563eb", color: "#fff", fontWeight: 700 }}>
                    CERTIFICATE OF COMPLETION
                  </span>
                  <Icon name="award" size={26} />
                </div>
                <strong style={{ fontSize: "1.12rem", color: "#0f2747", marginTop: "4px" }}>{c.title}</strong>
                <div style={{ fontSize: "0.86rem", color: "#5b6d86" }}>
                  Verification Code: <strong style={{ color: "#2563eb", fontFamily: "monospace" }}>{c.certificateCode}</strong>
                </div>
                <div style={{ fontSize: "0.8rem", color: "#8795a9" }}>
                  Issued on: {c.issueDate} {c.expiryDate ? `• Valid through: ${c.expiryDate}` : ""}
                </div>
                <div style={{ marginTop: "auto", paddingTop: "10px", borderTop: "1px solid #e2e9f3" }}>
                  <button
                    type="button"
                    className="btn btn--primary"
                    style={{ width: "100%", justifyContent: "center" }}
                    onClick={() => setSelectedCert(c)}
                  >
                    View / Print Certificate
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {/* Official Certificate Modal */}
      {selectedCert && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(11,37,69,0.55)",
            backdropFilter: "blur(4px)",
            display: "grid",
            placeItems: "center",
            zIndex: 999,
            padding: "20px",
          }}
        >
          <div
            className="panel"
            style={{
              width: "100%",
              maxWidth: "680px",
              padding: "40px",
              border: "6px double #2563eb",
              borderRadius: "16px",
              background: "#ffffff",
              textAlign: "center",
              position: "relative",
            }}
          >
            <button
              type="button"
              className="btn btn--ghost"
              onClick={() => setSelectedCert(null)}
              style={{ position: "absolute", top: "16px", right: "16px", padding: "4px 8px" }}
            >
              <Icon name="x" size={20} />
            </button>

            <div style={{ marginBottom: "20px" }}>
              <span style={{ fontSize: "0.85rem", letterSpacing: "2px", color: "#64748b", textTransform: "uppercase", fontWeight: 700 }}>
                SkillSphere Knowledge Platform
              </span>
              <h1 style={{ fontSize: "1.8rem", color: "#0f2747", margin: "10px 0" }}>
                CERTIFICATE OF COMPLETION
              </h1>
              <p style={{ color: "#64748b", margin: 0 }}>This is to certify that</p>
            </div>

            <h2 style={{ fontSize: "1.6rem", color: "#1d4ed8", textDecoration: "underline", margin: "14px 0" }}>
              {user.fullName || "Valued Employee"}
            </h2>

            <p style={{ color: "#334155", maxWidth: "480px", margin: "0 auto 16px auto", lineHeight: 1.5 }}>
              has successfully completed all requirements, practical assessments, and verified milestones for
            </p>

            <h3 style={{ fontSize: "1.25rem", color: "#0f2747", margin: "10px 0 20px 0" }}>
              {selectedCert.title}
            </h3>

            <div style={{ display: "flex", justifyContent: "space-around", marginTop: "30px", borderTop: "1px solid #e2e8f0", paddingTop: "20px", fontSize: "0.85rem" }}>
              <div>
                <strong>Issue Date:</strong>
                <div>{selectedCert.issueDate}</div>
              </div>
              <div>
                <strong>Verification Code:</strong>
                <div style={{ fontFamily: "monospace", color: "#2563eb" }}>{selectedCert.certificateCode}</div>
              </div>
              <div>
                <strong>Authority:</strong>
                <div>SkillSphere Platform</div>
              </div>
            </div>

            <div style={{ marginTop: "28px", display: "flex", justifyContent: "center", gap: "12px" }}>
              <button type="button" className="btn btn--primary" onClick={() => window.print()}>
                <Icon name="print" size={16} /> Print / Save PDF
              </button>
              <button type="button" className="btn btn--ghost" onClick={() => setSelectedCert(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

/* ------------------------------------------------------------------- My Progress */
export function MyProgress({ user, setActivePage }) {
  const [submissions, setSubmissions] = useState([]);
  const [learnings, setLearnings] = useState([]);
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    Promise.all([
      getMySubmissions().catch(() => []),
      getMyLearning().catch(() => []),
      getMyCertificates().catch(() => []),
    ]).then(([subs, lrns, certs]) => {
      if (isMounted) {
        setSubmissions(subs);
        setLearnings(lrns);
        setCertificates(certs);
      }
    }).finally(() => {
      if (isMounted) setLoading(false);
    });
    return () => { isMounted = false; };
  }, []);

  const totalSubs = submissions.length;
  const passedSubs = submissions.filter((s) => s.passed).length;
  const avgScore = totalSubs ? Math.round(submissions.reduce((acc, s) => acc + (s.score || 0), 0) / totalSubs) : 0;
  const completedCourses = learnings.filter((l) => l.progressPct >= 100).length;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <PageHeader
        eyebrow="ANALYTICS"
        title="My Progress"
        subtitle="Complete database records of your assessment scores, course completions, and capability milestones."
      />

      {/* Stats Overview */}
      <div className="stat-grid">
        <div className="stat-card">
          <span className="stat-icon"><Icon name="clipboard"/></span>
          <span className="stat-label">Assessments Completed</span>
          <strong>{totalSubs}</strong>
          <small>{passedSubs} passed ({totalSubs ? Math.round((passedSubs / totalSubs) * 100) : 0}%)</small>
        </div>
        <div className="stat-card">
          <span className="stat-icon"><Icon name="trending"/></span>
          <span className="stat-label">Average Score</span>
          <strong>{avgScore}%</strong>
          <small>Across all evaluations</small>
        </div>
        <div className="stat-card">
          <span className="stat-icon"><Icon name="book"/></span>
          <span className="stat-label">Courses Completed</span>
          <strong>{completedCourses}</strong>
          <small>{learnings.length} total enrolled</small>
        </div>
        <div className="stat-card">
          <span className="stat-icon"><Icon name="award"/></span>
          <span className="stat-label">Certificates</span>
          <strong style={{ color: "#1d4ed8" }}>{certificates.length}</strong>
          <small>Verified credentials</small>
        </div>
      </div>

      {/* Historical Assessment Attempts Table */}
      <section className="panel">
        <div className="section-heading">
          <div>
            <h2>Historical Assessment Submissions ({submissions.length})</h2>
            <p className="muted">Database-backed records of every dynamic AI and official assessment attempt.</p>
          </div>
          <button className="btn btn--primary" onClick={() => setActivePage("assessment")}>
            New Assessment &rarr;
          </button>
        </div>

        {loading ? (
          <p className="muted" style={{ padding: "2rem", textAlign: "center" }}>Loading records from server…</p>
        ) : submissions.length === 0 ? (
          <EmptyState
            icon="clipboard"
            title="No assessments taken yet"
            text="Take your first assessment to establish your technical benchmark."
            actionLabel="Start Assessment"
            onAction={() => setActivePage("assessment")}
          />
        ) : (
          <div className="table-wrap" style={{ marginTop: "14px" }}>
            <table>
              <thead>
                <tr>
                  <th>Skill / Assessment</th>
                  <th>Target Level</th>
                  <th>Date Completed</th>
                  <th>Score</th>
                  <th>Correct / Total</th>
                  <th>Result</th>
                </tr>
              </thead>
              <tbody>
                {submissions.map((sub) => (
                  <tr key={sub.submissionId || sub.id}>
                    <td><strong>{sub.assessmentTitle || `Assessment #${sub.assessmentId}`}</strong></td>
                    <td>{sub.targetLevel || "Intermediate"}</td>
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
                        {sub.passed ? "PASSED" : "RETAKE NEEDED"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

/* ----------------------------------------------------------------- Mentorship */
export function Mentorship() {
  return <MentorshipHub />;
}

/* -------------------------------------------------------------- Notifications */
export function Notifications() {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadNotifications = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const [list, count] = await Promise.all([
        getNotifications().catch(() => []),
        getUnreadNotificationsCount().catch(() => 0),
      ]);
      setNotifications(list);
      setUnreadCount(count);
    } catch (e) {
      setError("Unable to load notifications. Please try again later.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
    window.addEventListener("notifications-updated", loadNotifications);
    return () => window.removeEventListener("notifications-updated", loadNotifications);
  }, [loadNotifications]);

  const handleMarkOne = async (id) => {
    try {
      await markNotificationRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, isRead: true, read: true } : n))
      );
      setUnreadCount((c) => Math.max(0, c - 1));
      window.dispatchEvent(new Event("notifications-updated"));
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  const handleMarkAll = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, isRead: true, read: true }))
      );
      setUnreadCount(0);
      window.dispatchEvent(new Event("notifications-updated"));
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  const getIconForType = (type) => {
    switch (type) {
      case "NEW_MESSAGE":
        return "message";
      case "MENTORSHIP":
      case "MENTOR_ASSIGNMENT":
      case "MENTORSHIP_REQUEST":
        return "users";
      case "ASSESSMENT":
      case "ASSESSMENT_ASSIGNED":
        return "target";
      case "COURSE":
      case "CERTIFICATE_ISSUED":
      case "CERTIFICATE_EARNED":
        return "award";
      case "CALENDAR":
      case "EVENT_INVITE":
        return "calendar";
      default:
        return "sparkles";
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="UPDATES"
        title="Notifications"
        subtitle={
          unreadCount > 0
            ? `You have ${unreadCount} unread notification${unreadCount === 1 ? "" : "s"}.`
            : "All caught up! Real-time notifications for assessments, courses, mentorship, and messages."
        }
        action={
          <div style={{ display: "flex", gap: "8px" }}>
            <button className="btn btn--ghost" onClick={loadNotifications} disabled={loading}>
              <Icon name="refresh" size={16} /> Refresh
            </button>
            {unreadCount > 0 && (
              <button className="btn btn--primary" onClick={handleMarkAll}>
                <Icon name="check" size={16} /> Mark all as read
              </button>
            )}
          </div>
        }
      />

      {error && <Alert type="error">{error}</Alert>}

      <section className="panel">
        {loading ? (
          <p className="muted">Loading notifications from server…</p>
        ) : notifications.length === 0 ? (
          <EmptyState
            icon="bell"
            title="No notifications yet"
            text="You are completely caught up! Updates about assessments, learning paths, mentorship requests, and team messages will appear here."
          />
        ) : (
          <div>
            {notifications.map((n) => {
              const isUnread = !(n.isRead || n.read);
              return (
                <div
                  className="notice-item"
                  key={n.id}
                  style={{
                    background: isUnread ? "rgba(37, 99, 235, 0.03)" : "transparent",
                    padding: "16px 14px",
                    borderRadius: "10px",
                    marginBottom: "8px",
                  }}
                >
                  <span
                    className="notice-icon"
                    style={{
                      background: isUnread ? "#eff6ff" : "var(--e-tint)",
                      color: isUnread ? "#2563eb" : "var(--e-blue-d)",
                    }}
                  >
                    <Icon name={getIconForType(n.type)} size={18} />
                  </span>
                  <div style={{ flex: 1 }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "8px" }}>
                      <div>
                        <strong>{n.title}</strong>
                        {isUnread && (
                          <span
                            className="badge"
                            style={{
                              marginLeft: "8px",
                              background: "#e0edff",
                              color: "#2563eb",
                              fontSize: "0.7rem",
                            }}
                          >
                            New
                          </span>
                        )}
                      </div>
                      {isUnread && (
                        <button
                          type="button"
                          className="btn btn--ghost"
                          style={{ fontSize: "0.75rem", padding: "3px 8px" }}
                          onClick={() => handleMarkOne(n.id)}
                          title="Mark as read"
                        >
                          Mark read
                        </button>
                      )}
                    </div>
                    <p>{n.message}</p>
                    <small>{fmtDate(n.createdAt)}</small>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </>
  );
}

/* -------------------------------------------------------------------- Reports */
export function Reports({ user, profile }) {
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadReport = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      const data = await getMyReport();
      setReport(data);
    } catch (e) {
      setError(parseApiError(e).message || "Unable to generate employee report.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReport();
  }, [loadReport]);

  return (
    <>
      <PageHeader
        eyebrow="WORKFORCE INTELLIGENCE"
        title="Employee Competency & Progress Report"
        subtitle="A verified summary of your profile, evaluated skills, calibrated gaps, and learning achievements."
        action={
          <div style={{ display: "flex", gap: "8px" }}>
            <button className="btn btn--ghost" onClick={loadReport} disabled={loading}>
              <Icon name="refresh" size={16} /> Refresh
            </button>
            <button className="btn btn--primary" onClick={() => window.print()} disabled={!report}>
              <Icon name="print" size={16} /> Print / Save PDF
            </button>
          </div>
        }
      />

      {error && <Alert type="error">{error}</Alert>}

      <section className="panel">
        {loading ? (
          <p className="muted">Generating report from database records…</p>
        ) : !report ? (
          <EmptyState
            icon="file"
            title="No report data available"
            text="Could not load your report data. Please check your network connection or try refreshing."
          />
        ) : (
          <div>
            <div className="report-head">
              <div>
                <strong style={{ fontSize: "1.2rem" }}>{report.employeeName || user.fullName}</strong>
                <small>
                  {report.jobRole || profile?.jobRole || "Software Developer"}
                  {report.employeeId ? ` · ID: ${report.employeeId}` : (user.employeeId ? ` · ID: ${user.employeeId}` : "")}
                  {report.department ? ` · Department: ${report.department}` : (profile?.department ? ` · Department: ${profile.department}` : "")}
                </small>
                <small>{report.email || user.email}</small>
              </div>
              <div style={{ textAlign: "right" }}>
                <span className="badge" style={{ background: "#e6f6ee", color: "#17754f" }}>
                  Verified Server Report
                </span>
                <small style={{ display: "block", marginTop: "4px" }}>
                  Generated {new Date().toLocaleDateString([], { dateStyle: "long" })}
                </small>
              </div>
            </div>

            <div className="report-grid">
              <div className="report-metric">
                <small>Tracked Skills</small>
                <strong>{report.totalSkills}</strong>
                <span style={{ fontSize: "0.78rem", color: "#5b6d86" }}>
                  Avg Proficiency: Level {report.averageSkillLevel}/5
                </span>
              </div>
              <div className="report-metric">
                <small>Active Knowledge Gaps</small>
                <strong style={{ color: report.activeGaps > 0 ? "#b42318" : "#17754f" }}>
                  {report.activeGaps}
                </strong>
                <span style={{ fontSize: "0.78rem", color: "#5b6d86" }}>
                  {report.activeGaps > 0 ? "Requires upskilling" : "All skills on track"}
                </span>
              </div>
              <div className="report-metric">
                <small>Courses Completed</small>
                <strong>{report.coursesCompleted}</strong>
                <span style={{ fontSize: "0.78rem", color: "#5b6d86" }}>
                  {report.learningRecords?.length || 0} total enrolled
                </span>
              </div>
              <div className="report-metric">
                <small>Active Mentorships</small>
                <strong>{report.activeMentorships}</strong>
                <span style={{ fontSize: "0.78rem", color: "#5b6d86" }}>
                  {report.certificates?.length || 0} certificates earned
                </span>
              </div>
            </div>

            {/* Skill Gaps Breakdown */}
            <div style={{ marginTop: "28px" }}>
              <h2 style={{ fontSize: "1.1rem", marginBottom: "12px" }}>Skill Competency & Gap Analysis</h2>
              {(!report.skillGaps || report.skillGaps.length === 0) ? (
                <p className="muted" style={{ fontSize: "0.88rem" }}>
                  No skill gaps detected. Either all assessed skills meet target proficiency or assessments have not yet been evaluated.
                </p>
              ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Skill</th>
                        <th>Category</th>
                        <th>Current Proficiency</th>
                        <th>Required Proficiency</th>
                        <th>Gap Size</th>
                        <th>Priority</th>
                      </tr>
                    </thead>
                    <tbody>
                      {report.skillGaps.map((g) => (
                        <tr key={g.skillId}>
                          <td><strong>{g.skillName}</strong></td>
                          <td>{g.categoryName}</td>
                          <td>{g.currentProficiency} (L{g.currentLevel})</td>
                          <td>{g.requiredProficiency} (L{g.requiredLevel})</td>
                          <td>{g.gapSize} level{g.gapSize !== 1 ? "s" : ""}</td>
                          <td>
                            <span className={priorityClass(g.priority === "ON_TRACK" ? "on-track" : g.priority)}>
                              {g.priority}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Learning Records */}
            <div style={{ marginTop: "28px" }}>
              <h2 style={{ fontSize: "1.1rem", marginBottom: "12px" }}>Enrolled Courses & Training Progress</h2>
              {(!report.learningRecords || report.learningRecords.length === 0) ? (
                <p className="muted" style={{ fontSize: "0.88rem" }}>
                  No courses enrolled yet. Browse the Course Recommendations catalogue to start learning.
                </p>
              ) : (
                <ul className="report-list">
                  {report.learningRecords.map((l) => (
                    <li key={l.id}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <strong>{l.courseTitle}</strong>
                        <span className="badge" style={{ background: l.status === "COMPLETED" ? "#e6f6ee" : "#eff6ff", color: l.status === "COMPLETED" ? "#17754f" : "#2563eb" }}>
                          {l.status} ({l.progressPct}%)
                        </span>
                      </div>
                      <span>
                        Enrolled {fmtDate(l.enrolledAt)} {l.completedAt ? `· Completed ${fmtDate(l.completedAt)}` : ""}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* Verified Certificates */}
            <div style={{ marginTop: "28px" }}>
              <h2 style={{ fontSize: "1.1rem", marginBottom: "12px" }}>Verified SkillSphere Certificates</h2>
              {(!report.certificates || report.certificates.length === 0) ? (
                <p className="muted" style={{ fontSize: "0.88rem" }}>
                  No certificates issued yet. Complete enrolled courses to 100% to earn verified certificates.
                </p>
              ) : (
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: "12px", marginTop: "10px" }}>
                  {report.certificates.map((c) => (
                    <div
                      key={c.id}
                      style={{
                        padding: "14px",
                        borderRadius: "10px",
                        border: "1px solid #c7d9fc",
                        background: "#f8faff",
                      }}
                    >
                      <strong style={{ display: "block", color: "#0f2747", fontSize: "0.95rem" }}>{c.title}</strong>
                      <small style={{ display: "block", color: "#2563eb", fontFamily: "monospace", margin: "4px 0" }}>
                        Code: {c.certificateCode}
                      </small>
                      <small style={{ color: "#5b6d86" }}>Issued: {c.issueDate}</small>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}
      </section>
    </>
  );
}
