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
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30dd236b-94fc-4c56-aeae-215c71a009ea.jpg"
    }
}
