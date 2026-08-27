/**
 * Selection sub-slice — handles X cost, convoke, crew, delve, mana color,
 * decision selection, and mana source selection flows.
 */
import type {
  SliceCreator,
  EntityId,
  ModalModeSelectionState,
  XSelectionState,
  BlightVariableSelectionState,
  PayXLifeSelectionState,
  ConvokeSelectionState,
  TapForGenericSelectionState,
  HarmonizeSelectionState,
  TapForPowerSelectionState,
  DelveSelectionState,
  ManaColorSelectionState,
  DecisionSelectionState,
  ManaSelectionState,
  ConvokeCreatureSelection,
  PhaseResult,
} from '../types'
import type { LegalActionInfo } from '@/types'
import { createSubmitActionMessage } from '@/types'
import { getWebSocket } from '../shared'
import { mergeResult } from './pipelinePhases'

// Note: getWebSocket/createSubmitActionMessage are still used by confirmTapForPowerSelection
// and confirmDecisionSelection (which are not part of the pipeline).

export interface SelectionSliceState {
  modalModeSelectionState: ModalModeSelectionState | null
  xSelectionState: XSelectionState | null
  blightVariableSelectionState: BlightVariableSelectionState | null
  payXLifeSelectionState: PayXLifeSelectionState | null
  convokeSelectionState: ConvokeSelectionState | null
  tapForGenericSelectionState: TapForGenericSelectionState | null
  harmonizeSelectionState: HarmonizeSelectionState | null
  tapForPowerSelectionState: TapForPowerSelectionState | null
  delveSelectionState: DelveSelectionState | null
  manaColorSelectionState: ManaColorSelectionState | null
  decisionSelectionState: DecisionSelectionState | null
  manaSelectionState: ManaSelectionState | null
}

export interface SelectionSliceActions {
  startModalModeSelection: (state: ModalModeSelectionState) => void
  confirmModalModeSelection: (chosenModes: number[]) => void
  cancelModalModeSelection: () => void
  startXSelection: (state: XSelectionState) => void
  updateXValue: (x: number) => void
  cancelXSelection: () => void
  confirmXSelection: () => void
  startBlightVariableSelection: (state: BlightVariableSelectionState) => void
  updateBlightVariableX: (x: number) => void
  cancelBlightVariableSelection: () => void
  confirmBlightVariableSelection: () => void
  startPayXLifeSelection: (state: PayXLifeSelectionState) => void
  updatePayXLifeX: (x: number) => void
  cancelPayXLifeSelection: () => void
  confirmPayXLifeSelection: () => void
  startConvokeSelection: (state: ConvokeSelectionState) => void
  toggleConvokeCreature: (entityId: EntityId, name: string, payingColor: string | null) => void
  cancelConvokeSelection: () => void
  confirmConvokeSelection: () => void
  startTapForGenericSelection: (state: TapForGenericSelectionState) => void
  toggleTapForGenericPermanent: (entityId: EntityId) => void
  cancelTapForGenericSelection: () => void
  confirmTapForGenericSelection: () => void
  startHarmonizeSelection: (state: HarmonizeSelectionState) => void
  toggleHarmonizeCreature: (entityId: EntityId) => void
  cancelHarmonizeSelection: () => void
  confirmHarmonizeSelection: () => void
  startTapForPowerSelection: (state: TapForPowerSelectionState) => void
  toggleTapForPowerCreature: (entityId: EntityId) => void
  setTapForPowerCreatures: (entityIds: readonly EntityId[]) => void
  cancelTapForPowerSelection: () => void
  confirmTapForPowerSelection: () => void
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
  startManaSelection: (actionInfo: LegalActionInfo) => void
  toggleManaSource: (entityId: EntityId) => void
  togglePhyrexianLifePayment: (pipIndex: number) => void
  cancelManaSelection: () => void
  confirmManaSelection: () => void
}

export type SelectionSlice = SelectionSliceState & SelectionSliceActions

export const createSelectionSlice: SliceCreator<SelectionSlice> = (set, get) => ({
  modalModeSelectionState: null,
  xSelectionState: null,
  blightVariableSelectionState: null,
  payXLifeSelectionState: null,
  convokeSelectionState: null,
  tapForGenericSelectionState: null,
  harmonizeSelectionState: null,
  tapForPowerSelectionState: null,
  delveSelectionState: null,
  manaColorSelectionState: null,
  decisionSelectionState: null,
  manaSelectionState: null,

  // Choose-N modal (Spree) mode selection — confirm advances the cast pipeline with the
  // chosen mode subset; the panel component owns its own draft selection state.
  startModalModeSelection: (modalModeSelectionState) => {
    set({ modalModeSelectionState })
  },

  confirmModalModeSelection: (chosenModes) => {
    const { modalModeSelectionState, pipelineState, advancePipeline } = get()
    if (!modalModeSelectionState || !pipelineState) return
    set({ modalModeSelectionState: null })
    advancePipeline({ type: 'modalModes', chosenModes })
  },

  cancelModalModeSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ modalModeSelectionState: null })
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
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ xSelectionState: null })
  },

  confirmXSelection: () => {
    const { xSelectionState, pipelineState } = get()
    if (!xSelectionState || !pipelineState) return

    set({ xSelectionState: null })
    get().advancePipeline({
      type: 'xSelection',
      xValue: xSelectionState.selectedX,
      ...(xSelectionState.isRepeatCount ? { isRepeatCount: true } : {}),
    })
  },

  // BlightVariable selection actions
  startBlightVariableSelection: (blightVariableSelectionState) => {
    set({ blightVariableSelectionState })
  },

  updateBlightVariableX: (x) => {
    set((state) => {
      if (!state.blightVariableSelectionState) return state
      const clamped = Math.max(0, Math.min(state.blightVariableSelectionState.maxX, x))
      return {
        blightVariableSelectionState: {
          ...state.blightVariableSelectionState,
          selectedX: clamped,
        },
      }
    })
  },

  cancelBlightVariableSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ blightVariableSelectionState: null })
  },

  confirmBlightVariableSelection: () => {
    const { blightVariableSelectionState, pipelineState } = get()
    if (!blightVariableSelectionState || !pipelineState) return
    const { selectedX } = blightVariableSelectionState
    set({ blightVariableSelectionState: null })
    get().advancePipeline({
      type: 'blightVariable',
      blightAmount: selectedX,
    })
  },

  // PayXLife selection actions
  startPayXLifeSelection: (payXLifeSelectionState) => {
    set({ payXLifeSelectionState })
  },

  updatePayXLifeX: (x) => {
    set((state) => {
      if (!state.payXLifeSelectionState) return state
      const clamped = Math.max(0, Math.min(state.payXLifeSelectionState.maxX, x))
      return {
        payXLifeSelectionState: {
          ...state.payXLifeSelectionState,
          selectedX: clamped,
        },
      }
    })
  },

  cancelPayXLifeSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ payXLifeSelectionState: null })
  },

  confirmPayXLifeSelection: () => {
    const { payXLifeSelectionState, pipelineState } = get()
    if (!payXLifeSelectionState || !pipelineState) return
    const { selectedX } = payXLifeSelectionState
    set({ payXLifeSelectionState: null })
    get().advancePipeline({
      type: 'payXLife',
      payXLifeAmount: selectedX,
    })
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
    const selected = get().convokeSelectionState?.selectedCreatures ?? []
    const convokedCreatures: Record<string, { color: string | null }> = {}
    for (const c of selected) convokedCreatures[c.entityId] = { color: c.payingColor }
    previewDraft(get, { type: 'convoke', convokedCreatures })
  },

  cancelConvokeSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ convokeSelectionState: null })
  },

  confirmConvokeSelection: () => {
    const { convokeSelectionState, pipelineState } = get()
    if (!convokeSelectionState || !pipelineState) return

    const convokedCreatures: Record<string, { color: string | null }> = {}
    for (const creature of convokeSelectionState.selectedCreatures) {
      convokedCreatures[creature.entityId] = { color: creature.payingColor }
    }

    set({ convokeSelectionState: null })
    get().advancePipeline({ type: 'convoke', convokedCreatures })
  },

  // Tap-for-generic selection actions (improvise CR 702.126 / waterbend). Generic-only —
  // clicking an eligible permanent toggles whether it is tapped to pay {1} of the cost.
  startTapForGenericSelection: (tapForGenericSelectionState) => {
    set({ tapForGenericSelectionState })
  },

  toggleTapForGenericPermanent: (entityId) => {
    set((state) => {
      if (!state.tapForGenericSelectionState) return state
      const { selectedPermanents, maxTaps } = state.tapForGenericSelectionState
      const alreadySelected = selectedPermanents.includes(entityId)
      // Can't tap more permanents than the generic mana being paid this way (CR 702.126a for
      // improvise). Ignore an attempt to select beyond the cap; deselecting is always allowed.
      if (!alreadySelected && selectedPermanents.length >= maxTaps) return state
      const newSelected = alreadySelected
        ? selectedPermanents.filter((id) => id !== entityId)
        : [...selectedPermanents, entityId]
      return {
        tapForGenericSelectionState: {
          ...state.tapForGenericSelectionState,
          selectedPermanents: newSelected,
        },
      }
    })
    previewDraft(get, {
      type: 'tapForGeneric',
      tapForGenericPermanents: [...(get().tapForGenericSelectionState?.selectedPermanents ?? [])],
    })
  },

  cancelTapForGenericSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ tapForGenericSelectionState: null })
  },

  confirmTapForGenericSelection: () => {
    const { tapForGenericSelectionState, pipelineState } = get()
    if (!tapForGenericSelectionState || !pipelineState) return
    const tapForGenericPermanents = [...tapForGenericSelectionState.selectedPermanents]
    set({ tapForGenericSelectionState: null })
    get().advancePipeline({ type: 'tapForGeneric', tapForGenericPermanents })
  },

  // Harmonize creature-tap selection actions (cast from graveyard via Harmonize). At most
  // one creature may be tapped; clicking the selected creature again clears it. Confirming
  // with none selected pays the full harmonize cost.
  startHarmonizeSelection: (harmonizeSelectionState) => {
    set({ harmonizeSelectionState })
  },

  toggleHarmonizeCreature: (creatureEntityId) => {
    set((state) => {
      if (!state.harmonizeSelectionState) return state
      const current = state.harmonizeSelectionState.selectedCreature
      return {
        harmonizeSelectionState: {
          ...state.harmonizeSelectionState,
          selectedCreature: current === creatureEntityId ? null : creatureEntityId,
        },
      }
    })
    const selected = get().harmonizeSelectionState?.selectedCreature ?? null
    previewDraft(get, { type: 'harmonize', harmonizeCreature: selected, reduction: 0 })
  },

  cancelHarmonizeSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ harmonizeSelectionState: null })
  },

  confirmHarmonizeSelection: () => {
    const { harmonizeSelectionState, pipelineState } = get()
    if (!harmonizeSelectionState || !pipelineState) return
    const selected = harmonizeSelectionState.selectedCreature
    const reduction = selected
      ? harmonizeSelectionState.validCreatures.find((c) => c.entityId === selected)?.power ?? 0
      : 0
    set({ harmonizeSelectionState: null })
    get().advancePipeline({ type: 'harmonize', harmonizeCreature: selected, reduction })
  },

  // Tap-creatures-for-power selection actions (Crew N / Saddle N)
  startTapForPowerSelection: (tapForPowerSelectionState) => {
    set({ tapForPowerSelectionState })
  },

  toggleTapForPowerCreature: (entityId) => {
    set((state) => {
      if (!state.tapForPowerSelectionState) return state
      const { selectedCreatures } = state.tapForPowerSelectionState
      const exists = selectedCreatures.includes(entityId)

      const newSelectedCreatures = exists
        ? selectedCreatures.filter((id) => id !== entityId)
        : [...selectedCreatures, entityId]

      return {
        tapForPowerSelectionState: {
          ...state.tapForPowerSelectionState,
          selectedCreatures: newSelectedCreatures,
        },
      }
    })
  },

  setTapForPowerCreatures: (entityIds) => {
    set((state) =>
      state.tapForPowerSelectionState
        ? {
            tapForPowerSelectionState: {
              ...state.tapForPowerSelectionState,
              selectedCreatures: [...entityIds],
            },
          }
        : state
    )
  },

  cancelTapForPowerSelection: () => {
    set({ tapForPowerSelectionState: null })
  },

  confirmTapForPowerSelection: () => {
    const { tapForPowerSelectionState, playerId } = get()
    if (!tapForPowerSelectionState || !playerId) return

    const { actionInfo, selectedCreatures } = tapForPowerSelectionState
    const action = actionInfo.action

    // The tapped-creature list goes into the action's mechanic-specific field.
    if (action.type === 'CrewVehicle') {
      getWebSocket()?.send(createSubmitActionMessage({ ...action, crewCreatures: selectedCreatures }))
    } else if (action.type === 'SaddleMount') {
      getWebSocket()?.send(createSubmitActionMessage({ ...action, saddleCreatures: selectedCreatures }))
    }

    set({ tapForPowerSelectionState: null })
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
    previewDraft(get, { type: 'delve', delvedCards: [...(get().delveSelectionState?.selectedCards ?? [])] })
  },

  cancelDelveSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ delveSelectionState: null })
  },

  confirmDelveSelection: () => {
    const { delveSelectionState, pipelineState } = get()
    if (!delveSelectionState || !pipelineState) return
    set({ delveSelectionState: null })
    get().advancePipeline({
      type: 'delve',
      delvedCards: [...delveSelectionState.selectedCards],
    })
  },

  // Mana color selection actions
  startManaColorSelection: (manaColorSelectionState) => {
    set({ manaColorSelectionState })
  },

  confirmManaColorSelection: (color) => {
    const { manaColorSelectionState, pipelineState } = get()
    if (!manaColorSelectionState || !pipelineState) return

    set({ manaColorSelectionState: null })
    get().advancePipeline({ type: 'manaColorChoice', color })
  },

  cancelManaColorSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
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
            warning: null,
          },
        }
      }
      if (selectedOptions.length < maxSelections) {
        return {
          decisionSelectionState: {
            ...state.decisionSelectionState,
            selectedOptions: [...selectedOptions, cardId],
            warning: null,
          },
        }
      }
      if (maxSelections === 1) {
        // Single-select step: clicking a different card replaces the previous pick so the
        // user doesn't have to deselect first. Without this, flows like Wear Down's gift
        // silently reject the new click and the player is stuck on the wrong target.
        return {
          decisionSelectionState: {
            ...state.decisionSelectionState,
            selectedOptions: [cardId],
            warning: null,
          },
        }
      }
      // Multi-select at cap: keep existing picks but flag a warning so the user knows
      // the click was ignored on purpose (and their spell won't fizzle from picking too
      // many). Cleared the moment the user makes a legal toggle.
      return {
        decisionSelectionState: {
          ...state.decisionSelectionState,
          warning: `You can select at most ${maxSelections} target${maxSelections === 1 ? '' : 's'} here — deselect one first to pick a different target.`,
        },
      }
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
      // Decision owner, not the connection's own seat — see gameplaySlice for hotseat rationale.
      playerId: pendingDecision.playerId,
      response: {
        type: 'CardsSelectedResponse' as const,
        decisionId: pendingDecision.id,
        selectedCards: [...decisionSelectionState.selectedOptions],
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))

    set({ decisionSelectionState: null })
  },

  // Mana source selection actions (pre-cast)
  startManaSelection: (actionInfo) => {
    const sources = actionInfo.availableManaSources ?? []
    if (sources.length === 0 && !(actionInfo.manaCostString ?? '').includes('/P}')) return
    const sourceColors: Record<string, readonly string[]> = {}
    const sourceManaAmounts: Record<string, number> = {}
    for (const source of sources) {
      const colors: string[] = [...(source.producesColors ?? [])]
      if (source.producesColorless && colors.length === 0) colors.push('C')
      sourceColors[source.entityId] = colors
      sourceManaAmounts[source.entityId] = source.manaAmount ?? 1
    }
    const validSources = sources.map((s) => s.entityId)

    // The cost this step charges is whatever the pipeline has built so far — X announced, cards
    // delved, creatures convoked or tapped for improvise/harmonize, an emerge sacrifice, the
    // per-target tax from the targets just picked. The server prices all of that in one place
    // (the cost preview the pipeline requested on entering this phase); the client only decides
    // which of the offered sources to tap. When the preview has already answered for this exact
    // draft, price off it now; otherwise open with the printed cost ({X} folded in) marked
    // provisional and let `receiveCostPreview` correct it — and pre-select the engine's own
    // auto-tap — the moment it lands.
    const preview = get().costPreview
    const answered = preview && !preview.pending && preview.draft === actionInfo.action ? preview.preview : null
    const xValue = (actionInfo.action as { xValue?: number }).xValue ?? 0
    const provisionalCost = (actionInfo.manaCostString ?? '').replace(/\{X\}/g, `{${xValue}}`)
    const selectedSources = answered
      ? (answered.autoTapPreview ?? []).filter((id) => validSources.includes(id))
      : []

    set({
      selectedCardId: null,
      manaSelectionState: {
        action: actionInfo.action,
        actionInfo,
        validSources,
        selectedSources,
        manaCost: answered ? answered.manaCostString : provisionalCost,
        previewPending: !answered,
        userEdited: false,
        sourceColors,
        sourceManaAmounts,
        phyrexianLifePipIndices: [],
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
          userEdited: true,
          selectedSources: isSelected
            ? selectedSources.filter(id => id !== entityId)
            : [...selectedSources, entityId],
        },
      }
    })
  },

  togglePhyrexianLifePayment: (pipIndex) => {
    set((state) => {
      if (!state.manaSelectionState) return state
      const selected = state.manaSelectionState.phyrexianLifePipIndices
      return {
        manaSelectionState: {
          ...state.manaSelectionState,
          userEdited: true,
          phyrexianLifePipIndices: selected.includes(pipIndex)
            ? selected.filter((index) => index !== pipIndex)
            : [...selected, pipIndex],
        },
      }
    })
  },

  cancelManaSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ manaSelectionState: null })
  },

  confirmManaSelection: () => {
    // Note: actual confirm logic is in GameBoard's handleConfirmManaSelection
    // which routes through executeAction (legacy) or advancePipeline (pipeline).
    set({ manaSelectionState: null })
  },
})

/**
 * Price the pipeline's draft with an in-progress phase selection folded in — the same
 * `mergeResult` the confirm handler will use, so the preview describes exactly what would be
 * submitted. No-op outside a pipeline.
 */
function previewDraft(get: () => import('../types').GameStore, result: PhaseResult): void {
  const { pipelineState, gameState, requestCostPreview } = get()
  if (!pipelineState || !gameState) return
  requestCostPreview(mergeResult(pipelineState.accumulatedAction, pipelineState.actionInfo, result, gameState))
}
