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
import { ResumeManager } from '../resume/ResumeManager'

const ACTIVE_KEY = 'jobmatch.activeResume'

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
  const [managerOpen, setManagerOpen] = useState(false)
  const [loading, setLoading] = useState(true)
  const [searching, setSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [query, setQuery] = useState('')
  const [location, setLocation] = useState('')
  const [days, setDays] = useState(3)

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

  const updateStatus = useCallback(async (jobId: string, status: JobStatusValue | null) => {
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
      refreshStatuses() // reconcile on failure
    }
  }, [refreshStatuses])

  // Initial load.
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
      await searchJobs({ query, location, maxDaysOld: days, limit: 20 })
      await refreshJobs()
      await refreshScores(activeResumeId)
    } catch {
      setError('Search failed. If live search isn’t configured yet, browse the existing jobs below.')
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

  const savedCount = useMemo(
    () => Object.values(statuses).filter((s) => s === 'saved').length,
    [statuses],
  )
  const appliedCount = useMemo(
    () => Object.values(statuses).filter((s) => s === 'applied').length,
    [statuses],
  )

  const sortedJobs = useMemo(() => {
    const scored = Object.keys(scores).length > 0
    return [...jobs]
      .filter((j) => (filter === 'all' ? true : statuses[j.id] === filter))
      .sort((a, b) => {
        if (scored) return (scores[b.id] ?? -1) - (scores[a.id] ?? -1)
        const da = new Date(a.postedAt ?? a.createdAt).getTime()
        const db = new Date(b.postedAt ?? b.createdAt).getTime()
        return db - da
      })
  }, [jobs, scores, statuses, filter])

  return (
    <div className="app-shell">
      <Navbar
        resumes={resumes}
        activeResumeId={activeResumeId}
        onSelectResume={setActive}
        onManageResumes={() => setManagerOpen(true)}
      />

      <main className="board">
        <form className="search-bar" onSubmit={handleSearch}>
          <input
            className="search-query"
            placeholder="Search jobs — e.g. Java backend engineer"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <input
            className="search-location"
            placeholder="Location (optional)"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
          />
          <select value={days} onChange={(e) => setDays(Number(e.target.value))}>
            <option value={1}>Last 1 day</option>
            <option value={3}>Last 3 days</option>
            <option value={7}>Last week</option>
          </select>
          <button type="submit" disabled={searching}>
            {searching ? 'Searching…' : 'Search'}
          </button>
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

        <div className="board-meta muted small">
          {loading
            ? 'Loading…'
            : activeResumeId
              ? `${sortedJobs.length} jobs · ranked by match to your active résumé`
              : `${sortedJobs.length} jobs · pick a résumé (top-right) to see match scores`}
        </div>

        <div className="job-list">
          {sortedJobs.map((job) => (
            <JobCard
              key={job.id}
              job={job}
              score={scores[job.id]}
              status={statuses[job.id]}
              onOpen={() => setSelectedJob(job)}
              onToggleSave={() => updateStatus(job.id, statuses[job.id] === 'saved' ? null : 'saved')}
            />
          ))}
          {!loading && sortedJobs.length === 0 && (
            <p className="muted">
              {filter === 'all' ? 'No jobs yet. Try a search above.' : `No ${filter} jobs yet.`}
            </p>
          )}
        </div>
      </main>

      {selectedJob && (
        <JobDetailDrawer
          job={selectedJob}
          activeResumeId={activeResumeId}
          status={statuses[selectedJob.id]}
          onSetStatus={(s) => updateStatus(selectedJob.id, s)}
          onClose={() => setSelectedJob(null)}
        />
      )}

      {managerOpen && (
        <ResumeManager
          resumes={resumes}
          activeResumeId={activeResumeId}
          onClose={() => setManagerOpen(false)}
          onChanged={onResumesChanged}
          onSetActive={setActive}
        />
      )}
    </div>
  )
}
