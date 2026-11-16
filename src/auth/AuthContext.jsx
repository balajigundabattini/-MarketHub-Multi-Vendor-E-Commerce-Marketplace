import { useCallback, useEffect, useMemo, useState } from 'react'
import apiClient, { restoreSession } from '../api/client'
import { clearAccessToken, getSessionUser, setSession } from '../api/accessToken'
import { AuthContext } from './authContext'

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(getSessionUser)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    const handleSessionCleared = () => setUser(null)
    window.addEventListener('markethub:session-cleared', handleSessionCleared)
    restoreSession()
      .then(() => { if (active) setUser(getSessionUser()) })
      .catch(() => {
        if (active && !getSessionUser()) clearAccessToken()
      })
      .finally(() => { if (active) setLoading(false) })
    return () => {
      active = false
      window.removeEventListener('markethub:session-cleared', handleSessionCleared)
    }
  }, [])

  const signIn = useCallback((session) => {
    setSession(session.accessToken, session.user)
    setUser(session.user)
  }, [])

  const signOut = useCallback(async () => {
    await apiClient.post('/auth/logout')
    clearAccessToken()
    setUser(null)
  }, [])

  const value = useMemo(() => ({ user, loading, signIn, signOut }), [user, loading, signIn, signOut])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
