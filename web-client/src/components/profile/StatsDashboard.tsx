/**
 * Presentational stats dashboard shared by the signed-in /stats page and read-only public profiles
 * (/u/:userId). It renders everything analytic — win-rate donut, ranked rating cards + chart, the
 * colors / card-types / mana-curve / creature-types you play, most-played cards, head-to-head and
 * tournaments — from data passed in as props, so both call sites stay thin data-fetchers. Head-to-head
 * opponents with an account link to their own public profile. An optional read-only recent-games list
 * is shown when provided (public profiles); the signed-in page keeps its own paginated table.
 */
import { useState } from 'react'
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type {
  AccountStats,
  CardStat,
  GameHistoryEntry,
  HeadToHead,
  RankedModeName,
  RatingEntry,
  RatingPoint,
  StatBucket,
  UserTournamentEntry,
} from '@/api/account'
import { colorForIdentity, colorLabel, splitModeBucket } from '@/components/admin/statFormat'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview'
import { pageStyles as p } from '@/components/ui/PageShell'
import { TournamentDetailModal } from '@/components/profile/TournamentDetailModal'
import { GameHistoryList, TournamentList } from '@/components/profile/accountUi'
import type { StatsFilter } from '@/api/statsFilter'
import { NotCountedNote, OpponentSplit, StatsFilterBar } from '@/components/stats/StatsFilterBar'
import a from './account.module.css'

export interface StatsDashboardData {
  stats: AccountStats | null
  ratings: RatingEntry[]
  ratingHistory: RatingPoint[]
  colors: StatBucket[]
  cardTypes: StatBucket[]
  curve: StatBucket[]
  creatureTypes: StatBucket[]
  modes: StatBucket[]
  sets: StatBucket[]
  topCards: CardStat[]
  opponents: HeadToHead[]
  tournaments: UserTournamentEntry[]
  /** When provided, a compact read-only recent-games list is appended (public profiles). */
  recentGames?: GameHistoryEntry[]
  /** The slice every game-derived panel shows; with [onFilterChange], the filter bar is drawn. */
  filter?: StatsFilter
  onFilterChange?: (next: StatsFilter) => void
}

const MODE_LABELS: Record<RankedModeName, string> = {
  LIMITED: 'Limited',
  CONSTRUCTED: 'Constructed',
  COMMANDER: 'Commander',
}
const MODE_COLORS: Record<RankedModeName, string> = {
  LIMITED: '#5fd0b8',
  CONSTRUCTED: '#7d9bff',
  COMMANDER: '#f2b45c',
}
const TYPE_COLORS: Record<string, string> = {
  Creature: '#5bd1a0',
  Instant: '#5bb8d1',
  Sorcery: '#d15b9a',
  Artifact: '#b0b6c0',
  Enchantment: '#d1b85b',
  Planeswalker: '#d18b5b',
  Land: '#8a7a5b',
  Other: '#7a7f8c',
}
const PALETTE = ['#7d9bff', '#5bd1a0', '#d15b9a', '#d1b85b', '#5bb8d1', '#d18b5b', '#9a7ad1', '#b0b6c0']
const paletteColor = (i: number): string => PALETTE[i % PALETTE.length]!

export function StatsDashboard(props: StatsDashboardData) {
  const { stats, ratings, ratingHistory, colors, cardTypes, curve, creatureTypes, modes, sets, topCards, opponents, tournaments, recentGames, filter, onFilterChange } = props
  const navigate = useNavigate()
  const hasData = (stats?.games ?? 0) > 0
  const [openTournament, setOpenTournament] = useState<number | null>(null)

  return (
    <>
      {filter && onFilterChange && <StatsFilterBar filter={filter} onChange={onFilterChange} />}

      {/* Overview: record + win-rate donut, the period's split by opponent, and what isn't counted */}
      <section className={p.panel}>
        <div className={a.overview} data-donut={hasData}>
          <div className={a.overviewMain}>
            <div className={a.tiles}>
              <Stat label="Games" value={stats?.games ?? 0} />
              <Stat label="Wins" value={stats?.wins ?? 0} />
              <Stat label="Losses" value={stats?.losses ?? 0} />
              <Stat label="Win rate" value={stats && hasData ? `${Math.round(stats.winRate * 100)}%` : '—'} />
            </div>
            {stats && (
              <div className={a.overviewFoot}>
                {filter && onFilterChange && (stats.vsHumans.games > 0 || stats.vsAi.games > 0) && (
                  <OpponentSplit vsHumans={stats.vsHumans} vsAi={stats.vsAi} filter={filter} onChange={onFilterChange} />
                )}
                <NotCountedNote count={stats.notCounted} />
              </div>
            )}
          </div>
          {hasData && stats && (
            <div className={a.donut}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={[
                      { name: 'Wins', value: stats.wins },
                      { name: 'Losses', value: stats.losses },
                    ]}
                    dataKey="value"
                    innerRadius={52}
                    outerRadius={70}
                    startAngle={90}
                    endAngle={-270}
                    stroke="none"
                    paddingAngle={stats.wins > 0 && stats.losses > 0 ? 2 : 0}
                  >
                    <Cell fill="#5fd08a" />
                    <Cell fill="#e2686b" />
                  </Pie>
                  <Tooltip contentStyle={tooltipStyle} itemStyle={tooltipItemStyle} />
                </PieChart>
              </ResponsiveContainer>
              <div className={a.donutCenter}>
                <div className={a.donutPct}>{Math.round(stats.winRate * 100)}%</div>
                <div className={a.donutLabel}>win rate</div>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Ranked rating */}
      {ratings.some((r) => r.gamesPlayed > 0) && (
        <Panel title="Ranked rating">
          <div className={a.ratingRow}>
            {ratings.map((r) => (
              <RatingCard key={r.mode} rating={r} />
            ))}
          </div>
          <RatingChart points={ratingHistory} />
        </Panel>
      )}

      <div className={a.grid}>
        {colors.length > 0 && (
          <Panel title="Colors you play">
            <ColorsList colors={colors} />
          </Panel>
        )}

        {cardTypes.length > 0 && (
          <Panel title="Card types">
            <ResponsiveContainer width="100%" height={180}>
              <PieChart>
                <Pie data={cardTypes} dataKey="count" nameKey="label" cx="50%" cy="50%" innerRadius={44} outerRadius={78} stroke="none" paddingAngle={1}>
                  {cardTypes.map((t, i) => (
                    <Cell key={t.label} fill={TYPE_COLORS[t.label] ?? paletteColor(i)} />
                  ))}
                </Pie>
                <Tooltip contentStyle={tooltipStyle} itemStyle={tooltipItemStyle} />
              </PieChart>
            </ResponsiveContainer>
            <div className={a.legend}>
              {cardTypes.map((t, i) => (
                <span key={t.label} className={a.legendItem}>
                  <span className={a.legendSwatch} style={{ backgroundColor: TYPE_COLORS[t.label] ?? paletteColor(i) }} />
                  {t.label}
                  <span className={a.legendCount}>{t.count}</span>
                </span>
              ))}
            </div>
          </Panel>
        )}

        {curve.some((c) => c.count > 0) && (
          <Panel title="Mana curve">
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={curve} margin={{ top: 8, right: 8, bottom: 4, left: -16 }}>
                <CartesianGrid stroke={GRID} vertical={false} />
                <XAxis dataKey="label" stroke={AXIS} tickLine={false} fontSize={11} />
                <YAxis stroke={AXIS} tickLine={false} axisLine={false} fontSize={11} allowDecimals={false} />
                <Tooltip contentStyle={tooltipStyle} itemStyle={tooltipItemStyle} cursor={{ fill: 'rgba(255,255,255,0.06)' }} />
                <Bar dataKey="count" fill="#f2b45c" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </Panel>
        )}

        {creatureTypes.length > 0 && (
          <Panel title="Creature types you play most">
            <ResponsiveContainer width="100%" height={Math.max(160, creatureTypes.length * 26)}>
              <BarChart data={creatureTypes} layout="vertical" margin={{ top: 4, right: 16, bottom: 4, left: 8 }}>
                <XAxis type="number" stroke={AXIS} tickLine={false} fontSize={11} allowDecimals={false} />
                <YAxis type="category" dataKey="label" stroke={AXIS} tick={{ fill: '#c3c9d7' }} tickLine={false} axisLine={false} fontSize={11} width={92} />
                <Tooltip contentStyle={tooltipStyle} itemStyle={tooltipItemStyle} cursor={{ fill: 'rgba(255,255,255,0.06)' }} />
                <Bar dataKey="count" fill="#5fd0b8" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </Panel>
        )}

        {modes.length > 0 && (
          <Panel title="Game modes">
            <div className={a.chips}>
              {mergeModes(modes).map((m) => (
                <span key={`${m.primary}-${m.variant ?? ''}`} className={a.chip}>
                  {m.primary}
                  {m.variant ? <span className={a.chipVariant}>› {m.variant}</span> : null}
                  <span className={a.chipCount}>· {m.count}</span>
                </span>
              ))}
            </div>
          </Panel>
        )}

        {sets.length > 0 && (
          <Panel title="Sets you play">
            <div className={a.chips}>
              {sets.map((sb) => (
                <span key={sb.label} className={a.chip}>
                  {sb.label}
                  <span className={a.chipCount}>· {sb.count}</span>
                </span>
              ))}
            </div>
          </Panel>
        )}

        {opponents.length > 0 && (
          <Panel title="Head to head">
            <p className={a.subtle}>Most-played human opponents (AI excluded).</p>
            <div className={a.tableWrap}>
              <table className={a.table}>
                <thead>
                  <tr>
                    <th>Opponent</th>
                    <th className={a.num}>W</th>
                    <th className={a.num}>L</th>
                  </tr>
                </thead>
                <tbody>
                  {opponents.map((o, i) => (
                    <tr key={`${o.opponent}-${i}`}>
                      <td>
                        {o.opponentUserId ? (
                          <button type="button" className={a.link} onClick={() => navigate(`/u/${o.opponentUserId}`)}>
                            {o.opponent}
                          </button>
                        ) : (
                          o.opponent
                        )}
                      </td>
                      <td className={`${a.num} ${a.win}`}>{o.wins}</td>
                      <td className={`${a.num} ${a.loss}`}>{o.losses}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Panel>
        )}

        {topCards.length > 0 && (
          <Panel title="Most-played cards">
            <TopCards cards={topCards} />
          </Panel>
        )}

        {tournaments.length > 0 && (
          <Panel title="Tournaments" wide>
            <p className={a.subtle}>Open a tournament to see its final standings and every game’s replay.</p>
            <TournamentList tournaments={tournaments} onOpen={setOpenTournament} />
          </Panel>
        )}
      </div>

      {recentGames && recentGames.length > 0 && (
        <Panel title="Recent games">
          <GameHistoryList games={recentGames} />
        </Panel>
      )}

      {openTournament != null && (
        <TournamentDetailModal tournamentId={openTournament} onClose={() => setOpenTournament(null)} />
      )}
    </>
  )
}

/** Most-played cards as count chips, each showing the card art on hover (like the deck viewer). */
function TopCards({ cards }: { cards: CardStat[] }) {
  const [hover, setHover] = useState<{ name: string; imageUri: string | null; pos: { x: number; y: number } } | null>(null)
  return (
    <div className={a.chips}>
      {cards.map((c) => (
        <span
          key={c.cardName}
          className={a.cardChip}
          onMouseEnter={(e) => setHover({ name: c.cardName, imageUri: c.imageUri, pos: { x: e.clientX, y: e.clientY } })}
          onMouseMove={(e) => setHover({ name: c.cardName, imageUri: c.imageUri, pos: { x: e.clientX, y: e.clientY } })}
          onMouseLeave={() => setHover(null)}
        >
          <span className={a.cardChipCount}>{c.copies}×</span>
          {c.cardName}
        </span>
      ))}
      {hover && <HoverCardPreview name={hover.name} imageUri={hover.imageUri} pos={hover.pos} />}
    </div>
  )
}

/**
 * Merge raw mode buckets (keyed by a `"<gameMode>~<format>"` composite) into display rows, summing
 * any whose hierarchy label collapses to the same thing (e.g. several Quick Game engine formats).
 */
function mergeModes(modes: StatBucket[]): { primary: string; variant: string | null; count: number }[] {
  const acc = new Map<string, { primary: string; variant: string | null; count: number }>()
  for (const m of modes) {
    const { primary, variant } = splitModeBucket(m.label)
    const key = `${primary}|${variant ?? ''}`
    const cur = acc.get(key)
    if (cur) cur.count += m.count
    else acc.set(key, { primary, variant, count: m.count })
  }
  return [...acc.values()].sort((a, b) => b.count - a.count)
}

/** Colors-you-play as mana-pip rows with proportional bars. Reads nicer than an axis-labelled chart. */
function ColorsList({ colors }: { colors: StatBucket[] }) {
  const max = Math.max(1, ...colors.map((c) => c.count))
  return (
    <div className={a.colorList}>
      {colors.map((c) => (
        <div key={c.label || 'colorless'} className={a.colorRow}>
          <div className={a.colorHead}>
            <span className={a.colorPips}>{manaPips(c.label)}</span>
            <span className={a.colorName}>{colorLabel(c.label)}</span>
            <span className={a.colorCount}>{c.count}</span>
          </div>
          <span className={a.barTrack}>
            <span className={a.barFill} style={{ width: `${(c.count / max) * 100}%`, backgroundColor: colorForIdentity(c.label) }} />
          </span>
        </div>
      ))}
    </div>
  )
}

/** Render a WUBRG identity string as mana-font cost pips ("" → a single colorless pip). */
function manaPips(label: string) {
  const chars = label ? [...label] : ['C']
  return chars.map((ch, i) => (
    // eslint-disable-next-line react/no-array-index-key
    <i key={i} className={`ms ms-${ch.toLowerCase()} ms-cost`} aria-hidden />
  ))
}

function Stat({ label, value }: { label: string; value: number | string }) {
  return (
    <div className={p.stat}>
      <span className={p.statValue}>{value}</span>
      <span className={p.statLabel}>{label}</span>
    </div>
  )
}

function tierColor(tier: string): string {
  switch (tier) {
    case 'Mythic':
      return '#e15bd1'
    case 'Diamond':
      return '#5bd1d1'
    case 'Platinum':
      return '#9ad1e1'
    case 'Gold':
      return '#e1c45b'
    case 'Silver':
      return '#c0c4cc'
    case 'Bronze':
      return '#c08a5b'
    default:
      return '#9aa3b8'
  }
}

function RatingCard({ rating }: { rating: RatingEntry }) {
  const games = rating.gamesPlayed
  const record =
    games === 0
      ? 'Unrated'
      : rating.provisional
        ? `${games}/10 placement`
        : `${rating.wins}–${rating.losses}${rating.draws ? `–${rating.draws}` : ''}`
  return (
    <div className={a.ratingCard} style={{ borderTopColor: MODE_COLORS[rating.mode] }}>
      <span className={a.ratingMode}>{MODE_LABELS[rating.mode]}</span>
      <span className={a.ratingValue}>{rating.rating}</span>
      <span className={a.ratingTier} style={{ color: tierColor(rating.tier) }}>{rating.tier}</span>
      <span className={a.ratingRecord}>{record}</span>
    </div>
  )
}

/** Rating over time, one filled line per mode (each connected across its own games). */
function RatingChart({ points }: { points: RatingPoint[] }) {
  if (points.length === 0) {
    return <p className={a.muted} style={{ marginTop: 12 }}>Play ranked games to see your rating over time.</p>
  }
  const sorted = [...points].sort((a, b) => a.endedAt.localeCompare(b.endedAt))
  const data = sorted.map((p, i) => ({ idx: i, label: p.endedAt.slice(0, 10), [p.mode]: p.ratingAfter }))
  const modes = Array.from(new Set(points.map((p) => p.mode)))
  return (
    <div className={a.chart}>
      <ResponsiveContainer width="100%" height={240}>
        <AreaChart data={data} margin={{ top: 8, right: 16, bottom: 4, left: -8 }}>
          <defs>
            {modes.map((m) => (
              <linearGradient key={m} id={`grad-${m}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={MODE_COLORS[m]} stopOpacity={0.4} />
                <stop offset="100%" stopColor={MODE_COLORS[m]} stopOpacity={0} />
              </linearGradient>
            ))}
          </defs>
          <CartesianGrid stroke={GRID} vertical={false} />
          <XAxis dataKey="label" stroke={AXIS} tickLine={false} fontSize={11} minTickGap={32} />
          <YAxis stroke={AXIS} tickLine={false} axisLine={false} fontSize={11} domain={['dataMin - 30', 'dataMax + 30']} allowDecimals={false} width={44} />
          <Tooltip contentStyle={tooltipStyle} itemStyle={tooltipItemStyle} />
          {modes.map((m) => (
            <Area
              key={m}
              type="monotone"
              dataKey={m}
              name={MODE_LABELS[m]}
              stroke={MODE_COLORS[m]}
              strokeWidth={2}
              fill={`url(#grad-${m})`}
              connectNulls
              dot={false}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
      <div className={a.legend}>
        {modes.map((m) => (
          <span key={m} className={a.legendItem}>
            <span className={a.legendSwatch} style={{ backgroundColor: MODE_COLORS[m] }} />
            {MODE_LABELS[m]}
          </span>
        ))}
      </div>
    </div>
  )
}

function Panel({ title, children, wide }: { title: string; children: React.ReactNode; wide?: boolean }) {
  return (
    <section className={`${p.panel} ${wide ? a.wide : ''}`}>
      <h2 className={p.panelTitle} style={{ marginBottom: 14 }}>
        {title}
      </h2>
      {children}
    </section>
  )
}

const GRID = 'rgba(255,255,255,0.07)'
const AXIS = '#7c8498'

const tooltipStyle: React.CSSProperties = {
  backgroundColor: 'rgba(14,16,26,0.95)',
  border: '1px solid rgba(255,255,255,0.14)',
  borderRadius: 8,
  color: '#eef0f6',
  fontSize: 12,
}
const tooltipItemStyle: React.CSSProperties = { color: '#eef0f6' }
