import { apiStreamSse } from '../api'
import type { SseHandlers } from '../api'

/** Streams the résumé↔job gap analysis, invoking handlers.onToken as text arrives. */
export function streamAnalysis(
  resumeId: string,
  jobId: string,
  handlers: SseHandlers,
  signal?: AbortSignal,
): Promise<void> {
  return apiStreamSse(`/api/resumes/${resumeId}/jobs/${jobId}/analysis`, handlers, signal)
}
