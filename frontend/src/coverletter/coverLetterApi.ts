import { apiStreamSse } from '../api'
import type { SseHandlers } from '../api'

/** Streams a tailored cover letter for the given résumé + job. */
export function streamCoverLetter(
  resumeId: string,
  jobId: string,
  handlers: SseHandlers,
  signal?: AbortSignal,
): Promise<void> {
  return apiStreamSse(`/api/resumes/${resumeId}/jobs/${jobId}/cover-letter`, handlers, signal)
}
