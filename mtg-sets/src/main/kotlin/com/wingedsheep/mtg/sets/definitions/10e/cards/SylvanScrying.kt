package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Sylvan Scrying reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MRD's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SylvanScryingReprint = Printing(
    oracleId = "ee24bf27-484d-4e1c-998e-6a74e3d3f6c4",
    name = "Sylvan Scrying",
    setCode = "10E",
    collectorNumber = "302",
    artist = "Scott M. Fischer",
    imageUri = "https://cards.scryfall.io/normal/front/f/a/fafa5aac-d4b6-47c8-b148-3077e2077e53.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.UNCOMMON,
)
