package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Canyon Wildcat reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * TMP's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CanyonWildcatReprint = Printing(
    oracleId = "dbf7b3ef-1ca4-4b0d-bec2-f5e60b150013",
    name = "Canyon Wildcat",
    setCode = "8ED",
    collectorNumber = "181",
    artist = "Gary Leach",
    imageUri = "https://cards.scryfall.io/normal/front/7/a/7a761bfc-71dc-40be-8184-6e0be2f25d07.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
