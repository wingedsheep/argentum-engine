package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Snapping Drake
 * {3}{U}
 * Creature - Drake
 * 3/2
 * Flying
 */
val SnappingDrake = card("Snapping Drake") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Drake"
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "67"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c66d8a68-b0e1-4e22-9381-8c1d1a4ad65d.jpg"
    }
}
