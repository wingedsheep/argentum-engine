/**
 * Pipeline coordinator slice — manages the multi-phase action submission flow.
 *
 * Computes the full phase sequence up front, then advances through it as each
 * phase's confirm handler reports its result. Existing per-phase UI state
 * (xSelectionState, targetingState, etc.) is preserved — components keep their
 * current subscriptions.
 */
import type { SliceCreator, ActionPipelineState, PhaseResult } from '../types'
import type { ActivateAbilityAction, CastSpellAction, EntityId, LegalActionInfo } from '@/types'
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
  startPipeline: (
    actionInfo: LegalActionInfo,
    options?: { forceManualTap?: boolean },
  ) => void
  advancePipeline: (result: PhaseResult) => void
  cancelPipeline: () => void
}

export type PipelineSlice = PipelineSliceState & PipelineSliceActions

export const createPipelineSlice: SliceCreator<PipelineSlice> = (set, get) => ({
  pipelineState: null,

  startPipeline: (actionInfo, options) => {
    // Refuse to start a new cast/activation while one is already in progress — you must finish or
    // cancel the current pipeline (e.g. the improvise/waterbend tap step) first. Guards against casting a
    // second spell from hand mid-cast even if some interaction path slips past the UI gating.
    if (get().pipelineState != null) return
    const autoTapEnabled = options?.forceManualTap ? false : get().autoTapEnabled
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
    const gameStateForPhase = get().gameState
    enterPhase(
      firstPhase,
      actionInfo,
      accumulatedAction,
      getStoreMethods(get),
      gameStateForPhase ?? undefined,
    )
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
      // If X was resolved earlier, expand each {X} symbol to its numeric value so
      // getRemainingCostSymbols can reduce that generic via delve.
      const xValue =
        mergedAction.type === 'CastSpell' ? mergedAction.xValue ?? 0 : 0
      const resolvedSymbols =
        xValue > 0
          ? originalSymbols.map((s) => (s === 'X' ? String(xValue) : s))
          : originalSymbols
      const remainingSymbols = getRemainingCostSymbols(resolvedSymbols, result.delvedCards.length)
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

    // If a tap-for-generic payment (improvise / waterbend) tapped permanents, reduce the generic
    // cost shown to subsequent phases (each tapped permanent pays {1} generic). Mirrors convoke.
    if (result.type === 'tapForGeneric') {
      const originalSymbols = parseManaCostUtil(actionInfo.manaCostString ?? '')
      const remainingSymbols = [...originalSymbols]
      for (let i = 0; i < result.tapForGenericPermanents.length; i++) {
        const gIdx = remainingSymbols.findIndex((s) => /^\d+$/.test(s))
        if (gIdx < 0) break
        const val = parseInt(remainingSymbols[gIdx]!, 10)
        if (val > 1) remainingSymbols[gIdx] = String(val - 1)
        else remainingSymbols.splice(gIdx, 1)
      }
      const modifiedManaCost = remainingSymbols.map((s) => `{${s}}`).join('')
      const trimmedPreview: readonly EntityId[] | undefined =
        actionInfo.autoTapPreview && actionInfo.availableManaSources
          ? trimAutoTapPreview(actionInfo.autoTapPreview, actionInfo.availableManaSources, remainingSymbols)
          : actionInfo.autoTapPreview
      // A permanent tapped for improvise/waterbend is spent — it can't also be tapped for mana
      // (the Whir of Invention rulings say so explicitly, and the server rejects it). Drop the
      // tapped ids from the source list the manaSource phase offers, or a mana rock the player
      // just improvised with still shows up in the land picker and clicking it bounces.
      const tappedIds = new Set<EntityId>(result.tapForGenericPermanents)
      const remainingSources = actionInfo.availableManaSources?.filter(
        (source) => !tappedIds.has(source.entityId),
      )
      const {
        hasTapForGeneric: _,
        validTapForGenericPermanents: _2,
        autoTapPreview: _3,
        ...restActionInfo
      } = actionInfo
      actionInfo = {
        ...restActionInfo,
        manaCostString: modifiedManaCost,
        ...(trimmedPreview !== undefined ? { autoTapPreview: trimmedPreview } : {}),
        ...(remainingSources !== undefined ? { availableManaSources: remainingSources } : {}),
        action: mergedAction,
      }
    }

    // Note: a harmonize creature-tap reduces the generic the player owes, but we don't
    // rewrite manaCostString here. With auto-tap (default) the manaSource phase is skipped
    // and the server auto-pays the reduced cost (CastSpellHandler applies the tap + the
    // X-mana reduction). In manual-tap mode the manaSource pre-selection may over-pick by
    // the tap's power, which is harmless — the server re-solves on submit and taps only
    // what's needed. The HarmonizeSelector HUD shows the player the reduced cost.

    // Pop current phase
    let nextPhases = remainingPhases.slice(1)

    // Dynamic phase injection: when BlightVariable is paid with X > 0, we need
    // a follow-up battlefield-target step so the player picks which of their
    // creatures receives the X -1/-1 counters. Inject a costPayment phase that
    // reuses the existing `Blight`/`BlightVariable` targeting flow.
    if (result.type === 'blightVariable' && result.blightAmount > 0) {
      nextPhases = [{ type: 'costPayment' }, ...nextPhases]
    }

    // Dynamic phase injection: a non-mana escalate cost (CR 702.120a) is owed once per mode
    // chosen beyond the first, so how much there is to pay — and whether anything is owed at
    // all — is only known once the modal panel has confirmed its picks.
    if (
      result.type === 'modalModes' &&
      actionInfo.modalEnumeration?.additionalCostPerExtraMode &&
      result.chosenModes.length > 1
    ) {
      nextPhases = [{ type: 'escalateCost' }, ...nextPhases]
    }

    // Dynamic phase injection: damage distribution after targeting with >1 targets
    if (
      result.type === 'targeting' &&
      actionInfo.requiresDamageDistribution &&
      actionInfo.totalDamageToDistribute &&
      result.selectedTargets.length > 1
    ) {
      // Spells read "Cast <name>", activated abilities "Activate <name>: …" — strip either verb so
      // the modal header names the source (Chandra, Flameshaper's −4 divides damage the same way
      // Arc Lightning does).
      const cardName = actionInfo.description.replace(/^(Cast|Activate) /, '')
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
        action: mergedAction as CastSpellAction | ActivateAbilityAction,
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
    enterPhase(nextPhase, actionInfo, mergedAction, getStoreMethods(get), gameState)
  },

  cancelPipeline: () => {
    set({
      pipelineState: null,
      targetingState: null,
      xSelectionState: null,
      modalModeSelectionState: null,
      blightVariableSelectionState: null,
      payXLifeSelectionState: null,
      convokeSelectionState: null,
      tapForGenericSelectionState: null,
      harmonizeSelectionState: null,
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
    startModalModeSelection: state.startModalModeSelection,
    startXSelection: state.startXSelection,
    startBlightVariableSelection: state.startBlightVariableSelection,
    startPayXLifeSelection: state.startPayXLifeSelection,
    startConvokeSelection: state.startConvokeSelection,
    startTapForGenericSelection: state.startTapForGenericSelection,
    startHarmonizeSelection: state.startHarmonizeSelection,
    startDelveSelection: state.startDelveSelection,
    startCounterDistribution: state.startCounterDistribution,
    startManaSelection: state.startManaSelection,
    startManaColorSelection: state.startManaColorSelection,
    startTargeting: state.startTargeting,
    startDamageDistribution: state.startDamageDistribution,
  }
}
