import { apiFetch } from '../api'
import type { AtsResult } from '../types'

export function getAts(resumeId: string, jobId: string): Promise<AtsResult> {
  return apiFetch<AtsResult>(`/api/resumes/${resumeId}/jobs/${jobId}/ats`)
}
