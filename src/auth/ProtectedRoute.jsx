import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './useAuth'

const ProtectedRoute = ({ allowedRoles, children }) => {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <div className="container py-5" role="status">Restoring your session…</div>
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />
  if (allowedRoles?.length && !allowedRoles.some((role) => user.roles?.includes(role))) {
    return <Navigate to="/forbidden" replace />
  }
  return children
}

export default ProtectedRoute
