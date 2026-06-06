package com.wingedsheep.mtg.sets.definitions.`8ed`.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Carrion Wall reprint in 8ED.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * NEM's `cards/` package (the card's earliest real printing). This file contributes only
 * the 8ED-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val CarrionWallReprint = Printing(
    oracleId = "ce8750d4-1b74-4b02-afd8-6b639c1a9a78",
    name = "Carrion Wall",
    setCode = "8ED",
    collectorNumber = "121",
    artist = "Tony Szczudlo",
    imageUri = "https://cards.scryfall.io/normal/front/7/7/77ea9d0e-9188-4447-a9c9-8ed209121b43.jpg",
    releaseDate = "2003-07-28",
    rarity = Rarity.UNCOMMON,
)
