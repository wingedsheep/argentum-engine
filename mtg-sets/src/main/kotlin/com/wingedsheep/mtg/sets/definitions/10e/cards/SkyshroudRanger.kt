package com.wingedsheep.mtg.sets.definitions.`10e`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Skyshroud Ranger reprint in 10E.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 10E-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SkyshroudRangerReprint = Printing(
    oracleId = "8f47a230-7500-4afb-97cd-dbd444c307c8",
    name = "Skyshroud Ranger",
    setCode = "10E",
    collectorNumber = "297",
    artist = "Mark Tedin",
    imageUri = "https://cards.scryfall.io/normal/front/9/a/9a2fd9e5-1986-4998-b08c-80897b9b27e7.jpg",
    releaseDate = "2007-07-13",
    rarity = Rarity.COMMON,
)
