package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Vine Trellis reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * MMQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VineTrellisReprint = Printing(
    oracleId = "57de8fe7-3d1b-41cd-8354-38aff3d2d052",
    name = "Vine Trellis",
    setCode = "8ED",
    collectorNumber = "287",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/b/a/ba462242-82de-4173-98d2-b1b7a1f3d8b4.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
