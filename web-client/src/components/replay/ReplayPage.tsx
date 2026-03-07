import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useGameStore } from '../../store/gameStore'
import { SpectatorContext } from '../../contexts/SpectatorContext'
import { GameBoard } from '../game/GameBoard'
import { CombatArrows } from '../combat/CombatArrows'
import type { SpectatingState } from '../../store/slices/types'
import type { SpectatorStateUpdate } from '../admin/ReplayViewer'
import { reconstructSnapshots, type PublicReplayData } from '../../replay/reconstructSnapshots'

const HEADER_HEIGHT = 55

export function ReplayPage() {
  const { gameId } = useParams<{ gameId: string }>()
  const navigate = useNavigate()

  const [snapshots, setSnapshots] = useState<SpectatorStateUpdate[]>([])
  const [metadata, setMetadata] = useState<PublicReplayData['metadata'] | null>(null)
  const [currentStep, setCurrentStep] = useState(0)
  const [autoPlay, setAutoPlay] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const setSpectatingState = useGameStore((s) => s.setSpectatingState)

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

  // Load replay on mount
  useEffect(() => {
    if (!gameId) return
    let cancelled = false

    async function loadReplay() {
      setLoading(true)
      setError(null)
      try {
        const response = await fetch(`/api/public/replays/${gameId}`)
        if (!response.ok) {
          if (response.status === 404) {
            setError('Replay not found. It may have expired or the game ID is invalid.')
          } else {
            setError('Failed to load replay.')
          }
          setLoading(false)
          return
        }
        const data = await response.json() as PublicReplayData
        if (cancelled) return
        setMetadata(data.metadata)
        const reconstructed = reconstructSnapshots(data.initialSnapshot, data.deltas)
        setSnapshots(reconstructed)
        setCurrentStep(0)
        if (reconstructed.length > 0) {
          writeSnapshotToStore(reconstructed[0]!)
        }
      } catch {
        if (!cancelled) setError('Failed to load replay.')
      }
      if (!cancelled) setLoading(false)
    }

    loadReplay()
    return () => { cancelled = true }
  }, [gameId, writeSnapshotToStore])

  // Clean up spectating state on unmount
  useEffect(() => {
    return () => {
      setSpectatingState(null)
    }
  }, [setSpectatingState])

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

  // Keyboard shortcuts
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'ArrowLeft') { e.preventDefault(); goToStep(currentStep - 1) }
      else if (e.key === 'ArrowRight') { e.preventDefault(); goToStep(currentStep + 1) }
      else if (e.key === ' ') { e.preventDefault(); setAutoPlay((p) => !p) }
      else if (e.key === 'Escape') { navigate('/') }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [goToStep, currentStep, navigate])

  const handleShare = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // Fallback: select text
    }
  }

  if (loading) {
    return (
      <div style={styles.centered}>
        <div style={styles.spinner} />
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        <p style={styles.loadingText}>Loading replay...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div style={styles.centered}>
        <p style={styles.errorText}>{error}</p>
        <button onClick={() => navigate('/')} style={styles.backButton}>
          Go to Home
        </button>
      </div>
    )
  }

  const currentSnapshot = snapshots[currentStep]
  if (!currentSnapshot) return null

  return (
    <SpectatorContext.Provider
      value={{
        isSpectating: true,
        player1Id: currentSnapshot.player1Id,
        player2Id: currentSnapshot.player2Id,
        player1Name: currentSnapshot.player1Name ?? 'Player 1',
        player2Name: currentSnapshot.player2Name ?? 'Player 2',
      }}
    >
      <div style={styles.replayContainer}>
        <div style={styles.replayHeader}>
          <button onClick={() => navigate('/')} style={styles.backButton}>
            Back
          </button>
          <div style={styles.replayControls}>
            <button onClick={() => goToStep(currentStep - 1)} disabled={currentStep === 0} style={styles.controlButton} title="Previous (Left Arrow)">
              Prev
            </button>
            <button onClick={() => setAutoPlay(!autoPlay)} style={styles.controlButton} title="Play/Pause (Space)">
              {autoPlay ? 'Pause' : 'Play'}
            </button>
            <button onClick={() => goToStep(currentStep + 1)} disabled={currentStep >= snapshots.length - 1} style={styles.controlButton} title="Next (Right Arrow)">
              Next
            </button>
          </div>
          <div style={styles.scrubberContainer}>
            <input
              type="range"
              min={0}
              max={snapshots.length - 1}
              value={currentStep}
              onChange={(e) => goToStep(Number(e.target.value))}
              style={styles.scrubber}
            />
            <span style={styles.stepCounter}>
              {currentStep + 1} / {snapshots.length}
            </span>
          </div>
          <div style={styles.replayInfo}>
            <span style={styles.replayLabel}>Replay</span>
            <span style={styles.matchupText}>
              {metadata?.player1Name ?? currentSnapshot.player1Name} vs {metadata?.player2Name ?? currentSnapshot.player2Name}
            </span>
            {metadata?.winnerName && (
              <span style={styles.winnerText}>Winner: {metadata.winnerName}</span>
            )}
          </div>
          <button onClick={handleShare} style={styles.shareButton} title="Copy link to clipboard">
            {copied ? 'Copied!' : 'Share'}
          </button>
        </div>
        <div style={styles.gameBoardContainer}>
          <GameBoard spectatorMode topOffset={HEADER_HEIGHT} />
        </div>
      </div>
      <CombatArrows />
    </SpectatorContext.Provider>
  )
}

const styles: Record<string, React.CSSProperties> = {
  centered: {
    minHeight: '100vh',
    backgroundColor: '#0a0a12',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
  },
  spinner: {
    width: 40,
    height: 40,
    border: '3px solid #333',
    borderTopColor: '#888',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  loadingText: {
    color: '#888',
    fontSize: 16,
  },
  errorText: {
    color: '#ef4444',
    fontSize: 16,
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
    textAlign: 'right',
    flexShrink: 0,
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
  winnerText: {
    display: 'block',
    color: '#4fc3f7',
    fontSize: 11,
  },
  shareButton: {
    padding: '8px 16px',
    fontSize: 13,
    backgroundColor: '#1e40af',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
    flexShrink: 0,
  },
  gameBoardContainer: {
    flex: 1,
    position: 'relative',
    overflow: 'hidden',
  },
}
