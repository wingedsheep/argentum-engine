import type {
  ServerMessage,
  ConnectedMessage,
  GameCreatedMessage,
  GameStartedMessage,
  StateUpdateMessage,
  MulliganDecisionMessage,
  ChooseBottomCardsMessage,
  MulliganCompleteMessage,
  GameOverMessage,
  ErrorMessage,
} from '../types'

/**
 * Handler functions for each server message type.
 */
export interface MessageHandlers {
  onConnected: (message: ConnectedMessage) => void
  onGameCreated: (message: GameCreatedMessage) => void
  onGameStarted: (message: GameStartedMessage) => void
  onStateUpdate: (message: StateUpdateMessage) => void
  onMulliganDecision: (message: MulliganDecisionMessage) => void
  onChooseBottomCards: (message: ChooseBottomCardsMessage) => void
  onMulliganComplete: (message: MulliganCompleteMessage) => void
  onGameOver: (message: GameOverMessage) => void
  onError: (message: ErrorMessage) => void
}

/**
 * Route a server message to the appropriate handler.
 */
export function handleServerMessage(message: ServerMessage, handlers: MessageHandlers): void {
  switch (message.type) {
    case 'connected':
      handlers.onConnected(message)
      break
    case 'gameCreated':
      handlers.onGameCreated(message)
      break
    case 'gameStarted':
      handlers.onGameStarted(message)
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
    case 'gameOver':
      handlers.onGameOver(message)
      break
    case 'error':
      handlers.onError(message)
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
    onGameCreated: (msg) => {
      console.log('[Server] Game created:', msg)
      handlers.onGameCreated(msg)
    },
    onGameStarted: (msg) => {
      console.log('[Server] Game started:', msg)
      handlers.onGameStarted(msg)
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
    onGameOver: (msg) => {
      console.log('[Server] Game over:', msg)
      handlers.onGameOver(msg)
    },
    onError: (msg) => {
      console.error('[Server] Error:', msg)
      handlers.onError(msg)
    },
  }
}
