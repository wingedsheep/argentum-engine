/**
 * Admin Players: the roster of registered accounts and a per-account drill-down (lifetime record,
 * colors, modes, head-to-head, recent games, tournaments) — plus the control to grant/revoke admin
 * access. This is the page from which the first admin is bootstrapped: sign in once with the admin
 * password, open a player here, and promote their account; from then on they reach the dashboard with
 * their normal sign-in. Auth is the dashboard's shared {@link AdminAuth}.
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import {
  type AdminGamesPage,
  type AdminUserDetail,
  type AdminUserSummary,
  fetchUserDetail,
  fetchUserGames,
  fetchUsers,
  setUserAdmin,
} from '@/api/adminUsers'
import type { AdminAuth } from '@/api/adminAuth'
import { TournamentDetailModal } from '@/components/profile/TournamentDetailModal'
import { Avatar } from '@/components/profile/accountUi'
import { colorForIdentity, colorLabel, mergeModeBuckets } from './statFormat'
import { AdminScreen, Panel, StatCard, Table, adminTheme, cellStyle } from './adminUi'
import { AdminDeckModal, AdminSavedDecks } from './AdminPlayerDecks'

const GAMES_PAGE_SIZE = 10

export function AdminPlayers({ auth, onBack }: { auth: AdminAuth; onBack: () => void }) {
  const [users, setUsers] = useState<AdminUserSummary[]>([])
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    fetchUsers(auth)
      .then((u) => !cancelled && setUsers(u))
      .catch((e) => !cancelled && setError(e instanceof Error ? e.message : 'Failed to load players'))
      .finally(() => !cancelled && setLoading(false))
    return () => {
      cancelled = true
    }
  }, [auth])

  /** Reflect an admin-flag change from the detail view back into the roster row. */
  const applyAdminChange = (id: string, isAdmin: boolean) =>
    setUsers((prev) => prev.map((u) => (u.id === id ? { ...u, isAdmin } : u)))

  if (selectedId != null) {
    return (
      <PlayerDetail
        auth={auth}
        id={selectedId}
        onBack={() => setSelectedId(null)}
        onAdminChange={applyAdminChange}
      />
    )
  }

  return (
    <AdminScreen
      title="Players"
      subtitle={`${users.length} registered account${users.length === 1 ? '' : 's'}`}
      onBack={onBack}
      backLabel="← Dashboard"
    >
      {error && <p style={styles.error}>{error}</p>}
      <Panel>
        {loading ? (
          <p style={cellStyle.muted}>Loading players…</p>
        ) : users.length === 0 ? (
          <p style={cellStyle.muted}>No registered accounts yet.</p>
        ) : (
          <Table head={['Player', 'Games', 'Wins', 'Win %', 'Last played', '']}>
            {users.map((u) => {
              const winRate = u.games > 0 ? Math.round((u.wins / u.games) * 100) : 0
              return (
                <tr key={u.id} style={styles.row} onClick={() => setSelectedId(u.id)}>
                  <td style={cellStyle.td}>
                    <div style={styles.playerCell}>
                      <Avatar name={u.displayName} avatar={u.avatar} small />
                      <div>
                        <div style={styles.playerNameRow}>
                          <span style={styles.playerName}>{u.displayName}</span>
                          {u.isAdmin && <span style={styles.adminBadge}>ADMIN</span>}
                        </div>
                        <div style={styles.playerEmail}>{u.email}</div>
                      </div>
                    </div>
                  </td>
                  <td style={cellStyle.tdNum}>{u.games}</td>
                  <td style={cellStyle.tdNum}>{u.wins}</td>
                  <td style={cellStyle.tdNum}>{u.games > 0 ? `${winRate}%` : '—'}</td>
                  <td style={cellStyle.tdNum}>{u.lastPlayed ? u.lastPlayed.slice(0, 10) : '—'}</td>
                  <td style={cellStyle.tdNum}>
                    <span style={styles.viewLink}>View →</span>
                  </td>
                </tr>
              )
            })}
          </Table>
        )}
      </Panel>
    </AdminScreen>
  )
}

function PlayerDetail({
  auth,
  id,
  onBack,
  onAdminChange,
}: {
  auth: AdminAuth
  id: string
  onBack: () => void
  onAdminChange: (id: string, isAdmin: boolean) => void
}) {
  const [detail, setDetail] = useState<AdminUserDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [updating, setUpdating] = useState(false)
  const [games, setGames] = useState<AdminGamesPage>({ entries: [], total: 0 })
  const [gamesPage, setGamesPage] = useState(0)
  const [deckModal, setDeckModal] = useState<{ gameId: string; opponent: string } | null>(null)
  const [openTournament, setOpenTournament] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchUserDetail(auth, id)
      .then((d) => !cancelled && setDetail(d))
      .catch((e) => !cancelled && setError(e instanceof Error ? e.message : 'Failed to load player'))
    return () => {
      cancelled = true
    }
  }, [auth, id])

  useEffect(() => {
    let cancelled = false
    fetchUserGames(auth, id, GAMES_PAGE_SIZE, gamesPage * GAMES_PAGE_SIZE)
      .then((p) => !cancelled && setGames(p))
      .catch(() => !cancelled && setGames({ entries: [], total: 0 }))
    return () => {
      cancelled = true
    }
  }, [auth, id, gamesPage])

  /** Open a finished game's replay in a new tab so the admin keeps their place on this screen. */
  const watchReplay = (gameId: string) => window.open(`/replay/${gameId}`, '_blank', 'noopener')

  const toggleAdmin = async () => {
    if (!detail) return
    const next = !detail.isAdmin
    setUpdating(true)
    setError(null)
    try {
      const isAdmin = await setUserAdmin(auth, id, next)
      setDetail({ ...detail, isAdmin })
      onAdminChange(id, isAdmin)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to update admin status')
    } finally {
      setUpdating(false)
    }
  }

  const adminButton = detail && (
    <button
      type="button"
      style={detail.isAdmin ? styles.demoteBtn : styles.promoteBtn}
      disabled={updating}
      onClick={() => void toggleAdmin()}
    >
      {updating ? 'Saving…' : detail.isAdmin ? 'Revoke admin' : 'Make admin'}
    </button>
  )

  return (
    <AdminScreen
      title={detail?.displayName ?? 'Player'}
      subtitle={detail?.email}
      onBack={onBack}
      backLabel="← Players"
      right={adminButton}
    >
      {error && <p style={styles.error}>{error}</p>}
      {!detail ? (
        <p style={cellStyle.muted}>Loading…</p>
      ) : (
        <>
          <div style={styles.identity}>
            <Avatar name={detail.displayName} avatar={detail.avatar} />
            <div style={styles.meta}>Joined {detail.createdAt.slice(0, 10)}</div>
          </div>

          {detail.isAdmin && (
            <div style={styles.adminNotice}>
              <span style={styles.adminBadge}>ADMIN</span>
              <span style={cellStyle.muted}>This account can reach the admin dashboard with its own sign-in.</span>
            </div>
          )}

          <div style={styles.cardRow}>
            <StatCard label="Games" value={detail.stats.games} />
            <StatCard label="Wins" value={detail.stats.wins} />
            <StatCard label="Losses" value={detail.stats.losses} />
            <StatCard
              label="Win rate"
              value={detail.stats.games > 0 ? `${Math.round(detail.stats.winRate * 100)}%` : '—'}
              accent
            />
          </div>

          {detail.colors.length > 0 && (
            <Panel title="Colors played">
              <ChipList items={detail.colors.map((c) => `${colorLabel(c.label)} · ${c.count}`)} />
            </Panel>
          )}

          {detail.modes.length > 0 && (
            <Panel title="Game modes">
              <ChipList items={mergeModeBuckets(detail.modes).map((m) => `${m.label} · ${m.count}`)} />
            </Panel>
          )}

          {detail.opponents.length > 0 && (
            <Panel title="Head to head">
              <Table head={['Opponent', 'W', 'L']}>
                {detail.opponents.map((o, i) => (
                  <tr key={`${o.opponent}-${i}`}>
                    <td style={cellStyle.td}>
                      {o.opponent}
                      {o.isAi && <span style={styles.aiTag}> AI</span>}
                    </td>
                    <td style={cellStyle.tdNum}>{o.wins}</td>
                    <td style={cellStyle.tdNum}>{o.losses}</td>
                  </tr>
                ))}
              </Table>
            </Panel>
          )}

          {detail.topCards.length > 0 && (
            <Panel title="Most-played cards">
              <ChipList items={detail.topCards.map((c) => `${c.cardName} · ${c.copies}`)} />
            </Panel>
          )}

          {detail.tournaments.length > 0 && (
            <Panel title="Tournaments">
              <Table head={['Date', 'Tournament', 'Place']}>
                {detail.tournaments.map((t) => (
                  <tr key={t.id} style={styles.row} onClick={() => setOpenTournament(t.id)}>
                    <td style={cellStyle.td}>{t.endedAt.slice(0, 10)}</td>
                    <td style={cellStyle.td}>{t.name ?? '—'}</td>
                    <td style={cellStyle.tdNum}>
                      {t.placement}/{t.playerCount}
                    </td>
                  </tr>
                ))}
              </Table>
            </Panel>
          )}

          {games.total > 0 && (
            <Panel title={`Games · ${games.total}`}>
              <Table head={['Date', 'Mode', 'Colors', 'Opponent', 'Result', 'Deck', 'Replay']}>
                {games.entries.map((g, i) => (
                  <tr key={`${g.gameId}-${i}`}>
                    <td style={cellStyle.td}>{g.endedAt.slice(0, 10)}</td>
                    <td style={cellStyle.td}>{prettyMode(g.gameMode)}</td>
                    <td style={cellStyle.td}>
                      {g.colors ? (
                        <span style={styles.colorsCell}>
                          <span style={{ ...styles.colorDot, backgroundColor: colorForIdentity(g.colors) }} />
                          {colorLabel(g.colors)}
                        </span>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td style={cellStyle.td}>{g.opponents ?? '—'}</td>
                    <td style={{ ...cellStyle.tdNum, color: g.won ? adminTheme.good : adminTheme.bad }}>
                      {g.won ? 'Win' : 'Loss'}
                    </td>
                    <td style={cellStyle.tdNum}>
                      <button
                        type="button"
                        style={styles.rowLink}
                        onClick={() => setDeckModal({ gameId: g.gameId, opponent: g.opponents ?? 'opponent' })}
                      >
                        View
                      </button>
                    </td>
                    <td style={cellStyle.tdNum}>
                      {g.hasReplay ? (
                        <button type="button" style={styles.rowLink} onClick={() => watchReplay(g.gameId)}>
                          Watch
                        </button>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                ))}
              </Table>
              {games.total > GAMES_PAGE_SIZE && (
                <div style={styles.pager}>
                  <button
                    type="button"
                    style={{ ...styles.pagerBtn, ...(gamesPage === 0 ? styles.pagerBtnDisabled : {}) }}
                    disabled={gamesPage === 0}
                    onClick={() => setGamesPage((p) => Math.max(0, p - 1))}
                  >
                    ← Newer
                  </button>
                  <span style={cellStyle.muted}>
                    Page {gamesPage + 1} of {Math.max(1, Math.ceil(games.total / GAMES_PAGE_SIZE))}
                  </span>
                  <button
                    type="button"
                    style={{
                      ...styles.pagerBtn,
                      ...((gamesPage + 1) * GAMES_PAGE_SIZE >= games.total ? styles.pagerBtnDisabled : {}),
                    }}
                    disabled={(gamesPage + 1) * GAMES_PAGE_SIZE >= games.total}
                    onClick={() => setGamesPage((p) => p + 1)}
                  >
                    Older →
                  </button>
                </div>
              )}
            </Panel>
          )}

          <AdminSavedDecks auth={auth} userId={id} />
        </>
      )}

      {deckModal && (
        <AdminDeckModal
          auth={auth}
          userId={id}
          gameId={deckModal.gameId}
          opponentLabel={deckModal.opponent}
          onClose={() => setDeckModal(null)}
        />
      )}

      {openTournament != null && (
        <TournamentDetailModal tournamentId={openTournament} onClose={() => setOpenTournament(null)} />
      )}
    </AdminScreen>
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

/** Turn a raw game-mode token (TOURNAMENT / QUICK_GAME / ...) into a readable label. */
function prettyMode(mode: string | null): string {
  if (!mode) return '—'
  return mode
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

const styles: Record<string, React.CSSProperties> = {
  error: { color: adminTheme.bad, fontSize: 13, margin: 0 },
  row: { cursor: 'pointer' },
  playerCell: { display: 'flex', alignItems: 'center', gap: 12 },
  playerNameRow: { display: 'flex', alignItems: 'center', gap: 8 },
  playerName: { color: adminTheme.text, fontWeight: 600 },
  playerEmail: { color: adminTheme.textMuted, fontSize: 12, marginTop: 2 },
  adminBadge: {
    fontSize: 10,
    fontWeight: 700,
    letterSpacing: 0.6,
    color: adminTheme.accent,
    backgroundColor: 'rgba(139,155,255,0.12)',
    border: `1px solid ${adminTheme.accent}55`,
    borderRadius: 999,
    padding: '2px 7px',
  },
  viewLink: { color: adminTheme.accent, fontSize: 13, whiteSpace: 'nowrap' },
  cardRow: { display: 'flex', gap: 12, flexWrap: 'wrap' },
  meta: { color: adminTheme.textMuted, fontSize: 13 },
  identity: { display: 'flex', alignItems: 'center', gap: 16 },
  adminNotice: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    backgroundColor: 'rgba(139,155,255,0.06)',
    border: `1px solid ${adminTheme.border}`,
    borderRadius: 12,
    padding: '10px 14px',
  },
  aiTag: { color: adminTheme.textMuted, fontSize: 11 },
  chipRow: { display: 'flex', flexWrap: 'wrap', gap: 8 },
  chip: {
    backgroundColor: adminTheme.panelAlt,
    border: `1px solid ${adminTheme.border}`,
    borderRadius: 999,
    padding: '4px 12px',
    color: adminTheme.textSecondary,
    fontSize: 13,
  },
  promoteBtn: {
    padding: '8px 14px',
    borderRadius: 9,
    border: 'none',
    backgroundColor: adminTheme.accentSolid,
    color: adminTheme.onAccent,
    fontWeight: 600,
    fontSize: 13,
    cursor: 'pointer',
  },
  demoteBtn: {
    padding: '8px 14px',
    borderRadius: 9,
    border: `1px solid ${adminTheme.bad}66`,
    backgroundColor: 'transparent',
    color: adminTheme.bad,
    fontWeight: 600,
    fontSize: 13,
    cursor: 'pointer',
  },
  colorsCell: { display: 'inline-flex', alignItems: 'center', gap: 6 },
  colorDot: { width: 9, height: 9, borderRadius: 999, display: 'inline-block' },
  rowLink: { background: 'none', border: 'none', color: adminTheme.accent, cursor: 'pointer', fontSize: 13, padding: 0 },
  pager: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 14, marginTop: 12 },
  pagerBtn: {
    background: 'none',
    border: `1px solid ${adminTheme.border}`,
    borderRadius: 8,
    color: adminTheme.textSecondary,
    cursor: 'pointer',
    fontSize: 13,
    padding: '6px 12px',
  },
  pagerBtnDisabled: { color: adminTheme.textMuted, cursor: 'default', borderColor: adminTheme.borderSoft },
}
