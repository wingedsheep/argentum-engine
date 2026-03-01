import { useMemo } from 'react'
import { useGameStore } from '../../store/gameStore'
import { useViewingPlayer, useOpponent, useStackCards, selectPriorityMode, useGhostCards, useRevealedLibraryTopCard } from '../../store/selectors'
import { hand, getNextStep, StepShortNames } from '../../types'
import { StepStrip } from '../ui/StepStrip'
import { ManaPool } from '../ui/ManaPool'
import { ActionMenu } from '../ui/ActionMenu'
import { CombatArrows } from '../combat/CombatArrows'
import { TargetingArrows } from '../targeting/TargetingArrows'
import { DraggedCardOverlay } from './DraggedCardOverlay'
import { GameLog } from './GameLog'
import { DrawAnimations } from '../animations/DrawAnimations'
import { DamageAnimations } from '../animations/DamageAnimations'
import { RevealAnimations } from '../animations/RevealAnimations'
import { CoinFlipAnimations } from '../animations/CoinFlipAnimations'
import { useResponsive } from '../../hooks/useResponsive'

// Import extracted components
import { Battlefield, CardRow, StackDisplay, ZonePile, ResponsiveContext } from './board'
import { CardPreview } from './card'
import { TargetingOverlay, ManaColorSelectionOverlay, LifeDisplay, ActiveEffectsBadges, ConcedeButton, FullscreenButton } from './overlay'
import { styles } from './board/styles'

/**
 * Props for GameBoard component.
 */
interface GameBoardProps {
  /** When true, disables all interactions (for spectator view) */
  spectatorMode?: boolean
  /** Top offset in pixels for fixed elements (to account for headers) */
  topOffset?: number
}

/**
 * 2D Game board layout - MTG Arena style.
 * This is the main orchestrator component that composes all game UI elements.
 */
export function GameBoard({ spectatorMode = false, topOffset = 0 }: GameBoardProps) {
  const playerGameState = useGameStore((state) => state.gameState)
  const spectatingState = useGameStore((state) => state.spectatingState)
  const playerId = useGameStore((state) => state.playerId)
  const submitAction = useGameStore((state) => state.submitAction)
  const combatState = useGameStore((state) => state.combatState)
  const confirmCombat = useGameStore((state) => state.confirmCombat)
  const clearAttackers = useGameStore((state) => state.clearAttackers)
  const clearBlockerAssignments = useGameStore((state) => state.clearBlockerAssignments)
  const attackWithAll = useGameStore((state) => state.attackWithAll)
  const priorityMode = useGameStore(selectPriorityMode)
  const nextStopPoint = useGameStore((state) => state.nextStopPoint)
  const serverPriorityMode = useGameStore((state) => state.priorityMode)
  const cyclePriorityMode = useGameStore((state) => state.cyclePriorityMode)
  const opponentDecisionStatus = useGameStore((state) => state.opponentDecisionStatus)
  const stopOverrides = useGameStore((state) => state.stopOverrides)
  const toggleStopOverride = useGameStore((state) => state.toggleStopOverride)
  const distributeState = useGameStore((state) => state.distributeState)
  const confirmDistribute = useGameStore((state) => state.confirmDistribute)
  const undoAvailable = useGameStore((state) => state.undoAvailable)
  const requestUndo = useGameStore((state) => state.requestUndo)
  const responsive = useResponsive(topOffset)

  // In spectator mode, use spectatingState.gameState
  const gameState = spectatorMode ? spectatingState?.gameState : playerGameState

  const viewingPlayer = useViewingPlayer()
  const opponent = useOpponent()
  const stackCards = useStackCards()
  const ghostCards = useGhostCards(playerId ?? null)
  const opponentRevealedTopCard = useRevealedLibraryTopCard(opponent?.playerId ?? null)
  const opponentGhostCards = useMemo(
    () => opponentRevealedTopCard ? [opponentRevealedTopCard] : [],
    [opponentRevealedTopCard]
  )

  // For spectator mode, we need to find players differently since playerId won't match
  const spectatorPlayer1 = spectatorMode && gameState
    ? gameState.players.find(p => p.playerId === spectatingState?.player1Id) ?? gameState.players[0]
    : null
  const spectatorPlayer2 = spectatorMode && gameState
    ? gameState.players.find(p => p.playerId === spectatingState?.player2Id) ?? gameState.players[1]
    : null

  // In spectator mode, use player1 as "bottom" player and player2 as "top" (opponent)
  const effectiveViewingPlayer = spectatorMode ? spectatorPlayer1 : viewingPlayer
  const effectiveOpponent = spectatorMode ? spectatorPlayer2 : opponent

  if (!gameState || (!spectatorMode && (!playerId || !viewingPlayer))) {
    return null
  }

  // In spectator mode: disable all interaction
  const hasPriority = spectatorMode ? false : (gameState.priorityPlayerId === viewingPlayer?.playerId)
  const canAct = hasPriority && !opponentDecisionStatus
  const isMyTurn = spectatorMode ? false : (gameState.activePlayerId === viewingPlayer?.playerId)
  const isInCombatMode = spectatorMode ? false : (combatState !== null)
  const isInDistributeMode = !spectatorMode && distributeState !== null
  const distributeTotalAllocated = distributeState
    ? Object.values(distributeState.distribution).reduce((sum, v) => sum + v, 0)
    : 0
  const distributeRemaining = distributeState ? distributeState.totalAmount - distributeTotalAllocated : 0

  // Compute pass button label - prefer server-computed nextStopPoint, fall back to naive logic
  const getPassButtonLabel = () => {
    if (nextStopPoint) {
      return nextStopPoint
    }
    // Fallback for full control mode (server sends null)
    if (stackCards.length > 0) {
      return 'Resolve'
    }
    const nextStep = getNextStep(gameState.currentStep)
    if (nextStep) {
      if (nextStep === 'END' && isMyTurn) {
        return 'End Turn'
      }
      return `Pass to ${StepShortNames[nextStep]}`
    }
    return 'Pass'
  }

  // Get pass button colors based on priority mode
  const getPassButtonStyle = (): React.CSSProperties => {
    const hasStack = stackCards.length > 0
    if (hasStack) {
      // Resolve - keep orange
      return {
        backgroundColor: '#c76e00',
        borderColor: '#e08000',
      }
    }
    if (priorityMode === 'ownTurn') {
      // Own turn - blue/cyan
      return {
        backgroundColor: '#1976d2',
        borderColor: '#4fc3f7',
      }
    }
    // Responding - amber/gold
    return {
      backgroundColor: '#f57c00',
      borderColor: '#ffc107',
    }
  }

  return (
    <ResponsiveContext.Provider value={responsive}>
    <div style={{
      ...styles.container,
      padding: `0 ${responsive.containerPadding}px`,
      gap: responsive.sectionGap,
    }}>
      {/* Fullscreen button (top-left) */}
      <FullscreenButton />

      {/* Concede button (top-right) - hidden in spectator mode */}
      {!spectatorMode && <ConcedeButton />}

      {/* Opponent hand - fixed at top of screen */}
      {effectiveOpponent && (
        <div
          data-zone="opponent-hand"
          style={{
            position: 'fixed',
            top: topOffset,
            left: '50%',
            transform: 'translateX(-50%)',
            zIndex: 50,
          }}
        >
          <CardRow
            zoneId={hand(effectiveOpponent.playerId)}
            faceDown
            small
            inverted
            ghostCards={opponentGhostCards}
          />
        </div>
      )}

      {/* Spectator mode: floating player name labels on the left side */}
      {spectatorMode && effectiveOpponent && (
        <div
          style={{
            ...styles.spectatorNameLabel,
            position: 'fixed',
            top: topOffset + responsive.smallCardHeight + responsive.handBattlefieldGap + 8,
            left: 16,
          }}
        >
          {effectiveOpponent.name}
        </div>
      )}

      {/* Opponent area (top) */}
      <div style={{
        ...styles.opponentArea,
        marginTop: -responsive.containerPadding + responsive.sectionGap,
        paddingTop: responsive.smallCardHeight + topOffset + responsive.handBattlefieldGap,
      }}>
        <div style={styles.playerRowWithZones}>
          <div style={styles.playerMainArea}>
            {/* Opponent battlefield - lands first (closer to opponent), then creatures */}
            <Battlefield isOpponent spectatorMode={spectatorMode} />
          </div>

          {/* Opponent deck/graveyard (right side) */}
          {effectiveOpponent && <ZonePile player={effectiveOpponent} isOpponent />}
        </div>
      </div>

      {/* Center - Life totals, phase indicator, and stack */}
      <div style={{
        ...styles.centerArea,
        gap: responsive.isMobile ? 6 : 16,
      }}>
        {/* Opponent life (left side) */}
        <div style={styles.centerLifeSection}>
          {effectiveOpponent && (
            <>
              <LifeDisplay life={effectiveOpponent.life} playerId={effectiveOpponent.playerId} playerName={effectiveOpponent.name} spectatorMode={spectatorMode} />
              {!responsive.isMobile && <span style={{ ...styles.playerName, fontSize: responsive.fontSize.small }}>{effectiveOpponent.name}</span>}
              {!responsive.isMobile && <ActiveEffectsBadges effects={effectiveOpponent.activeEffects} />}
              {!responsive.isMobile && effectiveOpponent.manaPool && <ManaPool manaPool={effectiveOpponent.manaPool} />}
            </>
          )}
        </div>

        {/* Step strip (center) */}
        <StepStrip
          phase={gameState.currentPhase}
          step={gameState.currentStep}
          turnNumber={gameState.turnNumber}
          isActivePlayer={isMyTurn}
          hasPriority={hasPriority}
          priorityMode={priorityMode}
          activePlayerName={spectatorMode
            ? gameState.players.find(p => p.playerId === gameState.activePlayerId)?.name
            : undefined
          }
          stopOverrides={stopOverrides}
          onToggleStop={toggleStopOverride}
          isSpectator={spectatorMode}
        />

        {/* Player life (right side) */}
        <div style={styles.centerLifeSection}>
          {effectiveViewingPlayer && (
            <>
              <LifeDisplay life={effectiveViewingPlayer.life} isPlayer playerId={effectiveViewingPlayer.playerId} playerName={effectiveViewingPlayer.name} spectatorMode={spectatorMode} />
              {!responsive.isMobile && <span style={{ ...styles.playerName, fontSize: responsive.fontSize.small }}>{effectiveViewingPlayer.name}</span>}
              {!responsive.isMobile && <ActiveEffectsBadges effects={effectiveViewingPlayer.activeEffects} />}
              {!responsive.isMobile && effectiveViewingPlayer.manaPool && <ManaPool manaPool={effectiveViewingPlayer.manaPool} />}
            </>
          )}
        </div>
      </div>

      {/* Stack display - floating on the left side */}
      <StackDisplay />


      {/* Player area (bottom) */}
      <div style={{
        ...styles.playerArea,
        marginBottom: -responsive.containerPadding + responsive.sectionGap,
        paddingBottom: (spectatorMode ? responsive.smallCardHeight : responsive.cardHeight) + responsive.handBattlefieldGap,
      }}>
        <div style={styles.playerRowWithZones}>
          <div style={styles.playerMainArea}>
            {/* Player battlefield - creatures first (closer to center), then lands */}
            <Battlefield isOpponent={false} spectatorMode={spectatorMode} />

          </div>

          {/* Player deck/graveyard (right side) */}
          {effectiveViewingPlayer && <ZonePile player={effectiveViewingPlayer} />}
        </div>
      </div>

      {/* Spectator mode: floating player name label for bottom player */}
      {spectatorMode && effectiveViewingPlayer && (
        <div
          style={{
            ...styles.spectatorNameLabel,
            position: 'fixed',
            bottom: responsive.smallCardHeight + responsive.handBattlefieldGap + 8,
            left: 16,
          }}
        >
          {effectiveViewingPlayer.name}
        </div>
      )}

      {/* Player hand - fixed at bottom of screen */}
      <div
        data-zone="hand"
        style={{
          position: 'fixed',
          bottom: 0,
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 50,
        }}
      >
        {spectatorMode && effectiveViewingPlayer ? (
          <CardRow
            zoneId={hand(effectiveViewingPlayer.playerId)}
            faceDown
            small
          />
        ) : playerId ? (
          <CardRow
            zoneId={hand(playerId)}
            faceDown={false}
            interactive
            ghostCards={ghostCards}
          />
        ) : null}
      </div>

      {/* Floating pass/undo buttons (bottom-right) - hidden in spectator mode and distribute mode */}
      {!spectatorMode && canAct && !isInCombatMode && !isInDistributeMode && viewingPlayer && (
        <div style={{
          position: 'fixed',
          bottom: 16,
          right: 16,
          display: 'flex',
          gap: 8,
          zIndex: 100,
        }}>
          {undoAvailable && (
            <button
              onClick={requestUndo}
              style={{
                ...styles.floatingUndoButton,
                position: 'static',
                padding: responsive.isMobile ? '10px 16px' : '12px 20px',
                fontSize: responsive.fontSize.normal,
              }}
            >
              Undo
            </button>
          )}
          <button
            onClick={() => {
              submitAction({
                type: 'PassPriority',
                playerId: viewingPlayer.playerId,
              })
            }}
            style={{
              ...styles.floatingPassButton,
              ...getPassButtonStyle(),
              position: 'static',
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              border: `2px solid ${getPassButtonStyle().borderColor}`,
              transition: 'background-color 0.2s, border-color 0.2s',
            }}
          >
            {getPassButtonLabel()}
          </button>
        </div>
      )}

      {/* Priority mode toggle button (bottom-right, above pass button) - hidden in spectator mode */}
      {!spectatorMode && viewingPlayer && (
        <button
          onClick={cyclePriorityMode}
          title={
            serverPriorityMode === 'fullControl'
              ? 'Full Control: You receive priority at every step. Click to switch to Auto.'
              : serverPriorityMode === 'stops'
              ? 'Stops: Pauses on opponent spells/abilities and combat damage. Click to switch to Full Control.'
              : 'Auto: Smart auto-passing. Click to switch to Stops.'
          }
          style={{
            position: 'fixed',
            bottom: responsive.isMobile ? 60 : 70,
            right: 16,
            padding: responsive.isMobile ? '4px 10px' : '6px 12px',
            fontSize: responsive.fontSize.small,
            fontWeight: 500,
            backgroundColor:
              serverPriorityMode === 'fullControl' ? 'rgba(79, 195, 247, 0.9)' :
              serverPriorityMode === 'stops' ? 'rgba(245, 158, 11, 0.9)' :
              'rgba(40, 40, 40, 0.8)',
            color:
              serverPriorityMode === 'fullControl' ? '#000' :
              serverPriorityMode === 'stops' ? '#000' :
              '#999',
            border:
              serverPriorityMode === 'fullControl' ? '1px solid #4fc3f7' :
              serverPriorityMode === 'stops' ? '1px solid #f59e0b' :
              '1px solid #555',
            borderRadius: 4,
            cursor: 'pointer',
            transition: 'all 0.2s',
            zIndex: 100,
          }}
        >
          {serverPriorityMode === 'fullControl' ? 'Full Control' :
           serverPriorityMode === 'stops' ? 'Stops' :
           'Auto'}
        </button>
      )}

      {/* Combat buttons (bottom-right) */}
      {isInCombatMode && combatState?.mode === 'declareAttackers' && (
        <div style={styles.combatButtonContainer}>
          {combatState.selectedAttackers.length === 0 ? (
            <>
              <button
                onClick={attackWithAll}
                disabled={combatState.validCreatures.length === 0}
                style={{
                  ...styles.combatButton,
                  ...styles.combatButtonPrimary,
                  opacity: combatState.validCreatures.length === 0 ? 0.5 : 1,
                }}
              >
                Attack All
              </button>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.combatButton,
                  ...styles.combatButtonSecondary,
                }}
              >
                Skip Attacking
              </button>
            </>
          ) : (
            <>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.combatButton,
                  ...styles.combatButtonPrimary,
                }}
              >
                Attack with {combatState.selectedAttackers.length}
              </button>
              <button
                onClick={clearAttackers}
                style={{
                  ...styles.combatButton,
                  ...styles.combatButtonSecondary,
                }}
              >
                Clear Attackers
              </button>
            </>
          )}
        </div>
      )}

      {isInCombatMode && combatState?.mode === 'declareBlockers' && (
        <div style={styles.combatButtonContainer}>
          {Object.keys(combatState.blockerAssignments).length === 0 ? (
            <button
              onClick={confirmCombat}
              style={{
                ...styles.combatButton,
                ...styles.combatButtonSecondary,
              }}
            >
              No Blocks
            </button>
          ) : (
            <>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.combatButton,
                  ...styles.combatButtonPrimary,
                }}
              >
                Confirm Blocks
              </button>
              <button
                onClick={clearBlockerAssignments}
                style={{
                  ...styles.combatButton,
                  ...styles.combatButtonSecondary,
                }}
              >
                Clear Blockers
              </button>
            </>
          )}
        </div>
      )}


      {/* Floating distribute bar (bottom-right) */}
      {isInDistributeMode && distributeState && (
        <div style={{
          ...styles.combatButtonContainer,
          flexDirection: 'column',
          gap: 8,
          alignItems: 'flex-end',
        }}>
          {(() => {
            const isPartial = distributeState.allowPartial === true
            const isPrevention = distributeState.prompt.toLowerCase().includes('prevention')
            const isCounters = distributeState.prompt.toLowerCase().includes('counter')
            const noun = isCounters ? 'counters' : isPrevention ? 'prevention' : 'damage'
            const confirmLabel = isCounters ? 'Confirm' : isPrevention ? 'Confirm Prevention' : 'Confirm Damage'
            const canConfirm = isPartial ? true : distributeRemaining === 0
            const isComplete = distributeRemaining === 0
            return (
              <>
                <div style={{
                  backgroundColor: isComplete ? 'rgba(22, 163, 74, 0.9)' : isPartial ? 'rgba(59, 130, 246, 0.9)' : 'rgba(220, 38, 38, 0.9)',
                  padding: responsive.isMobile ? '6px 12px' : '8px 16px',
                  borderRadius: 6,
                  border: isComplete ? '1px solid #4ade80' : isPartial ? '1px solid #60a5fa' : '1px solid #f87171',
                  textAlign: 'center',
                }}>
                  <div style={{
                    color: 'white',
                    fontSize: responsive.fontSize.small,
                    fontWeight: 600,
                  }}>
                    {isComplete
                      ? `All ${noun} allocated`
                      : `${distributeRemaining} ${noun} remaining`}
                  </div>
                  <div style={{
                    color: 'rgba(255, 255, 255, 0.7)',
                    fontSize: responsive.isMobile ? 10 : 11,
                    marginTop: 2,
                  }}>
                    {distributeState.prompt}
                  </div>
                </div>
                <button
                  onClick={confirmDistribute}
                  disabled={!canConfirm}
                  style={{
                    ...styles.combatButton,
                    ...(canConfirm ? styles.combatButtonPrimary : {}),
                    backgroundColor: canConfirm ? '#16a34a' : '#333',
                    color: canConfirm ? 'white' : '#666',
                    cursor: canConfirm ? 'pointer' : 'not-allowed',
                    borderColor: canConfirm ? '#4ade80' : '#555',
                  }}
                >
                  {confirmLabel}
                </button>
              </>
            )
          })()}
        </div>
      )}

      {/* Action menu for selected card - hidden in spectator mode */}
      {!spectatorMode && <ActionMenu />}

      {/* Targeting overlay for spell/ability target selection */}
      {!spectatorMode && <TargetingOverlay />}

      {/* Mana color selection overlay */}
      {!spectatorMode && <ManaColorSelectionOverlay />}

      {/* Combat arrows for blocker assignments - rendered by SpectatorGameBoard in spectator mode to avoid stacking context issues */}
      {!spectatorMode && <CombatArrows />}

      {/* Targeting arrows for spells on the stack */}
      <TargetingArrows />

      {/* Dragged card overlay - hidden in spectator mode */}
      {!spectatorMode && <DraggedCardOverlay />}
      <CardPreview />
      {!spectatorMode && <GameLog />}

      {/* Draw animations */}
      <DrawAnimations />

      {/* Damage animations */}
      <DamageAnimations />

      {/* Morph reveal animations */}
      <RevealAnimations />

      {/* Coin flip animations */}
      <CoinFlipAnimations />
    </div>
    </ResponsiveContext.Provider>
  )
}
