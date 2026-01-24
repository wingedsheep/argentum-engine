package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Willow Dryad
 * {G}
 * Creature — Dryad
 * 1/1
 * Forestwalk (This creature can't be blocked as long as defending player controls a Forest.)
 */
val WillowDryad = card("Willow Dryad") {
    manaCost = "{G}"
    typeLine = "Creature — Dryad"
    power = 1
    toughness = 1

    keywords(Keyword.FORESTWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "193"
        artist = "Rebecca Guay"
        flavorText = "She dances among the willows, unseen by mortal eyes."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6d7e8f9-a0b1-4c2d-3e4f-5a6b7c8d9e0f.jpg"
    }
}
