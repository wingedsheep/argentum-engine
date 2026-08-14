/**
 * The in-app replay overlay: a list of finished games, and the player for whichever one you pick.
 *
 * It stays separate from the `/replay/:gameId` route because it must not navigate — this is opened
 * over a live screen (home, tournament standings, an FFA pod) and routing away would drop the
 * WebSocket. Only the *list* half is specific to it; playback is the shared {@link ReplayPlayer}.
 */
import { useState, useEffect, useCallback } from 'react'
import {
  reconstructSnapshots,
  type ReplayData,
  type SpectatorStateUpdate,
} from '@/replay/reconstructSnapshots.ts'
import { ReplayPlayer } from '../replay/ReplayPlayer'

// ============================================================================
// Types
// ============================================================================

export interface GameSummary {
  gameId: string
  player1Name: string
  player2Name: string
  startedAt: string
  endedAt: string
  winnerName: string | null
  snapshotCount: number
  tournamentName: string | null
  tournamentRound: number | null
}


// ============================================================================
// ReplayViewer
// ============================================================================

interface ReplayViewerProps {
  fetchGames: () => Promise<GameSummary[]>
  fetchReplay: (gameId: string) => Promise<ReplayData>
  onBack: () => void
}

type View = 'list' | 'replay'

export function ReplayViewer({ fetchGames, fetchReplay, onBack }: ReplayViewerProps) {
  const [view, setView] = useState<View>('list')
  const [games, setGames] = useState<GameSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [snapshots, setSnapshots] = useState<SpectatorStateUpdate[]>([])
  const [replayGameId, setReplayGameId] = useState<string>('')

  const loadGames = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setGames(await fetchGames())
    } catch {
      setError('Failed to load games')
    }
    setLoading(false)
  }, [fetchGames])

  useEffect(() => {
    loadGames()
  }, [loadGames])

  const handleReplay = async (gameId: string) => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchReplay(gameId)
      setSnapshots(reconstructSnapshots(data.initialSnapshot, data.deltas))
      setReplayGameId(gameId)
      setView('replay')
    } catch {
      setError('Failed to load replay')
    }
    setLoading(false)
  }

  const handleBackToList = useCallback(() => {
    setView('list')
    setSnapshots([])
  }, [])

  if (view === 'list') {
    return (
      <GameListView
        games={games}
        onReplay={handleReplay}
        onReload={loadGames}
        onBack={onBack}
        loading={loading}
        error={error}
      />
    )
  }

  // The admin/tournament replay endpoints return frames only — no metadata block — so the player
  // falls back to the frame's own seat names and hides the winner line. Everything else is shared.
  return <ReplayPlayer snapshots={snapshots} gameId={replayGameId} onExit={handleBackToList} />
}

// ============================================================================
// Game List View
// ============================================================================

interface GameGroup {
  label: string
  games: GameSummary[]
}

function groupByTournament(games: GameSummary[]): GameGroup[] {
  const groups: GameGroup[] = []
  const tournamentMap = new Map<string, GameSummary[]>()
  const casual: GameSummary[] = []

  for (const game of games) {
    if (game.tournamentName) {
      const existing = tournamentMap.get(game.tournamentName)
      if (existing) {
        existing.push(game)
      } else {
        tournamentMap.set(game.tournamentName, [game])
      }
    } else {
      casual.push(game)
    }
  }

  for (const [name, tournamentGames] of tournamentMap) {
    groups.push({ label: name, games: tournamentGames })
  }
  if (casual.length > 0) {
    groups.push({ label: 'Casual Games', games: casual })
  }
  return groups
}

function GameListView({
  games,
  onReplay,
  onReload,
  onBack,
  loading,
  error,
}: {
  games: GameSummary[]
  onReplay: (gameId: string) => void
  onReload: () => void
  onBack: () => void
  loading: boolean
  error: string | null
}) {
  const groups = groupByTournament(games)
  const hasTournaments = games.some((g) => g.tournamentName)

  return (
    <div style={styles.pageContainer}>
      <div style={styles.listContainer}>
        <div style={styles.listHeader}>
          <h1 style={styles.listTitle}>Game Replays</h1>
          <div style={styles.headerButtons}>
            <button onClick={onReload} disabled={loading} style={styles.secondaryButton}>
              {loading ? 'Loading...' : 'Reload'}
            </button>
            <button onClick={onBack} style={styles.secondaryButton}>
              Back
            </button>
          </div>
        </div>
        {error && <p style={styles.errorText}>{error}</p>}
        {games.length === 0 ? (
          <p style={styles.emptyText}>No completed games yet.</p>
        ) : hasTournaments ? (
          groups.map((group) => (
            <div key={group.label} style={styles.groupContainer}>
              <h2 style={styles.groupTitle}>{group.label}</h2>
              <GameTable games={group.games} onReplay={onReplay} showRound />
            </div>
          ))
        ) : (
          <GameTable games={games} onReplay={onReplay} showRound={false} />
        )}
      </div>
    </div>
  )
}

function GameTable({
  games,
  onReplay,
  showRound,
}: {
  games: GameSummary[]
  onReplay: (gameId: string) => void
  showRound: boolean
}) {
  return (
    <table style={styles.table}>
      <thead>
        <tr>
          {showRound && <th style={styles.th}>Round</th>}
          <th style={styles.th}>Players</th>
          <th style={styles.th}>Date</th>
          <th style={styles.th}>Winner</th>
          <th style={styles.th}>Steps</th>
          <th style={styles.th}></th>
        </tr>
      </thead>
      <tbody>
        {games.map((game) => (
          <tr key={game.gameId} style={styles.tr}>
            {showRound && (
              <td style={styles.td}>
                {game.tournamentRound != null ? game.tournamentRound + 1 : '-'}
              </td>
            )}
            <td style={styles.td}>
              {game.player1Name} vs {game.player2Name}
            </td>
            <td style={styles.td}>{formatDate(game.endedAt)}</td>
            <td style={styles.td}>{game.winnerName ?? 'Draw'}</td>
            <td style={styles.td}>{game.snapshotCount}</td>
            <td style={styles.td}>
              <button onClick={() => onReplay(game.gameId)} style={styles.replayButton}>
                Replay
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

// ============================================================================
// Replay View
// ============================================================================


// ============================================================================
// Helpers
// ============================================================================

function formatDate(iso: string): string {
  try {
    const d = new Date(iso)
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
  } catch {
    return iso
  }
}

// ============================================================================
// Styles
// ============================================================================

const styles: Record<string, React.CSSProperties> = {
  pageContainer: {
    height: '100vh',
    backgroundColor: '#0a0a12',
    color: '#ccc',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'flex-start',
    paddingTop: 80,
    paddingBottom: 80,
    overflowY: 'auto',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  },
  listContainer: {
    width: '100%',
    maxWidth: 900,
    padding: '0 24px',
  },
  listHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  listTitle: {
    margin: 0,
    fontSize: 24,
    color: '#e0e0e0',
  },
  headerButtons: {
    display: 'flex',
    gap: 8,
  },
  secondaryButton: {
    padding: '8px 16px',
    fontSize: 13,
    backgroundColor: 'transparent',
    color: '#888',
    border: '1px solid #333',
    borderRadius: 6,
    cursor: 'pointer',
  },
  errorText: {
    color: '#ef4444',
    fontSize: 13,
    marginTop: 12,
    marginBottom: 0,
  },
  emptyText: {
    color: '#555',
    fontSize: 14,
    textAlign: 'center',
    marginTop: 48,
  },
  groupContainer: {
    marginBottom: 32,
  },
  groupTitle: {
    margin: '0 0 12px 0',
    fontSize: 16,
    fontWeight: 500,
    color: '#8ab4f8',
    borderBottom: '1px solid #1a1a2e',
    paddingBottom: 8,
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
  },
  th: {
    textAlign: 'left',
    padding: '10px 12px',
    fontSize: 12,
    color: '#666',
    textTransform: 'uppercase',
    letterSpacing: '0.05em',
    borderBottom: '1px solid #1a1a2e',
  },
  tr: {
    borderBottom: '1px solid #111',
  },
  td: {
    padding: '12px 12px',
    fontSize: 14,
  },
  replayButton: {
    padding: '6px 14px',
    fontSize: 12,
    backgroundColor: '#1e40af',
    color: '#fff',
    border: 'none',
    borderRadius: 4,
    cursor: 'pointer',
  },
}
