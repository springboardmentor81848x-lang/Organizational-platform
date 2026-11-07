import axios from "axios";
import { ensureCsrf } from "./authService.js";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
  headers: { Accept: "application/json" },
});

api.interceptors.request.use(async (config) => {
  const method = (config.method || "get").toLowerCase();
  if (method !== "get" && method !== "head" && method !== "options") {
    await ensureCsrf();
  }
  return config;
});

// Employee Profile
export const getEmployeeProfile = () => api.get("/api/employee/profile").then((r) => r.data);
export const updateEmployeeProfile = (payload) => api.put("/api/employee/profile", payload).then((r) => r.data);

// Expert Directory & Employee Mentorship
export const getExpertDirectory = (params) =>
  api.get("/api/mentorship/experts", { params }).then((r) => r.data);

export const getMyMentor = () =>
  api.get("/api/mentorship/my-mentor").then((r) => (r.status === 204 ? null : r.data));

export const getMySessions = () => api.get("/api/mentorship/my-sessions").then((r) => r.data);
export const getMyGoals = () => api.get("/api/mentorship/my-goals").then((r) => r.data);

export const submitSessionFeedback = (sessionId, payload) =>
  api.post(`/api/mentorship/sessions/${sessionId}/feedback`, payload).then((r) => r.data);

// Direct Mentorship Requests
export const createMentorshipRequest = (payload) =>
  api.post("/api/mentorship/requests", payload).then((r) => r.data);

export const getMySentMentorshipRequests = () =>
  api.get("/api/mentorship/requests/sent").then((r) => r.data);

export const cancelMentorshipRequest = (requestId) =>
  api.patch(`/api/mentorship/requests/${requestId}/cancel`).then((r) => r.data);

// Mentor Workspace
export const getMentorStatus = () => api.get("/api/mentorship/mentor/status").then((r) => r.data);
export const getMyMentees = () => api.get("/api/mentorship/mentor/mentees").then((r) => r.data);
export const getMenteeProgress = (employeeId) =>
  api.get(`/api/mentorship/mentor/mentees/${employeeId}/progress`).then((r) => r.data);

export const createMentorshipSession = (payload) =>
  api.post("/api/mentorship/sessions", payload).then((r) => r.data);

export const updateSessionStatus = (sessionId, status) =>
  api.patch(`/api/mentorship/sessions/${sessionId}/status`, { status }).then((r) => r.data);

export const createMentorshipGoal = (payload) =>
  api.post("/api/mentorship/goals", payload).then((r) => r.data);

export const updateGoalStatus = (goalId, status) =>
  api.patch(`/api/mentorship/goals/${goalId}`, { status }).then((r) => r.data);

// Administrator Mentorship
export const adminListMentors = () => api.get("/api/admin/mentorship/mentors").then((r) => r.data);
export const adminListEligibleEmployees = () =>
  api.get("/api/admin/mentorship/eligible-employees").then((r) => r.data);

export const adminApproveMentor = (userId, payload) =>
  api.post(`/api/admin/mentorship/mentors/${userId}/approve`, payload).then((r) => r.data);

export const adminUpdateMentorStatus = (userId, isActive) =>
  api.patch(`/api/admin/mentorship/mentors/${userId}/status`, { isActive }).then((r) => r.data);

export const adminListAssignments = () =>
  api.get("/api/admin/mentorship/assignments").then((r) => r.data);

export const adminListUnassignedEmployees = () =>
  api.get("/api/admin/mentorship/unassigned-employees").then((r) => r.data);

export const adminAssignMentor = (payload) =>
  api.post("/api/admin/mentorship/assign", payload).then((r) => r.data);

export const adminEndAssignment = (assignmentId, payload) =>
  api.patch(`/api/admin/mentorship/assignments/${assignmentId}/end`, payload).then((r) => r.data);
