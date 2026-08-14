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
import type { AiDeckSpec, DeckFormat, QuickGameLobbyStateMessage } from '@/types'
import {
  createCreateQuickGameLobbyMessage,
  createJoinQuickGameLobbyMessage,
  createLeaveQuickGameLobbyMessage,
  createSubmitQuickGameLobbyDeckMessage,
  createSetQuickGameLobbyReadyMessage,
  createSetQuickGameLobbySetCodeMessage,
  createSetQuickGameLobbyPublicMessage,
  createSetQuickGameLobbyRankedMessage,
  createSetQuickGameLobbyFormatMessage,
  createSetQuickGameAiDeckMessage,
  createAddQuickGameAiMessage,
  createRemoveQuickGameAiMessage,
} from '@/types'
import type { SliceCreator } from './types'
import { getWebSocket } from './shared'

export interface QuickGameLobbySliceState {
  quickGameLobbyState: QuickGameLobbyStateMessage | null
}

export interface QuickGameLobbySliceActions {
  createQuickGameLobby: (
    vsAi?: boolean,
    setCode?: string,
    isPublic?: boolean,
    format?: DeckFormat,
    momirBasic?: boolean,
    ranked?: boolean,
  ) => void
  joinQuickGameLobby: (lobbyId: string) => void
  leaveQuickGameLobby: () => void
  submitQuickGameLobbyDeck: (deckList: Record<string, number>, commander?: string | null) => void
  setQuickGameLobbyReady: (ready: boolean) => void
  setQuickGameLobbySetCode: (setCodes: readonly string[]) => void
  setQuickGameLobbyPublic: (isPublic: boolean) => void
  setQuickGameLobbyRanked: (ranked: boolean) => void
  setQuickGameLobbyFormat: (format: DeckFormat | null, momirBasic?: boolean) => void
  /** Host-only: choose what the AI opponent plays. No-op in a human lobby (server rejects). */
  setQuickGameAiDeck: (spec: AiDeckSpec) => void
  addQuickGameAi: () => void
  removeQuickGameAi: () => void
}

export type QuickGameLobbySlice = QuickGameLobbySliceState & QuickGameLobbySliceActions

export const createQuickGameLobbySlice: SliceCreator<QuickGameLobbySlice> = (set) => ({
  quickGameLobbyState: null,

  createQuickGameLobby: (vsAi, setCode, isPublic, format, momirBasic, ranked) => {
    getWebSocket()?.send(createCreateQuickGameLobbyMessage(vsAi, setCode, isPublic, format, momirBasic, ranked))
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

  setQuickGameLobbySetCode: (setCodes) => {
    getWebSocket()?.send(createSetQuickGameLobbySetCodeMessage(setCodes))
  },

  setQuickGameLobbyPublic: (isPublic) => {
    getWebSocket()?.send(createSetQuickGameLobbyPublicMessage(isPublic))
  },

  setQuickGameLobbyRanked: (ranked) => {
    getWebSocket()?.send(createSetQuickGameLobbyRankedMessage(ranked))
  },

  setQuickGameLobbyFormat: (format, momirBasic) => {
    getWebSocket()?.send(createSetQuickGameLobbyFormatMessage(format, momirBasic))
  },

  setQuickGameAiDeck: (spec) => {
    getWebSocket()?.send(createSetQuickGameAiDeckMessage(spec))
  },

  addQuickGameAi: () => {
    getWebSocket()?.send(createAddQuickGameAiMessage())
  },

  removeQuickGameAi: () => {
    getWebSocket()?.send(createRemoveQuickGameAiMessage())
  },
})
