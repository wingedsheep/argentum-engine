import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { useHasLegalActions } from '@/store/selectors.ts'
import type { ClientCard, EntityId } from '@/types'
import { getCardImageUrl, getScryfallFallbackUrl, MORPH_FACE_DOWN_IMAGE_URL } from '@/utils/cardImages.ts'
import { useInteraction } from '@/hooks/useInteraction.ts'
import { ManaCost } from '@/components/ui/ManaSymbols.tsx'
import {
  useResponsiveContext,
  hasMultipleCastingOptions,
  getPTColor,
  getCounterStatModifier,
  hasStatCounters,
  getGoldCounters,
  getPlagueCounters,
  getChargeCounters,
  getGemCounters,
  getDepletionCounters,
  getTrapCounters,
  getLoyaltyCounters,
  getTokenFrameGradient,
  getTokenFrameTextColor,
  getCardFallbackColor,
  getLoreCounters,
  getStunCounters,
  getFinalityCounters,
  getSupplyCounters,
  getStashCounters,
  getFlyingCounters,
  getBlightCounters,
  getFloodCounters,
  getCoinCounters,
} from '../board/shared'
import { styles } from '../board/styles'
import {
  TARGET_COLOR, TARGET_COLOR_BRIGHT, TARGET_GLOW, TARGET_GLOW_BRIGHT, TARGET_GLOW_OUTER, TARGET_SHADOW,
  SELECTED_COLOR, SELECTED_GLOW, SELECTED_SHADOW,
} from '@/styles/targetingColors.ts'
import { KeywordIcons, ActiveEffectBadges } from './CardOverlays'
import { counterManaClass } from '@/assets/icons/keywords'

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
  const toggleAttacker = useGameStore((state) => state.toggleAttacker)
  const assignBlocker = useGameStore((state) => state.assignBlocker)
  const removeBlockerAssignment = useGameStore((state) => state.removeBlockerAssignment)
  const startDraggingBlocker = useGameStore((state) => state.startDraggingBlocker)
  const stopDraggingBlocker = useGameStore((state) => state.stopDraggingBlocker)
  const draggingBlockerId = useGameStore((state) => state.draggingBlockerId)
  const startDraggingAttacker = useGameStore((state) => state.startDraggingAttacker)
  const stopDraggingAttacker = useGameStore((state) => state.stopDraggingAttacker)
  const draggingAttackerId = useGameStore((state) => state.draggingAttackerId)
  const setAttackTarget = useGameStore((state) => state.setAttackTarget)
  const startDraggingCard = useGameStore((state) => state.startDraggingCard)
  const stopDraggingCard = useGameStore((state) => state.stopDraggingCard)
  const draggingCardId = useGameStore((state) => state.draggingCardId)
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const submitTargetsDecision = useGameStore((state) => state.submitTargetsDecision)
  const decisionSelectionState = useGameStore((state) => state.decisionSelectionState)
  const toggleDecisionSelection = useGameStore((state) => state.toggleDecisionSelection)
  const distributeState = useGameStore((state) => state.distributeState)
  const incrementDistribute = useGameStore((state) => state.incrementDistribute)
  const decrementDistribute = useGameStore((state) => state.decrementDistribute)
  const counterDistributionState = useGameStore((state) => state.counterDistributionState)
  const incrementCounterRemoval = useGameStore((state) => state.incrementCounterRemoval)
  const decrementCounterRemoval = useGameStore((state) => state.decrementCounterRemoval)
  const manaSelectionState = useGameStore((state) => state.manaSelectionState)
  const toggleManaSource = useGameStore((state) => state.toggleManaSource)
  const toggleCrewCreature = useGameStore((state) => state.toggleCrewCreature)
  const toggleConvokeCreature = useGameStore((state) => state.toggleConvokeCreature)
  const submitYesNoDecision = useGameStore((state) => state.submitYesNoDecision)
  const responsive = useResponsiveContext()
  const { handleCardClick, handleDoubleClick, executeAction } = useInteraction()
  const dragStartPos = useRef<{ x: number; y: number } | null>(null)
  const handledByDrag = useRef(false)
  /** Whether the attacker was already selected when drag started (to know if short press = select or deselect) */
  const attackerWasSelected = useRef(false)

  // Hover handlers for card preview — track position via onMouseMove like the deckbuilder
  const updateHoverPosition = useGameStore((s) => s.updateHoverPosition)

  const handleMouseEnter = useCallback((e: React.MouseEvent) => {
    hoverCard(card.id, { x: e.clientX, y: e.clientY })
  }, [card.id, hoverCard])

  const handleMouseMove = useCallback((e: React.MouseEvent) => {
    updateHoverPosition({ x: e.clientX, y: e.clientY })
  }, [updateHoverPosition])

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
  const isBeingCast = isInTargetingMode && targetingState?.action != null &&
    'cardId' in targetingState.action && targetingState.action.cardId === card.id

  // Check if this card is a valid target in a pending ChooseTargetsDecision (single-requirement only)
  const isChooseTargetsDecision = pendingDecision?.type === 'ChooseTargetsDecision'
  const isSingleRequirementDecision = isChooseTargetsDecision && pendingDecision.targetRequirements.length === 1
  const decisionLegalTargets = isSingleRequirementDecision ? (pendingDecision.legalTargets[0] ?? []) : []
  const isValidDecisionTarget = decisionLegalTargets.includes(card.id)

  // Check if this card is a valid option in decision selection mode (SelectCardsDecision with useTargetingUI)
  const isValidDecisionSelection = decisionSelectionState?.validOptions.includes(card.id) ?? false
  const isSelectedDecisionOption = decisionSelectionState?.selectedOptions.includes(card.id) ?? false

  // Inline counter distribution checks (for RemoveXPlusOnePlusOneCounters cost)
  const counterCreature = counterDistributionState?.creatures.find((c) => c.entityId === card.id)
  const isCounterDistTarget = counterCreature != null
  const counterAllocated = isCounterDistTarget ? (counterDistributionState?.distribution[card.id] ?? 0) : 0
  const counterAtMax = counterCreature != null && counterAllocated >= counterCreature.availableCounters

  // Inline damage distribution checks
  const isDistributeTarget = distributeState?.targets.includes(card.id) ?? false
  const distributeAllocated = isDistributeTarget ? (distributeState?.distribution[card.id] ?? 0) : 0
  const distributeTotalAllocated = distributeState
    ? Object.values(distributeState.distribution).reduce((sum, v) => sum + v, 0)
    : 0
  const distributeRemaining = distributeState ? distributeState.totalAmount - distributeTotalAllocated : 0
  const distributeMaxForCard = distributeState?.maxPerTarget?.[card.id]
  const distributeAtMax = distributeMaxForCard !== undefined && distributeAllocated >= distributeMaxForCard

  // Mana selection checks
  const isManaValidSource = manaSelectionState?.validSources.includes(card.id) ?? false
  const isManaSelected = manaSelectionState?.selectedSources.includes(card.id) ?? false
  const isInManaSelectionMode = manaSelectionState !== null

  // Crew selection checks
  const crewSelectionState = useGameStore((state) => state.crewSelectionState)
  const isInCrewMode = crewSelectionState !== null
  const isValidCrewCreature = crewSelectionState?.validCreatures.some((c) => c.entityId === card.id) ?? false
  const isSelectedCrewCreature = crewSelectionState?.selectedCreatures.includes(card.id) ?? false

  // Convoke selection checks
  const convokeSelectionState = useGameStore((state) => state.convokeSelectionState)
  const isInConvokeMode = convokeSelectionState !== null
  const isValidConvokeCreature = convokeSelectionState?.validCreatures.some((c) => c.entityId === card.id) ?? false
  const isSelectedConvokeCreature = convokeSelectionState?.selectedCreatures.some((c) => c.entityId === card.id) ?? false

  // Trigger YesNo check (inline buttons on triggering entity card, only when inlineOnTrigger is set)
  const isTriggerYesNo = pendingDecision?.type === 'YesNoDecision'
    && pendingDecision.context.inlineOnTrigger
    && pendingDecision.context.triggeringEntityId === card.id

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
  const isSelectedAsBlocker = isInBlockerMode && !!(combatState?.blockerAssignments[card.id]?.length)
  const isAttackingInBlockerMode = isInBlockerMode && isOpponentCard && combatState.attackingCreatures.includes(card.id)
  const isMustBeBlocked = isInBlockerMode && isOpponentCard && combatState.mustBeBlockedAttackers.includes(card.id)

  // For attacker mode: check if this is an opponent's planeswalker that can be attacked
  const isValidPlaneswalkerTarget = isInAttackerMode && isOpponentCard && combatState.validAttackTargets.includes(card.id)

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
           (action.type === 'CycleCard' && action.cardId === card.id) ||
           (action.type === 'TypecycleCard' && action.cardId === card.id)
  })
  const playableAction = playableActions[0]
  // Show modal if multiple legal actions OR if card has multiple potential options (e.g., morph + normal cast)
  const hasMultiplePotentialOptions = hasMultipleCastingOptions(playableActions)
  // Also show modal for cycling lands where play land is unavailable (so player sees grayed-out "Play land")
  const isCyclingLandWithoutPlayLand = card.cardTypes.includes('LAND') &&
    playableActions.length === 1 && (playableActions[0]?.action.type === 'CycleCard' || playableActions[0]?.action.type === 'TypecycleCard')
  const shouldShowCastModal = playableActions.length > 1 || (hasMultiplePotentialOptions && playableActions.length > 0) || isCyclingLandWithoutPlayLand
  const canDragToPlay = inHand && playableAction && !isInCombatMode && !isInTargetingMode

  // Determine mana cost display for cards in hand (always show, highlight changes)
  const handCostInfo = useMemo(() => {
    if (!inHand || faceDown || !card.manaCost) return null
    // Find the normal CastSpell action (not morph, not kicked, not mode)
    const castAction = playableActions.find((a) =>
      a.action.type === 'CastSpell' && a.actionType !== 'CastFaceDown' && a.actionType !== 'CastWithKicker' && a.actionType !== 'CastSpellMode'
    )
    const effectiveCost = castAction?.manaCostString
    // If no cast action available, show base cost as-is
    if (effectiveCost == null) return { cost: card.manaCost, isReduced: false, isIncreased: false }
    // Compare with the card's base mana cost
    if (effectiveCost === card.manaCost) return { cost: card.manaCost, isReduced: false, isIncreased: false }
    // Count total mana symbols to determine if cost went up or down
    const countSymbols = (cost: string) => {
      const symbols = cost.match(/\{([^}]+)\}/g) ?? []
      return symbols.reduce((total, s) => {
        const inner = s.slice(1, -1)
        const num = parseInt(inner, 10)
        return total + (isNaN(num) ? 1 : num)
      }, 0)
    }
    const baseMV = countSymbols(card.manaCost)
    const effectiveMV = countSymbols(effectiveCost)
    const displayCost = effectiveCost === '' ? '{0}' : effectiveCost
    return {
      cost: displayCost,
      isReduced: effectiveMV < baseMV,
      isIncreased: effectiveMV > baseMV,
    }
  }, [inHand, faceDown, playableActions, card.manaCost])

  // Handle mouse/touch down - start dragging for attackers, blockers, or hand cards
  const handlePointerDown = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    const clientX = 'touches' in e ? e.touches[0]!.clientX : e.clientX
    const clientY = 'touches' in e ? e.touches[0]!.clientY : e.clientY

    // Start dragging attacker to assign attack target (planeswalker or player)
    // Works for both already-selected and unselected valid attackers
    if (isInAttackerMode && (isSelectedAsAttacker || isValidAttacker) && combatState) {
      e.preventDefault()
      dragStartPos.current = { x: clientX, y: clientY }
      attackerWasSelected.current = isSelectedAsAttacker
      if (!isSelectedAsAttacker) {
        toggleAttacker(card.id) // Select it immediately so the arrow shows
      }
      startDraggingAttacker(card.id)
      return
    }

    if (isInBlockerMode && isValidBlocker) {
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
  }, [isInAttackerMode, isSelectedAsAttacker, isValidAttacker, combatState, startDraggingAttacker, toggleAttacker, isInBlockerMode, isValidBlocker, startDraggingBlocker, canDragToPlay, startDraggingCard, card.id])

  // Handle mouse/touch up - drop blocker on attacker
  const handlePointerUp = useCallback(() => {
    if (isInBlockerMode && draggingBlockerId && isAttackingInBlockerMode) {
      // Dropping on an attacker - assign the blocker
      assignBlocker(draggingBlockerId, card.id)
      stopDraggingBlocker()
    }
    // Attacker drag drop is handled in the global handler via resolveDropTarget
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

        // Delegate to the shared executeAction flow (handles X costs, convoke, delve,
        // additional costs, targeting, damage distribution, and direct submission)
        executeAction(playableAction)
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
  }, [draggingCardId, card.id, playableAction, shouldShowCastModal, executeAction, stopDraggingCard, handleCardClick, selectCard, isInAttackerMode, isValidAttacker, toggleAttacker, isInBlockerMode, isValidBlocker, isSelectedAsBlocker, removeBlockerAssignment])

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

  // Global mouse/touch up handler for attacker drag (planeswalker targeting)
  // Uses drag distance to distinguish click (toggle attacker off) from drag (assign target)
  useEffect(() => {
    if (draggingAttackerId !== card.id) return

    const resolveDropTarget = (clientX: number, clientY: number) => {
      const elementAtPoint = document.elementFromPoint(clientX, clientY)
      if (!elementAtPoint) return

      // Check if dropped on an opponent planeswalker
      const cardEl = elementAtPoint.closest('[data-card-id]')
      if (cardEl) {
        const targetCardId = cardEl.getAttribute('data-card-id')
        if (targetCardId && combatState?.validAttackTargets.includes(targetCardId as EntityId)) {
          setAttackTarget(draggingAttackerId, targetCardId as EntityId)
          return
        }
      }

      // Check if dropped on the opponent's life display → set player as attack target
      const lifeEl = elementAtPoint.closest('[data-life-id]')
      if (lifeEl) {
        const playerId = lifeEl.getAttribute('data-life-id')
        if (playerId) {
          setAttackTarget(draggingAttackerId, playerId as EntityId)
        }
      }
    }

    const handleGlobalPointerUp = (clientX: number, clientY: number) => {
      const MIN_DRAG_DISTANCE = 30
      const start = dragStartPos.current
      const draggedFarEnough = start != null &&
        Math.hypot(clientX - start.x, clientY - start.y) >= MIN_DRAG_DISTANCE
      dragStartPos.current = null
      handledByDrag.current = true

      if (!draggedFarEnough) {
        // Short press = click
        stopDraggingAttacker()
        if (attackerWasSelected.current) {
          // Was already selected → toggle off (deselect)
          toggleAttacker(card.id)
        }
        // If wasn't selected, we already selected it in handlePointerDown → stays selected
        return
      }

      // Long drag - resolve where we dropped
      resolveDropTarget(clientX, clientY)
      stopDraggingAttacker()
    }

    const handleMouseUp = (e: MouseEvent) => handleGlobalPointerUp(e.clientX, e.clientY)
    const handleTouchEnd = (e: TouchEvent) => {
      const touch = e.changedTouches[0]
      if (touch) {
        const MIN_DRAG_DISTANCE = 30
        const start = dragStartPos.current
        const draggedFarEnough = start != null &&
          Math.hypot(touch.clientX - start.x, touch.clientY - start.y) >= MIN_DRAG_DISTANCE
        dragStartPos.current = null
        handledByDrag.current = true

        if (draggedFarEnough) {
          resolveDropTarget(touch.clientX, touch.clientY)
        } else if (attackerWasSelected.current) {
          toggleAttacker(card.id)
        }
      }
      stopDraggingAttacker()
    }

    window.addEventListener('mouseup', handleMouseUp)
    window.addEventListener('touchend', handleTouchEnd)
    return () => {
      window.removeEventListener('mouseup', handleMouseUp)
      window.removeEventListener('touchend', handleTouchEnd)
    }
  }, [draggingAttackerId, card.id, stopDraggingAttacker, toggleAttacker, combatState, setAttackTarget])

  const handleClick = () => {
    // If the drag handler already processed this interaction, skip
    if (handledByDrag.current) {
      handledByDrag.current = false
      return
    }

    // Handle mana selection mode - click to toggle source
    if (isInManaSelectionMode && isManaValidSource) {
      toggleManaSource(card.id)
      return
    }

    // Block all other interactions during mana mode
    if (isInManaSelectionMode) return

    // Handle crew selection mode - click to toggle creature
    if (isInCrewMode && isValidCrewCreature) {
      toggleCrewCreature(card.id)
      return
    }

    // Handle convoke selection mode - click to toggle creature
    if (isInConvokeMode && isValidConvokeCreature) {
      // If already selected, deselect (payingColor doesn't matter for deselect)
      if (isSelectedConvokeCreature) {
        toggleConvokeCreature(card.id, card.name, null)
      } else {
        // Determine best color payment based on creature colors and remaining cost
        const creatureInfo = convokeSelectionState.validCreatures.find((c) => c.entityId === card.id)
        const colors = creatureInfo?.colors ?? []
        // Parse remaining cost to find colored symbols still needed
        const manaCost = convokeSelectionState.manaCost
        const symbols: string[] = []
        const regex = /\{([^}]+)\}/g
        let m
        while ((m = regex.exec(manaCost)) !== null) symbols.push(m[1]!)
        // Remove symbols already covered by existing selections
        const remaining = [...symbols]
        for (const sel of convokeSelectionState.selectedCreatures) {
          if (sel.payingColor) {
            const idx = remaining.indexOf(sel.payingColor)
            if (idx >= 0) remaining.splice(idx, 1)
          } else {
            const gIdx = remaining.findIndex(s => /^\d+$/.test(s))
            if (gIdx >= 0) {
              const val = parseInt(remaining[gIdx]!, 10)
              if (val > 1) remaining[gIdx] = String(val - 1)
              else remaining.splice(gIdx, 1)
            }
          }
        }
        // Pick a color this creature can pay that's still needed
        let payingColor: string | null = null
        for (const color of colors) {
          if (remaining.includes(color)) { payingColor = color; break }
        }
        toggleConvokeCreature(card.id, card.name, payingColor)
      }
      return
    }

    // Block all other interactions during crew mode
    if (isInCrewMode) return

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
      // Clicking an opponent's planeswalker assigns the last selected attacker to it
      if (isValidPlaneswalkerTarget && combatState && combatState.selectedAttackers.length > 0) {
        const lastAttacker = combatState.selectedAttackers[combatState.selectedAttackers.length - 1]!
        setAttackTarget(lastAttacker, card.id)
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
    if (isInTargetingMode || isChooseTargetsDecision || isValidDecisionSelection || isInManaSelectionMode) {
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
  } else if (isValidPlaneswalkerTarget && combatState && Object.values(combatState.attackerTargets).includes(card.id)) {
    // Red highlight for planeswalkers currently targeted by an attacker
    borderStyle = '3px solid #ff4444'
    boxShadow = '0 0 16px rgba(255, 68, 68, 0.7), 0 0 32px rgba(255, 68, 68, 0.4)'
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
  } else if (isTriggerYesNo) {
    // Orange/gold glow for the trigger creature (matches distribute target style)
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
  } else if (isManaSelected) {
    // Green highlight for selected mana sources
    borderStyle = `3px solid ${SELECTED_COLOR}`
    boxShadow = `0 0 20px ${SELECTED_GLOW}, 0 0 40px ${SELECTED_SHADOW}`
  } else if (isManaValidSource && isHovered) {
    // Bright blue highlight when hovering over a valid mana source
    borderStyle = `3px solid ${TARGET_COLOR_BRIGHT}`
    boxShadow = `0 0 20px ${TARGET_GLOW_BRIGHT}, 0 0 40px ${TARGET_GLOW_OUTER}`
  } else if (isManaValidSource) {
    // Blue highlight for valid mana sources
    borderStyle = `2px solid ${TARGET_COLOR}`
    boxShadow = `0 0 12px ${TARGET_GLOW}, 0 0 24px ${TARGET_SHADOW}`
  } else if (isSelectedCrewCreature) {
    // Green highlight for selected crew creatures
    borderStyle = `3px solid ${SELECTED_COLOR}`
    boxShadow = `0 0 20px ${SELECTED_GLOW}, 0 0 40px ${SELECTED_SHADOW}`
  } else if (isValidCrewCreature && isHovered) {
    // Bright blue highlight when hovering over a valid crew creature
    borderStyle = `3px solid ${TARGET_COLOR_BRIGHT}`
    boxShadow = `0 0 20px ${TARGET_GLOW_BRIGHT}, 0 0 40px ${TARGET_GLOW_OUTER}`
  } else if (isValidCrewCreature) {
    // Blue highlight for valid crew creatures
    borderStyle = `2px solid ${TARGET_COLOR}`
    boxShadow = `0 0 12px ${TARGET_GLOW}, 0 0 24px ${TARGET_SHADOW}`
  } else if (isSelectedConvokeCreature) {
    // Green highlight for selected convoke creatures
    borderStyle = `3px solid ${SELECTED_COLOR}`
    boxShadow = `0 0 20px ${SELECTED_GLOW}, 0 0 40px ${SELECTED_SHADOW}`
  } else if (isValidConvokeCreature && isHovered) {
    // Bright blue highlight when hovering over a valid convoke creature
    borderStyle = `3px solid ${TARGET_COLOR_BRIGHT}`
    boxShadow = `0 0 20px ${TARGET_GLOW_BRIGHT}, 0 0 40px ${TARGET_GLOW_OUTER}`
  } else if (isValidConvokeCreature) {
    // Blue highlight for valid convoke creatures
    borderStyle = `2px solid ${TARGET_COLOR}`
    boxShadow = `0 0 12px ${TARGET_GLOW}, 0 0 24px ${TARGET_SHADOW}`
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
  } else if (isValidPlaneswalkerTarget && combatState && combatState.selectedAttackers.length > 0 && isHovered) {
    // Bright orange highlight when hovering over a valid planeswalker attack target
    borderStyle = '3px solid #ff8800'
    boxShadow = '0 0 16px rgba(255, 136, 0, 0.7), 0 0 32px rgba(255, 136, 0, 0.4)'
  } else if (isValidPlaneswalkerTarget && combatState && combatState.selectedAttackers.length > 0) {
    // Orange highlight for valid planeswalker attack targets
    borderStyle = '2px solid #ff8800'
    boxShadow = '0 0 12px rgba(255, 136, 0, 0.5), 0 0 24px rgba(255, 136, 0, 0.3)'
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
  } else if (isBeingCast) {
    // Amber border for the card currently being cast (not selectable as a cost target)
    borderStyle = '2px solid #d4a017'
    boxShadow = '0 0 12px rgba(212, 160, 23, 0.5), 0 0 24px rgba(212, 160, 23, 0.3)'
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
  const canInteract = interactive || isValidTarget || isValidDecisionTarget || isValidDecisionSelection || isValidAttacker || isValidBlocker || isAttackingInBlockerMode || isValidPlaneswalkerTarget || canDragToPlay || isDistributeTarget || isManaValidSource || isValidCrewCreature || isValidConvokeCreature
  const baseCursor = canInteract ? 'pointer' : 'default'
  const cursor = isValidBlocker || isValidAttacker || isSelectedAsAttacker || canDragToPlay ? 'grab' : baseCursor

  // Check if currently being dragged (attacker, blocker, or hand card)
  const isBeingDragged = draggingBlockerId === card.id || draggingAttackerId === card.id || draggingCardId === card.id

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
      onMouseMove={handleMouseMove}
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
        opacity: isBeingDragged ? 0.6 : isGhost ? 0.55 : (inHand && isInTargetingMode && !isValidTarget && !isBeingCast) ? 0.35 : 1,
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

      {/* Loyalty counter badge for planeswalkers */}
      {battlefield && getLoyaltyCounters(card) > 0 && (card.power === null || card.power === undefined) && (
        <div style={{
          ...styles.ptOverlay,
          backgroundColor: 'rgba(60, 60, 60, 0.9)',
          border: '1px solid rgba(200, 160, 60, 0.6)',
        }}>
          <i className={`ms ms-${counterManaClass.LOYALTY}`} style={{ fontSize: responsive.isMobile ? 8 : 10, color: '#e0c060', marginRight: 2 }} />
          <span style={{
            color: '#e0c060',
            fontWeight: 700,
            fontSize: responsive.isMobile ? 10 : 12,
          }}>
            {getLoyaltyCounters(card)}
          </span>
        </div>
      )}

      {/* Loyalty counter badge for animated planeswalkers (has both P/T and loyalty) */}
      {battlefield && getLoyaltyCounters(card) > 0 && card.power !== null && card.power !== undefined && (
        <div style={{
          ...styles.chargeCounterBadge,
          backgroundColor: 'rgba(60, 60, 60, 0.9)',
          border: '1px solid rgba(200, 160, 60, 0.6)',
          color: '#e0c060',
        }}>
          <i className={`ms ms-${counterManaClass.LOYALTY}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getLoyaltyCounters(card)}
          </span>
        </div>
      )}

      {/* Threshold progress badge (graveyard-gated static abilities) */}
      {card.thresholdInfo && (
        <div
          title={`Graveyard threshold: ${card.thresholdInfo.current}/${card.thresholdInfo.required}${card.thresholdInfo.active ? ' (active)' : ''}`}
          style={{
            position: 'absolute',
            top: 2,
            left: 2,
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            padding: responsive.isMobile ? '1px 4px' : '2px 6px',
            borderRadius: 8,
            fontSize: responsive.isMobile ? 9 : 11,
            fontWeight: 700,
            background: card.thresholdInfo.active
              ? 'linear-gradient(135deg, #c9a227, #f5d76e)'
              : 'rgba(20, 20, 20, 0.78)',
            color: card.thresholdInfo.active ? '#1a1200' : '#e6e6e6',
            border: card.thresholdInfo.active
              ? '1px solid #fff3b0'
              : '1px solid rgba(255,255,255,0.25)',
            boxShadow: card.thresholdInfo.active
              ? '0 0 6px rgba(245, 215, 110, 0.85)'
              : '0 1px 2px rgba(0,0,0,0.5)',
            pointerEvents: 'none',
            zIndex: 3,
          }}
        >
          <i className="ms ms-counter-graveyard" style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span>
            {Math.min(card.thresholdInfo.current, card.thresholdInfo.required)}/{card.thresholdInfo.required}
          </span>
        </div>
      )}

      {/* Counter badge for creatures with +1/+1 or -1/-1 counters */}
      {battlefield && hasStatCounters(card) && (
        <div style={{
          ...styles.counterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${getCounterStatModifier(card) >= 0 ? counterManaClass.PLUS_ONE_PLUS_ONE : counterManaClass.MINUS_ONE_MINUS_ONE}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={styles.counterBadgeText}>
            {getCounterStatModifier(card) >= 0 ? '+' : ''}{getCounterStatModifier(card)}
          </span>
        </div>
      )}

      {/* Gold counter badge */}
      {battlefield && getGoldCounters(card) > 0 && (
        <div style={{
          ...styles.goldCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.GOLD}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getGoldCounters(card)}
          </span>
        </div>
      )}

      {/* Plague counter badge */}
      {battlefield && getPlagueCounters(card) > 0 && (
        <div style={{
          ...styles.plagueCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.PLAGUE}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getPlagueCounters(card)}
          </span>
        </div>
      )}

      {/* Charge counter badge */}
      {battlefield && getChargeCounters(card) > 0 && (
        <div style={{
          ...styles.chargeCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.CHARGE}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getChargeCounters(card)}
          </span>
        </div>
      )}

      {/* Gem counter badge */}
      {battlefield && getGemCounters(card) > 0 && (
        <div style={{
          ...styles.chargeCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.GEM}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getGemCounters(card)}
          </span>
        </div>
      )}

      {/* Depletion counter badge */}
      {battlefield && getDepletionCounters(card) > 0 && (
        <div style={{
          ...styles.depletionCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.DEPLETION}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getDepletionCounters(card)}
          </span>
        </div>
      )}

      {/* Trap counter badge */}
      {battlefield && getTrapCounters(card) > 0 && (
        <div style={{
          ...styles.trapCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.TRAP}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getTrapCounters(card)}
          </span>
        </div>
      )}

      {/* Stun counter badge */}
      {battlefield && getStunCounters(card) > 0 && (
        <div style={{
          ...styles.stunCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.STUN}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getStunCounters(card)}
          </span>
        </div>
      )}

      {/* Finality counter badge */}
      {battlefield && getFinalityCounters(card) > 0 && (
        <div style={{
          ...styles.finalityCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.FINALITY}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getFinalityCounters(card)}
          </span>
        </div>
      )}

      {/* Supply counter badge */}
      {battlefield && getSupplyCounters(card) > 0 && (
        <div style={{
          ...styles.supplyCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.SUPPLY}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getSupplyCounters(card)}
          </span>
        </div>
      )}

      {/* Stash counter badge */}
      {battlefield && getStashCounters(card) > 0 && (
        <div style={{
          ...styles.stashCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.STASH}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getStashCounters(card)}
          </span>
        </div>
      )}

      {/* Blight counter badge */}
      {battlefield && getBlightCounters(card) > 0 && (
        <div style={{
          ...styles.blightCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.BLIGHT}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getBlightCounters(card)}
          </span>
        </div>
      )}

      {/* Flood counter badge */}
      {battlefield && getFloodCounters(card) > 0 && (
        <div style={{
          ...styles.floodCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.FLOOD}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getFloodCounters(card)}
          </span>
        </div>
      )}

      {/* Coin counter badge */}
      {battlefield && getCoinCounters(card) > 0 && (
        <div style={{
          ...styles.coinCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.COIN}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getCoinCounters(card)}
          </span>
        </div>
      )}

      {/* Flying counter badge */}
      {battlefield && getFlyingCounters(card) > 0 && (
        <div style={{
          ...styles.flyingCounterBadge,
          fontSize: responsive.isMobile ? 9 : 11,
          padding: responsive.isMobile ? '1px 4px' : '2px 6px',
        }}>
          <i className={`ms ms-${counterManaClass.FLYING}`} style={{ fontSize: responsive.isMobile ? 8 : 10 }} />
          <span style={{ fontWeight: 700 }}>
            {getFlyingCounters(card)}
          </span>
        </div>
      )}

      {/* Saga lore counter badge and chapter progress track */}
      {battlefield && card.subtypes.includes('Saga') && (() => {
        const loreCount = getLoreCounters(card)
        const totalChapters = card.sagaTotalChapters ?? 3
        const toRoman = (n: number) => ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII'][n - 1] ?? String(n)
        return (
          <>
            {/* Chapter progress track along left edge */}
            <div style={{
              ...styles.sagaChapterTrack,
            }}>
              {Array.from({ length: totalChapters }, (_, i) => {
                const chapter = i + 1
                const isActive = loreCount === chapter
                const isCompleted = loreCount > chapter
                return (
                  <div key={chapter} style={{
                    ...styles.sagaChapterMarker,
                    ...(isActive ? styles.sagaChapterActive : {}),
                    ...(isCompleted ? styles.sagaChapterCompleted : {}),
                    width: responsive.isMobile ? 14 : 18,
                    height: responsive.isMobile ? 14 : 18,
                    fontSize: responsive.isMobile ? 7 : 9,
                  }}>
                    {toRoman(chapter)}
                  </div>
                )
              })}
            </div>
            {/* Lore counter badge in P/T position */}
            {loreCount > 0 && (
              <div style={{
                ...styles.sagaLoreBadge,
                fontSize: responsive.isMobile ? 10 : 12,
                padding: responsive.isMobile ? '1px 4px' : '2px 6px',
              }}>
                <span style={{ fontWeight: 700 }}>
                  {loreCount} / {totalChapters}
                </span>
              </div>
            )}
          </>
        )
      })()}

      {/* Class enchantment level badge and progress track */}
      {battlefield && card.classLevel != null && card.classMaxLevel != null && (() => {
        const currentLevel = card.classLevel
        const maxLevel = card.classMaxLevel
        return (
          <>
            {/* Level progress track along left edge */}
            <div style={{
              ...styles.classLevelTrack,
            }}>
              {Array.from({ length: maxLevel }, (_, i) => {
                const level = i + 1
                const isActive = currentLevel === level
                const isCompleted = currentLevel > level
                return (
                  <div key={level} style={{
                    ...styles.classLevelMarker,
                    ...(isActive ? styles.classLevelActive : {}),
                    ...(isCompleted ? styles.classLevelCompleted : {}),
                    width: responsive.isMobile ? 14 : 18,
                    height: responsive.isMobile ? 14 : 18,
                    fontSize: responsive.isMobile ? 7 : 9,
                  }}>
                    {level}
                  </div>
                )
              })}
            </div>
            {/* Level badge in P/T position */}
            <div style={{
              ...styles.classLevelBadge,
              fontSize: responsive.isMobile ? 10 : 12,
              padding: responsive.isMobile ? '1px 4px' : '2px 6px',
            }}>
              <span style={{ fontWeight: 700 }}>
                Lv.{currentLevel}
              </span>
            </div>
          </>
        )
      })()}

      {/* Keyword ability icons (shown for face-up cards, and for face-down cards with granted keywords) */}
      {battlefield && (card.keywords.length > 0 || (card.abilityFlags && card.abilityFlags.length > 0) || (card.protections && card.protections.length > 0)) && (
        <KeywordIcons keywords={card.keywords} abilityFlags={card.abilityFlags ?? []} protections={card.protections ?? []} size={responsive.isMobile ? 14 : 18} />
      )}

      {/* Revealed face-down eye icon (e.g., peeked via Spy Network) */}
      {faceDown && card.revealedName && (
        <div style={{
          position: 'absolute',
          top: responsive.isMobile ? 2 : 4,
          right: responsive.isMobile ? 2 : 4,
          backgroundColor: 'rgba(0, 0, 0, 0.75)',
          color: '#66ccff',
          fontSize: responsive.isMobile ? 10 : 13,
          padding: responsive.isMobile ? '1px 3px' : '2px 5px',
          borderRadius: 4,
          border: '1px solid rgba(102, 204, 255, 0.5)',
          pointerEvents: 'none',
          zIndex: 5,
          lineHeight: 1,
        }}>
          👁
        </div>
      )}

      {/* Chosen creature type / color badge (e.g., Doom Cannon, Riptide Replicator) */}
      {!faceDown && (card.chosenCreatureType ?? card.chosenColor) && (
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
          {[card.chosenColor, card.chosenCreatureType].filter(Boolean).join(' ')}
        </div>
      )}

      {/* DFC (double-faced card) indicator badge */}
      {!faceDown && battlefield && card.isDoubleFaced && (
        <div style={{
          position: 'absolute',
          top: card.copyOf ? 20 : 4,
          left: 4,
          backgroundColor: 'rgba(20, 20, 40, 0.9)',
          color: card.currentFace === 'BACK' ? '#b0b8d0' : '#f0d060',
          fontSize: responsive.isMobile ? 10 : 13,
          padding: responsive.isMobile ? '1px 3px' : '2px 5px',
          borderRadius: 4,
          border: `1px solid ${card.currentFace === 'BACK' ? 'rgba(160, 170, 200, 0.5)' : 'rgba(240, 208, 96, 0.5)'}`,
          pointerEvents: 'none',
          zIndex: 5,
          lineHeight: 1,
        }}>
          <i className={`ms ms-dfc-${card.currentFace === 'BACK' ? 'night' : 'day'}`} />
        </div>
      )}

      {/* Copy indicator badge (e.g., Clever Impersonator copying Wind Drake) */}
      {!faceDown && card.copyOf && (
        <div style={{
          position: 'absolute',
          top: 4,
          left: 4,
          backgroundColor: 'rgba(40, 40, 80, 0.9)',
          color: '#a0b0e0',
          fontSize: responsive.isMobile ? 7 : 9,
          padding: '1px 4px',
          borderRadius: 3,
          border: '1px solid rgba(100, 120, 200, 0.6)',
          whiteSpace: 'nowrap',
          pointerEvents: 'none',
          zIndex: 5,
        }}>
          {card.copyOf}
        </div>
      )}

      {/* Active effect badges (evasion, etc.) */}
      {battlefield && card.activeEffects && card.activeEffects.length > 0 && (
        <ActiveEffectBadges effects={card.activeEffects} />
      )}

      {/* Playable indicator glow effect (only outside combat mode) */}
      {isPlayable && !isSelected && (
        <div style={styles.playableGlow} />
      )}

      {/* Mana cost overlay for cards in hand — always shown, color-coded when modified */}
      {handCostInfo && (
        <div style={{
          position: 'absolute',
          top: responsive.isMobile ? 2 : 4,
          right: responsive.isMobile ? 2 : 4,
          backgroundColor: handCostInfo.isReduced || handCostInfo.isIncreased
            ? 'rgba(0, 0, 0, 0.85)'
            : 'rgba(0, 0, 0, 0.7)',
          padding: responsive.isMobile ? '1px 3px' : '2px 5px',
          borderRadius: 4,
          border: `1px solid ${
            handCostInfo.isReduced ? 'rgba(0, 200, 80, 0.5)'
            : handCostInfo.isIncreased ? 'rgba(255, 68, 68, 0.5)'
            : 'rgba(255, 255, 255, 0.3)'
          }`,
          boxShadow: handCostInfo.isReduced ? '0 0 6px rgba(0, 200, 80, 0.3)'
            : handCostInfo.isIncreased ? '0 0 6px rgba(255, 68, 68, 0.3)'
            : 'none',
          pointerEvents: 'none',
          zIndex: 10,
          display: 'flex',
          alignItems: 'center',
          gap: 1,
        }}>
          <ManaCost cost={handCostInfo.cost} size={responsive.isMobile ? 10 : 13} gap={1} />
        </div>
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

      {/* Inline Yes/No buttons for trigger (bottom) */}
      {isTriggerYesNo && pendingDecision?.type === 'YesNoDecision' && (
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

      {/* "Casting" badge for the spell being cast during cost selection */}
      {isBeingCast && (
        <div style={{
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'rgba(0, 0, 0, 0.45)',
          borderRadius: 'inherit',
          zIndex: 15,
          pointerEvents: 'none',
        }}>
          <span style={{
            backgroundColor: 'rgba(212, 160, 23, 0.9)',
            color: '#fff',
            fontSize: responsive.isMobile ? 10 : 13,
            fontWeight: 700,
            padding: responsive.isMobile ? '2px 6px' : '3px 10px',
            borderRadius: 4,
            textTransform: 'uppercase',
            letterSpacing: 1,
          }}>
            Casting
          </span>
        </div>
      )}

      {/* Inline +/- control strip for counter removal (top, so counter badges stay visible) */}
      {isCounterDistTarget && (
        <div
          onClick={(e) => e.stopPropagation()}
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 2,
            padding: responsive.isMobile ? '2px 1px' : '3px 2px',
            backgroundColor: 'rgba(0, 0, 0, 0.85)',
            borderBottom: '1px solid rgba(234, 179, 8, 0.5)',
            zIndex: 15,
          }}
        >
          <button
            onClick={(e) => { e.stopPropagation(); decrementCounterRemoval(card.id) }}
            disabled={counterAllocated <= 0}
            style={{
              width: responsive.isMobile ? 20 : 26,
              height: responsive.isMobile ? 20 : 26,
              borderRadius: 4,
              border: 'none',
              backgroundColor: counterAllocated <= 0 ? '#333' : '#dc2626',
              color: counterAllocated <= 0 ? '#666' : 'white',
              fontSize: responsive.isMobile ? 14 : 16,
              fontWeight: 'bold',
              cursor: counterAllocated <= 0 ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: 0,
            }}
          >
            -
          </button>
          <span style={{
            color: '#eab308',
            fontSize: responsive.isMobile ? 12 : 14,
            fontWeight: 700,
            minWidth: responsive.isMobile ? 18 : 24,
            textAlign: 'center',
          }}>
            {counterAllocated}
          </span>
          <button
            onClick={(e) => { e.stopPropagation(); incrementCounterRemoval(card.id) }}
            disabled={counterAtMax}
            style={{
              width: responsive.isMobile ? 20 : 26,
              height: responsive.isMobile ? 20 : 26,
              borderRadius: 4,
              border: 'none',
              backgroundColor: (counterAtMax) ? '#333' : '#16a34a',
              color: (counterAtMax) ? '#666' : 'white',
              fontSize: responsive.isMobile ? 14 : 16,
              fontWeight: 'bold',
              cursor: (counterAtMax) ? 'not-allowed' : 'pointer',
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
        pointerEvents: 'none',
      }}>
        {cardElement}
      </div>
    )
  }

  return cardElement
}
