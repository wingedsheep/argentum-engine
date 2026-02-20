package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Convenience factory for common DynamicAmount expressions.
 *
 * These build on the generic AggregateBattlefield/Count/math primitives
 * without adding to the sealed hierarchy.
 *
 * Usage:
 * ```kotlin
 * DynamicAmounts.battlefield(Player.You).count()
 * DynamicAmounts.battlefield(Player.You).maxManaValue()
 * DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
 * DynamicAmounts.creaturesYouControl()
 * DynamicAmounts.landsYouControl()
 * ```
 */
object DynamicAmounts {

    // =========================================================================
    // Fluent battlefield query builder
    // =========================================================================

    /**
     * Start a fluent query over battlefield permanents.
     *
     * ```kotlin
     * DynamicAmounts.battlefield(Player.You).count()
     * DynamicAmounts.battlefield(Player.You).maxManaValue()
     * DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
     * DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).sumPower()
     * ```
     */
    fun battlefield(player: Player, filter: GameObjectFilter = GameObjectFilter.Any) =
        BattlefieldQuery(player, filter)

    class BattlefieldQuery(private val player: Player, private val filter: GameObjectFilter) {
        fun count(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter)

        fun maxManaValue(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.MAX, CardNumericProperty.MANA_VALUE)

        fun maxPower(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.MAX, CardNumericProperty.POWER)

        fun maxToughness(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.MAX, CardNumericProperty.TOUGHNESS)

        fun minToughness(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.MIN, CardNumericProperty.TOUGHNESS)

        fun sumPower(): DynamicAmount =
            DynamicAmount.AggregateBattlefield(player, filter, Aggregation.SUM, CardNumericProperty.POWER)
    }

    // =========================================================================
    // Battlefield counting (convenience shortcuts)
    // =========================================================================

    fun creaturesYouControl(): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Creature).count()

    fun allCreatures(): DynamicAmount =
        battlefield(Player.Each, GameObjectFilter.Creature).count()

    fun landsYouControl(): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Land).count()

    fun attackingCreaturesYouControl(): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Creature.attacking()).count()

    fun creaturesWithSubtype(subtype: Subtype): DynamicAmount =
        battlefield(Player.Each, GameObjectFilter.Creature.withSubtype(subtype)).count()

    fun landsWithSubtype(subtype: Subtype): DynamicAmount =
        battlefield(Player.You, GameObjectFilter.Land.withSubtype(subtype)).count()

    // =========================================================================
    // "Other" counting (subtract 1 for self)
    // =========================================================================

    fun otherCreaturesYouControl(): DynamicAmount =
        DynamicAmount.Subtract(
            battlefield(Player.You, GameObjectFilter.Creature).count(),
            DynamicAmount.Fixed(1)
        )

    fun otherCreaturesWithSubtypeYouControl(subtype: Subtype): DynamicAmount =
        DynamicAmount.Subtract(
            battlefield(Player.You, GameObjectFilter.Creature.withSubtype(subtype)).count(),
            DynamicAmount.Fixed(1)
        )

    // =========================================================================
    // Graveyard counting
    // =========================================================================

    fun cardsInYourGraveyard(): DynamicAmount =
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD)

    fun creatureCardsInYourGraveyard(): DynamicAmount =
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature)

    // =========================================================================
    // Opponent-relative counting
    // =========================================================================

    fun creaturesAttackingYou(multiplier: Int = 1): DynamicAmount {
        val base = battlefield(Player.Opponent, GameObjectFilter.Creature.attacking()).count()
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun landsOfTypeTargetOpponentControls(landType: String, multiplier: Int = 1): DynamicAmount {
        val base = battlefield(Player.TargetOpponent, GameObjectFilter.Land.withSubtype(landType)).count()
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun creaturesOfColorTargetOpponentControls(color: Color, multiplier: Int = 1): DynamicAmount {
        val base = battlefield(Player.TargetOpponent, GameObjectFilter.Creature.withColor(color)).count()
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun tappedCreaturesTargetOpponentControls(): DynamicAmount =
        battlefield(Player.TargetOpponent, GameObjectFilter.Creature.tapped()).count()

    fun handSizeDifferenceFromTargetOpponent(): DynamicAmount =
        DynamicAmount.IfPositive(
            DynamicAmount.Subtract(
                DynamicAmount.Count(Player.TargetOpponent, Zone.HAND),
                DynamicAmount.Count(Player.You, Zone.HAND)
            )
        )
}
