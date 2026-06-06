package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Elvish Champion reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * INV's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val ElvishChampionReprint = Printing(
    oracleId = "7e40f37c-9a0c-40e0-b195-7ea94b12f798",
    name = "Elvish Champion",
    setCode = "8ED",
    collectorNumber = "241",
    artist = "Mark Zug",
    imageUri = "https://cards.scryfall.io/normal/front/8/8/88c329c4-6c40-467a-ab37-95896a0c1159.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
