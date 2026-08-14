import { useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { ApiRequestError } from '../api'
import type { Resume, ResumeDetail } from '../types'
import { deleteResume, getResume, uploadResume } from './resumeApi'

const ACCEPTED_EXT = ['.pdf', '.docx']

export function ResumeManager({
  resumes,
  activeResumeId,
  onClose,
  onChanged,
  onSetActive,
}: {
  resumes: Resume[]
  activeResumeId: string | null
  onClose: () => void
  onChanged: () => Promise<void> | void
  onSetActive: (id: string) => void
}) {
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [detail, setDetail] = useState<ResumeDetail | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)

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
      onSetActive(uploaded.id) // make the just-uploaded résumé active
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Upload failed.')
    } finally {
      setUploading(false)
      if (fileInput.current) fileInput.current.value = ''
    }
  }

  async function handleDelete(id: string) {
    setError(null)
    try {
      await deleteResume(id)
      if (detail?.id === id) setDetail(null)
      await onChanged()
    } catch {
      setError('Could not delete that résumé.')
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h2>Your résumés</h2>
          <button type="button" className="link" onClick={onClose}>
            Close
          </button>
        </div>

        <label className={`upload-btn ${uploading ? 'disabled' : ''}`}>
          {uploading ? 'Uploading…' : 'Upload résumé'}
          <input ref={fileInput} type="file" accept=".pdf,.docx,application/pdf" onChange={handleFile} hidden />
        </label>
        <p className="muted small">PDF or DOCX, up to 5 MB. The active résumé drives your match scores.</p>
        {error && <p className="error">{error}</p>}

        {resumes.length === 0 ? (
          <p className="muted">No résumés yet. Upload one to get match scores and AI analysis.</p>
        ) : (
          <ul className="resume-list">
            {resumes.map((r) => (
              <li key={r.id} className={activeResumeId === r.id ? 'active' : ''}>
                <div className="resume-meta">
                  <span className="resume-name">
                    {r.fileName}
                    {activeResumeId === r.id && <span className="badge">active</span>}
                  </span>
                </div>
                <div className="resume-actions">
                  {activeResumeId !== r.id && (
                    <button type="button" className="link strong" onClick={() => onSetActive(r.id)}>
                      Set active
                    </button>
                  )}
                  <button
                    type="button"
                    className="link"
                    onClick={async () => setDetail(await getResume(r.id))}
                  >
                    View text
                  </button>
                  <button type="button" className="link danger" onClick={() => handleDelete(r.id)}>
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}

        {detail && (
          <div className="extracted">
            <div className="panel-head">
              <h3>Extracted text — {detail.fileName}</h3>
              <button type="button" className="link" onClick={() => setDetail(null)}>
                Close
              </button>
            </div>
            <pre className="extracted-text">{detail.extractedText || '(no text found)'}</pre>
          </div>
        )}
      </div>
    </div>
  )
}
