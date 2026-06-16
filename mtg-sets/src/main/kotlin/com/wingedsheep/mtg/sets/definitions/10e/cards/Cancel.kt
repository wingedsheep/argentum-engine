package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Cancel reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TSP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CancelReprint = Printing(
    oracleId = "7d00fb28-ea6c-49a9-b4af-ffb38860a9a7",
    name = "Cancel",
    setCode = "10E",
    collectorNumber = "71",
    artist = "Mark Poole",
    imageUri = "https://cards.scryfall.io/normal/front/1/8/1850d1d5-f506-4e50-9d51-408f987bbbbd.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
