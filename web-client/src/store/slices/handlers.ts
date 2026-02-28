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
function getEventPlayerId(event: { type: string; playerId?: string; casterId?: string; controllerId?: string; attackingPlayerId?: string; viewingPlayerId?: string; revealingPlayerId?: string; activePlayerId?: string; newControllerId?: string }): EntityId | null {
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
    case 'turnedFaceUp': return event.controllerId as EntityId
    case 'coinFlipped': return event.playerId as EntityId
    case 'turnChanged': return event.activePlayerId as EntityId
    case 'permanentsSacrificed': return event.playerId as EntityId
    case 'cardCycled': return event.playerId as EntityId
    case 'libraryShuffled': return event.playerId as EntityId
    case 'controlChanged': return event.newControllerId as EntityId
    default: return null
  }
}

function getEventLogType(eventType: string): 'action' | 'turn' | 'combat' | 'system' {
  switch (eventType) {
    case 'turnChanged': return 'turn'
    case 'creatureAttacked':
    case 'creatureBlocked': return 'combat'
    case 'abilityFizzled':
    case 'gameEnded':
    case 'playerLost': return 'system'
    default: return 'action'
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
      localStorage.setItem('argentum-token', msg.token)
      set({
        connectionStatus: 'connected',
        playerId: entityId(msg.playerId),
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

      // Clear spectating state — active game takes priority
      set({ spectatingState: null })

      // Load persisted stop overrides and send to server
      try {
        const saved = localStorage.getItem('argentum-stop-overrides')
        if (saved) {
          const parsed = JSON.parse(saved) as { myTurnStops: string[]; opponentTurnStops: string[] }
          if (parsed.myTurnStops?.length || parsed.opponentTurnStops?.length) {
            set({
              stopOverrides: parsed as { myTurnStops: import('../../types').Step[]; opponentTurnStops: import('../../types').Step[] },
            })
            getWebSocket()?.send({
              type: 'setStopOverrides' as const,
              myTurnStops: parsed.myTurnStops,
              opponentTurnStops: parsed.opponentTurnStops,
            })
          }
        }
      } catch { /* ignore invalid localStorage data */ }

      // Show match intro animation
      const playerName = localStorage.getItem('argentum-player-name') ?? 'You'
      const { tournamentState, playerId } = get()
      let round: number | undefined
      let playerRecord: string | undefined
      let opponentRecord: string | undefined
      if (tournamentState && playerId) {
        round = tournamentState.currentRound
        const playerStanding = tournamentState.standings.find((s) => s.playerId === playerId)
        const opponentStanding = tournamentState.standings.find((s) => s.playerName === msg.opponentName)
        if (playerStanding) {
          playerRecord = `${playerStanding.wins}-${playerStanding.losses}${playerStanding.draws > 0 ? `-${playerStanding.draws}` : ''}`
        }
        if (opponentStanding) {
          opponentRecord = `${opponentStanding.wins}-${opponentStanding.losses}${opponentStanding.draws > 0 ? `-${opponentStanding.draws}` : ''}`
        }
      }
      set({
        matchIntro: {
          playerName,
          opponentName: msg.opponentName,
          ...(round != null ? { round } : {}),
          ...(playerRecord != null ? { playerRecord } : {}),
          ...(opponentRecord != null ? { opponentRecord } : {}),
        },
      })

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
      const { playerId, addDrawAnimation, addDamageAnimation, addRevealAnimation, addCoinFlipAnimation } = get()

      // Check for hand reveal events
      const handLookedAtEvent = msg.events.find(
        (e) => e.type === 'handLookedAt'
      ) as { type: 'handLookedAt'; cardIds: readonly EntityId[] } | undefined

      const handRevealedEvent = msg.events.find(
        (e) => e.type === 'handRevealed' && (e as { revealingPlayerId: EntityId }).revealingPlayerId !== playerId
      ) as { type: 'handRevealed'; cardIds: readonly EntityId[] } | undefined

      const cardsRevealedEvent = msg.events.find(
        (e) => e.type === 'cardsRevealed'
      ) as { type: 'cardsRevealed'; revealingPlayerId: EntityId; cardIds: readonly EntityId[]; cardNames: readonly string[]; imageUris: readonly (string | null)[]; source: string | null } | undefined

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

      // Process morph face-up events for reveal animations
      const turnedFaceUpEvents = msg.events.filter((e) => e.type === 'turnedFaceUp') as {
        type: 'turnedFaceUp'
        cardId: EntityId
        cardName: string
        controllerId: EntityId
      }[]

      turnedFaceUpEvents.forEach((event, index) => {
        const isOpponent = event.controllerId !== playerId
        const card = msg.state.cards[event.cardId]
        addRevealAnimation({
          id: `reveal-${event.cardId}-${Date.now()}-${index}`,
          cardName: event.cardName,
          imageUri: card?.imageUri ?? null,
          isOpponent,
          startTime: Date.now() + index * 200,
        })
      })

      // Process coin flip events for animations
      const coinFlipEvents = msg.events.filter((e) => e.type === 'coinFlipped') as {
        type: 'coinFlipped'
        playerId: EntityId
        won: boolean
        sourceId: EntityId
        sourceName: string
      }[]

      coinFlipEvents.forEach((event, index) => {
        const isOpponent = event.playerId !== playerId
        addCoinFlipAnimation({
          id: `coin-${event.sourceId}-${Date.now()}-${index}`,
          sourceName: event.sourceName,
          won: isOpponent ? !event.won : event.won,
          isOpponent,
          startTime: Date.now() + index * 200,
        })
      })

      // Sync stop overrides from server echo
      const serverOverrides = msg.stopOverrides
        ? {
            myTurnStops: msg.stopOverrides.myTurnStops as unknown as import('../../types').Step[],
            opponentTurnStops: msg.stopOverrides.opponentTurnStops as unknown as import('../../types').Step[],
          }
        : undefined

      // Sync priority mode from server echo
      const serverPriorityMode = msg.priorityMode ?? undefined

      set((state) => ({
        gameState: msg.state,
        legalActions: msg.legalActions,
        pendingDecision: msg.pendingDecision ?? null,
        opponentDecisionStatus: msg.opponentDecisionStatus ?? null,
        nextStopPoint: msg.nextStopPoint ?? null,
        undoAvailable: msg.undoAvailable ?? false,
        ...(serverPriorityMode ? { priorityMode: serverPriorityMode, fullControl: serverPriorityMode === 'fullControl' } : {}),
        ...(serverOverrides ? { stopOverrides: serverOverrides } : {}),
        pendingEvents: [...state.pendingEvents, ...msg.events],
        eventLog: (msg.state.gameLog ?? []).map((e) => ({
          description: e.description,
          playerId: getEventPlayerId(e as { type: string; playerId?: string; casterId?: string; controllerId?: string; attackingPlayerId?: string; viewingPlayerId?: string; revealingPlayerId?: string; activePlayerId?: string; newControllerId?: string }),
          timestamp: Date.now(),
          type: getEventLogType((e as { type: string }).type),
        })),
        waitingForOpponentMulligan: false,
        revealedHandCardIds: handLookedAtEvent?.cardIds ?? handRevealedEvent?.cardIds ?? state.revealedHandCardIds,
        revealedCardsInfo: cardsRevealedEvent
          ? { cardIds: cardsRevealedEvent.cardIds, cardNames: cardsRevealedEvent.cardNames, imageUris: cardsRevealedEvent.imageUris, source: cardsRevealedEvent.source, isYourReveal: cardsRevealedEvent.revealingPlayerId === playerId }
          : state.revealedCardsInfo,
        opponentBlockerAssignments: (msg.state.combat?.blockers?.length || !msg.state.combat) ? null : state.opponentBlockerAssignments,
      }))

      // Auto-initialize inline distribute state for DistributeDecision
      const decision = msg.pendingDecision
      if (decision?.type === 'DistributeDecision') {
        const initial: Record<string, number> = {}
        for (const targetId of decision.targets) {
          initial[targetId] = decision.minPerTarget
        }
        get().initDistribute({
          decisionId: decision.id,
          prompt: decision.prompt,
          totalAmount: decision.totalAmount,
          targets: decision.targets,
          minPerTarget: decision.minPerTarget,
          ...(decision.maxPerTarget ? { maxPerTarget: decision.maxPerTarget } : {}),
          distribution: initial,
        })
      } else if (!decision && get().distributeState) {
        // Clear distribute state when decision goes away
        get().clearDistribute()
      }
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
          isOnThePlay: msg.isOnThePlay,
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
          isOnThePlay: state.mulliganState?.isOnThePlay ?? false,
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

      set((state) => {
        // Preserve 'submitted' phase if already set, or if tournament state exists (deck must have been submitted)
        const existingPhase = state.deckBuildingState?.phase
        const phase = existingPhase === 'submitted' || state.tournamentState ? 'submitted' : 'building'

        return {
          deckBuildingState: {
            phase,
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
        }
      })
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
      // Add picked cards immediately for instant feedback.
      // The server's authoritative list in DraftPackReceived will replace this.
      set((state) => {
        if (!state.lobbyState?.draftState) return state

        const pickedCards = state.lobbyState.draftState.currentPack.filter((c) =>
          msg.cardNames.includes(c.name)
        )

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
          ? { ...state.lobbyState, state: 'DECK_BUILDING', draftState: null, winstonDraftState: null }
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
    // Winston Draft handlers
    // ========================================================================
    onWinstonDraftState: (msg) => {
      set((state) => ({
        lobbyState: state.lobbyState
          ? {
              ...state.lobbyState,
              winstonDraftState: {
                isYourTurn: msg.isYourTurn,
                activePlayerName: msg.activePlayerName,
                currentPileIndex: msg.currentPileIndex,
                pileSizes: msg.pileSizes,
                mainDeckRemaining: msg.mainDeckRemaining,
                currentPileCards: msg.currentPileCards,
                pickedCards: msg.pickedCards,
                totalPickedByOpponent: msg.totalPickedByOpponent,
                lastAction: msg.lastAction,
                timeRemaining: msg.timeRemainingSeconds,
              },
            }
          : null,
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
          winstonDraftState: null,
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
          draftState: msg.state === 'DRAFTING' && msg.settings.format === 'DRAFT' ? (lobbyState?.draftState ?? null) : null,
          winstonDraftState: msg.state === 'DRAFTING' && msg.settings.format === 'WINSTON_DRAFT' ? (lobbyState?.winstonDraftState ?? null) : null,
        },
        // Update deck building phase during DECK_BUILDING or TOURNAMENT_ACTIVE
        // This allows returning to deck building after unsubmitting during tournament
        deckBuildingState:
          state.deckBuildingState && (msg.state === 'DECK_BUILDING' || msg.state === 'TOURNAMENT_ACTIVE')
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
      // NOTE: Don't clear deckBuildingState - we allow returning to deck building
      // until the first match starts
      set((state) => ({
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
        // Keep deckBuildingState but ensure phase is 'submitted'
        deckBuildingState: state.deckBuildingState
          ? { ...state.deckBuildingState, phase: 'submitted' }
          : null,
      }))
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
              activeMatches: [], // Clear - player is now in a game
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
      set((state) => {
        // Don't clear game state if we're in an active game (no gameOverState yet)
        const inActiveGame = state.gameState != null && state.gameOverState == null
        return {
          tournamentState: state.tournamentState
            ? {
                ...state.tournamentState,
                currentRound: msg.round,
                standings: msg.standings,
                lastRoundResults: msg.results,
                ...(inActiveGame ? {} : {
                  currentMatchGameSessionId: null,
                  currentMatchOpponentName: null,
                }),
                isBye: false,
                isComplete: msg.isTournamentComplete ?? false,
                readyPlayerIds: [],
                nextOpponentName: msg.nextOpponentName ?? null,
                nextRoundHasBye: msg.nextRoundHasBye ?? false,
                activeMatches: [], // Clear - round is over, no active matches
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

    onMatchComplete: (msg) => {
      set((state) => {
        // Don't clear game state if we're in an active game (no gameOverState yet)
        const inActiveGame = state.gameState != null && state.gameOverState == null
        return {
          tournamentState: state.tournamentState
            ? {
                ...state.tournamentState,
                currentRound: msg.round,
                standings: msg.standings,
                lastRoundResults: msg.results.length > 0 ? msg.results : state.tournamentState.lastRoundResults,
                ...(inActiveGame ? {} : {
                  currentMatchGameSessionId: null,
                  currentMatchOpponentName: null,
                }),
                isBye: false,
                isComplete: msg.isTournamentComplete ?? false,
                readyPlayerIds: [],
                nextOpponentName: msg.nextOpponentName ?? null,
                nextRoundHasBye: msg.nextRoundHasBye ?? false,
                activeMatches: [],
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

    onPlayerReadyForRound: (msg) => {
      set((state) => ({
        tournamentState: state.tournamentState
          ? { ...state.tournamentState, readyPlayerIds: msg.readyPlayerIds }
          : null,
      }))
    },

    onTournamentComplete: (msg) => {
      set((state) => {
        const inActiveGame = state.gameState != null && state.gameOverState == null
        return {
          tournamentState: state.tournamentState
            ? {
                ...state.tournamentState,
                isComplete: true,
                finalStandings: msg.finalStandings,
                standings: msg.finalStandings,
                ...(inActiveGame ? {} : {
                  currentMatchGameSessionId: null,
                  currentMatchOpponentName: null,
                }),
              }
            : null,
          ...(inActiveGame ? {} : {
            gameState: state.gameOverState ? state.gameState : null,
          }),
        }
      })
    },

    // ========================================================================
    // Spectating handlers
    // ========================================================================
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
