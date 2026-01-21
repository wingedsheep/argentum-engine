import { create } from 'zustand'
import { subscribeWithSelector } from 'zustand/middleware'
import type {
  EntityId,
  ClientGameState,
  ClientEvent,
  GameAction,
  LegalActionInfo,
  GameOverReason,
  ErrorCode,
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
  requiredCount: number
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
 * Main game store interface.
 */
export interface GameStore {
  // Connection state
  connectionStatus: ConnectionStatus
  playerId: EntityId | null
  sessionId: string | null
  opponentName: string | null

  // Game state (from server)
  gameState: ClientGameState | null
  legalActions: readonly LegalActionInfo[]

  // Mulligan state
  mulliganState: MulliganState | null

  // UI state (local only)
  selectedCardId: EntityId | null
  targetingState: TargetingState | null
  hoveredCardId: EntityId | null

  // Animation queue
  pendingEvents: readonly ClientEvent[]

  // Game over state
  gameOverState: GameOverState | null

  // Error state
  lastError: ErrorState | null

  // Actions
  connect: (playerName: string) => void
  disconnect: () => void
  createGame: (deckList: Record<string, number>) => void
  joinGame: (sessionId: string, deckList: Record<string, number>) => void
  submitAction: (action: GameAction) => void
  keepHand: () => void
  mulligan: () => void
  chooseBottomCards: (cardIds: readonly EntityId[]) => void
  concede: () => void

  // UI actions
  selectCard: (cardId: EntityId | null) => void
  hoverCard: (cardId: EntityId | null) => void
  toggleMulliganCard: (cardId: EntityId) => void
  startTargeting: (state: TargetingState) => void
  addTarget: (targetId: EntityId) => void
  cancelTargeting: () => void
  confirmTargeting: () => void
  clearError: () => void
  consumeEvent: () => ClientEvent | undefined
}

// WebSocket instance (singleton)
let ws: GameWebSocket | null = null

/**
 * Determines if we should auto-pass priority.
 * Auto-passes when:
 * - It's our priority (implied by receiving legalActions)
 * - The only meaningful legal actions are PassPriority and/or mana abilities
 *   (mana abilities without anything to spend them on are not useful)
 * - We're not in a main phase where player might want to hold priority
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

  // Check if there are any meaningful actions besides PassPriority and mana abilities
  // Meaningful actions include: PlayLand, CastSpell, ActivateAbility (non-mana), DeclareAttacker, DeclareBlocker
  const hasMeaningfulActions = legalActions.some((a) => {
    const type = a.action.type
    // PassPriority and mana abilities are not considered "meaningful" for auto-pass
    if (type === 'PassPriority') return false
    if (type === 'ActivateManaAbility') return false
    // Everything else is meaningful
    return true
  })

  // If there are meaningful actions, don't auto-pass
  if (hasMeaningfulActions) {
    return false
  }

  // Skip auto-pass during main phases - player might be thinking or waiting
  // (Even if they only have mana abilities, they might want to hold priority)
  const step = gameState.currentStep
  if (step === 'PRECOMBAT_MAIN' || step === 'POSTCOMBAT_MAIN') {
    return false
  }

  // Auto-pass for all other steps when only PassPriority/mana abilities are available
  return true
}

/**
 * Main Zustand store for game state.
 */
export const useGameStore = create<GameStore>()(
  subscribeWithSelector((set, get) => {
    // Message handlers that update the store
    const handlers: MessageHandlers = {
      onConnected: (msg) => {
        set({
          connectionStatus: 'connected',
          playerId: entityId(msg.playerId),
        })
      },

      onGameCreated: (msg) => {
        set({ sessionId: msg.sessionId })
      },

      onGameStarted: (msg) => {
        set({
          opponentName: msg.opponentName,
          mulliganState: null,
        })
      },

      onStateUpdate: (msg) => {
        set((state) => ({
          gameState: msg.state,
          legalActions: msg.legalActions,
          pendingEvents: [...state.pendingEvents, ...msg.events],
        }))

        // Auto-pass when the only action available is PassPriority
        // This skips through steps with no meaningful player actions
        const { playerId } = get()
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

      onGameOver: (msg) => {
        const { playerId } = get()
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
      mulliganState: null,
      selectedCardId: null,
      targetingState: null,
      hoveredCardId: null,
      pendingEvents: [],
      gameOverState: null,
      lastError: null,

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

        ws = new GameWebSocket({
          url: getWebSocketUrl(),
          onMessage: (msg) => handleServerMessage(msg, wrappedHandlers),
          onStatusChange: (status) => set({ connectionStatus: status }),
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

        // Send connect message once connected
        const unsubscribe = useGameStore.subscribe(
          (state) => state.connectionStatus,
          (status) => {
            if (status === 'connected' && ws) {
              ws.send(createConnectMessage(playerName))
              unsubscribe()
            }
          }
        )
      },

      disconnect: () => {
        ws?.disconnect()
        ws = null
        set({
          connectionStatus: 'disconnected',
          playerId: null,
          sessionId: null,
          opponentName: null,
          gameState: null,
          legalActions: [],
          mulliganState: null,
          gameOverState: null,
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
        ws?.send(createConcedeMessage())
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

      cancelTargeting: () => {
        set({ targetingState: null })
      },

      confirmTargeting: () => {
        const { targetingState, submitAction } = get()
        if (!targetingState) return

        // Modify the action with selected targets and submit
        // Note: In practice, targets are usually pre-set in the action from legalActions
        submitAction(targetingState.action)
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
    }
  })
)
