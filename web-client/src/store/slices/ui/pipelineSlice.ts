/**
 * Pipeline coordinator slice — manages the multi-phase action submission flow.
 *
 * Computes the full phase sequence up front, then advances through it as each
 * phase's confirm handler reports its result. Existing per-phase UI state
 * (xSelectionState, targetingState, etc.) is preserved — components keep their
 * current subscriptions.
 */
import type { SliceCreator, ActionPipelineState, PhaseResult } from '../types'
import type { CastSpellAction, EntityId, LegalActionInfo } from '@/types'
import { computePhases, mergeResult, enterPhase } from './pipelinePhases'
import type { PipelineStoreMethods } from './pipelinePhases'
import {
  parseManaCost as parseManaCostUtil,
  getRemainingCostSymbols,
  getRemainingCostAfterConvoke,
  trimAutoTapPreview,
} from '@/utils/manaCost'

export interface PipelineSliceState {
  pipelineState: ActionPipelineState | null
}

export interface PipelineSliceActions {
  startPipeline: (actionInfo: LegalActionInfo) => void
  advancePipeline: (result: PhaseResult) => void
  cancelPipeline: () => void
}

export type PipelineSlice = PipelineSliceState & PipelineSliceActions

export const createPipelineSlice: SliceCreator<PipelineSlice> = (set, get) => ({
  pipelineState: null,

  startPipeline: (actionInfo) => {
    const { autoTapEnabled } = get()
    const phases = computePhases(actionInfo, { autoTapEnabled })

    if (phases.length === 0) {
      // No interaction needed — submit directly
      get().submitAction(actionInfo.action)
      get().selectCard(null)
      return
    }

    let accumulatedAction = actionInfo.action

    // Pre-merge auto-selectable SacrificeSelf cost (no UI needed)
    const costInfo = actionInfo.additionalCostInfo
    if (costInfo?.costType === 'SacrificeSelf') {
      const validSacTargets = costInfo.validSacrificeTargets ?? []
      const sacrificeCount = costInfo.sacrificeCount ?? 1
      if (validSacTargets.length === sacrificeCount) {
        if (accumulatedAction.type === 'CastSpell') {
          accumulatedAction = {
            ...accumulatedAction,
            additionalCostPayment: { sacrificedPermanents: [...validSacTargets] },
          }
        } else if (accumulatedAction.type === 'ActivateAbility') {
          accumulatedAction = {
            ...accumulatedAction,
            costPayment: { sacrificedPermanents: [...validSacTargets] },
          }
        }
      }
    }

    set({
      pipelineState: {
        actionInfo,
        accumulatedAction,
        remainingPhases: phases,
      },
    })

    const firstPhase = phases[0]!
    enterPhase(firstPhase, actionInfo, accumulatedAction, getStoreMethods(get))
    get().selectCard(null)
  },

  advancePipeline: (result) => {
    const { pipelineState, gameState, submitAction } = get()
    if (!pipelineState || !gameState) return

    let { actionInfo } = pipelineState
    const { accumulatedAction, remainingPhases } = pipelineState

    // Merge result into accumulated action
    const mergedAction = mergeResult(accumulatedAction, actionInfo, result, gameState)

    // If delve modified the mana cost, update actionInfo for subsequent phases.
    // Also trim the server's full-cost autoTapPreview down to the subset needed for
    // the reduced cost — the engine will re-solve on submit, but this keeps the UI
    // pre-selection honest about what will actually tap.
    if (result.type === 'delve') {
      const originalSymbols = parseManaCostUtil(actionInfo.manaCostString ?? '')
      const remainingSymbols = getRemainingCostSymbols(originalSymbols, result.delvedCards.length)
      const modifiedManaCost = remainingSymbols.map((s) => `{${s}}`).join('')
      const trimmedPreview: readonly EntityId[] | undefined =
        actionInfo.autoTapPreview && actionInfo.availableManaSources
          ? trimAutoTapPreview(actionInfo.autoTapPreview, actionInfo.availableManaSources, remainingSymbols)
          : actionInfo.autoTapPreview
      const {
        hasDelve: _,
        validDelveCards: _2,
        minDelveNeeded: _3,
        autoTapPreview: _4,
        ...restActionInfo
      } = actionInfo
      actionInfo = {
        ...restActionInfo,
        manaCostString: modifiedManaCost,
        ...(trimmedPreview !== undefined ? { autoTapPreview: trimmedPreview } : {}),
        action: mergedAction,
      }
    }

    // If convoke modified the mana cost, update actionInfo for subsequent phases.
    // Trim the preview similarly so the manaSource phase pre-selection reflects the
    // reduced cost rather than over-selecting based on the original full cost.
    if (result.type === 'convoke') {
      const originalSymbols = parseManaCostUtil(actionInfo.manaCostString ?? '')
      const remainingSymbols = getRemainingCostAfterConvoke(originalSymbols, result.convokedCreatures)
      const modifiedManaCost = remainingSymbols.map((s) => `{${s}}`).join('')
      const trimmedPreview: readonly EntityId[] | undefined =
        actionInfo.autoTapPreview && actionInfo.availableManaSources
          ? trimAutoTapPreview(actionInfo.autoTapPreview, actionInfo.availableManaSources, remainingSymbols)
          : actionInfo.autoTapPreview
      const {
        hasConvoke: _,
        validConvokeCreatures: _2,
        autoTapPreview: _3,
        ...restActionInfo
      } = actionInfo
      actionInfo = {
        ...restActionInfo,
        manaCostString: modifiedManaCost,
        ...(trimmedPreview !== undefined ? { autoTapPreview: trimmedPreview } : {}),
        action: mergedAction,
      }
    }

    // Pop current phase
    const nextPhases = remainingPhases.slice(1)

    // Dynamic phase injection: damage distribution after targeting with >1 targets
    if (
      result.type === 'targeting' &&
      actionInfo.requiresDamageDistribution &&
      actionInfo.totalDamageToDistribute &&
      result.selectedTargets.length > 1
    ) {
      const cardName = actionInfo.description.replace('Cast ', '')
      const minPerTarget = actionInfo.minDamagePerTarget ?? 1
      const initialDistribution: Record<string, number> = {}
      for (const targetId of result.selectedTargets) {
        initialDistribution[targetId] = minPerTarget
      }

      set({
        pipelineState: {
          actionInfo,
          accumulatedAction: mergedAction,
          remainingPhases: [{ type: 'damageDistribution' }, ...nextPhases],
        },
      })

      get().startDamageDistribution({
        actionInfo,
        action: mergedAction as CastSpellAction,
        cardName,
        targetIds: [...result.selectedTargets],
        totalDamage: actionInfo.totalDamageToDistribute,
        minPerTarget,
        distribution: initialDistribution,
      })
      return
    }

    if (nextPhases.length === 0) {
      // All phases complete — submit
      set({ pipelineState: null })
      submitAction(mergedAction)
      return
    }

    // Update pipeline state and enter next phase
    set({
      pipelineState: {
        actionInfo,
        accumulatedAction: mergedAction,
        remainingPhases: nextPhases,
      },
    })

    const nextPhase = nextPhases[0]!
    enterPhase(nextPhase, actionInfo, mergedAction, getStoreMethods(get))
  },

  cancelPipeline: () => {
    set({
      pipelineState: null,
      targetingState: null,
      xSelectionState: null,
      convokeSelectionState: null,
      delveSelectionState: null,
      manaSelectionState: null,
      manaColorSelectionState: null,
      counterDistributionState: null,
      damageDistributionState: null,
    })
  },
})

function getStoreMethods(get: () => import('../types').GameStore): PipelineStoreMethods {
  const state = get()
  return {
    startXSelection: state.startXSelection,
    startConvokeSelection: state.startConvokeSelection,
    startDelveSelection: state.startDelveSelection,
    startCounterDistribution: state.startCounterDistribution,
    startManaSelection: state.startManaSelection,
    startManaColorSelection: state.startManaColorSelection,
    startTargeting: state.startTargeting,
    startDamageDistribution: state.startDamageDistribution,
  }
}
