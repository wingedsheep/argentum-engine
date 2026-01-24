package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spotted Griffin
 * {3}{W}
 * Creature - Griffin
 * 2/3
 * Flying
 */
val SpottedGriffin = card("Spotted Griffin") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Griffin"
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "William Simpson"
        flavorText = "\"When the cat flies and the bird stalks, guard your midden.\"\n—Goblin saying"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f5b708b-368f-48d2-8eca-40f2ae6d5178.jpg"
    }
}
