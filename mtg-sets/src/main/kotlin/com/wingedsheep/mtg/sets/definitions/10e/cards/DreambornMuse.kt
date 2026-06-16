package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dreamborn Muse reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LGN's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DreambornMuseReprint = Printing(
    oracleId = "68d7f338-c6e7-4372-b41c-901e39e08bde",
    name = "Dreamborn Muse",
    setCode = "10E",
    collectorNumber = "82",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/5/3/53ebb96f-185b-4aa5-ad4b-9c02ce44d387.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.RARE,
)
