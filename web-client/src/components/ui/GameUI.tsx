import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import { useResponsive } from '../../hooks/useResponsive'

/**
 * Connection/lobby UI - shown when not in a game.
 * Combat mode and game UI are handled in App.tsx and GameBoard.tsx.
 */
export function GameUI() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const sessionId = useGameStore((state) => state.sessionId)
  const lastError = useGameStore((state) => state.lastError)

  return (
    <ConnectionOverlay
      status={connectionStatus}
      sessionId={sessionId}
      error={lastError?.message}
    />
  )
}

/**
 * Connection overlay shown before game starts.
 */
function ConnectionOverlay({
  status,
  sessionId,
  error,
}: {
  status: string
  sessionId: string | null
  error: string | undefined
}) {
  const createGame = useGameStore((state) => state.createGame)
  const joinGame = useGameStore((state) => state.joinGame)
  const [joinSessionId, setJoinSessionId] = useState('')
  const responsive = useResponsive()

  // Simple test deck using cards from PortalSet
  const testDeck = {
    'Forest': 24,
    'Grizzly Bears': 16,
    'Monstrous Growth': 10,
    'Gorilla Warrior': 10,
  }

  const handleJoin = () => {
    if (joinSessionId.trim()) {
      joinGame(joinSessionId.trim(), testDeck)
    }
  }

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        gap: responsive.isMobile ? 16 : 24,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
      }}
    >
      <h1 style={{ margin: 0, fontSize: responsive.fontSize.xlarge }}>Argentum Engine</h1>

      <p style={{ color: '#888', fontSize: responsive.fontSize.normal }}>Status: {status}</p>

      {error && (
        <p style={{ color: '#ff4444', fontSize: responsive.fontSize.normal }}>Error: {error}</p>
      )}

      {status === 'connected' && !sessionId && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: responsive.isMobile ? 16 : 24, alignItems: 'center', width: '100%', maxWidth: 400 }}>
          <button
            onClick={() => createGame(testDeck)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 24px',
              fontSize: responsive.fontSize.large,
              backgroundColor: '#0066cc',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
              width: '100%',
              maxWidth: 200,
            }}
          >
            Create Game
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#666' }}>
            <div style={{ width: 40, height: 1, backgroundColor: '#666' }} />
            <span style={{ fontSize: responsive.fontSize.small }}>or</span>
            <div style={{ width: 40, height: 1, backgroundColor: '#666' }} />
          </div>

          <div style={{ display: 'flex', gap: 8, flexDirection: responsive.isMobile ? 'column' : 'row', width: '100%' }}>
            <input
              type="text"
              value={joinSessionId}
              onChange={(e) => setJoinSessionId(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleJoin()}
              placeholder="Enter Session ID"
              style={{
                padding: responsive.isMobile ? '10px 12px' : '12px 16px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: '#222',
                color: 'white',
                border: '1px solid #444',
                borderRadius: 8,
                flex: 1,
                minWidth: 0,
              }}
            />
            <button
              onClick={handleJoin}
              disabled={!joinSessionId.trim()}
              style={{
                padding: responsive.isMobile ? '10px 20px' : '12px 24px',
                fontSize: responsive.fontSize.large,
                backgroundColor: joinSessionId.trim() ? '#00aa44' : '#333',
                color: 'white',
                border: 'none',
                borderRadius: 8,
                cursor: joinSessionId.trim() ? 'pointer' : 'not-allowed',
                whiteSpace: 'nowrap',
              }}
            >
              Join Game
            </button>
          </div>
        </div>
      )}

      {sessionId && (
        <div style={{ textAlign: 'center' }}>
          <p style={{ fontSize: responsive.fontSize.normal }}>Game Created!</p>
          <p style={{ fontSize: responsive.isMobile ? 16 : 24, fontFamily: 'monospace', wordBreak: 'break-all' }}>{sessionId}</p>
          <p style={{ color: '#888', fontSize: responsive.fontSize.normal }}>Waiting for opponent...</p>
        </div>
      )}
    </div>
  )
}
