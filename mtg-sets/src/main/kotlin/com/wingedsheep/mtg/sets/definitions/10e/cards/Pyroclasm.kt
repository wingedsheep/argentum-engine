package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Pyroclasm reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ICE's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PyroclasmReprint = Printing(
    oracleId = "e4bcd4ea-e7cd-4471-8f3b-18bb51d3d70c",
    name = "Pyroclasm",
    setCode = "10E",
    collectorNumber = "222",
    artist = "John Avon",
    imageUri = "https://cards.scryfall.io/normal/front/0/c/0c0e7131-db26-448d-afda-f48337a026f0.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
