package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wind Drake
 * {2}{U}
 * Creature — Drake
 * 2/2
 * Flying
 */
val WindDrake = card("Wind Drake") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Drake"
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Clyde Caldwell"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad80fab9-d356-4d72-a9b9-a2b1a7f0ab7f.jpg"
    }
}
