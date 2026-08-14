import { useWorkspace } from '../workspace/WorkspaceContext'
import { ResumeSidebar } from '../resume/ResumeSidebar'

export function ResumesPage() {
  const { resumes, activeResumeId, onResumesChanged, setActive } = useWorkspace()

  return (
    <div className="page narrow">
      <h1 className="page-title">Your résumés</h1>
      <p className="muted">
        Upload multiple résumés and set one active — the active résumé drives match scores, ATS
        analysis, cover letters, and tailoring across the app.
      </p>
      <ResumeSidebar
        resumes={resumes}
        activeResumeId={activeResumeId}
        onChanged={onResumesChanged}
        onSetActive={setActive}
      />
    </div>
  )
}
