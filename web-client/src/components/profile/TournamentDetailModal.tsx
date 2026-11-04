/**
 * Modal showing one tournament's full public detail: the final standings for every participant and
 * every game that was played in it (all players', not just the viewer's), each linkable to its
 * replay. Opened from the Tournaments table on a profile. Player names link to their public profile.
 */
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { type TournamentDetail, fetchTournamentDetail } from '@/api/account'
import { gameModeLabel } from '@/components/admin/statFormat'
import { TournamentStatusBadge } from '@/components/tournament/TournamentStatusBadge'
import { AccountModal, accountStyles as a } from '@/components/profile/accountUi'

export function TournamentDetailModal({
  tournamentId,
  onClose,
}: {
  tournamentId: number
  onClose: () => void
}) {
  const navigate = useNavigate()
  const [detail, setDetail] = useState<TournamentDetail | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let live = true
    setDetail(null)
    setError(null)
    fetchTournamentDetail(tournamentId)
      .then((d) => live && setDetail(d))
      .catch(() => live && setError('Could not load this tournament.'))
    return () => {
      live = false
    }
  }, [tournamentId])

  // Leaving for a profile or replay doesn't close the modal first: the page unmounts anyway, and a
  // host that keeps the modal in the URL (the admin screens) reopens it when the player comes Back.
  // Closing first would race that host's history pop against this push.
  const goProfile = (userId: string | null) => {
    if (userId) navigate(`/u/${userId}`)
  }

  const mode = detail ? gameModeLabel(detail.gameMode, detail.format) : null
  const hasForfeits = !!detail?.standings.some((s) => (s.forfeitWins ?? 0) + (s.forfeitLosses ?? 0) > 0)
  const title = detail?.name?.trim() || [detail?.setCodes, mode?.variant ?? mode?.primary].filter(Boolean).join(' ') || 'Tournament'

  return (
    <AccountModal
      title={title}
      titleExtra={detail && detail.status !== 'COMPLETED' ? <TournamentStatusBadge status={detail.status} /> : null}
      onClose={onClose}
    >
      {detail && mode && (
        <p className={a.muted} style={{ marginTop: 4 }}>
          {mode.primary}
          {mode.variant ? <span className={a.dim}> › {mode.variant}</span> : null}
          {' · '}
          {detail.playerCount} players · {detail.endedAt.slice(0, 10)}
        </p>
      )}

      {error ? (
        <p className={a.error} style={{ marginTop: 12 }}>{error}</p>
      ) : !detail ? (
        <p className={a.muted} style={{ marginTop: 12 }}>Loading…</p>
      ) : (
        <>
          <h3 className={a.dialogSection}>{detail.status === 'COMPLETED' ? 'Final standings' : 'Standings so far'}</h3>
          {/* W/L are the matches played; a leaver's forfeits sit in their own column (placement counts them). */}
          <div className={a.tableWrap}>
            <table className={a.table}>
              <thead>
                <tr>
                  <th style={{ width: 48 }}>#</th>
                  <th>Player</th>
                  <th className={a.num}>W</th>
                  <th className={a.num}>L</th>
                  <th className={a.num}>D</th>
                  {hasForfeits && (
                    <th className={a.num} title="Matches decided because a player left the tournament — not played">
                      Forfeits
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {detail.standings.map((s, i) => (
                  <tr key={`${s.playerName}-${i}`}>
                    <td className={a.num} style={{ textAlign: 'left', fontWeight: 600, color: s.placement === 1 ? '#f2b45c' : undefined }}>
                      {s.placement}
                      {s.placement === 1 ? ' 🏆' : ''}
                    </td>
                    <td>
                      <PlayerName name={s.playerName} userId={s.userId} isAi={s.isAi} onClick={goProfile} />
                    </td>
                    <td className={a.num}>{s.wins - (s.forfeitWins ?? 0)}</td>
                    <td className={a.num}>{s.losses - (s.forfeitLosses ?? 0)}</td>
                    <td className={a.num}>{s.draws}</td>
                    {hasForfeits && <td className={`${a.num} ${a.forfeits}`}>{forfeitLabel(s.forfeitWins, s.forfeitLosses)}</td>}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {hasForfeits && (
            <p className={a.subtle} style={{ marginTop: 8 }}>
              Wins and losses are matches played. Forfeits — matches a player left before playing — still count toward placement.
            </p>
          )}

          <h3 className={a.dialogSection}>Games</h3>
          {detail.games.length === 0 ? (
            <p className={a.muted}>No game records were stored for this tournament.</p>
          ) : (
            <ul className={a.tGames}>
              {detail.games.map((g, i) => (
                <li key={`${g.gameId}-${i}`} className={a.tGame}>
                  <span>
                    {g.players.map((p, j) => (
                      <span key={`${p.name}-${j}`}>
                        {j > 0 && <span className={a.dim}> vs </span>}
                        <span className={p.won ? a.tWinner : undefined}>
                          <PlayerName name={p.name} userId={p.userId} isAi={p.isAi} onClick={goProfile} />
                          {p.won ? ' ✓' : ''}
                        </span>
                      </span>
                    ))}
                  </span>
                  <span className={a.tMeta}>
                    {g.endedAt.slice(0, 10)}
                    {g.hasReplay ? (
                      <button
                        type="button"
                        className={a.miniButton}
                        onClick={() => navigate(`/replay/${g.gameId}`)}
                      >
                        Watch
                      </button>
                    ) : (
                      <span className={a.dim}>—</span>
                    )}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </AccountModal>
  )
}

/** A player name that links to their public profile when signed in, with an AI tag otherwise. */
/** A standing's forfeits, e.g. "+1 W" or "+3 L"; a dash when it has none. */
function forfeitLabel(wins = 0, losses = 0): string {
  const parts = [wins > 0 ? `+${wins} W` : null, losses > 0 ? `+${losses} L` : null].filter(Boolean)
  return parts.length > 0 ? parts.join(' ') : '—'
}

function PlayerName({
  name,
  userId,
  isAi,
  onClick,
}: {
  name: string
  userId: string | null
  isAi: boolean
  onClick: (userId: string | null) => void
}) {
  if (userId) {
    return (
      <button type="button" className={a.link} onClick={() => onClick(userId)}>
        {name}
      </button>
    )
  }
  return (
    <span>
      {name}
      {isAi ? <span className={a.aiTag}>AI</span> : null}
    </span>
  )
}
