package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.Player

/**
 * Stag Beetle
 * {3}{G}{G}
 * Creature — Insect
 * 0/0
 * Stag Beetle enters the battlefield with X +1/+1 counters on it,
 * where X is the number of other creatures on the battlefield.
 */
val StagBeetle = card("Stag Beetle") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Insect"
    power = 0
    toughness = 0
    oracleText = "Stag Beetle enters the battlefield with X +1/+1 counters on it, where X is the number of other creatures on the battlefield."

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature)
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "285"
        artist = "Anthony S. Waters"
        flavorText = "Its voice crackles with the hum of a thousand wings beating at once."
        imageUri = "https://cards.scryfall.io/large/front/7/2/72cc64b9-f5b9-42d3-9921-564c4c9f2c77.jpg?1562922134"
    }
}
