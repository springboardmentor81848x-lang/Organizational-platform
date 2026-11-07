import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import Icon from "../components/Icons.jsx";
import Alert from "../components/Alert.jsx";
import NotificationsPopover from "../components/NotificationsPopover.jsx";
import AdminMentorship from "./admin/AdminMentorship.jsx";
import DatabaseBackupSection from "./admin/DatabaseBackupSection.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import {
  listUsers,
  updateUserRole,
  updateUserStatus,
} from "../services/authService.js";
import {
  getAllSupportTickets,
  updateSupportTicket,
  getAllPlatformFeedback,
  getAuditLogs,
  getSystemConfig,
  updateSystemConfig,
  getAdminReport,
  getDepartments,
  createDepartment,
} from "../services/platformService.js";
import { parseApiError } from "../services/apiClient.js";

const ROLE_LABEL = {
  EMPLOYEE: "Employee",
  MANAGER: "Manager",
  MENTOR: "Mentor",
  HR_SPECIALIST: "HR Specialist",
  DEPARTMENT_HEAD: "Department Head",
  ADMIN: "Administrator",
};

const NAV_ITEMS = [
  { id: "overview", label: "Admin Overview", icon: "dashboard" },
  { id: "users", label: "User & Role Governance", icon: "user" },
  { id: "org", label: "Departments & Teams", icon: "clipboard" },
  { id: "mentorship", label: "Mentor Administration", icon: "users" },
  { id: "tickets", label: "Support Tickets", icon: "help" },
  { id: "feedback", label: "Platform Feedback", icon: "lightbulb" },
  { id: "backup", label: "Database Backup & Recovery", icon: "shield" },
  { id: "audit", label: "Audit Logs", icon: "file" },
  { id: "config", label: "System Configuration", icon: "settings" },
];

export default function AdminDashboard() {
  const { user: me, signOut } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");

  const [adminReport, setAdminReport] = useState(null);
  const [users, setUsers] = useState([]);
  const [tickets, setTickets] = useState([]);
  const [feedback, setFeedback] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [configs, setConfigs] = useState([]);
  const [departments, setDepartments] = useState([]);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // New dept form
  const [deptName, setDeptName] = useState("");
  const [deptCode, setDeptCode] = useState("");
  const [deptDesc, setDeptDesc] = useState("");

  // Ticket triage
  const [selectedTicket, setSelectedTicket] = useState(null);
  const [ticketStatus, setTicketStatus] = useState("IN_PROGRESS");
  const [adminNotes, setAdminNotes] = useState("");

  const loadData = useCallback(() => {
    getAdminReport().then(setAdminReport).catch(() => {});
    listUsers().then(setUsers).catch((e) => setError(parseApiError(e).message));
    getAllSupportTickets().then(setTickets).catch(() => []);
    getAllPlatformFeedback().then(setFeedback).catch(() => []);
    getAuditLogs().then(setAuditLogs).catch(() => []);
    getSystemConfig().then(setConfigs).catch(() => []);
    getDepartments().then(setDepartments).catch(() => []);
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const actUser = async (fn) => {
    setError(""); setSuccess("");
    try {
      const updated = await fn();
      setUsers((list) => list.map((u) => (u.id === updated.id ? updated : u)));
      setSuccess("User updated successfully.");
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  const handleCreateDept = async (e) => {
    e.preventDefault();
    if (!deptName || !deptCode) return;
    setError(""); setSuccess("");
    try {
      await createDepartment({ name: deptName, code: deptCode, description: deptDesc });
      setSuccess("Department created!");
      setDeptName(""); setDeptCode(""); setDeptDesc("");
      loadData();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handleUpdateTicket = async (e) => {
    e.preventDefault();
    if (!selectedTicket) return;
    setError(""); setSuccess("");
    try {
      await updateSupportTicket(selectedTicket.id, { status: ticketStatus, adminNotes });
      setSuccess("Ticket updated!");
      setSelectedTicket(null);
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
          <span>ADMIN WORKSPACE</span>
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
              {item.id === "tickets" && tickets.filter((t) => t.status === "OPEN").length > 0 && (
                <span className="nav-tag">{tickets.filter((t) => t.status === "OPEN").length}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="sidebar-avatar">{me.fullName.charAt(0)}</div>
          <div>
            <strong>{me.fullName}</strong>
            <small>{me.email}</small>
          </div>
          <button type="button" className="sidebar-signout" onClick={onSignOut} title="Sign out">
            <Icon name="logout" size={18} />
          </button>
        </div>
      </aside>

      <div className="employee-main">
        <header className="employee-topbar">
          <span className="topbar-title">Platform Administration • {NAV_ITEMS.find((n) => n.id === activeTab)?.label}</span>
          <div className="topbar-user">
            <span className="badge">ADMIN</span>
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
                  <span className="eyebrow" style={{ color: "#dbe7ff" }}>Security & Governance</span>
                  <h1>SkillSphere System Health</h1>
                  <p>
                    Full administrative oversight of user accounts, role authorizations, audit logs, and system configurations.
                  </p>
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Active Accounts</span>
                  <strong style={{ fontSize: "1.8rem", color: "#17754f" }}>
                    {adminReport ? adminReport.activeUsers : users.filter((u) => u.accountStatus === "ACTIVE").length}
                  </strong>
                  <span className="muted">Total users: {adminReport ? adminReport.totalUsers : users.length}</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Open Tickets</span>
                  <strong style={{ fontSize: "1.8rem", color: "#8a5a00" }}>
                    {tickets.filter((t) => t.status === "OPEN").length}
                  </strong>
                  <span className="muted">Awaiting triage</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">Audit Events</span>
                  <strong style={{ fontSize: "1.8rem", color: "#2563eb" }}>
                    {adminReport ? adminReport.totalAuditLogs : auditLogs.length}
                  </strong>
                  <span className="muted">Logged operations</span>
                </div>
                <div className="panel" style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <span className="eyebrow">System Messages</span>
                  <strong style={{ fontSize: "1.8rem" }}>
                    {adminReport ? adminReport.totalMessages : 24}
                  </strong>
                  <span className="muted">In {adminReport ? adminReport.totalConversations : 8} conversations</span>
                </div>
              </div>
            </div>
          )}

          {/* USERS */}
          {activeTab === "users" && (
            <div className="panel">
              <h2>User Governance & Role Assignment</h2>
              <p className="muted">Manage permissions and account statuses for all 6 documented platform roles.</p>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr><th>Name</th><th>Email</th><th>Assigned Role</th><th>Requested</th><th>Status</th></tr>
                  </thead>
                  <tbody>
                    {users.map((u) => {
                      const self = u.id === me.id;
                      return (
                        <tr key={u.id}>
                          <td><strong>{u.fullName}</strong><small>{u.employeeId || "No ID"}</small></td>
                          <td>{u.email}</td>
                          <td>
                            <select
                              aria-label={`Role for ${u.fullName}`}
                              value={u.role}
                              disabled={self}
                              onChange={(e) => actUser(() => updateUserRole(u.id, e.target.value))}
                            >
                              {Object.entries(ROLE_LABEL).map(([v, l]) => (
                                <option key={v} value={v}>{l}</option>
                              ))}
                            </select>
                          </td>
                          <td>
                            {u.requestedRole ? (
                              <span className="badge badge--warn">{ROLE_LABEL[u.requestedRole] || u.requestedRole}</span>
                            ) : "—"}
                          </td>
                          <td>
                            <select
                              aria-label={`Status for ${u.fullName}`}
                              value={u.accountStatus}
                              disabled={self}
                              onChange={(e) => actUser(() => updateUserStatus(u.id, e.target.value))}
                            >
                              <option value="ACTIVE">Active</option>
                              <option value="PENDING">Pending</option>
                              <option value="SUSPENDED">Suspended</option>
                            </select>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* ORG */}
          {activeTab === "org" && (
            <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
              <div className="panel" style={{ maxWidth: "600px" }}>
                <h2>Create Department</h2>
                <form onSubmit={handleCreateDept} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Department Name</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Quality Engineering"
                      value={deptName}
                      onChange={(e) => setDeptName(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Department Code</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. QE"
                      value={deptCode}
                      onChange={(e) => setDeptCode(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <div>
                    <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Description</label>
                    <textarea
                      rows={2}
                      placeholder="Department charter..."
                      value={deptDesc}
                      onChange={(e) => setDeptDesc(e.target.value)}
                      style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
                    />
                  </div>
                  <button type="submit" className="btn btn--primary">
                    Create Department
                  </button>
                </form>
              </div>

              <div className="panel">
                <h2>Configured Departments ({departments.length})</h2>
                <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(260px, 1fr))", gap: "14px", marginTop: "14px" }}>
                  {departments.map((d) => (
                    <div key={d.id} style={{ padding: "16px", borderRadius: "12px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                      <span className="badge">{d.code}</span>
                      <strong style={{ display: "block", margin: "6px 0" }}>{d.name}</strong>
                      <p style={{ margin: 0, fontSize: "0.84rem", color: "#5b6d86" }}>{d.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* MENTORSHIP */}
          {activeTab === "mentorship" && <AdminMentorship />}

          {/* TICKETS */}
          {activeTab === "tickets" && (
            <div className="panel">
              <h2>Help & Support Tickets</h2>
              <div className="table-wrap" style={{ marginTop: "14px" }}>
                <table>
                  <thead>
                    <tr><th>User</th><th>Subject</th><th>Priority</th><th>Status</th><th>Submitted</th><th>Actions</th></tr>
                  </thead>
                  <tbody>
                    {tickets.length === 0 ? (
                      <tr><td colSpan="6" style={{ textAlign: "center", color: "#8795a9" }}>No support tickets submitted.</td></tr>
                    ) : (
                      tickets.map((t) => (
                        <tr key={t.id}>
                          <td><strong>{t.userName}</strong><small>{t.userEmail}</small></td>
                          <td>
                            <strong>{t.subject}</strong>
                            <div style={{ fontSize: "0.8rem", color: "#5b6d86" }}>{t.description}</div>
                            {t.adminNotes && <div style={{ fontSize: "0.78rem", color: "#2563eb", marginTop: "4px" }}>Note: {t.adminNotes}</div>}
                          </td>
                          <td><span className="badge">{t.priority}</span></td>
                          <td>
                            <span className={`badge ${t.status === "OPEN" ? "badge--warn" : ""}`}>{t.status}</span>
                          </td>
                          <td>{new Date(t.createdAt).toLocaleDateString()}</td>
                          <td>
                            <button
                              type="button"
                              className="text-action"
                              onClick={() => {
                                setSelectedTicket(t);
                                setTicketStatus(t.status);
                                setAdminNotes(t.adminNotes || "");
                              }}
                            >
                              Triage
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>

              {selectedTicket && (
                <div style={{ marginTop: "24px", padding: "18px", border: "1.5px solid #2563eb", borderRadius: "12px", background: "#f9fbfe" }}>
                  <h3>Triage Ticket #{selectedTicket.id}: {selectedTicket.subject}</h3>
                  <form onSubmit={handleUpdateTicket} style={{ display: "flex", flexDirection: "column", gap: "10px", marginTop: "10px" }}>
                    <div>
                      <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Status</label>
                      <select
                        value={ticketStatus}
                        onChange={(e) => setTicketStatus(e.target.value)}
                        style={{ padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                      >
                        <option value="OPEN">OPEN</option>
                        <option value="IN_PROGRESS">IN_PROGRESS</option>
                        <option value="RESOLVED">RESOLVED</option>
                        <option value="CLOSED">CLOSED</option>
                      </select>
                    </div>
                    <div>
                      <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Admin Resolution Notes</label>
                      <textarea
                        rows={2}
                        value={adminNotes}
                        onChange={(e) => setAdminNotes(e.target.value)}
                        style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                      />
                    </div>
                    <div style={{ display: "flex", gap: "10px" }}>
                      <button type="submit" className="btn btn--primary">Update Ticket</button>
                      <button type="button" className="btn btn--ghost" onClick={() => setSelectedTicket(null)}>Cancel</button>
                    </div>
                  </form>
                </div>
              )}
            </div>
          )}

          {/* FEEDBACK */}
          {activeTab === "feedback" && (
            <div className="panel">
              <h2>Platform Feedback Reviews</h2>
              <div style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "14px" }}>
                {feedback.map((fb) => (
                  <div key={fb.id} style={{ padding: "14px", borderRadius: "10px", border: "1px solid #e2e9f3", background: "#f9fbfe" }}>
                    <div style={{ display: "flex", justifyContent: "space-between" }}>
                      <strong>{fb.userName} • {fb.category}</strong>
                      <span className="badge">Rating: {fb.rating} / 5 ⭐</span>
                    </div>
                    <p style={{ margin: "6px 0 0", fontSize: "0.86rem", color: "#5b6d86" }}>{fb.comments}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* BACKUP & RECOVERY */}
          {activeTab === "backup" && <DatabaseBackupSection />}

          {/* AUDIT */}
          {activeTab === "audit" && (
            <div className="panel">
              <h2>Security Audit Logs</h2>
              <div className="table-wrap" style={{ marginTop: "14px" }}>
                <table>
                  <thead>
                    <tr><th>Action</th><th>User</th><th>Entity</th><th>Details</th><th>IP Address</th><th>Timestamp</th></tr>
                  </thead>
                  <tbody>
                    {auditLogs.length === 0 ? (
                      <tr><td colSpan="6" style={{ textAlign: "center", color: "#8795a9" }}>No audit log entries recorded.</td></tr>
                    ) : (
                      auditLogs.map((l) => (
                        <tr key={l.id}>
                          <td><span className="badge">{l.action}</span></td>
                          <td><strong>{l.userName}</strong></td>
                          <td>{l.entityName} {l.entityId ? `#${l.entityId}` : ""}</td>
                          <td>{l.details}</td>
                          <td><small>{l.ipAddress || "127.0.0.1"}</small></td>
                          <td>{new Date(l.createdAt).toLocaleString()}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* CONFIG */}
          {activeTab === "config" && (
            <div className="panel">
              <h2>System Configuration</h2>
              <p className="muted">Runtime parameters persisted in database.</p>
              <div className="table-wrap" style={{ marginTop: "14px" }}>
                <table>
                  <thead>
                    <tr><th>Parameter Key</th><th>Value</th><th>Description</th></tr>
                  </thead>
                  <tbody>
                    {configs.map((c) => (
                      <tr key={c.configKey}>
                        <td><strong>{c.configKey}</strong></td>
                        <td><code>{c.configValue}</code></td>
                        <td>{c.description}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
