import { useEffect, useState } from "react";
import Icon from "./Icons.jsx";
import Alert from "./Alert.jsx";
import {
  getCalendarEvents,
  createCalendarEvent,
  rsvpCalendarEvent,
} from "../services/platformService.js";
import { parseApiError } from "../services/apiClient.js";

export default function CalendarWidget({ currentUserId, showCreateForm = true }) {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showModal, setShowModal] = useState(false);

  // Form state
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [eventType, setEventType] = useState("MENTORSHIP");
  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState("10:00");
  const [endTime, setEndTime] = useState("11:00");
  const [locationOrLink, setLocationOrLink] = useState("");

  const loadEvents = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await getCalendarEvents();
      setEvents(data);
    } catch (e) {
      setError(parseApiError(e).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEvents();
  }, []);

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!title || !date) return;

    try {
      const startIso = new Date(`${date}T${startTime}:00`).toISOString();
      const endIso = new Date(`${date}T${endTime}:00`).toISOString();

      await createCalendarEvent({
        title,
        description,
        eventType,
        startTime: startIso,
        endTime: endIso,
        locationOrLink,
        participantUserIds: [],
      });

      setShowModal(false);
      setTitle("");
      setDescription("");
      setLocationOrLink("");
      loadEvents();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleRsvp = async (id, status) => {
    try {
      await rsvpCalendarEvent(id, status);
      loadEvents();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const typeColor = (type) => {
    switch (type) {
      case "ASSESSMENT": return "#b42318";
      case "MENTORSHIP": return "#2563eb";
      case "TRAINING": return "#7c3aed";
      case "TEAM": return "#17754f";
      default: return "#5b6d86";
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <h2 style={{ margin: 0, fontSize: "1.15rem", fontWeight: 800 }}>Schedule & Calendar</h2>
          <p style={{ margin: "2px 0 0", fontSize: "0.86rem", color: "#5b6d86" }}>
            Keep track of assessments, mentoring check-ins, and team sessions.
          </p>
        </div>
        {showCreateForm && (
          <button
            type="button"
            className="btn btn--primary"
            onClick={() => setShowModal(true)}
            style={{ fontSize: "0.84rem", padding: "8px 16px" }}
          >
            <Icon name="plus" size={16} /> Schedule Event
          </button>
        )}
      </div>

      {error && <Alert type="error">{error}</Alert>}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(310px, 1fr))", gap: "14px" }}>
        {events.length === 0 ? (
          <div className="panel" style={{ gridColumn: "1/-1", textAlign: "center", padding: "32px", color: "#8795a9" }}>
            No upcoming events scheduled. Create one to get started!
          </div>
        ) : (
          events.map((ev) => {
            const start = new Date(ev.startTime);
            const end = new Date(ev.endTime);
            return (
              <div
                key={ev.id}
                className="panel"
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  borderLeft: `4px solid ${typeColor(ev.eventType)}`,
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                  <span
                    className="badge"
                    style={{
                      background: `${typeColor(ev.eventType)}15`,
                      color: typeColor(ev.eventType),
                    }}
                  >
                    {ev.eventType}
                  </span>
                  <span style={{ fontSize: "0.76rem", color: "#8795a9", fontWeight: 600 }}>
                    By {ev.creatorName}
                  </span>
                </div>

                <strong style={{ fontSize: "1rem", color: "#0f2747" }}>{ev.title}</strong>
                {ev.description && (
                  <p style={{ margin: 0, fontSize: "0.84rem", color: "#5b6d86" }}>{ev.description}</p>
                )}

                <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "0.82rem", color: "#5b6d86" }}>
                  <Icon name="clock" size={16} />
                  <span>
                    {start.toLocaleDateString(undefined, { month: "short", day: "numeric", weekday: "short" })},{" "}
                    {start.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })} –{" "}
                    {end.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                  </span>
                </div>

                {ev.locationOrLink && (
                  <div style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "0.82rem", color: "#2563eb" }}>
                    <Icon name="external" size={16} />
                    <a href={ev.locationOrLink.startsWith("http") ? ev.locationOrLink : `https://${ev.locationOrLink}`} target="_blank" rel="noreferrer" style={{ color: "#2563eb", textDecoration: "none" }}>
                      {ev.locationOrLink}
                    </a>
                  </div>
                )}

                {ev.participants && ev.participants.some((p) => p.userId === currentUserId) && (
                  <div style={{ marginTop: "6px", display: "flex", gap: "8px" }}>
                    <button
                      type="button"
                      className="btn btn--complete"
                      onClick={() => handleRsvp(ev.id, "ACCEPTED")}
                      style={{ padding: "4px 10px", fontSize: "0.78rem" }}
                    >
                      Accept
                    </button>
                    <button
                      type="button"
                      className="btn btn--ghost"
                      onClick={() => handleRsvp(ev.id, "DECLINED")}
                      style={{ padding: "4px 10px", fontSize: "0.78rem" }}
                    >
                      Decline
                    </button>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {showModal && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(11,37,69,0.45)",
            backdropFilter: "blur(4px)",
            display: "grid",
            placeItems: "center",
            zIndex: 999,
            padding: "20px",
          }}
        >
          <div
            className="panel"
            style={{ width: "100%", maxWidth: "480px", position: "relative" }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h2 style={{ margin: 0, fontSize: "1.1rem" }}>Schedule New Event</h2>
              <button
                type="button"
                className="btn btn--ghost"
                onClick={() => setShowModal(false)}
                style={{ padding: "4px 8px" }}
              >
                <Icon name="x" size={18} />
              </button>
            </div>

            <form onSubmit={handleCreate} style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>
                  Event Title
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Java Skill Gap Review"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>
                  Event Type
                </label>
                <select
                  value={eventType}
                  onChange={(e) => setEventType(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                >
                  <option value="MENTORSHIP">Mentorship Session</option>
                  <option value="ASSESSMENT">Assessment Deadline</option>
                  <option value="TRAINING">Training Workshop</option>
                  <option value="TEAM">Team Sync</option>
                </select>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "8px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Date</label>
                  <input
                    type="date"
                    required
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    style={{ width: "100%", padding: "8px 8px", borderRadius: "8px", border: "1.5px solid #e2e9f3", fontSize: "0.85rem" }}
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Start</label>
                  <input
                    type="time"
                    required
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    style={{ width: "100%", padding: "8px 8px", borderRadius: "8px", border: "1.5px solid #e2e9f3", fontSize: "0.85rem" }}
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>End</label>
                  <input
                    type="time"
                    required
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    style={{ width: "100%", padding: "8px 8px", borderRadius: "8px", border: "1.5px solid #e2e9f3", fontSize: "0.85rem" }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Meeting Link / Location</label>
                <input
                  type="text"
                  placeholder="https://meet.google.com/..."
                  value={locationOrLink}
                  onChange={(e) => setLocationOrLink(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                />
              </div>

              <div>
                <label style={{ display: "block", fontSize: "0.8rem", fontWeight: 700, marginBottom: "4px" }}>Description</label>
                <textarea
                  rows={3}
                  placeholder="Objectives, agenda, prep..."
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  style={{ width: "100%", padding: "8px 12px", borderRadius: "8px", border: "1.5px solid #e2e9f3" }}
                />
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "10px" }}>
                <button type="button" className="btn btn--ghost" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn--primary">
                  Save Event
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
