import { useWorkspace } from '../workspace/WorkspaceContext'
import { JobCard } from '../jobs/JobCard'

export function TrackerPage() {
  const { jobs, scores, statuses, updateStatus, openJob } = useWorkspace()

  const saved = jobs.filter((j) => statuses[j.id] === 'saved')
  const applied = jobs.filter((j) => statuses[j.id] === 'applied')

  const column = (label: string, items: typeof jobs, empty: string) => (
    <section className="tracker-col">
      <div className="section-label">
        {label} ({items.length})
      </div>
      <div className="job-list">
        {items.map((job) => (
          <JobCard
            key={job.id}
            job={job}
            score={scores[job.id]}
            status={statuses[job.id]}
            onOpen={() => openJob(job)}
            onToggleSave={() => updateStatus(job.id, statuses[job.id] === 'saved' ? null : 'saved')}
          />
        ))}
        {items.length === 0 && <p className="muted small">{empty}</p>}
      </div>
    </section>
  )

  return (
    <div className="page">
      <h1 className="page-title">Application tracker</h1>
      <p className="muted">Jobs you’ve saved to revisit and the ones you’ve applied to.</p>
      <div className="tracker-cols">
        {column('Saved', saved, 'Nothing saved yet — hit ☆ Save on a job.')}
        {column('Applied', applied, 'Nothing marked applied yet — open a job and “Mark as applied”.')}
      </div>
    </div>
  )
}
