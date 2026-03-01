package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import kotlinx.serialization.Serializable

/**
 * Universal filter for matching game objects (cards, permanents, spells).
 * Composes CardPredicate, StatePredicate, and ControllerPredicate for flexible filtering.
 *
 * This replaces the scattered filter types (CardFilter, CountFilter, etc.) with a unified,
 * composable approach.
 *
 * Note: Named GameObjectFilter to distinguish from other filter types in the SDK
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // Simple type filter
 * GameObjectFilter.Creature
 *
 * // Creature with specific properties
 * GameObjectFilter.Creature.withColor(Color.BLACK).tapped()
 *
 * // Controlled creatures
 * GameObjectFilter.Creature.youControl()
 *
 * // Complex filter: tapped creatures with power 2 or less that opponent controls
 * GameObjectFilter.Creature
 *     .tapped()
 *     .powerAtMost(2)
 *     .opponentControls()
 * ```
 */
@Serializable
data class GameObjectFilter(
    val cardPredicates: List<CardPredicate> = emptyList(),
    val statePredicates: List<StatePredicate> = emptyList(),
    val controllerPredicate: ControllerPredicate? = null,
    val matchAll: Boolean = true  // true = AND all predicates, false = OR
) {
    val description: String
        get() = buildDescription()

    private fun buildDescription(): String = buildString {
        controllerPredicate?.let {
            if (it.description.isNotEmpty()) {
                append(it.description)
                append(" ")
            }
        }
        statePredicates.forEach { predicate ->
            append(predicate.description)
            append(" ")
        }
        cardPredicates.forEach { predicate ->
            append(predicate.description)
            append(" ")
        }
    }.trim().ifEmpty { "card" }

    // =============================================================================
    // Pre-built Common Filters
    // =============================================================================

    companion object {
        /** Match any object */
        val Any = GameObjectFilter()

        // Type filters
        val Creature = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsCreature))
        val Land = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsLand))
        val BasicLand = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsBasicLand))
        val Artifact = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsArtifact))
        val Enchantment = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsEnchantment))
        val Planeswalker = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsPlaneswalker))
        val Instant = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsInstant))
        val Sorcery = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsSorcery))
        val Permanent = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsPermanent))
        val Nonland = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNonland))
        val NonlandPermanent = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsNonland, CardPredicate.IsPermanent)
        )
        val Noncreature = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsNoncreature))
        val Token = GameObjectFilter(cardPredicates = listOf(CardPredicate.IsToken))

        // Combined type filters
        val InstantOrSorcery = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsInstant, CardPredicate.IsSorcery))
            )
        )
        val CreatureOrPlaneswalker = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsPlaneswalker))
            )
        )
        val CreatureOrLand = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsLand))
            )
        )
        val CreatureOrSorcery = GameObjectFilter(
            cardPredicates = listOf(
                CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsSorcery))
            )
        )
        val NoncreaturePermanent = GameObjectFilter(
            cardPredicates = listOf(CardPredicate.IsNoncreature, CardPredicate.IsPermanent)
        )
    }

    // =============================================================================
    // Fluent Builder Methods - Card Predicates
    // =============================================================================

    /** Add a color requirement */
    fun withColor(color: Color) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasColor(color)
    )

    /** Exclude a color */
    fun notColor(color: Color) = copy(
        cardPredicates = cardPredicates + CardPredicate.NotColor(color)
    )

    /** Add a subtype requirement */
    fun withSubtype(subtype: Subtype) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasSubtype(subtype)
    )

    /** Add a subtype requirement by string */
    fun withSubtype(subtype: String) = withSubtype(Subtype(subtype))

    /** Exclude a subtype */
    fun notSubtype(subtype: Subtype) = copy(
        cardPredicates = cardPredicates + CardPredicate.NotSubtype(subtype)
    )

    /** Add a keyword requirement */
    fun withKeyword(keyword: Keyword) = copy(
        cardPredicates = cardPredicates + CardPredicate.HasKeyword(keyword)
    )

    /** Exclude a keyword */
    fun withoutKeyword(keyword: Keyword) = copy(
        cardPredicates = cardPredicates + CardPredicate.NotKeyword(keyword)
    )

    /** Match by exact card name */
    fun named(name: String) = copy(
        cardPredicates = cardPredicates + CardPredicate.NameEquals(name)
    )

    /** Mana value equals */
    fun manaValue(value: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueEquals(value)
    )

    /** Mana value at most */
    fun manaValueAtMost(max: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtMost(max)
    )

    /** Mana value at least */
    fun manaValueAtLeast(min: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ManaValueAtLeast(min)
    )

    /** Power equals */
    fun power(value: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerEquals(value)
    )

    /** Power at most */
    fun powerAtMost(max: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerAtMost(max)
    )

    /** Power at least */
    fun powerAtLeast(min: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.PowerAtLeast(min)
    )

    /** Toughness at most */
    fun toughnessAtMost(max: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ToughnessAtMost(max)
    )

    /** Toughness at least */
    fun toughnessAtLeast(min: Int) = copy(
        cardPredicates = cardPredicates + CardPredicate.ToughnessAtLeast(min)
    )

    /** Must not be of the creature type chosen on the source permanent */
    fun notOfSourceChosenType() = copy(
        cardPredicates = cardPredicates + CardPredicate.NotOfSourceChosenType
    )

    // =============================================================================
    // Fluent Builder Methods - State Predicates
    // =============================================================================

    /** Must be tapped */
    fun tapped() = copy(
        statePredicates = statePredicates + StatePredicate.IsTapped
    )

    /** Must be untapped */
    fun untapped() = copy(
        statePredicates = statePredicates + StatePredicate.IsUntapped
    )

    /** Must be attacking */
    fun attacking() = copy(
        statePredicates = statePredicates + StatePredicate.IsAttacking
    )

    /** Must be blocking */
    fun blocking() = copy(
        statePredicates = statePredicates + StatePredicate.IsBlocking
    )

    /** Must be attacking or blocking */
    fun attackingOrBlocking() = copy(
        statePredicates = statePredicates + StatePredicate.IsAttackingOrBlocking
    )

    /** Must have entered the battlefield this turn */
    fun enteredThisTurn() = copy(
        statePredicates = statePredicates + StatePredicate.EnteredThisTurn
    )

    /** Must be face-down */
    fun faceDown() = copy(
        statePredicates = statePredicates + StatePredicate.IsFaceDown
    )

    /** Must be face-up (not face-down) */
    fun faceUp() = copy(
        statePredicates = statePredicates + StatePredicate.IsFaceUp
    )

    /** Must have a morph ability */
    fun withMorph() = copy(
        statePredicates = statePredicates + StatePredicate.HasMorphAbility
    )

    /** Must have a counter of the specified type */
    fun withCounter(counterType: String) = copy(
        statePredicates = statePredicates + StatePredicate.HasCounter(counterType)
    )

    // =============================================================================
    // Fluent Builder Methods - Controller Predicates
    // =============================================================================

    /** Must be controlled by you */
    fun youControl() = copy(controllerPredicate = ControllerPredicate.ControlledByYou)

    /** Must be controlled by an opponent */
    fun opponentControls() = copy(controllerPredicate = ControllerPredicate.ControlledByOpponent)

    /** Must be controlled by the target opponent */
    fun targetOpponentControls() = copy(controllerPredicate = ControllerPredicate.ControlledByTargetOpponent)

    /** Must be controlled by the target player */
    fun targetPlayerControls() = copy(controllerPredicate = ControllerPredicate.ControlledByTargetPlayer)

    /** Must be owned by you (for cards in graveyards/exile that don't have controllers) */
    fun ownedByYou() = copy(controllerPredicate = ControllerPredicate.OwnedByYou)

    /** Must be owned by an opponent (for cards in graveyards/exile that don't have controllers) */
    fun ownedByOpponent() = copy(controllerPredicate = ControllerPredicate.OwnedByOpponent)

    // =============================================================================
    // Composition
    // =============================================================================

    /** Combine with another filter using AND logic */
    infix fun and(other: GameObjectFilter) = copy(
        cardPredicates = cardPredicates + other.cardPredicates,
        statePredicates = statePredicates + other.statePredicates,
        controllerPredicate = other.controllerPredicate ?: controllerPredicate
    )

    /** Combine with another filter using OR logic */
    infix fun or(other: GameObjectFilter) = GameObjectFilter(
        cardPredicates = listOf(
            CardPredicate.Or(
                cardPredicates + other.cardPredicates
            )
        ),
        statePredicates = statePredicates + other.statePredicates,
        controllerPredicate = other.controllerPredicate ?: controllerPredicate,
        matchAll = false
    )
}
