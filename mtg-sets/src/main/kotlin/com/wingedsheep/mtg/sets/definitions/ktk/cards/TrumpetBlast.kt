package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.GroupPatterns

/**
 * Trumpet Blast
 * {2}{R}
 * Instant
 * Attacking creatures get +2/+0 until end of turn.
 */
val TrumpetBlast = card("Trumpet Blast") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Attacking creatures get +2/+0 until end of turn."

    spell {
        effect = GroupPatterns.modifyStatsForAll(
            power = 2,
            toughness = 0,
            filter = GroupFilter.AttackingCreatures
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "124"
        artist = "Steve Prescott"
        flavorText = "\"Do you hear that, Sarkhan? The glory of the horde! I made a legend from what you abandoned.\"\n—Zurgo, khan of the Mardu"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb6deef5-876f-4c81-8af5-6e91e0c4656a.jpg?1562795504"
    }
}
