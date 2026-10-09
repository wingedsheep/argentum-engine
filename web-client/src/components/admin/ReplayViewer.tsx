/**
 * The in-app replay overlay: a list of finished games, and the player for whichever one you pick.
 *
 * It stays separate from the `/replay/:gameId` route because it must not navigate — this is opened
 * over a live screen (home, tournament standings, an FFA pod) and routing away would drop the
 * WebSocket. Only the *list* half is specific to it; playback is the shared {@link ReplayPlayer}.
 */
import { useState, useEffect, useCallback, useRef } from 'react'
import {
  reconstructSnapshots,
  type ReplayData,
  type SpectatorStateUpdate,
} from '@/replay/reconstructSnapshots.ts'
import { uploadReplayFile } from '@/replay/replayFile.ts'
import { ReplayPlayer, type ReplayMetadata } from '../replay/ReplayPlayer'
import { PageShell, pageStyles } from '../ui/PageShell'
import styles from '../replay/Replay.module.css'

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
  /** Set when watching an uploaded file: the upload response carries metadata, the list's endpoints don't. */
  const [fileMetadata, setFileMetadata] = useState<ReplayMetadata | null>(null)

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
      setFileMetadata(null)
      setView('replay')
    } catch {
      setError('Failed to load replay')
    }
    setLoading(false)
  }

  const handleOpenFile = async (file: File) => {
    setLoading(true)
    setError(null)
    try {
      const data = await uploadReplayFile(file)
      setSnapshots(reconstructSnapshots(data.initialSnapshot, data.deltas))
      setReplayGameId(data.metadata.gameId)
      setFileMetadata(data.metadata)
      setView('replay')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load replay file')
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
        onOpenFile={handleOpenFile}
        onReload={loadGames}
        onBack={onBack}
        loading={loading}
        error={error}
      />
    )
  }

  // The admin/tournament replay endpoints return frames only — no metadata block — so the player
  // falls back to the frame's own seat names and hides the winner line. Everything else is shared.
  return (
    <ReplayPlayer
      snapshots={snapshots}
      gameId={replayGameId}
      metadata={fileMetadata}
      fromFile={fileMetadata != null}
      onExit={handleBackToList}
      onHome={onBack}
    />
  )
}

// ============================================================================
// Game List View
// ============================================================================

interface GameGroup {
  label: string
  games: GameSummary[]
}

/** How the list is laid out. A flat date order is the default — grouping is opt-in. */
type ListOrder = 'newest' | 'oldest' | 'tournament'

const ORDER_LABELS: Record<ListOrder, string> = {
  newest: 'Newest first',
  oldest: 'Oldest first',
  tournament: 'By tournament',
}

function endedAtMillis(game: GameSummary): number {
  const t = Date.parse(game.endedAt)
  return Number.isNaN(t) ? 0 : t
}

/** Sorts by when the game ended; the endpoints don't agree on an order, so the list sets its own. */
function sortByEnded(games: GameSummary[], direction: 'asc' | 'desc'): GameSummary[] {
  const sign = direction === 'asc' ? 1 : -1
  return [...games].sort((a, b) => sign * (endedAtMillis(a) - endedAtMillis(b)))
}

/**
 * Tournaments first, most recently played first, each in the order its games were played (round
 * order); casual games last, newest first.
 */
function groupByTournament(games: GameSummary[]): GameGroup[] {
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

  const latest = (gs: GameSummary[]) => Math.max(...gs.map(endedAtMillis))
  const groups: GameGroup[] = [...tournamentMap]
    .sort(([, a], [, b]) => latest(b) - latest(a))
    .map(([name, tournamentGames]) => ({ label: name, games: sortByEnded(tournamentGames, 'asc') }))
  if (casual.length > 0) {
    groups.push({ label: 'Casual Games', games: sortByEnded(casual, 'desc') })
  }
  return groups
}

function GameListView({
  games,
  onReplay,
  onOpenFile,
  onReload,
  onBack,
  loading,
  error,
}: {
  games: GameSummary[]
  onReplay: (gameId: string) => void
  onOpenFile: (file: File) => void
  onReload: () => void
  onBack: () => void
  loading: boolean
  error: string | null
}) {
  const hasTournaments = games.some((g) => g.tournamentName)
  const [order, setOrder] = useState<ListOrder>('newest')
  // "By tournament" is only offered when there is a tournament to group by.
  const effectiveOrder: ListOrder = order === 'tournament' && !hasTournaments ? 'newest' : order
  const orders: ListOrder[] = hasTournaments ? ['newest', 'oldest', 'tournament'] : ['newest', 'oldest']
  const fileInputRef = useRef<HTMLInputElement>(null)

  return (
    // This list is an in-app screen, not a route: on the landing screen it is a flag in
    // HomeScreen's state at "/", so the shell's home link (a Link to "/") would go nowhere. Clicks on
    // it are taken here and closed the way Back closes them.
    <div
      className={styles.listScroll}
      onClickCapture={(e) => {
        const home = (e.target as HTMLElement).closest('a[href="/"]')
        if (home) { e.preventDefault(); e.stopPropagation(); onBack() }
      }}
    >
      <PageShell title="Replays" width="normal">
        <div className={styles.listHeader}>
          <div className={styles.listHeading}>
            <h1 className={pageStyles.h1}>Your replays</h1>
            <p className={pageStyles.lede}>Watch any finished game again, step by step.</p>
          </div>
          <div className={styles.listActions}>
            <input
              ref={fileInputRef}
              type="file"
              accept=".json,.gz,.replay,application/json,application/gzip"
              style={{ display: 'none' }}
              onChange={(e) => {
                const file = e.target.files?.[0]
                // Reset so choosing the same file again still fires onChange.
                e.target.value = ''
                if (file) onOpenFile(file)
              }}
            />
            <button type="button" onClick={onBack} className={pageStyles.buttonGhost}>
              ← Back
            </button>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={loading}
              className={pageStyles.button}
              title="Watch a replay file exported from any game ('Export' in the replay viewer)."
            >
              Open file
            </button>
            <button type="button" onClick={onReload} disabled={loading} className={pageStyles.button}>
              {loading ? 'Loading…' : 'Reload'}
            </button>
          </div>
        </div>
        {error && <p className={styles.error} role="alert">{error}</p>}
        {games.length > 1 && (
          <div className={pageStyles.tabs} role="tablist" aria-label="Order" style={{ alignSelf: 'flex-start' }}>
            {orders.map((o) => (
              <button
                key={o}
                type="button"
                role="tab"
                aria-selected={effectiveOrder === o}
                onClick={() => setOrder(o)}
                className={pageStyles.tab}
              >
                {ORDER_LABELS[o]}
              </button>
            ))}
          </div>
        )}
        <section className={pageStyles.panel}>
          {games.length === 0 ? (
            <div className={pageStyles.empty}>
              <p className={styles.emptyTitle}>{loading ? 'Loading games…' : 'No finished games yet'}</p>
              {!loading && <p className={pageStyles.muted}>Play a game and it shows up here when it ends.</p>}
            </div>
          ) : effectiveOrder === 'tournament' ? (
            <div className={styles.group} style={{ gap: 18 }}>
              {groupByTournament(games).map((group) => (
                <div key={group.label} className={styles.group}>
                  <h2 className={styles.groupTitle}>{group.label}</h2>
                  <GameTable games={group.games} onReplay={onReplay} showRound showTournament={false} />
                </div>
              ))}
            </div>
          ) : (
            <GameTable
              games={sortByEnded(games, effectiveOrder === 'oldest' ? 'asc' : 'desc')}
              onReplay={onReplay}
              showRound
              showTournament
            />
          )}
        </section>
      </PageShell>
    </div>
  )
}

function GameTable({
  games,
  onReplay,
  showRound,
  showTournament,
}: {
  games: GameSummary[]
  onReplay: (gameId: string) => void
  showRound: boolean
  /** In a flat list the tournament name is the only hint of which event a game belonged to. */
  showTournament: boolean
}) {
  return (
    <ul className={styles.rows} role="list">
      {games.map((game) => (
        <li key={game.gameId} className={styles.row}>
          <div className={styles.rowMain}>
            <span className={styles.players}>
              {game.player1Name}<span className={styles.vs}>vs</span>{game.player2Name}
            </span>
            <span className={styles.meta}>
              {showTournament && game.tournamentName && <span>{game.tournamentName}</span>}
              {showRound && game.tournamentRound != null && (
                <span className={styles.round}>Round {game.tournamentRound + 1}</span>
              )}
              <span>{formatDate(game.endedAt)}</span>
              <span className={styles.metaWinner}>{game.winnerName ? `${game.winnerName} won` : 'Draw'}</span>
              <span>{game.snapshotCount} steps</span>
            </span>
          </div>
          <button type="button" onClick={() => onReplay(game.gameId)} className={`${pageStyles.buttonPrimary} ${styles.watch}`}>
            Replay
          </button>
        </li>
      ))}
    </ul>
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
