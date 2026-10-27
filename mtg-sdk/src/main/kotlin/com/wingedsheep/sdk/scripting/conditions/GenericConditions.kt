package com.wingedsheep.sdk.scripting.conditions

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.TurnTracker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Collection Conditions (pipeline-based)
// =============================================================================

/**
 * Condition: "[subject] is [player]" — two resolved player references name the same player.
 *
 * The default [subject] is the triggering player (the one carried in the trigger context — e.g.
 * the player who lost the game, gained life, cast the spell), which narrows a broad "whenever a
 * player does X" trigger to a specific player without a bespoke event filter: Shinryu,
 * Transcendent Rival gates "When the chosen player loses the game, you win the game" as
 * `triggerRestriction = PlayerIs(player = Player.ChosenOpponent)`. Inside a per-player loop
 * `Player.You` is the visited player, so `Not(PlayerIs(Player.You, Player.DefendingPlayer))` is
 * "each opponent other than defending player" (myriad, CR 702.116a) and
 * `Not(PlayerIs(player = Player.You))` skips the triggering player (Zenith Chronicler). Both
 * sides resolve via the engine's shared player resolver, so any single-player [Player] reference
 * works on either side; an unresolvable side makes the condition false.
 */
@SerialName("PlayerIs")
@Serializable
data class PlayerIs(
    val subject: Player = Player.TriggeringPlayer,
    val player: Player,
) : Condition {
    override val description: String = "if ${subject.description} is ${player.description}"
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
 * [collection], inside both an [com.wingedsheep.sdk.dsl.Effects.If] (for the
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
    override val description: String =
        nthResolutionDescription() ?: lifeGainedDescription() ?: "${left.description} is ${operator.phrase} ${right.description}"

    /** Oracle's own wording for `Conditions.YouGainedLifeThisTurn[AtLeast]` (Infusion). */
    private fun lifeGainedDescription(): String? {
        val tracked = left as? DynamicAmount.TurnTracking
        val n = (right as? DynamicAmount.Fixed)?.amount
        if (tracked?.tracker != TurnTracker.LIFE_GAINED || operator != ComparisonOperator.GTE || n == null || n < 1) {
            return null
        }
        val who = tracked.player.description
        return if (n == 1) "$who gained life this turn" else "$who gained $n or more life this turn"
    }

    /** Oracle's own wording for `Conditions.SourceAbilityResolvedNTimes(n)` (Harvestrite Host). */
    private fun nthResolutionDescription(): String? {
        val property = (left as? DynamicAmount.EntityProperty)?.numericProperty
        val n = (right as? DynamicAmount.Fixed)?.amount
        if (property != EntityNumericProperty.AbilityResolutionsThisTurn || operator != ComparisonOperator.EQ ||
            n == null || n < 1
        ) return null
        val ordinal = when (n) {
            1 -> "first"
            2 -> "second"
            3 -> "third"
            else -> "$n" + if (n % 100 in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
        }
        return "if this is the $ordinal time this ability has resolved this turn"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newLeft = left.applyTextReplacement(replacer)
        val newRight = right.applyTextReplacement(replacer)
        return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
    }
}

/**
 * True when [player] has the most of [amount] among the players still in the game — the
 * "max over every player" comparison a binary [Compare] can't express.
 *
 * [amount] is measured **once per player**, with [Player.You] inside it rebound to the player
 * being measured — the same rebinding [com.wingedsheep.sdk.scripting.values.DynamicAmount.GreatestAmongPlayers]
 * and `ForEachPlayer` do. Write it from that player's own side: `LifeTotal(Player.You)`,
 * `Count(Player.You, Zone.HAND)`, a battlefield count of `Player.You`'s permanents (read from
 * projected state, so animated lands and lost types are honoured).
 *
 * [includeTies] is the axis between the two printed wordings:
 * - `true` — "has the most life **or is tied for** most life": no other player has strictly more.
 *   Preacher of the Schism (`PlayerHasMost(Player.DefendingPlayer, LifeTotal(You))`), No
 *   Witnesses ("each player who controls the most creatures"), The Black Gate's candidate.
 * - `false` — "has more cards in hand **than each other player**": strictly more than every
 *   other player, so a tie for the top fails. Battle Angels of Tyr reads the damaged player's
 *   hand, lands and life this way. Alone in the game, a player trivially has more than each
 *   other player.
 *
 * Write [amount] from the measured player's side only: an outward reference inside it
 * (`Player.EachOpponent`, `Player.TargetPlayer`) measures the same set for every player, so the
 * comparison is meaningless.
 *
 * "Strictly more than each other player" is why this is one condition rather than [Compare] over
 * [com.wingedsheep.sdk.scripting.values.DynamicAmount.GreatestAmongPlayers]: that maximum can't
 * leave the measured player out. The tied form shares the type so both wordings read one way.
 */
@SerialName("PlayerHasMost")
@Serializable
data class PlayerHasMost(
    val player: Player,
    val amount: DynamicAmount,
    val includeTies: Boolean = true,
) : Condition {
    override val description: String = run {
        // "has the most life" / "controls more lands": the printed verb + noun for the amounts the
        // wording takes, falling back to the amount's own phrase for anything else.
        val (verb, noun) = when (amount) {
            DynamicAmount.YourLifeTotal -> "has" to "life"
            is DynamicAmount.LifeTotal -> "has" to "life"
            is DynamicAmount.Count -> when (amount.zone) {
                Zone.BATTLEFIELD -> "controls" to DynamicAmount.pluralize(amount.filter.description)
                else -> "has" to "${DynamicAmount.pluralize(amount.filter.description)} in ${amount.zone.name.lowercase()}"
            }
            is DynamicAmount.AggregateBattlefield ->
                if (amount.aggregation == Aggregation.COUNT && amount.property == null && amount.counterType == null) {
                    "controls" to DynamicAmount.pluralize(amount.filter.description)
                } else "has" to "of ${amount.description}"
            else -> "has" to "of ${amount.description}"
        }
        if (includeTies) "if ${player.description} $verb the most $noun or is tied for the most"
        else "if ${player.description} $verb more $noun than each other player"
    }

    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
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
    override val description: String = youControlDescription() ?: buildString {
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
        append(zone.noun)
    }

    /**
     * A battlefield check on your own permanents reads the way Oracle prints it — "if you control a
     * white permanent", "if you control no other creatures" — rather than "if a permanent is in your
     * battlefield". Null for every other shape, which keeps the generic zone wording.
     */
    private fun youControlDescription(): String? {
        if (zone != Zone.BATTLEFIELD || filter.controllerPredicate != null) return null
        if (player != Player.You && player != Player.ControllerOfSource) return null
        return when {
            negate -> "if you control no " + (if (excludeSelf) "other " else "") +
                filter.permanentNounPhrase(plural = true)
            excludeSelf -> "if you control another " + filter.permanentNounPhrase(plural = false)
            else -> filter.permanentNounPhrase(plural = false).let { noun ->
                "if you control ${if (noun.first().lowercaseChar() in "aeiou") "an" else "a"} $noun"
            }
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Condition {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
