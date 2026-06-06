package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Hulking Cyclops reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * VIS's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HulkingCyclopsReprint = Printing(
    oracleId = "04936918-7ef3-42fa-b0cd-87d18cb69d2c",
    name = "Hulking Cyclops",
    setCode = "8ED",
    collectorNumber = "195",
    artist = "Paolo Parente",
    imageUri = "https://cards.scryfall.io/normal/front/d/c/dc1a5428-9bb4-4e2b-ba94-fccd321ce537.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
