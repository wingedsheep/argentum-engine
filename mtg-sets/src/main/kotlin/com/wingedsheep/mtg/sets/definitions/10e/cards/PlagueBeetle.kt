package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Plague Beetle reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PlagueBeetleReprint = Printing(
    oracleId = "96415c51-2d44-4a96-9caa-9c4fbde59fca",
    name = "Plague Beetle",
    setCode = "10E",
    collectorNumber = "168",
    artist = "Tom Fleming",
    imageUri = "https://cards.scryfall.io/normal/front/8/f/8f5d3dac-98df-433f-a417-bcb9c91722fb.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
