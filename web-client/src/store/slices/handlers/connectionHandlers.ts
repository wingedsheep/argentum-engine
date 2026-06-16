/**
 * Handlers for connection and reconnection messages.
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import { entityId, createJoinLobbyMessage, createSpectateGameMessage } from '@/types'
import { getWebSocket, clearLobbyId, loadLobbyId } from '../shared'
import type { SetState, GetState } from './types'

type ConnectionHandlerKeys =
  | 'onConnected'
  | 'onReconnected'
  | 'onOnlinePlayersCount'
  | 'onPong'
  | 'onSessionReplaced'

export function createConnectionHandlers(set: SetState, get: GetState): Pick<MessageHandlers, ConnectionHandlerKeys> {
  return {
    onConnected: (msg) => {
      localStorage.setItem('argentum-token', msg.token)
      // A plain Connected (not Reconnected) after an auto-reconnect means the server has
      // no memory of this identity (e.g. it restarted while the tab was backgrounded) —
      // any session state from before is stale. Drop it so the user lands in the lobby
      // instead of a dead game board.
      const stale = get().sessionId !== null || get().gameState !== null
      set({
        connectionStatus: 'connected',
        playerId: entityId(msg.playerId),
        aiEnabled: msg.aiEnabled ?? false,
        availableSets: msg.availableSets ?? [],
        ...(stale && {
          sessionId: null,
          opponentName: null,
          gameState: null,
          legalActions: [],
          pendingDecision: null,
          mulliganState: null,
          waitingForOpponentMulligan: false,
          deckBuildingState: null,
          lobbyState: null,
          tournamentState: null,
        }),
      })

      // Auto-join tournament if we have a pending tournament ID (from /tournament/:lobbyId route)
      const { pendingTournamentId, pendingSpectateGameId } = get()
      if (pendingTournamentId) {
        set({ pendingTournamentId: null })
        getWebSocket()?.send(createJoinLobbyMessage(pendingTournamentId))
      } else if (pendingSpectateGameId) {
        // Set when the user clicked Spectate on the landing page before being connected.
        set({ pendingSpectateGameId: null })
        getWebSocket()?.send(createSpectateGameMessage(pendingSpectateGameId))
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

    onOnlinePlayersCount: (msg) => {
      set({ onlinePlayers: msg.count })
    },

    // Liveness replies are consumed by GameWebSocket's last-message tracking; nothing
    // store-side to update.
    onPong: () => {},

    onSessionReplaced: () => {
      // Another tab/device took over this identity. Stop all auto-reconnect machinery —
      // reconnecting would just steal the session back and the two tabs would fight.
      // The takeover overlay offers an explicit "Use here" to reclaim.
      // Set the flag BEFORE the status flips to 'disconnected', so auto-connect effects
      // keyed on connectionStatus see it and stand down.
      set({ sessionReplaced: true })
      getWebSocket()?.disconnect()
      set({ connectionStatus: 'disconnected' })
    },
  }
}
