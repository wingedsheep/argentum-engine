package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Fodder Cannon reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * UDS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val FodderCannonReprint = Printing(
    oracleId = "aaf171bd-a4bb-4ce4-836a-da193c94f42e",
    name = "Fodder Cannon",
    setCode = "8ED",
    collectorNumber = "302",
    artist = "Christopher Moeller",
    imageUri = "https://cards.scryfall.io/normal/front/b/d/bde003e6-d674-42cd-9537-91928730e7dd.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
