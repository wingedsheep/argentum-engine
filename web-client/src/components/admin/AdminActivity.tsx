/**
 * Admin Activity: global, cross-player lists of recent games and recent tournaments — the same
 * "recent games + tournaments you can click through" experience a player gets on their own profile,
 * but spanning every player. Games page through every recorded game (newest first) and link to their
 * replay; tournaments open the shared {@link TournamentDetailModal} (standings + every game played).
 *
 * Read-only and gated behind the dashboard's shared {@link AdminAuth}. The screen scrolls itself
 * (see AdminScreen).
 */
import { useCallback, useEffect, useState } from 'react'
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import {
  type AdminGamePlayer,
  type AdminRecentGame,
  type TournamentSummary,
  fetchRecentGames,
  fetchTournaments,
} from '@/api/adminStats'
import type { AdminAuth } from '@/api/adminAuth'
import { TournamentDetailModal } from '@/components/profile/TournamentDetailModal'
import { formatDateTime } from '@/utils/datetime'
import { gameModeLabel } from './statFormat'
import { AdminScreen, Panel, Table, adminTheme, cellStyle } from './adminUi'

/** Games per page in the recent-games pager. */
const GAMES_PAGE_SIZE = 20
/** How many tournaments to list. */
const TOURNAMENTS_LIMIT = 50

export function AdminActivity({ auth, onBack }: { auth: AdminAuth; onBack: () => void }) {
  const navigate = useNavigate()

  const [games, setGames] = useState<AdminRecentGame[]>([])
  const [gamesTotal, setGamesTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [gamesError, setGamesError] = useState<string | null>(null)

  const [tournaments, setTournaments] = useState<TournamentSummary[]>([])
  const [tournamentsError, setTournamentsError] = useState<string | null>(null)
  const [openTournament, setOpenTournament] = useState<number | null>(null)

  const loadGames = useCallback(
    async (p: number) => {
      try {
        const { entries, total } = await fetchRecentGames(auth, GAMES_PAGE_SIZE, p * GAMES_PAGE_SIZE)
        setGames(entries)
        setGamesTotal(total)
        setGamesError(null)
      } catch (e) {
        setGamesError(e instanceof Error ? e.message : 'Failed to load games')
      }
    },
    [auth],
  )

  useEffect(() => {
    void loadGames(page)
  }, [loadGames, page])

  useEffect(() => {
    let cancelled = false
    fetchTournaments(auth, TOURNAMENTS_LIMIT)
      .then((t) => !cancelled && setTournaments(t))
      .catch((e) => !cancelled && setTournamentsError(e instanceof Error ? e.message : 'Failed to load tournaments'))
    return () => {
      cancelled = true
    }
  }, [auth])

  const lastPage = Math.max(0, Math.ceil(gamesTotal / GAMES_PAGE_SIZE) - 1)
  const rangeStart = gamesTotal === 0 ? 0 : page * GAMES_PAGE_SIZE + 1
  const rangeEnd = Math.min(gamesTotal, (page + 1) * GAMES_PAGE_SIZE)

  return (
    <AdminScreen
      title="Activity"
      subtitle="Recent games and tournaments across every player"
      onBack={onBack}
      backLabel="← Dashboard"
    >
      <Panel
        title="Recent games"
        action={
          gamesTotal > 0 ? (
            <span style={styles.pager}>
              <button type="button" style={pageBtn(page <= 0)} disabled={page <= 0} onClick={() => setPage((p) => Math.max(0, p - 1))}>
                ← Newer
              </button>
              <span style={styles.pageInfo}>
                {rangeStart}–{rangeEnd} of {gamesTotal}
              </span>
              <button type="button" style={pageBtn(page >= lastPage)} disabled={page >= lastPage} onClick={() => setPage((p) => Math.min(lastPage, p + 1))}>
                Older →
              </button>
            </span>
          ) : null
        }
      >
        {gamesError ? (
          <p style={styles.error}>{gamesError}</p>
        ) : games.length === 0 ? (
          <p style={cellStyle.muted}>No games recorded yet.</p>
        ) : (
          <Table head={['Date', 'Mode', 'Players', 'Winner', 'Replay']}>
            {games.map((g) => {
              const mode = gameModeLabel(g.gameMode, g.format)
              return (
                <tr key={g.gameId}>
                  <td style={cellStyle.td}>{formatDateTime(g.endedAt)}</td>
                  <td style={cellStyle.td}>
                    {mode.primary}
                    {mode.variant ? <span style={styles.variant}> › {mode.variant}</span> : null}
                    {g.tournamentName ? <span style={styles.tournamentTag}> · {g.tournamentName}</span> : null}
                  </td>
                  <td style={cellStyle.td}>
                    <Players players={g.players} onProfile={(uid) => navigate(`/u/${uid}`)} />
                  </td>
                  <td style={cellStyle.td}>{g.winnerName ?? '—'}</td>
                  <td style={cellStyle.tdNum}>
                    {g.hasReplay ? (
                      <button type="button" style={styles.link} onClick={() => navigate(`/replay/${g.gameId}`)}>
                        Watch
                      </button>
                    ) : (
                      <span style={styles.noReplay}>—</span>
                    )}
                  </td>
                </tr>
              )
            })}
          </Table>
        )}
      </Panel>

      <Panel title="Recent tournaments">
        {tournamentsError ? (
          <p style={styles.error}>{tournamentsError}</p>
        ) : tournaments.length === 0 ? (
          <p style={cellStyle.muted}>No tournaments recorded yet.</p>
        ) : (
          <Table head={['Date', 'Name', 'Mode', 'Players', 'Winner']}>
            {tournaments.map((t) => {
              const mode = gameModeLabel(t.gameMode, t.format)
              return (
                <tr key={t.id} style={styles.clickableRow} onClick={() => setOpenTournament(t.id)}>
                  <td style={cellStyle.td}>{t.endedAt.slice(0, 10)}</td>
                  <td style={cellStyle.td}>{t.name ?? '—'}</td>
                  <td style={cellStyle.td}>
                    {mode.primary}
                    {mode.variant ? <span style={styles.variant}> › {mode.variant}</span> : null}
                  </td>
                  <td style={cellStyle.tdNum}>{t.playerCount}</td>
                  <td style={cellStyle.td}>{t.winnerName ?? '—'}</td>
                </tr>
              )
            })}
          </Table>
        )}
      </Panel>

      {openTournament != null && (
        <TournamentDetailModal tournamentId={openTournament} onClose={() => setOpenTournament(null)} />
      )}
    </AdminScreen>
  )
}

/** A game's seats, joined by "vs", with the winner bolded; named accounts link to their profile. */
function Players({ players, onProfile }: { players: AdminGamePlayer[]; onProfile: (userId: string) => void }) {
  if (players.length === 0) return <span style={cellStyle.muted}>—</span>
  return (
    <span>
      {players.map((p, i) => (
        <span key={`${p.name}-${i}`}>
          {i > 0 && <span style={styles.vs}> vs </span>}
          {p.userId ? (
            <button type="button" style={p.won ? styles.playerLinkWon : styles.playerLink} onClick={() => onProfile(p.userId as string)}>
              {p.name}
            </button>
          ) : (
            <span style={p.won ? styles.winner : undefined}>
              {p.name}
              {p.isAi ? <span style={styles.aiTag}> AI</span> : null}
            </span>
          )}
        </span>
      ))}
    </span>
  )
}

function pageBtn(disabled: boolean): React.CSSProperties {
  return { ...styles.pageBtn, opacity: disabled ? 0.4 : 1, cursor: disabled ? 'default' : 'pointer' }
}

const styles: Record<string, React.CSSProperties> = {
  error: { color: adminTheme.bad, fontSize: 13, margin: 0 },
  variant: { color: adminTheme.textMuted },
  tournamentTag: { color: adminTheme.accent },
  link: { background: 'none', border: 'none', color: adminTheme.accent, cursor: 'pointer', fontSize: 13, padding: 0 },
  noReplay: { color: adminTheme.textMuted, fontSize: 13 },
  vs: { color: adminTheme.textMuted },
  winner: { color: adminTheme.text, fontWeight: 600 },
  playerLink: { background: 'none', border: 'none', color: adminTheme.accent, cursor: 'pointer', fontSize: 'inherit', padding: 0 },
  playerLinkWon: { background: 'none', border: 'none', color: adminTheme.accent, cursor: 'pointer', fontSize: 'inherit', padding: 0, fontWeight: 600 },
  aiTag: { color: adminTheme.textMuted, fontSize: 11 },
  clickableRow: { cursor: 'pointer' },
  pager: { display: 'inline-flex', alignItems: 'center', gap: 10 },
  pageInfo: { color: adminTheme.textMuted, fontSize: 12, fontVariantNumeric: 'tabular-nums' },
  pageBtn: {
    background: 'none',
    border: `1px solid ${adminTheme.border}`,
    color: adminTheme.accent,
    borderRadius: 8,
    padding: '5px 10px',
    fontSize: 12,
  },
}
