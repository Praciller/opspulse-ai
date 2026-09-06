import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import type { Role } from '../types'

export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const { user } = useAuth(); const location = useLocation()
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (roles && !roles.some(role => user.roles.includes(role))) return <Navigate to="/" replace />
  return <Outlet />
}
