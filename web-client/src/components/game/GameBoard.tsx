import { useMemo, useCallback } from 'react'
import { useGameStore } from '@/store/gameStore'
import { useInteraction } from '@/hooks/useInteraction'
import { useViewingPlayer, useOpponent, useStackCards, selectPriorityMode, useGhostCards, useRevealedLibraryTopCard, useBattlefieldCards } from '@/store/selectors'
import { hand, getNextStep, StepShortNames } from '@/types'
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
import { TargetReselectedAnimations } from '../animations/TargetReselectedAnimations'
import { useResponsive } from '@/hooks/useResponsive'
import { ManaSymbol } from '../ui/ManaSymbols'

// Import extracted components
import { Battlefield, CardRow, StackDisplay, ZonePile, ResponsiveContext } from './board'
import { RenderProfiler } from '@/utils/renderProfiler'
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
  const targetingState = useGameStore((state) => state.targetingState)
  const distributeState = useGameStore((state) => state.distributeState)
  const confirmDistribute = useGameStore((state) => state.confirmDistribute)
  const counterDistributionState = useGameStore((state) => state.counterDistributionState)
  const confirmCounterDistribution = useGameStore((state) => state.confirmCounterDistribution)
  const cancelCounterDistribution = useGameStore((state) => state.cancelCounterDistribution)
  const undoAvailable = useGameStore((state) => state.undoAvailable)
  const requestUndo = useGameStore((state) => state.requestUndo)
  const autoTapEnabled = useGameStore((state) => state.autoTapEnabled)
  const toggleAutoTap = useGameStore((state) => state.toggleAutoTap)
  const delveSelectionState = useGameStore((state) => state.delveSelectionState)
  const crewSelectionState = useGameStore((state) => state.crewSelectionState)
  const manaSelectionState = useGameStore((state) => state.manaSelectionState)
  const cancelManaSelection = useGameStore((state) => state.cancelManaSelection)
  const { executeAction } = useInteraction()

  // Card counts per battlefield zone — used by useResponsive to decide when wrapping
  // will occur and shrink cards so the wrapped rows fit vertically.
  const battlefieldCards = useBattlefieldCards()
  const zoneRowCounts = useMemo(
    () => [
      battlefieldCards.playerCreatures.length + battlefieldCards.playerPlaneswalkers.length,
      battlefieldCards.playerLands.length + battlefieldCards.playerOther.length,
      battlefieldCards.opponentCreatures.length + battlefieldCards.opponentPlaneswalkers.length,
      battlefieldCards.opponentLands.length + battlefieldCards.opponentOther.length,
    ],
    [battlefieldCards]
  )

  const responsive = useResponsive(topOffset, zoneRowCounts)

  // Confirm mana selection: if pipeline is active, advance it; otherwise build
  // modified LegalActionInfo with Explicit payment and route through executeAction
  const handleConfirmManaSelection = useCallback(() => {
    if (!manaSelectionState) return
    const { pipelineState, advancePipeline } = useGameStore.getState()
    if (pipelineState) {
      // Pipeline path: clear mana UI state directly (not via cancelManaSelection
      // which would cancel the entire pipeline) and advance
      useGameStore.setState({ manaSelectionState: null })
      advancePipeline({
        type: 'manaSource',
        selectedSources: [...manaSelectionState.selectedSources],
      })
      return
    }

    // Direct mana-button path: build Explicit payment and enter pipeline for remaining phases
    const paymentStrategy = {
      type: 'Explicit' as const,
      manaAbilitiesToActivate: [...manaSelectionState.selectedSources],
    }
    // Cast to add paymentStrategy - only actions with mana costs reach here
    const modifiedAction = { ...manaSelectionState.action, paymentStrategy } as import('../../types').GameAction
    // Strip mana-source fields so executeAction doesn't loop back here
    const { availableManaSources: _, autoTapPreview: _2, ...restActionInfo } = manaSelectionState.actionInfo
    const modifiedActionInfo: import('../../types').LegalActionInfo = {
      ...restActionInfo,
      action: modifiedAction,
    }
    cancelManaSelection()
    executeAction(modifiedActionInfo)
  }, [manaSelectionState, cancelManaSelection, executeAction])

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
  const isInCounterDistMode = !spectatorMode && counterDistributionState !== null
  const isInManaSelectionMode = !spectatorMode && manaSelectionState !== null

  // Compute mana selection progress using most-constrained-first matching
  const manaProgress = useMemo(() => {
    if (!manaSelectionState) return null

    // Parse mana cost into requirements
    const symbols = manaSelectionState.manaCost.match(/\{([^}]+)\}/g)
    if (!symbols) return { satisfied: 0, total: 0, entries: [] }

    // Build list of unfulfilled requirements: colored pips + generic pips
    const coloredReqs: string[] = []
    let genericCount = 0
    for (const match of symbols) {
      const inner = match.slice(1, -1)
      const num = parseInt(inner, 10)
      if (!isNaN(num)) {
        genericCount += num
      } else if (inner !== 'X') {
        coloredReqs.push(inner)
      }
    }
    if (manaSelectionState.xValue > 0) {
      genericCount += manaSelectionState.xValue
    }
    const total = coloredReqs.length + genericCount

    // Build source list: each source has the set of colors it can pay
    // A source that produces {W, B, G} can satisfy W, B, or G colored reqs, or 1 generic
    // Multi-mana sources (e.g., Gilded Lotus producing 3) contribute multiple entries
    const sources: { colors: readonly string[] }[] = []
    for (const id of manaSelectionState.selectedSources) {
      const colors = manaSelectionState.sourceColors[id] ?? []
      const manaAmount = manaSelectionState.sourceManaAmounts?.[id] ?? 1
      for (let i = 0; i < manaAmount; i++) {
        sources.push({ colors: colors.length > 0 ? colors : ['C'] })
      }
    }

    // Most-constrained-first: assign sources with fewest color options first
    // This prevents flexible sources from "wasting" on requirements that
    // less flexible sources could have covered
    const sortedSources = [...sources].sort((a, b) => a.colors.length - b.colors.length)

    // Track remaining colored requirements as a mutable count map
    const remainingColorReqs: Record<string, number> = {}
    for (const c of coloredReqs) {
      remainingColorReqs[c] = (remainingColorReqs[c] ?? 0) + 1
    }
    let remainingGeneric = genericCount
    const colorSatisfied: Record<string, number> = {}
    let satisfied = 0

    for (const source of sortedSources) {
      // Try to assign to a colored requirement this source can pay
      let assigned = false
      for (const color of source.colors) {
        if ((remainingColorReqs[color] ?? 0) > 0) {
          remainingColorReqs[color]!--
          colorSatisfied[color] = (colorSatisfied[color] ?? 0) + 1
          satisfied++
          assigned = true
          break
        }
      }
      // If no colored requirement matched, assign to generic
      if (!assigned && remainingGeneric > 0) {
        remainingGeneric--
        colorSatisfied['1'] = (colorSatisfied['1'] ?? 0) + 1
        satisfied++
      }
    }

    // Build per-color requirement counts for display
    const colorRequired: Record<string, number> = {}
    for (const c of coloredReqs) {
      colorRequired[c] = (colorRequired[c] ?? 0) + 1
    }
    if (genericCount > 0) colorRequired['1'] = genericCount

    // Sort: colored first, generic last
    const entries = Object.entries(colorRequired).sort(([a], [b]) => {
      if (a === '1' && b !== '1') return 1
      if (a !== '1' && b === '1') return -1
      return a.localeCompare(b)
    })

    return { satisfied, total, entries, colorSatisfied }
  }, [manaSelectionState])

  const counterTotalAllocated = counterDistributionState
    ? Object.values(counterDistributionState.distribution).reduce<number>((sum, v) => sum + v, 0)
    : 0
  // No "remaining" concept — X is determined by total allocated

  // Compute pass button label - prefer server-computed nextStopPoint, fall back to naive logic
  const getPassButtonLabel = () => {
    if (nextStopPoint) {
      return nextStopPoint
    }
    // Fallback for full control mode (server sends null)
    if (stackCards.length > 0) {
      return 'Resolve'
    }
    // On opponent's turn, just show "Pass" — we're yielding priority, not driving the turn
    if (!isMyTurn) {
      return 'Pass'
    }
    const nextStep = getNextStep(gameState.currentStep)
    if (nextStep) {
      if (nextStep === 'END') {
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
    <RenderProfiler id="GameBoard">
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
            top: topOffset + responsive.smallCardHeight + responsive.opponentHandBattlefieldGap + 8,
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
        paddingTop: responsive.smallCardHeight + topOffset + responsive.opponentHandBattlefieldGap,
      }}>
        <div style={{ ...styles.playerRowWithZones, alignItems: 'flex-start' }}>
          <div style={styles.playerMainArea}>
            {/* Opponent battlefield - lands first (closer to opponent), then creatures */}
            <Battlefield isOpponent spectatorMode={spectatorMode} />
          </div>

          {/* Opponent deck/graveyard (right side) */}
          {effectiveOpponent && <ZonePile player={effectiveOpponent} isOpponent />}
        </div>
      </div>

      {/* Center - Life totals, phase indicator, and stack.
          Wrapped in a row that mirrors `playerRowWithZones` (main area + zone
          pile spacer) so the step strip's center aligns with the battlefield
          cards above and below, which are centered inside `playerMainArea`. */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: 32,
        width: '100%',
        ...(spectatorMode ? {} : { transform: 'translateY(-50px)' }),
      }}>
      <div style={{
        ...styles.centerArea,
        flex: 1,
        minWidth: 0,
        width: 'auto',
        transform: 'none',
        columnGap: responsive.isMobile ? 6 : 16,
      }}>
        {/* Opponent life (left side) */}
        <div style={{ ...styles.centerLifeSection, ...styles.centerLifeSectionLeft }}>
          {effectiveOpponent && (
            <>
              <LifeDisplay life={effectiveOpponent.life} playerId={effectiveOpponent.playerId} playerName={effectiveOpponent.name} spectatorMode={spectatorMode} />
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
          activeSide={
            spectatorMode
              ? (gameState.activePlayerId === effectiveViewingPlayer?.playerId ? 'bottom' : 'top')
              : (isMyTurn ? 'bottom' : 'top')
          }
          stopOverrides={stopOverrides}
          onToggleStop={toggleStopOverride}
          isSpectator={spectatorMode}
        />

        {/* Player life (right side) */}
        <div style={{ ...styles.centerLifeSection, ...styles.centerLifeSectionRight }}>
          {effectiveViewingPlayer && (
            <>
              <LifeDisplay life={effectiveViewingPlayer.life} isPlayer playerId={effectiveViewingPlayer.playerId} playerName={effectiveViewingPlayer.name} spectatorMode={spectatorMode} />
              {!responsive.isMobile && <ActiveEffectsBadges effects={effectiveViewingPlayer.activeEffects} />}
              {!responsive.isMobile && effectiveViewingPlayer.manaPool && <ManaPool manaPool={effectiveViewingPlayer.manaPool} />}
            </>
          )}
        </div>
      </div>
        {/* Spacer matching the battlefield's ZonePile column so the centered
            content aligns with the cards above/below rather than the viewport. */}
        <div style={{ width: responsive.pileWidth, flexShrink: 0 }} aria-hidden />
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

      {/* Floating pass/resolve button (bottom-right) - always present, disabled when unavailable */}
      {!spectatorMode && viewingPlayer && !isInManaSelectionMode && !isInCounterDistMode && (() => {
        const passEnabled = canAct && !isInCombatMode && !isInDistributeMode && !isInCounterDistMode && !isInManaSelectionMode && !delveSelectionState && !crewSelectionState && !targetingState
        return (
          <div style={{
            position: 'fixed',
            bottom: 16,
            right: 16,
            zIndex: 100,
          }}>
            <button
              disabled={!passEnabled}
              onClick={() => {
                submitAction({
                  type: 'PassPriority',
                  playerId: viewingPlayer.playerId,
                })
              }}
              style={{
                ...styles.floatingBarButton,
                ...(passEnabled ? getPassButtonStyle() : {}),
                width: 170,
                height: 42,
                padding: '0 24px',
                color: passEnabled ? 'white' : '#555',
                fontWeight: 600,
                fontSize: responsive.fontSize.normal,
                border: passEnabled ? `1px solid ${getPassButtonStyle().borderColor}` : '1px solid #333',
                transition: 'background-color 0.2s, border-color 0.2s',
                opacity: passEnabled ? 1 : 0.4,
                cursor: passEnabled ? 'pointer' : 'default',
              }}
            >
              {passEnabled ? getPassButtonLabel() : 'Pass'}
            </button>
          </div>
        )
      })()}

      {/* Undo, priority mode icons (bottom-right, above pass button) */}
      {!spectatorMode && viewingPlayer && !isInManaSelectionMode && !isInCounterDistMode && (
        <div style={{
          position: 'fixed',
          bottom: responsive.isMobile ? 64 : 66,
          right: 16,
          display: 'flex',
          gap: 4,
          alignItems: 'stretch',
          zIndex: 100,
        }}>
          <button
            onClick={requestUndo}
            disabled={!undoAvailable}
            title="Undo"
            style={{
              ...styles.floatingBarButton,
              color: undoAvailable ? '#d4a017' : '#555',
              border: undoAvailable ? '1px solid #8b7000' : '1px solid #333',
              opacity: undoAvailable ? 1 : 0.4,
              cursor: undoAvailable ? 'pointer' : 'default',
            }}
          >
            <i className="ms ms-untap" style={{ fontSize: 14 }} />
          </button>
          <button
            onClick={toggleAutoTap}
            title={
              autoTapEnabled
                ? 'Auto Tap: Lands are tapped automatically. Click to switch to manual mana selection.'
                : 'Manual Tap: You choose which lands to tap. Click to switch to auto tap.'
            }
            style={{
              ...styles.floatingBarButton,
              backgroundColor: autoTapEnabled ? 'rgba(40, 40, 40, 0.8)' : 'rgba(245, 158, 11, 0.9)',
              color: autoTapEnabled ? '#999' : '#000',
              border: autoTapEnabled ? '1px solid #555' : '1px solid #f59e0b',
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            <i className="ms ms-land" style={{ fontSize: 14 }} />
          </button>
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
              ...styles.floatingBarButton,
              width: 'auto',
              padding: '0 8px',
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
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
          >
            {serverPriorityMode === 'fullControl' ? 'Full Control' :
             serverPriorityMode === 'stops' ? 'Stops' :
             'Auto'}
          </button>
        </div>
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
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#c62828',
                  border: '1px solid #ef5350',
                  opacity: combatState.validCreatures.length === 0 ? 0.5 : 1,
                }}
              >
                Attack All
              </button>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatPassButton,
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
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#c62828',
                  border: '1px solid #ef5350',
                }}
              >
                Attack with {combatState.selectedAttackers.length}
              </button>
              <button
                onClick={clearAttackers}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#424242',
                  border: '1px solid #757575',
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
            <>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatPassButton,
                }}
              >
                No Blocks
              </button>
            </>
          ) : (
            <>
              <button
                onClick={confirmCombat}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#c62828',
                  border: '1px solid #ef5350',
                }}
              >
                Confirm Blocks
              </button>
              <button
                onClick={clearBlockerAssignments}
                style={{
                  ...styles.floatingBarButton,
                  ...styles.combatActionButton,
                  backgroundColor: '#424242',
                  border: '1px solid #757575',
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

      {/* Floating counter distribution panel (bottom-right) */}
      {isInCounterDistMode && counterDistributionState && (() => {
        const requiredTotal = counterDistributionState.requiredTotal
        const canConfirm = requiredTotal != null
          ? counterTotalAllocated === requiredTotal
          : counterTotalAllocated > 0
        const hasFixedTotal = requiredTotal != null
        const progressPct = hasFixedTotal
          ? Math.min(100, (counterTotalAllocated / requiredTotal) * 100)
          : 0
        const subtext = counterDistributionState.description
          ?? 'Remove +1/+1 counters from your creatures'
        const panelWidth = responsive.isMobile ? 220 : 240
        return (
          <div style={{
            position: 'fixed',
            bottom: 16,
            right: 16,
            width: panelWidth,
            zIndex: 110,
            display: 'flex',
            flexDirection: 'column',
            backgroundColor: 'rgba(17, 24, 39, 0.92)',
            border: '1px solid rgba(255, 255, 255, 0.1)',
            borderRadius: 8,
            boxShadow: '0 6px 18px rgba(0, 0, 0, 0.35)',
            overflow: 'hidden',
            fontFamily: 'inherit',
          }}>
            {/* Body */}
            <div style={{ padding: '10px 12px' }}>
              <div style={{
                color: '#cbd5e1',
                fontSize: responsive.isMobile ? 11 : 12,
                lineHeight: 1.35,
                marginBottom: 8,
              }}>
                {subtext}
              </div>

              <div style={{
                display: 'flex',
                alignItems: 'baseline',
                justifyContent: 'space-between',
                marginBottom: hasFixedTotal ? 5 : 0,
              }}>
                <span style={{
                  color: '#94a3b8',
                  fontSize: 10,
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                  fontWeight: 600,
                }}>
                  {hasFixedTotal ? 'Allocated' : 'X'}
                </span>
                <span style={{
                  color: canConfirm ? '#86efac' : '#fbbf24',
                  fontSize: responsive.isMobile ? 13 : 14,
                  fontWeight: 700,
                  fontVariantNumeric: 'tabular-nums',
                }}>
                  {hasFixedTotal
                    ? `${counterTotalAllocated} / ${requiredTotal}`
                    : counterTotalAllocated}
                </span>
              </div>

              {hasFixedTotal && (
                <div style={{
                  height: 3,
                  backgroundColor: 'rgba(255, 255, 255, 0.06)',
                  borderRadius: 2,
                  overflow: 'hidden',
                }}>
                  <div style={{
                    width: `${progressPct}%`,
                    height: '100%',
                    backgroundColor: canConfirm ? '#22c55e' : '#f59e0b',
                    transition: 'width 0.15s ease-out, background-color 0.15s',
                  }} />
                </div>
              )}
            </div>

            {/* Footer actions */}
            <div style={{
              display: 'flex',
              gap: 6,
              padding: '8px 12px',
              borderTop: '1px solid rgba(255, 255, 255, 0.06)',
              backgroundColor: 'rgba(0, 0, 0, 0.2)',
            }}>
              <button
                onClick={cancelCounterDistribution}
                style={{
                  flex: 1,
                  height: 30,
                  padding: '0 10px',
                  backgroundColor: 'transparent',
                  color: '#94a3b8',
                  border: '1px solid rgba(255, 255, 255, 0.12)',
                  borderRadius: 5,
                  fontWeight: 500,
                  fontSize: responsive.isMobile ? 12 : 13,
                  cursor: 'pointer',
                  transition: 'background-color 0.15s, color 0.15s',
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = 'rgba(255, 255, 255, 0.04)'
                  e.currentTarget.style.color = '#cbd5e1'
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = 'transparent'
                  e.currentTarget.style.color = '#94a3b8'
                }}
              >
                Cancel
              </button>
              <button
                onClick={confirmCounterDistribution}
                disabled={!canConfirm}
                style={{
                  flex: 1,
                  height: 30,
                  padding: '0 10px',
                  backgroundColor: canConfirm ? 'rgba(22, 163, 74, 0.9)' : 'transparent',
                  color: canConfirm ? 'white' : '#64748b',
                  border: `1px solid ${canConfirm ? '#4ade80' : 'rgba(255, 255, 255, 0.08)'}`,
                  borderRadius: 5,
                  fontWeight: 600,
                  fontSize: responsive.isMobile ? 12 : 13,
                  cursor: canConfirm ? 'pointer' : 'not-allowed',
                  transition: 'background-color 0.15s, border-color 0.15s',
                }}
              >
                Confirm
              </button>
            </div>
          </div>
        )
      })()}

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

      {/* Target reselection animations (Grip of Chaos, etc.) */}
      <TargetReselectedAnimations />

      {/* Mana selection controls (bottom-right, replaces pass button during mana selection mode) */}
      {isInManaSelectionMode && manaSelectionState && (
        <div style={{
          position: 'fixed',
          bottom: 16,
          right: 16,
          display: 'flex',
          flexDirection: 'column',
          gap: 8,
          alignItems: 'flex-end',
          zIndex: 100,
        }}>
          {/* Mana progress indicator */}
          {manaProgress && (
            <div style={{
              backgroundColor: 'rgba(0, 0, 0, 0.9)',
              border: `1px solid ${manaProgress.satisfied >= manaProgress.total ? 'rgba(74, 222, 128, 0.5)' : 'rgba(255, 255, 255, 0.2)'}`,
              borderRadius: 8,
              padding: responsive.isMobile ? '8px 12px' : '10px 16px',
              display: 'flex',
              alignItems: 'center',
              gap: 10,
            }}>
              {manaProgress.entries.map(([symbol, required]) => {
                const fulfilled = manaProgress.colorSatisfied?.[symbol] ?? 0
                return (
                  <div key={symbol} style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
                    <ManaSymbol symbol={symbol} size={18} />
                    <span style={{
                      color: fulfilled >= required ? '#4ade80' : fulfilled > 0 ? '#fbbf24' : '#888',
                      fontWeight: 600,
                      fontSize: responsive.fontSize.normal,
                    }}>
                      {fulfilled}/{required}
                    </span>
                  </div>
                )
              })}
              <span style={{
                color: manaProgress.satisfied >= manaProgress.total ? '#4ade80' : '#888',
                fontSize: responsive.fontSize.small,
                marginLeft: 4,
              }}>
                ({manaProgress.satisfied}/{manaProgress.total})
              </span>
            </div>
          )}
          {/* Confirm / Cancel buttons */}
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={cancelManaSelection}
              style={{
                padding: responsive.isMobile ? '10px 20px' : '12px 24px',
                fontSize: responsive.fontSize.normal,
                fontWeight: 600,
                backgroundColor: 'rgba(40, 40, 40, 0.9)',
                color: '#ccc',
                border: '2px solid #555',
                borderRadius: 8,
                cursor: 'pointer',
              }}
            >
              Cancel
            </button>
            <button
              onClick={handleConfirmManaSelection}
              style={{
                padding: responsive.isMobile ? '10px 20px' : '12px 24px',
                fontSize: responsive.fontSize.normal,
                fontWeight: 600,
                backgroundColor: 'rgba(22, 101, 52, 0.9)',
                color: '#4ade80',
                border: '2px solid #4ade80',
                borderRadius: 8,
                cursor: 'pointer',
              }}
            >
              Confirm
            </button>
          </div>
        </div>
      )}
    </div>
    </ResponsiveContext.Provider>
    </RenderProfiler>
  )
}
