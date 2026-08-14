/**
 * `/replay/:gameId` — the shareable, deep-linkable replay URL.
 *
 * This route owns only "which replay, and did it load": the id from the URL, the public-replay
 * fetch, loading/error states, and stamping the seat→team map. Everything after that — transport,
 * scrubber, share/export, the board — is {@link ReplayPlayer}, shared with the in-app overlay so
 * the two cannot drift apart again.
 */
import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore.ts'
import {
  reconstructSnapshots,
  type PublicReplayData,
  type SpectatorStateUpdate,
} from '@/replay/reconstructSnapshots.ts'
import { ReplayPlayer, type ReplayMetadata } from './ReplayPlayer'

export function ReplayPage() {
  const { gameId } = useParams<{ gameId: string }>()
  const navigate = useNavigate()

  const [snapshots, setSnapshots] = useState<SpectatorStateUpdate[]>([])
  const [metadata, setMetadata] = useState<ReplayMetadata | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const setSeatTeams = useGameStore((s) => s.setSeatTeams)

  useEffect(() => {
    if (!gameId) return
    let cancelled = false

    async function loadReplay() {
      setLoading(true)
      setError(null)
      try {
        const response = await fetch(`/api/public/replays/${gameId}`)
        if (!response.ok) {
          setError(
            response.status === 404
              ? 'Replay not found. It may have expired or the game ID is invalid.'
              : 'Failed to load replay.',
          )
          setLoading(false)
          return
        }
        const data = await response.json() as PublicReplayData
        if (cancelled) return
        setMetadata(data.metadata)
        setSnapshots(reconstructSnapshots(data.initialSnapshot, data.deltas))
        // Stamp the seat → team map from the replay roster so a team-game replay lights up the
        // team-grouped rail, ally treatment, and team-split layout (team membership only rides
        // in the roster, never the per-frame board state).
        const roster = data.initialSnapshot.players
        if (roster?.some((p) => p.teamIndex != null)) {
          const teams: Record<string, number> = {}
          for (const p of roster) if (p.teamIndex != null) teams[p.playerId] = p.teamIndex
          setSeatTeams(teams, roster.some((p) => p.teamSharedLife))
        }
      } catch {
        if (!cancelled) setError('Failed to load replay.')
      }
      if (!cancelled) setLoading(false)
    }

    loadReplay()
    return () => { cancelled = true }
  }, [gameId, setSeatTeams])

  useEffect(() => {
    return () => { setSeatTeams({}) }
  }, [setSeatTeams])

  const goHome = useCallback(() => { navigate('/') }, [navigate])

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
        <button onClick={goHome} style={styles.backButton}>
          Go to Home
        </button>
      </div>
    )
  }

  return (
    <ReplayPlayer
      snapshots={snapshots}
      gameId={gameId ?? ''}
      metadata={metadata}
      onExit={goHome}
    />
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
}
