import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import { useResponsive } from '../../hooks/useResponsive'

type GameMode = 'normal' | 'sealed'

// Available sets for sealed play
const AVAILABLE_SETS = [
  { code: 'POR', name: 'Portal' },
]

/**
 * Connection/lobby UI - shown when not in a game.
 * Combat mode and game UI are handled in App.tsx and GameBoard.tsx.
 */
export function GameUI() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const sessionId = useGameStore((state) => state.sessionId)
  const lastError = useGameStore((state) => state.lastError)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)

  // Don't show connection overlay if actively building deck (but show during 'waiting' phase)
  if (deckBuildingState && deckBuildingState.phase !== 'waiting') return null

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
  const createSealedGame = useGameStore((state) => state.createSealedGame)
  const joinSealedGame = useGameStore((state) => state.joinSealedGame)
  const [joinSessionId, setJoinSessionId] = useState('')
  const [gameMode, setGameMode] = useState<GameMode>('normal')
  const [selectedSet, setSelectedSet] = useState(AVAILABLE_SETS[0]?.code ?? 'POR')
  const responsive = useResponsive()

  // Empty deck triggers server-side random deck generation from Portal set
  const randomDeck = {}

  const handleCreate = () => {
    if (gameMode === 'sealed') {
      createSealedGame(selectedSet)
    } else {
      createGame(randomDeck)
    }
  }

  const handleJoin = () => {
    if (joinSessionId.trim()) {
      if (gameMode === 'sealed') {
        joinSealedGame(joinSessionId.trim())
      } else {
        joinGame(joinSessionId.trim(), randomDeck)
      }
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
          {/* Game Mode Toggle */}
          <div
            style={{
              display: 'flex',
              backgroundColor: '#222',
              borderRadius: 8,
              padding: 4,
              gap: 4,
            }}
          >
            <ModeButton
              label="Normal Game"
              active={gameMode === 'normal'}
              onClick={() => setGameMode('normal')}
              responsive={responsive}
            />
            <ModeButton
              label="Sealed Draft"
              active={gameMode === 'sealed'}
              onClick={() => setGameMode('sealed')}
              responsive={responsive}
            />
          </div>

          {/* Set selector (only shown for sealed mode) */}
          {gameMode === 'sealed' && (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 8,
                width: '100%',
              }}
            >
              <label style={{ color: '#888', fontSize: responsive.fontSize.small }}>
                Select Set:
              </label>
              <select
                value={selectedSet}
                onChange={(e) => setSelectedSet(e.target.value)}
                style={{
                  padding: responsive.isMobile ? '10px 12px' : '12px 16px',
                  fontSize: responsive.fontSize.normal,
                  backgroundColor: '#222',
                  color: 'white',
                  border: '1px solid #444',
                  borderRadius: 8,
                  cursor: 'pointer',
                  width: '100%',
                  maxWidth: 200,
                }}
              >
                {AVAILABLE_SETS.map((set) => (
                  <option key={set.code} value={set.code}>
                    {set.name} ({set.code})
                  </option>
                ))}
              </select>
              <p style={{ color: '#666', fontSize: responsive.fontSize.small, margin: 0, textAlign: 'center' }}>
                Open 6 boosters and build a 40-card deck
              </p>
            </div>
          )}

          <button
            onClick={handleCreate}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 24px',
              fontSize: responsive.fontSize.large,
              backgroundColor: gameMode === 'sealed' ? '#9c27b0' : '#0066cc',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
              width: '100%',
              maxWidth: 200,
            }}
          >
            {gameMode === 'sealed' ? 'Create Sealed' : 'Create Game'}
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#666' }}>
            <div style={{ width: 40, height: 1, backgroundColor: '#666' }} />
            <span style={{ fontSize: responsive.fontSize.small }}>or join existing</span>
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
              Join
            </button>
          </div>

          {gameMode === 'sealed' && (
            <p style={{ color: '#666', fontSize: responsive.fontSize.small, margin: 0, textAlign: 'center' }}>
              When joining, mode is auto-detected from the session
            </p>
          )}
        </div>
      )}

      {sessionId && (
        <WaitingForOpponent sessionId={sessionId} responsive={responsive} />
      )}
    </div>
  )
}

/**
 * Mode toggle button.
 */
function ModeButton({
  label,
  active,
  onClick,
  responsive,
}: {
  label: string
  active: boolean
  onClick: () => void
  responsive: ReturnType<typeof useResponsive>
}) {
  return (
    <button
      onClick={onClick}
      style={{
        padding: responsive.isMobile ? '8px 12px' : '10px 16px',
        fontSize: responsive.fontSize.normal,
        backgroundColor: active ? '#4fc3f7' : 'transparent',
        color: active ? '#000' : '#888',
        border: 'none',
        borderRadius: 6,
        cursor: 'pointer',
        fontWeight: active ? 600 : 400,
        transition: 'all 0.15s',
      }}
    >
      {label}
    </button>
  )
}

/**
 * Waiting for opponent display.
 */
function WaitingForOpponent({
  sessionId,
  responsive,
}: {
  sessionId: string
  responsive: ReturnType<typeof useResponsive>
}) {
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const isSealed = deckBuildingState?.phase === 'waiting'

  return (
    <div style={{ textAlign: 'center' }}>
      <p style={{ fontSize: responsive.fontSize.normal }}>
        {isSealed ? `Sealed Game Created (${deckBuildingState.setName})` : 'Game Created!'}
      </p>
      <p style={{ fontSize: responsive.isMobile ? 16 : 24, fontFamily: 'monospace', wordBreak: 'break-all' }}>
        {sessionId}
      </p>
      <p style={{ color: '#888', fontSize: responsive.fontSize.normal }}>
        Waiting for opponent to join...
      </p>
      {isSealed && (
        <p style={{ color: '#666', fontSize: responsive.fontSize.small, marginTop: 8 }}>
          Once they join, you'll both receive your card pools
        </p>
      )}
    </div>
  )
}
