package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Rain of Blades reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * SCG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RainOfBladesReprint = Printing(
    oracleId = "c7d398a4-0eb3-43cc-9952-35f3b791aa7a",
    name = "Rain of Blades",
    setCode = "8ED",
    collectorNumber = "35",
    artist = "Rob Alexander",
    imageUri = "https://cards.scryfall.io/normal/front/7/a/7a25bf0b-7de0-4e71-ae3e-59a6665dc903.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
