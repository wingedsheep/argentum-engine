package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Slay reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * PLS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val SlayReprint = Printing(
    oracleId = "64418e1f-4fb1-4029-a31e-135b2d2c0ab9",
    name = "Slay",
    setCode = "8ED",
    collectorNumber = "164",
    artist = "Ben Thompson",
    imageUri = "https://cards.scryfall.io/normal/front/c/b/cba5a03c-8b5b-496e-81bf-0d6e36960907.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
