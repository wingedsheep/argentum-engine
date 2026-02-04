/**
 * Message handlers for server messages.
 * These update the store in response to server events.
 */
import type { MessageHandlers } from '../../network/messageHandlers'
import type { GameStore, EntityId, LobbyState } from './types'
import { entityId, createJoinLobbyMessage } from '../../types'
import { trackEvent, setInGame } from '../../utils/analytics'
import {
  getWebSocket,
  clearLobbyId,
  clearDeckState,
  saveLobbyId,
  loadLobbyId,
  loadDeckState,
} from './shared'

type SetState = (
  partial: GameStore | Partial<GameStore> | ((state: GameStore) => GameStore | Partial<GameStore>),
  replace?: false
) => void

type GetState = () => GameStore

/**
 * Extract the relevant player ID from an event for log coloring.
 */
function getEventPlayerId(event: { type: string; playerId?: string; casterId?: string; controllerId?: string; attackingPlayerId?: string; viewingPlayerId?: string; revealingPlayerId?: string }): EntityId | null {
  switch (event.type) {
    case 'lifeChanged': return event.playerId as EntityId
    case 'cardDrawn': return event.playerId as EntityId
    case 'cardDiscarded': return event.playerId as EntityId
    case 'manaAdded': return event.playerId as EntityId
    case 'playerLost': return event.playerId as EntityId
    case 'spellCast': return event.casterId as EntityId
    case 'permanentEntered': return event.controllerId as EntityId
    case 'creatureAttacked': return event.attackingPlayerId as EntityId
    case 'handLookedAt': return event.viewingPlayerId as EntityId
    case 'handRevealed': return event.revealingPlayerId as EntityId
    case 'cardsRevealed': return event.revealingPlayerId as EntityId
    default: return null
  }
}

/**
 * Create message handlers that update the store.
 */
export function createMessageHandlers(set: SetState, get: GetState): MessageHandlers {
  return {
    // ========================================================================
    // Connection handlers
    // ========================================================================
    onConnected: (msg) => {
      sessionStorage.setItem('argentum-token', msg.token)
      set({
        connectionStatus: 'connected',
        playerId: entityId(msg.playerId),
      })
      clearLobbyId()
    },

    onReconnected: (msg) => {
      sessionStorage.setItem('argentum-token', msg.token)
      const updates: Partial<GameStore> = {
        connectionStatus: 'connected',
        playerId: entityId(msg.playerId),
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

    // ========================================================================
    // Gameplay handlers
    // ========================================================================
    onGameCreated: (msg) => {
      set({ sessionId: msg.sessionId })
    },

    onGameStarted: (msg) => {
      trackEvent('game_started', { opponent_name: msg.opponentName })
      setInGame(true)
      set({
        opponentName: msg.opponentName,
        mulliganState: null,
        deckBuildingState: null,
      })
    },

    onGameCancelled: () => {
      trackEvent('game_cancelled_by_server')
      setInGame(false)
      set({
        sessionId: null,
        opponentName: null,
        gameState: null,
        legalActions: [],
        mulliganState: null,
        deckBuildingState: null,
      })
    },

    onStateUpdate: (msg) => {
      const { playerId, addDrawAnimation, addDamageAnimation } = get()

      // Check for hand reveal events
      const handLookedAtEvent = msg.events.find(
        (e) => e.type === 'handLookedAt'
      ) as { type: 'handLookedAt'; cardIds: readonly EntityId[] } | undefined

      const handRevealedEvent = msg.events.find(
        (e) => e.type === 'handRevealed' && (e as { revealingPlayerId: EntityId }).revealingPlayerId !== playerId
      ) as { type: 'handRevealed'; cardIds: readonly EntityId[] } | undefined

      const cardsRevealedEvent = msg.events.find(
        (e) => e.type === 'cardsRevealed' && (e as { revealingPlayerId: EntityId }).revealingPlayerId !== playerId
      ) as { type: 'cardsRevealed'; cardIds: readonly EntityId[]; cardNames: readonly string[]; imageUris: readonly (string | null)[]; source: string | null } | undefined

      // Process card draw events for animations
      const cardDrawnEvents = msg.events.filter((e) => e.type === 'cardDrawn') as {
        type: 'cardDrawn'
        playerId: EntityId
        cardId: EntityId
        cardName: string | null
      }[]

      cardDrawnEvents.forEach((event, index) => {
        const isOpponent = event.playerId !== playerId
        const card = msg.state.cards[event.cardId]
        addDrawAnimation({
          id: `draw-${event.cardId}-${Date.now()}-${index}`,
          cardId: event.cardId,
          cardName: event.cardName,
          imageUri: card?.imageUri ?? null,
          isOpponent,
          startTime: Date.now() + index * 100,
        })
      })

      // Process life changed events for animations
      const lifeChangedEvents = msg.events.filter(
        (e) => e.type === 'lifeChanged' && (e as { change: number }).change !== 0
      ) as {
        type: 'lifeChanged'
        playerId: EntityId
        change: number
      }[]

      const playerLifeChanges = new Map<EntityId, { damage: number; lifeGain: number }>()
      lifeChangedEvents.forEach((event) => {
        const current = playerLifeChanges.get(event.playerId) ?? { damage: 0, lifeGain: 0 }
        if (event.change < 0) {
          current.damage += Math.abs(event.change)
        } else {
          current.lifeGain += event.change
        }
        playerLifeChanges.set(event.playerId, current)
      })

      let animIndex = 0
      playerLifeChanges.forEach((changes, targetPlayerId) => {
        if (changes.damage > 0) {
          addDamageAnimation({
            id: `life-${targetPlayerId}-${Date.now()}-damage`,
            targetId: targetPlayerId,
            targetIsPlayer: true,
            amount: changes.damage,
            isLifeGain: false,
            startTime: Date.now() + animIndex * 50,
          })
          animIndex++
        }
        if (changes.lifeGain > 0) {
          addDamageAnimation({
            id: `life-${targetPlayerId}-${Date.now()}-gain`,
            targetId: targetPlayerId,
            targetIsPlayer: true,
            amount: changes.lifeGain,
            isLifeGain: true,
            startTime: Date.now() + animIndex * 50,
          })
          animIndex++
        }
      })

      set((state) => ({
        gameState: msg.state,
        legalActions: msg.legalActions,
        pendingDecision: msg.pendingDecision ?? null,
        opponentDecisionStatus: msg.opponentDecisionStatus ?? null,
        nextStopPoint: msg.nextStopPoint ?? null,
        pendingEvents: [...state.pendingEvents, ...msg.events],
        eventLog: (msg.state.gameLog ?? []).map((e) => ({
          description: e.description,
          playerId: getEventPlayerId(e as { type: string; playerId?: string; casterId?: string; controllerId?: string; attackingPlayerId?: string; viewingPlayerId?: string; revealingPlayerId?: string }),
          timestamp: Date.now(),
        })),
        waitingForOpponentMulligan: false,
        revealedHandCardIds: handLookedAtEvent?.cardIds ?? handRevealedEvent?.cardIds ?? state.revealedHandCardIds,
        revealedCardsInfo: cardsRevealedEvent
          ? { cardIds: cardsRevealedEvent.cardIds, cardNames: cardsRevealedEvent.cardNames, imageUris: cardsRevealedEvent.imageUris, source: cardsRevealedEvent.source }
          : state.revealedCardsInfo,
        opponentBlockerAssignments: (msg.state.combat?.blockers?.length || !msg.state.combat) ? null : state.opponentBlockerAssignments,
      }))
    },

    onMulliganDecision: (msg) => {
      set({
        mulliganState: {
          phase: 'deciding',
          hand: msg.hand,
          mulliganCount: msg.mulliganCount,
          cardsToPutOnBottom: msg.cardsToPutOnBottom,
          selectedCards: [],
          cards: msg.cards || {},
        },
      })
    },

    onChooseBottomCards: (msg) => {
      set((state) => ({
        mulliganState: {
          phase: 'choosingBottomCards',
          hand: msg.hand,
          mulliganCount: 0,
          cardsToPutOnBottom: msg.cardsToPutOnBottom,
          selectedCards: [],
          cards: state.mulliganState?.cards || {},
        },
      }))
    },

    onMulliganComplete: () => {
      set({ mulliganState: null })
    },

    onWaitingForOpponentMulligan: () => {
      set({ waitingForOpponentMulligan: true })
    },

    onGameOver: (msg) => {
      const { playerId } = get()
      const result: 'win' | 'lose' | 'draw' =
        msg.winnerId === null ? 'draw' : msg.winnerId === playerId ? 'win' : 'lose'
      trackEvent('game_over', { result, reason: msg.reason })
      setInGame(false)
      set({
        gameOverState: {
          winnerId: msg.winnerId,
          reason: msg.reason,
          result,
          message: msg.message,
        },
      })
    },

    onError: (msg) => {
      if (msg.code === 'GAME_NOT_FOUND' || msg.message?.toLowerCase().includes('lobby')) {
        clearLobbyId()
      }
      set({
        lastError: {
          code: msg.code,
          message: msg.message,
          timestamp: Date.now(),
        },
      })
    },

    // ========================================================================
    // Draft/Sealed handlers
    // ========================================================================
    onSealedGameCreated: (msg) => {
      set({
        sessionId: msg.sessionId,
        deckBuildingState: {
          phase: 'waiting',
          setCode: msg.setCode,
          setName: msg.setName,
          cardPool: [],
          basicLands: [],
          deck: [],
          landCounts: {
            Plains: 0,
            Island: 0,
            Swamp: 0,
            Mountain: 0,
            Forest: 0,
          },
          opponentReady: false,
        },
      })
    },

    onSealedPoolGenerated: (msg) => {
      trackEvent('sealed_pool_opened', {
        set_code: msg.setCode,
        set_name: msg.setName,
        pool_size: msg.cardPool.length,
      })
      const savedState = loadDeckState()

      set((state) => ({
        deckBuildingState: {
          phase: 'building',
          setCode: msg.setCode,
          setName: msg.setName,
          cardPool: msg.cardPool,
          basicLands: msg.basicLands,
          deck: state.deckBuildingState?.deck ?? savedState?.deck ?? [],
          landCounts: state.deckBuildingState?.landCounts ?? savedState?.landCounts ?? {
            Plains: 0,
            Island: 0,
            Swamp: 0,
            Mountain: 0,
            Forest: 0,
          },
          opponentReady: state.deckBuildingState?.opponentReady ?? false,
        },
      }))
    },

    onOpponentDeckSubmitted: () => {
      set((state) => ({
        deckBuildingState: state.deckBuildingState
          ? { ...state.deckBuildingState, opponentReady: true }
          : null,
      }))
    },

    onWaitingForOpponent: () => {
      // Already in submitted state
    },

    onDeckSubmitted: () => {
      set((state) => ({
        deckBuildingState: state.deckBuildingState
          ? { ...state.deckBuildingState, phase: 'submitted' }
          : null,
      }))
    },

    onDraftPackReceived: (msg) => {
      set((state) => ({
        lobbyState: state.lobbyState
          ? {
              ...state.lobbyState,
              draftState: {
                currentPack: msg.cards,
                packNumber: msg.packNumber,
                pickNumber: msg.pickNumber,
                pickedCards: msg.pickedCards ?? state.lobbyState.draftState?.pickedCards ?? [],
                timeRemaining: msg.timeRemainingSeconds,
                passDirection: msg.passDirection,
                picksPerRound: msg.picksPerRound,
                waitingForPlayers: [],
              },
            }
          : null,
      }))
    },

    onDraftPickMade: (msg) => {
      set((state) => ({
        lobbyState: state.lobbyState?.draftState
          ? {
              ...state.lobbyState,
              draftState: { ...state.lobbyState.draftState, waitingForPlayers: msg.waitingForPlayers },
            }
          : state.lobbyState,
      }))
    },

    onDraftPickConfirmed: (msg) => {
      set((state) => {
        if (!state.lobbyState?.draftState) return state
        const pickedCards = state.lobbyState.draftState.currentPack.filter((c) =>
          msg.cardNames.includes(c.name)
        )
        if (pickedCards.length === 0) return state

        return {
          lobbyState: {
            ...state.lobbyState,
            draftState: {
              ...state.lobbyState.draftState,
              currentPack: [],
              pickedCards: [...state.lobbyState.draftState.pickedCards, ...pickedCards],
            },
          },
        }
      })
    },

    onDraftComplete: (msg) => {
      trackEvent('draft_complete', { picked_cards: msg.pickedCards.length })

      set((state) => ({
        lobbyState: state.lobbyState
          ? { ...state.lobbyState, state: 'DECK_BUILDING', draftState: null }
          : null,
        deckBuildingState: {
          phase: 'building',
          setCode: state.lobbyState?.settings.setCodes[0] ?? '',
          setName: state.lobbyState?.settings.setNames[0] ?? '',
          cardPool: msg.pickedCards,
          basicLands: msg.basicLands,
          deck: [],
          landCounts: { Plains: 0, Island: 0, Swamp: 0, Mountain: 0, Forest: 0 },
          opponentReady: false,
        },
      }))
    },

    onDraftTimerUpdate: (msg) => {
      set((state) => ({
        lobbyState: state.lobbyState?.draftState
          ? {
              ...state.lobbyState,
              draftState: { ...state.lobbyState.draftState, timeRemaining: msg.secondsRemaining },
            }
          : state.lobbyState,
      }))
    },

    // ========================================================================
    // Lobby handlers
    // ========================================================================
    onLobbyCreated: (msg) => {
      saveLobbyId(msg.lobbyId)
      set({
        lobbyState: {
          lobbyId: msg.lobbyId,
          state: 'WAITING_FOR_PLAYERS',
          players: [],
          settings: { setCodes: [], setNames: [], availableSets: [], format: 'SEALED', boosterCount: 6, maxPlayers: 8, pickTimeSeconds: 45, picksPerRound: 1, gamesPerMatch: 1 },
          isHost: true,
          draftState: null,
        },
      })
    },

    onLobbyUpdate: (msg) => {
      const { playerId, lobbyState } = get()
      saveLobbyId(msg.lobbyId)

      const currentPlayer = msg.players.find((p) => p.playerId === playerId)
      const isDeckSubmitted = currentPlayer?.deckSubmitted ?? false

      set((state) => ({
        lobbyState: {
          lobbyId: msg.lobbyId,
          state: msg.state as LobbyState['state'],
          players: msg.players,
          settings: msg.settings,
          isHost: msg.isHost,
          draftState: msg.state === 'DRAFTING' ? (lobbyState?.draftState ?? null) : null,
        },
        deckBuildingState:
          state.deckBuildingState && msg.state === 'DECK_BUILDING'
            ? { ...state.deckBuildingState, phase: isDeckSubmitted ? 'submitted' : 'building' }
            : state.deckBuildingState,
      }))
    },

    onLobbyStopped: () => {
      clearDeckState()
      clearLobbyId()
      set({ lobbyState: null, deckBuildingState: null })
    },

    // ========================================================================
    // Tournament handlers
    // ========================================================================
    onTournamentStarted: (msg) => {
      clearDeckState()
      set({
        tournamentState: {
          lobbyId: msg.lobbyId,
          totalRounds: msg.totalRounds,
          currentRound: 0,
          standings: msg.standings,
          lastRoundResults: null,
          currentMatchGameSessionId: null,
          currentMatchOpponentName: null,
          isBye: false,
          isComplete: false,
          finalStandings: null,
          readyPlayerIds: [],
          nextOpponentName: msg.nextOpponentName ?? null,
          nextRoundHasBye: msg.nextRoundHasBye ?? false,
        },
        deckBuildingState: null,
      })
    },

    onTournamentMatchStarting: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? {
              ...state.tournamentState,
              currentRound: msg.round,
              currentMatchGameSessionId: msg.gameSessionId,
              currentMatchOpponentName: msg.opponentName,
              isBye: false,
              readyPlayerIds: [],
              nextOpponentName: null,
              nextRoundHasBye: false,
            }
          : null,
        sessionId: msg.gameSessionId,
        opponentName: msg.opponentName,
      }))
    },

    onTournamentBye: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? {
              ...state.tournamentState,
              currentRound: msg.round,
              currentMatchGameSessionId: null,
              currentMatchOpponentName: null,
              isBye: true,
            }
          : null,
      }))
    },

    onRoundComplete: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? {
              ...state.tournamentState,
              currentRound: msg.round,
              standings: msg.standings,
              lastRoundResults: msg.results,
              currentMatchGameSessionId: null,
              currentMatchOpponentName: null,
              isBye: false,
              isComplete: msg.isTournamentComplete ?? false,
              readyPlayerIds: [],
              nextOpponentName: msg.nextOpponentName ?? null,
              nextRoundHasBye: msg.nextRoundHasBye ?? false,
            }
          : null,
        gameState: null,
        gameOverState: null,
        mulliganState: null,
        waitingForOpponentMulligan: false,
        legalActions: [],
      }))
    },

    onPlayerReadyForRound: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? { ...state.tournamentState, readyPlayerIds: msg.readyPlayerIds }
          : null,
      }))
    },

    onTournamentComplete: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? {
              ...state.tournamentState,
              isComplete: true,
              finalStandings: msg.finalStandings,
              standings: msg.finalStandings,
              currentMatchGameSessionId: null,
              currentMatchOpponentName: null,
            }
          : null,
        gameState: null,
        gameOverState: null,
      }))
    },

    // ========================================================================
    // Spectating handlers
    // ========================================================================
    onActiveMatches: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? {
              ...state.tournamentState,
              activeMatches: msg.matches,
              standings: msg.standings,
              currentMatchGameSessionId: null,
              currentMatchOpponentName: null,
            }
          : null,
        gameState: null,
        gameOverState: null,
        mulliganState: null,
        waitingForOpponentMulligan: false,
        legalActions: [],
      }))
    },

    onSpectatorStateUpdate: (msg) => {
      const { spectatingState: prevState, addDamageAnimation } = get()

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

      set({
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
      })
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
    },

    // ========================================================================
    // Combat UI handlers
    // ========================================================================
    onOpponentBlockerAssignments: (msg) => {
      set({ opponentBlockerAssignments: msg.assignments })
    },
  }
}
