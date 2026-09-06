import { useEffect, useMemo, useState } from 'react'
import { api } from '../api/api'
import { authToken, SESSION_EXPIRED_EVENT, SESSION_REFRESHED_EVENT } from '../api/client'
import type { Role, User } from '../types'
import { AuthContext } from './context'

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = sessionStorage.getItem('opspulse.user')
    if (!stored) return null
    try { return JSON.parse(stored) as User } catch {
      sessionStorage.removeItem('opspulse.user')
      return null
    }
  })
  const [loading, setLoading] = useState(false)
  useEffect(() => {
    const expire = () => { authToken.set(null); setUser(null) }
    const refreshed = (event: Event) => {
      const nextUser = (event as CustomEvent<User>).detail
      if (nextUser) setUser(nextUser)
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, expire)
    window.addEventListener(SESSION_REFRESHED_EVENT, refreshed)
    return () => {
      window.removeEventListener(SESSION_EXPIRED_EVENT, expire)
      window.removeEventListener(SESSION_REFRESHED_EVENT, refreshed)
    }
  }, [])
  const login = async (email: string, password: string) => {
    setLoading(true)
    try {
      const response = await api.login(email, password)
      authToken.set(response.accessToken)
      sessionStorage.setItem('opspulse.refreshToken', response.refreshToken)
      sessionStorage.setItem('opspulse.user', JSON.stringify(response.user))
      setUser(response.user)
    } finally { setLoading(false) }
  }
  const logout = async () => {
    const token = sessionStorage.getItem('opspulse.refreshToken')
    try { if (token) await api.logout(token) } finally {
      authToken.set(null); sessionStorage.removeItem('opspulse.refreshToken'); sessionStorage.removeItem('opspulse.user'); setUser(null)
    }
  }
  const value = useMemo(() => ({ user, loading, login, logout, can: (...roles: Role[]) => Boolean(user && roles.some(role => user.roles.includes(role))) }), [user, loading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
