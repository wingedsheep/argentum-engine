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
        imageUri = "https://cards.scryfall.io/normal/front/5/4/5486d2dc-9a5d-4f58-a5ec-d94de54b852f.jpg"
    }
}
