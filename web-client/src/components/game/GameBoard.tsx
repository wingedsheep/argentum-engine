import { useGameStore } from '../../store/gameStore'
import { useViewingPlayer, useOpponent, useZoneCards, useZone, useBattlefieldCards, useHasLegalActions, useStackCards, groupCards, type GroupedCard, selectPriorityMode, selectGameState } from '../../store/selectors'
import { hand, graveyard, exile, getNextStep, StepShortNames } from '../../types'
import type { ClientCard, ZoneId, ClientPlayer, LegalActionInfo, EntityId, Keyword, ClientPlayerEffect, ClientCardEffect } from '../../types'
import { CounterType } from '../../types'
import { keywordIcons, genericKeywordIcon, displayableKeywords } from '../../assets/icons/keywords'
import { PhaseIndicator } from '../ui/PhaseIndicator'
import { ManaPool } from '../ui/ManaPool'
import { AbilityText } from '../ui/ManaSymbols'
import { CombatArrows } from '../combat/CombatArrows'
import { TargetingArrows } from '../targeting/TargetingArrows'
import { DraggedCardOverlay } from './DraggedCardOverlay'
import { GameLog } from './GameLog'
import { DrawAnimations } from '../animations/DrawAnimations'
import { DamageAnimations } from '../animations/DamageAnimations'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'
import { getCardImageUrl, getScryfallFallbackUrl } from '../../utils/cardImages'
import { useInteraction } from '../../hooks/useInteraction'
import React, { createContext, useContext, useCallback, useEffect, useRef, useState } from 'react'

// Context to pass responsive sizes down the component tree
const ResponsiveContext = createContext<ResponsiveSizes | null>(null)

function useResponsiveContext(): ResponsiveSizes {
  const ctx = useContext(ResponsiveContext)
  if (!ctx) throw new Error('ResponsiveContext not provided')
  return ctx
}

/**
 * Handle image load error by falling back to Scryfall API.
 */
function handleImageError(
  e: React.SyntheticEvent<HTMLImageElement>,
  cardName: string,
  version: 'small' | 'normal' | 'large' = 'normal'
): void {
  const img = e.currentTarget
  const fallbackUrl = getScryfallFallbackUrl(cardName, version)
  // Only switch to fallback if not already using it (prevent infinite loop)
  if (!img.src.includes('api.scryfall.com')) {
    img.src = fallbackUrl
  }
}

/**
 * Get color for P/T display based on modifications.
 * Green = buffed, Red = debuffed, White = normal
 */
function getPTColor(
  power: number | null,
  toughness: number | null,
  basePower: number | null,
  baseToughness: number | null
): string {
  if (power === null || toughness === null || basePower === null || baseToughness === null) {
    return 'white'
  }

  const powerDiff = power - basePower
  const toughnessDiff = toughness - baseToughness

  // If both are increased or both are unchanged, and at least one is increased
  if (powerDiff >= 0 && toughnessDiff >= 0 && (powerDiff > 0 || toughnessDiff > 0)) {
    return '#00ff00' // Green for buffed
  }
  // If both are decreased or both are unchanged, and at least one is decreased
  if (powerDiff <= 0 && toughnessDiff <= 0 && (powerDiff < 0 || toughnessDiff < 0)) {
    return '#ff4444' // Red for debuffed
  }
  // Mixed buff/debuff - show yellow
  if (powerDiff !== 0 || toughnessDiff !== 0) {
    return '#ffff00' // Yellow for mixed
  }

  return 'white'
}

/**
 * Calculate the stat contribution from +1/+1 and -1/-1 counters.
 * Returns the net modifier (positive or negative).
 */
function getCounterStatModifier(card: ClientCard): number {
  const plusCounters = card.counters[CounterType.PLUS_ONE_PLUS_ONE] ?? 0
  const minusCounters = card.counters[CounterType.MINUS_ONE_MINUS_ONE] ?? 0
  return plusCounters - minusCounters
}

/**
 * Check if a card has any +1/+1 or -1/-1 counters.
 */
function hasStatCounters(card: ClientCard): boolean {
  const plusCounters = card.counters[CounterType.PLUS_ONE_PLUS_ONE] ?? 0
  const minusCounters = card.counters[CounterType.MINUS_ONE_MINUS_ONE] ?? 0
  return plusCounters > 0 || minusCounters > 0
}

/**
 * Container component for keyword ability icons on a card.
 * Uses SVG icons from assets/icons/keywords.
 */
const KeywordIcons = ({ keywords, size }: { keywords: readonly Keyword[]; size: number }) => {
  const filteredKeywords = keywords.filter(k => displayableKeywords.has(k))

  if (filteredKeywords.length === 0) return null

  return (
    <div style={styles.keywordIconsContainer}>
      {filteredKeywords.map((keyword) => (
        <div key={keyword} style={styles.keywordIconWrapper} title={keyword.replace(/_/g, ' ')}>
          <img
            src={keywordIcons[keyword] ?? genericKeywordIcon}
            alt={keyword}
            style={{
              width: size,
              height: size,
              display: 'block',
              filter: 'brightness(0) invert(1)', // Make SVG white
            }}
          />
        </div>
      ))}
    </div>
  )
}

/**
 * Container component for active effect badges on a card.
 * Used for temporary effects like "can't be blocked except by black creatures".
 */
const ActiveEffectBadges = ({ effects }: { effects: readonly ClientCardEffect[] }) => {
  const [hoveredEffect, setHoveredEffect] = React.useState<string | null>(null)
  const [tooltipPos, setTooltipPos] = React.useState<{ x: number; y: number } | null>(null)

  if (!effects || effects.length === 0) return null

  const handleMouseEnter = (effectId: string, e: React.MouseEvent) => {
    const rect = e.currentTarget.getBoundingClientRect()
    setTooltipPos({ x: rect.left + rect.width / 2, y: rect.top })
    setHoveredEffect(effectId)
  }

  const handleMouseLeave = () => {
    setHoveredEffect(null)
    setTooltipPos(null)
  }

  const hoveredEffectData = effects.find(e => e.effectId === hoveredEffect)

  return (
    <>
      <div style={styles.activeEffectsContainer}>
        {effects.map((effect) => (
          <div
            key={effect.effectId}
            style={styles.activeEffectBadge}
            onMouseEnter={(e) => handleMouseEnter(effect.effectId, e)}
            onMouseLeave={handleMouseLeave}
          >
            <span style={styles.activeEffectText}>{effect.name}</span>
          </div>
        ))}
      </div>
      {hoveredEffect && tooltipPos && hoveredEffectData?.description && (
        <div style={{
          ...styles.cardEffectTooltip,
          left: tooltipPos.x,
          top: tooltipPos.y,
        }}>
          {hoveredEffectData.description}
        </div>
      )}
    </>
  )
}

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
 */
export function GameBoard({ spectatorMode = false, topOffset = 0 }: GameBoardProps) {
  const playerGameState = useGameStore((state) => state.gameState)
  const spectatingState = useGameStore((state) => state.spectatingState)
  const playerId = useGameStore((state) => state.playerId)
  const submitAction = useGameStore((state) => state.submitAction)
  const combatState = useGameStore((state) => state.combatState)
  const confirmCombat = useGameStore((state) => state.confirmCombat)
  const cancelCombat = useGameStore((state) => state.cancelCombat)
  const attackWithAll = useGameStore((state) => state.attackWithAll)
  const priorityMode = useGameStore(selectPriorityMode)
  const responsive = useResponsive(topOffset)

  // In spectator mode, use spectatingState.gameState
  const gameState = spectatorMode ? spectatingState?.gameState : playerGameState

  const viewingPlayer = useViewingPlayer()
  const opponent = useOpponent()
  const stackCards = useStackCards()

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
  const isMyTurn = spectatorMode ? false : (gameState.activePlayerId === viewingPlayer?.playerId)
  const isInCombatMode = spectatorMode ? false : (combatState !== null)


  // Compute pass button label
  const getPassButtonLabel = () => {
    // Show "Resolve" when there's something on the stack
    if (stackCards.length > 0) {
      return 'Resolve'
    }
    // Show "To my turn" when at opponent's end step
    const isOpponentsTurn = gameState.activePlayerId !== viewingPlayer?.playerId
    if (isOpponentsTurn && gameState.currentStep === 'END') {
      return 'To my turn'
    }
    // Otherwise show next step
    const nextStep = getNextStep(gameState.currentStep)
    if (nextStep) {
      // "End Turn" when passing to end step on my turn
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
            <BattlefieldArea isOpponent spectatorMode={spectatorMode} />
          </div>

          {/* Opponent deck/graveyard (right side) */}
          {effectiveOpponent && <ZonePile player={effectiveOpponent} isOpponent />}
        </div>
      </div>

      {/* Center - Life totals, phase indicator, and stack */}
      <div style={styles.centerArea}>
        {/* Opponent life (left side) */}
        <div style={styles.centerLifeSection}>
          {effectiveOpponent && (
            <>
              <LifeDisplay life={effectiveOpponent.life} playerId={effectiveOpponent.playerId} />
              <span style={{ ...styles.playerName, fontSize: responsive.fontSize.small }}>{effectiveOpponent.name}</span>
              <ActiveEffectsBadges effects={effectiveOpponent.activeEffects} />
              {effectiveOpponent.manaPool && <ManaPool manaPool={effectiveOpponent.manaPool} />}
            </>
          )}
        </div>

        {/* Phase indicator (center) */}
        <PhaseIndicator
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
        />

        {/* Player life (right side) */}
        <div style={styles.centerLifeSection}>
          {effectiveViewingPlayer && (
            <>
              <LifeDisplay life={effectiveViewingPlayer.life} isPlayer playerId={effectiveViewingPlayer.playerId} />
              <span style={{ ...styles.playerName, fontSize: responsive.fontSize.small }}>{effectiveViewingPlayer.name}</span>
              <ActiveEffectsBadges effects={effectiveViewingPlayer.activeEffects} />
              {effectiveViewingPlayer.manaPool && <ManaPool manaPool={effectiveViewingPlayer.manaPool} />}
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
            <BattlefieldArea isOpponent={false} spectatorMode={spectatorMode} />

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
          />
        ) : null}
      </div>

      {/* Floating pass button (bottom-right) - hidden in spectator mode */}
      {!spectatorMode && hasPriority && !isInCombatMode && viewingPlayer && (
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
            padding: responsive.isMobile ? '10px 20px' : '12px 28px',
            fontSize: responsive.fontSize.normal,
            border: `2px solid ${getPassButtonStyle().borderColor}`,
            transition: 'background-color 0.2s, border-color 0.2s',
          }}
        >
          {getPassButtonLabel()}
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
                Skip
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
                onClick={cancelCombat}
                style={{
                  ...styles.combatButton,
                  ...styles.combatButtonSecondary,
                }}
              >
                Cancel
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
                onClick={cancelCombat}
                style={{
                  ...styles.combatButton,
                  ...styles.combatButtonSecondary,
                }}
              >
                Cancel
              </button>
            </>
          )}
        </div>
      )}


      {/* Action menu for selected card - hidden in spectator mode */}
      {!spectatorMode && <ActionMenu />}

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
    </div>
    </ResponsiveContext.Provider>
  )
}

/**
 * Active effects badges - shows status effects on a player.
 */
function ActiveEffectsBadges({ effects }: { effects: readonly ClientPlayerEffect[] | undefined }) {
  const responsive = useResponsiveContext()
  const [hoveredEffect, setHoveredEffect] = React.useState<string | null>(null)

  if (!effects || effects.length === 0) return null

  return (
    <div style={styles.effectBadgesContainer}>
      {effects.map((effect) => (
        <div
          key={effect.effectId}
          style={{
            ...styles.effectBadge,
            padding: responsive.isMobile ? '2px 6px' : '4px 8px',
            fontSize: responsive.fontSize.small,
          }}
          onMouseEnter={() => setHoveredEffect(effect.effectId)}
          onMouseLeave={() => setHoveredEffect(null)}
        >
          {effect.icon && <span style={styles.effectBadgeIcon}>{getEffectIcon(effect.icon)}</span>}
          <span style={styles.effectBadgeName}>{effect.name}</span>
          {hoveredEffect === effect.effectId && effect.description && (
            <div style={styles.effectTooltip}>
              {effect.description}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

/**
 * Get an emoji or icon for an effect based on its icon identifier.
 */
function getEffectIcon(icon: string): string {
  switch (icon) {
    case 'shield-off':
      return '🛡️'
    case 'shield':
      return '⚡'
    case 'skip':
      return '⏭️'
    case 'lock':
      return '🔒'
    case 'skull':
      return '💀'
    case 'taunt':
      return '⚔️'
    default:
      return '⚡'
  }
}

/**
 * Life total display - interactive when in targeting mode or when a pending decision requires player targeting.
 */
function LifeDisplay({
  life,
  isPlayer = false,
  playerId
}: {
  life: number
  isPlayer?: boolean
  playerId: EntityId
}) {
  const responsive = useResponsiveContext()
  const targetingState = useGameStore((state) => state.targetingState)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)
  const submitTargetsDecision = useGameStore((state) => state.submitTargetsDecision)

  // Check if this player is a valid target in current targeting mode
  const isValidTargetingTarget = targetingState?.validTargets.includes(playerId) ?? false
  const isTargetingSelected = targetingState?.selectedTargets.includes(playerId) ?? false

  // Check if this player is a valid target in a pending ChooseTargetsDecision
  const isChooseTargetsDecision = pendingDecision?.type === 'ChooseTargetsDecision'
  const decisionLegalTargets = isChooseTargetsDecision
    ? (pendingDecision.legalTargets[0] ?? [])
    : []
  const isValidDecisionTarget = decisionLegalTargets.includes(playerId)

  // Combine both targeting modes
  const isValidTarget = isValidTargetingTarget || isValidDecisionTarget
  const isSelected = isTargetingSelected

  const handleClick = () => {
    // Handle regular targeting state - click to select, click again to unselect
    if (targetingState) {
      if (isTargetingSelected) {
        removeTarget(playerId)
        return
      }
      if (isValidTargetingTarget) {
        addTarget(playerId)
        return
      }
    }

    // Handle pending decision targeting
    if (isChooseTargetsDecision && isValidDecisionTarget) {
      // Submit the decision with this player as the target
      submitTargetsDecision({ 0: [playerId] })
    }
  }

  const size = responsive.isMobile ? 36 : responsive.isTablet ? 42 : 48

  // Dynamic styling based on targeting state
  const bgColor = isPlayer ? '#1a3a5a' : '#3a1a4a'
  const borderColor = isSelected
    ? '#ffff00' // Yellow if selected as target
    : isValidTarget
      ? '#ff4444' // Red glow if valid target
      : isPlayer ? '#3a7aba' : '#7a3a9a'

  const cursor = isValidTarget ? 'pointer' : 'default'
  const boxShadow = isSelected
    ? '0 0 20px rgba(255, 255, 0, 0.8)'
    : isValidTarget
      ? '0 0 15px rgba(255, 68, 68, 0.6)'
      : 'none'

  return (
    <div
      data-player-id={playerId}
      data-life-id={playerId}
      data-life-display={playerId}
      onClick={handleClick}
      style={{
        ...styles.lifeDisplay,
        width: size,
        height: size,
        fontSize: responsive.fontSize.large,
        backgroundColor: bgColor,
        borderColor: borderColor,
        cursor,
        boxShadow,
        transition: 'all 0.2s ease-in-out',
        position: 'relative',
      }}
    >
      <span
        style={{
          position: 'absolute',
          top: -8,
          left: '50%',
          transform: 'translateX(-50%)',
          fontSize: 9,
          fontWeight: 'bold',
          color: isPlayer ? '#4a9aea' : '#aa6aca',
          backgroundColor: '#1a1a2e',
          padding: '1px 4px',
          borderRadius: 3,
          whiteSpace: 'nowrap',
        }}
      >
        {isPlayer ? 'YOU' : 'OPPONENT'}
      </span>
      <span style={{ color: life <= 5 ? '#ff4444' : '#ffffff' }}>{life}</span>
    </div>
  )
}

/**
 * Stack display - shows spells/abilities waiting to resolve.
 * Cards stack on top of each other like a physical pile.
 */
function StackDisplay() {
  const stackCards = useStackCards()
  const responsive = useResponsiveContext()
  const hoverCard = useGameStore((state) => state.hoverCard)
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)

  if (stackCards.length === 0) return null

  const handleStackItemClick = (cardId: EntityId) => {
    if (!targetingState) return

    const isValidTarget = targetingState.validTargets.includes(cardId)
    const isSelectedTarget = targetingState.selectedTargets.includes(cardId)

    if (isSelectedTarget) {
      removeTarget(cardId)
    } else if (isValidTarget) {
      addTarget(cardId)
    }
  }

  // Offset between cards - shows a sliver of each card below
  const cardOffset = 20
  // Top of stack (most recently cast, resolves first) is last in the array
  const topCard = stackCards[stackCards.length - 1]

  return (
    <div style={styles.stackContainer}>
      <div style={{
        ...styles.stackHeader,
        fontSize: responsive.fontSize.small,
      }}>
        Stack ({stackCards.length})
      </div>
      <div style={styles.stackItems}>
        {stackCards.map((card, index) => {
          const isValidTarget = targetingState?.validTargets.includes(card.id) ?? false
          const isSelectedTarget = targetingState?.selectedTargets.includes(card.id) ?? false

          return (
            <div
              key={card.id}
              data-card-id={card.id}
              style={{
                ...styles.stackItem,
                marginTop: index === 0 ? 0 : -84 + cardOffset, // Overlap cards, showing cardOffset pixels of each
                zIndex: index + 1, // Later cards (higher index = cast later) on top
                ...(isValidTarget && !isSelectedTarget ? {
                  boxShadow: '0 0 12px 4px rgba(255, 200, 0, 0.8)',
                  borderRadius: 6,
                } : {}),
                ...(isSelectedTarget ? {
                  boxShadow: '0 0 12px 4px rgba(0, 255, 100, 0.8)',
                  borderRadius: 6,
                } : {}),
              }}
              onClick={() => handleStackItemClick(card.id)}
              onMouseEnter={() => hoverCard(card.id)}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'small')}
                alt={card.name}
                style={{
                  ...styles.stackItemImage,
                  cursor: isValidTarget ? 'pointer' : 'default',
                }}
                title={`${card.name}\n${card.oracleText || ''}`}
                onError={(e) => handleImageError(e, card.name, 'small')}
              />
            </div>
          )
        })}
        {/* Show name of top card (most recently cast) */}
        {topCard && (
          <div style={{
            ...styles.stackItemName,
            fontSize: responsive.fontSize.small,
            marginTop: 4,
          }}>
            {topCard.name}
          </div>
        )}
      </div>
    </div>
  )
}

/**
 * Card preview overlay - shows enlarged card when hovering.
 * Shows rulings after hovering for 1 second.
 */
function CardPreview() {
  const hoveredCardId = useGameStore((state) => state.hoveredCardId)
  const gameState = useGameStore(selectGameState)
  const responsive = useResponsiveContext()
  const [showRulings, setShowRulings] = useState(false)
  const [lastHoveredId, setLastHoveredId] = useState<EntityId | null>(null)

  // Show rulings after hovering for 1 second
  useEffect(() => {
    if (hoveredCardId !== lastHoveredId) {
      setLastHoveredId(hoveredCardId)
      setShowRulings(false)
    }

    if (!hoveredCardId) {
      setShowRulings(false)
      return
    }

    const timer = setTimeout(() => {
      setShowRulings(true)
    }, 1000)

    return () => clearTimeout(timer)
  }, [hoveredCardId, lastHoveredId])

  if (!hoveredCardId || !gameState) return null

  const card = gameState.cards[hoveredCardId]
  if (!card) return null

  const cardImageUrl = getCardImageUrl(card.name, card.imageUri, 'large')

  // Calculate preview size - larger than normal cards
  const previewWidth = responsive.isMobile ? 200 : 280
  const previewHeight = Math.round(previewWidth * 1.4)

  // Determine if stats are modified
  const isPowerBuffed = card.power !== null && card.basePower !== null && card.power > card.basePower
  const isPowerDebuffed = card.power !== null && card.basePower !== null && card.power < card.basePower
  const isToughnessBuffed = card.toughness !== null && card.baseToughness !== null && card.toughness > card.baseToughness
  const isToughnessDebuffed = card.toughness !== null && card.baseToughness !== null && card.toughness < card.baseToughness
  const hasStatModifications = isPowerBuffed || isPowerDebuffed || isToughnessBuffed || isToughnessDebuffed

  // Calculate stat breakdown
  const counterModifier = getCounterStatModifier(card)
  const hasCounters = hasStatCounters(card)
  // Effects = total change - counter contribution
  const effectPowerMod = card.power !== null && card.basePower !== null
    ? (card.power - card.basePower) - counterModifier
    : 0
  const effectToughnessMod = card.toughness !== null && card.baseToughness !== null
    ? (card.toughness - card.baseToughness) - counterModifier
    : 0
  const hasEffects = effectPowerMod !== 0 || effectToughnessMod !== 0

  const hasRulings = card.rulings && card.rulings.length > 0

  return (
    <div style={styles.cardPreviewOverlay}>
      <div style={{
        ...styles.cardPreviewContainer,
        width: previewWidth,
      }}>
        {/* Card image */}
        <div style={{
          ...styles.cardPreviewCard,
          width: previewWidth,
          height: previewHeight,
        }}>
          <img
            src={cardImageUrl}
            alt={card.name}
            style={styles.cardPreviewImage}
            onError={(e) => handleImageError(e, card.name, 'large')}
          />
        </div>

        {/* Stats box (for creatures with modifications) */}
        {card.power !== null && card.toughness !== null && hasStatModifications && (
          <div style={styles.cardPreviewStatsBox}>
            {/* Current P/T (large) */}
            <div style={styles.cardPreviewStatsMain}>
              <span style={{
                color: isPowerBuffed ? '#00ff00' : isPowerDebuffed ? '#ff4444' : '#ffffff',
                fontWeight: 700,
                fontSize: responsive.isMobile ? 20 : 26,
              }}>
                {card.power}
              </span>
              <span style={{ color: '#ffffff', fontSize: responsive.isMobile ? 20 : 26 }}>/</span>
              <span style={{
                color: isToughnessBuffed ? '#00ff00' : isToughnessDebuffed ? '#ff4444' : '#ffffff',
                fontWeight: 700,
                fontSize: responsive.isMobile ? 20 : 26,
              }}>
                {card.toughness}
              </span>
            </div>
            {/* Breakdown rows */}
            <div style={styles.cardPreviewStatsBreakdown}>
              {/* Base stats */}
              {card.basePower !== null && card.baseToughness !== null && (
                <div style={styles.cardPreviewStatsRow}>
                  <span style={styles.cardPreviewStatsLabel}>Base</span>
                  <span style={styles.cardPreviewStatsValue}>
                    {card.basePower}/{card.baseToughness}
                  </span>
                </div>
              )}
              {/* Counter contribution */}
              {hasCounters && (
                <div style={styles.cardPreviewStatsRow}>
                  <span style={{...styles.cardPreviewStatsLabel, color: '#66ccff'}}>
                    <span style={{marginRight: 4}}>⬡</span>Counters
                  </span>
                  <span style={{...styles.cardPreviewStatsValue, color: counterModifier >= 0 ? '#66ccff' : '#ff6666'}}>
                    {counterModifier >= 0 ? '+' : ''}{counterModifier}/{counterModifier >= 0 ? '+' : ''}{counterModifier}
                  </span>
                </div>
              )}
              {/* Effects contribution */}
              {hasEffects && (
                <div style={styles.cardPreviewStatsRow}>
                  <span style={{...styles.cardPreviewStatsLabel, color: '#ffcc66'}}>Effects</span>
                  <span style={{...styles.cardPreviewStatsValue, color: '#ffcc66'}}>
                    {effectPowerMod >= 0 ? '+' : ''}{effectPowerMod}/{effectToughnessMod >= 0 ? '+' : ''}{effectToughnessMod}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Keywords/abilities info panel */}
        {card.keywords.length > 0 && (
          <div style={styles.cardPreviewKeywords}>
            {card.keywords.map((keyword) => (
              <div key={keyword} style={styles.cardPreviewKeyword}>
                <span style={styles.cardPreviewKeywordName}>{keyword}</span>
              </div>
            ))}
          </div>
        )}

        {/* Rulings panel - appears after 1 second of hovering */}
        {showRulings && hasRulings && (
          <div style={styles.cardPreviewRulings}>
            <div style={styles.cardPreviewRulingsHeader}>Rulings</div>
            {card.rulings!.map((ruling, index) => (
              <div key={index} style={styles.cardPreviewRuling}>
                <div style={styles.cardPreviewRulingDate}>{ruling.date}</div>
                <div style={styles.cardPreviewRulingText}>{ruling.text}</div>
              </div>
            ))}
          </div>
        )}

        {/* Rulings indicator - shows immediately if card has rulings */}
        {!showRulings && hasRulings && (
          <div style={styles.cardPreviewRulingsHint}>
            Hold to see rulings...
          </div>
        )}
      </div>
    </div>
  )
}

/**
 * Row of cards (hand or other horizontal zone).
 * Cards in hand are NOT grouped - each card is shown individually.
 */
function CardRow({
  zoneId,
  faceDown = false,
  interactive = false,
  small = false,
  inverted = false,
}: {
  zoneId: ZoneId
  faceDown?: boolean
  interactive?: boolean
  small?: boolean
  inverted?: boolean
}) {
  const cards = useZoneCards(zoneId)
  const zone = useZone(zoneId)
  const responsive = useResponsiveContext()

  // For hidden zones (like opponent's hand), use zone size to show face-down placeholders
  const zoneSize = zone?.size ?? 0
  const showPlaceholders = faceDown && cards.length === 0 && zoneSize > 0

  if (cards.length === 0 && !showPlaceholders) {
    return <div style={{ ...styles.emptyZone, fontSize: responsive.fontSize.small }}>No cards</div>
  }

  // Calculate available width for the hand (viewport - padding - zone piles on sides)
  const sideZoneWidth = responsive.pileWidth + 20 // pile + margin
  const availableWidth = responsive.viewportWidth - (responsive.containerPadding * 2) - (sideZoneWidth * 2)

  // Calculate card width that fits all cards
  const cardCount = showPlaceholders ? zoneSize : cards.length
  const baseWidth = small ? responsive.smallCardWidth : responsive.cardWidth
  const minWidth = small ? 30 : 45
  const fittingWidth = calculateFittingCardWidth(
    cardCount,
    availableWidth,
    responsive.cardGap,
    baseWidth,
    minWidth
  )

  // For hands (player or opponent), create a fan effect
  // - Player's own hand: interactive, face-up
  // - Opponent's hand: face-down, inverted (top of screen)
  // - Spectator bottom hand: face-down, not inverted (bottom of screen)
  const isPlayerHand = interactive && !faceDown
  const isOpponentHand = faceDown && inverted
  const isSpectatorBottomHand = faceDown && !inverted && !interactive
  const cardHeight = Math.round(fittingWidth * 1.4)

  if ((isPlayerHand || isOpponentHand || isSpectatorBottomHand) && (cards.length > 0 || showPlaceholders)) {
    return (
      <HandFan
        cards={cards}
        placeholderCount={showPlaceholders ? zoneSize : 0}
        fittingWidth={fittingWidth}
        cardHeight={cardHeight}
        cardGap={responsive.cardGap}
        faceDown={faceDown}
        interactive={interactive}
        small={small}
        inverted={inverted}
      />
    )
  }

  // Render face-down placeholders for hidden zones (non-fan layout)
  if (showPlaceholders) {
    const cardRatio = 1.4
    const height = Math.round(fittingWidth * cardRatio)
    return (
      <div style={{ ...styles.cardRow, gap: responsive.cardGap, padding: responsive.cardGap }}>
        {Array.from({ length: zoneSize }).map((_, index) => (
          <div
            key={`placeholder-${index}`}
            style={{
              ...styles.card,
              width: fittingWidth,
              height,
              borderRadius: responsive.isMobile ? 4 : 8,
              border: '2px solid #333',
              boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
            }}
          >
            <img
              src="https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389"
              alt="Card back"
              style={styles.cardImage}
            />
          </div>
        ))}
      </div>
    )
  }

  // Render each card individually (no grouping for hand)
  return (
    <div style={{ ...styles.cardRow, gap: responsive.cardGap, padding: responsive.cardGap }}>
      {cards.map((card) => (
        <GameCard
          key={card.id}
          card={card}
          count={1}
          faceDown={faceDown}
          interactive={interactive}
          small={small}
          overrideWidth={fittingWidth}
          inHand={interactive && !faceDown}
        />
      ))}
    </div>
  )
}

/**
 * Hand display with fan/arc effect - cards slightly overlap and rotate like held cards.
 */
function HandFan({
  cards,
  placeholderCount = 0,
  fittingWidth,
  cardHeight,
  faceDown,
  interactive,
  small,
  inverted = false,
}: {
  cards: readonly ClientCard[]
  placeholderCount?: number
  fittingWidth: number
  cardHeight: number
  cardGap: number
  faceDown: boolean
  interactive: boolean
  small: boolean
  inverted?: boolean
}) {
  const [, setHoveredIndex] = useState<number | null>(null)

  const cardCount = placeholderCount > 0 ? placeholderCount : cards.length

  // Scale fan parameters based on card count
  // Fewer cards = more spread, more cards = tighter fan
  const maxRotation = Math.min(12, 40 / Math.max(cardCount, 1)) // Max rotation at edges (degrees)
  const maxVerticalOffset = Math.min(15, 45 / Math.max(cardCount, 1)) // Max rise at center (pixels)

  // Calculate overlap - more overlap with more cards, but keep it readable
  const overlapFactor = Math.max(0.5, 0.85 - (cardCount * 0.025))
  const cardSpacing = fittingWidth * overlapFactor

  // Total width of the hand fan
  const totalWidth = cardSpacing * (cardCount - 1) + fittingWidth

  // Allow cards to extend slightly beyond the visible area to save vertical space
  const edgeMargin = -15

  // For inverted fan, flip the arc and rotation direction
  const rotationMultiplier = inverted ? -1 : 1

  // Create array of items to render (either cards or placeholder indices)
  const items = placeholderCount > 0
    ? Array.from({ length: placeholderCount }, (_, i) => ({ type: 'placeholder' as const, index: i }))
    : cards.map((card, index) => ({ type: 'card' as const, card, index }))

  return (
    <div
      style={{
        position: 'relative',
        width: totalWidth,
        height: cardHeight + maxVerticalOffset + 40, // Extra space for hover lift
        marginBottom: inverted ? 0 : edgeMargin,
        marginTop: inverted ? edgeMargin : 0,
      }}
    >
      {items.map((item, index) => {
        // Calculate position from center (-1 to 1)
        const centerOffset = cardCount > 1
          ? (index - (cardCount - 1) / 2) / ((cardCount - 1) / 2)
          : 0

        // Calculate rotation (edges rotate away from center)
        const rotation = centerOffset * maxRotation * rotationMultiplier

        // Calculate vertical offset (arc shape - center cards are higher/lower)
        const verticalOffset = (1 - Math.abs(centerOffset) ** 1.5) * maxVerticalOffset

        // Calculate horizontal position
        const left = index * cardSpacing

        // Z-index: center cards on top
        const zIndex = 50 - Math.abs(index - Math.floor(cardCount / 2))

        const key = item.type === 'card' ? item.card.id : `placeholder-${item.index}`

        return (
          <div
            key={key}
            style={{
              position: 'absolute',
              left,
              ...(inverted
                ? { top: edgeMargin, transform: `translateY(${verticalOffset}px) rotate(${rotation}deg)` }
                : { bottom: edgeMargin, transform: `translateY(${-verticalOffset}px) rotate(${rotation}deg)` }
              ),
              transformOrigin: inverted ? 'top center' : 'bottom center',
              zIndex,
              transition: 'transform 0.12s ease-out, left 0.12s ease-out',
              cursor: interactive ? 'pointer' : 'default',
            }}
            onMouseEnter={() => !inverted && setHoveredIndex(index)}
            onMouseLeave={() => !inverted && setHoveredIndex(null)}
          >
            {item.type === 'card' ? (
              <GameCard
                card={item.card}
                count={1}
                faceDown={faceDown}
                interactive={interactive}
                small={small}
                overrideWidth={fittingWidth}
                inHand={interactive && !faceDown}
              />
            ) : (
              <div
                style={{
                  width: fittingWidth,
                  height: cardHeight,
                  borderRadius: 6,
                  border: '2px solid #333',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
                  overflow: 'hidden',
                }}
              >
                <img
                  src="https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389"
                  alt="Card back"
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}

/**
 * Deck, graveyard, and exile pile display.
 */
function ZonePile({ player, isOpponent = false }: { player: ClientPlayer; isOpponent?: boolean }) {
  const graveyardCards = useZoneCards(graveyard(player.playerId))
  const topGraveyardCard = graveyardCards[graveyardCards.length - 1]
  const exileCards = useZoneCards(exile(player.playerId))
  const topExileCard = exileCards[exileCards.length - 1]
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const [browsingGraveyard, setBrowsingGraveyard] = useState(false)
  const [browsingExile, setBrowsingExile] = useState(false)
  const stackCards = useStackCards()

  // Find any graveyard cards that are being targeted by spells on the stack
  const targetedGraveyardCard = React.useMemo(() => {
    for (const stackCard of stackCards) {
      if (!stackCard.targets) continue
      for (const target of stackCard.targets) {
        if (target.type === 'Card') {
          // Check if this card is in this player's graveyard
          const card = graveyardCards.find((c) => c.id === target.cardId)
          if (card) return card
        }
      }
    }
    return null
  }, [stackCards, graveyardCards])

  const pileStyle = {
    width: responsive.pileWidth,
    height: responsive.pileHeight,
    borderRadius: responsive.isMobile ? 4 : 6,
  }

  // Offset to avoid overlapping with buttons:
  // - Player zones move up to avoid pass priority button (bottom-right)
  // - Opponent zones move down to avoid concede button (top-right)
  const verticalOffset = isOpponent
    ? { marginTop: responsive.zonePileOffset }
    : { marginBottom: responsive.zonePileOffset + responsive.sectionGap * 3 }

  return (
    <div style={{ ...styles.zonePile, gap: responsive.cardGap, minWidth: responsive.pileWidth + 10, ...verticalOffset }}>
      {/* Library/Deck */}
      <div style={styles.zoneStack}>
        <div data-zone={isOpponent ? 'opponent-library' : 'player-library'} style={{ ...styles.deckPile, ...pileStyle }}>
          {player.librarySize > 0 ? (
            <img
              src="https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389"
              alt="Library"
              style={styles.pileImage}
            />
          ) : (
            <div style={styles.emptyPile} />
          )}
          <div style={{ ...styles.pileCount, fontSize: responsive.fontSize.small }}>{player.librarySize}</div>
        </div>
        <span style={{ ...styles.zoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>Deck</span>
      </div>

      {/* Graveyard */}
      <div style={styles.zoneStack}>
        <div
          data-graveyard-id={player.playerId}
          style={{ ...styles.graveyardPile, ...pileStyle, cursor: graveyardCards.length > 0 ? 'pointer' : 'default' }}
          onClick={() => { if (graveyardCards.length > 0) setBrowsingGraveyard(true) }}
          onMouseEnter={() => { if (topGraveyardCard) hoverCard(topGraveyardCard.id) }}
          onMouseLeave={() => hoverCard(null)}
        >
          {topGraveyardCard ? (
            <img
              src={getCardImageUrl(topGraveyardCard.name, topGraveyardCard.imageUri, 'normal')}
              alt={topGraveyardCard.name}
              style={{ ...styles.pileImage, opacity: 0.8 }}
              onError={(e) => handleImageError(e, topGraveyardCard.name, 'normal')}
            />
          ) : (
            <div style={styles.emptyPile} />
          )}
          {player.graveyardSize > 0 && (
            <div style={{ ...styles.pileCount, fontSize: responsive.fontSize.small }}>{player.graveyardSize}</div>
          )}
          {/* Show targeted card on top when a spell is targeting a card in this graveyard */}
          {targetedGraveyardCard && (
            <div
              data-card-id={targetedGraveyardCard.id}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                zIndex: 10,
                boxShadow: '0 0 12px 4px rgba(255, 136, 0, 0.8)',
                borderRadius: responsive.isMobile ? 4 : 6,
              }}
            >
              <img
                src={getCardImageUrl(targetedGraveyardCard.name, targetedGraveyardCard.imageUri, 'normal')}
                alt={targetedGraveyardCard.name}
                style={{ ...styles.pileImage, borderRadius: responsive.isMobile ? 4 : 6 }}
                onError={(e) => handleImageError(e, targetedGraveyardCard.name, 'normal')}
              />
            </div>
          )}
        </div>
        <span style={{ ...styles.zoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>Graveyard</span>
      </div>

      {/* Exile */}
      <div style={styles.zoneStack}>
        <div
          data-exile-id={player.playerId}
          style={{ ...styles.exilePile, ...pileStyle, cursor: exileCards.length > 0 ? 'pointer' : 'default' }}
          onClick={() => { if (exileCards.length > 0) setBrowsingExile(true) }}
          onMouseEnter={() => { if (topExileCard) hoverCard(topExileCard.id) }}
          onMouseLeave={() => hoverCard(null)}
        >
          {topExileCard ? (
            <img
              src={getCardImageUrl(topExileCard.name, topExileCard.imageUri, 'normal')}
              alt={topExileCard.name}
              style={{ ...styles.pileImage, opacity: 0.7 }}
              onError={(e) => handleImageError(e, topExileCard.name, 'normal')}
            />
          ) : (
            <div style={styles.emptyPile} />
          )}
          {player.exileSize > 0 && (
            <div style={{ ...styles.pileCount, fontSize: responsive.fontSize.small }}>{player.exileSize}</div>
          )}
        </div>
        <span style={{ ...styles.zoneLabel, fontSize: responsive.isMobile ? 8 : 10 }}>Exile</span>
      </div>

      {browsingGraveyard && (
        <GraveyardBrowser cards={graveyardCards} onClose={() => setBrowsingGraveyard(false)} />
      )}
      {browsingExile && (
        <ExileBrowser cards={exileCards} onClose={() => setBrowsingExile(false)} />
      )}
    </div>
  )
}

/**
 * Full-screen overlay for browsing exile cards.
 */
function ExileBrowser({ cards, onClose }: { cards: readonly ClientCard[], onClose: () => void }) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const [minimized, setMinimized] = useState(false)

  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (minimized) {
          setMinimized(false)
        } else {
          onClose()
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, minimized])

  // When minimized, show floating button to restore
  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          padding: responsive.isMobile ? '10px 16px' : '12px 24px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#7c3aed',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        ↑ Return to Exile
      </button>
    )
  }

  return (
    <div style={styles.exileOverlay} onClick={onClose}>
      <div style={styles.exileBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.exileBrowserHeader}>
          <h2 style={styles.exileBrowserTitle}>Exile ({cards.length})</h2>
          <button style={styles.exileCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.exileCardGrid}>
          {cards.map((card) => (
            <div
              key={card.id}
              style={{ width: cardWidth, height: cardHeight, borderRadius: 6, overflow: 'hidden', flexShrink: 0 }}
              onMouseEnter={() => hoverCard(card.id)}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
            </div>
          ))}
        </div>
        {/* Action buttons */}
        <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
          <button
            onClick={() => setMinimized(true)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#7c3aed',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            View Battlefield
          </button>
          <button
            onClick={onClose}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#333',
              color: '#aaa',
              border: '1px solid #555',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Full-screen overlay for browsing graveyard cards.
 */
function GraveyardBrowser({ cards, onClose }: { cards: readonly ClientCard[], onClose: () => void }) {
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsiveContext()
  const [minimized, setMinimized] = useState(false)

  const cardWidth = responsive.isMobile ? 120 : 160
  const cardHeight = Math.round(cardWidth * 1.4)

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (minimized) {
          setMinimized(false)
        } else {
          onClose()
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, minimized])

  // When minimized, show floating button to restore
  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          padding: responsive.isMobile ? '10px 16px' : '12px 24px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#1e40af',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        ↑ Return to Graveyard
      </button>
    )
  }

  return (
    <div style={styles.graveyardOverlay} onClick={onClose}>
      <div style={styles.graveyardBrowserContent} onClick={(e) => e.stopPropagation()}>
        <div style={styles.graveyardBrowserHeader}>
          <h2 style={styles.graveyardBrowserTitle}>Graveyard ({cards.length})</h2>
          <button style={styles.graveyardCloseButton} onClick={onClose}>✕</button>
        </div>
        <div style={styles.graveyardCardGrid}>
          {cards.map((card) => (
            <div
              key={card.id}
              style={{ width: cardWidth, height: cardHeight, borderRadius: 6, overflow: 'hidden', flexShrink: 0 }}
              onMouseEnter={() => hoverCard(card.id)}
              onMouseLeave={() => hoverCard(null)}
            >
              <img
                src={getCardImageUrl(card.name, card.imageUri, 'normal')}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
            </div>
          ))}
        </div>
        {/* Action buttons */}
        <div style={{ display: 'flex', gap: 16, marginTop: 16 }}>
          <button
            onClick={() => setMinimized(true)}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#1e40af',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            View Battlefield
          </button>
          <button
            onClick={onClose}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 28px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: '#333',
              color: '#aaa',
              border: '1px solid #555',
              borderRadius: 8,
              cursor: 'pointer',
            }}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Battlefield area with lands and creatures.
 * For player: creatures first (closer to center), then lands (closer to player)
 * For opponent: lands first (closer to opponent), then creatures (closer to center)
 */
function BattlefieldArea({ isOpponent, spectatorMode = false }: { isOpponent: boolean; spectatorMode?: boolean }) {
  const {
    playerLands,
    playerCreatures,
    playerOther,
    opponentLands,
    opponentCreatures,
    opponentOther,
  } = useBattlefieldCards()
  const responsive = useResponsiveContext()

  const lands = isOpponent ? opponentLands : playerLands
  const creatures = isOpponent ? opponentCreatures : playerCreatures
  const other = isOpponent ? opponentOther : playerOther

  // Group identical lands, display creatures and other individually
  const groupedLands = groupCards(lands)
  const groupedCreatures = creatures.map((card) => ({
    card,
    count: 1,
    cardIds: [card.id] as const,
    cards: [card] as const,
  }))
  const groupedOther = other.map((card) => ({
    card,
    count: 1,
    cardIds: [card.id] as const,
    cards: [card] as const,
  }))

  // Layout: Lands anchored near hand, creatures toward center
  // For player: lands at bottom (near hand), creatures above
  // For opponent: lands at top (near hand), creatures below
  const hasCreatures = groupedCreatures.length > 0
  const hasLands = groupedLands.length > 0
  const hasOther = groupedOther.length > 0
  const showDivider = (hasCreatures || hasOther) && hasLands

  return (
    <div
      style={{
        ...styles.battlefieldArea,
        justifyContent: isOpponent ? 'flex-start' : 'flex-end',
        gap: 0,
      }}
    >
      {/* For player: creatures first (top, toward center) */}
      {!isOpponent && (
        <>
          {/* Creatures row */}
          <div style={{
            ...styles.battlefieldRow,
            gap: responsive.cardGap,
            minHeight: responsive.battlefieldCardHeight + responsive.battlefieldRowPadding,
          }}>
            {groupedCreatures.map((group) => (
              <CardStack
                key={group.cardIds[0]}
                group={group}
                interactive={!spectatorMode && !isOpponent}
                isOpponentCard={isOpponent}
              />
            ))}
          </div>

          {/* Other permanents row */}
          {hasOther && (
            <div style={{ ...styles.battlefieldRow, gap: responsive.cardGap, marginTop: 4 }}>
              {groupedOther.map((group) => (
                <CardStack
                  key={group.cardIds[0]}
                  group={group}
                  interactive={!spectatorMode && !isOpponent}
                  isOpponentCard={isOpponent}
                />
              ))}
            </div>
          )}

          {/* Divider */}
          {showDivider && (
            <div
              style={{
                width: '40%',
                height: 1,
                backgroundColor: '#444',
                margin: '6px 0',
              }}
            />
          )}

          {/* Lands row (bottom, near hand) */}
          <div style={{
            ...styles.battlefieldRow,
            gap: responsive.cardGap,
            marginBottom: -40,
          }}>
            {groupedLands.map((group) => (
              <CardStack
                key={group.cardIds[0]}
                group={group}
                interactive={!spectatorMode && !isOpponent}
                isOpponentCard={isOpponent}
              />
            ))}
          </div>
        </>
      )}

      {/* For opponent: lands first (top, near hand) */}
      {isOpponent && (
        <>
          {/* Lands row (top, near hand) */}
          <div style={{
            ...styles.battlefieldRow,
            gap: responsive.cardGap,
            marginTop: -40,
          }}>
            {groupedLands.map((group) => (
              <CardStack
                key={group.cardIds[0]}
                group={group}
                interactive={!spectatorMode && !isOpponent}
                isOpponentCard={isOpponent}
              />
            ))}
          </div>

          {/* Divider */}
          {showDivider && (
            <div
              style={{
                width: '40%',
                height: 1,
                backgroundColor: '#444',
                margin: '6px 0',
              }}
            />
          )}

          {/* Other permanents row */}
          {hasOther && (
            <div style={{ ...styles.battlefieldRow, gap: responsive.cardGap, marginBottom: 4 }}>
              {groupedOther.map((group) => (
                <CardStack
                  key={group.cardIds[0]}
                  group={group}
                  interactive={!spectatorMode && !isOpponent}
                  isOpponentCard={isOpponent}
                />
              ))}
            </div>
          )}

          {/* Creatures row (bottom, toward center) */}
          <div style={{
            ...styles.battlefieldRow,
            gap: responsive.cardGap,
            minHeight: responsive.battlefieldCardHeight + responsive.battlefieldRowPadding,
          }}>
            {groupedCreatures.map((group) => (
              <CardStack
                key={group.cardIds[0]}
                group={group}
                interactive={!spectatorMode && !isOpponent}
                isOpponentCard={isOpponent}
              />
            ))}
          </div>
        </>
      )}
    </div>
  )
}

/**
 * Renders a group of identical cards as an overlapping stack.
 * Each card has its own data-card-id for targeting arrows.
 */
function CardStack({
  group,
  interactive,
  isOpponentCard,
}: {
  group: GroupedCard
  interactive: boolean
  isOpponentCard: boolean
}) {
  const responsive = useResponsiveContext()

  // For single cards, just render a normal GameCard
  if (group.count === 1) {
    return (
      <GameCard
        card={group.card}
        count={1}
        interactive={interactive}
        battlefield
        isOpponentCard={isOpponentCard}
      />
    )
  }

  // Calculate stack offset (how much each card is offset from the previous)
  const stackOffset = responsive.isMobile ? 12 : 18

  // Calculate total width needed for the stack
  // Use height for tapped cards since they rotate 90 degrees (visually wider)
  const hasAnyTapped = group.cards.some(c => c.isTapped)
  const cardWidth = hasAnyTapped ? responsive.battlefieldCardHeight : responsive.battlefieldCardWidth
  const totalWidth = cardWidth + stackOffset * (group.count - 1)
  const stackHeight = responsive.battlefieldCardHeight  // Always use full height for consistent alignment

  return (
    <div
      style={{
        position: 'relative',
        width: totalWidth,
        height: stackHeight,
        display: 'flex',
        alignItems: 'flex-end',
        transition: 'width 0.15s, height 0.15s',
      }}
    >
      {group.cards.map((card, index) => (
        <div
          key={card.id}
          style={{
            position: 'absolute',
            left: index * stackOffset,
            top: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'flex-end',
            zIndex: index,
          }}
        >
          <GameCard
            card={card}
            count={1}
            interactive={interactive}
            battlefield
            isOpponentCard={isOpponentCard}
          />
        </div>
      ))}
    </div>
  )
}

/**
 * Single card display.
 */
function GameCard({
  card,
  count = 1,
  faceDown = false,
  interactive = false,
  small = false,
  battlefield = false,
  overrideWidth,
  isOpponentCard = false,
  inHand = false,
}: {
  card: ClientCard
  count?: number
  faceDown?: boolean
  interactive?: boolean
  small?: boolean
  battlefield?: boolean
  overrideWidth?: number
  isOpponentCard?: boolean
  inHand?: boolean
}) {
  const selectCard = useGameStore((state) => state.selectCard)
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const hoverCard = useGameStore((state) => state.hoverCard)
  const targetingState = useGameStore((state) => state.targetingState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)
  const combatState = useGameStore((state) => state.combatState)
  const legalActions = useGameStore((state) => state.legalActions)
  const submitAction = useGameStore((state) => state.submitAction)
  const toggleAttacker = useGameStore((state) => state.toggleAttacker)
  const assignBlocker = useGameStore((state) => state.assignBlocker)
  const removeBlockerAssignment = useGameStore((state) => state.removeBlockerAssignment)
  const startDraggingBlocker = useGameStore((state) => state.startDraggingBlocker)
  const stopDraggingBlocker = useGameStore((state) => state.stopDraggingBlocker)
  const draggingBlockerId = useGameStore((state) => state.draggingBlockerId)
  const startDraggingCard = useGameStore((state) => state.startDraggingCard)
  const stopDraggingCard = useGameStore((state) => state.stopDraggingCard)
  const draggingCardId = useGameStore((state) => state.draggingCardId)
  const startTargeting = useGameStore((state) => state.startTargeting)
  const startXSelection = useGameStore((state) => state.startXSelection)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const submitTargetsDecision = useGameStore((state) => state.submitTargetsDecision)
  const responsive = useResponsiveContext()
  const { handleCardClick } = useInteraction()
  const dragStartPos = useRef<{ x: number; y: number } | null>(null)
  const handledByDrag = useRef(false)

  // Hover handlers for card preview
  const handleMouseEnter = useCallback(() => {
    if (!faceDown) {
      hoverCard(card.id)
    }
  }, [card.id, faceDown, hoverCard])

  const handleMouseLeave = useCallback(() => {
    hoverCard(null)
  }, [hoverCard])

  // Check if card has legal actions (is playable)
  const hasLegalActions = useHasLegalActions(card.id)

  const hoveredCardId = useGameStore((state) => state.hoveredCardId)

  const isSelected = selectedCardId === card.id
  const isHovered = hoveredCardId === card.id
  const isInTargetingMode = targetingState !== null
  const isValidTarget = targetingState?.validTargets.includes(card.id) ?? false
  const isSelectedTarget = targetingState?.selectedTargets.includes(card.id) ?? false

  // Check if this card is a valid target in a pending ChooseTargetsDecision
  const isChooseTargetsDecision = pendingDecision?.type === 'ChooseTargetsDecision'
  const decisionLegalTargets = isChooseTargetsDecision ? (pendingDecision.legalTargets[0] ?? []) : []
  const isValidDecisionTarget = decisionLegalTargets.includes(card.id)

  // Combat mode checks
  const isInAttackerMode = combatState?.mode === 'declareAttackers'
  const isInBlockerMode = combatState?.mode === 'declareBlockers'
  const isInCombatMode = isInAttackerMode || isInBlockerMode

  // For attacker mode: check if this is a valid attacker (own creature, untapped, no summoning sickness)
  const isOwnCreature = !isOpponentCard && card.cardTypes.includes('CREATURE')
  const isValidAttacker = isInAttackerMode && isOwnCreature && !card.isTapped && combatState.validCreatures.includes(card.id)
  const isSelectedAsAttacker = isInAttackerMode && combatState.selectedAttackers.includes(card.id)

  // For blocker mode: check if this is a valid blocker or an attacking creature to block
  const isValidBlocker = isInBlockerMode && isOwnCreature && !card.isTapped && combatState.validCreatures.includes(card.id)
  const isSelectedAsBlocker = isInBlockerMode && !!combatState?.blockerAssignments[card.id]
  const isAttackingInBlockerMode = isInBlockerMode && isOpponentCard && combatState.attackingCreatures.includes(card.id)
  const isMustBeBlocked = isInBlockerMode && isOpponentCard && combatState.mustBeBlockedAttackers.includes(card.id)

  // Only show playable highlight outside of combat mode (and when not targeting)
  const isPlayable = interactive && hasLegalActions && !faceDown && !isInCombatMode

  const cardImageUrl = faceDown
    ? 'https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389'
    : getCardImageUrl(card.name, card.imageUri, 'normal')

  // Use responsive sizes, but allow override for fitting cards in hand
  const baseWidth = small
    ? responsive.smallCardWidth
    : battlefield
      ? responsive.battlefieldCardWidth
      : responsive.cardWidth
  const width = overrideWidth ?? baseWidth
  const cardRatio = 1.4
  const height = Math.round(width * cardRatio)

  // Check if this card can be played/cast (for drag-to-play)
  const playableAction = legalActions.find((a) => {
    const action = a.action
    return (action.type === 'PlayLand' && action.cardId === card.id) ||
           (action.type === 'CastSpell' && action.cardId === card.id)
  })
  const canDragToPlay = inHand && playableAction && !isInCombatMode

  // Handle mouse down - start dragging for blockers or hand cards
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    if (isInBlockerMode && isValidBlocker && !isSelectedAsBlocker) {
      e.preventDefault()
      startDraggingBlocker(card.id)
      return
    }
    // Start dragging card from hand
    if (canDragToPlay) {
      e.preventDefault()
      dragStartPos.current = { x: e.clientX, y: e.clientY }
      startDraggingCard(card.id)
    }
  }, [isInBlockerMode, isValidBlocker, isSelectedAsBlocker, startDraggingBlocker, canDragToPlay, startDraggingCard, card.id])

  // Handle mouse up - drop blocker on attacker or cancel drag
  const handleMouseUp = useCallback(() => {
    if (isInBlockerMode && draggingBlockerId && isAttackingInBlockerMode) {
      // Dropping on an attacker - assign the blocker
      assignBlocker(draggingBlockerId, card.id)
      stopDraggingBlocker()
    }
  }, [isInBlockerMode, draggingBlockerId, isAttackingInBlockerMode, assignBlocker, stopDraggingBlocker, card.id])

  // Global mouse up handler for card dragging (to detect drop outside hand)
  useEffect(() => {
    if (draggingCardId !== card.id) return

    const handleGlobalMouseUp = (e: MouseEvent) => {
      // Require minimum drag distance to prevent accidental casts
      const MIN_DRAG_DISTANCE = 30
      const start = dragStartPos.current
      const draggedFarEnough = start != null &&
        Math.hypot(e.clientX - start.x, e.clientY - start.y) >= MIN_DRAG_DISTANCE
      dragStartPos.current = null

      // Check if dropped outside the hand area - if so, play the card
      const handEl = document.querySelector('[data-zone="hand"]')
      let isOverHand = false

      if (handEl) {
        const rect = handEl.getBoundingClientRect()
        isOverHand = e.clientX >= rect.left && e.clientX <= rect.right &&
                     e.clientY >= rect.top && e.clientY <= rect.bottom
      }

      // Mark that drag handled this interaction to prevent duplicate click
      handledByDrag.current = true

      if (!draggedFarEnough) {
        // Short drag = click: open action menu
        stopDraggingCard()
        handleCardClick(card.id)
        return
      }

      if (!isOverHand && playableAction) {
        // Check if spell has X cost - needs X selection first
        if (playableAction.hasXCost) {
          startXSelection({
            actionInfo: playableAction,
            cardName: playableAction.description.replace('Cast ', ''),
            minX: playableAction.minX ?? 0,
            maxX: playableAction.maxAffordableX ?? 0,
            selectedX: playableAction.maxAffordableX ?? 0,
          })
        } else if (playableAction.action.type === 'CastSpell' && playableAction.additionalCostInfo?.costType === 'SacrificePermanent') {
          // Check if spell requires sacrifice as additional cost
          const costInfo = playableAction.additionalCostInfo
          startTargeting({
            action: playableAction.action,
            validTargets: [...(costInfo.validSacrificeTargets ?? [])],
            selectedTargets: [],
            minTargets: costInfo.sacrificeCount ?? 1,
            maxTargets: costInfo.sacrificeCount ?? 1,
            isSacrificeSelection: true,
            pendingActionInfo: playableAction,
          })
        } else if (playableAction.requiresTargets && playableAction.validTargets && playableAction.validTargets.length > 0) {
          // Check if action requires targeting
          // Enter targeting mode
          startTargeting({
            action: playableAction.action,
            validTargets: playableAction.validTargets,
            selectedTargets: [],
            minTargets: playableAction.minTargets ?? playableAction.targetCount ?? 1,
            maxTargets: playableAction.targetCount ?? 1,
          })
        } else {
          // Play the card directly
          submitAction(playableAction.action)
        }
      }
      stopDraggingCard()
    }

    window.addEventListener('mouseup', handleGlobalMouseUp)
    return () => window.removeEventListener('mouseup', handleGlobalMouseUp)
  }, [draggingCardId, card.id, playableAction, submitAction, stopDraggingCard, startTargeting, startXSelection, handleCardClick])

  // Global mouse up handler to cancel drag
  useEffect(() => {
    if (!draggingBlockerId) return

    const handleGlobalMouseUp = () => {
      stopDraggingBlocker()
    }

    window.addEventListener('mouseup', handleGlobalMouseUp)
    return () => window.removeEventListener('mouseup', handleGlobalMouseUp)
  }, [draggingBlockerId, stopDraggingBlocker])

  const handleClick = () => {
    // If the drag handler already processed this interaction, skip
    if (handledByDrag.current) {
      handledByDrag.current = false
      return
    }

    // Handle targeting mode clicks - click to select, click again to unselect
    if (isInTargetingMode) {
      if (isSelectedTarget) {
        removeTarget(card.id)
        return
      }
      if (isValidTarget) {
        addTarget(card.id)
        return
      }
    }

    // Handle pending ChooseTargetsDecision clicks
    if (isChooseTargetsDecision && isValidDecisionTarget) {
      submitTargetsDecision({ 0: [card.id] })
      return
    }

    // Handle attacker mode clicks
    if (isInAttackerMode) {
      if (isValidAttacker) {
        toggleAttacker(card.id)
      }
      return
    }

    // Handle blocker mode clicks - clicking an assigned blocker removes it
    if (isInBlockerMode) {
      if (isValidBlocker && isSelectedAsBlocker) {
        removeBlockerAssignment(card.id)
        return
      }
      // Clicking is also handled by mouseup for drag & drop
      return
    }

    // Normal card selection (outside combat mode)
    // Use handleCardClick which handles X cost spells, targeting, and single-action auto-execute
    if (interactive && !isInTargetingMode) {
      if (isSelected) {
        selectCard(null)
      } else {
        handleCardClick(card.id)
      }
    }
  }

  // Determine border color based on state
  // Priority: attacking > blocking > selected > validTarget > validAttacker/Blocker > playable > default
  let borderStyle = '2px solid #333'
  let boxShadow = '0 2px 8px rgba(0,0,0,0.5)'

  if (isSelectedAsAttacker) {
    // Red for attacking creatures
    borderStyle = '3px solid #ff4444'
    boxShadow = '0 0 16px rgba(255, 68, 68, 0.7), 0 0 32px rgba(255, 68, 68, 0.4)'
  } else if (isSelectedAsBlocker) {
    // Blue for blocking creatures
    borderStyle = '3px solid #4488ff'
    boxShadow = '0 0 16px rgba(68, 136, 255, 0.7), 0 0 32px rgba(68, 136, 255, 0.4)'
  } else if (isMustBeBlocked) {
    // Red pulsing glow for must-be-blocked attackers (Alluring Scent)
    borderStyle = '3px solid #ff3333'
    boxShadow = '0 0 16px rgba(255, 51, 51, 0.8), 0 0 32px rgba(255, 51, 51, 0.5), 0 0 48px rgba(255, 51, 51, 0.3)'
  } else if (isAttackingInBlockerMode) {
    // Orange glow for attackers that can be blocked
    borderStyle = '3px solid #ff8800'
    boxShadow = '0 0 12px rgba(255, 136, 0, 0.6), 0 0 24px rgba(255, 136, 0, 0.3)'
  } else if (isSelectedTarget) {
    // Yellow highlight for selected targets (already chosen in targeting mode)
    borderStyle = '3px solid #ffff00'
    boxShadow = '0 0 20px rgba(255, 255, 0, 0.8), 0 0 40px rgba(255, 255, 0, 0.4)'
  } else if (isSelected && !isInCombatMode) {
    borderStyle = '3px solid #ffff00'
    boxShadow = '0 8px 20px rgba(255, 255, 0, 0.4)'
  } else if ((isValidTarget || isValidDecisionTarget) && isHovered) {
    // Bright highlight when hovering over a valid target
    borderStyle = '3px solid #ff6666'
    boxShadow = '0 0 20px rgba(255, 100, 100, 0.9), 0 0 40px rgba(255, 68, 68, 0.6)'
  } else if (isValidTarget || isValidDecisionTarget) {
    borderStyle = '3px solid #ff4444'
    boxShadow = '0 4px 15px rgba(255, 68, 68, 0.6)'
  } else if ((isValidAttacker || isValidBlocker) && isHovered) {
    // Bright highlight when hovering over a valid attacker/blocker
    borderStyle = '3px solid #44ff44'
    boxShadow = '0 0 20px rgba(68, 255, 68, 0.9), 0 0 40px rgba(0, 255, 0, 0.5)'
  } else if (isValidAttacker || isValidBlocker) {
    // Green highlight for valid attackers/blockers
    borderStyle = '2px solid #00ff00'
    boxShadow = '0 0 12px rgba(0, 255, 0, 0.5), 0 0 24px rgba(0, 255, 0, 0.3)'
  } else if (isPlayable && isHovered) {
    // Bright highlight when hovering over a playable card
    borderStyle = '3px solid #44ff44'
    boxShadow = '0 0 20px rgba(68, 255, 68, 0.9), 0 0 40px rgba(0, 255, 0, 0.5)'
  } else if (isPlayable) {
    // Green highlight for playable cards
    borderStyle = '2px solid #00ff00'
    boxShadow = '0 0 12px rgba(0, 255, 0, 0.5), 0 0 24px rgba(0, 255, 0, 0.3)'
  }

  // Determine cursor
  const canInteract = interactive || isValidTarget || isValidDecisionTarget || isValidAttacker || isValidBlocker || isAttackingInBlockerMode || canDragToPlay
  const baseCursor = canInteract ? 'pointer' : 'default'
  const cursor = (isValidBlocker && !isSelectedAsBlocker) || canDragToPlay ? 'grab' : baseCursor

  // Check if currently being dragged (blocker or hand card)
  const isBeingDragged = draggingBlockerId === card.id || draggingCardId === card.id

  // Container dimensions - expand width when tapped to prevent overlap
  // Tapped cards rotate 90deg, so they need width = height to not overlap
  const containerWidth = card.isTapped && battlefield ? height + 8 : width
  const containerHeight = height

  const cardElement = (
    <div
      data-card-id={card.id}
      onClick={handleClick}
      onMouseDown={handleMouseDown}
      onMouseUp={handleMouseUp}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      style={{
        ...styles.card,
        width,
        height,
        borderRadius: responsive.isMobile ? 4 : 8,
        cursor,
        border: borderStyle,
        transform: `${card.isTapped ? 'rotate(90deg)' : ''} ${isSelected && !isInCombatMode ? 'translateY(-8px)' : ''}`,
        transformOrigin: 'center',
        boxShadow,
        opacity: isBeingDragged ? 0.6 : 1,
        userSelect: 'none',
      }}
    >
      <img
        src={cardImageUrl}
        alt={faceDown ? 'Card back' : card.name}
        style={styles.cardImage}
        onError={(e) => {
          const img = e.currentTarget
          const fallbackUrl = getScryfallFallbackUrl(card.name, 'normal')
          // Try Scryfall API fallback if not already using it
          if (!faceDown && !img.src.includes('api.scryfall.com')) {
            img.src = fallbackUrl
          } else {
            // Both imageUri and Scryfall failed, show text fallback
            img.style.display = 'none'
            const fallback = img.nextElementSibling as HTMLElement
            if (fallback) fallback.style.display = 'flex'
          }
        }}
      />
      {/* Fallback when image fails */}
      <div style={styles.cardFallback}>
        <span style={{ ...styles.cardName, fontSize: responsive.fontSize.small }}>{faceDown ? '' : card.name}</span>
        {!faceDown && card.power !== null && card.toughness !== null && (
          <span style={{
            ...styles.cardPT,
            fontSize: responsive.fontSize.normal,
            color: getPTColor(card.power, card.toughness, card.basePower, card.baseToughness)
          }}>
            {card.power}/{card.toughness}
          </span>
        )}
      </div>

      {/* Tapped indicator */}
      {card.isTapped && (
        <div style={styles.tappedOverlay} />
      )}

      {/* Summoning sickness indicator */}
      {battlefield && card.hasSummoningSickness && card.cardTypes.includes('CREATURE') && (
        <div style={styles.summoningSicknessOverlay}>
          <div style={{ ...styles.summoningSicknessIcon, fontSize: responsive.isMobile ? 16 : 24 }}>💤</div>
        </div>
      )}

      {/* P/T overlay for creatures on battlefield */}
      {battlefield && !faceDown && card.power !== null && card.toughness !== null && (
        <div style={{
          ...styles.ptOverlay,
          backgroundColor: getPTColor(card.power, card.toughness, card.basePower, card.baseToughness) !== 'white'
            ? 'rgba(0, 0, 0, 0.85)'
            : 'rgba(0, 0, 0, 0.7)',
        }}>
          <span style={{
            color: getPTColor(card.power, card.toughness, card.basePower, card.baseToughness),
            fontWeight: 700,
            fontSize: responsive.isMobile ? 10 : 12,
          }}>
            {card.power}/
          </span>
          <span style={{
            color: card.damage != null && card.damage > 0
              ? '#ff4444'
              : getPTColor(card.power, card.toughness, card.basePower, card.baseToughness),
            fontWeight: 700,
            fontSize: responsive.isMobile ? 10 : 12,
          }}>
            {card.damage != null && card.damage > 0
              ? card.toughness - card.damage
              : card.toughness}
          </span>
        </div>
      )}

      {/* Counter badge for creatures with +1/+1 or -1/-1 counters */}
      {battlefield && !faceDown && hasStatCounters(card) && (
        <div style={{
          ...styles.counterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <span style={styles.counterBadgeIcon}>⬡</span>
          <span style={styles.counterBadgeText}>
            {getCounterStatModifier(card) >= 0 ? '+' : ''}{getCounterStatModifier(card)}
          </span>
        </div>
      )}

      {/* Keyword ability icons */}
      {battlefield && !faceDown && card.keywords.length > 0 && (
        <KeywordIcons keywords={card.keywords} size={responsive.isMobile ? 14 : 18} />
      )}

      {/* Active effect badges (evasion, etc.) */}
      {battlefield && !faceDown && card.activeEffects && card.activeEffects.length > 0 && (
        <ActiveEffectBadges effects={card.activeEffects} />
      )}

      {/* Playable indicator glow effect (only outside combat mode) */}
      {isPlayable && !isSelected && (
        <div style={styles.playableGlow} />
      )}

      {/* Count badge for grouped cards */}
      {count > 1 && !faceDown && (
        <div style={{
          position: 'absolute',
          top: responsive.isMobile ? 2 : 4,
          right: responsive.isMobile ? 2 : 4,
          backgroundColor: 'rgba(0, 0, 0, 0.85)',
          color: '#fff',
          borderRadius: '50%',
          width: responsive.isMobile ? 18 : 22,
          height: responsive.isMobile ? 18 : 22,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: responsive.isMobile ? 10 : 12,
          fontWeight: 700,
          border: '1px solid rgba(255, 255, 255, 0.3)',
          boxShadow: '0 2px 4px rgba(0,0,0,0.5)',
          zIndex: 10,
        }}>
          {count}
        </div>
      )}
    </div>
  )

  // Wrap in container for tapped battlefield cards to prevent overlap
  if (card.isTapped && battlefield) {
    return (
      <div style={{
        width: containerWidth,
        height: containerHeight,
        display: 'flex',
        alignItems: 'flex-end',
        justifyContent: 'center',
        transition: 'width 0.15s, height 0.15s',
      }}>
        {cardElement}
      </div>
    )
  }

  return cardElement
}

/**
 * Graveyard targeting overlay - shows when targeting mode requires selecting cards from graveyards.
 * Similar to GraveyardTargetingUI in DecisionUI but for client-side spell casting targeting.
 */
function GraveyardTargetingOverlay({
  graveyardCards,
  targetingState,
  responsive,
  onSelect,
  onDeselect,
  onConfirm,
  onCancel,
}: {
  graveyardCards: ClientCard[]
  targetingState: { selectedTargets: readonly EntityId[]; minTargets: number; maxTargets: number }
  responsive: ResponsiveSizes
  onSelect: (cardId: EntityId) => void
  onDeselect: (cardId: EntityId) => void
  onConfirm: () => void
  onCancel: () => void
}) {
  const hoverCard = useGameStore((s) => s.hoverCard)
  const gameState = useGameStore((s) => s.gameState)
  const viewingPlayerId = gameState?.viewingPlayerId

  const selectedCount = targetingState.selectedTargets.length
  const minTargets = targetingState.minTargets
  const maxTargets = targetingState.maxTargets
  const hasEnoughTargets = selectedCount >= minTargets
  const hasMaxTargets = selectedCount >= maxTargets

  // Group cards by graveyard owner
  const cardsByOwner = React.useMemo(() => {
    const grouped = new Map<EntityId, ClientCard[]>()
    for (const card of graveyardCards) {
      const ownerId = card.zone?.ownerId ?? card.ownerId
      if (!grouped.has(ownerId)) {
        grouped.set(ownerId, [])
      }
      grouped.get(ownerId)!.push(card)
    }
    return grouped
  }, [graveyardCards])

  // Get owner IDs sorted (viewer's graveyard first)
  const ownerIds = React.useMemo(() => {
    const ids = Array.from(cardsByOwner.keys())
    return ids.sort((a, b) => {
      if (a === viewingPlayerId) return -1
      if (b === viewingPlayerId) return 1
      return 0
    })
  }, [cardsByOwner, viewingPlayerId])

  const [selectedOwnerId, setSelectedOwnerId] = React.useState<EntityId | null>(() => ownerIds[0] ?? null)
  const currentOwnerId = selectedOwnerId && ownerIds.includes(selectedOwnerId) ? selectedOwnerId : ownerIds[0] ?? null
  const currentCards = currentOwnerId ? (cardsByOwner.get(currentOwnerId) ?? []) : []

  // Sort cards by type then name
  const sortedCards = React.useMemo(() => {
    return [...currentCards].sort((a, b) => {
      const typeOrder = (typeLine?: string) => {
        if (!typeLine) return 5
        const lower = typeLine.toLowerCase()
        if (lower.includes('land')) return 0
        if (lower.includes('creature')) return 1
        if (lower.includes('instant')) return 2
        if (lower.includes('sorcery')) return 3
        return 4
      }
      const typeCompare = typeOrder(a.typeLine) - typeOrder(b.typeLine)
      if (typeCompare !== 0) return typeCompare
      return a.name.localeCompare(b.name)
    })
  }, [currentCards])

  const getPlayerLabel = (ownerId: EntityId): string => {
    if (ownerId === viewingPlayerId) return 'Your Graveyard'
    const player = gameState?.players.find((p) => p.playerId === ownerId)
    return player ? `${player.name}'s Graveyard` : "Opponent's Graveyard"
  }

  const toggleCard = (cardId: EntityId) => {
    if (targetingState.selectedTargets.includes(cardId)) {
      onDeselect(cardId)
    } else if (selectedCount < maxTargets) {
      onSelect(cardId)
    }
  }

  const gap = responsive.isMobile ? 8 : 12
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const maxCardWidth = responsive.isMobile ? 100 : 140
  const cardWidth = calculateFittingCardWidth(
    Math.min(sortedCards.length, 8),
    availableWidth,
    gap,
    maxCardWidth,
    60
  )

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.92)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: responsive.isMobile ? 12 : 20,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
        zIndex: 1000,
      }}
    >
      {/* Header */}
      <div style={{ textAlign: 'center' }}>
        <h2
          style={{
            color: 'white',
            margin: 0,
            fontSize: responsive.isMobile ? 20 : 28,
            fontWeight: 600,
          }}
        >
          Choose Target from Graveyard
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          Select {minTargets === maxTargets ? minTargets : `${minTargets}-${maxTargets}`} target{maxTargets > 1 ? 's' : ''}
        </p>
      </div>

      {/* Graveyard tabs (if multiple graveyards) */}
      {ownerIds.length > 1 && (
        <div
          style={{
            display: 'flex',
            gap: responsive.isMobile ? 8 : 12,
            backgroundColor: 'rgba(0, 0, 0, 0.4)',
            padding: 4,
            borderRadius: 8,
          }}
        >
          {ownerIds.map((ownerId) => {
            const isActive = ownerId === currentOwnerId
            const ownerCards = cardsByOwner.get(ownerId) ?? []
            const cardCount = ownerCards.length
            // Count how many cards are selected from this graveyard
            const selectedFromThisGraveyard = ownerCards.filter((c) =>
              targetingState.selectedTargets.includes(c.id)
            ).length
            return (
              <button
                key={ownerId}
                onClick={() => setSelectedOwnerId(ownerId)}
                style={{
                  padding: responsive.isMobile ? '8px 16px' : '10px 24px',
                  fontSize: responsive.fontSize.normal,
                  backgroundColor: isActive ? '#4a5568' : 'transparent',
                  color: isActive ? 'white' : '#888',
                  border: selectedFromThisGraveyard > 0 && !isActive ? '2px solid #fbbf24' : 'none',
                  borderRadius: 6,
                  cursor: 'pointer',
                  fontWeight: isActive ? 600 : 400,
                  transition: 'all 0.15s',
                  position: 'relative',
                }}
              >
                {getPlayerLabel(ownerId)} ({cardCount})
                {selectedFromThisGraveyard > 0 && (
                  <span
                    style={{
                      position: 'absolute',
                      top: -6,
                      right: -6,
                      backgroundColor: '#fbbf24',
                      color: '#1a1a1a',
                      borderRadius: '50%',
                      width: 20,
                      height: 20,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 12,
                      fontWeight: 'bold',
                    }}
                  >
                    {selectedFromThisGraveyard}
                  </span>
                )}
              </button>
            )
          })}
        </div>
      )}

      {/* Selection counter */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 16,
          color: '#888',
          fontSize: responsive.fontSize.normal,
        }}
      >
        <span>
          Selected:{' '}
          <span
            style={{
              color: hasEnoughTargets ? '#4ade80' : selectedCount > 0 ? '#fbbf24' : '#888',
              fontWeight: 600,
            }}
          >
            {selectedCount}
          </span>
          {' / '}
          {maxTargets}
        </span>
      </div>

      {/* Card ribbon */}
      <div
        style={{
          display: 'flex',
          gap,
          padding: responsive.isMobile ? 12 : 24,
          justifyContent: sortedCards.length <= 6 ? 'center' : 'flex-start',
          overflowX: 'auto',
          maxWidth: '100%',
          scrollBehavior: 'smooth',
        }}
      >
        {sortedCards.map((card) => {
          const isSelected = targetingState.selectedTargets.includes(card.id)
          const cardImageUrl = getCardImageUrl(card.name, card.imageUri)
          const cardHeight = Math.round(cardWidth * 1.4)

          return (
            <div
              key={card.id}
              onClick={() => toggleCard(card.id)}
              onMouseEnter={() => hoverCard(card.id)}
              onMouseLeave={() => hoverCard(null)}
              style={{
                width: cardWidth,
                height: cardHeight,
                backgroundColor: isSelected ? '#1a3320' : '#1a1a1a',
                border: isSelected ? '3px solid #fbbf24' : '2px solid #333',
                borderRadius: responsive.isMobile ? 6 : 10,
                display: 'flex',
                flexDirection: 'column',
                overflow: 'hidden',
                cursor: hasMaxTargets && !isSelected ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s ease-out',
                transform: isSelected ? 'translateY(-12px) scale(1.05)' : 'none',
                boxShadow: isSelected
                  ? '0 12px 28px rgba(251, 191, 36, 0.4), 0 0 20px rgba(251, 191, 36, 0.2)'
                  : '0 4px 12px rgba(0, 0, 0, 0.6)',
                flexShrink: 0,
                position: 'relative',
                opacity: hasMaxTargets && !isSelected ? 0.5 : 1,
              }}
            >
              <img
                src={cardImageUrl}
                alt={card.name}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                onError={(e) => handleImageError(e, card.name, 'normal')}
              />
              {isSelected && (
                <div
                  style={{
                    position: 'absolute',
                    top: 6,
                    right: 6,
                    width: 24,
                    height: 24,
                    backgroundColor: '#fbbf24',
                    borderRadius: '50%',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: '#1a1a1a',
                    fontWeight: 'bold',
                    fontSize: 14,
                    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.4)',
                  }}
                >
                  &#10003;
                </div>
              )}
            </div>
          )
        })}
      </div>

      {/* No cards message */}
      {sortedCards.length === 0 && (
        <p style={{ color: '#666', fontSize: responsive.fontSize.normal }}>
          No valid targets in this graveyard.
        </p>
      )}

      {/* Buttons */}
      <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
        <button
          onClick={onConfirm}
          disabled={!hasEnoughTargets}
          style={{
            padding: responsive.isMobile ? '10px 24px' : '12px 36px',
            fontSize: responsive.fontSize.large,
            backgroundColor: hasEnoughTargets ? '#16a34a' : '#333',
            color: hasEnoughTargets ? 'white' : '#666',
            border: 'none',
            borderRadius: 8,
            cursor: hasEnoughTargets ? 'pointer' : 'not-allowed',
            fontWeight: 600,
            transition: 'all 0.15s',
          }}
        >
          Confirm Target
        </button>
        <button
          onClick={onCancel}
          style={{
            padding: responsive.isMobile ? '10px 24px' : '12px 36px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#444',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            transition: 'all 0.15s',
          }}
        >
          Cancel
        </button>
      </div>
    </div>
  )
}

/**
 * Action menu that appears when a card with legal actions is selected.
 * Handles targeting mode for blocking (select blocker, then click attacker).
 */
function ActionMenu() {
  const selectedCardId = useGameStore((state) => state.selectedCardId)
  const legalActions = useGameStore((state) => state.legalActions)
  const submitAction = useGameStore((state) => state.submitAction)
  const selectCard = useGameStore((state) => state.selectCard)
  const targetingState = useGameStore((state) => state.targetingState)
  const cancelTargeting = useGameStore((state) => state.cancelTargeting)
  const confirmTargeting = useGameStore((state) => state.confirmTargeting)
  const startTargeting = useGameStore((state) => state.startTargeting)
  const startXSelection = useGameStore((state) => state.startXSelection)
  const responsive = useResponsiveContext()

  const gameState = useGameStore((state) => state.gameState)
  const addTarget = useGameStore((state) => state.addTarget)
  const removeTarget = useGameStore((state) => state.removeTarget)

  // If in targeting mode, show targeting UI instead
  if (targetingState) {
    const selectedCount = targetingState.selectedTargets.length
    const minTargets = targetingState.minTargets
    const maxTargets = targetingState.maxTargets
    const hasEnoughTargets = selectedCount >= minTargets
    const hasMaxTargets = selectedCount >= maxTargets
    const isSacrifice = targetingState.isSacrificeSelection

    // Check if all valid targets are graveyard cards
    const graveyardCards: ClientCard[] = []
    let allTargetsAreGraveyard = targetingState.validTargets.length > 0
    for (const targetId of targetingState.validTargets) {
      const card = gameState?.cards[targetId]
      if (card && card.zone?.zoneType === 'GRAVEYARD') {
        graveyardCards.push(card)
      } else {
        allTargetsAreGraveyard = false
        break
      }
    }

    // If all targets are graveyard cards, show graveyard selection UI
    if (allTargetsAreGraveyard && graveyardCards.length > 0) {
      return (
        <GraveyardTargetingOverlay
          graveyardCards={graveyardCards}
          targetingState={targetingState}
          responsive={responsive}
          onSelect={addTarget}
          onDeselect={removeTarget}
          onConfirm={confirmTargeting}
          onCancel={cancelTargeting}
        />
      )
    }

    // Build the target count display
    const targetDisplay = minTargets === maxTargets
      ? `${selectedCount}/${maxTargets}`
      : `${selectedCount} (${minTargets}-${maxTargets})`

    // Build the prompt text based on selection type
    const promptText = isSacrifice
      ? `Select creature to sacrifice (${targetDisplay})`
      : `Select targets (${targetDisplay})`

    const hintText = hasMaxTargets
      ? isSacrifice ? 'Creature selected' : 'Maximum targets selected'
      : hasEnoughTargets
        ? 'Click Confirm or select more'
        : isSacrifice ? 'Click a creature you control' : 'Click a highlighted target'

    return (
      <div style={{
        ...styles.targetingOverlay,
        padding: responsive.isMobile ? '12px 16px' : '16px 24px',
        borderColor: isSacrifice ? '#ff8800' : '#ff4444',
      }}>
        <div style={{
          ...styles.targetingPrompt,
          fontSize: responsive.fontSize.normal,
          color: isSacrifice ? '#ff8800' : '#ff4444',
        }}>
          {promptText}
        </div>
        <div style={{ color: '#aaa', fontSize: responsive.fontSize.small, marginTop: 4 }}>
          {hintText}
        </div>
        <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
          {hasEnoughTargets && (
            <button onClick={confirmTargeting} style={{
              ...styles.actionButton,
              padding: responsive.isMobile ? '8px 12px' : '10px 16px',
              fontSize: responsive.fontSize.normal,
            }}>
              Confirm ({selectedCount})
            </button>
          )}
          <button onClick={cancelTargeting} style={{
            ...styles.cancelButton,
            padding: responsive.isMobile ? '8px 12px' : '10px 16px',
            fontSize: responsive.fontSize.normal,
          }}>
            Cancel
          </button>
        </div>
      </div>
    )
  }

  if (!selectedCardId) return null

  // Filter legal actions for the selected card
  const cardActions = legalActions.filter((info) => {
    const action = info.action
    switch (action.type) {
      case 'PlayLand':
        return action.cardId === selectedCardId
      case 'CastSpell':
        return action.cardId === selectedCardId
      case 'ActivateAbility':
        return action.sourceId === selectedCardId
      default:
        return false
    }
  })

  if (cardActions.length === 0) return null

  const handleActionClick = (info: LegalActionInfo) => {
    // Check if spell has X cost - needs X selection first
    if (info.action.type === 'CastSpell' && info.hasXCost) {
      startXSelection({
        actionInfo: info,
        cardName: info.description.replace('Cast ', ''),
        minX: info.minX ?? 0,
        maxX: info.maxAffordableX ?? 0,
        selectedX: info.maxAffordableX ?? 0,
      })
      selectCard(null)
      return
    }

    // Check if spell requires sacrifice as additional cost
    if (info.action.type === 'CastSpell' && info.additionalCostInfo?.costType === 'SacrificePermanent') {
      const costInfo = info.additionalCostInfo
      startTargeting({
        action: info.action,
        validTargets: [...(costInfo.validSacrificeTargets ?? [])],
        selectedTargets: [],
        minTargets: costInfo.sacrificeCount ?? 1,
        maxTargets: costInfo.sacrificeCount ?? 1,
        isSacrificeSelection: true,
        pendingActionInfo: info,
      })
      selectCard(null)
      return
    }

    // Check if action requires targeting
    if (info.requiresTargets && info.validTargets && info.validTargets.length > 0) {
      // Enter targeting mode
      startTargeting({
        action: info.action,
        validTargets: info.validTargets,
        selectedTargets: [],
        minTargets: info.minTargets ?? info.targetCount ?? 1,
        maxTargets: info.targetCount ?? 1,
      })
      selectCard(null)
    } else {
      // Submit action directly
      submitAction(info.action)
      selectCard(null)
    }
  }

  return (
    <div style={styles.actionMenuOverlay} onClick={() => selectCard(null)}>
      <div style={{
        ...styles.actionMenu,
        padding: responsive.isMobile ? 12 : 16,
        minWidth: responsive.isMobile ? 160 : 200,
      }} onClick={(e) => e.stopPropagation()}>
        <div style={{
          ...styles.actionMenuTitle,
          fontSize: responsive.fontSize.small,
        }}>Actions</div>

        {/* Card actions */}
        {cardActions.map((info, index) => (
          <button
            key={index}
            onClick={() => handleActionClick(info)}
            style={{
              ...styles.actionButton,
              padding: responsive.isMobile ? '10px 12px' : '12px 16px',
              fontSize: responsive.fontSize.normal,
              display: 'flex',
              alignItems: 'center',
              gap: 4,
            }}
          >
            <AbilityText text={info.description} size={responsive.isMobile ? 14 : 16} />
            {info.requiresTargets && <span style={{ color: '#888', marginLeft: 8 }}>(select target)</span>}
          </button>
        ))}

        <button
          onClick={() => selectCard(null)}
          style={{
            ...styles.cancelButton,
            padding: responsive.isMobile ? '8px 12px' : '10px 16px',
            fontSize: responsive.fontSize.normal,
          }}
        >
          Cancel
        </button>
      </div>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: '#0a0a15',
    overflow: 'hidden',
  },
  opponentArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'flex-start',
    flexShrink: 1,
    flexGrow: 0,
    minHeight: 0,
    maxHeight: '36vh',
    marginBottom: 8,
    paddingBottom: 8,
  },
  centerArea: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    padding: '2px 8px',
    flex: 1,
    gap: 16,
    width: '100%',
  },
  centerLifeSection: {
    display: 'flex',
    alignItems: 'center',
    gap: 6,
  },
  playerNameWithLabel: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 1,
  },
  playerLabel: {
    color: '#555',
    fontStyle: 'italic',
  },
  handWithMana: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    justifyContent: 'center',
  },
  floatingPassButton: {
    position: 'fixed',
    bottom: 16,
    right: 16,
    fontWeight: 600,
    backgroundColor: '#e67e22',
    color: 'white',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
    zIndex: 100,
  },
  combatButtonContainer: {
    position: 'fixed',
    bottom: 16,
    right: 16,
    display: 'flex',
    gap: 8,
    zIndex: 100,
  },
  combatButton: {
    padding: '12px 24px',
    fontWeight: 600,
    color: 'white',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
    fontSize: 14,
  },
  combatButtonPrimary: {
    backgroundColor: '#c62828',
    border: '2px solid #ef5350',
  },
  combatButtonSecondary: {
    backgroundColor: '#424242',
    border: '2px solid #757575',
  },
  playerArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'flex-end',
    flexShrink: 1,
    flexGrow: 0,
    minHeight: 0,
    maxHeight: '46vh',
    marginTop: 'auto',
    paddingBottom: 0,
  },
  playerRowWithZones: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    width: '100%',
    justifyContent: 'center',
    minHeight: 0, // Allow vertical shrinking
  },
  playerMainArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'flex-end',
    gap: 0,
    flex: 1,
    minWidth: 0, // Allow shrinking
    minHeight: 0, // Allow vertical shrinking
  },
  zonePile: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  zoneStack: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 2,
  },
  deckPile: {
    position: 'relative',
    overflow: 'hidden',
    boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
  },
  graveyardPile: {
    position: 'relative',
    overflow: 'hidden',
    boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
    backgroundColor: '#1a1a2e',
  },
  pileImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  emptyPile: {
    width: '100%',
    height: '100%',
    backgroundColor: '#1a1a2e',
    border: '2px dashed #333',
    borderRadius: 6,
  },
  pileCount: {
    position: 'absolute',
    bottom: 4,
    right: 4,
    backgroundColor: 'rgba(0,0,0,0.8)',
    color: 'white',
    fontSize: 12,
    fontWeight: 700,
    padding: '2px 6px',
    borderRadius: 4,
  },
  zoneLabel: {
    color: '#666',
    fontSize: 10,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  graveyardOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.85)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 2000,
  } as React.CSSProperties,
  graveyardBrowserContent: {
    maxWidth: '90vw',
    maxHeight: '80vh',
    display: 'flex',
    flexDirection: 'column',
    gap: 16,
  } as React.CSSProperties,
  graveyardBrowserHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  } as React.CSSProperties,
  graveyardBrowserTitle: {
    color: '#ccc',
    margin: 0,
    fontSize: 18,
    fontWeight: 600,
  } as React.CSSProperties,
  graveyardCloseButton: {
    background: 'none',
    border: '1px solid #555',
    color: '#aaa',
    fontSize: 18,
    cursor: 'pointer',
    padding: '4px 10px',
    borderRadius: 4,
  } as React.CSSProperties,
  graveyardCardGrid: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 10,
    overflowY: 'auto',
    justifyContent: 'center',
  } as React.CSSProperties,
  exilePile: {
    position: 'relative',
    overflow: 'hidden',
    boxShadow: '0 2px 8px rgba(124, 58, 237, 0.4)',
    backgroundColor: '#1a1a2e',
    border: '1px solid rgba(124, 58, 237, 0.5)',
  },
  exileOverlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(20, 0, 40, 0.9)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 2000,
  } as React.CSSProperties,
  exileBrowserContent: {
    maxWidth: '90vw',
    maxHeight: '80vh',
    display: 'flex',
    flexDirection: 'column',
    gap: 16,
  } as React.CSSProperties,
  exileBrowserHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  } as React.CSSProperties,
  exileBrowserTitle: {
    color: '#c4b5fd',
    margin: 0,
    fontSize: 18,
    fontWeight: 600,
  } as React.CSSProperties,
  exileCloseButton: {
    background: 'none',
    border: '1px solid #7c3aed',
    color: '#c4b5fd',
    fontSize: 18,
    cursor: 'pointer',
    padding: '4px 10px',
    borderRadius: 4,
  } as React.CSSProperties,
  exileCardGrid: {
    display: 'flex',
    flexWrap: 'wrap',
    gap: 10,
    overflowY: 'auto',
    justifyContent: 'center',
  } as React.CSSProperties,
  playerInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
  playerName: {
    color: '#888',
  },
  spectatorNameLabel: {
    color: '#e0e0e0',
    fontSize: 14,
    fontWeight: 600,
    backgroundColor: 'rgba(20, 20, 30, 0.9)',
    padding: '8px 16px',
    borderRadius: 6,
    border: '1px solid rgba(255, 255, 255, 0.15)',
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.4)',
    letterSpacing: '0.02em',
    zIndex: 100,
  },
  lifeDisplay: {
    borderRadius: '50%',
    border: '3px solid',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 700,
  },
  playerControls: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    padding: 4,
  },
  passButton: {
    padding: '10px 24px',
    fontWeight: 600,
    backgroundColor: '#e67e22',
    color: 'white',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  cardRow: {
    display: 'flex',
    justifyContent: 'center',
    flexWrap: 'wrap',
    maxWidth: '100%',
  },
  emptyZone: {
    color: '#444',
    padding: 4,
    minHeight: 60,
  },
  battlefieldArea: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    width: '100%',
    flexShrink: 1,
    minHeight: 0,
  },
  battlefieldRow: {
    display: 'flex',
    justifyContent: 'center',
    flexWrap: 'wrap',
  },
  card: {
    position: 'relative',
    overflow: 'hidden',
    backgroundColor: '#1a1a2e',
    transition: 'transform 0.15s, box-shadow 0.15s',
    flexShrink: 0,
  },
  cardImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  },
  cardFallback: {
    display: 'none',
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 4,
    backgroundColor: '#2a2a4e',
  },
  cardName: {
    color: 'white',
    textAlign: 'center',
    fontWeight: 500,
  },
  cardPT: {
    color: 'white',
    fontWeight: 700,
    marginTop: 4,
  },
  ptOverlay: {
    position: 'absolute',
    bottom: 4,
    right: 4,
    padding: '2px 6px',
    borderRadius: 4,
    border: '1px solid rgba(255, 255, 255, 0.3)',
  } as React.CSSProperties,
  keywordIconsContainer: {
    position: 'absolute',
    top: 4,
    left: 4,
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
    pointerEvents: 'none',
  } as React.CSSProperties,
  keywordIconWrapper: {
    backgroundColor: 'rgba(0, 0, 0, 0.75)',
    borderRadius: 4,
    padding: 2,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: '1px solid rgba(255, 255, 255, 0.2)',
  } as React.CSSProperties,
  activeEffectsContainer: {
    position: 'absolute',
    bottom: 28,
    left: 4,
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  } as React.CSSProperties,
  activeEffectBadge: {
    backgroundColor: 'rgba(150, 50, 200, 0.9)',
    borderRadius: 4,
    padding: '2px 6px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    border: '1px solid rgba(255, 200, 255, 0.4)',
    cursor: 'help',
  } as React.CSSProperties,
  activeEffectText: {
    color: '#fff',
    fontSize: 10,
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  } as React.CSSProperties,
  cardEffectTooltip: {
    position: 'fixed',
    transform: 'translate(-50%, -100%)',
    backgroundColor: 'rgba(0, 0, 0, 0.95)',
    color: '#ffffff',
    padding: '6px 10px',
    borderRadius: 4,
    fontSize: 11,
    whiteSpace: 'nowrap',
    zIndex: 10000,
    marginTop: -8,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
    pointerEvents: 'none',
    border: '1px solid rgba(150, 50, 200, 0.5)',
  } as React.CSSProperties,
  tappedOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
  },
  summoningSicknessOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(100, 100, 150, 0.3)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    pointerEvents: 'none',
  },
  summoningSicknessIcon: {
    opacity: 0.8,
  },
  playableGlow: {
    position: 'absolute',
    top: -2,
    left: -2,
    right: -2,
    bottom: -2,
    borderRadius: 10,
    pointerEvents: 'none',
    animation: 'playablePulse 2s ease-in-out infinite',
    background: 'transparent',
    boxShadow: '0 0 8px rgba(0, 255, 0, 0.4)',
  },
  targetingOverlay: {
    position: 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    padding: '16px 24px',
    borderRadius: 12,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: 12,
    zIndex: 100,
    border: '2px solid #ff4444',
  },
  targetingPrompt: {
    color: '#ff4444',
    fontSize: 16,
    fontWeight: 600,
  },
  actionMenuOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 100,
  },
  actionMenu: {
    backgroundColor: '#1a1a2e',
    border: '2px solid #444',
    borderRadius: 12,
    padding: 16,
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
    minWidth: 200,
  },
  actionMenuTitle: {
    color: '#888',
    fontSize: 12,
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 8,
    textAlign: 'center',
  },
  actionButton: {
    padding: '12px 16px',
    fontSize: 14,
    backgroundColor: '#2a5a2a',
    color: 'white',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
    textAlign: 'left',
  },
  cancelButton: {
    padding: '10px 16px',
    fontSize: 14,
    backgroundColor: '#444',
    color: '#888',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
    marginTop: 8,
  },
  stackContainer: {
    position: 'fixed',
    left: 16,
    top: '50%',
    transform: 'translateY(-50%)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '8px 12px',
    backgroundColor: 'rgba(100, 50, 150, 0.3)',
    borderRadius: 8,
    border: '1px solid rgba(150, 100, 200, 0.4)',
    zIndex: 50,
    maxHeight: '60vh',
    overflowY: 'auto',
  },
  stackHeader: {
    color: '#b088d0',
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: 1,
    marginBottom: 8,
  },
  stackItems: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    position: 'relative',
  },
  stackItem: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    position: 'relative',
    cursor: 'pointer',
    transition: 'transform 0.15s',
  },
  stackItemImage: {
    width: 60,
    height: 84,
    objectFit: 'cover',
    borderRadius: 4,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.5)',
  },
  stackItemName: {
    color: '#ccc',
    marginTop: 4,
    maxWidth: 80,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    textAlign: 'center',
  },
  // Card preview styles
  cardPreviewOverlay: {
    position: 'fixed',
    top: 20,
    left: 20,
    zIndex: 1500,
    pointerEvents: 'none',
  } as React.CSSProperties,
  cardPreviewContainer: {
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
  } as React.CSSProperties,
  cardPreviewCard: {
    borderRadius: 12,
    overflow: 'hidden',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.8), 0 0 0 2px rgba(255, 255, 255, 0.1)',
  } as React.CSSProperties,
  cardPreviewImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
  } as React.CSSProperties,
  cardPreviewKeywords: {
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
    backgroundColor: 'rgba(0, 0, 0, 0.85)',
    padding: 12,
    borderRadius: 8,
    border: '1px solid rgba(255, 255, 255, 0.1)',
  } as React.CSSProperties,
  cardPreviewKeyword: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  } as React.CSSProperties,
  cardPreviewKeywordName: {
    color: '#ffcc00',
    fontWeight: 600,
    fontSize: 14,
  } as React.CSSProperties,
  cardPreviewRulings: {
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
    backgroundColor: 'rgba(0, 0, 0, 0.92)',
    padding: 12,
    borderRadius: 8,
    border: '1px solid rgba(100, 150, 255, 0.3)',
    maxWidth: 320,
    maxHeight: 300,
    overflowY: 'auto',
  } as React.CSSProperties,
  cardPreviewRulingsHeader: {
    color: '#6699ff',
    fontWeight: 700,
    fontSize: 13,
    textTransform: 'uppercase',
    letterSpacing: 1,
    borderBottom: '1px solid rgba(100, 150, 255, 0.2)',
    paddingBottom: 6,
  } as React.CSSProperties,
  cardPreviewRuling: {
    display: 'flex',
    flexDirection: 'column',
    gap: 2,
  } as React.CSSProperties,
  cardPreviewRulingDate: {
    color: '#888888',
    fontSize: 11,
    fontStyle: 'italic',
  } as React.CSSProperties,
  cardPreviewRulingText: {
    color: '#dddddd',
    fontSize: 12,
    lineHeight: 1.4,
  } as React.CSSProperties,
  cardPreviewRulingsHint: {
    color: '#666666',
    fontSize: 11,
    fontStyle: 'italic',
    textAlign: 'center',
    padding: '4px 8px',
  } as React.CSSProperties,
  // Active effect badge styles
  effectBadgesContainer: {
    display: 'flex',
    flexDirection: 'row',
    gap: 4,
    flexWrap: 'wrap',
  } as React.CSSProperties,
  effectBadge: {
    display: 'flex',
    alignItems: 'center',
    gap: 4,
    backgroundColor: 'rgba(255, 100, 100, 0.2)',
    border: '1px solid rgba(255, 100, 100, 0.5)',
    borderRadius: 4,
    color: '#ff8888',
    position: 'relative',
    cursor: 'help',
  } as React.CSSProperties,
  effectBadgeIcon: {
    fontSize: 12,
  } as React.CSSProperties,
  effectBadgeName: {
    fontWeight: 500,
    whiteSpace: 'nowrap',
  } as React.CSSProperties,
  effectTooltip: {
    position: 'absolute',
    bottom: '100%',
    left: '50%',
    transform: 'translateX(-50%)',
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    color: '#ffffff',
    padding: '6px 10px',
    borderRadius: 4,
    fontSize: 12,
    whiteSpace: 'nowrap',
    zIndex: 1000,
    marginBottom: 4,
    boxShadow: '0 2px 8px rgba(0, 0, 0, 0.3)',
    pointerEvents: 'none',
  } as React.CSSProperties,
  // Counter badge styles (for +1/+1 counters on battlefield cards)
  counterBadge: {
    position: 'absolute',
    bottom: 22,
    right: 4,
    backgroundColor: 'rgba(30, 80, 140, 0.95)',
    borderRadius: 4,
    border: '1px solid rgba(100, 180, 255, 0.5)',
    display: 'flex',
    alignItems: 'center',
    gap: 2,
    color: '#66ccff',
    fontWeight: 700,
    zIndex: 5,
  } as React.CSSProperties,
  counterBadgeIcon: {
    fontSize: 8,
    opacity: 0.9,
  } as React.CSSProperties,
  counterBadgeText: {
    fontWeight: 700,
  } as React.CSSProperties,
  // Enhanced preview stats box styles
  cardPreviewStatsBox: {
    display: 'flex',
    flexDirection: 'column',
    backgroundColor: 'rgba(0, 0, 0, 0.92)',
    borderRadius: 8,
    padding: '8px 12px',
    gap: 6,
    border: '1px solid rgba(255, 255, 255, 0.15)',
  } as React.CSSProperties,
  cardPreviewStatsMain: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 2,
  } as React.CSSProperties,
  cardPreviewStatsBreakdown: {
    display: 'flex',
    flexDirection: 'column',
    gap: 3,
    borderTop: '1px solid rgba(255, 255, 255, 0.15)',
    paddingTop: 6,
  } as React.CSSProperties,
  cardPreviewStatsRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    fontSize: 12,
  } as React.CSSProperties,
  cardPreviewStatsLabel: {
    color: '#888888',
    display: 'flex',
    alignItems: 'center',
  } as React.CSSProperties,
  cardPreviewStatsValue: {
    color: '#cccccc',
    fontWeight: 600,
    fontFamily: 'monospace',
  } as React.CSSProperties,
}

/**
 * Concede button with confirmation, positioned top-right.
 */
function ConcedeButton() {
  const concede = useGameStore((state) => state.concede)
  const [confirming, setConfirming] = useState(false)
  const responsive = useResponsiveContext()

  const base: React.CSSProperties = {
    position: 'absolute',
    top: responsive.isMobile ? 8 : 12,
    right: responsive.isMobile ? 8 : 12,
    zIndex: 100,
    display: 'flex',
    gap: 4,
  }

  if (confirming) {
    return (
      <div style={base}>
        <button
          onClick={() => { concede(); setConfirming(false) }}
          style={{
            padding: responsive.isMobile ? '6px 10px' : '8px 14px',
            fontSize: responsive.fontSize.small,
            backgroundColor: '#cc0000',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          Confirm
        </button>
        <button
          onClick={() => setConfirming(false)}
          style={{
            padding: responsive.isMobile ? '6px 10px' : '8px 14px',
            fontSize: responsive.fontSize.small,
            backgroundColor: '#222',
            color: '#aaa',
            border: '1px solid #333',
            borderRadius: 6,
            cursor: 'pointer',
          }}
        >
          Cancel
        </button>
      </div>
    )
  }

  return (
    <div style={base}>
      <button
        onClick={() => setConfirming(true)}
        style={{
          padding: responsive.isMobile ? '6px 10px' : '8px 14px',
          fontSize: responsive.fontSize.small,
          backgroundColor: 'transparent',
          color: '#cc0000',
          border: '1px solid #cc0000',
          borderRadius: 6,
          cursor: 'pointer',
        }}
      >
        Concede
      </button>
    </div>
  )
}

/**
 * Fullscreen toggle button, positioned top-left.
 */
function FullscreenButton() {
  const [isFullscreen, setIsFullscreen] = useState(false)
  const responsive = useResponsiveContext()

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement)
    }
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange)
  }, [])

  const toggleFullscreen = async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen()
      } else {
        await document.exitFullscreen()
      }
    } catch (err) {
      console.error('Fullscreen error:', err)
    }
  }

  return (
    <button
      onClick={toggleFullscreen}
      style={{
        position: 'absolute',
        top: responsive.isMobile ? 8 : 12,
        left: responsive.isMobile ? 8 : 12,
        zIndex: 100,
        padding: responsive.isMobile ? '6px 10px' : '8px 14px',
        fontSize: responsive.fontSize.small,
        backgroundColor: 'transparent',
        color: '#888',
        border: '1px solid #444',
        borderRadius: 6,
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        gap: 4,
      }}
      title={isFullscreen ? 'Exit fullscreen (Esc)' : 'Enter fullscreen'}
    >
      {isFullscreen ? '⛶' : '⛶'} {isFullscreen ? 'Exit' : 'Fullscreen'}
    </button>
  )
}
