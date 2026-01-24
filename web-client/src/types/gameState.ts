import { Color, CounterType, Keyword, Phase, Step, ZoneType } from './enums'
import { EntityId, ZoneId } from './entities'

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

  /** Power for creatures (null if not a creature) */
  readonly power: number | null

  /** Toughness for creatures (null if not a creature) */
  readonly toughness: number | null

  /** Current damage on creature (only present on battlefield) */
  readonly damage: number | null

  /** Keywords the card has (flying, haste, etc.) */
  readonly keywords: readonly Keyword[]

  /** Counters on the card */
  readonly counters: Partial<Record<CounterType, number>>

  /** State flags */
  readonly isTapped: boolean
  readonly hasSummoningSickness: boolean
  readonly isTransformed: boolean

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

  /** Whether this card is face-down (for morph, manifest, hidden info) */
  readonly isFaceDown: boolean
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
 * Matches backend MaskedPlayer.kt
 */
export interface ClientPlayer {
  readonly playerId: EntityId
  readonly name: string
  readonly life: number
  readonly poisonCounters: number
  readonly handSize: number
  readonly librarySize: number
  readonly graveyardSize: number
  readonly landsPlayedThisTurn: number
  readonly hasLost: boolean
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
}

/**
 * Calculate total mana in pool.
 */
export function totalMana(pool: ClientManaPool): number {
  return pool.white + pool.blue + pool.black + pool.red + pool.green + pool.colorless
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
