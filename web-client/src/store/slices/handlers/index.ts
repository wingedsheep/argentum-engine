/**
 * Composes all domain-specific handler modules into a single MessageHandlers object.
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import { createConnectionHandlers } from './connectionHandlers'
import { createGameplayHandlers } from './gameplayHandlers'
import { createDraftHandlers } from './draftHandlers'
import { createLobbyHandlers } from './lobbyHandlers'
import { createSpectatingHandlers } from './spectatingHandlers'
import { createQuickGameLobbyHandlers } from './quickGameLobbyHandlers'
import type { SetState, GetState } from './types'

export function createMessageHandlers(set: SetState, get: GetState): MessageHandlers {
  return {
    ...createConnectionHandlers(set, get),
    ...createGameplayHandlers(set, get),
    ...createDraftHandlers(set, get),
    ...createLobbyHandlers(set, get),
    ...createSpectatingHandlers(set, get),
    ...createQuickGameLobbyHandlers(set, get),
  }
}
