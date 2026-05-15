package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bog Wraith
 * {3}{B}
 * Creature — Wraith
 * 3/3
 * Swampwalk
 */
val BogWraith = card("Bog Wraith") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wraith"
    power = 3
    toughness = 3
    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Jeff A. Menges"
        flavorText = "'Twas in the bogs of Cannelbrae\nMy mate did meet an early grave\n'Twas nothing left for us to save\nIn the peat-filled bogs of Cannelbrae."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6701874e-986e-4b81-9268-90b6171e6187.jpg?1559591359"
    }
}
