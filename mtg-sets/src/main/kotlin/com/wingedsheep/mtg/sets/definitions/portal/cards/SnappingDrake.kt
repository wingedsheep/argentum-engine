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
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b49dd4c-ead6-4d94-9acb-0518d1f6426e.jpg"
    }
}
