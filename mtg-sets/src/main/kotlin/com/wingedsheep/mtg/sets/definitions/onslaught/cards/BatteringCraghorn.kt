package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Battering Craghorn
 * {2}{R}{R}
 * Creature — Goat Beast
 * 3/1
 * First strike
 * Morph {1}{R}{R} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)
 *
 * Note: Morph ability not yet implemented.
 */
val BatteringCraghorn = card("Battering Craghorn") {
    manaCost = "{2}{R}{R}"
    typeLine = "Creature — Goat Beast"
    power = 3
    toughness = 1

    keywords(Keyword.FIRST_STRIKE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Edward P. Beard, Jr."
        flavorText = "Tread silently: Even the clatter of a pebble can trigger a craghorn stampede."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ef71f42-87e5-4b1d-aac1-3752b81cee7c.jpg"
    }
}
