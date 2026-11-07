import { useState, useEffect } from "react";
import Icon from "../../components/Icons.jsx";
import {
  createReassessmentRequest,
  recommendCourseToEmployee,
  referMentorToEmployee,
} from "../../services/managerService.js";
import { getAllSkills, getAllCourses } from "../../services/platformService.js";
import { parseApiError } from "../../services/apiClient.js";

export function ReassessmentModal({ isOpen, onClose, initialData = {}, teamMembers = [], onSuccess }) {
  const [employeeId, setEmployeeId] = useState(initialData.employeeId || "");
  const [skillId, setSkillId] = useState(initialData.skillId || "");
  const [reason, setReason] = useState("");
  const [skills, setSkills] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (initialData.employeeId) setEmployeeId(initialData.employeeId);
    if (initialData.skillId) setSkillId(initialData.skillId);
  }, [initialData]);

  useEffect(() => {
    if (isOpen) {
      getAllSkills().then(setSkills).catch(() => {});
      setError("");
      setReason("");
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!employeeId || !skillId || !reason.trim()) {
      setError("Please select an employee, skill, and provide a justification.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await createReassessmentRequest({
        employeeId: Number(employeeId),
        skillId: Number(skillId),
        reason: reason.trim(),
      });
      onSuccess("Reassessment request submitted successfully.");
      onClose();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <h2>Request Skill Reassessment</h2>
            <button type="button" style={{ border: 0, background: "transparent", cursor: "pointer" }} onClick={onClose}>
              <Icon name="x" size={20} />
            </button>
          </div>
          <div className="modal-body" style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            {error && (
              <div style={{ padding: "10px 14px", background: "#fee2e2", color: "#991b1b", borderRadius: "8px", fontSize: "0.85rem" }}>
                {error}
              </div>
            )}

            <div className="field">
              <span>Employee</span>
              <select value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} required>
                <option value="">Select an employee...</option>
                {teamMembers.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.fullName} ({m.jobRole} &bull; {m.teamName})
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <span>Target Skill</span>
              <select value={skillId} onChange={(e) => setSkillId(e.target.value)} required>
                <option value="">Select a skill to reassess...</option>
                {skills.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.categoryName || "General"})
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <span>Manager Justification / Performance Feedback</span>
              <textarea
                style={{
                  minHeight: "100px",
                  padding: "10px 12px",
                  borderRadius: "10px",
                  border: "1.5px solid #d7e1ef",
                  font: "inherit",
                  resize: "vertical",
                }}
                placeholder="Explain why this reassessment is required (e.g. project deliverables, recent training completion, or gap identified)..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                required
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn--ghost" onClick={onClose} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? "Submitting..." : "Submit Reassessment Request"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function CourseRecommendationModal({ isOpen, onClose, initialData = {}, teamMembers = [], onSuccess }) {
  const [employeeId, setEmployeeId] = useState(initialData.employeeId || "");
  const [courseId, setCourseId] = useState(initialData.courseId || "");
  const [skillId, setSkillId] = useState(initialData.skillId || "");
  const [reason, setReason] = useState("");
  const [courses, setCourses] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (initialData.employeeId) setEmployeeId(initialData.employeeId);
    if (initialData.courseId) setCourseId(initialData.courseId);
    if (initialData.skillId) setSkillId(initialData.skillId);
  }, [initialData]);

  useEffect(() => {
    if (isOpen) {
      getAllCourses().then(setCourses).catch(() => {});
      setError("");
      setReason("");
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!employeeId || !courseId || !reason.trim()) {
      setError("Please select an employee, course, and provide a recommendation note.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await recommendCourseToEmployee({
        employeeId: Number(employeeId),
        courseId: Number(courseId),
        skillId: skillId ? Number(skillId) : null,
        reason: reason.trim(),
      });
      onSuccess("Course recommended successfully.");
      onClose();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <h2>Recommend Course to Employee</h2>
            <button type="button" style={{ border: 0, background: "transparent", cursor: "pointer" }} onClick={onClose}>
              <Icon name="x" size={20} />
            </button>
          </div>
          <div className="modal-body" style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            {error && (
              <div style={{ padding: "10px 14px", background: "#fee2e2", color: "#991b1b", borderRadius: "8px", fontSize: "0.85rem" }}>
                {error}
              </div>
            )}

            <div className="field">
              <span>Employee</span>
              <select value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} required>
                <option value="">Select employee...</option>
                {teamMembers.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.fullName} ({m.jobRole} &bull; {m.teamName})
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <span>Verified Catalogue Course</span>
              <select value={courseId} onChange={(e) => setCourseId(e.target.value)} required>
                <option value="">Select course from database catalogue...</option>
                {courses.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.title} ({c.provider} &bull; {c.level})
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <span>Manager Recommendation Note</span>
              <textarea
                style={{
                  minHeight: "90px",
                  padding: "10px 12px",
                  borderRadius: "10px",
                  border: "1.5px solid #d7e1ef",
                  font: "inherit",
                  resize: "vertical",
                }}
                placeholder="Add guidance on why this course will benefit the employee and close their skill gap..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                required
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn--ghost" onClick={onClose} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? "Recommending..." : "Issue Recommendation"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function MentorReferralModal({ isOpen, onClose, initialData = {}, teamMembers = [], availableMentors = [], onSuccess }) {
  const [employeeId, setEmployeeId] = useState(initialData.employeeId || "");
  const [mentorId, setMentorId] = useState(initialData.mentorId || "");
  const [skillId, setSkillId] = useState(initialData.skillId || "");
  const [note, setNote] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (initialData.employeeId) setEmployeeId(initialData.employeeId);
    if (initialData.mentorId) setMentorId(initialData.mentorId);
    if (initialData.skillId) setSkillId(initialData.skillId);
  }, [initialData]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!employeeId || !mentorId || !note.trim()) {
      setError("Please select an employee, mentor, and provide a referral note.");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await referMentorToEmployee({
        employeeId: Number(employeeId),
        mentorId: Number(mentorId),
        skillId: skillId ? Number(skillId) : null,
        note: note.trim(),
      });
      onSuccess("Mentorship referral issued to both mentor and employee.");
      onClose();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
        <form onSubmit={handleSubmit}>
          <div className="modal-header">
            <h2>Refer Team Member to Mentor</h2>
            <button type="button" style={{ border: 0, background: "transparent", cursor: "pointer" }} onClick={onClose}>
              <Icon name="x" size={20} />
            </button>
          </div>
          <div className="modal-body" style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            {error && (
              <div style={{ padding: "10px 14px", background: "#fee2e2", color: "#991b1b", borderRadius: "8px", fontSize: "0.85rem" }}>
                {error}
              </div>
            )}

            <div className="field">
              <span>Employee</span>
              <select value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} required>
                <option value="">Select team employee...</option>
                {teamMembers.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.fullName} ({m.jobRole} &bull; {m.teamName})
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <span>Approved Technical Mentor</span>
              <select value={mentorId} onChange={(e) => setMentorId(e.target.value)} required>
                <option value="">Select approved mentor...</option>
                {availableMentors.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.fullName} ({m.expertiseAreas?.join(", ") || "General Mentorship"})
                  </option>
                ))}
              </select>
            </div>

            <div className="field">
              <span>Referral Note / Focus Areas</span>
              <textarea
                style={{
                  minHeight: "90px",
                  padding: "10px 12px",
                  borderRadius: "10px",
                  border: "1.5px solid #d7e1ef",
                  font: "inherit",
                  resize: "vertical",
                }}
                placeholder="Explain the developmental context and desired focus areas for this mentorship relationship..."
                value={note}
                onChange={(e) => setNote(e.target.value)}
                required
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn--ghost" onClick={onClose} disabled={submitting}>
              Cancel
            </button>
            <button type="submit" className="btn btn--primary" disabled={submitting}>
              {submitting ? "Referring..." : "Confirm Referral"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
