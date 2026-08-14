import { useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { ApiRequestError } from '../api'
import type { Resume, ResumeDetail } from '../types'
import { deleteResume, getResume, uploadResume } from './resumeApi'
import { useToast } from '../ui/ToastContext'

const ACCEPTED_EXT = ['.pdf', '.docx']

function EyeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}
function TrashIcon() {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M3 6h18M8 6V4h8v2m-9 0 1 14h8l1-14" />
    </svg>
  )
}
function DocIcon() {
  return (
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="1.6">
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8l-5-5Z" />
      <path d="M14 3v5h5M8 13h8M8 17h5" />
    </svg>
  )
}

export function ResumeSidebar({
  resumes,
  activeResumeId,
  onChanged,
  onSetActive,
}: {
  resumes: Resume[]
  activeResumeId: string | null
  onChanged: () => Promise<void> | void
  onSetActive: (id: string) => void
}) {
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [viewing, setViewing] = useState<ResumeDetail | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)
  const toast = useToast()

  async function handleFile(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setError(null)
    if (!ACCEPTED_EXT.some((ext) => file.name.toLowerCase().endsWith(ext))) {
      setError('Please upload a PDF or DOCX file.')
      return
    }
    setUploading(true)
    try {
      const uploaded = await uploadResume(file)
      await onChanged()
      onSetActive(uploaded.id)
      toast('Résumé uploaded and set active', 'success')
    } catch (err) {
      const msg = err instanceof ApiRequestError ? err.message : 'Upload failed.'
      setError(msg)
      toast(msg, 'error')
    } finally {
      setUploading(false)
      if (fileInput.current) fileInput.current.value = ''
    }
  }

  async function handleDelete(id: string) {
    setError(null)
    try {
      await deleteResume(id)
      await onChanged()
      toast('Résumé deleted')
    } catch {
      setError('Could not delete that résumé.')
      toast('Could not delete that résumé.', 'error')
    }
  }

  return (
    <aside className="resume-sidebar">
      <div className="sidebar-head">
        <span className="section-label">Résumé management</span>
        <label className={`pill-btn ${uploading ? 'disabled' : ''}`}>
          + {uploading ? 'Uploading…' : 'Upload New'}
          <input ref={fileInput} type="file" accept=".pdf,.docx,application/pdf" onChange={handleFile} hidden />
        </label>
      </div>

      {error && <p className="error">{error}</p>}

      {resumes.length === 0 ? (
        <p className="muted small">No résumés yet. Upload one to get match scores and AI analysis.</p>
      ) : (
        <ul className="sidebar-resumes">
          {resumes.map((r) => {
            const active = activeResumeId === r.id
            return (
              <li key={r.id} className={active ? 'active' : ''}>
                <span className="doc-icon">
                  <DocIcon />
                </span>
                <button type="button" className="resume-row" onClick={() => onSetActive(r.id)}>
                  <span className="resume-name">
                    {active && <span className="active-label">Active: </span>}
                    {r.fileName}
                  </span>
                  <span className="muted small">(PDF)</span>
                </button>
                <div className="row-icons">
                  <button
                    type="button"
                    title="View extracted text"
                    onClick={async () => setViewing(await getResume(r.id))}
                  >
                    <EyeIcon />
                  </button>
                  <button type="button" title="Delete" className="danger" onClick={() => handleDelete(r.id)}>
                    <TrashIcon />
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      )}

      {viewing && (
        <div className="modal-overlay" onClick={() => setViewing(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-head">
              <h2>{viewing.fileName}</h2>
              <button type="button" className="link" onClick={() => setViewing(null)}>
                Close
              </button>
            </div>
            <pre className="extracted-text">{viewing.extractedText || '(no text found)'}</pre>
          </div>
        </div>
      )}
    </aside>
  )
}
