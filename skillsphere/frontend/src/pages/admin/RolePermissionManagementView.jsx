import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getAdminRolesOverview,
  assignAdminUserRole,
  getAdminRoleHistory,
  getAdminUsers,
} from "../../services/adminService.js";

const ALL_ROLES = [
  "EMPLOYEE",
  "MANAGER",
  "MENTOR",
  "HR_SPECIALIST",
  "DEPARTMENT_HEAD",
  "ADMIN",
];

export default function RolePermissionManagementView() {
  const [roles, setRoles] = useState([]);
  const [history, setHistory] = useState([]);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Role Assignment State
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedRole, setSelectedRole] = useState("EMPLOYEE");
  const [assigning, setAssigning] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError("");
      const [rOverview, rHistory, uList] = await Promise.all([
        getAdminRolesOverview(),
        getAdminRoleHistory(),
        getAdminUsers(),
      ]);
      setRoles(rOverview || []);
      setHistory(rHistory || []);
      setUsers(uList || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load roles and permissions.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRoleAssignSubmit = async () => {
    if (!selectedUserId) {
      setError("Please select a user to assign.");
      return;
    }

    try {
      setAssigning(true);
      setError("");
      const targetUser = users.find((u) => u.id === Number(selectedUserId));

      await assignAdminUserRole(selectedUserId, selectedRole);
      setSuccess(`Successfully reassigned role of ${targetUser ? targetUser.email : "user"} to ${selectedRole}.`);
      setShowConfirm(false);
      setSelectedUserId("");
      loadData();
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to assign role.");
      setShowConfirm(false);
    } finally {
      setAssigning(false);
    }
  };

  return (
    <div>
      <div className="adm-panel">
        <h3 className="adm-panel-title">
          <Icon name="shield" size={18} />
          Role-Based Access Control (RBAC) Governance
        </h3>
        <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--adm-soft)" }}>
          SkillSphere enforces a strict 6-role permission matrix backed by Spring Security method authorization (@PreAuthorize).
        </p>
      </div>

      {error && <Alert type="danger" message={error} />}
      {success && <Alert type="success" message={success} />}

      {/* Role Cards Grid */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: "16px", marginBottom: "24px" }}>
        {roles.map((r) => (
          <div key={r.role} className="adm-panel" style={{ marginBottom: 0 }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "8px" }}>
              <div>
                <span className="adm-badge">{r.role}</span>
                <h4 style={{ margin: "6px 0 2px", fontSize: "16px", color: "var(--adm-ink)" }}>
                  {r.label}
                </h4>
              </div>
              <span className={`adm-status-badge ${r.isPrivileged ? "critical" : "low"}`}>
                {r.isPrivileged ? "Privileged Access" : "Standard Role"}
              </span>
            </div>

            <p style={{ fontSize: "12.5px", color: "var(--adm-soft)", lineHeight: "1.5", margin: "6px 0 12px" }}>
              {r.description}
            </p>

            <div style={{ fontSize: "12px", color: "var(--adm-faint)", marginBottom: "8px" }}>
              <strong>{r.userCount}</strong> assigned users
            </div>

            <div style={{ display: "flex", flexWrap: "wrap", gap: "4px" }}>
              {r.permissions.map((p, idx) => (
                <span key={idx} className="adm-badge" style={{ fontSize: "10px", background: "#f8fafc" }}>
                  {p}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Privileged Role Reassignment Panel */}
      <div className="adm-panel">
        <div className="adm-panel-header">
          <h3 className="adm-panel-title">
            <Icon name="award" size={18} />
            Privileged Role Reassignment & Escalation Control
          </h3>
        </div>

        <div style={{ display: "flex", gap: "12px", flexWrap: "wrap", alignItems: "flex-end" }}>
          <div style={{ flex: 1, minWidth: "260px" }}>
            <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
              Select Personnel
            </label>
            <select
              className="adm-select"
              style={{ width: "100%" }}
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
            >
              <option value="">Choose employee account...</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.fullName} ({u.email}) — Current Role: {u.role}
                </option>
              ))}
            </select>
          </div>

          <div style={{ width: "220px" }}>
            <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
              Target Role Assignment
            </label>
            <select
              className="adm-select"
              style={{ width: "100%" }}
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
            >
              {ALL_ROLES.map((r) => (
                <option key={r} value={r}>
                  {r.replace(/_/g, " ")}
                </option>
              ))}
            </select>
          </div>

          <button
            className="adm-btn adm-btn-primary"
            disabled={!selectedUserId}
            onClick={() => setShowConfirm(true)}
          >
            Authorize Reassignment
          </button>
        </div>

        <p style={{ margin: "12px 0 0", fontSize: "12px", color: "var(--adm-faint)" }}>
          🔒 Protection Policy: An administrator cannot modify their own role, and the system strictly prohibits demoting or deactivating the last active platform administrator.
        </p>
      </div>

      {/* Role Assignment History Audit Table */}
      <div className="adm-panel">
        <div className="adm-panel-header">
          <h3 className="adm-panel-title">
            <Icon name="file" size={18} />
            Role Assignment & Escalation Audit Trail
          </h3>
        </div>

        <div className="adm-table-container">
          <table className="adm-table">
            <thead>
              <tr>
                <th>Audit ID</th>
                <th>Target Entity ID</th>
                <th>Action</th>
                <th>Audit Details</th>
                <th>Authorized Administrator</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              {history.length > 0 ? (
                history.map((h) => (
                  <tr key={h.auditId}>
                    <td>#{h.auditId}</td>
                    <td>{h.targetUserEmail || "User"}</td>
                    <td>
                      <span className="adm-badge">{h.action}</span>
                    </td>
                    <td style={{ fontSize: "12.5px" }}>{h.details}</td>
                    <td style={{ fontWeight: "600" }}>{h.performedBy}</td>
                    <td style={{ fontSize: "12px", color: "var(--adm-faint)" }}>
                      {h.timestamp ? new Date(h.timestamp).toLocaleString() : ""}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} style={{ textAlign: "center", color: "var(--adm-faint)", padding: "20px" }}>
                    No privileged role changes recorded in audit history.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Confirmation Dialog */}
      {showConfirm && (
        <div className="adm-modal-overlay" onClick={() => setShowConfirm(false)}>
          <div className="adm-modal" onClick={(e) => e.stopPropagation()}>
            <h3 style={{ margin: "0 0 12px", fontSize: "18px" }}>Confirm Sensitive Role Modification</h3>
            <p style={{ fontSize: "13.5px", color: "var(--adm-soft)", lineHeight: "1.5" }}>
              Are you sure you want to change this user's role to <strong>{selectedRole}</strong>? This action alters backend authorization immediately and is logged in the permanent platform audit trail.
            </p>
            <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "20px" }}>
              <button
                className="adm-btn adm-btn-secondary"
                onClick={() => setShowConfirm(false)}
                disabled={assigning}
              >
                Cancel
              </button>
              <button
                className="adm-btn adm-btn-primary"
                onClick={handleRoleAssignSubmit}
                disabled={assigning}
              >
                {assigning ? "Authorizing..." : "Confirm & Save"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
