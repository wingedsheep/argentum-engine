/**
 * Connection slice - handles WebSocket connection state and authentication.
 */
import type { SliceCreator, EntityId } from './types'
import type { ConnectionStatus } from '../../network/websocket'
import { GameWebSocket, getWebSocketUrl } from '../../network/websocket'
import { handleServerMessage, createLoggingHandlers } from '../../network/messageHandlers'
import { createConnectMessage, ErrorCode } from '../../types'
import {
  getWebSocket,
  setWebSocket,
  clearLobbyId,
  clearDeckState,
} from './shared'
import { createMessageHandlers } from './handlers'

export interface ConnectionSliceState {
  connectionStatus: ConnectionStatus
  playerId: EntityId | null
  sessionId: string | null
}

export interface ConnectionSliceActions {
  connect: (playerName: string) => void
  disconnect: () => void
}

export type ConnectionSlice = ConnectionSliceState & ConnectionSliceActions

export const createConnectionSlice: SliceCreator<ConnectionSlice> = (set, get) => ({
  // Initial state
  connectionStatus: 'disconnected' as ConnectionStatus,
  playerId: null,
  sessionId: null,

  // Actions
  connect: (playerName) => {
    const { connectionStatus } = get()
    if (connectionStatus === 'connecting' || connectionStatus === 'connected') {
      return
    }

    const existingWs = getWebSocket()
    if (existingWs) {
      existingWs.disconnect()
    }

    // Store player name for reconnection
    sessionStorage.setItem('argentum-player-name', playerName)

    // Build message handlers from the full store
    const handlers = createMessageHandlers(set, get)
    const wrappedHandlers = import.meta.env.DEV
      ? createLoggingHandlers(handlers)
      : handlers

    const ws = new GameWebSocket({
      url: getWebSocketUrl(),
      onMessage: (msg) => handleServerMessage(msg, wrappedHandlers),
      onStatusChange: (status) => {
        set({ connectionStatus: status })
        if (status === 'connected' && getWebSocket()) {
          const token = sessionStorage.getItem('argentum-token') ?? undefined
          const storedName = sessionStorage.getItem('argentum-player-name') ?? playerName
          getWebSocket()?.send(createConnectMessage(storedName, token))
        }
      },
      onError: () => {
        set({
          lastError: {
            code: ErrorCode.INTERNAL_ERROR,
            message: 'WebSocket connection error',
            timestamp: Date.now(),
          },
        })
      },
    })

    setWebSocket(ws)
    ws.connect()
  },

  disconnect: () => {
    const ws = getWebSocket()
    ws?.disconnect()
    setWebSocket(null)
    sessionStorage.removeItem('argentum-token')
    sessionStorage.removeItem('argentum-player-name')
    clearLobbyId()
    clearDeckState()
    set({
      connectionStatus: 'disconnected',
      playerId: null,
      sessionId: null,
      opponentName: null,
      gameState: null,
      legalActions: [],
      pendingDecision: null,
      mulliganState: null,
      waitingForOpponentMulligan: false,
      gameOverState: null,
      deckBuildingState: null,
      lobbyState: null,
      tournamentState: null,
    })
  },
})
