package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Moss Monster reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MossMonsterReprint = Printing(
    oracleId = "60efa8a2-6903-4756-92fd-29f6ba6ef67a",
    name = "Moss Monster",
    setCode = "8ED",
    collectorNumber = "267",
    artist = "Glen Angus",
    imageUri = "https://cards.scryfall.io/normal/front/1/5/15511fda-92ae-4d27-8a83-37e821ec3adf.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
