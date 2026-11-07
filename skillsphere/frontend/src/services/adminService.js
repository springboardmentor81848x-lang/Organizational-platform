import { api } from "./apiClient.js";

// ==========================================
// 1. Dashboard Overview
// ==========================================
export const getAdminDashboardOverview = () =>
  api.get("/api/admin/overview").then((r) => r.data);

// ==========================================
// 2. User Lifecycle Management
// ==========================================
export const getAdminUsers = (params) =>
  api.get("/api/admin/users/detailed", { params }).then((r) => r.data);

export const getAdminUserById = (id) =>
  api.get(`/api/admin/users/detailed/${id}`).then((r) => r.data);

export const createAdminUser = (payload) =>
  api.post("/api/admin/users/detailed", payload).then((r) => r.data);

export const updateAdminUser = (id, payload) =>
  api.put(`/api/admin/users/detailed/${id}`, payload).then((r) => r.data);

export const updateAdminUserStatus = (id, status) =>
  api.patch(`/api/admin/users/${id}/status`, { status }).then((r) => r.data);

// ==========================================
// 3. Roles & Permissions Management
// ==========================================
export const getAdminRolesOverview = () =>
  api.get("/api/admin/roles/overview").then((r) => r.data);

export const assignAdminUserRole = (userId, role) =>
  api.post(`/api/admin/roles/assign/${userId}`, { role }).then((r) => r.data);

export const getAdminRoleHistory = () =>
  api.get("/api/admin/roles/history").then((r) => r.data);

// ==========================================
// 4. Department & Team Setup
// ==========================================
export const getAdminDepartments = () =>
  api.get("/api/organization/departments").then((r) => r.data);

export const createAdminDepartment = (payload) =>
  api.post("/api/organization/departments", payload).then((r) => r.data);

export const getAdminAllTeams = () =>
  api.get("/api/organization/teams").then((r) => r.data);

export const createAdminTeam = (payload) =>
  api.post("/api/organization/teams", payload).then((r) => r.data);

export const getAdminTeamMembers = (teamId) =>
  api.get(`/api/organization/teams/${teamId}/members`).then((r) => r.data);

export const addAdminTeamMember = (teamId, payload) =>
  api.post(`/api/organization/teams/${teamId}/members`, payload).then((r) => r.data);

export const removeAdminTeamMember = (teamId, userId) =>
  api.delete(`/api/organization/teams/${teamId}/members/${userId}`).then((r) => r.data);

// ==========================================
// 5. Skill Catalogue Settings
// ==========================================
export const getAdminSkillCategories = () =>
  api.get("/api/skills/categories").then((r) => r.data);

export const getAdminSkills = () =>
  api.get("/api/skills").then((r) => r.data);

export const createAdminSkill = (payload) =>
  api.post("/api/skills", payload).then((r) => r.data);

export const getAdminRoleRequirements = (jobRole) =>
  api.get("/api/skills/requirements", { params: { jobRole } }).then((r) => r.data);

export const setAdminRoleRequirement = (payload) =>
  api.post("/api/skills/requirements", payload).then((r) => r.data);

// ==========================================
// 6. Communication Settings (Privacy Protected)
// ==========================================
export const getAdminCommunicationSettings = () =>
  api.get("/api/admin/communication-settings").then((r) => r.data);

export const updateAdminCommunicationSettings = (payload) =>
  api.put("/api/admin/communication-settings", payload).then((r) => r.data);

// ==========================================
// 7. Notification Settings
// ==========================================
export const getAdminNotificationSettings = () =>
  api.get("/api/admin/notification-settings").then((r) => r.data);

export const updateAdminNotificationSettings = (payload) =>
  api.put("/api/admin/notification-settings", payload).then((r) => r.data);

// ==========================================
// 8. Audit Logs
// ==========================================
export const getAdminAuditLogs = () =>
  api.get("/api/admin/audit-logs").then((r) => r.data);

// ==========================================
// 9. Security Monitoring
// ==========================================
export const getAdminSecurityOverview = () =>
  api.get("/api/admin/security/overview").then((r) => r.data);

// ==========================================
// 10. System Configuration
// ==========================================
export const getAdminSystemConfigurationOverview = () =>
  api.get("/api/admin/configuration/overview").then((r) => r.data);

export const updateAdminSystemConfig = (key, value) =>
  api.put(`/api/admin/system-config/${key}`, { value }).then((r) => r.data);

// ==========================================
// 11. Database Backup & Recovery
// ==========================================
export const getAdminBackups = () =>
  api.get("/api/admin/backups").then((r) => r.data);

export const createAdminBackup = (note) =>
  api.post("/api/admin/backups", { note }).then((r) => r.data);

export const verifyAdminBackup = (filename) =>
  api.get(`/api/admin/backups/${filename}/verify`).then((r) => r.data);

export const testRestoreAdminBackup = (filename) =>
  api.post(`/api/admin/backups/${filename}/test-restore`).then((r) => r.data);

export const downloadAdminBackup = (filename) =>
  api.get(`/api/admin/backups/${filename}/download`, { responseType: "blob" });

// ==========================================
// 12. Support & Issues
// ==========================================
export const getAdminSupportTickets = (params) =>
  api.get("/api/admin/support-tickets", { params }).then((r) => r.data);

export const updateAdminSupportTicket = (id, payload) =>
  api.put(`/api/admin/support-tickets/${id}`, payload).then((r) => r.data);

export const getAdminPlatformFeedback = () =>
  api.get("/api/feedback/all").then((r) => r.data);
