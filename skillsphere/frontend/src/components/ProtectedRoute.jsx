import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { dashboardPath } from "../services/authService.js";

/** Client-side guard. The backend independently enforces every role rule. */
export default function ProtectedRoute({ allowedRoles }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) return <div className="screen-center" role="status">Loading your workspace…</div>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={dashboardPath(user.role)} replace />;
  }
  return <Outlet />;
}
