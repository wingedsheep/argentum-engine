package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Merfolk Looter reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MerfolkLooterReprint = Printing(
    oracleId = "67362406-b1ca-49e2-800d-9050bfe8742a",
    name = "Merfolk Looter",
    setCode = "10E",
    collectorNumber = "92",
    artist = "Tristan Elwell",
    imageUri = "https://cards.scryfall.io/normal/front/2/8/28e4e99d-5834-4f6c-b915-4f95edc1337f.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
