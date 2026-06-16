import { useEffect, useMemo, useRef, useState } from 'react'
import { fetchProgressHistory, type ProgressPoint } from '@/api/setCoverage'
import styles from './SetCompletionPage.module.css'

// Chart geometry — mirrors card-implementation-progress.html, sized to fill most of the screen
// while still fitting within the viewport (no modal scrollbar).
const VW = 1000
const VH = 500
const M = { l: 64, r: 64, t: 18, b: 40 }
const PW = VW - M.l - M.r
const PH = VH - M.t - M.b
const BAR_FRAC = 0.45 // daily-add bars occupy the bottom 45% of the plot, on their own right axis

const fmt = (n: number) => n.toLocaleString('en-US')
const niceFull = (s: string) =>
  new Date(`${s}T00:00:00`).toLocaleDateString('en-US', { weekday: 'short', month: 'long', day: 'numeric' })
const monthLabel = (s: string) => new Date(`${s}T00:00:00`).toLocaleDateString('en-US', { month: 'short' })

/** Modal showing the cumulative distinct-cards-over-time chart (blue area/line + teal daily bars). */
export function ProgressChartOverlay({ onClose }: { onClose: () => void }) {
  const [points, setPoints] = useState<readonly ProgressPoint[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [hoverI, setHoverI] = useState<number | null>(null)
  const svgRef = useRef<SVGSVGElement>(null)

  useEffect(() => {
    let cancelled = false
    fetchProgressHistory()
      .then((d) => !cancelled && setPoints(d))
      .catch((e) => !cancelled && setError(e instanceof Error ? e.message : String(e)))
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const model = useMemo(() => {
    if (!points || points.length < 2) return null
    const n = points.length
    const totals = points.map((p) => p.total)
    const added = points.map((p) => p.added)
    const final = totals[n - 1] ?? 0
    const busiest = added.reduce((a, b) => Math.max(a, b), 0)
    const busiestI = added.indexOf(busiest)
    const activeDays = added.filter((a) => a > 0).length

    const maxY = Math.max(500, Math.ceil(final / 1000) * 1000)
    const addMax = Math.max(50, Math.ceil(busiest / 50) * 50)
    const X = (i: number) => M.l + (PW * i) / (n - 1)
    const Y = (v: number) => M.t + PH - (PH * v) / maxY
    const Yr = (v: number) => M.t + PH - PH * BAR_FRAC * (v / addMax)

    const yGrid: number[] = []
    for (let v = 0; v <= maxY; v += 1000) yGrid.push(v)
    const rStep = addMax / 3
    const rGrid: number[] = []
    for (let v = 0; v <= addMax + 0.5; v += rStep) rGrid.push(Math.round(v))

    const monthTicks: { i: number; label: string }[] = []
    let lastMonth = ''
    points.forEach((p, i) => {
      const m = p.date.slice(0, 7)
      if (m !== lastMonth) {
        lastMonth = m
        monthTicks.push({ i, label: monthLabel(p.date) })
      }
    })

    let dArea = `M ${X(0)} ${Y(0)}`
    let dLine = ''
    points.forEach((_, i) => {
      const x = X(i)
      const y = Y(totals[i] ?? 0)
      dArea += ` L ${x} ${y}`
      dLine += `${i ? ' L' : 'M'} ${x} ${y}`
    })
    dArea += ` L ${X(n - 1)} ${Y(0)} Z`

    const bw = Math.max(1.4, (PW / n) * 0.55)
    const bars = points
      .map((p, i) => ({ i, a: p.added }))
      .filter((b) => b.a > 0)
      .map((b) => ({ x: X(b.i) - bw / 2, y: Yr(b.a), w: bw, h: M.t + PH - Yr(b.a) }))

    return { n, totals, added, final, busiest, busiestI, activeDays, maxY, addMax, X, Y, yGrid, rGrid, monthTicks, dArea, dLine, bars }
  }, [points])

  function onMove(e: React.MouseEvent) {
    if (!model || !svgRef.current) return
    const r = svgRef.current.getBoundingClientRect()
    const px = ((e.clientX - r.left) / r.width) * VW
    const i = Math.max(0, Math.min(model.n - 1, Math.round(((px - M.l) / PW) * (model.n - 1))))
    setHoverI(i)
  }

  return (
    <div className={styles.overlayBackdrop} onClick={onClose}>
      <div className={styles.chartOverlay} onClick={(e) => e.stopPropagation()}>
        <header className={styles.chartHeader}>
          <div className={styles.chartHeadText}>
            <div className={styles.chartTitle}>Card implementation progress</div>
            <div className={styles.chartSub}>Distinct implemented cards, day by day since the project began</div>
          </div>
          <button className={styles.overlayClose} onClick={onClose} aria-label="Close">
            ✕
          </button>
        </header>

        {error && <div className={styles.error}>Couldn’t load progress: {error}</div>}
        {!points && !error && <div className={styles.loading}>Loading progress…</div>}

        {model && points && (
          <>
            <div className={styles.chartStats}>
              <div className={styles.chartStat}>
                <div className="k" style={statK}>
                  Distinct cards
                </div>
                <div className="v" style={statV}>
                  {fmt(model.final)} <small style={statSmall}>implemented</small>
                </div>
              </div>
              <div className={styles.chartStat}>
                <div className="k" style={statK}>
                  Active days
                </div>
                <div className="v" style={statV}>
                  {fmt(model.activeDays)} <small style={statSmall}>of {model.n}</small>
                </div>
              </div>
              <div className={styles.chartStat}>
                <div className="k" style={statK}>
                  Busiest day
                </div>
                <div className="v" style={statV}>
                  +{fmt(model.busiest)}{' '}
                  <small style={statSmall}>
                    {new Date(`${points[model.busiestI]!.date}T00:00:00`).toLocaleDateString('en-US', {
                      month: 'short',
                      day: 'numeric',
                    })}
                  </small>
                </div>
              </div>
            </div>

            <div className={styles.chartBox}>
              <svg
                ref={svgRef}
                className={styles.chartSvg}
                viewBox={`0 0 ${VW} ${VH}`}
                onMouseMove={onMove}
                onMouseLeave={() => setHoverI(null)}
              >
                <defs>
                  <linearGradient id="prgArea" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#7c9cff" stopOpacity="0.42" />
                    <stop offset="55%" stopColor="#7c9cff" stopOpacity="0.12" />
                    <stop offset="100%" stopColor="#7c9cff" stopOpacity="0" />
                  </linearGradient>
                  <linearGradient id="prgStroke" x1="0" y1="0" x2="1" y2="0">
                    <stop offset="0%" stopColor="#56e0c2" />
                    <stop offset="100%" stopColor="#7c9cff" />
                  </linearGradient>
                </defs>

                {/* gridlines + left (cumulative) axis */}
                {model.yGrid.map((v) => (
                  <g key={`y${v}`}>
                    <line className={styles.chartGridLine} x1={M.l} y1={model.Y(v)} x2={VW - M.r} y2={model.Y(v)} />
                    <text x={M.l - 10} y={model.Y(v) + 4} textAnchor="end" fill="#9fb2ff" fontSize="11">
                      {fmt(v)}
                    </text>
                  </g>
                ))}
                {/* right (added/day) axis */}
                {model.rGrid.map((v, k) => {
                  const yr = M.t + PH - PH * BAR_FRAC * (v / model.addMax)
                  return (
                    <text key={`r${k}`} x={VW - M.r + 10} y={yr + 4} textAnchor="start" fill="#56e0c2" fontSize="11">
                      {fmt(v)}
                    </text>
                  )
                })}
                {/* month x labels */}
                {model.monthTicks.map((t) => (
                  <text key={`m${t.i}`} x={model.X(t.i)} y={VH - 10} textAnchor="middle" fill="#5a6276" fontSize="11">
                    {t.label}
                  </text>
                ))}

                {/* daily-add bars */}
                {model.bars.map((b, k) => (
                  <rect key={`b${k}`} x={b.x} y={b.y} width={b.w} height={b.h} rx="1" fill="#56e0c2" opacity="0.7" />
                ))}

                {/* cumulative area + line */}
                <path d={model.dArea} fill="url(#prgArea)" />
                <path
                  d={model.dLine}
                  fill="none"
                  stroke="url(#prgStroke)"
                  strokeWidth="2.5"
                  strokeLinejoin="round"
                  style={{ filter: 'drop-shadow(0 4px 14px rgba(124,156,255,0.45))' }}
                />

                {/* hover crosshair + dot */}
                {hoverI != null && (
                  <>
                    <line
                      className={styles.chartCross}
                      x1={model.X(hoverI)}
                      y1={M.t}
                      x2={model.X(hoverI)}
                      y2={M.t + PH}
                    />
                    <circle className={styles.chartDot} cx={model.X(hoverI)} cy={model.Y(model.totals[hoverI] ?? 0)} r="5" />
                  </>
                )}
              </svg>

              {hoverI != null && points[hoverI] && (
                <div
                  className={styles.chartTip}
                  style={{
                    left: `${(model.X(hoverI) / VW) * 100}%`,
                    top: `${(model.Y(model.totals[hoverI] ?? 0) / VH) * 100}%`,
                  }}
                >
                  <div className="d" style={tipD}>
                    {niceFull(points[hoverI]!.date)}
                  </div>
                  <div className="t" style={tipT}>
                    {fmt(model.totals[hoverI] ?? 0)} cards
                  </div>
                  <div className="a" style={tipA}>
                    {(model.added[hoverI] ?? 0) > 0 ? `+${fmt(model.added[hoverI] ?? 0)} that day` : 'no new cards'}
                  </div>
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  )
}

// Inline styles for the nested text spans (CSS-module class selectors can't target child class names).
const statK: React.CSSProperties = { fontSize: 'var(--font-xs)', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em' }
const statV: React.CSSProperties = { fontSize: 'var(--font-xl)', fontWeight: 700, marginTop: 2 }
const statSmall: React.CSSProperties = { fontSize: 'var(--font-sm)', color: 'var(--text-faint)', fontWeight: 500 }
const tipD: React.CSSProperties = { color: 'var(--text-muted)', fontSize: 'var(--font-xs)', marginBottom: 4 }
const tipT: React.CSSProperties = { fontSize: 'var(--font-lg)', fontWeight: 700 }
const tipA: React.CSSProperties = { color: '#56e0c2', marginTop: 2 }
