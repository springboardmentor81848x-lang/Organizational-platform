import { api } from "./apiClient.js";

// --- Organization ---
export const getDepartments = () => api.get("/api/organization/departments").then((r) => r.data);
export const getDepartment = (id) => api.get(`/api/organization/departments/${id}`).then((r) => r.data);
export const createDepartment = (payload) => api.post("/api/organization/departments", payload).then((r) => r.data);
export const getDepartmentTeams = (deptId) => api.get(`/api/organization/departments/${deptId}/teams`).then((r) => r.data);
export const createTeam = (deptId, payload) => api.post(`/api/organization/departments/${deptId}/teams`, payload).then((r) => r.data);
export const getTeamMembers = (teamId) => api.get(`/api/organization/teams/${teamId}/members`).then((r) => r.data);
export const addTeamMember = (teamId, payload) => api.post(`/api/organization/teams/${teamId}/members`, payload).then((r) => r.data);
export const removeTeamMember = (teamId, userId) => api.delete(`/api/organization/teams/${teamId}/members/${userId}`).then((r) => r.data);

// --- Skills & Competencies ---
export const getSkillCategories = () => api.get("/api/skills/categories").then((r) => r.data);
export const getAllSkills = () => api.get("/api/skills").then((r) => r.data);
export const createSkill = (payload) => api.post("/api/skills", payload).then((r) => r.data);
export const getRoleRequirements = (jobRole) => api.get("/api/skills/requirements", { params: { jobRole } }).then((r) => r.data);
export const setRoleRequirement = (payload) => api.post("/api/skills/requirements", payload).then((r) => r.data);
export const getMySkills = () => api.get("/api/skills/employee").then((r) => r.data);
export const getEmployeeSkills = (userId) => api.get(`/api/skills/employee/${userId}`).then((r) => r.data);
export const addOrUpdateSkill = (payload) => api.post("/api/skills/employee", payload).then((r) => r.data);
export const submitSkillEvidence = (payload) => api.post("/api/skills/evidence", payload).then((r) => r.data);
export const getPendingEvidence = () => api.get("/api/skills/evidence/pending").then((r) => r.data);
export const reviewEvidence = (evidenceId, status) => api.put(`/api/skills/evidence/${evidenceId}/review`, { status }).then((r) => r.data);

// --- Assessments & Skill Gaps ---
export const getAllAssessments = () => api.get("/api/assessments").then((r) => r.data);
export const getAssessmentById = (id) => api.get(`/api/assessments/${id}`).then((r) => r.data);
export const startAssessment = (payload) => api.post("/api/assessments/start", payload).then((r) => r.data);
export const getOngoingAttempt = () => api.get("/api/assessments/ongoing").then((r) => r.data);
export const getAttemptById = (attemptId) => api.get(`/api/assessments/attempts/${attemptId}`).then((r) => r.data);
export const saveAttemptAnswers = (attemptId, answers) => api.put(`/api/assessments/attempts/${attemptId}/save`, { answers }).then((r) => r.data);
export const submitAttempt = (attemptId, payload) => api.post(`/api/assessments/attempts/${attemptId}/submit`, payload).then((r) => r.data);
export const submitAssessment = (id, payload) => api.post(`/api/assessments/${id}/submit`, payload).then((r) => r.data);
export const getMySubmissions = () => api.get("/api/assessments/my-submissions").then((r) => r.data);
export const getMySkillGaps = () => api.get("/api/assessments/my-gaps").then((r) => r.data);
export const getMyAssignments = () => api.get("/api/assessments/my-assignments").then((r) => r.data);
export const assignAssessment = (payload) => api.post("/api/assessments/assign", payload).then((r) => r.data);
export const getUserSkillGaps = (userId) => api.get(`/api/assessments/gaps/user/${userId}`).then((r) => r.data);
export const getTeamSkillGaps = (teamId) => api.get(`/api/assessments/gaps/team/${teamId}`).then((r) => r.data);
export const getOrgSkillGaps = () => api.get("/api/assessments/gaps/organization").then((r) => r.data);

// --- Learning & Training ---
export const getAllCourses = () => api.get("/api/learning/courses").then((r) => r.data);
export const createCourse = (payload) => api.post("/api/learning/courses", payload).then((r) => r.data);
export const getCourseRecommendations = () => api.get("/api/learning/recommendations").then((r) => r.data);
export const getMyLearning = () => api.get("/api/learning/my").then((r) => r.data);
export const enrollInCourse = (courseId) => api.post(`/api/learning/enroll/${courseId}`).then((r) => r.data);
export const updateCourseProgress = (courseId, progressPct) => api.put(`/api/learning/progress/${courseId}`, { progressPct }).then((r) => r.data);
export const getMyCertificates = () => api.get("/api/learning/certificates").then((r) => r.data);
export const getTrainingProgrammes = () => api.get("/api/learning/training-programmes").then((r) => r.data);
export const createTrainingProgramme = (payload) => api.post("/api/learning/training-programmes", payload).then((r) => r.data);
export const enrollInTrainingProgramme = (id) => api.post(`/api/learning/training-programmes/${id}/enroll`).then((r) => r.data);

// --- Mentorship Direct Requests & Shared Resources ---
export const createMentorshipRequest = (payload) => api.post("/api/mentorship/requests", payload).then((r) => r.data);
export const getMySentMentorshipRequests = () => api.get("/api/mentorship/requests/sent").then((r) => r.data);
export const getMyIncomingMentorshipRequests = () => api.get("/api/mentorship/requests/incoming").then((r) => r.data);
export const respondToMentorshipRequest = (requestId, payload) => api.put(`/api/mentorship/requests/${requestId}/respond`, payload).then((r) => r.data);
export const shareMentorshipResource = (payload) => api.post("/api/mentorship/resources", payload).then((r) => r.data);
export const getMentorshipSharedResources = (assignmentId) => api.get(`/api/mentorship/assignments/${assignmentId}/resources`).then((r) => r.data);

// --- Chat & Messaging ---
export const getConversations = () => api.get("/api/chat/conversations").then((r) => r.data);
export const getOrCreateDirectChat = (targetUserId) => api.post("/api/chat/conversations/direct", { targetUserId }).then((r) => r.data);
export const getConversationMessages = (convId) => api.get(`/api/chat/conversations/${convId}/messages`).then((r) => r.data);
export const sendChatMessage = (convId, payload) => api.post(`/api/chat/conversations/${convId}/messages`, payload).then((r) => r.data);
export const markChatAsRead = (convId) => api.post(`/api/chat/conversations/${convId}/read`).then((r) => r.data);

// --- Knowledge Hub & Discussion Forum ---
export const getKnowledgeCategories = () => api.get("/api/knowledge/categories").then((r) => r.data);
export const getKnowledgeArticles = (search) => api.get("/api/knowledge/articles", { params: { search } }).then((r) => r.data);
export const getKnowledgeArticleById = (id) => api.get(`/api/knowledge/articles/${id}`).then((r) => r.data);
export const createKnowledgeArticle = (payload) => api.post("/api/knowledge/articles", payload).then((r) => r.data);
export const getForumThreads = (search) => api.get("/api/forum/threads", { params: { search } }).then((r) => r.data);
export const getForumThreadById = (id) => api.get(`/api/forum/threads/${id}`).then((r) => r.data);
export const createForumThread = (payload) => api.post("/api/forum/threads", payload).then((r) => r.data);
export const postForumReply = (threadId, payload) => api.post(`/api/forum/threads/${threadId}/replies`, payload).then((r) => r.data);
export const acceptForumAnswer = (threadId, replyId) => api.put(`/api/forum/threads/${threadId}/replies/${replyId}/accept`).then((r) => r.data);

// --- Calendar ---
export const getCalendarEvents = (from, to) => api.get("/api/calendar/events", { params: { from, to } }).then((r) => r.data);
export const createCalendarEvent = (payload) => api.post("/api/calendar/events", payload).then((r) => r.data);
export const rsvpCalendarEvent = (id, status) => api.put(`/api/calendar/events/${id}/rsvp`, { status }).then((r) => r.data);

// --- Notifications ---
export const getNotifications = () => api.get("/api/notifications").then((r) => r.data);
export const getUnreadNotificationsCount = () => api.get("/api/notifications/unread-count").then((r) => r.data?.unreadCount ?? r.data);
export const markNotificationRead = (id) => api.patch(`/api/notifications/${id}/read`).then((r) => r.data);
export const markAllNotificationsRead = () => api.post("/api/notifications/read-all").then((r) => r.data);

// --- Reports ---
export const getMyReport = () => api.get("/api/reports/my").then((r) => r.data);
export const getTeamReport = (teamId) => api.get(`/api/reports/team/${teamId}`).then((r) => r.data);
export const getDepartmentReport = (deptId) => api.get(`/api/reports/department/${deptId}`).then((r) => r.data);
export const getOrganizationReport = () => api.get("/api/reports/organization").then((r) => r.data);
export const getAdminReport = () => api.get("/api/reports/admin").then((r) => r.data);

// --- Governance, Feedback, Tickets, Audit ---
export const createSupportTicket = (payload) => api.post("/api/support/tickets", payload).then((r) => r.data);
export const getMySupportTickets = () => api.get("/api/support/tickets/my").then((r) => r.data);
export const getAllSupportTickets = () => api.get("/api/admin/support/tickets").then((r) => r.data);
export const updateSupportTicket = (id, payload) => api.patch(`/api/admin/support/tickets/${id}`, payload).then((r) => r.data);
export const submitPlatformFeedback = (payload) => api.post("/api/feedback", payload).then((r) => r.data);
export const getAllPlatformFeedback = () => api.get("/api/feedback/all").then((r) => r.data);
export const getAuditLogs = () => api.get("/api/admin/audit-logs").then((r) => r.data);
export const getSystemConfig = () => api.get("/api/admin/system-config").then((r) => r.data);
export const updateSystemConfig = (key, value) => api.put(`/api/admin/system-config/${key}`, { value }).then((r) => r.data);

// --- Database Backup & Recovery (Admin) ---
export const getBackups = () => api.get("/api/admin/backups").then((r) => r.data);
export const createBackup = (payload) => api.post("/api/admin/backups", payload || {}).then((r) => r.data);
export const verifyBackup = (filename) => api.get(`/api/admin/backups/${encodeURIComponent(filename)}/verify`).then((r) => r.data);
export const testRestoreBackup = (filename) => api.post(`/api/admin/backups/${encodeURIComponent(filename)}/test-restore`).then((r) => r.data);
export const deleteBackup = (filename) => api.delete(`/api/admin/backups/${encodeURIComponent(filename)}`).then((r) => r.data);
export const getBackupDownloadUrl = (filename) => `/api/admin/backups/${encodeURIComponent(filename)}/download`;
