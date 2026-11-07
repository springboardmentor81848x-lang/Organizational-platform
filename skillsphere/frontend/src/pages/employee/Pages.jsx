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
    : <span className="ai-badge ai-badge--rules">Standard recommendations</span>;
}

function CourseSkeleton() {
  return <div className="ai-loading" role="status" aria-live="polite">
    <div className="ai-loading__head"><span className="ai-spinner" aria-hidden="true"/><div><strong>Gemini is analysing your results…</strong><small>Building a learning path from your skill gaps. This usually takes a few seconds.</small></div></div>
    <div className="course-grid">{[0, 1, 2, 3].map((i) => <div className="course-card skeleton" key={i}><span className="sk sk--sm"/><span className="sk sk--lg"/><span className="sk"/><span className="sk sk--md"/></div>)}</div>
  </div>;
}

export function CourseCard({ course, done, onToggle }) {
  const link = `https://www.google.com/search?q=${encodeURIComponent(course.searchQuery || course.title)}`;
  return <article className="course-card">
    <div className="course-card__top"><span className={priorityClass(course.priority === "Low" ? "on-track" : course.priority)}>{course.priority} priority</span><span className="badge">{course.level}</span></div>
    <p className="eyebrow">{course.skill}</p>
    <h3>{course.title}</h3>
    <p className="course-reason">{course.reason}</p>
    {course.topics?.length > 0 && <ul className="chips">{course.topics.map((t) => <li key={t}>{t}</li>)}</ul>}
    <div className="course-meta"><span><Icon name="clock" size={15}/> {course.duration}</span><span><Icon name="book" size={15}/> {course.provider}</span></div>
    <div className="course-actions">
      <a className="btn btn--ghost" href={link} target="_blank" rel="noopener noreferrer">Find course <Icon name="external" size={15}/></a>
      <button className={`btn ${done ? "btn--complete" : "btn--primary"}`} onClick={onToggle} aria-pressed={done}>{done ? <><Icon name="check" size={16}/> Completed</> : "Mark complete"}</button>
    </div>
  </article>;
}

function RecommendationBlock({ recs, completed, toggleCourse, onRetry, limit }) {
  if (recs.status === "loading") return <CourseSkeleton/>;
  if (recs.status === "error") return <div className="ai-error"><Icon name="alert" size={22}/><div><strong>We couldn't generate recommendations</strong><p>{recs.error}</p></div><button className="btn btn--primary" onClick={onRetry}><Icon name="refresh" size={16}/> Try again</button></div>;
  if (!recs.data) return null;
  const list = limit ? recs.data.recommendations.slice(0, limit) : recs.data.recommendations;
  return <>
    <div className="ai-summary"><span className="ai-summary__icon"><Icon name="lightbulb" size={22}/></span><div><AiBadge source={recs.data.source}/><p>{recs.data.summary}</p></div></div>
    {recs.data.notice && <Alert type="success">{recs.data.notice}</Alert>}
    <div className="course-grid">{list.map((c) => <CourseCard key={slug(c.title)} course={c} done={completed.includes(slug(c.title))} onToggle={() => toggleCourse(slug(c.title))}/>)}</div>
  </>;
}

/* ------------------------------------------------------------------ Overview */
export function Overview({ user, assessment, latestSubmission, serverGaps = [], recs, progress, completed, setActivePage }) {
  const sub = latestSubmission || assessment;
  const gapCount = serverGaps.length > 0
    ? serverGaps.filter((s) => (s.gapSize ?? s.gap ?? 0) > 0).length
    : (sub?.skillResults ? sub.skillResults.filter((s) => s.gap > 0).length : null);
  const courseCount = recs.data?.recommendations?.length ?? 0;
  const steps = [
    { icon: "clipboard", title: "Take a skill assessment", text: "Official assessments calibrated to industry role targets." },
    { icon: "sparkles", title: "AI analyses your gaps", text: "Google Gemini studies your results and role requirements." },
    { icon: "book", title: "Follow your learning path", text: "Get courses ranked by what you need most." },
  ];
  return <>
    <section className="hero"><div><p className="eyebrow">SKILLSPHERE · EMPLOYEE WORKSPACE</p><h1>Welcome back, {user.fullName?.split(" ")[0] || "there"}</h1><p>Measure your skills, uncover knowledge gaps and get an AI-built learning path for your career.</p>
      <button className="btn btn--light" onClick={() => setActivePage("assessment")}>{sub ? "Take assessment" : "Start assessment"} <Icon name="arrow" size={16}/></button></div>
      <div className="hero__art" aria-hidden="true"><Icon name="sparkles" size={56} strokeWidth={1.4}/></div></section>
    <div className="stat-grid">
      <div className="stat-card">
        <span className="stat-icon"><Icon name="clipboard"/></span>
        <span className="stat-label">Latest score</span>
        <strong>{sub ? `${sub.score}%` : "—"}</strong>
        <small>{sub ? (sub.correctAnswers !== undefined ? `${sub.correctAnswers} of ${sub.totalQuestions} correct (${sub.passed ? "Passed" : "Not passed"})` : `${sub.correct} of ${sub.total} correct`) : "No assessment taken yet"}</small>
      </div>
      <div className="stat-card">
        <span className="stat-icon"><Icon name="target"/></span>
        <span className="stat-label">Knowledge gaps</span>
        <strong>{gapCount !== null ? gapCount : "—"}</strong>
        <small>{sub || serverGaps.length > 0 ? "Skills below target" : "Available after assessment"}</small>
      </div>
      <div className="stat-card">
        <span className="stat-icon"><Icon name="sparkles"/></span>
        <span className="stat-label">AI recommendations</span>
        <strong>{courseCount || "—"}</strong>
        <small>{courseCount ? "Courses picked for you" : "Generated after assessment"}</small>
      </div>
      <div className="stat-card">
        <span className="stat-icon"><Icon name="trending"/></span>
        <span className="stat-label">Learning progress</span>
        <strong>{progress}%</strong>
        <small>{completed.length} course{completed.length === 1 ? "" : "s"} completed</small>
      </div>
    </div>
    {(!sub && serverGaps.length === 0) ? (
      <section className="panel"><div className="section-heading"><div><p className="eyebrow">HOW IT WORKS</p><h2>Your path in three steps</h2></div></div>
        <div className="steps">{steps.map((s, i) => <div className="step" key={s.title}><span className="step__num">{i + 1}</span><span className="step__icon"><Icon name={s.icon}/></span><strong>{s.title}</strong><p>{s.text}</p></div>)}</div></section>
    ) : (
      <div className="two-col">
        <section className="panel"><div className="section-heading"><div><p className="eyebrow">SKILL SNAPSHOT</p><h2>Where you stand</h2></div><button className="text-action" onClick={() => setActivePage("gaps")}>Full summary <Icon name="arrow" size={14}/></button></div>
          <SkillBars results={serverGaps.length > 0 ? serverGaps : (sub?.skillResults || [])}/></section>
        <section className="panel"><div className="section-heading"><div><p className="eyebrow">RECOMMENDED FOR YOU</p><h2>Next courses</h2></div><button className="text-action" onClick={() => setActivePage("courses")}>View all <Icon name="arrow" size={14}/></button></div>
          {recs.status === "loading" && <p className="muted"><span className="ai-spinner ai-spinner--inline"/> Gemini is preparing your recommendations…</p>}
          {recs.data?.recommendations?.slice(0, 3).map((c) => <div className="mini-course" key={slug(c.title)}><span className="course-dot"><Icon name="book" size={18}/></span><div><strong>{c.title}</strong><small>{c.skill} · {c.duration}</small></div><span className={priorityClass(c.priority === "Low" ? "on-track" : c.priority)}>{c.priority}</span></div>)}
          {recs.status === "error" && <p className="muted">Recommendations are unavailable right now. Open the learning paths page to retry.</p>}
        </section>
      </div>
    )}
  </>;
}

/* -------------------------------------------------------------------- Profile */
export function Profile({ profile, setProfile, onSave, saved }) {
  const set = (k) => (e) => setProfile({ ...profile, [k]: e.target.value });
  return <>
    <PageHeader eyebrow="ACCOUNT" title="Employee Profile Management" subtitle="Your job role, department, and technical skills power personalized mentorship and course recommendations."/>
    <section className="panel">
      {saved && <Alert type="success">Profile saved successfully to your SkillSphere account.</Alert>}
      <div className="profile-grid">
        <label className="field"><span>Full name</span><input value={profile.fullName || ""} onChange={set("fullName")}/></label>
        <label className="field"><span>Employee ID</span><input value={profile.employeeId || ""} placeholder="Enter employee ID" onChange={set("employeeId")}/></label>
        <label className="field"><span>Email</span><input value={profile.email || ""} readOnly/></label>
        <label className="field"><span>Department</span><input value={profile.department || ""} placeholder="e.g. Engineering" maxLength={80} onChange={set("department")}/></label>
        <label className="field"><span>Job role</span><select value={profile.jobRole || "Software Developer"} onChange={set("jobRole")}><option>Software Developer</option><option>Data Analyst</option><option>QA Engineer</option><option>Support Engineer</option></select></label>
        <label className="field"><span>Experience level</span><select value={profile.experience || "Entry level"} onChange={set("experience")}><option>Entry level</option><option>Intermediate</option><option>Experienced</option></select></label>
      </div>
      <div style={{ marginTop: "1rem", display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem" }}>
        <label className="field">
          <span>Technical Skills & Expertise (comma-separated)</span>
          <input value={profile.skills || ""} placeholder="e.g. Java, Spring Boot, SQL, Docker, React" onChange={set("skills")} />
        </label>
        <label className="field">
          <span>Professional Bio / Growth Goals</span>
          <input value={profile.bio || ""} placeholder="Brief statement about your career or mentoring focus" onChange={set("bio")} />
        </label>
      </div>
      <button className="btn btn--primary" style={{ marginTop: "1rem" }} onClick={onSave}>Save profile</button>
    </section></>;
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

export function Results({ assessment, latestSubmission, serverGaps = [], recs, completed, toggleCourse, onRetry, setActivePage }) {
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
          <p>{sub.assessmentTitle ? `Assessment: ${sub.assessmentTitle}. ` : ""}The bars below compare your evaluated level with the role target for each skill.</p>
        </div>
      </div>
      <SkillBars results={serverGaps.length > 0 ? serverGaps : (sub.skillResults || [])}/>
    </section>
    <section className="panel"><div className="section-heading"><div><p className="eyebrow">AI RECOMMENDATIONS</p><h2>Courses picked for you</h2></div>
      {recs.status === "done" && <button className="text-action" onClick={() => setActivePage("courses")}>Open learning paths <Icon name="arrow" size={14}/></button>}</div>
      <RecommendationBlock recs={recs} completed={completed} toggleCourse={toggleCourse} onRetry={onRetry} limit={3}/></section></>;
}

/* ----------------------------------------------------------------------- Gaps */
export function Gaps({ assessment, serverGaps = [], setActivePage }) {
  const gaps = serverGaps.length > 0 ? serverGaps : (assessment?.skillResults || []);
  if (!gaps.length) {
    return (
      <>
        <PageHeader eyebrow="ANALYSIS" title="Knowledge Gap Summary"/>
        <section className="panel">
          <EmptyState
            icon="target"
            title="No gaps calculated yet"
            text="Complete an assessment and SkillSphere will compare your level with the target for each skill."
            actionLabel="Take assessment"
            onAction={() => setActivePage("assessment")}
          />
        </section>
      </>
    );
  }
  return <>
    <PageHeader eyebrow="ANALYSIS" title="Knowledge Gap Summary" subtitle="Your evaluated proficiency compared with the target requirement for each skill."/>
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
                  <td>{gap}</td>
                  <td>
                    <span className={priorityClass(priority === "ON_TRACK" ? "on-track" : priority)}>
                      {priority}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      <button className="btn btn--ghost" onClick={() => setActivePage("assessment")}>Take another assessment</button>
    </section>
  </>;
}

/* -------------------------------------------------------------------- Courses */
export function Courses({ assessment, latestSubmission, recs, completed, toggleCourse, onRetry, setActivePage }) {
  const sub = latestSubmission || assessment;
  const action = sub && recs.status !== "loading" && <button className="btn btn--ghost" onClick={onRetry}><Icon name="refresh" size={16}/> Regenerate</button>;
  return <>
    <PageHeader eyebrow="AI-POWERED" title="Recommended Training and Learning Paths" subtitle="Courses chosen by Google Gemini from your assessment result and job role." action={action}/>
    {!sub ? <section className="panel"><EmptyState icon="sparkles" title="Take an assessment to unlock recommendations" text="Gemini needs your assessment result to build a personalised learning path." actionLabel="Start assessment" onAction={() => setActivePage("assessment")}/></section>
      : <section className="panel"><RecommendationBlock recs={recs} completed={completed} toggleCourse={toggleCourse} onRetry={onRetry}/></section>}
  </>;
}

/* ------------------------------------------------------------------- Progress */
export function Progress({ recs, completed, progress, toggleCourse, setActivePage }) {
  const list = recs.data?.recommendations || [];
  if (!list.length) return <><PageHeader eyebrow="TRACKING" title="Learning Progress Tracking"/><section className="panel"><EmptyState icon="trending" title="Nothing to track yet" text="Once you receive course recommendations, you can track your completion here." actionLabel="Start assessment" onAction={() => setActivePage("assessment")}/></section></>;
  return <>
    <PageHeader eyebrow="TRACKING" title="Learning Progress Tracking" subtitle="Mark courses as complete as you finish them."/>
    <section className="panel"><div className="progress-summary"><div><p className="eyebrow">COURSE COMPLETION</p><strong>{progress}%</strong><p>{list.filter((c) => completed.includes(slug(c.title))).length} of {list.length} recommended courses completed</p></div><div className="progress-ring" style={{ "--progress": `${progress}%` }}><span>{progress}%</span></div></div>
      <div className="progress-track"><span style={{ width: `${progress}%` }}/></div>
      {list.map((c) => { const done = completed.includes(slug(c.title)); return <div className="progress-course" key={slug(c.title)}><div><strong>{c.title}</strong><small>{c.skill} · {c.duration}</small></div>
        <button className={`btn ${done ? "btn--complete" : "btn--ghost"}`} onClick={() => toggleCourse(slug(c.title))}>{done ? <><Icon name="check" size={16}/> Completed</> : "Mark complete"}</button></div>; })}
    </section></>;
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
