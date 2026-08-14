import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import type { Job, Resume } from '../types'
import { listResumes, matchResume } from '../resume/resumeApi'
import { listJobs, searchJobs } from '../jobs/jobApi'
import { clearStatus, listStatuses, setStatus } from '../jobstatus/jobStatusApi'
import type { JobStatusValue } from '../jobstatus/jobStatusApi'
import { Navbar } from '../components/Navbar'
import { JobCard } from '../jobs/JobCard'
import { JobDetailDrawer } from '../jobs/JobDetailDrawer'
import { ResumeSidebar } from '../resume/ResumeSidebar'

const ACTIVE_KEY = 'jobmatch.activeResume'
const CHIPS = ['Java', 'Spring Boot', 'Remote', 'Senior', 'Full-time']

export function Dashboard() {
  const [resumes, setResumes] = useState<Resume[]>([])
  const [activeResumeId, setActiveResumeId] = useState<string | null>(
    () => localStorage.getItem(ACTIVE_KEY),
  )
  const [jobs, setJobs] = useState<Job[]>([])
  const [scores, setScores] = useState<Record<string, number>>({})
  const [statuses, setStatuses] = useState<Record<string, JobStatusValue>>({})
  const [filter, setFilter] = useState<'all' | 'saved' | 'applied'>('all')
  const [selectedJob, setSelectedJob] = useState<Job | null>(null)
  const [loading, setLoading] = useState(true)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [query, setQuery] = useState('')
  const [days, setDays] = useState(3)
  const [textFilter, setTextFilter] = useState('')
  const [chips, setChips] = useState<string[]>([])

  const refreshScores = useCallback(async (resumeId: string | null) => {
    if (!resumeId) {
      setScores({})
      return
    }
    try {
      const matches = await matchResume(resumeId, 50)
      setScores(Object.fromEntries(matches.map((m) => [m.id, m.score])))
    } catch {
      setScores({})
    }
  }, [])

  const setActive = useCallback(
    (id: string | null) => {
      setActiveResumeId(id)
      if (id) localStorage.setItem(ACTIVE_KEY, id)
      else localStorage.removeItem(ACTIVE_KEY)
      refreshScores(id)
    },
    [refreshScores],
  )

  const refreshResumes = useCallback(async () => {
    const list = await listResumes()
    setResumes(list)
    return list
  }, [])

  const refreshJobs = useCallback(async () => {
    setJobs(await listJobs())
  }, [])

  const refreshStatuses = useCallback(async () => {
    try {
      const list = await listStatuses()
      setStatuses(Object.fromEntries(list.map((s) => [s.jobId, s.status])))
    } catch {
      setStatuses({})
    }
  }, [])

  const updateStatus = useCallback(
    async (jobId: string, status: JobStatusValue | null) => {
      setStatuses((prev) => {
        const next = { ...prev }
        if (status) next[jobId] = status
        else delete next[jobId]
        return next
      })
      try {
        if (status) await setStatus(jobId, status)
        else await clearStatus(jobId)
      } catch {
        refreshStatuses()
      }
    },
    [refreshStatuses],
  )

  useEffect(() => {
    ;(async () => {
      try {
        const [list] = await Promise.all([refreshResumes(), refreshJobs(), refreshStatuses()])
        const stored = localStorage.getItem(ACTIVE_KEY)
        const active = list.find((r) => r.id === stored)?.id ?? list[0]?.id ?? null
        setActive(active)
      } catch {
        setError('Could not load your data.')
      } finally {
        setLoading(false)
      }
    })()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleSearch(e: FormEvent) {
    e.preventDefault()
    if (!query.trim()) return
    setSearching(true)
    setError(null)
    try {
      await searchJobs({ query, maxDaysOld: days, limit: 20 })
      await refreshJobs()
      await refreshScores(activeResumeId)
    } catch {
      setError('Search failed. Browse the existing jobs below.')
    } finally {
      setSearching(false)
    }
  }

  async function onResumesChanged() {
    const list = await refreshResumes()
    if (activeResumeId && !list.some((r) => r.id === activeResumeId)) {
      setActive(list[0]?.id ?? null)
    }
  }

  function toggleChip(chip: string) {
    setChips((prev) => (prev.includes(chip) ? prev.filter((c) => c !== chip) : [...prev, chip]))
  }

  const savedCount = useMemo(() => Object.values(statuses).filter((s) => s === 'saved').length, [statuses])
  const appliedCount = useMemo(() => Object.values(statuses).filter((s) => s === 'applied').length, [statuses])

  const visibleJobs = useMemo(() => {
    const scored = Object.keys(scores).length > 0
    const needles = [...chips, textFilter].map((s) => s.trim().toLowerCase()).filter(Boolean)
    const text = (j: Job) => `${j.title} ${j.company ?? ''} ${j.location ?? ''} ${j.description}`.toLowerCase()
    return [...jobs]
      .filter((j) => (filter === 'all' ? true : statuses[j.id] === filter))
      .filter((j) => needles.every((n) => text(j).includes(n)))
      .sort((a, b) => {
        if (scored) return (scores[b.id] ?? -1) - (scores[a.id] ?? -1)
        const da = new Date(a.postedAt ?? a.createdAt).getTime()
        const db = new Date(b.postedAt ?? b.createdAt).getTime()
        return db - da
      })
  }, [jobs, scores, statuses, filter, chips, textFilter])

  return (
    <div className="app-shell">
      <Navbar />

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
                <input
                  placeholder="Search filters"
                  value={textFilter}
                  onChange={(e) => setTextFilter(e.target.value)}
                />
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

          <div className="filter-tabs">
            <button type="button" className={filter === 'all' ? 'active' : ''} onClick={() => setFilter('all')}>
              All jobs
            </button>
            <button type="button" className={filter === 'saved' ? 'active' : ''} onClick={() => setFilter('saved')}>
              Saved{savedCount ? ` (${savedCount})` : ''}
            </button>
            <button
              type="button"
              className={filter === 'applied' ? 'active' : ''}
              onClick={() => setFilter('applied')}
            >
              Applied{appliedCount ? ` (${appliedCount})` : ''}
            </button>
          </div>

          <div className="section-label">Smart search &amp; results</div>

          <div className="job-list">
            {visibleJobs.map((job) => (
              <JobCard
                key={job.id}
                job={job}
                score={scores[job.id]}
                status={statuses[job.id]}
                onOpen={() => setSelectedJob(job)}
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

      {selectedJob && (
        <JobDetailDrawer
          job={selectedJob}
          activeResumeId={activeResumeId}
          status={statuses[selectedJob.id]}
          onSetStatus={(s) => updateStatus(selectedJob.id, s)}
          onClose={() => setSelectedJob(null)}
        />
      )}
    </div>
  )
}
