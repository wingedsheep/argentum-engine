import { ErrorCode, GameOverReason } from './enums'
import { EntityId } from './entities'
import { GameAction } from './actions'
import { ClientEvent } from './events'
import { ClientGameState } from './gameState'

// ============================================================================
// Server Messages (received from server)
// ============================================================================

/**
 * Messages sent from server to client.
 * Matches backend ServerMessage.kt
 */
export type ServerMessage =
  | ConnectedMessage
  | GameCreatedMessage
  | GameStartedMessage
  | StateUpdateMessage
  | MulliganDecisionMessage
  | ChooseBottomCardsMessage
  | MulliganCompleteMessage
  | GameOverMessage
  | ErrorMessage

/**
 * Connection confirmed with assigned player ID.
 */
export interface ConnectedMessage {
  readonly type: 'connected'
  readonly playerId: string
}

/**
 * Game created successfully, waiting for opponent.
 */
export interface GameCreatedMessage {
  readonly type: 'gameCreated'
  readonly sessionId: string
}

/**
 * Game is starting with both players connected.
 */
export interface GameStartedMessage {
  readonly type: 'gameStarted'
  readonly opponentName: string
}

/**
 * Game state update after an action is executed.
 */
export interface StateUpdateMessage {
  readonly type: 'stateUpdate'
  readonly state: ClientGameState
  readonly events: readonly ClientEvent[]
  readonly legalActions: readonly LegalActionInfo[]
  /** Pending decision that requires player input (e.g., discard to hand size) */
  readonly pendingDecision?: PendingDecision
}

// ============================================================================
// Pending Decision Types
// ============================================================================

/**
 * Base interface for pending decisions.
 */
export interface PendingDecisionBase {
  readonly id: string
  readonly playerId: EntityId
  readonly prompt: string
  readonly context: DecisionContext
}

/**
 * Context information about the decision.
 */
export interface DecisionContext {
  readonly phase: DecisionPhase
  readonly sourceId?: EntityId
  readonly sourceName?: string
}

/**
 * Phase/reason for the decision.
 */
export type DecisionPhase =
  | 'CASTING'
  | 'RESOLUTION'
  | 'STATE_BASED'
  | 'COMBAT'
  | 'TRIGGER'

/**
 * Player must select cards from a list.
 */
export interface SelectCardsDecision extends PendingDecisionBase {
  readonly type: 'SelectCardsDecision'
  readonly options: readonly EntityId[]
  readonly minSelections: number
  readonly maxSelections: number
  readonly ordered: boolean
}

/**
 * Player must make a yes/no decision.
 */
export interface YesNoDecision extends PendingDecisionBase {
  readonly type: 'YesNoDecision'
  readonly yesText: string
  readonly noText: string
}

/**
 * Player must choose targets.
 */
export interface ChooseTargetsDecision extends PendingDecisionBase {
  readonly type: 'ChooseTargetsDecision'
  readonly targetRequirements: readonly TargetRequirementInfo[]
  readonly legalTargets: Record<number, readonly EntityId[]>
}

export interface TargetRequirementInfo {
  readonly index: number
  readonly minTargets: number
  readonly maxTargets: number
  readonly description: string
}

/**
 * Information about a card available for selection during library search.
 * This is embedded in the decision because library cards are normally hidden.
 */
export interface SearchCardInfo {
  readonly name: string
  readonly manaCost: string
  readonly typeLine: string
  readonly imageUri: string | null
}

/**
 * Player must select cards from their library.
 *
 * Unlike SelectCardsDecision, this includes embedded card info because
 * library contents are normally hidden from the client.
 */
export interface SearchLibraryDecision extends PendingDecisionBase {
  readonly type: 'SearchLibraryDecision'
  readonly options: readonly EntityId[]
  readonly minSelections: number
  readonly maxSelections: number
  readonly cards: Record<EntityId, SearchCardInfo>
  readonly filterDescription: string
}

/**
 * Player must reorder cards from the top of their library.
 *
 * Used for "look at the top N cards and put them back in any order" effects
 * like Omen. The client displays the cards with drag-and-drop reordering,
 * with a clear indication of which end is the top of the library.
 */
export interface ReorderLibraryDecision extends PendingDecisionBase {
  readonly type: 'ReorderLibraryDecision'
  readonly cards: readonly EntityId[]
  readonly cardInfo: Record<EntityId, SearchCardInfo>
}

/**
 * Player must order objects (e.g., damage assignment order for blockers).
 *
 * Used when an attacker is blocked by multiple creatures and the attacking
 * player must declare the order in which blockers receive damage.
 */
export interface OrderObjectsDecision extends PendingDecisionBase {
  readonly type: 'OrderObjectsDecision'
  readonly objects: readonly EntityId[]
  readonly cardInfo?: Record<EntityId, SearchCardInfo>
}

/**
 * Union of all pending decision types.
 */
export type PendingDecision =
  | SelectCardsDecision
  | YesNoDecision
  | ChooseTargetsDecision
  | SearchLibraryDecision
  | ReorderLibraryDecision
  | OrderObjectsDecision

/**
 * Information about a legal action the player can take.
 */
export interface LegalActionInfo {
  readonly actionType: string
  readonly description: string
  readonly action: GameAction
  /** Valid target IDs if this action requires targeting */
  readonly validTargets?: readonly EntityId[]
  /** Whether this action requires selecting targets before submission */
  readonly requiresTargets?: boolean
  /** Number of targets required (default 1) */
  readonly targetCount?: number
  /** Description of the target requirement */
  readonly targetDescription?: string
  /** Valid attacker IDs for DeclareAttackers action */
  readonly validAttackers?: readonly EntityId[]
  /** Valid blocker IDs for DeclareBlockers action */
  readonly validBlockers?: readonly EntityId[]
  /** Whether this spell has X in its mana cost */
  readonly hasXCost?: boolean
  /** Maximum X value the player can afford (null if not X cost spell) */
  readonly maxAffordableX?: number
  /** Minimum X value (usually 0) */
  readonly minX?: number
  /** Whether this is a mana ability (doesn't highlight card as playable) */
  readonly isManaAbility?: boolean
}

/**
 * Card info for mulligan display.
 */
export interface MulliganCardInfo {
  readonly name: string
  readonly imageUri: string | null
}

/**
 * Mulligan decision required.
 */
export interface MulliganDecisionMessage {
  readonly type: 'mulliganDecision'
  readonly hand: readonly EntityId[]
  readonly mulliganCount: number
  readonly cardsToPutOnBottom: number
  readonly cards: Record<EntityId, MulliganCardInfo>
}

/**
 * Player must choose cards to put on bottom of library.
 */
export interface ChooseBottomCardsMessage {
  readonly type: 'chooseBottomCards'
  readonly hand: readonly EntityId[]
  readonly cardsToPutOnBottom: number
}

/**
 * Mulligan phase is complete and the game is starting.
 */
export interface MulliganCompleteMessage {
  readonly type: 'mulliganComplete'
  readonly finalHandSize: number
}

/**
 * Game has ended.
 */
export interface GameOverMessage {
  readonly type: 'gameOver'
  readonly winnerId: EntityId | null
  readonly reason: GameOverReason
}

/**
 * Error response from the server.
 */
export interface ErrorMessage {
  readonly type: 'error'
  readonly code: ErrorCode
  readonly message: string
}

// ============================================================================
// Client Messages (sent to server)
// ============================================================================

/**
 * Messages sent from client to server.
 * Matches backend ClientMessage.kt
 */
export type ClientMessage =
  | ConnectMessage
  | CreateGameMessage
  | JoinGameMessage
  | SubmitActionMessage
  | KeepHandMessage
  | MulliganMessage
  | ClientChooseBottomCardsMessage
  | ConcedeMessage

/**
 * Connect to the server with a player name.
 */
export interface ConnectMessage {
  readonly type: 'connect'
  readonly playerName: string
}

/**
 * Create a new game with a deck list.
 */
export interface CreateGameMessage {
  readonly type: 'createGame'
  readonly deckList: Record<string, number>
}

/**
 * Join an existing game with a session ID and deck list.
 */
export interface JoinGameMessage {
  readonly type: 'joinGame'
  readonly sessionId: string
  readonly deckList: Record<string, number>
}

/**
 * Submit a game action for execution.
 */
export interface SubmitActionMessage {
  readonly type: 'submitAction'
  readonly action: GameAction
}

/**
 * Keep the current opening hand.
 */
export interface KeepHandMessage {
  readonly type: 'keepHand'
}

/**
 * Mulligan: shuffle hand back and draw a new hand.
 */
export interface MulliganMessage {
  readonly type: 'mulligan'
}

/**
 * Choose which cards to put on the bottom of the library.
 */
export interface ClientChooseBottomCardsMessage {
  readonly type: 'chooseBottomCards'
  readonly cardIds: readonly EntityId[]
}

/**
 * Concede the current game.
 */
export interface ConcedeMessage {
  readonly type: 'concede'
}

// ============================================================================
// Type Guards
// ============================================================================

export function isConnectedMessage(msg: ServerMessage): msg is ConnectedMessage {
  return msg.type === 'connected'
}

export function isGameCreatedMessage(msg: ServerMessage): msg is GameCreatedMessage {
  return msg.type === 'gameCreated'
}

export function isGameStartedMessage(msg: ServerMessage): msg is GameStartedMessage {
  return msg.type === 'gameStarted'
}

export function isStateUpdateMessage(msg: ServerMessage): msg is StateUpdateMessage {
  return msg.type === 'stateUpdate'
}

export function isMulliganDecisionMessage(msg: ServerMessage): msg is MulliganDecisionMessage {
  return msg.type === 'mulliganDecision'
}

export function isChooseBottomCardsMessage(msg: ServerMessage): msg is ChooseBottomCardsMessage {
  return msg.type === 'chooseBottomCards'
}

export function isMulliganCompleteMessage(msg: ServerMessage): msg is MulliganCompleteMessage {
  return msg.type === 'mulliganComplete'
}

export function isGameOverMessage(msg: ServerMessage): msg is GameOverMessage {
  return msg.type === 'gameOver'
}

export function isErrorMessage(msg: ServerMessage): msg is ErrorMessage {
  return msg.type === 'error'
}

// ============================================================================
// Message Factories
// ============================================================================

export function createConnectMessage(playerName: string): ConnectMessage {
  return { type: 'connect', playerName }
}

export function createCreateGameMessage(deckList: Record<string, number>): CreateGameMessage {
  return { type: 'createGame', deckList }
}

export function createJoinGameMessage(sessionId: string, deckList: Record<string, number>): JoinGameMessage {
  return { type: 'joinGame', sessionId, deckList }
}

export function createSubmitActionMessage(action: GameAction): SubmitActionMessage {
  return { type: 'submitAction', action }
}

export function createKeepHandMessage(): KeepHandMessage {
  return { type: 'keepHand' }
}

export function createMulliganMessage(): MulliganMessage {
  return { type: 'mulligan' }
}

export function createChooseBottomCardsMessage(cardIds: readonly EntityId[]): ClientChooseBottomCardsMessage {
  return { type: 'chooseBottomCards', cardIds }
}

export function createConcedeMessage(): ConcedeMessage {
  return { type: 'concede' }
}
