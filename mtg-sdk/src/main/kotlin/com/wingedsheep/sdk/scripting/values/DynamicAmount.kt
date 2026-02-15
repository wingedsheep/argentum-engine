package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Note: Player, Zone, and GameObjectFilter are in the same package (com.wingedsheep.sdk.scripting)

/**
 * Sources for dynamic values in effects.
 */
@Serializable
sealed interface DynamicAmount {
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
     * Fixed amount (for consistency in the type system).
     */
    @SerialName("Fixed")
    @Serializable
    data class Fixed(val amount: Int) : DynamicAmount {
        override val description: String = "$amount"
    }

    /**
     * Count of colors among permanents you control.
     */
    @SerialName("ColorsAmongPermanentsYouControl")
    @Serializable
    data object ColorsAmongPermanentsYouControl : DynamicAmount {
        override val description: String = "the number of colors among permanents you control"
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
     * Reference to a stored variable by name.
     * Used for effects that need to reference a previously computed/stored value.
     * Example: Scapeshift stores "sacrificedCount" and SearchLibrary reads it.
     */
    @SerialName("VariableReference")
    @Serializable
    data class VariableReference(val variableName: String) : DynamicAmount {
        override val description: String = "the stored $variableName"
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
    }

    /**
     * Subtract one dynamic amount from another.
     */
    @SerialName("Subtract")
    @Serializable
    data class Subtract(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} - ${right.description})"
    }

    /**
     * Multiply a dynamic amount by a fixed multiplier.
     * Example: Multiply(CountBattlefield(Player.Opponent, GameObjectFilter.Creature.attacking()), 3)
     */
    @SerialName("Multiply")
    @Serializable
    data class Multiply(val amount: DynamicAmount, val multiplier: Int) : DynamicAmount {
        override val description: String = "$multiplier × ${amount.description}"
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
    }

    /**
     * Maximum of two amounts.
     */
    @SerialName("Max")
    @Serializable
    data class Max(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "max(${left.description}, ${right.description})"
    }

    /**
     * Minimum of two amounts.
     */
    @SerialName("Min")
    @Serializable
    data class Min(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "min(${left.description}, ${right.description})"
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
    }

    // =========================================================================
    // Context-based Values - Values from cost payment or trigger context
    // =========================================================================

    /**
     * Power of a creature that was sacrificed as an additional cost.
     * Used for effects like Final Strike: "Deal damage equal to that creature's power"
     */
    @SerialName("SacrificedPermanentPower")
    @Serializable
    data object SacrificedPermanentPower : DynamicAmount {
        override val description: String = "the sacrificed creature's power"
    }

    /**
     * Toughness of a creature that was sacrificed as an additional cost.
     */
    @SerialName("SacrificedPermanentToughness")
    @Serializable
    data object SacrificedPermanentToughness : DynamicAmount {
        override val description: String = "the sacrificed creature's toughness"
    }

    /**
     * The amount of damage dealt, from a trigger context.
     * Used for abilities like "Whenever ~ is dealt damage, create that many tokens."
     */
    @SerialName("TriggerDamageAmount")
    @Serializable
    data object TriggerDamageAmount : DynamicAmount {
        override val description: String = "the damage dealt"
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
    }

    /**
     * Power of the source entity (the permanent that has the ability).
     * Used for effects like "deal damage equal to its power" on triggered abilities.
     */
    @SerialName("SourcePower")
    @Serializable
    data object SourcePower : DynamicAmount {
        override val description: String = "its power"
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
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : DynamicAmount {
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
     * Count of a specific counter type on the source entity.
     * Used for effects like Riptide Replicator: "where X is the number of charge counters on ~"
     */
    @SerialName("CountersOnSelf")
    @Serializable
    data class CountersOnSelf(val counterType: CounterTypeFilter) : DynamicAmount {
        override val description: String = "the number of ${counterType.description} counters on it"
    }

    /**
     * Count permanents on battlefield matching a unified filter.
     * Convenience wrapper for Count with Zone.BATTLEFIELD.
     *
     * Examples:
     * ```kotlin
     * // Creatures you control
     * CountBattlefield(Player.You, GameObjectFilter.Creature)
     *
     * // All creatures on the battlefield
     * CountBattlefield(Player.Each, GameObjectFilter.Creature)
     *
     * // Attacking creatures you control
     * CountBattlefield(Player.You, GameObjectFilter.Creature.attacking())
     *
     * // Tapped creatures target opponent controls
     * CountBattlefield(Player.TargetOpponent, GameObjectFilter.Creature.tapped())
     * ```
     */
    @SerialName("CountBattlefield")
    @Serializable
    data class CountBattlefield(
        val player: Player,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : DynamicAmount {
        override val description: String = buildString {
            append("the number of ")
            append(pluralize(filter.description))
            append(" ")
            when (player) {
                Player.You -> append("you control")
                Player.Opponent, Player.TargetOpponent -> append("${player.description} controls")
                Player.Each -> append("on the battlefield")
                else -> append("${player.description} controls")
            }
        }
    }
}
