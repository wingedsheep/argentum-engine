package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Remove Soul reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RemoveSoulReprint = Printing(
    oracleId = "b13c0f76-fbda-4911-9442-c3d7e97f1aac",
    name = "Remove Soul",
    setCode = "10E",
    collectorNumber = "100",
    artist = "Adam Rex",
    imageUri = "https://cards.scryfall.io/normal/front/3/5/35673701-684c-4ba6-99f5-74364bc6fec2.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
