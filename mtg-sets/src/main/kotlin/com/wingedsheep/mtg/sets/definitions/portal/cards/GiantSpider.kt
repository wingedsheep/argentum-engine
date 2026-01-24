package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Spider
 * {3}{G}
 * Creature — Spider
 * 2/4
 * Reach
 */
val GiantSpider = card("Giant Spider") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 4

    keywords(Keyword.REACH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Rob Alexander"
        flavorText = "Its web spans the gap between two ancient trees, catching anything foolish enough to fly through."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/058de00f-6ece-46a1-b9a9-84d25e7e0e2e.jpg"
    }
}
