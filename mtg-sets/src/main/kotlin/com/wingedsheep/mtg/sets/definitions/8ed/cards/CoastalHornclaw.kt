package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Coastal Hornclaw reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PCY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CoastalHornclawReprint = Printing(
    oracleId = "27d672a7-f73b-483d-80a3-a4cd2a9855f2",
    name = "Coastal Hornclaw",
    setCode = "8ED",
    collectorNumber = "66",
    artist = "DiTerlizzi",
    imageUri = "https://cards.scryfall.io/normal/front/9/b/9b152c0d-ba43-49c6-9bb5-1d20a9f411f6.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
