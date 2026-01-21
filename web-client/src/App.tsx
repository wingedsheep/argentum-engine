import { useEffect, useRef } from 'react'
import { GameBoard } from './components/game/GameBoard'
import { GameUI } from './components/ui/GameUI'
import { MulliganUI } from './components/mulligan/MulliganUI'
import { useGameStore } from './store/gameStore'

export default function App() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const gameState = useGameStore((state) => state.gameState)
  const mulliganState = useGameStore((state) => state.mulliganState)
  const connect = useGameStore((state) => state.connect)
  const hasConnectedRef = useRef(false)

  useEffect(() => {
    // Auto-connect on mount (in real app, would have login flow)
    // Use ref to prevent multiple connection attempts from Strict Mode
    if (connectionStatus === 'disconnected' && !hasConnectedRef.current) {
      hasConnectedRef.current = true
      connect('Player')
    }
  }, [connectionStatus, connect])

  // Show connection/game creation UI when not in a game
  const showLobby = connectionStatus !== 'connected' || !gameState

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      {/* Main game board (2D) */}
      {!showLobby && !mulliganState && <GameBoard />}

      {/* Connection/lobby UI overlay */}
      {showLobby && <GameUI />}

      {/* Mulligan overlay */}
      {mulliganState && <MulliganUI />}

      {/* Game over overlay */}
      {!showLobby && !mulliganState && <GameOverlay />}
    </div>
  )
}

/**
 * Game over and error overlays.
 */
function GameOverlay() {
  const gameOverState = useGameStore((state) => state.gameOverState)
  const lastError = useGameStore((state) => state.lastError)
  const clearError = useGameStore((state) => state.clearError)

  if (gameOverState) {
    return (
      <div style={overlayStyles.container}>
        <h1 style={{
          ...overlayStyles.title,
          color: gameOverState.isWinner ? '#00ff00' : '#ff0000',
        }}>
          {gameOverState.isWinner ? 'Victory!' : 'Defeat'}
        </h1>
        <p style={overlayStyles.subtitle}>{gameOverState.reason}</p>
        <button
          onClick={() => window.location.reload()}
          style={overlayStyles.button}
        >
          Play Again
        </button>
      </div>
    )
  }

  if (lastError) {
    return (
      <div style={overlayStyles.errorToast}>
        <span>{lastError.message}</span>
        <button onClick={clearError} style={overlayStyles.closeButton}>
          ×
        </button>
      </div>
    )
  }

  return null
}

const overlayStyles: Record<string, React.CSSProperties> = {
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.85)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 24,
    zIndex: 2000,
  },
  title: {
    margin: 0,
    fontSize: 48,
  },
  subtitle: {
    fontSize: 18,
    color: '#888',
  },
  button: {
    padding: '12px 24px',
    fontSize: 18,
    backgroundColor: '#333',
    color: 'white',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
  errorToast: {
    position: 'absolute',
    top: 16,
    left: '50%',
    transform: 'translateX(-50%)',
    backgroundColor: '#cc0000',
    color: 'white',
    padding: '12px 24px',
    borderRadius: 8,
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    zIndex: 2000,
  },
  closeButton: {
    background: 'none',
    border: 'none',
    color: 'white',
    fontSize: 18,
    cursor: 'pointer',
  },
}
