import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Logo from "../components/Logo.jsx";
import EmployeeDashboard from "./EmployeeDashboard.jsx";
import Alert from "../components/Alert.jsx";
import { useAuth } from "../context/AuthContext.jsx";
import AdminMentorship from "./admin/AdminMentorship.jsx";
import {
  getDashboard, listUsers, parseApiError, updateUserRole, updateUserStatus,
} from "../services/authService.js";

const ROLE_LABEL = { EMPLOYEE: "Employee", HR: "HR", MANAGER: "Manager", ADMIN: "Admin" };

function AdminUsers() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState([]);
  const [error, setError] = useState("");
  const load = useCallback(() => {
    listUsers().then(setUsers).catch((e) => setError(parseApiError(e).message));
  }, []);
  useEffect(load, [load]);
  const act = async (fn) => {
    setError("");
    try {
      const updated = await fn();
      setUsers((list) => list.map((u) => (u.id === updated.id ? updated : u)));
    } catch (e) { setError(parseApiError(e).message); }
  };
  return (
    <section className="panel" aria-labelledby="users-h">
      <h2 id="users-h">People and access</h2>
      <Alert type="error">{error}</Alert>
      <div className="table-wrap">
        <table>
          <thead>
            <tr><th>Name</th><th>Email</th><th>Role</th><th>Requested</th><th>Status</th></tr>
          </thead>
          <tbody>
            {users.map((u) => {
              const self = u.id === me.id;
              return (
                <tr key={u.id}>
                  <td>{u.fullName}<small>{u.employeeId || "No employee ID"}</small></td>
                  <td>{u.email}</td>
                  <td>
                    <select aria-label={`Role for ${u.fullName}`} value={u.role} disabled={self} onChange={(e) => act(() => updateUserRole(u.id, e.target.value))}>
                      {Object.entries(ROLE_LABEL).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                    </select>
                  </td>
                  <td>{u.requestedRole ? <span className="badge badge--warn">{ROLE_LABEL[u.requestedRole]}</span> : "—"}</td>
                  <td>
                    <select aria-label={`Status for ${u.fullName}`} value={u.accountStatus} disabled={self} onChange={(e) => act(() => updateUserStatus(u.id, e.target.value))}>
                      <option value="ACTIVE">Active</option>
                      <option value="PENDING">Pending</option>
                      <option value="SUSPENDED">Suspended</option>
                    </select>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default function Dashboard({ role }) {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [adminTab, setAdminTab] = useState("users"); // "users" | "mentorship"

  useEffect(() => {
    getDashboard(role).then(setData).catch((e) => setError(parseApiError(e).message));
  }, [role]);

  const onSignOut = async () => {
    await signOut();
    navigate("/login", { replace: true });
  };

  if (role === "EMPLOYEE") return <EmployeeDashboard />;

  return (
    <div className="dash">
      <header className="dash__bar">
        <Logo />
        <div className="dash__user">
          <span className="badge">{ROLE_LABEL[user.role]}</span>
          <span className="dash__name">{user.fullName}</span>
          <button className="btn btn--ghost" onClick={onSignOut}>Sign out</button>
        </div>
      </header>

      <main className="dash__main">
        <Alert type="error">{error}</Alert>

        <section className="panel">
          <h1>{data ? data.title : "Loading…"}</h1>
          <p>{data ? data.message : ""}</p>
          {data && (
            <p className="muted">
              Signed in as {user.email}. This page was authorized by the server for the {ROLE_LABEL[user.role]} role.
            </p>
          )}

          {role === "ADMIN" && (
            <div className="tabs" style={{ marginTop: "1rem", marginBottom: 0 }}>
              <button
                className={`tab-btn ${adminTab === "users" ? "is-active" : ""}`}
                onClick={() => setAdminTab("users")}
              >
                User Accounts & Access Control
              </button>
              <button
                className={`tab-btn ${adminTab === "mentorship" ? "is-active" : ""}`}
                onClick={() => setAdminTab("mentorship")}
              >
                Peer Mentorship & Expert Governance
              </button>
            </div>
          )}
        </section>

        {role === "ADMIN" && adminTab === "users" && <AdminUsers />}
        {role === "ADMIN" && adminTab === "mentorship" && <AdminMentorship />}
      </main>
    </div>
  );
}
