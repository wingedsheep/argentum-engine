/**
 * Lobby slice - handles tournament lobbies, spectating, and lobby management.
 */
import type { SliceCreator, LobbyState, TournamentState, SpectatingState } from './types'
import {
  createCreateTournamentLobbyMessage,
  createJoinLobbyMessage,
  createStartTournamentLobbyMessage,
  createLeaveLobbyMessage,
  createStopLobbyMessage,
  createUpdateLobbySettingsMessage,
  createReadyForNextRoundMessage,
  createSpectateGameMessage,
  createStopSpectatingMessage,
} from '../../types'
import { trackEvent } from '../../utils/analytics'
import { getWebSocket, clearLobbyId, clearDeckState } from './shared'

export interface LobbySliceState {
  lobbyState: LobbyState | null
  tournamentState: TournamentState | null
  spectatingState: SpectatingState | null
}

export interface LobbySliceActions {
  createTournamentLobby: (setCodes: string[], format?: 'SEALED' | 'DRAFT', boosterCount?: number, maxPlayers?: number, pickTimeSeconds?: number) => void
  joinLobby: (lobbyId: string) => void
  startLobby: () => void
  leaveLobby: () => void
  stopLobby: () => void
  updateLobbySettings: (settings: { setCodes?: string[]; format?: 'SEALED' | 'DRAFT'; boosterCount?: number; maxPlayers?: number; gamesPerMatch?: number; pickTimeSeconds?: number; picksPerRound?: number }) => void
  readyForNextRound: () => void
  spectateGame: (gameSessionId: string) => void
  stopSpectating: () => void
  leaveTournament: () => void
}

export type LobbySlice = LobbySliceState & LobbySliceActions

export const createLobbySlice: SliceCreator<LobbySlice> = (set, get) => ({
  // Initial state
  lobbyState: null,
  tournamentState: null,
  spectatingState: null,

  // Actions
  createTournamentLobby: (setCodes, format = 'SEALED', boosterCount = 6, maxPlayers = 8, pickTimeSeconds = 45) => {
    clearDeckState()
    set({ deckBuildingState: null })
    trackEvent('tournament_lobby_created', { set_codes: setCodes, format, booster_count: boosterCount, max_players: maxPlayers })
    getWebSocket()?.send(createCreateTournamentLobbyMessage(setCodes, format, boosterCount, maxPlayers, pickTimeSeconds))
  },

  joinLobby: (lobbyId) => {
    clearDeckState()
    set({ deckBuildingState: null })
    trackEvent('lobby_joined')
    getWebSocket()?.send(createJoinLobbyMessage(lobbyId))
  },

  startLobby: () => {
    const { lobbyState } = get()
    trackEvent('lobby_started', { format: lobbyState?.settings.format })
    getWebSocket()?.send(createStartTournamentLobbyMessage())
  },

  leaveLobby: () => {
    clearDeckState()
    clearLobbyId()
    getWebSocket()?.send(createLeaveLobbyMessage())
    set({ lobbyState: null, deckBuildingState: null })
  },

  stopLobby: () => {
    clearDeckState()
    clearLobbyId()
    getWebSocket()?.send(createStopLobbyMessage())
    set({ lobbyState: null, deckBuildingState: null })
  },

  updateLobbySettings: (settings) => {
    getWebSocket()?.send(createUpdateLobbySettingsMessage(settings))
  },

  readyForNextRound: () => {
    getWebSocket()?.send(createReadyForNextRoundMessage())
  },

  spectateGame: (gameSessionId) => {
    getWebSocket()?.send(createSpectateGameMessage(gameSessionId))
  },

  stopSpectating: () => {
    getWebSocket()?.send(createStopSpectatingMessage())
  },

  leaveTournament: () => {
    getWebSocket()?.send(createLeaveLobbyMessage())
    clearLobbyId()
    clearDeckState()
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
      hoveredCardId: null,
      draggingBlockerId: null,
      draggingCardId: null,
      revealedHandCardIds: null,
      revealedCardsInfo: null,
      fullControl: false,
      nextStopPoint: null,
      pendingEvents: [],
      eventLog: [],
      gameOverState: null,
      lastError: null,
      deckBuildingState: null,
      lobbyState: null,
      tournamentState: null,
      spectatingState: null,
    })
  },
})
