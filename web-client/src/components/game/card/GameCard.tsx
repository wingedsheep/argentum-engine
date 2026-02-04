import React, { useCallback, useEffect, useRef } from 'react'
import { useGameStore } from '../../../store/gameStore'
import { useHasLegalActions } from '../../../store/selectors'
import type { ClientCard } from '../../../types'
import { getCardImageUrl, getScryfallFallbackUrl, MORPH_FACE_DOWN_IMAGE_URL } from '../../../utils/cardImages'
import { useInteraction } from '../../../hooks/useInteraction'
import {
  useResponsiveContext,
  hasMultipleCastingOptions,
  getPTColor,
  getCounterStatModifier,
  hasStatCounters,
} from '../board/shared'
import { styles } from '../board/styles'
import { KeywordIcons, ActiveEffectBadges } from './CardOverlays'

interface GameCardProps {
  card: ClientCard
  count?: number
  faceDown?: boolean
  interactive?: boolean
  small?: boolean
  battlefield?: boolean
  overrideWidth?: number
  isOpponentCard?: boolean
  inHand?: boolean
}

/**
 * Single card display.
 */
export function GameCard({
  card,
  count = 1,
  faceDown = false,
  interactive = false,
  small = false,
  battlefield = false,
  overrideWidth,
  isOpponentCard = false,
  inHand = false,
}: GameCardProps) {
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
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)
  const toggleDecisionSelection = useGameStore((state) => state.toggleDecisionSelection)
  const responsive = useResponsiveContext()
  const { handleCardClick } = useInteraction()
  const dragStartPos = useRef<{ x: number; y: number } | null>(null)
  const handledByDrag = useRef(false)

  // Hover handlers for card preview
  // Allow hover for non-face-down cards, or for the controller's own face-down cards
  const handleMouseEnter = useCallback(() => {
    if (!faceDown || !isOpponentCard) {
      hoverCard(card.id)
    }
  }, [card.id, faceDown, isOpponentCard, hoverCard])

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

  // Check if this card is a valid option in decision selection mode (SelectCardsDecision with useTargetingUI)
  const isValidDecisionSelection = decisionSelectionState?.validOptions.includes(card.id) ?? false
  const isSelectedDecisionOption = decisionSelectionState?.selectedOptions.includes(card.id) ?? false

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
  // Face-down cards can be playable too (for TurnFaceUp action)
  const isPlayable = interactive && hasLegalActions && !isInCombatMode

  const cardImageUrl = faceDown
    ? MORPH_FACE_DOWN_IMAGE_URL
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
  // Get all playable actions (including morph, cycling, etc.)
  const playableActions = legalActions.filter((a) => {
    const action = a.action
    return (action.type === 'PlayLand' && action.cardId === card.id) ||
           (action.type === 'CastSpell' && action.cardId === card.id) ||
           (action.type === 'CycleCard' && action.cardId === card.id)
  })
  const playableAction = playableActions[0]
  // Show modal if multiple legal actions OR if card has multiple potential options (e.g., morph + normal cast)
  const hasMultiplePotentialOptions = hasMultipleCastingOptions(playableActions)
  const shouldShowCastModal = playableActions.length > 1 || (hasMultiplePotentialOptions && playableActions.length > 0)
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
        // Short drag = click
        stopDraggingCard()

        // During combat mode, toggle attacker/blocker selection instead of opening action menu
        if (isInAttackerMode) {
          if (isValidAttacker) {
            toggleAttacker(card.id)
          }
          return
        }
        if (isInBlockerMode) {
          if (isValidBlocker && isSelectedAsBlocker) {
            removeBlockerAssignment(card.id)
          }
          return
        }

        // Outside combat mode, open action menu
        handleCardClick(card.id)
        return
      }

      if (!isOverHand && playableAction) {
        // If multiple casting methods available, open the modal to let player choose
        if (shouldShowCastModal) {
          selectCard(card.id)
          stopDraggingCard()
          return
        }

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
            // Pass pendingActionInfo for damage distribution spells (e.g., Forked Lightning)
            ...(playableAction.requiresDamageDistribution ? { pendingActionInfo: playableAction } : {}),
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
  }, [draggingCardId, card.id, playableAction, shouldShowCastModal, submitAction, stopDraggingCard, startTargeting, startXSelection, handleCardClick, selectCard, isInAttackerMode, isValidAttacker, toggleAttacker, isInBlockerMode, isValidBlocker, isSelectedAsBlocker, removeBlockerAssignment])

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

    // Handle decision selection mode clicks (SelectCardsDecision with useTargetingUI)
    if (isValidDecisionSelection) {
      toggleDecisionSelection(card.id)
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
  } else if (isSelectedDecisionOption) {
    // Orange highlight for selected decision options
    borderStyle = '3px solid #ff9900'
    boxShadow = '0 0 20px rgba(255, 153, 0, 0.8), 0 0 40px rgba(255, 153, 0, 0.4)'
  } else if (isSelected && !isInCombatMode) {
    borderStyle = '3px solid #ffff00'
    boxShadow = '0 8px 20px rgba(255, 255, 0, 0.4)'
  } else if ((isValidTarget || isValidDecisionTarget || isValidDecisionSelection) && isHovered) {
    // Bright highlight when hovering over a valid target
    borderStyle = '3px solid #ff6666'
    boxShadow = '0 0 20px rgba(255, 100, 100, 0.9), 0 0 40px rgba(255, 68, 68, 0.6)'
  } else if (isValidTarget || isValidDecisionTarget || isValidDecisionSelection) {
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
  const canInteract = interactive || isValidTarget || isValidDecisionTarget || isValidDecisionSelection || isValidAttacker || isValidBlocker || isAttackingInBlockerMode || canDragToPlay
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

      {/* P/T overlay for creatures on battlefield (server sends 2/2 for face-down creatures) */}
      {battlefield && card.power !== null && card.toughness !== null && (
        <div style={{
          ...styles.ptOverlay,
          backgroundColor: faceDown
            ? 'rgba(0, 0, 0, 0.7)'
            : getPTColor(card.power, card.toughness, card.basePower, card.baseToughness) !== 'white'
              ? 'rgba(0, 0, 0, 0.85)'
              : 'rgba(0, 0, 0, 0.7)',
        }}>
          <span style={{
            color: faceDown ? 'white' : getPTColor(card.power, card.toughness, card.basePower, card.baseToughness),
            fontWeight: 700,
            fontSize: responsive.isMobile ? 10 : 12,
          }}>
            {card.power}/
          </span>
          <span style={{
            color: faceDown
              ? (card.damage != null && card.damage > 0 ? '#ff4444' : 'white')
              : card.damage != null && card.damage > 0
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
