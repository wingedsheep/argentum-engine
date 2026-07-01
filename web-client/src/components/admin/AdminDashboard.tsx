/**
 * Admin Stats: global, cross-user stats served from `/api/stats/admin/*`. Headline totals, a
 * games-per-day chart, mode/color distributions, the most-played cards and per-card win rates,
 * recorded tournaments, and an IP-based geolocation estimate of where players connect from.
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
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  type CardStat,
  type CardWinRate,
  type DailyCount,
  type GeoBucket,
  type GlobalOverview,
  type TournamentSummary,
  fetchCardWinRates,
  fetchColorDistribution,
  fetchGamesPerDay,
  fetchGeo,
  fetchModeDistribution,
  fetchOverview,
  fetchTopCards,
  fetchTournaments,
} from '@/api/adminStats'
import type { AdminAuth } from '@/api/adminAuth'
import type { StatBucket } from '@/api/account'
import { TournamentDetailModal } from '@/components/profile/TournamentDetailModal'
import { TournamentStatusBadge } from '@/components/tournament/TournamentStatusBadge'
import { colorLabel } from './statFormat'
import { GeoMap } from './GeoMap'
import { AdminScreen, Panel, StatCard, Table, adminTheme, cellStyle, chartTooltipStyle } from './adminUi'

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
  const [geo, setGeo] = useState<GeoBucket[]>([])
  const [showAllCards, setShowAllCards] = useState(false)
  const [showAllWinRates, setShowAllWinRates] = useState(false)
  const [openTournament, setOpenTournament] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [ov, pd, md, cl, cs, wr, tn] = await Promise.all([
          fetchOverview(auth),
          fetchGamesPerDay(auth, 30),
          fetchModeDistribution(auth),
          fetchColorDistribution(auth),
          fetchTopCards(auth, 100),
          fetchCardWinRates(auth, 5, 100),
          fetchTournaments(auth, 25),
        ])
        if (cancelled) return
        setOverview(ov)
        setPerDay(pd)
        setModes(md)
        setColors(cl)
        setCards(cs)
        setWinRates(wr)
        setTournaments(tn)
        // Geo can be slow (external lookups) — load it separately so the rest renders first.
        fetchGeo(auth).then((g) => !cancelled && setGeo(g)).catch(() => {})
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'Failed to load dashboard')
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [auth])

  return (
    <AdminScreen title="Stats" subtitle="Global activity across every recorded game" onBack={onBack} backLabel="← Dashboard">
      {error && <p style={styles.error}>{error}</p>}

      <div style={styles.cardRow}>
        <StatCard label="Games" value={overview?.totalGames} />
        <StatCard label="Players" value={overview?.totalPlayers} />
        <StatCard label="Accounts" value={overview?.totalAccounts} />
        <StatCard label="Tournaments" value={overview?.totalTournaments} />
        <StatCard label="Games (24h)" value={overview?.gamesLast24h} accent />
        <StatCard label="Games (7d)" value={overview?.gamesLast7d} accent />
      </div>

      <Panel title="Games per day (last 30 days)">
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={perDay} margin={{ top: 8, right: 16, bottom: 0, left: -16 }}>
            <CartesianGrid stroke="#22223a" strokeDasharray="3 3" />
            <XAxis dataKey="day" stroke="#666" fontSize={11} tickFormatter={(d: string) => d.slice(5)} />
            <YAxis stroke="#666" fontSize={11} allowDecimals={false} />
            <Tooltip contentStyle={chartTooltipStyle} />
            <Line type="monotone" dataKey="count" stroke={adminTheme.accentSolid} strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </Panel>

      <div style={styles.twoCol}>
        <Panel title="Game modes">
          <BucketBars data={modes} fill="#5b9ee1" />
        </Panel>
        <Panel title="Colors played">
          <BucketBars data={colors.map((c) => ({ label: colorLabel(c.label), count: c.count }))} fill="#e1a35b" />
        </Panel>
      </div>

      <div style={styles.twoCol}>
        <Panel
          title="Most-played cards"
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

      <Panel title="Tournaments">
        {tournaments.length === 0 ? (
          <p style={cellStyle.muted}>No tournaments recorded yet.</p>
        ) : (
          <Table head={['Date', 'Name', 'Mode', 'Status', 'Players', 'Winner']}>
            {tournaments.map((t) => (
              <tr key={t.id} style={styles.clickableRow} onClick={() => setOpenTournament(t.id)}>
                <td style={cellStyle.td}>{t.endedAt.slice(0, 10)}</td>
                <td style={cellStyle.td}>{t.name ?? '—'}</td>
                <td style={cellStyle.td}>{t.gameMode ?? '—'}</td>
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

      {openTournament != null && (
        <TournamentDetailModal tournamentId={openTournament} onClose={() => setOpenTournament(null)} />
      )}
    </AdminScreen>
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

function BucketBars({ data, fill }: { data: StatBucket[]; fill: string }) {
  if (data.length === 0) return <p style={cellStyle.muted}>No data yet.</p>
  return (
    <ResponsiveContainer width="100%" height={Math.max(120, data.length * 28)}>
      <BarChart data={data} layout="vertical" margin={{ top: 4, right: 16, bottom: 4, left: 8 }}>
        <XAxis type="number" stroke="#666" fontSize={11} allowDecimals={false} />
        <YAxis type="category" dataKey="label" stroke="#999" fontSize={11} width={90} />
        <Tooltip contentStyle={chartTooltipStyle} cursor={{ fill: '#ffffff10' }} />
        <Bar dataKey="count" fill={fill} radius={[0, 4, 4, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}

const styles: Record<string, React.CSSProperties> = {
  error: { color: adminTheme.bad, fontSize: 13, margin: 0 },
  cardRow: { display: 'flex', gap: 12, flexWrap: 'wrap' },
  twoCol: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 18 },
  toggle: { background: 'none', border: 'none', color: adminTheme.accent, cursor: 'pointer', fontSize: 12, padding: 0 },
  clickableRow: { cursor: 'pointer' },
}
