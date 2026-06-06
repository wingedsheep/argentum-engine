package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Wall of Spears reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ATQ's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val WallOfSpearsReprint = Printing(
    oracleId = "ed836d84-ff1e-4af8-b4b8-314569b3faec",
    name = "Wall of Spears",
    setCode = "8ED",
    collectorNumber = "320",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/b/4/b4f34f06-7f89-4f2b-8979-8219ac1c4560.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
