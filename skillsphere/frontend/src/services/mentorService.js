import { api } from "./apiClient.js";

// 1. Dashboard Overview
export const getMentorOverview = () =>
  api.get("/api/mentor/overview").then((r) => r.data);

// 2. Profile & Availability
export const getMentorProfile = () =>
  api.get("/api/mentor/profile").then((r) => r.data);

export const updateMentorProfile = (payload) =>
  api.put("/api/mentor/profile", payload).then((r) => r.data);

// 3. Mentorship Requests
export const getMentorRequests = (status) =>
  api.get("/api/mentor/requests", { params: { status } }).then((r) => r.data);

export const respondToMentorshipRequest = (requestId, payload) =>
  api.put(`/api/mentor/requests/${requestId}/respond`, payload).then((r) => r.data);

// 4. My Mentees & Dossier
export const getMyMentees = (status) =>
  api.get("/api/mentor/mentees", { params: { status } }).then((r) => r.data);

export const getMenteeDossier = (assignmentId) =>
  api.get(`/api/mentor/mentees/assignments/${assignmentId}/dossier`).then((r) => r.data);

export const endMentorship = (assignmentId, payload) =>
  api.post(`/api/mentor/mentees/assignments/${assignmentId}/end`, payload).then((r) => r.data);

// 5. Mentee Skill Overview (Consent Aware, Read-Only)
export const getMenteeSkills = (menteeId) =>
  api.get(`/api/mentor/mentees/${menteeId}/skills`).then((r) => r.data);

// 6. Development Goals & Action Items
export const getMentorGoals = (params) =>
  api.get("/api/mentor/goals", { params }).then((r) => r.data);

export const createMentorGoal = (payload) =>
  api.post("/api/mentor/goals", payload).then((r) => r.data);

export const updateMentorGoal = (goalId, payload) =>
  api.patch(`/api/mentor/goals/${goalId}`, payload).then((r) => r.data);

export const createGoalActionItem = (goalId, payload) =>
  api.post(`/api/mentor/goals/${goalId}/action-items`, payload).then((r) => r.data);

export const toggleGoalActionItem = (actionItemId) =>
  api.patch(`/api/mentor/goals/action-items/${actionItemId}/toggle`).then((r) => r.data);

export const deleteGoalActionItem = (actionItemId) =>
  api.delete(`/api/mentor/goals/action-items/${actionItemId}`).then((r) => r.data);

// 7. Mentoring Sessions & Notes
export const getMentorSessions = (params) =>
  api.get("/api/mentor/sessions", { params }).then((r) => r.data);

export const createMentorSession = (payload) =>
  api.post("/api/mentor/sessions", payload).then((r) => r.data);

export const updateSessionStatus = (sessionId, status) =>
  api.patch(`/api/mentor/sessions/${sessionId}/status`, { status }).then((r) => r.data);

export const updateSessionNotes = (sessionId, payload) =>
  api.patch(`/api/mentor/sessions/${sessionId}/notes`, payload).then((r) => r.data);

// 8. Shared Resources
export const getMentorResources = (params) =>
  api.get("/api/mentor/resources", { params }).then((r) => r.data);

export const createMentorResource = (payload) =>
  api.post("/api/mentor/resources", payload).then((r) => r.data);

export const deleteMentorResource = (resourceId) =>
  api.delete(`/api/mentor/resources/${resourceId}`).then((r) => r.data);

// 9. Knowledge Contributions
export const getMentorArticles = () =>
  api.get("/api/mentor/contributions").then((r) => r.data);

export const createMentorArticle = (payload) =>
  api.post("/api/mentor/contributions", payload).then((r) => r.data);

export const updateMentorArticle = (articleId, payload) =>
  api.put(`/api/mentor/contributions/${articleId}`, payload).then((r) => r.data);

export const deleteMentorArticle = (articleId) =>
  api.delete(`/api/mentor/contributions/${articleId}`).then((r) => r.data);

// 10. Feedback & Evaluations
export const getMentorFeedback = () =>
  api.get("/api/mentor/feedback").then((r) => r.data);

export const createMentorEvaluation = (payload) =>
  api.post("/api/mentor/feedback/evaluations", payload).then((r) => r.data);

// 11. Notifications
export const getMentorNotifications = (unreadOnly) =>
  api.get("/api/mentor/notifications", { params: { unreadOnly } }).then((r) => r.data);

export const markNotificationRead = (notificationId) =>
  api.patch(`/api/mentor/notifications/${notificationId}/read`).then((r) => r.data);

export const markAllNotificationsRead = () =>
  api.patch("/api/mentor/notifications/read-all").then((r) => r.data);

// Categories & Skills Lookup
export const getSkillCategories = () =>
  api.get("/api/skills/categories").then((r) => r.data);

export const getAllSkills = () =>
  api.get("/api/skills").then((r) => r.data);
