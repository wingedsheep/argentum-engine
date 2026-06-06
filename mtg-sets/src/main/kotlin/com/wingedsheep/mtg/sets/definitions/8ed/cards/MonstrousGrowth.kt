package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Monstrous Growth reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * POR's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val MonstrousGrowthReprint = Printing(
    oracleId = "35a05836-38d7-45c7-ac9a-996a682c2129",
    name = "Monstrous Growth",
    setCode = "8ED",
    collectorNumber = "266",
    artist = "Ron Spencer",
    imageUri = "https://cards.scryfall.io/normal/front/7/e/7e5a2687-af7a-42ad-8938-f3534c7da222.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
