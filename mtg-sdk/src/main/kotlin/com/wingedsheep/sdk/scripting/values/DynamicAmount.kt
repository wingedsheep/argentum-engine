package com.wingedsheep.sdk.scripting.values

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Note: Player, Zone, and GameObjectFilter are in the same package (com.wingedsheep.sdk.scripting)

/**
 * Trackable per-player statistics that reset at end of turn.
 *
 * Used by [DynamicAmount.TurnTracking] to read turn-tracking counters from player components.
 */
@Serializable
enum class TurnTracker {
    /** Count of all creatures (including tokens) that died under a player's control this turn. */
    CREATURES_DIED,
    /** Count of nontoken creatures put into a player's graveyard from the battlefield this turn. */
    NONTOKEN_CREATURES_DIED,
    /** Count of creatures exiled from opponents' control this turn. */
    OPPONENT_CREATURES_EXILED,
    /** Count of opponents who lost life this turn. */
    OPPONENTS_WHO_LOST_LIFE,
    /** Total damage received by the player this turn. */
    DAMAGE_RECEIVED,
    /** Total amount of life the player has gained this turn. */
    LIFE_GAINED,
    /**
     * Indicator (0 or 1) that the player has lost life at least once this turn. Backed by the
     * `LifeLostThisTurnComponent` marker — there is no engine-side accumulator for the *amount*
     * lost, so use `Compare(TurnTracking(player, LIFE_LOST), GTE, Fixed(1))` for boolean checks.
     */
    LIFE_LOST,
    /** Indicator (0 or 1) that the player declared at least one attacker this turn. */
    PLAYER_ATTACKED,
    /** Indicator (0 or 1) that the player was dealt combat damage this turn. */
    DEALT_COMBAT_DAMAGE,
    /** Indicator (0 or 1) that the player put one or more counters on a creature this turn. */
    COUNTERS_PUT_ON_CREATURE,
    /** Number of land cards the player played this turn (derived from `LandDropsComponent`). */
    LANDS_PLAYED,
    /**
     * Number of lands that entered the battlefield under the player's control this turn.
     * Unlike [LANDS_PLAYED] this counts *any* land ETB — land drops, Lander-token search,
     * Cultivate-style "put a land onto the battlefield" effects, opponent's
     * gift-a-land effects, etc. — so it matches "a land entered the battlefield under your
     * control this turn" wording (Bioengineered Future).
     */
    LANDS_ENTERED_UNDER_CONTROL,
    /** Indicator (0 or 1) that the player sacrificed at least one Food this turn. */
    FOOD_SACRIFICED,
    /** Total cards that left the player's graveyard this turn (Bonecache Overseer). */
    CARDS_LEFT_GRAVEYARD,
    /**
     * Number of times the player descended this turn (CR 700.11) — count of nontoken
     * permanent cards put into the player's graveyard from any zone. Backs the descend
     * gate and the descend N / fathomless descent ability words.
     */
    DESCENDED;

    fun descriptionFor(player: Player): String = when (this) {
        CREATURES_DIED -> "the number of creatures that died under ${player.possessive} control this turn"
        NONTOKEN_CREATURES_DIED -> "the number of nontoken creatures put into ${player.possessive} graveyard from the battlefield this turn"
        OPPONENT_CREATURES_EXILED -> "the number of creatures that were exiled under your opponents' control this turn"
        OPPONENTS_WHO_LOST_LIFE -> "the number of opponents who lost life this turn"
        DAMAGE_RECEIVED -> "the damage already dealt to that player this turn"
        LIFE_GAINED -> "the amount of life ${player.possessive} gained this turn"
        LIFE_LOST -> "whether ${player.description} lost life this turn"
        PLAYER_ATTACKED -> "whether ${player.description} attacked this turn"
        DEALT_COMBAT_DAMAGE -> "whether ${player.description} were dealt combat damage this turn"
        COUNTERS_PUT_ON_CREATURE -> "whether ${player.description} put a counter on a creature this turn"
        LANDS_PLAYED -> "the number of lands ${player.description} played this turn"
        LANDS_ENTERED_UNDER_CONTROL -> "the number of lands that entered the battlefield under ${player.possessive} control this turn"
        FOOD_SACRIFICED -> "whether ${player.description} sacrificed a Food this turn"
        CARDS_LEFT_GRAVEYARD -> "the number of cards that left ${player.possessive} graveyard this turn"
        DESCENDED -> "the number of times ${player.description} descended this turn"
    }
}

/**
 * Keys for [DynamicAmount.ContextProperty] — values that an effect reads from its
 * resolution context (trigger payload, additional-cost values, target list, linked-exile
 * pile attached to the source) rather than from any fixed entity property.
 *
 * Each key carries its own oracle-text [description] used in card text generation.
 */
@Serializable
enum class ContextPropertyKey(val description: String) {
    /** The amount of damage in the current trigger payload (Tephraderm, Wall of Hope, …). */
    TRIGGER_DAMAGE_AMOUNT("the damage dealt"),
    /**
     * The amount of damage prevented by a prevention shield's `onPrevented` reaction context
     * (New Way Forward, Deflecting Palm) — "that much" / "that many". Shares the trigger-amount
     * slot in [com.wingedsheep.engine.handlers.EffectContext].
     */
    PREVENTED_DAMAGE_AMOUNT("the prevented damage"),
    /** The amount of life gained in the current trigger payload (False Cure, Lich's Mastery). */
    TRIGGER_LIFE_GAINED("the life gained"),
    /** The amount of life lost in the current trigger payload (Lich's Mastery). */
    TRIGGER_LIFE_LOST("the life lost"),
    /** Number of cards exiled as an additional cost (Chill Haunting). */
    ADDITIONAL_COST_EXILED_COUNT("the number of cards exiled"),
    /** X chosen for a `blight X` additional cost (Soul Immolation). */
    ADDITIONAL_COST_BLIGHT_AMOUNT("X"),
    /** Number of (still-legal) targets in the current effect context. */
    TARGET_COUNT("the number of targets"),
    /** Number of +1/+1 counters on the source as it last existed on the battlefield (Hooded Hydra). */
    LAST_KNOWN_PLUS_ONE_COUNTER_COUNT("the number of +1/+1 counters on it"),
    /**
     * Number of counters placed in the triggering [CountersPlacedEvent] payload —
     * used by abilities like Simic Ascendancy: "Whenever one or more +1/+1 counters
     * are put on a creature you control, put **that many** growth counters on this
     * enchantment."
     */
    TRIGGER_COUNTERS_PLACED_AMOUNT("that many"),
    /** Total counters of any kind on the source as it last existed on the battlefield (Shadow Urchin). */
    LAST_KNOWN_TOTAL_COUNTER_COUNT("the number of counters on it"),
    /** Total cards exiled and linked to the source permanent (Veteran Survivor). */
    LINKED_EXILE_CARD_COUNT("the number of cards exiled with this creature"),
    /** Distinct card types among cards exiled and linked to the source permanent (Keen-Eyed Curator). */
    LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT("the number of card types among cards exiled with this creature"),
    /**
     * Number of times a mode was chosen for the modal spell that fired this trigger.
     * Counts mode selections, not distinct modes (Spree-style cards may pick the same
     * mode several times — see Riku of Many Paths: "X is the number of times you chose
     * a mode for that spell, not the number of distinct modes").
     */
    MODES_CHOSEN_ON_TRIGGERING_SPELL("the number of times you chose a mode for that spell"),
    /**
     * Number of cards actually looked at by the scry that fired this trigger. Equals the
     * scry N parameter unless the library held fewer cards. Read by "Whenever you scry,
     * ... for each card looked at" payoffs (Celeborn the Wise, Elrond Master of Healing).
     */
    TRIGGER_SCRY_COUNT("the number of cards looked at"),
    /**
     * Damage in excess of lethal dealt to the creature target in the trigger payload
     * (CR 120.4a). Set from `DamageDealtEvent.excessAmount`; non-zero only when the trigger
     * is a `DealsDamageEvent(requireExcess = true)`. Used by Fall of Cair Andros — "amass
     * Orcs X, where X is the excess damage."
     */
    TRIGGER_EXCESS_DAMAGE_AMOUNT("the excess damage"),
}

/**
 * Sources for dynamic values in effects.
 */
@Serializable
sealed interface DynamicAmount : TextReplaceable<DynamicAmount> {
    val description: String

    companion object {
        /**
         * Pluralize the last word of a filter description for use in counting phrases.
         * Examples: "creature" → "creatures", "land" → "lands", "sorcery" → "sorceries"
         */
        internal fun pluralize(filterDesc: String): String {
            if (filterDesc.isEmpty()) return "cards"
            val words = filterDesc.split(" ")
            val lastWord = words.last()
            val plural = when {
                lastWord.endsWith("s") -> lastWord
                lastWord.endsWith("y") && !lastWord.endsWith("ey") -> lastWord.dropLast(1) + "ies"
                else -> lastWord + "s"
            }
            return (words.dropLast(1) + plural).joinToString(" ")
        }

        /**
         * Strip article from zone displayName for use with possessives.
         * "a graveyard" → "graveyard", "the battlefield" → "battlefield"
         */
        internal fun zoneSimpleName(zone: Zone): String =
            zone.displayName.removePrefix("a ").removePrefix("the ")
    }

    /**
     * Your current life total.
     */
    @SerialName("YourLifeTotal")
    @Serializable
    data object YourLifeTotal : DynamicAmount {
        override val description: String = "your life total"
    }

    /**
     * Life total of a specific player.
     * Generalizes [YourLifeTotal] to support opponent comparisons.
     *
     * Examples:
     * ```kotlin
     * LifeTotal(Player.You)       // your life total
     * LifeTotal(Player.Opponent)  // opponent's life total
     * ```
     */
    @SerialName("LifeTotal")
    @Serializable
    data class LifeTotal(val player: Player) : DynamicAmount {
        override val description: String = "${player.possessive} life total"
    }

    /**
     * The starting life total of a player (e.g., 20 in standard, 40 in commander).
     * Used for conditions like "life total ≤ half your starting life total".
     */
    @SerialName("StartingLifeTotal")
    @Serializable
    data class StartingLifeTotal(val player: Player) : DynamicAmount {
        override val description: String = "${player.possessive} starting life total"
    }

    /**
     * Fixed amount (for consistency in the type system).
     */
    @SerialName("Fixed")
    @Serializable
    data class Fixed(val amount: Int) : DynamicAmount {
        override val description: String = "$amount"
    }

    /**
     * A value pulled from the current resolution context — trigger payload, additional-cost
     * accumulator, target list, or a linked-exile pile attached to the source permanent.
     *
     * The key uniquely identifies which contextual quantity to read; the evaluator dispatches
     * on the key. Replaces the per-context one-off cases (TriggerDamageAmount,
     * TriggerLifeGainAmount, TriggerLifeLossAmount, AdditionalCostExiledCount,
     * AdditionalCostBlightAmount, TargetCount, LastKnownCounterCount,
     * LastKnownTotalCounterCount, CardsInLinkedExile, CardTypesInLinkedExile).
     *
     * Examples:
     * ```kotlin
     * ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)         // Tephraderm
     * ContextProperty(ContextPropertyKey.LAST_KNOWN_PLUS_ONE_COUNTER_COUNT) // Hooded Hydra
     * ```
     */
    @SerialName("ContextProperty")
    @Serializable
    data class ContextProperty(val key: ContextPropertyKey) : DynamicAmount {
        override val description: String = key.description
    }

    // =========================================================================
    // X Value and Variable References
    // =========================================================================

    /**
     * The X value of the spell (from mana cost).
     * Used for X spells like Fireball.
     */
    @SerialName("XValue")
    @Serializable
    data object XValue : DynamicAmount {
        override val description: String = "X"
    }

    /**
     * The total amount of mana paid from the pool to cast the current spell.
     *
     * Sums every per-color and colorless bucket recorded on the spell's stack object.
     * For `{X}` spells the X portion is already included (the mana solver routes those
     * payments through the same buckets), so this is **not** the same as [XValue]:
     *  - `XValue` is the chosen value of X (e.g. 3 for Blaze cast with X=3).
     *  - `TotalManaSpent` is the full mana paid (e.g. 4 for `{X}{R}` Blaze with X=3).
     *
     * Used for effects like Memory Deluge: "where X is the amount of mana spent to cast this spell."
     */
    @SerialName("TotalManaSpent")
    @Serializable
    data object TotalManaSpent : DynamicAmount {
        override val description: String = "the total mana spent to cast this spell"
    }

    /**
     * The amount of mana of a specific [color] that was spent on the `{X}` portion of the
     * current spell or activated ability.
     *
     * Distinct from [TotalManaSpent] (which sums every color across the whole cost): this
     * counts only mana paid toward the variable `{X}` symbols, broken down by color. Used
     * by cards whose payoff scales with how much of a color was spent on X — e.g. Soul Burn
     * ("You gain life equal to the amount of {B} spent on X"). Typically paired with an
     * `xManaRestriction` on the spell/ability so the X portion can only be paid with the
     * relevant colors.
     */
    @SerialName("ManaSpentOnX")
    @Serializable
    data class ManaSpentOnX(val color: Color) : DynamicAmount {
        override val description: String = "the amount of {${color.symbol}} spent on X"
    }

    /**
     * Reference to a stored variable by name.
     * Used for effects that need to reference a previously computed/stored value.
     * Example: Scapeshift stores "sacrificedCount" and SearchLibrary reads it.
     */
    @SerialName("VariableReference")
    @Serializable
    data class VariableReference(val variableName: String) : DynamicAmount {
        override val description: String = "the stored $variableName"
    }

    /**
     * Mana value of a card stored in a named collection.
     * Reads the first card from the stored collection and returns its mana value.
     * Used for effects like Erratic Explosion: "damage equal to that card's mana value".
     */
    @SerialName("StoredCardManaValue")
    @Serializable
    data class StoredCardManaValue(val collectionName: String) : DynamicAmount {
        override val description: String = "the mana value of the $collectionName card"
    }

    /**
     * Number of *distinct* entities across several named pipeline collections.
     *
     * Each listed collection is unioned and de-duplicated by entity id, then the size of the
     * union is returned. Use this for "did you affect N *different* objects" payoffs where the
     * same object may have been selected in more than one pipeline step — e.g. Call the Spirit
     * Dragons puts a +1/+1 counter on a Dragon you control of each color (one selection per
     * color) and wins if five *different* Dragons received counters, even though a multicolored
     * Dragon could have been chosen for two colors.
     */
    @SerialName("DistinctEntitiesInCollections")
    @Serializable
    data class DistinctEntitiesInCollections(val collections: List<String>) : DynamicAmount {
        override val description: String = "the number of distinct selected permanents"
    }

    // =========================================================================
    // Math Operations - Composable arithmetic on DynamicAmounts
    // =========================================================================

    /**
     * Add two dynamic amounts.
     * Example: Add(Fixed(2), CreaturesYouControl) = "2 + creatures you control"
     */
    @SerialName("Add")
    @Serializable
    data class Add(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} + ${right.description})"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newLeft = left.applyTextReplacement(replacer)
            val newRight = right.applyTextReplacement(replacer)
            return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
        }
    }

    /**
     * Subtract one dynamic amount from another.
     */
    @SerialName("Subtract")
    @Serializable
    data class Subtract(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} - ${right.description})"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newLeft = left.applyTextReplacement(replacer)
            val newRight = right.applyTextReplacement(replacer)
            return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
        }
    }

    /**
     * Multiply a dynamic amount by a fixed multiplier.
     * Example: Multiply(AggregateBattlefield(Player.Opponent, GameObjectFilter.Creature.attacking()), 3)
     */
    @SerialName("Multiply")
    @Serializable
    data class Multiply(val amount: DynamicAmount, val multiplier: Int) : DynamicAmount {
        override val description: String = "$multiplier × ${amount.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newAmount = amount.applyTextReplacement(replacer)
            return if (newAmount !== amount) copy(amount = newAmount) else this
        }
    }

    /**
     * Take the maximum of zero and the amount (clamp negative to zero).
     * Useful for difference calculations that should not go negative.
     * Example: IfPositive(Subtract(Count(Player.TargetOpponent, Zone.HAND), Count(Player.You, Zone.HAND)))
     */
    @SerialName("IfPositive")
    @Serializable
    data class IfPositive(val amount: DynamicAmount) : DynamicAmount {
        override val description: String = "${amount.description} (if positive)"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newAmount = amount.applyTextReplacement(replacer)
            return if (newAmount !== amount) copy(amount = newAmount) else this
        }
    }

    /**
     * Maximum of two amounts.
     */
    @SerialName("Max")
    @Serializable
    data class Max(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "max(${left.description}, ${right.description})"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newLeft = left.applyTextReplacement(replacer)
            val newRight = right.applyTextReplacement(replacer)
            return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
        }
    }

    /**
     * Minimum of two amounts.
     */
    @SerialName("Min")
    @Serializable
    data class Min(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "min(${left.description}, ${right.description})"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newLeft = left.applyTextReplacement(replacer)
            val newRight = right.applyTextReplacement(replacer)
            return if (newLeft !== left || newRight !== right) copy(left = newLeft, right = newRight) else this
        }
    }

    /**
     * Conditional amount: evaluates to one of two amounts based on a condition.
     * Example: "2 if enchanted creature is a Wizard, otherwise 1"
     */
    @SerialName("Conditional")
    @Serializable
    data class Conditional(
        val condition: Condition,
        val ifTrue: DynamicAmount,
        val ifFalse: DynamicAmount
    ) : DynamicAmount {
        override val description: String = "${ifTrue.description} ${condition.description}, otherwise ${ifFalse.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newCondition = (condition as? TextReplaceable<*>)?.applyTextReplacement(replacer) as? Condition ?: condition
            val newIfTrue = ifTrue.applyTextReplacement(replacer)
            val newIfFalse = ifFalse.applyTextReplacement(replacer)
            return if (newCondition !== condition || newIfTrue !== ifTrue || newIfFalse !== ifFalse)
                copy(condition = newCondition, ifTrue = newIfTrue, ifFalse = newIfFalse) else this
        }
    }

    /**
     * Count of players in [scope] for whom [condition] evaluates to true.
     *
     * The condition is evaluated with the context's controllerId rebound to each candidate
     * player in turn, so `Player.You` inside the condition refers to the player being tested.
     * Used for effects like "draw an additional card for each opponent who has one or fewer
     * cards in hand" (Bandit's Talent, level 3).
     */
    @SerialName("CountPlayersWith")
    @Serializable
    data class CountPlayersWith(
        val scope: Player,
        val condition: Condition
    ) : DynamicAmount {
        override val description: String = "the number of ${scope.description} for whom ${condition.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newCondition = (condition as? TextReplaceable<*>)?.applyTextReplacement(replacer) as? Condition ?: condition
            return if (newCondition !== condition) copy(condition = newCondition) else this
        }
    }

    // =========================================================================
    // Zone-based Counting — generic counting primitives
    // =========================================================================

    /**
     * Count game objects in a zone matching a unified filter.
     * This is the preferred counting primitive using the new unified filter system.
     *
     * Examples:
     * ```kotlin
     * // Cards in your graveyard
     * Count(Player.You, Zone.GRAVEYARD)
     *
     * // Creature cards in your graveyard
     * Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)
     *
     * // Cards in target opponent's hand
     * Count(Player.TargetOpponent, Zone.HAND)
     * ```
     *
     * @param player Whose zone to count in
     * @param zone Which zone to count
     * @param filter Filter for what to count (default: any)
     */
    @SerialName("Count")
    @Serializable
    data class Count(
        val player: Player,
        val zone: Zone,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
        override val description: String = buildString {
            append("the number of ")
            append(pluralize(filter.description))
            append(" ")
            when (zone) {
                Zone.BATTLEFIELD -> {
                    when (player) {
                        Player.You -> append("you control")
                        Player.Opponent, Player.TargetOpponent -> append("${player.description} controls")
                        Player.Each -> append("on the battlefield")
                        else -> append("${player.description} controls")
                    }
                }
                else -> {
                    append("in ")
                    append(player.possessive)
                    append(" ")
                    append(zoneSimpleName(zone))
                }
            }
        }
    }

    /**
     * Generic battlefield aggregation primitive.
     * Queries permanents on the battlefield, filters them, optionally maps to a numeric
     * property, and applies an aggregation function.
     *
     * This unifies counting, max, min, and sum operations over battlefield entities:
     *
     * ```kotlin
     * // Count creatures you control
     * AggregateBattlefield(Player.You, GameObjectFilter.Creature)
     *
     * // Greatest mana value among permanents you control (Rush of Knowledge)
     * AggregateBattlefield(Player.You, aggregation = Aggregation.MAX, property = CardNumericProperty.MANA_VALUE)
     *
     * // Greatest power among creatures you control
     * AggregateBattlefield(Player.You, GameObjectFilter.Creature, Aggregation.MAX, CardNumericProperty.POWER)
     * ```
     *
     * Prefer using the fluent DSL via [DynamicAmounts.battlefield] rather than constructing directly.
     *
     * @param player Whose permanents to query
     * @param filter Filter for which permanents to include
     * @param aggregation How to aggregate (COUNT, MAX, MIN, SUM)
     * @param property Which numeric property to aggregate (ignored for COUNT)
     */
    @SerialName("AggregateBattlefield")
    @Serializable
    data class AggregateBattlefield(
        val player: Player,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val aggregation: Aggregation = Aggregation.COUNT,
        val property: CardNumericProperty? = null,
        val excludeSelf: Boolean = false
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
        override val description: String = buildString {
            when (aggregation) {
                Aggregation.COUNT -> {
                    append("the number of ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.MAX -> {
                    append("the greatest ${property?.description ?: "value"} among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.MIN -> {
                    append("the least ${property?.description ?: "value"} among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.SUM -> {
                    append("the total ${property?.description ?: "value"} of ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_TYPES -> {
                    append("the number of card types among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COLORS -> {
                    append("the number of colors among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_NAMES -> {
                    append("the number of differently named ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_BASIC_LAND_SUBTYPES -> {
                    append("the number of basic land types among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COUNTER_TYPES -> {
                    append("the number of different kinds of counters among ")
                    if (excludeSelf) append("other ")
                    append(pluralize(filter.description))
                }
            }
            append(" ")
            when (player) {
                Player.You -> append("you control")
                Player.Opponent, Player.TargetOpponent -> append("${player.description} controls")
                Player.Each -> append("on the battlefield")
                else -> append("${player.description} controls")
            }
        }
    }

    /**
     * Generic zone aggregation primitive.
     * Queries cards in a player's zone, filters them, optionally maps to a numeric
     * property, and applies an aggregation function.
     *
     * This is the zone-generic equivalent of [AggregateBattlefield], for non-battlefield zones
     * like graveyard, hand, library, and exile.
     *
     * ```kotlin
     * // Greatest mana value among cards in your graveyard (Wick's Patrol)
     * AggregateZone(Player.You, Zone.GRAVEYARD, aggregation = Aggregation.MAX, property = CardNumericProperty.MANA_VALUE)
     *
     * // Count creature cards in your graveyard
     * AggregateZone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)
     * ```
     *
     * Prefer using the fluent DSL via [DynamicAmounts.zone] rather than constructing directly.
     *
     * @param player Whose zone to query
     * @param zone Which zone to query (should not be BATTLEFIELD — use AggregateBattlefield for that)
     * @param filter Filter for which cards to include
     * @param aggregation How to aggregate (COUNT, MAX, MIN, SUM)
     * @param property Which numeric property to aggregate (ignored for COUNT)
     */
    @SerialName("AggregateZone")
    @Serializable
    data class AggregateZone(
        val player: Player,
        val zone: Zone,
        val filter: GameObjectFilter = GameObjectFilter.Companion.Any,
        val aggregation: Aggregation = Aggregation.COUNT,
        val property: CardNumericProperty? = null
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
        override val description: String = buildString {
            when (aggregation) {
                Aggregation.COUNT -> {
                    append("the number of ")
                    append(pluralize(filter.description))
                }
                Aggregation.MAX -> {
                    append("the greatest ${property?.description ?: "value"} among ")
                    append(pluralize(filter.description))
                }
                Aggregation.MIN -> {
                    append("the least ${property?.description ?: "value"} among ")
                    append(pluralize(filter.description))
                }
                Aggregation.SUM -> {
                    append("the total ${property?.description ?: "value"} of ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_TYPES -> {
                    append("the number of card types among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COLORS -> {
                    append("the number of colors among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_NAMES -> {
                    append("the number of differently named ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_BASIC_LAND_SUBTYPES -> {
                    append("the number of basic land types among ")
                    append(pluralize(filter.description))
                }
                Aggregation.DISTINCT_COUNTER_TYPES -> {
                    append("the number of different kinds of counters among ")
                    append(pluralize(filter.description))
                }
            }
            append(" in ")
            append(player.possessive)
            append(" ")
            append(zoneSimpleName(zone))
        }
    }

    // =========================================================================
    // Entity Property — unified access to any entity's numeric property
    // =========================================================================

    /**
     * Read a numeric property from a referenced entity.
     * This is the unified replacement for TargetPower, TargetManaValue, CountersOnTarget,
     * SourcePower, SacrificedPermanentPower, CountersOnSelf, etc.
     *
     * Examples:
     * ```kotlin
     * EntityProperty(EntityReference.Source, EntityNumericProperty.Power)        // SourcePower
     * EntityProperty(EntityReference.Target(0), EntityNumericProperty.ManaValue) // TargetManaValue
     * EntityProperty(EntityReference.Sacrificed(), EntityNumericProperty.Power)  // SacrificedPermanentPower
     * EntityProperty(EntityReference.Source, EntityNumericProperty.CounterCount(CounterTypeFilter.PlusOnePlusOne)) // CountersOnSelf
     * ```
     */
    @SerialName("EntityProperty")
    @Serializable
    data class EntityProperty(
        val entity: EntityReference,
        val numericProperty: EntityNumericProperty
    ) : DynamicAmount {
        override val description: String = "${entity.description}'s ${numericProperty.description}"
    }

    // =========================================================================
    // Division
    // =========================================================================

    /**
     * Divide one dynamic amount by another, with configurable rounding.
     * Used for "lose half your life, rounded up" (Divide(LifeTotal, Fixed(2), roundUp=true)).
     */
    @SerialName("Divide")
    @Serializable
    data class Divide(
        val numerator: DynamicAmount,
        val denominator: DynamicAmount,
        val roundUp: Boolean = true
    ) : DynamicAmount {
        override val description: String = "${numerator.description} / ${denominator.description}"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newNumerator = numerator.applyTextReplacement(replacer)
            val newDenominator = denominator.applyTextReplacement(replacer)
            return if (newNumerator !== numerator || newDenominator !== denominator)
                copy(numerator = newNumerator, denominator = newDenominator) else this
        }
    }

    /**
     * Reads a per-player turn-tracking counter.
     *
     * Unifies CreaturesDiedThisTurn, NonTokenCreaturesDiedThisTurn, OpponentCreaturesExiledThisTurn,
     * OpponentsWhoLostLifeThisTurn, and DamageDealtToTargetPlayerThisTurn into a single parameterized variant.
     *
     * @param player Which player(s) to read the counter from. Use [Player.ContextPlayer] with a target
     *   index when the player comes from a targeting context (e.g., "damage dealt to target player").
     * @param tracker Which turn-tracking stat to read
     */
    @SerialName("TurnTracking")
    @Serializable
    data class TurnTracking(val player: Player, val tracker: TurnTracker) : DynamicAmount {
        override val description: String = tracker.descriptionFor(player)
    }

    /**
     * Counts the spells a player has cast this turn, optionally filtered and optionally
     * excluding the currently-resolving spell itself.
     *
     * Reads the per-player `spellsCastThisTurnByPlayer` history (a list of [CastSpellRecord]
     * snapshots taken at cast time), so it counts spells regardless of where they are now —
     * the spell that triggered an ability is already recorded and counts unless [excludeSelf].
     *
     * - [filter] narrows to a spell characteristic captured at cast time (type, color, mana
     *   value). Face-down (Morph/Disguise) casts never match a non-empty filter. With
     *   [GameObjectFilter.Any] every cast counts.
     * - [excludeSelf] drops the resolving spell's own record (matched by its stack entity id
     *   against the evaluation context's source), for "the number of *other* spells you've
     *   cast this turn". It only has an effect when the source is itself the resolving spell.
     *
     * ```kotlin
     * // Thunder Salvo: "2 plus the number of other spells you've cast this turn"
     * DynamicAmount.Add(DynamicAmount.Fixed(2),
     *     DynamicAmount.SpellsCastThisTurn(Player.You, excludeSelf = true))
     *
     * // Magebane Lizard: "the number of noncreature spells they've cast this turn"
     * DynamicAmount.SpellsCastThisTurn(Player.TriggeringPlayer, GameObjectFilter.Noncreature)
     * ```
     *
     * @param player Whose cast history to count (summed when the ref resolves to several players)
     * @param filter Spell characteristics to match (default [GameObjectFilter.Any])
     * @param excludeSelf Exclude the resolving spell's own cast record (default false)
     */
    @SerialName("SpellsCastThisTurn")
    @Serializable
    data class SpellsCastThisTurn(
        val player: Player = Player.You,
        val filter: GameObjectFilter = GameObjectFilter.Any,
        val excludeSelf: Boolean = false
    ) : DynamicAmount {
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
        override val description: String = buildString {
            append("the number of ")
            if (excludeSelf) append("other ")
            if (filter == GameObjectFilter.Any) append("spells")
            else append("${filter.description} spells")
            append(" ")
            when (player) {
                Player.You -> append("you've cast")
                else -> append("${player.description} has cast")
            }
            append(" this turn")
        }
    }

    /**
     * Total printed power of the cards exiled to craft the source permanent (CR 702.167c).
     *
     * Reads the source's
     * `com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent` —
     * attached on battlefield entry by
     * [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect] when the
     * Craft activated ability resolves — and sums the printed power of those exiled cards.
     * Used by the Mastercraft Raptor back face of Saheeli's Lattice.
     *
     * Evaluates to zero when the source was not crafted (no component) — the back face has
     * no other way of legitimately existing, but a benign zero keeps the projector total order.
     */
    @SerialName("CraftedMaterialsTotalPower")
    @Serializable
    data object CraftedMaterialsTotalPower : DynamicAmount {
        override val description: String = "the total power of the exiled cards used to craft it"
    }

}
