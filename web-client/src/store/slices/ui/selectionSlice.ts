/**
 * Selection sub-slice — handles X cost, convoke, crew, delve, mana color,
 * decision selection, and mana source selection flows.
 */
import type {
  SliceCreator,
  EntityId,
  XSelectionState,
  ConvokeSelectionState,
  CrewSelectionState,
  DelveSelectionState,
  ManaColorSelectionState,
  DecisionSelectionState,
  ManaSelectionState,
  ConvokeCreatureSelection,
} from '../types'
import type { LegalActionInfo } from '../../../types'
import { createSubmitActionMessage } from '../../../types'
import { getWebSocket } from '../shared'
import { parseManaCost as parseManaCostUtil, getRemainingCostSymbols } from '../../../utils/manaCost'

export interface SelectionSliceState {
  xSelectionState: XSelectionState | null
  convokeSelectionState: ConvokeSelectionState | null
  crewSelectionState: CrewSelectionState | null
  delveSelectionState: DelveSelectionState | null
  manaColorSelectionState: ManaColorSelectionState | null
  decisionSelectionState: DecisionSelectionState | null
  manaSelectionState: ManaSelectionState | null
}

export interface SelectionSliceActions {
  startXSelection: (state: XSelectionState) => void
  updateXValue: (x: number) => void
  cancelXSelection: () => void
  confirmXSelection: () => void
  startConvokeSelection: (state: ConvokeSelectionState) => void
  toggleConvokeCreature: (entityId: EntityId, name: string, payingColor: string | null) => void
  cancelConvokeSelection: () => void
  confirmConvokeSelection: () => void
  startCrewSelection: (state: CrewSelectionState) => void
  toggleCrewCreature: (entityId: EntityId) => void
  cancelCrewSelection: () => void
  confirmCrewSelection: () => void
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
  cancelManaSelection: () => void
  confirmManaSelection: () => void
}

export type SelectionSlice = SelectionSliceState & SelectionSliceActions

export const createSelectionSlice: SliceCreator<SelectionSlice> = (set, get) => ({
  xSelectionState: null,
  convokeSelectionState: null,
  crewSelectionState: null,
  delveSelectionState: null,
  manaColorSelectionState: null,
  decisionSelectionState: null,
  manaSelectionState: null,

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
    const { xSelectionState, pipelineState, startTargeting, playerId, gameState } = get()
    if (!xSelectionState) return

    // Pipeline path
    if (pipelineState) {
      set({ xSelectionState: null })
      get().advancePipeline({
        type: 'xSelection',
        xValue: xSelectionState.selectedX,
        ...(xSelectionState.isRepeatCount ? { isRepeatCount: true } : {}),
      })
      return
    }

    // Legacy path
    if (!playerId || !gameState) return
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
    } else if (actionInfo.action.type === 'ActivateAbility') {
      const baseAction = actionInfo.action

      if (xSelectionState.isRepeatCount) {
        // Repeat count mode: action already has auto-selected targets, just set repeatCount
        const actionWithRepeat = {
          ...baseAction,
          repeatCount: selectedX,
        }
        getWebSocket()?.send(createSubmitActionMessage(actionWithRepeat))
      } else {
        const actionWithX = {
          ...baseAction,
          xValue: selectedX,
        }

        if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
          startTargeting({
            action: actionWithX,
            validTargets: [...actionInfo.validTargets],
            selectedTargets: [],
            minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
            maxTargets: actionInfo.targetCount ?? 1,
          })
        } else {
          getWebSocket()?.send(createSubmitActionMessage(actionWithX))
        }
      }
    } else if (actionInfo.action.type === 'TurnFaceUp') {
      const baseAction = actionInfo.action
      const actionWithX = {
        ...baseAction,
        xValue: selectedX,
      }
      getWebSocket()?.send(createSubmitActionMessage(actionWithX))
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
    const { pipelineState, cancelPipeline } = get()
    if (pipelineState) { cancelPipeline(); return }
    set({ convokeSelectionState: null })
  },

  confirmConvokeSelection: () => {
    const { convokeSelectionState, pipelineState, startTargeting, playerId } = get()
    if (!convokeSelectionState) return

    const { selectedCreatures } = convokeSelectionState
    const convokedCreatures: Record<string, { color: string | null }> = {}
    for (const creature of selectedCreatures) {
      convokedCreatures[creature.entityId] = { color: creature.payingColor }
    }

    // Pipeline path
    if (pipelineState) {
      set({ convokeSelectionState: null })
      get().advancePipeline({ type: 'convoke', convokedCreatures })
      return
    }

    // Legacy path
    if (!playerId) return
    const { actionInfo } = convokeSelectionState

    if (actionInfo.action.type === 'CastSpell') {
      const baseAction = actionInfo.action

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

  // Crew selection actions
  startCrewSelection: (crewSelectionState) => {
    set({ crewSelectionState })
  },

  toggleCrewCreature: (entityId) => {
    set((state) => {
      if (!state.crewSelectionState) return state
      const { selectedCreatures } = state.crewSelectionState
      const exists = selectedCreatures.includes(entityId)

      const newSelectedCreatures = exists
        ? selectedCreatures.filter((id) => id !== entityId)
        : [...selectedCreatures, entityId]

      return {
        crewSelectionState: {
          ...state.crewSelectionState,
          selectedCreatures: newSelectedCreatures,
        },
      }
    })
  },

  cancelCrewSelection: () => {
    set({ crewSelectionState: null })
  },

  confirmCrewSelection: () => {
    const { crewSelectionState, playerId } = get()
    if (!crewSelectionState || !playerId) return

    const { actionInfo, selectedCreatures } = crewSelectionState

    if (actionInfo.action.type === 'CrewVehicle') {
      const actionWithCrew = {
        ...actionInfo.action,
        crewCreatures: selectedCreatures,
      }
      getWebSocket()?.send(createSubmitActionMessage(actionWithCrew))
    }

    set({ crewSelectionState: null })
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
    const { delveSelectionState, pipelineState, startTargeting, startManaSelection } = get()
    if (!delveSelectionState) return

    const { actionInfo, selectedCards } = delveSelectionState

    // Pipeline path
    if (pipelineState) {
      const originalSymbols = parseManaCostUtil(delveSelectionState.manaCost)
      const remainingSymbols = getRemainingCostSymbols(originalSymbols, selectedCards.length)
      const modifiedManaCost = remainingSymbols.map(s => `{${s}}`).join('')
      set({ delveSelectionState: null })
      get().advancePipeline({
        type: 'delve',
        delvedCards: [...selectedCards],
        modifiedManaCost,
      })
      return
    }

    // Legacy path
    if (actionInfo.action.type === 'CastSpell') {
      const baseAction = actionInfo.action

      const actionWithDelve = {
        ...baseAction,
        alternativePayment: {
          delvedCards: [...selectedCards],
          convokedCreatures: {},
        },
      }

      // Calculate remaining cost after delve for display
      const originalSymbols = parseManaCostUtil(delveSelectionState.manaCost)
      const remainingSymbols = getRemainingCostSymbols(originalSymbols, selectedCards.length)
      const remainingCostString = remainingSymbols.map(s => `{${s}}`).join('')

      // Omit delve fields so executeAction won't re-open the Delve selector
      const { hasDelve: _, validDelveCards: _2, minDelveNeeded: _3, ...restActionInfo } = actionInfo
      const modifiedActionInfo: LegalActionInfo = {
        ...restActionInfo,
        action: actionWithDelve,
        manaCostString: remainingCostString,
      }

      if (actionInfo.availableManaSources && actionInfo.availableManaSources.length > 0) {
        // Mana selection first, then targeting (if needed) is handled after mana confirm
        startManaSelection(modifiedActionInfo)
      } else if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
        startTargeting({
          action: actionWithDelve,
          validTargets: [...actionInfo.validTargets],
          selectedTargets: [],
          minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
          maxTargets: actionInfo.targetCount ?? 1,
          ...(actionInfo.requiresDamageDistribution ? { pendingActionInfo: actionInfo } : {}),
        })
      } else {
        getWebSocket()?.send(createSubmitActionMessage(actionWithDelve))
      }
    }

    set({ delveSelectionState: null })
  },

  // Mana color selection actions
  startManaColorSelection: (manaColorSelectionState) => {
    set({ manaColorSelectionState })
  },

  confirmManaColorSelection: (color) => {
    const { manaColorSelectionState, pipelineState, submitAction } = get()
    if (!manaColorSelectionState) return

    // Pipeline path
    if (pipelineState) {
      set({ manaColorSelectionState: null })
      get().advancePipeline({ type: 'manaColorChoice', color })
      return
    }

    // Legacy path
    const action = manaColorSelectionState.action
    if (action.type === 'ActivateAbility') {
      submitAction({ ...action, manaColorChoice: color })
    } else {
      submitAction(action)
    }
    set({ manaColorSelectionState: null })
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

  // Mana source selection actions (pre-cast)
  startManaSelection: (actionInfo) => {
    const sources = actionInfo.availableManaSources
    if (!sources || sources.length === 0) return
    const sourceColors: Record<string, readonly string[]> = {}
    const sourceManaAmounts: Record<string, number> = {}
    for (const source of sources) {
      const colors: string[] = [...(source.producesColors ?? [])]
      if (source.producesColorless && colors.length === 0) colors.push('C')
      sourceColors[source.entityId] = colors
      sourceManaAmounts[source.entityId] = source.manaAmount ?? 1
    }
    // Pre-select the autoTapPreview sources as the default
    const preSelected = actionInfo.autoTapPreview ?? []
    set({
      selectedCardId: null,
      manaSelectionState: {
        action: actionInfo.action,
        actionInfo,
        validSources: sources.map(s => s.entityId),
        selectedSources: [...preSelected],
        manaCost: actionInfo.manaCostString ?? '',
        xValue: 0,
        sourceColors,
        sourceManaAmounts,
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
