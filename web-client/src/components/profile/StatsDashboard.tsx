/**
 * Presentational stats dashboard shared by the signed-in /stats page and read-only public profiles
 * (/u/:userId). It renders everything analytic — win-rate donut, ranked rating cards + chart, the
 * colors / card-types / mana-curve / creature-types you play, most-played cards, head-to-head and
 * tournaments — from data passed in as props, so both call sites stay thin data-fetchers. Head-to-head
 * opponents with an account link to their own public profile. An optional read-only recent-games list
 * is shown when provided (public profiles); the signed-in page keeps its own paginated table.
 */
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
import { colorForIdentity, colorLabel } from '@/components/admin/statFormat'

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
}

const MODE_LABELS: Record<RankedModeName, string> = {
  LIMITED: 'Limited',
  CONSTRUCTED: 'Constructed',
  COMMANDER: 'Commander',
}
const MODE_COLORS: Record<RankedModeName, string> = {
  LIMITED: '#5bd1a0',
  CONSTRUCTED: '#5b6ee1',
  COMMANDER: '#d18b5b',
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
const PALETTE = ['#5b6ee1', '#5bd1a0', '#d15b9a', '#d1b85b', '#5bb8d1', '#d18b5b', '#9a7ad1', '#b0b6c0']

export function StatsDashboard(props: StatsDashboardData) {
  const { stats, ratings, ratingHistory, colors, cardTypes, curve, creatureTypes, modes, sets, topCards, opponents, tournaments, recentGames } = props
  const navigate = useNavigate()
  const hasData = (stats?.games ?? 0) > 0

  return (
    <>
      {/* Overview: record + win-rate donut */}
      <div style={styles.overviewRow}>
        <div style={styles.tiles}>
          <Stat label="Games" value={stats?.games ?? 0} />
          <Stat label="Wins" value={stats?.wins ?? 0} />
          <Stat label="Losses" value={stats?.losses ?? 0} />
          <Stat label="Win rate" value={stats ? `${Math.round(stats.winRate * 100)}%` : '—'} />
        </div>
        {hasData && stats && (
          <div style={styles.donutCard}>
            <ResponsiveContainer width="100%" height={150}>
              <PieChart>
                <Pie
                  data={[
                    { name: 'Wins', value: stats.wins },
                    { name: 'Losses', value: stats.losses },
                  ]}
                  dataKey="value"
                  innerRadius={45}
                  outerRadius={64}
                  startAngle={90}
                  endAngle={-270}
                  stroke="none"
                >
                  <Cell fill="#5bd16e" />
                  <Cell fill="#e15b6e" />
                </Pie>
                <Tooltip contentStyle={tooltipStyle} />
              </PieChart>
            </ResponsiveContainer>
            <div style={styles.donutCenter}>
              <div style={styles.donutPct}>{Math.round(stats.winRate * 100)}%</div>
              <div style={styles.donutLabel}>win rate</div>
            </div>
          </div>
        )}
      </div>

      {/* Ranked rating */}
      {ratings.some((r) => r.gamesPlayed > 0) && (
        <Panel title="Ranked rating" wide>
          <div style={styles.ratingRow}>
            {ratings.map((r) => (
              <RatingCard key={r.mode} rating={r} />
            ))}
          </div>
          <RatingChart points={ratingHistory} />
        </Panel>
      )}

      <div style={styles.grid}>
        {colors.length > 0 && (
          <Panel title="Colors you play">
            <ColorsList colors={colors} />
          </Panel>
        )}

        {cardTypes.length > 0 && (
          <Panel title="Card types">
            <ResponsiveContainer width="100%" height={180}>
              <PieChart>
                <Pie data={cardTypes} dataKey="count" nameKey="label" cx="50%" cy="50%" outerRadius={78} stroke="none">
                  {cardTypes.map((t, i) => (
                    <Cell key={t.label} fill={TYPE_COLORS[t.label] ?? PALETTE[i % PALETTE.length]} />
                  ))}
                </Pie>
                <Tooltip contentStyle={tooltipStyle} />
              </PieChart>
            </ResponsiveContainer>
            <div style={styles.legend}>
              {cardTypes.map((t, i) => (
                <span key={t.label} style={styles.legendItem}>
                  <span style={{ ...styles.legendSwatch, backgroundColor: TYPE_COLORS[t.label] ?? PALETTE[i % PALETTE.length] }} />
                  <span style={styles.legendName}>{t.label}</span>
                  <span style={styles.legendCount}>{t.count}</span>
                </span>
              ))}
            </div>
          </Panel>
        )}

        {curve.some((c) => c.count > 0) && (
          <Panel title="Mana curve">
            <ResponsiveContainer width="100%" height={180}>
              <BarChart data={curve} margin={{ top: 8, right: 8, bottom: 4, left: -16 }}>
                <CartesianGrid stroke="#1f1f2e" vertical={false} />
                <XAxis dataKey="label" stroke="#888" fontSize={11} />
                <YAxis stroke="#666" fontSize={11} allowDecimals={false} />
                <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#ffffff10' }} />
                <Bar dataKey="count" fill="#5b6ee1" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </Panel>
        )}

        {creatureTypes.length > 0 && (
          <Panel title="Creature types you play most">
            <ResponsiveContainer width="100%" height={Math.max(160, creatureTypes.length * 26)}>
              <BarChart data={creatureTypes} layout="vertical" margin={{ top: 4, right: 16, bottom: 4, left: 8 }}>
                <XAxis type="number" stroke="#666" fontSize={11} allowDecimals={false} />
                <YAxis type="category" dataKey="label" stroke="#aaa" fontSize={11} width={92} />
                <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#ffffff10' }} />
                <Bar dataKey="count" fill="#5bd1a0" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </Panel>
        )}

        {modes.length > 0 && (
          <Panel title="Game modes">
            <ChipList items={modes.map((m) => `${prettyMode(m.label)} · ${m.count}`)} />
          </Panel>
        )}

        {sets.length > 0 && (
          <Panel title="Sets you play">
            <ChipList items={sets.map((sb) => `${sb.label} · ${sb.count}`)} />
          </Panel>
        )}

        {opponents.length > 0 && (
          <Panel title="Head to head">
            <p style={styles.subtle}>Most-played human opponents (AI excluded).</p>
            <SimpleTable head={['Opponent', 'W', 'L']}>
              {opponents.map((o, i) => (
                <tr key={`${o.opponent}-${i}`}>
                  <td style={styles.td}>
                    {o.opponentUserId ? (
                      <button type="button" style={styles.opponentLink} onClick={() => navigate(`/u/${o.opponentUserId}`)}>
                        {o.opponent}
                      </button>
                    ) : (
                      o.opponent
                    )}
                  </td>
                  <td style={styles.tdNum}>{o.wins}</td>
                  <td style={styles.tdNum}>{o.losses}</td>
                </tr>
              ))}
            </SimpleTable>
          </Panel>
        )}

        {topCards.length > 0 && (
          <Panel title="Most-played cards">
            <div style={styles.cardChips}>
              {topCards.map((c) => (
                <span key={c.cardName} style={styles.cardChip}>
                  <span style={styles.cardChipCount}>{c.copies}×</span>
                  {c.cardName}
                </span>
              ))}
            </div>
          </Panel>
        )}

        {tournaments.length > 0 && (
          <Panel title="Tournaments">
            <SimpleTable head={['Date', 'Tournament', 'Place']}>
              {tournaments.map((t, i) => (
                <tr key={`${t.endedAt}-${i}`}>
                  <td style={styles.td}>{t.endedAt.slice(0, 10)}</td>
                  <td style={styles.td}>{t.name ?? '—'}</td>
                  <td style={styles.tdNum}>
                    {t.placement}/{t.playerCount}
                  </td>
                </tr>
              ))}
            </SimpleTable>
          </Panel>
        )}
      </div>

      {recentGames && recentGames.length > 0 && (
        <Panel title="Recent games" wide>
          <SimpleTable head={['Date', 'Mode', 'Colors', 'Opponent', 'Result']}>
            {recentGames.map((g, i) => (
              <tr key={`${g.gameId}-${i}`}>
                <td style={styles.td}>{g.endedAt.slice(0, 10)}</td>
                <td style={styles.td}>{prettyMode(g.gameMode)}</td>
                <td style={styles.td}>
                  {g.colors ? (
                    <span style={styles.colorsCell}>
                      <span style={{ ...styles.colorDot, backgroundColor: colorForIdentity(g.colors) }} />
                      {colorLabel(g.colors)}
                    </span>
                  ) : (
                    '—'
                  )}
                </td>
                <td style={styles.td}>{g.opponents ?? '—'}</td>
                <td style={{ ...styles.tdNum, color: g.won ? '#5bd16e' : '#e15b6e' }}>{g.won ? 'Win' : 'Loss'}</td>
              </tr>
            ))}
          </SimpleTable>
        </Panel>
      )}
    </>
  )
}

/** Colors-you-play as mana-pip rows with proportional bars. Reads nicer than an axis-labelled chart. */
function ColorsList({ colors }: { colors: StatBucket[] }) {
  const max = Math.max(1, ...colors.map((c) => c.count))
  return (
    <div style={styles.colorList}>
      {colors.map((c) => (
        <div key={c.label || 'colorless'} style={styles.colorRow}>
          <span style={styles.colorPips}>{manaPips(c.label)}</span>
          <span style={styles.colorName}>{colorLabel(c.label)}</span>
          <span style={styles.colorBarTrack}>
            <span
              style={{ ...styles.colorBarFill, width: `${(c.count / max) * 100}%`, backgroundColor: colorForIdentity(c.label) }}
            />
          </span>
          <span style={styles.colorCount}>{c.count}</span>
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
    <i key={i} className={`ms ms-${ch.toLowerCase()} ms-cost`} style={{ marginRight: 3 }} aria-hidden />
  ))
}

function Stat({ label, value }: { label: string; value: number | string }) {
  return (
    <div style={styles.tile}>
      <div style={styles.tileValue}>{value}</div>
      <div style={styles.tileLabel}>{label}</div>
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
      return '#888'
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
    <div style={{ ...styles.ratingCard, borderColor: `${MODE_COLORS[rating.mode]}55` }}>
      <div style={styles.ratingMode}>{MODE_LABELS[rating.mode]}</div>
      <div style={styles.ratingValue}>{rating.rating}</div>
      <div style={{ ...styles.ratingTier, color: tierColor(rating.tier) }}>{rating.tier}</div>
      <div style={styles.ratingRecord}>{record}</div>
    </div>
  )
}

/** Rating over time, one filled line per mode (each connected across its own games). */
function RatingChart({ points }: { points: RatingPoint[] }) {
  if (points.length === 0) {
    return <p style={styles.muted}>Play ranked games to see your rating over time.</p>
  }
  const sorted = [...points].sort((a, b) => a.endedAt.localeCompare(b.endedAt))
  const data = sorted.map((p, i) => ({ idx: i, label: p.endedAt.slice(0, 10), [p.mode]: p.ratingAfter }))
  const modes = Array.from(new Set(points.map((p) => p.mode)))
  return (
    <div style={{ marginTop: 14 }}>
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
          <CartesianGrid stroke="#1f1f2e" />
          <XAxis dataKey="label" stroke="#666" fontSize={11} minTickGap={32} />
          <YAxis stroke="#666" fontSize={11} domain={['dataMin - 30', 'dataMax + 30']} allowDecimals={false} width={44} />
          <Tooltip contentStyle={tooltipStyle} />
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
      <div style={styles.legend}>
        {modes.map((m) => (
          <span key={m} style={styles.legendItem}>
            <span style={{ ...styles.legendSwatch, backgroundColor: MODE_COLORS[m] }} />
            <span style={styles.legendName}>{MODE_LABELS[m]}</span>
          </span>
        ))}
      </div>
    </div>
  )
}

function Panel({ title, children, wide }: { title: string; children: React.ReactNode; wide?: boolean }) {
  return (
    <div style={{ ...styles.panel, ...(wide ? styles.panelWide : {}) }}>
      <h2 style={styles.panelTitle}>{title}</h2>
      {children}
    </div>
  )
}

function ChipList({ items }: { items: string[] }) {
  return (
    <div style={styles.chipRow}>
      {items.map((it) => (
        <span key={it} style={styles.chip}>
          {it}
        </span>
      ))}
    </div>
  )
}

function SimpleTable({ head, children }: { head: string[]; children: React.ReactNode }) {
  return (
    <div style={styles.tableWrap}>
      <table style={styles.table}>
        <thead>
          <tr>
            {head.map((h, i) => (
              <th key={h} style={i === 0 ? styles.th : styles.thNum}>
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}

function prettyMode(mode: string | null): string {
  if (!mode) return '—'
  return mode
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

const tooltipStyle: React.CSSProperties = {
  backgroundColor: '#12121e',
  border: '1px solid #2a2a3e',
  borderRadius: 6,
  color: '#ddd',
  fontSize: 12,
}

const styles: Record<string, React.CSSProperties> = {
  muted: { margin: 0, color: '#888', fontSize: 14 },
  subtle: { margin: '0 0 8px', color: '#777', fontSize: 12 },
  overviewRow: { display: 'flex', gap: 14, flexWrap: 'wrap', alignItems: 'center' },
  tiles: { display: 'flex', gap: 12, flexWrap: 'wrap', flex: '2 1 360px' },
  tile: {
    flex: '1 1 110px',
    backgroundColor: '#14141f',
    border: '1px solid #2a2a3e',
    borderRadius: 12,
    padding: '16px 12px',
    textAlign: 'center',
  },
  tileValue: { color: '#fff', fontSize: 26, fontWeight: 700 },
  tileLabel: { color: '#888', fontSize: 12, marginTop: 4, textTransform: 'uppercase', letterSpacing: 0.5 },
  donutCard: {
    position: 'relative',
    flex: '1 1 180px',
    minWidth: 180,
    backgroundColor: '#14141f',
    border: '1px solid #2a2a3e',
    borderRadius: 12,
    padding: '10px 8px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  donutCenter: {
    position: 'absolute',
    inset: 0,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    pointerEvents: 'none',
  },
  donutPct: { color: '#fff', fontSize: 24, fontWeight: 800, lineHeight: 1 },
  donutLabel: { color: '#888', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.5, marginTop: 2 },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 14 },
  panel: { backgroundColor: '#14141f', border: '1px solid #2a2a3e', borderRadius: 12, padding: '16px 18px' },
  panelWide: { gridColumn: '1 / -1' },
  panelTitle: { margin: '0 0 12px', color: '#fff', fontSize: 16 },
  ratingRow: { display: 'flex', gap: 12, flexWrap: 'wrap' },
  ratingCard: {
    flex: '1 1 140px',
    backgroundColor: '#1a1a28',
    border: '1px solid #2a2a3e',
    borderRadius: 12,
    padding: '14px 12px',
    textAlign: 'center',
  },
  ratingMode: { color: '#9aa', fontSize: 12, textTransform: 'uppercase', letterSpacing: 0.5 },
  ratingValue: { color: '#fff', fontSize: 30, fontWeight: 800, lineHeight: 1.1, marginTop: 4 },
  ratingTier: { fontSize: 14, fontWeight: 700, marginTop: 2 },
  ratingRecord: { color: '#888', fontSize: 12, marginTop: 4 },
  colorList: { display: 'flex', flexDirection: 'column', gap: 10 },
  colorRow: { display: 'flex', alignItems: 'center', gap: 10 },
  colorPips: { display: 'inline-flex', minWidth: 70, fontSize: 15 },
  colorName: { color: '#cdd', fontSize: 13, width: 92, flexShrink: 0 },
  colorBarTrack: { flex: 1, height: 10, backgroundColor: '#1d1d2e', borderRadius: 999, overflow: 'hidden' },
  colorBarFill: { display: 'block', height: '100%', borderRadius: 999 },
  colorCount: { color: '#aab', fontSize: 12, width: 28, textAlign: 'right', fontVariantNumeric: 'tabular-nums' },
  legend: { display: 'flex', flexWrap: 'wrap', gap: '6px 14px', marginTop: 8, justifyContent: 'center' },
  legendItem: { display: 'inline-flex', alignItems: 'center', gap: 6, color: '#bbc', fontSize: 12 },
  legendSwatch: { width: 10, height: 10, borderRadius: 3, display: 'inline-block' },
  legendName: { color: '#cdd' },
  legendCount: { color: '#7f8694', fontVariantNumeric: 'tabular-nums' },
  chipRow: { display: 'flex', flexWrap: 'wrap', gap: 8 },
  chip: {
    backgroundColor: '#1d1d2e',
    border: '1px solid #2a2a3e',
    borderRadius: 999,
    padding: '4px 12px',
    color: '#cdd',
    fontSize: 13,
  },
  cardChips: { display: 'flex', flexWrap: 'wrap', gap: 8 },
  cardChip: {
    display: 'inline-flex',
    alignItems: 'center',
    gap: 6,
    backgroundColor: '#1d1d2e',
    border: '1px solid #2a2a3e',
    borderRadius: 8,
    padding: '4px 10px',
    color: '#cdd',
    fontSize: 13,
  },
  cardChipCount: { color: '#8b9bff', fontWeight: 700, fontVariantNumeric: 'tabular-nums' },
  tableWrap: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: { textAlign: 'left', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e' },
  thNum: { textAlign: 'right', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e' },
  td: { textAlign: 'left', color: '#ccc', padding: '6px 8px', borderBottom: '1px solid #1f1f2e' },
  tdNum: { textAlign: 'right', color: '#ccc', padding: '6px 8px', borderBottom: '1px solid #1f1f2e' },
  opponentLink: { background: 'none', border: 'none', color: '#8b9bff', cursor: 'pointer', fontSize: 13, padding: 0 },
  colorsCell: { display: 'inline-flex', alignItems: 'center', gap: 6 },
  colorDot: { width: 9, height: 9, borderRadius: 999, display: 'inline-block' },
}
