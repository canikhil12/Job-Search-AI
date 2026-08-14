import { useCallback, useEffect, useRef, useState } from 'react'
import Markdown from 'react-markdown'
import { ApiRequestError } from '../api'
import { streamAnalysis } from '../analysis/analysisApi'
import { streamCoverLetter } from '../coverletter/coverLetterApi'

type Kind = 'analysis' | 'cover-letter'

/** Streams an AI response (gap analysis or cover letter) and renders it as Markdown as it arrives. */
export function AiStreamPanel({ resumeId, jobId, kind }: { resumeId: string; jobId: string; kind: Kind }) {
  const [text, setText] = useState('')
  const [streaming, setStreaming] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)

  const run = useCallback(() => {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller
    setText('')
    setError(null)
    setStreaming(true)

    const stream = kind === 'analysis' ? streamAnalysis : streamCoverLetter
    stream(
      resumeId,
      jobId,
      {
        onToken: (t) => setText((prev) => prev + t),
        onError: (msg) => {
          setError(msg)
          setStreaming(false)
        },
      },
      controller.signal,
    )
      .then(() => setStreaming(false))
      .catch((err) => {
        if (controller.signal.aborted) return
        setError(err instanceof ApiRequestError ? err.message : 'Generation failed.')
        setStreaming(false)
      })
  }, [resumeId, jobId, kind])

  useEffect(() => {
    run()
    return () => abortRef.current?.abort()
  }, [run])

  const copy = () => navigator.clipboard?.writeText(text)

  return (
    <div className="ai-panel">
      <div className="ai-toolbar">
        <button type="button" className="link" onClick={run} disabled={streaming}>
          {streaming ? 'Generating…' : 'Regenerate'}
        </button>
        <button type="button" className="link" onClick={copy} disabled={!text}>
          Copy
        </button>
        {streaming && <span className="pulse">●</span>}
      </div>
      {error ? (
        <p className="error">{error}</p>
      ) : (
        <div className="ai-markdown">
          {text ? <Markdown>{text}</Markdown> : <span className="muted">Thinking…</span>}
        </div>
      )}
    </div>
  )
}
