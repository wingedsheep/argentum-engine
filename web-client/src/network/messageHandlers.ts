import type {
  ServerMessage,
  ConnectedMessage,
  ReconnectedMessage,
  GameCreatedMessage,
  GameStartedMessage,
  GameCancelledMessage,
  StateUpdateMessage,
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
  TournamentStartedMessage,
  TournamentMatchStartingMessage,
  TournamentByeMessage,
  RoundCompleteMessage,
  TournamentCompleteMessage,
} from '../types'

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
  // Tournament handlers
  onTournamentStarted: (message: TournamentStartedMessage) => void
  onTournamentMatchStarting: (message: TournamentMatchStartingMessage) => void
  onTournamentBye: (message: TournamentByeMessage) => void
  onRoundComplete: (message: RoundCompleteMessage) => void
  onTournamentComplete: (message: TournamentCompleteMessage) => void
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
    case 'tournamentComplete':
      handlers.onTournamentComplete(message)
      break
    default: {
      // TypeScript exhaustiveness check
      const _exhaustive: never = message
      console.warn('Unknown message type:', _exhaustive)
    }
  }
}

/**
 * Create a message handler that logs all messages (useful for debugging).
 */
export function createLoggingHandlers(handlers: MessageHandlers): MessageHandlers {
  return {
    onConnected: (msg) => {
      console.log('[Server] Connected:', msg)
      handlers.onConnected(msg)
    },
    onReconnected: (msg) => {
      console.log('[Server] Reconnected:', msg)
      handlers.onReconnected(msg)
    },
    onGameCreated: (msg) => {
      console.log('[Server] Game created:', msg)
      handlers.onGameCreated(msg)
    },
    onGameStarted: (msg) => {
      console.log('[Server] Game started:', msg)
      handlers.onGameStarted(msg)
    },
    onGameCancelled: (msg) => {
      console.log('[Server] Game cancelled:', msg)
      handlers.onGameCancelled(msg)
    },
    onStateUpdate: (msg) => {
      console.log('[Server] State update:', msg)
      handlers.onStateUpdate(msg)
    },
    onMulliganDecision: (msg) => {
      console.log('[Server] Mulligan decision:', msg)
      handlers.onMulliganDecision(msg)
    },
    onChooseBottomCards: (msg) => {
      console.log('[Server] Choose bottom cards:', msg)
      handlers.onChooseBottomCards(msg)
    },
    onMulliganComplete: (msg) => {
      console.log('[Server] Mulligan complete:', msg)
      handlers.onMulliganComplete(msg)
    },
    onWaitingForOpponentMulligan: () => {
      console.log('[Server] Waiting for opponent mulligan')
      handlers.onWaitingForOpponentMulligan()
    },
    onGameOver: (msg) => {
      console.log('[Server] Game over:', msg)
      handlers.onGameOver(msg)
    },
    onError: (msg) => {
      console.error('[Server] Error:', msg)
      handlers.onError(msg)
    },
    // Sealed draft handlers
    onSealedGameCreated: (msg) => {
      console.log('[Server] Sealed game created:', msg)
      handlers.onSealedGameCreated(msg)
    },
    onSealedPoolGenerated: (msg) => {
      console.log('[Server] Sealed pool generated:', msg)
      handlers.onSealedPoolGenerated(msg)
    },
    onOpponentDeckSubmitted: (msg) => {
      console.log('[Server] Opponent deck submitted:', msg)
      handlers.onOpponentDeckSubmitted(msg)
    },
    onWaitingForOpponent: (msg) => {
      console.log('[Server] Waiting for opponent:', msg)
      handlers.onWaitingForOpponent(msg)
    },
    onDeckSubmitted: (msg) => {
      console.log('[Server] Deck submitted:', msg)
      handlers.onDeckSubmitted(msg)
    },
    // Lobby handlers
    onLobbyCreated: (msg) => {
      console.log('[Server] Lobby created:', msg)
      handlers.onLobbyCreated(msg)
    },
    onLobbyUpdate: (msg) => {
      console.log('[Server] Lobby update:', msg)
      handlers.onLobbyUpdate(msg)
    },
    // Tournament handlers
    onTournamentStarted: (msg) => {
      console.log('[Server] Tournament started:', msg)
      handlers.onTournamentStarted(msg)
    },
    onTournamentMatchStarting: (msg) => {
      console.log('[Server] Tournament match starting:', msg)
      handlers.onTournamentMatchStarting(msg)
    },
    onTournamentBye: (msg) => {
      console.log('[Server] Tournament bye:', msg)
      handlers.onTournamentBye(msg)
    },
    onRoundComplete: (msg) => {
      console.log('[Server] Round complete:', msg)
      handlers.onRoundComplete(msg)
    },
    onTournamentComplete: (msg) => {
      console.log('[Server] Tournament complete:', msg)
      handlers.onTournamentComplete(msg)
    },
  }
}
