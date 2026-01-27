import { useEffect, useRef } from 'react'
import { GameBoard } from './components/game/GameBoard'
import { GameUI } from './components/ui/GameUI'
import { MulliganUI } from './components/mulligan/MulliganUI'
import { CombatOverlay } from './components/combat/CombatOverlay'
import { DecisionUI } from './components/decisions/DecisionUI'
import { RevealedHandUI } from './components/decisions/RevealedHandUI'
import { XCostSelector } from './components/ui/XCostSelector'
import { DeckBuilderOverlay } from './components/sealed/DeckBuilderOverlay'
import { useGameStore } from './store/gameStore'
import { useViewingPlayer, useBattlefieldCards } from './store/selectors'
import type { EntityId } from './types'

export default function App() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const gameState = useGameStore((state) => state.gameState)
  const mulliganState = useGameStore((state) => state.mulliganState)
  const legalActions = useGameStore((state) => state.legalActions)
  const combatState = useGameStore((state) => state.combatState)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const startCombat = useGameStore((state) => state.startCombat)
  const connect = useGameStore((state) => state.connect)
  const hasConnectedRef = useRef(false)

  const viewingPlayer = useViewingPlayer()
  const battlefieldCards = useBattlefieldCards()

  // Check for combat actions in legal actions
  const hasDeclareAttackersAction = legalActions.some(
    (a) => a.actionType === 'DeclareAttackers' || a.action.type === 'DeclareAttackers'
  )
  const hasDeclareBlockersAction = legalActions.some(
    (a) => a.actionType === 'DeclareBlockers' || a.action.type === 'DeclareBlockers'
  )

  useEffect(() => {
    // Only auto-connect if we already have a stored player name (returning user)
    // Otherwise, GameUI will show the name entry screen first
    const storedName = sessionStorage.getItem('argentum-player-name')
    if (storedName && connectionStatus === 'disconnected' && !hasConnectedRef.current) {
      hasConnectedRef.current = true
      connect(storedName)
    }
  }, [connectionStatus, connect])

  // Get the clearCombat action
  const clearCombat = useGameStore((state) => state.clearCombat)

  // Auto-enter/exit combat mode based on available actions
  useEffect(() => {
    if (!gameState || !viewingPlayer) return

    // Exit combat mode if the relevant action is no longer available
    if (combatState) {
      if (combatState.mode === 'declareAttackers' && !hasDeclareAttackersAction) {
        // DeclareAttackers was submitted and processed - exit combat mode
        clearCombat()
        return
      }
      if (combatState.mode === 'declareBlockers' && !hasDeclareBlockersAction) {
        // DeclareBlockers was submitted and processed - exit combat mode
        clearCombat()
        return
      }
      // Already in combat mode, don't re-enter
      return
    }

    // Enter combat mode when action becomes available
    if (hasDeclareAttackersAction) {
      // Find the DeclareAttackers action to get valid attackers from server
      const attackersAction = legalActions.find(
        (a) => a.actionType === 'DeclareAttackers' || a.action.type === 'DeclareAttackers'
      )
      // Use server-provided valid attackers (handles haste, defender, etc.)
      const validCreatures: EntityId[] = attackersAction?.validAttackers
        ? [...attackersAction.validAttackers]
        : []

      // Enter combat mode
      startCombat({
        mode: 'declareAttackers',
        selectedAttackers: [],
        blockerAssignments: {},
        validCreatures,
        attackingCreatures: [],
        mustBeBlockedAttackers: [],
      })
      return
    }

    if (hasDeclareBlockersAction) {
      // Find the DeclareBlockers action to get valid blockers from server
      const blockersAction = legalActions.find(
        (a) => a.actionType === 'DeclareBlockers' || a.action.type === 'DeclareBlockers'
      )
      // Use server-provided valid blockers
      const validCreatures: EntityId[] = blockersAction?.validBlockers
        ? [...blockersAction.validBlockers]
        : []

      // Find attacking creatures (opponent's creatures that are attacking)
      const opponentCreatures = battlefieldCards.opponentCreatures
      const attackingCreatures: EntityId[] = opponentCreatures
        .filter((card) => card.isAttacking)
        .map((card) => card.id)

      // Find attackers that must be blocked by all (from combat state in game state)
      const mustBeBlockedAttackers: EntityId[] = gameState?.combat?.attackers
        .filter((a) => a.mustBeBlockedByAll)
        .map((a) => a.creatureId) ?? []

      // Enter combat mode
      startCombat({
        mode: 'declareBlockers',
        selectedAttackers: [],
        blockerAssignments: {},
        validCreatures,
        attackingCreatures,
        mustBeBlockedAttackers,
      })
    }
  }, [hasDeclareAttackersAction, hasDeclareBlockersAction, gameState, viewingPlayer, combatState, startCombat, clearCombat, battlefieldCards, legalActions])

  // Show connection/game creation UI when not in a game
  const showLobby = connectionStatus !== 'connected' || !gameState
  const showGame = !showLobby && !mulliganState
  const showDeckBuilder = deckBuildingState?.phase === 'building' || deckBuildingState?.phase === 'submitted'

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      {/* Main game board (2D) */}
      {showGame && <GameBoard />}

      {/* Connection/lobby UI overlay */}
      {showLobby && <GameUI />}

      {/* Deck building overlay (sealed draft) */}
      {showDeckBuilder && <DeckBuilderOverlay />}

      {/* Mulligan overlay */}
      {mulliganState && <MulliganUI />}

      {/* Combat overlay (when declaring attackers/blockers) */}
      {showGame && combatState && <CombatOverlay />}

      {/* X cost selection overlay (when casting spells with X in cost) */}
      {showGame && <XCostSelector />}

      {/* Decision overlay (for pending decisions like discard to hand size) */}
      {showGame && <DecisionUI />}

      {/* Revealed hand overlay (when looking at opponent's hand) */}
      {showGame && <RevealedHandUI />}

      {/* Game over overlay */}
      {showGame && <GameOverlay />}
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
  const returnToMenu = useGameStore((state) => state.returnToMenu)

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
          onClick={returnToMenu}
          style={overlayStyles.button}
        >
          Return to Menu
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
