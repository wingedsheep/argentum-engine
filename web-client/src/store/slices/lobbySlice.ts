/**
 * Lobby slice - handles tournament lobbies, spectating, and lobby management.
 */
import type { SliceCreator, LobbyState, TournamentState, FfaState, SpectatingState } from './types'
import type { DeckFormat, TournamentFormat, LobbyGameMode, AttackMode } from '@/types'
import {
  createCreateTournamentLobbyMessage,
  createJoinLobbyMessage,
  createStartTournamentLobbyMessage,
  createLeaveLobbyMessage,
  createAddAiToLobbyMessage,
  createRemoveAiFromLobbyMessage,
  createStopLobbyMessage,
  createUpdateLobbySettingsMessage,
  createReadyForNextRoundMessage,
  createAddExtraRoundMessage,
  createSubmitSealedDeckMessage,
  createUnsubmitDeckMessage,
  createSpectateGameMessage,
  createStopSpectatingMessage,
  createAddDisconnectTimeMessage,
  createKickPlayerMessage,
} from '@/types'
import { trackEvent } from '@/utils/analytics.ts'
import { getWebSocket, clearLobbyId, clearDeckState, loadLobbyId } from './shared'

export interface LobbySliceState {
  lobbyState: LobbyState | null
  tournamentState: TournamentState | null
  ffaState: FfaState | null
  spectatingState: SpectatingState | null
  disconnectedPlayers: Record<string, { playerName: string; secondsRemaining: number; disconnectedAt: number }>
}

export interface LobbySliceActions {
  createTournamentLobby: (setCodes: string[], format?: TournamentFormat, boosterCount?: number, maxPlayers?: number, pickTimeSeconds?: number, isPublic?: boolean, gameMode?: LobbyGameMode) => void
  joinLobby: (lobbyId: string) => void
  startLobby: () => void
  leaveLobby: () => void
  stopLobby: () => void
  updateLobbySettings: (settings: { setCodes?: string[]; format?: TournamentFormat; boosterCount?: number; boosterDistribution?: Record<string, number>; maxPlayers?: number; gamesPerMatch?: number; pickTimeSeconds?: number; picksPerRound?: number; isPublic?: boolean; deckFormat?: DeckFormat | '' | null; chaosBoosters?: boolean; bannedCardNames?: string[]; aiAssistEnabled?: boolean; gameMode?: LobbyGameMode; attackMode?: AttackMode; randomTeams?: boolean; teamAssignments?: Record<string, number> }) => void
  addAiToLobby: () => void
  removeAiFromLobby: (playerId: string) => void
  readyForNextRound: () => void
  addExtraRound: () => void
  spectateGame: (gameSessionId: string) => void
  stopSpectating: () => void
  addDisconnectTime: (playerId: string) => void
  kickPlayer: (playerId: string) => void
  setSpectatingState: (state: SpectatingState | null) => void
  leaveTournament: () => void
  /**
   * Submit a deck directly from the lobby (Premade Decks tournament format).
   * For Sealed/Draft, players use the dedicated deckbuilder instead via `submitSealedDeck`.
   */
  submitLobbyDeck: (deckList: Record<string, number>, commander?: string | null) => void
  /** Unsubmit (and re-edit) a previously submitted lobby deck. */
  unsubmitLobbyDeck: () => void
}

export type LobbySlice = LobbySliceState & LobbySliceActions

export const createLobbySlice: SliceCreator<LobbySlice> = (set, get) => ({
  // Initial state
  lobbyState: null,
  tournamentState: null,
  ffaState: null,
  spectatingState: null,
  disconnectedPlayers: {},

  // Actions
  createTournamentLobby: (setCodes, format = 'SEALED', boosterCount = 6, maxPlayers = 8, pickTimeSeconds = 45, isPublic = false, gameMode = 'TOURNAMENT') => {
    clearDeckState()
    set({ deckBuildingState: null })
    trackEvent('tournament_lobby_created', { set_codes: setCodes, format, booster_count: boosterCount, max_players: maxPlayers, is_public: isPublic, game_mode: gameMode })
    getWebSocket()?.send(createCreateTournamentLobbyMessage(setCodes, format, boosterCount, maxPlayers, pickTimeSeconds, isPublic, gameMode))
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
    set({ lobbyState: null, deckBuildingState: null, ffaState: null })
  },

  addAiToLobby: () => {
    getWebSocket()?.send(createAddAiToLobbyMessage())
  },

  removeAiFromLobby: (playerId) => {
    getWebSocket()?.send(createRemoveAiFromLobbyMessage(playerId))
  },

  stopLobby: () => {
    clearDeckState()
    clearLobbyId()
    getWebSocket()?.send(createStopLobbyMessage())
    set({ lobbyState: null, deckBuildingState: null, ffaState: null })
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

  submitLobbyDeck: (deckList, commander) => {
    trackEvent('premade_deck_submitted', {
      deck_size: Object.values(deckList).reduce((a, b) => a + b, 0),
      has_commander: !!commander,
    })
    getWebSocket()?.send(createSubmitSealedDeckMessage(deckList, commander))
  },

  unsubmitLobbyDeck: () => {
    getWebSocket()?.send(createUnsubmitDeckMessage())
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
      waterbendSelectionState: null,
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
      ffaState: null,
      spectatingState: null,
    })
  },
})
