/**
 * Distribution sub-slice — handles damage distribution (for divided damage spells),
 * inline distribute decisions (server-driven), and counter distribution.
 */
import type {
  SliceCreator,
  EntityId,
  DamageDistributionState,
  DistributeState,
  CounterDistributionState,
} from '../types'
import { createSubmitActionMessage } from '../../../types'
import { getWebSocket } from '../shared'

export interface DistributionSliceState {
  damageDistributionState: DamageDistributionState | null
  /** Persisted damage distribution from the last confirmed DamageDistributionModal (target ID -> damage) */
  lastDamageDistribution: Record<EntityId, number> | null
  distributeState: DistributeState | null
  counterDistributionState: CounterDistributionState | null
}

export interface DistributionSliceActions {
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
}

export type DistributionSlice = DistributionSliceState & DistributionSliceActions

export const createDistributionSlice: SliceCreator<DistributionSlice> = (set, get) => ({
  damageDistributionState: null,
  lastDamageDistribution: null,
  distributeState: null,
  counterDistributionState: null,

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
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ damageDistributionState: null })
  },

  confirmDamageDistribution: () => {
    const { damageDistributionState, pipelineState, submitAction } = get()
    if (!damageDistributionState) return

    const distribution = { ...damageDistributionState.distribution }

    // Pipeline path
    if (pipelineState) {
      set({ damageDistributionState: null, lastDamageDistribution: distribution })
      get().advancePipeline({ type: 'damageDistribution', distribution })
      return
    }

    // Legacy path
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
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ counterDistributionState: null })
  },

  confirmCounterDistribution: () => {
    const { counterDistributionState, pipelineState, startTargeting } = get()
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

    // Pipeline path
    if (pipelineState) {
      set({ counterDistributionState: null })
      get().advancePipeline({
        type: 'counterDistribution',
        xValue: totalAllocated,
        counterRemovals,
      })
      return
    }

    // Legacy path
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
})
