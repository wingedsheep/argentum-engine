package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mouser Mark III
 * {1}{U/R}
 * Artifact Creature — Robot
 * 2/3
 *
 * This creature can't attack unless you control another artifact.
 */
val MouserMarkIii = card("Mouser Mark III") {
    manaCost = "{1}{U/R}"
    colorIdentity = "UR"
    typeLine = "Artifact Creature — Robot"
    oracleText = "This creature can't attack unless you control another artifact."
    power = 2
    toughness = 3

    staticAbility {
        ability = CantAttackUnless(
            condition = Compare(
                DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Artifact),
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(2)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Gabriel Tanko"
        flavorText = "Designed to get rodents out of small spaces, its Mouser maws can be used to seize all sorts of vermin!"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/608a13b4-0b94-4be5-ac4b-e939901e8419.jpg?1771502784"
    }
}
