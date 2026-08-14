import { useEffect, useState } from 'react'

/** Animated ATS gauge (0–100) — count-up number + ring fill, colored by level. */
export function AtsGauge({ score }: { score: number }) {
  const target = Math.max(0, Math.min(100, score))
  const [shown, setShown] = useState(0)

  useEffect(() => {
    let raf = 0
    let start = 0
    const duration = 800
    const tick = (t: number) => {
      if (!start) start = t
      const p = Math.min(1, (t - start) / duration)
      const eased = 1 - Math.pow(1 - p, 3)
      setShown(target * eased)
      if (p < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
    return () => cancelAnimationFrame(raf)
  }, [target])

  const level = target >= 75 ? 'good' : target >= 50 ? 'mid' : 'low'
  const size = 70
  const stroke = 6
  const r = (size - stroke) / 2
  const c = 2 * Math.PI * r
  const offset = c * (1 - shown / 100)
  const mid = size / 2

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className={`ats-ring ${level}`}>
      <circle cx={mid} cy={mid} r={r} className="ring-track" strokeWidth={stroke} fill="none" />
      <circle
        cx={mid}
        cy={mid}
        r={r}
        className="ats-ring-progress"
        strokeWidth={stroke}
        fill="none"
        strokeDasharray={c}
        strokeDashoffset={offset}
        strokeLinecap="round"
        transform={`rotate(-90 ${mid} ${mid})`}
      />
      <text x="50%" y="45%" dominantBaseline="central" textAnchor="middle" className="ats-ring-num">
        {Math.round(shown)}
      </text>
      <text x="50%" y="68%" dominantBaseline="central" textAnchor="middle" className="ats-ring-cap">
        ATS
      </text>
    </svg>
  )
}
