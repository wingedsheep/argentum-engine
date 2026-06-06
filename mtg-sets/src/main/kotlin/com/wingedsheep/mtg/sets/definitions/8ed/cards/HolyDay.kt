package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Holy Day reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * LEG's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val HolyDayReprint = Printing(
    oracleId = "98423a34-f044-4811-b288-56981d604b6e",
    name = "Holy Day",
    setCode = "8ED",
    collectorNumber = "23",
    artist = "Pete Venters",
    imageUri = "https://cards.scryfall.io/normal/front/c/b/cb72baf7-d9fe-41b4-862d-3709834ee46b.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.COMMON,
)
