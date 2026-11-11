import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { dashboardPath } from "../services/authService.js";

export default function PublicOnlyRoute() {
  const { user, loading } = useAuth();
  if (loading) return <div className="screen-center" role="status">Loading…</div>;
  if (user) return <Navigate to={dashboardPath(user.role)} replace />;
  return <Outlet />;
}
