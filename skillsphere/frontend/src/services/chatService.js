import { api } from "./apiClient.js";

// ==========================================
// SkillSphere — Dedicated Chat & Communication Service Client
// ==========================================

export const getConversations = () =>
  api.get("/api/chat/conversations").then((r) => r.data);

export const getGlobalUnreadCount = () =>
  api.get("/api/chat/unread-count").then((r) => r.data?.unreadCount ?? 0);

export const searchChatContacts = (query) =>
  api.get("/api/chat/contacts/search", { params: { query } }).then((r) => r.data);

export const getOrCreateDirectChat = (targetUserId) =>
  api.post("/api/chat/conversations/direct", { targetUserId }).then((r) => r.data);

export const createTeamChat = (teamId, title) =>
  api.post("/api/chat/conversations/team", { teamId, title }).then((r) => r.data);

export const getConversationMessages = (convId) =>
  api.get(`/api/chat/conversations/${convId}/messages`).then((r) => r.data);

export const sendChatMessage = (convId, payload) =>
  api.post(`/api/chat/conversations/${convId}/messages`, payload).then((r) => r.data);

export const markChatAsRead = (convId) =>
  api.post(`/api/chat/conversations/${convId}/read`).then((r) => r.data);

export const toggleMuteChat = (convId) =>
  api.patch(`/api/chat/conversations/${convId}/mute`).then((r) => r.data);

export const toggleArchiveChat = (convId) =>
  api.patch(`/api/chat/conversations/${convId}/archive`).then((r) => r.data);

export const uploadChatAttachment = (convId, file) => {
  const formData = new FormData();
  formData.append("file", file);
  return api.post(`/api/chat/conversations/${convId}/attachments`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  }).then((r) => r.data);
};

export const reportChatMessage = (messageId, payload) =>
  api.post(`/api/chat/messages/${messageId}/report`, payload).then((r) => r.data);

export const getAttachmentDownloadUrl = (fileName) =>
  `/api/chat/attachments/${encodeURIComponent(fileName)}/download`;
