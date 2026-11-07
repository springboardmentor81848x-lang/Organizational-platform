import React, { useState, useEffect } from "react";
import { updateMentorProfile } from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

const DAYS_OF_WEEK = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const FORMATS = ["VIRTUAL", "IN_PERSON", "HYBRID"];

export default function MentorProfileView({ profile, onProfileUpdated }) {
  const [headline, setHeadline] = useState("");
  const [bio, setBio] = useState("");
  const [expertiseAreas, setExpertiseAreas] = useState("");
  const [maxMentees, setMaxMentees] = useState(3);
  const [isActive, setIsActive] = useState(true);
  const [preferredFormat, setPreferredFormat] = useState("VIRTUAL");
  const [slots, setSlots] = useState([]);

  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState("");
  const [err, setErr] = useState("");

  useEffect(() => {
    if (profile) {
      setHeadline(profile.headline || "");
      setBio(profile.bio || "");
      setExpertiseAreas(profile.expertiseAreas || "");
      setMaxMentees(profile.maxMentees || 3);
      setIsActive(profile.isActive ?? true);
      setPreferredFormat(profile.preferredFormat || "VIRTUAL");
      setSlots(profile.availabilitySlots || []);
    }
  }, [profile]);

  const handleAddSlot = () => {
    setSlots([
      ...slots,
      {
        dayOfWeek: "MONDAY",
        startTime: "09:00",
        endTime: "10:00",
        sessionFormat: preferredFormat,
        isAvailable: true,
      },
    ]);
  };

  const handleUpdateSlot = (idx, field, val) => {
    const updated = [...slots];
    updated[idx] = { ...updated[idx], [field]: val };
    setSlots(updated);
  };

  const handleRemoveSlot = (idx) => {
    setSlots(slots.filter((_, i) => i !== idx));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setMsg("");
    setErr("");
    try {
      const payload = {
        headline,
        bio,
        expertiseAreas,
        maxMentees: Number(maxMentees),
        isActive,
        preferredFormat,
        availabilitySlots: slots,
      };
      const updated = await updateMentorProfile(payload);
      setMsg("Mentor profile and schedule updated successfully!");
      if (onProfileUpdated) onProfileUpdated(updated);
    } catch (ex) {
      setErr(parseApiError(ex).message);
    } finally {
      setSaving(false);
    }
  };

  if (!profile) return <div className="mentor-panel">Loading profile...</div>;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px", maxWidth: "900px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Mentor Profile & Availability</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Manage your technical advisor credentials, mentee capacity, and weekly mentoring schedule.
            </p>
          </div>
          <span className={`mt-badge ${isActive ? "mt-badge--active" : "mt-badge--neutral"}`}>
            {isActive ? "Accepting Mentees" : "Unavailable"}
          </span>
        </div>

        {msg && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{msg}</div>}
        {err && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{err}</div>}

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
          {/* Identity Info */}
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
            <div className="mt-form-group">
              <label className="mt-label">Full Name</label>
              <input type="text" className="mt-input" value={profile.fullName} disabled style={{ background: "#f8fafc" }} />
            </div>
            <div className="mt-form-group">
              <label className="mt-label">Corporate Email</label>
              <input type="email" className="mt-input" value={profile.email} disabled style={{ background: "#f8fafc" }} />
            </div>
          </div>

          <div className="mt-form-group">
            <label className="mt-label">Professional Headline *</label>
            <input
              type="text"
              required
              className="mt-input"
              value={headline}
              onChange={(e) => setHeadline(e.target.value)}
              placeholder="e.g. Principal Cloud Architect & Enterprise Java Specialist"
            />
          </div>

          <div className="mt-form-group">
            <label className="mt-label">Mentor Biography & Focus Areas</label>
            <textarea
              rows={4}
              className="mt-textarea"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder="Describe your engineering experience, mentoring style, and topics you specialize in..."
            />
          </div>

          <div className="mt-form-group">
            <label className="mt-label">Expertise Areas (comma-separated tags) *</label>
            <input
              type="text"
              required
              className="mt-input"
              value={expertiseAreas}
              onChange={(e) => setExpertiseAreas(e.target.value)}
              placeholder="e.g. Spring Boot, Distributed Systems, Cloud Architecture, PostgreSQL, DevOps"
            />
            {profile.expertiseTags && profile.expertiseTags.length > 0 && (
              <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "8px" }}>
                {profile.expertiseTags.map((tag, i) => (
                  <span key={i} className="mt-badge mt-badge--neutral">
                    {tag}
                  </span>
                ))}
              </div>
            )}
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "16px" }}>
            <div className="mt-form-group">
              <label className="mt-label">Maximum Mentees Capacity *</label>
              <input
                type="number"
                min={1}
                max={15}
                required
                className="mt-input"
                value={maxMentees}
                onChange={(e) => setMaxMentees(e.target.value)}
              />
              <small style={{ color: "var(--mt-faint)" }}>Currently paired: {profile.activeMenteesCount}</small>
            </div>

            <div className="mt-form-group">
              <label className="mt-label">Preferred Session Format</label>
              <select
                className="mt-select"
                value={preferredFormat}
                onChange={(e) => setPreferredFormat(e.target.value)}
              >
                {FORMATS.map((f) => (
                  <option key={f} value={f}>
                    {f}
                  </option>
                ))}
              </select>
            </div>

            <div className="mt-form-group">
              <label className="mt-label">Availability Status</label>
              <label style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "10px", cursor: "pointer" }}>
                <input
                  type="checkbox"
                  checked={isActive}
                  onChange={(e) => setIsActive(e.target.checked)}
                  style={{ width: "18px", height: "18px" }}
                />
                <span style={{ fontSize: "0.88rem", fontWeight: 600 }}>Accept New Requests</span>
              </label>
            </div>
          </div>

          {/* Weekly Schedule Slots */}
          <div style={{ borderTop: "1px solid var(--mt-line)", paddingTop: "20px" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "14px" }}>
              <div>
                <h3 style={{ margin: 0, fontSize: "1.05rem" }}>Weekly Mentoring Availability Slots</h3>
                <small style={{ color: "var(--mt-soft)" }}>Defined office hours for 1-on-1 mentee sessions</small>
              </div>
              <button type="button" className="mt-btn mt-btn--outline" onClick={handleAddSlot}>
                + Add Time Slot
              </button>
            </div>

            {slots.length === 0 ? (
              <p style={{ color: "var(--mt-soft)", fontStyle: "italic", background: "#f8fafc", padding: "14px", borderRadius: "8px" }}>
                No time slots specified. Add standard hours to help mentees schedule sessions easily.
              </p>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                {slots.map((s, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: "grid",
                      gridTemplateColumns: "140px 110px 110px 120px 40px",
                      gap: "10px",
                      alignItems: "center",
                      background: "#f8fafc",
                      padding: "10px 14px",
                      borderRadius: "8px",
                      border: "1px solid var(--mt-line)",
                    }}
                  >
                    <select
                      className="mt-select"
                      value={s.dayOfWeek}
                      onChange={(e) => handleUpdateSlot(idx, "dayOfWeek", e.target.value)}
                    >
                      {DAYS_OF_WEEK.map((d) => (
                        <option key={d} value={d}>
                          {d}
                        </option>
                      ))}
                    </select>
                    <input
                      type="time"
                      className="mt-input"
                      value={s.startTime}
                      onChange={(e) => handleUpdateSlot(idx, "startTime", e.target.value)}
                    />
                    <input
                      type="time"
                      className="mt-input"
                      value={s.endTime}
                      onChange={(e) => handleUpdateSlot(idx, "endTime", e.target.value)}
                    />
                    <select
                      className="mt-select"
                      value={s.sessionFormat || "VIRTUAL"}
                      onChange={(e) => handleUpdateSlot(idx, "sessionFormat", e.target.value)}
                    >
                      {FORMATS.map((f) => (
                        <option key={f} value={f}>
                          {f}
                        </option>
                      ))}
                    </select>
                    <button
                      type="button"
                      className="mt-btn mt-btn--danger"
                      style={{ padding: "6px", width: "32px", height: "32px" }}
                      onClick={() => handleRemoveSlot(idx)}
                      title="Remove slot"
                    >
                      &times;
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <button
            type="submit"
            className="mt-btn mt-btn--primary"
            style={{ alignSelf: "flex-start", padding: "10px 24px" }}
            disabled={saving}
          >
            {saving ? "Saving Changes..." : "Save Profile & Schedule"}
          </button>
        </form>
      </div>
    </div>
  );
}
