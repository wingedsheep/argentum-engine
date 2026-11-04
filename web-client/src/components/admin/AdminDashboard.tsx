/**
 * Admin Stats: global, cross-user stats served from `/api/stats/admin/*`. Headline totals, a
 * games-per-day chart, mode (broken down by format) / color distributions, the most-played cards and
 * per-card win rates, the sets played most in tournaments, recorded tournaments, and an IP-based
 * geolocation estimate of where players connect from.
 *
 * Read-only and gated behind the dashboard's shared {@link AdminAuth}. Raw IPs are never returned —
 * the server resolves them to coarse locations server-side. The screen scrolls itself (see AdminScreen).
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  type CardStat,
  type CardWinRate,
  type DailyCount,
  type DatabaseStats,
  type GeoBucket,
  type GlobalOverview,
  type TournamentSummary,
  fetchCardWinRates,
  fetchColorDistribution,
  fetchDatabaseStats,
  fetchGamesPerDay,
  fetchGeo,
  fetchModeDistribution,
  fetchOverview,
  fetchTopCards,
  fetchTournamentSets,
  fetchTournaments,
} from '@/api/adminStats'
import type { AdminAuth } from '@/api/adminAuth'
import type { StatBucket } from '@/api/account'
import { TournamentDetailModal } from '@/components/profile/TournamentDetailModal'
import { TournamentStatusBadge } from '@/components/tournament/TournamentStatusBadge'
import { colorLabel, formatBytes, formatCount, gameModeLabel, mergeModeBuckets } from './statFormat'
import { GeoMap } from './GeoMap'
import { AdminScreen, Panel, StatCard, Table, adminTheme, cellStyle, chartTooltipStyle } from './adminUi'
import { useAdminTournamentParam } from './adminNav'
import { describeFilter, isAllGames } from '@/api/statsFilter'
import { NotCountedNote, StatsFilterBar, useStatsFilterParam } from '@/components/stats/StatsFilterBar'

/**
 * Who played, for the games-per-day stack. Fixed order, validated on the dark admin surface
 * (dataviz validate_palette: lightness band, chroma, CVD and normal-vision separation all pass).
 */
const WHO_PLAYED = [
  { key: 'vsHumans', label: 'People only', color: '#4a8fd6' },
  { key: 'vsAi', label: 'One person vs AI', color: '#b97a30' },
  { key: 'mixed', label: 'Mixed table', color: '#9a7ad1' },
] as const

/** How many card rows to show before the "Show all" toggle — keeps the tables from running forever. */
const CARD_PREVIEW = 12

export function AdminDashboard({ auth, onBack }: { auth: AdminAuth; onBack: () => void }) {
  const [overview, setOverview] = useState<GlobalOverview | null>(null)
  const [perDay, setPerDay] = useState<DailyCount[]>([])
  const [modes, setModes] = useState<StatBucket[]>([])
  const [colors, setColors] = useState<StatBucket[]>([])
  const [cards, setCards] = useState<CardStat[]>([])
  const [winRates, setWinRates] = useState<CardWinRate[]>([])
  const [tournaments, setTournaments] = useState<TournamentSummary[]>([])
  const [tournamentSets, setTournamentSets] = useState<StatBucket[]>([])
  const [geo, setGeo] = useState<GeoBucket[]>([])
  const [database, setDatabase] = useState<DatabaseStats | null>(null)
  const [showAllCards, setShowAllCards] = useState(false)
  const [showAllWinRates, setShowAllWinRates] = useState(false)
  const [openTournament, setOpenTournament, closeTournament] = useAdminTournamentParam()
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useStatsFilterParam()

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [ov, pd, md, cl, cs, wr, tn, ts] = await Promise.all([
          fetchOverview(auth, filter),
          fetchGamesPerDay(auth, 30, filter.vs),
          fetchModeDistribution(auth, filter),
          fetchColorDistribution(auth, filter),
          fetchTopCards(auth, 100, filter),
          fetchCardWinRates(auth, 5, 100, filter),
          fetchTournaments(auth, 25),
          fetchTournamentSets(auth),
        ])
        if (cancelled) return
        setOverview(ov)
        setPerDay(pd)
        setModes(md)
        setColors(cl)
        setCards(cs)
        setWinRates(wr)
        setTournaments(tn)
        setTournamentSets(ts)
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load dashboard')
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [auth, filter])

  // Geo can be slow (external lookups) and the database panel scans every table; neither depends on
  // the filter, so they load once, apart from the game panels.
  useEffect(() => {
    let cancelled = false
    fetchGeo(auth).then((g) => !cancelled && setGeo(g)).catch(() => {})
    fetchDatabaseStats(auth).then((d) => !cancelled && setDatabase(d)).catch(() => {})
    return () => {
      cancelled = true
    }
  }, [auth])

  const sliceLabel = isAllGames(filter) ? null : describeFilter(filter)
  const perDayStacked = perDay.map((d) => ({ ...d, mixed: Math.max(0, d.count - d.vsHumans - d.vsAi) }))

  return (
    <AdminScreen title="Stats" subtitle="Global activity across every recorded game" onBack={onBack} backLabel="← Dashboard">
      {error && <p style={styles.error}>{error}</p>}

      <div style={styles.filterRow}>
        <StatsFilterBar filter={filter} onChange={setFilter} />
        {sliceLabel && <span style={cellStyle.muted}>Game panels show games {sliceLabel}.</span>}
        {overview && <NotCountedNote count={overview.notCountedGames} />}
      </div>

      <div style={styles.cardRow}>
        <StatCard label="Games" value={overview?.totalGames} />
        <StatCard label="People only" value={overview?.gamesVsHumans} />
        <StatCard label="vs AI" value={overview?.gamesVsAi} />
        <StatCard label="Players" value={overview?.totalPlayers} />
        <StatCard label="Accounts" value={overview?.totalAccounts} />
        <StatCard label="Tournaments" value={overview?.totalTournaments} />
        <StatCard label="Games (24h)" value={overview?.gamesLast24h} accent />
        <StatCard label="Games (7d)" value={overview?.gamesLast7d} accent />
      </div>

      <Panel title="Games per day (last 30 days)" subtitle={filter.vs === 'all' ? 'Split by who was at the table' : 'Counted games only'}>
        <ResponsiveContainer width="100%" height={220}>
          <BarChart data={perDayStacked} margin={{ top: 8, right: 16, bottom: 0, left: -16 }} barCategoryGap={2}>
            <CartesianGrid stroke="rgba(255,255,255,0.08)" strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="day" stroke="#8f98ac" fontSize={11} tickFormatter={(d: string) => d.slice(5)} />
            <YAxis stroke="#8f98ac" fontSize={11} allowDecimals={false} />
            <Tooltip contentStyle={chartTooltipStyle} cursor={{ fill: 'rgba(255,255,255,0.06)' }} />
            {filter.vs === 'all' ? (
              WHO_PLAYED.map((w, i) => (
                <Bar
                  key={w.key}
                  dataKey={w.key}
                  name={w.label}
                  stackId="who"
                  fill={w.color}
                  stroke="#141824"
                  strokeWidth={1}
                  radius={i === WHO_PLAYED.length - 1 ? [4, 4, 0, 0] : 0}
                />
              ))
            ) : (
              <Bar dataKey="count" name="Games" fill={WHO_PLAYED[filter.vs === 'humans' ? 0 : 1].color} radius={[4, 4, 0, 0]} />
            )}
            {filter.vs === 'all' && <Legend wrapperStyle={{ fontSize: 12, color: '#c3c9d7' }} iconType="square" />}
          </BarChart>
        </ResponsiveContainer>
      </Panel>

      <div style={styles.twoCol}>
        <Panel title="Game modes" subtitle="Broken down by format — e.g. Tournament › Draft vs Sealed">
          <BucketBars data={mergeModeBuckets(modes)} fill="#5b9ee1" />
        </Panel>
        <Panel title="Colors played" subtitle="People's decks only">
          <BucketBars data={colors.map((c) => ({ label: colorLabel(c.label), count: c.count }))} fill="#e1a35b" />
        </Panel>
      </div>

      <div style={styles.twoCol}>
        <Panel
          title="Most-played cards"
          subtitle="People's decks only"
          action={<ShowMoreToggle total={cards.length} expanded={showAllCards} onToggle={() => setShowAllCards((v) => !v)} />}
        >
          <Table head={['Card', 'Copies', 'Decks']}>
            {(showAllCards ? cards : cards.slice(0, CARD_PREVIEW)).map((c) => (
              <tr key={c.cardName}>
                <td style={cellStyle.td}>{c.cardName}</td>
                <td style={cellStyle.tdNum}>{c.copies}</td>
                <td style={cellStyle.tdNum}>{c.decks}</td>
              </tr>
            ))}
          </Table>
        </Panel>
        <Panel
          title="Highest win-rate cards (≥5 decks)"
          subtitle={filter.vs === 'humans' ? 'People’s decks, in games between people' : 'People’s decks'}
          action={<ShowMoreToggle total={winRates.length} expanded={showAllWinRates} onToggle={() => setShowAllWinRates((v) => !v)} />}
        >
          <Table head={['Card', 'Win %', 'Decks']}>
            {(showAllWinRates ? winRates : winRates.slice(0, CARD_PREVIEW)).map((c) => (
              <tr key={c.cardName}>
                <td style={cellStyle.td}>{c.cardName}</td>
                <td style={cellStyle.tdNum}>{Math.round(c.winRate * 100)}%</td>
                <td style={cellStyle.tdNum}>{c.decks}</td>
              </tr>
            ))}
          </Table>
        </Panel>
      </div>

      <Panel title="Sets played most in tournaments" subtitle="Counted once per tournament that used each set">
        <BucketBars data={tournamentSets} fill="#8f7ad9" />
      </Panel>

      <Panel title="Tournaments">
        {tournaments.length === 0 ? (
          <p style={cellStyle.muted}>No tournaments recorded yet.</p>
        ) : (
          <Table head={['Date', 'Name', 'Mode', 'Status', 'Players', 'Winner']}>
            {tournaments.map((t) => (
              <tr key={t.id} style={styles.clickableRow} onClick={() => setOpenTournament(t.id)}>
                <td style={cellStyle.td}>{t.endedAt.slice(0, 10)}</td>
                <td style={cellStyle.td}>{t.name ?? '—'}</td>
                <td style={cellStyle.td}>{tournamentModeLabel(t.gameMode, t.format)}</td>
                <td style={cellStyle.td}>
                  <TournamentStatusBadge status={t.status} />
                </td>
                <td style={cellStyle.tdNum}>{t.playerCount}</td>
                <td style={cellStyle.td}>{t.winnerName ?? '—'}</td>
              </tr>
            ))}
          </Table>
        )}
      </Panel>

      <Panel title="Where players connect from">
        {geo.length === 0 ? (
          <p style={cellStyle.muted}>Resolving locations…</p>
        ) : (
          <GeoMap buckets={geo} />
        )}
      </Panel>

      <Panel
        title="Database"
        subtitle={
          database
            ? `${database.databaseName} — ${formatBytes(database.databaseSizeBytes)} total (including catalogs)`
            : 'Row counts and on-disk size per table'
        }
      >
        <DatabaseTables stats={database} />
      </Panel>

      {openTournament != null && (
        <TournamentDetailModal tournamentId={openTournament} onClose={closeTournament} />
      )}
    </AdminScreen>
  )
}

/**
 * Per-table row counts and storage, largest first, with a totals row. Sizes are what the tables
 * themselves occupy; the whole-database figure in the panel subtitle is larger (it counts catalogs).
 */
function DatabaseTables({ stats }: { stats: DatabaseStats | null }) {
  if (!stats) return <p style={cellStyle.muted}>Measuring tables…</p>
  if (stats.tables.length === 0) return <p style={cellStyle.muted}>No tables found.</p>
  const sum = (pick: (t: DatabaseStats['tables'][number]) => number) => stats.tables.reduce((a, t) => a + pick(t), 0)
  return (
    <Table head={['Table', 'Rows', 'Data', 'Indexes', 'Total']}>
      {stats.tables.map((t) => (
        <tr key={t.tableName}>
          <td style={cellStyle.td}>{t.tableName}</td>
          <td style={cellStyle.tdNum}>{formatCount(t.rows)}</td>
          <td style={cellStyle.tdNum}>{formatBytes(t.tableBytes)}</td>
          <td style={cellStyle.tdNum}>{formatBytes(t.indexBytes)}</td>
          <td style={cellStyle.tdNum}>{formatBytes(t.totalBytes)}</td>
        </tr>
      ))}
      <tr>
        <td style={styles.totalCell}>{stats.tables.length} tables</td>
        <td style={styles.totalCellNum}>{formatCount(sum((t) => t.rows))}</td>
        <td style={styles.totalCellNum}>{formatBytes(sum((t) => t.tableBytes))}</td>
        <td style={styles.totalCellNum}>{formatBytes(sum((t) => t.indexBytes))}</td>
        <td style={styles.totalCellNum}>{formatBytes(sum((t) => t.totalBytes))}</td>
      </tr>
    </Table>
  )
}

/** "Show all (N) / Show top N" link rendered in a card panel's header when there's more to see. */
function ShowMoreToggle({ total, expanded, onToggle }: { total: number; expanded: boolean; onToggle: () => void }) {
  if (total <= CARD_PREVIEW) return null
  return (
    <button type="button" style={styles.toggle} onClick={onToggle}>
      {expanded ? `Show top ${CARD_PREVIEW}` : `Show all ${total}`}
    </button>
  )
}

/** A tournament's mode + deck format as a "Tournament › Draft"-style label (falls back to a dash). */
function tournamentModeLabel(gameMode: string | null, format: string | null): string {
  const { primary, variant } = gameModeLabel(gameMode, format)
  return variant ? `${primary} › ${variant}` : primary
}

function BucketBars({ data, fill }: { data: StatBucket[]; fill: string }) {
  if (data.length === 0) return <p style={cellStyle.muted}>No data yet.</p>
  return (
    <ResponsiveContainer width="100%" height={Math.max(120, data.length * 28)}>
      <BarChart data={data} layout="vertical" margin={{ top: 4, right: 16, bottom: 4, left: 8 }}>
        <XAxis type="number" stroke="#8f98ac" fontSize={11} allowDecimals={false} />
        <YAxis type="category" dataKey="label" stroke="#b4bccd" fontSize={11} width={90} />
        <Tooltip contentStyle={chartTooltipStyle} cursor={{ fill: '#ffffff10' }} />
        <Bar dataKey="count" fill={fill} radius={[0, 4, 4, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}

const styles: Record<string, React.CSSProperties> = {
  error: { color: adminTheme.bad, fontSize: 13, margin: 0 },
  // Four to a row (two on a phone), so the eight headline numbers never leave one orphaned.
  cardRow: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(45%, 200px), 1fr))', gap: 12 },
  filterRow: { display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '8px 16px' },
  twoCol: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 18 },
  toggle: { background: 'none', border: 'none', color: adminTheme.accent, cursor: 'pointer', fontSize: 12, padding: 0 },
  clickableRow: { cursor: 'pointer' },
  totalCell: { textAlign: 'left', color: adminTheme.text, fontWeight: 600, padding: '8px 10px', borderTop: `1px solid ${adminTheme.border}` },
  totalCellNum: { textAlign: 'right', color: adminTheme.text, fontWeight: 600, padding: '8px 10px', borderTop: `1px solid ${adminTheme.border}` },
}
