package com.wingedsheep.engine.core

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of all game events.
 * Events are emitted by the engine to describe what happened.
 */
@Serializable
sealed interface GameEvent

// =============================================================================
// Zone Change Events
// =============================================================================

/**
 * An entity moved between zones.
 */
@Serializable
@SerialName("ZoneChangeEvent")
data class ZoneChangeEvent(
    val entityId: EntityId,
    val entityName: String,
    val fromZone: ZoneType?,
    val toZone: ZoneType,
    val ownerId: EntityId
) : GameEvent

// =============================================================================
// Life Events
// =============================================================================

/**
 * A player's life total changed.
 */
@Serializable
@SerialName("LifeChangedEvent")
data class LifeChangedEvent(
    val playerId: EntityId,
    val oldLife: Int,
    val newLife: Int,
    val reason: LifeChangeReason
) : GameEvent

@Serializable
enum class LifeChangeReason {
    DAMAGE,
    LIFE_LOSS,
    LIFE_GAIN,
    PAYMENT
}

// =============================================================================
// Damage Events
// =============================================================================

/**
 * Damage was dealt.
 */
@Serializable
@SerialName("DamageDealtEvent")
data class DamageDealtEvent(
    val sourceId: EntityId?,
    val targetId: EntityId,
    val amount: Int,
    val isCombatDamage: Boolean
) : GameEvent

/**
 * Stats were modified (e.g., +3/+3 until end of turn).
 */
@Serializable
@SerialName("StatsModifiedEvent")
data class StatsModifiedEvent(
    val targetId: EntityId,
    val targetName: String,
    val powerChange: Int,
    val toughnessChange: Int,
    val sourceName: String
) : GameEvent

/**
 * A keyword was granted (e.g., "gains flying until end of turn").
 */
@Serializable
@SerialName("KeywordGrantedEvent")
data class KeywordGrantedEvent(
    val targetId: EntityId,
    val targetName: String,
    val keyword: String,
    val sourceName: String
) : GameEvent

// =============================================================================
// Spell/Ability Events
// =============================================================================

/**
 * A spell was cast.
 */
@Serializable
@SerialName("SpellCastEvent")
data class SpellCastEvent(
    val spellEntityId: EntityId,
    val cardName: String,
    val casterId: EntityId
) : GameEvent

/**
 * An ability was activated.
 */
@Serializable
@SerialName("AbilityActivatedEvent")
data class AbilityActivatedEvent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId
) : GameEvent

/**
 * An ability triggered.
 */
@Serializable
@SerialName("AbilityTriggeredEvent")
data class AbilityTriggeredEvent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val description: String
) : GameEvent

/**
 * A spell or ability resolved.
 */
@Serializable
@SerialName("ResolvedEvent")
data class ResolvedEvent(
    val entityId: EntityId,
    val name: String
) : GameEvent

/**
 * A spell was countered.
 */
@Serializable
@SerialName("SpellCounteredEvent")
data class SpellCounteredEvent(
    val spellEntityId: EntityId,
    val cardName: String
) : GameEvent

/**
 * A spell fizzled (all targets became invalid).
 */
@Serializable
@SerialName("SpellFizzledEvent")
data class SpellFizzledEvent(
    val spellEntityId: EntityId,
    val cardName: String,
    val reason: String
) : GameEvent

/**
 * An ability resolved.
 */
@Serializable
@SerialName("AbilityResolvedEvent")
data class AbilityResolvedEvent(
    val sourceId: EntityId,
    val description: String
) : GameEvent

/**
 * An ability fizzled (all targets became invalid).
 */
@Serializable
@SerialName("AbilityFizzledEvent")
data class AbilityFizzledEvent(
    val sourceId: EntityId,
    val description: String,
    val reason: String
) : GameEvent

// =============================================================================
// Combat Events
// =============================================================================

/**
 * Attackers were declared.
 */
@Serializable
@SerialName("AttackersDeclaredEvent")
data class AttackersDeclaredEvent(
    val attackers: List<EntityId>
) : GameEvent

/**
 * Blockers were declared.
 */
@Serializable
@SerialName("BlockersDeclaredEvent")
data class BlockersDeclaredEvent(
    val blockers: Map<EntityId, List<EntityId>>  // blocker -> blocked attackers
) : GameEvent

/**
 * Player ordered blockers for damage assignment.
 */
@Serializable
@SerialName("BlockerOrderDeclaredEvent")
data class BlockerOrderDeclaredEvent(
    val attackerId: EntityId,
    val orderedBlockers: List<EntityId>  // First in list receives damage first
) : GameEvent

/**
 * Combat damage was assigned.
 */
@Serializable
@SerialName("DamageAssignedEvent")
data class DamageAssignedEvent(
    val attackerId: EntityId,
    val assignments: Map<EntityId, Int>  // target -> damage amount
) : GameEvent

// =============================================================================
// Turn/Phase Events
// =============================================================================

/**
 * The phase changed.
 */
@Serializable
@SerialName("PhaseChangedEvent")
data class PhaseChangedEvent(
    val newPhase: Phase
) : GameEvent

/**
 * The step changed.
 */
@Serializable
@SerialName("StepChangedEvent")
data class StepChangedEvent(
    val newStep: Step
) : GameEvent

/**
 * The turn changed.
 */
@Serializable
@SerialName("TurnChangedEvent")
data class TurnChangedEvent(
    val turnNumber: Int,
    val activePlayerId: EntityId
) : GameEvent

/**
 * Priority changed to a player.
 */
@Serializable
@SerialName("PriorityChangedEvent")
data class PriorityChangedEvent(
    val playerId: EntityId
) : GameEvent

// =============================================================================
// Permanent Events
// =============================================================================

/**
 * A permanent was tapped.
 */
@Serializable
@SerialName("TappedEvent")
data class TappedEvent(
    val entityId: EntityId,
    val entityName: String
) : GameEvent

/**
 * A permanent was untapped.
 */
@Serializable
@SerialName("UntappedEvent")
data class UntappedEvent(
    val entityId: EntityId,
    val entityName: String
) : GameEvent

/**
 * Counters were added to a permanent.
 */
@Serializable
@SerialName("CountersAddedEvent")
data class CountersAddedEvent(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int
) : GameEvent

/**
 * Counters were removed from a permanent.
 */
@Serializable
@SerialName("CountersRemovedEvent")
data class CountersRemovedEvent(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int
) : GameEvent

// =============================================================================
// Card Events
// =============================================================================

/**
 * Cards were drawn.
 */
@Serializable
@SerialName("CardsDrawnEvent")
data class CardsDrawnEvent(
    val playerId: EntityId,
    val count: Int,
    val cardIds: List<EntityId>
) : GameEvent

/**
 * A player failed to draw (empty library).
 */
@Serializable
@SerialName("DrawFailedEvent")
data class DrawFailedEvent(
    val playerId: EntityId,
    val reason: String
) : GameEvent

/**
 * Cards were discarded.
 */
@Serializable
@SerialName("CardsDiscardedEvent")
data class CardsDiscardedEvent(
    val playerId: EntityId,
    val cardIds: List<EntityId>
) : GameEvent

/**
 * A player needs to discard cards during cleanup.
 */
@Serializable
@SerialName("DiscardRequiredEvent")
data class DiscardRequiredEvent(
    val playerId: EntityId,
    val count: Int
) : GameEvent

/**
 * Library was shuffled.
 */
@Serializable
@SerialName("LibraryShuffledEvent")
data class LibraryShuffledEvent(
    val playerId: EntityId
) : GameEvent

/**
 * Scry completed - cards were put on top/bottom of library.
 */
@Serializable
@SerialName("ScryCompletedEvent")
data class ScryCompletedEvent(
    val playerId: EntityId,
    val cardsOnTop: Int,
    val cardsOnBottom: Int
) : GameEvent

/**
 * Permanents were sacrificed.
 */
@Serializable
@SerialName("PermanentsSacrificedEvent")
data class PermanentsSacrificedEvent(
    val playerId: EntityId,
    val permanentIds: List<EntityId>
) : GameEvent

// =============================================================================
// Decision Events
// =============================================================================

/**
 * The engine paused and is awaiting a decision.
 */
@Serializable
@SerialName("DecisionRequestedEvent")
data class DecisionRequestedEvent(
    val decisionId: String,
    val playerId: EntityId,
    val decisionType: String,
    val prompt: String
) : GameEvent

/**
 * A player submitted a decision response.
 */
@Serializable
@SerialName("DecisionSubmittedEvent")
data class DecisionSubmittedEvent(
    val decisionId: String,
    val playerId: EntityId
) : GameEvent

// =============================================================================
// Game State Events
// =============================================================================

/**
 * The game ended.
 */
@Serializable
@SerialName("GameEndedEvent")
data class GameEndedEvent(
    val winnerId: EntityId?,
    val reason: GameEndReason
) : GameEvent

@Serializable
enum class GameEndReason {
    LIFE_ZERO,
    DECK_EMPTY,
    POISON_COUNTERS,
    CONCESSION,
    ALTERNATIVE_WIN,
    CARD_EFFECT,
    DRAW,
    UNKNOWN
}

/**
 * A player lost the game.
 */
@Serializable
@SerialName("PlayerLostEvent")
data class PlayerLostEvent(
    val playerId: EntityId,
    val reason: GameEndReason,
    val message: String? = null
) : GameEvent

// =============================================================================
// Creature Events
// =============================================================================

/**
 * A creature was destroyed.
 */
@Serializable
@SerialName("CreatureDestroyedEvent")
data class CreatureDestroyedEvent(
    val entityId: EntityId,
    val name: String,
    val reason: String
) : GameEvent

// =============================================================================
// Mana Events
// =============================================================================

/**
 * Mana was added to a player's pool.
 */
@Serializable
@SerialName("ManaAddedEvent")
data class ManaAddedEvent(
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0
) : GameEvent {
    val total: Int get() = white + blue + black + red + green + colorless
}

/**
 * Mana was spent from a player's pool.
 */
@Serializable
@SerialName("ManaSpentEvent")
data class ManaSpentEvent(
    val playerId: EntityId,
    val reason: String,
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0
) : GameEvent {
    val total: Int get() = white + blue + black + red + green + colorless
}

// =============================================================================
// Information Events
// =============================================================================

/**
 * A player looked at another player's hand.
 * The cards in the hand are now revealed to the viewing player.
 */
@Serializable
@SerialName("HandLookedAtEvent")
data class HandLookedAtEvent(
    val viewingPlayerId: EntityId,
    val targetPlayerId: EntityId,
    val cardIds: List<EntityId>
) : GameEvent

/**
 * A player revealed their hand to all players.
 * Unlike HandLookedAtEvent, this reveals cards publicly to everyone.
 */
@Serializable
@SerialName("HandRevealedEvent")
data class HandRevealedEvent(
    val revealingPlayerId: EntityId,
    val cardIds: List<EntityId>
) : GameEvent

/**
 * A player revealed specific cards to all players.
 * Used for tutor effects that require revealing the chosen card.
 */
@Serializable
@SerialName("CardsRevealedEvent")
data class CardsRevealedEvent(
    val revealingPlayerId: EntityId,
    val cardIds: List<EntityId>,
    val cardNames: List<String>,
    val source: String? = null
) : GameEvent

/**
 * A player looked at cards (from library, etc.).
 * Used for "look at the top N cards" effects.
 */
@Serializable
@SerialName("LookedAtCardsEvent")
data class LookedAtCardsEvent(
    val playerId: EntityId,
    val cardIds: List<EntityId>,
    val source: String? = null
) : GameEvent

/**
 * A player reordered cards on top of their library.
 * Used for effects like Omen ("put them back in any order").
 */
@Serializable
@SerialName("LibraryReorderedEvent")
data class LibraryReorderedEvent(
    val playerId: EntityId,
    val cardCount: Int,
    val source: String? = null
) : GameEvent
