import { api } from "./apiClient.js";

// ==========================================
// 1. Dashboard Overview
// ==========================================
export const getDepartmentHeadOverview = (params) =>
  api.get("/api/department-head/overview", { params }).then((r) => r.data);

// ==========================================
// 2. Department Overview & Teams
// ==========================================
export const getDepartmentOverview = (params) =>
  api.get("/api/department-head/department", { params }).then((r) => r.data);

export const getDepartmentTeams = (params) =>
  api.get("/api/department-head/teams", { params }).then((r) => r.data);

// ==========================================
// 3. Department Skill Matrix
// ==========================================
export const getDepartmentSkillMatrix = (params) =>
  api.get("/api/department-head/skill-matrix", { params }).then((r) => r.data);

// ==========================================
// 4. Gap Heatmap
// ==========================================
export const getDepartmentHeatmap = (params) =>
  api.get("/api/department-head/heatmap", { params }).then((r) => r.data);

// ==========================================
// 5. Workforce Capability
// ==========================================
export const getWorkforceCapability = (params) =>
  api.get("/api/department-head/capability", { params }).then((r) => r.data);

// ==========================================
// 6. Learning & Mentoring Overview
// ==========================================
export const getDepartmentLearningMentoring = (params) =>
  api.get("/api/department-head/learning-mentoring", { params }).then((r) => r.data);

// ==========================================
// 7. Risk & Priority Alerts
// ==========================================
export const getDepartmentRiskAlerts = (params) =>
  api.get("/api/department-head/risks", { params }).then((r) => r.data);

// ==========================================
// 8. Reports & Analytics
// ==========================================
export const getDepartmentReport = (params) =>
  api.get("/api/department-head/reports", { params }).then((r) => r.data);

export const exportDepartmentReportCsv = (params) =>
  api.get("/api/department-head/reports/csv", {
    params,
    responseType: "blob",
  });

export const getSharedLeadershipReports = (params) =>
  api.get("/api/department-head/leadership-reports/shared", { params }).then((r) => r.data);

// ==========================================
// 9. Announcements
// ==========================================
export const getDepartmentAnnouncements = (params) =>
  api.get("/api/department-head/announcements", { params }).then((r) => r.data);

export const createDepartmentAnnouncement = (payload, params) =>
  api.post("/api/department-head/announcements", payload, { params }).then((r) => r.data);

export const markDepartmentAnnouncementRead = (id) =>
  api.post(`/api/department-head/announcements/${id}/read`).then((r) => r.data);

// ==========================================
// 10. Notifications
// ==========================================
export const getDepartmentNotifications = (unreadOnly = false) =>
  api.get("/api/department-head/notifications", { params: { unreadOnly } }).then((r) => r.data);

export const markDepartmentNotificationRead = (id) =>
  api.post(`/api/department-head/notifications/${id}/read`).then((r) => r.data);

export const markAllDepartmentNotificationsRead = () =>
  api.post("/api/department-head/notifications/read-all").then((r) => r.data);
