package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fists of the Anvil reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FistsOfTheAnvilReprint = Printing(
    oracleId = "fc05e582-e760-4fe2-ba43-e9d8e63f3f85",
    name = "Fists of the Anvil",
    setCode = "10E",
    collectorNumber = "201",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/5/f/5fab4a7e-ef8a-4f38-b659-5598a2ead833.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
