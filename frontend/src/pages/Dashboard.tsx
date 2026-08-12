import { useAuth } from '../auth/AuthContext'
import { ResumePanel } from '../resume/ResumePanel'

export function Dashboard() {
  const { user, logout } = useAuth()

  return (
    <div className="dashboard">
      <header className="dashboard-head">
        <div>
          <h1>Hello, {user?.fullName}</h1>
          <p className="muted">Signed in as {user?.email}.</p>
        </div>
        <button type="button" className="secondary" onClick={logout}>
          Log out
        </button>
      </header>

      <ResumePanel />
    </div>
  )
}
