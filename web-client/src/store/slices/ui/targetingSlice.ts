/**
 * Targeting sub-slice — handles target selection for spells/abilities,
 * including sacrifice/discard/exile cost phases and multi-target flows.
 */
import type {
  SliceCreator,
  EntityId,
  TargetingState,
} from '../types'
import type { CastSpellAction } from '../../../types'

export interface TargetingSliceState {
  targetingState: TargetingState | null
}

export interface TargetingSliceActions {
  startTargeting: (state: TargetingState) => void
  addTarget: (targetId: EntityId) => void
  removeTarget: (targetId: EntityId) => void
  cancelTargeting: () => void
  confirmTargeting: () => void
}

export type TargetingSlice = TargetingSliceState & TargetingSliceActions

export const createTargetingSlice: SliceCreator<TargetingSlice> = (set, get) => ({
  targetingState: null,

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
          action: modifiedAction as CastSpellAction,
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
})
