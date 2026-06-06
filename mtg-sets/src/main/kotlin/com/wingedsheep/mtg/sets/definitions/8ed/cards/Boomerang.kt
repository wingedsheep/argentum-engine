package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Boomerang reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val BoomerangReprint = Printing(
    oracleId = "dc4a4996-108a-4aac-850f-2d9f76403446",
    name = "Boomerang",
    setCode = "8ED",
    collectorNumber = "63",
    artist = "Alan Rabinowitz",
    imageUri = "https://cards.scryfall.io/normal/front/2/2/2275b0a5-7777-49cb-9056-aef0a82424db.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
