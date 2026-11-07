import { useEffect, useState, useRef } from "react";
import Icon from "./Icons.jsx";
import Alert from "./Alert.jsx";
import {
  getConversations,
  getConversationMessages,
  sendChatMessage,
  getOrCreateDirectChat,
} from "../services/platformService.js";
import { parseApiError } from "../services/apiClient.js";

export default function ChatWidget({ currentUserId, defaultTargetUserId = null }) {
  const [conversations, setConversations] = useState([]);
  const [selectedConv, setSelectedConv] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const messagesEndRef = useRef(null);

  const loadConversations = async () => {
    try {
      const data = await getConversations();
      setConversations(data);
      if (defaultTargetUserId && !selectedConv) {
        // Find or create direct chat
        const conv = await getOrCreateDirectChat(defaultTargetUserId);
        setSelectedConv(conv);
      } else if (data.length > 0 && !selectedConv) {
        setSelectedConv(data[0]);
      }
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    loadConversations();
  }, [defaultTargetUserId]);

  const loadMessages = async (convId) => {
    if (!convId) return;
    try {
      const msgs = await getConversationMessages(convId);
      setMessages(msgs);
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    if (selectedConv) {
      loadMessages(selectedConv.id);
      const interval = setInterval(() => loadMessages(selectedConv.id), 8000);
      return () => clearInterval(interval);
    }
  }, [selectedConv]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() || !selectedConv) return;

    const text = newMessage.trim();
    setNewMessage("");
    try {
      const sent = await sendChatMessage(selectedConv.id, { content: text });
      setMessages((prev) => [...prev, sent]);
      loadConversations();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "280px 1fr",
        height: "600px",
        background: "#fff",
        borderRadius: "16px",
        border: "1px solid var(--e-line, #e2e9f3)",
        overflow: "hidden",
      }}
    >
      {/* Sidebar / Conversation List */}
      <div
        style={{
          borderRight: "1px solid var(--e-line, #e2e9f3)",
          display: "flex",
          flexDirection: "column",
          background: "#fafcff",
        }}
      >
        <div
          style={{
            padding: "16px",
            borderBottom: "1px solid var(--e-line, #e2e9f3)",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <strong style={{ fontSize: "0.95rem" }}>Messages</strong>
          <button
            type="button"
            className="text-action"
            onClick={loadConversations}
            title="Refresh conversations"
          >
            <Icon name="refresh" size={16} />
          </button>
        </div>

        <div style={{ flex: 1, overflowY: "auto" }}>
          {conversations.length === 0 ? (
            <div style={{ padding: "20px 16px", textAlign: "center", color: "#8795a9", fontSize: "0.85rem" }}>
              No conversations yet. Messages from team or mentors will show here.
            </div>
          ) : (
            conversations.map((c) => {
              const active = selectedConv && selectedConv.id === c.id;
              return (
                <div
                  key={c.id}
                  onClick={() => setSelectedConv(c)}
                  style={{
                    padding: "12px 14px",
                    borderBottom: "1px solid #f0f4f9",
                    cursor: "pointer",
                    background: active ? "var(--e-tint, #eaf1ff)" : "transparent",
                    borderLeft: active ? "3px solid #2563eb" : "3px solid transparent",
                    display: "flex",
                    flexDirection: "column",
                    gap: "4px",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <span style={{ fontWeight: 700, fontSize: "0.88rem", color: "#0f2747" }}>
                      {c.title}
                    </span>
                    {c.unreadCount > 0 && (
                      <span
                        style={{
                          background: "#2563eb",
                          color: "#fff",
                          borderRadius: "99px",
                          fontSize: "0.68rem",
                          fontWeight: 800,
                          padding: "1px 6px",
                        }}
                      >
                        {c.unreadCount}
                      </span>
                    )}
                  </div>
                  <span
                    style={{
                      fontSize: "0.78rem",
                      color: "#5b6d86",
                      overflow: "hidden",
                      textOverflow: "ellipsis",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {c.latestMessage ? c.latestMessage.content : "No messages yet"}
                  </span>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Main Chat Area */}
      <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
        {selectedConv ? (
          <>
            {/* Header */}
            <div
              style={{
                padding: "14px 20px",
                borderBottom: "1px solid var(--e-line, #e2e9f3)",
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                background: "#fff",
              }}
            >
              <div>
                <strong style={{ fontSize: "1rem" }}>{selectedConv.title}</strong>
                <div style={{ fontSize: "0.76rem", color: "#8795a9" }}>
                  {selectedConv.type === "TEAM" ? "Team Channel" : "Direct Conversation (Confidential)"}
                </div>
              </div>
            </div>

            {/* Error banner */}
            {error && <div style={{ padding: "8px 16px" }}><Alert type="error">{error}</Alert></div>}

            {/* Messages Thread */}
            <div
              style={{
                flex: 1,
                overflowY: "auto",
                padding: "20px",
                display: "flex",
                flexDirection: "column",
                gap: "12px",
                background: "#f9fbfe",
              }}
            >
              {messages.length === 0 ? (
                <div style={{ margin: "auto", color: "#8795a9", fontSize: "0.88rem" }}>
                  Start a conversation by typing a message below.
                </div>
              ) : (
                messages.map((m) => {
                  const isMine = m.senderId === currentUserId;
                  return (
                    <div
                      key={m.id}
                      style={{
                        alignSelf: isMine ? "flex-end" : "flex-start",
                        maxWidth: "70%",
                        display: "flex",
                        flexDirection: "column",
                        alignItems: isMine ? "flex-end" : "flex-start",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "0.72rem",
                          fontWeight: 700,
                          color: "#8795a9",
                          marginBottom: "2px",
                        }}
                      >
                        {isMine ? "You" : m.senderName}
                      </div>
                      <div
                        style={{
                          padding: "10px 14px",
                          borderRadius: isMine ? "14px 14px 2px 14px" : "14px 14px 14px 2px",
                          background: isMine ? "#2563eb" : "#fff",
                          color: isMine ? "#fff" : "#0f2747",
                          boxShadow: "0 1px 2px rgba(15,39,71,0.06)",
                          border: isMine ? "none" : "1px solid #e2e9f3",
                          fontSize: "0.88rem",
                          lineHeight: 1.45,
                          wordBreak: "break-word",
                        }}
                      >
                        {m.content}
                      </div>
                      <span style={{ fontSize: "0.68rem", color: "#8795a9", marginTop: "2px" }}>
                        {new Date(m.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                      </span>
                    </div>
                  );
                })
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Form */}
            <form
              onSubmit={handleSend}
              style={{
                padding: "14px 18px",
                borderTop: "1px solid var(--e-line, #e2e9f3)",
                display: "flex",
                gap: "10px",
                background: "#fff",
              }}
            >
              <input
                type="text"
                placeholder="Type a message..."
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                style={{
                  flex: 1,
                  padding: "10px 14px",
                  borderRadius: "10px",
                  border: "1.5px solid var(--e-line, #e2e9f3)",
                  fontSize: "0.9rem",
                  fontFamily: "inherit",
                }}
              />
              <button
                type="submit"
                className="btn btn--primary"
                disabled={!newMessage.trim()}
                style={{ padding: "0 18px" }}
              >
                <Icon name="send" size={16} /> Send
              </button>
            </form>
          </>
        ) : (
          <div style={{ margin: "auto", color: "#8795a9", fontSize: "0.95rem" }}>
            Select a conversation on the left to start messaging.
          </div>
        )}
      </div>
    </div>
  );
}
