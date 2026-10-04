package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Banefire
 * {X}{R}
 * Sorcery
 * Banefire deals X damage to any target.
 * If X is 5 or more, this spell can't be countered and the damage can't be prevented.
 *
 * The two halves of the rider live in two places: "can't be countered" is the card-level
 * [cantBeCounteredIf], read off the spell on the stack whenever something tries to counter it;
 * "the damage can't be prevented" is an [Effects.If] at resolution over the same X test.
 */
val Banefire = card("Banefire") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Banefire deals X damage to any target.\n" +
        "If X is 5 or more, this spell can't be countered and the damage can't be prevented."

    val xAtLeastFive = Conditions.CompareAmounts(DynamicAmounts.xValue(), ComparisonOperator.GTE, 5)
    cantBeCounteredIf = xAtLeastFive

    spell {
        val t = target(Targets.Any)
        effect = Effects.If(
            condition = xAtLeastFive,
            then = Effects.DealDamage(DynamicAmounts.xValue(), t, cantBePrevented = true),
            otherwise = Effects.DealDamage(DynamicAmounts.xValue(), t),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Raymond Swanland"
        flavorText = "For Sarkhan Vol, the dragon is the purest expression of life's savage splendor."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b188c68a-e9df-4803-a722-1993dd88f833.jpg?1783942480"
    }
}
