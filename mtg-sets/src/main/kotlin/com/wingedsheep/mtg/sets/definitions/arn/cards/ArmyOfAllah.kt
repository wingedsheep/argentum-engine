package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Army of Allah
 * {1}{W}{W}
 * Instant
 * Attacking creatures get +2/+0 until end of turn.
 */
val ArmyOfAllah = card("Army of Allah") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Attacking creatures get +2/+0 until end of turn."

    spell {
        effect = GroupPatterns.modifyStatsForAll(
            power = 2,
            toughness = 0,
            filter = GroupFilter.AttackingCreatures,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Brian Snõddy"
        flavorText = "On the day of victory no one is tired. —Arab proverb"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d170015-b125-49a6-a15e-8fd116bbcb14.jpg?1562906251"
    }
}
