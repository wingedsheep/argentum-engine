import type {
  ServerMessage,
  ConnectedMessage,
  ReconnectedMessage,
  GameCreatedMessage,
  GameStartedMessage,
  GameCancelledMessage,
  StateUpdateMessage,
  StateDeltaUpdateMessage,
  MulliganDecisionMessage,
  ChooseBottomCardsMessage,
  MulliganCompleteMessage,
  GameOverMessage,
  ErrorMessage,
  SealedGameCreatedMessage,
  SealedPoolGeneratedMessage,
  OpponentDeckSubmittedMessage,
  WaitingForOpponentMessage,
  DeckSubmittedMessage,
  LobbyCreatedMessage,
  LobbyUpdateMessage,
  LobbyStoppedMessage,
  DraftPackReceivedMessage,
  DraftPickMadeMessage,
  DraftPickConfirmedMessage,
  DraftCompleteMessage,
  DraftTimerUpdateMessage,
  WinstonDraftStateMessage,
  GridDraftStateMessage,
  TournamentStartedMessage,
  TournamentMatchStartingMessage,
  TournamentByeMessage,
  RoundCompleteMessage,
  MatchCompleteMessage,
  PlayerReadyForRoundMessage,
  TournamentCompleteMessage,
  TournamentResumedMessage,
  ActiveMatchesMessage,
  SpectatorStateUpdateMessage,
  SpectatingStartedMessage,
  SpectatingStoppedMessage,
  OpponentAttackerTargetsMessage,
  OpponentBlockerAssignmentsMessage,
  OpponentDisconnectedMessage,
  OpponentReconnectedMessage,
  TournamentPlayerDisconnectedMessage,
  TournamentPlayerReconnectedMessage,
  QuickGameLobbyStateMessage,
  QuickGameLobbyClosedMessage,
} from '@/types'

/**
 * Handler functions for each server message type.
 */
export interface MessageHandlers {
  onConnected: (message: ConnectedMessage) => void
  onReconnected: (message: ReconnectedMessage) => void
  onGameCreated: (message: GameCreatedMessage) => void
  onGameStarted: (message: GameStartedMessage) => void
  onGameCancelled: (message: GameCancelledMessage) => void
  onStateUpdate: (message: StateUpdateMessage) => void
  onStateDeltaUpdate: (message: StateDeltaUpdateMessage) => void
  onMulliganDecision: (message: MulliganDecisionMessage) => void
  onChooseBottomCards: (message: ChooseBottomCardsMessage) => void
  onMulliganComplete: (message: MulliganCompleteMessage) => void
  onWaitingForOpponentMulligan: () => void
  onGameOver: (message: GameOverMessage) => void
  onError: (message: ErrorMessage) => void
  // Sealed draft handlers
  onSealedGameCreated: (message: SealedGameCreatedMessage) => void
  onSealedPoolGenerated: (message: SealedPoolGeneratedMessage) => void
  onOpponentDeckSubmitted: (message: OpponentDeckSubmittedMessage) => void
  onWaitingForOpponent: (message: WaitingForOpponentMessage) => void
  onDeckSubmitted: (message: DeckSubmittedMessage) => void
  // Lobby handlers
  onLobbyCreated: (message: LobbyCreatedMessage) => void
  onLobbyUpdate: (message: LobbyUpdateMessage) => void
  onLobbyStopped: (message: LobbyStoppedMessage) => void
  // Draft handlers
  onDraftPackReceived: (message: DraftPackReceivedMessage) => void
  onDraftPickMade: (message: DraftPickMadeMessage) => void
  onDraftPickConfirmed: (message: DraftPickConfirmedMessage) => void
  onDraftComplete: (message: DraftCompleteMessage) => void
  onDraftTimerUpdate: (message: DraftTimerUpdateMessage) => void
  // Winston Draft handlers
  onWinstonDraftState: (message: WinstonDraftStateMessage) => void
  // Grid Draft handlers
  onGridDraftState: (message: GridDraftStateMessage) => void
  // Tournament handlers
  onTournamentStarted: (message: TournamentStartedMessage) => void
  onTournamentMatchStarting: (message: TournamentMatchStartingMessage) => void
  onTournamentBye: (message: TournamentByeMessage) => void
  onRoundComplete: (message: RoundCompleteMessage) => void
  onMatchComplete: (message: MatchCompleteMessage) => void
  onPlayerReadyForRound: (message: PlayerReadyForRoundMessage) => void
  onTournamentComplete: (message: TournamentCompleteMessage) => void
  onTournamentResumed: (message: TournamentResumedMessage) => void
  // Spectating handlers
  onActiveMatches: (message: ActiveMatchesMessage) => void
  onSpectatorStateUpdate: (message: SpectatorStateUpdateMessage) => void
  onSpectatingStarted: (message: SpectatingStartedMessage) => void
  onSpectatingStopped: (message: SpectatingStoppedMessage) => void
  // Combat UI handlers
  onOpponentAttackerTargets: (message: OpponentAttackerTargetsMessage) => void
  onOpponentBlockerAssignments: (message: OpponentBlockerAssignmentsMessage) => void
  // Disconnect handlers
  onOpponentDisconnected: (message: OpponentDisconnectedMessage) => void
  onOpponentReconnected: (message: OpponentReconnectedMessage) => void
  onTournamentPlayerDisconnected: (message: TournamentPlayerDisconnectedMessage) => void
  onTournamentPlayerReconnected: (message: TournamentPlayerReconnectedMessage) => void
  // Quick Game Lobby handlers
  onQuickGameLobbyState: (message: QuickGameLobbyStateMessage) => void
  onQuickGameLobbyClosed: (message: QuickGameLobbyClosedMessage) => void
}

/**
 * Route a server message to the appropriate handler.
 */
export function handleServerMessage(message: ServerMessage, handlers: MessageHandlers): void {
  switch (message.type) {
    case 'connected':
      handlers.onConnected(message)
      break
    case 'reconnected':
      handlers.onReconnected(message)
      break
    case 'gameCreated':
      handlers.onGameCreated(message)
      break
    case 'gameStarted':
      handlers.onGameStarted(message)
      break
    case 'gameCancelled':
      handlers.onGameCancelled(message)
      break
    case 'stateUpdate':
      handlers.onStateUpdate(message)
      break
    case 'stateDeltaUpdate':
      handlers.onStateDeltaUpdate(message)
      break
    case 'mulliganDecision':
      handlers.onMulliganDecision(message)
      break
    case 'chooseBottomCards':
      handlers.onChooseBottomCards(message)
      break
    case 'mulliganComplete':
      handlers.onMulliganComplete(message)
      break
    case 'waitingForOpponentMulligan':
      handlers.onWaitingForOpponentMulligan()
      break
    case 'gameOver':
      handlers.onGameOver(message)
      break
    case 'error':
      handlers.onError(message)
      break
    // Sealed draft messages
    case 'sealedGameCreated':
      handlers.onSealedGameCreated(message)
      break
    case 'sealedPoolGenerated':
      handlers.onSealedPoolGenerated(message)
      break
    case 'opponentDeckSubmitted':
      handlers.onOpponentDeckSubmitted(message)
      break
    case 'waitingForOpponent':
      handlers.onWaitingForOpponent(message)
      break
    case 'deckSubmitted':
      handlers.onDeckSubmitted(message)
      break
    // Lobby messages
    case 'lobbyCreated':
      handlers.onLobbyCreated(message)
      break
    case 'lobbyUpdate':
      handlers.onLobbyUpdate(message)
      break
    case 'lobbyStopped':
      handlers.onLobbyStopped(message)
      break
    // Draft messages
    case 'draftPackReceived':
      handlers.onDraftPackReceived(message)
      break
    case 'draftPickMade':
      handlers.onDraftPickMade(message)
      break
    case 'draftPickConfirmed':
      handlers.onDraftPickConfirmed(message)
      break
    case 'draftComplete':
      handlers.onDraftComplete(message)
      break
    case 'draftTimerUpdate':
      handlers.onDraftTimerUpdate(message)
      break
    // Winston Draft messages
    case 'winstonDraftState':
      handlers.onWinstonDraftState(message)
      break
    // Grid Draft messages
    case 'gridDraftState':
      handlers.onGridDraftState(message)
      break
    // Tournament messages
    case 'tournamentStarted':
      handlers.onTournamentStarted(message)
      break
    case 'tournamentMatchStarting':
      handlers.onTournamentMatchStarting(message)
      break
    case 'tournamentBye':
      handlers.onTournamentBye(message)
      break
    case 'roundComplete':
      handlers.onRoundComplete(message)
      break
    case 'matchComplete':
      handlers.onMatchComplete(message)
      break
    case 'playerReadyForRound':
      handlers.onPlayerReadyForRound(message)
      break
    case 'tournamentComplete':
      handlers.onTournamentComplete(message)
      break
    case 'tournamentResumed':
      handlers.onTournamentResumed(message)
      break
    // Spectating messages
    case 'activeMatches':
      handlers.onActiveMatches(message)
      break
    case 'spectatorStateUpdate':
      handlers.onSpectatorStateUpdate(message)
      break
    case 'spectatingStarted':
      handlers.onSpectatingStarted(message)
      break
    case 'spectatingStopped':
      handlers.onSpectatingStopped(message)
      break
    // Combat UI messages
    case 'opponentAttackerTargets':
      handlers.onOpponentAttackerTargets(message)
      break
    case 'opponentBlockerAssignments':
      handlers.onOpponentBlockerAssignments(message)
      break
    case 'opponentDisconnected':
      handlers.onOpponentDisconnected(message)
      break
    case 'opponentReconnected':
      handlers.onOpponentReconnected(message)
      break
    case 'tournamentPlayerDisconnected':
      handlers.onTournamentPlayerDisconnected(message)
      break
    case 'tournamentPlayerReconnected':
      handlers.onTournamentPlayerReconnected(message)
      break
    case 'quickGameLobbyState':
      handlers.onQuickGameLobbyState(message)
      break
    case 'quickGameLobbyClosed':
      handlers.onQuickGameLobbyClosed(message)
      break
    default: {
      // TypeScript exhaustiveness check
      const _exhaustive: never = message
      console.warn('Unknown message type:', _exhaustive)
    }
  }
}

/**
 * Create a message handler proxy that logs all messages before delegating (useful for debugging).
 * Converts handler names like "onGameStarted" to log labels like "[Server] Game Started".
 */
export function createLoggingHandlers(handlers: MessageHandlers): MessageHandlers {
  return new Proxy(handlers, {
    get(target, prop, receiver) {
      const original = Reflect.get(target, prop, receiver)
      if (typeof original !== 'function' || typeof prop !== 'string') return original
      // Convert "onGameStarted" → "Game Started"
      const label = prop
        .replace(/^on/, '')
        .replace(/([A-Z])/g, ' $1')
        .trim()
      return (...args: unknown[]) => {
        const logFn = prop === 'onError' ? console.error : console.log
        logFn(`[Server] ${label}:`, ...args)
        return (original as (...a: unknown[]) => unknown).apply(target, args)
      }
    },
  }) as MessageHandlers
}
