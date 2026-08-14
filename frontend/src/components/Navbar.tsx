import { useAuth } from '../auth/AuthContext'

function initials(name?: string): string {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || '?'
}

export function Navbar() {
  const { user, logout } = useAuth()

  return (
    <header className="navbar">
      <div className="brand">
        JobMatch <span>AI</span>
      </div>
      <div className="nav-right">
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
