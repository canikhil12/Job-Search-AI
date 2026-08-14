import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useWorkspace } from '../workspace/WorkspaceContext'

function initials(name?: string): string {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || '?'
}

export function Navbar() {
  const { user, logout } = useAuth()
  const { resumes, activeResumeId, setActive } = useWorkspace()

  return (
    <header className="navbar">
      <div className="brand">
        JobMatch <span>AI</span>
      </div>

      <nav className="nav-links">
        <NavLink to="/jobs" className={({ isActive }) => (isActive ? 'active' : '')}>
          Jobs
        </NavLink>
        <NavLink to="/tracker" className={({ isActive }) => (isActive ? 'active' : '')}>
          Tracker
        </NavLink>
        <NavLink to="/resumes" className={({ isActive }) => (isActive ? 'active' : '')}>
          Résumés
        </NavLink>
      </nav>

      <div className="nav-right">
        <select
          className="nav-resume"
          value={activeResumeId ?? ''}
          onChange={(e) => setActive(e.target.value || null)}
          title="Active résumé (drives match scores)"
        >
          <option value="">No active résumé</option>
          {resumes.map((r) => (
            <option key={r.id} value={r.id}>
              {r.fileName}
            </option>
          ))}
        </select>
        <div className="nav-user">
          <span className="avatar">{initials(user?.fullName)}</span>
          <span className="nav-name">{user?.fullName}</span>
        </div>
        <button type="button" className="secondary small-btn" onClick={logout}>
          Log out
        </button>
      </div>
    </header>
  )
}
