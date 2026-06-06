package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fleeting Image reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ULG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FleetingImageReprint = Printing(
    oracleId = "623f3efb-5431-42d4-98aa-9caac40e4e83",
    name = "Fleeting Image",
    setCode = "8ED",
    collectorNumber = "79",
    artist = "Dave Dorman",
    imageUri = "https://cards.scryfall.io/normal/front/b/8/b8c53ee6-9fe0-459e-903f-214a25a07634.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
