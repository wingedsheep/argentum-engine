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
 * Condition: "the player who triggered this ability is [player]".
 *
 * Compares the triggering player (the one carried in the trigger context — e.g. the player who
 * lost the game, gained life, cast the spell) against another resolved player reference. Used to
 * narrow a broad "whenever a player does X" trigger to a specific player without a bespoke event
 * filter: Shinryu, Transcendent Rival gates "When the chosen player loses the game, you win the
 * game" as `triggerCondition = TriggeringPlayerIs(Player.ChosenOpponent)`. Both sides resolve via
 * the engine's shared player resolver, so any [Player] reference works on the right-hand side.
 */
@SerialName("TriggeringPlayerIs")
@Serializable
data class TriggeringPlayerIs(
    val player: Player
) : Condition {
    override val description: String = "if the triggering player is ${player.description}"
}

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
 * Condition: "if two cards in the named collection share a card type".
 *
 * True when at least two distinct cards in the stored pipeline [collection] have a card type
 * (artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, sorcery — CR
 * 205.2a) in common. A collection of fewer than two cards is always false. Supertypes (legendary,
 * basic, snow) and subtypes (Horror, Room, …) are not card types and are ignored.
 *
 * Models "if two cards that share a card type were [milled/revealed] this way" — e.g. The Tale of
 * Tamiyo's "Mill two cards. If two cards that share a card type were milled this way, draw a card
 * and repeat this process." Pair it with a prior mill/reveal that stores the cards under
 * [collection], inside both an [com.wingedsheep.sdk.scripting.effects.ConditionalEffect] (for the
 * "draw a card") and a [com.wingedsheep.sdk.scripting.effects.RepeatCondition.WhileCondition] (for
 * the "repeat this process").
 */
@SerialName("CollectionSharesCardType")
@Serializable
data class CollectionSharesCardType(
    val collection: String
) : Condition {
    override val description: String = "if two of those cards share a card type"
    override fun applyTextReplacement(replacer: TextReplacer): Condition = this
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

/**
 * Condition: the chosen target at [index] is a **spell on the stack** (a `ChosenTarget.Spell`),
 * as opposed to a permanent on the battlefield.
 *
 * The right test to branch a single "target creature **or spell**" target (Aang, Swift Savior —
 * "airbend up to one other target creature or spell"): a creature *spell* on the stack would still
 * satisfy a `Creature` `GameObjectFilter`, so [com.wingedsheep.sdk.dsl.Conditions.TargetMatchesFilter]
 * can't distinguish it — this checks the target's *zone role* instead. Resolution-only.
 *
 * @property index Which context target to test (default: the first target).
 */
@SerialName("TargetIsSpellOnStack")
@Serializable
data class TargetIsSpellOnStack(
    val index: Int = 0
) : Condition {
    override val description: String = "if the target is a spell"
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

    /**
     * The mathematical symbol. Debug/diagnostic rendering only — anything a player reads should use
     * [phrase] instead, so "…can't attack unless X >= 2" comes out as "…unless X is at least 2".
     */
    val symbol: String
        get() = when (this) {
            LT -> "<"
            LTE -> "<="
            EQ -> "=="
            NEQ -> "!="
            GT -> ">"
            GTE -> ">="
        }

    /**
     * English rendering of the comparison, phrased to follow an "is": "is **at least** 2",
     * "is **greater than** the number of creatures defending player controls".
     *
     * Mirrors Magic's own comparative wording ("two or more" → at least, "fewer than" → less than)
     * rather than inventing a house style, so a [Compare] description drops into rules text without
     * reading like a debug dump.
     */
    val phrase: String
        get() = when (this) {
            LT -> "less than"
            LTE -> "at most"
            EQ -> "exactly"
            NEQ -> "not"
            GT -> "greater than"
            GTE -> "at least"
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
    override val description: String = "${left.description} is ${operator.phrase} ${right.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newLeft = left.applyTextReplacement(replacer)
        val newRight = right.applyTextReplacement(replacer)
        return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
    }
}

/**
 * True when [player] has the most life, or is tied for the most life, among all players
 * (their life total ≥ every player's). The "most life" comparison that a binary [Compare] can't
 * express (it would need the max over every player). Resolve [player] to the controller
 * (`Player.You`), the attacked player (`Player.DefendingPlayer`), the triggering player, etc.
 *
 * Preacher of the Schism: "Whenever this creature attacks the player with the most life or tied for
 * most life, …" (`triggerCondition = PlayerHasMostLife(Player.DefendingPlayer)`) and "Whenever this
 * creature attacks while you have the most life or are tied for most life, …"
 * (`PlayerHasMostLife(Player.You)`).
 */
@SerialName("PlayerHasMostLife")
@Serializable
data class PlayerHasMostLife(val player: Player) : Condition {
    override val description: String =
        "if ${player.description} has the most life or is tied for most life"
}

/**
 * True when [player] controls the most permanents matching [filter], or is tied for the most,
 * among all players (their count ≥ every player's). The board-count sibling of
 * [PlayerHasMostLife] — the same shape a binary [Compare] can't express, since it needs the max
 * over every player rather than a fixed threshold.
 *
 * Counts are read from the projected battlefield, so type-changing continuous effects (an animated
 * land, a creature that lost its types) are honored.
 *
 * No Witnesses: "Each player who controls the most creatures investigates" — a per-player loop
 * whose body is gated on `PlayerControlsMostPermanents(Player.You, GameObjectFilter.Creature)`,
 * which inside the loop asks about the iterated player. Ties are included, exactly as printed.
 */
@SerialName("PlayerControlsMostPermanents")
@Serializable
data class PlayerControlsMostPermanents(
    val player: Player,
    val filter: GameObjectFilter = GameObjectFilter.Creature,
) : Condition {
    override val description: String =
        "if ${player.description} controls the most ${filter.description} or is tied for the most"

    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * A unary arithmetic property a single number can satisfy, tested by [NumberMatches].
 *
 * Distinct from [ComparisonOperator], which is *binary* (compares two amounts). These are
 * *unary* predicates over one evaluated [DynamicAmount] — "is the count prime / even / odd /
 * a multiple of N" — shapes a threshold comparison can't express. The arithmetic itself lives
 * in the engine's `ConditionEvaluator` (these stay pure data, mirroring how `ComparisonOperator`
 * carries no comparison logic), so this type only names the property and supplies its wording.
 */
@Serializable
sealed interface NumberProperty {
    /** Adjective phrase for condition descriptions, e.g. "prime", "even", "a multiple of 3". */
    val description: String

    /** A prime number (CR has no notion of this — it's a card-defined property; 0 and 1 are not prime). */
    @SerialName("Prime")
    @Serializable
    data object Prime : NumberProperty {
        override val description: String get() = "prime"
    }

    /** An even number (0 is even). */
    @SerialName("Even")
    @Serializable
    data object Even : NumberProperty {
        override val description: String get() = "even"
    }

    /** An odd number. */
    @SerialName("Odd")
    @Serializable
    data object Odd : NumberProperty {
        override val description: String get() = "odd"
    }

    /** A multiple of [divisor] (0 counts as a multiple of every nonzero divisor). */
    @SerialName("MultipleOf")
    @Serializable
    data class MultipleOf(val divisor: Int) : NumberProperty {
        init { require(divisor != 0) { "MultipleOf divisor must be non-zero" } }
        override val description: String get() = "a multiple of $divisor"
    }
}

/**
 * Unary numeric-predicate condition: evaluates [amount] and tests whether the resulting number
 * satisfies [property].
 *
 * The counterpart to [Compare] for properties that aren't a two-sided threshold — primality,
 * parity, divisibility. Like [Compare] it evaluates identically at resolution and under
 * projection, so it can gate either a triggered ability's intervening-if or an "as long as"
 * static ability.
 *
 * Example:
 * ```kotlin
 * // "you control a prime number of lands" (Zimone, All-Questioning)
 * NumberMatches(AggregateBattlefield(Player.You, GameObjectFilter.Land), NumberProperty.Prime)
 * ```
 */
@SerialName("NumberMatches")
@Serializable
data class NumberMatches(
    val amount: DynamicAmount,
    val property: NumberProperty
) : Condition {
    override val description: String = "${amount.description} is ${property.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
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

/** Condition: every player in the game has [threshold] or less life. */
@SerialName("EachPlayerLifeAtMost")
@Serializable
data class EachPlayerLifeAtMost(val threshold: Int) : Condition {
    override val description: String = "each player has $threshold or less life"
}

/** Condition: at least one opponent of the ability's controller has [threshold] or less life. */
@SerialName("AnOpponentLifeAtMost")
@Serializable
data class AnOpponentLifeAtMost(val threshold: Int) : Condition {
    override val description: String = "an opponent has $threshold or less life"
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
