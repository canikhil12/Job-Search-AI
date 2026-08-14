import type { Job } from '../types'
import type { JobStatusValue } from '../jobstatus/jobStatusApi'
import { ScoreRing } from './ScoreRing'

function relativeDate(iso: string | null): string | null {
  if (!iso) return null
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86_400_000)
  if (days <= 0) return 'today'
  if (days === 1) return '1 day ago'
  return `${days} days ago`
}

export function JobCard({
  job,
  score,
  status,
  onOpen,
  onToggleSave,
}: {
  job: Job
  score?: number
  status?: JobStatusValue
  onOpen: () => void
  onToggleSave: () => void
}) {
  const posted = relativeDate(job.postedAt)
  return (
    <div className="job-card" role="button" tabIndex={0} onClick={onOpen}>
      <div className="job-card-main">
        <h3>{job.title}</h3>
        <p className="muted small">{[job.company, job.location].filter(Boolean).join(' · ')}</p>
        <div className="job-card-tags">
          {posted && <span className="tag recency">{posted}</span>}
          {status === 'applied' && <span className="tag applied">Applied ✓</span>}
          {status === 'saved' && <span className="tag saved">Saved</span>}
        </div>
      </div>
      <div className="job-card-side">
        {score != null && <ScoreRing score={score} />}
        <div className="job-card-actions">
          <button
            type="button"
            className="link"
            onClick={(e) => {
              e.stopPropagation()
              onToggleSave()
            }}
          >
            {status === 'saved' ? '★ Saved' : '☆ Save'}
          </button>
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
      </div>
    </div>
  )
}
