import { useEffect, useState } from "react";
import Icon from "../../components/Icons.jsx";
import Alert from "../../components/Alert.jsx";
import {
  getAdminUsers,
  createAdminUser,
  updateAdminUser,
  getAdminDepartments,
} from "../../services/adminService.js";

const ROLES = [
  "EMPLOYEE",
  "MANAGER",
  "MENTOR",
  "HR_SPECIALIST",
  "DEPARTMENT_HEAD",
  "ADMIN",
];

export default function UserManagementView() {
  const [users, setUsers] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Filters
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState("");

  // Create User Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newFullName, setNewFullName] = useState("");
  const [newEmail, setNewEmail] = useState("");
  const [newEmpId, setNewEmpId] = useState("");
  const [newRole, setNewRole] = useState("EMPLOYEE");
  const [newDept, setNewDept] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [creating, setCreating] = useState(false);

  // Edit User Modal
  const [editingUser, setEditingUser] = useState(null);
  const [editFullName, setEditFullName] = useState("");
  const [editEmpId, setEditEmpId] = useState("");
  const [editDept, setEditDept] = useState("");
  const [editStatus, setEditStatus] = useState("ACTIVE");
  const [updating, setUpdating] = useState(false);

  const loadUsers = async () => {
    try {
      setLoading(true);
      setError("");
      const params = {};
      if (search) params.search = search;
      if (roleFilter) params.role = roleFilter;
      if (statusFilter) params.status = statusFilter;

      const res = await getAdminUsers(params);
      setUsers(res || []);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to load users.");
    } finally {
      setLoading(false);
    }
  };

  const loadDepartments = async () => {
    try {
      const depts = await getAdminDepartments();
      setDepartments(depts || []);
    } catch {
      // ignore
    }
  };

  useEffect(() => {
    loadUsers();
    loadDepartments();
  }, [roleFilter, statusFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    loadUsers();
  };

  const handleCreateUser = async (e) => {
    e.preventDefault();
    if (!newFullName || !newEmail || !newPassword) {
      setError("Please complete all required fields.");
      return;
    }

    try {
      setCreating(true);
      setError("");
      await createAdminUser({
        fullName: newFullName.trim(),
        email: newEmail.trim(),
        employeeId: newEmpId.trim() || null,
        role: newRole,
        department: newDept || null,
        password: newPassword,
      });

      setSuccess(`Account for ${newEmail} successfully created and verified.`);
      setShowCreateModal(false);
      setNewFullName("");
      setNewEmail("");
      setNewEmpId("");
      setNewPassword("");
      setNewRole("EMPLOYEE");
      loadUsers();
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to create user account.");
    } finally {
      setCreating(false);
    }
  };

  const openEditModal = (user) => {
    setEditingUser(user);
    setEditFullName(user.fullName || "");
    setEditEmpId(user.employeeId || "");
    setEditDept(user.department === "—" ? "" : user.department || "");
    setEditStatus(user.accountStatus || "ACTIVE");
  };

  const handleUpdateUser = async (e) => {
    e.preventDefault();
    if (!editingUser) return;

    try {
      setUpdating(true);
      setError("");
      await updateAdminUser(editingUser.id, {
        fullName: editFullName.trim(),
        employeeId: editEmpId.trim() || null,
        department: editDept || null,
        accountStatus: editStatus,
      });

      setSuccess(`Account details for ${editingUser.email} updated.`);
      setEditingUser(null);
      loadUsers();
      setTimeout(() => setSuccess(""), 4000);
    } catch (err) {
      setError(err?.response?.data?.message || err?.message || "Failed to update user account.");
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div>
      <div className="adm-panel">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "12px" }}>
          <div>
            <h3 className="adm-panel-title" style={{ margin: 0 }}>
              <Icon name="users" size={18} />
              Enterprise User Lifecycle & Account Directory
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: "13px", color: "var(--adm-soft)" }}>
              Provision, govern, and maintain platform accounts across all organizational tiers.
            </p>
          </div>

          <button className="adm-btn adm-btn-primary" onClick={() => setShowCreateModal(true)}>
            <Icon name="plus" size={14} />
            Create Account
          </button>
        </div>

        {/* Filter Bar */}
        <div style={{ display: "flex", gap: "10px", marginTop: "16px", flexWrap: "wrap", alignItems: "center" }}>
          <form onSubmit={handleSearchSubmit} style={{ display: "flex", gap: "6px", flex: "1", minWidth: "240px" }}>
            <input
              type="text"
              className="adm-input"
              style={{ width: "100%" }}
              placeholder="Search by full name, email, or employee ID..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button type="submit" className="adm-btn adm-btn-secondary">
              <Icon name="search" size={14} />
            </button>
          </form>

          <select className="adm-select" value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)}>
            <option value="">All Roles</option>
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {r.replace(/_/g, " ")}
              </option>
            ))}
          </select>

          <select className="adm-select" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All Statuses</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="PENDING">PENDING</option>
            <option value="SUSPENDED">SUSPENDED</option>
          </select>
        </div>
      </div>

      {error && <Alert type="danger" message={error} />}
      {success && <Alert type="success" message={success} />}

      {/* Users Table */}
      <div className="adm-panel">
        {loading ? (
          <div style={{ textAlign: "center", padding: "60px 20px" }}>
            <Icon name="refresh" className="spin" size={32} />
            <h4 style={{ marginTop: "12px", color: "var(--adm-soft)" }}>Loading User Accounts...</h4>
          </div>
        ) : (
          <div className="adm-table-container">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Personnel</th>
                  <th>Employee ID</th>
                  <th>Assigned Role</th>
                  <th>Department</th>
                  <th>Account Status</th>
                  <th>Registered</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.length > 0 ? (
                  users.map((u) => (
                    <tr key={u.id}>
                      <td>
                        <div style={{ fontWeight: "700", color: "var(--adm-ink)" }}>{u.fullName}</div>
                        <div style={{ fontSize: "12px", color: "var(--adm-soft)" }}>{u.email}</div>
                      </td>
                      <td>{u.employeeId || "—"}</td>
                      <td>
                        <span className="adm-badge">{u.role}</span>
                        {u.requestedRole && (
                          <div style={{ fontSize: "11px", color: "var(--adm-amber)", marginTop: "2px" }}>
                            Req: {u.requestedRole}
                          </div>
                        )}
                      </td>
                      <td>{u.department}</td>
                      <td>
                        <span className={`adm-status-badge ${u.accountStatus.toLowerCase()}`}>
                          {u.accountStatus}
                        </span>
                      </td>
                      <td style={{ fontSize: "12px", color: "var(--adm-faint)" }}>
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}
                      </td>
                      <td>
                        <button
                          className="adm-btn adm-btn-secondary"
                          style={{ fontSize: "11.5px", padding: "4px 8px" }}
                          onClick={() => openEditModal(u)}
                        >
                          Edit Profile
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} style={{ textAlign: "center", color: "var(--adm-faint)", padding: "24px" }}>
                      No accounts matched the search criteria.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Create User Modal */}
      {showCreateModal && (
        <div className="adm-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="adm-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px" }}>Provision New Enterprise Account</h3>
              <button
                style={{ background: "none", border: "none", cursor: "pointer", color: "var(--adm-soft)" }}
                onClick={() => setShowCreateModal(false)}
              >
                <Icon name="x" size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateUser}>
              <div style={{ marginBottom: "12px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Full Legal Name *
                </label>
                <input
                  type="text"
                  className="adm-input"
                  style={{ width: "100%" }}
                  placeholder="e.g. Eleanor Vance"
                  value={newFullName}
                  onChange={(e) => setNewFullName(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "12px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Corporate Email *
                  </label>
                  <input
                    type="email"
                    className="adm-input"
                    style={{ width: "100%" }}
                    placeholder="e.g. eleanor.vance@company.com"
                    value={newEmail}
                    onChange={(e) => setNewEmail(e.target.value)}
                    required
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Employee ID
                  </label>
                  <input
                    type="text"
                    className="adm-input"
                    style={{ width: "100%" }}
                    placeholder="e.g. EMP-504"
                    value={newEmpId}
                    onChange={(e) => setNewEmpId(e.target.value)}
                  />
                </div>
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "12px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Platform Role *
                  </label>
                  <select
                    className="adm-select"
                    style={{ width: "100%" }}
                    value={newRole}
                    onChange={(e) => setNewRole(e.target.value)}
                  >
                    {ROLES.map((r) => (
                      <option key={r} value={r}>
                        {r.replace(/_/g, " ")}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Department
                  </label>
                  <select
                    className="adm-select"
                    style={{ width: "100%" }}
                    value={newDept}
                    onChange={(e) => setNewDept(e.target.value)}
                  >
                    <option value="">No Department Assigned</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.name}>
                        {d.name} ({d.code})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ marginBottom: "20px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Initial Password * (Minimum 8 chars)
                </label>
                <input
                  type="password"
                  className="adm-input"
                  style={{ width: "100%" }}
                  placeholder="••••••••"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  minLength={8}
                />
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
                <button
                  type="button"
                  className="adm-btn adm-btn-secondary"
                  onClick={() => setShowCreateModal(false)}
                  disabled={creating}
                >
                  Cancel
                </button>
                <button type="submit" className="adm-btn adm-btn-primary" disabled={creating}>
                  {creating ? "Creating..." : "Provision Account"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit User Modal */}
      {editingUser && (
        <div className="adm-modal-overlay" onClick={() => setEditingUser(null)}>
          <div className="adm-modal" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
              <h3 style={{ margin: 0, fontSize: "18px" }}>Edit Profile: {editingUser.email}</h3>
              <button
                style={{ background: "none", border: "none", cursor: "pointer", color: "var(--adm-soft)" }}
                onClick={() => setEditingUser(null)}
              >
                <Icon name="x" size={20} />
              </button>
            </div>

            <form onSubmit={handleUpdateUser}>
              <div style={{ marginBottom: "12px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Full Legal Name
                </label>
                <input
                  type="text"
                  className="adm-input"
                  style={{ width: "100%" }}
                  value={editFullName}
                  onChange={(e) => setEditFullName(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "10px", marginBottom: "12px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Employee ID
                  </label>
                  <input
                    type="text"
                    className="adm-input"
                    style={{ width: "100%" }}
                    value={editEmpId}
                    onChange={(e) => setEditEmpId(e.target.value)}
                  />
                </div>
                <div>
                  <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                    Department
                  </label>
                  <select
                    className="adm-select"
                    style={{ width: "100%" }}
                    value={editDept}
                    onChange={(e) => setEditDept(e.target.value)}
                  >
                    <option value="">No Department Assigned</option>
                    {departments.map((d) => (
                      <option key={d.id} value={d.name}>
                        {d.name} ({d.code})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div style={{ marginBottom: "20px" }}>
                <label style={{ display: "block", fontSize: "12.5px", fontWeight: "600", marginBottom: "4px" }}>
                  Account Lifecycle Status
                </label>
                <select
                  className="adm-select"
                  style={{ width: "100%" }}
                  value={editStatus}
                  onChange={(e) => setEditStatus(e.target.value)}
                >
                  <option value="ACTIVE">ACTIVE (Full access permitted)</option>
                  <option value="PENDING">PENDING (Awaiting verification)</option>
                  <option value="SUSPENDED">SUSPENDED (Access revoked)</option>
                </select>
                {editStatus === "SUSPENDED" && editingUser.role === "ADMIN" && (
                  <div style={{ color: "var(--adm-rose)", fontSize: "12px", marginTop: "6px" }}>
                    ⚠️ Caution: Suspending an Administrator requires at least one other active administrator account to exist.
                  </div>
                )}
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px" }}>
                <button
                  type="button"
                  className="adm-btn adm-btn-secondary"
                  onClick={() => setEditingUser(null)}
                  disabled={updating}
                >
                  Cancel
                </button>
                <button type="submit" className="adm-btn adm-btn-primary" disabled={updating}>
                  {updating ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
