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
  ActiveMatchInfo,
  SpectatorPlayerState,
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
  createCreateTournamentLobbyMessage,
  createJoinLobbyMessage,
  createStartTournamentLobbyMessage,
  createMakePickMessage,
  createLeaveLobbyMessage,
  createStopLobbyMessage,
  createUnsubmitDeckMessage,
  createUpdateLobbySettingsMessage,
  createReadyForNextRoundMessage,
  createSpectateGameMessage,
  createStopSpectatingMessage,
  createUpdateBlockerAssignmentsMessage,
  SealedCardInfo,
} from '../types'
import { GameWebSocket, getWebSocketUrl, type ConnectionStatus } from '../network/websocket'
import { handleServerMessage, createLoggingHandlers, type MessageHandlers } from '../network/messageHandlers'

// ============================================================================
// Deck Building State Persistence
// ============================================================================

interface SavedDeckState {
  deck: readonly string[]
  landCounts: Record<string, number>
}

const DECK_STATE_KEY = 'argentum-deck-state'

function saveDeckState(deck: readonly string[], landCounts: Record<string, number>): void {
  const state: SavedDeckState = { deck, landCounts }
  sessionStorage.setItem(DECK_STATE_KEY, JSON.stringify(state))
}

function loadDeckState(): SavedDeckState | null {
  const saved = sessionStorage.getItem(DECK_STATE_KEY)
  if (!saved) return null
  try {
    return JSON.parse(saved) as SavedDeckState
  } catch {
    return null
  }
}

function clearDeckState(): void {
  sessionStorage.removeItem(DECK_STATE_KEY)
}

// ============================================================================
// Lobby State Persistence
// ============================================================================

const LOBBY_ID_KEY = 'argentum-lobby-id'

function saveLobbyId(lobbyId: string): void {
  sessionStorage.setItem(LOBBY_ID_KEY, lobbyId)
}

function loadLobbyId(): string | null {
  return sessionStorage.getItem(LOBBY_ID_KEY)
}

function clearLobbyId(): void {
  sessionStorage.removeItem(LOBBY_ID_KEY)
}

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
  result: 'win' | 'lose' | 'draw'
  message?: string | undefined
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
 * Draft state during the drafting phase.
 */
export interface DraftState {
  currentPack: readonly SealedCardInfo[]
  packNumber: number
  pickNumber: number
  pickedCards: readonly SealedCardInfo[]
  timeRemaining: number
  passDirection: 'LEFT' | 'RIGHT'
  picksPerRound: number  // Cards to pick this round (1 or 2)
  waitingForPlayers: readonly string[]
}

/**
 * Lobby state for tournament lobbies (sealed or draft).
 */
export interface LobbyState {
  lobbyId: string
  state: 'WAITING_FOR_PLAYERS' | 'DRAFTING' | 'DECK_BUILDING' | 'TOURNAMENT_ACTIVE' | 'TOURNAMENT_COMPLETE'
  players: readonly LobbyPlayerInfo[]
  settings: LobbySettings
  isHost: boolean
  /** Draft-specific state (only populated when format is DRAFT and state is DRAFTING) */
  draftState: DraftState | null
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
  activeMatches?: readonly ActiveMatchInfo[]
  /** Players who are ready for the next round */
  readyPlayerIds: readonly string[]
  /** Name of next opponent (null if BYE or tournament complete) */
  nextOpponentName: string | null
  /** True if player has a BYE in the next round */
  nextRoundHasBye: boolean
}

/**
 * State for spectating a game.
 */
export interface SpectatingState {
  gameSessionId: string
  player1: SpectatorPlayerState | null
  player2: SpectatorPlayerState | null
  player1Name?: string
  player2Name?: string
  currentPhase: string | null
  activePlayerId: string | null
  priorityPlayerId: string | null
  combat: import('../types/messages').SpectatorCombatState | null
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
  /** Opponent's blocker assignments (for attacking player to see real-time) */
  opponentBlockerAssignments: Record<EntityId, EntityId> | null

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
  spectatingState: SpectatingState | null

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
  unsubmitDeck: () => void

  // Lobby actions
  createTournamentLobby: (setCode: string, format?: 'SEALED' | 'DRAFT', boosterCount?: number, maxPlayers?: number, pickTimeSeconds?: number) => void
  /** @deprecated Use createTournamentLobby instead */
  createSealedLobby: (setCode: string, boosterCount?: number, maxPlayers?: number) => void
  joinLobby: (lobbyId: string) => void
  startLobby: () => void
  /** @deprecated Use startLobby instead */
  startSealedLobby: () => void
  leaveLobby: () => void
  stopLobby: () => void
  updateLobbySettings: (settings: { setCode?: string; format?: 'SEALED' | 'DRAFT'; boosterCount?: number; maxPlayers?: number; gamesPerMatch?: number; pickTimeSeconds?: number; picksPerRound?: number }) => void

  // Draft actions
  makePick: (cardNames: string[]) => void

  // Tournament actions
  readyForNextRound: () => void

  // Spectating actions
  spectateGame: (gameSessionId: string) => void
  stopSpectating: () => void

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
  leaveTournament: () => void
  clearError: () => void
  consumeEvent: () => ClientEvent | undefined
  showRevealedHand: (cardIds: readonly EntityId[]) => void
  dismissRevealedHand: () => void
  // Draw animation
  drawAnimations: readonly DrawAnimation[]
  addDrawAnimation: (animation: DrawAnimation) => void
  removeDrawAnimation: (id: string) => void
  // Damage animation
  damageAnimations: readonly DamageAnimation[]
  addDamageAnimation: (animation: DamageAnimation) => void
  removeDamageAnimation: (id: string) => void
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

/**
 * A life change animation (damage or life gain).
 */
export interface DamageAnimation {
  id: string
  targetId: EntityId
  targetIsPlayer: boolean
  amount: number
  isLifeGain: boolean
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

/** Result from shouldAutoPass - either no auto-pass or auto-pass with delay */
type AutoPassResult = { autoPass: false } | { autoPass: true; delay: number }

// Delay constants
const QUICK_AUTO_PASS_DELAY = 50 // ms - for own spells, empty stack phases, opponent's permanents

/**
 * Determines if we should auto-pass priority and with what delay.
 * Auto-passes when:
 * - It's our priority
 * - The stack is empty (opponent's spells require player to click "Resolve")
 * - The only meaningful legal actions are PassPriority and/or mana abilities
 * - We're not in a main phase
 * - OR the top of the stack is our own spell/ability (we rarely want to respond to ourselves)
 *
 * This skips through steps like UNTAP, UPKEEP, DRAW, BEGIN_COMBAT, etc.
 * when there are no meaningful actions available.
 *
 * Returns an object with autoPass flag and delay in milliseconds.
 */
function shouldAutoPass(
  legalActions: readonly LegalActionInfo[],
  gameState: ClientGameState,
  playerId: EntityId,
  pendingDecision: PendingDecision | null | undefined
): AutoPassResult {
  // Must have priority
  if (gameState.priorityPlayerId !== playerId) {
    return { autoPass: false }
  }

  // Don't auto-pass if the opponent has a pending decision (e.g., discarding cards)
  // The game is waiting on them, not us
  if (pendingDecision && pendingDecision.playerId !== playerId) {
    return { autoPass: false }
  }

  // Must have at least PassPriority available
  const hasPassPriority = legalActions.some((a) => a.action.type === 'PassPriority')
  if (!hasPassPriority) {
    return { autoPass: false }
  }

  // Auto-pass when responding to our own spell/ability on the stack
  // (99% of the time, players want their spell to resolve, not counter it themselves)
  const topOfStack = getTopOfStack(gameState)
  if (topOfStack && topOfStack.controllerId === playerId) {
    return { autoPass: true, delay: QUICK_AUTO_PASS_DELAY }
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
    return { autoPass: false }
  }

  // If there's something on the stack (opponent's spell/ability), don't auto-pass
  // Let the player see it and click "Resolve" - trust the backend's decision to give priority
  const stackZone = gameState.zones.find((z) => z.zoneId.zoneType === 'STACK')
  const stackHasItems = stackZone && stackZone.cardIds && stackZone.cardIds.length > 0
  if (stackHasItems) {
    return { autoPass: false }
  }

  // Skip auto-pass during main phases when stack is empty - player might be thinking
  // about what to do or waiting to tap lands
  const step = gameState.currentStep
  if (step === 'PRECOMBAT_MAIN' || step === 'POSTCOMBAT_MAIN') {
    return { autoPass: false }
  }

  // Auto-pass for all other steps when only PassPriority/mana abilities are available
  return { autoPass: true, delay: QUICK_AUTO_PASS_DELAY }
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
    case 'handRevealed': return event.revealingPlayerId
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
        // Note: onConnected means we got a NEW identity (server didn't recognize our token).
        // This can happen if the server restarted or our session expired.
        // Clear any stale lobby state since we're starting fresh.
        clearLobbyId()
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

        // Server handles tournament/lobby/deckBuilding/game contexts automatically
        // Only try to rejoin lobby if server doesn't know our context
        if (!msg.context && !msg.contextId) {
          const savedLobbyId = loadLobbyId()
          if (savedLobbyId && ws) {
            ws.send(createJoinLobbyMessage(savedLobbyId))
          }
        }
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
        // Get state first (needed for filtering events)
        const { playerId, addDrawAnimation, addDamageAnimation } = get()

        // Check for handLookedAt or handRevealed event and show the revealed hand overlay
        const handLookedAtEvent = msg.events.find(
          (e) => e.type === 'handLookedAt'
        ) as { type: 'handLookedAt'; cardIds: readonly EntityId[] } | undefined

        // Also check for handRevealed events (from RevealHandEffect, e.g., Withering Gaze)
        // Only show the overlay if the OPPONENT is revealing their hand (not the current player)
        const handRevealedEvent = msg.events.find(
          (e) => e.type === 'handRevealed' && (e as { revealingPlayerId: EntityId }).revealingPlayerId !== playerId
        ) as { type: 'handRevealed'; cardIds: readonly EntityId[] } | undefined

        // Process cardDrawn events for draw animations
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

        // Process lifeChanged events for player life changes (damage, life loss, life gain)
        // Aggregate changes per player to show combined damage/healing from combat
        const lifeChangedEvents = msg.events.filter(
          (e) => e.type === 'lifeChanged' && (e as { change: number }).change !== 0
        ) as {
          type: 'lifeChanged'
          playerId: EntityId
          change: number
        }[]

        // Sum life changes per player, keeping damage and life gain separate
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

        // Create aggregated life change animations for players
        let animIndex = 0
        playerLifeChanges.forEach((changes, odPlayerId) => {
          if (changes.damage > 0) {
            addDamageAnimation({
              id: `life-${odPlayerId}-${Date.now()}-damage`,
              targetId: odPlayerId,
              targetIsPlayer: true,
              amount: changes.damage,
              isLifeGain: false,
              startTime: Date.now() + animIndex * 50,
            })
            animIndex++
          }
          if (changes.lifeGain > 0) {
            addDamageAnimation({
              id: `life-${odPlayerId}-${Date.now()}-gain`,
              targetId: odPlayerId,
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
          pendingEvents: [...state.pendingEvents, ...msg.events],
          eventLog: (msg.state.gameLog ?? []).map((e: ClientEvent) => ({
            description: e.description,
            playerId: getEventPlayerId(e),
            timestamp: Date.now(),
          })),
          waitingForOpponentMulligan: false,
          // Show revealed hand overlay if handLookedAt or handRevealed event received
          revealedHandCardIds: handLookedAtEvent?.cardIds ?? handRevealedEvent?.cardIds ?? state.revealedHandCardIds,
          // Clear opponent's blocker assignments when combat ends or blockers are confirmed
          opponentBlockerAssignments: (msg.state.combat?.blockers?.length || !msg.state.combat) ? null : state.opponentBlockerAssignments,
        }))

        // Auto-pass when the only action available is PassPriority
        // This skips through steps with no meaningful player actions
        if (playerId) {
          const autoPassResult = shouldAutoPass(msg.legalActions, msg.state, playerId, msg.pendingDecision)
          if (autoPassResult.autoPass) {
            setTimeout(() => {
              // Re-check current state - game may have changed during the delay
              const currentState = get()
              if (!currentState.gameState || !currentState.playerId) return

              // Verify auto-pass is still valid with current state
              const recheck = shouldAutoPass(
                currentState.legalActions,
                currentState.gameState,
                currentState.playerId,
                currentState.pendingDecision
              )
              if (!recheck.autoPass) return

              const passAction = currentState.legalActions.find((a) => a.action.type === 'PassPriority')
              if (passAction && ws) {
                ws.send(createSubmitActionMessage(passAction.action))
              }
            }, autoPassResult.delay)
          }
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
        // Determine result: draw if no winner, otherwise win/lose based on winnerId
        const result: 'win' | 'lose' | 'draw' =
          msg.winnerId === null ? 'draw' : msg.winnerId === playerId ? 'win' : 'lose'
        trackEvent('game_over', {
          result,
          reason: msg.reason,
        })
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
        // Clear saved lobby ID if lobby not found (prevents infinite rejoin loops)
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

        // Try to restore saved deck state (for page refresh during deck building)
        const savedState = loadDeckState()

        set((state) => ({
          deckBuildingState: {
            phase: 'building',
            setCode: msg.setCode,
            setName: msg.setName,
            cardPool: msg.cardPool,
            basicLands: msg.basicLands,
            // Priority: existing state > saved state > empty
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
        // Save lobby ID for reconnection after refresh
        saveLobbyId(msg.lobbyId)
        set({
          lobbyState: {
            lobbyId: msg.lobbyId,
            state: 'WAITING_FOR_PLAYERS',
            players: [],
            settings: { setCode: '', setName: '', format: 'SEALED', boosterCount: 6, maxPlayers: 8, pickTimeSeconds: 45, picksPerRound: 1, gamesPerMatch: 1 },
            isHost: true,
            draftState: null,
          },
        })
      },

      onLobbyUpdate: (msg) => {
        const { playerId, lobbyState } = get()

        // Save lobby ID for reconnection after refresh
        saveLobbyId(msg.lobbyId)

        // Check if current player's deck submission status changed
        const currentPlayer = msg.players.find((p) => p.playerId === playerId)
        const isDeckSubmitted = currentPlayer?.deckSubmitted ?? false

        set((state) => ({
          lobbyState: {
            lobbyId: msg.lobbyId,
            state: msg.state as LobbyState['state'],
            players: msg.players,
            settings: msg.settings,
            isHost: msg.isHost,
            // Preserve draft state if still drafting
            draftState: msg.state === 'DRAFTING' ? (lobbyState?.draftState ?? null) : null,
          },
          // Update deck building phase based on submission status
          deckBuildingState:
            state.deckBuildingState && msg.state === 'DECK_BUILDING'
              ? {
                  ...state.deckBuildingState,
                  phase: isDeckSubmitted ? 'submitted' : 'building',
                }
              : state.deckBuildingState,
        }))
      },

      onLobbyStopped: () => {
        clearDeckState()
        clearLobbyId()
        set({
          lobbyState: null,
          deckBuildingState: null,
        })
      },

      // Draft handlers
      onDraftPackReceived: (msg) => {
        set((state) => ({
          lobbyState: state.lobbyState
            ? {
                ...state.lobbyState,
                draftState: {
                  currentPack: msg.cards,
                  packNumber: msg.packNumber,
                  pickNumber: msg.pickNumber,
                  // Use pickedCards from server if provided (reconnect), otherwise preserve existing
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
                draftState: {
                  ...state.lobbyState.draftState,
                  waitingForPlayers: msg.waitingForPlayers,
                },
              }
            : state.lobbyState,
        }))
      },

      onDraftPickConfirmed: (msg) => {
        set((state) => {
          if (!state.lobbyState?.draftState) return state

          // Find the picked cards in the current pack
          const pickedCards = state.lobbyState.draftState.currentPack.filter((c) =>
            msg.cardNames.includes(c.name)
          )
          if (pickedCards.length === 0) return state

          return {
            lobbyState: {
              ...state.lobbyState,
              draftState: {
                ...state.lobbyState.draftState,
                // Clear current pack - we're now waiting for the next pack to be passed
                currentPack: [],
                // Add to picked cards
                pickedCards: [...state.lobbyState.draftState.pickedCards, ...pickedCards],
              },
            },
          }
        })
      },

      onDraftComplete: (msg) => {
        trackEvent('draft_complete', { picked_cards: msg.pickedCards.length })

        // Transition to deck building
        set((state) => ({
          lobbyState: state.lobbyState
            ? {
                ...state.lobbyState,
                state: 'DECK_BUILDING',
                draftState: null,
              }
            : null,
          deckBuildingState: {
            phase: 'building',
            setCode: state.lobbyState?.settings.setCode ?? '',
            setName: state.lobbyState?.settings.setName ?? '',
            cardPool: msg.pickedCards,
            basicLands: msg.basicLands,
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
        }))
      },

      onDraftTimerUpdate: (msg) => {
        set((state) => ({
          lobbyState: state.lobbyState?.draftState
            ? {
                ...state.lobbyState,
                draftState: {
                  ...state.lobbyState.draftState,
                  timeRemaining: msg.secondsRemaining,
                },
              }
            : state.lobbyState,
        }))
      },

      // Tournament handlers
      onTournamentStarted: (msg) => {
        // Clear saved deck state since tournament is starting
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
          // Clear game state for between rounds
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
            ? {
                ...state.tournamentState,
                readyPlayerIds: msg.readyPlayerIds,
              }
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

      // Spectating handlers
      onActiveMatches: (msg) => {
        set((state) => ({
          tournamentState: state.tournamentState
            ? {
                ...state.tournamentState,
                activeMatches: msg.matches,
                standings: msg.standings,
                // Mark that this player's match is done (waiting for others)
                currentMatchGameSessionId: null,
                currentMatchOpponentName: null,
              }
            : null,
          // Clear game state when receiving active matches (player is now in tournament view)
          gameState: null,
          gameOverState: null,
          mulliganState: null,
          waitingForOpponentMulligan: false,
          legalActions: [],
        }))
      },

      onSpectatorStateUpdate: (msg) => {
        set({
          spectatingState: {
            gameSessionId: msg.gameSessionId,
            player1: msg.player1,
            player2: msg.player2,
            currentPhase: msg.currentPhase,
            activePlayerId: msg.activePlayerId,
            priorityPlayerId: msg.priorityPlayerId,
            combat: msg.combat,
          },
        })
      },

      onSpectatingStarted: (msg) => {
        set({
          spectatingState: {
            gameSessionId: msg.gameSessionId,
            player1: null,
            player2: null,
            player1Name: msg.player1Name,
            player2Name: msg.player2Name,
            currentPhase: null,
            activePlayerId: null,
            priorityPlayerId: null,
            combat: null,
          },
        })
      },

      onSpectatingStopped: () => {
        set({
          spectatingState: null,
        })
      },

      // Combat UI handlers
      onOpponentBlockerAssignments: (msg) => {
        set({
          opponentBlockerAssignments: msg.assignments,
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
      waitingForOpponentMulligan: false,
      selectedCardId: null,
      targetingState: null,
      combatState: null,
      xSelectionState: null,
      hoveredCardId: null,
      draggingBlockerId: null,
      draggingCardId: null,
      revealedHandCardIds: null,
      opponentBlockerAssignments: null,
      drawAnimations: [],
      damageAnimations: [],
      pendingEvents: [],
      eventLog: [],
      gameOverState: null,
      lastError: null,
      deckBuildingState: null,
      lobbyState: null,
      tournamentState: null,
      spectatingState: null,

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
        clearLobbyId()
        clearDeckState()
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

          const newDeck = [...state.deckBuildingState.deck, cardName]
          saveDeckState(newDeck, state.deckBuildingState.landCounts)

          return {
            deckBuildingState: {
              ...state.deckBuildingState,
              deck: newDeck,
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
          saveDeckState(newDeck, state.deckBuildingState.landCounts)

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

          const newLandCounts = {
            ...state.deckBuildingState.landCounts,
            [landType]: Math.max(0, count),
          }
          saveDeckState(state.deckBuildingState.deck, newLandCounts)

          return {
            deckBuildingState: {
              ...state.deckBuildingState,
              landCounts: newLandCounts,
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

      unsubmitDeck: () => {
        ws?.send(createUnsubmitDeckMessage())
        // The server will send a lobby update which will update our state
      },

      // Lobby actions
      createTournamentLobby: (setCode, format = 'SEALED', boosterCount = 6, maxPlayers = 8, pickTimeSeconds = 45) => {
        trackEvent('tournament_lobby_created', { set_code: setCode, format, booster_count: boosterCount, max_players: maxPlayers })
        ws?.send(createCreateTournamentLobbyMessage(setCode, format, boosterCount, maxPlayers, pickTimeSeconds))
      },

      createSealedLobby: (setCode, boosterCount = 6, maxPlayers = 8) => {
        // Backwards compatibility
        trackEvent('sealed_lobby_created', { set_code: setCode, booster_count: boosterCount, max_players: maxPlayers })
        ws?.send(createCreateTournamentLobbyMessage(setCode, 'SEALED', boosterCount, maxPlayers, 45))
      },

      joinLobby: (lobbyId) => {
        trackEvent('lobby_joined')
        ws?.send(createJoinLobbyMessage(lobbyId))
      },

      startLobby: () => {
        const { lobbyState } = get()
        trackEvent('lobby_started', { format: lobbyState?.settings.format })
        ws?.send(createStartTournamentLobbyMessage())
      },

      startSealedLobby: () => {
        // Backwards compatibility
        trackEvent('sealed_lobby_started')
        ws?.send(createStartTournamentLobbyMessage())
      },

      leaveLobby: () => {
        clearDeckState()
        clearLobbyId()
        ws?.send(createLeaveLobbyMessage())
        set({ lobbyState: null, deckBuildingState: null })
      },

      stopLobby: () => {
        clearDeckState()
        clearLobbyId()
        ws?.send(createStopLobbyMessage())
        set({ lobbyState: null, deckBuildingState: null })
      },

      updateLobbySettings: (settings) => {
        ws?.send(createUpdateLobbySettingsMessage(settings))
      },

      // Draft actions
      makePick: (cardNames) => {
        trackEvent('draft_pick_made', { card_names: cardNames })
        ws?.send(createMakePickMessage(cardNames))
      },

      // Tournament actions
      readyForNextRound: () => {
        ws?.send(createReadyForNextRoundMessage())
      },

      // Spectating actions
      spectateGame: (gameSessionId) => {
        ws?.send(createSpectateGameMessage(gameSessionId))
      },

      stopSpectating: () => {
        ws?.send(createStopSpectatingMessage())
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
              // Check if this is a player ID
              const isPlayer = gameState.players.some(p => p.playerId === targetId)
              if (isPlayer) {
                return { type: 'Player' as const, playerId: targetId }
              }
              // Check if this is a card in a graveyard
              const card = gameState.cards[targetId]
              if (card && card.zone?.zoneType === 'GRAVEYARD') {
                return {
                  type: 'Card' as const,
                  cardId: targetId,
                  ownerId: card.zone.ownerId,
                  zone: 'GRAVEYARD' as const,
                }
              }
              // Default to permanent (battlefield)
              return { type: 'Permanent' as const, entityId: targetId }
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
          const targets = targetingState.selectedTargets.map((targetId) => {
            // Check if this is a player ID
            const isPlayer = gameState.players.some(p => p.playerId === targetId)
            if (isPlayer) {
              return { type: 'Player' as const, playerId: targetId }
            }
            // Check if this is a card in a graveyard
            const card = gameState.cards[targetId]
            if (card && card.zone?.zoneType === 'GRAVEYARD') {
              return {
                type: 'Card' as const,
                cardId: targetId,
                ownerId: card.zone.ownerId,
                zone: 'GRAVEYARD' as const,
              }
            }
            // Default to permanent (battlefield)
            return { type: 'Permanent' as const, entityId: targetId }
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

          const newAssignments = {
            ...state.combatState.blockerAssignments,
            [blockerId]: attackerId,
          }

          // Send assignments to server for real-time sync with opponent
          ws?.send(createUpdateBlockerAssignmentsMessage(newAssignments))

          return {
            combatState: {
              ...state.combatState,
              blockerAssignments: newAssignments,
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

          // Send assignments to server for real-time sync with opponent
          ws?.send(createUpdateBlockerAssignmentsMessage(remaining))

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
        const state = get()
        // If in a tournament, only clear game state, not tournament/lobby state
        const isInTournament = state.tournamentState != null
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
          // Preserve lobby/tournament state if in tournament
          lobbyState: isInTournament ? state.lobbyState : null,
          tournamentState: isInTournament ? state.tournamentState : null,
        })
      },

      leaveTournament: () => {
        // Clear all state including tournament/lobby - used when tournament is complete
        clearLobbyId()
        clearDeckState()
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

      addDamageAnimation: (animation) => {
        set((state) => ({
          damageAnimations: [...state.damageAnimations, animation],
        }))
      },

      removeDamageAnimation: (id) => {
        set((state) => ({
          damageAnimations: state.damageAnimations.filter((a) => a.id !== id),
        }))
      },
    }
  })
)
