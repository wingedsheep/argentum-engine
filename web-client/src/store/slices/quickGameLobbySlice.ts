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
import type {
  AiDeckSpec,
  DeckFormat,
  MatchFoundMessage,
  MatchmakingMode,
  MatchmakingQueueCount,
  MatchmakingStatusMessage,
  QuickGameLobbyStateMessage,
} from '@/types'
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
  createJoinMatchmakingMessage,
  createLeaveMatchmakingMessage,
  createRespondToMatchMessage,
} from '@/types'
import type { SliceCreator } from './types'
import { getWebSocket } from './shared'

export interface QuickGameLobbySliceState {
  quickGameLobbyState: QuickGameLobbyStateMessage | null
  /**
   * The server's last word on this player's matchmaking: searching (with format/ranked/since) or
   * idle, plus a notice when something happened that they didn't do. Null before any.
   */
  matchmaking: MatchmakingStatusMessage | null
  /** An open "match found" prompt, stamped with when it arrived so the countdown can run locally. */
  matchOffer: (MatchFoundMessage & { readonly receivedAt: number }) | null
  /** Searching players per queue; null until the first REST fetch or push. */
  matchmakingQueues: readonly MatchmakingQueueCount[] | null
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
  submitQuickGameLobbyDeck: (
    deckList: Record<string, number>,
    commander?: string | null,
    sideboard?: Record<string, number>,
    /** The second commander of a partner pair; ignored without `commander`. */
    secondCommander?: string | null,
    /** The deck's companion (CR 702.139), a sideboard card; null for none. */
    companion?: string | null,
  ) => void
  setQuickGameLobbyReady: (ready: boolean) => void
  setQuickGameLobbySetCode: (setCodes: readonly string[]) => void
  setQuickGameLobbyPublic: (isPublic: boolean) => void
  setQuickGameLobbyRanked: (ranked: boolean) => void
  setQuickGameLobbyFormat: (format: DeckFormat | null, momirBasic?: boolean) => void
  /** Host-only: choose what the AI opponent plays. No-op in a human lobby (server rejects). */
  setQuickGameAiDeck: (spec: AiDeckSpec) => void
  addQuickGameAi: () => void
  removeQuickGameAi: () => void
  /** Search for a stranger to play. `format: null` is Limited (a random sealed pool each). */
  joinMatchmaking: (mode: MatchmakingMode, format: DeckFormat | null, ranked: boolean) => void
  /** Stop searching; while a match prompt is open this declines it. */
  leaveMatchmaking: () => void
  respondToMatch: (accept: boolean) => void
  dismissMatchmakingNotice: () => void
}

export type QuickGameLobbySlice = QuickGameLobbySliceState & QuickGameLobbySliceActions

export const createQuickGameLobbySlice: SliceCreator<QuickGameLobbySlice> = (set, get) => ({
  quickGameLobbyState: null,
  matchmaking: null,
  matchOffer: null,
  matchmakingQueues: null,

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

  submitQuickGameLobbyDeck: (deckList, commander, sideboard, secondCommander, companion) => {
    getWebSocket()?.send(
      createSubmitQuickGameLobbyDeckMessage(
        deckList, commander, undefined, undefined, sideboard, secondCommander, undefined, companion,
      ),
    )
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

  joinMatchmaking: (mode, format, ranked) => {
    getWebSocket()?.send(createJoinMatchmakingMessage(mode, format, ranked))
  },

  leaveMatchmaking: () => {
    getWebSocket()?.send(createLeaveMatchmakingMessage())
  },

  respondToMatch: (accept) => {
    const offer = get().matchOffer
    if (!offer) return
    getWebSocket()?.send(createRespondToMatchMessage(offer.matchId, accept))
    // A decline is final on our side; an accept waits for the server's confirmation.
    if (!accept) set({ matchOffer: null })
  },

  dismissMatchmakingNotice: () => {
    const status = get().matchmaking
    if (status?.notice) set({ matchmaking: { ...status, notice: null } })
  },
})
