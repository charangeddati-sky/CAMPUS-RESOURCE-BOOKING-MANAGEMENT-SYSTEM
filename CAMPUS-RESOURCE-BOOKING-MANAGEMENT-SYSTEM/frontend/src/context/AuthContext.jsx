import { createContext, useContext, useMemo, useState } from 'react'
import client from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('campusos_user')
    return raw ? JSON.parse(raw) : null
  })

  const value = useMemo(() => {
    const persist = (data) => {
      localStorage.setItem('campusos_token', data.accessToken)
      localStorage.setItem('campusos_refresh', data.refreshToken)
      localStorage.setItem('campusos_user', JSON.stringify(data.user))
      setUser(data.user)
    }

    return {
      user,
      isAuthenticated: Boolean(user),
      role: user?.role,
      async login(email, password) {
        const { data } = await client.post('/auth/login', { email, password })
        persist(data)
        return data.user
      },
      async register(payload) {
        const { data } = await client.post('/auth/register', payload)
        persist(data)
        return data.user
      },
      logout() {
        localStorage.removeItem('campusos_token')
        localStorage.removeItem('campusos_refresh')
        localStorage.removeItem('campusos_user')
        setUser(null)
      },
    }
  }, [user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
