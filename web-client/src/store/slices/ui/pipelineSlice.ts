/**
 * Pipeline coordinator slice — manages the multi-phase action submission flow.
 *
 * Computes the full phase sequence up front, then advances through it as each
 * phase's confirm handler reports its result. Existing per-phase UI state
 * (xSelectionState, targetingState, etc.) is preserved — components keep their
 * current subscriptions.
 */
import type { SliceCreator, ActionPipelineState, CostPreviewState, PhaseResult, PipelinePhase } from '../types'
import type {
  ActivateAbilityAction,
  CastSpellAction,
  CostPreviewMessage,
  EntityId,
  GameAction,
  LegalActionInfo,
} from '@/types'
import { createPreviewCostMessage } from '@/types'
import { computePhases, mergeResult, enterPhase } from './pipelinePhases'
import type { PipelineStoreMethods } from './pipelinePhases'
import { getWebSocket } from '../shared'

export interface PipelineSliceState {
  pipelineState: ActionPipelineState | null
  /**
   * The server's price for the draft being built — the one source every payment HUD and the
   * manual mana picker read their remaining cost, affordability and pre-selection from. Null
   * outside a pipeline.
   */
  costPreview: CostPreviewState | null
}

export interface PipelineSliceActions {
  startPipeline: (
    actionInfo: LegalActionInfo,
    options?: { forceManualTap?: boolean },
  ) => void
  advancePipeline: (result: PhaseResult) => void
  cancelPipeline: () => void
  /**
   * Ask the server what [draft] costs as it stands. Fire-and-forget: the reply lands through
   * [receiveCostPreview], and only the newest request's reply is honoured.
   */
  requestCostPreview: (draft: GameAction) => void
  /** A `costPreview` reply arrived; adopt it if it answers the latest request. */
  receiveCostPreview: (message: CostPreviewMessage) => void
}

/**
 * Phases whose UI shows a running cost: the preview is requested on entering each of them, and
 * again on every selection change inside them (see the toggle actions in `selectionSlice`).
 */
const COST_PHASES: ReadonlySet<PipelinePhase['type']> = new Set([
  'delve',
  'convoke',
  'tapForGeneric',
  'harmonize',
  'manaSource',
])

let previewRequestCounter = 0

export type PipelineSlice = PipelineSliceState & PipelineSliceActions

export const createPipelineSlice: SliceCreator<PipelineSlice> = (set, get) => ({
  pipelineState: null,
  costPreview: null,

  requestCostPreview: (draft) => {
    const requestId = `cp-${++previewRequestCounter}`
    set((state) => ({
      costPreview: {
        requestId,
        draft,
        pending: true,
        preview: state.costPreview?.preview ?? null,
      },
    }))
    getWebSocket()?.send(createPreviewCostMessage(draft, requestId))
  },

  receiveCostPreview: (message) => {
    const current = get().costPreview
    if (!current || current.requestId !== message.requestId) return
    set((state) => {
      const patch: Partial<import('../types').GameStore> = {
        costPreview: { ...current, pending: false, preview: message },
      }
      // The caps the phase HUDs enforce are the server's remaining generic: one more delve exile
      // or improvise tap can only buy what is still generic in the cost. Each card/permanent
      // already selected has been credited, so the cap is what's left plus what's already spent.
      const delve = state.delveSelectionState
      if (delve) {
        patch.delveSelectionState = {
          ...delve,
          maxDelve: Math.min(delve.validCards.length, message.genericRemaining + delve.selectedCards.length),
        }
      }
      const taps = state.tapForGenericSelectionState
      if (taps && taps.capFollowsGeneric) {
        patch.tapForGenericSelectionState = {
          ...taps,
          maxTaps: Math.min(taps.validPermanents.length, message.genericRemaining + taps.selectedPermanents.length),
        }
      }
      // The manual mana picker prices itself off the preview: the exact remaining cost and the
      // engine's own auto-tap as the pre-selection — unless the player has already started
      // picking, in which case only the cost readout is refreshed.
      const mana = state.manaSelectionState
      if (mana) {
        const validSources = new Set(mana.validSources)
        patch.manaSelectionState = {
          ...mana,
          manaCost: message.manaCostString,
          previewPending: false,
          ...(mana.userEdited
            ? {}
            : { selectedSources: (message.autoTapPreview ?? []).filter((id) => validSources.has(id)) }),
        }
      }
      return patch
    })
  },

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
    if (COST_PHASES.has(firstPhase.type)) get().requestCostPreview(accumulatedAction)
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

    // The cost after this step is the server's business — every later phase reads it from the
    // cost preview requested below, never from a client-side rewrite of `manaCostString`. What
    // does change here is the source list the manual mana picker offers: a permanent tapped for
    // convoke / improvise / waterbend / harmonize is spent, so it can't also be tapped for mana
    // (the Whir of Invention rulings say so explicitly, and the server rejects it). Drop those
    // ids, or a mana rock the player just improvised with still shows up in the land picker and
    // clicking it bounces.
    const spent = new Set<EntityId>()
    if (result.type === 'convoke') for (const id of Object.keys(result.convokedCreatures)) spent.add(id as EntityId)
    if (result.type === 'tapForGeneric') for (const id of result.tapForGenericPermanents) spent.add(id)
    if (result.type === 'harmonize' && result.harmonizeCreature) spent.add(result.harmonizeCreature)
    if (spent.size > 0 && actionInfo.availableManaSources) {
      actionInfo = {
        ...actionInfo,
        availableManaSources: actionInfo.availableManaSources.filter((source) => !spent.has(source.entityId)),
        action: mergedAction,
      }
    }

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
      set({ pipelineState: null, costPreview: null })
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
    // Price the draft as it now stands before the next cost HUD opens, so it can show the
    // engine's remaining cost (and, for the manual picker, the engine's own auto-tap).
    if (COST_PHASES.has(nextPhase.type)) get().requestCostPreview(mergedAction)
    enterPhase(nextPhase, actionInfo, mergedAction, getStoreMethods(get), gameState)
  },

  cancelPipeline: () => {
    set({
      pipelineState: null,
      costPreview: null,
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
