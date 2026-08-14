import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import type { Job } from '../types'
import { useWorkspace } from '../workspace/WorkspaceContext'
import { useToast } from '../ui/ToastContext'
import { JobCard } from '../jobs/JobCard'
import { ResumeSidebar } from '../resume/ResumeSidebar'

const CHIPS = ['Java', 'Spring Boot', 'Remote', 'Senior', 'Full-time']

export function JobsPage() {
  const {
    resumes,
    activeResumeId,
    jobs,
    scores,
    statuses,
    loading,
    searching,
    setActive,
    onResumesChanged,
    updateStatus,
    search,
    openJob,
  } = useWorkspace()
  const toast = useToast()

  const [query, setQuery] = useState('')
  const [days, setDays] = useState(3)
  const [textFilter, setTextFilter] = useState('')
  const [chips, setChips] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)

  async function handleSearch(e: FormEvent) {
    e.preventDefault()
    if (!query.trim()) return
    setError(null)
    try {
      const count = await search(query, days)
      toast(`Found ${count} job${count === 1 ? '' : 's'} for “${query}”`, 'success')
    } catch {
      setError('Search failed. Browse the existing jobs below.')
      toast('Search failed. Try again.', 'error')
    }
  }

  function toggleChip(chip: string) {
    setChips((prev) => (prev.includes(chip) ? prev.filter((c) => c !== chip) : [...prev, chip]))
  }

  const visibleJobs = useMemo(() => {
    const scored = Object.keys(scores).length > 0
    const needles = [...chips, textFilter].map((s) => s.trim().toLowerCase()).filter(Boolean)
    const text = (j: Job) => `${j.title} ${j.company ?? ''} ${j.location ?? ''} ${j.description}`.toLowerCase()
    return [...jobs]
      .filter((j) => needles.every((n) => text(j).includes(n)))
      .sort((a, b) => {
        if (scored) return (scores[b.id] ?? -1) - (scores[a.id] ?? -1)
        const da = new Date(a.postedAt ?? a.createdAt).getTime()
        const db = new Date(b.postedAt ?? b.createdAt).getTime()
        return db - da
      })
  }, [jobs, scores, chips, textFilter])

  return (
    <div className="layout">
      <main className="board">
        <form className="search-block" onSubmit={handleSearch}>
          <input
            className="search-query"
            placeholder="Search jobs — e.g. Java backend engineer"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <select value={days} onChange={(e) => setDays(Number(e.target.value))}>
            <option value={1}>Last 1 day</option>
            <option value={3}>Last 3 days</option>
            <option value={7}>Last week</option>
          </select>
          <div className="filter-row">
            <div className="filter-input">
              <span className="filter-icon">⌕</span>
              <input placeholder="Search filters" value={textFilter} onChange={(e) => setTextFilter(e.target.value)} />
            </div>
            <button type="submit" disabled={searching}>
              {searching ? 'Searching…' : '⌕ Search'}
            </button>
          </div>
          <div className="chips">
            <span className="chips-icon">☰</span>
            {CHIPS.map((c) => (
              <button
                key={c}
                type="button"
                className={`chip ${chips.includes(c) ? 'on' : ''}`}
                onClick={() => toggleChip(c)}
              >
                {c}
              </button>
            ))}
          </div>
        </form>

        {error && <p className="error">{error}</p>}

        <div className="section-label">Smart search &amp; results</div>

        <div className="job-list">
          {loading
            ? Array.from({ length: 5 }).map((_, i) => <div key={i} className="skeleton-card" />)
            : visibleJobs.map((job) => (
                <JobCard
                  key={job.id}
                  job={job}
                  score={scores[job.id]}
                  status={statuses[job.id]}
                  onOpen={() => openJob(job)}
                  onToggleSave={() => updateStatus(job.id, statuses[job.id] === 'saved' ? null : 'saved')}
                />
              ))}
          {!loading && visibleJobs.length === 0 && (
            <p className="muted">No jobs match. Try a search or clear the filters.</p>
          )}
        </div>
      </main>

      <ResumeSidebar
        resumes={resumes}
        activeResumeId={activeResumeId}
        onChanged={onResumesChanged}
        onSetActive={setActive}
      />
    </div>
  )
}
