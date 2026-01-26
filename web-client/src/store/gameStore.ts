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
  pendingDecision: PendingDecision | null

  // Mulligan state
  mulliganState: MulliganState | null

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
  submitDecision: (selectedCards: readonly EntityId[]) => void
  submitTargetsDecision: (selectedTargets: Record<number, readonly EntityId[]>) => void
  submitOrderedDecision: (orderedObjects: readonly EntityId[]) => void
  submitYesNoDecision: (choice: boolean) => void
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
  clearError: () => void
  consumeEvent: () => ClientEvent | undefined
  showRevealedHand: (cardIds: readonly EntityId[]) => void
  dismissRevealedHand: () => void
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

  // If there's something on the stack (e.g., opponent's triggered ability) and we have
  // no meaningful actions, auto-pass to let it resolve - even during main phases
  const stackZone = gameState.zones.find((z) => z.zoneId.zoneType === 'STACK')
  const stackHasItems = stackZone && stackZone.cardIds && stackZone.cardIds.length > 0
  if (stackHasItems) {
    return true
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
        // Check for handLookedAt event and show the revealed hand overlay
        const handLookedAtEvent = msg.events.find(
          (e) => e.type === 'handLookedAt'
        ) as { type: 'handLookedAt'; cardIds: readonly EntityId[] } | undefined

        set((state) => ({
          gameState: msg.state,
          legalActions: msg.legalActions,
          pendingDecision: msg.pendingDecision ?? null,
          pendingEvents: [...state.pendingEvents, ...msg.events],
          // Show revealed hand overlay if handLookedAt event received
          revealedHandCardIds: handLookedAtEvent?.cardIds ?? state.revealedHandCardIds,
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
      pendingDecision: null,
      mulliganState: null,
      selectedCardId: null,
      targetingState: null,
      combatState: null,
      xSelectionState: null,
      hoveredCardId: null,
      draggingBlockerId: null,
      draggingCardId: null,
      revealedHandCardIds: null,
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
          pendingDecision: null,
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
        const { targetingState, submitAction, gameState } = get()
        if (!targetingState || !gameState) return

        // Modify the action with selected targets and submit
        const action = targetingState.action
        if (action.type === 'CastSpell') {
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
              requiredCount: actionInfo.targetCount ?? 1,
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
    }
  })
)
