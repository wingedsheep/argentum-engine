/**
 * Handlers for lobby and tournament messages.
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import type { LobbyState } from '../types'
import { saveLobbyId, clearLobbyId, clearDeckState } from '../shared'
import type { SetState, GetState } from './types'

type LobbyHandlerKeys =
  | 'onLobbyCreated' | 'onLobbyUpdate' | 'onLobbyStopped'
  | 'onTournamentStarted' | 'onTournamentMatchStarting' | 'onTournamentBye'
  | 'onRoundComplete' | 'onMatchComplete'
  | 'onPlayerReadyForRound' | 'onTournamentComplete' | 'onTournamentResumed'

export function createLobbyHandlers(set: SetState, get: GetState): Pick<MessageHandlers, LobbyHandlerKeys> {
  return {
    onLobbyCreated: (msg) => {
      saveLobbyId(msg.lobbyId)
      set({
        lobbyState: {
          lobbyId: msg.lobbyId,
          state: 'WAITING_FOR_PLAYERS',
          players: [],
          settings: { setCodes: [], setNames: [], availableSets: [], format: 'SEALED', boosterCount: 6, boosterDistribution: {}, maxPlayers: 8, pickTimeSeconds: 45, picksPerRound: 1, gamesPerMatch: 1, isPublic: false },
          isHost: true,
          draftState: null,
          winstonDraftState: null,
          gridDraftState: null,
        },
      })
    },

    onLobbyUpdate: (msg) => {
      const { playerId, lobbyState } = get()
      saveLobbyId(msg.lobbyId)

      const currentPlayer = msg.players.find((p) => p.playerId === playerId)
      const isDeckSubmitted = currentPlayer?.deckSubmitted ?? false

      set((state) => ({
        lobbyState: {
          lobbyId: msg.lobbyId,
          state: msg.state as LobbyState['state'],
          players: msg.players,
          settings: msg.settings,
          isHost: msg.isHost,
          draftState: msg.state === 'DRAFTING' && msg.settings.format === 'DRAFT' ? (lobbyState?.draftState ?? null) : null,
          winstonDraftState: msg.state === 'DRAFTING' && msg.settings.format === 'WINSTON_DRAFT' ? (lobbyState?.winstonDraftState ?? null) : null,
          gridDraftState: msg.state === 'DRAFTING' && msg.settings.format === 'GRID_DRAFT' ? (lobbyState?.gridDraftState ?? null) : null,
        },
        // Update deck building phase during DECK_BUILDING or TOURNAMENT_ACTIVE
        // This allows returning to deck building after unsubmitting during tournament
        deckBuildingState:
          state.deckBuildingState && (msg.state === 'DECK_BUILDING' || msg.state === 'TOURNAMENT_ACTIVE')
            ? { ...state.deckBuildingState, phase: isDeckSubmitted ? 'submitted' : 'building' }
            : state.deckBuildingState,
      }))
    },

    onLobbyStopped: () => {
      clearDeckState()
      clearLobbyId()
      set({ lobbyState: null, deckBuildingState: null })
    },

    onTournamentStarted: (msg) => {
      // NOTE: Don't clear deckBuildingState - we allow returning to deck building
      // until the first match starts
      set((state) => ({
        tournamentState: {
          lobbyId: msg.lobbyId,
          totalRounds: msg.totalRounds,
          currentRound: 0,
          standings: msg.standings,
          lastRoundResults: null,
          currentMatchGameSessionId: null,
          currentMatchOpponentName: null,
          isBye: false,
          isComplete: false,
          finalStandings: null,
          readyPlayerIds: [],
          nextOpponentName: msg.nextOpponentName ?? null,
          nextRoundHasBye: msg.nextRoundHasBye ?? false,
        },
        // Keep deckBuildingState but ensure phase is 'submitted'
        deckBuildingState: state.deckBuildingState
          ? { ...state.deckBuildingState, phase: 'submitted' }
          : null,
      }))
    },

    onTournamentMatchStarting: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? {
              ...state.tournamentState,
              currentRound: msg.round,
              currentMatchGameSessionId: msg.gameSessionId,
              currentMatchOpponentName: msg.opponentName,
              isBye: false,
              readyPlayerIds: [],
              nextOpponentName: null,
              nextRoundHasBye: false,
              activeMatches: [], // Clear - player is now in a game
            }
          : null,
        sessionId: msg.gameSessionId,
        opponentName: msg.opponentName,
      }))
    },

    onTournamentBye: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? {
              ...state.tournamentState,
              currentRound: msg.round,
              currentMatchGameSessionId: null,
              currentMatchOpponentName: null,
              isBye: true,
            }
          : null,
      }))
    },

    onRoundComplete: (msg) => {
      set((state) => {
        // Don't clear game state if we're in an active game (no gameOverState yet)
        const inActiveGame = state.gameState != null && state.gameOverState == null
        return {
          tournamentState: state.tournamentState
            ? {
                ...state.tournamentState,
                currentRound: msg.round,
                standings: msg.standings,
                lastRoundResults: msg.results,
                ...(inActiveGame ? {} : {
                  currentMatchGameSessionId: null,
                  currentMatchOpponentName: null,
                }),
                isBye: false,
                isComplete: msg.isTournamentComplete ?? false,
                readyPlayerIds: [],
                nextOpponentName: msg.nextOpponentName ?? null,
                nextRoundHasBye: msg.nextRoundHasBye ?? false,
                activeMatches: [], // Clear - round is over, no active matches
              }
            : null,
          // Preserve game state while in an active game or game-over banner is showing
          ...(inActiveGame ? {} : {
            gameState: state.gameOverState ? state.gameState : null,
            mulliganState: null,
            waitingForOpponentMulligan: false,
            legalActions: [],
          }),
        }
      })
    },

    onMatchComplete: (msg) => {
      set((state) => {
        // Don't clear game state if we're in an active game (no gameOverState yet)
        const inActiveGame = state.gameState != null && state.gameOverState == null
        return {
          tournamentState: state.tournamentState
            ? {
                ...state.tournamentState,
                currentRound: msg.round,
                standings: msg.standings,
                lastRoundResults: msg.results.length > 0 ? msg.results : state.tournamentState.lastRoundResults,
                ...(inActiveGame ? {} : {
                  currentMatchGameSessionId: null,
                  currentMatchOpponentName: null,
                }),
                isBye: false,
                isComplete: msg.isTournamentComplete ?? false,
                readyPlayerIds: [],
                nextOpponentName: msg.nextOpponentName ?? null,
                nextRoundHasBye: msg.nextRoundHasBye ?? false,
                activeMatches: [],
              }
            : null,
          // Preserve game state while in an active game or game-over banner is showing
          ...(inActiveGame ? {} : {
            gameState: state.gameOverState ? state.gameState : null,
            mulliganState: null,
            waitingForOpponentMulligan: false,
            legalActions: [],
          }),
        }
      })
    },

    onPlayerReadyForRound: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? { ...state.tournamentState, readyPlayerIds: msg.readyPlayerIds }
          : null,
      }))
    },

    onTournamentComplete: (msg) => {
      set((state) => {
        const inActiveGame = state.gameState != null && state.gameOverState == null
        return {
          tournamentState: state.tournamentState
            ? {
                ...state.tournamentState,
                isComplete: true,
                finalStandings: msg.finalStandings,
                standings: msg.finalStandings,
                ...(inActiveGame ? {} : {
                  currentMatchGameSessionId: null,
                  currentMatchOpponentName: null,
                }),
              }
            : null,
          ...(inActiveGame ? {} : {
            gameState: state.gameOverState ? state.gameState : null,
          }),
        }
      })
    },

    onTournamentResumed: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? {
              ...state.tournamentState,
              isComplete: false,
              finalStandings: null,
              totalRounds: msg.totalRounds,
              standings: msg.standings,
              readyPlayerIds: [],
              nextOpponentName: msg.nextOpponentName ?? null,
              nextRoundHasBye: msg.nextRoundHasBye ?? false,
            }
          : null,
      }))
    },
  }
}
