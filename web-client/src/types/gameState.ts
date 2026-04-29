import { AbilityFlag, Color, CounterType, Keyword, Phase, Step, ZoneType } from './enums'
import { EntityId, ZoneId } from './entities'
import { ClientEvent } from './events'

/**
 * Client-facing game state DTO.
 * Matches backend ClientGameState.kt
 */
export interface ClientGameState {
  /** The player viewing this state */
  readonly viewingPlayerId: EntityId

  /** All visible cards/permanents */
  readonly cards: Record<EntityId, ClientCard>

  /** Zone information */
  readonly zones: readonly ClientZone[]

  /** Player information */
  readonly players: readonly ClientPlayer[]

  /** Current phase and step */
  readonly currentPhase: Phase
  readonly currentStep: Step

  /** Whose turn it is */
  readonly activePlayerId: EntityId

  /** Who currently has priority */
  readonly priorityPlayerId: EntityId

  /** Turn number */
  readonly turnNumber: number

  /** Whether the game is over */
  readonly isGameOver: boolean

  /** The winner, if the game is over */
  readonly winnerId: EntityId | null

  /** Combat state, if in combat */
  readonly combat: ClientCombatState | null

  /** Accumulated game log entries from the server */
  readonly gameLog?: readonly ClientEvent[]
}

/**
 * Card/permanent information for client display.
 * Matches backend ClientCard.kt
 */
export interface ClientCard {
  /** Unique identifier */
  readonly id: EntityId

  /** Card name for display */
  readonly name: string

  /** Mana cost as a string (e.g., "{2}{G}{G}") */
  readonly manaCost: string

  /** Converted mana cost / mana value */
  readonly manaValue: number

  /** Type line as displayed on the card (e.g., "Creature - Human Warrior") */
  readonly typeLine: string

  /** Card types for filtering (creature, land, instant, etc.) */
  readonly cardTypes: readonly string[]

  /** Subtypes for display and filtering (e.g., "Human", "Warrior", "Forest") */
  readonly subtypes: readonly string[]

  /** Card colors */
  readonly colors: readonly Color[]

  /** Oracle text / rules text (for display in card details) */
  readonly oracleText: string

  /** Power for creatures (null if not a creature) - projected/modified value */
  readonly power: number | null

  /** Toughness for creatures (null if not a creature) - projected/modified value */
  readonly toughness: number | null

  /** Base power before modifications (for buff indicators) */
  readonly basePower: number | null

  /** Base toughness before modifications (for buff indicators) */
  readonly baseToughness: number | null

  /** Current damage on creature (only present on battlefield) */
  readonly damage: number | null

  /** Keywords the card has (flying, haste, etc.) */
  readonly keywords: readonly Keyword[]

  /** Ability flags (non-keyword static abilities like "can't be blocked") */
  readonly abilityFlags?: readonly AbilityFlag[]

  /** Protection colors (for colored protection shield icons) */
  readonly protections?: readonly Color[]

  /** Hexproof-from-color colors (for colored hexproof shield icons) */
  readonly hexproofFromColors?: readonly Color[]

  /** Counters on the card */
  readonly counters: Partial<Record<CounterType, number>>

  /** State flags */
  readonly isTapped: boolean
  readonly hasSummoningSickness: boolean
  readonly isTransformed: boolean

  /** True when this card is a double-faced card (DFC). */
  readonly isDoubleFaced?: boolean
  /** For DFCs currently on the battlefield: 'FRONT' or 'BACK'. Null otherwise. */
  readonly currentFace?: 'FRONT' | 'BACK' | null
  /** Back face display name for DFCs. */
  readonly backFaceName?: string | null
  /** Back face type line for DFCs. */
  readonly backFaceTypeLine?: string | null
  /** Back face oracle text for DFCs. */
  readonly backFaceOracleText?: string | null
  /** Back face image URI for DFCs. */
  readonly backFaceImageUri?: string | null

  /** Combat state (if in combat) */
  readonly isAttacking: boolean
  readonly isBlocking: boolean
  readonly attackingTarget: EntityId | null
  readonly blockingTarget: EntityId | null

  /** Controller (who controls it now) */
  readonly controllerId: EntityId

  /** Owner (who started with it in their deck) */
  readonly ownerId: EntityId

  /** Whether this is a token */
  readonly isToken: boolean

  /** Zone this card is currently in */
  readonly zone: ZoneId | null

  /** Attached to (for auras, equipment) */
  readonly attachedTo: EntityId | null

  /** What's attached to this card (auras, equipment on this permanent) */
  readonly attachments: readonly EntityId[]

  /** Cards exiled by this permanent via linked exile (e.g., Suspension Field) */
  readonly linkedExile?: readonly EntityId[]

  /** Whether this card is face-down (for morph, manifest, hidden info) */
  readonly isFaceDown: boolean

  /** Morph cost for face-down creatures (only visible to controller) */
  readonly morphCost?: string | null

  /** Targets for spells/abilities on the stack (for targeting arrows) */
  readonly targets: readonly ClientChosenTarget[]

  /** Image URI from card metadata (for rendering card images) */
  readonly imageUri?: string | null

  /** Active effects on this card (e.g., "can't be blocked except by black creatures") */
  readonly activeEffects?: readonly ClientCardEffect[]

  /** Official rulings for this card (for card details view) */
  readonly rulings?: readonly ClientRuling[]

  /** Whether this spell was kicked (only present on stack) */
  readonly wasKicked?: boolean

  /** Whether this spell promised a gift (Bloomburrow gift mechanic — only present on stack) */
  readonly giftPromised?: boolean

  /** Whether this spell's optional Blight additional cost was paid (Lorwyn Eclipsed — only present on stack) */
  readonly wasBlightPaid?: boolean

  /** Chosen X value for spells with X in their cost (only present on stack) */
  readonly chosenX?: number | null

  /** Copy index for storm/copy effects on the stack (1, 2, 3...) */
  readonly copyIndex?: number | null

  /** Total number of copies for storm/copy effects on the stack */
  readonly copyTotal?: number | null

  /** Chosen creature type for "as enters, choose a creature type" permanents (e.g., Doom Cannon) */
  readonly chosenCreatureType?: string | null

  /** Chosen color for "as enters, choose a color" permanents (e.g., Riptide Replicator) */
  readonly chosenColor?: string | null

  /** Triggering entity ID for triggered abilities on the stack (for source arrows) */
  readonly triggeringEntityId?: EntityId | null

  /** Source zone for triggered abilities on the stack (e.g., "GRAVEYARD" for graveyard triggers) */
  readonly sourceZone?: string | null

  /** Creature types of the sacrificed permanent (for spells like Endemic Plague on the stack) */
  readonly sacrificedCreatureTypes?: readonly string[] | null

  /** Specific ability text when on the stack (e.g., spell effect description, not full oracle text) */
  readonly stackText?: string | null

  /**
   * Runtime descriptions of each chosen mode, in the order they were picked (rule 700.2).
   * Empty for non-modal spells and for modal spells whose mode hasn't been selected yet.
   * For opponent visibility of choose-N commands (Brigid's Command, Sygg's Command, etc.).
   */
  readonly chosenModeDescriptions?: readonly string[]

  /**
   * Per-mode target groups for modal spells on the stack, aligned 1:1 with `chosenModeDescriptions`.
   * Each group carries the mode description, chosen targets (for arrows), and human-readable target names.
   */
  readonly perModeTargets?: readonly ClientPerModeTargetGroup[]

  /** Revealed name for face-down creatures that this player has peeked at (e.g., via Spy Network) */
  readonly revealedName?: string | null

  /** Revealed image URI for face-down creatures that this player has peeked at */
  readonly revealedImageUri?: string | null

  /** Whether this card can be played from exile (e.g., Mind's Desire impulse draw) */
  readonly playableFromExile?: boolean

  /** Original card name when this permanent is a copy (e.g., "Clever Impersonator") */
  readonly copyOf?: string | null

  /** Damage distribution for DividedDamageEffect spells on the stack (target entity ID -> damage amount) */
  readonly damageDistribution?: Record<EntityId, number> | null

  /** For Sagas: the total number of chapters (e.g., 3). Null for non-Sagas. */
  readonly sagaTotalChapters?: number | null

  /** For Class enchantments: the current class level (1, 2, or 3). Null for non-Classes. */
  readonly classLevel?: number | null

  /** For Class enchantments: the maximum class level (e.g., 3). Null for non-Classes. */
  readonly classMaxLevel?: number | null

  /**
   * Threshold-style progress: present on cards whose static ability turns on at a
   * graveyard-size milestone (e.g. classic Threshold = 7+). Lets the UI render a badge
   * showing current/required graveyard count for the card's controller.
   */
  readonly thresholdInfo?: {
    readonly current: number
    readonly required: number
    readonly active: boolean
  } | null

  /**
   * For planeswalkers on the battlefield: the complete set of loyalty abilities,
   * in declaration order. The UI renders the full list and grays out any ability
   * whose `abilityId` isn't present in the legal actions for this card.
   */
  readonly planeswalkerAbilities?: readonly ClientPlaneswalkerAbility[] | null
}

/** One loyalty ability on a planeswalker (always-visible menu rendering). */
export interface ClientPlaneswalkerAbility {
  /** Matches `ActivateAbility.abilityId` in legal actions. */
  readonly abilityId: string
  /** Signed loyalty change (+1, -2, -8, etc.). */
  readonly loyaltyChange: number
  /** Ability text. */
  readonly description: string
}

/**
 * Zone information for client display.
 * Matches backend ClientZone.kt
 */
export interface ClientZone {
  readonly zoneId: ZoneId

  /** Card IDs in this zone, in order (may be empty for hidden zones) */
  readonly cardIds: readonly EntityId[]

  /** Number of cards in the zone (always available, even for hidden zones) */
  readonly size: number

  /** Whether the contents are visible to the viewing player */
  readonly isVisible: boolean
}

/**
 * Player information for client display.
 * Matches backend ClientPlayer.kt
 */
export interface ClientPlayer {
  readonly playerId: EntityId
  readonly name: string
  readonly life: number
  readonly poisonCounters: number
  readonly handSize: number
  readonly librarySize: number
  readonly graveyardSize: number
  readonly exileSize: number
  readonly landsPlayedThisTurn: number
  readonly hasLost: boolean
  readonly manaPool?: ClientManaPool
  readonly activeEffects?: readonly ClientPlayerEffect[]
}

/**
 * An active effect on a player that should be displayed as a badge.
 * Matches backend ClientPlayerEffect.kt
 */
export interface ClientPlayerEffect {
  /** Unique identifier for the effect type */
  readonly effectId: string
  /** Human-readable name for display */
  readonly name: string
  /** Optional description/tooltip text */
  readonly description?: string
  /** Optional icon identifier for UI rendering */
  readonly icon?: string
}

/**
 * An active effect on a card that should be displayed as a badge.
 * Matches backend ClientCardEffect.kt
 */
export interface ClientCardEffect {
  /** Unique identifier for the effect type */
  readonly effectId: string
  /** Human-readable name for display */
  readonly name: string
  /** Optional description/tooltip text */
  readonly description?: string
  /** Optional icon identifier for UI rendering */
  readonly icon?: string
}

/**
 * An official ruling for a card.
 * Displayed in card details view to clarify complex interactions.
 * Matches backend ClientRuling.kt
 */
export interface ClientRuling {
  /** Date of the ruling (e.g., "6/8/2016") */
  readonly date: string
  /** The ruling text */
  readonly text: string
}

/**
 * Mana pool state for client display.
 * Matches backend ClientManaPool.kt
 */
export interface ClientManaPool {
  readonly white: number
  readonly blue: number
  readonly black: number
  readonly red: number
  readonly green: number
  readonly colorless: number
  readonly restrictedMana: ReadonlyArray<ClientRestrictedManaEntry>
}

/**
 * A single unit of restricted mana for client display.
 * Matches backend ClientRestrictedManaEntry.kt.
 */
export interface ClientRestrictedManaEntry {
  /** Mana color symbol ("W"/"U"/"B"/"R"/"G") or null for colorless. */
  readonly color: string | null
  /** Human-readable restriction (e.g., "Spend this mana only to cast spells with mana value 4 or greater"). */
  readonly restrictionDescription: string
}

/**
 * Calculate total mana in pool.
 */
export function totalMana(pool: ClientManaPool): number {
  return pool.white + pool.blue + pool.black + pool.red + pool.green + pool.colorless +
    (pool.restrictedMana?.length ?? 0)
}

/**
 * Check if mana pool is empty.
 */
export function isManaPoolEmpty(pool: ClientManaPool): boolean {
  return totalMana(pool) === 0
}

/**
 * Combat state for client display.
 * Matches backend ClientCombatState.kt
 */
export interface ClientCombatState {
  /** Who is attacking */
  readonly attackingPlayerId: EntityId

  /** Who is defending */
  readonly defendingPlayerId: EntityId

  /** All declared attackers with their targets */
  readonly attackers: readonly ClientAttacker[]

  /** All declared blockers with what they're blocking */
  readonly blockers: readonly ClientBlocker[]
}

/**
 * Attacker information for combat display.
 * Matches backend ClientAttacker.kt
 */
export interface ClientAttacker {
  readonly creatureId: EntityId
  readonly creatureName: string
  readonly attackingTarget: ClientCombatTarget
  readonly blockedBy: readonly EntityId[]
  /** True if all creatures that can block this creature must do so (Alluring Scent) */
  readonly mustBeBlockedByAll?: boolean
  /** Ordered list of blockers for damage assignment (first receives damage first). Null if not yet ordered. */
  readonly damageAssignmentOrder?: readonly EntityId[]
  /** Damage assigned to each target (blocker ID or player ID -> damage amount). Null if not yet assigned. */
  readonly damageAssignments?: Readonly<Record<EntityId, number>>
}

/**
 * What an attacker is attacking.
 * Matches backend ClientCombatTarget.kt
 */
export type ClientCombatTarget =
  | { readonly type: 'Player'; readonly playerId: EntityId }
  | { readonly type: 'Planeswalker'; readonly permanentId: EntityId }

/**
 * Blocker information for combat display.
 * Matches backend ClientBlocker.kt
 */
export interface ClientBlocker {
  readonly creatureId: EntityId
  readonly creatureName: string
  readonly blockingAttacker: EntityId
}

/**
 * Represents a chosen target for a spell or ability on the stack.
 * Matches backend ClientChosenTarget.kt
 */
export type ClientChosenTarget =
  | { readonly type: 'Player'; readonly playerId: EntityId }
  | { readonly type: 'Permanent'; readonly entityId: EntityId }
  | { readonly type: 'Spell'; readonly spellEntityId: EntityId }
  | { readonly type: 'Card'; readonly cardId: EntityId }

/**
 * A group of targets chosen for a single mode of a modal spell on the stack. The index
 * refers to the position within `ClientCard.perModeTargets`, not the original `Mode` index
 * (the same mode can appear twice with `allowRepeat`).
 * Matches backend ClientPerModeTargetGroup.kt
 */
export interface ClientPerModeTargetGroup {
  /** The mode index in the spell's `ModalEffect.modes` list. */
  readonly modeIndex: number
  /** Runtime description of the mode, with dynamic amounts evaluated. */
  readonly modeDescription: string
  /** Chosen targets for this mode, for arrow rendering. */
  readonly targets: readonly ClientChosenTarget[]
  /**
   * Human-readable target names aligned 1:1 with `targets`. For hidden-zone targets, the name
   * is generic ("a card in Opponent's hand") to avoid leaking hidden information.
   */
  readonly targetNames: readonly string[]
}

/**
 * Helper to check if a card is a creature.
 */
export function isCreature(card: ClientCard): boolean {
  return card.cardTypes.includes('Creature')
}

/**
 * Helper to check if a card is a land.
 */
export function isLand(card: ClientCard): boolean {
  return card.cardTypes.includes('Land')
}

/**
 * Helper to check if a card is an instant.
 */
export function isInstant(card: ClientCard): boolean {
  return card.cardTypes.includes('Instant')
}

/**
 * Helper to check if a card is a sorcery.
 */
export function isSorcery(card: ClientCard): boolean {
  return card.cardTypes.includes('Sorcery')
}

/**
 * Helper to get the effective toughness after damage.
 */
export function remainingToughness(card: ClientCard): number | null {
  if (card.toughness === null) return null
  return card.toughness - (card.damage ?? 0)
}

/**
 * Find a zone by type in the game state.
 */
export function findZone(
  state: ClientGameState,
  zoneType: ZoneType,
  ownerId: EntityId
): ClientZone | undefined {
  return state.zones.find(
    (z) => z.zoneId.zoneType === zoneType && z.zoneId.ownerId === ownerId
  )
}

/**
 * Get the viewing player's data.
 */
export function getViewingPlayer(state: ClientGameState): ClientPlayer | undefined {
  return state.players.find((p) => p.playerId === state.viewingPlayerId)
}

/**
 * Get the opponent's data.
 */
export function getOpponent(state: ClientGameState): ClientPlayer | undefined {
  return state.players.find((p) => p.playerId !== state.viewingPlayerId)
}

/**
 * Check if it's the viewing player's turn.
 */
export function isMyTurn(state: ClientGameState): boolean {
  return state.activePlayerId === state.viewingPlayerId
}

/**
 * Check if the viewing player has priority.
 */
export function hasPriority(state: ClientGameState): boolean {
  return state.priorityPlayerId === state.viewingPlayerId
}
