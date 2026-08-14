import type { Job } from '../types'

function relativeDate(iso: string | null): string | null {
  if (!iso) return null
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000)
  if (days <= 0) return 'today'
  if (days === 1) return '1 day ago'
  return `${days} days ago`
}

export function JobCard({ job, score, onOpen }: { job: Job; score?: number; onOpen: () => void }) {
  const posted = relativeDate(job.postedAt)
  return (
    <button type="button" className="job-card" onClick={onOpen}>
      <div className="job-card-main">
        <h3>{job.title}</h3>
        <p className="muted small">{[job.company, job.location].filter(Boolean).join(' · ')}</p>
        <div className="job-card-tags">
          {posted && <span className="tag recency">{posted}</span>}
          {job.source && job.source !== 'manual' && job.source !== 'seed:indeed' && (
            <span className="tag">{job.source}</span>
          )}
        </div>
      </div>
      <div className="job-card-side">
        {score != null && (
          <div className="score-chip">
            <strong>{Math.round(score * 100)}%</strong>
            <span>match</span>
          </div>
        )}
        {job.sourceUrl && (
          <a
            className="apply-link"
            href={job.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            onClick={(e) => e.stopPropagation()}
          >
            Apply →
          </a>
        )}
      </div>
    </button>
  )
}
