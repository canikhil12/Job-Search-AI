import { apiFetch, apiUpload } from '../api'
import type { Resume, ResumeDetail } from '../types'

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
