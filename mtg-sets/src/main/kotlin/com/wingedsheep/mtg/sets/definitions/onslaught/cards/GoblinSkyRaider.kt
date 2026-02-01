package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Sky Raider
 * {2}{R}
 * Creature — Goblin Warrior
 * 1/2
 * Flying
 */
val GoblinSkyRaider = card("Goblin Sky Raider") {
    manaCost = "{2}{R}"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 2

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "208"
        artist = "Daren Bader"
        flavorText = "The goblin word for 'flying' is more accurately translated as 'falling slowly.'"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/738cbf9b-e3d3-4568-93ce-7915b248e5b3.jpg?1562922311"
    }
}
