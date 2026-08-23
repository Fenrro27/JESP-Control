import { Routes, Route, Navigate, NavLink } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import Login from './pages/Login.jsx'
import Dashboard from './pages/Dashboard.jsx'
import History from './pages/History.jsx'
import Stats from './pages/Stats.jsx'
import Rules from './pages/Rules.jsx'
import Users from './pages/Users.jsx'

function Protected({ children, adminOnly = false }) {
  const { token, isAdmin } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  if (adminOnly && !isAdmin) return <Navigate to="/" replace />
  return children
}

function Nav() {
  const { isAdmin, user, logout } = useAuth()
  return (
    <nav className="nav">
      <span className="brand">JESP</span>
      <NavLink to="/">Panel</NavLink>
      <NavLink to="/historial">Historial</NavLink>
      <NavLink to="/estadisticas">Estadísticas</NavLink>
      {isAdmin && <NavLink to="/reglas">Reglas</NavLink>}
      {isAdmin && <NavLink to="/usuarios">Usuarios</NavLink>}
      <button className="link" onClick={logout} title={user?.username}>
        Salir
      </button>
    </nav>
  )
}

export default function App() {
  const { token } = useAuth()

  if (!token) {
    return (
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <>
      <Nav />
      <main className="content">
        <Routes>
          <Route path="/" element={<Protected><Dashboard /></Protected>} />
          <Route path="/historial" element={<Protected><History /></Protected>} />
          <Route path="/estadisticas" element={<Protected><Stats /></Protected>} />
          <Route path="/reglas" element={<Protected adminOnly><Rules /></Protected>} />
          <Route path="/usuarios" element={<Protected adminOnly><Users /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  )
}
