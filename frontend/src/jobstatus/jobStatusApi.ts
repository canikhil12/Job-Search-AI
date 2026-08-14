import { apiFetch } from '../api'

export type JobStatusValue = 'saved' | 'applied'

export interface JobStatusEntry {
  jobId: string
  status: JobStatusValue
}

export function listStatuses(): Promise<JobStatusEntry[]> {
  return apiFetch<JobStatusEntry[]>('/api/jobs/statuses')
}

export function setStatus(jobId: string, status: JobStatusValue): Promise<void> {
  return apiFetch<void>(`/api/jobs/${jobId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  })
}

export function clearStatus(jobId: string): Promise<void> {
  return apiFetch<void>(`/api/jobs/${jobId}/status`, { method: 'DELETE' })
}
