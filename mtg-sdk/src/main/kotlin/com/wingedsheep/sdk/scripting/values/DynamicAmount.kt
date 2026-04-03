package com.wingedsheep.sdk.scripting.values

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
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
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
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Fixed amount (for consistency in the type system).
     */
    @SerialName("Fixed")
    @Serializable
    data class Fixed(val amount: Int) : DynamicAmount {
        override val description: String = "$amount"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Count of colors among permanents you control.
     */
    @SerialName("ColorsAmongPermanentsYouControl")
    @Serializable
    data object ColorsAmongPermanentsYouControl : DynamicAmount {
        override val description: String = "the number of colors among permanents you control"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    // =========================================================================
    // Graveyard-based DynamicAmounts (for Tarmogoyf, etc.)
    // =========================================================================

    /**
     * Count of card types among cards in all graveyards.
     * Used for Tarmogoyf's characteristic-defining ability.
     */
    @SerialName("CardTypesInAllGraveyards")
    @Serializable
    data object CardTypesInAllGraveyards : DynamicAmount {
        override val description: String = "the number of card types among cards in all graveyards"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Count of distinct card types among cards exiled and linked to the source permanent.
     * Used for Keen-Eyed Curator: "four or more card types among cards exiled with this creature"
     */
    @SerialName("CardTypesInLinkedExile")
    @Serializable
    data object CardTypesInLinkedExile : DynamicAmount {
        override val description: String = "the number of card types among cards exiled with this creature"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
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
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
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
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
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
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
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

    // =========================================================================
    // Context-based Values - Values from cost payment or trigger context
    // =========================================================================

    /**
     * The amount of damage dealt, from a trigger context.
     * Used for abilities like "Whenever ~ is dealt damage, create that many tokens."
     */
    @SerialName("TriggerDamageAmount")
    @Serializable
    data object TriggerDamageAmount : DynamicAmount {
        override val description: String = "the damage dealt"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * The amount of life gained, from a trigger context.
     * Used for abilities like False Cure: "that player loses 2 life for each 1 life gained."
     * Resolves from the same trigger amount pipeline as TriggerDamageAmount.
     */
    @SerialName("TriggerLifeGainAmount")
    @Serializable
    data object TriggerLifeGainAmount : DynamicAmount {
        override val description: String = "the life gained"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * The amount of life lost, from a trigger context.
     * Used for abilities like Vilis, Broker of Blood: "Whenever you lose life, draw that many cards."
     * Resolves from the triggerDamageAmount field in EffectContext (absolute value of life lost).
     */
    @SerialName("TriggerLifeLossAmount")
    @Serializable
    data object TriggerLifeLossAmount : DynamicAmount {
        override val description: String = "the life lost"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Number of cards exiled as an additional cost to cast a spell.
     * Used for effects like Chill Haunting: "Target creature gets -X/-X where X is
     * the number of creature cards exiled as an additional cost."
     */
    @SerialName("AdditionalCostExiledCount")
    @Serializable
    data object AdditionalCostExiledCount : DynamicAmount {
        override val description: String = "the number of cards exiled"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
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
     * The number of +1/+1 counters on the source when it last existed on the battlefield.
     * Used for death triggers that reference the creature's counters (e.g., Hooded Hydra:
     * "create a 1/1 Snake token for each +1/+1 counter on it").
     * Reads from the triggerCounterCount field in EffectContext, which is populated from
     * last-known information captured in the ZoneChangeEvent.
     */
    @SerialName("LastKnownCounterCount")
    @Serializable
    data object LastKnownCounterCount : DynamicAmount {
        override val description: String = "the number of +1/+1 counters on it"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
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
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
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
     * Total damage dealt to a target player this turn.
     * Used for Final Punishment: "Target player loses life equal to the damage
     * already dealt to that player this turn."
     *
     * @param targetIndex Index into the context targets array to find the player
     */
    @SerialName("DamageDealtToTargetPlayerThisTurn")
    @Serializable
    data class DamageDealtToTargetPlayerThisTurn(val targetIndex: Int = 0) : DynamicAmount {
        override val description: String = "the damage already dealt to that player this turn"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Count creatures the controller controls that share a creature type with the triggering entity.
     * Used for Mana Echoes: "add {C} equal to the number of creatures you control that share a creature type with it."
     */
    @SerialName("CreaturesSharingTypeWithTriggeringEntity")
    @Serializable
    data object CreaturesSharingTypeWithTriggeringEntity : DynamicAmount {
        override val description: String = "the number of creatures you control that share a creature type with it"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Count of nontoken creatures put into a player's graveyard from the battlefield this turn.
     * Used for Caller of the Claw: "create a 2/2 green Bear creature token for each nontoken creature
     * put into your graveyard from the battlefield this turn."
     *
     * Reads from NonTokenCreaturesDiedThisTurnComponent on the player entity.
     */
    @SerialName("NonTokenCreaturesDiedThisTurn")
    @Serializable
    data class NonTokenCreaturesDiedThisTurn(val player: Player) : DynamicAmount {
        override val description: String = "the number of nontoken creatures put into ${player.possessive} graveyard from the battlefield this turn"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Count of all creatures (including tokens) that died under a player's control this turn.
     * Used for Season of Loss: "Draw a card for each creature that died under your control this turn."
     *
     * Reads from CreaturesDiedThisTurnComponent on the player entity.
     */
    @SerialName("CreaturesDiedThisTurn")
    @Serializable
    data class CreaturesDiedThisTurn(val player: Player) : DynamicAmount {
        override val description: String = "the number of creatures that died under ${player.possessive} control this turn"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Count of opponents who lost life this turn.
     * Used for Gev, Scaled Scorch: "for each opponent who lost life this turn"
     *
     * Reads from LifeLostThisTurnComponent on each opponent entity.
     */
    @SerialName("OpponentsWhoLostLifeThisTurn")
    @Serializable
    data object OpponentsWhoLostLifeThisTurn : DynamicAmount {
        override val description: String = "the number of opponents who lost life this turn"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Count of creatures that were exiled from opponents' control this turn.
     * Used for Vren, the Relentless.
     *
     * Reads from OpponentCreaturesExiledThisTurnComponent on the player entity.
     */
    @SerialName("OpponentCreaturesExiledThisTurn")
    @Serializable
    data object OpponentCreaturesExiledThisTurn : DynamicAmount {
        override val description: String = "the number of creatures that were exiled under your opponents' control this turn"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }

    /**
     * Count creatures the controller controls that have the creature type chosen by the source permanent.
     * Used for Three Tree City: "Add an amount of mana of that color equal to the number of creatures
     * you control of the chosen type."
     *
     * Reads ChosenCreatureTypeComponent from the source entity.
     */
    @SerialName("CountCreaturesOfSourceChosenType")
    @Serializable
    data object CountCreaturesOfSourceChosenType : DynamicAmount {
        override val description: String = "the number of creatures you control of the chosen type"
        override fun applyTextReplacement(replacer: TextReplacer): DynamicAmount = this
    }
}
