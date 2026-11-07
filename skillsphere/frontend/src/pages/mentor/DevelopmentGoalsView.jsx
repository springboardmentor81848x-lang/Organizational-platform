import React, { useState, useEffect } from "react";
import {
  getMyMentees,
  getMentorGoals,
  createMentorGoal,
  updateMentorGoal,
  createGoalActionItem,
  toggleGoalActionItem,
  deleteGoalActionItem,
  getAllSkills,
} from "../../services/mentorService.js";
import { parseApiError } from "../../services/apiClient.js";

const GOAL_STATUSES = ["IN_PROGRESS", "ACHIEVED", "COMPLETED", "ON_HOLD", "CANCELLED"];
const PRIORITIES = ["HIGH", "MEDIUM", "LOW"];

export default function DevelopmentGoalsView() {
  const [mentees, setMentees] = useState([]);
  const [selectedMenteeFilter, setSelectedMenteeFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [goals, setGoals] = useState([]);
  const [skills, setSkills] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Create Goal Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [assignmentId, setAssignmentId] = useState("");
  const [goalTitle, setGoalTitle] = useState("");
  const [goalDesc, setGoalDesc] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [priority, setPriority] = useState("MEDIUM");
  const [skillId, setSkillId] = useState("");
  const [submittingGoal, setSubmittingGoal] = useState(false);

  // Add Action Item State
  const [activeGoalForAction, setActiveGoalForAction] = useState(null);
  const [actionTitle, setActionTitle] = useState("");
  const [actionDueDate, setActionDueDate] = useState("");
  const [submittingAction, setSubmittingAction] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const [mList, gList, sList] = await Promise.all([
        getMyMentees("ACTIVE"),
        getMentorGoals({
          menteeId: selectedMenteeFilter ? Number(selectedMenteeFilter) : undefined,
          status: statusFilter !== "ALL" ? statusFilter : undefined,
        }),
        getAllSkills().catch(() => []),
      ]);
      setMentees(mList);
      setGoals(gList);
      setSkills(sList);
      if (mList.length > 0 && !assignmentId) {
        setAssignmentId(mList[0].assignmentId);
      }
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [selectedMenteeFilter, statusFilter]);

  const handleCreateGoal = async (e) => {
    e.preventDefault();
    if (!assignmentId || !goalTitle) return;
    setSubmittingGoal(true);
    setError("");
    setSuccess("");
    try {
      await createMentorGoal({
        assignmentId: Number(assignmentId),
        title: goalTitle,
        description: goalDesc,
        targetDate: targetDate || null,
        priority,
        skillId: skillId ? Number(skillId) : null,
      });
      setSuccess("Development goal successfully established!");
      setShowCreateModal(false);
      setGoalTitle("");
      setGoalDesc("");
      setTargetDate("");
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmittingGoal(false);
    }
  };

  const handleUpdateStatus = async (goalId, newStatus) => {
    try {
      await updateMentorGoal(goalId, { status: newStatus });
      setSuccess(`Goal status updated to ${newStatus}`);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleCreateActionItem = async (e) => {
    e.preventDefault();
    if (!activeGoalForAction || !actionTitle) return;
    setSubmittingAction(true);
    try {
      await createGoalActionItem(activeGoalForAction.id, {
        title: actionTitle,
        dueDate: actionDueDate || null,
      });
      setActionTitle("");
      setActionDueDate("");
      setActiveGoalForAction(null);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleToggleAction = async (itemId) => {
    try {
      await toggleGoalActionItem(itemId);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  const handleDeleteAction = async (itemId) => {
    try {
      await deleteGoalActionItem(itemId);
      loadData();
    } catch (err) {
      setError(parseApiError(err).message);
    }
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "24px" }}>
      <div className="mentor-panel">
        <div className="mentor-panel-header">
          <div>
            <h2>Mentorship Development Goals</h2>
            <p style={{ color: "var(--mt-soft)", margin: "4px 0 0", fontSize: "0.88rem" }}>
              Collaboratively set targets, milestones, and track skill mastery.
            </p>
          </div>

          <button
            type="button"
            className="mt-btn mt-btn--primary"
            onClick={() => setShowCreateModal(true)}
          >
            + Create Goal
          </button>
        </div>

        {/* Filter Controls */}
        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", alignItems: "center", marginBottom: "16px" }}>
          <select
            className="mt-select"
            style={{ width: "240px" }}
            value={selectedMenteeFilter}
            onChange={(e) => setSelectedMenteeFilter(e.target.value)}
          >
            <option value="">All Mentees</option>
            {mentees.map((m) => (
              <option key={m.employeeId} value={m.employeeId}>
                {m.employeeName}
              </option>
            ))}
          </select>

          <div className="mentor-filter-bar">
            <button
              type="button"
              className={`mentor-pill-btn ${statusFilter === "ALL" ? "is-active" : ""}`}
              onClick={() => setStatusFilter("ALL")}
            >
              All Statuses
            </button>
            <button
              type="button"
              className={`mentor-pill-btn ${statusFilter === "IN_PROGRESS" ? "is-active" : ""}`}
              onClick={() => setStatusFilter("IN_PROGRESS")}
            >
              In Progress
            </button>
            <button
              type="button"
              className={`mentor-pill-btn ${statusFilter === "ACHIEVED" ? "is-active" : ""}`}
              onClick={() => setStatusFilter("ACHIEVED")}
            >
              Achieved / Completed
            </button>
          </div>
        </div>

        {error && <div style={{ padding: "12px", background: "var(--mt-bad-tint)", color: "var(--mt-bad)", borderRadius: "8px", marginBottom: "16px" }}>{error}</div>}
        {success && <div style={{ padding: "12px", background: "var(--mt-ok-tint)", color: "var(--mt-ok)", borderRadius: "8px", marginBottom: "16px" }}>{success}</div>}

        {loading ? (
          <p style={{ color: "var(--mt-soft)" }}>Loading development goals...</p>
        ) : goals.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 20px", color: "var(--mt-soft)" }}>
            <p style={{ fontSize: "1.05rem", fontWeight: 600 }}>No development goals found.</p>
            <p style={{ fontSize: "0.86rem", color: "var(--mt-faint)" }}>Click "+ Create Goal" to establish milestone targets with your mentees.</p>
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
            {goals.map((g) => {
              const isDone = g.status === "ACHIEVED" || g.status === "COMPLETED";

              return (
                <div
                  key={g.id}
                  style={{
                    padding: "20px",
                    borderRadius: "10px",
                    border: "1px solid var(--mt-line)",
                    background: isDone ? "#f8fafc" : "#ffffff",
                    display: "flex",
                    flexDirection: "column",
                    gap: "12px",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", flexWrap: "wrap", gap: "10px" }}>
                    <div>
                      <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                        <strong style={{ fontSize: "1.05rem" }}>{g.title}</strong>
                        <span className={`mt-badge ${isDone ? "mt-badge--active" : "mt-badge--pending"}`}>
                          {g.status}
                        </span>
                        <span className="mt-badge mt-badge--neutral">
                          Priority: {g.priority}
                        </span>
                      </div>
                      <small style={{ color: "var(--mt-soft)", display: "block", marginTop: "2px" }}>
                        Assigned to: <strong>{g.employeeName}</strong> &bull; Target Date: {g.targetDate ? new Date(g.targetDate).toLocaleDateString() : "Flexible"}
                        {g.skillName && ` • Skill: ${g.skillName}`}
                      </small>
                    </div>

                    <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                      <select
                        className="mt-select"
                        style={{ fontSize: "0.82rem", padding: "6px 10px" }}
                        value={g.status}
                        onChange={(e) => handleUpdateStatus(g.id, e.target.value)}
                      >
                        {GOAL_STATUSES.map((st) => (
                          <option key={st} value={st}>
                            {st}
                          </option>
                        ))}
                      </select>

                      <button
                        type="button"
                        className="mt-btn mt-btn--outline"
                        style={{ fontSize: "0.8rem", padding: "6px 12px" }}
                        onClick={() => setActiveGoalForAction(g)}
                      >
                        + Add Milestone
                      </button>
                    </div>
                  </div>

                  {g.description && (
                    <p style={{ margin: 0, fontSize: "0.88rem", color: "var(--mt-soft)" }}>
                      {g.description}
                    </p>
                  )}

                  {/* Action Items List */}
                  {g.actionItems && g.actionItems.length > 0 && (
                    <div style={{ borderTop: "1px solid var(--mt-line)", paddingTop: "12px", display: "flex", flexDirection: "column", gap: "8px" }}>
                      <span style={{ fontSize: "0.78rem", fontWeight: 700, color: "var(--mt-faint)", textTransform: "uppercase" }}>
                        Action Items & Milestones ({g.actionItems.filter((i) => i.isCompleted).length} / {g.actionItems.length})
                      </span>
                      {g.actionItems.map((item) => (
                        <div
                          key={item.id}
                          className={`mt-checklist-item ${item.isCompleted ? "is-done" : ""}`}
                        >
                          <input
                            type="checkbox"
                            checked={item.isCompleted}
                            onChange={() => handleToggleAction(item.id)}
                            style={{ width: "16px", height: "16px", marginTop: "2px", cursor: "pointer" }}
                          />
                          <div style={{ flex: 1 }}>
                            <span style={{ textDecoration: item.isCompleted ? "line-through" : "none", fontSize: "0.86rem", color: item.isCompleted ? "var(--mt-faint)" : "var(--mt-ink)" }}>
                              {item.title}
                            </span>
                            {item.dueDate && (
                              <small style={{ display: "block", color: "var(--mt-soft)", fontSize: "0.76rem" }}>
                                Due: {new Date(item.dueDate).toLocaleDateString()}
                              </small>
                            )}
                          </div>
                          <button
                            type="button"
                            style={{ background: "transparent", border: "none", color: "var(--mt-faint)", cursor: "pointer", fontSize: "1rem" }}
                            onClick={() => handleDeleteAction(item.id)}
                            title="Delete milestone"
                          >
                            &times;
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Create Goal Modal */}
      {showCreateModal && (
        <div className="mt-modal-backdrop" onClick={() => setShowCreateModal(false)}>
          <div className="mt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>Create Mentorship Goal</h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setShowCreateModal(false)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateGoal}>
              <div className="mt-modal-body">
                <div className="mt-form-group">
                  <label className="mt-label">Mentee Assignment *</label>
                  <select
                    className="mt-select"
                    required
                    value={assignmentId}
                    onChange={(e) => setAssignmentId(e.target.value)}
                  >
                    {mentees.map((m) => (
                      <option key={m.assignmentId} value={m.assignmentId}>
                        {m.employeeName} ({m.jobRole})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Goal Title *</label>
                  <input
                    type="text"
                    required
                    className="mt-input"
                    value={goalTitle}
                    onChange={(e) => setGoalTitle(e.target.value)}
                    placeholder="e.g. Master Production Microservice Observability"
                  />
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Description & Success Criteria</label>
                  <textarea
                    rows={3}
                    className="mt-textarea"
                    value={goalDesc}
                    onChange={(e) => setGoalDesc(e.target.value)}
                    placeholder="Describe target outcomes and evaluation criteria..."
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "12px" }}>
                  <div className="mt-form-group">
                    <label className="mt-label">Priority</label>
                    <select
                      className="mt-select"
                      value={priority}
                      onChange={(e) => setPriority(e.target.value)}
                    >
                      {PRIORITIES.map((p) => (
                        <option key={p} value={p}>
                          {p}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="mt-form-group">
                    <label className="mt-label">Target Date</label>
                    <input
                      type="date"
                      className="mt-input"
                      value={targetDate}
                      onChange={(e) => setTargetDate(e.target.value)}
                    />
                  </div>

                  <div className="mt-form-group">
                    <label className="mt-label">Focus Skill (Optional)</label>
                    <select
                      className="mt-select"
                      value={skillId}
                      onChange={(e) => setSkillId(e.target.value)}
                    >
                      <option value="">None</option>
                      {skills.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>

              <div className="mt-modal-footer">
                <button
                  type="button"
                  className="mt-btn mt-btn--outline"
                  onClick={() => setShowCreateModal(false)}
                  disabled={submittingGoal}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="mt-btn mt-btn--primary"
                  disabled={submittingGoal}
                >
                  {submittingGoal ? "Saving..." : "Create Goal"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Add Milestone Modal */}
      {activeGoalForAction && (
        <div className="mt-modal-backdrop" onClick={() => setActiveGoalForAction(null)}>
          <div className="mt-modal" onClick={(e) => e.stopPropagation()}>
            <div className="mt-modal-header">
              <h3 style={{ margin: 0 }}>Add Milestone to: {activeGoalForAction.title}</h3>
              <button
                type="button"
                className="mt-btn mt-btn--outline"
                style={{ padding: "4px 8px" }}
                onClick={() => setActiveGoalForAction(null)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateActionItem}>
              <div className="mt-modal-body">
                <div className="mt-form-group">
                  <label className="mt-label">Milestone / Action Item Title *</label>
                  <input
                    type="text"
                    required
                    className="mt-input"
                    value={actionTitle}
                    onChange={(e) => setActionTitle(e.target.value)}
                    placeholder="e.g. Set up distributed tracing with OpenTelemetry"
                  />
                </div>

                <div className="mt-form-group">
                  <label className="mt-label">Due Date (Optional)</label>
                  <input
                    type="date"
                    className="mt-input"
                    value={actionDueDate}
                    onChange={(e) => setActionDueDate(e.target.value)}
                  />
                </div>
              </div>

              <div className="mt-modal-footer">
                <button
                  type="button"
                  className="mt-btn mt-btn--outline"
                  onClick={() => setActiveGoalForAction(null)}
                  disabled={submittingAction}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="mt-btn mt-btn--primary"
                  disabled={submittingAction}
                >
                  {submittingAction ? "Adding..." : "Add Milestone"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
