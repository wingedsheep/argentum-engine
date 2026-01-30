import { trackEvent } from '../utils/analytics'
import { create } from 'zustand'
import { subscribeWithSelector } from 'zustand/middleware'
import type {
  EntityId,
  ClientGameState,
  ClientCard,
  ClientEvent,
  GameAction,
  LegalActionInfo,
  GameOverReason,
  ErrorCode,
  PendingDecision,
} from '../types'
import type {
  LobbyPlayerInfo,
  LobbySettings,
  PlayerStandingInfo,
  MatchResultInfo,
} from '../types'
import {
  entityId,
  createConnectMessage,
  createCreateGameMessage,
  createJoinGameMessage,
  createSubmitActionMessage,
  createKeepHandMessage,
  createMulliganMessage,
  createChooseBottomCardsMessage,
  createConcedeMessage,
  createCancelGameMessage,
  createCreateSealedGameMessage,
  createJoinSealedGameMessage,
  createSubmitSealedDeckMessage,
  createCreateSealedLobbyMessage,
  createJoinLobbyMessage,
  createStartSealedLobbyMessage,
  createLeaveLobbyMessage,
  createUpdateLobbySettingsMessage,
  createReadyForNextRoundMessage,
  SealedCardInfo,
} from '../types'
import { GameWebSocket, getWebSocketUrl, type ConnectionStatus } from '../network/websocket'
import { handleServerMessage, createLoggingHandlers, type MessageHandlers } from '../network/messageHandlers'

/**
 * Mulligan card info.
 */
export interface MulliganCardInfo {
  name: string
  imageUri: string | null
}

/**
 * Mulligan state during the mulligan phase.
 */
export interface MulliganState {
  phase: 'deciding' | 'choosingBottomCards'
  hand: readonly EntityId[]
  mulliganCount: number
  cardsToPutOnBottom: number
  selectedCards: readonly EntityId[]
  cards: Record<EntityId, MulliganCardInfo>
}

/**
 * Targeting state when selecting targets for a spell/ability.
 */
export interface TargetingState {
  action: GameAction
  validTargets: readonly EntityId[]
  selectedTargets: readonly EntityId[]
  minTargets: number
  maxTargets: number
  /** If set, this targeting phase is for sacrifice selection, not spell targets */
  isSacrificeSelection?: boolean
  /** The original action info, used to chain sacrifice -> spell targeting */
  pendingActionInfo?: LegalActionInfo
  /** Current target requirement index for multi-target spells (0-indexed) */
  currentRequirementIndex?: number
  /** All selected targets for each requirement (for multi-target spells) */
  allSelectedTargets?: readonly (readonly EntityId[])[]
  /** All target requirements (for multi-target spells) */
  targetRequirements?: LegalActionInfo['targetRequirements']
}

/**
 * Combat mode state for declaring attackers or blockers.
 */
export interface CombatState {
  mode: 'declareAttackers' | 'declareBlockers'
  /** Selected attackers (creature IDs) */
  selectedAttackers: readonly EntityId[]
  /** Blocker assignments: blocker ID -> attacker ID */
  blockerAssignments: Record<EntityId, EntityId>
  /** Valid creatures that can participate (attackers or blockers depending on mode) */
  validCreatures: readonly EntityId[]
  /** For blockers mode: creatures that are attacking */
  attackingCreatures: readonly EntityId[]
  /** For blockers mode: attackers that must be blocked by all creatures able to block them */
  mustBeBlockedAttackers: readonly EntityId[]
}

/**
 * Game over state when the game has ended.
 */
export interface GameOverState {
  winnerId: EntityId | null
  reason: GameOverReason
  isWinner: boolean
}

/**
 * Error state for displaying errors to the user.
 */
export interface ErrorState {
  code: ErrorCode
  message: string
  timestamp: number
}

/**
 * X cost selection state when casting spells with X in their mana cost.
 */
export interface XSelectionState {
  actionInfo: LegalActionInfo
  cardName: string
  minX: number
  maxX: number
  selectedX: number
}

/**
 * Deck building state during sealed draft.
 */
export interface DeckBuildingState {
  phase: 'waiting' | 'building' | 'submitted'
  setCode: string
  setName: string
  cardPool: readonly SealedCardInfo[]
  basicLands: readonly SealedCardInfo[]
  /** Card names currently in the deck */
  deck: readonly string[]
  /** Basic land counts by land name */
  landCounts: Record<string, number>
  opponentReady: boolean
}

/**
 * Lobby state for sealed lobbies.
 */
export interface LobbyState {
  lobbyId: string
  state: string
  players: readonly LobbyPlayerInfo[]
  settings: LobbySettings
  isHost: boolean
}

/**
 * Tournament state.
 */
export interface TournamentState {
  lobbyId: string
  totalRounds: number
  currentRound: number
  standings: readonly PlayerStandingInfo[]
  lastRoundResults: readonly MatchResultInfo[] | null
  currentMatchGameSessionId: string | null
  currentMatchOpponentName: string | null
  isBye: boolean
  isComplete: boolean
  finalStandings: readonly PlayerStandingInfo[] | null
}

/**
 * Main game store interface.
 */
/**
 * A log entry for the game log panel.
 */
export interface LogEntry {
  description: string
  playerId: EntityId | null
  timestamp: number
}

export interface GameStore {
  // Connection state
  connectionStatus: ConnectionStatus
  playerId: EntityId | null
  sessionId: string | null
  opponentName: string | null

  // Game state (from server)
  gameState: ClientGameState | null
  legalActions: readonly LegalActionInfo[]
  pendingDecision: PendingDecision | null

  // Mulligan state
  mulliganState: MulliganState | null
  waitingForOpponentMulligan: boolean

  // UI state (local only)
  selectedCardId: EntityId | null
  targetingState: TargetingState | null
  combatState: CombatState | null
  xSelectionState: XSelectionState | null
  hoveredCardId: EntityId | null
  draggingBlockerId: EntityId | null
  draggingCardId: EntityId | null
  revealedHandCardIds: readonly EntityId[] | null

  // Animation queue
  pendingEvents: readonly ClientEvent[]

  // Event log
  eventLog: readonly LogEntry[]

  // Game over state
  gameOverState: GameOverState | null

  // Error state
  lastError: ErrorState | null

  // Deck building state (sealed draft)
  deckBuildingState: DeckBuildingState | null

  // Lobby state
  lobbyState: LobbyState | null

  // Tournament state
  tournamentState: TournamentState | null

  // Actions
  connect: (playerName: string) => void
  disconnect: () => void
  createGame: (deckList: Record<string, number>) => void
  joinGame: (sessionId: string, deckList: Record<string, number>) => void
  submitAction: (action: GameAction) => void
  submitDecision: (selectedCards: readonly EntityId[]) => void
  submitTargetsDecision: (selectedTargets: Record<number, readonly EntityId[]>) => void
  submitOrderedDecision: (orderedObjects: readonly EntityId[]) => void
  submitYesNoDecision: (choice: boolean) => void
  submitNumberDecision: (number: number) => void
  submitDistributeDecision: (distribution: Record<EntityId, number>) => void
  keepHand: () => void
  mulligan: () => void
  chooseBottomCards: (cardIds: readonly EntityId[]) => void
  concede: () => void
  cancelGame: () => void
  // Sealed draft actions
  createSealedGame: (setCode: string) => void
  joinSealedGame: (sessionId: string) => void
  addCardToDeck: (cardName: string) => void
  removeCardFromDeck: (cardName: string) => void
  setLandCount: (landType: string, count: number) => void
  submitSealedDeck: () => void

  // Lobby actions
  createSealedLobby: (setCode: string, boosterCount?: number, maxPlayers?: number) => void
  joinLobby: (lobbyId: string) => void
  startSealedLobby: () => void
  leaveLobby: () => void
  updateLobbySettings: (settings: { boosterCount?: number; maxPlayers?: number; gamesPerMatch?: number }) => void

  // Tournament actions
  readyForNextRound: () => void

  // UI actions
  selectCard: (cardId: EntityId | null) => void
  hoverCard: (cardId: EntityId | null) => void
  toggleMulliganCard: (cardId: EntityId) => void
  startTargeting: (state: TargetingState) => void
  addTarget: (targetId: EntityId) => void
  removeTarget: (targetId: EntityId) => void
  cancelTargeting: () => void
  confirmTargeting: () => void
  // Combat actions
  startCombat: (state: CombatState) => void
  toggleAttacker: (creatureId: EntityId) => void
  assignBlocker: (blockerId: EntityId, attackerId: EntityId) => void
  removeBlockerAssignment: (blockerId: EntityId) => void
  startDraggingBlocker: (blockerId: EntityId) => void
  stopDraggingBlocker: () => void
  startDraggingCard: (cardId: EntityId) => void
  stopDraggingCard: () => void
  confirmCombat: () => void
  cancelCombat: () => void
  clearCombat: () => void
  // X cost selection actions
  startXSelection: (state: XSelectionState) => void
  updateXValue: (x: number) => void
  cancelXSelection: () => void
  confirmXSelection: () => void
  returnToMenu: () => void
  clearError: () => void
  consumeEvent: () => ClientEvent | undefined
  showRevealedHand: (cardIds: readonly EntityId[]) => void
  dismissRevealedHand: () => void
  // Draw animation
  drawAnimations: readonly DrawAnimation[]
  addDrawAnimation: (animation: DrawAnimation) => void
  removeDrawAnimation: (id: string) => void
}

/**
 * A card draw animation.
 */
export interface DrawAnimation {
  id: string
  cardId: EntityId
  cardName: string | null
  imageUri: string | null
  isOpponent: boolean
  startTime: number
}

// WebSocket instance (singleton)
let ws: GameWebSocket | null = null

/**
 * Gets the top item on the stack (the most recently added spell/ability).
 */
function getTopOfStack(gameState: ClientGameState): ClientCard | null {
  const stackZone = gameState.zones.find((z) => z.zoneId.zoneType === 'STACK')
  if (!stackZone || !stackZone.cardIds || stackZone.cardIds.length === 0) {
    return null
  }
  // Stack is ordered with newest at end (last in, first out)
  const topId = stackZone.cardIds[stackZone.cardIds.length - 1]
  return topId ? (gameState.cards[topId] ?? null) : null
}

/**
 * Determines if we should auto-pass priority.
 * Auto-passes when:
 * - It's our priority (implied by receiving legalActions)
 * - The only meaningful legal actions are PassPriority and/or mana abilities
 *   (mana abilities without anything to spend them on are not useful)
 * - We're not in a main phase where player might want to hold priority (unless stack is non-empty)
 * - OR the top of the stack is our own spell/ability (we rarely want to respond to ourselves)
 *
 * This skips through steps like UNTAP, UPKEEP, DRAW, BEGIN_COMBAT, etc.
 * when there are no meaningful actions available.
 */
function shouldAutoPass(
  legalActions: readonly LegalActionInfo[],
  gameState: ClientGameState,
  playerId: EntityId
): boolean {
  // Must have priority
  if (gameState.priorityPlayerId !== playerId) {
    return false
  }

  // Must have at least PassPriority available
  const hasPassPriority = legalActions.some((a) => a.action.type === 'PassPriority')
  if (!hasPassPriority) {
    return false
  }

  // Auto-pass when responding to our own spell/ability on the stack
  // (99% of the time, players want their spell to resolve, not counter it themselves)
  const topOfStack = getTopOfStack(gameState)
  if (topOfStack && topOfStack.controllerId === playerId) {
    return true
  }

  // Check if there are any meaningful actions besides PassPriority
  // Meaningful actions include: PlayLand, CastSpell, non-mana ActivateAbility, etc.
  const hasMeaningfulActions = legalActions.some((a) => {
    const type = a.action.type
    // PassPriority is not considered "meaningful" for auto-pass
    if (type === 'PassPriority') return false
    // Mana abilities are not meaningful for auto-pass (they're always available)
    if (a.isManaAbility) return false
    // Everything else is meaningful
    return true
  })

  // If there are meaningful actions, don't auto-pass
  if (hasMeaningfulActions) {
    return false
  }

  // If there's something on the stack (opponent's spell/ability), STOP to let the player
  // see what was cast - even if they have no responses
  const stackZone = gameState.zones.find((z) => z.zoneId.zoneType === 'STACK')
  const stackHasItems = stackZone && stackZone.cardIds && stackZone.cardIds.length > 0
  if (stackHasItems) {
    return false
  }

  // Skip auto-pass during main phases when stack is empty - player might be thinking
  // about what to do or waiting to tap lands
  const step = gameState.currentStep
  if (step === 'PRECOMBAT_MAIN' || step === 'POSTCOMBAT_MAIN') {
    return false
  }

  // Auto-pass for all other steps when only PassPriority/mana abilities are available
  return true
}

/**
 * Extract the relevant player ID from an event for log coloring.
 */
function getEventPlayerId(event: ClientEvent): EntityId | null {
  switch (event.type) {
    case 'lifeChanged': return event.playerId
    case 'cardDrawn': return event.playerId
    case 'cardDiscarded': return event.playerId
    case 'manaAdded': return event.playerId
    case 'playerLost': return event.playerId
    case 'spellCast': return event.casterId
    case 'permanentEntered': return event.controllerId
    case 'creatureAttacked': return event.attackingPlayerId
    case 'handLookedAt': return event.viewingPlayerId
    case 'cardsRevealed': return event.revealingPlayerId
    default: return null
  }
}

/**
 * Main Zustand store for game state.
 */
export const useGameStore = create<GameStore>()(
  subscribeWithSelector((set, get) => {
    // Message handlers that update the store
    const handlers: MessageHandlers = {
      onConnected: (msg) => {
        // Store token for reconnection
        sessionStorage.setItem('argentum-token', msg.token)
        set({
          connectionStatus: 'connected',
          playerId: entityId(msg.playerId),
        })
      },

      onReconnected: (msg) => {
        sessionStorage.setItem('argentum-token', msg.token)
        const updates: Partial<GameStore> = {
          connectionStatus: 'connected',
          playerId: entityId(msg.playerId),
        }
        // Restore session context based on what the server tells us
        if (msg.context === 'game' && msg.contextId) {
          updates.sessionId = msg.contextId
        }
        set(updates)
        // The server will re-send appropriate state messages after reconnection
      },

      onGameCreated: (msg) => {
        set({ sessionId: msg.sessionId })
      },

      onGameStarted: (msg) => {
        trackEvent('game_started', { opponent_name: msg.opponentName })
        set({
          opponentName: msg.opponentName,
          mulliganState: null,
          deckBuildingState: null, // Clear deck building state when game starts
        })
      },

      onGameCancelled: () => {
        trackEvent('game_cancelled_by_server')
        // Return to menu state
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
        // Check for handLookedAt event and show the revealed hand overlay
        const handLookedAtEvent = msg.events.find(
          (e) => e.type === 'handLookedAt'
        ) as { type: 'handLookedAt'; cardIds: readonly EntityId[] } | undefined

        // Process cardDrawn events for draw animations
        const { playerId, addDrawAnimation } = get()
        const cardDrawnEvents = msg.events.filter((e) => e.type === 'cardDrawn') as {
          type: 'cardDrawn'
          playerId: EntityId
          cardId: EntityId
          cardName: string | null
        }[]

        // Create draw animations for each card drawn
        cardDrawnEvents.forEach((event, index) => {
          const isOpponent = event.playerId !== playerId
          const card = msg.state.cards[event.cardId]
          addDrawAnimation({
            id: `draw-${event.cardId}-${Date.now()}-${index}`,
            cardId: event.cardId,
            cardName: event.cardName,
            imageUri: card?.imageUri ?? null,
            isOpponent,
            startTime: Date.now() + index * 100, // Stagger multiple draws
          })
        })

        set((state) => ({
          gameState: msg.state,
          legalActions: msg.legalActions,
          pendingDecision: msg.pendingDecision ?? null,
          pendingEvents: [...state.pendingEvents, ...msg.events],
          eventLog: (msg.state.gameLog ?? []).map((e: ClientEvent) => ({
            description: e.description,
            playerId: getEventPlayerId(e),
            timestamp: Date.now(),
          })),
          waitingForOpponentMulligan: false,
          // Show revealed hand overlay if handLookedAt event received
          revealedHandCardIds: handLookedAtEvent?.cardIds ?? state.revealedHandCardIds,
        }))

        // Auto-pass when the only action available is PassPriority
        // This skips through steps with no meaningful player actions
        if (playerId && shouldAutoPass(msg.legalActions, msg.state, playerId)) {
          // Small delay to allow state to settle and prevent rapid-fire passes
          setTimeout(() => {
            const passAction = msg.legalActions.find((a) => a.action.type === 'PassPriority')
            if (passAction && ws) {
              ws.send(createSubmitActionMessage(passAction.action))
            }
          }, 50)
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
            // Preserve cards from the deciding phase
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
        trackEvent('game_over', {
          result: msg.winnerId === playerId ? 'win' : 'loss',
          reason: msg.reason,
        })
        set({
          gameOverState: {
            winnerId: msg.winnerId,
            reason: msg.reason,
            isWinner: msg.winnerId === playerId,
          },
        })
      },

      onError: (msg) => {
        set({
          lastError: {
            code: msg.code,
            message: msg.message,
            timestamp: Date.now(),
          },
        })
      },

      // Sealed draft handlers
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
        set((state) => ({
          deckBuildingState: {
            phase: 'building',
            setCode: msg.setCode,
            setName: msg.setName,
            cardPool: msg.cardPool,
            basicLands: msg.basicLands,
            // Preserve existing deck/lands if already set, otherwise initialize
            deck: state.deckBuildingState?.deck ?? [],
            landCounts: state.deckBuildingState?.landCounts ?? {
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
            ? {
                ...state.deckBuildingState,
                opponentReady: true,
              }
            : null,
        }))
      },

      onWaitingForOpponent: () => {
        // Already in submitted state, no additional update needed
      },

      onDeckSubmitted: () => {
        set((state) => ({
          deckBuildingState: state.deckBuildingState
            ? {
                ...state.deckBuildingState,
                phase: 'submitted',
              }
            : null,
        }))
      },

      // Lobby handlers
      onLobbyCreated: (msg) => {
        set({
          lobbyState: {
            lobbyId: msg.lobbyId,
            state: 'WAITING_FOR_PLAYERS',
            players: [],
            settings: { setCode: '', setName: '', boosterCount: 6, maxPlayers: 8, gamesPerMatch: 1 },
            isHost: true,
          },
        })
      },

      onLobbyUpdate: (msg) => {
        set({
          lobbyState: {
            lobbyId: msg.lobbyId,
            state: msg.state,
            players: msg.players,
            settings: msg.settings,
            isHost: msg.isHost,
          },
        })
      },

      // Tournament handlers
      onTournamentStarted: (msg) => {
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
          },
          // Clear deck building state since tournament is starting
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
              }
            : null,
          // Clear game state for between rounds
          gameState: null,
          gameOverState: null,
          mulliganState: null,
          waitingForOpponentMulligan: false,
          legalActions: [],
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
    }

    // Wrap handlers with logging in development
    const wrappedHandlers = import.meta.env.DEV
      ? createLoggingHandlers(handlers)
      : handlers

    return {
      // Initial state
      connectionStatus: 'disconnected',
      playerId: null,
      sessionId: null,
      opponentName: null,
      gameState: null,
      legalActions: [],
      pendingDecision: null,
      mulliganState: null,
      waitingForOpponentMulligan: false,
      selectedCardId: null,
      targetingState: null,
      combatState: null,
      xSelectionState: null,
      hoveredCardId: null,
      draggingBlockerId: null,
      draggingCardId: null,
      revealedHandCardIds: null,
      drawAnimations: [],
      pendingEvents: [],
      eventLog: [],
      gameOverState: null,
      lastError: null,
      deckBuildingState: null,
      lobbyState: null,
      tournamentState: null,

      // Connection actions
      connect: (playerName) => {
        // Prevent multiple connection attempts
        const { connectionStatus } = get()
        if (connectionStatus === 'connecting' || connectionStatus === 'connected') {
          return
        }

        if (ws) {
          ws.disconnect()
        }

        // Store player name for reconnection
        sessionStorage.setItem('argentum-player-name', playerName)

        ws = new GameWebSocket({
          url: getWebSocketUrl(),
          onMessage: (msg) => handleServerMessage(msg, wrappedHandlers),
          onStatusChange: (status) => {
            set({ connectionStatus: status })
            // Send connect message on every successful connection (including reconnects)
            if (status === 'connected' && ws) {
              const token = sessionStorage.getItem('argentum-token') ?? undefined
              const storedName = sessionStorage.getItem('argentum-player-name') ?? playerName
              ws.send(createConnectMessage(storedName, token))
            }
          },
          onError: () => {
            set({
              lastError: {
                code: 'INTERNAL_ERROR' as ErrorCode,
                message: 'WebSocket connection error',
                timestamp: Date.now(),
              },
            })
          },
        })

        ws.connect()
      },

      disconnect: () => {
        ws?.disconnect()
        ws = null
        sessionStorage.removeItem('argentum-token')
        sessionStorage.removeItem('argentum-player-name')
        set({
          connectionStatus: 'disconnected',
          playerId: null,
          sessionId: null,
          opponentName: null,
          gameState: null,
          legalActions: [],
          pendingDecision: null,
          mulliganState: null,
          waitingForOpponentMulligan: false,
          gameOverState: null,
          deckBuildingState: null,
          lobbyState: null,
          tournamentState: null,
        })
      },

      createGame: (deckList) => {
        ws?.send(createCreateGameMessage(deckList))
      },

      joinGame: (sessionId, deckList) => {
        ws?.send(createJoinGameMessage(sessionId, deckList))
      },

      submitAction: (action) => {
        ws?.send(createSubmitActionMessage(action))
        // Clear selection after submitting
        set({ selectedCardId: null, targetingState: null })
      },

      submitDecision: (selectedCards) => {
        const { pendingDecision, playerId } = get()
        if (!pendingDecision || !playerId) return

        const action = {
          type: 'SubmitDecision' as const,
          playerId,
          response: {
            type: 'CardsSelectedResponse' as const,
            decisionId: pendingDecision.id,
            selectedCards: [...selectedCards],
          },
        }
        ws?.send(createSubmitActionMessage(action))
      },

      submitTargetsDecision: (selectedTargets) => {
        const { pendingDecision, playerId } = get()
        if (!pendingDecision || !playerId) return

        const action = {
          type: 'SubmitDecision' as const,
          playerId,
          response: {
            type: 'TargetsResponse' as const,
            decisionId: pendingDecision.id,
            selectedTargets,
          },
        }
        ws?.send(createSubmitActionMessage(action))
      },

      submitOrderedDecision: (orderedObjects) => {
        const { pendingDecision, playerId } = get()
        if (!pendingDecision || !playerId) return

        const action = {
          type: 'SubmitDecision' as const,
          playerId,
          response: {
            type: 'OrderedResponse' as const,
            decisionId: pendingDecision.id,
            orderedObjects: [...orderedObjects],
          },
        }
        ws?.send(createSubmitActionMessage(action))
      },

      submitYesNoDecision: (choice) => {
        const { pendingDecision, playerId } = get()
        if (!pendingDecision || !playerId) return

        const action = {
          type: 'SubmitDecision' as const,
          playerId,
          response: {
            type: 'YesNoResponse' as const,
            decisionId: pendingDecision.id,
            choice,
          },
        }
        ws?.send(createSubmitActionMessage(action))
      },

      submitNumberDecision: (number) => {
        const { pendingDecision, playerId } = get()
        if (!pendingDecision || !playerId) return

        const action = {
          type: 'SubmitDecision' as const,
          playerId,
          response: {
            type: 'NumberChosenResponse' as const,
            decisionId: pendingDecision.id,
            number,
          },
        }
        ws?.send(createSubmitActionMessage(action))
      },

      submitDistributeDecision: (distribution) => {
        const { pendingDecision, playerId } = get()
        if (!pendingDecision || !playerId) return

        const action = {
          type: 'SubmitDecision' as const,
          playerId,
          response: {
            type: 'DistributionResponse' as const,
            decisionId: pendingDecision.id,
            distribution,
          },
        }
        ws?.send(createSubmitActionMessage(action))
      },

      keepHand: () => {
        ws?.send(createKeepHandMessage())
      },

      mulligan: () => {
        ws?.send(createMulliganMessage())
      },

      chooseBottomCards: (cardIds) => {
        ws?.send(createChooseBottomCardsMessage(cardIds))
      },

      concede: () => {
        trackEvent('player_conceded')
        ws?.send(createConcedeMessage())
      },

      cancelGame: () => {
        trackEvent('game_cancelled')
        ws?.send(createCancelGameMessage())
      },

      // Sealed draft actions
      createSealedGame: (setCode) => {
        trackEvent('sealed_game_created', { set_code: setCode })
        ws?.send(createCreateSealedGameMessage(setCode))
      },

      joinSealedGame: (sessionId) => {
        ws?.send(createJoinSealedGameMessage(sessionId))
      },

      addCardToDeck: (cardName) => {
        set((state) => {
          if (!state.deckBuildingState) return state

          return {
            deckBuildingState: {
              ...state.deckBuildingState,
              deck: [...state.deckBuildingState.deck, cardName],
            },
          }
        })
      },

      removeCardFromDeck: (cardName) => {
        set((state) => {
          if (!state.deckBuildingState) return state

          // Remove first occurrence of the card name
          const index = state.deckBuildingState.deck.indexOf(cardName)
          if (index === -1) return state

          const newDeck = [...state.deckBuildingState.deck]
          newDeck.splice(index, 1)

          return {
            deckBuildingState: {
              ...state.deckBuildingState,
              deck: newDeck,
            },
          }
        })
      },

      setLandCount: (landType, count) => {
        set((state) => {
          if (!state.deckBuildingState) return state

          return {
            deckBuildingState: {
              ...state.deckBuildingState,
              landCounts: {
                ...state.deckBuildingState.landCounts,
                [landType]: Math.max(0, count),
              },
            },
          }
        })
      },

      submitSealedDeck: () => {
        const { deckBuildingState } = get()
        if (!deckBuildingState) return

        // Build the deck list from deck cards + land counts
        const deckList: Record<string, number> = {}

        // Count non-land cards
        for (const cardName of deckBuildingState.deck) {
          deckList[cardName] = (deckList[cardName] || 0) + 1
        }

        // Add basic lands
        for (const [landType, count] of Object.entries(deckBuildingState.landCounts)) {
          if (count > 0) {
            deckList[landType] = count
          }
        }

        const landCount = Object.values(deckBuildingState.landCounts).reduce((a, b) => a + b, 0)
        trackEvent('sealed_deck_submitted', {
          deck_size: deckBuildingState.deck.length + landCount,
          land_count: landCount,
          nonland_count: deckBuildingState.deck.length,
        })
        ws?.send(createSubmitSealedDeckMessage(deckList))
      },

      // Lobby actions
      createSealedLobby: (setCode, boosterCount = 6, maxPlayers = 8) => {
        trackEvent('sealed_lobby_created', { set_code: setCode, booster_count: boosterCount, max_players: maxPlayers })
        ws?.send(createCreateSealedLobbyMessage(setCode, boosterCount, maxPlayers))
      },

      joinLobby: (lobbyId) => {
        trackEvent('sealed_lobby_joined')
        ws?.send(createJoinLobbyMessage(lobbyId))
      },

      startSealedLobby: () => {
        trackEvent('sealed_lobby_started')
        ws?.send(createStartSealedLobbyMessage())
      },

      leaveLobby: () => {
        ws?.send(createLeaveLobbyMessage())
        set({ lobbyState: null })
      },

      updateLobbySettings: (settings) => {
        ws?.send(createUpdateLobbySettingsMessage(settings))
      },

      // Tournament actions
      readyForNextRound: () => {
        ws?.send(createReadyForNextRoundMessage())
      },

      // UI actions
      selectCard: (cardId) => {
        set({ selectedCardId: cardId })
      },

      hoverCard: (cardId) => {
        set({ hoveredCardId: cardId })
      },

      toggleMulliganCard: (cardId) => {
        set((state) => {
          if (!state.mulliganState || state.mulliganState.phase !== 'choosingBottomCards') {
            return state
          }

          const isSelected = state.mulliganState.selectedCards.includes(cardId)
          const newSelected = isSelected
            ? state.mulliganState.selectedCards.filter((id) => id !== cardId)
            : [...state.mulliganState.selectedCards, cardId]

          return {
            mulliganState: {
              ...state.mulliganState,
              selectedCards: newSelected,
            },
          }
        })
      },

      startTargeting: (targetingState) => {
        set({ targetingState })
      },

      addTarget: (targetId) => {
        set((state) => {
          if (!state.targetingState) return state

          const newTargets = [...state.targetingState.selectedTargets, targetId]
          return {
            targetingState: {
              ...state.targetingState,
              selectedTargets: newTargets,
            },
          }
        })
      },

      removeTarget: (targetId) => {
        set((state) => {
          if (!state.targetingState) return state

          const newTargets = state.targetingState.selectedTargets.filter((id) => id !== targetId)
          return {
            targetingState: {
              ...state.targetingState,
              selectedTargets: newTargets,
            },
          }
        })
      },

      cancelTargeting: () => {
        set({ targetingState: null })
      },

      confirmTargeting: () => {
        const { targetingState, submitAction, gameState, startTargeting } = get()
        if (!targetingState || !gameState) return

        // Handle sacrifice selection phase
        if (targetingState.isSacrificeSelection && targetingState.pendingActionInfo) {
          const actionInfo = targetingState.pendingActionInfo
          const action = targetingState.action
          if (action.type === 'CastSpell') {
            const actionWithCost = {
              ...action,
              additionalCostPayment: {
                sacrificedPermanents: [...targetingState.selectedTargets],
              },
            }

            // Check if the spell also requires spell targets
            if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
              // Chain to spell targeting phase
              set({ targetingState: null })
              startTargeting({
                action: actionWithCost,
                validTargets: [...actionInfo.validTargets],
                selectedTargets: [],
                minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
                maxTargets: actionInfo.targetCount ?? 1,
              })
              return
            } else {
              submitAction(actionWithCost)
              set({ targetingState: null })
              return
            }
          }
        }

        // Normal targeting flow
        const action = targetingState.action

        // Handle multi-target spells (e.g., Wicked Pact with two target creatures)
        if (targetingState.targetRequirements && targetingState.targetRequirements.length > 1) {
          const currentIndex = targetingState.currentRequirementIndex ?? 0
          const nextIndex = currentIndex + 1

          // Collect all selected targets so far, including current selection
          const allSelected = targetingState.allSelectedTargets
            ? [...targetingState.allSelectedTargets, targetingState.selectedTargets]
            : [targetingState.selectedTargets]

          // Check if there are more requirements to fill
          const nextReq = targetingState.targetRequirements[nextIndex]
          if (nextReq) {
            // Filter out already-selected targets from valid targets for the next requirement
            const alreadySelected = allSelected.flat()
            const filteredValidTargets = nextReq.validTargets.filter(
              (t) => !alreadySelected.includes(t)
            )

            // Chain to next targeting phase
            startTargeting({
              action,
              validTargets: filteredValidTargets,
              selectedTargets: [],
              minTargets: nextReq.minTargets,
              maxTargets: nextReq.maxTargets,
              currentRequirementIndex: nextIndex,
              allSelectedTargets: allSelected,
              targetRequirements: targetingState.targetRequirements,
            })
            return
          }

          // All requirements filled - submit with all targets
          if (action.type === 'CastSpell' || action.type === 'ActivateAbility') {
            const targets = allSelected.flat().map((targetId) => {
              const isPlayer = gameState.players.some(p => p.playerId === targetId)
              if (isPlayer) {
                return { type: 'Player' as const, playerId: targetId }
              } else {
                return { type: 'Permanent' as const, entityId: targetId }
              }
            })
            const modifiedAction = {
              ...action,
              targets,
            }
            submitAction(modifiedAction)
          } else {
            submitAction(action)
          }
          set({ targetingState: null })
          return
        }

        // Single target requirement flow
        if (action.type === 'CastSpell' || action.type === 'ActivateAbility') {
          // Convert selected targets to ChosenTarget format
          // Check if target is a player or permanent
          const targets = targetingState.selectedTargets.map((targetId) => {
            // Check if this is a player ID
            const isPlayer = gameState.players.some(p => p.playerId === targetId)
            if (isPlayer) {
              return { type: 'Player' as const, playerId: targetId }
            } else {
              return { type: 'Permanent' as const, entityId: targetId }
            }
          })
          const modifiedAction = {
            ...action,
            targets,
          }
          submitAction(modifiedAction)
        } else {
          submitAction(action)
        }
        set({ targetingState: null })
      },

      // Combat actions
      startCombat: (combatState) => {
        set({ combatState })
      },

      toggleAttacker: (creatureId) => {
        set((state) => {
          if (!state.combatState || state.combatState.mode !== 'declareAttackers') {
            return state
          }

          const isSelected = state.combatState.selectedAttackers.includes(creatureId)
          const newAttackers = isSelected
            ? state.combatState.selectedAttackers.filter((id) => id !== creatureId)
            : [...state.combatState.selectedAttackers, creatureId]

          return {
            combatState: {
              ...state.combatState,
              selectedAttackers: newAttackers,
            },
          }
        })
      },

      assignBlocker: (blockerId, attackerId) => {
        set((state) => {
          if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
            return state
          }

          return {
            combatState: {
              ...state.combatState,
              blockerAssignments: {
                ...state.combatState.blockerAssignments,
                [blockerId]: attackerId,
              },
            },
          }
        })
      },

      removeBlockerAssignment: (blockerId) => {
        set((state) => {
          if (!state.combatState || state.combatState.mode !== 'declareBlockers') {
            return state
          }

          const { [blockerId]: _, ...remaining } = state.combatState.blockerAssignments
          return {
            combatState: {
              ...state.combatState,
              blockerAssignments: remaining,
            },
          }
        })
      },

      startDraggingBlocker: (blockerId) => {
        set({ draggingBlockerId: blockerId })
      },

      stopDraggingBlocker: () => {
        set({ draggingBlockerId: null })
      },

      startDraggingCard: (cardId) => {
        set({ draggingCardId: cardId })
      },

      stopDraggingCard: () => {
        set({ draggingCardId: null })
      },

      confirmCombat: () => {
        const { combatState, playerId } = get()
        if (!combatState || !playerId) return

        if (combatState.mode === 'declareAttackers') {
          // Build attackers map: attacker -> defending player (opponent)
          const { gameState } = get()
          if (!gameState) return

          const opponent = gameState.players.find((p) => p.playerId !== playerId)
          if (!opponent) return

          const attackers: Record<EntityId, EntityId> = {}
          for (const attackerId of combatState.selectedAttackers) {
            attackers[attackerId] = opponent.playerId
          }

          const action = {
            type: 'DeclareAttackers' as const,
            playerId,
            attackers,
          }
          ws?.send(createSubmitActionMessage(action))
        } else if (combatState.mode === 'declareBlockers') {
          // Build blockers map: blocker -> [attackers]
          const blockers: Record<EntityId, readonly EntityId[]> = {}
          for (const [blockerIdStr, attackerId] of Object.entries(combatState.blockerAssignments)) {
            blockers[entityId(blockerIdStr)] = [attackerId]
          }

          const action = {
            type: 'DeclareBlockers' as const,
            playerId,
            blockers,
          }
          ws?.send(createSubmitActionMessage(action))
        }

        // Don't clear combatState here - let the server response drive state changes
        // The useEffect in App.tsx will exit combat mode when legalActions changes
        set({ draggingBlockerId: null })
      },

      cancelCombat: () => {
        // Submit empty attackers/blockers to skip combat step
        const { combatState, playerId } = get()
        if (!combatState || !playerId) return

        if (combatState.mode === 'declareAttackers') {
          const action = {
            type: 'DeclareAttackers' as const,
            playerId,
            attackers: {} as Record<EntityId, EntityId>,
          }
          ws?.send(createSubmitActionMessage(action))
        } else if (combatState.mode === 'declareBlockers') {
          const action = {
            type: 'DeclareBlockers' as const,
            playerId,
            blockers: {} as Record<EntityId, readonly EntityId[]>,
          }
          ws?.send(createSubmitActionMessage(action))
        }

        // Don't clear combatState here - let the server response drive state changes
        set({ draggingBlockerId: null })
      },

      clearCombat: () => {
        // Called when server confirms combat action was processed (action no longer in legalActions)
        set({ combatState: null, draggingBlockerId: null })
      },

      // X cost selection actions
      startXSelection: (xSelectionState) => {
        set({ xSelectionState })
      },

      updateXValue: (x) => {
        set((state) => {
          if (!state.xSelectionState) return state
          return {
            xSelectionState: {
              ...state.xSelectionState,
              selectedX: x,
            },
          }
        })
      },

      cancelXSelection: () => {
        set({ xSelectionState: null })
      },

      confirmXSelection: () => {
        const { xSelectionState, startTargeting, playerId, gameState } = get()
        if (!xSelectionState || !playerId || !gameState) return

        const { actionInfo, selectedX } = xSelectionState

        // Build the CastSpell action with the selected X value
        if (actionInfo.action.type === 'CastSpell') {
          const baseAction = actionInfo.action

          // Check if the spell also requires targets
          if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
            // Need to enter targeting mode with the X value baked in
            const actionWithX = {
              ...baseAction,
              xValue: selectedX,
            }
            startTargeting({
              action: actionWithX,
              validTargets: [...actionInfo.validTargets],
              selectedTargets: [],
              minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
              maxTargets: actionInfo.targetCount ?? 1,
            })
          } else {
            // No targets needed, submit directly
            const actionWithX = {
              ...baseAction,
              xValue: selectedX,
            }
            ws?.send(createSubmitActionMessage(actionWithX))
          }
        }

        set({ xSelectionState: null })
      },

      returnToMenu: () => {
        set({
          sessionId: null,
          opponentName: null,
          gameState: null,
          legalActions: [],
          pendingDecision: null,
          mulliganState: null,
          waitingForOpponentMulligan: false,
          selectedCardId: null,
          targetingState: null,
          combatState: null,
          xSelectionState: null,
          hoveredCardId: null,
          draggingBlockerId: null,
          draggingCardId: null,
          revealedHandCardIds: null,
          pendingEvents: [],
          eventLog: [],
          gameOverState: null,
          lastError: null,
          deckBuildingState: null,
          lobbyState: null,
          tournamentState: null,
        })
      },

      clearError: () => {
        set({ lastError: null })
      },

      consumeEvent: () => {
        const { pendingEvents } = get()
        if (pendingEvents.length === 0) return undefined

        const [event, ...rest] = pendingEvents
        set({ pendingEvents: rest })
        return event
      },

      showRevealedHand: (cardIds) => {
        set({ revealedHandCardIds: cardIds })
      },

      dismissRevealedHand: () => {
        set({ revealedHandCardIds: null })
      },

      addDrawAnimation: (animation) => {
        set((state) => ({
          drawAnimations: [...state.drawAnimations, animation],
        }))
      },

      removeDrawAnimation: (id) => {
        set((state) => ({
          drawAnimations: state.drawAnimations.filter((a) => a.id !== id),
        }))
      },
    }
  })
)
