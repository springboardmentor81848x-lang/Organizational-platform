import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getMySupportTickets,
  createSupportTicket,
  closeSupportTicket,
  submitPlatformFeedback,
} from "../../services/platformService.js";
import { parseApiError } from "../../services/apiClient.js";

export default function HelpSupportSection() {
  const [tickets, setTickets] = useState([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Ticket form
  const [subject, setSubject] = useState("");
  const [desc, setDesc] = useState("");
  const [priority, setPriority] = useState("MEDIUM");

  // Feedback form
  const [category, setCategory] = useState("Assessments");
  const [rating, setRating] = useState(5);
  const [comments, setComments] = useState("");

  const loadTickets = async () => {
    try {
      const data = await getMySupportTickets();
      setTickets(data);
    } catch (e) {
      setError(parseApiError(e).message);
    }
  };

  useEffect(() => {
    loadTickets();
  }, []);

  const handleCreateTicket = async (e) => {
    e.preventDefault();
    if (!subject || !desc) return;
    setError(""); setSuccess("");
    try {
      await createSupportTicket({ subject, description: desc, priority });
      setSuccess("Support ticket submitted! An administrator will review it.");
      setSubject(""); setDesc("");
      loadTickets();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handleCloseTicket = async (ticketId) => {
    if (!window.confirm("Are you sure you want to mark this support ticket as closed?")) return;
    setError(""); setSuccess("");
    try {
      await closeSupportTicket(ticketId);
      setSuccess("Support ticket marked as closed.");
      loadTickets();
    } catch (err) { setError(parseApiError(err).message); }
  };

  const handleSubmitFeedback = async (e) => {
    e.preventDefault();
    if (!comments) return;
    setError(""); setSuccess("");
    try {
      await submitPlatformFeedback({ category, rating: Number(rating), comments });
      setSuccess("Thank you for your feedback! Your review helps us improve SkillSphere.");
      setComments("");
    } catch (err) { setError(parseApiError(err).message); }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
      <div className="section-heading">
        <div>
          <h2>Help, Support & Platform Feedback</h2>
          <p className="muted">Submit technical tickets to platform administrators or provide feature feedback.</p>
        </div>
      </div>

      {error && <Alert type="error">{error}</Alert>}
      {success && <Alert type="success">{success}</Alert>}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "20px" }}>
        {/* Support Ticket Form */}
        <div className="panel">
          <h3>Create Support Ticket</h3>
          <form onSubmit={handleCreateTicket} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "12px" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Subject</label>
              <input
                type="text"
                required
                placeholder="e.g. Assessment timer issue or course access"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
              />
            </div>
            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Priority</label>
              <select
                value={priority}
                onChange={(e) => setPriority(e.target.value)}
                style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
              >
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>
            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Description</label>
              <textarea
                rows={4}
                required
                placeholder="Please describe the issue or assistance needed..."
                value={desc}
                onChange={(e) => setDesc(e.target.value)}
                style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
              />
            </div>
            <button type="submit" className="btn btn--primary" style={{ alignSelf: "flex-start" }}>
              Submit Ticket
            </button>
          </form>
        </div>

        {/* Feedback Form */}
        <div className="panel">
          <h3>Share Platform Feedback</h3>
          <form onSubmit={handleSubmitFeedback} style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "12px" }}>
            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Category</label>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
              >
                <option value="Assessments">Assessments & Testing</option>
                <option value="Learning Paths">Learning Paths & Courses</option>
                <option value="Mentorship">Peer Mentorship</option>
                <option value="UI & UX">User Interface & Design</option>
                <option value="Other">General Feedback</option>
              </select>
            </div>
            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Rating (1 - 5 Stars)</label>
              <select
                value={rating}
                onChange={(e) => setRating(Number(e.target.value))}
                style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
              >
                <option value={5}>5 Stars - Excellent ⭐⭐⭐⭐⭐</option>
                <option value={4}>4 Stars - Good ⭐⭐⭐⭐</option>
                <option value={3}>3 Stars - Average ⭐⭐⭐</option>
                <option value={2}>2 Stars - Needs Improvement ⭐⭐</option>
                <option value={1}>1 Star - Poor ⭐</option>
              </select>
            </div>
            <div>
              <label style={{ display: "block", fontSize: "0.82rem", fontWeight: 700, marginBottom: "4px" }}>Your Comments</label>
              <textarea
                rows={4}
                required
                placeholder="What do you enjoy or what should we improve?"
                value={comments}
                onChange={(e) => setComments(e.target.value)}
                style={{ width: "100%", padding: "10px", borderRadius: "10px", border: "1.5px solid #e2e9f3" }}
              />
            </div>
            <button type="submit" className="btn btn--primary" style={{ alignSelf: "flex-start" }}>
              Submit Feedback
            </button>
          </form>
        </div>
      </div>

      {/* My Tickets History */}
      <div className="panel">
        <h3>My Submitted Tickets ({tickets.length})</h3>
        {tickets.length === 0 ? (
          <p className="muted">You have no active support tickets.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Ticket #</th>
                  <th>Subject</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Admin Notes</th>
                  <th>Submitted</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((t) => (
                  <tr key={t.id}>
                    <td>#{t.id}</td>
                    <td><strong>{t.subject}</strong><div style={{ fontSize: "0.82rem", color: "#5b6d86" }}>{t.description}</div></td>
                    <td><span className="badge">{t.priority}</span></td>
                    <td>
                      <span
                        className="badge"
                        style={{
                          background: t.status === "CLOSED" ? "#f1f5f9" : (t.status === "RESOLVED" ? "#e6f6ee" : "#fff4d6"),
                          color: t.status === "CLOSED" ? "#64748b" : (t.status === "RESOLVED" ? "#17754f" : "#8a5a00"),
                        }}
                      >
                        {t.status}
                      </span>
                    </td>
                    <td>{t.adminNotes || "—"}</td>
                    <td>{new Date(t.createdAt).toLocaleDateString()}</td>
                    <td>
                      {t.status !== "CLOSED" ? (
                        <button
                          type="button"
                          className="btn btn--ghost"
                          onClick={() => handleCloseTicket(t.id)}
                          style={{ fontSize: "0.78rem", padding: "4px 8px" }}
                        >
                          Close Ticket
                        </button>
                      ) : (
                        <span style={{ fontSize: "0.8rem", color: "#94a3b8" }}>Closed</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
