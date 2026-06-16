package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Soul Warden reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SoulWardenReprint = Printing(
    oracleId = "f3fad295-1af2-4ecc-8546-b121ad6be27b",
    name = "Soul Warden",
    setCode = "10E",
    collectorNumber = "44",
    artist = "Randy Gallegos",
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5fe5fe13-b57d-4514-89e6-79909474f7e8.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
