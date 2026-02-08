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
  DecisionSelectionState,
  DamageDistributionState,
  DistributeState,
  DrawAnimation,
  DamageAnimation,
  ConvokeCreatureSelection,
} from './types'
import {
  entityId,
  createSubmitActionMessage,
  createUpdateBlockerAssignmentsMessage,
} from '../../types'
import { getWebSocket } from './shared'

export interface UISliceState {
  selectedCardId: EntityId | null
  targetingState: TargetingState | null
  combatState: CombatState | null
  xSelectionState: XSelectionState | null
  convokeSelectionState: ConvokeSelectionState | null
  decisionSelectionState: DecisionSelectionState | null
  damageDistributionState: DamageDistributionState | null
  distributeState: DistributeState | null
  hoveredCardId: EntityId | null
  autoTapPreview: readonly EntityId[] | null
  draggingBlockerId: EntityId | null
  draggingCardId: EntityId | null
  revealedHandCardIds: readonly EntityId[] | null
  revealedCardsInfo: {
    cardIds: readonly EntityId[]
    cardNames: readonly string[]
    imageUris: readonly (string | null)[]
    source: string | null
    isYourReveal: boolean
  } | null
  opponentBlockerAssignments: Record<EntityId, EntityId> | null
  drawAnimations: readonly DrawAnimation[]
  damageAnimations: readonly DamageAnimation[]
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
  assignBlocker: (blockerId: EntityId, attackerId: EntityId) => void
  removeBlockerAssignment: (blockerId: EntityId) => void
  clearBlockerAssignments: () => void
  startDraggingBlocker: (blockerId: EntityId) => void
  stopDraggingBlocker: () => void
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
  showRevealedHand: (cardIds: readonly EntityId[]) => void
  dismissRevealedHand: () => void
  showRevealedCards: (cardIds: readonly EntityId[], cardNames: readonly string[], imageUris: readonly (string | null)[], source: string | null, isYourReveal: boolean) => void
  dismissRevealedCards: () => void
  addDrawAnimation: (animation: DrawAnimation) => void
  removeDrawAnimation: (id: string) => void
  addDamageAnimation: (animation: DamageAnimation) => void
  removeDamageAnimation: (id: string) => void
}

export type UISlice = UISliceState & UISliceActions

export const createUISlice: SliceCreator<UISlice> = (set, get) => ({
  // Initial state
  selectedCardId: null,
  targetingState: null,
  combatState: null,
  xSelectionState: null,
  convokeSelectionState: null,
  decisionSelectionState: null,
  damageDistributionState: null,
  distributeState: null,
  hoveredCardId: null,
  autoTapPreview: null,
  draggingBlockerId: null,
  draggingCardId: null,
  revealedHandCardIds: null,
  revealedCardsInfo: null,
  opponentBlockerAssignments: null,
  drawAnimations: [],
  damageAnimations: [],

  // Card selection actions
  selectCard: (cardId) => {
    set({ selectedCardId: cardId })
  },

  hoverCard: (cardId) => {
    let autoTapPreview: readonly EntityId[] | null = null
    if (cardId) {
      const { legalActions } = get()
      const castAction = legalActions.find(
        (a) => a.action.type === 'CastSpell' && a.action.cardId === cardId
      )
      if (castAction?.autoTapPreview) {
        autoTapPreview = castAction.autoTapPreview
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
    // Auto-advance for multi-target spells when max targets reached
    const currentState = get().targetingState
    if (
      currentState &&
      currentState.targetRequirements &&
      currentState.targetRequirements.length > 1 &&
      currentState.selectedTargets.length >= currentState.maxTargets
    ) {
      get().confirmTargeting()
    }
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
        const actionWithCost = {
          ...action,
          additionalCostPayment: {
            sacrificedPermanents: [...targetingState.selectedTargets],
          },
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
        const isTapCost = actionInfo.additionalCostInfo?.costType === 'TapPermanents'
        const actionWithCost = {
          ...action,
          costPayment: isTapCost
            ? { tappedPermanents: [...targetingState.selectedTargets] }
            : { sacrificedPermanents: [...targetingState.selectedTargets] },
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

      return {
        combatState: {
          ...state.combatState,
          selectedAttackers: newAttackers,
        },
      }
    })
  },

  assignBlocker: (blockerId, attackerId) => {
    set((state) => {
      if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
        return state
      }

      const newAssignments = {
        ...state.combatState.blockerAssignments,
        [blockerId]: attackerId,
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
        attackers[attackerId] = opponent.playerId
      }

      const action = {
        type: 'DeclareAttackers' as const,
        playerId,
        attackers,
      }
      getWebSocket()?.send(createSubmitActionMessage(action))
    } else if (combatState.mode === 'declareBlockers') {
      const blockers: Record<EntityId, readonly EntityId[]> = {}
      for (const [blockerIdStr, attackerId] of Object.entries(combatState.blockerAssignments)) {
        blockers[entityId(blockerIdStr)] = [attackerId]
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
    set({ damageDistributionState: state })
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

    const actionWithDistribution = {
      ...damageDistributionState.action,
      damageDistribution: { ...damageDistributionState.distribution },
    }

    submitAction(actionWithDistribution)
    set({ damageDistributionState: null })
  },

  // Inline distribute actions (server-driven DistributeDecision)
  initDistribute: (distributeState) => {
    set({ distributeState })
  },

  incrementDistribute: (targetId) => {
    set((state) => {
      if (!state.distributeState) return state
      const dist = state.distributeState
      const totalAllocated = Object.values(dist.distribution).reduce((sum, v) => sum + v, 0)
      if (totalAllocated >= dist.totalAmount) return state
      return {
        distributeState: {
          ...dist,
          distribution: {
            ...dist.distribution,
            [targetId]: (dist.distribution[targetId] ?? 0) + 1,
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
    if (totalAllocated !== distributeState.totalAmount) return
    submitDistributeDecision(distributeState.distribution)
    set({ distributeState: null })
  },

  clearDistribute: () => {
    set({ distributeState: null })
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
})
