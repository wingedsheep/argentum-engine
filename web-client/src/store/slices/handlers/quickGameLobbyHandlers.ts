/**
 * Server-message handlers for the Quick Game Lobby flow. Wires lobby state-snapshot updates
 * and lobby-closed notifications into the [quickGameLobbySlice].
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import { ErrorCode } from '@/types'
import type { SetState, GetState } from './types'

type QuickGameLobbyHandlerKeys = 'onQuickGameLobbyState' | 'onQuickGameLobbyClosed'

export function createQuickGameLobbyHandlers(
  set: SetState,
  _get: GetState
): Pick<MessageHandlers, QuickGameLobbyHandlerKeys> {
  return {
    onQuickGameLobbyState: (msg) => {
      set({ quickGameLobbyState: msg })
    },

    onQuickGameLobbyClosed: (msg) => {
      // Surface the close reason via the existing global error channel so the home screen
      // can render it like any other connection-time error message.
      set({
        quickGameLobbyState: null,
        lastError: { message: msg.reason, code: ErrorCode.INVALID_ACTION, timestamp: Date.now() },
      })
    },
  }
}
