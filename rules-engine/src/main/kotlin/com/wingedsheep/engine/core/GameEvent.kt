package com.wingedsheep.engine.core

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
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
    val fromZone: Zone?,
    val toZone: Zone,
    val ownerId: EntityId,
    /** Last known +1/+1 counter count when leaving battlefield (for death triggers needing last known info) */
    val lastKnownCounterCount: Int = 0,
    /** Last known -1/-1 counter count when leaving battlefield (used by the persist keyword's "had no -1/-1 counters" check) */
    val lastKnownMinusOneMinusOneCounterCount: Int = 0,
    /** Last known total counter count (all counter types) when leaving battlefield. Used by triggers that care about any counter (e.g., Shadow Urchin). */
    val lastKnownTotalCounterCount: Int = 0,
    /** True if the leaving entity was a token. Used to suppress persist-style return triggers on tokens (Rule 702.79b). */
    val lastKnownWasToken: Boolean = false,
    /** Last known projected power when leaving battlefield (for trigger filters needing last known info) */
    val lastKnownPower: Int? = null,
    /** Last known projected toughness when leaving battlefield (for trigger filters needing last known info) */
    val lastKnownToughness: Int? = null,
    /** The original card name when this permanent entered as a copy (e.g., "Clever Impersonator") */
    val copyOfOriginalName: String? = null,
    /** For auras: the entity this aura was attached to when it left the battlefield (for "enchanted creature dies" triggers) */
    val lastKnownAttachedTo: EntityId? = null,
    /** Last known type line when leaving battlefield (for trigger detection when entity has been cleaned up, e.g., tokens) */
    val lastKnownTypeLine: TypeLine? = null,
    /**
     * Last known card definition id when leaving battlefield. Needed so dies/leaves triggers can
     * still be resolved for tokens after 704.5s cleans them out of the graveyard in the same SBA
     * pass as they were put there.
     */
    val lastKnownCardDefinitionId: String? = null,
    /** Last known projected keywords when leaving battlefield (for trigger filters needing keyword info after death) */
    val lastKnownKeywords: Set<String> = emptySet(),
    /** X value from the spell that put this permanent onto the battlefield (for ETB triggers using DynamicAmount.XValue) */
    val xValue: Int? = null
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
    val isCombatDamage: Boolean,
    val sourceName: String? = null,
    val targetName: String? = null,
    val targetIsPlayer: Boolean = false,
    val targetWasFaceDown: Boolean = false
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

/**
 * A player chose a creature type (e.g., "Choose a creature type" for Walking Desecration).
 * This is a public announcement visible to all players.
 */
@Serializable
@SerialName("CreatureTypeChosenEvent")
data class CreatureTypeChosenEvent(
    val playerId: EntityId,
    val chosenType: String,
    val sourceName: String?
) : GameEvent

/**
 * A creature's type was changed (e.g., "becomes a Goblin until end of turn").
 */
@Serializable
@SerialName("CreatureTypeChangedEvent")
data class CreatureTypeChangedEvent(
    val targetId: EntityId,
    val targetName: String,
    val newType: String,
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
    val casterId: EntityId,
    val targetNames: List<String> = emptyList(),
    val xValue: Int? = null,
    val wasKicked: Boolean = false,
    /** Total mana spent to cast this spell (for Expend trigger detection) */
    val totalManaSpent: Int = 0
) : GameEvent

/**
 * An ability was activated.
 */
@Serializable
@SerialName("AbilityActivatedEvent")
data class AbilityActivatedEvent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val abilityEntityId: EntityId? = null
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
    val description: String,
    val abilityEntityId: EntityId? = null
) : GameEvent

/**
 * A spell or ability's target was randomly reselected (e.g., by Grip of Chaos).
 */
@Serializable
@SerialName("TargetReselectedEvent")
data class TargetReselectedEvent(
    val spellOrAbilityName: String,
    val oldTargetName: String,
    val newTargetName: String,
    val sourceName: String
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
 * A copy of a spell was put onto the stack (Storm, Fork, Copy Target Spell, etc.).
 * Per rule 707.10 copies aren't cast, so this event is distinct from [SpellCastEvent]
 * and must not match "whenever you cast" triggers.
 */
@Serializable
@SerialName("SpellCopiedEvent")
data class SpellCopiedEvent(
    val copyEntityId: EntityId,
    val cardName: String,
    val controllerId: EntityId,
    val originalSpellId: EntityId? = null,
    val copyIndex: Int? = null,
    val copyTotal: Int? = null
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
 * An activated or triggered ability was countered.
 */
@Serializable
@SerialName("AbilityCounteredEvent")
data class AbilityCounteredEvent(
    val abilityEntityId: EntityId,
    val description: String
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
    val attackers: List<EntityId>,
    val attackerNames: List<String> = emptyList(),
    val attackingPlayerId: EntityId? = null
) : GameEvent

/**
 * Blockers were declared.
 */
@Serializable
@SerialName("BlockersDeclaredEvent")
data class BlockersDeclaredEvent(
    val blockers: Map<EntityId, List<EntityId>>,  // blocker -> blocked attackers
    val blockerNames: Map<EntityId, String> = emptyMap(),
    val attackerNames: Map<EntityId, String> = emptyMap()
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
 * Attacking player ordered their attackers for a blocker's damage assignment.
 */
@Serializable
@SerialName("AttackerOrderDeclaredEvent")
data class AttackerOrderDeclaredEvent(
    val blockerId: EntityId,
    val orderedAttackers: List<EntityId>  // First in list receives damage first
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
    val amount: Int,
    val entityName: String = ""
) : GameEvent

/**
 * Counters were removed from a permanent.
 */
@Serializable
@SerialName("CountersRemovedEvent")
data class CountersRemovedEvent(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int,
    val entityName: String = ""
) : GameEvent

/**
 * Loyalty on a planeswalker changed (due to ability activation).
 */
@Serializable
@SerialName("LoyaltyChangedEvent")
data class LoyaltyChangedEvent(
    val entityId: EntityId,
    val entityName: String,
    val change: Int
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
    val cardIds: List<EntityId>,
    val cardNames: List<String> = emptyList()
) : GameEvent

/**
 * A card was revealed from the first draw of a turn.
 * Emitted when a permanent with RevealFirstDrawEachTurn is on the battlefield
 * and the controller draws their first card of a turn.
 */
@Serializable
@SerialName("CardRevealedFromDrawEvent")
data class CardRevealedFromDrawEvent(
    val playerId: EntityId,
    val cardEntityId: EntityId,
    val cardName: String,
    val isCreature: Boolean
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
    val cardIds: List<EntityId>,
    val cardNames: List<String> = emptyList()
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
 * Permanents were sacrificed.
 */
@Serializable
@SerialName("PermanentsSacrificedEvent")
data class PermanentsSacrificedEvent(
    val playerId: EntityId,
    val permanentIds: List<EntityId>,
    val permanentNames: List<String> = emptyList()
) : GameEvent

// =============================================================================
// Class Level Events
// =============================================================================

/**
 * A Class enchantment gained a new level.
 * Used to fire "When this Class becomes level N" triggers.
 */
@Serializable
@SerialName("ClassLevelChangedEvent")
data class ClassLevelChangedEvent(
    val entityId: EntityId,
    val newLevel: Int,
    val controllerId: EntityId
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
    val playerId: EntityId,
    /** Human-readable description of what was decided, for the game log */
    val description: String? = null
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
    /** Rule 104.4c — SBAs never stabilized, treated as an unbreakable infinite loop. */
    INFINITE_LOOP,
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
    val reason: String,
    val controllerId: EntityId? = null
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
    val imageUris: List<String?> = emptyList(),
    val source: String? = null,
    /** If false, the revealing player does not see the reveal overlay (e.g., behold from hand) */
    val revealToSelf: Boolean = true,
    /**
     * Optional zone transition context. When the reveal represents a card moving
     * between zones (e.g., graveyard → hand via Morcant's Loyalist), the UI can
     * use these to render an explanatory message like
     * "Returned from graveyard to hand — <source>" instead of the generic "Revealed".
     */
    val fromZone: com.wingedsheep.sdk.core.Zone? = null,
    val toZone: com.wingedsheep.sdk.core.Zone? = null
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

// =============================================================================
// Morph Events
// =============================================================================

/**
 * A face-down creature was turned face up.
 */
@Serializable
@SerialName("TurnFaceUpEvent")
data class TurnFaceUpEvent(
    val entityId: EntityId,
    val cardName: String,
    val controllerId: EntityId,
    val xValue: Int? = null
) : GameEvent

/**
 * A creature was turned face down (e.g., by Backslide).
 */
@Serializable
@SerialName("TurnedFaceDownEvent")
data class TurnedFaceDownEvent(
    val entityId: EntityId,
    val controllerId: EntityId
) : GameEvent

/**
 * A double-faced permanent transformed (CR 701.28).
 *
 * [intoBackFace] is true when the permanent transformed from its front face to its back face,
 * and false when it transformed from its back face to its front face.
 * [newFaceName] is the name of the face that is now up after transform.
 */
@Serializable
@SerialName("TransformedEvent")
data class TransformedEvent(
    val entityId: EntityId,
    val intoBackFace: Boolean,
    val newFaceName: String,
    val controllerId: EntityId
) : GameEvent

// =============================================================================
// Control Events
// =============================================================================

/**
 * Control of a permanent changed.
 */
@Serializable
@SerialName("ControlChangedEvent")
data class ControlChangedEvent(
    val permanentId: EntityId,
    val permanentName: String,
    val oldControllerId: EntityId,
    val newControllerId: EntityId
) : GameEvent

// =============================================================================
// Targeting Events
// =============================================================================

/**
 * A permanent became the target of a spell or ability.
 * [firstTimeByThisController] indicates whether this is the first time this turn
 * the target was targeted by a spell/ability controlled by [controllerId].
 * Used for Valiant triggers ("for the first time each turn").
 */
@Serializable
@SerialName("BecomesTargetEvent")
data class BecomesTargetEvent(
    val targetEntityId: EntityId,
    val targetName: String,
    val sourceEntityId: EntityId,
    val controllerId: EntityId,
    val firstTimeByThisController: Boolean = true
) : GameEvent

// =============================================================================
// Cycling Events
// =============================================================================

/**
 * A player cycled a card.
 */
@Serializable
@SerialName("CardCycledEvent")
data class CardCycledEvent(
    val playerId: EntityId,
    val cardId: EntityId,
    val cardName: String
) : GameEvent

// =============================================================================
// Gift Events
// =============================================================================

/**
 * A player gave a gift (Bloomburrow gift mechanic).
 * Emitted when a gift mode is chosen and the gift effect resolves.
 *
 * @property controllerId The player who gave the gift
 * @property sourceId The card/spell that provided the gift
 * @property sourceName The name of the source card
 */
@Serializable
data class GiftGivenEvent(
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : GameEvent

// =============================================================================
// Coin Flip Events
// =============================================================================

/**
 * A player flipped a coin.
 *
 * @property playerId The player who flipped the coin
 * @property won Whether the player won the flip
 * @property sourceId The entity that caused the coin flip
 * @property sourceName The name of the card/ability that caused the coin flip
 */
@Serializable
@SerialName("CoinFlipEvent")
data class CoinFlipEvent(
    val playerId: EntityId,
    val won: Boolean,
    val sourceId: EntityId,
    val sourceName: String
) : GameEvent
