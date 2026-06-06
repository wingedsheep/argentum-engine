package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Peach Garden Oath reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PTK's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val PeachGardenOathReprint = Printing(
    oracleId = "ad66a99b-c7e7-4b2c-8525-299d3609d1df",
    name = "Peach Garden Oath",
    setCode = "8ED",
    collectorNumber = "34",
    artist = "Qiao Dafu",
    imageUri = "https://cards.scryfall.io/normal/front/d/0/d05bd3b7-02fb-4d83-ab8f-fd056c7db2f1.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
