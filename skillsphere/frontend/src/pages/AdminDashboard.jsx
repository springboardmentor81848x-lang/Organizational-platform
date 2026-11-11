import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import { useAuth } from "../context/AuthContext.jsx";

// Import Admin Styles
import "../styles/admin.css";

// Import All 12 Administrative Views
import AdminDashboardOverviewView from "./admin/AdminDashboardOverviewView.jsx";
import UserManagementView from "./admin/UserManagementView.jsx";
import RolePermissionManagementView from "./admin/RolePermissionManagementView.jsx";
import DepartmentTeamSetupView from "./admin/DepartmentTeamSetupView.jsx";
import SkillCatalogueSettingsView from "./admin/SkillCatalogueSettingsView.jsx";
import ChatCommunicationSettingsView from "./admin/ChatCommunicationSettingsView.jsx";
import NotificationSettingsView from "./admin/NotificationSettingsView.jsx";
import AdminAuditLogsView from "./admin/AdminAuditLogsView.jsx";
import SecurityMonitoringView from "./admin/SecurityMonitoringView.jsx";
import SystemConfigurationView from "./admin/SystemConfigurationView.jsx";
import BackupRecoveryView from "./admin/BackupRecoveryView.jsx";
import SupportIssueLogsView from "./admin/SupportIssueLogsView.jsx";

export const ADMIN_NAV_ITEMS = [
  { id: "overview", label: "Executive Dashboard", icon: "dashboard", path: "overview" },
  { id: "users", label: "User Management", icon: "user", path: "users" },
  { id: "roles", label: "Role & Permissions", icon: "shield", path: "roles" },
  { id: "organization", label: "Departments & Teams", icon: "clipboard", path: "organization" },
  { id: "skills", label: "Skill Catalogue", icon: "layers", path: "skills" },
  { id: "communications", label: "Chat & Communications", icon: "message-square", path: "communications" },
  { id: "notifications", label: "Notification Settings", icon: "bell", path: "notifications" },
  { id: "audit-logs", label: "Audit Logs", icon: "file", path: "audit-logs" },
  { id: "security", label: "Security Monitoring", icon: "lock", path: "security" },
  { id: "configuration", label: "System Configuration", icon: "settings", path: "configuration" },
  { id: "backup", label: "Backup & Recovery", icon: "database", path: "backup" },
  { id: "support", label: "Support & Issues", icon: "help-circle", path: "support" },
];

export default function AdminDashboard() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const { tab } = useParams();

  // Active tab state synced with URL parameter
  const [activeTab, setActiveTab] = useState(() => {
    if (!tab) return "overview";
    const found = ADMIN_NAV_ITEMS.find((n) => n.path === tab || n.id === tab);
    return found ? found.id : "overview";
  });

  useEffect(() => {
    if (tab) {
      const match = ADMIN_NAV_ITEMS.find((n) => n.path === tab || n.id === tab);
      if (match && match.id !== activeTab) {
        setActiveTab(match.id);
      }
    }
  }, [tab]);

  const handleSelectTab = (tabId) => {
    setActiveTab(tabId);
    const item = ADMIN_NAV_ITEMS.find((n) => n.id === tabId);
    if (item) {
      navigate(`/admin/${item.path}`, { replace: true });
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

  const activeItem = ADMIN_NAV_ITEMS.find((n) => n.id === activeTab) || ADMIN_NAV_ITEMS[0];

  return (
    <div className="adm-app">
      {/* Sidebar Navigation */}
      <aside className="adm-sidebar">
        <div className="sidebar-brand">
          <Logo size="small" />
          <span className="adm-badge">System Admin</span>
        </div>

        <nav className="adm-nav">
          {ADMIN_NAV_ITEMS.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                className={`adm-nav-item ${isActive ? "active" : ""}`}
                onClick={() => handleSelectTab(item.id)}
              >
                <Icon name={item.icon} size={16} />
                <span>{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* User Footer */}
        <div className="adm-user-footer">
          <div className="adm-user-meta">
            <span className="adm-user-name" title={user?.fullName || user?.email}>
              {user?.fullName || "System Administrator"}
            </span>
            <span className="adm-user-role">
              {user?.role || "ADMIN"}
            </span>
          </div>
          <button className="adm-logout-btn" onClick={handleSignOut} title="Sign Out">
            <Icon name="logout" size={14} />
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main className="adm-main">
        {/* Executive Header */}
        <header className="adm-header">
          <div className="adm-title-area">
            <h1>{activeItem.label}</h1>
            <p className="adm-subtitle">
              Enterprise Governance, Operational Reliability & Platform Security
            </p>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
            <div className="adm-pill">
              <Icon name="shield" size={16} />
              <span>Platform Administration</span>
            </div>
            <NotificationsPopover />
          </div>
        </header>

        {/* Subviews Container */}
        <div className="adm-content">
          {activeTab === "overview" && (
            <AdminDashboardOverviewView onNavigateTab={handleSelectTab} />
          )}
          {activeTab === "users" && <UserManagementView />}
          {activeTab === "roles" && <RolePermissionManagementView />}
          {activeTab === "organization" && <DepartmentTeamSetupView />}
          {activeTab === "skills" && <SkillCatalogueSettingsView />}
          {activeTab === "communications" && <ChatCommunicationSettingsView />}
          {activeTab === "notifications" && <NotificationSettingsView />}
          {activeTab === "audit-logs" && <AdminAuditLogsView />}
          {activeTab === "security" && <SecurityMonitoringView />}
          {activeTab === "configuration" && <SystemConfigurationView />}
          {activeTab === "backup" && <BackupRecoveryView />}
          {activeTab === "support" && <SupportIssueLogsView />}
        </div>
      </main>
    </div>
  );
}
