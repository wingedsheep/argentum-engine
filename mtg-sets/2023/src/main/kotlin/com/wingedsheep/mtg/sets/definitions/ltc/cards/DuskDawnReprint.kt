package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dusk // Dawn reprint in LTC.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in Amonkhet (`akh/cards/DuskDawn.kt`).
 * This file contributes only the LTC-specific presentation row — set, collector number, art.
 */
val DuskDawnReprint = Printing(
    oracleId = "7bc3f92f-68a2-4934-afc4-89f6d0e8cf98",
    name = "Dusk // Dawn",
    setCode = "LTC",
    collectorNumber = "166",
    scryfallId = "13aab303-8693-4d67-87af-09188a3f617c",
    artist = "Samuele Bandini",
    imageUri = "https://cards.scryfall.io/normal/front/1/3/13aab303-8693-4d67-87af-09188a3f617c.jpg?1783915965",
    releaseDate = "2023-06-23",
    rarity = Rarity.RARE,
)
