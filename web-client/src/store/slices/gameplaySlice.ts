/**
 * Gameplay slice - handles game state, actions, events, and core game mechanics.
 */
import type { SliceCreator, EntityId, LogEntry, MulliganState, GameOverState, ErrorState } from './types'
import type { ClientGameState, ClientEvent, GameAction, LegalActionInfo, PendingDecision, OpponentDecisionStatus } from '../../types'
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
} from '../../types'
import { trackEvent } from '../../utils/analytics'
import { getWebSocket } from './shared'

export interface GameplaySliceState {
  gameState: ClientGameState | null
  legalActions: readonly LegalActionInfo[]
  pendingDecision: PendingDecision | null
  opponentDecisionStatus: OpponentDecisionStatus | null
  mulliganState: MulliganState | null
  waitingForOpponentMulligan: boolean
  pendingEvents: readonly ClientEvent[]
  eventLog: readonly LogEntry[]
  gameOverState: GameOverState | null
  lastError: ErrorState | null
  fullControl: boolean
  nextStopPoint: string | null
  opponentName: string | null
  opponentDisconnectCountdown: number | null
}

export interface GameplaySliceActions {
  createGame: (deckList: Record<string, number>) => void
  joinGame: (sessionId: string, deckList: Record<string, number>) => void
  submitAction: (action: GameAction) => void
  submitDecision: (selectedCards: readonly EntityId[]) => void
  submitTargetsDecision: (selectedTargets: Record<number, readonly EntityId[]>) => void
  submitOrderedDecision: (orderedObjects: readonly EntityId[]) => void
  submitYesNoDecision: (choice: boolean) => void
  submitNumberDecision: (number: number) => void
  submitOptionDecision: (optionIndex: number) => void
  submitDistributeDecision: (distribution: Record<EntityId, number>) => void
  submitColorDecision: (color: string) => void
  keepHand: () => void
  mulligan: () => void
  chooseBottomCards: (cardIds: readonly EntityId[]) => void
  toggleMulliganCard: (cardId: EntityId) => void
  concede: () => void
  cancelGame: () => void
  setFullControl: (enabled: boolean) => void
  returnToMenu: () => void
  clearError: () => void
  consumeEvent: () => ClientEvent | undefined
}

export type GameplaySlice = GameplaySliceState & GameplaySliceActions

export const createGameplaySlice: SliceCreator<GameplaySlice> = (set, get) => ({
  // Initial state
  gameState: null,
  legalActions: [],
  pendingDecision: null,
  opponentDecisionStatus: null,
  mulliganState: null,
  waitingForOpponentMulligan: false,
  pendingEvents: [],
  eventLog: [],
  gameOverState: null,
  lastError: null,
  fullControl: false,
  nextStopPoint: null,
  opponentName: null,
  opponentDisconnectCountdown: null,

  // Actions
  createGame: (deckList) => {
    getWebSocket()?.send(createCreateGameMessage(deckList))
  },

  joinGame: (sessionId, deckList) => {
    getWebSocket()?.send(createJoinGameMessage(sessionId, deckList))
  },

  submitAction: (action) => {
    getWebSocket()?.send(createSubmitActionMessage(action))
    set({ selectedCardId: null, targetingState: null })
  },

  submitDecision: (selectedCards) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'CardsSelectedResponse' as const,
        decisionId: pendingDecision.id,
        selectedCards: [...selectedCards],
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))
  },

  submitTargetsDecision: (selectedTargets) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'TargetsResponse' as const,
        decisionId: pendingDecision.id,
        selectedTargets,
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))
  },

  submitOrderedDecision: (orderedObjects) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'OrderedResponse' as const,
        decisionId: pendingDecision.id,
        orderedObjects: [...orderedObjects],
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))
  },

  submitYesNoDecision: (choice) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'YesNoResponse' as const,
        decisionId: pendingDecision.id,
        choice,
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))
  },

  submitNumberDecision: (number) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'NumberChosenResponse' as const,
        decisionId: pendingDecision.id,
        number,
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))
  },

  submitOptionDecision: (optionIndex) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'OptionChosenResponse' as const,
        decisionId: pendingDecision.id,
        optionIndex,
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))
  },

  submitDistributeDecision: (distribution) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'DistributionResponse' as const,
        decisionId: pendingDecision.id,
        distribution,
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))
  },

  submitColorDecision: (color) => {
    const { pendingDecision, playerId } = get()
    if (!pendingDecision || !playerId) return

    const action = {
      type: 'SubmitDecision' as const,
      playerId,
      response: {
        type: 'ColorChosenResponse' as const,
        decisionId: pendingDecision.id,
        color,
      },
    }
    getWebSocket()?.send(createSubmitActionMessage(action))
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

  cancelGame: () => {
    trackEvent('game_cancelled')
    getWebSocket()?.send(createCancelGameMessage())
  },

  setFullControl: (enabled) => {
    getWebSocket()?.send(createSetFullControlMessage(enabled))
    set({ fullControl: enabled })
  },

  returnToMenu: () => {
    const state = get()
    const isInTournament = state.tournamentState != null
    set({
      sessionId: null,
      opponentName: null,
      gameState: null,
      legalActions: [],
      pendingDecision: null,
      mulliganState: null,
      waitingForOpponentMulligan: false,
      selectedCardId: null,
      targetingState: null,
      combatState: null,
      xSelectionState: null,
      convokeSelectionState: null,
      decisionSelectionState: null,
      damageDistributionState: null,
      distributeState: null,
      hoveredCardId: null,
      draggingBlockerId: null,
      draggingCardId: null,
      revealedHandCardIds: null,
      revealedCardsInfo: null,
      fullControl: false,
      nextStopPoint: null,
      opponentDisconnectCountdown: null,
      pendingEvents: [],
      eventLog: [],
      gameOverState: null,
      lastError: null,
      deckBuildingState: null,
      lobbyState: isInTournament ? state.lobbyState : null,
      tournamentState: isInTournament ? state.tournamentState : null,
    })
  },

  clearError: () => {
    set({ lastError: null })
  },

  consumeEvent: () => {
    const { pendingEvents } = get()
    if (pendingEvents.length === 0) return undefined

    const [event, ...rest] = pendingEvents
    set({ pendingEvents: rest })
    return event
  },
})
