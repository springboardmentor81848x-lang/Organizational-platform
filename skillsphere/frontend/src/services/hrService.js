import { api } from "./apiClient.js";

// 1. Overview
export const getHrOverview = () =>
  api.get("/api/hr/overview").then((r) => r.data);

// 2. Employee Directory & Profiles
export const getHrEmployees = (params) =>
  api.get("/api/hr/employees", { params }).then((r) => r.data);

export const getHrEmployeeDossier = (id) =>
  api.get(`/api/hr/employees/${id}/dossier`).then((r) => r.data);

// 3. Competency Framework
export const getHrCompetencies = () =>
  api.get("/api/hr/competencies").then((r) => r.data);

export const createHrCompetency = (payload) =>
  api.post("/api/hr/competencies", payload).then((r) => r.data);

export const updateHrCompetency = (id, payload) =>
  api.put(`/api/hr/competencies/${id}`, payload).then((r) => r.data);

export const toggleHrCompetencyActive = (id) =>
  api.patch(`/api/hr/competencies/${id}/toggle-active`).then((r) => r.data);

// 4. Role Requirements
export const getHrRoleRequirements = (jobRole) =>
  api.get("/api/hr/role-requirements", { params: { jobRole } }).then((r) => r.data);

export const getHrRoleMatrix = () =>
  api.get("/api/hr/role-requirements/matrix").then((r) => r.data);

export const createHrRoleRequirement = (payload) =>
  api.post("/api/hr/role-requirements", payload).then((r) => r.data);

export const updateHrRoleRequirement = (id, payload) =>
  api.put(`/api/hr/role-requirements/${id}`, payload).then((r) => r.data);

export const deleteHrRoleRequirement = (id) =>
  api.delete(`/api/hr/role-requirements/${id}`).then((r) => r.data);

// 5. Skill Gap Analysis
export const getHrSkillGapSummary = () =>
  api.get("/api/hr/skill-gaps/summary").then((r) => r.data);

export const getHrOrgHeatmap = () =>
  api.get("/api/hr/skill-gaps/heatmap").then((r) => r.data);

// 6. Learning Catalogue
export const getHrCourses = () =>
  api.get("/api/hr/courses").then((r) => r.data);

export const createHrCourse = (payload) =>
  api.post("/api/hr/courses", payload).then((r) => r.data);

export const updateHrCourse = (id, payload) =>
  api.put(`/api/hr/courses/${id}`, payload).then((r) => r.data);

export const toggleHrCourseActive = (id) =>
  api.patch(`/api/hr/courses/${id}/toggle-active`).then((r) => r.data);

// 7. Learning Paths
export const getHrLearningPaths = () =>
  api.get("/api/hr/learning-paths").then((r) => r.data);

export const createHrLearningPath = (payload) =>
  api.post("/api/hr/learning-paths", payload).then((r) => r.data);

export const updateHrLearningPath = (id, payload) =>
  api.put(`/api/hr/learning-paths/${id}`, payload).then((r) => r.data);

export const toggleHrLearningPathActive = (id) =>
  api.patch(`/api/hr/learning-paths/${id}/toggle-active`).then((r) => r.data);

// 8. Training Programmes
export const getHrTrainingProgrammes = () =>
  api.get("/api/hr/training-programmes").then((r) => r.data);

export const createHrTrainingProgramme = (payload) =>
  api.post("/api/hr/training-programmes", payload).then((r) => r.data);

export const updateHrTrainingProgramme = (id, payload) =>
  api.put(`/api/hr/training-programmes/${id}`, payload).then((r) => r.data);

export const getHrProgrammeEnrollments = (id) =>
  api.get(`/api/hr/training-programmes/${id}/enrollments`).then((r) => r.data);

export const enrollEmployeeInProgramme = (id, payload) =>
  api.post(`/api/hr/training-programmes/${id}/enroll`, payload).then((r) => r.data);

export const updateHrProgrammeEnrollmentStatus = (enrollmentId, payload) =>
  api.patch(`/api/hr/training-programmes/enrollments/${enrollmentId}/status`, payload).then((r) => r.data);

// 9. Mentor Directory
export const getHrMentors = () =>
  api.get("/api/hr/mentors").then((r) => r.data);

export const getHrMentorsBySkill = (skillId) =>
  api.get(`/api/hr/mentors/by-skill/${skillId}`).then((r) => r.data);

// 10. Learning Progress
export const getHrLearningProgress = () =>
  api.get("/api/hr/learning-progress").then((r) => r.data);

// 11. Feedback & Effectiveness
export const getHrFeedback = () =>
  api.get("/api/hr/feedback").then((r) => r.data);

// 12. Reports & Analytics & Leadership Sharing
export const generateHrReport = (params) =>
  api.get("/api/hr/reports/generate", { params }).then((r) => r.data);

export const shareHrReportWithLeadership = (payload) =>
  api.post("/api/hr/leadership-reports/share", payload).then((r) => r.data);

export const getSharedLeadershipReports = (params) =>
  api.get("/api/hr/leadership-reports/shared", { params }).then((r) => r.data);

// 13. Notifications & Announcements
export const getHrAnnouncements = () =>
  api.get("/api/hr/announcements").then((r) => r.data);

export const createHrAnnouncement = (payload) =>
  api.post("/api/hr/announcements", payload).then((r) => r.data);

export const markHrAnnouncementRead = (id) =>
  api.post(`/api/hr/announcements/${id}/read`).then((r) => r.data);
