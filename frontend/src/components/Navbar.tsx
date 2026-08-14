import { useAuth } from '../auth/AuthContext'
import type { Resume } from '../types'

export function Navbar({
  resumes,
  activeResumeId,
  onSelectResume,
  onManageResumes,
}: {
  resumes: Resume[]
  activeResumeId: string | null
  onSelectResume: (id: string | null) => void
  onManageResumes: () => void
}) {
  const { user, logout } = useAuth()

  return (
    <header className="navbar">
      <div className="brand">
        JobMatch <span>AI</span>
      </div>
      <div className="nav-right">
        <label className="resume-picker">
          <span className="muted small">Résumé</span>
          <select
            value={activeResumeId ?? ''}
            onChange={(e) => onSelectResume(e.target.value || null)}
          >
            <option value="">None — no match scores</option>
            {resumes.map((r) => (
              <option key={r.id} value={r.id}>
                {r.fileName}
              </option>
            ))}
          </select>
        </label>
        <button type="button" className="secondary small-btn" onClick={onManageResumes}>
          Résumés
        </button>
        <div className="nav-user">
          <span className="muted small">{user?.email}</span>
          <button type="button" className="secondary small-btn" onClick={logout}>
            Log out
          </button>
        </div>
      </div>
    </header>
  )
}
