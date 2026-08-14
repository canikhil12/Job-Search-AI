export function ScoreRing({ score, size = 62 }: { score: number; size?: number }) {
  const pct = Math.max(0, Math.min(1, score))
  const stroke = 6
  const r = (size - stroke) / 2
  const circumference = 2 * Math.PI * r
  const offset = circumference * (1 - pct)
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
        {Math.round(pct * 100)}%
      </text>
    </svg>
  )
}
