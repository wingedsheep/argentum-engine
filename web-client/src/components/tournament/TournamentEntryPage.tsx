import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useGameStore } from '../../store/gameStore'

/**
 * Entry point for the /tournament/:lobbyId route.
 *
 * Handles two cases:
 * 1. Player not yet connected: show name entry, connect, then auto-join lobby
 * 2. Player already connected: auto-join lobby immediately
 *
 * Once lobby state is received, navigates to "/" where the normal lobby/tournament UI takes over.
 */
export function TournamentEntryPage() {
  const { lobbyId } = useParams<{ lobbyId: string }>()
  const navigate = useNavigate()

  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const connect = useGameStore((state) => state.connect)
  const joinLobby = useGameStore((state) => state.joinLobby)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const lastError = useGameStore((state) => state.lastError)
  const setPendingTournamentId = useGameStore((state) => state.setPendingTournamentId)

  const [playerName, setPlayerName] = useState(() => localStorage.getItem('argentum-player-name') || '')
  const [joining, setJoining] = useState(false)
  const [tournamentInfo, setTournamentInfo] = useState<{ exists: boolean; state: string; playerCount: number; format: string } | null>(null)
  const [fetchError, setFetchError] = useState<string | null>(null)
  const hasJoinedRef = useRef(false)
  const hasConnectedRef = useRef(false)

  // Fetch tournament info from REST endpoint for display
  useEffect(() => {
    if (!lobbyId) return
    fetch(`/api/tournaments/${lobbyId}/status`)
      .then((res) => {
        if (!res.ok) throw new Error('Tournament not found')
        return res.json()
      })
      .then((data) => setTournamentInfo(data))
      .catch(() => setFetchError('Tournament not found or no longer active.'))
  }, [lobbyId])

  // Auto-connect if we have a stored name (returning user)
  useEffect(() => {
    const storedName = localStorage.getItem('argentum-player-name')
    if (storedName && connectionStatus === 'disconnected' && !hasConnectedRef.current) {
      hasConnectedRef.current = true
      connect(storedName)
    }
  }, [connectionStatus, connect])

  // Auto-join lobby once connected
  useEffect(() => {
    if (connectionStatus === 'connected' && lobbyId && !hasJoinedRef.current) {
      hasJoinedRef.current = true
      setJoining(true)
      joinLobby(lobbyId)
    }
  }, [connectionStatus, lobbyId, joinLobby])

  // Navigate to "/" once lobby or tournament state is received
  useEffect(() => {
    if (lobbyState || tournamentState) {
      navigate('/', { replace: true })
    }
  }, [lobbyState, tournamentState, navigate])

  const handleConnect = () => {
    if (playerName.trim() && lobbyId) {
      localStorage.setItem('argentum-player-name', playerName.trim())
      setPendingTournamentId(lobbyId)
      connect(playerName.trim())
    }
  }

  // Show error if lobby not found via REST or WebSocket error
  const errorMessage = fetchError || (lastError?.message?.toLowerCase().includes('lobby') || lastError?.message?.toLowerCase().includes('not found') ? lastError.message : null)

  return (
    <div style={pageStyles.container}>
      <div style={pageStyles.card}>
        <h1 style={pageStyles.title}>Argentum Engine</h1>

        {tournamentInfo && (
          <div style={pageStyles.info}>
            <div style={pageStyles.infoRow}>
              <span style={pageStyles.infoLabel}>Format</span>
              <span style={pageStyles.infoValue}>{tournamentInfo.format}</span>
            </div>
            <div style={pageStyles.infoRow}>
              <span style={pageStyles.infoLabel}>Status</span>
              <span style={pageStyles.infoValue}>{tournamentInfo.state.replace(/_/g, ' ')}</span>
            </div>
            <div style={pageStyles.infoRow}>
              <span style={pageStyles.infoLabel}>Players</span>
              <span style={pageStyles.infoValue}>{tournamentInfo.playerCount}</span>
            </div>
          </div>
        )}

        {errorMessage && (
          <p style={pageStyles.error}>{errorMessage}</p>
        )}

        {!errorMessage && connectionStatus === 'disconnected' && (
          <div style={pageStyles.form}>
            <label style={pageStyles.label}>Enter your name to join</label>
            <input
              type="text"
              value={playerName}
              onChange={(e) => setPlayerName(e.target.value)}
              onKeyDown={(e) => { if (e.key === 'Enter') handleConnect() }}
              placeholder="Your name"
              autoFocus
              maxLength={20}
              style={pageStyles.input}
            />
            <button
              onClick={handleConnect}
              disabled={!playerName.trim()}
              style={{
                ...pageStyles.button,
                opacity: playerName.trim() ? 1 : 0.5,
              }}
            >
              Join Tournament
            </button>
          </div>
        )}

        {(connectionStatus === 'connecting' || joining) && !errorMessage && (
          <div style={pageStyles.loading}>
            <div style={pageStyles.spinner} />
            <p style={pageStyles.loadingText}>
              {connectionStatus === 'connecting' ? 'Connecting...' : 'Joining tournament...'}
            </p>
            <style>{`@keyframes tournament-spin { to { transform: rotate(360deg); } }`}</style>
          </div>
        )}
      </div>
    </div>
  )
}

const pageStyles: Record<string, React.CSSProperties> = {
  container: {
    width: '100%',
    height: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#0a0a0f',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  },
  card: {
    backgroundColor: '#1a1a2e',
    borderRadius: 12,
    padding: '40px 48px',
    maxWidth: 420,
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 24,
    border: '1px solid #2a2a3e',
  },
  title: {
    margin: 0,
    fontSize: 28,
    color: '#e0e0e0',
    fontWeight: 600,
  },
  info: {
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
    padding: '12px 16px',
    backgroundColor: '#12121e',
    borderRadius: 8,
    border: '1px solid #2a2a3e',
  },
  infoRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  infoLabel: {
    color: '#888',
    fontSize: 13,
  },
  infoValue: {
    color: '#ccc',
    fontSize: 13,
    fontWeight: 500,
    textTransform: 'capitalize' as const,
  },
  error: {
    color: '#ff6b6b',
    fontSize: 14,
    margin: 0,
    textAlign: 'center' as const,
  },
  form: {
    width: '100%',
    display: 'flex',
    flexDirection: 'column',
    gap: 12,
    alignItems: 'center',
  },
  label: {
    color: '#888',
    fontSize: 14,
  },
  input: {
    width: '100%',
    padding: '10px 14px',
    fontSize: 16,
    backgroundColor: '#12121e',
    color: '#e0e0e0',
    border: '1px solid #3a3a4e',
    borderRadius: 6,
    outline: 'none',
    boxSizing: 'border-box' as const,
  },
  button: {
    width: '100%',
    padding: '12px 24px',
    fontSize: 16,
    fontWeight: 600,
    backgroundColor: '#9b59b6',
    color: 'white',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  loading: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 12,
  },
  spinner: {
    width: 32,
    height: 32,
    border: '3px solid #333',
    borderTopColor: '#888',
    borderRadius: '50%',
    animation: 'tournament-spin 1s linear infinite',
  },
  loadingText: {
    color: '#888',
    fontSize: 14,
    margin: 0,
  },
}
