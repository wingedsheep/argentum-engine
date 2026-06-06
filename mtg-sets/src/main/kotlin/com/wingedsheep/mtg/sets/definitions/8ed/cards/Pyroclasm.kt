package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pyroclasm reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PyroclasmReprint = Printing(
    oracleId = "e4bcd4ea-e7cd-4471-8f3b-18bb51d3d70c",
    name = "Pyroclasm",
    setCode = "8ED",
    collectorNumber = "210",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/3/4/34ec6e8f-a8be-4efe-8082-d807378066b1.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
