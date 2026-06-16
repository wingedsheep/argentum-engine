/**
 * Quick Game Lobby slice — staging-area state for the new quick-game flow.
 *
 * Holds the latest [QuickGameLobbyStateMessage] received from the server and exposes the four
 * actions the UI uses to drive the lobby (create, join, leave, submit deck, ready toggle).
 *
 * The slice deliberately mirrors the server's snapshot rather than building its own derived
 * model — every state change comes from the server as a fresh `quickGameLobbyState` message,
 * so we just store and re-render.
 */
import type { DeckFormat, QuickGameLobbyStateMessage } from '@/types'
import {
  createCreateQuickGameLobbyMessage,
  createJoinQuickGameLobbyMessage,
  createLeaveQuickGameLobbyMessage,
  createSubmitQuickGameLobbyDeckMessage,
  createSetQuickGameLobbyReadyMessage,
  createSetQuickGameLobbySetCodeMessage,
  createSetQuickGameLobbyPublicMessage,
  createSetQuickGameLobbyFormatMessage,
} from '@/types'
import type { SliceCreator } from './types'
import { getWebSocket } from './shared'

export interface QuickGameLobbySliceState {
  quickGameLobbyState: QuickGameLobbyStateMessage | null
}

export interface QuickGameLobbySliceActions {
  createQuickGameLobby: (vsAi?: boolean, setCode?: string, isPublic?: boolean, format?: DeckFormat, momirBasic?: boolean) => void
  joinQuickGameLobby: (lobbyId: string) => void
  leaveQuickGameLobby: () => void
  submitQuickGameLobbyDeck: (deckList: Record<string, number>, commander?: string | null) => void
  setQuickGameLobbyReady: (ready: boolean) => void
  setQuickGameLobbySetCode: (setCode: string | null) => void
  setQuickGameLobbyPublic: (isPublic: boolean) => void
  setQuickGameLobbyFormat: (format: DeckFormat | null, momirBasic?: boolean) => void
}

export type QuickGameLobbySlice = QuickGameLobbySliceState & QuickGameLobbySliceActions

export const createQuickGameLobbySlice: SliceCreator<QuickGameLobbySlice> = (set) => ({
  quickGameLobbyState: null,

  createQuickGameLobby: (vsAi, setCode, isPublic, format, momirBasic) => {
    getWebSocket()?.send(createCreateQuickGameLobbyMessage(vsAi, setCode, isPublic, format, momirBasic))
  },

  joinQuickGameLobby: (lobbyId) => {
    getWebSocket()?.send(createJoinQuickGameLobbyMessage(lobbyId))
  },

  leaveQuickGameLobby: () => {
    getWebSocket()?.send(createLeaveQuickGameLobbyMessage())
    set({ quickGameLobbyState: null })
  },

  submitQuickGameLobbyDeck: (deckList, commander) => {
    getWebSocket()?.send(createSubmitQuickGameLobbyDeckMessage(deckList, commander))
  },

  setQuickGameLobbyReady: (ready) => {
    getWebSocket()?.send(createSetQuickGameLobbyReadyMessage(ready))
  },

  setQuickGameLobbySetCode: (setCode) => {
    getWebSocket()?.send(createSetQuickGameLobbySetCodeMessage(setCode))
  },

  setQuickGameLobbyPublic: (isPublic) => {
    getWebSocket()?.send(createSetQuickGameLobbyPublicMessage(isPublic))
  },

  setQuickGameLobbyFormat: (format, momirBasic) => {
    getWebSocket()?.send(createSetQuickGameLobbyFormatMessage(format, momirBasic))
  },
})
