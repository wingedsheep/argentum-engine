package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Aven Flock reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * ODY's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val AvenFlockReprint = Printing(
    oracleId = "fcf0bb85-bca9-4f4c-b954-ca3bc5f91dd8",
    name = "Aven Flock",
    setCode = "8ED",
    collectorNumber = "6",
    artist = "Greg Hildebrandt & Tim Hildebrandt",
    imageUri = "https://cards.scryfall.io/normal/front/7/f/7fa12005-465d-40b2-804b-a04cc2686ebd.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
