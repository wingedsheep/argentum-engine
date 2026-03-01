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
  createAddExtraRoundMessage,
  createSpectateGameMessage,
  createStopSpectatingMessage,
  createAddDisconnectTimeMessage,
  createKickPlayerMessage,
} from '../../types'
import { trackEvent } from '../../utils/analytics'
import { getWebSocket, clearLobbyId, clearDeckState, loadLobbyId } from './shared'

export interface LobbySliceState {
  lobbyState: LobbyState | null
  tournamentState: TournamentState | null
  spectatingState: SpectatingState | null
  disconnectedPlayers: Record<string, { playerName: string; secondsRemaining: number; disconnectedAt: number }>
}

export interface LobbySliceActions {
  createTournamentLobby: (setCodes: string[], format?: 'SEALED' | 'DRAFT' | 'WINSTON_DRAFT' | 'GRID_DRAFT', boosterCount?: number, maxPlayers?: number, pickTimeSeconds?: number) => void
  joinLobby: (lobbyId: string) => void
  startLobby: () => void
  leaveLobby: () => void
  stopLobby: () => void
  updateLobbySettings: (settings: { setCodes?: string[]; format?: 'SEALED' | 'DRAFT' | 'WINSTON_DRAFT' | 'GRID_DRAFT'; boosterCount?: number; maxPlayers?: number; gamesPerMatch?: number; pickTimeSeconds?: number; picksPerRound?: number }) => void
  readyForNextRound: () => void
  addExtraRound: () => void
  spectateGame: (gameSessionId: string) => void
  stopSpectating: () => void
  addDisconnectTime: (playerId: string) => void
  kickPlayer: (playerId: string) => void
  setSpectatingState: (state: SpectatingState | null) => void
  leaveTournament: () => void
}

export type LobbySlice = LobbySliceState & LobbySliceActions

export const createLobbySlice: SliceCreator<LobbySlice> = (set, get) => ({
  // Initial state
  lobbyState: null,
  tournamentState: null,
  spectatingState: null,
  disconnectedPlayers: {},

  // Actions
  createTournamentLobby: (setCodes, format = 'SEALED', boosterCount = 6, maxPlayers = 8, pickTimeSeconds = 45) => {
    clearDeckState()
    set({ deckBuildingState: null })
    trackEvent('tournament_lobby_created', { set_codes: setCodes, format, booster_count: boosterCount, max_players: maxPlayers })
    getWebSocket()?.send(createCreateTournamentLobbyMessage(setCodes, format, boosterCount, maxPlayers, pickTimeSeconds))
  },

  joinLobby: (lobbyId) => {
    // Only clear deck state when joining a different lobby (preserve on rejoin/reconnect)
    if (loadLobbyId() !== lobbyId) {
      clearDeckState()
      set({ deckBuildingState: null })
    }
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

  addExtraRound: () => {
    getWebSocket()?.send(createAddExtraRoundMessage())
  },

  spectateGame: (gameSessionId) => {
    getWebSocket()?.send(createSpectateGameMessage(gameSessionId))
  },

  stopSpectating: () => {
    getWebSocket()?.send(createStopSpectatingMessage())
  },

  addDisconnectTime: (playerId) => {
    getWebSocket()?.send(createAddDisconnectTimeMessage(playerId))
  },

  kickPlayer: (playerId) => {
    getWebSocket()?.send(createKickPlayerMessage(playerId))
  },

  setSpectatingState: (state) => {
    set({ spectatingState: state })
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
