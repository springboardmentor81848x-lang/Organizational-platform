import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getAdminDepartments,
  createAdminDepartment,
  getAdminAllTeams,
  createAdminTeam,
  getAdminTeamMembers,
  addAdminTeamMember,
  removeAdminTeamMember,
  getAdminUsers,
} from "../../services/adminService.js";

export default function DepartmentTeamSetupView() {
  const [departments, setDepartments] = useState([]);
  const [teams, setTeams] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Create Dept Modal
  const [showDeptModal, setShowDeptModal] = useState(false);
  const [deptName, setDeptName] = useState("");
  const [deptCode, setDeptCode] = useState("");
  const [deptDesc, setDeptDesc] = useState("");
  const [deptHeadId, setDeptHeadId] = useState("");

  // Create Team Modal
  const [showTeamModal, setShowTeamModal] = useState(false);
  const [teamName, setTeamName] = useState("");
  const [teamDeptId, setTeamDeptId] = useState("");
  const [teamManagerId, setTeamManagerId] = useState("");

  // Team Members Modal
  const [selectedTeam, setSelectedTeam] = useState(null);
  const [teamMembers, setTeamMembers] = useState([]);
  const [newMemberUserId, setNewMemberUserId] = useState("");
  const [newMemberRole, setNewMemberRole] = useState("MEMBER");

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [deptList, teamList, userList] = await Promise.all([
        getAdminDepartments(),
        getAdminAllTeams(),
        getAdminUsers(),
      ]);
      setDepartments(deptList || []);
      setTeams(teamList || []);
      setUsers(userList || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load organizational structure.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateDept = async (e) => {
    e.preventDefault();
    if (!deptName || !deptCode) {
      setError("Department name and code are required.");
      return;
    }

    try {
      setError("");
      await createAdminDepartment({
        name: deptName.trim(),
        code: deptCode.trim().toUpperCase(),
        description: deptDesc.trim(),
        headUserId: deptHeadId ? Number(deptHeadId) : null,
      });

      setSuccess(`Department ${deptName} created successfully.`);
      setShowDeptModal(false);
      setDeptName("");
      setDeptCode("");
      setDeptDesc("");
      setDeptHeadId("");
      loadData();
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to create department.");
    }
  };

  const handleCreateTeam = async (e) => {
    e.preventDefault();
    if (!teamName || !teamDeptId) {
      setError("Team name and department are required.");
      return;
    }

    try {
      setError("");
      await createAdminTeam({
        name: teamName.trim(),
        departmentId: Number(teamDeptId),
        effectiveManagerUserId: teamManagerId ? Number(teamManagerId) : null,
      });

      setSuccess(`Team ${teamName} created successfully.`);
      setShowTeamModal(false);
      setTeamName("");
      setTeamDeptId("");
      setTeamManagerId("");
      loadData();
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to create team.");
    }
  };

  const openTeamMembersModal = async (team) => {
    setSelectedTeam(team);
    try {
      const members = await getAdminTeamMembers(team.id);
      setTeamMembers(members || []);
    } catch {
      setTeamMembers([]);
    }
  };

  const handleAddMember = async (e) => {
    e.preventDefault();
    if (!newMemberUserId || !selectedTeam) return;

    try {
      await addAdminTeamMember(selectedTeam.id, {
        userId: Number(newMemberUserId),
        roleInTeam: newMemberRole,
      });
      const members = await getAdminTeamMembers(selectedTeam.id);
      setTeamMembers(members || []);
      setNewMemberUserId("");
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to add member.");
    }
  };

  const handleRemoveMember = async (userId) => {
    if (!selectedTeam) return;
    try {
      await removeAdminTeamMember(selectedTeam.id, userId);
      const members = await getAdminTeamMembers(selectedTeam.id);
      setTeamMembers(members || []);
      loadData();
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to remove member.");
    }
  };

  return (
    <div>
      <div className="adm-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="adm-panel-title" style={{ margin: 0 }}>
              <Icon name="clipboard" size={18} />
              Organizational Infrastructure & Team Hierarchy
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--adm-soft)" }}>
              Maintain departments, functional teams, and membership assignments across the enterprise.
            </p>
          </div>

          <div style={{ display: "flex", gap: "10px" }}>
            <button className="adm-btn adm-btn-secondary" onClick={() => setShowDeptModal(true)}>
              <Icon name="plus" size={14} />
              New Department
            </button>
            <button className="adm-btn adm-btn-primary" onClick={() => setShowTeamModal(true)}>
              <Icon name="plus" size={14} />
              New Team
            </button>
          </div>
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}
      {success && <Alert type="success" message={success} />}

      {/* Departments Table */}
      <div className="adm-panel">
        <div className="adm-panel-header">
          <h3 className="adm-panel-title">
            <Icon name="shield" size={18} />
            Enterprise Departments ({departments.length})
          </h3>
        </div>

        <div className="adm-table-container">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Department Name</th>
                <th>Code</th>
                <th>Department Head</th>
                <th>Teams Count</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {departments.length > 0 ? (
                departments.map((d) => (
                  <tr key={d.id}>
                    <td>
                      <strong>{d.name}</strong>
                    </td>
                    <td>
                      <span className="adm-badge">{d.code}</span>
                    </td>
                    <td>
                      {d.headUserName ? (
                        <span style={{ fontWeight: "600", color: "var(--adm-ink)" }}>{d.headUserName}</span>
                      ) : (
                        <span style={{ color: "var(--adm-faint)" }}>Unassigned</span>
                      )}
                    </td>
                    <td>{d.teamCount ?? 0} Teams</td>
                    <td style={{ fontSize: "12.5px", color: "var(--adm-soft)" }}>{d.description || "—"}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} style={{ textAlign: "center", color: "var(--adm-faint)", padding: "20px" }}>
                    No departments created.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Teams Table */}
      <div className="adm-panel">
        <div className="adm-panel-header">
          <h3 className="adm-panel-title">
            <Icon name="users" size={18} />
            Functional Teams ({teams.length})
          </h3>
        </div>

        <div className="adm-table-container">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Team Name</th>
                <th>Department</th>
                <th>Team Manager / Lead</th>
                <th>Members</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {teams.length > 0 ? (
                teams.map((t) => (
                  <tr key={t.id}>
                    <td>
                      <strong>{t.name}</strong>
                    </td>
                    <td>
                      <span className="adm-badge">{t.departmentName}</span>
                    </td>
                    <td>
                      {t.managerName ? (
                        <span style={{ fontWeight: "600" }}>{t.managerName}</span>
                      ) : (
                        <span style={{ color: "var(--adm-rose)" }}>Unassigned Manager</span>
                      )}
                    </td>
                    <td>{t.memberCount ?? 0} Personnel</td>
                    <td>
                      <button
                        className="adm-btn adm-btn-secondary"
                        style={{ fontSize: "11.5px", padding: "4px 8px" }}
                        onClick={() => openTeamMembersModal(t)}
                      >
                        Manage Roster
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} style={{ textAlign: "center", color: "var(--adm-faint)", padding: "20px" }}>
                    No teams configured.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Department Modal */}
      {showDeptModal && (
        <div className="adm-modal-overlay" onClick={() => setShowDeptModal(false)}>
          <div className="adm-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px" }}>Create New Department</h3>
              <button style={{ background: "none", border: "none", cursor: "pointer" }} onClick={() => setShowDeptModal(false)}>
                <Icon name="x" size={20} />
              </button>
            </div>
            <form onSubmit={handleCreateDept}>
              <div style={{ marginBottom: "12px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Department Name *
                </label>
                <input
                  type="text"
                  className="adm-input"
                  style={{ width: "100%" }}
                  placeholder="e.g. Artificial Intelligence Research"
                  value={deptName}
                  onChange={(e) => setDeptName(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "12px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Code * (3-10 chars)
                  </label>
                  <input
                    type="text"
                    className="adm-input"
                    style={{ width: "100%" }}
                    placeholder="e.g. AI-RES"
                    value={deptCode}
                    onChange={(e) => setDeptCode(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Assign Department Head
                  </label>
                  <select
                    className="adm-select"
                    style={{ width: "100%" }}
                    value={deptHeadId}
                    onChange={(e) => setDeptHeadId(e.target.value)}
                  >
                    <option value="">Unassigned</option>
                    {users
                      .filter((u) => u.role === "DEPARTMENT_HEAD" || u.role === "ADMIN")
                      .map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.fullName} ({u.role})
                        </option>
                      ))}
                  </select>
                </div>
              </div>

              <div style={{ marginBottom: "20px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Description
                </label>
                <textarea
                  className="adm-input"
                  rows={3}
                  style={{ width: "100%", fontFamily: "inherit" }}
                  placeholder="Operational scope and mandate..."
                  value={deptDesc}
                  onChange={(e) => setDeptDesc(e.target.value)}
                />
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
                <button type="button" className="adm-btn adm-btn-secondary" onClick={() => setShowDeptModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="adm-btn adm-btn-primary">
                  Create Department
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Create Team Modal */}
      {showTeamModal && (
        <div className="adm-modal-overlay" onClick={() => setShowTeamModal(false)}>
          <div className="adm-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px" }}>Create Functional Team</h3>
              <button style={{ background: "none", border: "none", cursor: "pointer" }} onClick={() => setShowTeamModal(false)}>
                <Icon name="x" size={20} />
              </button>
            </div>
            <form onSubmit={handleCreateTeam}>
              <div style={{ marginBottom: "12px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Team Name *
                </label>
                <input
                  type="text"
                  className="adm-input"
                  style={{ width: "100%" }}
                  placeholder="e.g. Platform Architecture Team"
                  value={teamName}
                  onChange={(e) => setTeamName(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "20px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Department *
                  </label>
                  <select
                    className="adm-select"
                    style={{ width: "100%" }}
                    value={teamDeptId}
                    onChange={(e) => setTeamDeptId(e.target.value)}
                    required
                  >
                    <option value="">Select Department...</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name} ({d.code})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Team Manager / Lead
                  </label>
                  <select
                    className="adm-select"
                    style={{ width: "100%" }}
                    value={teamManagerId}
                    onChange={(e) => setTeamManagerId(e.target.value)}
                  >
                    <option value="">Unassigned Manager</option>
                    {users
                      .filter((u) => u.role === "MANAGER" || u.role === "DEPARTMENT_HEAD" || u.role === "ADMIN")
                      .map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.fullName} ({u.role})
                        </option>
                      ))}
                  </select>
                </div>
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
                <button type="button" className="adm-btn adm-btn-secondary" onClick={() => setShowTeamModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="adm-btn adm-btn-primary">
                  Create Team
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Team Roster Modal */}
      {selectedTeam && (
        <div className="adm-modal-overlay" onClick={() => setSelectedTeam(null)}>
          <div className="adm-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px" }}>Team Roster: {selectedTeam.name}</h3>
              <button style={{ background: "none", border: "none", cursor: "pointer" }} onClick={() => setSelectedTeam(null)}>
                <Icon name="x" size={20} />
              </button>
            </div>

            {/* Add Member Form */}
            <form onSubmit={handleAddMember} style={{ display: "flex", gap: "8px", marginBottom: "16px" }}>
              <select
                className="adm-select"
                style={{ flex: 1 }}
                value={newMemberUserId}
                onChange={(e) => setNewMemberUserId(e.target.value)}
                required
              >
                <option value="">Add employee to team...</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.fullName} ({u.email})
                  </option>
                ))}
              </select>

              <select
                className="adm-select"
                style={{ width: "130px" }}
                value={newMemberRole}
                onChange={(e) => setNewMemberRole(e.target.value)}
              >
                <option value="MEMBER">Member</option>
                <option value="LEAD">Lead</option>
                <option value="CONTRIBUTOR">Contributor</option>
              </select>

              <button type="submit" className="adm-btn adm-btn-primary" style={{ padding: "8px 12px" }}>
                Add
              </button>
            </form>

            {/* Members Table */}
            <div className="adm-table-container">
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>Personnel</th>
                    <th>Role in Team</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {teamMembers.length > 0 ? (
                    teamMembers.map((m) => (
                      <tr key={m.userId}>
                        <td>
                          <div style={{ fontWeight: "600" }}>{m.fullName}</div>
                          <div style={{ fontSize: "11px", color: "var(--adm-faint)" }}>{m.email}</div>
                        </td>
                        <td>
                          <span className="adm-badge">{m.roleInTeam || "MEMBER"}</span>
                        </td>
                        <td>
                          <button
                            className="adm-btn adm-btn-danger"
                            style={{ fontSize: "11px", padding: "3px 6px" }}
                            onClick={() => handleRemoveMember(m.userId)}
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={3} style={{ textAlign: "center", color: "var(--adm-faint)", padding: "16px" }}>
                        No members currently assigned to this team.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
