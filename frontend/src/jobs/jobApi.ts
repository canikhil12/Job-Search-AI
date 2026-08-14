import { apiFetch } from '../api'
import type { Job } from '../types'

export function listJobs(): Promise<Job[]> {
  return apiFetch<Job[]>('/api/jobs')
}

export interface JobSearchParams {
  query: string
  location?: string
  maxDaysOld?: number
  limit?: number
}

export function searchJobs(params: JobSearchParams): Promise<Job[]> {
  return apiFetch<Job[]>('/api/jobs/search', {
    method: 'POST',
    body: JSON.stringify(params),
  })
}
