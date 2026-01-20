import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import { LifeCounter } from './LifeCounter'
import { PhaseIndicator } from './PhaseIndicator'
import { ActionMenu } from './ActionMenu'
import { TargetingOverlay2D } from '../targeting/TargetingOverlay'
import { useViewingPlayer, useOpponent } from '../../store/selectors'

/**
 * Main UI overlay containing all 2D interface elements.
 */
export function GameUI() {
  const gameState = useGameStore((state) => state.gameState)
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const sessionId = useGameStore((state) => state.sessionId)
  const lastError = useGameStore((state) => state.lastError)
  const gameOverState = useGameStore((state) => state.gameOverState)

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

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        pointerEvents: 'none',
      }}
    >
      {/* Top bar - opponent info and phase */}
      <div
        style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          padding: 16,
        }}
      >
        {/* Opponent info */}
        {opponent && (
          <LifeCounter
            playerId={opponent.playerId}
            name={opponent.name}
            life={opponent.life}
            poisonCounters={opponent.poisonCounters}
            isOpponent
          />
        )}

        {/* Phase indicator */}
        <PhaseIndicator
          phase={gameState.currentPhase}
          step={gameState.currentStep}
          turnNumber={gameState.turnNumber}
          isActivePlayer={gameState.activePlayerId === viewingPlayer?.playerId}
          hasPriority={gameState.priorityPlayerId === viewingPlayer?.playerId}
        />
      </div>

      {/* Bottom bar - player info and actions */}
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-end',
          padding: 16,
        }}
      >
        {/* Player info */}
        {viewingPlayer && (
          <LifeCounter
            playerId={viewingPlayer.playerId}
            name={viewingPlayer.name}
            life={viewingPlayer.life}
            poisonCounters={viewingPlayer.poisonCounters}
            isOpponent={false}
          />
        )}

        {/* Mana pool - currently not sent by server */}
        {/* TODO: Add mana pool display when server sends MaskedPlayer.manaPool */}
      </div>

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
        gap: 24,
        pointerEvents: 'auto',
      }}
    >
      <h1 style={{ margin: 0, fontSize: 36 }}>Argentum Engine</h1>

      <p style={{ color: '#888' }}>Status: {status}</p>

      {error && (
        <p style={{ color: '#ff4444' }}>Error: {error}</p>
      )}

      {status === 'connected' && !sessionId && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24, alignItems: 'center' }}>
          <button
            onClick={() => createGame(testDeck)}
            style={{
              padding: '12px 24px',
              fontSize: 18,
              backgroundColor: '#0066cc',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            Create Game
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#666' }}>
            <div style={{ width: 60, height: 1, backgroundColor: '#666' }} />
            <span>or</span>
            <div style={{ width: 60, height: 1, backgroundColor: '#666' }} />
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <input
              type="text"
              value={joinSessionId}
              onChange={(e) => setJoinSessionId(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleJoin()}
              placeholder="Enter Session ID"
              style={{
                padding: '12px 16px',
                fontSize: 16,
                backgroundColor: '#222',
                color: 'white',
                border: '1px solid #444',
                borderRadius: 8,
                width: 200,
              }}
            />
            <button
              onClick={handleJoin}
              disabled={!joinSessionId.trim()}
              style={{
                padding: '12px 24px',
                fontSize: 18,
                backgroundColor: joinSessionId.trim() ? '#00aa44' : '#333',
                color: 'white',
                border: 'none',
                borderRadius: 8,
                cursor: joinSessionId.trim() ? 'pointer' : 'not-allowed',
              }}
            >
              Join Game
            </button>
          </div>
        </div>
      )}

      {sessionId && (
        <div style={{ textAlign: 'center' }}>
          <p>Game Created!</p>
          <p style={{ fontSize: 24, fontFamily: 'monospace' }}>{sessionId}</p>
          <p style={{ color: '#888' }}>Waiting for opponent...</p>
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
        gap: 24,
        pointerEvents: 'auto',
      }}
    >
      <h1
        style={{
          margin: 0,
          fontSize: 48,
          color: isWinner ? '#00ff00' : '#ff0000',
        }}
      >
        {isWinner ? 'Victory!' : 'Defeat'}
      </h1>

      <p style={{ fontSize: 18, color: '#888' }}>{reason}</p>

      <button
        onClick={() => window.location.reload()}
        style={{
          padding: '12px 24px',
          fontSize: 18,
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
 * Error toast notification.
 */
function ErrorToast({ message }: { message: string }) {
  const clearError = useGameStore((state) => state.clearError)

  return (
    <div
      style={{
        position: 'absolute',
        top: 80,
        left: '50%',
        transform: 'translateX(-50%)',
        backgroundColor: '#cc0000',
        color: 'white',
        padding: '12px 24px',
        borderRadius: 8,
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        pointerEvents: 'auto',
      }}
    >
      <span>{message}</span>
      <button
        onClick={clearError}
        style={{
          background: 'none',
          border: 'none',
          color: 'white',
          fontSize: 18,
          cursor: 'pointer',
        }}
      >
        ×
      </button>
    </div>
  )
}
