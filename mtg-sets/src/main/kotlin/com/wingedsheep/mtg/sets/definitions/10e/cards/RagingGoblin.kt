package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Raging Goblin reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RagingGoblinReprint = Printing(
    oracleId = "30997b43-fc13-41d3-8064-1ccc2cb6fd2b",
    name = "Raging Goblin",
    setCode = "10E",
    collectorNumber = "224",
    artist = "Jeff Miracola",
    imageUri = "https://cards.scryfall.io/normal/front/1/8/181ed588-5b9e-4160-967c-ed927b7e0048.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
