import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore.ts'

/**
 * Entry point for the `/join/:lobbyId` deep link — the target of a lobby's QR code / share link.
 *
 * Lobby-kind agnostic: it always joins via `joinQuickGameLobby`, whose server handler delegates to
 * the tournament join handler when the id is a tournament lobby. So one link/QR covers Quick Game,
 * sealed/draft and tournament lobbies alike.
 *
 * Two cases, mirroring {@link TournamentEntryPage}:
 * 1. Not connected: show a name entry, connect, then auto-join once connected.
 * 2. Already connected (returning user with a stored name): auto-join immediately.
 *
 * Once any lobby state arrives, navigate to "/" where the normal lobby UI takes over.
 */
export function JoinLobbyPage() {
  const { lobbyId } = useParams<{ lobbyId: string }>()
  const navigate = useNavigate()

  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const connect = useGameStore((state) => state.connect)
  const joinAnyLobby = useGameStore((state) => state.joinQuickGameLobby)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const quickGameLobbyState = useGameStore((state) => state.quickGameLobbyState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const lastError = useGameStore((state) => state.lastError)
  const sessionReplaced = useGameStore((state) => state.sessionReplaced)

  const [playerName, setPlayerName] = useState(() => localStorage.getItem('argentum-player-name') || '')
  const [joining, setJoining] = useState(false)
  const hasJoinedRef = useRef(false)
  const hasConnectedRef = useRef(false)

  // Auto-connect a returning user (stored name). Never while another tab/device owns the session —
  // reconnecting would steal it back (mirrors App.tsx / TournamentEntryPage).
  useEffect(() => {
    if (sessionReplaced) return
    const storedName = localStorage.getItem('argentum-player-name')
    if (storedName && connectionStatus === 'disconnected' && !hasConnectedRef.current) {
      hasConnectedRef.current = true
      connect(storedName)
    }
  }, [connectionStatus, connect, sessionReplaced])

  // Auto-join once connected (fires for both the returning user and the fresh name-entry path).
  useEffect(() => {
    if (connectionStatus === 'connected' && lobbyId && !hasJoinedRef.current) {
      hasJoinedRef.current = true
      setJoining(true)
      joinAnyLobby(lobbyId)
    }
  }, [connectionStatus, lobbyId, joinAnyLobby])

  // Navigate home once any lobby/tournament state is received.
  useEffect(() => {
    if (lobbyState || quickGameLobbyState || tournamentState) {
      navigate('/', { replace: true })
    }
  }, [lobbyState, quickGameLobbyState, tournamentState, navigate])

  const handleConnect = () => {
    if (playerName.trim()) {
      localStorage.setItem('argentum-player-name', playerName.trim())
      connect(playerName.trim())
    }
  }

  const errorMessage = lastError?.message?.toLowerCase().includes('lobby')
    || lastError?.message?.toLowerCase().includes('not found')
    ? lastError?.message
    : null

  return (
    <div style={pageStyles.container}>
      <div style={pageStyles.card}>
        <h1 style={pageStyles.title}>Argentum Engine</h1>

        {errorMessage && <p style={pageStyles.error}>{errorMessage}</p>}

        {!errorMessage && connectionStatus === 'disconnected' && (
          <div style={pageStyles.form}>
            <label style={pageStyles.label}>Enter your name to join the lobby</label>
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
              style={{ ...pageStyles.button, opacity: playerName.trim() ? 1 : 0.5 }}
            >
              Join Lobby
            </button>
          </div>
        )}

        {(connectionStatus === 'connecting' || joining) && !errorMessage && (
          <div style={pageStyles.loading}>
            <div style={pageStyles.spinner} />
            <p style={pageStyles.loadingText}>
              {connectionStatus === 'connecting' ? 'Connecting...' : 'Joining lobby...'}
            </p>
            <style>{`@keyframes join-spin { to { transform: rotate(360deg); } }`}</style>
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
  title: { margin: 0, fontSize: 28, color: '#e0e0e0', fontWeight: 600 },
  error: { color: '#ff6b6b', fontSize: 14, margin: 0, textAlign: 'center' },
  form: { width: '100%', display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'center' },
  label: { color: '#888', fontSize: 14 },
  input: {
    width: '100%',
    padding: '10px 14px',
    fontSize: 16,
    backgroundColor: '#12121e',
    color: '#e0e0e0',
    border: '1px solid #3a3a4e',
    borderRadius: 6,
    outline: 'none',
    boxSizing: 'border-box',
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
  loading: { display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 },
  spinner: {
    width: 32,
    height: 32,
    border: '3px solid #333',
    borderTopColor: '#888',
    borderRadius: '50%',
    animation: 'join-spin 1s linear infinite',
  },
  loadingText: { color: '#888', fontSize: 14, margin: 0 },
}
