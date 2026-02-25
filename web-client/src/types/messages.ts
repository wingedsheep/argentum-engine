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
  | GameCancelledMessage
  | StateUpdateMessage
  | MulliganDecisionMessage
  | ChooseBottomCardsMessage
  | MulliganCompleteMessage
  | WaitingForOpponentMulliganMessage
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
  | LobbyStoppedMessage
  // Draft Messages
  | DraftPackReceivedMessage
  | DraftPickMadeMessage
  | DraftPickConfirmedMessage
  | DraftCompleteMessage
  | DraftTimerUpdateMessage
  // Tournament Messages
  | TournamentStartedMessage
  | TournamentMatchStartingMessage
  | TournamentByeMessage
  | RoundCompleteMessage
  | MatchCompleteMessage
  | PlayerReadyForRoundMessage
  | TournamentCompleteMessage
  // Spectating Messages
  | ActiveMatchesMessage
  | SpectatorStateUpdateMessage
  | SpectatingStartedMessage
  | SpectatingStoppedMessage
  // Combat UI Messages
  | OpponentBlockerAssignmentsMessage
  // Disconnect Messages
  | OpponentDisconnectedMessage
  | OpponentReconnectedMessage
  | TournamentPlayerDisconnectedMessage
  | TournamentPlayerReconnectedMessage

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
  readonly context: 'lobby' | 'drafting' | 'deckBuilding' | 'game' | 'tournament' | null
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
 * Game was cancelled before it started (by the creator).
 */
export interface GameCancelledMessage {
  readonly type: 'gameCancelled'
}

/**
 * Summary of opponent's pending decision (masked for privacy).
 * Sent to the non-deciding player so they know the opponent is making a choice.
 */
export interface OpponentDecisionStatus {
  readonly decisionType: string
  readonly displayText: string
  readonly sourceName?: string | null
}

/**
 * Game state update after an action is executed.
 */
/**
 * Per-step stop overrides echoed back from the server.
 */
export interface StopOverrideInfo {
  readonly myTurnStops: readonly string[]
  readonly opponentTurnStops: readonly string[]
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
  /** Where passing priority will take the player (e.g., "Combat", "End Step", "My turn") */
  readonly nextStopPoint?: string | null
  /** Summary of opponent's pending decision (null if opponent has no decision) */
  readonly opponentDecisionStatus?: OpponentDecisionStatus | null
  /** Per-step stop overrides echoed back for client sync */
  readonly stopOverrides?: StopOverrideInfo | null
  /** Whether the player can undo their last action */
  readonly undoAvailable?: boolean
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
  /** The entity that triggered this decision (e.g., the blocked creature for combat triggers) */
  readonly triggeringEntityId?: EntityId
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
  /** If true, use targeting UI (click on board) instead of modal overlay */
  readonly useTargetingUI?: boolean
  /** Label describing where selected cards go (e.g., "Put on bottom") */
  readonly selectedLabel?: string | null
  /** Label describing where non-selected cards go (e.g., "Put on top") */
  readonly remainderLabel?: string | null
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
 * Player must choose from a list of string options (e.g., creature types).
 * Used by effects like Artificial Evolution.
 */
export interface ChooseOptionDecision extends PendingDecisionBase {
  readonly type: 'ChooseOptionDecision'
  readonly options: readonly string[]
  readonly defaultSearch?: string | null
  /** Maps option index to entity IDs of cards associated with that option (for preview) */
  readonly optionCardIds?: Record<number, readonly EntityId[]> | null
}

/**
 * Player must distribute an amount (e.g., damage) among targets.
 * Used for effects like Forked Lightning that deal divided damage.
 */
export interface DistributeDecision extends PendingDecisionBase {
  readonly type: 'DistributeDecision'
  readonly totalAmount: number
  readonly targets: readonly EntityId[]
  readonly minPerTarget: number
  readonly maxPerTarget?: Record<EntityId, number>
}

/**
 * Player must choose a color (e.g., protection from color of your choice).
 */
export interface ChooseColorDecision extends PendingDecisionBase {
  readonly type: 'ChooseColorDecision'
  readonly availableColors: readonly string[]
}

/**
 * Information about a mana source available for manual selection.
 */
export interface ManaSourceOption {
  readonly entityId: EntityId
  readonly name: string
  readonly producesColors: readonly string[]
  readonly producesColorless: boolean
}

/**
 * Player must select mana sources to pay a cost.
 * Includes an "Auto Pay" shortcut.
 */
export interface SelectManaSourcesDecision extends PendingDecisionBase {
  readonly type: 'SelectManaSourcesDecision'
  readonly availableSources: readonly ManaSourceOption[]
  readonly requiredCost: string
  readonly autoPaySuggestion: readonly EntityId[]
  readonly canDecline?: boolean
}

/**
 * Player must assign combat damage from an attacker to blockers (and defending player for trample).
 * Used when a creature with trample or multiple blockers needs manual damage assignment.
 */
export interface AssignDamageDecision extends PendingDecisionBase {
  readonly type: 'AssignDamageDecision'
  readonly attackerId: EntityId
  readonly availablePower: number
  readonly orderedTargets: readonly EntityId[]
  readonly defenderId: EntityId | null
  readonly minimumAssignments: Record<EntityId, number>
  readonly defaultAssignments: Record<EntityId, number>
  readonly hasTrample: boolean
  readonly hasDeathtouch: boolean
}

/**
 * Player must split cards into piles (e.g., Surveil, Fact or Fiction).
 * Each card is assigned to one of the labeled piles.
 */
export interface SplitPilesDecision extends PendingDecisionBase {
  readonly type: 'SplitPilesDecision'
  readonly cards: readonly EntityId[]
  readonly numberOfPiles: number
  readonly pileLabels: readonly string[]
  readonly cardInfo?: Record<EntityId, SearchCardInfo> | null
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
  | ChooseOptionDecision
  | DistributeDecision
  | ChooseColorDecision
  | SelectManaSourcesDecision
  | AssignDamageDecision
  | SplitPilesDecision

/**
 * Information about a single target requirement for legal actions.
 * Includes valid targets so the client knows which entities can be selected.
 */
export interface LegalActionTargetInfo {
  readonly index: number
  readonly description: string
  readonly minTargets: number
  readonly maxTargets: number
  readonly validTargets: readonly EntityId[]
  /** The zone these targets are in (e.g., "Graveyard" for graveyard targets). Null for battlefield targets. */
  readonly targetZone?: string
}

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
  /** Multiple target requirements for spells with multiple distinct targets */
  readonly targetRequirements?: readonly LegalActionTargetInfo[]
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
  /** Whether this action can currently be afforded/executed (default true) */
  readonly isAffordable?: boolean
  /** Additional cost info (sacrifice, etc.) */
  readonly additionalCostInfo?: AdditionalCostInfo
  /** Whether this spell has Convoke */
  readonly hasConvoke?: boolean
  /** Creatures that can be tapped to help pay for Convoke */
  readonly validConvokeCreatures?: readonly ConvokeCreatureInfo[]
  /** The spell's mana cost string for Convoke UI display */
  readonly manaCostString?: string
  /** Whether this spell requires damage distribution at cast time (for DividedDamageEffect) */
  readonly requiresDamageDistribution?: boolean
  /** Total damage to distribute for DividedDamageEffect spells */
  readonly totalDamageToDistribute?: number
  /** Minimum damage per target (usually 1 per MTG rules) */
  readonly minDamagePerTarget?: number
  /** Preview of which lands/sources would be auto-tapped if this spell is cast (for UI highlighting) */
  readonly autoTapPreview?: readonly EntityId[]
  /** Whether this ability produces mana of any color and needs a color choice from the player */
  readonly requiresManaColorChoice?: boolean
  /** Source zone if this action is from a non-hand zone (e.g., "LIBRARY" for Future Sight) */
  readonly sourceZone?: string
}

/**
 * Information about a creature that can be tapped for Convoke.
 */
export interface ConvokeCreatureInfo {
  readonly entityId: EntityId
  readonly name: string
  /** Colors this creature can pay (based on its colors) */
  readonly colors: readonly string[]
}

/**
 * Information about additional costs for a spell.
 */
export interface AdditionalCostInfo {
  readonly description: string
  readonly costType: string
  readonly validSacrificeTargets?: readonly EntityId[]
  readonly sacrificeCount?: number
  readonly validTapTargets?: readonly EntityId[]
  readonly tapCount?: number
  readonly validDiscardTargets?: readonly EntityId[]
  readonly discardCount?: number
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
  readonly isOnThePlay: boolean
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
 * Waiting for opponent to complete their mulligan.
 */
export interface WaitingForOpponentMulliganMessage {
  readonly type: 'waitingForOpponentMulligan'
}

/**
 * Game has ended.
 */
export interface GameOverMessage {
  readonly type: 'gameOver'
  readonly winnerId: EntityId | null
  readonly reason: GameOverReason
  readonly message?: string
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
 * An official ruling for a sealed card.
 */
export interface SealedRuling {
  readonly date: string
  readonly text: string
}

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
  readonly rulings?: readonly SealedRuling[]
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

export interface AvailableSet {
  readonly code: string
  readonly name: string
  readonly incomplete?: boolean
  readonly block?: string
  readonly implementedCount?: number
  readonly totalCount?: number
}

export interface LobbySettings {
  readonly setCodes: readonly string[]
  readonly setNames: readonly string[]
  readonly availableSets: readonly AvailableSet[]
  readonly format: 'SEALED' | 'DRAFT'
  readonly boosterCount: number
  readonly maxPlayers: number
  readonly pickTimeSeconds: number
  readonly picksPerRound: number  // Draft only: 1 or 2 (Pick 2 mode)
  readonly gamesPerMatch: number
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

/**
 * Lobby was stopped/disbanded by the host.
 */
export interface LobbyStoppedMessage {
  readonly type: 'lobbyStopped'
}

// ============================================================================
// Draft Server Messages
// ============================================================================

/**
 * Player received a pack to draft from.
 */
export interface DraftPackReceivedMessage {
  readonly type: 'draftPackReceived'
  readonly packNumber: number
  readonly pickNumber: number
  readonly cards: readonly SealedCardInfo[]
  readonly timeRemainingSeconds: number
  readonly passDirection: 'LEFT' | 'RIGHT'
  readonly picksPerRound: number  // Cards to pick this round (1 or 2)
  readonly pickedCards?: readonly SealedCardInfo[]  // Cards already picked (for reconnect)
}

/**
 * A player made a pick (broadcast to all).
 */
export interface DraftPickMadeMessage {
  readonly type: 'draftPickMade'
  readonly playerId: string
  readonly playerName: string
  readonly waitingForPlayers: readonly string[]
}

/**
 * Confirmation that your pick was accepted.
 */
export interface DraftPickConfirmedMessage {
  readonly type: 'draftPickConfirmed'
  readonly cardNames: readonly string[]
  readonly totalPicked: number
}

/**
 * Draft is complete, transitioning to deck building.
 */
export interface DraftCompleteMessage {
  readonly type: 'draftComplete'
  readonly pickedCards: readonly SealedCardInfo[]
  readonly basicLands: readonly SealedCardInfo[]
}

/**
 * Timer update during draft.
 */
export interface DraftTimerUpdateMessage {
  readonly type: 'draftTimerUpdate'
  readonly secondsRemaining: number
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
  readonly gamesWon?: number
  readonly gamesLost?: number
  readonly lifeDifferential?: number
  readonly rank?: number
  /** Tiebreaker reason: "HEAD_TO_HEAD", "H2H_GAMES", "LIFE_DIFF", "TIED", or null if no tie */
  readonly tiebreakerReason?: string | null
}

export interface MatchResultInfo {
  readonly player1Name: string
  readonly player2Name: string
  readonly player1Id: string
  readonly player2Id: string | null
  readonly winnerId: string | null
  readonly isDraw: boolean
  readonly isBye: boolean
}

export interface TournamentStartedMessage {
  readonly type: 'tournamentStarted'
  readonly lobbyId: string
  readonly totalRounds: number
  readonly standings: readonly PlayerStandingInfo[]
  /** Name of first opponent (null if BYE) */
  readonly nextOpponentName?: string | null
  /** True if player has a BYE in the first round */
  readonly nextRoundHasBye?: boolean
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
  /** Name of next opponent (null if BYE or tournament complete) */
  readonly nextOpponentName?: string | null
  /** True if player has a BYE in the next round */
  readonly nextRoundHasBye?: boolean
  /** True if the tournament is complete (no more rounds) */
  readonly isTournamentComplete?: boolean
}

export interface MatchCompleteMessage {
  readonly type: 'matchComplete'
  readonly lobbyId: string
  readonly round: number
  readonly results: readonly MatchResultInfo[]
  readonly standings: readonly PlayerStandingInfo[]
  /** Name of next opponent (null if BYE or tournament complete) */
  readonly nextOpponentName?: string | null
  /** True if player has a BYE in the next round */
  readonly nextRoundHasBye?: boolean
  /** True if the tournament is complete (no more rounds) */
  readonly isTournamentComplete?: boolean
}

export interface PlayerReadyForRoundMessage {
  readonly type: 'playerReadyForRound'
  readonly lobbyId: string
  readonly playerId: string
  readonly playerName: string
  readonly readyPlayerIds: readonly string[]
  readonly totalConnectedPlayers: number
}

export interface TournamentCompleteMessage {
  readonly type: 'tournamentComplete'
  readonly lobbyId: string
  readonly finalStandings: readonly PlayerStandingInfo[]
}

// ============================================================================
// Spectating Messages
// ============================================================================

export interface ActiveMatchInfo {
  readonly gameSessionId: string
  readonly player1Name: string
  readonly player2Name: string
  readonly player1Life: number
  readonly player2Life: number
}

export interface ActiveMatchesMessage {
  readonly type: 'activeMatches'
  readonly lobbyId: string
  readonly round: number
  readonly matches: readonly ActiveMatchInfo[]
  readonly standings: readonly PlayerStandingInfo[]
}

export interface SpectatorCardInfo {
  readonly entityId: string
  readonly name: string
  readonly imageUri: string | null
  readonly isTapped: boolean
  readonly power: number | null
  readonly toughness: number | null
  readonly damage: number
  readonly cardTypes: readonly string[]
  readonly isAttacking: boolean
  readonly targets: readonly SpectatorTarget[]
}

export type SpectatorTarget =
  | { readonly type: 'Player'; readonly playerId: string }
  | { readonly type: 'Permanent'; readonly entityId: string }
  | { readonly type: 'Spell'; readonly spellEntityId: string }

export interface SpectatorAttacker {
  readonly creatureId: string
  readonly blockedBy: readonly string[]
}

export interface SpectatorCombatState {
  readonly attackingPlayerId: string
  readonly defendingPlayerId: string
  readonly attackers: readonly SpectatorAttacker[]
}

export interface SpectatorPlayerState {
  readonly playerId: string
  readonly playerName: string
  readonly life: number
  readonly handSize: number
  readonly librarySize: number
  readonly battlefield: readonly SpectatorCardInfo[]
  readonly graveyard: readonly SpectatorCardInfo[]
  readonly stack: readonly SpectatorCardInfo[]
}

/**
 * Summary of a pending decision for spectators.
 * Includes player name since spectators need to know who is deciding.
 */
export interface SpectatorDecisionStatus {
  readonly playerName: string
  readonly playerId: string
  readonly decisionType: string
  readonly displayText: string
  readonly sourceName?: string | null
}

export interface SpectatorStateUpdateMessage {
  readonly type: 'spectatorStateUpdate'
  readonly gameSessionId: string
  /** Full ClientGameState for reusing GameBoard component (both hands masked) */
  readonly gameState?: ClientGameState | null
  /** Player 1's entity ID */
  readonly player1Id?: string | null
  /** Player 2's entity ID */
  readonly player2Id?: string | null
  /** Player 1 name */
  readonly player1Name?: string | null
  /** Player 2 name */
  readonly player2Name?: string | null
  // Legacy fields for backward compatibility
  readonly player1: SpectatorPlayerState
  readonly player2: SpectatorPlayerState
  readonly currentPhase: string
  readonly activePlayerId: string | null
  readonly priorityPlayerId: string | null
  readonly combat: SpectatorCombatState | null
  /** Pending decision status (null if no decision in progress) */
  readonly decisionStatus?: SpectatorDecisionStatus | null
}

export interface SpectatingStartedMessage {
  readonly type: 'spectatingStarted'
  readonly gameSessionId: string
  readonly player1Name: string
  readonly player2Name: string
}

export interface SpectatingStoppedMessage {
  readonly type: 'spectatingStopped'
}

// ============================================================================
// Combat UI Messages
// ============================================================================

/**
 * Opponent's tentative blocker assignments during declare blockers phase.
 * Sent to the attacking player in real-time.
 */
export interface OpponentBlockerAssignmentsMessage {
  readonly type: 'opponentBlockerAssignments'
  /** Map of blocker creature ID to attacker creature IDs */
  readonly assignments: Record<EntityId, EntityId[]>
}

/**
 * Opponent has disconnected. A countdown timer is running and they will
 * auto-concede if they don't reconnect in time.
 */
export interface OpponentDisconnectedMessage {
  readonly type: 'opponentDisconnected'
  readonly secondsRemaining: number
}

/**
 * Opponent has reconnected. Cancels the disconnect countdown.
 */
export interface OpponentReconnectedMessage {
  readonly type: 'opponentReconnected'
}

/**
 * A tournament player has disconnected. Shown to all players in the lobby.
 */
export interface TournamentPlayerDisconnectedMessage {
  readonly type: 'tournamentPlayerDisconnected'
  readonly playerId: string
  readonly playerName: string
  readonly secondsRemaining: number
}

/**
 * A disconnected tournament player has reconnected.
 */
export interface TournamentPlayerReconnectedMessage {
  readonly type: 'tournamentPlayerReconnected'
  readonly playerId: string
  readonly playerName: string
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
  | CancelGameMessage
  // Sealed Draft Messages
  | CreateSealedGameMessage
  | JoinSealedGameMessage
  | SubmitSealedDeckMessage
  // Lobby Messages
  | CreateTournamentLobbyMessage
  | JoinLobbyMessage
  | StartTournamentLobbyMessage
  | MakePickMessage
  | LeaveLobbyMessage
  | StopLobbyMessage
  | UnsubmitDeckMessage
  | UpdateLobbySettingsMessage
  // Tournament Messages
  | ReadyForNextRoundMessage
  | SpectateGameMessage
  | StopSpectatingMessage
  | AddDisconnectTimeMessage
  | KickPlayerMessage
  // Combat UI Messages
  | UpdateBlockerAssignmentsMessage
  // Game Settings Messages
  | SetFullControlMessage
  | SetStopOverridesMessage
  // Undo
  | RequestUndoMessage

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

/**
 * Cancel a game that hasn't started yet (waiting for opponent).
 */
export interface CancelGameMessage {
  readonly type: 'cancelGame'
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

export function isGameCancelledMessage(msg: ServerMessage): msg is GameCancelledMessage {
  return msg.type === 'gameCancelled'
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

export function isWaitingForOpponentMulliganMessage(msg: ServerMessage): msg is WaitingForOpponentMulliganMessage {
  return msg.type === 'waitingForOpponentMulligan'
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

export function createCancelGameMessage(): CancelGameMessage {
  return { type: 'cancelGame' }
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

export interface CreateTournamentLobbyMessage {
  readonly type: 'createTournamentLobby'
  readonly setCodes: readonly string[]
  readonly format: 'SEALED' | 'DRAFT'
  readonly boosterCount: number
  readonly maxPlayers: number
  readonly pickTimeSeconds: number
}

export interface JoinLobbyMessage {
  readonly type: 'joinLobby'
  readonly lobbyId: string
}

export interface StartTournamentLobbyMessage {
  readonly type: 'startTournamentLobby'
}

export interface MakePickMessage {
  readonly type: 'makePick'
  readonly cardNames: readonly string[]
}

export interface LeaveLobbyMessage {
  readonly type: 'leaveLobby'
}

export interface StopLobbyMessage {
  readonly type: 'stopLobby'
}

export interface UnsubmitDeckMessage {
  readonly type: 'unsubmitDeck'
}

export interface UpdateLobbySettingsMessage {
  readonly type: 'updateLobbySettings'
  readonly setCodes?: readonly string[]
  readonly format?: 'SEALED' | 'DRAFT'
  readonly boosterCount?: number
  readonly maxPlayers?: number
  readonly gamesPerMatch?: number
  readonly pickTimeSeconds?: number
  readonly picksPerRound?: number
}

// Tournament Client Messages

export interface ReadyForNextRoundMessage {
  readonly type: 'readyForNextRound'
}

export interface SpectateGameMessage {
  readonly type: 'spectateGame'
  readonly gameSessionId: string
}

export interface StopSpectatingMessage {
  readonly type: 'stopSpectating'
}

export interface AddDisconnectTimeMessage {
  readonly type: 'addDisconnectTime'
  readonly playerId: string
}

export interface KickPlayerMessage {
  readonly type: 'kickPlayer'
  readonly playerId: string
}

// Combat UI Client Messages

/**
 * Update tentative blocker assignments during declare blockers phase.
 * Sent in real-time as the defending player assigns blockers.
 */
export interface UpdateBlockerAssignmentsMessage {
  readonly type: 'updateBlockerAssignments'
  /** Map of blocker creature ID to attacker creature IDs */
  readonly assignments: Record<EntityId, EntityId[]>
}

// Game Settings Client Messages

/**
 * Toggle full control mode for the current game.
 * When enabled, auto-pass is disabled and player receives priority at every possible point.
 */
export interface SetFullControlMessage {
  readonly type: 'setFullControl'
  readonly enabled: boolean
}

/**
 * Set per-step stop overrides for the current game.
 * When a stop is set for a step, auto-pass will not skip that step.
 */
export interface SetStopOverridesMessage {
  readonly type: 'setStopOverrides'
  readonly myTurnStops: readonly string[]
  readonly opponentTurnStops: readonly string[]
}

/**
 * Request to undo the last non-respondable action.
 */
export interface RequestUndoMessage {
  readonly type: 'requestUndo'
}

// Lobby Message Factories
export function createCreateTournamentLobbyMessage(
  setCodes: readonly string[],
  format: 'SEALED' | 'DRAFT' = 'SEALED',
  boosterCount: number = 6,
  maxPlayers: number = 8,
  pickTimeSeconds: number = 45
): CreateTournamentLobbyMessage {
  return { type: 'createTournamentLobby', setCodes, format, boosterCount, maxPlayers, pickTimeSeconds }
}

// Backwards compatibility alias
export function createCreateSealedLobbyMessage(
  setCode: string,
  boosterCount: number = 6,
  maxPlayers: number = 8
): CreateTournamentLobbyMessage {
  return createCreateTournamentLobbyMessage([setCode], 'SEALED', boosterCount, maxPlayers, 45)
}

export function createJoinLobbyMessage(lobbyId: string): JoinLobbyMessage {
  return { type: 'joinLobby', lobbyId }
}

export function createStartTournamentLobbyMessage(): StartTournamentLobbyMessage {
  return { type: 'startTournamentLobby' }
}

// Backwards compatibility alias
export function createStartSealedLobbyMessage(): StartTournamentLobbyMessage {
  return createStartTournamentLobbyMessage()
}

export function createMakePickMessage(cardNames: string[]): MakePickMessage {
  return { type: 'makePick', cardNames }
}

export function createLeaveLobbyMessage(): LeaveLobbyMessage {
  return { type: 'leaveLobby' }
}

export function createStopLobbyMessage(): StopLobbyMessage {
  return { type: 'stopLobby' }
}

export function createUnsubmitDeckMessage(): UnsubmitDeckMessage {
  return { type: 'unsubmitDeck' }
}

export function createUpdateLobbySettingsMessage(
  settings: {
    setCodes?: readonly string[]
    format?: 'SEALED' | 'DRAFT'
    boosterCount?: number
    maxPlayers?: number
    gamesPerMatch?: number
    pickTimeSeconds?: number
    picksPerRound?: number
  }
): UpdateLobbySettingsMessage {
  return { type: 'updateLobbySettings', ...settings }
}

export function createReadyForNextRoundMessage(): ReadyForNextRoundMessage {
  return { type: 'readyForNextRound' }
}

export function createSpectateGameMessage(gameSessionId: string): SpectateGameMessage {
  return { type: 'spectateGame', gameSessionId }
}

export function createStopSpectatingMessage(): StopSpectatingMessage {
  return { type: 'stopSpectating' }
}

export function createAddDisconnectTimeMessage(playerId: string): AddDisconnectTimeMessage {
  return { type: 'addDisconnectTime', playerId }
}

export function createKickPlayerMessage(playerId: string): KickPlayerMessage {
  return { type: 'kickPlayer', playerId }
}

export function createUpdateBlockerAssignmentsMessage(
  assignments: Record<EntityId, EntityId[]>
): UpdateBlockerAssignmentsMessage {
  return { type: 'updateBlockerAssignments', assignments }
}

// Game Settings Message Factories

export function createSetFullControlMessage(enabled: boolean): SetFullControlMessage {
  return { type: 'setFullControl', enabled }
}

export function createSetStopOverridesMessage(myTurnStops: readonly string[], opponentTurnStops: readonly string[]): SetStopOverridesMessage {
  return { type: 'setStopOverrides', myTurnStops, opponentTurnStops }
}

export function createRequestUndoMessage(): RequestUndoMessage {
  return { type: 'requestUndo' }
}

// Draft Type Guards
export function isDraftPackReceivedMessage(msg: ServerMessage): msg is DraftPackReceivedMessage {
  return msg.type === 'draftPackReceived'
}

export function isDraftPickMadeMessage(msg: ServerMessage): msg is DraftPickMadeMessage {
  return msg.type === 'draftPickMade'
}

export function isDraftPickConfirmedMessage(msg: ServerMessage): msg is DraftPickConfirmedMessage {
  return msg.type === 'draftPickConfirmed'
}

export function isDraftCompleteMessage(msg: ServerMessage): msg is DraftCompleteMessage {
  return msg.type === 'draftComplete'
}

export function isDraftTimerUpdateMessage(msg: ServerMessage): msg is DraftTimerUpdateMessage {
  return msg.type === 'draftTimerUpdate'
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

export function isLobbyStoppedMessage(msg: ServerMessage): msg is LobbyStoppedMessage {
  return msg.type === 'lobbyStopped'
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

export function isMatchCompleteMessage(msg: ServerMessage): msg is MatchCompleteMessage {
  return msg.type === 'matchComplete'
}

export function isTournamentCompleteMessage(msg: ServerMessage): msg is TournamentCompleteMessage {
  return msg.type === 'tournamentComplete'
}

// Spectating Type Guards
export function isActiveMatchesMessage(msg: ServerMessage): msg is ActiveMatchesMessage {
  return msg.type === 'activeMatches'
}

export function isSpectatorStateUpdateMessage(msg: ServerMessage): msg is SpectatorStateUpdateMessage {
  return msg.type === 'spectatorStateUpdate'
}

export function isSpectatingStartedMessage(msg: ServerMessage): msg is SpectatingStartedMessage {
  return msg.type === 'spectatingStarted'
}

export function isSpectatingStoppedMessage(msg: ServerMessage): msg is SpectatingStoppedMessage {
  return msg.type === 'spectatingStopped'
}
