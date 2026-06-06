package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Dwarven Demolition Team reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEA's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val DwarvenDemolitionTeamReprint = Printing(
    oracleId = "caf3c6ec-d17e-497c-8fe7-6f818ce93f96",
    name = "Dwarven Demolition Team",
    setCode = "8ED",
    collectorNumber = "184",
    artist = "Kev Walker",
    imageUri = "https://cards.scryfall.io/normal/front/9/4/94d17787-1f59-4e88-ba57-e69f02bb3ffe.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
