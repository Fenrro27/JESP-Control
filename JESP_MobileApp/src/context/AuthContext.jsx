import { createContext, useContext, useState } from 'react'
import { api, storeSession, clearSession, getToken, getStoredUser } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(getToken())
  const [user, setUser] = useState(getStoredUser())

  async function login(username, password) {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: { username, password },
    })
    storeSession(data.token, { username: data.username, role: data.role })
    setToken(data.token)
    setUser({ username: data.username, role: data.role })
    return data
  }

  function logout() {
    clearSession()
    setToken(null)
    setUser(null)
  }

  const isAdmin = user?.role === 'ADMIN'

  return (
    <AuthContext.Provider value={{ token, user, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
