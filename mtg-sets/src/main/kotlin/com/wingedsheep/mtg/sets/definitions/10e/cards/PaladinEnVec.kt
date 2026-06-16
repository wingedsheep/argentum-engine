package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Paladin en-Vec reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * EXO's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PaladinEnVecReprint = Printing(
    oracleId = "fd8722e1-9c41-4037-ab7e-47e2aa9858a0",
    name = "Paladin en-Vec",
    setCode = "10E",
    collectorNumber = "32",
    artist = "Dave Kendall",
    imageUri = "https://cards.scryfall.io/normal/front/a/e/aea1a8e1-35f4-4b4a-aa68-21b5219527a9.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
