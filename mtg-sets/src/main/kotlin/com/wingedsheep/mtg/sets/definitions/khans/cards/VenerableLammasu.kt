package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Venerable Lammasu
 * {6}{W}
 * Creature — Lammasu
 * 5/4
 * Flying
 */
val VenerableLammasu = card("Venerable Lammasu") {
    manaCost = "{6}{W}"
    typeLine = "Creature — Lammasu"
    power = 5
    toughness = 4
    oracleText = "Flying"

    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "YW Tang"
        flavorText = "Lammasu are the enigmatic travelers of Tarkir, soaring high above all lands in all seasons. None know their true purpose, but they often arrive on the eve of great conflicts or turning points in history."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/229919ef-e39f-4bdc-bcc5-46224a3eb7b4.jpg?1562783617"
    }
}
