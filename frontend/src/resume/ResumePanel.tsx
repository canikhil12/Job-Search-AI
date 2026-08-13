import { useEffect, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { ApiRequestError } from '../api'
import type { JobMatch, Resume, ResumeDetail } from '../types'
import { deleteResume, getResume, listResumes, matchResume, uploadResume } from './resumeApi'

const ACCEPTED = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
const ACCEPTED_EXT = ['.pdf', '.docx']

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString()
}

export function ResumePanel() {
  const [resumes, setResumes] = useState<Resume[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [detail, setDetail] = useState<ResumeDetail | null>(null)
  const [matches, setMatches] = useState<{ resumeId: string; fileName: string; items: JobMatch[] } | null>(null)
  const [matchingId, setMatchingId] = useState<string | null>(null)
  const fileInput = useRef<HTMLInputElement>(null)

  async function refresh() {
    try {
      setResumes(await listResumes())
    } catch {
      setError('Could not load your resumes.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refresh()
  }, [])

  async function handleFile(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setError(null)

    const isAccepted =
      ACCEPTED.includes(file.type) || ACCEPTED_EXT.some((ext) => file.name.toLowerCase().endsWith(ext))
    if (!isAccepted) {
      setError('Please upload a PDF or DOCX file.')
      resetInput()
      return
    }

    setUploading(true)
    try {
      const uploaded = await uploadResume(file)
      setDetail(uploaded)
      await refresh()
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Upload failed. Please try again.')
    } finally {
      setUploading(false)
      resetInput()
    }
  }

  async function handleView(id: string) {
    setError(null)
    try {
      setDetail(await getResume(id))
    } catch {
      setError('Could not load that resume.')
    }
  }

  async function handleMatch(resume: Resume) {
    setError(null)
    setMatchingId(resume.id)
    try {
      const items = await matchResume(resume.id)
      setMatches({ resumeId: resume.id, fileName: resume.fileName, items })
    } catch (err) {
      setError(err instanceof ApiRequestError ? err.message : 'Could not find matches.')
    } finally {
      setMatchingId(null)
    }
  }

  async function handleDelete(id: string) {
    setError(null)
    try {
      await deleteResume(id)
      if (detail?.id === id) setDetail(null)
      if (matches?.resumeId === id) setMatches(null)
      await refresh()
    } catch {
      setError('Could not delete that resume.')
    }
  }

  function resetInput() {
    if (fileInput.current) fileInput.current.value = ''
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Your resumes</h2>
        <label className={`upload-btn ${uploading ? 'disabled' : ''}`}>
          {uploading ? 'Uploading…' : 'Upload resume'}
          <input
            ref={fileInput}
            type="file"
            accept=".pdf,.docx,application/pdf"
            onChange={handleFile}
            disabled={uploading}
            hidden
          />
        </label>
      </div>
      <p className="muted small">PDF or DOCX, up to 5 MB. We extract the text for matching.</p>

      {error && <p className="error">{error}</p>}

      {loading ? (
        <p className="muted">Loading…</p>
      ) : resumes.length === 0 ? (
        <p className="muted">No resumes yet. Upload one to get started.</p>
      ) : (
        <ul className="resume-list">
          {resumes.map((r) => (
            <li key={r.id} className={detail?.id === r.id ? 'active' : ''}>
              <div className="resume-meta">
                <span className="resume-name">{r.fileName}</span>
                <span className="muted small">
                  {formatBytes(r.sizeBytes)} · {formatDate(r.createdAt)}
                </span>
              </div>
              <div className="resume-actions">
                <button
                  type="button"
                  className="link strong"
                  onClick={() => handleMatch(r)}
                  disabled={matchingId === r.id}
                >
                  {matchingId === r.id ? 'Matching…' : 'Find matches'}
                </button>
                <button type="button" className="link" onClick={() => handleView(r.id)}>
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

      {matches && (
        <div className="extracted">
          <div className="panel-head">
            <h3>Top matches — {matches.fileName}</h3>
            <button type="button" className="link" onClick={() => setMatches(null)}>
              Close
            </button>
          </div>
          {matches.items.length === 0 ? (
            <p className="muted small">No jobs to match against yet.</p>
          ) : (
            <ul className="match-list">
              {matches.items.map((m, i) => (
                <li key={m.id}>
                  <div className="match-rank">{i + 1}</div>
                  <div className="match-body">
                    <div className="match-title">
                      {m.sourceUrl ? (
                        <a href={m.sourceUrl} target="_blank" rel="noopener noreferrer">
                          {m.title}
                        </a>
                      ) : (
                        m.title
                      )}
                    </div>
                    <div className="muted small">
                      {[m.company, m.location].filter(Boolean).join(' · ')}
                    </div>
                    <div className="score-bar" aria-hidden="true">
                      <span style={{ width: `${Math.max(0, Math.min(1, m.score)) * 100}%` }} />
                    </div>
                  </div>
                  <div className="match-score">{(m.score * 100).toFixed(0)}%</div>
                </li>
              ))}
            </ul>
          )}
        </div>
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
    </section>
  )
}
