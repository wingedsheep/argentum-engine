package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Viashino Runner reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ViashinoRunnerReprint = Printing(
    oracleId = "ac7e317b-387c-403f-bcf1-403f96302f21",
    name = "Viashino Runner",
    setCode = "10E",
    collectorNumber = "245",
    artist = "Steve White",
    imageUri = "https://cards.scryfall.io/normal/front/8/a/8aa862a0-388d-43b8-973f-3a00ebf53952.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
