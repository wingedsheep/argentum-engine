/**
 * Handlers for connection and reconnection messages.
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import { entityId, createJoinLobbyMessage } from '@/types'
import { getWebSocket, clearLobbyId, loadLobbyId } from '../shared'
import type { SetState, GetState } from './types'

type ConnectionHandlerKeys = 'onConnected' | 'onReconnected'

export function createConnectionHandlers(set: SetState, get: GetState): Pick<MessageHandlers, ConnectionHandlerKeys> {
  return {
    onConnected: (msg) => {
      localStorage.setItem('argentum-token', msg.token)
      set({
        connectionStatus: 'connected',
        playerId: entityId(msg.playerId),
        aiEnabled: msg.aiEnabled ?? false,
        availableSets: msg.availableSets ?? [],
      })

      // Auto-join tournament if we have a pending tournament ID (from /tournament/:lobbyId route)
      const { pendingTournamentId } = get()
      if (pendingTournamentId) {
        set({ pendingTournamentId: null })
        getWebSocket()?.send(createJoinLobbyMessage(pendingTournamentId))
      } else {
        clearLobbyId()
      }
    },

    onReconnected: (msg) => {
      localStorage.setItem('argentum-token', msg.token)
      const updates: Partial<import('../types').GameStore> = {
        connectionStatus: 'connected',
        playerId: entityId(msg.playerId),
        aiEnabled: msg.aiEnabled ?? false,
        availableSets: msg.availableSets ?? [],
      }
      if (msg.context === 'game' && msg.contextId) {
        updates.sessionId = msg.contextId
      }
      set(updates)

      if (!msg.context && !msg.contextId) {
        const savedLobbyId = loadLobbyId()
        if (savedLobbyId) {
          getWebSocket()?.send(createJoinLobbyMessage(savedLobbyId))
        }
      }
    },
  }
}
