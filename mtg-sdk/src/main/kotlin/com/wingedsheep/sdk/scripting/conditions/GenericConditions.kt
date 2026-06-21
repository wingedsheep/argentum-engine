package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Collection Conditions (pipeline-based)
// =============================================================================

/**
 * Condition: "if a card in the named collection matches [filter]"
 * Checks whether any entity in a stored pipeline collection matches the given filter.
 * Used for "if you did X this way" patterns where the card selected/milled/returned
 * needs to be checked for a property (e.g., "if you returned a Squirrel card").
 */
@SerialName("CollectionContainsMatch")
@Serializable
data class CollectionContainsMatch(
    val collection: String,
    val filter: GameObjectFilter = GameObjectFilter.Any
) : Condition {
    override val description: String = "if those cards contain a ${filter.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Condition: "if [the target] is a creature card" — tests the *underlying card's* card type,
 * reading the base [com.wingedsheep.sdk.model.CardDefinition] characteristics rather than projected
 * state. This is the correct test for a face-down permanent: while face down it projects as a
 * typeless 2/2 Creature regardless of what it really is (so [EntityMatches] over projected state
 * would match every face-down permanent), but "creature *card*" refers to the hidden card itself
 * (CR 708.2). Resolution-only; resolves the chosen target ([index]) to its game object and checks
 * its card's printed types.
 *
 * Models the "If it's a creature card, ..." clause of "Reveal target face-down permanent. If it's a
 * creature card, you may turn it face up." (Hauntwoods Shrieker).
 *
 * @property index Which context target to test (default: the first target).
 */
@SerialName("TargetIsCreatureCard")
@Serializable
data class TargetIsCreatureCard(
    val index: Int = 0
) : Condition {
    override val description: String = "if it's a creature card"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
}

// =============================================================================
// Generic Condition Primitives
// =============================================================================

/**
 * Comparison operators for [Compare] conditions.
 */
@Serializable
enum class ComparisonOperator {
    LT, LTE, EQ, NEQ, GT, GTE;

    val symbol: String
        get() = when (this) {
            LT -> "<"
            LTE -> "<="
            EQ -> "=="
            NEQ -> "!="
            GT -> ">"
            GTE -> ">="
        }
}

/**
 * Generic numeric comparison condition.
 * Evaluates two [DynamicAmount] sides and compares them with [operator].
 *
 * Examples:
 * ```kotlin
 * // Life at most 5
 * Compare(LifeTotal(Player.You), ComparisonOperator.LTE, Fixed(5))
 *
 * // More life than opponent
 * Compare(LifeTotal(Player.You), ComparisonOperator.GT, LifeTotal(Player.EachOpponent))
 * ```
 */
@SerialName("Compare")
@Serializable
data class Compare(
    val left: DynamicAmount,
    val operator: ComparisonOperator,
    val right: DynamicAmount
) : Condition {
    override val description: String = "${left.description} ${operator.symbol} ${right.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newLeft = left.applyTextReplacement(replacer)
        val newRight = right.applyTextReplacement(replacer)
        return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
    }
}

/**
 * Generic zone-presence condition.
 * Checks whether any matching object exists in a player's zone.
 *
 * Examples:
 * ```kotlin
 * // You control a creature
 * Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature)
 *
 * // Empty hand (negate = true means "no objects exist")
 * Exists(Player.You, Zone.HAND, negate = true)
 * ```
 */
/**
 * Condition: some player in the game has [threshold] or less life.
 *
 * Existential quantifier over all players in turn order — distinct from
 * [Compare] of `DynamicAmount.LifeTotal(player)`, which resolves to a single
 * player's life and can't express "any player" / "some player" cleanly. The
 * engine iterates `state.turnOrder` and returns true as soon as one player's
 * life total is at most [threshold].
 *
 * Used by cards like Razortrap Gorge ("this land enters tapped unless a
 * player has 13 or less life").
 *
 * @property threshold The maximum life total at which the condition is true.
 */
@SerialName("APlayerLifeAtMost")
@Serializable
data class APlayerLifeAtMost(val threshold: Int) : Condition {
    override val description: String = "a player has $threshold or less life"
}

@SerialName("Exists")
@Serializable
data class Exists(
    val player: Player,
    val zone: Zone,
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val negate: Boolean = false,
    /** When true, exclude the source entity from the search (for "another" wording). */
    val excludeSelf: Boolean = false
) : Condition {
    override val description: String = buildString {
        if (negate) {
            append("if there are no ")
            if (excludeSelf) append("other ")
            append(filter.description.ifEmpty { "cards" })
            append(" in ")
        } else {
            append("if ")
            if (excludeSelf) append("another ") else append("a ")
            append(filter.description.ifEmpty { "card" })
            append(" is in ")
        }
        append(player.possessive)
        append(" ")
        append(zone.displayName.removePrefix("a ").removePrefix("the "))
    }
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
