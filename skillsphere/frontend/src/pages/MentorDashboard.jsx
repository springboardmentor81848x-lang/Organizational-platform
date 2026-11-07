import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import Alert from "../components/Alert.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import { useAuth } from "../context/AuthContext.jsx";

// Import Mentor CSS
import "../styles/mentor.css";

// Import Subviews
import DashboardOverviewView from "./mentor/DashboardOverviewView.jsx";
import MentorProfileView from "./mentor/MentorProfileView.jsx";
import MentorshipRequestsView from "./mentor/MentorshipRequestsView.jsx";
import MyMenteesView from "./mentor/MyMenteesView.jsx";
import MenteeSkillOverviewView from "./mentor/MenteeSkillOverviewView.jsx";
import DevelopmentGoalsView from "./mentor/DevelopmentGoalsView.jsx";
import MentoringSessionsView from "./mentor/MentoringSessionsView.jsx";
import MentorChatView from "./mentor/MentorChatView.jsx";
import SharedResourcesView from "./mentor/SharedResourcesView.jsx";
import KnowledgeContributionsView from "./mentor/KnowledgeContributionsView.jsx";
import MentoringFeedbackView from "./mentor/MentoringFeedbackView.jsx";
import MentorNotificationsView from "./mentor/MentorNotificationsView.jsx";

import { getMentorOverview, getMentorProfile } from "../services/mentorService.js";
import { parseApiError } from "../services/apiClient.js";

const NAV_ITEMS = [
  { id: "overview", label: "Dashboard", icon: "dashboard", path: "dashboard" },
  { id: "profile", label: "Mentor Profile", icon: "user", path: "profile" },
  { id: "requests", label: "Mentorship Requests", icon: "inbox", path: "requests" },
  { id: "mentees", label: "My Mentees", icon: "users", path: "mentees" },
  { id: "mentee-skills", label: "Mentee Skill Overview", icon: "target", path: "mentee-skills" },
  { id: "goals", label: "Development Goals", icon: "clipboard", path: "goals" },
  { id: "sessions", label: "Mentoring Sessions", icon: "clock", path: "sessions" },
  { id: "chat", label: "Messages / Chatbox", icon: "message", path: "chat" },
  { id: "resources", label: "Shared Resources", icon: "book", path: "resources" },
  { id: "contributions", label: "Knowledge Contributions", icon: "award", path: "contributions" },
  { id: "feedback", label: "Feedback", icon: "star", path: "feedback" },
  { id: "notifications", label: "Notifications", icon: "bell", path: "notifications" },
];

export default function MentorDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const { tab } = useParams();

  // Active Tab state synced with URL
  const [activeTab, setActiveTab] = useState(() => {
    if (!tab) return "overview";
    const found = NAV_ITEMS.find((n) => n.path === tab || n.id === tab);
    return found ? found.id : "overview";
  });

  const [overview, setOverview] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [triggerScheduleModal, setTriggerScheduleModal] = useState(false);

  useEffect(() => {
    if (tab) {
      const found = NAV_ITEMS.find((n) => n.path === tab || n.id === tab);
      if (found && found.id !== activeTab) {
        setActiveTab(found.id);
      }
    }
  }, [tab]);

  const loadBaseData = async () => {
    setLoading(true);
    try {
      const [ov, pr] = await Promise.all([
        getMentorOverview().catch(() => null),
        getMentorProfile().catch(() => null),
      ]);
      setOverview(ov);
      setProfile(pr);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBaseData();
  }, []);

  const handleNavClick = (id) => {
    setActiveTab(id);
    const item = NAV_ITEMS.find((n) => n.id === id);
    if (item) {
      navigate(`/mentor/${item.path}`);
    }
  };

  const onSignOut = async () => {
    await signOut();
    navigate("/login", { replace: true });
  };

  const pendingRequestsCount = overview ? overview.pendingRequestsCount : 0;

  return (
    <div className="mentor-app">
      {/* 12-Module Sidebar */}
      <aside className="mentor-sidebar">
        <div className="sidebar-brand">
          <Logo />
          <div>
            <strong style={{ display: "block", fontSize: "0.92rem", letterSpacing: "0.02em" }}>SkillSphere</strong>
            <span className="sidebar-brand-badge">MENTOR WORKSPACE</span>
          </div>
        </div>

        <nav>
          {NAV_ITEMS.map((item) => (
            <button
              key={item.id}
              type="button"
              className={`mentor-nav-item ${activeTab === item.id ? "is-active" : ""}`}
              onClick={() => handleNavClick(item.id)}
            >
              <Icon name={item.icon} size={18} />
              <span>{item.label}</span>
              {item.id === "requests" && pendingRequestsCount > 0 && (
                <span className="mentor-nav-badge">{pendingRequestsCount}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="mentor-sidebar-footer">
          <div className="mentor-avatar">{user.fullName.charAt(0)}</div>
          <div style={{ flex: 1, minWidth: 0, overflow: "hidden" }}>
            <strong style={{ display: "block", fontSize: "0.85rem", whiteSpace: "nowrap", textOverflow: "ellipsis", overflow: "hidden" }}>
              {user.fullName}
            </strong>
            <small style={{ color: "var(--mt-faint)", fontSize: "0.75rem", whiteSpace: "nowrap", textOverflow: "ellipsis", overflow: "hidden", display: "block" }}>
              {user.email}
            </small>
          </div>
          <button
            type="button"
            className="mt-btn mt-btn--outline"
            style={{ padding: "6px 8px" }}
            onClick={onSignOut}
            title="Sign out"
          >
            <Icon name="logout" size={16} />
          </button>
        </div>
      </aside>

      {/* Main Workspace Area */}
      <div className="mentor-main">
        <header className="mentor-topbar">
          <div className="mentor-topbar-title">
            <span>Mentor Hub</span>
            <span style={{ color: "var(--mt-faint)" }}>/</span>
            <span style={{ color: "var(--mt-primary-dark)" }}>
              {NAV_ITEMS.find((n) => n.id === activeTab)?.label}
            </span>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
            <span className="mt-badge mt-badge--active">Role: MENTOR</span>
            <NotificationsPopover />
          </div>
        </header>

        <main className="mentor-content">
          {error && <Alert type="error">{error}</Alert>}

          {/* Module 1: Dashboard */}
          {activeTab === "overview" && (
            <DashboardOverviewView
              overview={overview}
              user={user}
              onNavigateTab={handleNavClick}
              onOpenScheduleModal={() => {
                setTriggerScheduleModal(true);
                handleNavClick("sessions");
              }}
            />
          )}

          {/* Module 2: Mentor Profile */}
          {activeTab === "profile" && (
            <MentorProfileView
              profile={profile}
              onProfileUpdated={(updated) => {
                setProfile(updated);
                loadBaseData();
              }}
            />
          )}

          {/* Module 3: Mentorship Requests */}
          {activeTab === "requests" && (
            <MentorshipRequestsView onRequestsUpdated={loadBaseData} />
          )}

          {/* Module 4: My Mentees */}
          {activeTab === "mentees" && (
            <MyMenteesView onNavigateTab={handleNavClick} />
          )}

          {/* Module 5: Mentee Skill Overview */}
          {activeTab === "mentee-skills" && (
            <MenteeSkillOverviewView />
          )}

          {/* Module 6: Development Goals */}
          {activeTab === "goals" && (
            <DevelopmentGoalsView />
          )}

          {/* Module 7: Mentoring Sessions */}
          {activeTab === "sessions" && (
            <MentoringSessionsView
              showScheduleModal={triggerScheduleModal}
              onCloseScheduleModal={() => setTriggerScheduleModal(false)}
            />
          )}

          {/* Module 8: Messages / Chatbox */}
          {activeTab === "chat" && (
            <MentorChatView user={user} />
          )}

          {/* Module 9: Shared Resources */}
          {activeTab === "resources" && (
            <SharedResourcesView />
          )}

          {/* Module 10: Knowledge Contributions */}
          {activeTab === "contributions" && (
            <KnowledgeContributionsView />
          )}

          {/* Module 11: Feedback */}
          {activeTab === "feedback" && (
            <MentoringFeedbackView />
          )}

          {/* Module 12: Notifications */}
          {activeTab === "notifications" && (
            <MentorNotificationsView onNavigateTab={handleNavClick} />
          )}
        </main>
      </div>
    </div>
  );
}
