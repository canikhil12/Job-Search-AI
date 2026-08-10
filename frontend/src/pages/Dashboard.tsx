import { useAuth } from '../auth/AuthContext'

export function Dashboard() {
  const { user, logout } = useAuth()

  return (
    <div className="card">
      <h1>Hello, {user?.fullName}</h1>
      <p className="muted">You are signed in as {user?.email}.</p>
      <button type="button" onClick={logout}>
        Log out
      </button>
    </div>
  )
}
