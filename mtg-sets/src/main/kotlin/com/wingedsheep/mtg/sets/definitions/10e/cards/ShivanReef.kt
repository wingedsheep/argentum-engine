package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Shivan Reef reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * APC's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ShivanReefReprint = Printing(
    oracleId = "0fe16212-66c3-4e45-a641-7391e9b2e304",
    name = "Shivan Reef",
    setCode = "10E",
    collectorNumber = "357",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/d/6/d62ea973-7799-4179-9d36-60e2287feae7.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
