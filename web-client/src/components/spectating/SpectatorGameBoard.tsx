import { useGameStore } from '../../store/gameStore'
import { SpectatorContext } from '../../contexts/SpectatorContext'
import { GameBoard } from '../game/GameBoard'

/**
 * Wrapper component that renders GameBoard in spectator mode.
 * Provides spectator context and handles loading/back button.
 */
export function SpectatorGameBoard() {
  const spectatingState = useGameStore((state) => state.spectatingState)
  const stopSpectating = useGameStore((state) => state.stopSpectating)

  if (!spectatingState) return null

  // Show loading state while waiting for gameState
  if (!spectatingState.gameState) {
    return (
      <div style={containerStyle}>
        <div style={{ textAlign: 'center', color: 'white' }}>
          <div style={spinnerStyle} />
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
          <h2 style={{ margin: '16px 0 8px 0', fontSize: 20 }}>
            Connecting to match...
          </h2>
          <p style={{ color: '#666', margin: 0, fontSize: 14 }}>
            {spectatingState.player1Name} vs {spectatingState.player2Name}
          </p>
          <button onClick={stopSpectating} style={backButtonStyle}>
            Back to Overview
          </button>
        </div>
      </div>
    )
  }

  return (
    <SpectatorContext.Provider
      value={{
        isSpectating: true,
        player1Id: spectatingState.player1Id,
        player2Id: spectatingState.player2Id,
        player1Name: spectatingState.player1Name,
        player2Name: spectatingState.player2Name,
      }}
    >
      <div style={containerStyle}>
        {/* Spectator header with back button */}
        <div style={headerStyle}>
          <button onClick={stopSpectating} style={backButtonStyle}>
            Back to Overview
          </button>
          <div style={{ textAlign: 'center', flex: 1 }}>
            <div style={{ color: '#888', fontSize: 12, textTransform: 'uppercase', letterSpacing: '0.1em' }}>
              Spectating
            </div>
            <div style={{ color: '#aaa', fontSize: 14, marginTop: 4 }}>
              {spectatingState.player1Name} vs {spectatingState.player2Name}
            </div>
          </div>
          <div style={{ width: 120 }} />
        </div>

        {/* Main game board */}
        <div style={gameBoardContainerStyle}>
          <GameBoard spectatorMode topOffset={55} />
        </div>
      </div>
    </SpectatorContext.Provider>
  )
}

const containerStyle: React.CSSProperties = {
  position: 'fixed',
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  backgroundColor: '#0a0a12',
  display: 'flex',
  flexDirection: 'column',
  zIndex: 1500,
}

const headerStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  padding: '10px 16px',
  borderBottom: '1px solid #1a1a25',
  backgroundColor: '#0d0d15',
  flexShrink: 0,
  zIndex: 1600,
}

const backButtonStyle: React.CSSProperties = {
  padding: '8px 16px',
  fontSize: 13,
  backgroundColor: 'transparent',
  color: '#888',
  border: '1px solid #333',
  borderRadius: 6,
  cursor: 'pointer',
}

const gameBoardContainerStyle: React.CSSProperties = {
  flex: 1,
  position: 'relative',
  overflow: 'hidden',
}

const spinnerStyle: React.CSSProperties = {
  width: 40,
  height: 40,
  border: '3px solid #333',
  borderTopColor: '#888',
  borderRadius: '50%',
  animation: 'spin 1s linear infinite',
  margin: '0 auto',
}
