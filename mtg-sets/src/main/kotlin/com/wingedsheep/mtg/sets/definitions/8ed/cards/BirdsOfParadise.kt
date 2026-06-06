package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Birds of Paradise reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BirdsOfParadiseReprint = Printing(
    oracleId = "d3a0b660-358c-41bd-9cd2-41fbf3491b1a",
    name = "Birds of Paradise",
    setCode = "8ED",
    collectorNumber = "233",
    artist = "Edward P. Beard, Jr.",
    imageUri = "https://cards.scryfall.io/normal/front/7/1/715eea77-a7d7-4731-b706-3ea17bded7fd.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
