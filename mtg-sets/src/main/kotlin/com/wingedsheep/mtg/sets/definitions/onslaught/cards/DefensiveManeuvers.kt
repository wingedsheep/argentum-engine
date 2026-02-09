package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChooseCreatureTypeModifyStatsEffect

/**
 * Defensive Maneuvers
 * {3}{W}
 * Instant
 * Creatures of the creature type of your choice get +0/+4 until end of turn.
 */
val DefensiveManeuvers = card("Defensive Maneuvers") {
    manaCost = "{3}{W}"
    typeLine = "Instant"

    spell {
        effect = ChooseCreatureTypeModifyStatsEffect(
            powerModifier = 0,
            toughnessModifier = 4
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "23"
        artist = "Luca Zontini"
        flavorText = "\"Only on the battlefield can we repay all the Order has given us.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58f9eb25-4140-4ecf-bcaa-1b193d884007.jpg?1562913615"
    }
}
