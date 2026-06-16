package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Stalking Tiger reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MIR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val StalkingTigerReprint = Printing(
    oracleId = "e6af1429-276f-41a8-88d0-062661dc0cd4",
    name = "Stalking Tiger",
    setCode = "10E",
    collectorNumber = "299",
    artist = "Terese Nielsen",
    imageUri = "https://cards.scryfall.io/normal/front/8/7/8741d0e8-2e4c-49f1-96ad-d3903d29a6fd.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
