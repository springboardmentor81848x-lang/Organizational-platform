import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import { useAuth } from "../context/AuthContext.jsx";

// Import HR Styles
import "../styles/hr.css";

// Import 13 Subviews
import HrDashboardOverviewView from "./hr/HrDashboardOverviewView.jsx";
import EmployeeDirectoryView from "./hr/EmployeeDirectoryView.jsx";
import CompetencyFrameworkView from "./hr/CompetencyFrameworkView.jsx";
import RoleRequirementsView from "./hr/RoleRequirementsView.jsx";
import OrgSkillGapView from "./hr/OrgSkillGapView.jsx";
import LearningCatalogueView from "./hr/LearningCatalogueView.jsx";
import LearningPathsView from "./hr/LearningPathsView.jsx";
import TrainingProgrammesView from "./hr/TrainingProgrammesView.jsx";
import MentorDirectoryView from "./hr/MentorDirectoryView.jsx";
import LearningProgressView from "./hr/LearningProgressView.jsx";
import FeedbackEffectivenessView from "./hr/FeedbackEffectivenessView.jsx";
import ReportsAnalyticsView from "./hr/ReportsAnalyticsView.jsx";
import NotificationsAnnouncementsView from "./hr/NotificationsAnnouncementsView.jsx";

const NAV_ITEMS = [
  { id: "overview", label: "HR Dashboard", icon: "dashboard", path: "dashboard" },
  { id: "employees", label: "Employee Directory", icon: "user", path: "employees" },
  { id: "competencies", label: "Competency Framework", icon: "clipboard", path: "competencies" },
  { id: "role-requirements", label: "Role Requirements", icon: "target", path: "role-requirements" },
  { id: "skill-gaps", label: "Org Skill Gap Analysis", icon: "chart", path: "skill-gaps" },
  { id: "learning-catalogue", label: "Learning Catalogue", icon: "book", path: "learning-catalogue" },
  { id: "learning-paths", label: "Learning Paths", icon: "map", path: "learning-paths" },
  { id: "training-programmes", label: "Training Programmes", icon: "calendar", path: "training-programmes" },
  { id: "mentors", label: "Mentor Directory", icon: "users", path: "mentors" },
  { id: "learning-progress", label: "Learning Progress", icon: "trending", path: "learning-progress" },
  { id: "feedback", label: "Feedback & Effectiveness", icon: "star", path: "feedback" },
  { id: "reports", label: "Reports & Analytics", icon: "file", path: "reports" },
  { id: "notifications", label: "Notifications & Reminders", icon: "bell", path: "notifications" },
];

export default function HrDashboard() {
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
      navigate(`/hr/${item.path}`, { replace: true });
    }
  };

  const handleSignOut = async () => {
    try {
      await signOut();
      navigate("/login");
    } catch (e) {
      navigate("/login");
    }
  };

  const activeItem = NAV_ITEMS.find((n) => n.id === activeTab) || NAV_ITEMS[0];

  return (
    <div className="hr-app">
      {/* Sidebar */}
      <aside className="hr-sidebar">
        <div className="sidebar-brand">
          <Logo size="small" />
          <span className="hr-badge">HR Workspace</span>
        </div>

        <nav className="hr-nav">
          {NAV_ITEMS.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                className={`hr-nav-item ${isActive ? "active" : ""}`}
                onClick={() => handleSelectTab(item.id)}
              >
                <Icon name={item.icon} size={16} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* User Card */}
        <div className="sidebar-user">
          <div className="sidebar-user-info">
            <span className="sidebar-user-name">{user?.fullName || "HR Specialist"}</span>
            <span className="sidebar-user-role">HR Specialist</span>
          </div>
          <button className="btn-signout" onClick={handleSignOut} title="Sign Out">
            <Icon name="logout" size={14} />
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="hr-main">
        {/* Header */}
        <header className="hr-header">
          <div className="hr-header-title">
            <Icon name={activeItem.icon} size={20} />
            <h1>{activeItem.label}</h1>
          </div>

          <div className="hr-header-actions">
            <NotificationsPopover />
          </div>
        </header>

        {/* Content View */}
        <div className="hr-content">
          {activeTab === "overview" && (
            <HrDashboardOverviewView onNavigateTab={handleSelectTab} />
          )}
          {activeTab === "employees" && <EmployeeDirectoryView />}
          {activeTab === "competencies" && <CompetencyFrameworkView />}
          {activeTab === "role-requirements" && <RoleRequirementsView />}
          {activeTab === "skill-gaps" && <OrgSkillGapView />}
          {activeTab === "learning-catalogue" && <LearningCatalogueView />}
          {activeTab === "learning-paths" && <LearningPathsView />}
          {activeTab === "training-programmes" && <TrainingProgrammesView />}
          {activeTab === "mentors" && <MentorDirectoryView />}
          {activeTab === "learning-progress" && <LearningProgressView />}
          {activeTab === "feedback" && <FeedbackEffectivenessView />}
          {activeTab === "reports" && <ReportsAnalyticsView />}
          {activeTab === "notifications" && <NotificationsAnnouncementsView />}
        </div>
      </main>
    </div>
  );
}
