import { api } from "./apiClient.js";

export const getManagedTeams = () =>
  api.get("/api/manager/teams").then((r) => r.data);

export const getManagerOverview = (teamId) =>
  api.get("/api/manager/overview", { params: { teamId } }).then((r) => r.data);

export const getManagerTeamMembers = ({ teamId, search } = {}) =>
  api.get("/api/manager/team-members", { params: { teamId, search } }).then((r) => r.data);

export const getManagerEmployeeDetail = (employeeId) =>
  api.get(`/api/manager/team-members/${employeeId}`).then((r) => r.data);

export const getTeamSkillInventory = ({ teamId, categoryId, search } = {}) =>
  api.get("/api/manager/skill-inventory", { params: { teamId, categoryId, search } }).then((r) => r.data);

export const getTeamGapAnalysis = ({ teamId, priority, categoryId, search } = {}) =>
  api.get("/api/manager/gap-analysis", { params: { teamId, priority, categoryId, search } }).then((r) => r.data);

export const getManagerHeatmap = (teamId) =>
  api.get("/api/manager/heatmap", { params: { teamId } }).then((r) => r.data);

export const getTeamAssessments = (teamId) =>
  api.get("/api/manager/assessments", { params: { teamId } }).then((r) => r.data);

export const getReassessmentRequests = (teamId) =>
  api.get("/api/manager/reassessments", { params: { teamId } }).then((r) => r.data);

export const createReassessmentRequest = (payload) =>
  api.post("/api/manager/reassessments", payload).then((r) => r.data);

export const cancelReassessmentRequest = (id) =>
  api.delete(`/api/manager/reassessments/${id}`).then((r) => r.data);

export const getTeamAiRecommendations = (teamId) =>
  api.get("/api/manager/recommendations", { params: { teamId } }).then((r) => r.data);

export const getManagerCourseRecommendations = (teamId) =>
  api.get("/api/manager/course-recommendations", { params: { teamId } }).then((r) => r.data);

export const recommendCourseToEmployee = (payload) =>
  api.post("/api/manager/course-recommendations", payload).then((r) => r.data);

export const getTeamLearningProgress = ({ teamId, status, search } = {}) =>
  api.get("/api/manager/learning-progress", { params: { teamId, status, search } }).then((r) => r.data);

export const getManagerMentorshipStatus = (teamId) =>
  api.get("/api/manager/mentorship", { params: { teamId } }).then((r) => r.data);

export const referMentorToEmployee = (payload) =>
  api.post("/api/manager/mentorship/refer", payload).then((r) => r.data);

export const getManagerReport = (reportType, teamId) =>
  api.get("/api/manager/reports", { params: { reportType, teamId } }).then((r) => r.data);

export const downloadManagerReportCsv = async (reportType, teamId) => {
  const response = await api.get("/api/manager/reports/export", {
    params: { reportType, teamId },
    responseType: "blob",
  });
  const url = window.URL.createObjectURL(new Blob([response.data], { type: "text/csv;charset=utf-8;" }));
  const link = document.createElement("a");
  link.href = url;
  const filename = `${(reportType || "report").toLowerCase()}_${new Date().toISOString().slice(0, 10)}.csv`;
  link.setAttribute("download", filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};
