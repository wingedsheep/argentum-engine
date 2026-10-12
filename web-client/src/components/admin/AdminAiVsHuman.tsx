/**
 * Admin "AI vs humans": the built-in AI's record against human players, served by
 * `/api/stats/admin/ai-vs-human`. It's the screen to watch while improving the AI — the headline win
 * rate and its change against the previous period, the trend with its confidence band, the record of
 * each deployed build, per-mode records, the latest games, and the humans who play the AI most.
 *
 * Only games with at least one AI seat and one human seat count. Win rates are over decisive games
 * (draws excluded) and always carry a 95% interval: a week of eight games says much less than a week
 * of eighty, and the band is what stops a lucky week reading as progress.
 */
import { useEffect, useMemo, useState } from 'react'
import type React from 'react'
import {
  Area,
  Bar,
  BarChart,
  CartesianGrid,
  ComposedChart,
  Line,
  ReferenceLine,
  ResponsiveContainer,
  Scatter,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { type AiChange, type AiMatchOutcome, type AiRecord, type AiVsHumanReport, fetchAiVsHuman } from '@/api/adminStats'
import type { AdminAuth } from '@/api/adminAuth'
import { pageStyles } from '@/components/ui/PageShell'
import { GITHUB_REPOSITORY_URL } from '@/utils/links'
import { gameModeLabel } from './statFormat'
import { AdminScreen, Panel, StatCard, Table, cellStyle } from './adminUi'
import css from './aiVsHuman.module.css'

const AI = '#e66767'
const HUMAN = '#3987e5'
const DRAW = '#6b7080'
/** Engine AI changes — validated against the AI/human pair; markers also carry a shape and a count. */
const CHANGE = '#199e70'
const AXIS = '#8f98ac'

interface Range {
  key: string
  label: string
  days: number
  bucket: 'day' | 'week' | 'month'
  /** "vs previous 30 days" — how the delta chip names the comparison window. */
  previousLabel: string | null
}

const RANGES: Range[] = [
  { key: '30d', label: '30 days', days: 30, bucket: 'day', previousLabel: 'previous 30 days' },
  { key: '90d', label: '90 days', days: 90, bucket: 'week', previousLabel: 'previous 90 days' },
  { key: '1y', label: '1 year', days: 365, bucket: 'week', previousLabel: 'previous year' },
  { key: 'all', label: 'All time', days: 0, bucket: 'week', previousLabel: null },
]

const RANGE_STORAGE_KEY = 'admin.aiVsHuman.range'

function initialRange(): Range {
  try {
    const saved = localStorage.getItem(RANGE_STORAGE_KEY)
    return RANGES.find((r) => r.key === saved) ?? (RANGES[1] as Range)
  } catch {
    return RANGES[1] as Range
  }
}

export function AdminAiVsHuman({ auth, onBack }: { auth: AdminAuth; onBack: () => void }) {
  const [range, setRange] = useState<Range>(initialRange)
  const [report, setReport] = useState<AiVsHumanReport | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchAiVsHuman(auth, range.days, range.bucket)
      .then((r) => !cancelled && setReport(r))
      .catch((e: unknown) => !cancelled && setError(e instanceof Error ? e.message : 'Failed to load'))
      .finally(() => !cancelled && setLoading(false))
    return () => {
      cancelled = true
    }
  }, [auth, range])

  const pickRange = (r: Range) => {
    setRange(r)
    try {
      localStorage.setItem(RANGE_STORAGE_KEY, r.key)
    } catch {
      // Remembering the range is a convenience; a blocked storage just forgets it.
    }
  }

  const rangeTabs = (
    <div className={pageStyles.tabs} role="tablist" aria-label="Time range">
      {RANGES.map((r) => (
        <button
          key={r.key}
          type="button"
          role="tab"
          aria-selected={r.key === range.key}
          className={pageStyles.tab}
          onClick={() => pickRange(r)}
        >
          {r.label}
        </button>
      ))}
    </div>
  )

  return (
    <AdminScreen
      title="AI vs humans"
      subtitle="How the built-in AI fares against human players"
      onBack={onBack}
      backLabel="← Dashboard"
      right={rangeTabs}
    >
      <div className={css.root} data-loading={loading && report != null}>
        {error && <p className={css.error} role="alert">{error}</p>}
        {report == null ? (
          !error && <Panel><p style={cellStyle.muted}>Loading…</p></Panel>
        ) : report.overall.games === 0 ? (
          <Panel>
            <div className={css.empty}>
              <p className={css.emptyTitle}>No AI-vs-human games {range.days ? `in the last ${range.label}` : 'yet'}</p>
              <p className={css.emptyText}>
                A game counts once it has at least one AI seat and one human seat — a Quick Game against the AI,
                or a tournament or pod with an AI filling a seat.
              </p>
            </div>
          </Panel>
        ) : (
          <ReportView report={report} range={range} />
        )}
      </div>
    </AdminScreen>
  )
}

function ReportView({ report, range }: { report: AiVsHumanReport; range: Range }) {
  const { overall } = report
  /** A bucket picked on the trend chart narrows the AI-changes list to it. */
  const [selectedBucket, setSelectedBucket] = useState<string | null>(null)
  useEffect(() => setSelectedBucket(null), [report])
  const changesByBucket = useMemo(() => {
    const map = new Map<string, AiChange[]>()
    for (const c of report.aiChanges.changes) map.set(c.bucketStart, [...(map.get(c.bucketStart) ?? []), c])
    return map
  }, [report.aiChanges.changes])
  const changesByBuild = useMemo(() => {
    const map = new Map<string, AiChange[]>()
    for (const c of report.aiChanges.changes) if (c.shippedIn) map.set(c.shippedIn, [...(map.get(c.shippedIn) ?? []), c])
    return map
  }, [report.aiChanges.changes])
  return (
    <>
      <Panel>
        <Hero overall={overall} previous={report.previous} previousLabel={range.previousLabel} />
      </Panel>

      <div className={css.tiles}>
        <StatCard label="Games" value={overall.games.toLocaleString()} />
        <StatCard label="AI wins" value={overall.aiWins.toLocaleString()} />
        <StatCard label="Human wins" value={overall.humanWins.toLocaleString()} />
        <StatCard label="Draws" value={overall.draws.toLocaleString()} />
        <StatCard label="Builds seen" value={report.byBuild.length} />
      </div>

      <Panel
        title="AI win rate over time"
        subtitle={`Trailing ${TREND_WINDOW[report.bucket].label} rate over decisive games · click a ${report.bucket.toLowerCase()} to list its AI changes`}
        action={<TrendLegend bucket={report.bucket} showChanges={report.aiChanges.available} />}
      >
        <TrendCharts
          report={report}
          changesByBucket={changesByBucket}
          selectedBucket={selectedBucket}
          onSelectBucket={(b) => setSelectedBucket((cur) => (cur === b ? null : b))}
        />
      </Panel>

      <AiChangesPanel
        report={report}
        selectedBucket={selectedBucket}
        onClearBucket={() => setSelectedBucket(null)}
      />

      <div className={css.twoCol}>
        <Panel title="By build" subtitle="Each deployed build's record, newest first — is the latest one stronger?">
          {report.byBuild.length === 0 ? (
            <p style={cellStyle.muted}>No build information — builds are read from stored replays.</p>
          ) : (
            <ForestRows
              rows={report.byBuild.map((b, i) => ({
                key: b.version,
                title: (
                  <>
                    <span className={css.mono}>{shortVersion(b.version)}</span>
                    {i === 0 && <span className={css.latest}>Latest</span>}
                    <BuildChangesBadge count={b.newAiChanges} changes={changesByBuild.get(b.version) ?? []} />
                  </>
                ),
                titleText: b.version,
                sub: dateSpan(b.firstSeen, b.lastSeen),
                record: b.record,
              }))}
            />
          )}
        </Panel>
        <Panel title="By game mode" subtitle="Where the AI holds up, and where it struggles">
          <ForestRows
            rows={report.byMode.map((m) => {
              const { primary, variant } = gameModeLabel(m.gameMode, m.format)
              const games = `${m.record.games.toLocaleString()} game${m.record.games === 1 ? '' : 's'}`
              return {
                key: `${m.gameMode}~${m.format}`,
                title: primary,
                titleText: variant ? `${primary} › ${variant}` : primary,
                // The variant rides on the second line, so "Multiplayer › Two-Headed Giant" never truncates.
                sub: variant ? `${variant} · ${games}` : games,
                record: m.record,
              }
            })}
          />
        </Panel>
      </div>

      <div className={css.twoCol}>
        <Panel title="Recent form" subtitle={`The last ${report.recent.length} games, oldest to newest`} action={<OutcomeLegend />}>
          <RecentForm recent={report.recent} />
        </Panel>
        <Panel title="Humans vs the AI" subtitle="The players who have faced the AI most in this range">
          <HumansTable humans={report.topHumans} />
        </Panel>
      </div>

      <p className={css.footnote}>
        Counts every recorded game with at least one AI seat and one human seat. Win rates are over decisive games — a
        draw (or an AI and a human winning together on a Two-Headed Giant team) counts for neither side. Bands and
        whiskers are 95% Wilson intervals. A game's build comes from its stored replay. AI changes are the entries of{' '}
        <span className={css.mono}>ai/CHANGELOG.md</span>, placed on the day their PR merged; a build's{' '}
        <span className={css.buildBadge}>+n</span> counts the ones it carried that the build before it did not.
      </p>
    </>
  )
}

// ============================================================================
// Hero: headline rate + balance bar
// ============================================================================

function Hero({ overall, previous, previousLabel }: { overall: AiRecord; previous: AiRecord | null; previousLabel: string | null }) {
  const decisive = overall.aiWins + overall.humanWins
  const rate = overall.aiWinRate
  const aiShare = decisive === 0 ? 0.5 : overall.aiWins / decisive
  return (
    <div className={css.hero}>
      <div>
        <p className={css.heroLabel}>AI win rate</p>
        <p className={css.heroValue}>
          {rate == null ? '—' : pctNumber(rate)}
          {rate != null && <small>%</small>}
        </p>
        <p className={css.heroMeta}>
          {decisive.toLocaleString()} decisive game{decisive === 1 ? '' : 's'}
          {overall.aiWinRateLow != null && overall.aiWinRateHigh != null && (
            <>
              {' · '}95% CI {pct(overall.aiWinRateLow)}–{pct(overall.aiWinRateHigh)}
            </>
          )}
        </p>
        {previousLabel && <DeltaChip current={overall} previous={previous} previousLabel={previousLabel} />}
      </div>

      <div className={css.balance} aria-label={`AI ${pct(aiShare)} versus humans ${pct(1 - aiShare)} of decisive games`}>
        <div className={css.balanceSides}>
          <div className={css.side}>
            <span className={css.sideName}>
              <span className={`${css.swatch} ${css.swatchAi}`} aria-hidden />
              Engine AI
            </span>
            <span className={css.sidePct}>{pct(aiShare)}</span>
            <span>{overall.aiWins.toLocaleString()} wins</span>
          </div>
          <div className={`${css.side} ${css.sideRight}`}>
            <span className={css.sideName}>
              Humans
              <span className={`${css.swatch} ${css.swatchHuman}`} aria-hidden />
            </span>
            <span className={css.sidePct}>{pct(1 - aiShare)}</span>
            <span>{overall.humanWins.toLocaleString()} wins</span>
          </div>
        </div>
        <div className={css.bar}>
          <div className={css.barAi} style={{ flexGrow: overall.aiWins, flexBasis: 0 }} />
          <div className={css.barHuman} style={{ flexGrow: overall.humanWins, flexBasis: 0 }} />
          <div className={css.evenTick} title="Even — 50%" />
        </div>
        <div className={css.balanceFoot}>
          <span>{verdict(rate)}</span>
          <span>
            {overall.draws > 0 ? `${overall.draws.toLocaleString()} draw${overall.draws === 1 ? '' : 's'} not counted` : 'No draws'}
          </span>
        </div>
      </div>
    </div>
  )
}

/**
 * The change in win rate against the previous window, in percentage points, with a plain-language
 * read of whether it's more than noise (a two-proportion z-test at 95%).
 */
function DeltaChip({ current, previous, previousLabel }: { current: AiRecord; previous: AiRecord | null; previousLabel: string }) {
  if (current.aiWinRate == null) return null
  if (previous == null || previous.aiWinRate == null) {
    return <span className={css.delta}>No games in the {previousLabel} to compare</span>
  }
  const points = (current.aiWinRate - previous.aiWinRate) * 100
  const dir = Math.abs(points) < 0.05 ? 'flat' : points > 0 ? 'up' : 'down'
  const arrow = dir === 'up' ? '▲' : dir === 'down' ? '▼' : '■'
  const significant = isSignificant(current, previous)
  return (
    <span className={css.delta} data-dir={dir} title={`Was ${pct(previous.aiWinRate)} over ${previous.aiWins + previous.humanWins} decisive games`}>
      <span aria-hidden>{arrow}</span>
      {points > 0 ? '+' : ''}
      {points.toFixed(1)} pts vs {previousLabel}
      <span className={css.deltaNote}>· {significant ? 'a real shift' : 'within noise'}</span>
    </span>
  )
}

function isSignificant(a: AiRecord, b: AiRecord): boolean {
  const n1 = a.aiWins + a.humanWins
  const n2 = b.aiWins + b.humanWins
  if (n1 === 0 || n2 === 0) return false
  const pooled = (a.aiWins + b.aiWins) / (n1 + n2)
  const se = Math.sqrt(pooled * (1 - pooled) * (1 / n1 + 1 / n2))
  if (se === 0) return false
  return Math.abs((a.aiWins / n1 - b.aiWins / n2) / se) >= 1.96
}

function verdict(rate: number | null): string {
  if (rate == null) return 'No decisive games'
  if (rate >= 0.55) return 'The AI has the upper hand'
  if (rate > 0.45) return 'An even contest'
  if (rate > 0.3) return 'Humans have the edge'
  return 'Humans dominate'
}

// ============================================================================
// Trend: a trailing win rate with its interval, the raw per-bucket rates, and the games behind them
// ============================================================================

/** How many buckets the trailing trend spans: a week of days, a month of weeks, a quarter of months. */
const TREND_WINDOW: Record<AiVsHumanReport['bucket'], { size: number; label: string }> = {
  DAY: { size: 7, label: '7-day' },
  WEEK: { size: 4, label: '4-week' },
  MONTH: { size: 3, label: '3-month' },
}

interface TrendPoint {
  start: string
  /** This bucket alone, in percent. */
  rate: number | null
  /** The trailing window ending at this bucket, in percent — the line to read. */
  trend: number | null
  band: [number, number] | null
  trendDecisive: number
  aiWins: number
  humanWins: number
  draws: number
  games: number
  aiChanges: number
  /** Pins the bucket's AI-change marker to the top of the plot; null when nothing merged. */
  changeMarker: number | null
}

function TrendCharts({
  report,
  changesByBucket,
  selectedBucket,
  onSelectBucket,
}: {
  report: AiVsHumanReport
  changesByBucket: Map<string, AiChange[]>
  selectedBucket: string | null
  onSelectBucket: (bucket: string) => void
}) {
  const trendWindow = TREND_WINDOW[report.bucket]
  const data: TrendPoint[] = useMemo(() => {
    const series = report.series
    return series.map(({ start, record: r, aiChanges }, i) => {
      let ai = 0
      let human = 0
      for (const { record: w } of series.slice(Math.max(0, i - trendWindow.size + 1), i + 1)) {
        ai += w.aiWins
        human += w.humanWins
      }
      const interval = wilson(ai, ai + human)
      return {
        start,
        rate: r.aiWinRate == null ? null : r.aiWinRate * 100,
        trend: interval ? (ai / (ai + human)) * 100 : null,
        band: interval ? [interval[0] * 100, interval[1] * 100] : null,
        trendDecisive: ai + human,
        aiWins: r.aiWins,
        humanWins: r.humanWins,
        draws: r.draws,
        games: r.games,
        aiChanges,
        changeMarker: aiChanges > 0 ? 100 : null,
      }
    })
  }, [report.series, trendWindow.size])
  const tick = (d: string) => tickLabel(d, report.bucket)
  const select = (state: { activeLabel?: string | number } | null) => {
    if (state?.activeLabel != null) onSelectBucket(String(state.activeLabel))
  }
  const selectedLine = selectedBucket && (
    <ReferenceLine x={selectedBucket} stroke={CHANGE} strokeWidth={1.5} strokeDasharray="4 3" ifOverflow="hidden" />
  )

  return (
    <div className={css.trendCharts}>
      <ResponsiveContainer width="100%" height={280}>
        <ComposedChart data={data} syncId="ai-vs-human" margin={{ top: 26, right: 12, bottom: 0, left: -12 }} onClick={select}>
          <CartesianGrid stroke="rgba(255,255,255,0.06)" vertical={false} />
          <XAxis dataKey="start" stroke={AXIS} fontSize={11} tickFormatter={tick} minTickGap={24} tickLine={false} />
          <YAxis
            stroke={AXIS}
            fontSize={11}
            domain={[0, 100]}
            ticks={[0, 25, 50, 75, 100]}
            tickFormatter={(v: number) => `${v}%`}
            tickLine={false}
            axisLine={false}
          />
          <Tooltip
            content={<TrendTooltip bucket={report.bucket} windowLabel={trendWindow.label} changesByBucket={changesByBucket} />}
            cursor={{ stroke: 'rgba(255,255,255,0.25)' }}
          />
          <ReferenceLine y={50} stroke={AXIS} strokeDasharray="5 4" strokeWidth={1.5} />
          {selectedLine}
          <Area dataKey="band" stroke="none" fill={AI} fillOpacity={0.16} isAnimationActive={false} activeDot={false} />
          {/* Each bucket on its own: dots only, so a thin week reads as one noisy sample, not a swing. */}
          <Line
            dataKey="rate"
            stroke="none"
            dot={{ r: 3, fill: 'rgba(230,103,103,0.55)', stroke: 'none' }}
            activeDot={{ r: 4, fill: 'rgba(230,103,103,0.8)', stroke: 'none' }}
            isAnimationActive={false}
          />
          <Line
            dataKey="trend"
            stroke={AI}
            strokeWidth={2.5}
            dot={false}
            activeDot={{ r: 5, fill: AI, stroke: 'rgba(10,12,20,0.95)', strokeWidth: 2 }}
            isAnimationActive={false}
          />
          {report.aiChanges.available && (
            <Scatter
              dataKey="changeMarker"
              isAnimationActive={false}
              shape={(props: unknown) => <ChangeMarker {...(props as ChangeMarkerProps)} selectedBucket={selectedBucket} />}
            />
          )}
        </ComposedChart>
      </ResponsiveContainer>
      <p className={css.subchartLabel}>Games played</p>
      <ResponsiveContainer width="100%" height={92}>
        <BarChart data={data} syncId="ai-vs-human" margin={{ top: 0, right: 12, bottom: 0, left: -12 }} barCategoryGap="18%" onClick={select}>
          <XAxis dataKey="start" hide />
          <YAxis stroke={AXIS} fontSize={10} allowDecimals={false} tickLine={false} axisLine={false} width={48} />
          {/* Synced with the rate chart, whose tooltip already tells the record — just highlight the column. */}
          <Tooltip content={() => null} cursor={{ fill: 'rgba(255,255,255,0.06)' }} />
          <Bar dataKey="aiWins" stackId="g" fill={AI} stroke="rgba(10,12,20,0.95)" strokeWidth={1} isAnimationActive={false} />
          <Bar dataKey="draws" stackId="g" fill={DRAW} stroke="rgba(10,12,20,0.95)" strokeWidth={1} isAnimationActive={false} />
          <Bar
            dataKey="humanWins"
            stackId="g"
            fill={HUMAN}
            stroke="rgba(10,12,20,0.95)"
            strokeWidth={1}
            radius={[3, 3, 0, 0]}
            isAnimationActive={false}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

interface ChangeMarkerProps {
  cx?: number
  cy?: number
  payload?: TrendPoint
}

/**
 * An AI-change marker above the plot: a downward tab with the bucket's merge count, so its shape and
 * number carry it as well as its colour. The picked bucket's marker is filled.
 */
function ChangeMarker({ cx, cy, payload, selectedBucket }: ChangeMarkerProps & { selectedBucket: string | null }) {
  if (cx == null || cy == null || !payload || payload.aiChanges === 0) return null
  const label = payload.aiChanges > 99 ? '99+' : String(payload.aiChanges)
  const w = Math.max(16, 6 + label.length * 6)
  const top = cy - 22
  const selected = payload.start === selectedBucket
  return (
    <g style={{ cursor: 'pointer' }}>
      <title>{`${payload.aiChanges} AI change${payload.aiChanges === 1 ? '' : 's'} merged`}</title>
      <path
        d={`M${cx - w / 2},${top + 3} q0,-3 3,-3 h${w - 6} q3,0 3,3 v9 q0,3 -3,3 h-${w / 2 - 7} l-4,4 l-4,-4 h-${w / 2 - 7} q-3,0 -3,-3 z`}
        fill={selected ? CHANGE : 'rgba(10,12,20,0.95)'}
        stroke={CHANGE}
        strokeWidth={1.5}
      />
      <text
        x={cx}
        y={top + 10.5}
        textAnchor="middle"
        fontSize={9.5}
        fontWeight={700}
        fill={selected ? '#06140f' : '#7fe3bf'}
        style={{ fontVariantNumeric: 'tabular-nums' }}
      >
        {label}
      </text>
    </g>
  )
}

/** The 95% Wilson score interval for `wins` of `n`; null when `n` is zero. Mirrors the server's `AiRecord`. */
function wilson(wins: number, n: number): [number, number] | null {
  if (n === 0) return null
  const p = wins / n
  const z2 = 1.96 * 1.96
  const centre = p + z2 / (2 * n)
  const margin = 1.96 * Math.sqrt((p * (1 - p)) / n + z2 / (4 * n * n))
  const denom = 1 + z2 / n
  return [Math.max(0, (centre - margin) / denom), Math.min(1, (centre + margin) / denom)]
}

function SwatchRow({ swatch, label, value }: { swatch: string; label: string; value: number }) {
  return (
    <div className={css.tooltipRow}>
      <span className={css.tooltipKey}>
        <span className={`${css.swatch} ${swatch}`} />
        {label}
      </span>
      <b>{value}</b>
    </div>
  )
}

/** How many change titles a tooltip lists before "and N more". */
const TOOLTIP_CHANGES = 3

function TrendTooltip({
  active,
  payload,
  bucket,
  windowLabel,
  changesByBucket,
}: {
  active?: boolean
  payload?: { payload?: TrendPoint }[]
  bucket: AiVsHumanReport['bucket']
  windowLabel: string
  changesByBucket: Map<string, AiChange[]>
}) {
  const p = active ? payload?.[0]?.payload : undefined
  if (!p) return null
  const unit = bucket.toLowerCase()
  const changes = changesByBucket.get(p.start) ?? []
  return (
    <div className={css.tooltip}>
      <div className={css.tooltipTitle}>{tooltipTitle(p.start, bucket)}</div>
      {p.trend != null && (
        <>
          <div className={css.tooltipRow}>
            {windowLabel} trend <b>{Math.round(p.trend)}%</b>
          </div>
          {p.band && (
            <div className={css.tooltipRow}>
              95% interval <b>{Math.round(p.band[0])}–{Math.round(p.band[1])}%</b>
            </div>
          )}
          <div className={css.tooltipMuted}>over {p.trendDecisive} decisive games</div>
          <div className={css.tooltipDivider} />
        </>
      )}
      {p.games === 0 ? (
        <div className={css.tooltipMuted}>No games this {unit}</div>
      ) : (
        <>
          <div className={css.tooltipRow}>
            This {unit} <b>{p.rate == null ? '—' : `${Math.round(p.rate)}%`}</b>
          </div>
          <SwatchRow swatch={css.swatchAi ?? ''} label="AI wins" value={p.aiWins} />
          <SwatchRow swatch={css.swatchHuman ?? ''} label="Human wins" value={p.humanWins} />
          {p.draws > 0 && <SwatchRow swatch={css.swatchDraw ?? ''} label="Draws" value={p.draws} />}
        </>
      )}
      {changes.length > 0 && (
        <>
          <div className={css.tooltipDivider} />
          <div className={css.tooltipChangesHead}>
            {changes.length} AI change{changes.length === 1 ? '' : 's'} merged
          </div>
          <ul className={css.tooltipChanges}>
            {changes.slice(0, TOOLTIP_CHANGES).map((c) => (
              <li key={c.sha}>{c.subject}</li>
            ))}
          </ul>
          <div className={css.tooltipMuted}>
            {changes.length > TOOLTIP_CHANGES ? `and ${changes.length - TOOLTIP_CHANGES} more · ` : ''}click to list
          </div>
        </>
      )}
    </div>
  )
}

function TrendLegend({ bucket, showChanges }: { bucket: AiVsHumanReport['bucket']; showChanges: boolean }) {
  return (
    <div className={css.legend}>
      <span className={css.legendItem}>
        <span className={css.legendLine} />
        {TREND_WINDOW[bucket].label} trend
      </span>
      <span className={css.legendItem}>
        <span className={css.legendDot} />
        Single {bucket.toLowerCase()}
      </span>
      <span className={css.legendItem}>
        <span className={css.legendBand} />
        95% interval
      </span>
      <span className={css.legendItem}>
        <span className={css.legendEven} />
        Even
      </span>
      {showChanges && (
        <span className={css.legendItem}>
          <span className={css.legendChange}>n</span>
          AI changes merged
        </span>
      )}
    </div>
  )
}

// ============================================================================
// Engine AI changes: what merged, and the build that first carried it
// ============================================================================

/** Rows shown before "Show all". */
const CHANGES_PREVIEW = 8

function AiChangesPanel({
  report,
  selectedBucket,
  onClearBucket,
}: {
  report: AiVsHumanReport
  selectedBucket: string | null
  onClearBucket: () => void
}) {
  const [expanded, setExpanded] = useState(false)
  const { available, changes } = report.aiChanges
  const shown = selectedBucket ? changes.filter((c) => c.bucketStart === selectedBucket) : changes
  const visible = expanded || selectedBucket ? shown : shown.slice(0, CHANGES_PREVIEW)
  const subtitle = selectedBucket
    ? `${shown.length} merged in the ${tooltipTitle(selectedBucket, report.bucket).replace(/^Week of/, 'week of')}`
    : `${changes.length} merged in this range, from ai/CHANGELOG.md — each with the first deployed build that carried it`

  const action = selectedBucket ? (
    <button type="button" className={css.filterChip} onClick={onClearBucket}>
      {tooltipTitle(selectedBucket, report.bucket)}
      <span aria-hidden>✕</span>
      <span className={css.srOnly}>Show every change</span>
    </button>
  ) : shown.length > CHANGES_PREVIEW ? (
    <button type="button" className={css.linkButton} onClick={() => setExpanded((v) => !v)}>
      {expanded ? `Show latest ${CHANGES_PREVIEW}` : `Show all ${shown.length}`}
    </button>
  ) : undefined

  return (
    <Panel title="Engine AI changes" {...(available ? { subtitle } : {})} action={action}>
      {!available ? (
        <p style={cellStyle.muted}>
          This server build was made without git history, so it can't tell when the AI changed. Deploys build with
          full history; a local build from a full clone has it too.
        </p>
      ) : shown.length === 0 ? (
        <p style={cellStyle.muted}>
          {selectedBucket ? 'No AI changes merged then.' : 'No AI changes logged in ai/CHANGELOG.md for this range.'}
        </p>
      ) : (
        <ol className={css.changes}>
          {visible.map((c) => (
            <li key={c.sha} className={css.change}>
              <span className={css.changeDate} title={new Date(c.committedAt).toLocaleString()}>
                {shortDate(c.committedAt)}
              </span>
              <span className={css.changeMain}>
                <span className={css.changeSubject}>{c.subject}</span>
                <span className={css.changeMeta}>
                  {c.pr != null ? (
                    <a href={`${GITHUB_REPOSITORY_URL}/pull/${c.pr}`} target="_blank" rel="noreferrer" className={css.changeLink}>
                      #{c.pr}
                    </a>
                  ) : (
                    <a href={`${GITHUB_REPOSITORY_URL}/commit/${c.sha}`} target="_blank" rel="noreferrer" className={css.changeLink}>
                      <span className={css.mono}>{c.sha.slice(0, 7)}</span>
                    </a>
                  )}
                </span>
              </span>
              <span className={css.changeShipped}>
                {c.shippedIn ? (
                  <>
                    <span className={css.buildChip}>{shortVersion(c.shippedIn)}</span>
                    <span className={css.changeShippedWhen}>live {shortDate(c.shippedAt ?? c.committedAt)}</span>
                  </>
                ) : (
                  <span className={css.changeShippedWhen}>No games on it yet</span>
                )}
              </span>
            </li>
          ))}
        </ol>
      )}
    </Panel>
  )
}

/** A "+3" chip — the timeline marker's look — on a build that added AI changes; titles on hover. */
function BuildChangesBadge({ count, changes }: { count: number | null; changes: AiChange[] }) {
  if (count == null || count === 0) return null
  const head = `${count} AI change${count === 1 ? '' : 's'} since the build before`
  const titles = [head, ...changes.map((c) => `• ${c.subject}`)].join('\n')
  return (
    <span className={css.buildBadge} title={titles} aria-label={head}>
      +{count}
    </span>
  )
}

function OutcomeLegend() {
  return (
    <div className={css.legend}>
      <span className={css.legendItem}>
        <span className={`${css.swatch} ${css.swatchAi}`} />
        AI won
      </span>
      <span className={css.legendItem}>
        <span className={`${css.swatch} ${css.swatchHuman}`} />
        Human won
      </span>
      <span className={css.legendItem}>
        <span className={css.swatch} style={{ boxShadow: `inset 0 0 0 2px ${DRAW}` }} />
        Draw
      </span>
    </div>
  )
}

// ============================================================================
// Forest rows: each record as a point on a 0–100% track with its interval
// ============================================================================

interface ForestRow {
  key: string
  title: React.ReactNode
  titleText: string
  sub: React.ReactNode
  record: AiRecord
}

function ForestRows({ rows }: { rows: ForestRow[] }) {
  return (
    <div className={css.rows}>
      <div className={`${css.row} ${css.rowHead}`}>
        <span />
        <div className={css.trackAxis}>
          <span>0%</span>
          <span>50%</span>
          <span>100%</span>
        </div>
        <span className={css.rowValue}>AI wins</span>
      </div>
      {rows.map((row) => {
        const { aiWinRate: rate, aiWinRateLow: low, aiWinRateHigh: high } = row.record
        const color = rate == null ? DRAW : rate >= 0.5 ? AI : HUMAN
        const decisive = row.record.aiWins + row.record.humanWins
        return (
          <div key={row.key} className={css.row}>
            <div className={css.rowLabel}>
              <span className={css.rowTitle} title={row.titleText}>{row.title}</span>
              <span className={css.rowSub}>{row.sub}</span>
            </div>
            <div
              className={css.track}
              title={rate == null ? 'No decisive games' : `${pct(rate)} (95% CI ${pct(low ?? 0)}–${pct(high ?? 1)})`}
            >
              <div className={css.trackLine} />
              <div className={css.trackEven} />
              {low != null && high != null && (
                <div className={css.ci} style={{ left: `${low * 100}%`, width: `${(high - low) * 100}%`, background: color }} />
              )}
              {rate != null && <div className={css.dot} style={{ left: `${rate * 100}%`, background: color }} />}
            </div>
            <div className={css.rowValue}>
              <div className={css.rowPct}>{rate == null ? '—' : pct(rate)}</div>
              <div className={css.rowN}>
                {row.record.aiWins}–{row.record.humanWins}
                {decisive === 0 && row.record.draws > 0 ? ` · ${row.record.draws}D` : ''}
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

// ============================================================================
// Recent form + humans
// ============================================================================

const OUTCOME_TEXT = { AI: 'AI won', HUMAN: 'Human won', DRAW: 'Draw' } as const

function RecentForm({ recent }: { recent: AiVsHumanReport['recent'] }) {
  const lastTen = recent.slice(-10)
  const lastTenAi = lastTen.filter((g) => g.outcome === 'AI').length
  const lastTenHuman = lastTen.filter((g) => g.outcome === 'HUMAN').length
  const streak = currentStreak(recent)
  const bestAi = longestRun(recent, 'AI')
  return (
    <>
      <div className={css.form} role="list">
        {recent.map((g, i) => {
          const { primary, variant } = gameModeLabel(g.gameMode, g.format)
          const label = `${new Date(g.endedAt).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })} · ${
            variant ? `${primary} › ${variant}` : primary
          } · ${OUTCOME_TEXT[g.outcome]}`
          return <span key={i} role="listitem" className={css.pip} data-outcome={g.outcome} title={label} aria-label={label} />
        })}
      </div>
      <div className={css.formFoot}>
        <span>Older</span>
        <span>Newest →</span>
      </div>
      <div className={css.formStats}>
        <div className={css.formStat}>
          <div className={css.formStatValue}>
            {lastTenAi}–{lastTenHuman}
          </div>
          <div className={css.formStatLabel}>Last {lastTen.length}</div>
          <div className={css.formStatSub}>AI – humans</div>
        </div>
        <div className={css.formStat}>
          <div className={css.formStatValue}>
            {streak && <span className={`${css.swatch} ${streakSwatch(streak.outcome)}`} aria-hidden />}
            {streak ? `×${streak.length}` : '—'}
          </div>
          <div className={css.formStatLabel}>Current streak</div>
          <div className={css.formStatSub}>{streak ? streakText(streak.outcome) : 'No games'}</div>
        </div>
        <div className={css.formStat}>
          <div className={css.formStatValue}>
            <span className={`${css.swatch} ${css.swatchAi}`} aria-hidden />×{bestAi}
          </div>
          <div className={css.formStatLabel}>Best AI run</div>
          <div className={css.formStatSub}>Consecutive AI wins</div>
        </div>
      </div>
    </>
  )
}

function currentStreak(recent: AiVsHumanReport['recent']): { outcome: AiMatchOutcome; length: number } | null {
  const last = recent[recent.length - 1]
  if (!last) return null
  let length = 0
  for (let i = recent.length - 1; i >= 0 && recent[i]?.outcome === last.outcome; i--) length++
  return { outcome: last.outcome, length }
}

function longestRun(recent: AiVsHumanReport['recent'], outcome: AiMatchOutcome): number {
  let best = 0
  let run = 0
  for (const g of recent) {
    run = g.outcome === outcome ? run + 1 : 0
    best = Math.max(best, run)
  }
  return best
}

function streakSwatch(outcome: AiMatchOutcome): string {
  return (outcome === 'AI' ? css.swatchAi : outcome === 'HUMAN' ? css.swatchHuman : css.swatchDraw) ?? ''
}

function streakText(outcome: AiMatchOutcome): string {
  return outcome === 'AI' ? 'AI wins in a row' : outcome === 'HUMAN' ? 'Human wins in a row' : 'Draws in a row'
}

function HumansTable({ humans }: { humans: AiVsHumanReport['topHumans'] }) {
  if (humans.length === 0) return <p style={cellStyle.muted}>No human players in this range.</p>
  return (
    <Table head={['Player', 'Games', 'W–L–D', 'Beats the AI']}>
      {humans.map((h) => {
        const decisive = h.wins + h.losses
        const rate = decisive === 0 ? null : h.wins / decisive
        return (
          <tr key={`${h.isGuest ? 'g' : 'u'}:${h.name}`}>
            <td style={cellStyle.td}>
              <span style={{ color: '#f1f3f9' }}>{h.name}</span>
              {h.isGuest && <span className={css.guest}>guest</span>}
            </td>
            <td style={cellStyle.tdNum}>{h.games}</td>
            <td style={cellStyle.tdNum} className={css.nowrap}>
              {h.wins}–{h.losses}–{h.draws}
            </td>
            <td style={cellStyle.tdNum}>
              <span className={css.miniBar}>
                <span className={css.miniTrack} aria-hidden>
                  <span className={css.miniFill} style={{ display: 'block', width: `${(rate ?? 0) * 100}%` }} />
                </span>
                {rate == null ? '—' : pct(rate)}
              </span>
            </td>
          </tr>
        )
      })}
    </Table>
  )
}

// ============================================================================
// Formatting
// ============================================================================

function pct(v: number): string {
  return `${Math.round(v * 100)}%`
}

function pctNumber(v: number): string {
  return (v * 100).toFixed(1)
}

/** A deploy sha shortened to git's 7; anything that isn't a sha (`dev`) stays as it is. */
function shortVersion(v: string): string {
  return /^[0-9a-f]{12,}$/i.test(v) ? v.slice(0, 7) : v
}

const utcDay = (iso: string) => new Date(iso.length === 10 ? `${iso}T00:00:00Z` : iso)

function shortDate(iso: string): string {
  return utcDay(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' })
}

function dateSpan(first: string, last: string): string {
  const a = shortDate(first)
  const b = shortDate(last)
  return a === b ? a : `${a} – ${b}`
}

function tickLabel(start: string, bucket: AiVsHumanReport['bucket']): string {
  if (bucket === 'MONTH') return utcDay(start).toLocaleDateString('en-US', { month: 'short', year: '2-digit', timeZone: 'UTC' })
  return shortDate(start)
}

function tooltipTitle(start: string, bucket: AiVsHumanReport['bucket']): string {
  const d = utcDay(start)
  if (bucket === 'MONTH') return d.toLocaleDateString('en-US', { month: 'long', year: 'numeric', timeZone: 'UTC' })
  if (bucket === 'WEEK') return `Week of ${d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' })}`
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', timeZone: 'UTC' })
}
