/**
 * UI slice - handles local UI state like targeting, combat, selections, and animations.
 */
import type {
  SliceCreator,
  EntityId,
  TargetingState,
  CombatState,
  XSelectionState,
  ConvokeSelectionState,
  CrewSelectionState,
  DelveSelectionState,
  ManaColorSelectionState,
  DecisionSelectionState,
  DamageDistributionState,
  DistributeState,
  CounterDistributionState,
  ManaSelectionState,
  DrawAnimation,
  DamageAnimation,
  RevealAnimation,
  CoinFlipAnimation,
  TargetReselectedAnimation,
  ConvokeCreatureSelection,
  MatchIntro,
} from './types'
import {
  entityId,
  createSubmitActionMessage,
  createUpdateAttackerTargetsMessage,
  createUpdateBlockerAssignmentsMessage,
} from '../../types'
import { getWebSocket } from './shared'
import { parseManaCost as parseManaCostUtil, getRemainingCostSymbols } from '../../utils/manaCost'

export interface UISliceState {
  selectedCardId: EntityId | null
  targetingState: TargetingState | null
  combatState: CombatState | null
  xSelectionState: XSelectionState | null
  convokeSelectionState: ConvokeSelectionState | null
  crewSelectionState: CrewSelectionState | null
  delveSelectionState: DelveSelectionState | null
  manaColorSelectionState: ManaColorSelectionState | null
  decisionSelectionState: DecisionSelectionState | null
  damageDistributionState: DamageDistributionState | null
  /** Persisted damage distribution from the last confirmed DamageDistributionModal (target ID -> damage) */
  lastDamageDistribution: Record<EntityId, number> | null
  distributeState: DistributeState | null
  counterDistributionState: CounterDistributionState | null
  manaSelectionState: ManaSelectionState | null
  hoveredCardId: EntityId | null
  autoTapPreview: readonly EntityId[] | null
  draggingBlockerId: EntityId | null
  draggingAttackerId: EntityId | null
  draggingCardId: EntityId | null
  revealedHandCardIds: readonly EntityId[] | null
  revealedCardsInfo: {
    cardIds: readonly EntityId[]
    cardNames: readonly string[]
    imageUris: readonly (string | null)[]
    source: string | null
    isYourReveal: boolean
  } | null
  opponentAttackerTargets: { selectedAttackers: readonly EntityId[]; attackerTargets: Record<EntityId, EntityId> } | null
  opponentBlockerAssignments: Record<EntityId, EntityId[]> | null
  drawAnimations: readonly DrawAnimation[]
  damageAnimations: readonly DamageAnimation[]
  revealAnimations: readonly RevealAnimation[]
  coinFlipAnimations: readonly CoinFlipAnimation[]
  targetReselectedAnimations: readonly TargetReselectedAnimation[]
  matchIntro: MatchIntro | null
}

export interface UISliceActions {
  selectCard: (cardId: EntityId | null) => void
  hoverCard: (cardId: EntityId | null) => void
  startTargeting: (state: TargetingState) => void
  addTarget: (targetId: EntityId) => void
  removeTarget: (targetId: EntityId) => void
  cancelTargeting: () => void
  confirmTargeting: () => void
  startCombat: (state: CombatState) => void
  toggleAttacker: (creatureId: EntityId) => void
  setAttackTarget: (attackerId: EntityId, targetId: EntityId) => void
  assignBlocker: (blockerId: EntityId, attackerId: EntityId) => void
  removeBlockerAssignment: (blockerId: EntityId) => void
  clearBlockerAssignments: () => void
  startDraggingBlocker: (blockerId: EntityId) => void
  stopDraggingBlocker: () => void
  startDraggingAttacker: (attackerId: EntityId) => void
  stopDraggingAttacker: () => void
  startDraggingCard: (cardId: EntityId) => void
  stopDraggingCard: () => void
  confirmCombat: () => void
  cancelCombat: () => void
  attackWithAll: () => void
  clearAttackers: () => void
  clearCombat: () => void
  startXSelection: (state: XSelectionState) => void
  updateXValue: (x: number) => void
  cancelXSelection: () => void
  confirmXSelection: () => void
  startConvokeSelection: (state: ConvokeSelectionState) => void
  toggleConvokeCreature: (entityId: EntityId, name: string, payingColor: string | null) => void
  cancelConvokeSelection: () => void
  confirmConvokeSelection: () => void
  startCrewSelection: (state: CrewSelectionState) => void
  toggleCrewCreature: (entityId: EntityId) => void
  cancelCrewSelection: () => void
  confirmCrewSelection: () => void
  startDelveSelection: (state: DelveSelectionState) => void
  toggleDelveCard: (entityId: EntityId) => void
  cancelDelveSelection: () => void
  confirmDelveSelection: () => void
  startManaColorSelection: (state: ManaColorSelectionState) => void
  confirmManaColorSelection: (color: string) => void
  cancelManaColorSelection: () => void
  startDecisionSelection: (state: DecisionSelectionState) => void
  toggleDecisionSelection: (cardId: EntityId) => void
  cancelDecisionSelection: () => void
  confirmDecisionSelection: () => void
  startDamageDistribution: (state: DamageDistributionState) => void
  updateDamageDistribution: (targetId: EntityId, amount: number) => void
  cancelDamageDistribution: () => void
  confirmDamageDistribution: () => void
  initDistribute: (state: DistributeState) => void
  incrementDistribute: (targetId: EntityId) => void
  decrementDistribute: (targetId: EntityId) => void
  confirmDistribute: () => void
  clearDistribute: () => void
  startCounterDistribution: (state: CounterDistributionState) => void
  incrementCounterRemoval: (entityId: EntityId) => void
  decrementCounterRemoval: (entityId: EntityId) => void
  cancelCounterDistribution: () => void
  confirmCounterDistribution: () => void
  startManaSelection: (actionInfo: import('../../types').LegalActionInfo) => void
  toggleManaSource: (entityId: EntityId) => void
  cancelManaSelection: () => void
  confirmManaSelection: () => void
  showRevealedHand: (cardIds: readonly EntityId[]) => void
  dismissRevealedHand: () => void
  showRevealedCards: (cardIds: readonly EntityId[], cardNames: readonly string[], imageUris: readonly (string | null)[], source: string | null, isYourReveal: boolean) => void
  dismissRevealedCards: () => void
  addDrawAnimation: (animation: DrawAnimation) => void
  removeDrawAnimation: (id: string) => void
  addDamageAnimation: (animation: DamageAnimation) => void
  removeDamageAnimation: (id: string) => void
  addRevealAnimation: (animation: RevealAnimation) => void
  removeRevealAnimation: (id: string) => void
  addCoinFlipAnimation: (animation: CoinFlipAnimation) => void
  removeCoinFlipAnimation: (id: string) => void
  addTargetReselectedAnimation: (animation: TargetReselectedAnimation) => void
  removeTargetReselectedAnimation: (id: string) => void
  setAutoTapPreview: (preview: readonly EntityId[] | null) => void
  setMatchIntro: (intro: MatchIntro) => void
  clearMatchIntro: () => void
}

export type UISlice = UISliceState & UISliceActions

export const createUISlice: SliceCreator<UISlice> = (set, get) => ({
  // Initial state
  selectedCardId: null,
  targetingState: null,
  combatState: null,
  xSelectionState: null,
  convokeSelectionState: null,
  crewSelectionState: null,
  delveSelectionState: null,
  manaColorSelectionState: null,
  decisionSelectionState: null,
  damageDistributionState: null,
  lastDamageDistribution: null,
  distributeState: null,
  counterDistributionState: null,
  manaSelectionState: null,
  hoveredCardId: null,
  autoTapPreview: null,
  draggingBlockerId: null,
  draggingAttackerId: null,
  draggingCardId: null,
  revealedHandCardIds: null,
  revealedCardsInfo: null,
  opponentAttackerTargets: null,
  opponentBlockerAssignments: null,
  drawAnimations: [],
  damageAnimations: [],
  revealAnimations: [],
  coinFlipAnimations: [],
  targetReselectedAnimations: [],
  matchIntro: null,

  // Card selection actions
  selectCard: (cardId) => {
    set({ selectedCardId: cardId })
  },

  hoverCard: (cardId) => {
    let autoTapPreview: readonly EntityId[] | null = null
    if (cardId) {
      const { legalActions, pendingDecision } = get()
      if (!pendingDecision) {
        const castAction = legalActions.find(
          (a) => a.action.type === 'CastSpell' && a.action.cardId === cardId
        )
        if (castAction?.autoTapPreview) {
          autoTapPreview = castAction.autoTapPreview
        } else {
          const turnFaceUpAction = legalActions.find(
            (a) => a.action.type === 'TurnFaceUp' && a.action.sourceId === cardId
          )
          if (turnFaceUpAction?.autoTapPreview) {
            autoTapPreview = turnFaceUpAction.autoTapPreview
          }
        }
      }
    }
    set({ hoveredCardId: cardId, autoTapPreview })
  },

  // Targeting actions
  startTargeting: (targetingState) => {
    set({ targetingState })
  },

  addTarget: (targetId) => {
    set((state) => {
      if (!state.targetingState) return state
      const newTargets = [...state.targetingState.selectedTargets, targetId]
      return {
        targetingState: {
          ...state.targetingState,
          selectedTargets: newTargets,
        },
      }
    })
  },

  removeTarget: (targetId) => {
    set((state) => {
      if (!state.targetingState) return state
      const newTargets = state.targetingState.selectedTargets.filter((id) => id !== targetId)
      return {
        targetingState: {
          ...state.targetingState,
          selectedTargets: newTargets,
        },
      }
    })
  },

  cancelTargeting: () => {
    set({ targetingState: null })
  },

  confirmTargeting: () => {
    const { targetingState, submitAction, gameState, startTargeting, startDamageDistribution } = get()
    if (!targetingState || !gameState) return

    // Handle sacrifice selection phase
    if (targetingState.isSacrificeSelection && targetingState.pendingActionInfo) {
      const actionInfo = targetingState.pendingActionInfo
      const action = targetingState.action
      if (action.type === 'CastSpell') {
        const costType = actionInfo.additionalCostInfo?.costType
        const additionalCostPayment = costType === 'DiscardCard'
          ? { discardedCards: [...targetingState.selectedTargets] }
          : costType === 'ExileFromGraveyard'
            ? { exiledCards: [...targetingState.selectedTargets] }
            : { sacrificedPermanents: [...targetingState.selectedTargets] }
        const actionWithCost = {
          ...action,
          additionalCostPayment,
        }

        if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
          set({ targetingState: null })
          startTargeting({
            action: actionWithCost,
            validTargets: [...actionInfo.validTargets],
            selectedTargets: [],
            minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
            maxTargets: actionInfo.targetCount ?? 1,
            ...(actionInfo.requiresDamageDistribution ? { pendingActionInfo: actionInfo } : {}),
          })
          return
        } else {
          submitAction(actionWithCost)
          set({ targetingState: null })
          return
        }
      } else if (action.type === 'ActivateAbility') {
        const costType = actionInfo.additionalCostInfo?.costType
        const costPayment = costType === 'TapPermanents'
          ? { tappedPermanents: [...targetingState.selectedTargets] }
          : costType === 'DiscardCard'
            ? { discardedCards: [...targetingState.selectedTargets] }
            : costType === 'BouncePermanent'
              ? { bouncedPermanents: [...targetingState.selectedTargets] }
              : costType === 'ExileFromGraveyard'
                ? { exiledCards: [...targetingState.selectedTargets] }
                : { sacrificedPermanents: [...targetingState.selectedTargets] }
        const actionWithCost = {
          ...action,
          costPayment,
        }

        if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
          set({ targetingState: null })
          startTargeting({
            action: actionWithCost,
            validTargets: [...actionInfo.validTargets],
            selectedTargets: [],
            minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
            maxTargets: actionInfo.targetCount ?? 1,
            ...(actionInfo.requiresDamageDistribution ? { pendingActionInfo: actionInfo } : {}),
          })
          return
        } else if (actionInfo.requiresManaColorChoice) {
          // Need mana color selection before submitting (e.g., Birchlore Rangers)
          set({ targetingState: null, manaColorSelectionState: { action: actionWithCost } })
          return
        } else {
          submitAction(actionWithCost)
          set({ targetingState: null })
          return
        }
      } else if (action.type === 'TurnFaceUp') {
        // Non-mana morph cost (e.g., return a Bird to hand)
        const actionWithCost = {
          ...action,
          costTargetIds: [...targetingState.selectedTargets],
        }
        submitAction(actionWithCost)
        set({ targetingState: null })
        return
      }
    }

    // Normal targeting flow
    const action = targetingState.action

    // Handle multi-target spells
    if (targetingState.targetRequirements && targetingState.targetRequirements.length > 1) {
      const currentIndex = targetingState.currentRequirementIndex ?? 0
      const nextIndex = currentIndex + 1

      const allSelected = targetingState.allSelectedTargets
        ? [...targetingState.allSelectedTargets, targetingState.selectedTargets]
        : [targetingState.selectedTargets]

      const nextReq = targetingState.targetRequirements[nextIndex]
      if (nextReq) {
        const alreadySelected = allSelected.flat()
        const filteredValidTargets = nextReq.validTargets.filter(
          (t) => !alreadySelected.includes(t)
        )

        startTargeting({
          action,
          validTargets: filteredValidTargets,
          selectedTargets: [],
          minTargets: nextReq.minTargets,
          maxTargets: nextReq.maxTargets,
          currentRequirementIndex: nextIndex,
          allSelectedTargets: allSelected,
          targetRequirements: targetingState.targetRequirements,
          ...(targetingState.pendingActionInfo ? { pendingActionInfo: targetingState.pendingActionInfo } : {}),
          ...(nextReq.targetZone ? { targetZone: nextReq.targetZone } : {}),
          targetDescription: nextReq.description,
          ...(targetingState.totalRequirements ? { totalRequirements: targetingState.totalRequirements } : {}),
        })
        return
      }

      // All requirements filled
      if (action.type === 'CastSpell' || action.type === 'ActivateAbility') {
        const targets = allSelected.flat().map((targetId) => {
          const isPlayer = gameState.players.some(p => p.playerId === targetId)
          if (isPlayer) {
            return { type: 'Player' as const, playerId: targetId }
          }
          const card = gameState.cards[targetId]
          if (card && card.zone?.zoneType === 'Graveyard') {
            return {
              type: 'Card' as const,
              cardId: targetId,
              ownerId: card.zone.ownerId,
              zone: 'Graveyard' as const,
            }
          }
          if (card && card.zone?.zoneType === 'Stack') {
            return { type: 'Spell' as const, spellEntityId: targetId }
          }
          return { type: 'Permanent' as const, entityId: targetId }
        })
        const modifiedAction = { ...action, targets }
        submitAction(modifiedAction)
      } else {
        submitAction(action)
      }
      set({ targetingState: null })
      return
    }

    // Single target requirement flow
    if (action.type === 'CastSpell' || action.type === 'ActivateAbility') {
      const targets = targetingState.selectedTargets.map((targetId) => {
        const isPlayer = gameState.players.some(p => p.playerId === targetId)
        if (isPlayer) {
          return { type: 'Player' as const, playerId: targetId }
        }
        const card = gameState.cards[targetId]
        if (card && card.zone?.zoneType === 'Graveyard') {
          return {
            type: 'Card' as const,
            cardId: targetId,
            ownerId: card.zone.ownerId,
            zone: 'Graveyard' as const,
          }
        }
        if (card && card.zone?.zoneType === 'Stack') {
          return { type: 'Spell' as const, spellEntityId: targetId }
        }
        return { type: 'Permanent' as const, entityId: targetId }
      })
      const modifiedAction = { ...action, targets }

      // Check for damage distribution
      const actionInfo = targetingState.pendingActionInfo
      if (
        action.type === 'CastSpell' &&
        actionInfo?.requiresDamageDistribution &&
        actionInfo.totalDamageToDistribute &&
        targetingState.selectedTargets.length > 1
      ) {
        const cardName = actionInfo.description.replace('Cast ', '')
        const minPerTarget = actionInfo.minDamagePerTarget ?? 1

        const initialDistribution: Record<EntityId, number> = {}
        for (const targetId of targetingState.selectedTargets) {
          initialDistribution[targetId] = minPerTarget
        }

        startDamageDistribution({
          actionInfo,
          action: modifiedAction as import('../../types').CastSpellAction,
          cardName,
          targetIds: [...targetingState.selectedTargets],
          totalDamage: actionInfo.totalDamageToDistribute,
          minPerTarget,
          distribution: initialDistribution,
        })
        set({ targetingState: null })
        return
      }

      submitAction(modifiedAction)
    } else {
      submitAction(action)
    }
    set({ targetingState: null })
  },

  // Combat actions
  startCombat: (combatState) => {
    set({ combatState })
    // Sync pre-populated blocker assignments with opponent
    if (combatState.mode === 'declareBlockers' && Object.keys(combatState.blockerAssignments).length > 0) {
      getWebSocket()?.send(createUpdateBlockerAssignmentsMessage(combatState.blockerAssignments))
    }
  },

  toggleAttacker: (creatureId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
        return state
      }

      const isSelected = state.combatState.selectedAttackers.includes(creatureId)
      const newAttackers = isSelected
        ? state.combatState.selectedAttackers.filter((id) => id !== creatureId)
        : [...state.combatState.selectedAttackers, creatureId]

      // Clean up attackerTargets if deselecting
      const newTargets = { ...state.combatState.attackerTargets }
      if (isSelected) {
        delete newTargets[creatureId]
      }

      getWebSocket()?.send(createUpdateAttackerTargetsMessage(newAttackers, newTargets))

      return {
        combatState: {
          ...state.combatState,
          selectedAttackers: newAttackers,
          attackerTargets: newTargets,
        },
      }
    })
  },

  setAttackTarget: (attackerId, targetId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
        return state
      }

      const newTargets = {
        ...state.combatState.attackerTargets,
        [attackerId]: targetId,
      }

      getWebSocket()?.send(createUpdateAttackerTargetsMessage(
        [...state.combatState.selectedAttackers],
        newTargets,
      ))

      return {
        combatState: {
          ...state.combatState,
          attackerTargets: newTargets,
        },
      }
    })
  },

  assignBlocker: (blockerId, attackerId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
        return state
      }

      const existing = state.combatState.blockerAssignments[blockerId] ?? []
      // If already blocking this attacker, don't add duplicate
      if (existing.includes(attackerId)) return state
      // Check max block count (default 1 for normal creatures)
      const maxBlocks = state.combatState.blockerMaxBlockCounts[blockerId] ?? 1
      if (existing.length >= maxBlocks) {
        return state
      }
      const newAssignments = {
        ...state.combatState.blockerAssignments,
        [blockerId]: [...existing, attackerId],
      }

      getWebSocket()?.send(createUpdateBlockerAssignmentsMessage(newAssignments))

      return {
        combatState: {
          ...state.combatState,
          blockerAssignments: newAssignments,
        },
      }
    })
  },

  removeBlockerAssignment: (blockerId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
        return state
      }

      const { [blockerId]: _, ...remaining } = state.combatState.blockerAssignments
      getWebSocket()?.send(createUpdateBlockerAssignmentsMessage(remaining))

      return {
        combatState: {
          ...state.combatState,
          blockerAssignments: remaining,
        },
      }
    })
  },

  clearBlockerAssignments: () => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
        return state
      }

      getWebSocket()?.send(createUpdateBlockerAssignmentsMessage({}))

      return {
        combatState: {
          ...state.combatState,
          blockerAssignments: {},
        },
      }
    })
  },

  startDraggingBlocker: (blockerId) => {
    set({ draggingBlockerId: blockerId })
  },

  stopDraggingBlocker: () => {
    set({ draggingBlockerId: null })
  },

  startDraggingAttacker: (attackerId) => {
    set({ draggingAttackerId: attackerId })
  },

  stopDraggingAttacker: () => {
    set({ draggingAttackerId: null })
  },

  startDraggingCard: (cardId) => {
    set({ draggingCardId: cardId })
  },

  stopDraggingCard: () => {
    set({ draggingCardId: null })
  },

  confirmCombat: () => {
    const { combatState, playerId } = get()
    if (!combatState || !playerId) return

    if (combatState.mode === 'declareAttackers') {
      const { gameState } = get()
      if (!gameState) return

      const opponent = gameState.players.find((p) => p.playerId !== playerId)
      if (!opponent) return

      const attackers: Record<EntityId, EntityId> = {}
      for (const attackerId of combatState.selectedAttackers) {
        // Use per-attacker target if set, otherwise default to opponent player
        attackers[attackerId] = combatState.attackerTargets[attackerId] ?? opponent.playerId
      }

      const action = {
        type: 'DeclareAttackers' as const,
        playerId,
        attackers,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    } else if (combatState.mode === 'declareBlockers') {
      const blockers: Record<EntityId, readonly EntityId[]> = {}
      for (const [blockerIdStr, attackerIds] of Object.entries(combatState.blockerAssignments)) {
        blockers[entityId(blockerIdStr)] = attackerIds
      }

      const action = {
        type: 'DeclareBlockers' as const,
        playerId,
        blockers,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    }

    set({ draggingBlockerId: null })
  },

  attackWithAll: () => {
    const { combatState } = get()
    if (!combatState) return
    if (combatState.mode !== 'declareAttackers') return
    if (combatState.validCreatures.length === 0) return

    set({
      combatState: {
        ...combatState,
        selectedAttackers: [...combatState.validCreatures],
      },
    })
  },

  cancelCombat: () => {
    const { combatState, playerId } = get()
    if (!combatState || !playerId) return

    if (combatState.mode === 'declareAttackers') {
      const action = {
        type: 'DeclareAttackers' as const,
        playerId,
        attackers: {} as Record<EntityId, EntityId>,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    } else if (combatState.mode === 'declareBlockers') {
      const action = {
        type: 'DeclareBlockers' as const,
        playerId,
        blockers: {} as Record<EntityId, readonly EntityId[]>,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    }

    set({ draggingBlockerId: null })
  },

  clearAttackers: () => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
        return state
      }
      return {
        combatState: {
          ...state.combatState,
          selectedAttackers: [],
          attackerTargets: {},
        },
      }
    })
  },

  clearCombat: () => {
    set({ combatState: null, draggingBlockerId: null })
  },

  // X cost selection actions
  startXSelection: (xSelectionState) => {
    set({ xSelectionState })
  },

  updateXValue: (x) => {
    set((state) => {
      if (!state.xSelectionState) return state
      return {
        xSelectionState: {
          ...state.xSelectionState,
          selectedX: x,
        },
      }
    })
  },

  cancelXSelection: () => {
    set({ xSelectionState: null })
  },

  confirmXSelection: () => {
    const { xSelectionState, startTargeting, playerId, gameState } = get()
    if (!xSelectionState || !playerId || !gameState) return

    const { actionInfo, selectedX } = xSelectionState

    if (actionInfo.action.type === 'CastSpell') {
      const baseAction = actionInfo.action

      if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        const actionWithX = {
          ...baseAction,
          xValue: selectedX,
        }
        startTargeting({
          action: actionWithX,
          validTargets: [...actionInfo.validTargets],
          selectedTargets: [],
          minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
          maxTargets: actionInfo.targetCount ?? 1,
          ...(actionInfo.requiresDamageDistribution ? { pendingActionInfo: actionInfo } : {}),
        })
      } else {
        const actionWithX = {
          ...baseAction,
          xValue: selectedX,
        }
        getWebSocket()?.send(createSubmitActionMessage(actionWithX))
      }
    } else if (actionInfo.action.type === 'ActivateAbility') {
      const baseAction = actionInfo.action

      if (xSelectionState.isRepeatCount) {
        // Repeat count mode: action already has auto-selected targets, just set repeatCount
        const actionWithRepeat = {
          ...baseAction,
          repeatCount: selectedX,
        }
        getWebSocket()?.send(createSubmitActionMessage(actionWithRepeat))
      } else {
        const actionWithX = {
          ...baseAction,
          xValue: selectedX,
        }

        if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
          startTargeting({
            action: actionWithX,
            validTargets: [...actionInfo.validTargets],
            selectedTargets: [],
            minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
            maxTargets: actionInfo.targetCount ?? 1,
          })
        } else {
          getWebSocket()?.send(createSubmitActionMessage(actionWithX))
        }
      }
    } else if (actionInfo.action.type === 'TurnFaceUp') {
      const baseAction = actionInfo.action
      const actionWithX = {
        ...baseAction,
        xValue: selectedX,
      }
      getWebSocket()?.send(createSubmitActionMessage(actionWithX))
    }

    set({ xSelectionState: null })
  },

  // Convoke selection actions
  startConvokeSelection: (convokeSelectionState) => {
    set({ convokeSelectionState })
  },

  toggleConvokeCreature: (creatureEntityId, name, payingColor) => {
    set((state) => {
      if (!state.convokeSelectionState) return state
      const { selectedCreatures } = state.convokeSelectionState
      const existingIndex = selectedCreatures.findIndex((c) => c.entityId === creatureEntityId)

      let newSelectedCreatures: ConvokeCreatureSelection[]
      if (existingIndex >= 0) {
        newSelectedCreatures = selectedCreatures.filter((c) => c.entityId !== creatureEntityId)
      } else {
        newSelectedCreatures = [...selectedCreatures, { entityId: creatureEntityId, name, payingColor }]
      }

      return {
        convokeSelectionState: {
          ...state.convokeSelectionState,
          selectedCreatures: newSelectedCreatures,
        },
      }
    })
  },

  cancelConvokeSelection: () => {
    set({ convokeSelectionState: null })
  },

  confirmConvokeSelection: () => {
    const { convokeSelectionState, startTargeting, playerId } = get()
    if (!convokeSelectionState || !playerId) return

    const { actionInfo, selectedCreatures } = convokeSelectionState

    if (actionInfo.action.type === 'CastSpell') {
      const baseAction = actionInfo.action

      const convokedCreatures: Record<string, { color: string | null }> = {}
      for (const creature of selectedCreatures) {
        convokedCreatures[creature.entityId] = { color: creature.payingColor }
      }

      const actionWithConvoke = {
        ...baseAction,
        alternativePayment: {
          delvedCards: [],
          convokedCreatures,
        },
      }

      if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        startTargeting({
          action: actionWithConvoke,
          validTargets: [...actionInfo.validTargets],
          selectedTargets: [],
          minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
          maxTargets: actionInfo.targetCount ?? 1,
          ...(actionInfo.requiresDamageDistribution ? { pendingActionInfo: actionInfo } : {}),
        })
      } else {
        getWebSocket()?.send(createSubmitActionMessage(actionWithConvoke))
      }
    }

    set({ convokeSelectionState: null })
  },

  // Crew selection actions
  startCrewSelection: (crewSelectionState) => {
    set({ crewSelectionState })
  },

  toggleCrewCreature: (entityId) => {
    set((state) => {
      if (!state.crewSelectionState) return state
      const { selectedCreatures } = state.crewSelectionState
      const exists = selectedCreatures.includes(entityId)

      const newSelectedCreatures = exists
        ? selectedCreatures.filter((id) => id !== entityId)
        : [...selectedCreatures, entityId]

      return {
        crewSelectionState: {
          ...state.crewSelectionState,
          selectedCreatures: newSelectedCreatures,
        },
      }
    })
  },

  cancelCrewSelection: () => {
    set({ crewSelectionState: null })
  },

  confirmCrewSelection: () => {
    const { crewSelectionState, playerId } = get()
    if (!crewSelectionState || !playerId) return

    const { actionInfo, selectedCreatures } = crewSelectionState

    if (actionInfo.action.type === 'CrewVehicle') {
      const actionWithCrew = {
        ...actionInfo.action,
        crewCreatures: selectedCreatures,
      }
      getWebSocket()?.send(createSubmitActionMessage(actionWithCrew))
    }

    set({ crewSelectionState: null })
  },

  // Delve selection actions
  startDelveSelection: (delveSelectionState) => {
    set({ delveSelectionState })
  },

  toggleDelveCard: (entityId) => {
    set((state) => {
      if (!state.delveSelectionState) return state
      const { selectedCards, maxDelve } = state.delveSelectionState
      const isSelected = selectedCards.includes(entityId)

      let newSelectedCards: EntityId[]
      if (isSelected) {
        newSelectedCards = selectedCards.filter((id) => id !== entityId)
      } else {
        // Don't exceed the max generic mana we can pay via Delve
        if (selectedCards.length >= maxDelve) return state
        newSelectedCards = [...selectedCards, entityId]
      }

      return {
        delveSelectionState: {
          ...state.delveSelectionState,
          selectedCards: newSelectedCards,
        },
      }
    })
  },

  cancelDelveSelection: () => {
    set({ delveSelectionState: null })
  },

  confirmDelveSelection: () => {
    const { delveSelectionState, startTargeting, startManaSelection } = get()
    if (!delveSelectionState) return

    const { actionInfo, selectedCards } = delveSelectionState

    if (actionInfo.action.type === 'CastSpell') {
      const baseAction = actionInfo.action

      const actionWithDelve = {
        ...baseAction,
        alternativePayment: {
          delvedCards: [...selectedCards],
          convokedCreatures: {},
        },
      }

      // Calculate remaining cost after delve for display
      const originalSymbols = parseManaCostUtil(delveSelectionState.manaCost)
      const remainingSymbols = getRemainingCostSymbols(originalSymbols, selectedCards.length)
      const remainingCostString = remainingSymbols.map(s => `{${s}}`).join('')

      // Omit delve fields so executeAction won't re-open the Delve selector
      const { hasDelve: _, validDelveCards: _2, minDelveNeeded: _3, ...restActionInfo } = actionInfo
      const modifiedActionInfo: import('../../types').LegalActionInfo = {
        ...restActionInfo,
        action: actionWithDelve,
        manaCostString: remainingCostString,
      }

      if (actionInfo.availableManaSources && actionInfo.availableManaSources.length > 0) {
        // Mana selection first, then targeting (if needed) is handled after mana confirm
        startManaSelection(modifiedActionInfo)
      } else if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        startTargeting({
          action: actionWithDelve,
          validTargets: [...actionInfo.validTargets],
          selectedTargets: [],
          minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
          maxTargets: actionInfo.targetCount ?? 1,
          ...(actionInfo.requiresDamageDistribution ? { pendingActionInfo: actionInfo } : {}),
        })
      } else {
        getWebSocket()?.send(createSubmitActionMessage(actionWithDelve))
      }
    }

    set({ delveSelectionState: null })
  },

  // Mana color selection actions
  startManaColorSelection: (manaColorSelectionState) => {
    set({ manaColorSelectionState })
  },

  confirmManaColorSelection: (color) => {
    const { manaColorSelectionState, submitAction } = get()
    if (!manaColorSelectionState) return

    const action = manaColorSelectionState.action
    if (action.type === 'ActivateAbility') {
      submitAction({ ...action, manaColorChoice: color })
    } else {
      submitAction(action)
    }
    set({ manaColorSelectionState: null })
  },

  cancelManaColorSelection: () => {
    set({ manaColorSelectionState: null })
  },

  // Decision selection actions
  startDecisionSelection: (state) => {
    set({ decisionSelectionState: state })
  },

  toggleDecisionSelection: (cardId) => {
    set((state) => {
      if (!state.decisionSelectionState) return state
      const { selectedOptions, maxSelections } = state.decisionSelectionState
      const isSelected = selectedOptions.includes(cardId)
      if (isSelected) {
        return {
          decisionSelectionState: {
            ...state.decisionSelectionState,
            selectedOptions: selectedOptions.filter((id) => id !== cardId),
          },
        }
      } else if (selectedOptions.length < maxSelections) {
        return {
          decisionSelectionState: {
            ...state.decisionSelectionState,
            selectedOptions: [...selectedOptions, cardId],
          },
        }
      }
      return state
    })
  },

  cancelDecisionSelection: () => {
    set({ decisionSelectionState: null })
  },

  confirmDecisionSelection: () => {
    const { decisionSelectionState, pendingDecision, playerId } = get()
    if (!decisionSelectionState || !pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'CardsSelectedResponse' as const,
        decisionId: pendingDecision.id,
        selectedCards: [...decisionSelectionState.selectedOptions],
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))

    set({ decisionSelectionState: null })
  },

  // Damage distribution actions
  startDamageDistribution: (state) => {
    set({ damageDistributionState: state, lastDamageDistribution: null })
  },

  updateDamageDistribution: (targetId, amount) => {
    set((state) => {
      if (!state.damageDistributionState) return state
      return {
        damageDistributionState: {
          ...state.damageDistributionState,
          distribution: {
            ...state.damageDistributionState.distribution,
            [targetId]: amount,
          },
        },
      }
    })
  },

  cancelDamageDistribution: () => {
    set({ damageDistributionState: null })
  },

  confirmDamageDistribution: () => {
    const { damageDistributionState, submitAction } = get()
    if (!damageDistributionState) return

    const distribution = { ...damageDistributionState.distribution }
    const actionWithDistribution = {
      ...damageDistributionState.action,
      damageDistribution: distribution,
    }

    submitAction(actionWithDistribution)
    set({
      damageDistributionState: null,
      lastDamageDistribution: distribution,
    })
  },

  // Inline distribute actions (server-driven DistributeDecision)
  initDistribute: (distributeState) => {
    set({ distributeState })
  },

  incrementDistribute: (targetId) => {
    set((state) => {
      if (!state.distributeState) return state
      const dist = state.distributeState
      const totalAllocated = Object.values(dist.distribution).reduce<number>((sum, v) => sum + v, 0)
      if (totalAllocated >= dist.totalAmount) return state
      const current = dist.distribution[targetId] ?? 0
      const maxForTarget = dist.maxPerTarget?.[targetId]
      if (maxForTarget !== undefined && current >= maxForTarget) return state
      return {
        distributeState: {
          ...dist,
          distribution: {
            ...dist.distribution,
            [targetId]: current + 1,
          },
        },
      }
    })
  },

  decrementDistribute: (targetId) => {
    set((state) => {
      if (!state.distributeState) return state
      const dist = state.distributeState
      const current = dist.distribution[targetId] ?? 0
      if (current <= dist.minPerTarget) return state
      return {
        distributeState: {
          ...dist,
          distribution: {
            ...dist.distribution,
            [targetId]: current - 1,
          },
        },
      }
    })
  },

  confirmDistribute: () => {
    const { distributeState, submitDistributeDecision } = get()
    if (!distributeState) return
    const totalAllocated = Object.values(distributeState.distribution).reduce((sum, v) => sum + v, 0)
    if (distributeState.allowPartial) {
      if (totalAllocated > distributeState.totalAmount) return
    } else {
      if (totalAllocated !== distributeState.totalAmount) return
    }
    submitDistributeDecision(distributeState.distribution)
    set({ distributeState: null })
  },

  clearDistribute: () => {
    set({ distributeState: null })
  },

  // Counter distribution actions (for RemoveXPlusOnePlusOneCounters cost)
  startCounterDistribution: (counterDistributionState) => {
    set({ counterDistributionState })
  },

  incrementCounterRemoval: (entityId) => {
    set((state) => {
      if (!state.counterDistributionState) return state
      const dist = state.counterDistributionState
      const current = dist.distribution[entityId] ?? 0
      const creature = dist.creatures.find((c) => c.entityId === entityId)
      if (!creature || current >= creature.availableCounters) return state
      return {
        counterDistributionState: {
          ...dist,
          distribution: {
            ...dist.distribution,
            [entityId]: current + 1,
          },
        },
      }
    })
  },

  decrementCounterRemoval: (entityId) => {
    set((state) => {
      if (!state.counterDistributionState) return state
      const dist = state.counterDistributionState
      const current = dist.distribution[entityId] ?? 0
      if (current <= 0) return state
      return {
        counterDistributionState: {
          ...dist,
          distribution: {
            ...dist.distribution,
            [entityId]: current - 1,
          },
        },
      }
    })
  },

  cancelCounterDistribution: () => {
    set({ counterDistributionState: null })
  },

  confirmCounterDistribution: () => {
    const { counterDistributionState, startTargeting } = get()
    if (!counterDistributionState) return

    const { actionInfo, distribution } = counterDistributionState
    const totalAllocated = Object.values(distribution).reduce<number>((sum, v) => sum + v, 0)
    if (totalAllocated <= 0) return

    // Filter out zero entries
    const counterRemovals: Record<string, number> = {}
    for (const [eid, count] of Object.entries(distribution) as [string, number][]) {
      if (count > 0) {
        counterRemovals[eid] = count
      }
    }

    if (actionInfo.action.type === 'ActivateAbility') {
      const baseAction = actionInfo.action
      const actionWithCost = {
        ...baseAction,
        xValue: totalAllocated,
        costPayment: {
          ...baseAction.costPayment,
          counterRemovals,
        },
      }

      if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        startTargeting({
          action: actionWithCost,
          validTargets: [...actionInfo.validTargets],
          selectedTargets: [],
          minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
          maxTargets: actionInfo.targetCount ?? 1,
        })
      } else {
        getWebSocket()?.send(createSubmitActionMessage(actionWithCost))
      }
    }

    set({ counterDistributionState: null })
  },

  // Mana source selection actions (pre-cast)
  startManaSelection: (actionInfo) => {
    const sources = actionInfo.availableManaSources
    if (!sources || sources.length === 0) return
    const sourceColors: Record<string, readonly string[]> = {}
    const sourceManaAmounts: Record<string, number> = {}
    for (const source of sources) {
      const colors: string[] = [...(source.producesColors ?? [])]
      if (source.producesColorless && colors.length === 0) colors.push('C')
      sourceColors[source.entityId] = colors
      sourceManaAmounts[source.entityId] = source.manaAmount ?? 1
    }
    // Pre-select the autoTapPreview sources as the default
    const preSelected = actionInfo.autoTapPreview ?? []
    set({
      selectedCardId: null,
      manaSelectionState: {
        action: actionInfo.action,
        actionInfo,
        validSources: sources.map(s => s.entityId),
        selectedSources: [...preSelected],
        manaCost: actionInfo.manaCostString ?? '',
        xValue: 0,
        sourceColors,
        sourceManaAmounts,
      },
    })
  },

  toggleManaSource: (entityId) => {
    set((state) => {
      if (!state.manaSelectionState) return state
      const { selectedSources } = state.manaSelectionState
      const isSelected = selectedSources.includes(entityId)
      return {
        manaSelectionState: {
          ...state.manaSelectionState,
          selectedSources: isSelected
            ? selectedSources.filter(id => id !== entityId)
            : [...selectedSources, entityId],
        },
      }
    })
  },

  cancelManaSelection: () => {
    set({ manaSelectionState: null })
  },

  confirmManaSelection: () => {
    // Note: actual confirm logic is in GameBoard's handleConfirmManaSelection
    // which routes through executeAction for proper targeting/X/convoke flows.
    set({ manaSelectionState: null })
  },

  // Revealed cards actions
  showRevealedHand: (cardIds) => {
    set({ revealedHandCardIds: cardIds })
  },

  dismissRevealedHand: () => {
    set({ revealedHandCardIds: null })
  },

  showRevealedCards: (cardIds, cardNames, imageUris, source, isYourReveal) => {
    set({ revealedCardsInfo: { cardIds, cardNames, imageUris, source, isYourReveal } })
  },

  dismissRevealedCards: () => {
    set({ revealedCardsInfo: null })
  },

  // Animation actions
  addDrawAnimation: (animation) => {
    set((state) => ({
      drawAnimations: [...state.drawAnimations, animation],
    }))
  },

  removeDrawAnimation: (id) => {
    set((state) => ({
      drawAnimations: state.drawAnimations.filter((a) => a.id !== id),
    }))
  },

  addDamageAnimation: (animation) => {
    set((state) => ({
      damageAnimations: [...state.damageAnimations, animation],
    }))
  },

  removeDamageAnimation: (id) => {
    set((state) => ({
      damageAnimations: state.damageAnimations.filter((a) => a.id !== id),
    }))
  },

  addRevealAnimation: (animation) => {
    set((state) => ({
      revealAnimations: [...state.revealAnimations, animation],
    }))
  },

  removeRevealAnimation: (id) => {
    set((state) => ({
      revealAnimations: state.revealAnimations.filter((a) => a.id !== id),
    }))
  },

  addCoinFlipAnimation: (animation) => {
    set((state) => ({
      coinFlipAnimations: [...state.coinFlipAnimations, animation],
    }))
  },

  removeCoinFlipAnimation: (id) => {
    set((state) => ({
      coinFlipAnimations: state.coinFlipAnimations.filter((a) => a.id !== id),
    }))
  },

  addTargetReselectedAnimation: (animation) => {
    set((state) => ({
      targetReselectedAnimations: [...state.targetReselectedAnimations, animation],
    }))
  },

  removeTargetReselectedAnimation: (id) => {
    set((state) => ({
      targetReselectedAnimations: state.targetReselectedAnimations.filter((a) => a.id !== id),
    }))
  },

  setAutoTapPreview: (preview) => {
    set({ autoTapPreview: preview })
  },

  setMatchIntro: (intro) => {
    set({ matchIntro: intro })
  },

  clearMatchIntro: () => {
    set({ matchIntro: null })
  },
})
