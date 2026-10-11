package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dusk // Dawn reprint in C19.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] lives in Amonkhet (`akh/cards/DuskDawn.kt`).
 * This file contributes only the C19-specific presentation row — set, collector number, art.
 */
val DuskDawnReprint = Printing(
    oracleId = "7bc3f92f-68a2-4934-afc4-89f6d0e8cf98",
    name = "Dusk // Dawn",
    setCode = "C19",
    collectorNumber = "63",
    scryfallId = "e9d5aee0-5963-41db-a22b-cfea40a967a3",
    artist = "Noah Bradley",
    imageUri = "https://cards.scryfall.io/normal/front/e/9/e9d5aee0-5963-41db-a22b-cfea40a967a3.jpg?1783932792",
    releaseDate = "2019-08-23",
    rarity = Rarity.RARE,
)
