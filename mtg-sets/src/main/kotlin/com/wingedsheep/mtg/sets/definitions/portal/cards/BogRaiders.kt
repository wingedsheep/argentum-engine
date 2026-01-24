package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Raiders
 * {2}{B}
 * Creature — Zombie
 * 2/2
 * Swampwalk
 */
val BogRaiders = card("Bog Raiders") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54d5e5c3-456a-4ccf-9d06-3f968b1c5d53.jpg"
    }
}
