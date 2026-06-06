package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Ravenous Rats reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * P02's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val RavenousRatsReprint = Printing(
    oracleId = "2fa1bbfd-92b5-482c-b32d-4cdc286474c4",
    name = "Ravenous Rats",
    setCode = "8ED",
    collectorNumber = "158",
    artist = "Tom Wänerstrand",
    imageUri = "https://cards.scryfall.io/normal/front/0/e/0ee0ece8-2598-40d4-a222-27e3b34c01a1.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
