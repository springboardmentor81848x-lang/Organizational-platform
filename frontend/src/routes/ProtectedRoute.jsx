import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute({ roles }) {
  const { isAuthenticated, loading, hasRole } = useAuth()
  const location = useLocation()

  if (loading) return <div className="p-10 text-center">Loading...</div>
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location }} />
  if (roles && !hasRole(...roles)) return <Navigate to="/" replace />
  return <Outlet />
}