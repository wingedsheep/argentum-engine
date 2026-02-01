import { EntityId } from './entities'

/**
 * Client-facing game events.
 * Matches backend ClientEvent.kt
 *
 * These are simplified versions of internal GameActionEvents for
 * driving animations and displaying game log.
 */
export type ClientEvent =
  | LifeChangedEvent
  | DamageDealtEvent
  | StatsModifiedEvent
  | CardDrawnEvent
  | CardDiscardedEvent
  | PermanentEnteredEvent
  | PermanentLeftEvent
  | CreatureAttackedEvent
  | CreatureBlockedEvent
  | CreatureDiedEvent
  | SpellCastEvent
  | SpellResolvedEvent
  | SpellCounteredEvent
  | AbilityTriggeredEvent
  | AbilityActivatedEvent
  | PermanentTappedEvent
  | PermanentUntappedEvent
  | CounterAddedEvent
  | CounterRemovedEvent
  | ManaAddedEvent
  | PlayerLostEvent
  | GameEndedEvent
  | HandLookedAtEvent
  | HandRevealedEvent
  | CardsRevealedEvent

// ============================================================================
// Life/Damage Events
// ============================================================================

export interface LifeChangedEvent {
  readonly type: 'lifeChanged'
  readonly playerId: EntityId
  readonly oldLife: number
  readonly newLife: number
  readonly change: number
  readonly description: string
}

export interface DamageDealtEvent {
  readonly type: 'damageDealt'
  readonly sourceId: EntityId | null
  readonly sourceName: string | null
  readonly targetId: EntityId
  readonly targetName: string
  readonly amount: number
  readonly targetIsPlayer: boolean
  readonly isCombatDamage: boolean
  readonly description: string
}

export interface StatsModifiedEvent {
  readonly type: 'statsModified'
  readonly targetId: EntityId
  readonly targetName: string
  readonly powerChange: number
  readonly toughnessChange: number
  readonly sourceName: string
  readonly description: string
}

// ============================================================================
// Card Movement Events
// ============================================================================

export interface CardDrawnEvent {
  readonly type: 'cardDrawn'
  readonly playerId: EntityId
  readonly cardId: EntityId
  readonly cardName: string | null // Null if hidden from viewing player
  readonly description: string
}

export interface CardDiscardedEvent {
  readonly type: 'cardDiscarded'
  readonly playerId: EntityId
  readonly cardId: EntityId
  readonly cardName: string
  readonly description: string
}

export interface PermanentEnteredEvent {
  readonly type: 'permanentEntered'
  readonly cardId: EntityId
  readonly cardName: string
  readonly controllerId: EntityId
  readonly enteredTapped: boolean
  readonly description: string
}

export interface PermanentLeftEvent {
  readonly type: 'permanentLeft'
  readonly cardId: EntityId
  readonly cardName: string
  readonly destination: 'graveyard' | 'exile' | 'hand' | 'library'
  readonly description: string
}

// ============================================================================
// Combat Events
// ============================================================================

export interface CreatureAttackedEvent {
  readonly type: 'creatureAttacked'
  readonly creatureId: EntityId
  readonly creatureName: string
  readonly attackingPlayerId: EntityId
  readonly defendingPlayerId: EntityId
  readonly description: string
}

export interface CreatureBlockedEvent {
  readonly type: 'creatureBlocked'
  readonly blockerId: EntityId
  readonly blockerName: string
  readonly attackerId: EntityId
  readonly attackerName: string
  readonly description: string
}

export interface CreatureDiedEvent {
  readonly type: 'creatureDied'
  readonly creatureId: EntityId
  readonly creatureName: string
  readonly description: string
}

// ============================================================================
// Spell/Ability Events
// ============================================================================

export interface SpellCastEvent {
  readonly type: 'spellCast'
  readonly spellId: EntityId
  readonly spellName: string
  readonly casterId: EntityId
  readonly description: string
}

export interface SpellResolvedEvent {
  readonly type: 'spellResolved'
  readonly spellId: EntityId
  readonly spellName: string
  readonly description: string
}

export interface SpellCounteredEvent {
  readonly type: 'spellCountered'
  readonly spellId: EntityId
  readonly spellName: string
  readonly description: string
}

export interface AbilityTriggeredEvent {
  readonly type: 'abilityTriggered'
  readonly sourceId: EntityId
  readonly sourceName: string
  readonly abilityDescription: string
  readonly description: string
}

export interface AbilityActivatedEvent {
  readonly type: 'abilityActivated'
  readonly sourceId: EntityId
  readonly sourceName: string
  readonly abilityDescription: string
  readonly description: string
}

// ============================================================================
// State Change Events
// ============================================================================

export interface PermanentTappedEvent {
  readonly type: 'permanentTapped'
  readonly permanentId: EntityId
  readonly permanentName: string
  readonly description: string
}

export interface PermanentUntappedEvent {
  readonly type: 'permanentUntapped'
  readonly permanentId: EntityId
  readonly permanentName: string
  readonly description: string
}

export interface CounterAddedEvent {
  readonly type: 'counterAdded'
  readonly permanentId: EntityId
  readonly permanentName: string
  readonly counterType: string
  readonly count: number
  readonly description: string
}

export interface CounterRemovedEvent {
  readonly type: 'counterRemoved'
  readonly permanentId: EntityId
  readonly permanentName: string
  readonly counterType: string
  readonly count: number
  readonly description: string
}

// ============================================================================
// Mana Events
// ============================================================================

export interface ManaAddedEvent {
  readonly type: 'manaAdded'
  readonly playerId: EntityId
  readonly manaString: string // e.g., "{G}{G}" or "{R}"
  readonly description: string
}

// ============================================================================
// Game State Events
// ============================================================================

export interface PlayerLostEvent {
  readonly type: 'playerLost'
  readonly playerId: EntityId
  readonly reason: string
  readonly description: string
}

export interface GameEndedEvent {
  readonly type: 'gameEnded'
  readonly winnerId: EntityId | null
  readonly description: string
}

// ============================================================================
// Information Events
// ============================================================================

export interface HandLookedAtEvent {
  readonly type: 'handLookedAt'
  readonly viewingPlayerId: EntityId
  readonly targetPlayerId: EntityId
  readonly cardIds: readonly EntityId[]
  readonly description: string
}

export interface HandRevealedEvent {
  readonly type: 'handRevealed'
  readonly revealingPlayerId: EntityId
  readonly cardIds: readonly EntityId[]
  readonly description: string
}

export interface CardsRevealedEvent {
  readonly type: 'cardsRevealed'
  readonly revealingPlayerId: EntityId
  readonly cardIds: readonly EntityId[]
  readonly cardNames: readonly string[]
  readonly imageUris: readonly (string | null)[]
  readonly source: string | null
  readonly description: string
}

// ============================================================================
// Type Guards
// ============================================================================

export function isLifeChangedEvent(event: ClientEvent): event is LifeChangedEvent {
  return event.type === 'lifeChanged'
}

export function isDamageDealtEvent(event: ClientEvent): event is DamageDealtEvent {
  return event.type === 'damageDealt'
}

export function isStatsModifiedEvent(event: ClientEvent): event is StatsModifiedEvent {
  return event.type === 'statsModified'
}

export function isCardDrawnEvent(event: ClientEvent): event is CardDrawnEvent {
  return event.type === 'cardDrawn'
}

export function isPermanentEnteredEvent(event: ClientEvent): event is PermanentEnteredEvent {
  return event.type === 'permanentEntered'
}

export function isCreatureDiedEvent(event: ClientEvent): event is CreatureDiedEvent {
  return event.type === 'creatureDied'
}

export function isSpellCastEvent(event: ClientEvent): event is SpellCastEvent {
  return event.type === 'spellCast'
}

export function isPermanentTappedEvent(event: ClientEvent): event is PermanentTappedEvent {
  return event.type === 'permanentTapped'
}

export function isPermanentUntappedEvent(event: ClientEvent): event is PermanentUntappedEvent {
  return event.type === 'permanentUntapped'
}

export function isHandLookedAtEvent(event: ClientEvent): event is HandLookedAtEvent {
  return event.type === 'handLookedAt'
}
