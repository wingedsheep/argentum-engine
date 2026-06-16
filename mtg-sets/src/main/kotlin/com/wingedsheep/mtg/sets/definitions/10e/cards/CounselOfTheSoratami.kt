package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Counsel of the Soratami reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * CHK's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CounselOfTheSoratamiReprint = Printing(
    oracleId = "62ddc5ae-ced9-4319-854c-1a114c6afc3f",
    name = "Counsel of the Soratami",
    setCode = "10E",
    collectorNumber = "76",
    artist = "Randy Gallegos",
    imageUri = "https://cards.scryfall.io/normal/front/1/2/1224718a-e1a7-473d-ac9d-497e624376cd.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
