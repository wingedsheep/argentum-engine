package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Merchant Scroll reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * HML's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MerchantScrollReprint = Printing(
    oracleId = "86cebe2a-95e7-4f22-99cc-e805aeaf347e",
    name = "Merchant Scroll",
    setCode = "8ED",
    collectorNumber = "91",
    artist = "David Martin",
    imageUri = "https://cards.scryfall.io/normal/front/b/b/bb5385d2-ca5d-4fb9-934b-b7cc8b38ac89.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
