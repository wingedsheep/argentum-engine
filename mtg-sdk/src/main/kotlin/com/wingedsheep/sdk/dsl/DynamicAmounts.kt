package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Convenience factory for common DynamicAmount expressions.
 *
 * These build on the generic Count/CountBattlefield/math primitives
 * without adding to the sealed hierarchy.
 *
 * Usage:
 * ```kotlin
 * DynamicAmounts.creaturesYouControl()
 * DynamicAmounts.landsYouControl()
 * DynamicAmounts.cardsInYourGraveyard()
 * DynamicAmounts.otherCreaturesYouControl()
 * ```
 */
object DynamicAmounts {

    // =========================================================================
    // Battlefield counting
    // =========================================================================

    fun creaturesYouControl(): DynamicAmount =
        DynamicAmount.CountBattlefield(Player.You, GameObjectFilter.Creature)

    fun allCreatures(): DynamicAmount =
        DynamicAmount.CountBattlefield(Player.Each, GameObjectFilter.Creature)

    fun landsYouControl(): DynamicAmount =
        DynamicAmount.CountBattlefield(Player.You, GameObjectFilter.Land)

    fun attackingCreaturesYouControl(): DynamicAmount =
        DynamicAmount.CountBattlefield(Player.You, GameObjectFilter.Creature.attacking())

    fun creaturesWithSubtype(subtype: Subtype): DynamicAmount =
        DynamicAmount.CountBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype(subtype))

    fun landsWithSubtype(subtype: Subtype): DynamicAmount =
        DynamicAmount.CountBattlefield(Player.You, GameObjectFilter.Land.withSubtype(subtype))

    // =========================================================================
    // "Other" counting (subtract 1 for self)
    // =========================================================================

    fun otherCreaturesYouControl(): DynamicAmount =
        DynamicAmount.Subtract(
            DynamicAmount.CountBattlefield(Player.You, GameObjectFilter.Creature),
            DynamicAmount.Fixed(1)
        )

    fun otherCreaturesWithSubtypeYouControl(subtype: Subtype): DynamicAmount =
        DynamicAmount.Subtract(
            DynamicAmount.CountBattlefield(Player.You, GameObjectFilter.Creature.withSubtype(subtype)),
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
        val base = DynamicAmount.CountBattlefield(Player.Opponent, GameObjectFilter.Creature.attacking())
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun landsOfTypeTargetOpponentControls(landType: String, multiplier: Int = 1): DynamicAmount {
        val base = DynamicAmount.CountBattlefield(Player.TargetOpponent, GameObjectFilter.Land.withSubtype(landType))
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun creaturesOfColorTargetOpponentControls(color: Color, multiplier: Int = 1): DynamicAmount {
        val base = DynamicAmount.CountBattlefield(Player.TargetOpponent, GameObjectFilter.Creature.withColor(color))
        return if (multiplier == 1) base else DynamicAmount.Multiply(base, multiplier)
    }

    fun tappedCreaturesTargetOpponentControls(): DynamicAmount =
        DynamicAmount.CountBattlefield(Player.TargetOpponent, GameObjectFilter.Creature.tapped())

    fun handSizeDifferenceFromTargetOpponent(): DynamicAmount =
        DynamicAmount.IfPositive(
            DynamicAmount.Subtract(
                DynamicAmount.Count(Player.TargetOpponent, Zone.HAND),
                DynamicAmount.Count(Player.You, Zone.HAND)
            )
        )
}
