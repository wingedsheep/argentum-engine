package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ooze Flux
 * {3}{G}
 * Enchantment
 *
 * {1}{G}, Remove one or more +1/+1 counters from among creatures you control: Create an X/X green
 * Ooze creature token, where X is the number of +1/+1 counters removed this way.
 *
 * Retribution of the Ancients' cost shape: X is announced with the activation and the removal is
 * distributed among creatures you control, totalling exactly X, so "the number removed this way"
 * is the ability's X. "One or more" is `minimumXValue = 1`.
 */
val OozeFlux = card("Ooze Flux") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "{1}{G}, Remove one or more +1/+1 counters from among creatures you control: Create an X/X " +
        "green Ooze creature token, where X is the number of +1/+1 counters removed this way."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}"),
            Costs.RemoveXCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                filter = GameObjectFilter.Creature
            )
        )
        minimumXValue = 1
        effect = Effects.CreateDynamicToken(
            dynamicPower = DynamicAmounts.xValue(),
            dynamicToughness = DynamicAmounts.xValue(),
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Ooze"),
            imageUri = "https://cards.scryfall.io/normal/front/d/1/d1821a53-1799-4162-b0b6-f8ab84a55c30.jpg?1783909274",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9554369-7961-4cea-9da0-a1235805a26a.jpg?1783940116"
    }
}
