/**
 * Handlers for lobby and tournament messages.
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import type { LobbyState } from '../types'
import { saveLobbyId, clearLobbyId, clearDeckState } from '../shared'
import { clearPendingLobby, takePendingLobbyApply } from '../pendingLobbyIntent'
import type { SetState, GetState } from './types'

type LobbyHandlerKeys =
  | 'onLobbyCreated' | 'onLobbyUpdate' | 'onLobbyStopped'
  | 'onTournamentStarted' | 'onTournamentMatchStarting' | 'onTournamentBye'
  | 'onRoundComplete' | 'onMatchComplete'
  | 'onPlayerReadyForRound' | 'onTournamentComplete' | 'onTournamentResumed'
  | 'onFreeForAllGameStarting' | 'onFreeForAllGameComplete'

export function createLobbyHandlers(set: SetState, get: GetState): Pick<MessageHandlers, LobbyHandlerKeys> {
  return {
    onLobbyCreated: (msg) => {
      saveLobbyId(msg.lobbyId)
      set({
        lobbyState: {
          lobbyId: msg.lobbyId,
          state: 'WAITING_FOR_PLAYERS',
          players: [],
          settings: { setCodes: [], setNames: [], availableSets: [], format: 'SEALED', boosterCount: 6, boosterDistribution: {}, maxPlayers: 8, pickTimeSeconds: 45, picksPerRound: 1, gamesPerMatch: 1, isPublic: false, deckSizeMin: 60, allowDuplicates: true, commanderPreset: 'BRAWL', chaosBoosters: false, includedSetProducts: {}, bannedCardNames: [], aiAssistEnabled: false, gameMode: 'TOURNAMENT', attackMode: 'MULTIPLE', randomTeams: true, teamAssignments: {} },
          isHost: true,
          draftState: null,
          winstonDraftState: null,
          gridDraftState: null,
        },
      })
      flushPendingApply(get)
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
      clearPendingLobby()
      set({ lobbyState: null, deckBuildingState: null, ffaState: null })
    },

    onFreeForAllGameStarting: (msg) => {
      // FFA counterpart of onTournamentMatchStarting: enter the game. The seat roster arrives
      // again via GameStarted; the legacy opponentName field gets the other seats' names.
      const opponentName = msg.players.filter((p) => !p.isYou).map((p) => p.name).join(', ')
      // Two-Headed Giant (CR 810): stamp the seat → team map here too, not only in onGameStarted.
      // On a mid-game reconnect (refresh) the pod re-sends *this* message — not GameStarted — and
      // its roster carries teamIndex (the game is running, so TeamComponent is populated). Without
      // this the team-grouped rail / ally board / shared-life headers would be lost on refresh.
      const seatTeams: Record<string, number> = {}
      for (const p of msg.players) {
        if (p.teamIndex != null) seatTeams[p.playerId] = p.teamIndex
      }
      // Shared life (2HG) vs. per-player life (Team vs. Team) — game-level, same on every seat.
      const sharedLife = msg.players.some((p) => p.teamSharedLife)
      set({
        ffaState: {
          lobbyId: msg.lobbyId,
          currentGameSessionId: msg.gameSessionId,
          gameNumber: msg.gameNumber,
          standings: null,
          gamesPlayed: msg.gameNumber - 1,
          readyPlayerIds: [],
        },
        sessionId: msg.gameSessionId,
        opponentName,
        teamByPlayerId: seatTeams,
        teamSharedLife: sharedLife,
      })
    },

    onFreeForAllGameComplete: (msg) => {
      set((state) => {
        // Keep the board visible while the game-over overlay is up (mirrors onTournamentComplete).
        const inActiveGame = state.gameState != null && state.gameOverState == null
        return {
          ffaState: {
            lobbyId: msg.lobbyId,
            currentGameSessionId: null,
            gameNumber: msg.gamesPlayed + 1,
            standings: msg.standings,
            gamesPlayed: msg.gamesPlayed,
            readyPlayerIds: [],
          },
          ...(inActiveGame ? {} : {
            gameState: state.gameOverState ? state.gameState : null,
            mulliganState: null,
            waitingForOpponentMulligan: false,
            legalActions: [],
          }),
        }
      })
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
          currentRoundComplete: false,
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
              // We're playing in this round, so it plainly isn't finished.
              currentRoundComplete: false,
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
              // The round we're sitting out has just opened, so it is not finished. Without this the
              // flag stays true from the previous roundComplete and the header reads one round ahead.
              currentRoundComplete: false,
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
                // This message *is* the round ending — every table in it has finished.
                currentRoundComplete: true,
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
                // Only *our* match ended. The server says whether the round did — an early finisher
                // gets false here and must keep seeing the round in progress, not the next one.
                currentRoundComplete: msg.roundComplete ?? false,
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
        ffaState: state.ffaState
          ? { ...state.ffaState, readyPlayerIds: msg.readyPlayerIds }
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
              // Extra rounds are appended to a bracket that had finished, so the last played round is
              // done — and if the tournament ended without a final roundComplete, the flag could still
              // be false here and claim that round is running.
              currentRoundComplete: true,
            }
          : null,
      }))
    },
  }
}

/**
 * Send the settings a recipe queued for a lobby that didn't exist yet.
 *
 * Fired from `onLobbyCreated` — the create's own acknowledgement, so the server has already set
 * `identity.currentLobbyId` and an `updateLobbySettings` will land on the right lobby. Doing it here
 * rather than firing straight after the create removes a real race: the update handler looks the
 * lobby up by that id, and before the create is processed there isn't one.
 *
 * The order is the server's, not a preference — see `pendingLobbyIntent.ts`:
 *
 * 1. **Cube alone, first.** `handleUpdateLobbySettings` resolves `cubeCards` immediately and
 *    `return`s if a name doesn't resolve, which would discard everything else in the same message.
 * 2. **Everything else as one bag**, because a `format` change resets several of the fields that
 *    follow it and the handler is already ordered to cope with that.
 * 3. **AI seats last.** Switching `gameMode` to a multiplayer table with AI already seated is
 *    rejected outright ("doesn't support AI players yet — remove them first").
 */
function flushPendingApply(get: GetState): void {
  const pending = takePendingLobbyApply()
  if (!pending) return
  const s = get()
  if (pending.cube) s.updateLobbySettings(pending.cube)
  if (pending.settings && Object.keys(pending.settings).length > 0) {
    s.updateLobbySettings(pending.settings)
  }
  for (let i = 0; i < pending.aiSeats; i += 1) s.addAiToLobby()
}
