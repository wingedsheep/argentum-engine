package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Eastern Paladin reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * USG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val EasternPaladinReprint = Printing(
    oracleId = "b445482a-9422-41c7-bedb-3b9f500d2f27",
    name = "Eastern Paladin",
    setCode = "8ED",
    collectorNumber = "131",
    artist = "Carl Critchlow",
    imageUri = "https://cards.scryfall.io/normal/front/d/7/d75f7d29-9c73-4b5e-a6fd-5608bb737278.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.RARE,
)
