package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Snapping Drake reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SnappingDrakeReprint = Printing(
    oracleId = "e15060c3-3773-4548-8747-ff59dcf2b519",
    name = "Snapping Drake",
    setCode = "10E",
    collectorNumber = "110",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/0/3/03b2ad93-8287-4b87-88ed-c627bd54fd49.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
