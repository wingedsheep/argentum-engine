/**
 * Admin Live Games: what the server is doing right now — every game session in memory (public or
 * private, human or AI) and every tournament lobby past its waiting room. Built to answer "is this a
 * good moment for maintenance?": the headline counts the games and lobbies a restart would interrupt,
 * and each game shows how long ago someone last acted in it. Any started game can be watched in a new
 * tab through the ordinary `/?spectate=` deep link (as an ephemeral spectator the seated players see in
 * their watcher list). Above the games, every online player with what they're doing — playing,
 * drafting, in a lobby, searching, in the deckbuilder — and a feed of recent moves
 * ({@link OnlinePlayersPanel}, {@link ActivityFeedPanel}).
 *
 * Polls every {@link REFRESH_MS}; read-only and gated behind the dashboard's shared {@link AdminAuth}.
 */
import { useCallback, useEffect, useState } from 'react'
import type React from 'react'
import { type LiveGame, type LiveLobby, type LiveOverview, fetchLiveOverview } from '@/api/adminLiveGames'
import type { AdminAuth } from '@/api/adminAuth'
import { formatAgo, formatClock, gameModeLabel } from './statFormat'
import { ActivityFeedPanel, OnlinePlayersPanel } from './AdminOnlinePlayers'
import { AdminScreen, Panel, StatCard, Table, adminTheme, cellStyle } from './adminUi'

const REFRESH_MS = 10_000
/** A game with no action for this long is shown as idle — likely abandoned rather than being played. */
const IDLE_AFTER_MS = 5 * 60_000

export function AdminLiveGames({ auth, onBack }: { auth: AdminAuth; onBack: () => void }) {
  const [overview, setOverview] = useState<LiveOverview | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [showAiOnly, setShowAiOnly] = useState(false)

  const load = useCallback(async () => {
    try {
      setOverview(await fetchLiveOverview(auth))
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load live games')
    }
  }, [auth])

  useEffect(() => {
    void load()
    const id = window.setInterval(() => void load(), REFRESH_MS)
    return () => window.clearInterval(id)
  }, [load])

  const now = overview ? Date.parse(overview.generatedAt) : Date.now()
  const running = overview?.games.filter((g) => !g.gameOver) ?? []
  const withHumans = running.filter((g) => g.hasConnectedHuman)
  const activeWithHumans = withHumans.filter((g) => !isIdle(g, now))
  const aiOnly = running.filter((g) => !g.hasConnectedHuman)
  const busyLobbies = overview?.lobbies.filter((l) => l.connectedHumans > 0) ?? []
  const visible = showAiOnly ? running : withHumans

  return (
    <AdminScreen
      title="Live games"
      subtitle="Everything running on the server right now"
      onBack={onBack}
      backLabel="← Dashboard"
      right={
        <button type="button" style={styles.refresh} onClick={() => void load()}>
          Refresh
        </button>
      }
    >
      {error && <p style={styles.error}>{error}</p>}
      {overview && (
        <>
          <MaintenanceVerdict activeGames={activeWithHumans.length} idleGames={withHumans.length - activeWithHumans.length} lobbies={busyLobbies.length} />

          <div style={styles.metrics}>
            <StatCard label="Online players" value={overview.onlinePlayers} />
            <StatCard label="Games with players" value={withHumans.length} accent={withHumans.length > 0} />
            <StatCard label="…active in last 5 min" value={activeWithHumans.length} />
            <StatCard label="No connected human" value={aiOnly.length} />
            <StatCard label="Running tournaments" value={busyLobbies.length} />
          </div>

          <div style={styles.activityRow}>
            <div style={styles.playersCol}>
              <OnlinePlayersPanel players={overview.players} now={now} />
            </div>
            <div style={styles.feedCol}>
              <ActivityFeedPanel feed={overview.feed} now={now} />
            </div>
          </div>

          <Panel
            title="Games"
            subtitle={`Updated ${formatClock(overview.generatedAt)} · refreshes every ${REFRESH_MS / 1000}s`}
            action={
              <label style={styles.toggle}>
                <input type="checkbox" checked={showAiOnly} onChange={(e) => setShowAiOnly(e.target.checked)} />
                Show games with no connected human ({aiOnly.length})
              </label>
            }
          >
            {visible.length === 0 ? (
              <p style={cellStyle.muted}>No games with connected players.</p>
            ) : (
              <Table head={['Players', 'Mode', 'Turn', 'Last action', 'Running for', 'Watching', '']} leftColumns={5}>
                {visible.map((g) => (
                  <GameRow key={g.gameSessionId} game={g} now={now} />
                ))}
              </Table>
            )}
          </Panel>

          <Panel title="Tournament lobbies" subtitle="Drafting, deckbuilding, or playing rounds">
            {overview.lobbies.length === 0 ? (
              <p style={cellStyle.muted}>No tournaments in progress.</p>
            ) : (
              <Table head={['Mode', 'Sets', 'Stage', 'Players online', 'Round']} leftColumns={3}>
                {overview.lobbies.map((l) => (
                  <LobbyRow key={l.lobbyId} lobby={l} />
                ))}
              </Table>
            )}
          </Panel>
        </>
      )}
    </AdminScreen>
  )
}

function MaintenanceVerdict({ activeGames, idleGames, lobbies }: { activeGames: number; idleGames: number; lobbies: number }) {
  const quiet = activeGames === 0 && lobbies === 0
  const parts: string[] = []
  if (activeGames > 0) parts.push(`${activeGames} game${activeGames === 1 ? '' : 's'} being played`)
  if (lobbies > 0) parts.push(`${lobbies} tournament${lobbies === 1 ? '' : 's'} running`)
  if (idleGames > 0) parts.push(`${idleGames} idle game${idleGames === 1 ? '' : 's'} with a player still connected`)
  return (
    <div style={{ ...styles.verdict, borderColor: quiet ? adminTheme.good : adminTheme.bad }}>
      <span style={{ ...styles.verdictDot, backgroundColor: quiet ? adminTheme.good : adminTheme.bad }} aria-hidden />
      <span>
        <strong style={{ color: adminTheme.text }}>{quiet ? 'Quiet — a restart would interrupt no active play.' : 'Busy — a restart would interrupt play.'}</strong>
        {parts.length > 0 && <span style={styles.verdictDetail}> {parts.join(' · ')}.</span>}
      </span>
    </div>
  )
}

function GameRow({ game, now }: { game: LiveGame; now: number }) {
  const mode = gameModeLabel(game.gameMode, game.format)
  const idle = isIdle(game, now)
  return (
    <tr>
      <td style={cellStyle.td}>
        {game.seats.map((s, i) => (
          <span key={`${s.name}-${i}`}>
            {i > 0 && <span style={styles.vs}> vs </span>}
            <span style={s.name === game.activePlayerName ? styles.active : undefined}>{s.name}</span>
            {s.isAi ? (
              <span style={styles.tag}> AI</span>
            ) : (
              !s.connected && <span style={styles.disconnected} title="Disconnected"> ⦸</span>
            )}
            {s.life != null && <span style={styles.life}> {s.life}</span>}
          </span>
        ))}
      </td>
      <td style={cellStyle.td}>
        {mode.primary}
        {mode.variant ? <span style={styles.tag}> › {mode.variant}</span> : null}
        {game.setCode ? <span style={styles.tag}> · {game.setCode}</span> : null}
        {game.ranked ? <span style={styles.ranked}> · Ranked</span> : null}
      </td>
      <td style={cellStyle.td}>
        {game.gameOver ? 'Over' : !game.started ? 'Pregame' : `T${game.turnNumber ?? '?'} · ${prettyStep(game.step)}`}
      </td>
      <td style={{ ...cellStyle.td, color: idle ? adminTheme.textMuted : adminTheme.good }}>
        {game.lastActionAt ? `${formatAgo(now - Date.parse(game.lastActionAt))} ago` : '—'}
      </td>
      <td style={cellStyle.td}>{game.startedAt ? formatAgo(now - Date.parse(game.startedAt)) : '—'}</td>
      <td style={cellStyle.tdNum}>{game.spectatorCount || '—'}</td>
      <td style={cellStyle.tdNum}>
        {game.started && !game.gameOver ? (
          <a style={styles.link} href={`/?spectate=${encodeURIComponent(game.gameSessionId)}`} target="_blank" rel="noreferrer">
            Watch ↗
          </a>
        ) : (
          <span style={styles.tag}>—</span>
        )}
      </td>
    </tr>
  )
}

function LobbyRow({ lobby }: { lobby: LiveLobby }) {
  const mode = gameModeLabel(lobby.gameMode, lobby.format)
  return (
    <tr>
      <td style={cellStyle.td}>
        {mode.primary}
        {mode.variant ? <span style={styles.tag}> › {mode.variant}</span> : null}
      </td>
      <td style={cellStyle.td}>{lobby.setNames.join(', ') || '—'}</td>
      <td style={cellStyle.td}>{prettyToken(lobby.state)}</td>
      <td style={cellStyle.tdNum}>
        {lobby.connectedHumans} / {lobby.humanPlayers}
      </td>
      <td style={cellStyle.tdNum}>{lobby.currentRound != null ? `${lobby.currentRound} / ${lobby.totalRounds ?? '?'}` : '—'}</td>
    </tr>
  )
}

function isIdle(game: LiveGame, now: number): boolean {
  const last = game.lastActionAt ?? game.startedAt
  return !last || now - Date.parse(last) > IDLE_AFTER_MS
}

function prettyToken(token: string): string {
  return token
    .toLowerCase()
    .split('_')
    .map((w) => (w ? w.charAt(0).toUpperCase() + w.slice(1) : w))
    .join(' ')
}

function prettyStep(step: string | null): string {
  return step ? prettyToken(step) : '—'
}

const styles: Record<string, React.CSSProperties> = {
  error: { color: adminTheme.bad, fontSize: 13, margin: '0 0 12px' },
  // Players take the width; the feed sits beside them on a wide screen and drops below on a narrow one.
  activityRow: { display: 'flex', flexWrap: 'wrap', gap: 18, marginBottom: 18, alignItems: 'flex-start' },
  playersCol: { flex: '2 1 480px', minWidth: 0 },
  feedCol: { flex: '1 1 280px', minWidth: 0 },
  metrics: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: 12, marginBottom: 18 },
  verdict: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    padding: '14px 16px',
    marginBottom: 18,
    borderRadius: 12,
    border: '1px solid',
    backgroundColor: adminTheme.panel,
    color: adminTheme.textSecondary,
    fontSize: 14,
  },
  verdictDot: { width: 10, height: 10, borderRadius: '50%', flexShrink: 0 },
  verdictDetail: { color: adminTheme.textSecondary },
  toggle: { display: 'inline-flex', alignItems: 'center', gap: 6, fontSize: 12, color: adminTheme.textMuted, cursor: 'pointer' },
  vs: { color: adminTheme.textMuted },
  active: { color: adminTheme.text, fontWeight: 600 },
  tag: { color: adminTheme.textMuted, fontSize: 12 },
  ranked: { color: adminTheme.accent, fontSize: 12 },
  life: { color: adminTheme.textMuted, fontSize: 12, fontVariantNumeric: 'tabular-nums' },
  disconnected: { color: adminTheme.bad, fontSize: 12 },
  link: { color: adminTheme.accent, fontSize: 13, textDecoration: 'none', whiteSpace: 'nowrap' },
  refresh: {
    background: 'none',
    border: `1px solid ${adminTheme.border}`,
    borderRadius: 8,
    color: adminTheme.text,
    padding: '6px 12px',
    font: 'inherit',
    fontSize: 13,
    cursor: 'pointer',
  },
}
