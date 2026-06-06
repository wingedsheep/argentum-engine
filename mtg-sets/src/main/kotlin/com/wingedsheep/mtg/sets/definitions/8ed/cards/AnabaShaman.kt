package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Anaba Shaman reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * HML's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AnabaShamanReprint = Printing(
    oracleId = "bc416c17-e1f0-4344-8c21-6184cac10205",
    name = "Anaba Shaman",
    setCode = "8ED",
    collectorNumber = "175",
    artist = "Alex Horley-Orlandelli",
    imageUri = "https://cards.scryfall.io/normal/front/f/3/f3c2a5d1-cdf0-4551-aad0-c6590fb3c018.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
