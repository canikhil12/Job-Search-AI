import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import type { Job, Resume } from '../types'
import { listResumes, matchResume } from '../resume/resumeApi'
import { listJobs, searchJobs } from '../jobs/jobApi'
import { clearStatus, listStatuses, setStatus } from '../jobstatus/jobStatusApi'
import type { JobStatusValue } from '../jobstatus/jobStatusApi'

const ACTIVE_KEY = 'jobmatch.activeResume'

interface WorkspaceValue {
  resumes: Resume[]
  activeResumeId: string | null
  jobs: Job[]
  scores: Record<string, number>
  statuses: Record<string, JobStatusValue>
  loading: boolean
  searching: boolean
  setActive: (id: string | null) => void
  refreshResumes: () => Promise<Resume[]>
  onResumesChanged: () => Promise<void>
  updateStatus: (jobId: string, status: JobStatusValue | null) => Promise<void>
  search: (query: string, days: number) => Promise<number>
  selectedJob: Job | null
  openJob: (job: Job) => void
  closeJob: () => void
}

const WorkspaceContext = createContext<WorkspaceValue | undefined>(undefined)

export function WorkspaceProvider({ children }: { children: ReactNode }) {
  const [resumes, setResumes] = useState<Resume[]>([])
  const [activeResumeId, setActiveResumeId] = useState<string | null>(() => localStorage.getItem(ACTIVE_KEY))
  const [jobs, setJobs] = useState<Job[]>([])
  const [scores, setScores] = useState<Record<string, number>>({})
  const [statuses, setStatuses] = useState<Record<string, JobStatusValue>>({})
  const [loading, setLoading] = useState(true)
  const [searching, setSearching] = useState(false)
  const [selectedJob, setSelectedJob] = useState<Job | null>(null)

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

  const search = useCallback(
    async (query: string, days: number) => {
      setSearching(true)
      try {
        const found = await searchJobs({ query, maxDaysOld: days, limit: 20 })
        await refreshJobs()
        await refreshScores(activeResumeId)
        return found.length
      } finally {
        setSearching(false)
      }
    },
    [activeResumeId, refreshJobs, refreshScores],
  )

  const onResumesChanged = useCallback(async () => {
    const list = await refreshResumes()
    if (activeResumeId && !list.some((r) => r.id === activeResumeId)) {
      setActive(list[0]?.id ?? null)
    }
  }, [activeResumeId, refreshResumes, setActive])

  useEffect(() => {
    ;(async () => {
      try {
        const [list] = await Promise.all([refreshResumes(), refreshJobs(), refreshStatuses()])
        const stored = localStorage.getItem(ACTIVE_KEY)
        setActive(list.find((r) => r.id === stored)?.id ?? list[0]?.id ?? null)
      } finally {
        setLoading(false)
      }
    })()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const value: WorkspaceValue = {
    resumes,
    activeResumeId,
    jobs,
    scores,
    statuses,
    loading,
    searching,
    setActive,
    refreshResumes,
    onResumesChanged,
    updateStatus,
    search,
    selectedJob,
    openJob: setSelectedJob,
    closeJob: () => setSelectedJob(null),
  }

  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useWorkspace() {
  const ctx = useContext(WorkspaceContext)
  if (!ctx) throw new Error('useWorkspace must be used within a WorkspaceProvider')
  return ctx
}
