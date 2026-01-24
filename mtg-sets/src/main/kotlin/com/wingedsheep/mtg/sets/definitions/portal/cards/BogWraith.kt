package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
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
    typeLine = "Creature — Wraith"
    power = 3
    toughness = 3
    keywords(Keyword.SWAMPWALK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e8e4194-d8a3-4dd3-a7ab-ae2c5c6cc0e2.jpg"
    }
}
