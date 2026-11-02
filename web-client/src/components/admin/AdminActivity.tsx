/**
 * Admin Activity: global, cross-player lists of recent games and recent tournaments — the same
 * "recent games + tournaments you can click through" experience a player gets on their own profile,
 * but spanning every player. Games page through every recorded game (newest first), show each seat's
 * deck colours, and open every seat's decklist (the shared {@link MatchDecksModal}, kept in `?game=`)
 * or the replay; tournaments open the shared {@link TournamentDetailModal} (standings + every game played).
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
import { useAdminPageParam, useAdminParam, useAdminTournamentParam } from './adminNav'
import { ColorPips, MatchDecksModal } from './AiVsHumanGames'

/** Games per page in the recent-games pager. */
const GAMES_PAGE_SIZE = 20
/** How many tournaments to list. */
const TOURNAMENTS_LIMIT = 50

export function AdminActivity({ auth, onBack }: { auth: AdminAuth; onBack: () => void }) {
  const navigate = useNavigate()

  const [games, setGames] = useState<AdminRecentGame[]>([])
  const [gamesTotal, setGamesTotal] = useState(0)
  const [page, setPage] = useAdminPageParam()
  const [gamesError, setGamesError] = useState<string | null>(null)

  const [tournaments, setTournaments] = useState<TournamentSummary[]>([])
  const [tournamentsError, setTournamentsError] = useState<string | null>(null)
  const [openTournament, setOpenTournament, closeTournament] = useAdminTournamentParam()
  const [gameParam, openGame, closeGame] = useAdminParam('game')
  const openMatchId = gameParam != null && /^\d+$/.test(gameParam) ? Number(gameParam) : null

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
          <Table head={['Date', 'Mode', 'Players & decks', 'Winner', '']} leftColumns={4}>
            {games.map((g) => {
              const mode = gameModeLabel(g.gameMode, g.format)
              return (
                <tr
                  key={g.gameId}
                  style={g.hasDecks ? styles.clickableRow : undefined}
                  title={g.hasDecks ? 'Open both decks' : undefined}
                  onClick={g.hasDecks ? () => openGame(String(g.matchId)) : undefined}
                >
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
                  <td style={{ ...cellStyle.tdNum, whiteSpace: 'nowrap' }}>
                    <span style={styles.actions} onClick={(e) => e.stopPropagation()}>
                      {g.hasDecks ? (
                        <button type="button" style={styles.actionPrimary} onClick={() => openGame(String(g.matchId))}>
                          Decks
                        </button>
                      ) : (
                        <span style={styles.actionNone} title="No decklist was recorded for this game">
                          No decks
                        </span>
                      )}
                      {g.hasReplay ? (
                        <button type="button" style={styles.actionSecondary} onClick={() => navigate(`/replay/${g.gameId}`)}>
                          Replay
                        </button>
                      ) : (
                        <span style={styles.actionNone} title="No replay was stored for this game">
                          No replay
                        </span>
                      )}
                    </span>
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

      {openMatchId != null && <MatchDecksModal auth={auth} matchId={openMatchId} onClose={closeGame} />}

      {openTournament != null && (
        <TournamentDetailModal tournamentId={openTournament} onClose={closeTournament} />
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
          {/* Name and deck colours wrap as one unit, so the pips never land on a line of their own. */}
          <span style={styles.seat}>
            {p.userId ? (
              <button
                type="button"
                style={p.won ? styles.playerLinkWon : styles.playerLink}
                onClick={(e) => {
                  e.stopPropagation()
                  onProfile(p.userId as string)
                }}
              >
                {p.name}
              </button>
            ) : (
              <span style={p.won ? styles.winner : undefined}>
                {p.name}
                {p.isAi ? <span style={styles.aiTag}> AI</span> : null}
              </span>
            )}
            {p.colors != null && (
              <span style={styles.seatPips}>
                <ColorPips colors={p.colors} />
              </span>
            )}
          </span>
          <PlayerOrigin location={p.location} />
        </span>
      ))}
    </span>
  )
}

/** A 2-letter ISO country code → flag emoji (regional-indicator letters); '' when not a real code. */
function flagEmoji(countryCode: string | null): string {
  if (!countryCode || countryCode.length !== 2) return ''
  const base = 0x1f1e6 // 🇦
  return String.fromCodePoint(...[...countryCode.toUpperCase()].map((c) => base + c.charCodeAt(0) - 65))
}

/** Where a seat connected from, as a compact muted tag with the full detail on hover. AI seats and
 *  unresolved IPs (every field null) render nothing. */
function PlayerOrigin({ location }: { location: AdminGamePlayer['location'] }) {
  if (!location) return null
  const { city, region, country, countryCode } = location
  const detail = [city, region, country].filter(Boolean).join(', ')
  if (!detail) return null
  const flag = flagEmoji(countryCode)
  const short = city ?? region ?? country ?? ''
  return (
    <span style={styles.origin} title={detail}>
      {' '}
      {flag ? `${flag} ` : ''}
      {short}
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
  actions: { display: 'inline-flex', alignItems: 'center', gap: 6, justifyContent: 'flex-end' },
  actionPrimary: {
    padding: '3px 10px',
    borderRadius: 6,
    border: '1px solid rgba(242, 201, 122, 0.4)',
    background: 'rgba(242, 201, 122, 0.1)',
    color: '#f2c97a',
    fontSize: 12,
    fontWeight: 600,
    cursor: 'pointer',
  },
  actionSecondary: {
    padding: '3px 10px',
    borderRadius: 6,
    border: `1px solid ${adminTheme.border}`,
    background: 'none',
    color: adminTheme.accent,
    fontSize: 12,
    cursor: 'pointer',
  },
  actionNone: { color: adminTheme.textMuted, fontSize: 11, padding: '0 6px' },
  seat: { whiteSpace: 'nowrap' },
  seatPips: { marginLeft: 4, verticalAlign: 'middle' },
  vs: { color: adminTheme.textMuted },
  winner: { color: adminTheme.text, fontWeight: 600 },
  playerLink: { background: 'none', border: 'none', color: adminTheme.accent, cursor: 'pointer', fontSize: 'inherit', padding: 0 },
  playerLinkWon: { background: 'none', border: 'none', color: adminTheme.accent, cursor: 'pointer', fontSize: 'inherit', padding: 0, fontWeight: 600 },
  aiTag: { color: adminTheme.textMuted, fontSize: 11 },
  origin: { color: adminTheme.textMuted, fontSize: 11, whiteSpace: 'nowrap' },
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
