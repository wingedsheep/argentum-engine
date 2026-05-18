import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useGameStore } from '@/store/gameStore.ts'
import { useHasLegalActions } from '@/store/selectors.ts'
import type { ClientCard, EntityId } from '@/types'
import { Color, ColorSymbols, Keyword } from '@/types/enums'
import { getCardImageUrl, getScryfallFallbackUrl, MORPH_FACE_DOWN_IMAGE_URL } from '@/utils/cardImages.ts'
import { useInteraction } from '@/hooks/useInteraction.ts'
import { ManaCost } from '@/components/ui/ManaSymbols.tsx'
import { HoverCardPreview } from '@/components/ui/HoverCardPreview.tsx'
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
  getFirstStrikeCounters,
  getLifelinkCounters,
  getBlightCounters,
  getFloodCounters,
  getCoinCounters,
  getChorusCounters,
  getDreamCounters,
  getQuestCounters,
  getGrowthCounters,
} from '../board/shared'
import { styles, bandColorFor } from '../board/styles'
import {
  TARGET_COLOR, TARGET_COLOR_BRIGHT, TARGET_GLOW, TARGET_GLOW_BRIGHT, TARGET_GLOW_OUTER, TARGET_SHADOW,
  SELECTED_COLOR, SELECTED_GLOW, SELECTED_SHADOW,
} from '@/styles/targetingColors.ts'
import { KeywordIcons, ActiveEffectBadges } from './CardOverlays'
import { counterManaClass, counterSvgIcon } from '@/assets/icons/keywords'
import { SvgGlyph } from '@/assets/icons/SvgGlyph'
import { RenderProfiler } from '@/utils/renderProfiler'

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
  /** Suppress the built-in tap rotation even when the card is tapped. Used when an outer
   * wrapper is rotating the entire attachment stack so this card doesn't double-rotate. */
  suppressTapRotation?: boolean
  /** Hide the keyword-ability icon overlay. Used for peeking attachments where only a
   * sliver of the card is visible and the icons just clutter the parent underneath. */
  hideKeywordIcons?: boolean
  /** Ghost card from graveyard (translucent, purple glow) */
  isGhost?: boolean
  /**
   * Allow dragging this card to cast it even when it's not in hand. Used by the command zone
   * widget so commanders can be cast with the same drag-to-play gesture as hand cards. Drop
   * heuristic (above the hand top = cast) is unchanged — dragging the commander toward the
   * battlefield casts it; dropping anywhere along the bottom cancels.
   */
  enableDragToCast?: boolean
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
  suppressTapRotation = false,
  hideKeywordIcons = false,
  isGhost = false,
  enableDragToCast = false,
}: GameCardProps) {
  const voidActive = useGameStore(
    (state) => (state.spectatingState?.gameState ?? state.gameState)?.voidActive ?? false
  )
  // Server-side combat attackers (used to render bands during declare-blockers).
  const serverCombatAttackers = useGameStore(
    (state) => (state.spectatingState?.gameState ?? state.gameState)?.combat?.attackers ?? null
  )
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
  const linkBand = useGameStore((state) => state.linkBand)
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
  const isBeheldPulsing = useGameStore((state) => state.beholdPulses.some((p) => p.cardId === card.id))
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

  // Hover state for the copy-of badge — shows the *original* card preview
  // (e.g. Mockingbird) instead of the copied card's preview (e.g. Glory Seeker).
  const [copyBadgeHoverPos, setCopyBadgeHoverPos] = useState<{ x: number; y: number } | null>(null)

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

  const isTapped = suppressTapRotation ? false : (card.isTapped || forceTapped)
  // Rooms (CR 709.5) are printed landscape; rotate the permanent +90° on the battlefield
  // so the image reads landscape (the source orientation matches "tilt head right").
  // Tap state stacks an additional +90° on top (= 180° upside-down portrait), preserving
  // the standard "tap = +90° from current" semantic.
  const isRoomLandscape = !faceDown && !!battlefield && card.isRoom === true
  const totalRotateDeg = (isRoomLandscape ? 90 : 0) + (isTapped ? 90 : 0)
  const needsLandscapeContainer = Math.abs(totalRotateDeg) % 180 === 90
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

  // Inline counter distribution checks (for RemoveXPlusOnePlusOneCounters cost
  // and Dawnhand Dissident's `RemoveCountersFromYourCreatures` cost). The slice
  // tracks allocation per (entityId, counterType); we render one +/- row per
  // type, so build the row list here from `availableCountersByType`. Single-type
  // creatures get one row; multi-type creatures (e.g. +1/+1 + stun) get one row
  // per type with independent caps.
  const counterCreature = counterDistributionState?.creatures.find((c) => c.entityId === card.id)
  const isCounterDistTarget = counterCreature != null
  const counterDistInner: Record<string, number> | undefined = isCounterDistTarget
    ? counterDistributionState?.distribution[card.id]
    : undefined
  const counterRows: ReadonlyArray<{ counterType: string; available: number; allocated: number }> =
    isCounterDistTarget && counterCreature
      ? (() => {
          const byType = counterCreature.availableCountersByType
          const types =
            byType && Object.keys(byType).length > 0 ? Object.keys(byType) : ['+1/+1']
          return types
            .map((t) => ({
              counterType: t,
              available: byType?.[t] ?? counterCreature.availableCounters,
              allocated: counterDistInner?.[t] ?? 0,
            }))
            .filter((r) => r.available > 0)
        })()
      : []

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

  // Banding (CR 702.21): surface a colored ring + corner badge on every banded
  // attacker so the player can see which creatures will be blocked as a group, both
  // during their own declare-attackers (client-side combatState.bands) and during
  // the defender's declare-blockers (server-side combat.attackers[].bandId).
  const bandIndex = (() => {
    if (isInAttackerMode) {
      const localIdx = combatState.bands.findIndex((band) => band.includes(card.id))
      if (localIdx !== -1) return localIdx
    }
    const serverAttacker = serverCombatAttackers?.find((a) => a.creatureId === card.id)
    const serverBandId = serverAttacker?.bandId
    if (!serverBandId) return -1
    // Order bands by the *first* appearance of each unique bandId in attacker order,
    // so band coloring is stable and matches what the attacker submitted.
    const seen: string[] = []
    for (const att of serverCombatAttackers ?? []) {
      if (att.bandId && !seen.includes(att.bandId)) seen.push(att.bandId)
    }
    return seen.indexOf(serverBandId)
  })()
  const isBanded = bandIndex >= 0

  // Banding drag-and-drop: while another player's attacker is being dragged, this card
  // is a legal band drop target iff it's a valid attacker (excluding the dragged one),
  // and at least one of (drag source, this card) has BANDING.
  const draggingAttackerHasBanding = useGameStore((state) => state.draggingAttackerHasBanding)
  const cardHasBanding = card.keywords.includes('BANDING' as Keyword)
  const isBandDropTarget =
    isInAttackerMode &&
    !!draggingAttackerId &&
    draggingAttackerId !== card.id &&
    combatState.validCreatures.includes(card.id) &&
    (cardHasBanding || draggingAttackerHasBanding === true)

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

  // Check if this card can be played/cast (for drag-to-play).
  // Memoized so the filter only runs when legalActions actually changes —
  // otherwise re-runs on every unrelated GameCard re-render (hover, etc.).
  const playableActions = useMemo(() => legalActions.filter((a) => {
    const action = a.action
    return (action.type === 'PlayLand' && action.cardId === card.id) ||
           (action.type === 'CastSpell' && action.cardId === card.id) ||
           (action.type === 'CycleCard' && action.cardId === card.id) ||
           (action.type === 'TypecycleCard' && action.cardId === card.id)
  }), [legalActions, card.id])
  const playableAction = playableActions[0]
  // Show modal if multiple legal actions OR if card has multiple potential options (e.g., morph + normal cast)
  const hasMultiplePotentialOptions = hasMultipleCastingOptions(playableActions)
  // Also show modal for cycling lands where play land is unavailable (so player sees grayed-out "Play land")
  const isCyclingLandWithoutPlayLand = card.cardTypes.includes('LAND') &&
    playableActions.length === 1 && (playableActions[0]?.action.type === 'CycleCard' || playableActions[0]?.action.type === 'TypecycleCard')
  const shouldShowCastModal = playableActions.length > 1 || (hasMultiplePotentialOptions && playableActions.length > 0) || isCyclingLandWithoutPlayLand
  const canDragToPlay = (inHand || enableDragToCast) && playableAction && !isInCombatMode && !isInTargetingMode

  // Determine mana cost display for cards the player can cast directly from a face-up zone
  // (hand or, for Commander, the command zone). Commander tax (CR 903.8) folds in here for free
  // because the server's `enumerateCommandZone` enumerator already builds CastSpell actions with
  // the post-tax `manaCostString` — we just have to read whichever cost the active action carries.
  const showCastCostOverlay = inHand || enableDragToCast
  const handCostInfo = useMemo(() => {
    if (!showCastCostOverlay || faceDown || !card.manaCost) return null
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
  }, [showCastCostOverlay, faceDown, playableActions, card.manaCost])

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
      startDraggingAttacker(card.id, card.keywords.includes('BANDING' as Keyword))
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
        const targetCardId = cardEl.getAttribute('data-card-id') as EntityId | null
        if (targetCardId && combatState?.validAttackTargets.includes(targetCardId)) {
          setAttackTarget(draggingAttackerId, targetCardId)
          return
        }
        // CR 702.21: drop on another of your valid attackers → form/extend a band.
        // validCreatures is the canonical set of creatures the viewing player can
        // attack with, so checking membership keeps this path scoped to legal targets.
        if (
          targetCardId &&
          targetCardId !== draggingAttackerId &&
          combatState?.validCreatures.includes(targetCardId)
        ) {
          const sourceHasBanding = card.keywords.includes('BANDING' as Keyword)
          const targetHasBanding = cardEl.getAttribute('data-banding') === 'true'
          if (sourceHasBanding || targetHasBanding) {
            // CR 702.21c: a band may contain at most one creature without banding.
            // Reject the drag client-side when the resulting band would exceed that.
            // We read banding status of existing band members via the data-banding
            // attribute set on each card element; missing attribute = no banding.
            const hasBandingFor = (memberId: EntityId): boolean => {
              if (memberId === draggingAttackerId) return sourceHasBanding
              if (memberId === targetCardId) return targetHasBanding
              const el = document.querySelector(`[data-card-id="${memberId}"]`)
              return el?.getAttribute('data-banding') === 'true'
            }
            const bands = combatState.bands
            const sourceBand = bands.find((b) => b.includes(draggingAttackerId)) ?? []
            const targetBand = bands.find((b) => b.includes(targetCardId)) ?? []
            const merged = new Set<EntityId>([
              draggingAttackerId,
              targetCardId,
              ...sourceBand,
              ...targetBand,
            ])
            const nonBandingCount = Array.from(merged).filter((id) => !hasBandingFor(id)).length
            if (nonBandingCount > 1) {
              // Illegal band — silently no-op. The banding panel's illegal-reason
              // text already explains the rule when there's a single live selection.
              return
            }
            linkBand(draggingAttackerId, targetCardId, sourceHasBanding, targetHasBanding)
            return
          }
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
  }, [draggingAttackerId, card.id, card.keywords, stopDraggingAttacker, toggleAttacker, combatState, setAttackTarget, linkBand])

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
        // Determine best color payment based on creature colors and remaining cost.
        // The backend sends creature colors as Color enum names ("WHITE", "BLUE"...)
        // but mana costs parse as pip letters ("W", "U"...), so we normalise to pip
        // letters for comparison. payingColor submitted back to the server stays as
        // the Color enum name so kotlinx serialization can deserialize it.
        const creatureInfo = convokeSelectionState.validCreatures.find((c) => c.entityId === card.id)
        const colorNames = creatureInfo?.colors ?? []
        const colorPips = colorNames.map(c => ColorSymbols[c as Color] ?? c)
        // Parse remaining cost to find colored symbols still needed
        const manaCost = convokeSelectionState.manaCost
        const symbols: string[] = []
        const regex = /\{([^}]+)\}/g
        let m
        while ((m = regex.exec(manaCost)) !== null) symbols.push(m[1]!)
        // Remove symbols already covered by existing selections. Hybrid pips (CR 107.4e)
        // are colored symbols of both halves, so a previous W selection can consume a
        // {W/U} pip. Prefer exact colored matches before hybrids to avoid wasting pips.
        const hybridCovers = (sym: string, pip: string): boolean =>
          sym.includes('/') && sym.split('/').includes(pip)
        const remaining = [...symbols]
        for (const sel of convokeSelectionState.selectedCreatures) {
          if (sel.payingColor) {
            const pip = ColorSymbols[sel.payingColor as Color] ?? sel.payingColor
            const idx = remaining.indexOf(pip)
            if (idx >= 0) { remaining.splice(idx, 1); continue }
            const hIdx = remaining.findIndex(s => hybridCovers(s, pip))
            if (hIdx >= 0) remaining.splice(hIdx, 1)
          } else {
            const gIdx = remaining.findIndex(s => /^\d+$/.test(s))
            if (gIdx >= 0) {
              const val = parseInt(remaining[gIdx]!, 10)
              if (val > 1) remaining[gIdx] = String(val - 1)
              else remaining.splice(gIdx, 1)
            }
          }
        }
        // Pick a color this creature can pay that's still needed. Exact colored pips
        // first, then hybrid pips where one half matches.
        let payingColor: string | null = null
        for (let i = 0; i < colorPips.length; i++) {
          if (remaining.includes(colorPips[i]!)) { payingColor = colorNames[i]!; break }
        }
        if (!payingColor) {
          for (let i = 0; i < colorPips.length; i++) {
            if (remaining.some(s => hybridCovers(s, colorPips[i]!))) { payingColor = colorNames[i]!; break }
          }
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
    // Red for attacking creatures. Layer a banded-color outer glow on top when the
    // attacker is part of a band so the grouping reads alongside the attacker ring.
    borderStyle = '3px solid #ff4444'
    if (isBanded) {
      const band = bandColorFor(bandIndex)
      borderStyle = `3px solid ${band.border}`
      boxShadow = `0 0 16px rgba(255, 68, 68, 0.6), 0 0 0 4px ${band.base}, 0 0 28px ${band.glow}`
    } else {
      boxShadow = '0 0 16px rgba(255, 68, 68, 0.7), 0 0 32px rgba(255, 68, 68, 0.4)'
    }
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
    // Orange glow for attackers that can be blocked. Layer the band-color ring on
    // top when the attacker is part of a band so the defender sees the grouping
    // alongside the standard blockable indicator.
    borderStyle = '3px solid #ff8800'
    if (isBanded) {
      const band = bandColorFor(bandIndex)
      boxShadow = `0 0 12px rgba(255, 136, 0, 0.6), 0 0 0 4px ${band.base}, 0 0 22px ${band.glow}`
    } else {
      boxShadow = '0 0 12px rgba(255, 136, 0, 0.6), 0 0 24px rgba(255, 136, 0, 0.3)'
    }
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

  // Void ability word (Edge of Eternities): when the global void condition is satisfied this
  // turn and this card has a "Void —" ability, render an eerie cosmic halo — a tight dark
  // indigo rim with a soft violet/starlight bloom layered on top of any other state glow.
  // The em dash check covers both U+2014 ("—") and the ASCII fallback "--".
  const hasVoidAbility =
    card.oracleText.includes('Void —') || card.oracleText.includes('Void --')
  const voidEligible = inHand && !faceDown && voidActive && hasVoidAbility
  if (voidEligible) {
    // Inner: razor-thin near-black ring (the "edge of nothing").
    // Mid:   cool violet bloom.
    // Outer: faint cyan-purple starlight diffused into the dark.
    const voidHalo = [
      '0 0 0 1px rgba(8, 4, 20, 0.95)',
      '0 0 8px rgba(60, 22, 120, 0.7)',
      '0 0 22px rgba(96, 44, 180, 0.55)',
      '0 0 44px rgba(150, 100, 230, 0.28)',
      '0 0 72px rgba(80, 60, 200, 0.18)',
    ].join(', ')
    boxShadow = boxShadow ? `${boxShadow}, ${voidHalo}` : voidHalo
  }

  // Band drop-target hint (CR 702.21). Layered on top of any existing style so an
  // already-selected attacker still reads as such while showing it's a viable drop.
  if (isBandDropTarget) {
    const band = bandColorFor(isBanded ? bandIndex : combatState!.bands.length)
    boxShadow = `0 0 0 3px ${band.base}, 0 0 22px ${band.glow}` + (boxShadow ? `, ${boxShadow}` : '')
  }

  // Determine cursor
  const canInteract = interactive || isValidTarget || isValidDecisionTarget || isValidDecisionSelection || isValidAttacker || isValidBlocker || isAttackingInBlockerMode || isValidPlaneswalkerTarget || canDragToPlay || isDistributeTarget || isManaValidSource || isValidCrewCreature || isValidConvokeCreature
  const baseCursor = canInteract ? 'pointer' : 'default'
  const cursor = isValidBlocker || isValidAttacker || isSelectedAsAttacker || canDragToPlay ? 'grab' : baseCursor

  // Check if currently being dragged (attacker, blocker, or hand card)
  const isBeingDragged = draggingBlockerId === card.id || draggingAttackerId === card.id || draggingCardId === card.id

  // Container dimensions - expand width when the card sits sideways (tapped permanents
  // and Rooms always-landscape) to prevent overlap with neighbours.
  const containerWidth = needsLandscapeContainer && battlefield ? height + 8 : width
  const containerHeight = height

  const cardElement = (
    <div
      data-card-id={card.id}
      {...(card.keywords.includes('BANDING' as Keyword) ? { 'data-banding': 'true' } : {})}
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
        border: isBeheldPulsing ? '3px solid #eab308' : borderStyle,
        pointerEvents: 'auto',
        transform: `${totalRotateDeg ? `rotate(${totalRotateDeg}deg)` : ''} ${isSelected && (!isInCombatMode || !isCombatRoleCard) ? 'translateY(-8px)' : ''}`,
        transformOrigin: 'center',
        // Commander gold *glow* — soft halo, deliberately no hard 1–2px rim so it doesn't read
        // like the playable-action outline. Inner halo sits close to the card for readable
        // intensity; outer halo gives the glow some reach without bleeding too far.
        boxShadow: card.isCommander && !faceDown
          ? `${boxShadow}, 0 0 6px 2px rgba(212, 175, 55, 0.6), 0 0 14px 4px rgba(212, 175, 55, 0.3)`
          : boxShadow,
        opacity: isBeingDragged ? 0.6 : isGhost ? 0.55 : (inHand && isInTargetingMode && !isValidTarget && !isBeingCast) ? 0.35 : 1,
        userSelect: 'none',
        ...(voidEligible ? { outline: '1px solid rgba(140, 90, 220, 0.55)', outlineOffset: '2px' } : {}),
        ...(isBeheldPulsing ? { animation: 'beholdPulse 1.1s ease-in-out infinite alternate' } : {}),
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
            fontSize: responsive.badges.smallLabelFontSize,
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
          <div style={{ ...styles.summoningSicknessIcon, fontSize: responsive.badges.sicknessIconSize }}>💤</div>
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
            fontSize: responsive.badges.ptFontSize,
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
            fontSize: responsive.badges.ptFontSize,
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
          <i className={`ms ms-${counterManaClass.LOYALTY}`} style={{ fontSize: responsive.badges.counterIconFontSize, color: '#e0c060', marginRight: 2 }} />
          <span style={{
            color: '#e0c060',
            fontWeight: 700,
            fontSize: responsive.badges.ptFontSize,
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
          <i className={`ms ms-${counterManaClass.LOYALTY}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
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
            bottom: 2,
            left: 2,
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            padding: responsive.badges.badgePadding,
            borderRadius: 8,
            fontSize: responsive.badges.counterTextFontSize,
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
          <i className="ms ms-counter-graveyard" style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span>
            {Math.min(card.thresholdInfo.current, card.thresholdInfo.required)}/{card.thresholdInfo.required}
          </span>
        </div>
      )}

      {/* Banding (CR 702.21) band-membership badge */}
      {battlefield && isBanded && (() => {
        const band = bandColorFor(bandIndex)
        return (
          <div
            style={{
              ...styles.bandBadge,
              backgroundColor: band.chipBg,
              border: `1px solid ${band.border}`,
            }}
            title={`Band ${bandIndex + 1} — blocked as a group`}
          >
            B{bandIndex + 1}
          </div>
        )
      })()}

      {/* Counter badge for creatures with +1/+1 or -1/-1 counters */}
      {battlefield && hasStatCounters(card) && (
        <div style={{
          ...styles.counterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${getCounterStatModifier(card) >= 0 ? counterManaClass.PLUS_ONE_PLUS_ONE : counterManaClass.MINUS_ONE_MINUS_ONE}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={styles.counterBadgeText}>
            {getCounterStatModifier(card) >= 0 ? '+' : ''}{getCounterStatModifier(card)}
          </span>
        </div>
      )}

      {/* Gold counter badge */}
      {battlefield && getGoldCounters(card) > 0 && (
        <div style={{
          ...styles.goldCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.GOLD}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getGoldCounters(card)}
          </span>
        </div>
      )}

      {/* Plague counter badge */}
      {battlefield && getPlagueCounters(card) > 0 && (
        <div style={{
          ...styles.plagueCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.PLAGUE}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getPlagueCounters(card)}
          </span>
        </div>
      )}

      {/* Charge counter badge */}
      {battlefield && getChargeCounters(card) > 0 && (
        <div style={{
          ...styles.chargeCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.CHARGE}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getChargeCounters(card)}
          </span>
        </div>
      )}

      {/* Gem counter badge */}
      {battlefield && getGemCounters(card) > 0 && (
        <div style={{
          ...styles.chargeCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.GEM}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getGemCounters(card)}
          </span>
        </div>
      )}

      {/* Depletion counter badge */}
      {battlefield && getDepletionCounters(card) > 0 && (
        <div style={{
          ...styles.depletionCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.DEPLETION}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getDepletionCounters(card)}
          </span>
        </div>
      )}

      {/* Trap counter badge */}
      {battlefield && getTrapCounters(card) > 0 && (
        <div style={{
          ...styles.trapCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.TRAP}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getTrapCounters(card)}
          </span>
        </div>
      )}

      {/* Stun counter badge */}
      {battlefield && getStunCounters(card) > 0 && (
        <div style={{
          ...styles.stunCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.STUN}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getStunCounters(card)}
          </span>
        </div>
      )}

      {/* Finality counter badge */}
      {battlefield && getFinalityCounters(card) > 0 && (
        <div style={{
          ...styles.finalityCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.FINALITY}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getFinalityCounters(card)}
          </span>
        </div>
      )}

      {/* Supply counter badge */}
      {battlefield && getSupplyCounters(card) > 0 && (
        <div style={{
          ...styles.supplyCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.SUPPLY}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getSupplyCounters(card)}
          </span>
        </div>
      )}

      {/* Stash counter badge */}
      {battlefield && getStashCounters(card) > 0 && (
        <div style={{
          ...styles.stashCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.STASH}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getStashCounters(card)}
          </span>
        </div>
      )}

      {/* Blight counter badge */}
      {battlefield && getBlightCounters(card) > 0 && (
        <div style={{
          ...styles.blightCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.BLIGHT}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getBlightCounters(card)}
          </span>
        </div>
      )}

      {/* Flood counter badge */}
      {battlefield && getFloodCounters(card) > 0 && (
        <div style={{
          ...styles.floodCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.FLOOD}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getFloodCounters(card)}
          </span>
        </div>
      )}

      {/* Coin counter badge */}
      {battlefield && getCoinCounters(card) > 0 && (
        <div style={{
          ...styles.coinCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.COIN}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getCoinCounters(card)}
          </span>
        </div>
      )}

      {/* Chorus counter badge */}
      {battlefield && getChorusCounters(card) > 0 && (
        <div style={{
          ...styles.chorusCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.CHORUS}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getChorusCounters(card)}
          </span>
        </div>
      )}

      {/* Growth counter badge (Simic Ascendancy — 20 = win) */}
      {battlefield && getGrowthCounters(card) > 0 && (
        <div style={{
          ...styles.growthCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.GROWTH}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getGrowthCounters(card)}
          </span>
        </div>
      )}

      {/* Quest counter badge (Beastmaster Ascension etc.) */}
      {battlefield && getQuestCounters(card) > 0 && (
        <div style={{
          ...styles.questCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <SvgGlyph url={counterSvgIcon.QUEST!} size={responsive.badges.counterIconFontSize} color="#d8e8a0" />
          <span style={{ fontWeight: 700 }}>
            {getQuestCounters(card)}
          </span>
        </div>
      )}

      {/* Dream counter badge — visible in exile too, since that's where dream-counter cards live */}
      {getDreamCounters(card) > 0 && (
        <div style={{
          ...styles.dreamCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.DREAM}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getDreamCounters(card)}
          </span>
        </div>
      )}

      {/* Flying counter badge */}
      {battlefield && getFlyingCounters(card) > 0 && (
        <div style={{
          ...styles.flyingCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.FLYING}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getFlyingCounters(card)}
          </span>
        </div>
      )}

      {/* First strike counter badge */}
      {battlefield && getFirstStrikeCounters(card) > 0 && (
        <div style={{
          ...styles.firstStrikeCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.FIRST_STRIKE}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getFirstStrikeCounters(card)}
          </span>
        </div>
      )}

      {/* Lifelink counter badge */}
      {battlefield && getLifelinkCounters(card) > 0 && (
        <div style={{
          ...styles.lifelinkCounterBadge,
          fontSize: responsive.badges.counterTextFontSize,
          padding: responsive.badges.badgePadding,
        }}>
          <i className={`ms ms-${counterManaClass.LIFELINK}`} style={{ fontSize: responsive.badges.counterIconFontSize }} />
          <span style={{ fontWeight: 700 }}>
            {getLifelinkCounters(card)}
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
                    width: responsive.badges.keywordIconSize,
                    height: responsive.badges.keywordIconSize,
                    fontSize: responsive.badges.smallLabelFontSize,
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
                fontSize: responsive.badges.ptFontSize,
                padding: responsive.badges.badgePadding,
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
                    width: responsive.badges.keywordIconSize,
                    height: responsive.badges.keywordIconSize,
                    fontSize: responsive.badges.smallLabelFontSize,
                  }}>
                    {level}
                  </div>
                )
              })}
            </div>
            {/* Level badge in P/T position */}
            <div style={{
              ...styles.classLevelBadge,
              fontSize: responsive.badges.ptFontSize,
              padding: responsive.badges.badgePadding,
            }}>
              <span style={{ fontWeight: 700 }}>
                Lv.{currentLevel}
              </span>
            </div>
          </>
        )
      })()}

      {/* Keyword ability icons (shown for face-up cards, and for face-down cards with granted keywords) */}
      {battlefield && !hideKeywordIcons && (card.keywords.length > 0 || (card.abilityFlags && card.abilityFlags.length > 0) || (card.protections && card.protections.length > 0) || (card.hexproofFromColors && card.hexproofFromColors.length > 0) || card.isSuspected) && (
        <KeywordIcons keywords={card.keywords} abilityFlags={card.abilityFlags ?? []} protections={card.protections ?? []} hexproofFromColors={card.hexproofFromColors ?? []} isSuspected={card.isSuspected ?? false} size={responsive.badges.keywordIconSize} />
      )}

      {/* Revealed face-down eye icon (e.g., peeked via Spy Network) */}
      {faceDown && card.revealedName && (
        <div style={{
          position: 'absolute',
          top: responsive.badges.badgeInset,
          right: responsive.badges.badgeInset,
          backgroundColor: 'rgba(0, 0, 0, 0.75)',
          color: '#66ccff',
          fontSize: responsive.badges.manaCostFontSize,
          padding: responsive.badges.badgePaddingTight,
          borderRadius: 4,
          border: '1px solid rgba(102, 204, 255, 0.5)',
          pointerEvents: 'none',
          zIndex: 5,
          lineHeight: 1,
        }}>
          👁
        </div>
      )}

      {/* Chosen creature type / color / mode badge (e.g., Doom Cannon, Riptide Replicator, Outpost Siege) */}
      {!faceDown && (card.chosenCreatureType ?? card.chosenColor ?? card.chosenMode) && (
        <div style={{
          position: 'absolute',
          bottom: card.power != null ? 22 : 4,
          left: 4,
          backgroundColor: 'rgba(80, 60, 30, 0.9)',
          color: '#f0d890',
          fontSize: responsive.badges.counterIconFontSize,
          padding: '1px 4px',
          borderRadius: 3,
          border: '1px solid rgba(200, 170, 80, 0.6)',
          whiteSpace: 'nowrap',
          pointerEvents: 'none',
          zIndex: 5,
        }}>
          {[card.chosenColor, card.chosenCreatureType, card.chosenMode].filter(Boolean).join(' ')}
        </div>
      )}

      {/* Room (CR 709.5) door indicators — dim the locked half of the source image and
          drop an upright lock chip on top. Source image is portrait with face[1] on top
          and face[0] on bottom (Scryfall's stored orientation for landscape-printed Rooms).
          The chip counter-rotates the parent so the text stays readable when the card lies
          sideways or upside-down. */}
      {!faceDown && battlefield && card.isRoom && card.cardFaces && card.cardFaces.length === 2 && (
        <>
          {card.cardFaces.map((face, faceIdx) => {
            if (face.isUnlocked) return null
            const isTopHalf = faceIdx === 1
            return (
              <div key={face.faceId} style={{
                position: 'absolute',
                left: 0,
                right: 0,
                top: isTopHalf ? 0 : '50%',
                height: '50%',
                backgroundColor: 'rgba(0, 0, 0, 0.45)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                pointerEvents: 'none',
                zIndex: 5,
                borderRadius: isTopHalf ? '6px 6px 0 0' : '0 0 6px 6px',
              }}>
                <div style={{
                  transform: totalRotateDeg ? `rotate(${-totalRotateDeg}deg)` : undefined,
                  backgroundColor: 'rgba(40, 20, 20, 0.92)',
                  color: '#e89b9b',
                  fontSize: responsive.badges.manaCostFontSize,
                  padding: '2px 5px',
                  borderRadius: 4,
                  border: '1px solid rgba(200, 100, 100, 0.6)',
                  lineHeight: 1,
                }}>
                  🔒
                </div>
              </div>
            )
          })}
        </>
      )}

      {/* DFC (double-faced card) indicator badge */}
      {!faceDown && battlefield && card.isDoubleFaced && (
        <div style={{
          position: 'absolute',
          bottom: 4,
          left: '50%',
          transform: 'translateX(-50%)',
          backgroundColor: 'rgba(20, 20, 40, 0.9)',
          color: card.currentFace === 'BACK' ? '#b0b8d0' : '#f0d060',
          fontSize: responsive.badges.manaCostFontSize,
          padding: responsive.badges.badgePaddingTight,
          borderRadius: 4,
          border: `1px solid ${card.currentFace === 'BACK' ? 'rgba(160, 170, 200, 0.5)' : 'rgba(240, 208, 96, 0.5)'}`,
          pointerEvents: 'none',
          zIndex: 5,
          lineHeight: 1,
        }}>
          <i className={`ms ms-dfc-${card.currentFace === 'BACK' ? 'night' : 'day'}`} />
        </div>
      )}

      {/* Copy indicator badge (e.g., Clever Impersonator copying Wind Drake).
          Hovering the badge previews the *original* card (the printed identity),
          not the copy's current characteristics. */}
      {!faceDown && card.copyOf && (
        <div
          onMouseEnter={(e) => {
            e.stopPropagation()
            hoverCard(null)
            setCopyBadgeHoverPos({ x: e.clientX, y: e.clientY })
          }}
          onMouseMove={(e) => {
            e.stopPropagation()
            setCopyBadgeHoverPos({ x: e.clientX, y: e.clientY })
          }}
          onMouseLeave={(e) => {
            e.stopPropagation()
            setCopyBadgeHoverPos(null)
            // Restore the regular card preview as the cursor moves back onto the card body.
            hoverCard(card.id, { x: e.clientX, y: e.clientY })
          }}
          style={{
            position: 'absolute',
            top: 4,
            left: 4,
            backgroundColor: 'rgba(40, 40, 80, 0.9)',
            color: '#a0b0e0',
            fontSize: responsive.badges.smallLabelFontSize,
            padding: '1px 4px',
            borderRadius: 3,
            border: '1px solid rgba(100, 120, 200, 0.6)',
            whiteSpace: 'nowrap',
            cursor: 'help',
            zIndex: 5,
          }}
        >
          {card.copyOf}
        </div>
      )}

      {/* Original-card preview portalled to <body> so it escapes the card's
          overflow:hidden / transform containing block (tapped cards rotate). */}
      {!faceDown && card.copyOf && copyBadgeHoverPos && createPortal(
        <HoverCardPreview
          name={card.copyOf}
          imageUri={null}
          pos={copyBadgeHoverPos}
        />,
        document.body,
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
          top: responsive.badges.badgeInset,
          right: responsive.badges.badgeInset,
          backgroundColor: handCostInfo.isReduced || handCostInfo.isIncreased
            ? 'rgba(0, 0, 0, 0.85)'
            : 'rgba(0, 0, 0, 0.7)',
          padding: responsive.badges.badgePaddingTight,
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
          <ManaCost cost={handCostInfo.cost} size={responsive.badges.manaCostFontSize} gap={1} />
        </div>
      )}

      {/* Count badge for grouped cards */}
      {count > 1 && !faceDown && (
        <div style={{
          position: 'absolute',
          top: responsive.badges.badgeInset,
          right: responsive.badges.badgeInset,
          backgroundColor: 'rgba(0, 0, 0, 0.85)',
          color: '#fff',
          borderRadius: '50%',
          width: responsive.badges.countBadgeSize,
          height: responsive.badges.countBadgeSize,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontSize: responsive.badges.ptFontSize,
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
          top: responsive.badges.badgeInset,
          right: responsive.badges.badgeInset,
          backgroundColor: '#dc2626',
          color: 'white',
          width: responsive.badges.distributeBadgeSize,
          height: responsive.badges.distributeBadgeSize,
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          fontWeight: 'bold',
          fontSize: responsive.badges.distributeBadgeFontSize,
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
              fontSize: responsive.badges.ptFontSize,
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
              fontSize: responsive.badges.ptFontSize,
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
              width: responsive.badges.distributeBadgeSize,
              height: responsive.badges.distributeBadgeSize,
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
              width: responsive.badges.distributeBadgeSize,
              height: responsive.badges.distributeBadgeSize,
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
            fontSize: responsive.badges.manaCostFontSize,
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

      {/* Inline +/- control strip for counter removal (top, so counter badges stay visible).
       * One row per counter type: single-type creatures get one row; multi-type
       * creatures (e.g. +1/+1 + stun) get a row per type so the player can pick
       * exactly which to remove. */}
      {isCounterDistTarget && (
        <div
          onClick={(e) => e.stopPropagation()}
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'stretch',
            gap: 1,
            padding: responsive.isMobile ? '2px 1px' : '3px 2px',
            backgroundColor: 'rgba(0, 0, 0, 0.85)',
            borderBottom: '1px solid rgba(234, 179, 8, 0.5)',
            zIndex: 15,
          }}
        >
          {counterRows.map((row) => {
            const atMax = row.allocated >= row.available
            const showLabel = counterRows.length > 1
            return (
              <div
                key={row.counterType}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: 2,
                }}
              >
                {showLabel && (
                  <span
                    style={{
                      color: '#94a3b8',
                      fontSize: responsive.isMobile ? 9 : 10,
                      fontWeight: 700,
                      textTransform: 'uppercase',
                      letterSpacing: '0.04em',
                      marginRight: 2,
                      minWidth: responsive.isMobile ? 22 : 28,
                      textAlign: 'right',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {row.counterType}
                  </span>
                )}
                <button
                  onClick={(e) => { e.stopPropagation(); decrementCounterRemoval(card.id, row.counterType) }}
                  disabled={row.allocated <= 0}
                  style={{
                    width: responsive.badges.distributeBadgeSize,
                    height: responsive.badges.distributeBadgeSize,
                    borderRadius: 4,
                    border: 'none',
                    backgroundColor: row.allocated <= 0 ? '#333' : '#dc2626',
                    color: row.allocated <= 0 ? '#666' : 'white',
                    fontSize: responsive.isMobile ? 14 : 16,
                    fontWeight: 'bold',
                    cursor: row.allocated <= 0 ? 'not-allowed' : 'pointer',
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
                  {row.allocated}
                </span>
                <button
                  onClick={(e) => { e.stopPropagation(); incrementCounterRemoval(card.id, row.counterType) }}
                  disabled={atMax}
                  style={{
                    width: responsive.badges.distributeBadgeSize,
                    height: responsive.badges.distributeBadgeSize,
                    borderRadius: 4,
                    border: 'none',
                    backgroundColor: atMax ? '#333' : '#16a34a',
                    color: atMax ? '#666' : 'white',
                    fontSize: responsive.isMobile ? 14 : 16,
                    fontWeight: 'bold',
                    cursor: atMax ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: 0,
                  }}
                >
                  +
                </button>
              </div>
            )
          })}
        </div>
      )}

      {/* Behold label — gold ribbon fading in/out while this permanent is pulsing */}
      {isBeheldPulsing && (
        <div style={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          padding: '4px 10px',
          borderRadius: 4,
          backgroundColor: 'rgba(0, 0, 0, 0.78)',
          color: '#eab308',
          fontWeight: 800,
          fontSize: responsive.isMobile ? 10 : 12,
          letterSpacing: '0.12em',
          textTransform: 'uppercase',
          border: '1px solid rgba(234, 179, 8, 0.7)',
          boxShadow: '0 0 10px rgba(234, 179, 8, 0.6)',
          pointerEvents: 'none',
          zIndex: 5,
          animation: 'beholdLabelFade 300ms ease-out forwards',
          whiteSpace: 'nowrap',
        }}>
          Beheld
        </div>
      )}
    </div>
  )

  const profilerId = battlefield ? 'GameCard(battlefield)' : inHand ? 'GameCard(hand)' : 'GameCard(other)'

  // Minimalist commander crown — three-point silhouette in a thin wrapper that sits *above* the
  // card. Lives outside the cardElement because `styles.card` is `overflow: hidden`, which would
  // clip anything positioned over the top edge. Sized relative to the card so it stays legible
  // from the small command-zone peek up to a full-size battlefield card.
  const showCommanderCrown = card.isCommander && !faceDown
  const commanderCrown = showCommanderCrown ? (() => {
    const crownWidth = Math.max(14, Math.round(width * 0.18))
    const crownHeight = Math.round(crownWidth * 0.55)
    return (
      <div
        aria-label="Commander"
        title="Commander"
        style={{
          position: 'absolute',
          top: -crownHeight - 3,
          left: '50%',
          transform: 'translateX(-50%)',
          width: crownWidth,
          height: crownHeight,
          pointerEvents: 'none',
          zIndex: 6,
          opacity: 0.9,
          filter: 'drop-shadow(0 1px 1.5px rgba(0, 0, 0, 0.55))',
        }}
      >
        <svg
          viewBox="0 0 24 13"
          width="100%"
          height="100%"
          preserveAspectRatio="none"
          fill="#d4af37"
          stroke="rgba(0, 0, 0, 0.45)"
          strokeWidth="0.5"
          strokeLinejoin="round"
        >
          {/* Three-point coronet on a thin band: outer points at the corners, taller centre point */}
          <path d="M1.5 12 L1.5 9 L4.5 5 L8 8 L12 2 L16 8 L19.5 5 L22.5 9 L22.5 12 Z" />
        </svg>
      </div>
    )
  })() : null

  // "Not Legendary" chip — pinned to a copy whose printed card is legendary but whose
  // projected type line had the supertype stripped ("except it isn't legendary" copy
  // effects, e.g. Impostor Syndrome). The printed card art still shows the legendary
  // frame, so without this chip a player can't tell the token copy isn't subject to
  // the legend rule. The server sets `nonLegendaryCopy` after comparing the printed
  // CardDefinition's supertypes to the live CardComponent's supertypes.
  const showNonLegendaryChip = battlefield && !faceDown && card.nonLegendaryCopy === true
  const nonLegendaryChip = showNonLegendaryChip ? (() => {
    const chipHeight = Math.max(10, Math.round(width * 0.12))
    const crownW = Math.round(chipHeight * 0.95)
    const crownH = Math.round(chipHeight * 0.55)
    return (
      <div
        aria-label="Not legendary"
        title={`Not legendary — copy effect stripped the Legendary supertype (${card.typeLine})`}
        style={{
          position: 'absolute',
          top: -Math.round(chipHeight * 0.55),
          left: '50%',
          transform: 'translateX(-50%)',
          height: chipHeight,
          padding: `0 ${Math.max(4, Math.round(chipHeight * 0.6))}px`,
          display: 'flex',
          alignItems: 'center',
          gap: 4,
          background: 'linear-gradient(135deg, #4a4a4a 0%, #6b6b6b 50%, #4a4a4a 100%)',
          color: '#f0f0f0',
          fontSize: Math.max(8, Math.round(chipHeight * 0.62)),
          fontWeight: 800,
          letterSpacing: '0.08em',
          textTransform: 'uppercase',
          borderRadius: chipHeight,
          border: '1px solid rgba(0, 0, 0, 0.55)',
          boxShadow: '0 1px 2px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.18)',
          pointerEvents: 'none',
          zIndex: 6,
          whiteSpace: 'nowrap',
          lineHeight: 1,
        }}
      >
        {/* Crown silhouette with a diagonal strike-through to read "no crown". */}
        <span style={{ position: 'relative', width: crownW, height: crownH, display: 'inline-block' }} aria-hidden>
          <svg
            viewBox="0 0 24 13"
            width={crownW}
            height={crownH}
            preserveAspectRatio="none"
            fill="rgba(220, 220, 220, 0.55)"
            stroke="rgba(0, 0, 0, 0.6)"
            strokeWidth="0.5"
            strokeLinejoin="round"
            style={{ position: 'absolute', inset: 0 }}
          >
            <path d="M1.5 12 L1.5 9 L4.5 5 L8 8 L12 2 L16 8 L19.5 5 L22.5 9 L22.5 12 Z" />
          </svg>
          <svg
            viewBox="0 0 24 13"
            width={crownW}
            height={crownH}
            preserveAspectRatio="none"
            style={{ position: 'absolute', inset: 0 }}
          >
            <line x1="2" y1="11.5" x2="22" y2="1.5" stroke="#ff6464" strokeWidth="1.4" strokeLinecap="round" />
          </svg>
        </span>
        Not Legendary
      </div>
    )
  })() : null

  // Wrap in container for sideways battlefield cards (tapped permanents and Rooms) to
  // prevent overlap with neighbours.
  if (needsLandscapeContainer && battlefield) {
    return (
      <RenderProfiler id={profilerId}>
      <div style={{
        width: containerWidth,
        height: containerHeight,
        display: 'flex',
        alignItems: 'flex-end',
        justifyContent: 'center',
        transition: 'width 0.15s, height 0.15s',
        pointerEvents: 'none',
        position: 'relative',
      }}>
        {commanderCrown}
        {nonLegendaryChip}
        {cardElement}
      </div>
      </RenderProfiler>
    )
  }

  // Commander OR non-legendary-copy permanents need a relative-positioned wrapper so the chip
  // can float above the card without being clipped by the card's `overflow: hidden`.
  if (showCommanderCrown || nonLegendaryChip) {
    return (
      <RenderProfiler id={profilerId}>
        <div style={{
          position: 'relative',
          width,
          height,
          pointerEvents: 'none',
        }}>
          {commanderCrown}
          {nonLegendaryChip}
          {cardElement}
        </div>
      </RenderProfiler>
    )
  }

  return <RenderProfiler id={profilerId}>{cardElement}</RenderProfiler>
}
