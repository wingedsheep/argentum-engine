/**
 * Shared utilities and WebSocket instance for store slices.
 */
import { GameWebSocket } from '@/network/websocket.ts'
import type { MessageHandlers } from '@/network/messageHandlers.ts'

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
// Re-authentication
// ============================================================================

// Registered by connectionSlice when it opens a connection. Re-sends the connect
// message (with the stored token) over the current socket — used to recover when the
// server answers NOT_CONNECTED because it no longer recognizes this socket as an
// authenticated session (e.g. after a server restart).
let reauthHandler: (() => void) | null = null
let lastReauthAt = 0

const REAUTH_DEBOUNCE_MS = 5_000

export function setReauthHandler(handler: (() => void) | null): void {
  reauthHandler = handler
  lastReauthAt = 0
}

/**
 * Re-send the connect message if a handler is registered. Returns true when a
 * re-auth is in flight (just sent or sent within the debounce window).
 */
export function requestReauth(): boolean {
  if (!reauthHandler) return false
  const now = Date.now()
  if (now - lastReauthAt < REAUTH_DEBOUNCE_MS) return true
  lastReauthAt = now
  reauthHandler()
  return true
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
