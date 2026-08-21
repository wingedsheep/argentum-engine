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
} from '../types'
import type { LegalActionInfo } from '@/types'
import { createSubmitActionMessage } from '@/types'
import { getWebSocket } from '../shared'
import {
  parseManaCost as parseManaCostUtil,
  getRemainingCostSymbols,
  computeAutoTapPreview,
  reduceCostByHarmonizeTap,
} from '@/utils/manaCost'

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
  },

  cancelDelveSelection: () => {
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ delveSelectionState: null })
  },

  confirmDelveSelection: () => {
    const { delveSelectionState, pipelineState } = get()
    if (!delveSelectionState || !pipelineState) return

    const originalSymbols = parseManaCostUtil(delveSelectionState.manaCost)
    const remainingSymbols = getRemainingCostSymbols(originalSymbols, delveSelectionState.selectedCards.length)
    const modifiedManaCost = remainingSymbols.map(s => `{${s}}`).join('')

    set({ delveSelectionState: null })
    get().advancePipeline({
      type: 'delve',
      delvedCards: [...delveSelectionState.selectedCards],
      modifiedManaCost,
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
    // The accumulated action carries xValue from the prior xSelection phase.
    // The server's autoTapPreview only covers the fixed cost (X is unknown
    // server-side), so we extend the pre-selection with enough additional
    // sources to also cover xValue * (number of {X} symbols).
    const action = actionInfo.action as {
      xValue?: number
      targets?: readonly unknown[]
      alternativePayment?: { harmonizeCreature?: EntityId | null }
      additionalCostPayment?: { sacrificedPermanents?: readonly EntityId[] }
    }
    const xValue = action.xValue ?? 0
    // Emerge (CR 702.119): the creature chosen in the prior costPayment phase reduces the emerge
    // cost by its mana value, so the printed `manaCostString` overstates what's owed. The server
    // sent the resulting cost for every candidate, so price this step off the chosen entry rather
    // than re-deriving the (generic-only) reduction here — that clamp is a rule, not client math.
    const emergeSacrifice = action.additionalCostPayment?.sacrificedPermanents?.[0]
    const emergeCost = emergeSacrifice
      ? actionInfo.additionalCostInfo?.costAfterSacrifice?.[emergeSacrifice]
      : undefined
    // "This spell costs {W}{U} more to cast for each target beyond the first" (Officious
    // Interrogation): the server advertises the one-target minimum, because it prices the cost
    // before any target exists. `computePhases` puts targeting ahead of this step precisely so the
    // tax is knowable here — append one copy of the increment per target beyond the first. Same
    // shape as the emerge/harmonize adjustments around it: the server owns the rule, the client
    // only applies the choice the player has since made.
    const perExtraTarget = actionInfo.manaCostPerExtraTarget
    const extraTargets = Math.max(0, (action.targets?.length ?? 0) - 1)
    const targetTax =
      perExtraTarget && extraTargets > 0 ? perExtraTarget.repeat(extraTargets) : ''
    const manaCost = (emergeCost ?? actionInfo.manaCostString ?? '') + targetTax
    const xSymbolCount = Math.max(1, (manaCost.match(/\{X\}/g)?.length ?? 0))

    // Harmonize creature-tap (chosen in the prior `harmonize` phase) reduces the
    // generic mana to pay by the tapped creature's power. The server can't know
    // which creature the player picked, so neither autoTapPreview nor the X
    // extension below account for it — apply it here.
    const harmonizeCreatureId = action.alternativePayment?.harmonizeCreature
    const harmonizeReduction = harmonizeCreatureId
      ? actionInfo.validHarmonizeCreatures?.find((c) => c.entityId === harmonizeCreatureId)?.power ?? 0
      : 0

    // When a creature was tapped, recompute the whole pre-selection from the
    // reduced cost (colored pips + generic remaining after the tap) so we don't
    // over-tap lands for generic the tap already covered.
    if (harmonizeReduction > 0) {
      const reduced = reduceCostByHarmonizeTap(manaCost, xValue, harmonizeReduction)
      set({
        selectedCardId: null,
        manaSelectionState: {
          action: actionInfo.action,
          actionInfo,
          validSources: sources.map((s) => s.entityId),
          selectedSources: computeAutoTapPreview(sources, reduced),
          manaCost,
          xValue,
          harmonizeReduction,
          sourceColors,
          sourceManaAmounts,
          phyrexianLifePipIndices: [],
        },
      })
      return
    }

    const xManaNeeded = xValue * xSymbolCount

    // The server only computes [autoTapPreview] when the spell is affordable
    // from lands alone — for spells that require convoke/delve to be castable
    // (e.g. Sun-Dappled Celebrant when lands provide too few colored pips),
    // [autoTapPreview] is null. Once convoke/delve has trimmed the cost we can
    // compute a fresh preview from [availableManaSources] so the player isn't
    // left to hand-pick lands.
    // An emerge cast's server preview was solved for the *first* candidate; the player may have
    // picked a different creature, so recompute from the cost that choice actually left.
    const reducedSymbols = parseManaCostUtil(manaCost)
    const preSelectedIds: EntityId[] = (!emergeCost && actionInfo.autoTapPreview && actionInfo.autoTapPreview.length > 0)
      ? [...actionInfo.autoTapPreview]
      : (reducedSymbols.length > 0 ? computeAutoTapPreview(sources, reducedSymbols) : [])
    if (xManaNeeded > 0) {
      const alreadySelected = new Set(preSelectedIds)
      const manaProvided = (id: string) => sourceManaAmounts[id] ?? 1
      // Extend with sources not yet picked, preferring least-flexible
      // (colorless / fewest colors) first since X is generic mana and we
      // want to keep multi-color sources available for future plays. Server
      // re-solves on submit (CastPaymentProcessor.explicitPay), so the exact
      // ordering only affects the default — over-selection is safe.
      const candidates = sources
        .filter(s => !alreadySelected.has(s.entityId))
        .map(s => ({ id: s.entityId, flexibility: (sourceColors[s.entityId]?.length ?? 0) }))
        .sort((a, b) => a.flexibility - b.flexibility)
      let remaining = xManaNeeded
      for (const c of candidates) {
        if (remaining <= 0) break
        preSelectedIds.push(c.id)
        remaining -= manaProvided(c.id)
      }
    }
    set({
      selectedCardId: null,
      manaSelectionState: {
        action: actionInfo.action,
        actionInfo,
        validSources: sources.map(s => s.entityId),
        selectedSources: preSelectedIds,
        manaCost,
        xValue,
        harmonizeReduction: 0,
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
