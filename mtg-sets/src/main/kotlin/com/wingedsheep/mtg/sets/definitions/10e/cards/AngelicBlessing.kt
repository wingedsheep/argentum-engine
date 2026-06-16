package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Angelic Blessing reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AngelicBlessingReprint = Printing(
    oracleId = "d3758fca-0522-4b5a-a1cc-3b2b3ab299ba",
    name = "Angelic Blessing",
    setCode = "10E",
    collectorNumber = "3",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/a/2/a285aa3f-bcfb-4fc3-8441-85a56c72a3e4.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
