import React, { useCallback, useEffect, useRef, useState } from 'react'
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
  getGoldCounters,
  getPlagueCounters,
  getTokenFrameGradient,
  getTokenFrameTextColor,
  getCardFallbackColor,
} from '../board/shared'
import { styles } from '../board/styles'
import {
  TARGET_COLOR, TARGET_COLOR_BRIGHT, TARGET_GLOW, TARGET_GLOW_BRIGHT, TARGET_GLOW_OUTER, TARGET_SHADOW,
  SELECTED_COLOR, SELECTED_GLOW, SELECTED_SHADOW,
} from '../../../styles/targetingColors'
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
  /** Force tapped visual (e.g. for attachments of tapped permanents) */
  forceTapped?: boolean
  /** Ghost card from graveyard (translucent, purple glow) */
  isGhost?: boolean
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
  forceTapped = false,
  isGhost = false,
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
  const distributeState = useGameStore((state) => state.distributeState)
  const incrementDistribute = useGameStore((state) => state.incrementDistribute)
  const decrementDistribute = useGameStore((state) => state.decrementDistribute)
  const submitYesNoDecision = useGameStore((state) => state.submitYesNoDecision)
  const gameState = useGameStore((state) => state.gameState)
  const responsive = useResponsiveContext()
  const { handleCardClick, handleDoubleClick } = useInteraction()
  const dragStartPos = useRef<{ x: number; y: number } | null>(null)
  const handledByDrag = useRef(false)

  // Hover handlers for card preview
  const handleMouseEnter = useCallback(() => {
    hoverCard(card.id)
  }, [card.id, hoverCard])

  const handleMouseLeave = useCallback(() => {
    hoverCard(null)
  }, [hoverCard])

  // Long-press handler for mobile card preview
  const longPressTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const [longPressActive, setLongPressActive] = useState(false)

  const handleTouchStartPreview = useCallback((_e: React.TouchEvent) => {
    longPressTimer.current = setTimeout(() => {
      setLongPressActive(true)
      hoverCard(card.id)
      // Cancel any in-progress drag
      stopDraggingCard()
      stopDraggingBlocker()
    }, 400)
  }, [card.id, hoverCard, stopDraggingCard, stopDraggingBlocker])

  const handleTouchEndPreview = useCallback(() => {
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current)
      longPressTimer.current = null
    }
    if (longPressActive) {
      setLongPressActive(false)
      hoverCard(null)
      // Mark as handled by drag to suppress click
      handledByDrag.current = true
    }
  }, [longPressActive, hoverCard])

  const handleTouchMovePreview = useCallback(() => {
    // Cancel long-press if finger moves (user is scrolling/dragging)
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current)
      longPressTimer.current = null
    }
  }, [])

  // Check if card has legal actions (is playable)
  const hasLegalActions = useHasLegalActions(card.id)

  const hoveredCardId = useGameStore((state) => state.hoveredCardId)
  const autoTapPreview = useGameStore((state) => state.autoTapPreview)

  const isTapped = card.isTapped || forceTapped
  const isSelected = selectedCardId === card.id
  const isInAutoTapPreview = autoTapPreview?.includes(card.id) ?? false
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

  // Inline damage distribution checks
  const isDistributeTarget = distributeState?.targets.includes(card.id) ?? false
  const distributeAllocated = isDistributeTarget ? (distributeState?.distribution[card.id] ?? 0) : 0
  const distributeTotalAllocated = distributeState
    ? Object.values(distributeState.distribution).reduce((sum, v) => sum + v, 0)
    : 0
  const distributeRemaining = distributeState ? distributeState.totalAmount - distributeTotalAllocated : 0
  const distributeMaxForCard = distributeState?.maxPerTarget?.[card.id]
  const distributeAtMax = distributeMaxForCard !== undefined && distributeAllocated >= distributeMaxForCard

  // Combat trigger YesNo check (inline buttons on triggering entity card)
  const isCombatTriggerYesNo = pendingDecision?.type === 'YesNoDecision'
    && pendingDecision.context.triggeringEntityId === card.id
    && !!gameState?.combat

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

  // Show playable highlight for cards that aren't purely combat-role cards.
  // Valid blockers with legal actions (e.g., activated abilities) are still playable since blocking uses drag.
  // Face-down cards can be playable too (for TurnFaceUp action)
  const isCombatRoleCard = isValidAttacker || (isValidBlocker && !hasLegalActions) || isAttackingInBlockerMode
  const hasActiveDecision = pendingDecision !== null
  const isPlayable = interactive && hasLegalActions && (!isInCombatMode || !isCombatRoleCard) && !hasActiveDecision

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
  // Also show modal for cycling lands where play land is unavailable (so player sees grayed-out "Play land")
  const isCyclingLandWithoutPlayLand = card.cardTypes.includes('LAND') &&
    playableActions.length === 1 && playableActions[0]?.action.type === 'CycleCard'
  const shouldShowCastModal = playableActions.length > 1 || (hasMultiplePotentialOptions && playableActions.length > 0) || isCyclingLandWithoutPlayLand
  const canDragToPlay = inHand && playableAction && !isInCombatMode

  // Handle mouse/touch down - start dragging for blockers or hand cards
  const handlePointerDown = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    const clientX = 'touches' in e ? e.touches[0]!.clientX : e.clientX
    const clientY = 'touches' in e ? e.touches[0]!.clientY : e.clientY

    if (isInBlockerMode && isValidBlocker && !isSelectedAsBlocker) {
      e.preventDefault()
      startDraggingBlocker(card.id)
      return
    }
    // Start dragging card from hand
    if (canDragToPlay) {
      e.preventDefault()
      dragStartPos.current = { x: clientX, y: clientY }
      startDraggingCard(card.id)
    }
  }, [isInBlockerMode, isValidBlocker, isSelectedAsBlocker, startDraggingBlocker, canDragToPlay, startDraggingCard, card.id])

  // Handle mouse/touch up - drop blocker on attacker or cancel drag
  const handlePointerUp = useCallback(() => {
    if (isInBlockerMode && draggingBlockerId && isAttackingInBlockerMode) {
      // Dropping on an attacker - assign the blocker
      assignBlocker(draggingBlockerId, card.id)
      stopDraggingBlocker()
    }
  }, [isInBlockerMode, draggingBlockerId, isAttackingInBlockerMode, assignBlocker, stopDraggingBlocker, card.id])

  // Global mouse/touch up handler for card dragging (to detect drop outside hand)
  useEffect(() => {
    if (draggingCardId !== card.id) return

    const handleGlobalPointerUp = (clientX: number, clientY: number) => {
      // Require minimum drag distance to prevent accidental casts
      const MIN_DRAG_DISTANCE = 30
      const start = dragStartPos.current
      const draggedFarEnough = start != null &&
        Math.hypot(clientX - start.x, clientY - start.y) >= MIN_DRAG_DISTANCE
      dragStartPos.current = null

      // Check if dropped outside the hand area - if so, play the card
      // Use the top of the hand zone as a threshold: anything at or below it counts as "over the hand"
      // This way dropping anywhere along the bottom of the screen cancels the cast
      const handEl = document.querySelector('[data-zone="hand"]')
      let isOverHand = false

      if (handEl) {
        const rect = handEl.getBoundingClientRect()
        isOverHand = clientY >= rect.top
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
        } else if (playableAction.action.type === 'CastSpell' && (playableAction.additionalCostInfo?.costType === 'SacrificePermanent' || playableAction.additionalCostInfo?.costType === 'SacrificeSelf')) {
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
          if (playableAction.targetRequirements && playableAction.targetRequirements.length > 1) {
            // Multi-target spell (e.g., Cruel Revival) — set up full multi-target state
            const firstReq = playableAction.targetRequirements[0]!
            startTargeting({
              action: playableAction.action,
              validTargets: [...firstReq.validTargets],
              selectedTargets: [],
              minTargets: firstReq.minTargets,
              maxTargets: firstReq.maxTargets,
              currentRequirementIndex: 0,
              allSelectedTargets: [],
              targetRequirements: playableAction.targetRequirements,
              ...(firstReq.targetZone ? { targetZone: firstReq.targetZone } : {}),
              targetDescription: firstReq.description,
              totalRequirements: playableAction.targetRequirements.length,
              ...(playableAction.requiresDamageDistribution ? { pendingActionInfo: playableAction } : {}),
            })
          } else {
            // Single-target spell — enter targeting mode
            startTargeting({
              action: playableAction.action,
              validTargets: playableAction.validTargets,
              selectedTargets: [],
              minTargets: playableAction.minTargets ?? playableAction.targetCount ?? 1,
              maxTargets: playableAction.targetCount ?? 1,
              // Pass pendingActionInfo for damage distribution spells (e.g., Forked Lightning)
              ...(playableAction.requiresDamageDistribution ? { pendingActionInfo: playableAction } : {}),
            })
          }
        } else {
          // Play the card directly
          submitAction(playableAction.action)
        }
      }
      stopDraggingCard()
    }

    const handleMouseUp = (e: MouseEvent) => handleGlobalPointerUp(e.clientX, e.clientY)
    const handleTouchEnd = (e: TouchEvent) => {
      const touch = e.changedTouches[0]
      if (touch) handleGlobalPointerUp(touch.clientX, touch.clientY)
    }

    window.addEventListener('mouseup', handleMouseUp)
    window.addEventListener('touchend', handleTouchEnd)
    return () => {
      window.removeEventListener('mouseup', handleMouseUp)
      window.removeEventListener('touchend', handleTouchEnd)
    }
  }, [draggingCardId, card.id, playableAction, shouldShowCastModal, submitAction, stopDraggingCard, startTargeting, startXSelection, handleCardClick, selectCard, isInAttackerMode, isValidAttacker, toggleAttacker, isInBlockerMode, isValidBlocker, isSelectedAsBlocker, removeBlockerAssignment])

  // Global mouse/touch up handler to cancel blocker drag
  // For touch, we also detect drop target since touchend fires on the originating element
  useEffect(() => {
    if (!draggingBlockerId) return

    const handleGlobalMouseUp = () => {
      stopDraggingBlocker()
    }

    const handleGlobalTouchEnd = (e: TouchEvent) => {
      const touch = e.changedTouches[0]
      if (touch && isInBlockerMode) {
        // Find the element under the touch point
        const elementAtPoint = document.elementFromPoint(touch.clientX, touch.clientY)
        if (elementAtPoint) {
          // Walk up the DOM to find a card element
          const cardEl = elementAtPoint.closest('[data-card-id]')
          if (cardEl) {
            const targetCardId = cardEl.getAttribute('data-card-id')
            if (targetCardId && combatState?.attackingCreatures.includes(targetCardId as any)) {
              assignBlocker(draggingBlockerId, targetCardId as any)
            }
          }
        }
      }
      stopDraggingBlocker()
    }

    window.addEventListener('mouseup', handleGlobalMouseUp)
    window.addEventListener('touchend', handleGlobalTouchEnd)
    return () => {
      window.removeEventListener('mouseup', handleGlobalMouseUp)
      window.removeEventListener('touchend', handleGlobalTouchEnd)
    }
  }, [draggingBlockerId, stopDraggingBlocker, isInBlockerMode, combatState, assignBlocker])

  const handleClick = () => {
    // If the drag handler already processed this interaction, skip
    if (handledByDrag.current) {
      handledByDrag.current = false
      return
    }

    // Handle inline distribute mode - click to add damage
    if (isDistributeTarget && distributeRemaining > 0 && !distributeAtMax) {
      incrementDistribute(card.id)
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
    if (isChooseTargetsDecision && isValidDecisionTarget && !decisionSelectionState) {
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
        return
      }
      // Non-attacker cards fall through to normal selection
    }

    // Handle blocker mode clicks - clicking an assigned blocker removes it
    if (isInBlockerMode) {
      if (isValidBlocker && isSelectedAsBlocker) {
        removeBlockerAssignment(card.id)
        return
      }
      if (isAttackingInBlockerMode) {
        return  // Handled by drag-and-drop mouseup
      }
      if (isValidBlocker && !hasLegalActions) {
        return  // Pure blocker with no abilities, handled by drag-and-drop
      }
      // Non-blocker/non-attacker cards fall through to normal selection
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

  // Double-click handler - auto-cast if possible
  const handleDoubleClickEvent = () => {
    // Skip if in special modes
    if (isInTargetingMode || isChooseTargetsDecision || isValidDecisionSelection) {
      return
    }
    // Skip for combat-role cards during combat (attackers, blockers, attacking creatures)
    if (isInCombatMode && (isValidAttacker || isValidBlocker || isAttackingInBlockerMode)) {
      return
    }

    // Only handle interactive cards
    if (interactive) {
      handleDoubleClick(card.id)
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
    // Green highlight for selected targets (already chosen in targeting mode)
    borderStyle = `3px solid ${SELECTED_COLOR}`
    boxShadow = `0 0 20px ${SELECTED_GLOW}, 0 0 40px ${SELECTED_SHADOW}`
  } else if (isSelectedDecisionOption) {
    // Green highlight for selected decision options
    borderStyle = `3px solid ${SELECTED_COLOR}`
    boxShadow = `0 0 20px ${SELECTED_GLOW}, 0 0 40px ${SELECTED_SHADOW}`
  } else if (isCombatTriggerYesNo) {
    // Orange/gold glow for the combat trigger creature (matches distribute target style)
    borderStyle = '3px solid #ff6b35'
    boxShadow = '0 0 16px rgba(255, 107, 53, 0.7), 0 0 32px rgba(255, 107, 53, 0.4)'
  } else if (isDistributeTarget && distributeAllocated > 0) {
    // Orange for distribute targets with damage allocated
    borderStyle = '3px solid #ff6b35'
    boxShadow = '0 0 16px rgba(255, 107, 53, 0.7), 0 0 32px rgba(255, 107, 53, 0.4)'
  } else if (isDistributeTarget) {
    // Dim orange for unallocated distribute targets
    borderStyle = '2px solid #ff8c42'
    boxShadow = '0 0 12px rgba(255, 140, 66, 0.5), 0 0 24px rgba(255, 140, 66, 0.3)'
  } else if (isSelected && (!isInCombatMode || !isCombatRoleCard)) {
    borderStyle = '3px solid #ffff00'
    boxShadow = '0 8px 20px rgba(255, 255, 0, 0.4)'
  } else if ((isValidDecisionSelection || isValidTarget || isValidDecisionTarget) && isHovered) {
    // Bright light-blue highlight when hovering over a valid target/option
    borderStyle = `3px solid ${TARGET_COLOR_BRIGHT}`
    boxShadow = `0 0 20px ${TARGET_GLOW_BRIGHT}, 0 0 40px ${TARGET_GLOW_OUTER}`
  } else if (isValidDecisionSelection || isValidTarget || isValidDecisionTarget) {
    // Light-blue highlight for valid targets/options
    borderStyle = `2px solid ${TARGET_COLOR}`
    boxShadow = `0 0 12px ${TARGET_GLOW}, 0 0 24px ${TARGET_SHADOW}`
  } else if ((isValidAttacker || isValidBlocker) && isHovered) {
    // Bright highlight when hovering over a valid attacker/blocker
    borderStyle = `3px solid ${TARGET_COLOR_BRIGHT}`
    boxShadow = `0 0 20px ${TARGET_GLOW_BRIGHT}, 0 0 40px ${TARGET_GLOW_OUTER}`
  } else if (isValidAttacker || isValidBlocker) {
    // Light-blue highlight for valid attackers/blockers
    borderStyle = `2px solid ${TARGET_COLOR}`
    boxShadow = `0 0 12px ${TARGET_GLOW}, 0 0 24px ${TARGET_SHADOW}`
  } else if (isPlayable && isGhost && isHovered) {
    // Bright purple highlight when hovering over a playable ghost card
    borderStyle = '3px solid #aa77ee'
    boxShadow = '0 0 20px rgba(170, 119, 238, 0.9), 0 0 40px rgba(136, 85, 204, 0.5)'
  } else if (isPlayable && isGhost) {
    // Purple highlight for playable ghost cards
    borderStyle = '2px solid #8855cc'
    boxShadow = '0 0 12px rgba(136, 85, 204, 0.5), 0 0 24px rgba(136, 85, 204, 0.3)'
  } else if (isGhost) {
    // Dim purple border for ghost cards that aren't playable
    borderStyle = '2px solid #6644aa'
    boxShadow = '0 0 8px rgba(102, 68, 170, 0.4), 0 0 16px rgba(102, 68, 170, 0.2)'
  } else if (isPlayable && isHovered) {
    // Bright cyan highlight when hovering over a playable card
    borderStyle = `3px solid ${TARGET_COLOR_BRIGHT}`
    boxShadow = `0 0 20px ${TARGET_GLOW_BRIGHT}, 0 0 40px ${TARGET_GLOW_OUTER}`
  } else if (isPlayable) {
    // Cyan highlight for playable cards
    borderStyle = `2px solid ${TARGET_COLOR}`
    boxShadow = `0 0 12px ${TARGET_GLOW}, 0 0 24px ${TARGET_SHADOW}`
  } else if (isInAutoTapPreview) {
    // Cyan highlight for lands that would be auto-tapped
    borderStyle = `2px solid ${TARGET_COLOR}`
    boxShadow = `0 0 12px ${TARGET_GLOW}, 0 0 24px ${TARGET_SHADOW}`
  }

  // Determine cursor
  const canInteract = interactive || isValidTarget || isValidDecisionTarget || isValidDecisionSelection || isValidAttacker || isValidBlocker || isAttackingInBlockerMode || canDragToPlay || isDistributeTarget
  const baseCursor = canInteract ? 'pointer' : 'default'
  const cursor = (isValidBlocker && !isSelectedAsBlocker) || canDragToPlay ? 'grab' : baseCursor

  // Check if currently being dragged (blocker or hand card)
  const isBeingDragged = draggingBlockerId === card.id || draggingCardId === card.id

  // Container dimensions - expand width when tapped to prevent overlap
  // Tapped cards rotate 90deg, so they need width = height to not overlap
  const containerWidth = isTapped && battlefield ? height + 8 : width
  const containerHeight = height

  const cardElement = (
    <div
      data-card-id={card.id}
      {...(isGhost ? { 'data-ghost': 'true' } : {})}
      onClick={handleClick}
      onDoubleClick={handleDoubleClickEvent}
      onMouseDown={handlePointerDown}
      onMouseUp={handlePointerUp}
      onTouchStart={(e) => { handleTouchStartPreview(e); handlePointerDown(e) }}
      onTouchEnd={() => { handleTouchEndPreview(); handlePointerUp() }}
      onTouchMove={handleTouchMovePreview}
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      style={{
        ...styles.card,
        width,
        height,
        borderRadius: responsive.isMobile ? 4 : 8,
        cursor,
        border: borderStyle,
        pointerEvents: 'auto',
        transform: `${isTapped ? 'rotate(90deg)' : ''} ${isSelected && (!isInCombatMode || !isCombatRoleCard) ? 'translateY(-8px)' : ''}`,
        transformOrigin: 'center',
        boxShadow,
        opacity: isBeingDragged ? 0.6 : isGhost ? 0.55 : 1,
        userSelect: 'none',
      }}
    >
      {/* Token with art_crop image — render a custom card frame */}
      {!faceDown && card.isToken && card.imageUri ? (
        <div style={{
          ...styles.tokenFrame,
          background: getTokenFrameGradient(card.colors),
        }}>
          <div style={{
            ...styles.tokenNameBar,
            color: getTokenFrameTextColor(card.colors),
            fontSize: responsive.isMobile ? 7 : 9,
          }}>
            {card.name}
          </div>
          <div style={styles.tokenArtBox}>
            <img
              src={cardImageUrl}
              alt={card.name}
              style={styles.tokenArtImage}
            />
          </div>
          <div style={{
            ...styles.tokenTypeBar,
            color: getTokenFrameTextColor(card.colors),
            fontSize: responsive.isMobile ? 6 : 8,
          }}>
            {card.typeLine}
          </div>
        </div>
      ) : (
        <>
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
            <div style={{
              ...styles.cardFallbackInner,
              backgroundColor: getCardFallbackColor(card.colors),
            }}>
              <span style={{
                ...styles.cardFallbackName,
                fontSize: responsive.fontSize.small,
              }}>
                {faceDown ? '' : card.name}
              </span>
              {!faceDown && card.typeLine && (
                <span style={{
                  ...styles.cardFallbackType,
                  fontSize: Math.max(responsive.fontSize.small - 2, 8),
                }}>
                  {card.typeLine}
                </span>
              )}
              {!faceDown && card.power !== null && card.toughness !== null && (
                <span style={{
                  ...styles.cardFallbackPT,
                  fontSize: responsive.fontSize.normal,
                  color: getPTColor(card.power, card.toughness, card.basePower, card.baseToughness),
                }}>
                  {card.power}/{card.toughness}
                </span>
              )}
            </div>
          </div>
        </>
      )}

      {/* Tapped indicator */}
      {isTapped && (
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

      {/* Gold counter badge */}
      {battlefield && !faceDown && getGoldCounters(card) > 0 && (
        <div style={{
          ...styles.goldCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <span style={{ fontSize: responsive.isMobile ? 8 : 10 }}>&#x2B22;</span>
          <span style={{ fontWeight: 700 }}>
            {getGoldCounters(card)}
          </span>
        </div>
      )}

      {/* Plague counter badge */}
      {battlefield && !faceDown && getPlagueCounters(card) > 0 && (
        <div style={{
          ...styles.plagueCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <span style={{ fontSize: responsive.isMobile ? 8 : 10 }}>&#x2623;</span>
          <span style={{ fontWeight: 700 }}>
            {getPlagueCounters(card)}
          </span>
        </div>
      )}

      {/* Keyword ability icons */}
      {battlefield && !faceDown && (card.keywords.length > 0 || (card.protections && card.protections.length > 0)) && (
        <KeywordIcons keywords={card.keywords} protections={card.protections ?? []} size={responsive.isMobile ? 14 : 18} />
      )}

      {/* Chosen creature type badge (e.g., Doom Cannon on battlefield, Aphetto Dredging on stack) */}
      {!faceDown && card.chosenCreatureType && (
        <div style={{
          position: 'absolute',
          bottom: card.power != null ? 22 : 4,
          left: 4,
          backgroundColor: 'rgba(80, 60, 30, 0.9)',
          color: '#f0d890',
          fontSize: responsive.isMobile ? 8 : 10,
          padding: '1px 4px',
          borderRadius: 3,
          border: '1px solid rgba(200, 170, 80, 0.6)',
          whiteSpace: 'nowrap',
          pointerEvents: 'none',
          zIndex: 5,
        }}>
          {card.chosenCreatureType}
        </div>
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

      {/* Inline damage distribution badge (top-right) */}
      {isDistributeTarget && distributeAllocated > 0 && (
        <div style={{
          position: 'absolute',
          top: responsive.isMobile ? 2 : 4,
          right: responsive.isMobile ? 2 : 4,
          backgroundColor: '#dc2626',
          color: 'white',
          width: responsive.isMobile ? 20 : 26,
          height: responsive.isMobile ? 20 : 26,
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 'bold',
          fontSize: responsive.isMobile ? 11 : 14,
          boxShadow: '0 2px 8px rgba(220, 38, 38, 0.6)',
          zIndex: 15,
        }}>
          {distributeAllocated}
        </div>
      )}

      {/* Inline Yes/No buttons for combat trigger (bottom) */}
      {isCombatTriggerYesNo && pendingDecision?.type === 'YesNoDecision' && (
        <div
          onClick={(e) => e.stopPropagation()}
          style={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 4,
            padding: responsive.isMobile ? '3px 2px' : '4px 3px',
            backgroundColor: 'rgba(0, 0, 0, 0.9)',
            borderTop: '1px solid rgba(255, 107, 53, 0.5)',
            zIndex: 15,
          }}
        >
          <button
            onClick={(e) => { e.stopPropagation(); submitYesNoDecision(true) }}
            style={{
              flex: 1,
              height: responsive.isMobile ? 22 : 26,
              borderRadius: 4,
              border: 'none',
              backgroundColor: '#16a34a',
              color: 'white',
              fontSize: responsive.isMobile ? 10 : 12,
              fontWeight: 700,
              cursor: 'pointer',
              padding: 0,
            }}
          >
            Yes
          </button>
          <button
            onClick={(e) => { e.stopPropagation(); submitYesNoDecision(false) }}
            style={{
              flex: 1,
              height: responsive.isMobile ? 22 : 26,
              borderRadius: 4,
              border: 'none',
              backgroundColor: '#555',
              color: 'white',
              fontSize: responsive.isMobile ? 10 : 12,
              fontWeight: 700,
              cursor: 'pointer',
              padding: 0,
            }}
          >
            No
          </button>
        </div>
      )}

      {/* Inline +/- control strip (bottom) */}
      {isDistributeTarget && (
        <div
          onClick={(e) => e.stopPropagation()}
          style={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 2,
            padding: responsive.isMobile ? '2px 1px' : '3px 2px',
            backgroundColor: 'rgba(0, 0, 0, 0.85)',
            borderTop: '1px solid rgba(255, 140, 66, 0.5)',
            zIndex: 15,
          }}
        >
          <button
            onClick={(e) => { e.stopPropagation(); decrementDistribute(card.id) }}
            disabled={distributeAllocated <= (distributeState?.minPerTarget ?? 0)}
            style={{
              width: responsive.isMobile ? 20 : 26,
              height: responsive.isMobile ? 20 : 26,
              borderRadius: 4,
              border: 'none',
              backgroundColor: distributeAllocated <= (distributeState?.minPerTarget ?? 0) ? '#333' : '#dc2626',
              color: distributeAllocated <= (distributeState?.minPerTarget ?? 0) ? '#666' : 'white',
              fontSize: responsive.isMobile ? 14 : 16,
              fontWeight: 'bold',
              cursor: distributeAllocated <= (distributeState?.minPerTarget ?? 0) ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 0,
            }}
          >
            -
          </button>
          <span style={{
            color: 'white',
            fontSize: responsive.isMobile ? 12 : 14,
            fontWeight: 700,
            minWidth: responsive.isMobile ? 18 : 24,
            textAlign: 'center',
          }}>
            {distributeAllocated}
          </span>
          <button
            onClick={(e) => { e.stopPropagation(); incrementDistribute(card.id) }}
            disabled={distributeRemaining <= 0 || distributeAtMax}
            style={{
              width: responsive.isMobile ? 20 : 26,
              height: responsive.isMobile ? 20 : 26,
              borderRadius: 4,
              border: 'none',
              backgroundColor: (distributeRemaining <= 0 || distributeAtMax) ? '#333' : '#16a34a',
              color: (distributeRemaining <= 0 || distributeAtMax) ? '#666' : 'white',
              fontSize: responsive.isMobile ? 14 : 16,
              fontWeight: 'bold',
              cursor: (distributeRemaining <= 0 || distributeAtMax) ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 0,
            }}
          >
            +
          </button>
        </div>
      )}
    </div>
  )

  // Wrap in container for tapped battlefield cards to prevent overlap
  if (isTapped && battlefield) {
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
