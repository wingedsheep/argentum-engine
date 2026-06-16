package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dross Crocodile reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * 5DN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DrossCrocodileReprint = Printing(
    oracleId = "028fa430-722d-45aa-b415-3f84076091a9",
    name = "Dross Crocodile",
    setCode = "10E",
    collectorNumber = "138",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/e/f/efd5c07e-4ece-4c8b-93c8-6abd7dd3a39a.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
