package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Looming Shade reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val LoomingShadeReprint = Printing(
    oracleId = "3ce8560d-dc68-4551-81c9-3a7b8d8da9a1",
    name = "Looming Shade",
    setCode = "10E",
    collectorNumber = "153",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/a/b/aba629ab-a322-4a7c-b6e5-477ca1b709bf.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
