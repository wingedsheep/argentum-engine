package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Mind Sludge reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TOR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MindSludgeReprint = Printing(
    oracleId = "86e64a29-6ed1-451b-a114-f74341322eeb",
    name = "Mind Sludge",
    setCode = "8ED",
    collectorNumber = "146",
    artist = "Eric Peterson",
    imageUri = "https://cards.scryfall.io/normal/front/e/8/e86a26e9-e4cb-452a-9263-489d57233d41.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
