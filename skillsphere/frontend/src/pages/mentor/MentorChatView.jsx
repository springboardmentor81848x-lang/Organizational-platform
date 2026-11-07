import React, { useState, useEffect } from "react";
import ChatWidget from "../../components/ChatWidget.jsx";
import { getMyMentees } from "../../services/mentorService.js";

export default function MentorChatView({ user }) {
  const [mentees, setMentees] = useState([]);

  useEffect(() => {
    getMyMentees("ACTIVE").then(setMentees).catch(() => {});
  }, []);

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="mentor-panel" style={{ padding: "16px 20px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h2 style={{ margin: 0, fontSize: "1.1rem" }}>Mentee Direct Messaging</h2>
            <p style={{ margin: "2px 0 0", fontSize: "0.84rem", color: "var(--mt-soft)" }}>
              Real-time asynchronous guidance and architectural support with authorized mentees.
            </p>
          </div>
          <div style={{ display: "flex", gap: "6px", alignItems: "center" }}>
            <span style={{ fontSize: "0.82rem", color: "var(--mt-soft)" }}>Active Mentee Channels:</span>
            {mentees.map((m) => (
              <span key={m.employeeId} className="mt-badge mt-badge--neutral">
                {m.employeeName}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div style={{ minHeight: "600px" }}>
        <ChatWidget currentUserId={user.id} />
      </div>
    </div>
  );
}
