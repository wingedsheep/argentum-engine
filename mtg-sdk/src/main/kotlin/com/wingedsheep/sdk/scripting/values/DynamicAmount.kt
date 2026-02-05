package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.Serializable

// Note: Player, Zone, and GameObjectFilter are in the same package (com.wingedsheep.sdk.scripting)

/**
 * Sources for dynamic values in effects.
 */
@Serializable
sealed interface DynamicAmount {
    val description: String

    /**
     * Count of other creatures you control.
     */
    @Serializable
    data object OtherCreaturesYouControl : DynamicAmount {
        override val description: String = "the number of other creatures you control"
    }

    /**
     * Count of other creatures with a specific subtype you control.
     * "the number of other Goblins you control"
     */
    @Serializable
    data class OtherCreaturesWithSubtypeYouControl(val subtype: Subtype) : DynamicAmount {
        override val description: String = "the number of other ${subtype.value}s you control"
    }

    /**
     * Count of creatures you control (including self).
     */
    @Serializable
    data object CreaturesYouControl : DynamicAmount {
        override val description: String = "the number of creatures you control"
    }

    /**
     * Count of all creatures on the battlefield.
     */
    @Serializable
    data object AllCreatures : DynamicAmount {
        override val description: String = "the number of creatures on the battlefield"
    }

    /**
     * Count of all creatures with a specific subtype on the battlefield.
     * Used for tribal effects like Wellwisher ("for each Elf on the battlefield").
     */
    @Serializable
    data class CreaturesWithSubtypeOnBattlefield(val subtype: Subtype) : DynamicAmount {
        override val description: String = "the number of ${subtype.value}s on the battlefield"
    }

    /**
     * Your current life total.
     */
    @Serializable
    data object YourLifeTotal : DynamicAmount {
        override val description: String = "your life total"
    }

    /**
     * Fixed amount (for consistency in the type system).
     */
    @Serializable
    data class Fixed(val amount: Int) : DynamicAmount {
        override val description: String = "$amount"
    }

    /**
     * Count of creatures that entered the battlefield under your control this turn.
     */
    @Serializable
    data object CreaturesEnteredThisTurn : DynamicAmount {
        override val description: String = "the number of creatures that entered the battlefield under your control this turn"
    }

    /**
     * Count of attacking creatures you control.
     */
    @Serializable
    data object AttackingCreaturesYouControl : DynamicAmount {
        override val description: String = "the number of attacking creatures you control"
    }

    /**
     * Count of colors among permanents you control.
     */
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
    @Serializable
    data object CardTypesInAllGraveyards : DynamicAmount {
        override val description: String = "the number of card types among cards in all graveyards"
    }

    /**
     * Count of cards in your graveyard.
     * Used for creatures like Lhurgoyf.
     */
    @Serializable
    data object CardsInYourGraveyard : DynamicAmount {
        override val description: String = "the number of cards in your graveyard"
    }

    /**
     * Count of creature cards in your graveyard.
     */
    @Serializable
    data object CreatureCardsInYourGraveyard : DynamicAmount {
        override val description: String = "the number of creature cards in your graveyard"
    }

    /**
     * Count of lands you control.
     */
    @Serializable
    data object LandsYouControl : DynamicAmount {
        override val description: String = "the number of lands you control"
    }

    /**
     * Count of lands with a specific subtype you control.
     * "the number of Mountains you control"
     */
    @Serializable
    data class LandsWithSubtypeYouControl(val subtype: Subtype) : DynamicAmount {
        override val description: String = "the number of ${subtype.value}s you control"
    }

    // =========================================================================
    // X Value and Variable References
    // =========================================================================

    /**
     * The X value of the spell (from mana cost).
     * Used for X spells like Fireball.
     */
    @Serializable
    data object XValue : DynamicAmount {
        override val description: String = "X"
    }

    /**
     * Reference to a stored variable by name.
     * Used for effects that need to reference a previously computed/stored value.
     * Example: Scapeshift stores "sacrificedCount" and SearchLibrary reads it.
     */
    @Serializable
    data class VariableReference(val variableName: String) : DynamicAmount {
        override val description: String = "the stored $variableName"
    }

    // =========================================================================
    // Opponent-relative DynamicAmounts
    // =========================================================================

    /**
     * Count of creatures attacking you, optionally with a multiplier.
     * Used for effects like Blessed Reversal ("gain 3 life for each creature attacking you").
     */
    @Serializable
    data class CreaturesAttackingYou(val multiplier: Int = 1) : DynamicAmount {
        override val description: String = if (multiplier == 1) {
            "the number of creatures attacking you"
        } else {
            "$multiplier for each creature attacking you"
        }
    }

    /**
     * Count of lands of a specific type target opponent controls.
     * Used for color-hosers like Renewing Dawn.
     */
    @Serializable
    data class LandsOfTypeTargetOpponentControls(
        val landType: String,
        val multiplier: Int = 1
    ) : DynamicAmount {
        override val description: String = if (multiplier == 1) {
            "the number of ${landType}s target opponent controls"
        } else {
            "$multiplier for each $landType target opponent controls"
        }
    }

    /**
     * Count of creatures of a specific color target opponent controls.
     * Used for color-hosers like Starlight.
     */
    @Serializable
    data class CreaturesOfColorTargetOpponentControls(
        val color: Color,
        val multiplier: Int = 1
    ) : DynamicAmount {
        override val description: String = if (multiplier == 1) {
            "the number of ${color.displayName.lowercase()} creatures target opponent controls"
        } else {
            "$multiplier for each ${color.displayName.lowercase()} creature target opponent controls"
        }
    }

    /**
     * Difference in hand sizes between target opponent and you (if positive).
     * Used for effects like Balance of Power.
     */
    @Serializable
    data object HandSizeDifferenceFromTargetOpponent : DynamicAmount {
        override val description: String = "the difference if target opponent has more cards in hand than you"
    }

    /**
     * Number of tapped creatures target opponent controls.
     * Used for Theft of Dreams.
     * @deprecated Use CountInZone with appropriate filter instead
     */
    @Serializable
    data object TappedCreaturesTargetOpponentControls : DynamicAmount {
        override val description: String = "the number of tapped creatures target opponent controls"
    }

    // =========================================================================
    // Math Operations - Composable arithmetic on DynamicAmounts
    // =========================================================================

    /**
     * Add two dynamic amounts.
     * Example: Add(Fixed(2), CreaturesYouControl) = "2 + creatures you control"
     */
    @Serializable
    data class Add(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} + ${right.description})"
    }

    /**
     * Subtract one dynamic amount from another.
     * Example: Subtract(CardsInHand(Opponent), CardsInHand(Controller))
     * Replaces HandSizeDifferenceFromTargetOpponent
     */
    @Serializable
    data class Subtract(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "(${left.description} - ${right.description})"
    }

    /**
     * Multiply a dynamic amount by a fixed multiplier.
     * Example: Multiply(CreaturesAttackingYou, 3) for Blessed Reversal
     */
    @Serializable
    data class Multiply(val amount: DynamicAmount, val multiplier: Int) : DynamicAmount {
        override val description: String = "$multiplier × ${amount.description}"
    }

    /**
     * Take the maximum of zero and the amount (clamp negative to zero).
     * Useful for difference calculations that should not go negative.
     * Example: IfPositive(Subtract(opponentHand, yourHand))
     */
    @Serializable
    data class IfPositive(val amount: DynamicAmount) : DynamicAmount {
        override val description: String = "${amount.description} (if positive)"
    }

    /**
     * Maximum of two amounts.
     */
    @Serializable
    data class Max(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "max(${left.description}, ${right.description})"
    }

    /**
     * Minimum of two amounts.
     */
    @Serializable
    data class Min(val left: DynamicAmount, val right: DynamicAmount) : DynamicAmount {
        override val description: String = "min(${left.description}, ${right.description})"
    }

    // =========================================================================
    // Context-based Values - Values from cost payment or trigger context
    // =========================================================================

    /**
     * Power of a creature that was sacrificed as an additional cost.
     * Used for effects like Final Strike: "Deal damage equal to that creature's power"
     */
    @Serializable
    data object SacrificedPermanentPower : DynamicAmount {
        override val description: String = "the sacrificed creature's power"
    }

    /**
     * Toughness of a creature that was sacrificed as an additional cost.
     */
    @Serializable
    data object SacrificedPermanentToughness : DynamicAmount {
        override val description: String = "the sacrificed creature's toughness"
    }

    // =========================================================================
    // Zone-based Counting
    // =========================================================================

    /**
     * Count game objects in a zone matching a unified filter.
     * This is the preferred counting primitive using the new unified filter system.
     *
     * Examples:
     * ```kotlin
     * // Cards in your hand
     * Count(Player.You, Zone.Hand)
     *
     * // Forests on the battlefield (any player)
     * Count(Player.Each, Zone.Battlefield, GameObjectFilter.Land.withSubtype("Forest"))
     *
     * // Tapped creatures opponent controls
     * Count(Player.Opponent, Zone.Battlefield, GameObjectFilter.Creature.tapped())
     *
     * // Green cards in target opponent's hand
     * Count(Player.TargetOpponent, Zone.Hand, GameObjectFilter.Any.withColor(Color.GREEN))
     * ```
     *
     * @param player Whose zone to count in
     * @param zone Which zone to count
     * @param filter Filter for what to count (default: any)
     */
    @Serializable
    data class Count(
        val player: Player,
        val zone: Zone,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : DynamicAmount {
        override val description: String = buildString {
            append("the number of ")
            val filterDesc = filter.description
            if (filterDesc.isNotEmpty()) {
                append(filterDesc)
                append(" ")
            }
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
                    append(player.description)
                    append("'s ")
                    append(zone.displayName)
                }
            }
        }
    }

    /**
     * Count permanents on battlefield matching a unified filter.
     * Convenience wrapper for Count with Zone.Battlefield.
     *
     * Examples:
     * ```kotlin
     * // Creatures you control
     * CountBattlefield(Player.You, GameObjectFilter.Creature)
     *
     * // All Forests
     * CountBattlefield(Player.Each, GameObjectFilter.Land.withSubtype("Forest"))
     *
     * // Attacking creatures you control
     * CountBattlefield(Player.You, GameObjectFilter.Creature.attacking())
     * ```
     */
    @Serializable
    data class CountBattlefield(
        val player: Player,
        val filter: GameObjectFilter = GameObjectFilter.Any
    ) : DynamicAmount {
        override val description: String = buildString {
            append("the number of ")
            val filterDesc = filter.description
            if (filterDesc.isNotEmpty()) {
                append(filterDesc)
                append(" ")
            }
            when (player) {
                Player.You -> append("you control")
                Player.Opponent, Player.TargetOpponent -> append("${player.description} controls")
                Player.Each -> append("on the battlefield")
                else -> append("${player.description} controls")
            }
        }
    }
}
