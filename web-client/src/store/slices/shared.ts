/**
 * Shared utilities and WebSocket instance for store slices.
 */
import { GameWebSocket } from '../../network/websocket'
import type { MessageHandlers } from '../../network/messageHandlers'

// WebSocket singleton instance
let wsInstance: GameWebSocket | null = null

export function getWebSocket(): GameWebSocket | null {
  return wsInstance
}

export function setWebSocket(ws: GameWebSocket | null): void {
  wsInstance = ws
}

// Message handlers - set during store initialization
let messageHandlersInstance: MessageHandlers | null = null

export function getMessageHandlers(): MessageHandlers | null {
  return messageHandlersInstance
}

export function setMessageHandlers(handlers: MessageHandlers | null): void {
  messageHandlersInstance = handlers
}

// ============================================================================
// Deck Building State Persistence
// ============================================================================

interface SavedDeckState {
  deck: readonly string[]
  landCounts: Record<string, number>
}

const DECK_STATE_KEY = 'argentum-deck-state'

export function saveDeckState(deck: readonly string[], landCounts: Record<string, number>): void {
  const state: SavedDeckState = { deck, landCounts }
  sessionStorage.setItem(DECK_STATE_KEY, JSON.stringify(state))
}

export function loadDeckState(): SavedDeckState | null {
  const saved = sessionStorage.getItem(DECK_STATE_KEY)
  if (!saved) return null
  try {
    return JSON.parse(saved) as SavedDeckState
  } catch {
    return null
  }
}

export function clearDeckState(): void {
  sessionStorage.removeItem(DECK_STATE_KEY)
}

// ============================================================================
// Lobby State Persistence
// ============================================================================

const LOBBY_ID_KEY = 'argentum-lobby-id'

export function saveLobbyId(lobbyId: string): void {
  localStorage.setItem(LOBBY_ID_KEY, lobbyId)
}

export function loadLobbyId(): string | null {
  return localStorage.getItem(LOBBY_ID_KEY)
}

export function clearLobbyId(): void {
  localStorage.removeItem(LOBBY_ID_KEY)
}
