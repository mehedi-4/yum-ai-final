import { createContext, useContext, useState } from 'react'
import api from '../api/client'

const AuthContext = createContext(null)

// Role hierarchy used for UI gating (FR-01.3): ADMIN > MANAGER > STAFF > VIEWER
const RANK = { ADMIN: 3, MANAGER: 2, STAFF: 1, VIEWER: 0 }

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('yumai_user')
    return stored ? JSON.parse(stored) : null
  })

  const login = async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password })
    localStorage.setItem('yumai_token', data.token)
    localStorage.setItem('yumai_user', JSON.stringify(data.user))
    setUser(data.user)
    return data.user
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      /* token may already be invalid */
    }
    localStorage.removeItem('yumai_token')
    localStorage.removeItem('yumai_user')
    setUser(null)
  }

  const hasRole = (minRole) => user && RANK[user.role] >= RANK[minRole]

  return (
    <AuthContext.Provider value={{ user, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
