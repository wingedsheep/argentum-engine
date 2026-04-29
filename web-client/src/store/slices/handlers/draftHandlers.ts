/**
 * Handlers for draft, sealed, and deck building messages.
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import { trackEvent } from '@/utils/analytics.ts'
import { loadDeckState } from '../shared'
import type { SetState, GetState } from './types'

type DraftHandlerKeys =
  | 'onSealedGameCreated' | 'onSealedPoolGenerated' | 'onOpponentDeckSubmitted'
  | 'onWaitingForOpponent' | 'onDeckSubmitted'
  | 'onDraftPackReceived' | 'onDraftPickMade' | 'onDraftPickConfirmed'
  | 'onDraftComplete' | 'onDraftTimerUpdate'
  | 'onWinstonDraftState' | 'onGridDraftState'

export function createDraftHandlers(set: SetState, _get: GetState): Pick<MessageHandlers, DraftHandlerKeys> {
  return {
    onSealedGameCreated: (msg) => {
      set({
        sessionId: msg.sessionId,
        deckBuildingState: {
          phase: 'waiting',
          setCodes: msg.setCodes,
          setNames: msg.setNames,
          cardPool: [],
          basicLands: [],
          deck: [],
          landCounts: {
            Plains: 0,
            Island: 0,
            Swamp: 0,
            Mountain: 0,
            Forest: 0,
          },
          opponentReady: false,
          llmHighlightedCards: null,
        },
      })
    },

    onSealedPoolGenerated: (msg) => {
      trackEvent('sealed_pool_opened', {
        set_codes: msg.setCodes.join(','),
        set_names: msg.setNames.join(','),
        pool_size: msg.cardPool.length,
      })
      const savedState = loadDeckState()

      set((state) => {
        // Preserve 'submitted' phase if already set, or if tournament state exists (deck must have been submitted)
        const existingPhase = state.deckBuildingState?.phase
        const phase = existingPhase === 'submitted' || state.tournamentState ? 'submitted' : 'building'

        return {
          deckBuildingState: {
            phase,
            setCodes: msg.setCodes,
            setNames: msg.setNames,
            cardPool: msg.cardPool,
            basicLands: msg.basicLands,
            deck: state.deckBuildingState?.deck ?? savedState?.deck ?? [],
            landCounts: state.deckBuildingState?.landCounts ?? savedState?.landCounts ?? {
              Plains: 0,
              Island: 0,
              Swamp: 0,
              Mountain: 0,
              Forest: 0,
            },
            opponentReady: state.deckBuildingState?.opponentReady ?? false,
            llmHighlightedCards: state.deckBuildingState?.llmHighlightedCards ?? null,
          },
        }
      })
    },

    onOpponentDeckSubmitted: () => {
      set((state) => ({
        deckBuildingState: state.deckBuildingState
          ? { ...state.deckBuildingState, opponentReady: true }
          : null,
      }))
    },

    onWaitingForOpponent: () => {
      // Already in submitted state
    },

    onDeckSubmitted: () => {
      set((state) => ({
        deckBuildingState: state.deckBuildingState
          ? { ...state.deckBuildingState, phase: 'submitted' }
          : null,
      }))
    },

    onDraftPackReceived: (msg) => {
      set((state) => ({
        lobbyState: state.lobbyState
          ? {
              ...state.lobbyState,
              draftState: {
                currentPack: msg.cards,
                packNumber: msg.packNumber,
                pickNumber: msg.pickNumber,
                pickedCards: msg.pickedCards ?? state.lobbyState.draftState?.pickedCards ?? [],
                timeRemaining: msg.timeRemainingSeconds,
                passDirection: msg.passDirection,
                picksPerRound: msg.picksPerRound,
                queuedPacks: msg.queuedPacks ?? 0,
                playerPackCounts: state.lobbyState.draftState?.playerPackCounts ?? {},
              },
            }
          : null,
      }))
    },

    onDraftPickMade: (msg) => {
      set((state) => ({
        lobbyState: state.lobbyState?.draftState
          ? {
              ...state.lobbyState,
              draftState: { ...state.lobbyState.draftState, playerPackCounts: msg.playerPackCounts },
            }
          : state.lobbyState,
      }))
    },

    onDraftPickConfirmed: (msg) => {
      // Add picked cards immediately for instant feedback.
      // The server's authoritative list in DraftPackReceived will replace this.
      set((state) => {
        if (!state.lobbyState?.draftState) return state

        const pickedCards = state.lobbyState.draftState.currentPack.filter((c) =>
          msg.cardNames.includes(c.name)
        )

        return {
          lobbyState: {
            ...state.lobbyState,
            draftState: {
              ...state.lobbyState.draftState,
              currentPack: [],
              pickedCards: [...state.lobbyState.draftState.pickedCards, ...pickedCards],
            },
          },
        }
      })
    },

    onDraftComplete: (msg) => {
      trackEvent('draft_complete', { picked_cards: msg.pickedCards.length })

      set((state) => ({
        lobbyState: state.lobbyState
          ? { ...state.lobbyState, state: 'DECK_BUILDING', draftState: null, winstonDraftState: null, gridDraftState: null }
          : null,
        deckBuildingState: {
          phase: 'building',
          setCodes: state.lobbyState?.settings.setCodes ?? [],
          setNames: state.lobbyState?.settings.setNames ?? [],
          cardPool: msg.pickedCards,
          basicLands: msg.basicLands,
          deck: [],
          landCounts: { Plains: 0, Island: 0, Swamp: 0, Mountain: 0, Forest: 0 },
          opponentReady: false,
          llmHighlightedCards: null,
        },
      }))
    },

    onDraftTimerUpdate: (msg) => {
      set((state) => {
        if (!state.lobbyState) return {}
        const lobby = state.lobbyState
        return {
          lobbyState: {
            ...lobby,
            draftState: lobby.draftState
              ? { ...lobby.draftState, timeRemaining: msg.secondsRemaining }
              : lobby.draftState,
            winstonDraftState: lobby.winstonDraftState
              ? { ...lobby.winstonDraftState, timeRemaining: msg.secondsRemaining }
              : lobby.winstonDraftState,
            gridDraftState: lobby.gridDraftState
              ? { ...lobby.gridDraftState, timeRemaining: msg.secondsRemaining }
              : lobby.gridDraftState,
          },
        }
      })
    },

    onWinstonDraftState: (msg) => {
      set((state) => ({
        lobbyState: state.lobbyState
          ? {
              ...state.lobbyState,
              winstonDraftState: {
                isYourTurn: msg.isYourTurn,
                activePlayerName: msg.activePlayerName,
                currentPileIndex: msg.currentPileIndex,
                pileSizes: msg.pileSizes,
                mainDeckRemaining: msg.mainDeckRemaining,
                currentPileCards: msg.currentPileCards,
                pickedCards: msg.pickedCards,
                totalPickedByOpponent: msg.totalPickedByOpponent,
                knownOpponentCards: msg.knownOpponentCards ?? [],
                unknownOpponentCardCount: msg.unknownOpponentCardCount ?? 0,
                lastAction: msg.lastAction,
                timeRemaining: msg.timeRemainingSeconds,
                lastPickedCards: msg.lastPickedCards ?? [],
              },
            }
          : null,
      }))
    },

    onGridDraftState: (msg) => {
      set((state) => ({
        lobbyState: state.lobbyState
          ? {
              ...state.lobbyState,
              gridDraftState: {
                isYourTurn: msg.isYourTurn,
                activePlayerName: msg.activePlayerName,
                grid: msg.grid,
                mainDeckRemaining: msg.mainDeckRemaining,
                pickedCards: msg.pickedCards,
                totalPickedByOthers: msg.totalPickedByOthers,
                pickedCardsByOthers: msg.pickedCardsByOthers ?? {},
                lastPickedCards: msg.lastPickedCards ?? [],
                lastAction: msg.lastAction,
                timeRemaining: msg.timeRemainingSeconds,
                availableSelections: msg.availableSelections,
                playerOrder: msg.playerOrder,
                currentPickerIndex: msg.currentPickerIndex,
                gridNumber: msg.gridNumber,
              },
            }
          : null,
      }))
    },
  }
}
