import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import Alert from "../components/Alert.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import ChatWidget from "../components/ChatWidget.jsx";
import CalendarWidget from "../components/CalendarWidget.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import {
  getManagedTeams,
  getManagerOverview,
  getManagerTeamMembers,
  getTeamSkillInventory,
  getTeamGapAnalysis,
  getManagerHeatmap,
  getTeamAssessments,
  getReassessmentRequests,
  cancelReassessmentRequest,
  getTeamAiRecommendations,
  getManagerCourseRecommendations,
  getTeamLearningProgress,
  getManagerMentorshipStatus,
} from "../services/managerService.js";
import {
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from "../services/platformService.js";
import { parseApiError } from "../services/apiClient.js";

import DashboardView from "./manager/DashboardView.jsx";
import MyTeamView from "./manager/MyTeamView.jsx";
import SkillInventoryView from "./manager/SkillInventoryView.jsx";
import GapAnalysisView from "./manager/GapAnalysisView.jsx";
import SkillHeatmapView from "./manager/SkillHeatmapView.jsx";
import AssessmentsView from "./manager/AssessmentsView.jsx";
import LearningRecommendationsView from "./manager/LearningRecommendationsView.jsx";
import LearningProgressView from "./manager/LearningProgressView.jsx";
import MentorshipSupportView from "./manager/MentorshipSupportView.jsx";
import TeamReportsView from "./manager/TeamReportsView.jsx";
import {
  ReassessmentModal,
  CourseRecommendationModal,
  MentorReferralModal,
} from "./manager/ManagerModals.jsx";

import "../styles/manager.css";

const NAV_ITEMS = [
  { id: "dashboard", label: "Dashboard", icon: "dashboard" },
  { id: "team", label: "My Team", icon: "users" },
  { id: "inventory", label: "Team Skill Inventory", icon: "clipboard" },
  { id: "gaps", label: "Team Gap Analysis", icon: "alert" },
  { id: "heatmap", label: "Skill Gap Heatmap", icon: "chart" },
  { id: "assessments", label: "Employee Assessments", icon: "target" },
  { id: "learning-recs", label: "Learning Recommendations", icon: "sparkles", tag: "AI" },
  { id: "learning-progress", label: "Learning Progress", icon: "trending" },
  { id: "mentorship", label: "Mentorship Support", icon: "award" },
  { id: "chat", label: "Messages / Team Chat", icon: "message" },
  { id: "notifications", label: "Alerts & Notifications", icon: "bell" },
  { id: "reports", label: "Team Reports", icon: "file" },
  { id: "calendar", label: "Calendar", icon: "calendar" },
];

export default function ManagerDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const { tab: pathTab } = useParams();
  const [searchParams, setSearchParams] = useSearchParams();

  const activeTab = pathTab || searchParams.get("tab") || "dashboard";
  const setActiveTab = (tabId) => {
    navigate(`/dashboard/manager?tab=${tabId}`);
  };

  // Team Selector State
  const [managedTeams, setManagedTeams] = useState([]);
  const [selectedTeamId, setSelectedTeamId] = useState("");

  // Domain States
  const [overview, setOverview] = useState(null);
  const [teamMembers, setTeamMembers] = useState([]);
  const [inventory, setInventory] = useState([]);
  const [gaps, setGaps] = useState([]);
  const [heatmapData, setHeatmapData] = useState(null);
  const [assessments, setAssessments] = useState([]);
  const [reassessments, setReassessments] = useState([]);
  const [aiRecs, setAiRecs] = useState(null);
  const [courseRecs, setCourseRecs] = useState([]);
  const [learnings, setLearnings] = useState([]);
  const [mentorshipStatus, setMentorshipStatus] = useState(null);
  const [notificationsList, setNotificationsList] = useState([]);

  // UI States
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(false);

  // Modal States
  const [reassessmentModal, setReassessmentModal] = useState({ open: false, data: {} });
  const [courseModal, setCourseModal] = useState({ open: false, data: {} });
  const [mentorModal, setMentorModal] = useState({ open: false, data: {} });

  // 1. Initial Load of Managed Teams
  useEffect(() => {
    getManagedTeams()
      .then((teams) => {
        setManagedTeams(teams);
      })
      .catch((err) => {
        setError(parseApiError(err).message);
      });
  }, []);

  // 2. Fetch Data Scoped to Selected Team
  const loadScopedData = useCallback(async () => {
    setLoading(true);
    setError("");
    const teamIdParam = selectedTeamId ? Number(selectedTeamId) : null;

    try {
      const [
        ov,
        members,
        inv,
        gapList,
        heat,
        asms,
        reasm,
        ai,
        cRecs,
        lrn,
        ment,
        notifs,
      ] = await Promise.all([
        getManagerOverview(teamIdParam).catch(() => null),
        getManagerTeamMembers({ teamId: teamIdParam }).catch(() => []),
        getTeamSkillInventory({ teamId: teamIdParam }).catch(() => []),
        getTeamGapAnalysis({ teamId: teamIdParam }).catch(() => []),
        getManagerHeatmap(teamIdParam).catch(() => null),
        getTeamAssessments(teamIdParam).catch(() => []),
        getReassessmentRequests(teamIdParam).catch(() => []),
        getTeamAiRecommendations(teamIdParam).catch(() => null),
        getManagerCourseRecommendations(teamIdParam).catch(() => []),
        getTeamLearningProgress({ teamId: teamIdParam }).catch(() => []),
        getManagerMentorshipStatus(teamIdParam).catch(() => null),
        getNotifications().catch(() => []),
      ]);

      setOverview(ov);
      setTeamMembers(members);
      setInventory(inv);
      setGaps(gapList);
      setHeatmapData(heat);
      setAssessments(asms);
      setReassessments(reasm);
      setAiRecs(ai);
      setCourseRecs(cRecs);
      setLearnings(lrn);
      setMentorshipStatus(ment);
      setNotificationsList(notifs);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  }, [selectedTeamId]);

  useEffect(() => {
    loadScopedData();
  }, [loadScopedData]);

  const handleSignOut = async () => {
    await signOut();
    navigate("/login");
  };

  const handleCancelReassessment = async (id) => {
    setError("");
    setSuccess("");
    try {
      await cancelReassessmentRequest(id);
      setSuccess("Reassessment request cancelled.");
      setReassessments((prev) =>
        prev.map((r) => (r.id === id ? { ...r, status: "CANCELLED" } : r))
      );
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleMarkAllNotifications = async () => {
    await markAllNotificationsRead();
    setNotificationsList((prev) => prev.map((n) => ({ ...n, read: true })));
    window.dispatchEvent(new Event("notifications-updated"));
  };

  const handleMarkOneNotification = async (id) => {
    await markNotificationRead(id);
    setNotificationsList((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
    window.dispatchEvent(new Event("notifications-updated"));
  };

  const selectedTeamName = selectedTeamId
    ? managedTeams.find((t) => String(t.id) === String(selectedTeamId))?.name || "Managed Team"
    : "All Managed Teams";

  return (
    <div className="manager-app">
      {/* Sidebar Navigation */}
      <aside className="manager-sidebar">
        <div className="sidebar-brand">
          <Logo />
          <span>TEAM LEAD WORKSPACE</span>
        </div>

        <nav>
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${activeTab === item.id ? "is-active" : ""}`}
              onClick={() => setActiveTab(item.id)}
            >
              <Icon name={item.icon} size={18} />
              <span className="nav-label">{item.label}</span>
              {item.tag && <span className="nav-tag">{item.tag}</span>}
              {item.id === "gaps" && overview?.criticalGaps > 0 && (
                <span className="nav-tag" style={{ background: "#dc2626" }}>
                  {overview.criticalGaps}
                </span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-avatar">
            {user?.fullName?.charAt(0) || "M"}
          </div>
          <div>
            <strong>{user?.fullName}</strong>
            <small>{user?.role || "Manager"}</small>
          </div>
          <button
            className="sidebar-signout"
            title="Sign out"
            onClick={handleSignOut}
          >
            <Icon name="logout" size={18} />
          </button>
        </div>
      </aside>

      {/* Main Workspace */}
      <main className="manager-main">
        {/* Topbar */}
        <header className="manager-topbar">
          <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
            <span style={{ fontSize: "0.88rem", fontWeight: 700, color: "#475569" }}>
              Reporting Scope:
            </span>
            <select
              className="filter-select"
              style={{ fontWeight: 600, minWidth: "220px", background: "#f8fafc" }}
              value={selectedTeamId}
              onChange={(e) => setSelectedTeamId(e.target.value)}
            >
              <option value="">All Managed Teams ({managedTeams.length})</option>
              {managedTeams.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} ({t.memberCount} members)
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
            <NotificationsPopover />
            <div style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "0.85rem", fontWeight: 600 }}>
              <span className="badge" style={{ background: "#eff6ff", color: "#1d4ed8" }}>
                {user?.role}
              </span>
              <span>{user?.fullName}</span>
            </div>
          </div>
        </header>

        {/* Dynamic Content */}
        <div className="manager-content">
          {error && <Alert type="error" message={error} onClose={() => setError("")} />}
          {success && <Alert type="success" message={success} onClose={() => setSuccess("")} />}

          {/* Module 1: Dashboard */}
          {activeTab === "dashboard" && (
            <DashboardView
              overview={overview}
              onNavigate={setActiveTab}
              selectedTeamName={selectedTeamName}
            />
          )}

          {/* Module 2: My Team */}
          {activeTab === "team" && (
            <MyTeamView
              teamMembers={teamMembers}
              onRequestReassessment={(data) => setReassessmentModal({ open: true, data: data || {} })}
              onRecommendCourse={(data) => setCourseModal({ open: true, data: data || {} })}
              onReferMentor={(data) => setMentorModal({ open: true, data: data || {} })}
            />
          )}

          {/* Module 3: Team Skill Inventory */}
          {activeTab === "inventory" && (
            <SkillInventoryView
              inventory={inventory}
              onNavigateToHeatmap={() => setActiveTab("heatmap")}
            />
          )}

          {/* Module 4: Team Gap Analysis */}
          {activeTab === "gaps" && (
            <GapAnalysisView
              gaps={gaps}
              onRequestReassessment={(data) => setReassessmentModal({ open: true, data: data || {} })}
              onRecommendCourse={(data) => setCourseModal({ open: true, data: data || {} })}
              onReferMentor={(data) => setMentorModal({ open: true, data: data || {} })}
            />
          )}

          {/* Module 5: Skill Gap Heatmap */}
          {activeTab === "heatmap" && (
            <SkillHeatmapView
              heatmapData={heatmapData}
              onRequestReassessment={(data) => setReassessmentModal({ open: true, data: data || {} })}
              onRecommendCourse={(data) => setCourseModal({ open: true, data: data || {} })}
            />
          )}

          {/* Module 6: Employee Assessments & Reassessments */}
          {activeTab === "assessments" && (
            <AssessmentsView
              assessments={assessments}
              reassessments={reassessments}
              onRequestReassessment={(data) => setReassessmentModal({ open: true, data: data || {} })}
              onCancelReassessment={handleCancelReassessment}
            />
          )}

          {/* Module 7: Learning Recommendations */}
          {activeTab === "learning-recs" && (
            <LearningRecommendationsView
              aiRecommendations={aiRecs}
              courseRecommendations={courseRecs}
              onRecommendCourse={(data) => setCourseModal({ open: true, data: data || {} })}
            />
          )}

          {/* Module 8: Learning Progress */}
          {activeTab === "learning-progress" && (
            <LearningProgressView learnings={learnings} />
          )}

          {/* Module 9: Mentorship Support */}
          {activeTab === "mentorship" && (
            <MentorshipSupportView
              mentorshipStatus={mentorshipStatus}
              onReferMentor={(data) => setMentorModal({ open: true, data: data || {} })}
            />
          )}

          {/* Module 10: Messages / Team Chat */}
          {activeTab === "chat" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="page-header">
                <div>
                  <span className="eyebrow">Real-Time Communication</span>
                  <h1>Team Messaging & Direct Communication</h1>
                  <p>
                    Direct messaging channels with managed team members. Privacy isolation enforced:
                    only conversations where you are a direct participant are accessible.
                  </p>
                </div>
              </div>
              <div className="panel" style={{ padding: "16px" }}>
                <ChatWidget currentUserId={user?.id} isManager={true} />
              </div>
            </div>
          )}

          {/* Module 11: Alerts & Notifications */}
          {activeTab === "notifications" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="page-header">
                <div>
                  <span className="eyebrow">Event Notifications</span>
                  <h1>Manager Alerts & Team Notifications</h1>
                  <p>
                    Real-time operational alerts for critical skill gaps, assessment attempts,
                    course completions, and team milestones.
                  </p>
                </div>
                <button className="btn btn--ghost" onClick={handleMarkAllNotifications}>
                  <Icon name="check" size={16} /> Mark All as Read
                </button>
              </div>

              <div className="panel" style={{ padding: 0 }}>
                {notificationsList.length === 0 ? (
                  <p className="muted" style={{ textAlign: "center", padding: "40px" }}>
                    No alerts or notifications recorded.
                  </p>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column" }}>
                    {notificationsList.map((n) => (
                      <div
                        key={n.id}
                        style={{
                          padding: "16px 20px",
                          borderBottom: "1px solid #e2e8f0",
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          background: n.read ? "transparent" : "#f8fafc",
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "flex-start", gap: "12px" }}>
                          <div
                            style={{
                              width: "36px",
                              height: "36px",
                              borderRadius: "10px",
                              background: n.read ? "#f1f5f9" : "#eff6ff",
                              color: n.read ? "#64748b" : "#2563eb",
                              display: "grid",
                              placeItems: "center",
                            }}
                          >
                            <Icon name="bell" size={18} />
                          </div>
                          <div>
                            <strong style={{ display: "block", fontSize: "0.92rem" }}>{n.title}</strong>
                            <p style={{ margin: "2px 0 4px", fontSize: "0.85rem", color: "#475569" }}>
                              {n.message}
                            </p>
                            <small className="muted">{new Date(n.createdAt).toLocaleString()}</small>
                          </div>
                        </div>

                        {!n.read && (
                          <button
                            className="btn btn--ghost"
                            style={{ padding: "4px 10px", minHeight: "28px", fontSize: "0.78rem" }}
                            onClick={() => handleMarkOneNotification(n.id)}
                          >
                            Mark Read
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Module 12: Team Reports */}
          {activeTab === "reports" && (
            <TeamReportsView selectedTeamId={selectedTeamId ? Number(selectedTeamId) : null} />
          )}

          {/* Module 13: Calendar */}
          {activeTab === "calendar" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="page-header">
                <div>
                  <span className="eyebrow">Team Development Schedules</span>
                  <h1>Team Calendar & Event Scheduling</h1>
                  <p>
                    Coordinate skill reviews, assessment deadlines, mentoring syncs, and team training
                    sessions with persistent attendee RSVP tracking.
                  </p>
                </div>
              </div>
              <div className="panel" style={{ padding: "20px" }}>
                <CalendarWidget currentUserId={user?.id} showCreateForm={true} />
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Global Modals */}
      <ReassessmentModal
        isOpen={reassessmentModal.open}
        onClose={() => setReassessmentModal({ open: false, data: {} })}
        initialData={reassessmentModal.data}
        teamMembers={teamMembers}
        onSuccess={(msg) => {
          setSuccess(msg);
          loadScopedData();
        }}
      />

      <CourseRecommendationModal
        isOpen={courseModal.open}
        onClose={() => setCourseModal({ open: false, data: {} })}
        initialData={courseModal.data}
        teamMembers={teamMembers}
        onSuccess={(msg) => {
          setSuccess(msg);
          loadScopedData();
        }}
      />

      <MentorReferralModal
        isOpen={mentorModal.open}
        onClose={() => setMentorModal({ open: false, data: {} })}
        initialData={mentorModal.data}
        teamMembers={teamMembers}
        availableMentors={mentorshipStatus?.availableMentors || []}
        onSuccess={(msg) => {
          setSuccess(msg);
          loadScopedData();
        }}
      />
    </div>
  );
}
