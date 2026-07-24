/**
 * Handlers for spectating, combat UI, and disconnect messages.
 */
import type { MessageHandlers } from '@/network/messageHandlers.ts'
import type { SetState, GetState } from './types'

type SpectatingHandlerKeys =
  | 'onActiveMatches' | 'onSpectatorStateUpdate' | 'onSpectatingStarted' | 'onSpectatingStopped'
  | 'onSpectatorCountChanged'
  | 'onOpponentAttackerTargets' | 'onOpponentBlockerAssignments'
  | 'onOpponentDisconnected' | 'onOpponentReconnected'
  | 'onTournamentPlayerDisconnected' | 'onTournamentPlayerReconnected'

export function createSpectatingHandlers(set: SetState, get: GetState): Pick<MessageHandlers, SpectatingHandlerKeys> {
  return {
    onActiveMatches: (msg) => {
      set((state) => {
        // Don't clear game state if we're in an active game (no gameOverState yet)
        const inActiveGame = state.gameState != null && state.gameOverState == null
        return {
          tournamentState: state.tournamentState
            ? {
                ...state.tournamentState,
                activeMatches: msg.matches,
                standings: msg.standings,
                ...(inActiveGame ? {} : {
                  currentMatchGameSessionId: null,
                  currentMatchOpponentName: null,
                }),
              }
            : null,
          // Preserve game state while in an active game or game-over banner is showing
          ...(inActiveGame ? {} : {
            gameState: state.gameOverState ? state.gameState : null,
            mulliganState: null,
            waitingForOpponentMulligan: false,
            legalActions: [],
          }),
        }
      })
    },

    onSpectatorStateUpdate: (msg) => {
      const { spectatingState: prevState, addDamageAnimation, sessionId } = get()

      // Ignore spectator updates if we're in an active game
      if (sessionId) return

      // Stamp the seat → team map from the roster (once). Team membership only rides in the
      // seat roster, so without this a spectator never knows the game is a team game (2HG /
      // Team vs. Team) — the team-grouped rail, ally treatment, and team-split layout all read it.
      if (Object.keys(get().teamByPlayerId).length === 0 && msg.players?.some((p) => p.teamIndex != null)) {
        const teams: Record<string, number> = {}
        for (const p of msg.players) if (p.teamIndex != null) teams[p.playerId] = p.teamIndex
        get().setSeatTeams(teams, msg.players.some((p) => p.teamSharedLife))
      }

      if (msg.gameState && prevState?.gameState) {
        const prevPlayers = prevState.gameState.players
        const newPlayers = msg.gameState.players

        for (const newPlayer of newPlayers) {
          const prevPlayer = prevPlayers.find(p => p.playerId === newPlayer.playerId)
          if (prevPlayer && prevPlayer.life !== newPlayer.life) {
            const diff = newPlayer.life - prevPlayer.life
            addDamageAnimation({
              id: `spectator-life-${newPlayer.playerId}-${Date.now()}`,
              targetId: newPlayer.playerId,
              targetIsPlayer: true,
              amount: Math.abs(diff),
              isLifeGain: diff > 0,
              startTime: Date.now(),
            })
          }
        }
      }

      set((state) => ({
        spectatingState: {
          gameSessionId: msg.gameSessionId,
          gameState: msg.gameState ?? null,
          player1Id: msg.player1Id ?? null,
          player2Id: msg.player2Id ?? null,
          player1Name: msg.player1Name ?? msg.player1.playerName,
          player2Name: msg.player2Name ?? msg.player2.playerName,
          player1: msg.player1,
          player2: msg.player2,
          currentPhase: msg.currentPhase,
          activePlayerId: msg.activePlayerId,
          priorityPlayerId: msg.priorityPlayerId,
          combat: msg.combat,
          decisionStatus: msg.decisionStatus ?? null,
        },
        opponentAttackerTargets: msg.combat ? null : state.opponentAttackerTargets,
        opponentBlockerAssignments: (msg.combat?.attackers?.some(a => a.blockedBy.length > 0) || !msg.combat) ? null : state.opponentBlockerAssignments,
      }))
    },

    onSpectatingStarted: (msg) => {
      set({
        spectatingState: {
          gameSessionId: msg.gameSessionId,
          gameState: null,
          player1Id: null,
          player2Id: null,
          player1Name: msg.player1Name,
          player2Name: msg.player2Name,
          player1: null,
          player2: null,
          currentPhase: null,
          activePlayerId: null,
          priorityPlayerId: null,
          combat: null,
          decisionStatus: null,
        },
      })
    },

    onSpectatingStopped: () => {
      set({ spectatingState: null })
      // Leaving the stream: drop the stamped team map so a later non-team spectate starts clean.
      get().setSeatTeams({})
    },

    onSpectatorCountChanged: (msg) => {
      set({
        spectatorCount: msg.count,
        spectatorNames: msg.spectatorNames ?? [],
      })
    },

    onOpponentAttackerTargets: (msg) => {
      set({ opponentAttackerTargets: { selectedAttackers: msg.selectedAttackers, attackerTargets: msg.attackerTargets } })
    },

    onOpponentBlockerAssignments: (msg) => {
      set({ opponentBlockerAssignments: msg.assignments })
    },

    onOpponentDisconnected: (msg) => {
      set({ opponentDisconnectCountdown: msg.secondsRemaining })
    },

    onOpponentReconnected: () => {
      set({ opponentDisconnectCountdown: null })
    },

    onTournamentPlayerDisconnected: (msg) => {
      set((state) => ({
        disconnectedPlayers: {
          ...state.disconnectedPlayers,
          [msg.playerId]: {
            playerName: msg.playerName,
            secondsRemaining: msg.secondsRemaining,
            disconnectedAt: Date.now(),
          },
        },
      }))
    },

    onTournamentPlayerReconnected: (msg) => {
      set((state) => {
        const { [msg.playerId]: _, ...rest } = state.disconnectedPlayers
        return { disconnectedPlayers: rest }
      })
    },
  }
}
