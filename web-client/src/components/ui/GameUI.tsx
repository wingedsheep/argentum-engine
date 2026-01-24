import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import { PhaseIndicator } from './PhaseIndicator'
import { ActionMenu } from './ActionMenu'
import { TargetingOverlay2D } from '../targeting/TargetingOverlay'
import { useViewingPlayer, useOpponent } from '../../store/selectors'
import type { PassPriorityAction } from '../../types'
import { useResponsive } from '../../hooks/useResponsive'

/**
 * Main UI overlay containing all 2D interface elements.
 */
export function GameUI() {
  // All hooks must be called before any early returns
  const gameState = useGameStore((state) => state.gameState)
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const sessionId = useGameStore((state) => state.sessionId)
  const lastError = useGameStore((state) => state.lastError)
  const gameOverState = useGameStore((state) => state.gameOverState)
  const submitAction = useGameStore((state) => state.submitAction)
  const responsive = useResponsive()

  const viewingPlayer = useViewingPlayer()
  const opponent = useOpponent()

  // Show connection overlay if not in game
  if (connectionStatus !== 'connected' || !gameState) {
    return (
      <ConnectionOverlay
        status={connectionStatus}
        sessionId={sessionId}
        error={lastError?.message}
      />
    )
  }

  // Show game over overlay
  if (gameOverState) {
    return (
      <GameOverOverlay
        isWinner={gameOverState.isWinner}
        reason={gameOverState.reason}
      />
    )
  }

  const hasPriority = gameState.priorityPlayerId === viewingPlayer?.playerId

  const spacing = responsive.isMobile ? 6 : responsive.isTablet ? 10 : 16

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        pointerEvents: 'none',
        fontFamily: 'system-ui, -apple-system, sans-serif',
      }}
    >
      {/* Opponent life - centered at top */}
      {opponent && (
        <div
          style={{
            position: 'absolute',
            top: spacing,
            left: '50%',
            transform: 'translateX(-50%)',
          }}
        >
          <PlayerAvatar
            name={opponent.name}
            life={opponent.life}
            isOpponent={true}
            isMobile={responsive.isMobile}
          />
        </div>
      )}

      {/* Opponent name - top left */}
      {opponent && (
        <div
          style={{
            position: 'absolute',
            top: spacing,
            left: spacing,
            color: '#888',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {opponent.name}
        </div>
      )}

      {/* Phase indicator - top right */}
      <div
        style={{
          position: 'absolute',
          top: spacing,
          right: spacing,
        }}
      >
        <PhaseIndicator
          phase={gameState.currentPhase}
          step={gameState.currentStep}
          turnNumber={gameState.turnNumber}
          isActivePlayer={gameState.activePlayerId === viewingPlayer?.playerId}
          hasPriority={hasPriority}
        />
      </div>

      {/* Player life - centered at bottom */}
      {viewingPlayer && (
        <div
          style={{
            position: 'absolute',
            bottom: responsive.isMobile ? 50 : 80,
            left: '50%',
            transform: 'translateX(-50%)',
          }}
        >
          <PlayerAvatar
            name={viewingPlayer.name}
            life={viewingPlayer.life}
            isOpponent={false}
            isMobile={responsive.isMobile}
          />
        </div>
      )}

      {/* Player name - bottom left */}
      {viewingPlayer && (
        <div
          style={{
            position: 'absolute',
            bottom: spacing,
            left: spacing,
            color: '#aaa',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {viewingPlayer.name}
        </div>
      )}

      {/* Pass button - bottom right */}
      {hasPriority && viewingPlayer && (
        <div
          style={{
            position: 'absolute',
            bottom: spacing,
            right: spacing,
            pointerEvents: 'auto',
          }}
        >
          <button
            onClick={() => {
              const action: PassPriorityAction = {
                type: 'PassPriority',
                playerId: viewingPlayer.playerId,
              }
              submitAction(action)
            }}
            style={{
              padding: responsive.isMobile ? '8px 16px' : '12px 32px',
              fontSize: responsive.fontSize.normal,
              fontWeight: 600,
              backgroundColor: '#e67e22',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              cursor: 'pointer',
              boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
            }}
          >
            Pass
          </button>
        </div>
      )}

      {/* Action menu (centered, when card selected) */}
      <ActionMenu />

      {/* Targeting overlay */}
      <TargetingOverlay2D />

      {/* Error toast */}
      {lastError && <ErrorToast message={lastError.message} />}
    </div>
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

/**
 * Game over overlay.
 */
function GameOverOverlay({
  isWinner,
  reason,
}: {
  isWinner: boolean
  reason: string
}) {
  const responsive = useResponsive()

  return (
    <div
      style={{
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
        color: 'white',
        gap: responsive.isMobile ? 16 : 24,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
      }}
    >
      <h1
        style={{
          margin: 0,
          fontSize: responsive.isMobile ? 32 : 48,
          color: isWinner ? '#00ff00' : '#ff0000',
        }}
      >
        {isWinner ? 'Victory!' : 'Defeat'}
      </h1>

      <p style={{ fontSize: responsive.fontSize.large, color: '#888', textAlign: 'center' }}>{reason}</p>

      <button
        onClick={() => window.location.reload()}
        style={{
          padding: responsive.isMobile ? '10px 20px' : '12px 24px',
          fontSize: responsive.fontSize.large,
          backgroundColor: '#333',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
        }}
      >
        Play Again
      </button>
    </div>
  )
}

/**
 * Player avatar with life total - MTG Arena style.
 */
function PlayerAvatar({
  life,
  isOpponent,
  isMobile = false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  name: _name,
}: {
  name: string
  life: number
  isOpponent: boolean
  isMobile?: boolean
}) {
  const bgColor = isOpponent ? '#2a1a3a' : '#1a2a3a'
  const borderColor = isOpponent ? '#6a3a8a' : '#3a6a8a'
  const size = isMobile ? 40 : 56

  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
      }}
    >
      {/* Avatar circle with life */}
      <div
        style={{
          width: size,
          height: size,
          borderRadius: '50%',
          backgroundColor: bgColor,
          border: `${isMobile ? 2 : 3}px solid ${borderColor}`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 12px rgba(0,0,0,0.5)',
        }}
      >
        <span
          style={{
            fontSize: isMobile ? 16 : 24,
            fontWeight: 700,
            color: life <= 5 ? '#ff4444' : '#ffffff',
          }}
        >
          {life}
        </span>
      </div>
    </div>
  )
}

/**
 * Error toast notification.
 */
function ErrorToast({ message }: { message: string }) {
  const clearError = useGameStore((state) => state.clearError)
  const responsive = useResponsive()

  return (
    <div
      style={{
        position: 'absolute',
        top: responsive.isMobile ? 50 : 80,
        left: '50%',
        transform: 'translateX(-50%)',
        backgroundColor: '#cc0000',
        color: 'white',
        padding: responsive.isMobile ? '8px 16px' : '12px 24px',
        borderRadius: 8,
        display: 'flex',
        alignItems: 'center',
        gap: responsive.isMobile ? 8 : 12,
        pointerEvents: 'auto',
        maxWidth: '90%',
        fontSize: responsive.fontSize.normal,
      }}
    >
      <span style={{ wordBreak: 'break-word' }}>{message}</span>
      <button
        onClick={clearError}
        style={{
          background: 'none',
          border: 'none',
          color: 'white',
          fontSize: responsive.isMobile ? 16 : 18,
          cursor: 'pointer',
          flexShrink: 0,
        }}
      >
        ×
      </button>
    </div>
  )
}
