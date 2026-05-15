package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Disciple of Grace reprint in ONS.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes
 * only the ONS-specific presentation row.
 */
val DiscipleOfGraceReprint = Printing(
    oracleId = "7f4ceca9-0f55-4fa6-8e64-befbc4303d4e",
    name = "Disciple of Grace",
    setCode = "ONS",
    collectorNumber = "25",
    artist = "Thomas M. Baxa",
    imageUri = "https://cards.scryfall.io/normal/front/1/d/1d1790cb-34e4-4f23-8a13-1906fd9a956f.jpg?1562901832",
    releaseDate = "2002-10-07",
    rarity = Rarity.COMMON,
)
