import { Navigate, Route, Routes } from "react-router-dom";
import Login from "./pages/Login.jsx";
import Register from "./pages/Register.jsx";
import ForgotPassword from "./pages/ForgotPassword.jsx";
import ResetPassword from "./pages/ResetPassword.jsx";
import EmployeeDashboard from "./pages/EmployeeDashboard.jsx";
import ManagerDashboard from "./pages/ManagerDashboard.jsx";
import MentorDashboard from "./pages/MentorDashboard.jsx";
import HrDashboard from "./pages/HrDashboard.jsx";
import DepartmentHeadDashboard from "./pages/DepartmentHeadDashboard.jsx";
import AdminDashboard from "./pages/AdminDashboard.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import PublicOnlyRoute from "./components/PublicOnlyRoute.jsx";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />

      <Route element={<PublicOnlyRoute />}>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={["EMPLOYEE"]} />}>
        <Route path="/dashboard/employee" element={<EmployeeDashboard />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={["MANAGER"]} />}>
        <Route path="/dashboard/manager" element={<ManagerDashboard />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={["MENTOR"]} />}>
        <Route path="/dashboard/mentor" element={<MentorDashboard />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={["HR_SPECIALIST", "HR"]} />}>
        <Route path="/dashboard/hr" element={<HrDashboard />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={["DEPARTMENT_HEAD"]} />}>
        <Route path="/dashboard/department-head" element={<DepartmentHeadDashboard />} />
      </Route>
      <Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
        <Route path="/dashboard/admin" element={<AdminDashboard />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
