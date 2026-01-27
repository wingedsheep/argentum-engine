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
  | ReconnectedMessage
  | GameCreatedMessage
  | GameStartedMessage
  | StateUpdateMessage
  | MulliganDecisionMessage
  | ChooseBottomCardsMessage
  | MulliganCompleteMessage
  | GameOverMessage
  | ErrorMessage
  // Sealed Draft Messages
  | SealedGameCreatedMessage
  | SealedPoolGeneratedMessage
  | OpponentDeckSubmittedMessage
  | WaitingForOpponentMessage
  | DeckSubmittedMessage
  // Lobby Messages
  | LobbyCreatedMessage
  | LobbyUpdateMessage
  // Tournament Messages
  | TournamentStartedMessage
  | TournamentMatchStartingMessage
  | TournamentByeMessage
  | RoundCompleteMessage
  | TournamentCompleteMessage

/**
 * Connection confirmed with assigned player ID.
 */
export interface ConnectedMessage {
  readonly type: 'connected'
  readonly playerId: string
  readonly token: string
}

/**
 * Reconnection confirmed — previous session restored.
 */
export interface ReconnectedMessage {
  readonly type: 'reconnected'
  readonly playerId: string
  readonly token: string
  readonly context: 'lobby' | 'deckBuilding' | 'game' | 'tournament' | null
  readonly contextId: string | null
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
 *
 * For hidden cards (e.g., opponent's library during Cruel Fate), the cardInfo
 * field provides information about the cards since they're not in gameState.cards.
 */
export interface SelectCardsDecision extends PendingDecisionBase {
  readonly type: 'SelectCardsDecision'
  readonly options: readonly EntityId[]
  readonly minSelections: number
  readonly maxSelections: number
  readonly ordered: boolean
  /** Card info for hidden cards (null/undefined if cards are visible in gameState) */
  readonly cardInfo?: Record<EntityId, SearchCardInfo> | null
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
 * Player must choose a number (e.g., how many cards to draw).
 */
export interface ChooseNumberDecision extends PendingDecisionBase {
  readonly type: 'ChooseNumberDecision'
  readonly minValue: number
  readonly maxValue: number
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
  | ChooseNumberDecision

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
  /** Maximum number of targets (default 1) */
  readonly targetCount?: number
  /** Minimum number of targets required (default = targetCount) */
  readonly minTargets?: number
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
// Sealed Draft Server Messages
// ============================================================================

/**
 * Card information for sealed deck building UI.
 */
export interface SealedCardInfo {
  readonly name: string
  readonly manaCost: string | null
  readonly typeLine: string
  readonly rarity: string
  readonly imageUri: string | null
  readonly power?: number | null
  readonly toughness?: number | null
  readonly oracleText?: string | null
}

/**
 * Sealed game created successfully, waiting for opponent.
 */
export interface SealedGameCreatedMessage {
  readonly type: 'sealedGameCreated'
  readonly sessionId: string
  readonly setCode: string
  readonly setName: string
}

/**
 * Sealed pool has been generated for the player.
 */
export interface SealedPoolGeneratedMessage {
  readonly type: 'sealedPoolGenerated'
  readonly setCode: string
  readonly setName: string
  readonly cardPool: readonly SealedCardInfo[]
  readonly basicLands: readonly SealedCardInfo[]
}

/**
 * Opponent has submitted their sealed deck.
 */
export interface OpponentDeckSubmittedMessage {
  readonly type: 'opponentDeckSubmitted'
}

/**
 * Waiting for opponent to submit their deck.
 */
export interface WaitingForOpponentMessage {
  readonly type: 'waitingForOpponent'
}

/**
 * Deck submission was successful.
 */
export interface DeckSubmittedMessage {
  readonly type: 'deckSubmitted'
  readonly deckSize: number
}

// ============================================================================
// Lobby Server Messages
// ============================================================================

export interface LobbyPlayerInfo {
  readonly playerId: string
  readonly playerName: string
  readonly isHost: boolean
  readonly isConnected: boolean
  readonly deckSubmitted: boolean
}

export interface LobbySettings {
  readonly setCode: string
  readonly setName: string
  readonly boosterCount: number
  readonly maxPlayers: number
}

export interface LobbyCreatedMessage {
  readonly type: 'lobbyCreated'
  readonly lobbyId: string
}

export interface LobbyUpdateMessage {
  readonly type: 'lobbyUpdate'
  readonly lobbyId: string
  readonly state: string
  readonly players: readonly LobbyPlayerInfo[]
  readonly settings: LobbySettings
  readonly isHost: boolean
}

// ============================================================================
// Tournament Server Messages
// ============================================================================

export interface PlayerStandingInfo {
  readonly playerId: string
  readonly playerName: string
  readonly wins: number
  readonly losses: number
  readonly draws: number
  readonly points: number
  readonly isConnected: boolean
}

export interface MatchResultInfo {
  readonly player1Name: string
  readonly player2Name: string
  readonly winnerId: string | null
  readonly isDraw: boolean
  readonly isBye: boolean
}

export interface TournamentStartedMessage {
  readonly type: 'tournamentStarted'
  readonly lobbyId: string
  readonly totalRounds: number
  readonly standings: readonly PlayerStandingInfo[]
}

export interface TournamentMatchStartingMessage {
  readonly type: 'tournamentMatchStarting'
  readonly lobbyId: string
  readonly round: number
  readonly gameSessionId: string
  readonly opponentName: string
}

export interface TournamentByeMessage {
  readonly type: 'tournamentBye'
  readonly lobbyId: string
  readonly round: number
}

export interface RoundCompleteMessage {
  readonly type: 'roundComplete'
  readonly lobbyId: string
  readonly round: number
  readonly results: readonly MatchResultInfo[]
  readonly standings: readonly PlayerStandingInfo[]
}

export interface TournamentCompleteMessage {
  readonly type: 'tournamentComplete'
  readonly lobbyId: string
  readonly finalStandings: readonly PlayerStandingInfo[]
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
  // Sealed Draft Messages
  | CreateSealedGameMessage
  | JoinSealedGameMessage
  | SubmitSealedDeckMessage
  // Lobby Messages
  | CreateSealedLobbyMessage
  | JoinLobbyMessage
  | StartSealedLobbyMessage
  | LeaveLobbyMessage
  | UpdateLobbySettingsMessage
  // Tournament Messages
  | ReadyForNextRoundMessage

/**
 * Connect to the server with a player name.
 */
export interface ConnectMessage {
  readonly type: 'connect'
  readonly playerName: string
  readonly token?: string
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
// Sealed Draft Client Messages
// ============================================================================

/**
 * Create a new sealed game with a specific set.
 */
export interface CreateSealedGameMessage {
  readonly type: 'createSealedGame'
  readonly setCode: string
}

/**
 * Join an existing sealed game session.
 */
export interface JoinSealedGameMessage {
  readonly type: 'joinSealedGame'
  readonly sessionId: string
}

/**
 * Submit the built deck for a sealed game.
 */
export interface SubmitSealedDeckMessage {
  readonly type: 'submitSealedDeck'
  readonly deckList: Record<string, number>
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

// Sealed Draft Type Guards
export function isSealedGameCreatedMessage(msg: ServerMessage): msg is SealedGameCreatedMessage {
  return msg.type === 'sealedGameCreated'
}

export function isSealedPoolGeneratedMessage(msg: ServerMessage): msg is SealedPoolGeneratedMessage {
  return msg.type === 'sealedPoolGenerated'
}

export function isOpponentDeckSubmittedMessage(msg: ServerMessage): msg is OpponentDeckSubmittedMessage {
  return msg.type === 'opponentDeckSubmitted'
}

export function isWaitingForOpponentMessage(msg: ServerMessage): msg is WaitingForOpponentMessage {
  return msg.type === 'waitingForOpponent'
}

export function isDeckSubmittedMessage(msg: ServerMessage): msg is DeckSubmittedMessage {
  return msg.type === 'deckSubmitted'
}

// ============================================================================
// Message Factories
// ============================================================================

export function createConnectMessage(playerName: string, token?: string): ConnectMessage {
  return token ? { type: 'connect', playerName, token } : { type: 'connect', playerName }
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

// Sealed Draft Message Factories
export function createCreateSealedGameMessage(setCode: string): CreateSealedGameMessage {
  return { type: 'createSealedGame', setCode }
}

export function createJoinSealedGameMessage(sessionId: string): JoinSealedGameMessage {
  return { type: 'joinSealedGame', sessionId }
}

export function createSubmitSealedDeckMessage(deckList: Record<string, number>): SubmitSealedDeckMessage {
  return { type: 'submitSealedDeck', deckList }
}

// ============================================================================
// Lobby Client Messages
// ============================================================================

export interface CreateSealedLobbyMessage {
  readonly type: 'createSealedLobby'
  readonly setCode: string
  readonly boosterCount: number
  readonly maxPlayers: number
}

export interface JoinLobbyMessage {
  readonly type: 'joinLobby'
  readonly lobbyId: string
}

export interface StartSealedLobbyMessage {
  readonly type: 'startSealedLobby'
}

export interface LeaveLobbyMessage {
  readonly type: 'leaveLobby'
}

export interface UpdateLobbySettingsMessage {
  readonly type: 'updateLobbySettings'
  readonly boosterCount?: number
  readonly maxPlayers?: number
}

// Tournament Client Messages

export interface ReadyForNextRoundMessage {
  readonly type: 'readyForNextRound'
}

// Lobby Message Factories
export function createCreateSealedLobbyMessage(
  setCode: string,
  boosterCount: number = 6,
  maxPlayers: number = 8
): CreateSealedLobbyMessage {
  return { type: 'createSealedLobby', setCode, boosterCount, maxPlayers }
}

export function createJoinLobbyMessage(lobbyId: string): JoinLobbyMessage {
  return { type: 'joinLobby', lobbyId }
}

export function createStartSealedLobbyMessage(): StartSealedLobbyMessage {
  return { type: 'startSealedLobby' }
}

export function createLeaveLobbyMessage(): LeaveLobbyMessage {
  return { type: 'leaveLobby' }
}

export function createUpdateLobbySettingsMessage(
  settings: { boosterCount?: number; maxPlayers?: number }
): UpdateLobbySettingsMessage {
  return { type: 'updateLobbySettings', ...settings }
}

export function createReadyForNextRoundMessage(): ReadyForNextRoundMessage {
  return { type: 'readyForNextRound' }
}

// Lobby/Tournament Type Guards
export function isReconnectedMessage(msg: ServerMessage): msg is ReconnectedMessage {
  return msg.type === 'reconnected'
}

export function isLobbyCreatedMessage(msg: ServerMessage): msg is LobbyCreatedMessage {
  return msg.type === 'lobbyCreated'
}

export function isLobbyUpdateMessage(msg: ServerMessage): msg is LobbyUpdateMessage {
  return msg.type === 'lobbyUpdate'
}

export function isTournamentStartedMessage(msg: ServerMessage): msg is TournamentStartedMessage {
  return msg.type === 'tournamentStarted'
}

export function isTournamentMatchStartingMessage(msg: ServerMessage): msg is TournamentMatchStartingMessage {
  return msg.type === 'tournamentMatchStarting'
}

export function isTournamentByeMessage(msg: ServerMessage): msg is TournamentByeMessage {
  return msg.type === 'tournamentBye'
}

export function isRoundCompleteMessage(msg: ServerMessage): msg is RoundCompleteMessage {
  return msg.type === 'roundComplete'
}

export function isTournamentCompleteMessage(msg: ServerMessage): msg is TournamentCompleteMessage {
  return msg.type === 'tournamentComplete'
}
