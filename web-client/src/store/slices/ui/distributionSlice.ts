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
// Note: getWebSocket/createSubmitActionMessage removed — no longer needed
// after legacy routing was replaced by the pipeline coordinator.

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
  incrementCounterRemoval: (entityId: EntityId, counterType: string) => void
  decrementCounterRemoval: (entityId: EntityId, counterType: string) => void
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
    const { damageDistributionState, pipelineState } = get()
    if (!damageDistributionState || !pipelineState) return

    const distribution = { ...damageDistributionState.distribution }
    set({ damageDistributionState: null, lastDamageDistribution: distribution })
    get().advancePipeline({ type: 'damageDistribution', distribution })
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

  incrementCounterRemoval: (entityId, counterType) => {
    set((state) => {
      if (!state.counterDistributionState) return state
      const dist = state.counterDistributionState
      const inner = dist.distribution[entityId] ?? {}
      const current = inner[counterType] ?? 0
      const creature = dist.creatures.find((c) => c.entityId === entityId)
      if (!creature) return state
      // Cap per-type by what the creature actually has of that type. Falls
      // back to total `availableCounters` for legacy payloads that didn't
      // include the per-type breakdown.
      const perTypeCap = creature.availableCountersByType?.[counterType] ?? creature.availableCounters
      if (current >= perTypeCap) return state
      // Fixed-cost mode: prevent exceeding the requiredTotal.
      if (dist.requiredTotal != null) {
        const allocated = totalAllocated(dist.distribution)
        if (allocated >= dist.requiredTotal) return state
      }
      return {
        counterDistributionState: {
          ...dist,
          distribution: {
            ...dist.distribution,
            [entityId]: { ...inner, [counterType]: current + 1 },
          },
        },
      }
    })
  },

  decrementCounterRemoval: (entityId, counterType) => {
    set((state) => {
      if (!state.counterDistributionState) return state
      const dist = state.counterDistributionState
      const inner = dist.distribution[entityId] ?? {}
      const current = inner[counterType] ?? 0
      if (current <= 0) return state
      return {
        counterDistributionState: {
          ...dist,
          distribution: {
            ...dist.distribution,
            [entityId]: { ...inner, [counterType]: current - 1 },
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
    const { counterDistributionState, pipelineState } = get()
    if (!counterDistributionState || !pipelineState) return

    const { distribution, requiredTotal } = counterDistributionState
    const allocated = totalAllocated(distribution)
    // Fixed-cost mode: must match exactly. X cost mode: any positive total confirms.
    if (requiredTotal != null) {
      if (allocated !== requiredTotal) return
    } else if (allocated <= 0) {
      return
    }

    const distributedCounterRemovals: { entityId: EntityId; counterType: string; count: number }[] = []
    for (const [entityId, byType] of Object.entries(distribution)) {
      for (const [counterType, count] of Object.entries(byType)) {
        if (count > 0) {
          distributedCounterRemovals.push({ entityId: entityId as EntityId, counterType, count })
        }
      }
    }

    set({ counterDistributionState: null })
    get().advancePipeline({
      type: 'counterDistribution',
      xValue: allocated,
      distributedCounterRemovals,
    })
  },
})

function totalAllocated(distribution: Record<string, Record<string, number>>): number {
  let sum = 0
  for (const byType of Object.values(distribution)) {
    for (const v of Object.values(byType)) sum += v
  }
  return sum
}
