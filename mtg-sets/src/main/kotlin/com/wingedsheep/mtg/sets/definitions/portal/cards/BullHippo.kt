package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bull Hippo
 * {3}{G}
 * Creature — Hippo
 * 3/3
 * Islandwalk
 */
val BullHippo = card("Bull Hippo") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Hippo"
    power = 3
    toughness = 3

    keywords(Keyword.ISLANDWALK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Una Fricker"
        flavorText = "The rivers are its domain, and it does not share."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ef0a1b2-c3d4-e5f6-a7b8-c9d0e1f2a3b4.jpg"
    }
}
