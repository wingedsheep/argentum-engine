/**
 * Modal showing one tournament's full public detail: the final standings for every participant and
 * every game that was played in it (all players', not just the viewer's), each linkable to its
 * replay. Opened from the Tournaments table on a profile. Player names link to their public profile.
 */
import { useEffect, useState } from 'react'
import type React from 'react'
import { useNavigate } from 'react-router-dom'
import { type TournamentDetail, fetchTournamentDetail } from '@/api/account'
import { gameModeLabel } from '@/components/admin/statFormat'
import { TournamentStatusBadge } from '@/components/tournament/TournamentStatusBadge'

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

  const goProfile = (userId: string | null) => {
    if (userId) {
      onClose()
      navigate(`/u/${userId}`)
    }
  }

  const mode = detail ? gameModeLabel(detail.gameMode, detail.format) : null
  const title = detail?.name?.trim() || [detail?.setCodes, mode?.variant ?? mode?.primary].filter(Boolean).join(' ') || 'Tournament'

  return (
    <div style={styles.backdrop} onClick={onClose} role="presentation">
      <div style={styles.modal} onClick={(e) => e.stopPropagation()} role="dialog" aria-modal>
        <div style={styles.header}>
          <span style={styles.titleRow}>
            <h2 style={styles.title}>{title}</h2>
            {detail && detail.status !== 'COMPLETED' && <TournamentStatusBadge status={detail.status} />}
          </span>
          <button type="button" style={styles.close} onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        {detail && mode && (
          <p style={styles.muted}>
            {mode.primary}
            {mode.variant ? <span style={styles.variant}> › {mode.variant}</span> : null}
            {' · '}
            {detail.playerCount} players · {detail.endedAt.slice(0, 10)}
          </p>
        )}

        {error ? (
          <p style={styles.error}>{error}</p>
        ) : !detail ? (
          <p style={styles.muted}>Loading…</p>
        ) : (
          <>
            <h3 style={styles.section}>{detail.status === 'COMPLETED' ? 'Final standings' : 'Standings so far'}</h3>
            <div style={styles.tableWrap}>
              <table style={styles.table}>
                <thead>
                  <tr>
                    <th style={styles.thNumLeft}>#</th>
                    <th style={styles.th}>Player</th>
                    <th style={styles.thNum}>W</th>
                    <th style={styles.thNum}>L</th>
                    <th style={styles.thNum}>D</th>
                  </tr>
                </thead>
                <tbody>
                  {detail.standings.map((s, i) => (
                    <tr key={`${s.playerName}-${i}`}>
                      <td style={styles.tdRank}>
                        {s.placement}
                        {s.placement === 1 ? ' 🏆' : ''}
                      </td>
                      <td style={styles.td}>
                        <PlayerName name={s.playerName} userId={s.userId} isAi={s.isAi} onClick={goProfile} />
                      </td>
                      <td style={styles.tdNum}>{s.wins}</td>
                      <td style={styles.tdNum}>{s.losses}</td>
                      <td style={styles.tdNum}>{s.draws}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <h3 style={styles.section}>Games</h3>
            {detail.games.length === 0 ? (
              <p style={styles.muted}>No game records were stored for this tournament.</p>
            ) : (
              <ul style={styles.gameList}>
                {detail.games.map((g, i) => (
                  <li key={`${g.gameId}-${i}`} style={styles.gameRow}>
                    <span style={styles.gamePlayers}>
                      {g.players.map((p, j) => (
                        <span key={`${p.name}-${j}`}>
                          {j > 0 && <span style={styles.vs}> vs </span>}
                          <span style={p.won ? styles.winner : undefined}>
                            <PlayerName name={p.name} userId={p.userId} isAi={p.isAi} onClick={goProfile} />
                            {p.won ? ' ✓' : ''}
                          </span>
                        </span>
                      ))}
                    </span>
                    <span style={styles.gameActions}>
                      <span style={styles.gameDate}>{g.endedAt.slice(0, 10)}</span>
                      {g.hasReplay ? (
                        <button
                          type="button"
                          style={styles.link}
                          onClick={() => {
                            onClose()
                            navigate(`/replay/${g.gameId}`)
                          }}
                        >
                          Watch
                        </button>
                      ) : (
                        <span style={styles.noReplay}>—</span>
                      )}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </div>
    </div>
  )
}

/** A player name that links to their public profile when signed in, with an AI tag otherwise. */
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
      <button type="button" style={styles.playerLink} onClick={() => onClick(userId)}>
        {name}
      </button>
    )
  }
  return (
    <span style={styles.playerPlain}>
      {name}
      {isAi ? <span style={styles.aiTag}> AI</span> : null}
    </span>
  )
}

const styles: Record<string, React.CSSProperties> = {
  backdrop: {
    position: 'fixed',
    inset: 0,
    backgroundColor: 'rgba(0,0,0,0.6)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
    zIndex: 1000,
  },
  modal: {
    width: 'min(720px, 100%)',
    maxHeight: '85vh',
    overflowY: 'auto',
    backgroundColor: '#12121e',
    border: '1px solid #2a2a3e',
    borderRadius: 14,
    padding: '20px 22px',
  },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 },
  titleRow: { display: 'inline-flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' },
  title: { margin: 0, color: '#fff', fontSize: 20 },
  close: { background: 'none', border: 'none', color: '#999', cursor: 'pointer', fontSize: 18 },
  muted: { margin: '4px 0 0', color: '#888', fontSize: 13 },
  variant: { color: '#aab' },
  error: { margin: '8px 0 0', color: '#ff6b6b', fontSize: 13 },
  section: { margin: '18px 0 8px', color: '#fff', fontSize: 15 },
  tableWrap: { overflowX: 'auto' },
  table: { width: '100%', borderCollapse: 'collapse', fontSize: 13 },
  th: { textAlign: 'left', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e' },
  thNum: { textAlign: 'right', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e' },
  thNumLeft: { textAlign: 'left', color: '#888', fontWeight: 600, padding: '6px 8px', borderBottom: '1px solid #2a2a3e', width: 48 },
  td: { textAlign: 'left', color: '#ccc', padding: '6px 8px', borderBottom: '1px solid #1f1f2e' },
  tdNum: { textAlign: 'right', color: '#ccc', padding: '6px 8px', borderBottom: '1px solid #1f1f2e', fontVariantNumeric: 'tabular-nums' },
  tdRank: { textAlign: 'left', color: '#cdd', padding: '6px 8px', borderBottom: '1px solid #1f1f2e', fontVariantNumeric: 'tabular-nums', fontWeight: 600 },
  gameList: { listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: 6 },
  gameRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
    flexWrap: 'wrap',
    backgroundColor: '#171723',
    border: '1px solid #23233a',
    borderRadius: 8,
    padding: '8px 12px',
  },
  gamePlayers: { color: '#cdd', fontSize: 13 },
  vs: { color: '#666' },
  winner: { color: '#fff', fontWeight: 600 },
  gameActions: { display: 'inline-flex', alignItems: 'center', gap: 12, flexShrink: 0 },
  gameDate: { color: '#777', fontSize: 12, fontVariantNumeric: 'tabular-nums' },
  link: { background: 'none', border: 'none', color: '#8b9bff', cursor: 'pointer', fontSize: 13, padding: 0 },
  noReplay: { color: '#555', fontSize: 13 },
  playerLink: { background: 'none', border: 'none', color: '#8b9bff', cursor: 'pointer', fontSize: 'inherit', padding: 0 },
  playerPlain: { color: 'inherit' },
  aiTag: { color: '#888', fontSize: 11 },
}
