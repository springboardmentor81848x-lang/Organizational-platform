import { useEffect, useState, useRef, useCallback } from "react";
import Icon from "./Icons.jsx";
import Alert from "./Alert.jsx";
import {
  getConversations,
  getConversationMessages,
  sendChatMessage,
  getOrCreateDirectChat,
  createTeamChat,
  searchChatContacts,
  toggleMuteChat,
  toggleArchiveChat,
  uploadChatAttachment,
  reportChatMessage,
} from "../services/chatService.js";
import { parseApiError } from "../services/apiClient.js";
import "../styles/chat.css";

// Safe Link Formatter
function renderSafeContent(text) {
  if (!text) return "";
  const urlRegex = /(https?:\/\/[^\s]+)/g;
  const parts = text.split(urlRegex);

  return parts.map((part, index) => {
    if (part.match(urlRegex)) {
      return (
        <a
          key={index}
          href={part}
          target="_blank"
          rel="noopener noreferrer"
          className="chat-bubble-link"
        >
          {part}
        </a>
      );
    }
    return part;
  });
}

const fmtTime = (iso) => (iso ? new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "");
const fmtDate = (iso) => (iso ? new Date(iso).toLocaleDateString([], { month: "short", day: "numeric" }) : "");

export default function ChatWorkspace({ currentUserId, defaultTargetUserId = null, isManager = false }) {
  const [conversations, setConversations] = useState([]);
  const [selectedConv, setSelectedConv] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState("");
  const [filterTab, setFilterTab] = useState("ALL"); // ALL, DIRECT, TEAM, ARCHIVED
  const [searchConvQuery, setSearchConvQuery] = useState("");

  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Attachments
  const [selectedFile, setSelectedFile] = useState(null);
  const fileInputRef = useRef(null);

  // New Chat Modal
  const [showNewChatModal, setShowNewChatModal] = useState(false);
  const [contactQuery, setContactQuery] = useState("");
  const [contacts, setContacts] = useState([]);
  const [searchingContacts, setSearchingContacts] = useState(false);

  // Team Channel Modal (Manager)
  const [showTeamModal, setShowTeamModal] = useState(false);
  const [teamChannelTitle, setTeamChannelTitle] = useState("");
  const [teamIdInput, setTeamIdInput] = useState("");

  // Report Modal
  const [reportingMessage, setReportingMessage] = useState(null);
  const [reportReason, setReportReason] = useState("Inappropriate language");
  const [reportDetails, setReportDetails] = useState("");
  const [reportingSubmitting, setReportingSubmitting] = useState(false);

  const messagesEndRef = useRef(null);

  // Load conversations
  const loadConversationsList = useCallback(async () => {
    try {
      const data = await getConversations();
      setConversations(data || []);

      if (defaultTargetUserId && !selectedConv) {
        const directConv = await getOrCreateDirectChat(defaultTargetUserId);
        setSelectedConv(directConv);
      } else if (selectedConv) {
        // Sync selected conversation object
        const updated = data.find((c) => c.id === selectedConv.id);
        if (updated) setSelectedConv(updated);
      }
    } catch (e) {
      setError(parseApiError(e).message || "Failed to load conversations.");
    }
  }, [defaultTargetUserId, selectedConv]);

  useEffect(() => {
    loadConversationsList();
  }, [defaultTargetUserId]);

  // Load messages for selected conversation
  const loadMessages = useCallback(async (convId) => {
    if (!convId) return;
    try {
      const msgs = await getConversationMessages(convId);
      setMessages(msgs || []);
    } catch (e) {
      setError(parseApiError(e).message || "Failed to load message history.");
    }
  }, []);

  useEffect(() => {
    if (selectedConv) {
      loadMessages(selectedConv.id);
      // Active background polling every 4 seconds
      const timer = setInterval(() => {
        loadMessages(selectedConv.id);
      }, 4000);
      return () => clearInterval(timer);
    }
  }, [selectedConv, loadMessages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // Search permitted contacts
  const handleOpenNewChat = async () => {
    setShowNewChatModal(true);
    setSearchingContacts(true);
    setError("");
    try {
      const res = await searchChatContacts("");
      setContacts(res || []);
    } catch (err) {
      setError(parseApiError(err).message || "Failed to load permitted contacts.");
    } finally {
      setSearchingContacts(false);
    }
  };

  const handleSearchContacts = async (q) => {
    setContactQuery(q);
    setSearchingContacts(true);
    try {
      const res = await searchChatContacts(q);
      setContacts(res || []);
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSearchingContacts(false);
    }
  };

  const handleSelectContact = async (contact) => {
    try {
      setLoading(true);
      setError("");
      const conv = await getOrCreateDirectChat(contact.id);
      setShowNewChatModal(false);
      setSelectedConv(conv);
      await loadConversationsList();
    } catch (err) {
      setError(parseApiError(err).message || "Failed to start direct conversation.");
    } finally {
      setLoading(false);
    }
  };

  // Create Team Channel
  const handleCreateTeamChannel = async (e) => {
    e.preventDefault();
    if (!teamIdInput) return;
    try {
      setLoading(true);
      setError("");
      const conv = await createTeamChat(Number(teamIdInput), teamChannelTitle);
      setShowTeamModal(false);
      setTeamChannelTitle("");
      setTeamIdInput("");
      setSelectedConv(conv);
      await loadConversationsList();
      setSuccess("Team channel created successfully.");
    } catch (err) {
      setError(parseApiError(err).message || "Failed to create team channel.");
    } finally {
      setLoading(false);
    }
  };

  // Send message
  const handleSendMessage = async (e) => {
    e.preventDefault();
    if ((!newMessage.trim() && !selectedFile) || !selectedConv) return;

    try {
      setSending(true);
      setError("");

      let attachmentUrl = null;
      let attachmentName = null;

      if (selectedFile) {
        const uploadRes = await uploadChatAttachment(selectedConv.id, selectedFile);
        attachmentUrl = uploadRes.fileUrl;
        attachmentName = uploadRes.originalFileName;
      }

      const payload = {
        content: newMessage.trim() || (attachmentName ? `Sent file: ${attachmentName}` : ""),
        attachmentUrl,
        attachmentName,
      };

      const sent = await sendChatMessage(selectedConv.id, payload);
      setMessages((prev) => [...prev, sent]);
      setNewMessage("");
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
      loadConversationsList();
    } catch (err) {
      setError(parseApiError(err).message || "Failed to send message.");
    } finally {
      setSending(false);
    }
  };

  // Toggle Mute
  const handleToggleMute = async () => {
    if (!selectedConv) return;
    try {
      const res = await toggleMuteChat(selectedConv.id);
      setSelectedConv((prev) => ({ ...prev, isMuted: res.isMuted }));
      loadConversationsList();
    } catch (err) {
      setError(parseApiError(err).message || "Failed to update mute setting.");
    }
  };

  // Toggle Archive
  const handleToggleArchive = async () => {
    if (!selectedConv) return;
    try {
      const res = await toggleArchiveChat(selectedConv.id);
      setSelectedConv((prev) => ({ ...prev, isArchived: res.isArchived }));
      loadConversationsList();
    } catch (err) {
      setError(parseApiError(err).message || "Failed to update archive setting.");
    }
  };

  // Report message
  const handleOpenReport = (msg) => {
    setReportingMessage(msg);
    setReportReason("Inappropriate language");
    setReportDetails("");
  };

  const handleSubmitReport = async (e) => {
    e.preventDefault();
    if (!reportingMessage) return;
    try {
      setReportingSubmitting(true);
      setError("");
      await reportChatMessage(reportingMessage.id, {
        reason: reportReason,
        details: reportDetails,
      });
      setSuccess("Message report submitted to moderation queue.");
      setReportingMessage(null);
    } catch (err) {
      setError(parseApiError(err).message || "Failed to report message.");
    } finally {
      setReportingSubmitting(false);
    }
  };

  // Filter conversations
  const filteredConversations = conversations.filter((c) => {
    if (filterTab === "ARCHIVED" && !c.isArchived) return false;
    if (filterTab !== "ARCHIVED" && c.isArchived) return false;
    if (filterTab === "DIRECT" && c.type !== "DIRECT") return false;
    if (filterTab === "TEAM" && c.type !== "TEAM") return false;

    if (searchConvQuery.trim()) {
      const q = searchConvQuery.toLowerCase();
      const matchTitle = c.title?.toLowerCase().includes(q);
      const matchLatest = c.latestMessage?.content?.toLowerCase().includes(q);
      if (!matchTitle && !matchLatest) return false;
    }
    return true;
  });

  return (
    <div className="chat-workspace">
      {/* =========================================================================
          Left Sidebar: Conversation List
          ========================================================================= */}
      <div className="chat-sidebar">
        <div className="chat-sidebar-header">
          <h2 className="chat-sidebar-title">
            <Icon name="message-square" size={17} /> Messages
          </h2>
          <div className="chat-sidebar-actions">
            <button
              type="button"
              className="chat-icon-btn"
              onClick={loadConversationsList}
              title="Refresh conversations"
            >
              <Icon name="refresh" size={14} />
            </button>
            <button
              type="button"
              className="chat-btn-primary-sm"
              onClick={handleOpenNewChat}
              title="Start a new direct conversation"
            >
              <Icon name="plus" size={13} /> New Chat
            </button>
            {isManager && (
              <button
                type="button"
                className="chat-btn-primary-sm"
                onClick={() => setShowTeamModal(true)}
                title="Create a team channel"
                style={{ backgroundColor: "#0284c7" }}
              >
                <Icon name="users" size={13} /> Team Channel
              </button>
            )}
          </div>
        </div>

        {/* Search within conversations */}
        <div className="chat-search-bar">
          <Icon name="search" size={14} className="chat-search-icon" />
          <input
            type="text"
            className="chat-search-input"
            placeholder="Search conversations..."
            value={searchConvQuery}
            onChange={(e) => setSearchConvQuery(e.target.value)}
          />
        </div>

        {/* Filter Tabs */}
        <div className="chat-filter-tabs">
          <button
            type="button"
            className={`chat-tab-pill ${filterTab === "ALL" ? "active" : ""}`}
            onClick={() => setFilterTab("ALL")}
          >
            All
          </button>
          <button
            type="button"
            className={`chat-tab-pill ${filterTab === "DIRECT" ? "active" : ""}`}
            onClick={() => setFilterTab("DIRECT")}
          >
            Direct
          </button>
          <button
            type="button"
            className={`chat-tab-pill ${filterTab === "TEAM" ? "active" : ""}`}
            onClick={() => setFilterTab("TEAM")}
          >
            Team
          </button>
          <button
            type="button"
            className={`chat-tab-pill ${filterTab === "ARCHIVED" ? "active" : ""}`}
            onClick={() => setFilterTab("ARCHIVED")}
          >
            Archived
          </button>
        </div>

        {/* Conversations List */}
        <div className="chat-conversation-list">
          {filteredConversations.length === 0 ? (
            <div style={{ padding: "32px 16px", textAlign: "center", color: "var(--cw-text-muted)", fontSize: "13px" }}>
              <Icon name="message-square" size={24} style={{ opacity: 0.4, marginBottom: "8px" }} />
              <div>No conversations found.</div>
              <p style={{ margin: "4px 0 0", fontSize: "11.5px", color: "#94a3b8" }}>
                Click "+ New Chat" to message an authorized colleague or mentor.
              </p>
            </div>
          ) : (
            filteredConversations.map((c) => {
              const active = selectedConv && selectedConv.id === c.id;
              const initials = (c.title || "SS")
                .split(" ")
                .map((w) => w[0])
                .slice(0, 2)
                .join("");

              return (
                <div
                  key={c.id}
                  className={`chat-conv-item ${active ? "active" : ""}`}
                  onClick={() => setSelectedConv(c)}
                >
                  <div className={`chat-avatar-badge ${c.type === "TEAM" ? "team" : ""}`}>
                    {c.type === "TEAM" ? <Icon name="users" size={15} /> : initials}
                  </div>

                  <div className="chat-conv-meta">
                    <div className="chat-conv-top">
                      <span className="chat-conv-name">{c.title}</span>
                      <span className="chat-conv-time">{fmtDate(c.updatedAt)}</span>
                    </div>

                    <div className="chat-conv-preview">
                      {c.latestMessage ? (
                        <span>{c.latestMessage.content}</span>
                      ) : (
                        <span style={{ fontStyle: "italic", color: "#94a3b8" }}>No messages yet</span>
                      )}
                    </div>

                    <div className="chat-conv-badges">
                      {c.isMuted && <span className="chat-pill-muted" title="Muted"><Icon name="bell-off" size={11} /></span>}
                      {c.isArchived && <span className="chat-pill-muted" title="Archived"><Icon name="archive" size={11} /></span>}
                      {c.unreadCount > 0 && (
                        <span className="chat-unread-pill">{c.unreadCount}</span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* =========================================================================
          Right Main Area: Message Thread & Composer
          ========================================================================= */}
      <div className="chat-main">
        {selectedConv ? (
          <>
            {/* Header */}
            <div className="chat-main-header">
              <div className="chat-header-info">
                <div className={`chat-avatar-badge ${selectedConv.type === "TEAM" ? "team" : ""}`}>
                  {selectedConv.type === "TEAM" ? <Icon name="users" size={15} /> : "💬"}
                </div>
                <div>
                  <h3 className="chat-header-title">{selectedConv.title}</h3>
                  <div className="chat-header-sub">
                    <span>{selectedConv.type === "TEAM" ? "Team Channel" : "Direct Conversation"}</span>
                    <span>•</span>
                    <span>{selectedConv.participants?.length || 2} participants</span>
                    {selectedConv.isMuted && <span style={{ color: "#d97706" }}>• Muted</span>}
                    {selectedConv.isArchived && <span style={{ color: "#64748b" }}>• Archived</span>}
                  </div>
                </div>
              </div>

              <div className="chat-header-actions">
                <button
                  type="button"
                  className="chat-icon-btn"
                  onClick={handleToggleMute}
                  title={selectedConv.isMuted ? "Unmute notifications" : "Mute notifications"}
                >
                  <Icon name={selectedConv.isMuted ? "bell" : "bell-off"} size={14} />
                </button>
                <button
                  type="button"
                  className="chat-icon-btn"
                  onClick={handleToggleArchive}
                  title={selectedConv.isArchived ? "Unarchive conversation" : "Archive conversation"}
                >
                  <Icon name="archive" size={14} />
                </button>
              </div>
            </div>

            {error && <div style={{ padding: "8px 16px" }}><Alert type="error" message={error} onClose={() => setError("")} /></div>}
            {success && <div style={{ padding: "8px 16px" }}><Alert type="success" message={success} onClose={() => setSuccess("")} /></div>}

            {/* Messages Thread */}
            <div className="chat-messages">
              {messages.length === 0 ? (
                <div style={{ margin: "auto", textAlign: "center", color: "var(--cw-text-muted)" }}>
                  <Icon name="message-circle" size={32} style={{ opacity: 0.3, marginBottom: "8px" }} />
                  <div style={{ fontSize: "14px", fontWeight: 600 }}>No messages in this conversation yet.</div>
                  <p style={{ margin: "4px 0 0", fontSize: "12.5px" }}>
                    Send a message below to start collaborating securely.
                  </p>
                </div>
              ) : (
                messages.map((m) => {
                  const isMine = m.senderId === currentUserId;

                  return (
                    <div
                      key={m.id}
                      className={`chat-msg-row ${isMine ? "mine" : "theirs"}`}
                    >
                      <span className="chat-msg-sender">
                        {isMine ? "You" : m.senderName}
                      </span>

                      <div className="chat-bubble">
                        <div>{renderSafeContent(m.content)}</div>

                        {/* Attachment chip */}
                        {m.attachmentUrl && (
                          <div>
                            <a
                              href={m.attachmentUrl}
                              className="chat-attachment-chip"
                              download={m.attachmentName || "attachment"}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              <Icon name="paperclip" size={13} />
                              <span>{m.attachmentName || "Attached File"}</span>
                              <Icon name="download" size={12} style={{ marginLeft: "4px" }} />
                            </a>
                          </div>
                        )}
                      </div>

                      <div className="chat-msg-footer">
                        <span>{fmtTime(m.createdAt)}</span>
                        {isMine && (
                          <span className={`chat-status-check ${m.isRead ? "read" : ""}`}>
                            {m.isRead ? "✓✓ Read" : "✓ Sent"}
                          </span>
                        )}
                        {!isMine && (
                          <button
                            type="button"
                            className="chat-report-btn"
                            onClick={() => handleOpenReport(m)}
                            title="Report message to moderation queue"
                          >
                            Flag
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Composer */}
            <div className="chat-composer-area">
              {selectedFile && (
                <div className="chat-attachment-preview">
                  <span>
                    <Icon name="paperclip" size={13} style={{ verticalAlign: "middle", marginRight: "6px" }} />
                    {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
                  </span>
                  <button
                    type="button"
                    style={{ background: "transparent", border: "none", cursor: "pointer", color: "#ef4444" }}
                    onClick={() => {
                      setSelectedFile(null);
                      if (fileInputRef.current) fileInputRef.current.value = "";
                    }}
                  >
                    ✕
                  </button>
                </div>
              )}

              <form onSubmit={handleSendMessage} className="chat-composer-form">
                <input
                  type="file"
                  ref={fileInputRef}
                  style={{ display: "none" }}
                  onChange={(e) => {
                    if (e.target.files && e.target.files[0]) {
                      setSelectedFile(e.target.files[0]);
                    }
                  }}
                />

                <button
                  type="button"
                  className="chat-icon-btn"
                  title="Attach file (PDF, Doc, Image, CSV up to 10MB)"
                  onClick={() => fileInputRef.current?.click()}
                  style={{ height: "38px", width: "38px" }}
                >
                  <Icon name="paperclip" size={16} />
                </button>

                <textarea
                  className="chat-textarea"
                  placeholder="Type a message (Shift+Enter for newline, Enter to send)..."
                  value={newMessage}
                  rows={1}
                  maxLength={5000}
                  onChange={(e) => setNewMessage(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      handleSendMessage(e);
                    }
                  }}
                />

                <button
                  type="submit"
                  className="chat-send-btn"
                  disabled={sending || (!newMessage.trim() && !selectedFile)}
                >
                  <Icon name="send" size={15} />
                  <span>{sending ? "Sending..." : "Send"}</span>
                </button>
              </form>

              <div className="chat-composer-footer">
                <span>Enterprise Privacy Guarantee: Private conversation content is isolated and participant-authenticated.</span>
                <span>{newMessage.length} / 5000</span>
              </div>
            </div>
          </>
        ) : (
          <div style={{ margin: "auto", textAlign: "center", padding: "40px 20px", color: "var(--cw-text-muted)" }}>
            <Icon name="message-square" size={48} style={{ opacity: 0.3, marginBottom: "16px" }} />
            <h3 style={{ margin: "0 0 8px 0", fontSize: "18px", color: "var(--cw-text-main)" }}>
              Select a conversation to start messaging
            </h3>
            <p style={{ margin: "0 0 20px 0", fontSize: "13.5px", maxWidth: "420px", lineHeight: 1.5 }}>
              Collaborate securely with your team members, assigned mentors, or department colleagues with role-governed privacy protection.
            </p>
            <button
              type="button"
              className="chat-btn-primary-sm"
              onClick={handleOpenNewChat}
              style={{ padding: "8px 18px", fontSize: "13px" }}
            >
              <Icon name="plus" size={14} /> Start New Conversation
            </button>
          </div>
        )}
      </div>

      {/* =========================================================================
          Modal: Start New Direct Chat (Permitted Contact Search)
          ========================================================================= */}
      {showNewChatModal && (
        <div className="chat-modal-overlay">
          <div className="chat-modal">
            <div className="chat-modal-header">
              <h3 className="chat-modal-title">New Direct Conversation</h3>
              <button
                type="button"
                className="chat-icon-btn"
                onClick={() => setShowNewChatModal(false)}
              >
                ✕
              </button>
            </div>

            <div className="chat-modal-body">
              <div style={{ position: "relative", marginBottom: "14px" }}>
                <Icon name="search" size={14} style={{ position: "absolute", left: "10px", top: "50%", transform: "translateY(-50%)", color: "#94a3b8" }} />
                <input
                  type="text"
                  className="chat-search-input"
                  style={{ paddingLeft: "32px" }}
                  placeholder="Search permitted contacts by name or email..."
                  value={contactQuery}
                  onChange={(e) => handleSearchContacts(e.target.value)}
                />
              </div>

              {searchingContacts ? (
                <div style={{ textAlign: "center", padding: "24px", color: "#64748b" }}>
                  Searching authorized directory...
                </div>
              ) : contacts.length === 0 ? (
                <div style={{ textAlign: "center", padding: "24px", color: "#94a3b8", fontSize: "13px" }}>
                  No authorized contacts found matching "{contactQuery}".
                </div>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  {contacts.map((contact) => (
                    <div
                      key={contact.id}
                      style={{
                        padding: "10px 12px",
                        border: "1px solid #e2e8f0",
                        borderRadius: "8px",
                        cursor: "pointer",
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        transition: "background 0.15s",
                      }}
                      onClick={() => handleSelectContact(contact)}
                      onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#f8fafc")}
                      onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "#ffffff")}
                    >
                      <div>
                        <div style={{ fontWeight: 600, fontSize: "13.5px", color: "#0f172a" }}>
                          {contact.fullName}
                        </div>
                        <div style={{ fontSize: "12px", color: "#64748b" }}>
                          {contact.email} {contact.department ? `• ${contact.department}` : ""}
                        </div>
                      </div>

                      <span
                        style={{
                          fontSize: "11px",
                          fontWeight: 700,
                          padding: "3px 8px",
                          borderRadius: "9999px",
                          backgroundColor: contact.relationship === "MENTOR" ? "#dbeafe" : "#f1f5f9",
                          color: contact.relationship === "MENTOR" ? "#1e40af" : "#475569",
                        }}
                      >
                        {contact.relationship}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="chat-modal-footer">
              <button
                type="button"
                className="chat-icon-btn"
                style={{ width: "auto", padding: "6px 14px", borderRadius: "6px" }}
                onClick={() => setShowNewChatModal(false)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          Modal: Create Team Channel (Manager Only)
          ========================================================================= */}
      {showTeamModal && (
        <div className="chat-modal-overlay">
          <div className="chat-modal">
            <div className="chat-modal-header">
              <h3 className="chat-modal-title">Create Team Channel</h3>
              <button
                type="button"
                className="chat-icon-btn"
                onClick={() => setShowTeamModal(false)}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateTeamChannel}>
              <div className="chat-modal-body">
                <div style={{ marginBottom: "14px" }}>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: 600, color: "#334155", marginBottom: "4px" }}>
                    Managed Team ID *
                  </label>
                  <input
                    type="number"
                    required
                    className="chat-search-input"
                    placeholder="Enter your team ID (e.g. 1)"
                    value={teamIdInput}
                    onChange={(e) => setTeamIdInput(e.target.value)}
                  />
                  <span style={{ fontSize: "11px", color: "#64748b" }}>
                    Only teams you actively manage can be selected. All team members will be automatically added.
                  </span>
                </div>

                <div style={{ marginBottom: "14px" }}>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: 600, color: "#334155", marginBottom: "4px" }}>
                    Channel Title
                  </label>
                  <input
                    type="text"
                    className="chat-search-input"
                    placeholder="e.g. Engineering Sprint Sync"
                    value={teamChannelTitle}
                    onChange={(e) => setTeamChannelTitle(e.target.value)}
                  />
                </div>
              </div>

              <div className="chat-modal-footer">
                <button
                  type="button"
                  className="chat-icon-btn"
                  style={{ width: "auto", padding: "6px 14px", borderRadius: "6px" }}
                  onClick={() => setShowTeamModal(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="chat-btn-primary-sm"
                  disabled={loading || !teamIdInput}
                >
                  {loading ? "Creating..." : "Create Team Channel"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =========================================================================
          Modal: Report Message
          ========================================================================= */}
      {reportingMessage && (
        <div className="chat-modal-overlay">
          <div className="chat-modal">
            <div className="chat-modal-header">
              <h3 className="chat-modal-title">Report Inappropriate Message</h3>
              <button
                type="button"
                className="chat-icon-btn"
                onClick={() => setReportingMessage(null)}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSubmitReport}>
              <div className="chat-modal-body">
                <p style={{ margin: "0 0 12px 0", fontSize: "13px", color: "#475569" }}>
                  Flag message from <strong>{reportingMessage.senderName}</strong> for moderation review.
                </p>

                <div style={{ marginBottom: "14px" }}>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: 600, color: "#334155", marginBottom: "4px" }}>
                    Report Category *
                  </label>
                  <select
                    className="chat-search-input"
                    value={reportReason}
                    onChange={(e) => setReportReason(e.target.value)}
                    required
                  >
                    <option value="Inappropriate language">Inappropriate language</option>
                    <option value="Harassment or bullying">Harassment or bullying</option>
                    <option value="Confidentiality breach">Confidentiality breach</option>
                    <option value="Spam or unsolicited links">Spam or unsolicited links</option>
                    <option value="Other policy violation">Other policy violation</option>
                  </select>
                </div>

                <div style={{ marginBottom: "14px" }}>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: 600, color: "#334155", marginBottom: "4px" }}>
                    Additional Context / Details
                  </label>
                  <textarea
                    className="chat-textarea"
                    rows={3}
                    placeholder="Provide details about the incident for the moderation log..."
                    value={reportDetails}
                    onChange={(e) => setReportDetails(e.target.value)}
                  />
                </div>
              </div>

              <div className="chat-modal-footer">
                <button
                  type="button"
                  className="chat-icon-btn"
                  style={{ width: "auto", padding: "6px 14px", borderRadius: "6px" }}
                  onClick={() => setReportingMessage(null)}
                  disabled={reportingSubmitting}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="chat-btn-primary-sm"
                  style={{ backgroundColor: "#dc2626" }}
                  disabled={reportingSubmitting}
                >
                  {reportingSubmitting ? "Submitting..." : "Submit Report"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
