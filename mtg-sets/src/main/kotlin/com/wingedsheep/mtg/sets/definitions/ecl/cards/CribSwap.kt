package com.wingedsheep.mtg.sets.definitions.ecl.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Crib Swap reprint in ECL.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LRW's `cards/` package (the card's earliest real printing). This file contributes
 * only the ECL-specific presentation row.
 */
val CribSwapReprint = Printing(
    oracleId = "2987c385-011a-4032-a516-a46d1e9dc9e8",
    name = "Crib Swap",
    setCode = "ECL",
    collectorNumber = "11",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/8/f/8f2fb3c6-af75-47a3-9f97-521872c32890.jpg?1767732469",
    releaseDate = "2026-01-23",
    rarity = Rarity.UNCOMMON,
)
