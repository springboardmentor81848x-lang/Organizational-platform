import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import { useAuth } from "../context/AuthContext.jsx";

// Import Department Head Styles
import "../styles/department-head.css";

// Import 10 Subviews
import DepartmentHeadDashboardOverviewView from "./department-head/DepartmentHeadDashboardOverviewView.jsx";
import DepartmentOverviewView from "./department-head/DepartmentOverviewView.jsx";
import DepartmentSkillMatrixView from "./department-head/DepartmentSkillMatrixView.jsx";
import DepartmentGapHeatmapView from "./department-head/DepartmentGapHeatmapView.jsx";
import WorkforceCapabilityView from "./department-head/WorkforceCapabilityView.jsx";
import LearningMentoringOverviewView from "./department-head/LearningMentoringOverviewView.jsx";
import RiskPriorityAlertsView from "./department-head/RiskPriorityAlertsView.jsx";
import DepartmentReportsAnalyticsView from "./department-head/DepartmentReportsAnalyticsView.jsx";
import DepartmentAnnouncementsView from "./department-head/DepartmentAnnouncementsView.jsx";
import DepartmentNotificationsView from "./department-head/DepartmentNotificationsView.jsx";

const NAV_ITEMS = [
  { id: "overview", label: "Executive Dashboard", icon: "dashboard", path: "dashboard" },
  { id: "department", label: "Department & Teams", icon: "users", path: "overview" },
  { id: "skill-matrix", label: "Skill Matrix", icon: "clipboard", path: "skill-matrix" },
  { id: "heatmap", label: "Gap Heatmap", icon: "chart", path: "heatmap" },
  { id: "capability", label: "Workforce Capability", icon: "award", path: "capability" },
  { id: "learning-mentoring", label: "Learning & Mentoring", icon: "book", path: "learning-mentoring" },
  { id: "risks", label: "Risk & Priority Alerts", icon: "alert", path: "risks" },
  { id: "reports", label: "Reports & Analytics", icon: "file", path: "reports" },
  { id: "announcements", label: "Communications", icon: "message", path: "announcements" },
  { id: "notifications", label: "Notifications", icon: "bell", path: "notifications" },
];

export default function DepartmentHeadDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const { tab } = useParams();

  // Active tab state synced with URL
  const [activeTab, setActiveTab] = useState(() => {
    if (!tab) return "overview";
    const found = NAV_ITEMS.find((n) => n.path === tab || n.id === tab);
    return found ? found.id : "overview";
  });

  useEffect(() => {
    if (tab) {
      const match = NAV_ITEMS.find((n) => n.path === tab || n.id === tab);
      if (match && match.id !== activeTab) {
        setActiveTab(match.id);
      }
    }
  }, [tab]);

  const handleSelectTab = (tabId) => {
    setActiveTab(tabId);
    const item = NAV_ITEMS.find((n) => n.id === tabId);
    if (item) {
      navigate(`/department-head/${item.path}`, { replace: true });
    }
  };

  const handleSignOut = async () => {
    try {
      await signOut();
      navigate("/login");
    } catch {
      navigate("/login");
    }
  };

  const activeItem = NAV_ITEMS.find((n) => n.id === activeTab) || NAV_ITEMS[0];

  return (
    <div className="dh-app">
      {/* Sidebar */}
      <aside className="dh-sidebar">
        <div className="sidebar-brand">
          <Logo size="small" />
          <span className="dh-badge">Dept Head</span>
        </div>

        <nav className="dh-nav">
          {NAV_ITEMS.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                className={`dh-nav-item ${isActive ? "active" : ""}`}
                onClick={() => handleSelectTab(item.id)}
              >
                <Icon name={item.icon} size={16} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* User Footer */}
        <div className="dh-user-footer">
          <div className="dh-user-meta">
            <span className="dh-user-name" title={user?.fullName || user?.email}>
              {user?.fullName || "Department Executive"}
            </span>
            <span className="dh-user-dept">
              {user?.department || "Department Head"}
            </span>
          </div>
          <button className="dh-logout-btn" onClick={handleSignOut} title="Sign Out">
            <Icon name="logout" size={14} />
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="dh-main">
        {/* Top Header */}
        <header className="dh-header">
          <div className="dh-title-area">
            <h1>{activeItem.label}</h1>
            <p className="dh-subtitle">
              Strategic Capability, Risk & Workforce Intelligence
            </p>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
            <div className="dh-dept-pill">
              <Icon name="shield" size={16} />
              <span>{user?.department || "Executive Leadership"}</span>
            </div>
            <NotificationsPopover />
          </div>
        </header>

        {/* Subviews Container */}
        <div className="dh-content">
          {activeTab === "overview" && (
            <DepartmentHeadDashboardOverviewView onNavigateTab={handleSelectTab} />
          )}
          {activeTab === "department" && (
            <DepartmentOverviewView onNavigateTab={handleSelectTab} />
          )}
          {activeTab === "skill-matrix" && <DepartmentSkillMatrixView />}
          {activeTab === "heatmap" && <DepartmentGapHeatmapView />}
          {activeTab === "capability" && (
            <WorkforceCapabilityView onNavigateTab={handleSelectTab} />
          )}
          {activeTab === "learning-mentoring" && <LearningMentoringOverviewView />}
          {activeTab === "risks" && <RiskPriorityAlertsView />}
          {activeTab === "reports" && <DepartmentReportsAnalyticsView />}
          {activeTab === "announcements" && <DepartmentAnnouncementsView />}
          {activeTab === "notifications" && <DepartmentNotificationsView />}
        </div>
      </main>
    </div>
  );
}
