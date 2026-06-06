package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Spineless Thug reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * NEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SpinelessThugReprint = Printing(
    oracleId = "78bbd213-e8f4-40da-aef5-853244e95756",
    name = "Spineless Thug",
    setCode = "8ED",
    collectorNumber = "166",
    artist = "Matthew D. Wilson",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/711dfcd8-3083-4876-ac3f-1f7c6924382a.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
