import { apiFetch, apiUpload } from '../api'
import type { JobMatch, Resume, ResumeDetail } from '../types'

export function listResumes(): Promise<Resume[]> {
  return apiFetch<Resume[]>('/api/resumes')
}

export function getResume(id: string): Promise<ResumeDetail> {
  return apiFetch<ResumeDetail>(`/api/resumes/${id}`)
}

export function uploadResume(file: File): Promise<ResumeDetail> {
  const form = new FormData()
  form.append('file', file)
  return apiUpload<ResumeDetail>('/api/resumes', form)
}

export function deleteResume(id: string): Promise<void> {
  return apiFetch<void>(`/api/resumes/${id}`, { method: 'DELETE' })
}

export function matchResume(id: string, limit = 5): Promise<JobMatch[]> {
  return apiFetch<JobMatch[]>(`/api/resumes/${id}/matches?limit=${limit}`)
}
