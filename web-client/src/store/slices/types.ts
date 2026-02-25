/**
 * Shared types for store slices.
 */
import type { StateCreator } from 'zustand'
import type {
  EntityId,
  ClientGameState,
  ClientEvent,
  GameAction,
  LegalActionInfo,
  ConvokeCreatureInfo,
  GameOverReason,
  ErrorCode,
  PendingDecision,
  OpponentDecisionStatus,
  LobbyPlayerInfo,
  LobbySettings,
  PlayerStandingInfo,
  MatchResultInfo,
  ActiveMatchInfo,
  SpectatorPlayerState,
  SealedCardInfo,
  Step,
} from '../../types'
import type { ConnectionStatus } from '../../network/websocket'
import type { SpectatorCombatState, SpectatorDecisionStatus } from '../../types/messages'

// Re-export for convenience
export type { EntityId, ConnectionStatus }

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
  isOnThePlay: boolean
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
  /** If set, this targeting phase is for tapping permanents as a cost */
  isTapPermanentSelection?: boolean
  /** If set, this targeting phase is for discarding cards as a cost */
  isDiscardSelection?: boolean
  /** If set, this targeting phase is for returning permanents to hand as a cost */
  isBounceSelection?: boolean
  /** The original action info, used to chain sacrifice -> spell targeting -> damage distribution */
  pendingActionInfo?: LegalActionInfo
  /** Current target requirement index for multi-target spells (0-indexed) */
  currentRequirementIndex?: number
  /** All selected targets for each requirement (for multi-target spells) */
  allSelectedTargets?: readonly (readonly EntityId[])[]
  /** All target requirements (for multi-target spells) */
  targetRequirements?: LegalActionInfo['targetRequirements']
  /** If set, this spell requires damage distribution after target selection */
  requiresDamageDistribution?: boolean
  /** The zone the current targets are in (e.g., "Graveyard"). Set by server via targetRequirements. */
  targetZone?: string
  /** Description of the current target requirement (e.g., "non-Zombie creature") */
  targetDescription?: string
  /** Total number of target requirements for multi-target spells (for step indicator) */
  totalRequirements?: number
}

/**
 * Combat mode state for declaring attackers or blockers.
 */
export interface CombatState {
  mode: 'declareAttackers' | 'declareBlockers'
  /** Selected attackers (creature IDs) */
  selectedAttackers: readonly EntityId[]
  /** Blocker assignments: blocker ID -> attacker IDs (supports blocking multiple attackers) */
  blockerAssignments: Record<EntityId, EntityId[]>
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
 * Decision selection state for SelectCardsDecision with useTargetingUI=true.
 */
export interface DecisionSelectionState {
  /** The pending decision this selection is for */
  decisionId: string
  /** Valid card IDs that can be selected */
  validOptions: readonly EntityId[]
  /** Currently selected card IDs */
  selectedOptions: readonly EntityId[]
  /** Minimum selections required */
  minSelections: number
  /** Maximum selections allowed */
  maxSelections: number
  /** Prompt to display */
  prompt: string
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
 * Damage distribution state for DividedDamageEffect spells.
 */
export interface DamageDistributionState {
  /** The action info containing the spell being cast */
  actionInfo: LegalActionInfo
  /** The action with targets already set */
  action: import('../../types').CastSpellAction
  /** Card name for display */
  cardName: string
  /** Selected target entity IDs */
  targetIds: readonly EntityId[]
  /** Total damage to distribute */
  totalDamage: number
  /** Minimum damage per target (usually 1 per MTG rules) */
  minPerTarget: number
  /** Current distribution (targetId -> damage amount) */
  distribution: Record<EntityId, number>
}

/**
 * Distribute decision state for inline damage distribution on the board.
 * Used when the server sends a DistributeDecision (e.g., Butcher Orgg combat damage).
 */
export interface DistributeState {
  decisionId: string
  prompt: string
  totalAmount: number
  targets: readonly EntityId[]
  minPerTarget: number
  maxPerTarget?: Record<EntityId, number>
  distribution: Record<EntityId, number>
}

/**
 * Selected creature for Convoke with its payment choice.
 */
export interface ConvokeCreatureSelection {
  entityId: EntityId
  name: string
  /** If set, this creature pays for this color. If null, pays for generic. */
  payingColor: string | null
}

/**
 * Mana color selection state when activating "add one mana of any color" abilities.
 */
export interface ManaColorSelectionState {
  /** The action ready to submit (with costPayment already set) */
  action: GameAction
}

/**
 * Convoke selection state when casting spells with Convoke.
 */
export interface ConvokeSelectionState {
  actionInfo: LegalActionInfo
  cardName: string
  /** Original mana cost of the spell */
  manaCost: string
  /** Creatures that have been selected for Convoke */
  selectedCreatures: ConvokeCreatureSelection[]
  /** All valid creatures that can be tapped for Convoke */
  validCreatures: readonly ConvokeCreatureInfo[]
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
  picksPerRound: number
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
  /** Full ClientGameState for reusing GameBoard component (both hands masked) */
  gameState: ClientGameState | null
  /** Player 1's entity ID */
  player1Id: string | null
  /** Player 2's entity ID */
  player2Id: string | null
  player1Name: string
  player2Name: string
  // Legacy fields for backward compatibility
  player1: SpectatorPlayerState | null
  player2: SpectatorPlayerState | null
  currentPhase: string | null
  activePlayerId: string | null
  priorityPlayerId: string | null
  combat: SpectatorCombatState | null
  /** Pending decision status (null if no decision in progress) */
  decisionStatus: SpectatorDecisionStatus | null
  /** When true, the ReplayViewer handles its own UI — SpectatorGameBoard should not render */
  isReplay?: boolean
}

/**
 * A log entry for the game log panel.
 */
export interface LogEntry {
  description: string
  playerId: EntityId | null
  timestamp: number
  type: 'action' | 'turn' | 'combat' | 'system'
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
 * A morph face-up reveal animation.
 */
export interface RevealAnimation {
  id: string
  cardName: string
  imageUri: string | null
  isOpponent: boolean
  startTime: number
}

/**
 * A coin flip animation.
 */
export interface CoinFlipAnimation {
  id: string
  sourceName: string
  won: boolean
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

// ============================================================================
// Slice Creator Type
// ============================================================================

// Forward declaration - actual types are in each slice file
// This type allows slices to access the full store while defining their own subset
export type SliceCreator<T> = StateCreator<GameStore, [], [], T>

// Combined store type - imports from individual slices
// This is forward-declared here and the actual slices import from their own files
export type GameStore = {
  // Connection slice
  connectionStatus: ConnectionStatus
  playerId: EntityId | null
  sessionId: string | null
  pendingTournamentId: string | null
  connect: (playerName: string) => void
  disconnect: () => void
  setPendingTournamentId: (lobbyId: string | null) => void

  // Gameplay slice
  gameState: ClientGameState | null
  legalActions: readonly LegalActionInfo[]
  pendingDecision: PendingDecision | null
  opponentDecisionStatus: OpponentDecisionStatus | null
  mulliganState: MulliganState | null
  waitingForOpponentMulligan: boolean
  pendingEvents: readonly ClientEvent[]
  eventLog: readonly LogEntry[]
  gameOverState: GameOverState | null
  lastError: ErrorState | null
  fullControl: boolean
  priorityMode: import('../../types').PriorityModeValue
  stopOverrides: { myTurnStops: Step[]; opponentTurnStops: Step[] }
  nextStopPoint: string | null
  opponentName: string | null
  undoAvailable: boolean
  /** Seconds remaining on opponent's disconnect countdown (null = connected) */
  opponentDisconnectCountdown: number | null
  createGame: (deckList: Record<string, number>) => void
  joinGame: (sessionId: string, deckList: Record<string, number>) => void
  submitAction: (action: GameAction) => void
  submitDecision: (selectedCards: readonly EntityId[]) => void
  submitTargetsDecision: (selectedTargets: Record<number, readonly EntityId[]>) => void
  submitOrderedDecision: (orderedObjects: readonly EntityId[]) => void
  submitYesNoDecision: (choice: boolean) => void
  submitNumberDecision: (number: number) => void
  submitOptionDecision: (optionIndex: number) => void
  submitDistributeDecision: (distribution: Record<EntityId, number>) => void
  submitDamageAssignmentDecision: (assignments: Record<EntityId, number>) => void
  submitColorDecision: (color: string) => void
  submitManaSourcesDecision: (selectedSources: readonly EntityId[], autoPay: boolean) => void
  submitSplitPilesDecision: (piles: readonly (readonly EntityId[])[]) => void
  keepHand: () => void
  mulligan: () => void
  chooseBottomCards: (cardIds: readonly EntityId[]) => void
  toggleMulliganCard: (cardId: EntityId) => void
  concede: () => void
  requestUndo: () => void
  cancelGame: () => void
  setFullControl: (enabled: boolean) => void
  cyclePriorityMode: () => void
  toggleStopOverride: (step: Step, isMyTurn: boolean) => void
  returnToMenu: () => void
  clearError: () => void
  consumeEvent: () => ClientEvent | undefined

  // Lobby slice
  lobbyState: LobbyState | null
  tournamentState: TournamentState | null
  spectatingState: SpectatingState | null
  createTournamentLobby: (setCodes: string[], format?: 'SEALED' | 'DRAFT', boosterCount?: number, maxPlayers?: number, pickTimeSeconds?: number) => void
  joinLobby: (lobbyId: string) => void
  startLobby: () => void
  leaveLobby: () => void
  stopLobby: () => void
  updateLobbySettings: (settings: { setCodes?: string[]; format?: 'SEALED' | 'DRAFT'; boosterCount?: number; maxPlayers?: number; gamesPerMatch?: number; pickTimeSeconds?: number; picksPerRound?: number }) => void
  /** Disconnected tournament players: playerId -> info */
  disconnectedPlayers: Record<string, { playerName: string; secondsRemaining: number; disconnectedAt: number }>
  readyForNextRound: () => void
  spectateGame: (gameSessionId: string) => void
  stopSpectating: () => void
  setSpectatingState: (state: SpectatingState | null) => void
  addDisconnectTime: (playerId: string) => void
  kickPlayer: (playerId: string) => void
  leaveTournament: () => void

  // Draft slice
  deckBuildingState: DeckBuildingState | null
  createSealedGame: (setCode: string) => void
  joinSealedGame: (sessionId: string) => void
  addCardToDeck: (cardName: string) => void
  removeCardFromDeck: (cardName: string) => void
  clearDeck: () => void
  setLandCount: (landType: string, count: number) => void
  submitSealedDeck: () => void
  unsubmitDeck: () => void
  makePick: (cardNames: string[]) => void

  // UI slice
  selectedCardId: EntityId | null
  targetingState: TargetingState | null
  combatState: CombatState | null
  xSelectionState: XSelectionState | null
  convokeSelectionState: ConvokeSelectionState | null
  manaColorSelectionState: ManaColorSelectionState | null
  decisionSelectionState: DecisionSelectionState | null
  damageDistributionState: DamageDistributionState | null
  distributeState: DistributeState | null
  hoveredCardId: EntityId | null
  autoTapPreview: readonly EntityId[] | null
  draggingBlockerId: EntityId | null
  draggingCardId: EntityId | null
  revealedHandCardIds: readonly EntityId[] | null
  revealedCardsInfo: {
    cardIds: readonly EntityId[]
    cardNames: readonly string[]
    imageUris: readonly (string | null)[]
    source: string | null
    isYourReveal: boolean
  } | null
  opponentBlockerAssignments: Record<EntityId, EntityId[]> | null
  drawAnimations: readonly DrawAnimation[]
  damageAnimations: readonly DamageAnimation[]
  revealAnimations: readonly RevealAnimation[]
  coinFlipAnimations: readonly CoinFlipAnimation[]
  selectCard: (cardId: EntityId | null) => void
  hoverCard: (cardId: EntityId | null) => void
  startTargeting: (state: TargetingState) => void
  addTarget: (targetId: EntityId) => void
  removeTarget: (targetId: EntityId) => void
  cancelTargeting: () => void
  confirmTargeting: () => void
  startCombat: (state: CombatState) => void
  toggleAttacker: (creatureId: EntityId) => void
  assignBlocker: (blockerId: EntityId, attackerId: EntityId) => void
  removeBlockerAssignment: (blockerId: EntityId) => void
  clearBlockerAssignments: () => void
  startDraggingBlocker: (blockerId: EntityId) => void
  stopDraggingBlocker: () => void
  startDraggingCard: (cardId: EntityId) => void
  stopDraggingCard: () => void
  confirmCombat: () => void
  cancelCombat: () => void
  attackWithAll: () => void
  clearAttackers: () => void
  clearCombat: () => void
  startXSelection: (state: XSelectionState) => void
  updateXValue: (x: number) => void
  cancelXSelection: () => void
  confirmXSelection: () => void
  startConvokeSelection: (state: ConvokeSelectionState) => void
  toggleConvokeCreature: (entityId: EntityId, name: string, payingColor: string | null) => void
  cancelConvokeSelection: () => void
  confirmConvokeSelection: () => void
  startManaColorSelection: (state: ManaColorSelectionState) => void
  confirmManaColorSelection: (color: string) => void
  cancelManaColorSelection: () => void
  startDecisionSelection: (state: DecisionSelectionState) => void
  toggleDecisionSelection: (cardId: EntityId) => void
  cancelDecisionSelection: () => void
  confirmDecisionSelection: () => void
  startDamageDistribution: (state: DamageDistributionState) => void
  updateDamageDistribution: (targetId: EntityId, amount: number) => void
  cancelDamageDistribution: () => void
  confirmDamageDistribution: () => void
  initDistribute: (state: DistributeState) => void
  incrementDistribute: (targetId: EntityId) => void
  decrementDistribute: (targetId: EntityId) => void
  confirmDistribute: () => void
  clearDistribute: () => void
  showRevealedHand: (cardIds: readonly EntityId[]) => void
  dismissRevealedHand: () => void
  showRevealedCards: (cardIds: readonly EntityId[], cardNames: readonly string[], imageUris: readonly (string | null)[], source: string | null, isYourReveal: boolean) => void
  dismissRevealedCards: () => void
  addDrawAnimation: (animation: DrawAnimation) => void
  removeDrawAnimation: (id: string) => void
  addDamageAnimation: (animation: DamageAnimation) => void
  removeDamageAnimation: (id: string) => void
  addRevealAnimation: (animation: RevealAnimation) => void
  removeRevealAnimation: (id: string) => void
  addCoinFlipAnimation: (animation: CoinFlipAnimation) => void
  removeCoinFlipAnimation: (id: string) => void
  setAutoTapPreview: (preview: readonly EntityId[] | null) => void
}
