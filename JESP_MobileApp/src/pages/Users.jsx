import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'

export default function Users() {
  const { user } = useAuth()
  const [users, setUsers] = useState([])
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('USER')
  const [error, setError] = useState(null)

  async function refresh() {
    try {
      setUsers(await api('/api/users'))
      setError(null)
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => {
    refresh()
  }, [])

  async function create(e) {
    e.preventDefault()
    try {
      await api('/api/users', { method: 'POST', body: { username, password, role } })
      setUsername('')
      setPassword('')
      refresh()
    } catch (err) {
      setError(err.message)
    }
  }

  async function remove(u) {
    if (!confirm(`¿Eliminar al usuario "${u.username}"?`)) return
    try {
      await api(`/api/users/${u.id}`, { method: 'DELETE' })
      refresh()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <h2>Usuarios</h2>
      {error && <p className="error">{error}</p>}
      {users.map((u) => (
        <div key={u.id} className="card rule-row">
          <div className="row space-between">
            <strong>{u.username}</strong>
            <span className={`badge ${u.role === 'ADMIN' ? 'ok' : ''}`}>{u.role}</span>
          </div>
          {u.username !== user.username && (
            <button className="danger" onClick={() => remove(u)}>Eliminar</button>
          )}
        </div>
      ))}

      <h3>Nuevo usuario</h3>
      <form className="card" onSubmit={create}>
        <label>
          Usuario
          <input value={username} onChange={(e) => setUsername(e.target.value)} required minLength={3} />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={6}
          />
        </label>
        <label>
          Rol
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="USER">USER (solo consulta)</option>
            <option value="ADMIN">ADMIN (control total)</option>
          </select>
        </label>
        <button type="submit">Crear</button>
      </form>
    </div>
  )
}
