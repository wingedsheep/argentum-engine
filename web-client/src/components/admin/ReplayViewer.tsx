import { useState, useEffect, useCallback } from 'react'
import { useGameStore } from '../../store/gameStore'
import { SpectatorContext } from '../../contexts/SpectatorContext'
import { GameBoard } from '../game/GameBoard'
import { CombatArrows } from '../combat/CombatArrows'
import type { SpectatingState } from '../../store/slices/types'

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

export interface SpectatorStateUpdate {
  gameSessionId: string
  gameState: unknown
  player1Id: string | null
  player2Id: string | null
  player1Name: string | null
  player2Name: string | null
  player1: unknown
  player2: unknown
  currentPhase: string
  activePlayerId: string | null
  priorityPlayerId: string | null
  combat: unknown
  decisionStatus: unknown
}

// ============================================================================
// ReplayViewer
// ============================================================================

interface ReplayViewerProps {
  fetchGames: () => Promise<GameSummary[]>
  fetchReplay: (gameId: string) => Promise<SpectatorStateUpdate[]>
  onBack: () => void
}

type View = 'list' | 'replay'

export function ReplayViewer({ fetchGames, fetchReplay, onBack }: ReplayViewerProps) {
  const [view, setView] = useState<View>('list')
  const [games, setGames] = useState<GameSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Replay state
  const [snapshots, setSnapshots] = useState<SpectatorStateUpdate[]>([])
  const [currentStep, setCurrentStep] = useState(0)
  const [autoPlay, setAutoPlay] = useState(false)
  const setSpectatingState = useGameStore((s) => s.setSpectatingState)

  const loadGames = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchGames()
      setGames(data)
    } catch {
      setError('Failed to load games')
    }
    setLoading(false)
  }, [fetchGames])

  // Load games on mount
  useEffect(() => {
    loadGames()
  }, [loadGames])

  const handleReplay = async (gameId: string) => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchReplay(gameId)
      setSnapshots(data)
      setCurrentStep(0)
      setAutoPlay(false)
      setView('replay')
      if (data.length > 0) {
        writeSnapshotToStore(data[0]!)
      }
    } catch {
      setError('Failed to load replay')
    }
    setLoading(false)
  }

  const writeSnapshotToStore = useCallback(
    (snapshot: SpectatorStateUpdate) => {
      const state: SpectatingState = {
        gameSessionId: snapshot.gameSessionId,
        gameState: snapshot.gameState as SpectatingState['gameState'],
        player1Id: snapshot.player1Id,
        player2Id: snapshot.player2Id,
        player1Name: snapshot.player1Name ?? 'Player 1',
        player2Name: snapshot.player2Name ?? 'Player 2',
        player1: snapshot.player1 as SpectatingState['player1'],
        player2: snapshot.player2 as SpectatingState['player2'],
        currentPhase: snapshot.currentPhase,
        activePlayerId: snapshot.activePlayerId,
        priorityPlayerId: snapshot.priorityPlayerId,
        combat: snapshot.combat as SpectatingState['combat'],
        decisionStatus: snapshot.decisionStatus as SpectatingState['decisionStatus'],
        isReplay: true,
      }
      setSpectatingState(state)
    },
    [setSpectatingState],
  )

  const goToStep = useCallback(
    (step: number) => {
      if (step < 0 || step >= snapshots.length) return
      setCurrentStep(step)
      writeSnapshotToStore(snapshots[step]!)
    },
    [snapshots, writeSnapshotToStore],
  )

  // Auto-play timer
  useEffect(() => {
    if (!autoPlay) return
    const timer = setInterval(() => {
      setCurrentStep((prev) => {
        const next = prev + 1
        if (next >= snapshots.length) {
          setAutoPlay(false)
          return prev
        }
        writeSnapshotToStore(snapshots[next]!)
        return next
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [autoPlay, snapshots, writeSnapshotToStore])

  const handleBackToList = () => {
    setView('list')
    setSpectatingState(null)
    setSnapshots([])
    setAutoPlay(false)
  }

  // Clean up spectating state on unmount
  useEffect(() => {
    return () => {
      setSpectatingState(null)
    }
  }, [setSpectatingState])

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

  // Replay view
  const currentSnapshot = snapshots[currentStep]
  if (!currentSnapshot) return null

  return (
    <ReplayView
      snapshot={currentSnapshot}
      currentStep={currentStep}
      totalSteps={snapshots.length}
      autoPlay={autoPlay}
      onPrev={() => goToStep(currentStep - 1)}
      onNext={() => goToStep(currentStep + 1)}
      onGoToStep={goToStep}
      onToggleAutoPlay={() => setAutoPlay(!autoPlay)}
      onBack={handleBackToList}
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

const HEADER_HEIGHT = 55

function ReplayView({
  snapshot,
  currentStep,
  totalSteps,
  autoPlay,
  onPrev,
  onNext,
  onGoToStep,
  onToggleAutoPlay,
  onBack,
}: {
  snapshot: SpectatorStateUpdate
  currentStep: number
  totalSteps: number
  autoPlay: boolean
  onPrev: () => void
  onNext: () => void
  onGoToStep: (step: number) => void
  onToggleAutoPlay: () => void
  onBack: () => void
}) {
  // Keyboard shortcuts
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') { e.preventDefault(); onPrev() }
      else if (e.key === 'ArrowRight') { e.preventDefault(); onNext() }
      else if (e.key === ' ') { e.preventDefault(); onToggleAutoPlay() }
      else if (e.key === 'Escape') { onBack() }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onPrev, onNext, onToggleAutoPlay, onBack])

  return (
    <SpectatorContext.Provider
      value={{
        isSpectating: true,
        player1Id: snapshot.player1Id,
        player2Id: snapshot.player2Id,
        player1Name: snapshot.player1Name ?? 'Player 1',
        player2Name: snapshot.player2Name ?? 'Player 2',
      }}
    >
      <div style={styles.replayContainer}>
        <div style={styles.replayHeader}>
          <button onClick={onBack} style={styles.backButton}>
            Back
          </button>
          <div style={styles.replayControls}>
            <button onClick={onPrev} disabled={currentStep === 0} style={styles.controlButton} title="Previous (Left Arrow)">
              Prev
            </button>
            <button onClick={onToggleAutoPlay} style={styles.controlButton} title="Play/Pause (Space)">
              {autoPlay ? 'Pause' : 'Play'}
            </button>
            <button onClick={onNext} disabled={currentStep >= totalSteps - 1} style={styles.controlButton} title="Next (Right Arrow)">
              Next
            </button>
          </div>
          <div style={styles.scrubberContainer}>
            <input
              type="range"
              min={0}
              max={totalSteps - 1}
              value={currentStep}
              onChange={(e) => onGoToStep(Number(e.target.value))}
              style={styles.scrubber}
            />
            <span style={styles.stepCounter}>
              {currentStep + 1} / {totalSteps}
            </span>
          </div>
          <div style={styles.replayInfo}>
            <span style={styles.replayLabel}>Replay</span>
            <span style={styles.matchupText}>
              {snapshot.player1Name} vs {snapshot.player2Name}
            </span>
          </div>
        </div>
        <div style={styles.gameBoardContainer}>
          <GameBoard spectatorMode topOffset={HEADER_HEIGHT} />
        </div>
      </div>
      <CombatArrows />
    </SpectatorContext.Provider>
  )
}

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
    minHeight: '100vh',
    backgroundColor: '#0a0a12',
    color: '#ccc',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'flex-start',
    paddingTop: 80,
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
  replayContainer: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: '#0a0a12',
    display: 'flex',
    flexDirection: 'column',
    zIndex: 1500,
  },
  replayHeader: {
    display: 'flex',
    alignItems: 'center',
    padding: '10px 16px',
    borderBottom: '1px solid #1a1a25',
    backgroundColor: '#0d0d15',
    flexShrink: 0,
    zIndex: 1600,
    gap: 16,
  },
  backButton: {
    padding: '8px 16px',
    fontSize: 13,
    backgroundColor: 'transparent',
    color: '#888',
    border: '1px solid #333',
    borderRadius: 6,
    cursor: 'pointer',
    flexShrink: 0,
  },
  replayControls: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
  controlButton: {
    padding: '6px 14px',
    fontSize: 13,
    backgroundColor: '#1a1a2e',
    color: '#ccc',
    border: '1px solid #2a2a3e',
    borderRadius: 4,
    cursor: 'pointer',
  },
  scrubberContainer: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    flex: 1,
    minWidth: 0,
  },
  scrubber: {
    flex: 1,
    minWidth: 80,
    height: 4,
    appearance: 'none' as const,
    WebkitAppearance: 'none' as const,
    background: '#2a2a3e',
    borderRadius: 2,
    outline: 'none',
    cursor: 'pointer',
    accentColor: '#4fc3f7',
  },
  stepCounter: {
    color: '#888',
    fontSize: 13,
    minWidth: 70,
    flexShrink: 0,
  },
  replayInfo: {
    marginLeft: 'auto',
    textAlign: 'right',
  },
  replayLabel: {
    display: 'block',
    color: '#666',
    fontSize: 11,
    textTransform: 'uppercase',
    letterSpacing: '0.1em',
  },
  matchupText: {
    color: '#aaa',
    fontSize: 13,
  },
  gameBoardContainer: {
    flex: 1,
    position: 'relative',
    overflow: 'hidden',
  },
}
