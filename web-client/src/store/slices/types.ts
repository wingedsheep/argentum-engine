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
  WaterbendPermanentInfo,
  TapForPowerCreatureInfo,
  DelveCardInfo,
  HarmonizeCreatureInfo,

  GameOverReason,
  ErrorCode,
  PendingDecision,
  OpponentDecisionStatus,
  LobbyPlayerInfo,
  LobbySettings,
  TournamentFormat,
  CommanderPreset,
  PlayerStandingInfo,
  MatchResultInfo,
  ActiveMatchInfo,
  FfaStandingInfo,
  LobbyGameMode,
  AttackMode,
  SpectatorPlayerState,
  SealedCardInfo,
  Step,
  AvailableSet,
  QuickGameLobbyStateMessage,
  DeckFormat,
  YieldKind,
} from '@/types'
import type { ConnectionStatus } from '@/network/websocket.ts'
import type { CounterRemovalCreatureInfo, SpectatorCombatState, SpectatorDecisionStatus } from '@/types/messages.ts'

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
  /** If set, this targeting phase is for revealing cards from hand as a cost */
  isRevealSelection?: boolean
  /** If set, this targeting phase is for returning permanents to hand as a cost */
  isBounceSelection?: boolean
  /** If set, this targeting phase is for beholding (choosing from battlefield or hand) */
  isBeholdSelection?: boolean
  /**
   * If set, this targeting phase is for selecting Craft materials across the activator's
   * battlefield and graveyard (CR 702.167a-b). Materials live in both zones simultaneously
   * — render with a dedicated overlay rather than the single-zone battlefield/graveyard
   * targeting flows.
   */
  isCraftMaterialSelection?: boolean
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
  /** Name of the card that initiated this targeting (shown in overlay header) */
  sourceCardName?: string
  /** Transient warning shown when the user tries an illegal toggle (e.g. clicking past max
   * on a multi-target step). Cleared on the next successful add/remove. */
  warning?: string | null
}

/**
 * Combat mode state for declaring attackers or blockers.
 */
export interface CombatState {
  mode: 'declareAttackers' | 'declareBlockers'
  /**
   * The seat this declaration is being made for, from the legal action's
   * `playerId`. Matters in multiplayer: a hotseat connection declares for the
   * acting seat, and a defender's blocks are scoped to the attackers attacking
   * *them* (CR 509.1b). Null when unknown (legacy 2-player fallbacks apply).
   */
  actingSeat: EntityId | null
  /**
   * Multiplayer declare-attackers: the last defender the player assigned —
   * newly selected attackers default to it (sticky assignment). Null until the
   * first defender pick; never set in 2-player games (the sole opponent is the
   * implicit default).
   */
  stickyDefenderId: EntityId | null
  /** Selected attackers (creature IDs) */
  selectedAttackers: readonly EntityId[]
  /** Attack target per attacker: attacker ID -> defender ID (player or planeswalker). If absent, defaults to opponent player. */
  attackerTargets: Readonly<Record<EntityId, EntityId>>
  /** Planeswalker IDs that can be attacked */
  validAttackTargets: readonly EntityId[]
  /** Blocker assignments: blocker ID -> attacker IDs (supports blocking multiple attackers) */
  blockerAssignments: Record<EntityId, EntityId[]>
  /** Valid creatures that can participate (attackers or blockers depending on mode) */
  validCreatures: readonly EntityId[]
  /** Creatures that must attack this combat (from MustAttack, Taunt, etc.) */
  mandatoryAttackers: readonly EntityId[]
  /** For blockers mode: creatures that are attacking */
  attackingCreatures: readonly EntityId[]
  /** For blockers mode: attackers that must be blocked by all creatures able to block them */
  mustBeBlockedAttackers: readonly EntityId[]
  /** Max block counts for blockers that can block more than one attacker (blocker ID -> max attackers) */
  blockerMaxBlockCounts: Readonly<Record<EntityId, number>>
  /**
   * Banding bands declared during declare-attackers (CR 702.21): each entry is the set of
   * attacker IDs grouped into one band. Assembled client-side by dragging an attacker onto
   * another (see `linkBand`); legality is re-checked server-side on confirm. Empty otherwise.
   */
  bands: readonly (readonly EntityId[])[]
}

/**
 * Game over state when the game has ended.
 */
export interface GameOverState {
  winnerId: EntityId | null
  reason: GameOverReason
  result: 'win' | 'lose' | 'draw'
  message?: string | undefined
  gameId?: string | undefined
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
  /** Transient warning shown when the user tries an illegal toggle (e.g. clicking past max
   * on a multi-select step). Cleared on the next successful toggle. */
  warning?: string | null
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
 * Mana source selection state for pre-cast mana source selection.
 * The player clicks sources on the battlefield to toggle them before casting.
 */
export interface ManaSelectionState {
  /** The action we're selecting mana for */
  action: GameAction
  /** The full legal action info (needed to route through executeAction on confirm) */
  actionInfo: LegalActionInfo
  /** All source entity IDs available for selection */
  validSources: readonly EntityId[]
  /** Currently selected source IDs */
  selectedSources: readonly EntityId[]
  /** The mana cost string (e.g., "{2}{R}") */
  manaCost: string
  /** X value if spell has X cost */
  xValue: number
  /**
   * Generic mana the Harmonize creature-tap removes from the cost (the tapped
   * creature's power), or 0 when none is tapped / not a Harmonize cast. Reduces
   * both the pre-selected sources and the generic shown in the confirmation HUD.
   */
  harmonizeReduction: number
  /** Color production per source: entityId -> list of color symbols (W/U/B/R/G) */
  sourceColors: Readonly<Record<EntityId, readonly string[]>>
  /** Mana amount per source: entityId -> amount (e.g., 3 for Gilded Lotus) */
  sourceManaAmounts: Readonly<Record<EntityId, number>>
}

/**
 * Variable-blight (`AdditionalCost.BlightVariable`) X-amount selection state.
 * Only handles the numeric X chooser; the creature target (when X > 0) is
 * picked inline on the battlefield via the standard targeting overlay.
 */
export interface BlightVariableSelectionState {
  actionInfo: LegalActionInfo
  cardName: string
  maxX: number
  selectedX: number
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
  /** When true, this is a repeat count selector (not X cost) */
  isRepeatCount?: boolean
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
  allowPartial?: boolean
  distribution: Record<EntityId, number>
}

/**
 * Counter distribution state for counter-removal costs.
 *
 * Two flavours:
 * - X cost (e.g., `Remove X +1/+1 counters`): `requiredTotal` is null, any positive total confirms.
 * - Fixed cost (e.g., Dawnhand Dissident's "remove three counters from among creatures you control"):
 *   `requiredTotal` is set; confirm only enables at exactly that total.
 */
export interface CounterDistributionState {
  actionInfo: LegalActionInfo
  cardName: string
  xValue: number
  creatures: readonly CounterRemovalCreatureInfo[]
  /**
   * Per-creature, per-type allocation. Outer key is entity id; inner key is the
   * counter-type symbol (e.g. "+1/+1", "stun"). For creatures that carry only one
   * type, the inner record has a single entry.
   */
  distribution: Record<EntityId, Record<string, number>>
  /** When set, total allocated counters must equal this to confirm (fixed-cost mode). */
  requiredTotal?: number
  /** Label shown to the player (e.g., "Remove +1/+1 counters" vs. "Remove counters"). */
  description?: string
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
  /**
   * Restricted set of producible colors for this ability ("WHITE", ...). Undefined = all five.
   */
  availableColors?: readonly string[]
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
 * Waterbend selection state (Avatar: The Last Airbender). Like Convoke but generic-only —
 * each tapped artifact/creature pays {1} of the generic cost, with no color choice.
 */
export interface WaterbendSelectionState {
  actionInfo: LegalActionInfo
  cardName: string
  /** The cost being paid (the waterbend cost's mana string) */
  manaCost: string
  /** Entity ids of permanents selected to tap for waterbend */
  selectedPermanents: EntityId[]
  /** All valid artifacts/creatures that can be tapped */
  validPermanents: readonly WaterbendPermanentInfo[]
}

/**
 * Harmonize creature-tap selection state when casting a spell from the graveyard via
 * Harmonize. The player may tap at most one creature to reduce the generic portion of the
 * harmonize cost by its power (and {X} is generic, so it reduces the X mana paid). Tapping
 * is optional — the player can confirm with no creature selected to pay the full cost.
 */
export interface HarmonizeSelectionState {
  actionInfo: LegalActionInfo
  cardName: string
  /** Harmonize cost with {X} already expanded to the chosen X (e.g. "{3}{G}{G}{G}{G}"). */
  manaCost: string
  /** The single creature chosen to tap, or null when none is selected yet. */
  selectedCreature: EntityId | null
  /** All untapped creatures that can be tapped, with the power each would reduce by. */
  validCreatures: readonly HarmonizeCreatureInfo[]
}

/**
 * "Tap creatures with total power N" selection state — shared by Crew N (Vehicles) and
 * Saddle N (Mounts). The action type on [actionInfo] determines which is being paid.
 */
export interface TapForPowerSelectionState {
  actionInfo: LegalActionInfo
  /** The source permanent's name (Vehicle or Mount). */
  sourceName: string
  /** The verb shown to the player: "Crew" or "Saddle". */
  verb: string
  /** Total power requirement (N in "Crew N" / "Saddle N"). */
  requiredPower: number
  /** Creatures selected to tap. */
  selectedCreatures: EntityId[]
  /** All valid creatures that can be tapped. */
  validCreatures: readonly TapForPowerCreatureInfo[]
}

/**
 * Delve selection state when casting spells with Delve.
 */
export interface DelveSelectionState {
  actionInfo: LegalActionInfo
  cardName: string
  /** Original mana cost of the spell */
  manaCost: string
  /** Cards that have been selected for Delve (to exile from graveyard) */
  selectedCards: readonly EntityId[]
  /** All valid cards in graveyard that can be exiled for Delve */
  validCards: readonly DelveCardInfo[]
  /** Maximum number of generic mana that can be paid via Delve */
  maxDelve: number
  /** Minimum number of cards to exile to afford the spell (from server) */
  minDelveNeeded: number
}

/**
 * Deck building state during sealed draft.
 */
/** A single card's AI pick evaluation (keyed by card name in the store's `pickScores`). */
export interface PickScore {
  readonly score: number
  readonly reason: string
}

/** One candidate deck from an Auto-build, shown as a choice in the deck builder. */
export interface AutoBuildOption {
  /** Deck score (Draftsim: 0–10; heuristic: sum of card ratings), or null if unscored. */
  readonly score: number | null
  /** Targeted archetype / colour label, if the engine identifies one. */
  readonly archetype: string | null
  /** Build colors as WUBRG single-letter codes, for the option's color pips. */
  readonly colors: readonly string[]
  /** Full decklist (spells + basics) for this option, re-applied when the player switches to it. */
  readonly deckList: Readonly<Record<string, number>>
}

/** The most recent Auto-build: its candidate decks plus which one is currently applied. */
export interface AutoBuildResult {
  /** Engine that produced the builds (e.g. "draftsim", "heuristic"). */
  readonly advisorId: string
  /** Candidate decks, ordered best-first. */
  readonly options: readonly AutoBuildOption[]
  /** Index of the option currently applied to the deck. */
  readonly appliedIndex: number
}

export interface DeckBuildingState {
  phase: 'waiting' | 'building' | 'submitted'
  setCodes: readonly string[]
  setNames: readonly string[]
  cardPool: readonly SealedCardInfo[]
  basicLands: readonly SealedCardInfo[]
  /** Card names currently in the deck */
  deck: readonly string[]
  /** Basic land counts by land name */
  landCounts: Record<string, number>
  opponentReady: boolean
  /**
   * Card names highlighted by the LLM deckbuilding assistant. When non-null,
   * these replace any archetype-driven highlights in the pool view.
   */
  llmHighlightedCards: readonly string[] | null
  /**
   * Selected commander name for Commander Draft / Sealed lobbies. Null when no commander
   * is chosen yet, or when the lobby format isn't commander-shape. The card MUST exist in
   * [cardPool] (the deckbuilder UI gates the picker to eligible pool cards).
   */
  commander: string | null
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
  queuedPacks: number
  playerPackCounts: Readonly<Record<string, number>>
  /**
   * Total picks a pack of this round's size yields, derived from the latest received
   * pack (booster sizes vary: 15-card classic, 13-card Play Booster, 20-card commander).
   * Null until a non-empty pack has been seen.
   */
  picksPerPack: number | null
}

/**
 * Winston Draft state during the Winston drafting phase.
 */
export interface WinstonDraftState {
  isYourTurn: boolean
  activePlayerName: string
  currentPileIndex: number
  pileSizes: readonly number[]
  mainDeckRemaining: number
  currentPileCards: readonly SealedCardInfo[] | null
  pickedCards: readonly SealedCardInfo[]
  totalPickedByOpponent: number
  knownOpponentCards: readonly SealedCardInfo[]
  unknownOpponentCardCount: number
  lastAction: string | null
  timeRemaining: number
  lastPickedCards: readonly SealedCardInfo[]
}

/**
 * Grid Draft state during the grid drafting phase.
 */
export interface GridDraftState {
  isYourTurn: boolean
  activePlayerName: string
  grid: readonly (SealedCardInfo | null)[]
  mainDeckRemaining: number
  pickedCards: readonly SealedCardInfo[]
  totalPickedByOthers: Record<string, number>
  pickedCardsByOthers: Record<string, readonly SealedCardInfo[]>
  lastPickedCards: readonly SealedCardInfo[]
  lastAction: string | null
  timeRemaining: number
  availableSelections: readonly string[]
  playerOrder: readonly string[]
  currentPickerIndex: number
  gridNumber: number
}

/**
 * Lobby state for tournament lobbies (sealed, draft, Winston draft, or grid draft).
 */
export interface LobbyState {
  lobbyId: string
  state: 'WAITING_FOR_PLAYERS' | 'DRAFTING' | 'DECK_BUILDING' | 'TOURNAMENT_ACTIVE' | 'TOURNAMENT_COMPLETE'
  players: readonly LobbyPlayerInfo[]
  settings: LobbySettings
  isHost: boolean
  /** Draft-specific state (only populated when format is DRAFT and state is DRAFTING) */
  draftState: DraftState | null
  /** Winston Draft-specific state (only populated when format is WINSTON_DRAFT and state is DRAFTING) */
  winstonDraftState: WinstonDraftState | null
  /** Grid Draft-specific state (only populated when format is GRID_DRAFT and state is DRAFTING) */
  gridDraftState: GridDraftState | null
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
 * Free-for-All pod state (the FFA-mode counterpart of [TournamentState]). Created when the
 * first FFA game starts and lives for the lifetime of the pod's play-again loop.
 */
export interface FfaState {
  lobbyId: string
  /** Session id of the running game, or null between games. */
  currentGameSessionId: string | null
  /** Number of the running/next game in the play-again loop (1-based). */
  gameNumber: number
  /** Final standings of the last completed game (placement order), or null before game 1 ends. */
  standings: readonly FfaStandingInfo[] | null
  gamesPlayed: number
  /** Players who are ready for the next game ("play again"). */
  readyPlayerIds: readonly string[]
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
 * Match intro animation state.
 */
export interface MatchIntro {
  playerName: string
  opponentName: string
  round?: number
  playerRecord?: string
  opponentRecord?: string
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
 * A target reselection animation (Grip of Chaos, etc.).
 */
export interface TargetReselectedAnimation {
  id: string
  spellOrAbilityName: string
  oldTargetName: string
  newTargetName: string
  sourceName: string
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
// Action Pipeline Types
// ============================================================================

/**
 * A phase in the action pipeline. Computed up front by computePhases().
 */
export type PipelinePhase =
  | { type: 'counterDistribution' }
  | { type: 'xSelection' }
  | { type: 'delve' }
  | { type: 'convoke' }
  | { type: 'waterbend' }
  | { type: 'harmonize' }
  | { type: 'manaSource' }
  | { type: 'costPayment' }
  | { type: 'blightVariable' }
  | { type: 'manaColorChoice' }
  | { type: 'targeting' }
  | { type: 'damageDistribution' }

/**
 * Result reported by a phase's confirm handler.
 */
export type PhaseResult =
  | {
      type: 'counterDistribution'
      xValue: number
      /** Typed distribution: each entry removes `count` counters of `counterType` from `entityId`. */
      distributedCounterRemovals: ReadonlyArray<{ entityId: EntityId; counterType: string; count: number }>
    }
  | { type: 'xSelection'; xValue: number; isRepeatCount?: boolean }
  | { type: 'delve'; delvedCards: EntityId[]; modifiedManaCost: string }
  | { type: 'convoke'; convokedCreatures: Record<string, { color: string | null }> }
  | { type: 'waterbend'; waterbendPermanents: EntityId[] }
  | { type: 'harmonize'; harmonizeCreature: EntityId | null; reduction: number }
  | { type: 'manaSource'; selectedSources: EntityId[] }
  | { type: 'costPayment'; costType: string; selectedTargets: EntityId[] }
  | { type: 'blightVariable'; blightAmount: number }
  | { type: 'manaColorChoice'; color: string }
  | { type: 'targeting'; selectedTargets: EntityId[] }
  | { type: 'damageDistribution'; distribution: Record<EntityId, number> }

/**
 * Pipeline coordinator state: tracks the action being built and remaining phases.
 */
export interface ActionPipelineState {
  actionInfo: import('../../types').LegalActionInfo
  accumulatedAction: import('../../types').GameAction
  remainingPhases: readonly PipelinePhase[]
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
  pendingSpectateGameId: string | null
  aiEnabled: boolean
  availableSets: readonly AvailableSet[]
  onlinePlayers: number | null
  /** True when another tab/device took over this identity; auto-reconnect is stopped. */
  sessionReplaced: boolean
  connect: (playerName: string, options?: { spectator?: boolean }) => void
  disconnect: () => void
  setPendingTournamentId: (lobbyId: string | null) => void
  setPendingSpectateGameId: (gameSessionId: string | null) => void

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
  autoTapEnabled: boolean
  /** Number of spectators currently watching this player's game (0 if none). */
  spectatorCount: number
  /** Names of currently-active spectators (for hover display on the badge). */
  spectatorNames: readonly string[]
  createGame: (deckList: Record<string, number>, setCode?: string) => void
  createAiGame: (deckList: Record<string, number>, setCode?: string) => void
  joinGame: (sessionId: string, deckList: Record<string, number>) => void
  submitAction: (action: GameAction) => void
  submitDecision: (selectedCards: readonly EntityId[]) => void
  submitTargetsDecision: (selectedTargets: Record<number, readonly EntityId[]>) => void
  submitOrderedDecision: (orderedObjects: readonly EntityId[]) => void
  submitYesNoDecision: (choice: boolean) => void
  submitBatchYesNoDecision: (choice: boolean, applyToAll: boolean) => void
  submitNumberDecision: (number: number) => void
  submitOptionDecision: (optionIndex: number) => void
  submitReplacementDecision: (fromIndex: number, toIndex: number) => void
  submitBudgetModalDecision: (selectedModeIndices: readonly number[]) => void
  submitDistributeDecision: (distribution: Record<EntityId, number>) => void
  submitDamageAssignmentDecision: (assignments: Record<EntityId, number>) => void
  submitCombatResolutionDecision: (edges: ReadonlyArray<{ edgeId: string; amount: number }>) => void
  submitColorDecision: (color: string) => void
  submitManaSourcesDecision: (selectedSources: readonly EntityId[], autoPay: boolean) => void
  submitCancelDecision: () => void
  submitSplitPilesDecision: (piles: readonly (readonly EntityId[])[]) => void
  keepHand: () => void
  mulligan: () => void
  chooseBottomCards: (cardIds: readonly EntityId[]) => void
  toggleMulliganCard: (cardId: EntityId) => void
  concede: () => void
  requestUndo: () => void
  toggleAutoTap: () => void
  cancelGame: () => void
  setFullControl: (enabled: boolean) => void
  cyclePriorityMode: () => void
  toggleStopOverride: (step: Step, isMyTurn: boolean) => void
  setAbilityYield: (cardDefinitionId: string, abilityId: string, kind: YieldKind) => void
  clearAbilityYield: (cardDefinitionId: string, abilityId: string) => void
  clearAllYields: () => void
  returnToMenu: () => void
  setError: (error: ErrorState) => void
  clearError: () => void
  consumeEvent: () => ClientEvent | undefined

  // Lobby slice
  lobbyState: LobbyState | null
  tournamentState: TournamentState | null
  ffaState: FfaState | null
  spectatingState: SpectatingState | null
  createTournamentLobby: (setCodes: string[], format?: TournamentFormat, boosterCount?: number, maxPlayers?: number, pickTimeSeconds?: number, isPublic?: boolean, gameMode?: LobbyGameMode) => void
  joinLobby: (lobbyId: string) => void
  startLobby: () => void
  leaveLobby: () => void
  addAiToLobby: () => void
  removeAiFromLobby: (playerId: string) => void
  stopLobby: () => void
  updateLobbySettings: (settings: { setCodes?: string[]; format?: TournamentFormat; boosterCount?: number; boosterDistribution?: Record<string, number>; maxPlayers?: number; gamesPerMatch?: number; pickTimeSeconds?: number; picksPerRound?: number; isPublic?: boolean; deckFormat?: DeckFormat | '' | null; deckSizeMin?: number; allowDuplicates?: boolean; commanderPreset?: CommanderPreset; chaosBoosters?: boolean; bannedCardNames?: string[]; aiAssistEnabled?: boolean; gameMode?: LobbyGameMode; attackMode?: AttackMode; randomTeams?: boolean; teamAssignments?: Record<string, number> }) => void
  /** Disconnected tournament players: playerId -> info */
  disconnectedPlayers: Record<string, { playerName: string; secondsRemaining: number; disconnectedAt: number }>
  readyForNextRound: () => void
  addExtraRound: () => void
  spectateGame: (gameSessionId: string) => void
  stopSpectating: () => void
  setSpectatingState: (state: SpectatingState | null) => void
  addDisconnectTime: (playerId: string) => void
  kickPlayer: (playerId: string) => void
  leaveTournament: () => void
  /** Submit a deck directly from the lobby (Premade Decks tournament format). */
  submitLobbyDeck: (deckList: Record<string, number>, commander?: string | null) => void
  unsubmitLobbyDeck: () => void

  // Quick Game Lobby slice
  quickGameLobbyState: QuickGameLobbyStateMessage | null
  createQuickGameLobby: (vsAi?: boolean, setCode?: string, isPublic?: boolean, format?: DeckFormat, momirBasic?: boolean) => void
  joinQuickGameLobby: (lobbyId: string) => void
  leaveQuickGameLobby: () => void
  submitQuickGameLobbyDeck: (deckList: Record<string, number>, commander?: string | null) => void
  setQuickGameLobbyReady: (ready: boolean) => void
  setQuickGameLobbySetCode: (setCode: string | null) => void
  setQuickGameLobbyPublic: (isPublic: boolean) => void
  setQuickGameLobbyFormat: (format: DeckFormat | null, momirBasic?: boolean) => void

  // Draft slice
  deckBuildingState: DeckBuildingState | null
  createSealedGame: (setCode: string) => void
  joinSealedGame: (sessionId: string) => void
  addCardToDeck: (cardName: string) => void
  removeCardFromDeck: (cardName: string) => void
  clearDeck: () => void
  setLandCount: (landType: string, count: number) => void
  /** Replace the entire deck (non-basic cards as a flat list) and basic land counts. */
  setDeck: (deck: readonly string[], landCounts: Record<string, number>) => void
  /** Set or clear the commander for Commander Draft / Sealed lobbies. */
  setCommander: (cardName: string | null) => void
  /** Set the LLM-driven highlight set. Pass null to clear. */
  setLlmHighlights: (cardNames: readonly string[] | null) => void
  submitSealedDeck: () => void
  unsubmitDeck: () => void
  makePick: (cardNames: string[]) => void
  winstonTakePile: () => void
  winstonSkipPile: () => void
  gridDraftPick: (selection: string) => void
  // AI assistance (Suggest Pick / Auto-build)
  pickScores: Readonly<Record<string, PickScore>> | null
  recommendedPick: readonly string[]
  aiAssistBusy: boolean
  aiAssistError: string | null
  /** Candidate decks + applied index from the most recent Auto-build, or null. */
  autoBuildResult: AutoBuildResult | null
  /** Selected engine for the draft / deckbuild dropdowns; persists across edits and remounts. */
  draftAdvisorId: string | null
  deckbuildAdvisorId: string | null
  setDraftAdvisorId: (advisorId: string | null) => void
  setDeckbuildAdvisorId: (advisorId: string | null) => void
  /** Per-card scores for the deck-builder pool (name → score/reason), or null. */
  deckCardScores: Readonly<Record<string, PickScore>> | null
  scoreDeckCards: (advisorId?: string) => Promise<void>
  clearDeckCardScores: () => void
  suggestPick: (advisorId?: string) => Promise<void>
  clearPickSuggestion: () => void
  autoBuildDeck: (advisorId?: string) => Promise<void>
  /** Switch the deck to a different Auto-build candidate (by index into `autoBuildResult.options`). */
  applyAutoBuildOption: (index: number) => void

  // Pipeline slice
  pipelineState: ActionPipelineState | null
  startPipeline: (
    actionInfo: import('../../types').LegalActionInfo,
    options?: { forceManualTap?: boolean },
  ) => void
  advancePipeline: (result: PhaseResult) => void
  cancelPipeline: () => void

  // Board view slice (multiplayer viewed-opponent board + follow-the-action camera)
  viewedOpponentId: EntityId | null
  viewPinned: boolean
  followAction: boolean
  spectatorBottomSeatId: EntityId | null
  teamByPlayerId: Readonly<Record<EntityId, number>>
  teamSharedLife: boolean
  viewOpponent: (playerId: EntityId, opts?: { pin?: boolean }) => void
  unpinView: () => void
  toggleFollowAction: () => void
  followViewTo: (playerId: EntityId) => void
  setSpectatorBottomSeat: (playerId: EntityId | null) => void
  setSeatTeams: (teamByPlayerId: Record<EntityId, number>, sharedLife?: boolean) => void
  resetBoardView: () => void

  // UI slice
  selectedCardId: EntityId | null
  targetingState: TargetingState | null
  combatState: CombatState | null
  xSelectionState: XSelectionState | null
  convokeSelectionState: ConvokeSelectionState | null
  waterbendSelectionState: WaterbendSelectionState | null
  tapForPowerSelectionState: TapForPowerSelectionState | null
  delveSelectionState: DelveSelectionState | null
  manaColorSelectionState: ManaColorSelectionState | null
  decisionSelectionState: DecisionSelectionState | null
  damageDistributionState: DamageDistributionState | null
  lastDamageDistribution: Record<EntityId, number> | null
  distributeState: DistributeState | null
  counterDistributionState: CounterDistributionState | null
  manaSelectionState: ManaSelectionState | null
  hoveredCardId: EntityId | null
  hoverPosition: { x: number; y: number } | null
  autoTapPreview: readonly EntityId[] | null
  draggingBlockerId: EntityId | null
  draggingAttackerId: EntityId | null
  draggingAttackerHasBanding: boolean | null
  draggingCardId: EntityId | null
  revealedHandCardIds: readonly EntityId[] | null
  revealedCardsInfo: {
    cardIds: readonly EntityId[]
    cardNames: readonly string[]
    imageUris: readonly (string | null)[]
    source: string | null
    isYourReveal: boolean
    /**
     * Per-card revealer attribution (parallel to cardIds), present when one effect reveals cards
     * from more than one player at once (e.g. Psychic Battle). `true` = the viewing player,
     * `false` = an opponent. Absent for single-player reveals (use [isYourReveal]).
     */
    cardOwnerIsYours?: readonly boolean[]
    /** Zone the card came from (e.g., 'Graveyard', 'Exile') when this reveal is a zone transition. */
    fromZone?: string | null
    /** Zone the card moved to (e.g., 'Hand', 'Library') when this reveal is a zone transition. */
    toZone?: string | null
  } | null
  opponentAttackerTargets: { selectedAttackers: readonly EntityId[]; attackerTargets: Record<EntityId, EntityId> } | null
  opponentBlockerAssignments: Record<EntityId, EntityId[]> | null
  drawAnimations: readonly DrawAnimation[]
  damageAnimations: readonly DamageAnimation[]
  revealAnimations: readonly RevealAnimation[]
  coinFlipAnimations: readonly CoinFlipAnimation[]
  targetReselectedAnimations: readonly TargetReselectedAnimation[]
  /**
   * Battlefield card ids currently pulsing after being beheld, along with the
   * name of the spell/ability that beheld them. A pulse persists until the
   * stack no longer contains an item with that source name (i.e. the beholding
   * spell has resolved, been countered, or otherwise left the stack). Each
   * pulse also has a minimum floor duration so it stays visible briefly even
   * when the beholding spell auto-resolves without any response.
   */
  beholdPulses: readonly { cardId: EntityId; sourceName: string; floorUntil: number }[]
  selectCard: (cardId: EntityId | null) => void
  hoverCard: (cardId: EntityId | null, position?: { x: number; y: number }) => void
  updateHoverPosition: (position: { x: number; y: number }) => void
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
  startDraggingAttacker: (attackerId: EntityId, hasBanding?: boolean) => void
  stopDraggingAttacker: () => void
  setAttackTarget: (attackerId: EntityId, targetId: EntityId) => void
  assignDefenderToSelectedAttackers: (defenderId: EntityId) => void
  startDraggingCard: (cardId: EntityId) => void
  stopDraggingCard: () => void
  confirmCombat: () => void
  cancelCombat: () => void
  attackWithAll: () => void
  clearAttackers: () => void
  clearCombat: () => void
  removeBand: (bandIndex: number) => void
  clearBands: () => void
  linkBand: (sourceId: EntityId, targetId: EntityId, sourceHasBanding: boolean, targetHasBanding: boolean) => void
  startXSelection: (state: XSelectionState) => void
  updateXValue: (x: number) => void
  cancelXSelection: () => void
  confirmXSelection: () => void
  blightVariableSelectionState: BlightVariableSelectionState | null
  startBlightVariableSelection: (state: BlightVariableSelectionState) => void
  updateBlightVariableX: (x: number) => void
  cancelBlightVariableSelection: () => void
  confirmBlightVariableSelection: () => void
  startConvokeSelection: (state: ConvokeSelectionState) => void
  toggleConvokeCreature: (entityId: EntityId, name: string, payingColor: string | null) => void
  cancelConvokeSelection: () => void
  confirmConvokeSelection: () => void
  startWaterbendSelection: (state: WaterbendSelectionState) => void
  toggleWaterbendPermanent: (entityId: EntityId) => void
  cancelWaterbendSelection: () => void
  confirmWaterbendSelection: () => void
  harmonizeSelectionState: HarmonizeSelectionState | null
  startHarmonizeSelection: (state: HarmonizeSelectionState) => void
  toggleHarmonizeCreature: (entityId: EntityId) => void
  cancelHarmonizeSelection: () => void
  confirmHarmonizeSelection: () => void
  startTapForPowerSelection: (state: TapForPowerSelectionState) => void
  toggleTapForPowerCreature: (entityId: EntityId) => void
  cancelTapForPowerSelection: () => void
  confirmTapForPowerSelection: () => void
  startDelveSelection: (state: DelveSelectionState) => void
  toggleDelveCard: (entityId: EntityId) => void
  cancelDelveSelection: () => void
  confirmDelveSelection: () => void
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
  startCounterDistribution: (state: CounterDistributionState) => void
  incrementCounterRemoval: (entityId: EntityId, counterType: string) => void
  decrementCounterRemoval: (entityId: EntityId, counterType: string) => void
  cancelCounterDistribution: () => void
  confirmCounterDistribution: () => void
  startManaSelection: (actionInfo: LegalActionInfo) => void
  toggleManaSource: (entityId: EntityId) => void
  cancelManaSelection: () => void
  confirmManaSelection: () => void
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
  addTargetReselectedAnimation: (animation: TargetReselectedAnimation) => void
  removeTargetReselectedAnimation: (id: string) => void
  addBeholdPulse: (cardId: EntityId, sourceName: string) => void
  reconcileBeholdPulses: (stackItemNames: readonly string[]) => void
  setAutoTapPreview: (preview: readonly EntityId[] | null) => void
  matchIntro: MatchIntro | null
  setMatchIntro: (intro: MatchIntro) => void
  clearMatchIntro: () => void
}
