package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mantis Engine reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * UDS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MantisEngineReprint = Printing(
    oracleId = "57bea272-5b38-444e-acc2-6104af2ece22",
    name = "Mantis Engine",
    setCode = "10E",
    collectorNumber = "333",
    artist = "John Zeleznik",
    imageUri = "https://cards.scryfall.io/normal/front/8/6/86111211-b3d9-42eb-bab7-ee2cf9acda5d.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
