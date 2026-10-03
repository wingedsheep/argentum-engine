/**
 * Gameplay slice - handles game state, actions, events, and core game mechanics.
 */
import { decisionInteractionEpoch } from '@/network/liveAction'
import type { SliceCreator, EntityId, LogEntry, MulliganState, GameOverState, ErrorState } from './types'
import type { ClientGameState, GameAction, LegalActionInfo, PendingDecision, OpponentDecisionStatus } from '@/types'
import {
  createCreateGameMessage,
  createJoinGameMessage,
  createSubmitActionMessage,
  createKeepHandMessage,
  createMulliganMessage,
  createChooseBottomCardsMessage,
  createConcedeMessage,
  createCancelGameMessage,
  createSetFullControlMessage,
  createSetPriorityModeMessage,
  createSetStopOverridesMessage,
  createSetAbilityYieldMessage,
  createClearAbilityYieldMessage,
  createClearAllYieldsMessage,
  createRequestUndoMessage,

} from '@/types'
import type { Step, PriorityModeValue, YieldKind } from '@/types'
import { trackEvent } from '@/utils/analytics.ts'
import { getWebSocket } from './shared'

/** How long an error toast stays up before auto-dismissing. */
const ERROR_AUTO_DISMISS_MS = 5000
/** Pending auto-dismiss timer for the global error toast (module-scoped: one toast at a time). */
let errorDismissTimer: ReturnType<typeof setTimeout> | null = null

export interface GameplaySliceState {
  gameState: ClientGameState | null
  interactionEpoch: string | null
  legalActions: readonly LegalActionInfo[]
  pendingDecision: PendingDecision | null
  opponentDecisionStatus: OpponentDecisionStatus | null
  mulliganState: MulliganState | null
  waitingForOpponentMulligan: boolean
  eventLog: readonly LogEntry[]
  gameOverState: GameOverState | null
  lastError: ErrorState | null
  fullControl: boolean
  priorityMode: PriorityModeValue
  stopOverrides: { myTurnStops: Step[]; opponentTurnStops: Step[] }
  nextStopPoint: string | null
  opponentName: string | null
  undoAvailable: boolean
  opponentDisconnectCountdown: number | null
  autoTapEnabled: boolean
  /** Number of spectators currently watching this player's game (0 if none). */
  spectatorCount: number
  /** Names of currently-active spectators (for hover display on the badge). */
  spectatorNames: readonly string[]
}

export interface GameplaySliceActions {
  createGame: (deckList: Record<string, number>, setCode?: string) => void
  createAiGame: (deckList: Record<string, number>, setCode?: string) => void
  joinGame: (sessionId: string, deckList: Record<string, number>) => void
  submitAction: (action: GameAction, interactionEpoch: string | null | undefined) => void
  /** The decision ID must come from the rendered prompt, never from a later store snapshot. */
  submitDecision: (decisionId: string, selectedCards: readonly EntityId[]) => void
  submitTargetsDecision: (decisionId: string, selectedTargets: Record<number, readonly EntityId[]>) => void
  submitOrderedDecision: (decisionId: string, orderedObjects: readonly EntityId[]) => void
  submitYesNoDecision: (decisionId: string, choice: boolean) => void
  submitBatchYesNoDecision: (decisionId: string, choice: boolean, applyToAll: boolean) => void
  submitNumberDecision: (decisionId: string, number: number) => void
  submitOptionDecision: (decisionId: string, optionIndex: number) => void
  submitReplacementDecision: (decisionId: string, fromIndex: number, toIndex: number) => void
  submitBudgetModalDecision: (decisionId: string, selectedModeIndices: readonly number[]) => void
  submitDistributeDecision: (decisionId: string, distribution: Record<EntityId, number>) => void
  submitDamageAssignmentDecision: (decisionId: string, assignments: Record<EntityId, number>) => void
  submitCombatResolutionDecision: (decisionId: string, edges: ReadonlyArray<{ edgeId: string; amount: number }>) => void
  submitColorDecision: (decisionId: string, color: string, colors?: readonly string[]) => void
  submitManaSourcesDecision: (
    decisionId: string,
    selectedSources: readonly EntityId[],
    autoPay: boolean,
    waterbendPermanents?: readonly EntityId[],
    declined?: boolean,
  ) => void
  submitCancelDecision: (decisionId: string) => void
  submitSplitPilesDecision: (decisionId: string, piles: readonly (readonly EntityId[])[]) => void
  keepHand: () => void
  mulligan: () => void
  chooseBottomCards: (cardIds: readonly EntityId[]) => void
  toggleMulliganCard: (cardId: EntityId) => void
  concede: () => void
  cancelGame: () => void
  setFullControl: (enabled: boolean) => void
  cyclePriorityMode: () => void
  toggleStopOverride: (step: Step, isMyTurn: boolean) => void
  setAbilityYield: (cardDefinitionId: string, abilityId: string, kind: YieldKind) => void
  clearAbilityYield: (cardDefinitionId: string, abilityId: string) => void
  clearAllYields: () => void
  requestUndo: () => void
  toggleAutoTap: () => void
  returnToMenu: () => void
  /**
   * Surface a global error toast. Auto-dismisses after a few seconds (a fresh error
   * resets the timer). Route every error through this — setting `lastError` directly
   * skips the auto-dismiss, leaving the toast stuck on routes where no component owns a timer.
   */
  setError: (error: ErrorState) => void
  clearError: () => void
}

export type GameplaySlice = GameplaySliceState & GameplaySliceActions

export const createGameplaySlice: SliceCreator<GameplaySlice> = (set, get) => ({
  // Initial state
  gameState: null,
  interactionEpoch: null,
  legalActions: [],
  pendingDecision: null,
  opponentDecisionStatus: null,
  mulliganState: null,
  waitingForOpponentMulligan: false,
  eventLog: [],
  gameOverState: null,
  lastError: null,
  fullControl: false,
  priorityMode: 'auto' as PriorityModeValue,
  stopOverrides: { myTurnStops: [], opponentTurnStops: [] },
  nextStopPoint: null,
  opponentName: null,
  undoAvailable: false,
  opponentDisconnectCountdown: null,
  autoTapEnabled: localStorage.getItem('argentum-auto-tap') !== 'false',
  spectatorCount: 0,
  spectatorNames: [],

  // Actions
  createGame: (deckList, setCode) => {
    getWebSocket()?.send(createCreateGameMessage(deckList, undefined, setCode))
  },

  createAiGame: (deckList, setCode) => {
    getWebSocket()?.send(createCreateGameMessage(deckList, true, setCode))
  },

  joinGame: (sessionId, deckList) => {
    // Track the joined session locally — the joiner only gets GameStarted (no session id),
    // and onGameOver matches its gameId against this.
    set({ sessionId })
    getWebSocket()?.send(createJoinGameMessage(sessionId, deckList))
  },

  submitAction: (action, interactionEpoch) => {
    if (!get().sessionId || !get().gameState) return
    if (!interactionEpoch || interactionEpoch !== get().interactionEpoch) return
    const play = get().pendingDecision
    if (play?.type === 'PlayCardDecision' && (action.type === 'CastSpell' || action.type === 'PlayLand')) {
      action = { type: 'SubmitDecision', playerId: play.playerId,
        response: { type: 'PlayCardResponse', decisionId: play.id, action } }
    }
    if (action.type === 'SubmitDecision' && action.response.decisionId !== get().pendingDecision?.id) return
    getWebSocket()?.send(createSubmitActionMessage(action, interactionEpoch))
    set({ selectedCardId: null, targetingState: null })
  },

  submitDecision: (decisionId, selectedCards) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'CardsSelectedResponse' as const,
        decisionId,
        selectedCards: [...selectedCards],
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitTargetsDecision: (decisionId, selectedTargets) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'TargetsResponse' as const,
        decisionId,
        selectedTargets,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitCancelDecision: (decisionId) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'CancelDecisionResponse' as const,
        decisionId,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitOrderedDecision: (decisionId, orderedObjects) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'OrderedResponse' as const,
        decisionId,
        orderedObjects: [...orderedObjects],
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitYesNoDecision: (decisionId, choice) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'YesNoResponse' as const,
        decisionId,
        choice,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitBatchYesNoDecision: (decisionId, choice, applyToAll) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'BatchYesNoResponse' as const,
        decisionId,
        choice,
        applyToAll,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitNumberDecision: (decisionId, number) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'NumberChosenResponse' as const,
        decisionId,
        number,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitOptionDecision: (decisionId, optionIndex) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'OptionChosenResponse' as const,
        decisionId,
        optionIndex,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitReplacementDecision: (decisionId, fromIndex, toIndex) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'ReplacementChosenResponse' as const,
        decisionId,
        fromIndex,
        toIndex,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitBudgetModalDecision: (decisionId, selectedModeIndices) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'BudgetModalResponse' as const,
        decisionId,
        selectedModeIndices,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitDistributeDecision: (decisionId, distribution) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'DistributionResponse' as const,
        decisionId,
        distribution,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitDamageAssignmentDecision: (decisionId: string, assignments: Record<string, number>) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'DamageAssignmentResponse' as const,
        decisionId,
        assignments,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitColorDecision: (decisionId, color, colors) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'ColorChosenResponse' as const,
        decisionId,
        color,
        // Multi-color decisions ("the color or colors of your choice") send the whole set.
        ...(colors && colors.length > 0 ? { colors } : {}),
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitCombatResolutionDecision: (decisionId: string, edges: ReadonlyArray<{ edgeId: string; amount: number }>) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'CombatResolutionResponse' as const,
        decisionId,
        edges,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitManaSourcesDecision: (
    decisionId: string,
    selectedSources: readonly EntityId[],
    autoPay: boolean,
    waterbendPermanents: readonly EntityId[] = [],
    declined = false,
  ) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'ManaSourcesSelectedResponse' as const,
        decisionId,
        selectedSources: [...selectedSources],
        autoPay,
        // Artifacts/creatures tapped to pay {1} each via Waterbend (Ward—Waterbend).
        waterbendPermanents: [...waterbendPermanents],
        // Explicit refusal. An empty submission alone is ambiguous now that the player can float
        // mana themselves during the payment (CR 605.3a) and then confirm with nothing selected.
        declined,
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  submitSplitPilesDecision: (decisionId: string, piles: readonly (readonly EntityId[])[]) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || pendingDecision.id !== decisionId || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      // Stamp the decision's owner, not the connection's own seat. Equal in normal play;
      // in hotseat / Mindslaver control the one connection answers the other seat's
      // decisions, and the engine requires SubmitDecision.playerId == pendingDecision.playerId.
      playerId: pendingDecision.playerId,
      response: {
        type: 'PilesSplitResponse' as const,
        decisionId,
        piles: piles.map((pile) => [...pile]),
      },
    }
    get().submitAction(action, decisionInteractionEpoch(action.response.decisionId))
  },

  keepHand: () => {
    getWebSocket()?.send(createKeepHandMessage())
  },

  mulligan: () => {
    getWebSocket()?.send(createMulliganMessage())
  },

  chooseBottomCards: (cardIds) => {
    getWebSocket()?.send(createChooseBottomCardsMessage(cardIds))
  },

  toggleMulliganCard: (cardId) => {
    set((state) => {
      if (!state.mulliganState || state.mulliganState.phase !== 'choosingBottomCards') {
        return state
      }

      const isSelected = state.mulliganState.selectedCards.includes(cardId)
      const newSelected = isSelected
        ? state.mulliganState.selectedCards.filter((id) => id !== cardId)
        : [...state.mulliganState.selectedCards, cardId]

      return {
        mulliganState: {
          ...state.mulliganState,
          selectedCards: newSelected,
        },
      }
    })
  },

  concede: () => {
    trackEvent('player_conceded')
    getWebSocket()?.send(createConcedeMessage())
  },

  requestUndo: () => {
    getWebSocket()?.send(createRequestUndoMessage())
  },

  toggleAutoTap: () => {
    const { autoTapEnabled } = get()
    const newValue = !autoTapEnabled
    set({ autoTapEnabled: newValue })
    localStorage.setItem('argentum-auto-tap', String(newValue))
  },

  cancelGame: () => {
    trackEvent('game_cancelled')
    getWebSocket()?.send(createCancelGameMessage())
  },

  setFullControl: (enabled) => {
    getWebSocket()?.send(createSetFullControlMessage(enabled))
    set({ fullControl: enabled })
  },

  cyclePriorityMode: () => {
    const { priorityMode } = get()
    const nextMode: PriorityModeValue =
      priorityMode === 'auto' ? 'stops' :
      priorityMode === 'stops' ? 'fullControl' :
      'auto'
    getWebSocket()?.send(createSetPriorityModeMessage(nextMode))
    set({ priorityMode: nextMode, fullControl: nextMode === 'fullControl' })
  },

  toggleStopOverride: (step, isMyTurn) => {
    const { stopOverrides } = get()
    const key = isMyTurn ? 'myTurnStops' : 'opponentTurnStops'
    const current = stopOverrides[key]
    const newStops = current.includes(step)
      ? current.filter((s) => s !== step)
      : [...current, step]
    const newOverrides = { ...stopOverrides, [key]: newStops }
    set({ stopOverrides: newOverrides })
    getWebSocket()?.send(createSetStopOverridesMessage(newOverrides.myTurnStops, newOverrides.opponentTurnStops))
    // Persist to localStorage
    localStorage.setItem('argentum-stop-overrides', JSON.stringify(newOverrides))
  },

  // Persistent per-ability yields (MTGO right-click yields — backlog §C). The server owns the
  // authoritative yield state (on GameState); these just dispatch intent and the next state update
  // reflects it in gameState.activeYields.
  setAbilityYield: (cardDefinitionId, abilityId, kind) => {
    getWebSocket()?.send(createSetAbilityYieldMessage(cardDefinitionId, abilityId, kind))
  },

  clearAbilityYield: (cardDefinitionId, abilityId) => {
    getWebSocket()?.send(createClearAbilityYieldMessage(cardDefinitionId, abilityId))
  },

  clearAllYields: () => {
    getWebSocket()?.send(createClearAllYieldsMessage())
  },

  returnToMenu: () => {
    const state = get()
    // FFA pods keep their lobby context too — "Return to Menu" from the game-over (or
    // eliminated) overlay drops back to the pod's standings screen, not the main menu.
    const isInTournament = state.tournamentState != null || state.ffaState != null
    set({
      sessionId: null,
      opponentName: null,
      gameState: null,
      interactionEpoch: null,
      legalActions: [],
      pendingDecision: null,
      mulliganState: null,
      waitingForOpponentMulligan: false,
      selectedCardId: null,
      targetingState: null,
      combatState: null,
      xSelectionState: null,
      convokeSelectionState: null,
      tapForGenericSelectionState: null,
      decisionSelectionState: null,
      damageDistributionState: null,
      distributeState: null,
      counterDistributionState: null,
      manaSelectionState: null,
      hoveredCardId: null,
      draggingBlockerId: null,
      draggingCardId: null,
      revealedHandCardIds: null,
      revealedCardsInfo: null,
      fullControl: false,
      priorityMode: 'auto' as PriorityModeValue,
      undoAvailable: false,
      stopOverrides: { myTurnStops: [], opponentTurnStops: [] },
      nextStopPoint: null,
      opponentDisconnectCountdown: null,
      eventLog: [],
      gameOverState: null,
      lastError: null,
      spectatorCount: 0,
      spectatorNames: [],
      // Preserve the drafted deck in tournament mode — the user may still want to
      // view it or save it to My Decks from the standings screen, including after
      // the final round has ended.
      deckBuildingState: isInTournament ? state.deckBuildingState : null,
      lobbyState: isInTournament ? state.lobbyState : null,
      tournamentState: isInTournament ? state.tournamentState : null,
    })
  },

  setError: (error: ErrorState) => {
    if (errorDismissTimer !== null) clearTimeout(errorDismissTimer)
    set({ lastError: error })
    errorDismissTimer = setTimeout(() => {
      errorDismissTimer = null
      set({ lastError: null })
    }, ERROR_AUTO_DISMISS_MS)
  },

  clearError: () => {
    if (errorDismissTimer !== null) {
      clearTimeout(errorDismissTimer)
      errorDismissTimer = null
    }
    set({ lastError: null })
  },
})
