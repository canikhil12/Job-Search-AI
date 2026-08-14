import { useEffect, useState } from 'react'

export function ScoreRing({ score, size = 62 }: { score: number; size?: number }) {
  const target = Math.max(0, Math.min(1, score))
  const [shown, setShown] = useState(0)

  // Animate the ring fill + number count-up on mount / when the score changes.
  useEffect(() => {
    let raf = 0
    let start = 0
    const duration = 750
    const tick = (t: number) => {
      if (!start) start = t
      const p = Math.min(1, (t - start) / duration)
      const eased = 1 - Math.pow(1 - p, 3) // ease-out cubic
      setShown(target * eased)
      if (p < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [target])

  const stroke = 6
  const r = (size - stroke) / 2
  const circumference = 2 * Math.PI * r
  const offset = circumference * (1 - shown)
  const mid = size / 2

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="score-ring">
      <defs>
        <linearGradient id="ringGradient" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#22d3ee" />
          <stop offset="100%" stopColor="#4ade80" />
        </linearGradient>
      </defs>
      <circle cx={mid} cy={mid} r={r} className="ring-track" strokeWidth={stroke} fill="none" />
      <circle
        cx={mid}
        cy={mid}
        r={r}
        className="ring-progress"
        strokeWidth={stroke}
        fill="none"
        strokeDasharray={circumference}
        strokeDashoffset={offset}
        strokeLinecap="round"
        transform={`rotate(-90 ${mid} ${mid})`}
      />
      <text x="50%" y="50%" dominantBaseline="central" textAnchor="middle" className="ring-label">
        {Math.round(shown * 100)}%
      </text>
    </svg>
  )
}
