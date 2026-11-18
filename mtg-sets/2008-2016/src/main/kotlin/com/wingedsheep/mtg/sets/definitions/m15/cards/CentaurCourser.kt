package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Centaur Courser reprint in M15.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (types, P/T) lives in
 * M10's `cards/` package (the card's earliest real printing). This file
 * contributes only the M15-specific presentation row — set, collector number, art —
 * picked up automatically by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CentaurCourserReprint = Printing(
    oracleId = "2f5bf099-2e01-4e1c-9ebf-0ce0ac66939e",
    name = "Centaur Courser",
    setCode = "M15",
    collectorNumber = "282",
    artist = "Vance Kovacs",
    imageUri = "https://cards.scryfall.io/normal/front/3/b/3b625203-6c50-4b14-928c-6b0aec1375a2.jpg?1783939144",
    releaseDate = "2014-07-18",
    rarity = Rarity.COMMON,
    inBooster = false,
)
