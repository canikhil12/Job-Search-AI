import { apiStreamSse } from '../api'
import type { SseHandlers } from '../api'

/** Streams a job-tailored résumé rewrite. */
export function streamTailor(
  resumeId: string,
  jobId: string,
  handlers: SseHandlers,
  signal?: AbortSignal,
): Promise<void> {
  return apiStreamSse(`/api/resumes/${resumeId}/jobs/${jobId}/tailor`, handlers, signal)
}
