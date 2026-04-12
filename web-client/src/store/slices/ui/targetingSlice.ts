/**
 * Targeting sub-slice — handles target selection for spells/abilities,
 * including sacrifice/discard/exile cost phases and multi-target flows.
 *
 * All routing logic lives in the pipeline coordinator. This slice only
 * manages the targeting UI state and reports results via advancePipeline.
 */
import type {
  SliceCreator,
  EntityId,
  TargetingState,
} from '../types'

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
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ targetingState: null })
  },

  confirmTargeting: () => {
    const { targetingState, pipelineState, gameState, startTargeting } = get()
    if (!targetingState || !gameState || !pipelineState) return

    const currentPhase = pipelineState.remainingPhases[0]

    // Cost payment phase (sacrifice/discard/tap/bounce/exile/blight selection)
    if (currentPhase?.type === 'costPayment') {
      const costType =
        pipelineState.actionInfo.additionalCostInfo?.costType ?? 'SacrificePermanent'
      set({ targetingState: null })
      get().advancePipeline({
        type: 'costPayment',
        costType,
        selectedTargets: [...targetingState.selectedTargets],
      })
      return
    }

    // Targeting phase
    if (currentPhase?.type === 'targeting') {
      // Multi-target advancement within the targeting phase
      if (targetingState.targetRequirements && targetingState.targetRequirements.length > 1) {
        const currentIndex = targetingState.currentRequirementIndex ?? 0
        const nextIndex = currentIndex + 1
        const allSelected = targetingState.allSelectedTargets
          ? [...targetingState.allSelectedTargets, targetingState.selectedTargets]
          : [targetingState.selectedTargets]

        const nextReq = targetingState.targetRequirements[nextIndex]
        if (nextReq) {
          // More requirements — stay within targeting phase
          const alreadySelected = allSelected.flat()
          const filteredValidTargets = nextReq.validTargets.filter(
            (t) => !alreadySelected.includes(t),
          )
          startTargeting({
            action: pipelineState.accumulatedAction,
            validTargets: filteredValidTargets,
            selectedTargets: [],
            minTargets: nextReq.minTargets,
            maxTargets: nextReq.maxTargets,
            currentRequirementIndex: nextIndex,
            allSelectedTargets: allSelected,
            targetRequirements: targetingState.targetRequirements,
            ...(nextReq.targetZone ? { targetZone: nextReq.targetZone } : {}),
            targetDescription: nextReq.description,
            ...(targetingState.totalRequirements != null
              ? { totalRequirements: targetingState.totalRequirements }
              : {}),
            ...(targetingState.requiresDamageDistribution
              ? { requiresDamageDistribution: true }
              : {}),
          })
          return
        }

        // All requirements filled
        const allTargets = [...allSelected.flat()]
        set({ targetingState: null })
        get().advancePipeline({ type: 'targeting', selectedTargets: allTargets })
        return
      }

      // Single-target flow
      set({ targetingState: null })
      get().advancePipeline({
        type: 'targeting',
        selectedTargets: [...targetingState.selectedTargets],
      })
    }
  },
})
