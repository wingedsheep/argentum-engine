package com.wingedsheep.mtg.sets.definitions.inr.cards

import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * Village Rites reprint in INR.
 *
 * The canonical [com.wingedsheep.sdk.model.CardDefinition] (script, types, P/T) lives in
 * M21's `cards/` package (the card's earliest real printing). This file contributes only
 * the INR-specific presentation row — set, collector number, art — picked up automatically
 * by `CardDiscovery.findPrintingsIn` and surfaced via the set's `printings`.
 */
val VillageRitesReprint = Printing(
    oracleId = "365548fb-5acc-4a8a-b20b-26d28b7d029f",
    name = "Village Rites",
    setCode = "INR",
    collectorNumber = "137",
    artist = "Bud Cook",
    imageUri = "https://cards.scryfall.io/normal/front/4/2/42ed35e9-51cd-468a-86a9-9412553cf50d.jpg",
    releaseDate = "2025-01-24",
    rarity = Rarity.COMMON,
)
