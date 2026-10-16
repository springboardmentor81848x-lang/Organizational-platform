import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/services'
import { TOKEN_KEY } from '../api/client'

const MOCK = import.meta.env.VITE_MOCK_AUTH === 'true'
const mockUser = (email = 'demo@company.com') => ({
  id: 1, firstName: 'Demo', lastName: 'User', email, roles: ['EMPLOYEE'],
})

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(localStorage.getItem(TOKEN_KEY)))

  const loadUser = useCallback(async () => {
    try {
      if (MOCK) setUser(mockUser())
      else setUser((await authApi.me()).data)
    } catch {
      localStorage.removeItem(TOKEN_KEY)
      setUser(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (localStorage.getItem(TOKEN_KEY)) loadUser()
  }, [loadUser])

  const login = async (email, password) => {
    if (MOCK) {
      localStorage.setItem(TOKEN_KEY, 'mock-token')
      setUser(mockUser(email))
      return
    }
    const { data } = await authApi.login(email, password)
    localStorage.setItem(TOKEN_KEY, data.accessToken)
    setUser(data.user)
  }

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }

  const hasRole = useCallback(
    (...roles) => Boolean(user?.roles?.some((r) => roles.includes(r))),
    [user]
  )

  const value = useMemo(
    () => ({ user, loading, isAuthenticated: !!user, login, logout, hasRole }),
    [user, loading, hasRole]
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)